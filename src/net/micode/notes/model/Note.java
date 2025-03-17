/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 该包下的类主要负责笔记数据的模型相关操作，包含笔记的创建、修改、同步等功能。
 */
package net.micode.notes.model;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;

import java.util.ArrayList;

/**
 * Note 类表示笔记实体，负责管理笔记的基本信息和数据，提供创建、修改和同步笔记的功能。
 */
public class Note {
    // 存储笔记差异值的 ContentValues 对象
    private ContentValues mNoteDiffValues;
    // 存储笔记数据的 NoteData 对象
    private NoteData mNoteData;
    // 日志标签
    private static final String TAG = "Note";

    /**
     * 创建一个新的笔记 ID，用于向数据库中添加新笔记。
     * 
     * @param context 上下文对象
     * @param folderId 笔记所属文件夹的 ID
     * @return 新笔记的 ID
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // Create a new note in the database
        ContentValues values = new ContentValues();
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime);
        values.put(NoteColumns.MODIFIED_DATE, createdTime);
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        values.put(NoteColumns.PARENT_ID, folderId);
        // 向数据库插入新笔记并获取返回的 URI
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values);

        long noteId = 0;
        try {
            // 从 URI 中解析出笔记 ID
            noteId = Long.valueOf(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Get note id error :" + e.toString());
            noteId = 0;
        }
        if (noteId == -1) {
            throw new IllegalStateException("Wrong note id:" + noteId);
        }
        return noteId;
    }

    /**
     * 构造函数，初始化笔记差异值和笔记数据对象。
     */
    public Note() {
        mNoteDiffValues = new ContentValues();
        mNoteData = new NoteData();
    }

    /**
     * 设置笔记的某个属性值，并标记笔记为本地修改。
     * 
     * @param key 属性名
     * @param value 属性值
     */
    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value);
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
    }

    /**
     * 设置笔记的文本数据的某个属性值。
     * 
     * @param key 属性名
     * @param value 属性值
     */
    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value);
    }

    /**
     * 设置笔记的文本数据 ID。
     * 
     * @param id 文本数据 ID
     */
    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id);
    }

    /**
     * 获取笔记的文本数据 ID。
     * 
     * @return 文本数据 ID
     */
    public long getTextDataId() {
        return mNoteData.mTextDataId;
    }

    /**
     * 设置笔记的通话数据 ID。
     * 
     * @param id 通话数据 ID
     */
    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id);
    }

    /**
     * 设置笔记的通话数据的某个属性值。
     * 
     * @param key 属性名
     * @param value 属性值
     */
    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value);
    }

    /**
     * 判断笔记是否有本地修改。
     * 
     * @return 如果有本地修改返回 true，否则返回 false
     */
    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified();
    }

    /**
     * 同步笔记到数据库。
     * 
     * @param context 上下文对象
     * @param noteId 笔记 ID
     * @return 同步成功返回 true，否则返回 false
     */
    public boolean syncNote(Context context, long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Wrong note id:" + noteId);
        }

        if (!isLocalModified()) {
            return true;
        }

        /**
         * In theory, once data changed, the note should be updated on {@link NoteColumns#LOCAL_MODIFIED} and
         * {@link NoteColumns#MODIFIED_DATE}. For data safety, though update note fails, we also update the
         * note data info
         */
        // 更新笔记的基本信息
        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            Log.e(TAG, "Update note error, should not happen");
            // Do not return, fall through
        }
        mNoteDiffValues.clear();

        // 同步笔记的数据信息
        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false;
        }

        return true;
    }

    /**
     * 内部类，用于管理笔记的数据信息，包括文本数据和通话数据。
     */
    private class NoteData {
        // 文本数据 ID
        private long mTextDataId;
        // 存储文本数据的 ContentValues 对象
        private ContentValues mTextDataValues;
        // 通话数据 ID
        private long mCallDataId;
        // 存储通话数据的 ContentValues 对象
        private ContentValues mCallDataValues;
        // 日志标签
        private static final String TAG = "NoteData";

        /**
         * 构造函数，初始化文本数据和通话数据的 ContentValues 对象。
         */
        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        }

        /**
         * 判断笔记数据是否有本地修改。
         * 
         * @return 如果有本地修改返回 true，否则返回 false
         */
        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0;
        }

        /**
         * 设置文本数据 ID。
         * 
         * @param id 文本数据 ID
         */
        void setTextDataId(long id) {
            if(id <= 0) {
                throw new IllegalArgumentException("Text data id should larger than 0");
            }
            mTextDataId = id;
        }

        /**
         * 设置通话数据 ID。
         * 
         * @param id 通话数据 ID
         */
        void setCallDataId(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("Call data id should larger than 0");
            }
            mCallDataId = id;
        }

        /**
         * 设置通话数据的某个属性值，并标记笔记为本地修改。
         * 
         * @param key 属性名
         * @param value 属性值
         */
        void setCallData(String key, String value) {
            mCallDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }

        /**
         * 设置文本数据的某个属性值，并标记笔记为本地修改。
         * 
         * @param key 属性名
         * @param value 属性值
         */
        void setTextData(String key, String value) {
            mTextDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }

        /**
         * 将笔记数据同步到 ContentResolver 中。
         * 
         * @param context 上下文对象
         * @param noteId 笔记 ID
         * @return 同步成功返回笔记的 URI，否则返回 null
         */
        Uri pushIntoContentResolver(Context context, long noteId) {
            /**
             * Check for safety
             */
            if (noteId <= 0) {
                throw new IllegalArgumentException("Wrong note id:" + noteId);
            }

            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;

            if(mTextDataValues.size() > 0) {
                mTextDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mTextDataId == 0) {
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                    // 插入新的文本数据
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mTextDataValues);
                    try {
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new text data fail with noteId" + noteId);
                        mTextDataValues.clear();
                        return null;
                    }
                } else {
                    // 更新已有的文本数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues);
                    operationList.add(builder.build());
                }
                mTextDataValues.clear();
            }

            if(mCallDataValues.size() > 0) {
                mCallDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mCallDataId == 0) {
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                    // 插入新的通话数据
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mCallDataValues);
                    try {
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new call data fail with noteId" + noteId);
                        mCallDataValues.clear();
                        return null;
                    }
                } else {
                    // 更新已有的通话数据
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues);
                    operationList.add(builder.build());
                }
                mCallDataValues.clear();
            }

            if (operationList.size() > 0) {
                try {
                    // 批量执行操作
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(
                            Notes.AUTHORITY, operationList);
                    return (results == null || results.length == 0 || results[0] == null) ? null
                            : ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId);
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                }
            }
            return null;
        }
    }
}
