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
 * 该类是用于编辑笔记的Activity，继承自Activity类，实现了OnClickListener、NoteSettingChangedListener和OnTextViewChangeListener接口。
 * 它处理笔记的创建、编辑、保存、删除、共享等操作，同时支持设置提醒、背景颜色、字体大小等功能。
 */

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.model.WorkingNote.NoteSettingChangedListener;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 该类用于编辑笔记的活动，继承自 Activity 类，实现了 OnClickListener、NoteSettingChangedListener 和 OnTextViewChangeListener 接口。
 */
public class NoteEditActivity extends Activity implements OnClickListener,
        NoteSettingChangedListener, OnTextViewChangeListener {
    /**
     * 头部视图的 ViewHolder 类，用于存储头部视图的控件引用。
     */
    private class HeadViewHolder {
        // 显示笔记修改日期的 TextView
        public TextView tvModified;

        // 显示提醒图标的 ImageView
        public ImageView ivAlertIcon;

        // 显示提醒日期的 TextView
        public TextView tvAlertDate;

        // 设置背景颜色的 ImageView 按钮
        public ImageView ibSetBgColor;
    }

    // 背景选择按钮与背景资源 ID 的映射
    private static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();
    static {
        // 黄色背景按钮对应黄色背景资源 ID
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, ResourceParser.YELLOW);
        // 红色背景按钮对应红色背景资源 ID
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, ResourceParser.RED);
        // 蓝色背景按钮对应蓝色背景资源 ID
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, ResourceParser.BLUE);
        // 绿色背景按钮对应绿色背景资源 ID
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, ResourceParser.GREEN);
        // 白色背景按钮对应白色背景资源 ID
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, ResourceParser.WHITE);
    }

    // 背景资源 ID 与背景选择选中状态视图 ID 的映射
    private static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        // 黄色背景资源 ID 对应黄色背景选中状态视图 ID
        sBgSelectorSelectionMap.put(ResourceParser.YELLOW, R.id.iv_bg_yellow_select);
        // 红色背景资源 ID 对应红色背景选中状态视图 ID
        sBgSelectorSelectionMap.put(ResourceParser.RED, R.id.iv_bg_red_select);
        // 蓝色背景资源 ID 对应蓝色背景选中状态视图 ID
        sBgSelectorSelectionMap.put(ResourceParser.BLUE, R.id.iv_bg_blue_select);
        // 绿色背景资源 ID 对应绿色背景选中状态视图 ID
        sBgSelectorSelectionMap.put(ResourceParser.GREEN, R.id.iv_bg_green_select);
        // 白色背景资源 ID 对应白色背景选中状态视图 ID
        sBgSelectorSelectionMap.put(ResourceParser.WHITE, R.id.iv_bg_white_select);
    }

    // 字体大小按钮与字体大小资源 ID 的映射
    private static final Map<Integer, Integer> sFontSizeBtnsMap = new HashMap<Integer, Integer>();
    static {
        // 大字体按钮对应大字体资源 ID
        sFontSizeBtnsMap.put(R.id.ll_font_large, ResourceParser.TEXT_LARGE);
        // 小字体按钮对应小字体资源 ID
        sFontSizeBtnsMap.put(R.id.ll_font_small, ResourceParser.TEXT_SMALL);
        // 正常字体按钮对应正常字体资源 ID
        sFontSizeBtnsMap.put(R.id.ll_font_normal, ResourceParser.TEXT_MEDIUM);
        // 超大字体按钮对应超大字体资源 ID
        sFontSizeBtnsMap.put(R.id.ll_font_super, ResourceParser.TEXT_SUPER);
    }

    // 字体大小资源 ID 与字体选择选中状态视图 ID 的映射
    private static final Map<Integer, Integer> sFontSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        // 大字体资源 ID 对应大字体选中状态视图 ID
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_LARGE, R.id.iv_large_select);
        // 小字体资源 ID 对应小字体选中状态视图 ID
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SMALL, R.id.iv_small_select);
        // 正常字体资源 ID 对应正常字体选中状态视图 ID
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_MEDIUM, R.id.iv_medium_select);
        // 超大字体资源 ID 对应超大字体选中状态视图 ID
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SUPER, R.id.iv_super_select);
    }

    // 日志标签
    private static final String TAG = "NoteEditActivity";

    // 头部视图的 ViewHolder 实例
    private HeadViewHolder mNoteHeaderHolder;

    // 头部视图面板
    private View mHeadViewPanel;

    // 背景颜色选择器视图
    private View mNoteBgColorSelector;

    // 字体大小选择器视图
    private View mFontSizeSelector;

    // 笔记编辑器
    private EditText mNoteEditor;

    // 笔记编辑器面板
    private View mNoteEditorPanel;

    // 正在编辑的笔记对象
    private WorkingNote mWorkingNote;

    // 共享偏好设置
    private SharedPreferences mSharedPrefs;
    // 当前字体大小的资源 ID
    private int mFontSizeId;

    // 共享偏好设置中存储字体大小的键
    private static final String PREFERENCE_FONT_SIZE = "pref_font_size";

    // 快捷图标标题的最大长度
    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10;

    // 已勾选标记
    public static final String TAG_CHECKED = String.valueOf('\u221A');
    // 未勾选标记
    public static final String TAG_UNCHECKED = String.valueOf('\u25A1');

    // 编辑文本列表的 LinearLayout
    private LinearLayout mEditTextList;

    // 用户查询的关键词
    private String mUserQuery;
    // 用于匹配用户查询关键词的正则表达式模式
    private Pattern mPattern;

    /**
     * 活动创建时调用的方法，用于初始化界面和状态。
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置活动的布局
        this.setContentView(R.layout.note_edit);

        // 如果 savedInstanceState 为空且初始化活动状态失败，则结束活动
        if (savedInstanceState == null && !initActivityState(getIntent())) {
            finish();
            return;
        }
        // 初始化资源
        initResources();
    }

    /**
     * 恢复活动状态时调用的方法，用于恢复之前保存的状态。
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 如果 savedInstanceState 不为空且包含 Intent.EXTRA_UID，则恢复活动状态
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, savedInstanceState.getLong(Intent.EXTRA_UID));
            // 如果初始化活动状态失败，则结束活动
            if (!initActivityState(intent)) {
                finish();
                return;
            }
            // 记录恢复活动状态的日志
            Log.d(TAG, "Restoring from killed activity");
        }
    }

    /**
     * 初始化活动状态的方法，根据传入的 Intent 初始化正在编辑的笔记。
     * @param intent 传入的 Intent
     * @return 如果初始化成功返回 true，否则返回 false
     */
    private boolean initActivityState(Intent intent) {
        /**
         * 如果用户指定了 {@link Intent#ACTION_VIEW} 但未提供笔记 ID，则跳转到 NotesListActivity
         */
        mWorkingNote = null;
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) {
            // 获取笔记 ID
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
            mUserQuery = "";

            /**
             * 从搜索结果启动
             */
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
                noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
                mUserQuery = intent.getStringExtra(SearchManager.USER_QUERY);
            }

            // 检查笔记是否在数据库中可见
            if (!DataUtils.visibleInNoteDatabase(getContentResolver(), noteId, Notes.TYPE_NOTE)) {
                // 如果笔记不可见，跳转到 NotesListActivity 并显示错误提示
                Intent jump = new Intent(this, NotesListActivity.class);
                startActivity(jump);
                showToast(R.string.error_note_not_exist);
                finish();
                return false;
            } else {
                // 加载笔记
                mWorkingNote = WorkingNote.load(this, noteId);
                if (mWorkingNote == null) {
                    // 如果加载笔记失败，记录错误日志并结束活动
                    Log.e(TAG, "load note failed with note id" + noteId);
                    finish();
                    return false;
                }
            }
            // 设置软键盘的显示模式
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else if(TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) {
            // 新建笔记
            long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0);
            int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                    Notes.TYPE_WIDGET_INVALIDE);
            int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                    ResourceParser.getDefaultBgId(this));

            // 解析通话记录笔记
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);
            if (callDate != 0 && phoneNumber != null) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    // 如果通话记录号码为空，记录警告日志
                    Log.w(TAG, "The call record number is null");
                }
                long noteId = 0;
                if ((noteId = DataUtils.getNoteIdByPhoneNumberAndCallDate(getContentResolver(),
                        phoneNumber, callDate)) > 0) {
                    // 如果存在对应的通话记录笔记，加载该笔记
                    mWorkingNote = WorkingNote.load(this, noteId);
                    if (mWorkingNote == null) {
                        // 如果加载通话记录笔记失败，记录错误日志并结束活动
                        Log.e(TAG, "load call note failed with note id" + noteId);
                        finish();
                        return false;
                    }
                } else {
                    // 如果不存在对应的通话记录笔记，创建一个新的笔记并转换为通话记录笔记
                    mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId,
                            widgetType, bgResId);
                    mWorkingNote.convertToCallNote(phoneNumber, callDate);
                }
            } else {
                // 创建一个新的笔记
                mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType,
                        bgResId);
            }

            // 设置软键盘的显示模式
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            // 如果 Intent 未指定操作，记录错误日志并结束活动
            Log.e(TAG, "Intent not specified action, should not support");
            finish();
            return false;
        }
        // 设置笔记设置状态改变的监听器
        mWorkingNote.setOnSettingStatusChangedListener(this);
        return true;
    }

    /**
     * 活动恢复时调用的方法，用于初始化笔记界面。
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 初始化笔记界面
        initNoteScreen();
    }

    /**
     * 初始化笔记界面的方法，设置笔记编辑器的文本外观、显示模式、背景颜色等。
     */
    private void initNoteScreen() {
        // 设置笔记编辑器的文本外观
        mNoteEditor.setTextAppearance(this, TextAppearanceResources
                .getTexAppearanceResource(mFontSizeId));
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            // 如果是列表模式，切换到列表模式并显示笔记内容
            switchToListMode(mWorkingNote.getContent());
        } else {
            // 如果不是列表模式，设置笔记编辑器的文本并高亮显示查询结果
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            // 将光标移动到文本末尾
            mNoteEditor.setSelection(mNoteEditor.getText().length());
        }
        // 隐藏所有背景选择选中状态视图
        for (Integer id : sBgSelectorSelectionMap.keySet()) {
            findViewById(sBgSelectorSelectionMap.get(id)).setVisibility(View.GONE);
        }
        // 设置头部视图面板的背景资源
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
        // 设置笔记编辑器面板的背景资源
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());

        // 设置笔记修改日期的显示文本
        mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                mWorkingNote.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));

        /**
         * TODO: Add the menu for setting alert. Currently disable it because the DateTimePicker
         * is not ready
         */
        // 显示提醒头部信息
        showAlertHeader();
    }

    /**
     * 显示提醒头部信息的方法，根据笔记的提醒状态显示提醒图标和日期。
     */
    private void showAlertHeader() {
        if (mWorkingNote.hasClockAlert()) {
            long time = System.currentTimeMillis();
            if (time > mWorkingNote.getAlertDate()) {
                // 如果提醒时间已过，显示提醒已过期
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired);
            } else {
                // 如果提醒时间未过，显示相对时间
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        mWorkingNote.getAlertDate(), time, DateUtils.MINUTE_IN_MILLIS));
            }
            // 显示提醒日期和图标
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE);
        } else {
            // 如果没有提醒，隐藏提醒日期和图标
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE);
        };
    }

    /**
     * 当活动接收到新的 Intent 时调用的方法，重新初始化活动状态。
     * @param intent 新的 Intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 初始化活动状态
        initActivityState(intent);
    }

    /**
     * 保存活动状态时调用的方法，保存正在编辑的笔记 ID。
     * @param outState 保存的实例状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /**
         * 对于没有笔记 ID 的新笔记，首先保存它以生成一个 ID。如果正在编辑的笔记不值得保存，则没有 ID，相当于创建新笔记
         */
        if (!mWorkingNote.existInDatabase()) {
            // 保存笔记
            saveNote();
        }
        // 保存笔记 ID
        outState.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        // 记录保存笔记 ID 的日志
        Log.d(TAG, "Save working note id: " + mWorkingNote.getNoteId() + " onSaveInstanceState");
    }

    /**
     * 分发触摸事件的方法，用于处理背景颜色选择器和字体大小选择器的显示和隐藏。
     * @param ev 触摸事件
     * @return 如果事件被处理返回 true，否则返回父类的处理结果
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mNoteBgColorSelector, ev)) {
            // 如果背景颜色选择器可见且触摸事件不在选择器范围内，隐藏选择器
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }

        if (mFontSizeSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mFontSizeSelector, ev)) {
            // 如果字体大小选择器可见且触摸事件不在选择器范围内，隐藏选择器
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判断触摸事件是否在指定视图范围内的方法。
     * @param view 指定的视图
     * @param ev 触摸事件
     * @return 如果触摸事件在视图范围内返回 true，否则返回 false
     */
    private boolean inRangeOfView(View view, MotionEvent ev) {
        int []location = new int[2];
        // 获取视图在屏幕上的位置
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        if (ev.getX() < x
                || ev.getX() > (x + view.getWidth())
                || ev.getY() < y
                || ev.getY() > (y + view.getHeight())) {
            // 如果触摸事件不在视图范围内，返回 false
            return false;
        }
        return true;
    }

    /**
     * 初始化资源的方法，初始化界面控件和共享偏好设置。
     */
    private void initResources() {
        // 获取头部视图面板
        mHeadViewPanel = findViewById(R.id.note_title);
        // 初始化头部视图的 ViewHolder
        mNoteHeaderHolder = new HeadViewHolder();
        // 获取显示笔记修改日期的 TextView
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date);
        // 获取显示提醒图标的 ImageView
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        // 获取显示提醒日期的 TextView
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date);
        // 获取设置背景颜色的 ImageView 按钮
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color);
        // 设置背景颜色按钮的点击监听器
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this);
        // 获取笔记编辑器
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view);
        // 获取笔记编辑器面板
        mNoteEditorPanel = findViewById(R.id.sv_note_edit);
        // 获取背景颜色选择器视图
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector);
        // 为背景选择按钮设置点击监听器
        for (int id : sBgSelectorBtnsMap.keySet()) {
            ImageView iv = (ImageView) findViewById(id);
            iv.setOnClickListener(this);
        }

        // 获取字体大小选择器视图
        mFontSizeSelector = findViewById(R.id.font_size_selector);
        // 为字体大小选择按钮设置点击监听器
        for (int id : sFontSizeBtnsMap.keySet()) {
            View view = findViewById(id);
            view.setOnClickListener(this);
        };
        // 获取共享偏好设置
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // 获取当前字体大小的资源 ID
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE);
        /**
         * HACKME: Fix bug of store the resource id in shared preference.
         * The id may larger than the length of resources, in this case,
         * return the {@link ResourceParser#BG_DEFAULT_FONT_SIZE}
         */
        if(mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            // 如果字体大小资源 ID 超出范围，使用默认字体大小
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE;
        }
        // 获取编辑文本列表的 LinearLayout
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list);
    }

    /**
     * 活动暂停时调用的方法，保存笔记并清除设置状态。
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(saveNote()) {
            // 如果笔记保存成功，记录保存日志
            Log.d(TAG, "Note data was saved with length:" + mWorkingNote.getContent().length());
        }
        // 清除设置状态
        clearSettingState();
    }

    /**
     * 更新小部件的方法，根据笔记的小部件类型发送广播更新小部件。
     */
    private void updateWidget() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_2X) {
            // 如果是 2x 小部件，设置广播的目标类为 NoteWidgetProvider_2x
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_4X) {
            // 如果是 4x 小部件，设置广播的目标类为 NoteWidgetProvider_4x
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            // 如果是不支持的小部件类型，记录错误日志并返回
            Log.e(TAG, "Unspported widget type");
            return;
        }

        // 设置广播的小部件 ID
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
            mWorkingNote.getWidgetId()
        });

        // 发送广播更新小部件
        sendBroadcast(intent);
        // 设置活动结果为成功
        setResult(RESULT_OK, intent);
    }

    /**
     * 处理点击事件的方法，根据点击的视图 ID 执行相应的操作。
     * @param v 被点击的视图
     */
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_set_bg_color) {
            // 如果点击的是设置背景颜色的按钮，显示背景颜色选择器并高亮显示当前选中的背景颜色
            mNoteBgColorSelector.setVisibility(View.VISIBLE);
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                    View.VISIBLE);
        } else if (sBgSelectorBtnsMap.containsKey(id)) {
            // 如果点击的是背景选择按钮，隐藏当前选中的背景颜色，设置新的背景颜色并隐藏背景颜色选择器
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                    View.GONE);
            mWorkingNote.setBgColorId(sBgSelectorBtnsMap.get(id));
            mNoteBgColorSelector.setVisibility(View.GONE);
        } else if (sFontSizeBtnsMap.containsKey(id)) {
            // 如果点击的是字体大小选择按钮，隐藏当前选中的字体大小，设置新的字体大小并保存到共享偏好设置中，更新字体大小选择器的显示状态，根据笔记的显示模式更新笔记编辑器的文本外观
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.GONE);
            mFontSizeId = sFontSizeBtnsMap.get(id);
            mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).commit();
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
            if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
                // 如果是列表模式，获取工作文本并切换到列表模式
                getWorkingText();
                switchToListMode(mWorkingNote.getContent());
            } else {
                // 如果不是列表模式，设置笔记编辑器的文本外观
                mNoteEditor.setTextAppearance(this,
                        TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
            }
            // 隐藏字体大小选择器
            mFontSizeSelector.setVisibility(View.GONE);
        }
    }

    /**
     * 处理返回按钮事件的方法，清除设置状态并保存笔记。
     */
    @Override
    public void onBackPressed() {
        if(clearSettingState()) {
            // 如果清除设置状态成功，返回
            return;
        }

        // 保存笔记
        saveNote();
        // 调用父类的返回按钮处理方法
        super.onBackPressed();
    }

    /**
     * 清除设置状态的方法，隐藏背景颜色选择器和字体大小选择器。
     * @return 如果设置状态被清除返回 true，否则返回 false
     */
    private boolean clearSettingState() {
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
            // 如果背景颜色选择器可见，隐藏选择器并返回 true
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        } else if (mFontSizeSelector.getVisibility() == View.VISIBLE) {
            // 如果字体大小选择器可见，隐藏选择器并返回 true
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    /**
     * 背景颜色改变时调用的方法，更新背景颜色选择器的显示状态和笔记编辑器面板的背景资源。
     */
    public void onBackgroundColorChanged() {
        // 高亮显示当前选中的背景颜色
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                View.VISIBLE);
        // 设置笔记编辑器面板的背景资源
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());
        // 设置头部视图面板的背景资源
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
    }

    /**
     * 准备选项菜单时调用的方法，根据笔记的状态更新菜单。
     * @param menu 选项菜单
     * @return 如果菜单准备成功返回 true，否则返回父类的处理结果
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isFinishing()) {
            // 如果活动正在结束，返回 true
            return true;
        }
        // 清除设置状态
        clearSettingState();
        // 清除菜单
        menu.clear();
        if (mWorkingNote.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 如果笔记在通话记录文件夹中，加载通话记录笔记编辑菜单
            getMenuInflater().inflate(R.menu.call_note_edit, menu);
        } else {
            // 如果笔记不在通话记录文件夹中，加载普通笔记编辑菜单
            getMenuInflater().inflate(R.menu.note_edit, menu);
        }
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            // 如果是列表模式，设置列表模式菜单项的标题为普通模式
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
        } else {
            // 如果不是列表模式，设置列表模式菜单项的标题为列表模式
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
        }
        if (mWorkingNote.hasClockAlert()) {
            // 如果笔记有提醒，隐藏设置提醒菜单项
            menu.findItem(R.id.menu_alert).setVisible(false);
        } else {
            // 如果笔记没有提醒，隐藏删除提醒菜单项
            menu.findItem(R.id.menu_delete_remind).setVisible(false);
        }
        return true;
    }

    /**
     * 处理选项菜单项点击事件的方法，根据点击的菜单项 ID 执行相应的操作。
     * @param item 被点击的菜单项
     * @return 如果菜单项点击事件被处理返回 true，否则返回父类的处理结果
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_note:
                // 如果点击的是新建笔记菜单项，创建新笔记
                createNewNote();
                break;
            case R.id.menu_delete:
                // 如果点击的是删除笔记菜单项，显示确认删除对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_note));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // 如果确认删除，删除当前笔记并结束活动
                                deleteCurrentNote();
                                finish();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case R.id.menu_font_size:
                // 如果点击的是字体大小菜单项，显示字体大小选择器并高亮显示当前选中的字体大小
                mFontSizeSelector.setVisibility(View.VISIBLE);
                findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
                break;
            case R.id.menu_list_mode:
                // 如果点击的是列表模式菜单项，切换笔记的显示模式
                mWorkingNote.setCheckListMode(mWorkingNote.getCheckListMode() == 0 ?
                        TextNote.MODE_CHECK_LIST : 0);
                break;
            case R.id.menu_share:
                // 如果点击的是分享菜单项，获取工作文本并分享笔记
                getWorkingText();
                sendTo(this, mWorkingNote.getContent());
                break;
            case R.id.menu_send_to_desktop:
                // 如果点击的是发送到桌面菜单项，将笔记发送到桌面
                sendToDesktop();
                break;
            case R.id.menu_alert:
                // 如果点击的是设置提醒菜单项，设置提醒
                setReminder();
                break;
            case R.id.menu_delete_remind:
                // 如果点击的是删除提醒菜单项，删除笔记的提醒
                mWorkingNote.setAlertDate(0, false);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 设置提醒的方法，显示日期时间选择对话框，用户选择日期时间后设置笔记的提醒时间。
     */
    private void setReminder() {
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                // 用户选择日期时间后，设置笔记的提醒时间
                mWorkingNote.setAlertDate(date	, true);
            }
        });
        // 显示日期时间选择对话框
        d.show();
    }

    /**
     * 分享笔记的方法，将笔记内容分享到支持 {@link Intent#ACTION_SEND} 动作和 {@text/plain} 类型的应用。
     * @param context 上下文
     * @param info 笔记内容
     */
    private void sendTo(Context context, String info) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, info);
        intent.setType("text/plain");
        // 启动分享意图
        context.startActivity(intent);
    }

    /**
     * 创建新笔记的方法，首先保存当前正在编辑的笔记，然后启动新的笔记编辑活动。
     */
    private void createNewNote() {
        // 保存当前正在编辑的笔记
        saveNote();

        // 结束当前活动
        finish();
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mWorkingNote.getFolderId());
        // 启动新的笔记编辑活动
        startActivity(intent);
    }

    /**
     * 删除当前笔记的方法，根据同步模式将笔记删除或移动到回收站。
     */
    private void deleteCurrentNote() {
        if (mWorkingNote.existInDatabase()) {
            HashSet<Long> ids = new HashSet<Long>();
            long id = mWorkingNote.getNoteId();
            if (id != Notes.ID_ROOT_FOLDER) {
                // 如果笔记 ID 不是根文件夹 ID，将笔记 ID 添加到要删除的 ID 集合中
                ids.add(id);
            } else {
                // 如果笔记 ID 是根文件夹 ID，记录错误日志
                Log.d(TAG, "Wrong note id, should not happen");
            }
            if (!isSyncMode()) {
                // 如果不是同步模式，直接删除笔记
                if (!DataUtils.batchDeleteNotes(getContentResolver(), ids)) {
                    // 如果删除笔记失败，记录错误日志
                    Log.e(TAG, "Delete Note error");
                }
            } else {
                // 如果是同步模式，将笔记移动到回收站
                if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) {
                    // 如果移动笔记到回收站失败，记录错误日志
                    Log.e(TAG, "Move notes to trash folder error, should not happens");
                }
            }
        }
        // 标记笔记为已删除
        mWorkingNote.markDeleted(true);
    }

    /**
     * 判断是否为同步模式的方法，根据同步账户名是否为空判断。
     * @return 如果是同步模式返回 true，否则返回 false
     */
    private boolean isSyncMode() {
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    /**
     * 时钟提醒改变时调用的方法，根据提醒设置更新闹钟。
     * @param date 提醒日期
     * @param set 是否设置提醒
     */
    public void onClockAlertChanged(long date, boolean set) {
        /**
         * 用户可以为未保存的笔记设置时钟提醒，因此在设置提醒时钟之前，应先保存笔记
         */
        if (!mWorkingNote.existInDatabase()) {
            // 如果笔记未保存，保存笔记
            saveNote();
        }
        if (mWorkingNote.getNoteId() > 0) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mWorkingNote.getNoteId()));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            AlarmManager alarmManager = ((AlarmManager) getSystemService(ALARM_SERVICE));
            // 显示提醒头部信息
            showAlertHeader();
            if(!set) {
                // 如果取消提醒，取消闹钟
                alarmManager.cancel(pendingIntent);
            } else {
                // 如果设置提醒，设置闹钟
                alarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
            }
        } else {
            /**
             * 存在用户未输入任何内容（笔记不值得保存）的情况，此时没有笔记 ID，提醒用户应输入内容
             */
            Log.e(TAG, "Clock alert setting error");
            showToast(R.string.error_note_empty_for_clock);
        }
    }

    /**
     * 小部件改变时调用的方法，更新小部件。
     */
    public void onWidgetChanged() {
        // 更新小部件
        updateWidget();
    }

    /**
     * 编辑文本删除时调用的方法，处理编辑文本的删除操作。
     * @param index 删除的编辑文本的索引
     * @param text 删除的编辑文本的内容
     */
    public void onEditTextDelete(int index, String text) {
        int childCount = mEditTextList.getChildCount();
        if (childCount == 1) {
            // 如果编辑文本列表中只有一个子项，不进行删除操作
            return;
        }

        for (int i = index + 1; i < childCount; i++) {
            // 更新后续编辑文本的索引
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i - 1);
        }

        // 从编辑文本列表中移除指定索引的子项
        mEditTextList.removeViewAt(index);
        NoteEditText edit = null;
        if(index == 0) {
            // 如果删除的是第一个子项，获取第一个子项的编辑文本
            edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(
                    R.id.et_edit_text);
        } else {
            // 如果删除的不是第一个子项，获取前一个子项的编辑文本
            edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(
                    R.id.et_edit_text);
        }
        int length = edit.length();
        // 将删除的文本追加到前一个编辑文本的末尾
        edit.append(text);
        // 请求焦点
        edit.requestFocus();
        // 将光标移动到追加文本的末尾
        edit.setSelection(length);
    }

    /**
     * 编辑文本换行时调用的方法，处理编辑文本的换行操作。
     * @param index 换行的编辑文本的索引
     * @param text 换行的编辑文本的内容
     */
    public void onEditTextEnter(int index, String text) {
        /**
         * Should not happen, check for debug
         */
        if(index > mEditTextList.getChildCount()) {
            // 如果索引超出编辑文本列表的范围，记录错误日志
            Log.e(TAG, "Index out of mEditTextList boundrary, should not happen");
        }

        // 获取新的编辑文本项视图
        View view = getListItem(text, index);
        // 将新的编辑文本项视图添加到指定索引位置
        mEditTextList.addView(view, index);
        NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        // 请求焦点
        edit.requestFocus();
        // 将光标移动到文本开头
        edit.setSelection(0);
        for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
            // 更新后续编辑文本的索引
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i);
        }
    }

    /**
     * 切换到列表模式的方法，将笔记内容显示为列表模式。
     * @param text 笔记内容
     */
    private void switchToListMode(String text) {
        // 清空编辑文本列表
        mEditTextList.removeAllViews();
        // 将笔记内容按换行符分割成多个项
        String[] items = text.split("\n");
        int index = 0;
        for (String item : items) {
            if(!TextUtils.isEmpty(item)) {
                // 如果项不为空，将项添加到编辑文本列表中
                mEditTextList.addView(getListItem(item, index));
                index++;
            }
        }
        // 添加一个空的编辑文本项
        mEditTextList.addView(getListItem("", index));
        // 请求焦点
        mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus();

        // 隐藏笔记编辑器
        mNoteEditor.setVisibility(View.GONE);
        // 显示编辑文本列表
        mEditTextList.setVisibility(View.VISIBLE);
    }

    /**
     * 高亮显示查询结果的方法，将查询关键词在笔记内容中高亮显示。
     * @param fullText 笔记内容
     * @param userQuery 查询关键词
     * @return 高亮显示后的 Spannable 对象
     */
    private Spannable getHighlightQueryResult(String fullText, String userQuery) {
        SpannableString spannable = new SpannableString(fullText == null ? "" : fullText);
        if (!TextUtils.isEmpty(userQuery)) {
            // 编译查询关键词的正则表达式模式
            mPattern = Pattern.compile(userQuery);
            Matcher m = mPattern.matcher(fullText);
            int start = 0;
            while (m.find(start)) {
                // 在匹配到的位置设置背景颜色跨度，实现高亮显示
                spannable.setSpan(
                        new BackgroundColorSpan(this.getResources().getColor(
                                R.color.user_query_highlight)), m.start(), m.end(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                start = m.end();
            }
        }
        return spannable;
    }

    /**
     * 获取列表项视图的方法，根据项内容和索引创建列表项视图。
     * @param item 项内容
     * @param index 项索引
     * @return 列表项视图
     */
    private View getListItem(String item, int index) {
        // 从布局文件中加载列表项视图
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);
        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        // 设置编辑文本的文本外观
        edit.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 如果复选框被选中，设置编辑文本的删除线效果
                    edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    // 如果复选框未被选中，清除编辑文本的删除线效果
                    edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                }
            }
        });

        if (item.startsWith(TAG_CHECKED)) {
            // 如果项内容以已勾选标记开头，设置复选框为选中状态并设置编辑文本的删除线效果
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            item = item.substring(TAG_CHECKED.length(), item.length()).trim();
        } else if (item.startsWith(TAG_UNCHECKED)) {
            // 如果项内容以未勾选标记开头，设置复选框为未选中状态并清除编辑文本的删除线效果
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            item = item.substring(TAG_UNCHECKED.length(), item.length()).trim();
        }

        // 设置编辑文本的文本改变监听器
        edit.setOnTextViewChangeListener(this);
        // 设置编辑文本的索引
        edit.setIndex(index);
        // 设置编辑文本的文本并高亮显示查询结果
        edit.setText(getHighlightQueryResult(item, mUserQuery));
        return view;
    }

    /**
     * 编辑文本内容改变时调用的方法，根据编辑文本是否有内容显示或隐藏复选框。
     * @param index 编辑文本的索引
     * @param hasText 编辑文本是否有内容
     */
    public void onTextChange(int index, boolean hasText) {
        if (index >= mEditTextList.getChildCount()) {
            // 如果索引超出编辑文本列表的范围，记录错误日志
            Log.e(TAG, "Wrong index, should not happen");
            return;
        }
        if(hasText) {
            // 如果编辑文本有内容，显示复选框
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.VISIBLE);
        } else {
            // 如果编辑文本没有内容，隐藏复选框
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.GONE);
        }
    }

    /**
     * 列表模式改变时调用的方法，根据新的列表模式切换笔记的显示模式。
     * @param oldMode 旧的列表模式
     * @param newMode 新的列表模式
     */
    public void onCheckListModeChanged(int oldMode, int newMode) {
        if (newMode == TextNote.MODE_CHECK_LIST) {
            // 如果新的列表模式是列表模式，切换到列表模式
            switchToListMode(mNoteEditor.getText().toString());
        } else {
            if (!getWorkingText()) {
                // 如果获取工作文本失败，去除笔记内容中的未勾选标记
                mWorkingNote.setWorkingText(mWorkingNote.getContent().replace(TAG_UNCHECKED + " ",
                        ""));
            }
            // 设置笔记编辑器的文本并高亮显示查询结果
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            // 隐藏编辑文本列表
            mEditTextList.setVisibility(View.GONE);
            // 显示笔记编辑器
            mNoteEditor.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 获取工作文本的方法，根据笔记的显示模式获取笔记内容。
     * @return 如果笔记中有已勾选的项返回 true，否则返回 false
     */
    private boolean getWorkingText() {
        boolean hasChecked = false;
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mEditTextList.getChildCount(); i++) {
                View view = mEditTextList.getChildAt(i);
                NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
                if (!TextUtils.isEmpty(edit.getText())) {
                    if (((CheckBox) view.findViewById(R.id.cb_edit_item)).isChecked()) {
                        // 如果复选框被选中，添加已勾选标记和编辑文本内容
                        sb.append(TAG_CHECKED).append(" ").append(edit.getText()).append("\n");
                        hasChecked = true;
                    } else {
                        // 如果复选框未被选中，添加未勾选标记和编辑文本内容
                        sb.append(TAG_UNCHECKED).append(" ").append(edit.getText()).append("\n");
                    }
                }
            }
            // 设置工作文本
            mWorkingNote.setWorkingText(sb.toString());
        } else {
            // 如果不是列表模式，设置工作文本为笔记编辑器的文本
            mWorkingNote.setWorkingText(mNoteEditor.getText().toString());
        }
        return hasChecked;
    }

    /**
     * 保存笔记的方法，获取工作文本并保存笔记。
     * @return 如果笔记保存成功返回 true，否则返回 false
     */
    private boolean saveNote() {
        // 获取工作文本
        getWorkingText();
        boolean saved = mWorkingNote.saveNote();
        if (saved) {
            /**
             * 从列表视图进入编辑视图有两种模式，打开一个笔记，创建/编辑一个笔记。打开笔记需要在从编辑视图返回时回到列表中的原始位置，而创建一个新笔记需要回到列表的顶部。此代码 {@link #RESULT_OK} 用于标识创建/编辑状态
             */
            // 设置活动结果为成功
            setResult(RESULT_OK);
        }
        return saved;
    }

    /**
     * 将笔记发送到桌面的方法，创建笔记的快捷方式并发送到桌面。
     */
    private void sendToDesktop() {
        /**
         * 在向主屏幕发送消息之前，应确保当前正在编辑的笔记存在于数据库中。因此，对于新笔记，首先保存它
         */
        if (!mWorkingNote.existInDatabase()) {
            // 如果笔记未保存，保存笔记
            saveNote();
        }

        if (mWorkingNote.getNoteId() > 0) {
            Intent sender = new Intent();
            Intent shortcutIntent = new Intent(this, NoteEditActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId());
            sender.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            sender.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    makeShortcutIconTitle(mWorkingNote.getContent()));
            sender.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_app));
            sender.putExtra("duplicate", true);
            sender.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            // 显示笔记已发送到桌面的提示
            showToast(R.string.info_note_enter_desktop);
            // 发送广播创建快捷方式
            sendBroadcast(sender);
        } else {
            /**
             * 存在用户未输入任何内容（笔记不值得保存）的情况，此时没有笔记 ID，提醒用户应输入内容
             */
            Log.e(TAG, "Send to desktop error");
            // 显示笔记为空无法发送到桌面的提示
            showToast(R.string.error_note_empty_for_send_to_desktop);
        }
    }

    /**
     * 生成快捷图标标题的方法，去除笔记内容中的勾选标记并截取标题长度。
     * @param content 笔记内容
     * @return 生成的快捷图标标题
     */
    private String makeShortcutIconTitle(String content) {
        // 去除笔记内容中的已勾选标记
        content = content.replace(TAG_CHECKED, "");
        // 去除笔记内容中的未勾选标记
        content = content.replace(TAG_UNCHECKED, "");
        return content.length() > SHORTCUT_ICON_TITLE_MAX_LEN ? content.substring(0,
                SHORTCUT_ICON_TITLE_MAX_LEN) : content;
    }

    /**
     * 显示短时间提示的方法。
     * @param resId 提示信息的资源 ID
     */
    private void showToast(int resId) {
        // 显示短时间提示
        showToast(resId, Toast.LENGTH_SHORT);
    }

    /**
     * 显示提示的方法。
     * @param resId 提示信息的资源 ID
     * @param duration 提示显示的时长
     */
    private void showToast(int resId, int duration) {
        // 显示提示
        Toast.makeText(this, resId, duration).show();
    }
}
