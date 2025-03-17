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

package net.micode.notes.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
// 导入上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入意图类，用于在组件间传递消息
import android.content.Intent;
// 导入游标类，用于处理数据库查询结果
import android.database.Cursor;

// 导入笔记数据相关的类
import net.micode.notes.data.Notes;
// 导入笔记列相关的类，用于定义数据库表的列名
import net.micode.notes.data.Notes.NoteColumns;

/**
 * AlarmInitReceiver 类继承自 BroadcastReceiver，用于在接收到广播时初始化闹钟提醒。
 * 该类会查询数据库中所有提醒时间晚于当前时间的笔记，并为这些笔记设置闹钟提醒。
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    /**
     * 定义查询投影，指定要从数据库中查询的列。
     * 这里只查询笔记的 ID 和提醒日期。
     */
    private static final String [] PROJECTION = new String [] {
        NoteColumns.ID, // 笔记的 ID 列
        NoteColumns.ALERTED_DATE // 笔记的提醒日期列
    };

    /**
     * 定义投影中笔记 ID 列的索引，方便后续从游标中获取对应的值。
     */
    private static final int COLUMN_ID = 0;
    /**
     * 定义投影中笔记提醒日期列的索引，方便后续从游标中获取对应的值。
     */
    private static final int COLUMN_ALERTED_DATE = 1;

    /**
     * 当接收到广播时，此方法会被调用。
     * 它会查询数据库中所有提醒时间晚于当前时间的笔记，并为这些笔记设置闹钟提醒。
     *
     * @param context 广播接收器所在的上下文环境
     * @param intent  接收到的广播意图
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取当前时间，用于筛选出提醒时间晚于当前时间的笔记
        long currentDate = System.currentTimeMillis();
        // 执行数据库查询，获取提醒时间晚于当前时间且类型为笔记的记录
        Cursor c = context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                // 查询条件：提醒日期大于当前时间且笔记类型为普通笔记
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[] { String.valueOf(currentDate) },
                null);

        // 检查游标是否不为空，即是否有符合条件的记录
        if (c != null) {
            // 尝试将游标移动到第一条记录
            if (c.moveToFirst()) {
                // 使用 do-while 循环遍历游标中的所有记录
                do {
                    // 从游标中获取当前记录的提醒日期
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                    // 创建一个新的意图，用于启动 AlarmReceiver 广播接收器
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    // 设置意图的数据为笔记的 URI，包含笔记的 ID
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
                    // 创建一个待处理意图，用于在指定时间触发广播
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
                    // 获取系统的闹钟服务
                    AlarmManager alermManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);
                    // 设置闹钟，在指定的提醒日期触发待处理意图
                    alermManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext()); // 移动到下一条记录，继续循环
            }
            // 关闭游标，释放资源
            c.close();
        }
    }
}
