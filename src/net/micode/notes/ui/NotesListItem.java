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

// 声明该类所在的包名
package net.micode.notes.ui;

// 导入 Android 上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 日期格式化工具类，用于格式化日期和时间
import android.text.format.DateUtils;
// 导入 Android 视图类，是所有 UI 组件的基类
import android.view.View;
// 导入 Android 复选框类，用于提供复选功能
import android.widget.CheckBox;
// 导入 Android 图像视图类，用于显示图像
import android.widget.ImageView;
// 导入 Android 线性布局类，用于按照线性方向排列子视图
import android.widget.LinearLayout;
// 导入 Android 文本视图类，用于显示文本
import android.widget.TextView;

// 导入应用的资源类，用于访问应用的资源
import net.micode.notes.R;
// 导入应用的笔记数据类
import net.micode.notes.data.Notes;
// 导入应用的数据工具类，提供一些数据处理的实用方法
import net.micode.notes.tool.DataUtils;
// 导入应用的资源解析器类，用于解析笔记项的背景资源
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

/**
 * NotesListItem 类继承自 LinearLayout，用于表示笔记列表中的单个项。
 * 该类负责初始化和绑定笔记项的视图组件，包括标题、时间、提醒图标等，
 * 并根据笔记项的类型和状态设置不同的背景资源。
 */
public class NotesListItem extends LinearLayout {
    // 提醒图标视图
    private ImageView mAlert;
    // 标题文本视图
    private TextView mTitle;
    // 时间文本视图
    private TextView mTime;
    // 通话记录联系人姓名文本视图
    private TextView mCallName;
    // 笔记项数据对象
    private NoteItemData mItemData;
    // 复选框视图，用于选择笔记项
    private CheckBox mCheckBox;

    /**
     * 构造函数，用于初始化 NotesListItem 视图。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     */
    public NotesListItem(Context context) {
        // 调用父类的构造函数
        super(context);
        // 填充布局文件 note_item.xml 到当前视图
        inflate(context, R.layout.note_item, this);
        // 查找提醒图标视图
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        // 查找标题文本视图
        mTitle = (TextView) findViewById(R.id.tv_title);
        // 查找时间文本视图
        mTime = (TextView) findViewById(R.id.tv_time);
        // 查找通话记录联系人姓名文本视图
        mCallName = (TextView) findViewById(R.id.tv_name);
        // 查找复选框视图
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    /**
     * 绑定笔记项数据到视图。
     *
     * @param context    上下文对象，用于获取系统服务和资源
     * @param data       笔记项数据对象
     * @param choiceMode 是否处于选择模式
     * @param checked    笔记项是否被选中
     */
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        // 如果处于选择模式且笔记类型为普通笔记
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            // 显示复选框
            mCheckBox.setVisibility(View.VISIBLE);
            // 设置复选框的选中状态
            mCheckBox.setChecked(checked);
        } else {
            // 隐藏复选框
            mCheckBox.setVisibility(View.GONE);
        }

        // 保存笔记项数据
        mItemData = data;
        // 如果笔记 ID 是通话记录文件夹的 ID
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 隐藏通话记录联系人姓名文本视图
            mCallName.setVisibility(View.GONE);
            // 显示提醒图标视图
            mAlert.setVisibility(View.VISIBLE);
            // 设置标题文本的样式
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);
            // 设置标题文本，显示通话记录文件夹名称和文件数量
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            // 设置提醒图标为通话记录图标
            mAlert.setImageResource(R.drawable.call_record);
        // 如果笔记的父 ID 是通话记录文件夹的 ID
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 显示通话记录联系人姓名文本视图
            mCallName.setVisibility(View.VISIBLE);
            // 设置通话记录联系人姓名文本
            mCallName.setText(data.getCallName());
            // 设置标题文本的样式
            mTitle.setTextAppearance(context,R.style.TextAppearanceSecondaryItem);
            // 设置标题文本，显示格式化后的笔记摘要
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
            // 如果笔记有提醒
            if (data.hasAlert()) {
                // 设置提醒图标为时钟图标
                mAlert.setImageResource(R.drawable.clock);
                // 显示提醒图标视图
                mAlert.setVisibility(View.VISIBLE);
            } else {
                // 隐藏提醒图标视图
                mAlert.setVisibility(View.GONE);
            }
        } else {
            // 隐藏通话记录联系人姓名文本视图
            mCallName.setVisibility(View.GONE);
            // 设置标题文本的样式
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);

            // 如果笔记类型为文件夹
            if (data.getType() == Notes.TYPE_FOLDER) {
                // 设置标题文本，显示文件夹名称和文件数量
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));
                // 隐藏提醒图标视图
                mAlert.setVisibility(View.GONE);
            } else {
                // 设置标题文本，显示格式化后的笔记摘要
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
                // 如果笔记有提醒
                if (data.hasAlert()) {
                    // 设置提醒图标为时钟图标
                    mAlert.setImageResource(R.drawable.clock);
                    // 显示提醒图标视图
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    // 隐藏提醒图标视图
                    mAlert.setVisibility(View.GONE);
                }
            }
        }
        // 设置时间文本，显示笔记的修改时间
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        // 设置背景资源
        setBackground(data);
    }

    /**
     * 根据笔记项数据设置背景资源。
     *
     * @param data 笔记项数据对象
     */
    private void setBackground(NoteItemData data) {
        // 获取笔记项的背景颜色 ID
        int id = data.getBgColorId();
        // 如果笔记类型为普通笔记
        if (data.getType() == Notes.TYPE_NOTE) {
            // 如果笔记是列表中唯一的一项或文件夹后跟随的单条笔记
            if (data.isSingle() || data.isOneFollowingFolder()) {
                // 设置背景资源为单条笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            // 如果笔记是列表中的最后一项
            } else if (data.isLast()) {
                // 设置背景资源为最后一条笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            // 如果笔记是列表中的第一项或文件夹后跟随的多条笔记
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                // 设置背景资源为第一条笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                // 设置背景资源为普通笔记的背景资源
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        } else {
            // 设置背景资源为文件夹的背景资源
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }

    /**
     * 获取笔记项数据对象。
     *
     * @return 笔记项数据对象
     */
    public NoteItemData getItemData() {
        return mItemData;
    }
}
