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

// 包声明，指定类所在的包
package net.micode.notes.gtask.data;

// 导入 Android 数据库游标类，用于处理数据库查询结果
import android.database.Cursor;
// 导入 Android 文本工具类，用于文本处理
import android.text.TextUtils;
// 导入 Android 日志工具类，用于记录日志信息
import android.util.Log;

// 导入笔记数据相关的类
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
// 导入任务操作失败异常类
import net.micode.notes.gtask.exception.ActionFailureException;
// 导入任务字符串工具类
import net.micode.notes.tool.GTaskStringUtils;

// 导入 JSON 相关的类，用于处理 JSON 数据
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Task 类表示一个任务，继承自 Node 类。
 * 该类负责处理任务的创建、更新、内容设置以及同步操作。
 */
public class Task extends Node {
    // 定义日志标签，使用类名作为标签
    private static final String TAG = Task.class.getSimpleName();

    // 任务是否完成的标志
    private boolean mCompleted;
    // 任务的备注信息
    private String mNotes;
    // 任务的元信息，以 JSON 对象形式存储
    private JSONObject mMetaInfo;
    // 前一个兄弟任务
    private Task mPriorSibling;
    // 任务所属的任务列表
    private TaskList mParent;

    /**
     * 构造函数，初始化任务对象。
     */
    public Task() {
        super();
        // 初始化任务为未完成状态
        mCompleted = false;
        // 初始化备注信息为空
        mNotes = null;
        // 初始化前一个兄弟任务为空
        mPriorSibling = null;
        // 初始化任务所属的任务列表为空
        mParent = null;
        // 初始化元信息为空
        mMetaInfo = null;
    }

    /**
     * 生成创建任务的 JSON 对象。
     *
     * @param actionId 操作 ID
     * @return 包含创建任务信息的 JSON 对象
     */
    public JSONObject getCreateAction(int actionId) {
        // 创建一个新的 JSON 对象
        JSONObject js = new JSONObject();

        try {
            // 设置操作类型为创建任务
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE);
            // 设置操作 ID
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);
            // 设置任务在任务列表中的索引
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mParent.getChildTaskIndex(this));

            // 创建一个新的 JSON 对象，用于存储实体信息
            JSONObject entity = new JSONObject();
            // 设置任务名称
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            // 设置任务创建者 ID
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            // 设置实体类型为任务
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_TASK);
            // 如果任务有备注信息，则添加到实体信息中
            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
            }
            // 将实体信息添加到主 JSON 对象中
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

            // 设置任务所属的父任务列表 ID
            js.put(GTaskStringUtils.GTASK_JSON_PARENT_ID, mParent.getGid());
            // 设置目标父任务列表类型为组
            js.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);
            // 设置任务列表 ID
            js.put(GTaskStringUtils.GTASK_JSON_LIST_ID, mParent.getGid());

            // 如果有前一个兄弟任务，则添加其 ID
            if (mPriorSibling != null) {
                js.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, mPriorSibling.getGid());
            }

        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("fail to generate task-create jsonobject");
        }

        return js;
    }

    /**
     * 生成更新任务的 JSON 对象。
     *
     * @param actionId 操作 ID
     * @return 包含更新任务信息的 JSON 对象
     */
    public JSONObject getUpdateAction(int actionId) {
        // 创建一个新的 JSON 对象
        JSONObject js = new JSONObject();

        try {
            // 设置操作类型为更新任务
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE);
            // 设置操作 ID
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);
            // 设置任务 ID
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid());

            // 创建一个新的 JSON 对象，用于存储实体信息
            JSONObject entity = new JSONObject();
            // 设置任务名称
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            // 如果任务有备注信息，则添加到实体信息中
            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
            }
            // 设置任务是否删除的标志
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted());
            // 将实体信息添加到主 JSON 对象中
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);

        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("fail to generate task-update jsonobject");
        }

        return js;
    }

    /**
     * 根据远程 JSON 对象设置任务内容。
     *
     * @param js 包含任务信息的 JSON 对象
     */
    public void setContentByRemoteJSON(JSONObject js) {
        if (js != null) {
            try {
                // 如果 JSON 对象包含任务 ID，则设置任务 ID
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) {
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID));
                }
                // 如果 JSON 对象包含最后修改时间，则设置最后修改时间
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) {
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED));
                }
                // 如果 JSON 对象包含任务名称，则设置任务名称
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) {
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME));
                }
                // 如果 JSON 对象包含任务备注信息，则设置任务备注信息
                if (js.has(GTaskStringUtils.GTASK_JSON_NOTES)) {
                    setNotes(js.getString(GTaskStringUtils.GTASK_JSON_NOTES));
                }
                // 如果 JSON 对象包含任务是否删除的标志，则设置任务是否删除的标志
                if (js.has(GTaskStringUtils.GTASK_JSON_DELETED)) {
                    setDeleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_DELETED));
                }
                // 如果 JSON 对象包含任务是否完成的标志，则设置任务是否完成的标志
                if (js.has(GTaskStringUtils.GTASK_JSON_COMPLETED)) {
                    setCompleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_COMPLETED));
                }
            } catch (JSONException e) {
                // 记录错误日志
                Log.e(TAG, e.toString());
                // 打印异常堆栈信息
                e.printStackTrace();
                // 抛出操作失败异常
                throw new ActionFailureException("fail to get task content from jsonobject");
            }
        }
    }

    /**
     * 根据本地 JSON 对象设置任务内容。
     *
     * @param js 包含任务信息的 JSON 对象
     */
    public void setContentByLocalJSON(JSONObject js) {
        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)
                || !js.has(GTaskStringUtils.META_HEAD_DATA)) {
            // 记录警告日志
            Log.w(TAG, "setContentByLocalJSON: nothing is avaiable");
        }

        try {
            // 获取笔记信息的 JSON 对象
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            // 获取数据数组的 JSON 数组
            JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);

            // 检查笔记类型是否为任务类型
            if (note.getInt(NoteColumns.TYPE) != Notes.TYPE_NOTE) {
                // 记录错误日志
                Log.e(TAG, "invalid type");
                return;
            }

            // 遍历数据数组
            for (int i = 0; i < dataArray.length(); i++) {
                // 获取当前数据的 JSON 对象
                JSONObject data = dataArray.getJSONObject(i);
                // 检查数据的 MIME 类型是否为笔记类型
                if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) {
                    // 设置任务名称
                    setName(data.getString(DataColumns.CONTENT));
                    break;
                }
            }

        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 根据任务内容生成本地 JSON 对象。
     *
     * @return 包含任务信息的 JSON 对象
     */
    public JSONObject getLocalJSONFromContent() {
        // 获取任务名称
        String name = getName();
        try {
            if (mMetaInfo == null) {
                // 如果元信息为空，说明是从网页创建的新任务
                if (name == null) {
                    // 记录警告日志
                    Log.w(TAG, "the note seems to be an empty one");
                    return null;
                }

                // 创建一个新的 JSON 对象
                JSONObject js = new JSONObject();
                // 创建一个新的 JSON 对象，用于存储笔记信息
                JSONObject note = new JSONObject();
                // 创建一个新的 JSON 数组，用于存储数据
                JSONArray dataArray = new JSONArray();
                // 创建一个新的 JSON 对象，用于存储数据内容
                JSONObject data = new JSONObject();
                // 设置数据内容为任务名称
                data.put(DataColumns.CONTENT, name);
                // 将数据添加到数据数组中
                dataArray.put(data);
                // 将数据数组添加到主 JSON 对象中
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray);
                // 设置笔记类型为任务类型
                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
                // 将笔记信息添加到主 JSON 对象中
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);
                return js;
            } else {
                // 如果元信息不为空，说明是已同步的任务
                // 获取笔记信息的 JSON 对象
                JSONObject note = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                // 获取数据数组的 JSON 数组
                JSONArray dataArray = mMetaInfo.getJSONArray(GTaskStringUtils.META_HEAD_DATA);

                // 遍历数据数组
                for (int i = 0; i < dataArray.length(); i++) {
                    // 获取当前数据的 JSON 对象
                    JSONObject data = dataArray.getJSONObject(i);
                    // 检查数据的 MIME 类型是否为笔记类型
                    if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) {
                        // 设置数据内容为任务名称
                        data.put(DataColumns.CONTENT, getName());
                        break;
                    }
                }

                // 设置笔记类型为任务类型
                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
                return mMetaInfo;
            }
        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置任务的元信息。
     *
     * @param metaData 包含元信息的对象
     */
    public void setMetaInfo(MetaData metaData) {
        if (metaData != null && metaData.getNotes() != null) {
            try {
                // 将元信息转换为 JSON 对象
                mMetaInfo = new JSONObject(metaData.getNotes());
            } catch (JSONException e) {
                // 记录警告日志
                Log.w(TAG, e.toString());
                // 设置元信息为空
                mMetaInfo = null;
            }
        }
    }

    /**
     * 根据数据库游标获取同步操作类型。
     *
     * @param c 数据库游标
     * @return 同步操作类型
     */
    public int getSyncAction(Cursor c) {
        try {
            // 初始化笔记信息的 JSON 对象
            JSONObject noteInfo = null;
            if (mMetaInfo != null && mMetaInfo.has(GTaskStringUtils.META_HEAD_NOTE)) {
                // 如果元信息包含笔记信息，则获取笔记信息的 JSON 对象
                noteInfo = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
            }

            if (noteInfo == null) {
                // 如果笔记信息为空，说明笔记元信息已被删除
                Log.w(TAG, "it seems that note meta has been deleted");
                return SYNC_ACTION_UPDATE_REMOTE;
            }

            if (!noteInfo.has(NoteColumns.ID)) {
                // 如果笔记信息不包含笔记 ID，说明远程笔记 ID 已被删除
                Log.w(TAG, "remote note id seems to be deleted");
                return SYNC_ACTION_UPDATE_LOCAL;
            }

            // 验证笔记 ID
            if (c.getLong(SqlNote.ID_COLUMN) != noteInfo.getLong(NoteColumns.ID)) {
                // 如果笔记 ID 不匹配，说明需要更新本地笔记
                Log.w(TAG, "note id doesn't match");
                return SYNC_ACTION_UPDATE_LOCAL;
            }

            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                // 如果本地没有更新
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // 如果本地和远程都没有更新
                    return SYNC_ACTION_NONE;
                } else {
                    // 如果远程有更新，需要将远程更新应用到本地
                    return SYNC_ACTION_UPDATE_LOCAL;
                }
            } else {
                // 如果本地有更新
                // 验证 GTask ID
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                    // 如果 GTask ID 不匹配，说明同步出错
                    Log.e(TAG, "gtask id doesn't match");
                    return SYNC_ACTION_ERROR;
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // 如果本地有修改，远程没有修改，需要将本地修改更新到远程
                    return SYNC_ACTION_UPDATE_REMOTE;
                } else {
                    // 如果本地和远程都有修改，说明发生了冲突
                    return SYNC_ACTION_UPDATE_CONFLICT;
                }
            }
        } catch (Exception e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
        }

        return SYNC_ACTION_ERROR;
    }

    /**
     * 判断任务是否值得保存。
     *
     * @return 如果任务值得保存返回 true，否则返回 false
     */
    public boolean isWorthSaving() {
        return mMetaInfo != null || (getName() != null && getName().trim().length() > 0)
                || (getNotes() != null && getNotes().trim().length() > 0);
    }

    /**
     * 设置任务是否完成的标志。
     *
     * @param completed 任务是否完成的标志
     */
    public void setCompleted(boolean completed) {
        this.mCompleted = completed;
    }

    /**
     * 设置任务的备注信息。
     *
     * @param notes 任务的备注信息
     */
    public void setNotes(String notes) {
        this.mNotes = notes;
    }

    /**
     * 设置前一个兄弟任务。
     *
     * @param priorSibling 前一个兄弟任务
     */
    public void setPriorSibling(Task priorSibling) {
        this.mPriorSibling = priorSibling;
    }

    /**
     * 设置任务所属的任务列表。
     *
     * @param parent 任务所属的任务列表
     */
    public void setParent(TaskList parent) {
        this.mParent = parent;
    }

    /**
     * 获取任务是否完成的标志。
     *
     * @return 任务是否完成的标志
     */
    public boolean getCompleted() {
        return this.mCompleted;
    }

    /**
     * 获取任务的备注信息。
     *
     * @return 任务的备注信息
     */
    public String getNotes() {
        return this.mNotes;
    }

    /**
     * 获取前一个兄弟任务。
     *
     * @return 前一个兄弟任务
     */
    public Task getPriorSibling() {
        return this.mPriorSibling;
    }

    /**
     * 获取任务所属的任务列表。
     *
     * @return 任务所属的任务列表
     */
    public TaskList getParent() {
        return this.mParent;
    }

}
