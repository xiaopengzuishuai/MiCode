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

package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.data.MetaData;
import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.SqlNote;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


/**
 * GTaskManager类负责管理Google任务的同步操作。
 * 它是一个单例类，确保在整个应用程序中只有一个实例。
 * 该类处理与Google任务服务的通信，包括登录、获取任务列表、同步内容等操作。
 */
public class GTaskManager {
    // 日志标签，用于调试和日志记录
    private static final String TAG = GTaskManager.class.getSimpleName();

    // 同步成功的状态码
    public static final int STATE_SUCCESS = 0;

    // 网络错误的状态码
    public static final int STATE_NETWORK_ERROR = 1;

    // 内部错误的状态码
    public static final int STATE_INTERNAL_ERROR = 2;

    // 同步正在进行的状态码
    public static final int STATE_SYNC_IN_PROGRESS = 3;

    // 同步已取消的状态码
    public static final int STATE_SYNC_CANCELLED = 4;

    // 单例实例
    private static GTaskManager mInstance = null;

    // 用于获取认证令牌的Activity
    private Activity mActivity;

    // 应用程序上下文
    private Context mContext;

    // 内容解析器，用于与内容提供者进行交互
    private ContentResolver mContentResolver;

    // 表示同步是否正在进行的标志
    private boolean mSyncing;

    // 表示同步是否已取消的标志
    private boolean mCancelled;

    // 存储Google任务列表的哈希映射，键为任务列表的ID
    private HashMap<String, TaskList> mGTaskListHashMap;

    // 存储Google任务的哈希映射，键为任务的ID
    private HashMap<String, Node> mGTaskHashMap;

    // 存储元数据的哈希映射，键为相关任务的ID
    private HashMap<String, MetaData> mMetaHashMap;

    // 元数据任务列表
    private TaskList mMetaList;

    // 存储本地已删除笔记ID的哈希集
    private HashSet<Long> mLocalDeleteIdMap;

    // 存储Google任务ID到本地笔记ID的映射
    private HashMap<String, Long> mGidToNid;

    // 存储本地笔记ID到Google任务ID的映射
    private HashMap<Long, String> mNidToGid;

    /**
     * 私有构造函数，确保只能通过getInstance方法创建实例
     */
    private GTaskManager() {
        // 初始化同步状态为未开始
        mSyncing = false;
        // 初始化取消标志为未取消
        mCancelled = false;
        // 初始化Google任务列表的哈希映射
        mGTaskListHashMap = new HashMap<String, TaskList>();
        // 初始化Google任务的哈希映射
        mGTaskHashMap = new HashMap<String, Node>();
        // 初始化元数据的哈希映射
        mMetaHashMap = new HashMap<String, MetaData>();
        // 初始化元数据任务列表为null
        mMetaList = null;
        // 初始化本地已删除笔记ID的哈希集
        mLocalDeleteIdMap = new HashSet<Long>();
        // 初始化Google任务ID到本地笔记ID的映射
        mGidToNid = new HashMap<String, Long>();
        // 初始化本地笔记ID到Google任务ID的映射
        mNidToGid = new HashMap<Long, String>();
    }

    /**
     * 获取GTaskManager的单例实例
     * @return GTaskManager的单例实例
     */
    public static synchronized GTaskManager getInstance() {
        // 如果实例未创建，则创建一个新实例
        if (mInstance == null) {
            mInstance = new GTaskManager();
        }
        // 返回单例实例
        return mInstance;
    }

    /**
     * 设置用于获取认证令牌的Activity上下文
     * @param activity 用于获取认证令牌的Activity
     */
    public synchronized void setActivityContext(Activity activity) {
        // 用于获取认证令牌
        mActivity = activity;
    }

    /**
     * 执行Google任务的同步操作
     * @param context 应用程序上下文
     * @param asyncTask 异步任务，用于发布同步进度
     * @return 同步结果的状态码
     */
    public int sync(Context context, GTaskASyncTask asyncTask) {
        // 检查同步是否正在进行
        if (mSyncing) {
            // 记录日志，提示同步正在进行
            Log.d(TAG, "Sync is in progress");
            // 返回同步正在进行的状态码
            return STATE_SYNC_IN_PROGRESS;
        }
        // 设置应用程序上下文
        mContext = context;
        // 获取内容解析器
        mContentResolver = mContext.getContentResolver();
        // 设置同步状态为正在进行
        mSyncing = true;
        // 设置取消标志为未取消
        mCancelled = false;
        // 清空Google任务列表的哈希映射
        mGTaskListHashMap.clear();
        // 清空Google任务的哈希映射
        mGTaskHashMap.clear();
        // 清空元数据的哈希映射
        mMetaHashMap.clear();
        // 清空本地已删除笔记ID的哈希集
        mLocalDeleteIdMap.clear();
        // 清空Google任务ID到本地笔记ID的映射
        mGidToNid.clear();
        // 清空本地笔记ID到Google任务ID的映射
        mNidToGid.clear();

        try {
            // 获取GTaskClient的实例
            GTaskClient client = GTaskClient.getInstance();
            // 重置更新数组
            client.resetUpdateArray();

            // 登录Google任务服务
            if (!mCancelled) {
                // 如果登录失败，抛出网络错误异常
                if (!client.login(mActivity)) {
                    throw new NetworkFailureException("login google task failed");
                }
            }

            // 从Google获取任务列表
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_init_list));
            // 初始化Google任务列表
            initGTaskList();

            // 执行内容同步工作
            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_syncing));
            // 同步内容
            syncContent();
        } catch (NetworkFailureException e) {
            // 记录网络错误日志
            Log.e(TAG, e.toString());
            // 返回网络错误的状态码
            return STATE_NETWORK_ERROR;
        } catch (ActionFailureException e) {
            // 记录内部错误日志
            Log.e(TAG, e.toString());
            // 返回内部错误的状态码
            return STATE_INTERNAL_ERROR;
        } catch (Exception e) {
            // 记录异常日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 返回内部错误的状态码
            return STATE_INTERNAL_ERROR;
        } finally {
            // 清空Google任务列表的哈希映射
            mGTaskListHashMap.clear();
            // 清空Google任务的哈希映射
            mGTaskHashMap.clear();
            // 清空元数据的哈希映射
            mMetaHashMap.clear();
            // 清空本地已删除笔记ID的哈希集
            mLocalDeleteIdMap.clear();
            // 清空Google任务ID到本地笔记ID的映射
            mGidToNid.clear();
            // 清空本地笔记ID到Google任务ID的映射
            mNidToGid.clear();
            // 设置同步状态为未进行
            mSyncing = false;
        }

        // 根据取消标志返回同步结果的状态码
        return mCancelled ? STATE_SYNC_CANCELLED : STATE_SUCCESS;
    }

    /**
     * 初始化Google任务列表
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void initGTaskList() throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled)
            return;
        // 获取GTaskClient的实例
        GTaskClient client = GTaskClient.getInstance();
        try {
            // 获取Google任务列表的JSON数组
            JSONArray jsTaskLists = client.getTaskLists();

            // 首先初始化元数据列表
            mMetaList = null;
            // 遍历任务列表的JSON数组
            for (int i = 0; i < jsTaskLists.length(); i++) {
                // 获取当前任务列表的JSON对象
                JSONObject object = jsTaskLists.getJSONObject(i);
                // 获取任务列表的ID
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                // 获取任务列表的名称
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                // 如果任务列表的名称是元数据列表的名称
                if (name
                        .equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) {
                    // 创建一个新的任务列表
                    mMetaList = new TaskList();
                    // 根据远程JSON对象设置任务列表的内容
                    mMetaList.setContentByRemoteJSON(object);

                    // 加载元数据
                    JSONArray jsMetas = client.getTaskList(gid);
                    // 遍历元数据的JSON数组
                    for (int j = 0; j < jsMetas.length(); j++) {
                        // 获取当前元数据的JSON对象
                        object = (JSONObject) jsMetas.getJSONObject(j);
                        // 创建一个新的元数据对象
                        MetaData metaData = new MetaData();
                        // 根据远程JSON对象设置元数据的内容
                        metaData.setContentByRemoteJSON(object);
                        // 如果元数据值得保存
                        if (metaData.isWorthSaving()) {
                            // 将元数据添加到元数据列表中
                            mMetaList.addChildTask(metaData);
                            // 如果元数据有ID
                            if (metaData.getGid() != null) {
                                // 将元数据添加到元数据哈希映射中
                                mMetaHashMap.put(metaData.getRelatedGid(), metaData);
                            }
                        }
                    }
                }
            }

            // 如果元数据列表不存在，则创建一个新的元数据列表
            if (mMetaList == null) {
                // 创建一个新的任务列表
                mMetaList = new TaskList();
                // 设置任务列表的名称为元数据列表的名称
                mMetaList.setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                        + GTaskStringUtils.FOLDER_META);
                // 在Google任务服务中创建元数据列表
                GTaskClient.getInstance().createTaskList(mMetaList);
            }

            // 初始化任务列表
            for (int i = 0; i < jsTaskLists.length(); i++) {
                // 获取当前任务列表的JSON对象
                JSONObject object = jsTaskLists.getJSONObject(i);
                // 获取任务列表的ID
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                // 获取任务列表的名称
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                // 如果任务列表的名称以特定前缀开头，且不是元数据列表
                if (name.startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX)
                        && !name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX
                                + GTaskStringUtils.FOLDER_META)) {
                    // 创建一个新的任务列表
                    TaskList tasklist = new TaskList();
                    // 根据远程JSON对象设置任务列表的内容
                    tasklist.setContentByRemoteJSON(object);
                    // 将任务列表添加到任务列表哈希映射中
                    mGTaskListHashMap.put(gid, tasklist);
                    // 将任务列表添加到任务哈希映射中
                    mGTaskHashMap.put(gid, tasklist);

                    // 加载任务
                    JSONArray jsTasks = client.getTaskList(gid);
                    // 遍历任务的JSON数组
                    for (int j = 0; j < jsTasks.length(); j++) {
                        // 获取当前任务的JSON对象
                        object = (JSONObject) jsTasks.getJSONObject(j);
                        // 获取任务的ID
                        gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                        // 创建一个新的任务对象
                        Task task = new Task();
                        // 根据远程JSON对象设置任务的内容
                        task.setContentByRemoteJSON(object);
                        // 如果任务值得保存
                        if (task.isWorthSaving()) {
                            // 设置任务的元数据信息
                            task.setMetaInfo(mMetaHashMap.get(gid));
                            // 将任务添加到任务列表中
                            tasklist.addChildTask(task);
                            // 将任务添加到任务哈希映射中
                            mGTaskHashMap.put(gid, task);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            // 记录JSON解析错误日志
            Log.e(TAG, e.toString());
            // 打印异常堆栈信息
            e.printStackTrace();
            // 抛出操作失败异常
            throw new ActionFailureException("initGTaskList: handing JSONObject failed");
        }
    }

    /**
     * 同步Google任务的内容
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void syncContent() throws NetworkFailureException {
        // 同步类型
        int syncType;
        // 游标，用于查询数据库
        Cursor c = null;
        // Google任务的ID
        String gid;
        // 任务节点
        Node node;

        // 清空本地已删除笔记ID的哈希集
        mLocalDeleteIdMap.clear();

        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 处理本地已删除的笔记
        try {
            // 查询本地已删除的笔记
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id=?)", new String[] {
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, null);
            // 如果查询结果不为空
            if (c != null) {
                // 遍历查询结果
                while (c.moveToNext()) {
                    // 获取Google任务的ID
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    // 从任务哈希映射中获取任务节点
                    node = mGTaskHashMap.get(gid);
                    // 如果任务节点存在
                    if (node != null) {
                        // 从任务哈希映射中移除任务节点
                        mGTaskHashMap.remove(gid);
                        // 执行内容同步操作，删除远程任务
                        doContentSync(Node.SYNC_ACTION_DEL_REMOTE, node, c);
                    }

                    // 将本地笔记的ID添加到本地已删除笔记ID的哈希集中
                    mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN));
                }
            } else {
                // 记录查询失败日志
                Log.w(TAG, "failed to query trash folder");
            }
        } finally {
            // 关闭游标
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 首先同步文件夹
        syncFolder();

        // 处理数据库中存在的笔记
        try {
            // 查询数据库中存在的笔记
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[] {
                            String.valueOf(Notes.TYPE_NOTE), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            // 如果查询结果不为空
            if (c != null) {
                // 遍历查询结果
                while (c.moveToNext()) {
                    // 获取Google任务的ID
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    // 从任务哈希映射中获取任务节点
                    node = mGTaskHashMap.get(gid);
                    // 如果任务节点存在
                    if (node != null) {
                        // 从任务哈希映射中移除任务节点
                        mGTaskHashMap.remove(gid);
                        // 将Google任务ID到本地笔记ID的映射添加到哈希映射中
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN));
                        // 将本地笔记ID到Google任务ID的映射添加到哈希映射中
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid);
                        // 获取同步类型
                        syncType = node.getSyncAction(c);
                    } else {
                        // 如果Google任务ID为空
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // 本地添加任务
                            syncType = Node.SYNC_ACTION_ADD_REMOTE;
                        } else {
                            // 远程删除任务
                            syncType = Node.SYNC_ACTION_DEL_LOCAL;
                        }
                    }
                    // 执行内容同步操作
                    doContentSync(syncType, node, c);
                }
            } else {
                // 记录查询失败日志
                Log.w(TAG, "failed to query existing note in database");
            }

        } finally {
            // 关闭游标
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 遍历剩余的任务节点
        Iterator<Map.Entry<String, Node>> iter = mGTaskHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            // 获取当前任务节点的映射项
            Map.Entry<String, Node> entry = iter.next();
            // 获取任务节点
            node = entry.getValue();
            // 执行内容同步操作，添加本地任务
            doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null);
        }

        // mCancelled可以由另一个线程设置，所以需要逐个检查
        // 清空本地已删除笔记表
        if (!mCancelled) {
            // 如果批量删除本地已删除笔记失败，抛出操作失败异常
            if (!DataUtils.batchDeleteNotes(mContentResolver, mLocalDeleteIdMap)) {
                throw new ActionFailureException("failed to batch-delete local deleted notes");
            }
        }

        // 刷新本地同步ID
        if (!mCancelled) {
            // 提交更新
            GTaskClient.getInstance().commitUpdate();
            // 刷新本地同步ID
            refreshLocalSyncId();
        }

    }

    /**
     * 同步文件夹
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void syncFolder() throws NetworkFailureException {
        // 游标，用于查询数据库
        Cursor c = null;
        // Google任务的ID
        String gid;
        // 任务节点
        Node node;
        // 同步类型
        int syncType;

        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 处理根文件夹
        try {
            // 查询根文件夹
            c = mContentResolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                    Notes.ID_ROOT_FOLDER), SqlNote.PROJECTION_NOTE, null, null, null);
            // 如果查询结果不为空
            if (c != null) {
                // 移动到查询结果的第一行
                c.moveToNext();
                // 获取Google任务的ID
                gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                // 从任务哈希映射中获取任务节点
                node = mGTaskHashMap.get(gid);
                // 如果任务节点存在
                if (node != null) {
                    // 从任务哈希映射中移除任务节点
                    mGTaskHashMap.remove(gid);
                    // 将Google任务ID到本地笔记ID的映射添加到哈希映射中
                    mGidToNid.put(gid, (long) Notes.ID_ROOT_FOLDER);
                    // 将本地笔记ID到Google任务ID的映射添加到哈希映射中
                    mNidToGid.put((long) Notes.ID_ROOT_FOLDER, gid);
                    // 对于系统文件夹，仅在必要时更新远程名称
                    if (!node.getName().equals(
                            GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT))
                        // 执行内容同步操作，更新远程任务
                        doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c);
                } else {
                    // 执行内容同步操作，添加远程任务
                    doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c);
                }
            } else {
                // 记录查询失败日志
                Log.w(TAG, "failed to query root folder");
            }
        } finally {
            // 关闭游标
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 处理通话记录文件夹
        try {
            // 查询通话记录文件夹
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE, "(_id=?)",
                    new String[] {
                        String.valueOf(Notes.ID_CALL_RECORD_FOLDER)
                    }, null);
            // 如果查询结果不为空
            if (c != null) {
                // 移动到查询结果的第一行
                if (c.moveToNext()) {
                    // 获取Google任务的ID
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    // 从任务哈希映射中获取任务节点
                    node = mGTaskHashMap.get(gid);
                    // 如果任务节点存在
                    if (node != null) {
                        // 从任务哈希映射中移除任务节点
                        mGTaskHashMap.remove(gid);
                        // 将Google任务ID到本地笔记ID的映射添加到哈希映射中
                        mGidToNid.put(gid, (long) Notes.ID_CALL_RECORD_FOLDER);
                        // 将本地笔记ID到Google任务ID的映射添加到哈希映射中
                        mNidToGid.put((long) Notes.ID_CALL_RECORD_FOLDER, gid);
                        // 对于系统文件夹，仅在必要时更新远程名称
                        if (!node.getName().equals(
                                GTaskStringUtils.MIUI_FOLDER_PREFFIX
                                        + GTaskStringUtils.FOLDER_CALL_NOTE))
                            // 执行内容同步操作，更新远程任务
                            doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c);
                    } else {
                        // 执行内容同步操作，添加远程任务
                        doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c);
                    }
                }
            } else {
                // 记录查询失败日志
                Log.w(TAG, "failed to query call note folder");
            }
        } finally {
            // 关闭游标
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 处理本地存在的文件夹
        try {
            // 查询本地存在的文件夹
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[] {
                            String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            // 如果查询结果不为空
            if (c != null) {
                // 遍历查询结果
                while (c.moveToNext()) {
                    // 获取Google任务的ID
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    // 从任务哈希映射中获取任务节点
                    node = mGTaskHashMap.get(gid);
                    // 如果任务节点存在
                    if (node != null) {
                        // 从任务哈希映射中移除任务节点
                        mGTaskHashMap.remove(gid);
                        // 将Google任务ID到本地笔记ID的映射添加到哈希映射中
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN));
                        // 将本地笔记ID到Google任务ID的映射添加到哈希映射中
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid);
                        // 获取同步类型
                        syncType = node.getSyncAction(c);
                    } else {
                        // 如果Google任务ID为空
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // 本地添加任务
                            syncType = Node.SYNC_ACTION_ADD_REMOTE;
                        } else {
                            // 远程删除任务
                            syncType = Node.SYNC_ACTION_DEL_LOCAL;
                        }
                    }
                    // 执行内容同步操作
                    doContentSync(syncType, node, c);
                }
            } else {
                // 记录查询失败日志
                Log.w(TAG, "failed to query existing folder");
            }
        } finally {
            // 关闭游标
            if (c != null) {
                c.close();
                c = null;
            }
        }

        // 处理远程添加的文件夹
        Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            // 获取当前任务列表的映射项
            Map.Entry<String, TaskList> entry = iter.next();
            // 获取任务列表的ID
            gid = entry.getKey();
            // 获取任务列表
            node = entry.getValue();
            // 如果任务哈希映射中包含该任务列表的ID
            if (mGTaskHashMap.containsKey(gid)) {
                // 从任务哈希映射中移除该任务列表
                mGTaskHashMap.remove(gid);
                // 执行内容同步操作，添加本地任务
                doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null);
            }
        }

        // 如果同步未取消，提交更新
        if (!mCancelled)
            GTaskClient.getInstance().commitUpdate();
    }

    /**
     * 执行内容同步操作
     * @param syncType 同步类型
     * @param node 任务节点
     * @param c 游标，用于查询数据库
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void doContentSync(int syncType, Node node, Cursor c) throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 元数据
        MetaData meta;
        // 根据同步类型执行相应的操作
        switch (syncType) {
            case Node.SYNC_ACTION_ADD_LOCAL:
                // 添加本地任务节点
                addLocalNode(node);
                break;
            case Node.SYNC_ACTION_ADD_REMOTE:
                // 添加远程任务节点
                addRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_DEL_LOCAL:
                // 获取元数据
                meta = mMetaHashMap.get(c.getString(SqlNote.GTASK_ID_COLUMN));
                // 如果元数据存在
                if (meta != null) {
                    // 删除远程元数据节点
                    GTaskClient.getInstance().deleteNode(meta);
                }
                // 将本地笔记的ID添加到本地已删除笔记ID的哈希集中
                mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN));
                break;
            case Node.SYNC_ACTION_DEL_REMOTE:
                // 获取元数据
                meta = mMetaHashMap.get(node.getGid());
                // 如果元数据存在
                if (meta != null) {
                    // 删除远程元数据节点
                    GTaskClient.getInstance().deleteNode(meta);
                }
                // 删除远程任务节点
                GTaskClient.getInstance().deleteNode(node);
                break;
            case Node.SYNC_ACTION_UPDATE_LOCAL:
                // 更新本地任务节点
                updateLocalNode(node, c);
                break;
            case Node.SYNC_ACTION_UPDATE_REMOTE:
                // 更新远程任务节点
                updateRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_UPDATE_CONFLICT:
                // 合并双方的修改可能是个好主意
                // 目前只是简单地使用本地更新
                updateRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_NONE:
                break;
            case Node.SYNC_ACTION_ERROR:
            default:
                // 抛出未知同步操作类型的异常
                throw new ActionFailureException("unkown sync action type");
        }
    }

    /**
     * 添加本地任务节点
     * @param node 任务节点
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void addLocalNode(Node node) throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 本地笔记对象
        SqlNote sqlNote;
        // 如果任务节点是任务列表
        if (node instanceof TaskList) {
            // 如果任务列表的名称是默认文件夹的名称
            if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT)) {
                // 创建一个新的本地笔记对象，父ID为根文件夹的ID
                sqlNote = new SqlNote(mContext, Notes.ID_ROOT_FOLDER);
            } else if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE)) {
                // 如果任务列表的名称是通话记录文件夹的名称
                // 创建一个新的本地笔记对象，父ID为通话记录文件夹的ID
                sqlNote = new SqlNote(mContext, Notes.ID_CALL_RECORD_FOLDER);
            } else {
                // 创建一个新的本地笔记对象
                sqlNote = new SqlNote(mContext);
                // 根据任务节点的内容设置本地笔记的内容
                sqlNote.setContent(node.getLocalJSONFromContent());
                // 设置本地笔记的父ID为根文件夹的ID
                sqlNote.setParentId(Notes.ID_ROOT_FOLDER);
            }
        } else {
            // 创建一个新的本地笔记对象
            sqlNote = new SqlNote(mContext);
            // 获取任务节点的本地JSON对象
            JSONObject js = node.getLocalJSONFromContent();
            try {
                // 如果JSON对象包含笔记头部信息
                if (js.has(GTaskStringUtils.META_HEAD_NOTE)) {
                    // 获取笔记头部的JSON对象
                    JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                    // 如果笔记头部包含ID
                    if (note.has(NoteColumns.ID)) {
                        // 获取笔记的ID
                        long id = note.getLong(NoteColumns.ID);
                        // 如果该ID在数据库中已存在
                        if (DataUtils.existInNoteDatabase(mContentResolver, id)) {
                            // 该ID不可用，需要创建一个新的ID
                            note.remove(NoteColumns.ID);
                        }
                    }
                }

                // 如果JSON对象包含数据头部信息
                if (js.has(GTaskStringUtils.META_HEAD_DATA)) {
                    // 获取数据头部的JSON数组
                    JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                    // 遍历数据头部的JSON数组
                    for (int i = 0; i < dataArray.length(); i++) {
                        // 获取当前数据的JSON对象
                        JSONObject data = dataArray.getJSONObject(i);
                        // 如果数据包含ID
                        if (data.has(DataColumns.ID)) {
                            // 获取数据的ID
                            long dataId = data.getLong(DataColumns.ID);
                            // 如果该ID在数据库中已存在
                            if (DataUtils.existInDataDatabase(mContentResolver, dataId)) {
                                // 该数据ID不可用，需要创建一个新的ID
                                data.remove(DataColumns.ID);
                            }
                        }
                    }

                }
            } catch (JSONException e) {
                // 记录JSON解析错误日志
                Log.w(TAG, e.toString());
                // 打印异常堆栈信息
                e.printStackTrace();
            }
            // 根据任务节点的内容设置本地笔记的内容
            sqlNote.setContent(js);

            // 获取任务节点的父节点的Google任务ID对应的本地笔记ID
            Long parentId = mGidToNid.get(((Task) node).getParent().getGid());
            // 如果父节点的本地笔记ID不存在
            if (parentId == null) {
                // 记录错误日志，提示无法找到任务的父节点ID
                Log.e(TAG, "cannot find task's parent id locally");
                // 抛出操作失败异常
                throw new ActionFailureException("cannot add local node");
            }
            // 设置本地笔记的父ID
            sqlNote.setParentId(parentId.longValue());
        }

        // 创建本地任务节点
        sqlNote.setGtaskId(node.getGid());
        // 提交本地笔记的更改
        sqlNote.commit(false);

        // 更新Google任务ID到本地笔记ID的映射
        mGidToNid.put(node.getGid(), sqlNote.getId());
        // 更新本地笔记ID到Google任务ID的映射
        mNidToGid.put(sqlNote.getId(), node.getGid());

        // 更新远程元数据
        updateRemoteMeta(node.getGid(), sqlNote);
    }

    /**
     * 更新本地任务节点
     * @param node 任务节点
     * @param c 游标，用于查询数据库
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void updateLocalNode(Node node, Cursor c) throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 本地笔记对象
        SqlNote sqlNote;
        // 更新本地笔记
        sqlNote = new SqlNote(mContext, c);
        // 根据任务节点的内容设置本地笔记的内容
        sqlNote.setContent(node.getLocalJSONFromContent());

        // 获取任务节点的父节点的Google任务ID对应的本地笔记ID
        Long parentId = (node instanceof Task) ? mGidToNid.get(((Task) node).getParent().getGid())
                : new Long(Notes.ID_ROOT_FOLDER);
        // 如果父节点的本地笔记ID不存在
        if (parentId == null) {
            // 记录错误日志，提示无法找到任务的父节点ID
            Log.e(TAG, "cannot find task's parent id locally");
            // 抛出操作失败异常
            throw new ActionFailureException("cannot update local node");
        }
        // 设置本地笔记的父ID
        sqlNote.setParentId(parentId.longValue());
        // 提交本地笔记的更改
        sqlNote.commit(true);

        // 更新元数据信息
        updateRemoteMeta(node.getGid(), sqlNote);
    }

    /**
     * 添加远程任务节点
     * @param node 任务节点
     * @param c 游标，用于查询数据库
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void addRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 本地笔记对象
        SqlNote sqlNote = new SqlNote(mContext, c);
        // 任务节点
        Node n;

        // 远程更新
        if (sqlNote.isNoteType()) {
            // 创建一个新的任务对象
            Task task = new Task();
            // 根据本地笔记的内容设置任务的内容
            task.setContentByLocalJSON(sqlNote.getContent());

            // 获取本地笔记的父节点的Google任务ID
            String parentGid = mNidToGid.get(sqlNote.getParentId());
            // 如果父节点的Google任务ID不存在
            if (parentGid == null) {
                // 记录错误日志，提示无法找到任务的父任务列表
                Log.e(TAG, "cannot find task's parent tasklist");
                // 抛出操作失败异常
                throw new ActionFailureException("cannot add remote task");
            }
            // 将任务添加到父任务列表中
            mGTaskListHashMap.get(parentGid).addChildTask(task);

            // 在Google任务服务中创建任务
            GTaskClient.getInstance().createTask(task);
            // 获取任务节点
            n = (Node) task;

            // 添加元数据
            updateRemoteMeta(task.getGid(), sqlNote);
        } else {
            // 任务列表
            TaskList tasklist = null;

            // 如果文件夹已经存在，需要跳过
            String folderName = GTaskStringUtils.MIUI_FOLDER_PREFFIX;
            // 如果本地笔记的ID是根文件夹的ID
            if (sqlNote.getId() == Notes.ID_ROOT_FOLDER)
                // 设置文件夹名称为默认文件夹的名称
                folderName += GTaskStringUtils.FOLDER_DEFAULT;
            // 如果本地笔记的ID是通话记录文件夹的ID
            else if (sqlNote.getId() == Notes.ID_CALL_RECORD_FOLDER)
                // 设置文件夹名称为通话记录文件夹的名称
                folderName += GTaskStringUtils.FOLDER_CALL_NOTE;
            else
                // 设置文件夹名称为本地笔记的摘要
                folderName += sqlNote.getSnippet();

            // 遍历任务列表的哈希映射
            Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
            while (iter.hasNext()) {
                // 获取当前任务列表的映射项
                Map.Entry<String, TaskList> entry = iter.next();
                // 获取任务列表的ID
                String gid = entry.getKey();
                // 获取任务列表
                TaskList list = entry.getValue();

                // 如果任务列表的名称与文件夹名称相同
                if (list.getName().equals(folderName)) {
                    // 获取任务列表
                    tasklist = list;
                    // 如果任务哈希映射中包含该任务列表的ID
                    if (mGTaskHashMap.containsKey(gid)) {
                        // 从任务哈希映射中移除该任务列表
                        mGTaskHashMap.remove(gid);
                    }
                    break;
                }
            }

            // 如果没有匹配的任务列表，则可以添加新的任务列表
            if (tasklist == null) {
                // 创建一个新的任务列表
                tasklist = new TaskList();
                // 根据本地笔记的内容设置任务列表的内容
                tasklist.setContentByLocalJSON(sqlNote.getContent());
                // 在Google任务服务中创建任务列表
                GTaskClient.getInstance().createTaskList(tasklist);
                // 将任务列表添加到任务列表哈希映射中
                mGTaskListHashMap.put(tasklist.getGid(), tasklist);
            }
            // 获取任务节点
            n = (Node) tasklist;
        }

        // 更新本地笔记
        sqlNote.setGtaskId(n.getGid());
        // 提交本地笔记的更改
        sqlNote.commit(false);
        // 重置本地修改标志
        sqlNote.resetLocalModified();
        // 提交本地笔记的更改
        sqlNote.commit(true);

        // 更新Google任务ID到本地笔记ID的映射
        mGidToNid.put(n.getGid(), sqlNote.getId());
        // 更新本地笔记ID到Google任务ID的映射
        mNidToGid.put(sqlNote.getId(), n.getGid());
    }

    /**
     * 更新远程任务节点
     * @param node 任务节点
     * @param c 游标，用于查询数据库
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void updateRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 本地笔记对象
        SqlNote sqlNote = new SqlNote(mContext, c);

        // 远程更新
        // 根据本地笔记的内容设置任务节点的内容
        node.setContentByLocalJSON(sqlNote.getContent());
        // 将任务节点添加到更新列表中
        GTaskClient.getInstance().addUpdateNode(node);

        // 更新元数据
        updateRemoteMeta(node.getGid(), sqlNote);

        // 必要时移动任务
        if (sqlNote.isNoteType()) {
            // 获取任务节点
            Task task = (Task) node;
            // 获取任务节点的父任务列表
            TaskList preParentList = task.getParent();

            // 获取本地笔记的父节点的Google任务ID
            String curParentGid = mNidToGid.get(sqlNote.getParentId());
            // 如果父节点的Google任务ID不存在
            if (curParentGid == null) {
                // 记录错误日志，提示无法找到任务的父任务列表
                Log.e(TAG, "cannot find task's parent tasklist");
                // 抛出操作失败异常
                throw new ActionFailureException("cannot update remote task");
            }
            // 获取当前父任务列表
            TaskList curParentList = mGTaskListHashMap.get(curParentGid);

            // 如果父任务列表发生了变化
            if (preParentList != curParentList) {
                // 从原父任务列表中移除任务节点
                preParentList.removeChildTask(task);
                // 将任务节点添加到新父任务列表中
                curParentList.addChildTask(task);
                // 在Google任务服务中移动任务节点
                GTaskClient.getInstance().moveTask(task, preParentList, curParentList);
            }
        }

        // 清除本地修改标志
        sqlNote.resetLocalModified();
        // 提交本地笔记的更改
        sqlNote.commit(true);
    }

    /**
     * 更新远程元数据
     * @param gid Google任务的ID
     * @param sqlNote 本地笔记对象
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void updateRemoteMeta(String gid, SqlNote sqlNote) throws NetworkFailureException {
        // 如果本地笔记对象不为空且是笔记类型
        if (sqlNote != null && sqlNote.isNoteType()) {
            // 从元数据哈希映射中获取元数据
            MetaData metaData = mMetaHashMap.get(gid);
            // 如果元数据存在
            if (metaData != null) {
                // 设置元数据的内容
                metaData.setMeta(gid, sqlNote.getContent());
                // 将元数据添加到更新列表中
                GTaskClient.getInstance().addUpdateNode(metaData);
            } else {
                // 创建一个新的元数据对象
                metaData = new MetaData();
                // 设置元数据的内容
                metaData.setMeta(gid, sqlNote.getContent());
                // 将元数据添加到元数据列表中
                mMetaList.addChildTask(metaData);
                // 将元数据添加到元数据哈希映射中
                mMetaHashMap.put(gid, metaData);
                // 在Google任务服务中创建元数据
                GTaskClient.getInstance().createTask(metaData);
            }
        }
    }

    /**
     * 刷新本地同步ID
     * @throws NetworkFailureException 如果网络连接失败
     */
    private void refreshLocalSyncId() throws NetworkFailureException {
        // 检查同步是否已取消
        if (mCancelled) {
            return;
        }

        // 获取最新的Google任务列表
        mGTaskHashMap.clear();
        mGTaskListHashMap.clear();
        mMetaHashMap.clear();
        // 初始化Google任务列表
        initGTaskList();

        // 游标，用于查询数据库
        Cursor c = null;
        try {
            // 查询本地笔记
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id<>?)", new String[] {
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            // 如果查询结果不为空
            if (c != null) {
                // 遍历查询结果
                while (c.moveToNext()) {
                    // 获取Google任务的ID
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    // 从任务哈希映射中获取任务节点
                    Node node = mGTaskHashMap.get(gid);
                    // 如果任务节点存在
                    if (node != null) {
                        // 从任务哈希映射中移除任务节点
                        mGTaskHashMap.remove(gid);
                        // 创建内容值对象
                        ContentValues values = new ContentValues();
                        // 设置同步ID
                        values.put(NoteColumns.SYNC_ID, node.getLastModified());
                        // 更新本地笔记的同步ID
                        mContentResolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                                c.getLong(SqlNote.ID_COLUMN)), values, null, null);
                    } else {
                        // 记录错误日志，提示同步后有些本地项没有Google任务ID
                        Log.e(TAG, "something is missed");
                        // 抛出操作失败异常
                        throw new ActionFailureException(
                                "some local items don't have gid after sync");
                    }
                }
            } else {
                // 记录查询失败日志
                Log.w(TAG, "failed to query local note to refresh sync id");
            }
        } finally {
            // 关闭游标
            if (c != null) {
                c.close();
                c = null;
            }
        }
    }

    /**
     * 获取同步账户的名称
     * @return 同步账户的名称
     */
    public String getSyncAccount() {
        // 返回同步账户的名称
        return GTaskClient.getInstance().getSyncAccount().name;
    }

    /**
     * 取消同步操作
     */
    public void cancelSync() {
        // 设置取消标志为已取消
        mCancelled = true;
    }
}
