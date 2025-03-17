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

// 指定当前类所在的包名
package net.micode.notes.ui;

// 导入 Android 上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 游标类，用于处理数据库查询结果
import android.database.Cursor;
// 导入 Android 日志类，用于记录日志信息
import android.util.Log;
// 导入 Android 视图类，是所有 UI 组件的基类
import android.view.View;
// 导入 Android 视图组类，用于容纳其他视图
import android.view.ViewGroup;
// 导入 Android 游标适配器类，用于将游标数据绑定到视图
import android.widget.CursorAdapter;

// 导入应用的笔记数据类
import net.micode.notes.data.Notes;

// 导入 Java 中的集合接口，用于表示一组对象
import java.util.Collection;
// 导入 Java 中的哈希映射类，用于存储键值对
import java.util.HashMap;
// 导入 Java 中的哈希集合类，用于存储唯一的元素
import java.util.HashSet;
// 导入 Java 中的迭代器接口，用于遍历集合中的元素
import java.util.Iterator;

/**
 * NotesListAdapter 类继承自 CursorAdapter，用于将数据库中的笔记数据绑定到视图列表中。
 * 该类提供了笔记列表项的选择、全选、获取选中项 ID 等功能，同时支持选择模式的切换。
 */
public class NotesListAdapter extends CursorAdapter {
    // 定义日志标签，用于在日志中标识该类的日志信息
    private static final String TAG = "NotesListAdapter";
    // 上下文对象，用于获取系统服务和资源
    private Context mContext;
    // 存储选中项的索引及其选中状态的映射
    private HashMap<Integer, Boolean> mSelectedIndex;
    // 笔记的数量
    private int mNotesCount;
    // 是否处于选择模式
    private boolean mChoiceMode;

    /**
     * 内部类，用于表示小部件的属性，包含小部件的 ID 和类型。
     */
    public static class AppWidgetAttribute {
        // 小部件的 ID
        public int widgetId;
        // 小部件的类型
        public int widgetType;
    };

    /**
     * 构造函数，用于创建 NotesListAdapter 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     */
    public NotesListAdapter(Context context) {
        // 调用父类的构造函数，传入上下文和初始游标为 null
        super(context, null);
        // 初始化选中项索引映射
        mSelectedIndex = new HashMap<Integer, Boolean>();
        // 保存上下文对象
        mContext = context;
        // 初始化笔记数量为 0
        mNotesCount = 0;
    }

    /**
     * 创建一个新的视图，用于显示游标中的数据。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param cursor  包含笔记数据的游标
     * @param parent  新视图的父视图组
     * @return 新创建的视图实例
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // 创建一个新的笔记列表项视图
        return new NotesListItem(context);
    }

    /**
     * 将游标中的数据绑定到已有的视图上。
     *
     * @param view    要绑定数据的视图
     * @param context 上下文对象，用于获取系统服务和资源
     * @param cursor  包含笔记数据的游标
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // 检查视图是否为 NotesListItem 类型
        if (view instanceof NotesListItem) {
            // 创建一个 NoteItemData 对象，用于封装笔记项的数据
            NoteItemData itemData = new NoteItemData(context, cursor);
            // 将笔记项数据、选择模式和选中状态绑定到视图上
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
        }
    }

    /**
     * 设置指定位置的笔记项的选中状态。
     *
     * @param position 笔记项的位置
     * @param checked  是否选中
     */
    public void setCheckedItem(final int position, final boolean checked) {
        // 将指定位置的选中状态存入映射
        mSelectedIndex.put(position, checked);
        // 通知适配器数据已更改，刷新视图
        notifyDataSetChanged();
    }

    /**
     * 判断是否处于选择模式。
     *
     * @return 如果处于选择模式则返回 true，否则返回 false
     */
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    /**
     * 设置选择模式。
     *
     * @param mode 是否开启选择模式
     */
    public void setChoiceMode(boolean mode) {
        // 清空选中项索引映射
        mSelectedIndex.clear();
        // 设置选择模式
        mChoiceMode = mode;
    }

    /**
     * 全选或取消全选所有笔记项。
     *
     * @param checked 是否全选
     */
    public void selectAll(boolean checked) {
        // 获取游标
        Cursor cursor = getCursor();
        // 遍历所有笔记项
        for (int i = 0; i < getCount(); i++) {
            // 将游标移动到指定位置
            if (cursor.moveToPosition(i)) {
                // 如果笔记项的类型为普通笔记
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    // 设置指定位置的笔记项的选中状态
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    /**
     * 获取所有选中的笔记项的 ID。
     *
     * @return 包含所有选中笔记项 ID 的哈希集合
     */
    public HashSet<Long> getSelectedItemIds() {
        // 创建一个哈希集合，用于存储选中笔记项的 ID
        HashSet<Long> itemSet = new HashSet<Long>();
        // 遍历选中项索引映射的键集
        for (Integer position : mSelectedIndex.keySet()) {
            // 如果指定位置的笔记项被选中
            if (mSelectedIndex.get(position) == true) {
                // 获取指定位置的笔记项的 ID
                Long id = getItemId(position);
                // 如果笔记项的 ID 为根文件夹的 ID
                if (id == Notes.ID_ROOT_FOLDER) {
                    // 记录日志，提示错误的笔记项 ID
                    Log.d(TAG, "Wrong item id, should not happen");
                } else {
                    // 将笔记项的 ID 存入哈希集合
                    itemSet.add(id);
                }
            }
        }
        // 返回哈希集合
        return itemSet;
    }

    /**
     * 获取所有选中的笔记项的小部件属性。
     *
     * @return 包含所有选中笔记项小部件属性的哈希集合
     */
    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        // 创建一个哈希集合，用于存储选中笔记项的小部件属性
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        // 遍历选中项索引映射的键集
        for (Integer position : mSelectedIndex.keySet()) {
            // 如果指定位置的笔记项被选中
            if (mSelectedIndex.get(position) == true) {
                // 获取指定位置的游标数据
                Cursor c = (Cursor) getItem(position);
                // 如果游标数据不为空
                if (c != null) {
                    // 创建一个 AppWidgetAttribute 对象，用于存储小部件属性
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    // 创建一个 NoteItemData 对象，用于封装笔记项的数据
                    NoteItemData item = new NoteItemData(mContext, c);
                    // 设置小部件的 ID
                    widget.widgetId = item.getWidgetId();
                    // 设置小部件的类型
                    widget.widgetType = item.getWidgetType();
                    // 将小部件属性存入哈希集合
                    itemSet.add(widget);
                    /**
                     * 不要在这里关闭游标，只有适配器可以关闭它
                     */
                } else {
                    // 记录错误日志，提示无效的游标
                    Log.e(TAG, "Invalid cursor");
                    // 返回 null
                    return null;
                }
            }
        }
        // 返回哈希集合
        return itemSet;
    }

    /**
     * 获取选中的笔记项的数量。
     *
     * @return 选中的笔记项的数量
     */
    public int getSelectedCount() {
        // 获取选中项索引映射的值集
        Collection<Boolean> values = mSelectedIndex.values();
        // 如果值集为空
        if (null == values) {
            // 返回 0
            return 0;
        }
        // 获取值集的迭代器
        Iterator<Boolean> iter = values.iterator();
        // 初始化选中项数量为 0
        int count = 0;
        // 遍历值集
        while (iter.hasNext()) {
            // 如果当前元素为 true
            if (true == iter.next()) {
                // 选中项数量加 1
                count++;
            }
        }
        // 返回选中项数量
        return count;
    }

    /**
     * 判断是否全选。
     *
     * @return 如果全选则返回 true，否则返回 false
     */
    public boolean isAllSelected() {
        // 获取选中的笔记项的数量
        int checkedCount = getSelectedCount();
        // 判断选中的笔记项数量是否不为 0 且等于笔记的总数
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    /**
     * 判断指定位置的笔记项是否被选中。
     *
     * @param position 笔记项的位置
     * @return 如果被选中则返回 true，否则返回 false
     */
    public boolean isSelectedItem(final int position) {
        // 如果指定位置的选中状态为 null
        if (null == mSelectedIndex.get(position)) {
            // 返回 false
            return false;
        }
        // 返回指定位置的选中状态
        return mSelectedIndex.get(position);
    }

    /**
     * 当内容发生变化时的回调方法。
     * 重新计算笔记的数量。
     */
    @Override
    protected void onContentChanged() {
        // 调用父类的内容变化处理方法
        super.onContentChanged();
        // 重新计算笔记的数量
        calcNotesCount();
    }

    /**
     * 更改游标时的回调方法。
     * 重新计算笔记的数量。
     *
     * @param cursor 新的游标
     */
    @Override
    public void changeCursor(Cursor cursor) {
        // 调用父类的更改游标方法
        super.changeCursor(cursor);
        // 重新计算笔记的数量
        calcNotesCount();
    }

    /**
     * 计算笔记的数量。
     */
    private void calcNotesCount() {
        // 初始化笔记数量为 0
        mNotesCount = 0;
        // 遍历所有笔记项
        for (int i = 0; i < getCount(); i++) {
            // 获取指定位置的游标数据
            Cursor c = (Cursor) getItem(i);
            // 如果游标数据不为空
            if (c != null) {
                // 如果笔记项的类型为普通笔记
                if (NoteItemData.getNoteType(c) == Notes.TYPE_NOTE) {
                    // 笔记数量加 1
                    mNotesCount++;
                }
            } else {
                // 记录错误日志，提示无效的游标
                Log.e(TAG, "Invalid cursor");
                // 返回
                return;
            }
        }
    }
}
