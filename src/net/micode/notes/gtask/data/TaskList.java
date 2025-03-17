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

// 包声明，表明该类所属的包
package net.micode.notes.gtask.data;

// 导入 android.database.Cursor 类，用于处理数据库查询结果
import android.database.Cursor;
// 导入 android.util.Log 类，用于日志记录
import android.util.Log;

// 导入 net.micode.notes.data.Notes 类，包含笔记相关的数据结构和常量
import net.micode.notes.data.Notes;
// 导入 net.micode.notes.data.Notes.NoteColumns 类，包含笔记列的常量
import net.micode.notes.data.Notes.NoteColumns;
// 导入 net.micode.notes.gtask.exception.ActionFailureException 类，用于处理操作失败异常
import net.micode.notes.gtask.exception.ActionFailureException;
// 导入 net.micode.notes.tool.GTaskStringUtils 类，包含 Google 任务相关的字符串常量
import net.micode.notes.tool.GTaskStringUtils;

// 导入 org.json.JSONException 类，用于处理 JSON 操作异常
import org.json.JSONException;
// 导入 org.json.JSONObject 类，用于处理 JSON 对象
import org.json.JSONObject;

// 导入 java.util.ArrayList 类，用于创建动态数组
import java.util.ArrayList;

/**
 * TaskList 类表示一个任务列表，继承自 Node 类。
 * 该类包含了任务列表的基本信息和操作方法，如创建、更新、同步等。
 */
public class TaskList extends Node {
    // 定义日志标签，用于标识该类的日志信息
    private static final String TAG = TaskList.class.getSimpleName();

    // 定义任务列表的索引，用于排序或标识
    private int mIndex;

    // 存储该任务列表下的子任务
    private ArrayList<Task> mChildren;

    /**
     * 构造函数，初始化 TaskList 对象。
     * 调用父类的构造函数，并初始化子任务列表和索引。
     */
    public TaskList() {
        // 调用父类的构造函数
        super();
        // 初始化子任务列表
        mChildren = new ArrayList<Task>();
        // 初始化索引为 1
        mIndex = 1;
    }

    /**
     * 生成创建任务列表的 JSON 操作对象。
     *
     * @param actionId 操作的唯一标识符
     * @return 包含创建任务列表操作信息的 JSONObject
     * @throws ActionFailureException 如果生成 JSON 对象时出现异常
     */
    public JSONObject getCreateAction(int actionId) {
        // 创建一个新的 JSONObject 用于存储操作信息
        JSONObject js = new JSONObject();
    
        try {
            // 设置操作类型为创建任务列表
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE);
    
            // 设置操作的唯一标识符
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);
    
            // 设置任务列表的索引
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mIndex);
    
            // 创建一个新的 JSONObject 用于存储实体信息
            JSONObject entity = new JSONObject();
            // 设置任务列表的名称
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            // 设置创建者 ID 为 null
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            // 设置实体类型为组
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);
            // 将实体信息添加到操作信息中
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);
    
        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("fail to generate tasklist-create jsonobject");
        }
    
        return js;
    }

    /**
     * 生成更新任务列表的 JSON 操作对象。
     *
     * @param actionId 操作的唯一标识符
     * @return 包含更新任务列表操作信息的 JSONObject
     * @throws ActionFailureException 如果生成 JSON 对象时出现异常
     */
    public JSONObject getUpdateAction(int actionId) {
        // 创建一个新的 JSONObject 用于存储操作信息
        JSONObject js = new JSONObject();
    
        try {
            // 设置操作类型为更新任务列表
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE);
    
            // 设置操作的唯一标识符
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);
    
            // 设置任务列表的全局 ID
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid());
    
            // 创建一个新的 JSONObject 用于存储实体信息
            JSONObject entity = new JSONObject();
            // 设置任务列表的名称
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            // 设置任务列表是否已删除
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted());
            // 将实体信息添加到操作信息中
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);
    
        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("fail to generate tasklist-update jsonobject");
        }
    
        return js;
    }

    /**
     * 根据远程 JSON 对象设置任务列表的内容。
     *
     * @param js 包含任务列表信息的远程 JSON 对象
     * @throws ActionFailureException 如果解析 JSON 对象时出现异常
     */
    public void setContentByRemoteJSON(JSONObject js) {
        // 检查 JSON 对象是否为空
        if (js != null) {
            try {
                // 如果 JSON 对象包含全局 ID，则设置任务列表的全局 ID
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) {
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID));
                }
    
                // 如果 JSON 对象包含最后修改时间，则设置任务列表的最后修改时间
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) {
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED));
                }
    
                // 如果 JSON 对象包含名称，则设置任务列表的名称
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) {
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME));
                }
    
            } catch (JSONException e) {
                // 记录错误日志
                Log.e(TAG, e.toString());
                // 打印异常堆栈信息
                e.printStackTrace();
                // 抛出操作失败异常
                throw new ActionFailureException("fail to get tasklist content from jsonobject");
            }
        }
    }

    /**
     * 根据本地 JSON 对象设置任务列表的内容。
     *
     * @param js 包含任务列表信息的本地 JSON 对象
     */
    public void setContentByLocalJSON(JSONObject js) {
        // 检查 JSON 对象是否为空或是否包含笔记元数据
        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)) {
            // 记录警告日志
            Log.w(TAG, "setContentByLocalJSON: nothing is avaiable");
        }
    
        try {
            // 获取笔记元数据
            JSONObject folder = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
    
            // 如果笔记类型为文件夹
            if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                // 获取文件夹名称
                String name = folder.getString(NoteColumns.SNIPPET);
                // 设置任务列表的名称，并添加前缀
                setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + name);
            } 
            // 如果笔记类型为系统文件夹
            else if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                // 如果是根文件夹
                if (folder.getLong(NoteColumns.ID) == Notes.ID_ROOT_FOLDER)
                    // 设置任务列表的名称为默认文件夹名称，并添加前缀
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT);
                // 如果是通话记录文件夹
                else if (folder.getLong(NoteColumns.ID) == Notes.ID_CALL_RECORD_FOLDER)
                    // 设置任务列表的名称为通话记录文件夹名称，并添加前缀
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                            + GTaskStringUtils.FOLDER_CALL_NOTE);
                else
                    // 记录错误日志
                    Log.e(TAG, "invalid system folder");
            } 
            else {
                // 记录错误日志
                Log.e(TAG, "error type");
            }
        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 根据任务列表的内容生成本地 JSON 对象。
     *
     * @return 包含任务列表信息的本地 JSON 对象
     */
    public JSONObject getLocalJSONFromContent() {
        try {
            // 创建一个新的 JSONObject 用于存储本地 JSON 信息
            JSONObject js = new JSONObject();
            // 创建一个新的 JSONObject 用于存储文件夹信息
            JSONObject folder = new JSONObject();
    
            // 获取任务列表的名称
            String folderName = getName();
            // 如果名称以特定前缀开头，则去除前缀
            if (getName().startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX))
                folderName = folderName.substring(GTaskStringUtils.MIUI_FOLDER_PREFFIX.length(),
                        folderName.length());
            // 将文件夹名称添加到文件夹信息中
            folder.put(NoteColumns.SNIPPET, folderName);
            // 如果文件夹名称为默认文件夹或通话记录文件夹
            if (folderName.equals(GTaskStringUtils.FOLDER_DEFAULT)
                    || folderName.equals(GTaskStringUtils.FOLDER_CALL_NOTE))
                // 设置文件夹类型为系统文件夹
                folder.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM);
            else
                // 设置文件夹类型为普通文件夹
                folder.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
    
            // 将文件夹信息添加到本地 JSON 信息中
            js.put(GTaskStringUtils.META_HEAD_NOTE, folder);
    
            return js;
        } catch (JSONException e) {
            // 记录错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据数据库游标确定同步操作类型。
     *
     * @param c 数据库游标，包含任务列表的本地和远程同步信息
     * @return 同步操作类型，如无操作、更新本地、更新远程或错误
     */
    public int getSyncAction(Cursor c) {
        try {
            // 检查本地是否有修改
            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                // 本地无更新
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // 双方都无更新
                    return SYNC_ACTION_NONE;
                } else {
                    // 应用远程更新到本地
                    return SYNC_ACTION_UPDATE_LOCAL;
                }
            } else {
                // 验证 GTask ID
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                    // 记录错误日志
                    Log.e(TAG, "gtask id doesn't match");
                    return SYNC_ACTION_ERROR;
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // 仅本地有修改
                    return SYNC_ACTION_UPDATE_REMOTE;
                } else {
                    // 对于文件夹冲突，直接应用本地修改
                    return SYNC_ACTION_UPDATE_REMOTE;
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
     * 获取子任务的数量。
     *
     * @return 子任务的数量
     */
    public int getChildTaskCount() {
        return mChildren.size();
    }

    /**
     * 向任务列表中添加一个子任务。
     *
     * @param task 要添加的子任务
     * @return 如果添加成功返回 true，否则返回 false
     */
    public boolean addChildTask(Task task) {
        boolean ret = false;
        // 检查任务是否为空且任务列表中不包含该任务
        if (task != null && !mChildren.contains(task)) {
            // 尝试将任务添加到任务列表中
            ret = mChildren.add(task);
            if (ret) {
                // 设置任务的前一个兄弟任务
                task.setPriorSibling(mChildren.isEmpty() ? null : mChildren
                        .get(mChildren.size() - 1));
                // 设置任务的父任务列表
                task.setParent(this);
            }
        }
        return ret;
    }

    /**
     * 向任务列表的指定位置添加一个子任务。
     *
     * @param task 要添加的子任务
     * @param index 要添加的位置索引
     * @return 如果添加成功返回 true，否则返回 false
     */
    public boolean addChildTask(Task task, int index) {
        // 检查索引是否有效
        if (index < 0 || index > mChildren.size()) {
            // 记录错误日志
            Log.e(TAG, "add child task: invalid index");
            return false;
        }
    
        // 获取任务在任务列表中的位置
        int pos = mChildren.indexOf(task);
        // 检查任务是否为空且任务列表中不包含该任务
        if (task != null && pos == -1) {
            // 在指定位置添加任务
            mChildren.add(index, task);
    
            // 更新任务列表
            Task preTask = null;
            Task afterTask = null;
            if (index != 0)
                preTask = mChildren.get(index - 1);
            if (index != mChildren.size() - 1)
                afterTask = mChildren.get(index + 1);
    
            // 设置任务的前一个兄弟任务
            task.setPriorSibling(preTask);
            if (afterTask != null)
                afterTask.setPriorSibling(task);
        }
    
        return true;
    }

    /**
     * 从任务列表中移除一个子任务。
     *
     * @param task 要移除的子任务
     * @return 如果移除成功返回 true，否则返回 false
     */
    public boolean removeChildTask(Task task) {
        boolean ret = false;
        // 获取任务在任务列表中的索引
        int index = mChildren.indexOf(task);
        if (index != -1) {
            // 尝试从任务列表中移除任务
            ret = mChildren.remove(task);
    
            if (ret) {
                // 重置任务的前一个兄弟任务和父任务列表
                task.setPriorSibling(null);
                task.setParent(null);
    
                // 更新任务列表
                if (index != mChildren.size()) {
                    mChildren.get(index).setPriorSibling(
                            index == 0 ? null : mChildren.get(index - 1));
                }
            }
        }
        return ret;
    }

    /**
     * 将一个子任务移动到任务列表的指定位置。
     *
     * @param task 要移动的子任务
     * @param index 要移动到的位置索引
     * @return 如果移动成功返回 true，否则返回 false
     */
    public boolean moveChildTask(Task task, int index) {
        // 检查索引是否有效
        if (index < 0 || index >= mChildren.size()) {
            // 记录错误日志
            Log.e(TAG, "move child task: invalid index");
            return false;
        }
    
        // 获取任务在任务列表中的位置
        int pos = mChildren.indexOf(task);
        if (pos == -1) {
            // 记录错误日志
            Log.e(TAG, "move child task: the task should in the list");
            return false;
        }
    
        if (pos == index)
            return true;
        // 先移除任务，再将任务添加到指定位置
        return (removeChildTask(task) && addChildTask(task, index));
    }

    /**
     * 根据全局 ID 查找子任务。
     *
     * @param gid 子任务的全局 ID
     * @return 如果找到则返回子任务，否则返回 null
     */
    public Task findChildTaskByGid(String gid) {
        // 遍历子任务列表
        for (int i = 0; i < mChildren.size(); i++) {
            // 获取当前子任务
            Task t = mChildren.get(i);
            if (t.getGid().equals(gid)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 获取子任务在任务列表中的索引。
     *
     * @param task 要查找索引的子任务
     * @return 子任务的索引，如果未找到则返回 -1
     */
    public int getChildTaskIndex(Task task) {
        return mChildren.indexOf(task);
    }

    /**
     * 根据索引获取子任务。
     *
     * @param index 子任务的索引
     * @return 如果索引有效则返回子任务，否则返回 null
     */
    public Task getChildTaskByIndex(int index) {
        // 检查索引是否有效
        if (index < 0 || index >= mChildren.size()) {
            // 记录错误日志
            Log.e(TAG, "getTaskByIndex: invalid index");
            return null;
        }
        return mChildren.get(index);
    }

    /**
     * 根据全局 ID 获取子任务。
     *
     * @param gid 子任务的全局 ID
     * @return 如果找到则返回子任务，否则返回 null
     */
    public Task getChilTaskByGid(String gid) {
        // 遍历子任务列表
        for (Task task : mChildren) {
            if (task.getGid().equals(gid))
                return task;
        }
        return null;
    }

    /**
     * 获取任务列表的所有子任务。
     *
     * @return 包含所有子任务的 ArrayList
     */
    public ArrayList<Task> getChildTaskList() {
        return this.mChildren;
    }

    /**
     * 设置任务列表的索引。
     *
     * @param index 要设置的索引值
     */
    public void setIndex(int index) {
        this.mIndex = index;
    }

    /**
     * 获取任务列表的索引。
     *
     * @return 任务列表的索引值
     */
    public int getIndex() {
        return this.mIndex;
    }
}
