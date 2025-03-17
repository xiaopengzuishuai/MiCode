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

package net.micode.notes.widget;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NoteEditActivity;
import net.micode.notes.ui.NotesListActivity;

/**
 * NoteWidgetProvider 是一个抽象类，继承自 AppWidgetProvider，用于处理便签小部件的更新、删除等操作。
 */
public abstract class NoteWidgetProvider extends AppWidgetProvider {
    // 查询便签信息时使用的投影列
    public static final String [] PROJECTION = new String [] {
        NoteColumns.ID,
        NoteColumns.BG_COLOR_ID,
        NoteColumns.SNIPPET
    };

    // 投影列的索引
    public static final int COLUMN_ID           = 0;
    public static final int COLUMN_BG_COLOR_ID  = 1;
    public static final int COLUMN_SNIPPET      = 2;

    // 日志标签
    private static final String TAG = "NoteWidgetProvider";

    /**
     * 当小部件被删除时调用此方法。
     *
     * @param context       应用程序上下文
     * @param appWidgetIds  被删除的小部件 ID 数组
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // 创建 ContentValues 对象，用于更新数据库中的便签信息
        ContentValues values = new ContentValues();
        values.put(NoteColumns.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        // 遍历被删除的小部件 ID 数组，更新数据库中的便签信息
        for (int i = 0; i < appWidgetIds.length; i++) {
            context.getContentResolver().update(Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.WIDGET_ID + "=?",
                    new String[] { String.valueOf(appWidgetIds[i])});
        }
    }

    /**
     * 根据小部件 ID 获取便签信息的游标。
     *
     * @param context   应用程序上下文
     * @param widgetId  小部件 ID
     * @return 包含便签信息的游标
     */
    private Cursor getNoteWidgetInfo(Context context, int widgetId) {
        return context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(widgetId), String.valueOf(Notes.ID_TRASH_FOLER) },
                null);
    }

    /**
     * 更新小部件的方法，不考虑隐私模式。
     *
     * @param context          应用程序上下文
     * @param appWidgetManager AppWidgetManager 实例
     * @param appWidgetIds     要更新的小部件 ID 数组
     */
    protected void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, false);
    }

    /**
     * 更新小部件的方法，可考虑隐私模式。
     *
     * @param context          应用程序上下文
     * @param appWidgetManager AppWidgetManager 实例
     * @param appWidgetIds     要更新的小部件 ID 数组
     * @param privacyMode      是否处于隐私模式
     */
    private void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
            boolean privacyMode) {
        // 遍历要更新的小部件 ID 数组
        for (int i = 0; i < appWidgetIds.length; i++) {
            if (appWidgetIds[i] != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // 默认背景 ID
                int bgId = ResourceParser.getDefaultBgId(context);
                // 便签摘要
                String snippet = "";
                // 创建启动便签编辑活动的意图
                Intent intent = new Intent(context, NoteEditActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_ID, appWidgetIds[i]);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_TYPE, getWidgetType());

                // 获取便签信息的游标
                Cursor c = getNoteWidgetInfo(context, appWidgetIds[i]);
                if (c != null && c.moveToFirst()) {
                    if (c.getCount() > 1) {
                        // 日志记录：存在多个具有相同小部件 ID 的便签
                        Log.e(TAG, "Multiple message with same widget id:" + appWidgetIds[i]);
                        c.close();
                        return;
                    }
                    // 获取便签摘要
                    snippet = c.getString(COLUMN_SNIPPET);
                    // 获取背景 ID
                    bgId = c.getInt(COLUMN_BG_COLOR_ID);
                    // 添加便签 ID 到意图中
                    intent.putExtra(Intent.EXTRA_UID, c.getLong(COLUMN_ID));
                    // 设置意图动作为查看
                    intent.setAction(Intent.ACTION_VIEW);
                } else {
                    // 如果没有找到便签信息，设置默认摘要
                    snippet = context.getResources().getString(R.string.widget_havenot_content);
                    // 设置意图动作为插入或编辑
                    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                }

                if (c != null) {
                    // 关闭游标
                    c.close();
                }

                // 创建 RemoteViews 对象，用于更新小部件的视图
                RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
                // 设置小部件的背景图像资源
                rv.setImageViewResource(R.id.widget_bg_image, getBgResourceId(bgId));
                // 添加背景 ID 到意图中
                intent.putExtra(Notes.INTENT_EXTRA_BACKGROUND_ID, bgId);

                /**
                 * 生成用于启动小部件宿主的 PendingIntent
                 */
                PendingIntent pendingIntent = null;
                if (privacyMode) {
                    // 在隐私模式下，设置小部件文本为隐私模式提示
                    rv.setTextViewText(R.id.widget_text,
                            context.getString(R.string.widget_under_visit_mode));
                    // 创建启动便签列表活动的 PendingIntent
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], new Intent(
                            context, NotesListActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                } else {
                    // 在非隐私模式下，设置小部件文本为便签摘要
                    rv.setTextViewText(R.id.widget_text, snippet);
                    // 创建启动便签编辑活动的 PendingIntent
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }

                // 设置小部件文本的点击事件
                rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
                // 更新小部件
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        }
    }

    /**
     * 根据背景 ID 获取小部件的背景资源 ID，由子类实现。
     *
     * @param bgId 背景 ID
     * @return 背景资源 ID
     */
    protected abstract int getBgResourceId(int bgId);

    /**
     * 获取小部件的布局资源 ID，由子类实现。
     *
     * @return 布局资源 ID
     */
    protected abstract int getLayoutId();

    /**
     * 获取小部件的类型，由子类实现。
     *
     * @return 小部件类型
     */
    protected abstract int getWidgetType();
}
