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

// 导入 Android 上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 游标类，用于处理数据库查询结果
import android.database.Cursor;
// 导入 Android 视图类，是所有 UI 组件的基类
import android.view.View;
// 导入 Android 视图组类，用于容纳其他视图
import android.view.ViewGroup;
// 导入 Android 游标适配器类，用于将游标数据绑定到视图
import android.widget.CursorAdapter;
// 导入 Android 线性布局类，用于按线性方向排列子视图
import android.widget.LinearLayout;
// 导入 Android 文本视图类，用于显示文本
import android.widget.TextView;

// 导入应用的资源类，用于访问应用的资源，如字符串、布局等
import net.micode.notes.R;
// 导入应用的笔记数据类
import net.micode.notes.data.Notes;
// 导入应用的笔记列相关类，用于定义数据库表的列名
import net.micode.notes.data.Notes.NoteColumns;

/**
 * FoldersListAdapter 类继承自 CursorAdapter，用于将数据库中的文件夹数据绑定到视图列表中。
 * 该类提供了将文件夹数据显示在列表项中的功能，同时处理了根文件夹的特殊显示。
 */
public class FoldersListAdapter extends CursorAdapter {
    // 定义查询投影，指定要从数据库中查询的列。
    // 这里查询笔记的 ID 和摘要，用于显示文件夹信息。
    public static final String [] PROJECTION = {
        NoteColumns.ID, // 笔记的 ID 列
        NoteColumns.SNIPPET // 笔记的摘要列
    };

    // 定义投影中笔记 ID 列的索引，方便后续从游标中获取对应的值。
    public static final int ID_COLUMN   = 0;
    // 定义投影中笔记摘要列的索引，这里将摘要作为文件夹名称使用。
    public static final int NAME_COLUMN = 1;

    /**
     * 构造函数，用于创建 FoldersListAdapter 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param c       包含文件夹数据的游标
     */
    public FoldersListAdapter(Context context, Cursor c) {
        // 调用父类的构造函数
        super(context, c);
        // TODO: 这里是自动生成的构造函数代码，可能需要根据实际需求进行完善
    }

    /**
     * 创建一个新的视图，用于显示游标中的数据。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param cursor  包含文件夹数据的游标
     * @param parent  新视图的父视图组
     * @return 新创建的视图实例
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // 创建一个新的文件夹列表项视图
        return new FolderListItem(context);
    }

    /**
     * 将游标中的数据绑定到已有的视图上。
     *
     * @param view    要绑定数据的视图
     * @param context 上下文对象，用于获取系统服务和资源
     * @param cursor  包含文件夹数据的游标
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // 检查视图是否为 FolderListItem 类型
        if (view instanceof FolderListItem) {
            // 获取当前行的文件夹 ID
            long folderId = cursor.getLong(ID_COLUMN);
            // 如果文件夹 ID 等于根文件夹的 ID
            if (folderId == Notes.ID_ROOT_FOLDER) {
                // 获取根文件夹的显示名称，从资源文件中读取
                String folderName = context.getString(R.string.menu_move_parent_folder);
                // 将文件夹名称绑定到视图上
                ((FolderListItem) view).bind(folderName);
            } else {
                // 否则，获取游标中摘要列的值作为文件夹名称
                String folderName = cursor.getString(NAME_COLUMN);
                // 将文件夹名称绑定到视图上
                ((FolderListItem) view).bind(folderName);
            }
        }
    }

    /**
     * 根据列表项的位置获取文件夹的名称。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param position 列表项的位置
     * @return 文件夹的名称
     */
    public String getFolderName(Context context, int position) {
        // 获取指定位置的游标数据
        Cursor cursor = (Cursor) getItem(position);
        // 获取当前行的文件夹 ID
        long folderId = cursor.getLong(ID_COLUMN);
        // 如果文件夹 ID 等于根文件夹的 ID
        if (folderId == Notes.ID_ROOT_FOLDER) {
            // 获取根文件夹的显示名称，从资源文件中读取
            return context.getString(R.string.menu_move_parent_folder);
        } else {
            // 否则，获取游标中摘要列的值作为文件夹名称
            return cursor.getString(NAME_COLUMN);
        }
    }

    /**
     * 内部类，用于表示文件夹列表项的视图。
     */
    private class FolderListItem extends LinearLayout {
        // 用于显示文件夹名称的文本视图
        private TextView mName;

        /**
         * 构造函数，用于创建 FolderListItem 实例。
         *
         * @param context 上下文对象，用于获取系统服务和资源
         */
        public FolderListItem(Context context) {
            // 调用父类的构造函数
            super(context);
            // 从布局文件中填充视图
            inflate(context, R.layout.folder_list_item, this);
            // 查找用于显示文件夹名称的文本视图
            mName = (TextView) findViewById(R.id.tv_folder_name);
        }

        /**
         * 将文件夹名称绑定到文本视图上。
         *
         * @param name 文件夹的名称
         */
        public void bind(String name) {
            // 设置文本视图的文本为传入的文件夹名称
            mName.setText(name);
        }
    }
}
