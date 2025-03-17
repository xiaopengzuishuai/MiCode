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
// 导入 Android 游标类，用于处理数据库查询结果
import android.database.Cursor;
// 导入 Android 文本工具类，提供一些文本处理的实用方法
import android.text.TextUtils;

// 导入应用的联系人数据类
import net.micode.notes.data.Contact;
// 导入应用的笔记数据类
import net.micode.notes.data.Notes;
// 导入应用的笔记列相关类，用于定义数据库表的列名
import net.micode.notes.data.Notes.NoteColumns;
// 导入应用的数据工具类，提供一些数据处理的实用方法
import net.micode.notes.tool.DataUtils;

/**
 * NoteItemData 类用于封装笔记项的数据，从数据库游标中提取笔记的相关信息，
 * 包括笔记的基本信息（如 ID、创建日期、修改日期等）以及位置信息（是否为第一个、最后一个等）。
 */
public class NoteItemData {
    // 定义查询投影，指定要从数据库中查询的列
    static final String [] PROJECTION = new String [] {
        NoteColumns.ID, // 笔记的 ID 列
        NoteColumns.ALERTED_DATE, // 笔记的提醒日期列
        NoteColumns.BG_COLOR_ID, // 笔记的背景颜色 ID 列
        NoteColumns.CREATED_DATE, // 笔记的创建日期列
        NoteColumns.HAS_ATTACHMENT, // 笔记是否有附件列
        NoteColumns.MODIFIED_DATE, // 笔记的修改日期列
        NoteColumns.NOTES_COUNT, // 笔记的数量列
        NoteColumns.PARENT_ID, // 笔记的父 ID 列
        NoteColumns.SNIPPET, // 笔记的摘要列
        NoteColumns.TYPE, // 笔记的类型列
        NoteColumns.WIDGET_ID, // 笔记的小部件 ID 列
        NoteColumns.WIDGET_TYPE, // 笔记的小部件类型列
    };

    // 定义投影中笔记 ID 列的索引，方便后续从游标中获取对应的值
    private static final int ID_COLUMN                    = 0;
    // 定义投影中笔记提醒日期列的索引
    private static final int ALERTED_DATE_COLUMN          = 1;
    // 定义投影中笔记背景颜色 ID 列的索引
    private static final int BG_COLOR_ID_COLUMN           = 2;
    // 定义投影中笔记创建日期列的索引
    private static final int CREATED_DATE_COLUMN          = 3;
    // 定义投影中笔记是否有附件列的索引
    private static final int HAS_ATTACHMENT_COLUMN        = 4;
    // 定义投影中笔记修改日期列的索引
    private static final int MODIFIED_DATE_COLUMN         = 5;
    // 定义投影中笔记数量列的索引
    private static final int NOTES_COUNT_COLUMN           = 6;
    // 定义投影中笔记父 ID 列的索引
    private static final int PARENT_ID_COLUMN             = 7;
    // 定义投影中笔记摘要列的索引
    private static final int SNIPPET_COLUMN               = 8;
    // 定义投影中笔记类型列的索引
    private static final int TYPE_COLUMN                  = 9;
    // 定义投影中笔记小部件 ID 列的索引
    private static final int WIDGET_ID_COLUMN             = 10;
    // 定义投影中笔记小部件类型列的索引
    private static final int WIDGET_TYPE_COLUMN           = 11;

    // 笔记的 ID
    private long mId;
    // 笔记的提醒日期
    private long mAlertDate;
    // 笔记的背景颜色 ID
    private int mBgColorId;
    // 笔记的创建日期
    private long mCreatedDate;
    // 笔记是否有附件
    private boolean mHasAttachment;
    // 笔记的修改日期
    private long mModifiedDate;
    // 笔记的数量
    private int mNotesCount;
    // 笔记的父 ID
    private long mParentId;
    // 笔记的摘要
    private String mSnippet;
    // 笔记的类型
    private int mType;
    // 笔记的小部件 ID
    private int mWidgetId;
    // 笔记的小部件类型
    private int mWidgetType;
    // 联系人姓名
    private String mName;
    // 联系人电话号码
    private String mPhoneNumber;

    // 是否为列表中的最后一项
    private boolean mIsLastItem;
    // 是否为列表中的第一项
    private boolean mIsFirstItem;
    // 是否为列表中唯一的一项
    private boolean mIsOnlyOneItem;
    // 是否为文件夹后跟随的单条笔记
    private boolean mIsOneNoteFollowingFolder;
    // 是否为文件夹后跟随的多条笔记
    private boolean mIsMultiNotesFollowingFolder;

    /**
     * 构造函数，用于从数据库游标中提取笔记项的数据。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param cursor  包含笔记数据的游标
     */
    public NoteItemData(Context context, Cursor cursor) {
        // 从游标中获取笔记的 ID
        mId = cursor.getLong(ID_COLUMN);
        // 从游标中获取笔记的提醒日期
        mAlertDate = cursor.getLong(ALERTED_DATE_COLUMN);
        // 从游标中获取笔记的背景颜色 ID
        mBgColorId = cursor.getInt(BG_COLOR_ID_COLUMN);
        // 从游标中获取笔记的创建日期
        mCreatedDate = cursor.getLong(CREATED_DATE_COLUMN);
        // 从游标中获取笔记是否有附件的信息
        mHasAttachment = (cursor.getInt(HAS_ATTACHMENT_COLUMN) > 0) ? true : false;
        // 从游标中获取笔记的修改日期
        mModifiedDate = cursor.getLong(MODIFIED_DATE_COLUMN);
        // 从游标中获取笔记的数量
        mNotesCount = cursor.getInt(NOTES_COUNT_COLUMN);
        // 从游标中获取笔记的父 ID
        mParentId = cursor.getLong(PARENT_ID_COLUMN);
        // 从游标中获取笔记的摘要
        mSnippet = cursor.getString(SNIPPET_COLUMN);
        // 去除摘要中的特定标签
        mSnippet = mSnippet.replace(NoteEditActivity.TAG_CHECKED, "").replace(
                NoteEditActivity.TAG_UNCHECKED, "");
        // 从游标中获取笔记的类型
        mType = cursor.getInt(TYPE_COLUMN);
        // 从游标中获取笔记的小部件 ID
        mWidgetId = cursor.getInt(WIDGET_ID_COLUMN);
        // 从游标中获取笔记的小部件类型
        mWidgetType = cursor.getInt(WIDGET_TYPE_COLUMN);

        // 初始化电话号码为空字符串
        mPhoneNumber = "";
        // 如果笔记的父 ID 是通话记录文件夹的 ID
        if (mParentId == Notes.ID_CALL_RECORD_FOLDER) {
            // 根据笔记 ID 获取通话号码
            mPhoneNumber = DataUtils.getCallNumberByNoteId(context.getContentResolver(), mId);
            // 如果电话号码不为空
            if (!TextUtils.isEmpty(mPhoneNumber)) {
                // 根据电话号码获取联系人姓名
                mName = Contact.getContact(context, mPhoneNumber);
                // 如果联系人姓名为空
                if (mName == null) {
                    // 使用电话号码作为联系人姓名
                    mName = mPhoneNumber;
                }
            }
        }

        // 如果联系人姓名为空
        if (mName == null) {
            // 初始化联系人姓名为空字符串
            mName = "";
        }
        // 检查笔记在列表中的位置
        checkPostion(cursor);
    }

    /**
     * 检查笔记在列表中的位置，确定是否为第一个、最后一个、唯一的一个，
     * 以及是否为文件夹后跟随的单条或多条笔记。
     *
     * @param cursor 包含笔记数据的游标
     */
    private void checkPostion(Cursor cursor) {
        // 判断是否为列表中的最后一项
        mIsLastItem = cursor.isLast() ? true : false;
        // 判断是否为列表中的第一项
        mIsFirstItem = cursor.isFirst() ? true : false;
        // 判断是否为列表中唯一的一项
        mIsOnlyOneItem = (cursor.getCount() == 1);
        // 初始化是否为文件夹后跟随的多条笔记为 false
        mIsMultiNotesFollowingFolder = false;
        // 初始化是否为文件夹后跟随的单条笔记为 false
        mIsOneNoteFollowingFolder = false;

        // 如果笔记类型为普通笔记且不是第一项
        if (mType == Notes.TYPE_NOTE && !mIsFirstItem) {
            // 获取当前游标位置
            int position = cursor.getPosition();
            // 移动游标到上一项
            if (cursor.moveToPrevious()) {
                // 如果上一项是文件夹或系统类型
                if (cursor.getInt(TYPE_COLUMN) == Notes.TYPE_FOLDER
                        || cursor.getInt(TYPE_COLUMN) == Notes.TYPE_SYSTEM) {
                    // 如果列表中还有后续项
                    if (cursor.getCount() > (position + 1)) {
                        // 标记为文件夹后跟随的多条笔记
                        mIsMultiNotesFollowingFolder = true;
                    } else {
                        // 标记为文件夹后跟随的单条笔记
                        mIsOneNoteFollowingFolder = true;
                    }
                }
                // 移动游标回到原来的位置
                if (!cursor.moveToNext()) {
                    // 如果无法移动回原来的位置，抛出异常
                    throw new IllegalStateException("cursor move to previous but can't move back");
                }
            }
        }
    }

    /**
     * 判断是否为文件夹后跟随的单条笔记。
     *
     * @return 如果是则返回 true，否则返回 false
     */
    public boolean isOneFollowingFolder() {
        return mIsOneNoteFollowingFolder;
    }

    /**
     * 判断是否为文件夹后跟随的多条笔记。
     *
     * @return 如果是则返回 true，否则返回 false
     */
    public boolean isMultiFollowingFolder() {
        return mIsMultiNotesFollowingFolder;
    }

    /**
     * 判断是否为列表中的最后一项。
     *
     * @return 如果是则返回 true，否则返回 false
     */
    public boolean isLast() {
        return mIsLastItem;
    }

    /**
     * 获取通话记录的联系人姓名。
     *
     * @return 联系人姓名
     */
    public String getCallName() {
        return mName;
    }

    /**
     * 判断是否为列表中的第一项。
     *
     * @return 如果是则返回 true，否则返回 false
     */
    public boolean isFirst() {
        return mIsFirstItem;
    }

    /**
     * 判断是否为列表中唯一的一项。
     *
     * @return 如果是则返回 true，否则返回 false
     */
    public boolean isSingle() {
        return mIsOnlyOneItem;
    }

    /**
     * 获取笔记的 ID。
     *
     * @return 笔记的 ID
     */
    public long getId() {
        return mId;
    }

    /**
     * 获取笔记的提醒日期。
     *
     * @return 笔记的提醒日期
     */
    public long getAlertDate() {
        return mAlertDate;
    }

    /**
     * 获取笔记的创建日期。
     *
     * @return 笔记的创建日期
     */
    public long getCreatedDate() {
        return mCreatedDate;
    }

    /**
     * 判断笔记是否有附件。
     *
     * @return 如果有附件则返回 true，否则返回 false
     */
    public boolean hasAttachment() {
        return mHasAttachment;
    }

    /**
     * 获取笔记的修改日期。
     *
     * @return 笔记的修改日期
     */
    public long getModifiedDate() {
        return mModifiedDate;
    }

    /**
     * 获取笔记的背景颜色 ID。
     *
     * @return 笔记的背景颜色 ID
     */
    public int getBgColorId() {
        return mBgColorId;
    }

    /**
     * 获取笔记的父 ID。
     *
     * @return 笔记的父 ID
     */
    public long getParentId() {
        return mParentId;
    }

    /**
     * 获取笔记的数量。
     *
     * @return 笔记的数量
     */
    public int getNotesCount() {
        return mNotesCount;
    }

    /**
     * 获取笔记所在文件夹的 ID。
     *
     * @return 文件夹的 ID
     */
    public long getFolderId () {
        return mParentId;
    }

    /**
     * 获取笔记的类型。
     *
     * @return 笔记的类型
     */
    public int getType() {
        return mType;
    }

    /**
     * 获取笔记的小部件类型。
     *
     * @return 笔记的小部件类型
     */
    public int getWidgetType() {
        return mWidgetType;
    }

    /**
     * 获取笔记的小部件 ID。
     *
     * @return 笔记的小部件 ID
     */
    public int getWidgetId() {
        return mWidgetId;
    }

    /**
     * 获取笔记的摘要。
     *
     * @return 笔记的摘要
     */
    public String getSnippet() {
        return mSnippet;
    }

    /**
     * 判断笔记是否有提醒。
     *
     * @return 如果有提醒则返回 true，否则返回 false
     */
    public boolean hasAlert() {
        return (mAlertDate > 0);
    }

    /**
     * 判断笔记是否为通话记录。
     *
     * @return 如果是通话记录则返回 true，否则返回 false
     */
    public boolean isCallRecord() {
        return (mParentId == Notes.ID_CALL_RECORD_FOLDER && !TextUtils.isEmpty(mPhoneNumber));
    }

    /**
     * 从游标中获取笔记的类型。
     *
     * @param cursor 包含笔记数据的游标
     * @return 笔记的类型
     */
    public static int getNoteType(Cursor cursor) {
        return cursor.getInt(TYPE_COLUMN);
    }
}
