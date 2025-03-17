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

// 包声明，指定该类所在的包
package net.micode.notes.gtask.data;

// 导入 Android 系统相关类，用于处理内容解析、URI 操作、数据库游标等
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

// 导入自定义的笔记数据相关类
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;
// 导入自定义的异常类
import net.micode.notes.gtask.exception.ActionFailureException;

// 导入 JSON 处理相关类
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SqlData 类用于处理与笔记数据相关的数据库操作，
 * 包括从数据库加载数据、将数据保存到数据库、设置和获取数据内容等功能。
 */
public class SqlData {
    // 日志标签，用于在日志中标识该类的输出
    private static final String TAG = SqlData.class.getSimpleName();
    // 定义无效 ID 的常量，用于标识未初始化或无效的 ID
    private static final int INVALID_ID = -99999;

    // 定义查询数据时需要投影的列，即需要从数据库中查询的列
    public static final String[] PROJECTION_DATA = new String[] {
            DataColumns.ID, DataColumns.MIME_TYPE, DataColumns.CONTENT, DataColumns.DATA1,
            DataColumns.DATA3
    };

    // 定义投影列在查询结果游标中的索引
    public static final int DATA_ID_COLUMN = 0;
    public static final int DATA_MIME_TYPE_COLUMN = 1;
    public static final int DATA_CONTENT_COLUMN = 2;
    public static final int DATA_CONTENT_DATA_1_COLUMN = 3;
    public static final int DATA_CONTENT_DATA_3_COLUMN = 4;

    // 内容解析器，用于与 ContentProvider 进行交互，实现对数据库的操作
    private ContentResolver mContentResolver;
    // 标识数据是否为新创建的
    private boolean mIsCreate;
    // 数据的 ID
    private long mDataId;
    // 数据的 MIME 类型
    private String mDataMimeType;
    // 数据的内容
    private String mDataContent;
    // 数据的 DATA1 字段值
    private long mDataContentData1;
    // 数据的 DATA3 字段值
    private String mDataContentData3;
    // 用于存储数据差异的 ContentValues 对象，用于更新数据库
    private ContentValues mDiffDataValues;

    /**
     * 构造函数，用于创建一个新的 SqlData 对象。
     *
     * @param context 上下文对象，用于获取 ContentResolver
     */
    public SqlData(Context context) {
        // 获取上下文的 ContentResolver
        mContentResolver = context.getContentResolver();
        // 标记数据为新创建
        mIsCreate = true;
        // 初始化数据 ID 为无效 ID
        mDataId = INVALID_ID;
        // 初始化 MIME 类型为默认的笔记类型
        mDataMimeType = DataConstants.NOTE;
        // 初始化内容为空字符串
        mDataContent = "";
        // 初始化 DATA1 字段值为 0
        mDataContentData1 = 0;
        // 初始化 DATA3 字段值为空字符串
        mDataContentData3 = "";
        // 初始化差异数据存储对象
        mDiffDataValues = new ContentValues();
    }

    /**
     * 构造函数，用于从数据库游标中加载数据创建 SqlData 对象。
     *
     * @param context 上下文对象，用于获取 ContentResolver
     * @param c       数据库游标，包含要加载的数据
     */
    public SqlData(Context context, Cursor c) {
        // 获取上下文的 ContentResolver
        mContentResolver = context.getContentResolver();
        // 标记数据不是新创建的
        mIsCreate = false;
        // 从游标中加载数据
        loadFromCursor(c);
        // 初始化差异数据存储对象
        mDiffDataValues = new ContentValues();
    }

    /**
     * 从数据库游标中加载数据到当前对象。
     *
     * @param c 数据库游标，包含要加载的数据
     */
    private void loadFromCursor(Cursor c) {
        // 从游标中获取数据 ID
        mDataId = c.getLong(DATA_ID_COLUMN);
        // 从游标中获取 MIME 类型
        mDataMimeType = c.getString(DATA_MIME_TYPE_COLUMN);
        // 从游标中获取数据内容
        mDataContent = c.getString(DATA_CONTENT_COLUMN);
        // 从游标中获取 DATA1 字段值
        mDataContentData1 = c.getLong(DATA_CONTENT_DATA_1_COLUMN);
        // 从游标中获取 DATA3 字段值
        mDataContentData3 = c.getString(DATA_CONTENT_DATA_3_COLUMN);
    }

    /**
     * 根据传入的 JSON 对象设置数据内容，并记录数据差异。
     *
     * @param js 包含数据内容的 JSON 对象
     * @throws JSONException 如果解析 JSON 对象时发生错误
     */
    public void setContent(JSONObject js) throws JSONException {
        // 从 JSON 对象中获取数据 ID，如果不存在则使用无效 ID
        long dataId = js.has(DataColumns.ID) ? js.getLong(DataColumns.ID) : INVALID_ID;
        // 如果是新创建的数据或者 ID 不同，则记录差异
        if (mIsCreate || mDataId != dataId) {
            mDiffDataValues.put(DataColumns.ID, dataId);
        }
        // 更新数据 ID
        mDataId = dataId;

        // 从 JSON 对象中获取 MIME 类型，如果不存在则使用默认的笔记类型
        String dataMimeType = js.has(DataColumns.MIME_TYPE) ? js.getString(DataColumns.MIME_TYPE)
                : DataConstants.NOTE;
        // 如果是新创建的数据或者 MIME 类型不同，则记录差异
        if (mIsCreate || !mDataMimeType.equals(dataMimeType)) {
            mDiffDataValues.put(DataColumns.MIME_TYPE, dataMimeType);
        }
        // 更新 MIME 类型
        mDataMimeType = dataMimeType;

        // 从 JSON 对象中获取数据内容，如果不存在则使用空字符串
        String dataContent = js.has(DataColumns.CONTENT) ? js.getString(DataColumns.CONTENT) : "";
        // 如果是新创建的数据或者内容不同，则记录差异
        if (mIsCreate || !mDataContent.equals(dataContent)) {
            mDiffDataValues.put(DataColumns.CONTENT, dataContent);
        }
        // 更新数据内容
        mDataContent = dataContent;

        // 从 JSON 对象中获取 DATA1 字段值，如果不存在则使用 0
        long dataContentData1 = js.has(DataColumns.DATA1) ? js.getLong(DataColumns.DATA1) : 0;
        // 如果是新创建的数据或者 DATA1 字段值不同，则记录差异
        if (mIsCreate || mDataContentData1 != dataContentData1) {
            mDiffDataValues.put(DataColumns.DATA1, dataContentData1);
        }
        // 更新 DATA1 字段值
        mDataContentData1 = dataContentData1;

        // 从 JSON 对象中获取 DATA3 字段值，如果不存在则使用空字符串
        String dataContentData3 = js.has(DataColumns.DATA3) ? js.getString(DataColumns.DATA3) : "";
        // 如果是新创建的数据或者 DATA3 字段值不同，则记录差异
        if (mIsCreate || !mDataContentData3.equals(dataContentData3)) {
            mDiffDataValues.put(DataColumns.DATA3, dataContentData3);
        }
        // 更新 DATA3 字段值
        mDataContentData3 = dataContentData3;
    }

    /**
     * 获取当前对象的数据内容并封装为 JSON 对象。
     *
     * @return 包含数据内容的 JSON 对象，如果数据是新创建的则返回 null
     * @throws JSONException 如果创建 JSON 对象时发生错误
     */
    public JSONObject getContent() throws JSONException {
        // 如果数据是新创建的，说明还未保存到数据库，记录错误日志并返回 null
        if (mIsCreate) {
            Log.e(TAG, "it seems that we haven't created this in database yet");
            return null;
        }
        // 创建一个新的 JSON 对象
        JSONObject js = new JSONObject();
        // 将数据 ID 添加到 JSON 对象中
        js.put(DataColumns.ID, mDataId);
        // 将 MIME 类型添加到 JSON 对象中
        js.put(DataColumns.MIME_TYPE, mDataMimeType);
        // 将数据内容添加到 JSON 对象中
        js.put(DataColumns.CONTENT, mDataContent);
        // 将 DATA1 字段值添加到 JSON 对象中
        js.put(DataColumns.DATA1, mDataContentData1);
        // 将 DATA3 字段值添加到 JSON 对象中
        js.put(DataColumns.DATA3, mDataContentData3);
        // 返回包含数据内容的 JSON 对象
        return js;
    }

    /**
     * 将数据提交到数据库，根据数据是否为新创建执行插入或更新操作。
     *
     * @param noteId         笔记的 ID
     * @param validateVersion 是否验证版本
     * @param version        版本号
     */
    public void commit(long noteId, boolean validateVersion, long version) {
        // 如果数据是新创建的
        if (mIsCreate) {
            // 如果数据 ID 为无效 ID 且差异数据中包含 ID 字段，则移除该字段
            if (mDataId == INVALID_ID && mDiffDataValues.containsKey(DataColumns.ID)) {
                mDiffDataValues.remove(DataColumns.ID);
            }
            // 将笔记 ID 添加到差异数据中
            mDiffDataValues.put(DataColumns.NOTE_ID, noteId);
            // 执行插入操作，将差异数据插入到数据库中
            Uri uri = mContentResolver.insert(Notes.CONTENT_DATA_URI, mDiffDataValues);
            try {
                // 从插入操作返回的 URI 中获取新插入数据的 ID
                mDataId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                // 如果获取 ID 时发生错误，记录错误日志并抛出异常
                Log.e(TAG, "Get note id error :" + e.toString());
                throw new ActionFailureException("create note failed");
            }
        } else {
            // 如果数据不是新创建的，且差异数据不为空
            if (mDiffDataValues.size() > 0) {
                // 用于记录更新操作的结果
                int result = 0;
                // 如果不验证版本
                if (!validateVersion) {
                    // 执行更新操作，不进行版本验证
                    result = mContentResolver.update(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mDataId), mDiffDataValues, null, null);
                } else {
                    // 如果验证版本，执行带版本验证的更新操作
                    result = mContentResolver.update(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mDataId), mDiffDataValues,
                            " ? in (SELECT " + NoteColumns.ID + " FROM " + TABLE.NOTE
                                    + " WHERE " + NoteColumns.VERSION + "=?)", new String[] {
                                    String.valueOf(noteId), String.valueOf(version)
                            });
                }
                // 如果更新操作未生效，记录警告日志
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }
        }
        // 清空差异数据
        mDiffDataValues.clear();
        // 标记数据不再是新创建的
        mIsCreate = false;
    }

    /**
     * 获取数据的 ID。
     *
     * @return 数据的 ID
     */
    public long getId() {
        // 返回数据的 ID
        return mDataId;
    }
}
