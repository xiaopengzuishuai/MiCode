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

package net.micode.notes.gtask.data;

import android.database.Cursor;
import org.json.JSONObject;

// 同步任务节点抽象基类
public abstract class Node {
    // 同步动作常量定义
    public static final int SYNC_ACTION_NONE = 0;          // 无同步动作
    public static final int SYNC_ACTION_ADD_REMOTE = 1;    // 需要远程添加的同步动作
    public static final int SYNC_ACTION_ADD_LOCAL = 2;     // 需要本地添加的同步动作
    public static final int SYNC_ACTION_DEL_REMOTE = 3;    // 需要远程删除的同步动作
    public static final int SYNC_ACTION_DEL_LOCAL = 4;     // 需要本地删除的同步动作
    public static final int SYNC_ACTION_UPDATE_REMOTE = 5; // 需要远程更新的同步动作
    public static final int SYNC_ACTION_UPDATE_LOCAL = 6;  // 需要本地更新的同步动作
    public static final int SYNC_ACTION_UPDATE_CONFLICT = 7; // 存在更新冲突的同步动作
    public static final int SYNC_ACTION_ERROR = 8;         // 同步错误状态

    // 节点属性
    private String mGid;       // 节点全局唯一标识
    private String mName;      // 节点名称
    private long mLastModified; // 最后修改时间戳
    private boolean mDeleted;   // 删除标志位

    // 节点构造函数
    public Node() {
        mGid = null;
        mName = "";
        mLastModified = 0;
        mDeleted = false;
    }

    // 抽象方法定义
    public abstract JSONObject getCreateAction(int actionId); // 生成创建动作的JSON对象
    public abstract JSONObject getUpdateAction(int actionId); // 生成更新动作的JSON对象
    public abstract void setContentByRemoteJSON(JSONObject js); // 从远程JSON设置节点内容
    public abstract void setContentByLocalJSON(JSONObject js);  // 从本地JSON设置节点内容
    public abstract JSONObject getLocalJSONFromContent();       // 生成本地存储用的JSON对象
    public abstract int getSyncAction(Cursor c);                // 从数据库游标获取同步动作

    // 以下是属性的getter和setter方法
    public void setGid(String gid) {
        this.mGid = gid;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    public void setDeleted(boolean deleted) {
        this.mDeleted = deleted;
    }

    public String getGid() {
        return this.mGid;
    }

    public String getName() {
        return this.mName;
    }

    public long getLastModified() {
        return this.mLastModified;
    }

    public boolean getDeleted() {
        return this.mDeleted;
    }
}
