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
 * Contact 类提供了一个实用方法，用于根据电话号码获取联系人姓名。
 * 它使用静态缓存来提高性能，避免对相同电话号码进行重复的数据库查询。
 */
package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * 用于从电话号码中检索联系人姓名的实用类。
 * 该类使用静态缓存来存储电话号码和联系人姓名之间的映射关系，
 * 有助于减少数据库查询次数。
 */
public class Contact {
    /**
     * 静态缓存，用于存储电话号码及其对应的联系人姓名。
     * 此缓存用于避免对相同电话号码进行重复的数据库查询。
     */
    private static HashMap<String, String> sContactCache;

    /**
     * 用于记录与该类相关日志消息的标签。
     */
    private static final String TAG = "Contact";

    /**
     * SQL 查询语句，用于从联系人数据库中查询特定电话号码对应的联系人信息。
     * 占位符 '+' 会被替换为电话号码的最小匹配值。
     */
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
    + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
    + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    /**
     * 根据给定的电话号码获取联系人姓名。
     * 如果联系人姓名已经存在于缓存中，则直接返回。
     * 否则，执行数据库查询以查找联系人姓名，并将结果存入缓存。
     *
     * @param context 应用程序上下文，用于访问内容解析器。
     * @param phoneNumber 要查询联系人姓名的电话号码。
     * @return 如果找到联系人姓名，则返回该姓名；如果未找到匹配项或发生错误，则返回 null。
     */
    public static String getContact(Context context, String phoneNumber) {
        // 如果缓存为空，则初始化缓存
        if(sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }

        // 如果缓存中已经存在该电话号码对应的联系人姓名，则直接返回
        if(sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        // 替换查询语句中的占位符
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        // 执行数据库查询
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String [] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);

        // 如果查询结果不为空且有数据
        if (cursor != null && cursor.moveToFirst()) {
            try {
                // 获取联系人姓名
                String name = cursor.getString(0);
                // 将电话号码和联系人姓名存入缓存
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                // 记录异常信息
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                // 关闭游标
                cursor.close();
            }
        } else {
            // 记录未找到匹配联系人的信息
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}
