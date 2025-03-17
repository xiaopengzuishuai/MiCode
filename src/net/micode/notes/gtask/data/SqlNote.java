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

// 包声明，指定该类所属的包
package net.micode.notes.gtask.data;

// 导入 AppWidgetManager 类，用于管理应用小部件
import android.appwidget.AppWidgetManager;
// 导入 ContentResolver 类，用于与内容提供者进行交互
import android.content.ContentResolver;
// 导入 ContentValues 类，用于存储键值对，通常用于数据库操作
import android.content.ContentValues;
// 导入 Context 类，提供对应用程序环境的访问
import android.content.Context;
// 导入 Cursor 类，用于遍历数据库查询结果
import android.database.Cursor;
// 导入 Uri 类，用于表示统一资源标识符
import android.net.Uri;
// 导入 Log 类，用于记录日志信息
import android.util.Log;

// 导入 Notes 类，可能包含笔记相关的常量和数据结构
import net.micode.notes.data.Notes;
// 导入 Notes 类中的 DataColumns 内部类，包含数据列的常量
import net.micode.notes.data.Notes.DataColumns;
// 导入 Notes 类中的 NoteColumns 内部类，包含笔记列的常量
import net.micode.notes.data.Notes.NoteColumns;
// 导入 ActionFailureException 类，用于处理操作失败的异常
import net.micode.notes.gtask.exception.ActionFailureException;
// 导入 GTaskStringUtils 类，可能包含与 Google 任务相关的字符串处理工具
import net.micode.notes.tool.GTaskStringUtils;
// 导入 ResourceParser 类，可能用于解析资源
import net.micode.notes.tool.ResourceParser;

// 导入 JSONArray 类，用于处理 JSON 数组
import org.json.JSONArray;
// 导入 JSONException 类，用于处理 JSON 解析异常
import org.json.JSONException;
// 导入 JSONObject 类，用于处理 JSON 对象
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * SqlNote 类用于管理和操作笔记数据，包括从数据库加载笔记、设置笔记内容以及获取笔记内容等功能。
 */
public class SqlNote {
    // 日志标签，用于标识该类的日志信息
    private static final String TAG = SqlNote.class.getSimpleName();
    // 无效 ID 的常量值
    private static final int INVALID_ID = -99999;
    // 查询笔记时使用的投影列数组
    public static final String[] PROJECTION_NOTE = new String[] {
            NoteColumns.ID, NoteColumns.ALERTED_DATE, NoteColumns.BG_COLOR_ID,
            NoteColumns.CREATED_DATE, NoteColumns.HAS_ATTACHMENT, NoteColumns.MODIFIED_DATE,
            NoteColumns.NOTES_COUNT, NoteColumns.PARENT_ID, NoteColumns.SNIPPET, NoteColumns.TYPE,
            NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE, NoteColumns.SYNC_ID,
            NoteColumns.LOCAL_MODIFIED, NoteColumns.ORIGIN_PARENT_ID, NoteColumns.GTASK_ID,
            NoteColumns.VERSION
    };
    // 投影列数组中 ID 列的索引
    public static final int ID_COLUMN = 0;
    // 投影列数组中提醒日期列的索引
    public static final int ALERTED_DATE_COLUMN = 1;
    // 投影列数组中背景颜色 ID 列的索引
    public static final int BG_COLOR_ID_COLUMN = 2;
    // 投影列数组中创建日期列的索引
    public static final int CREATED_DATE_COLUMN = 3;
    // 投影列数组中是否有附件列的索引
    public static final int HAS_ATTACHMENT_COLUMN = 4;
    // 投影列数组中修改日期列的索引
    public static final int MODIFIED_DATE_COLUMN = 5;
    // 投影列数组中笔记数量列的索引
    public static final int NOTES_COUNT_COLUMN = 6;
    // 投影列数组中父 ID 列的索引
    public static final int PARENT_ID_COLUMN = 7;
    // 投影列数组中摘要列的索引
    public static final int SNIPPET_COLUMN = 8;
    // 投影列数组中类型列的索引
    public static final int TYPE_COLUMN = 9;
    // 投影列数组中小部件 ID 列的索引
    public static final int WIDGET_ID_COLUMN = 10;
    // 投影列数组中小部件类型列的索引
    public static final int WIDGET_TYPE_COLUMN = 11;
    // 投影列数组中同步 ID 列的索引
    public static final int SYNC_ID_COLUMN = 12;
    // 投影列数组中本地修改列的索引
    public static final int LOCAL_MODIFIED_COLUMN = 13;
    // 投影列数组中原始父 ID 列的索引
    public static final int ORIGIN_PARENT_ID_COLUMN = 14;
    // 投影列数组中 Google 任务 ID 列的索引
    public static final int GTASK_ID_COLUMN = 15;
    // 投影列数组中版本列的索引
    public static final int VERSION_COLUMN = 16;

    // 应用程序上下文
    private Context mContext;
    // 内容解析器，用于与内容提供者交互
    private ContentResolver mContentResolver;
    // 标记该笔记是否为新创建的
    private boolean mIsCreate;
    // 笔记的 ID
    private long mId;
    // 笔记的提醒日期
    private long mAlertDate;
    // 笔记的背景颜色 ID
    private int mBgColorId;
    // 笔记的创建日期
    private long mCreatedDate;
    // 笔记是否有附件
    private int mHasAttachment;
    // 笔记的修改日期
    private long mModifiedDate;
    // 笔记的父 ID
    private long mParentId;
    // 笔记的摘要
    private String mSnippet;
    // 笔记的类型
    private int mType;
    // 笔记小部件的 ID
    private int mWidgetId;
    // 笔记小部件的类型
    private int mWidgetType;
    // 笔记的原始父 ID
    private long mOriginParent;
    // 笔记的版本
    private long mVersion;
    // 存储笔记差异值的 ContentValues 对象
    private ContentValues mDiffNoteValues;
    // 存储笔记相关数据的列表
    private ArrayList<SqlData> mDataList;

    /**
     * 构造函数，用于创建一个新的笔记对象。
     *
     * @param context 应用程序上下文
     */
    public SqlNote(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = true;
        mId = INVALID_ID;
        mAlertDate = 0;
        mBgColorId = ResourceParser.getDefaultBgId(context);
        mCreatedDate = System.currentTimeMillis();
        mHasAttachment = 0;
        mModifiedDate = System.currentTimeMillis();
        mParentId = 0;
        mSnippet = "";
        mType = Notes.TYPE_NOTE;
        mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
        mOriginParent = 0;
        mVersion = 0;
        mDiffNoteValues = new ContentValues();
        mDataList = new ArrayList<SqlData>();
    }

    /**
     * 构造函数，用于从 Cursor 中加载笔记数据。
     *
     * @param context 应用程序上下文
     * @param c       包含笔记数据的 Cursor 对象
     */
    public SqlNote(Context context, Cursor c) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(c);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();
    }

    /**
     * 构造函数，用于根据笔记 ID 从数据库中加载笔记数据。
     *
     * @param context 应用程序上下文
     * @param id      笔记的 ID
     */
    public SqlNote(Context context, long id) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(id);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();
    }

    /**
     * 根据笔记 ID 从数据库中查询并加载笔记数据。
     *
     * @param id 笔记的 ID
     */
    private void loadFromCursor(long id) {
        Cursor c = null;
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, PROJECTION_NOTE, "(_id=?)",
                    new String[] {
                        String.valueOf(id)
                    }, null);
            if (c != null) {
                c.moveToNext();
                loadFromCursor(c);
            } else {
                Log.w(TAG, "loadFromCursor: cursor = null");
            }
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * 从 Cursor 中加载笔记数据。
     *
     * @param c 包含笔记数据的 Cursor 对象
     */
    private void loadFromCursor(Cursor c) {
        mId = c.getLong(ID_COLUMN);
        mAlertDate = c.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = c.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = c.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = c.getInt(HAS_ATTACHMENT_COLUMN);
        mModifiedDate = c.getLong(MODIFIED_DATE_COLUMN);
        mParentId = c.getLong(PARENT_ID_COLUMN);
        mSnippet = c.getString(SNIPPET_COLUMN);
        mType = c.getInt(TYPE_COLUMN);
        mWidgetId = c.getInt(WIDGET_ID_COLUMN);
        mWidgetType = c.getInt(WIDGET_TYPE_COLUMN);
        mVersion = c.getLong(VERSION_COLUMN);
    }

    /**
     * 加载笔记相关的数据内容。
     */
    private void loadDataContent() {
        Cursor c = null;
        mDataList.clear();
        try {
            c = mContentResolver.query(Notes.CONTENT_DATA_URI, SqlData.PROJECTION_DATA,
                    "(note_id=?)", new String[] {
                        String.valueOf(mId)
                    }, null);
            if (c != null) {
                if (c.getCount() == 0) {
                    Log.w(TAG, "it seems that the note has not data");
                    return;
                }
                while (c.moveToNext()) {
                    SqlData data = new SqlData(mContext, c);
                    mDataList.add(data);
                }
            } else {
                Log.w(TAG, "loadDataContent: cursor = null");
            }
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * 根据 JSON 对象设置笔记的内容。
     *
     * @param js 包含笔记内容的 JSON 对象
     * @return 如果设置成功返回 true，否则返回 false
     */
    public boolean setContent(JSONObject js) {
        try {
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                Log.w(TAG, "cannot set system folder");
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                // 对于文件夹，只能更新摘要和类型
                String snippet = note.has(NoteColumns.SNIPPET) ? note
                        .getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE)
                        : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_NOTE) {
                JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                long id = note.has(NoteColumns.ID) ? note.getLong(NoteColumns.ID) : INVALID_ID;
                if (mIsCreate || mId != id) {
                    mDiffNoteValues.put(NoteColumns.ID, id);
                }
                mId = id;

                long alertDate = note.has(NoteColumns.ALERTED_DATE) ? note
                        .getLong(NoteColumns.ALERTED_DATE) : 0;
                if (mIsCreate || mAlertDate != alertDate) {
                    mDiffNoteValues.put(NoteColumns.ALERTED_DATE, alertDate);
                }
                mAlertDate = alertDate;

                int bgColorId = note.has(NoteColumns.BG_COLOR_ID) ? note
                        .getInt(NoteColumns.BG_COLOR_ID) : ResourceParser.getDefaultBgId(mContext);
                if (mIsCreate || mBgColorId != bgColorId) {
                    mDiffNoteValues.put(NoteColumns.BG_COLOR_ID, bgColorId);
                }
                mBgColorId = bgColorId;

                long createDate = note.has(NoteColumns.CREATED_DATE) ? note
                        .getLong(NoteColumns.CREATED_DATE) : System.currentTimeMillis();
                if (mIsCreate || mCreatedDate != createDate) {
                    mDiffNoteValues.put(NoteColumns.CREATED_DATE, createDate);
                }
                mCreatedDate = createDate;

                int hasAttachment = note.has(NoteColumns.HAS_ATTACHMENT) ? note
                        .getInt(NoteColumns.HAS_ATTACHMENT) : 0;
                if (mIsCreate || mHasAttachment != hasAttachment) {
                    mDiffNoteValues.put(NoteColumns.HAS_ATTACHMENT, hasAttachment);
                }
                mHasAttachment = hasAttachment;

                long modifiedDate = note.has(NoteColumns.MODIFIED_DATE) ? note
                        .getLong(NoteColumns.MODIFIED_DATE) : System.currentTimeMillis();
                if (mIsCreate || mModifiedDate != modifiedDate) {
                    mDiffNoteValues.put(NoteColumns.MODIFIED_DATE, modifiedDate);
                }
                mModifiedDate = modifiedDate;

                long parentId = note.has(NoteColumns.PARENT_ID) ? note
                        .getLong(NoteColumns.PARENT_ID) : 0;
                if (mIsCreate || mParentId != parentId) {
                    mDiffNoteValues.put(NoteColumns.PARENT_ID, parentId);
                }
                mParentId = parentId;

                String snippet = note.has(NoteColumns.SNIPPET) ? note
                        .getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE)
                        : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;

                int widgetId = note.has(NoteColumns.WIDGET_ID) ? note.getInt(NoteColumns.WIDGET_ID)
                        : AppWidgetManager.INVALID_APPWIDGET_ID;
                if (mIsCreate || mWidgetId != widgetId) {
                    mDiffNoteValues.put(NoteColumns.WIDGET_ID, widgetId);
                }
                mWidgetId = widgetId;

                int widgetType = note.has(NoteColumns.WIDGET_TYPE) ? note
                        .getInt(NoteColumns.WIDGET_TYPE) : Notes.TYPE_WIDGET_INVALIDE;
                if (mIsCreate || mWidgetType != widgetType) {
                    mDiffNoteValues.put(NoteColumns.WIDGET_TYPE, widgetType);
                }
                mWidgetType = widgetType;

                long originParent = note.has(NoteColumns.ORIGIN_PARENT_ID) ? note
                        .getLong(NoteColumns.ORIGIN_PARENT_ID) : 0;
                if (mIsCreate || mOriginParent != originParent) {
                    mDiffNoteValues.put(NoteColumns.ORIGIN_PARENT_ID, originParent);
                }
                mOriginParent = originParent;

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);
                    SqlData sqlData = null;
                    if (data.has(DataColumns.ID)) {
                        long dataId = data.getLong(DataColumns.ID);
                        for (SqlData temp : mDataList) {
                            if (dataId == temp.getId()) {
                                sqlData = temp;
                            }
                        }
                    }

                    if (sqlData == null) {
                        sqlData = new SqlData(mContext);
                        mDataList.add(sqlData);
                    }

                    sqlData.setContent(data);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取笔记的内容并以 JSON 对象的形式返回。
     *
     * @return 包含笔记内容的 JSON 对象，如果笔记未创建则返回 null
     */
    public JSONObject getContent() {
        try {
            JSONObject js = new JSONObject();

            if (mIsCreate) {
                Log.e(TAG, "it seems that we haven't created this in database yet");
                return null;
            }

            JSONObject note = new JSONObject();
            if (mType == Notes.TYPE_NOTE) {
                note.put(NoteColumns.ID, mId);
                note.put(NoteColumns.ALERTED_DATE, mAlertDate);
                note.put(NoteColumns.BG_COLOR_ID, mBgColorId);
                note.put(NoteColumns.CREATED_DATE, mCreatedDate);
                note.put(NoteColumns.HAS_ATTACHMENT, mHasAttachment);
                note.put(NoteColumns.MODIFIED_DATE, mModifiedDate);
                note.put(NoteColumns.PARENT_ID, mParentId);
                note.put(NoteColumns.SNIPPET, mSnippet);
                note.put(NoteColumns.TYPE, mType);
                note.put(NoteColumns.WIDGET_ID, mWidgetId);
                note.put(NoteColumns.WIDGET_TYPE, mWidgetType);
                note.put(NoteColumns.ORIGIN_PARENT_ID, mOriginParent);
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);

                JSONArray dataArray = new JSONArray();
                for (SqlData sqlData : mDataList) {
                    JSONObject data = sqlData.getContent();
                    if (data != null) {
                        dataArray.put(data);
                    }
                }
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray);
            } else if (mType == Notes.TYPE_FOLDER || mType == Notes.TYPE_SYSTEM) {
                note.put(NoteColumns.ID, mId);
                note.put(NoteColumns.TYPE, mType);
                note.put(NoteColumns.SNIPPET, mSnippet);
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);
            }

            return js;
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置笔记的父 ID。
     *
     * @param id 父 ID
     */
    public void setParentId(long id) {
        mParentId = id;
        mDiffNoteValues.put(NoteColumns.PARENT_ID, id);
    }

    /**
     * 设置笔记的 Google 任务 ID。
     *
     * @param gid Google 任务 ID
     */
    public void setGtaskId(String gid) {
        mDiffNoteValues.put(NoteColumns.GTASK_ID, gid);
    }

    public void setSyncId(long syncId) {
        mDiffNoteValues.put(NoteColumns.SYNC_ID, syncId);
    }

    public void resetLocalModified() {
        mDiffNoteValues.put(NoteColumns.LOCAL_MODIFIED, 0);
    }

    public long getId() {
        return mId;
    }

    public long getParentId() {
        return mParentId;
    }

    public String getSnippet() {
        return mSnippet;
    }

    public boolean isNoteType() {
        return mType == Notes.TYPE_NOTE;
    }

    public void commit(boolean validateVersion) {
        if (mIsCreate) {
            if (mId == INVALID_ID && mDiffNoteValues.containsKey(NoteColumns.ID)) {
                mDiffNoteValues.remove(NoteColumns.ID);
            }

            Uri uri = mContentResolver.insert(Notes.CONTENT_NOTE_URI, mDiffNoteValues);
            try {
                mId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Get note id error :" + e.toString());
                throw new ActionFailureException("create note failed");
            }
            if (mId == 0) {
                throw new IllegalStateException("Create thread id failed");
            }

            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, false, -1);
                }
            }
        } else {
            if (mId <= 0 && mId != Notes.ID_ROOT_FOLDER && mId != Notes.ID_CALL_RECORD_FOLDER) {
                Log.e(TAG, "No such note");
                throw new IllegalStateException("Try to update note with invalid id");
            }
            if (mDiffNoteValues.size() > 0) {
                mVersion ++;
                int result = 0;
                if (!validateVersion) {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                            + NoteColumns.ID + "=?)", new String[] {
                        String.valueOf(mId)
                    });
                } else {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                            + NoteColumns.ID + "=?) AND (" + NoteColumns.VERSION + "<=?)",
                            new String[] {
                                    String.valueOf(mId), String.valueOf(mVersion)
                            });
                }
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }

            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, validateVersion, mVersion);
                }
            }
        }

        // refresh local info
        loadFromCursor(mId);
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();

        mDiffNoteValues.clear();
        mIsCreate = false;
    }
}
