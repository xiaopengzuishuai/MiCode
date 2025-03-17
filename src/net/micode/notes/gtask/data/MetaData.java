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

// 包声明，表明该类属于 net.micode.notes.gtask.data 包
package net.micode.notes.gtask.data;

// 导入 Android 数据库游标类，用于处理数据库查询结果
import android.database.Cursor;
// 导入 Android 日志工具类，用于记录日志信息
import android.util.Log;

// 导入自定义的 GTask 字符串工具类，包含相关常量和字符串处理方法
import net.micode.notes.tool.GTaskStringUtils;

// 导入 JSON 异常类，用于处理 JSON 操作时可能出现的异常
import org.json.JSONException;
// 导入 JSON 对象类，用于处理 JSON 数据
import org.json.JSONObject;

/**
 * MetaData 类继承自 Task 类，用于处理任务相关的元数据。
 * 它包含了设置元数据、获取关联 GID 等功能，并且重写了一些父类方法以适应元数据处理的需求。
 */
public class MetaData extends Task {
    // 定义日志标签，使用类名作为标签，方便在日志中定位和区分不同类的日志信息
    private final static String TAG = MetaData.class.getSimpleName();

    // 关联的 GID（全局唯一标识符），用于标识与该元数据相关的任务
    private String mRelatedGid = null;

    /**
     * 设置元数据信息。
     * 将关联的 GID 放入元数据 JSON 对象中，并设置笔记的内容和名称。
     * 
     * @param gid 关联的 GID
     * @param metaInfo 包含元数据信息的 JSON 对象
     */
    public void setMeta(String gid, JSONObject metaInfo) {
        try {
            // 尝试将关联的 GID 放入元数据 JSON 对象中
            metaInfo.put(GTaskStringUtils.META_HEAD_GTASK_ID, gid);
        } catch (JSONException e) {
            // 若发生 JSON 异常，记录错误日志
            Log.e(TAG, "failed to put related gid");
        }
        // 将元数据 JSON 对象转换为字符串并设置为笔记的内容
        setNotes(metaInfo.toString());
        // 设置笔记的名称为元数据笔记名称
        setName(GTaskStringUtils.META_NOTE_NAME);
    }

    /**
     * 获取关联的 GID。
     * 
     * @return 关联的 GID
     */
    public String getRelatedGid() {
        return mRelatedGid;
    }

    /**
     * 判断该元数据是否值得保存。
     * 如果笔记内容不为空，则认为该元数据值得保存。
     * 
     * @return 如果笔记内容不为空返回 true，否则返回 false
     */
    @Override
    public boolean isWorthSaving() {
        return getNotes() != null;
    }

    /**
     * 根据远程 JSON 对象设置元数据内容。
     * 调用父类的方法设置内容，并尝试从笔记内容中解析出关联的 GID。
     * 
     * @param js 包含远程元数据信息的 JSON 对象
     */
    @Override
    public void setContentByRemoteJSON(JSONObject js) {
        // 调用父类的方法设置内容
        super.setContentByRemoteJSON(js);
        // 如果笔记内容不为空
        if (getNotes() != null) {
            try {
                // 去除笔记内容的首尾空格并转换为 JSON 对象
                JSONObject metaInfo = new JSONObject(getNotes().trim());
                // 从 JSON 对象中获取关联的 GID
                mRelatedGid = metaInfo.getString(GTaskStringUtils.META_HEAD_GTASK_ID);
            } catch (JSONException e) {
                // 若发生 JSON 异常，记录警告日志并将关联的 GID 置为 null
                Log.w(TAG, "failed to get related gid");
                mRelatedGid = null;
            }
        }
    }

    /**
     * 根据本地 JSON 对象设置元数据内容。
     * 该方法不应该被调用，调用时会抛出非法访问错误。
     * 
     * @param js 包含本地元数据信息的 JSON 对象
     */
    @Override
    public void setContentByLocalJSON(JSONObject js) {
        // 该函数不应该被调用，抛出异常提示
        throw new IllegalAccessError("MetaData:setContentByLocalJSON should not be called");
    }

    /**
     * 从元数据内容中获取本地 JSON 对象。
     * 该方法不应该被调用，调用时会抛出非法访问错误。
     * 
     * @return 无返回值，因为会抛出异常
     */
    @Override
    public JSONObject getLocalJSONFromContent() {
        // 该函数不应该被调用，抛出异常提示
        throw new IllegalAccessError("MetaData:getLocalJSONFromContent should not be called");
    }

    /**
     * 获取同步操作。
     * 该方法不应该被调用，调用时会抛出非法访问错误。
     * 
     * @param c 数据库游标
     * @return 无返回值，因为会抛出异常
     */
    @Override
    public int getSyncAction(Cursor c) {
        // 该函数不应该被调用，抛出异常提示
        throw new IllegalAccessError("MetaData:getSyncAction should not be called");
    }

}
