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

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;

/**
 * NoteWidgetProvider_2x 类继承自 NoteWidgetProvider，用于处理 2x 大小的便签小部件的更新逻辑。
 */
public class NoteWidgetProvider_2x extends NoteWidgetProvider {
    /**
     * 当小部件更新时调用此方法。
     *
     * @param context          应用程序上下文
     * @param appWidgetManager AppWidgetManager 实例
     * @param appWidgetIds     要更新的小部件 ID 数组
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 调用父类的 update 方法进行小部件更新
        super.update(context, appWidgetManager, appWidgetIds);
    }

    /**
     * 获取 2x 小部件的布局资源 ID。
     *
     * @return 布局资源 ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.widget_2x;
    }

    /**
     * 根据背景 ID 获取 2x 小部件的背景资源 ID。
     *
     * @param bgId 背景 ID
     * @return 背景资源 ID
     */
    @Override
    protected int getBgResourceId(int bgId) {
        return ResourceParser.WidgetBgResources.getWidget2xBgResource(bgId);
    }

    /**
     * 获取 2x 小部件的类型。
     *
     * @return 小部件类型
     */
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_2X;
    }
}
