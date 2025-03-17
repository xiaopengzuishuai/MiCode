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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.tool.BackupUtils;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * 该类用于展示笔记列表，并处理与笔记列表相关的操作，如创建、删除、移动笔记等。
 * 实现了OnClickListener和OnItemLongClickListener接口，用于处理点击和长按事件。
 */
public class NotesListActivity extends Activity implements OnClickListener, OnItemLongClickListener {
    // 文件夹笔记列表查询的令牌
    private static final int FOLDER_NOTE_LIST_QUERY_TOKEN = 0;

    // 文件夹列表查询的令牌
    private static final int FOLDER_LIST_QUERY_TOKEN      = 1;

    // 文件夹删除菜单的ID
    private static final int MENU_FOLDER_DELETE = 0;

    // 文件夹查看菜单的ID
    private static final int MENU_FOLDER_VIEW = 1;

    // 文件夹重命名菜单的ID
    private static final int MENU_FOLDER_CHANGE_NAME = 2;

    // 用于存储应用介绍是否已插入的偏好设置键
    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction";

    /**
     * 定义列表编辑状态的枚举类型
     */
    private enum ListEditState {
        // 笔记列表状态
        NOTE_LIST,
        // 子文件夹状态
        SUB_FOLDER,
        // 通话记录文件夹状态
        CALL_RECORD_FOLDER
    };

    // 当前列表编辑状态
    private ListEditState mState;

    // 用于在后台执行查询操作的处理程序
    private BackgroundQueryHandler mBackgroundQueryHandler;

    // 笔记列表的适配器
    private NotesListAdapter mNotesListAdapter;

    // 显示笔记列表的ListView
    private ListView mNotesListView;

    // 用于添加新笔记的按钮
    private Button mAddNewNote;

    // 标记是否需要分发触摸事件
    private boolean mDispatch;

    // 触摸事件的原始Y坐标
    private int mOriginY;

    // 触摸事件分发的Y坐标
    private int mDispatchY;

    // 标题栏的TextView
    private TextView mTitleBar;

    // 当前所在文件夹的ID
    private long mCurrentFolderId;

    // 内容解析器，用于与内容提供者交互
    private ContentResolver mContentResolver;

    // 多选模式的回调处理类
    private ModeCallback mModeCallBack;

    // 日志标签
    private static final String TAG = "NotesListActivity";

    // 笔记列表滚动速率
    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30;

    // 当前聚焦的笔记数据项
    private NoteItemData mFocusNoteDataItem;

    // 普通选择条件，用于查询指定父文件夹下的笔记
    private static final String NORMAL_SELECTION = NoteColumns.PARENT_ID + "=?";

    // 根文件夹选择条件，用于查询根文件夹下的笔记和通话记录文件夹
    private static final String ROOT_FOLDER_SELECTION = "(" + NoteColumns.TYPE + "<>"
            + Notes.TYPE_SYSTEM + " AND " + NoteColumns.PARENT_ID + "=?)" + " OR ("
            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER + " AND "
            + NoteColumns.NOTES_COUNT + ">0)";

    // 打开笔记的请求码
    private final static int REQUEST_CODE_OPEN_NODE = 102;
    // 创建新笔记的请求码
    private final static int REQUEST_CODE_NEW_NODE  = 103;

    /**
     * 活动创建时调用，初始化界面和资源，并插入应用介绍笔记
     *
     * @param savedInstanceState 保存的活动状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_list);
        initResources();

        /**
         * 当用户首次使用该应用时，插入一条介绍信息
         */
        setAppInfoFromRawRes();
    }

    /**
     * 处理活动返回结果，当打开或创建笔记成功时，更新笔记列表
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        返回的数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && (requestCode == REQUEST_CODE_OPEN_NODE || requestCode == REQUEST_CODE_NEW_NODE)) {
            mNotesListAdapter.changeCursor(null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 从原始资源文件中读取应用介绍信息，并创建一条笔记保存
     */
    private void setAppInfoFromRawRes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sp.getBoolean(PREFERENCE_ADD_INTRODUCTION, false)) {
            StringBuilder sb = new StringBuilder();
            InputStream in = null;
            try {
                 in = getResources().openRawResource(R.raw.introduction);
                if (in != null) {
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    char [] buf = new char[1024];
                    int len = 0;
                    // 循环读取文件内容
                    while ((len = br.read(buf)) > 0) {
                        sb.append(buf, 0, len);
                    }
                } else {
                    Log.e(TAG, "Read introduction file error");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            // 创建一个空的笔记
            WorkingNote note = WorkingNote.createEmptyNote(this, Notes.ID_ROOT_FOLDER,
                    AppWidgetManager.INVALID_APPWIDGET_ID, Notes.TYPE_WIDGET_INVALIDE,
                    ResourceParser.RED);
            note.setWorkingText(sb.toString());
            if (note.saveNote()) {
                // 保存介绍笔记成功后，标记为已插入
                sp.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).commit();
            } else {
                Log.e(TAG, "Save introduction note error");
                return;
            }
        }
    }

    /**
     * 活动开始时调用，启动异步笔记列表查询
     */
    @Override
    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery();
    }

    /**
     * 初始化资源，包括视图、适配器、监听器等
     */
    private void initResources() {
        mContentResolver = this.getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(this.getContentResolver());
        mCurrentFolderId = Notes.ID_ROOT_FOLDER;
        mNotesListView = (ListView) findViewById(R.id.notes_list);
        mNotesListView.addFooterView(LayoutInflater.from(this).inflate(R.layout.note_list_footer, null),
                null, false);
        mNotesListView.setOnItemClickListener(new OnListItemClickListener());
        mNotesListView.setOnItemLongClickListener(this);
        mNotesListAdapter = new NotesListAdapter(this);
        mNotesListView.setAdapter(mNotesListAdapter);
        mAddNewNote = (Button) findViewById(R.id.btn_new_note);
        mAddNewNote.setOnClickListener(this);
        mAddNewNote.setOnTouchListener(new NewNoteOnTouchListener());
        mDispatch = false;
        mDispatchY = 0;
        mOriginY = 0;
        mTitleBar = (TextView) findViewById(R.id.tv_title_bar);
        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
    }

    /**
     * 多选模式的回调处理类，实现了ListView.MultiChoiceModeListener和OnMenuItemClickListener接口
     */
    private class ModeCallback implements ListView.MultiChoiceModeListener, OnMenuItemClickListener {
        // 下拉菜单
        private DropdownMenu mDropDownMenu;
        // 动作模式
        private ActionMode mActionMode;
        // 移动菜单的菜单项
        private MenuItem mMoveMenu;

        /**
         * 创建动作模式时调用，初始化菜单和多选模式
         *
         * @param mode 动作模式
         * @param menu 菜单
         * @return 是否创建成功
         */
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            menu.findItem(R.id.delete).setOnMenuItemClickListener(this);
            mMoveMenu = menu.findItem(R.id.move);
            if (mFocusNoteDataItem.getParentId() == Notes.ID_CALL_RECORD_FOLDER
                    || DataUtils.getUserFolderCount(mContentResolver) == 0) {
                mMoveMenu.setVisible(false);
            } else {
                mMoveMenu.setVisible(true);
                mMoveMenu.setOnMenuItemClickListener(this);
            }
            mActionMode = mode;
            mNotesListAdapter.setChoiceMode(true);
            mNotesListView.setLongClickable(false);
            mAddNewNote.setVisibility(View.GONE);

            View customView = LayoutInflater.from(NotesListActivity.this).inflate(
                    R.layout.note_list_dropdown_menu, null);
            mode.setCustomView(customView);
            mDropDownMenu = new DropdownMenu(NotesListActivity.this,
                    (Button) customView.findViewById(R.id.selection_menu),
                    R.menu.note_list_dropdown);
            mDropDownMenu.setOnDropdownMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                /**
                 * 处理下拉菜单项点击事件，全选或反选所有笔记
                 *
                 * @param item 被点击的菜单项
                 * @return 是否处理成功
                 */
                public boolean onMenuItemClick(MenuItem item) {
                    mNotesListAdapter.selectAll(!mNotesListAdapter.isAllSelected());
                    updateMenu();
                    return true;
                }

            });
            return true;
        }

        /**
         * 更新菜单状态，包括下拉菜单的标题和全选/反选菜单项的状态
         */
        private void updateMenu() {
            int selectedCount = mNotesListAdapter.getSelectedCount();
            // 更新下拉菜单
            String format = getResources().getString(R.string.menu_select_title, selectedCount);
            mDropDownMenu.setTitle(format);
            MenuItem item = mDropDownMenu.findItem(R.id.action_select_all);
            if (item != null) {
                if (mNotesListAdapter.isAllSelected()) {
                    item.setChecked(true);
                    item.setTitle(R.string.menu_deselect_all);
                } else {
                    item.setChecked(false);
                    item.setTitle(R.string.menu_select_all);
                }
            }
        }

        /**
         * 准备动作模式时调用，目前未实现具体逻辑
         *
         * @param mode 动作模式
         * @param menu 菜单
         * @return 是否准备成功
         */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * 处理动作模式菜单项点击事件，目前未实现具体逻辑
         *
         * @param mode 动作模式
         * @param item 被点击的菜单项
         * @return 是否处理成功
         */
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * 销毁动作模式时调用，恢复多选模式前的状态
         *
         * @param mode 动作模式
         */
        public void onDestroyActionMode(ActionMode mode) {
            mNotesListAdapter.setChoiceMode(false);
            mNotesListView.setLongClickable(true);
            mAddNewNote.setVisibility(View.VISIBLE);
        }

        /**
         * 结束动作模式
         */
        public void finishActionMode() {
            mActionMode.finish();
        }

        /**
         * 处理笔记选中状态改变事件，更新菜单状态
         *
         * @param mode    动作模式
         * @param position 笔记位置
         * @param id       笔记ID
         * @param checked  是否选中
         */
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            mNotesListAdapter.setCheckedItem(position, checked);
            updateMenu();
        }

        /**
         * 处理菜单项点击事件，根据菜单项ID执行相应操作
         *
         * @param item 被点击的菜单项
         * @return 是否处理成功
         */
        public boolean onMenuItemClick(MenuItem item) {
            if (mNotesListAdapter.getSelectedCount() == 0) {
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            switch (item.getItemId()) {
                case R.id.delete:
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(getString(R.string.alert_title_delete));
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(getString(R.string.alert_message_delete_notes,
                                             mNotesListAdapter.getSelectedCount()));
                    builder.setPositiveButton(android.R.string.ok,
                                             new DialogInterface.OnClickListener() {
                                                 /**
                                                  * 处理删除确认对话框的确定按钮点击事件，批量删除笔记
                                                  *
                                                  * @param dialog 对话框
                                                  * @param which  点击的按钮索引
                                                  */
                                                 public void onClick(DialogInterface dialog,
                                                         int which) {
                                                     batchDelete();
                                                 }
                                             });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.show();
                    break;
                case R.id.move:
                    startQueryDestinationFolders();
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    /**
     * 新笔记按钮的触摸监听器，处理触摸事件的分发
     */
    private class NewNoteOnTouchListener implements OnTouchListener {

        /**
         * 处理触摸事件，根据事件类型进行相应处理
         *
         * @param v     被触摸的视图
         * @param event 触摸事件
         * @return 是否处理成功
         */
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    Display display = getWindowManager().getDefaultDisplay();
                    int screenHeight = display.getHeight();
                    int newNoteViewHeight = mAddNewNote.getHeight();
                    int start = screenHeight - newNoteViewHeight;
                    int eventY = start + (int) event.getY();
                    /**
                     * 减去标题栏的高度
                     */
                    if (mState == ListEditState.SUB_FOLDER) {
                        eventY -= mTitleBar.getHeight();
                        start -= mTitleBar.getHeight();
                    }
                    /**
                     * HACKME:当点击“新建笔记”按钮的透明部分时，将事件分发到该按钮后面的列表视图。
                     * “新建笔记”按钮的透明部分可以用公式y=-0.12x+94（单位：像素）和按钮顶部的线来表示。
                     * 坐标基于“新建笔记”按钮的左侧。94表示透明部分的最大高度。
                     * 注意，如果按钮的背景发生变化，公式也应该相应改变。这只是为了满足UI设计师的强烈要求。
                     */
                    if (event.getY() < (event.getX() * (-0.12) + 94)) {
                        View view = mNotesListView.getChildAt(mNotesListView.getChildCount() - 1
                                - mNotesListView.getFooterViewsCount());
                        if (view != null && view.getBottom() > start
                                && (view.getTop() < (start + 94))) {
                            mOriginY = (int) event.getY();
                            mDispatchY = eventY;
                            event.setLocation(event.getX(), mDispatchY);
                            mDispatch = true;
                            return mNotesListView.dispatchTouchEvent(event);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mDispatch) {
                        mDispatchY += (int) event.getY() - mOriginY;
                        event.setLocation(event.getX(), mDispatchY);
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
                default: {
                    if (mDispatch) {
                        event.setLocation(event.getX(), mDispatchY);
                        mDispatch = false;
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
            }
            return false;
        }

    };

    /**
     * 启动异步笔记列表查询
     */
    private void startAsyncNotesListQuery() {
        String selection = (mCurrentFolderId == Notes.ID_ROOT_FOLDER) ? ROOT_FOLDER_SELECTION
                : NORMAL_SELECTION;
        mBackgroundQueryHandler.startQuery(FOLDER_NOTE_LIST_QUERY_TOKEN, null,
                Notes.CONTENT_NOTE_URI, NoteItemData.PROJECTION, selection, new String[] {
                    String.valueOf(mCurrentFolderId)
                }, NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC");
    }

    /**
     * 后台查询处理类，继承自AsyncQueryHandler，处理查询完成后的回调
     */
    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        /**
         * 查询完成时调用，根据查询令牌处理不同的查询结果
         *
         * @param token  查询令牌
         * @param cookie 额外信息
         * @param cursor 查询结果的游标
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case FOLDER_NOTE_LIST_QUERY_TOKEN:
                    mNotesListAdapter.changeCursor(cursor);
                    break;
                case FOLDER_LIST_QUERY_TOKEN:
                    if (cursor != null && cursor.getCount() > 0) {
                        showFolderListMenu(cursor);
                    } else {
                        Log.e(TAG, "Query folder failed");
                    }
                    break;
                default:
                    return;
            }
        }
    }

    /**
     * 显示文件夹列表菜单，供用户选择目标文件夹
     *
     * @param cursor 文件夹列表的游标
     */
    private void showFolderListMenu(Cursor cursor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        builder.setTitle(R.string.menu_title_select_folder);
        final FoldersListAdapter adapter = new FoldersListAdapter(this, cursor);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            /**
             * 处理文件夹列表菜单的点击事件，将选中的笔记移动到目标文件夹
             *
             * @param dialog 对话框
             * @param which  点击的文件夹索引
             */
            public void onClick(DialogInterface dialog, int which) {
                DataUtils.batchMoveToFolder(mContentResolver,
                        mNotesListAdapter.getSelectedItemIds(), adapter.getItemId(which));
                Toast.makeText(
                        NotesListActivity.this,
                        getString(R.string.format_move_notes_to_folder,
                                mNotesListAdapter.getSelectedCount(),
                                adapter.getFolderName(NotesListActivity.this, which)),
                        Toast.LENGTH_SHORT).show();
                mModeCallBack.finishActionMode();
            }
        });
        builder.show();
    }

    /**
     * 创建新笔记，启动笔记编辑活动
     */
    private void createNewNote() {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mCurrentFolderId);
        this.startActivityForResult(intent, REQUEST_CODE_NEW_NODE);
    }

    /**
     * 批量删除笔记，根据同步模式将笔记直接删除或移动到回收站
     */
    private void batchDelete() {
        new AsyncTask<Void, Void, HashSet<AppWidgetAttribute>>() {
            /**
             * 在后台执行批量删除操作，返回受影响的小部件列表
             *
             * @param unused 未使用的参数
             * @return 受影响的小部件列表
             */
            protected HashSet<AppWidgetAttribute> doInBackground(Void... unused) {
                HashSet<AppWidgetAttribute> widgets = mNotesListAdapter.getSelectedWidget();
                if (!isSyncMode()) {
                    // 如果未同步，直接删除笔记
                    if (DataUtils.batchDeleteNotes(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds())) {
                    } else {
                        Log.e(TAG, "Delete notes error, should not happens");
                    }
                } else {
                    // 在同步模式下，将删除的笔记移动到回收站
                    if (!DataUtils.batchMoveToFolder(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds(), Notes.ID_TRASH_FOLER)) {
                        Log.e(TAG, "Move notes to trash folder error, should not happens");
                    }
                }
                return widgets;
            }

            /**
             * 处理后台任务完成后的操作，更新受影响的小部件
             *
             * @param widgets 受影响的小部件列表
             */
            @Override
            protected void onPostExecute(HashSet<AppWidgetAttribute> widgets) {
                if (widgets != null) {
                    for (AppWidgetAttribute widget : widgets) {
                        if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                                && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                            updateWidget(widget.widgetId, widget.widgetType);
                        }
                    }
                }
                mModeCallBack.finishActionMode();
            }
        }.execute();
    }

    /**
     * 删除文件夹，根据同步模式将文件夹直接删除或移动到回收站
     *
     * @param folderId 要删除的文件夹ID
     */
    private void deleteFolder(long folderId) {
        if (folderId == Notes.ID_ROOT_FOLDER) {
            Log.e(TAG, "Wrong folder id, should not happen " + folderId);
            return;
        }

        HashSet<Long> ids = new HashSet<Long>();
        ids.add(folderId);
        HashSet<AppWidgetAttribute> widgets = DataUtils.getFolderNoteWidget(mContentResolver,
                folderId);
        if (!isSyncMode()) {
            // 如果未同步，直接删除文件夹
            DataUtils.batchDeleteNotes(mContentResolver, ids);
        } else {
            // 在同步模式下，将删除的文件夹移动到回收站
            DataUtils.batchMoveToFolder(mContentResolver, ids, Notes.ID_TRASH_FOLER);
        }
        if (widgets != null) {
            for (AppWidgetAttribute widget : widgets) {
                if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                        && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                    updateWidget(widget.widgetId, widget.widgetType);
                }
            }
        }
    }

    /**
     * 打开笔记，启动笔记查看活动
     *
     * @param data 笔记数据项
     */
    private void openNode(NoteItemData data) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_UID, data.getId());
        this.startActivityForResult(intent, REQUEST_CODE_OPEN_NODE);
    }

    /**
     * 打开文件夹，更新当前文件夹ID，查询并显示文件夹内的笔记
     *
     * @param data 文件夹数据项
     */
    private void openFolder(NoteItemData data) {
        mCurrentFolderId = data.getId();
        startAsyncNotesListQuery();
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mState = ListEditState.CALL_RECORD_FOLDER;
            mAddNewNote.setVisibility(View.GONE);
        } else {
            mState = ListEditState.SUB_FOLDER;
        }
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mTitleBar.setText(R.string.call_record_folder_name);
        } else {
            mTitleBar.setText(data.getSnippet());
        }
        mTitleBar.setVisibility(View.VISIBLE);
    }

    /**
     * 处理点击事件，根据点击的视图ID执行相应操作
     *
     * @param v 被点击的视图
     */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_new_note:
                createNewNote();
                break;
            default:
                break;
        }
    }

    /**
     * 显示软键盘
     */
    private void showSoftInput() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    /**
     * 隐藏软键盘
     *
     * @param view 触发隐藏软键盘的视图
     */
    private void hideSoftInput(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 显示创建或修改文件夹的对话框
     *
     * @param create 是否为创建文件夹
     */
    private void showCreateOrModifyFolderDialog(final boolean create) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
        showSoftInput();
        if (!create) {
            if (mFocusNoteDataItem != null) {
                etName.setText(mFocusNoteDataItem.getSnippet());
                builder.setTitle(getString(R.string.menu_folder_change_name));
            } else {
                Log.e(TAG, "The long click data item is null");
                return;
            }
        } else {
            etName.setText("");
            builder.setTitle(this.getString(R.string.menu_create_folder));
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            /**
             * 处理取消按钮点击事件，隐藏软键盘
             *
             * @param dialog 对话框
             * @param which  点击的按钮索引
             */
            public void onClick(DialogInterface dialog, int which) {
                hideSoftInput(etName);
            }
        });

        final Dialog dialog = builder.setView(view).show();
        final Button positive = (Button)dialog.findViewById(android.R.id.button1);
        positive.setOnClickListener(new OnClickListener() {
            /**
             * 处理确定按钮点击事件，创建或修改文件夹
             *
             * @param v 被点击的视图
             */
            public void onClick(View v) {
                hideSoftInput(etName);
                String name = etName.getText().toString();
                if (DataUtils.checkVisibleFolderName(mContentResolver, name)) {
                    Toast.makeText(NotesListActivity.this, getString(R.string.folder_exist, name),
                            Toast.LENGTH_LONG).show();
                    etName.setSelection(0, etName.length());
                    return;
                }
                if (!create) {
                    if (!TextUtils.isEmpty(name)) {
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SNIPPET, name);
                        values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                        values.put(NoteColumns.LOCAL_MODIFIED, 1);
                        mContentResolver.update(Notes.CONTENT_NOTE_URI, values, NoteColumns.ID
                                + "=?", new String[] {
                            String.valueOf(mFocusNoteDataItem.getId())
                        });
                    }
                } else if (!TextUtils.isEmpty(name)) {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.SNIPPET, name);
                    values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                    mContentResolver.insert(Notes.CONTENT_NOTE_URI, values);
                }
                dialog.dismiss();
            }
        });

        if (TextUtils.isEmpty(etName.getText())) {
            positive.setEnabled(false);
        }
        /**
         * 当文件夹名称编辑框为空时，禁用确定按钮
         */
        etName.addTextChangedListener(new TextWatcher() {
            /**
             * 文本改变前的回调，目前未实现具体逻辑
             *
             * @param s     文本内容
             * @param start 起始位置
             * @param count 改变的字符数
             * @param after 改变后的字符数
             */
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            /**
             * 文本改变时的回调，根据文本内容启用或禁用确定按钮
             *
             * @param s      文本内容
             * @param start  起始位置
             * @param before 改变前的字符数
             * @param count  改变的字符数
             */
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(etName.getText())) {
                    positive.setEnabled(false);
                } else {
                    positive.setEnabled(true);
                }
            }

            /**
             * 文本改变后的回调，目前未实现具体逻辑
             *
             * @param s 编辑后的文本
             */
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
    }

    /**
     * 处理返回按钮点击事件，根据当前状态返回上一级或退出活动
     */
    @Override
    public void onBackPressed() {
        switch (mState) {
            case SUB_FOLDER:
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                startAsyncNotesListQuery();
                mTitleBar.setVisibility(View.GONE);
                break;
            case CALL_RECORD_FOLDER:
                mCurrentFolderId = Notes.ID_ROOT_FOLDER;
                mState = ListEditState.NOTE_LIST;
                mAddNewNote.setVisibility(View.VISIBLE);
                mTitleBar.setVisibility(View.GONE);
                startAsyncNotesListQuery();
                break;
            case NOTE_LIST:
                super.onBackPressed();
                break;
            default:
                break;
        }
    }

    /**
     * 更新小部件，发送广播通知小部件更新
     *
     * @param appWidgetId  小部件ID
     * @param appWidgetType 小部件类型
     */
    private void updateWidget(int appWidgetId, int appWidgetType) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        if (appWidgetType == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (appWidgetType == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unspported widget type");
            return;
        }

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
            appWidgetId
        });

        sendBroadcast(intent);
        setResult(RESULT_OK, intent);
    }

    /**
     * 文件夹上下文菜单创建监听器，用于创建文件夹的上下文菜单
     */
    private final OnCreateContextMenuListener mFolderOnCreateContextMenuListener = new OnCreateContextMenuListener() {
        /**
         * 创建上下文菜单时调用，设置菜单标题和菜单项
         *
         * @param menu       上下文菜单
         * @param v          触发上下文菜单的视图
         * @param menuInfo   菜单信息
         */
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (mFocusNoteDataItem != null) {
                menu.setHeaderTitle(mFocusNoteDataItem.getSnippet());
                menu.add(0, MENU_FOLDER_VIEW, 0, R.string.menu_folder_view);
                menu.add(0, MENU_FOLDER_DELETE, 0, R.string.menu_folder_delete);
                menu.add(0, MENU_FOLDER_CHANGE_NAME, 0, R.string.menu_folder_change_name);
            }
        }
    };

    /**
     * 上下文菜单关闭时调用，移除上下文菜单创建监听器
     *
     * @param menu 上下文菜单
     */
    @Override
    public void onContextMenuClosed(Menu menu) {
        if (mNotesListView != null) {
            mNotesListView.setOnCreateContextMenuListener(null);
        }
        super.onContextMenuClosed(menu);
    }

    /**
     * 处理上下文菜单项选中事件，根据菜单项ID执行相应操作
     *
     * @param item 被选中的菜单项
     * @return 是否处理成功
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mFocusNoteDataItem == null) {
            Log.e(TAG, "The long click data item is null");
            return false;
        }
        switch (item.getItemId()) {
            case MENU_FOLDER_VIEW:
                openFolder(mFocusNoteDataItem);
                break;
            case MENU_FOLDER_DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_folder));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            /**
                             * 处理删除文件夹确认对话框的确定按钮点击事件，删除文件夹
                             *
                             * @param dialog 对话框
                             * @param which  点击的按钮索引
                             */
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFolder(mFocusNoteDataItem.getId());
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case MENU_FOLDER_CHANGE_NAME:
                showCreateOrModifyFolderDialog(false);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * 准备选项菜单时调用，根据当前状态加载不同的菜单
     *
     * @param menu 选项菜单
     * @return 是否准备成功
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mState == ListEditState.NOTE_LIST) {
            getMenuInflater().inflate(R.menu.note_list, menu);
            // 设置同步或取消同步菜单项的标题
            menu.findItem(R.id.menu_sync).setTitle(
                    GTaskSyncService.isSyncing() ? R.string.menu_sync_cancel : R.string.menu_sync);
        } else if (mState == ListEditState.SUB_FOLDER) {
            getMenuInflater().inflate(R.menu.sub_folder, menu);
        } else if (mState == ListEditState.CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_record_folder, menu);
        } else {
            Log.e(TAG, "Wrong state:" + mState);
        }
        return true;
    }

    /**
     * 处理选项菜单项选中事件，根据菜单项ID执行相应操作
     *
     * @param item 被选中的菜单项
     * @return 是否处理成功
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                showCreateOrModifyFolderDialog(true);
                break;
            }
            case R.id.menu_export_text: {
                exportNoteToText();
                break;
            }
            case R.id.menu_sync: {
                if (isSyncMode()) {
                    if (TextUtils.equals(item.getTitle(), getString(R.string.menu_sync))) {
                        GTaskSyncService.startSync(this);
                    } else {
                        GTaskSyncService.cancelSync(this);
                    }
                } else {
                    startPreferenceActivity();
                }
                break;
            }
            case R.id.menu_setting: {
                startPreferenceActivity();
                break;
            }
            case R.id.menu_new_note: {
                createNewNote();
                break;
            }
            case R.id.menu_search:
                onSearchRequested();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 处理搜索请求，启动搜索活动
     *
     * @return 是否处理成功
     */
    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /* appData */, false);
        return true;
    }

    /**
     * 导出笔记为文本文件，使用异步任务处理导出操作
     */
    private void exportNoteToText() {
        final BackupUtils backup = BackupUtils.getInstance(NotesListActivity.this);
        new AsyncTask<Void, Void, Integer>() {

            /**
             * 在后台执行导出操作，返回导出结果状态码
             *
             * @param unused 未使用的参数
             * @return 导出结果状态码
             */
            @Override
            protected Integer doInBackground(Void... unused) {
                return backup.exportToText();
            }

            /**
             * 处理后台任务完成后的操作，根据导出结果显示相应的提示对话框
             *
             * @param result 导出结果状态码
             */
            @Override
            protected void onPostExecute(Integer result) {
                if (result == BackupUtils.STATE_SD_CARD_UNMOUONTED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.failed_sdcard_export));
                    builder.setMessage(NotesListActivity.this
                            .getString(R.string.error_sdcard_unmounted));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                } else if (result == BackupUtils.STATE_SUCCESS) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.success_sdcard_export));
                    builder.setMessage(NotesListActivity.this.getString(
                            R.string.format_exported_file_location, backup
                                    .getExportedTextFileName(), backup.getExportedTextFileDir()));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                } else if (result == BackupUtils.STATE_SYSTEM_ERROR) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.failed_sdcard_export));
                    builder.setMessage(NotesListActivity.this
                            .getString(R.string.error_sdcard_export));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
            }

        }.execute();
    }

    /**
     * 判断是否处于同步模式，根据同步账户名称是否为空来判断
     *
     * @return 是否处于同步模式
     */
    private boolean isSyncMode() {
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    /**
     * 启动偏好设置活动
     */
    private void startPreferenceActivity() {
        Activity from = getParent() != null ? getParent() : this;
        Intent intent = new Intent(from, NotesPreferenceActivity.class);
        from.startActivityIfNeeded(intent, -1);
    }

    /**
     * 笔记列表项点击监听器，处理笔记列表项的点击事件
     */
    private class OnListItemClickListener implements OnItemClickListener {

        /**
         * 处理笔记列表项的点击事件，根据当前状态和笔记类型执行相应操作
         *
         * @param parent   父适配器视图
         * @param view     被点击的视图
         * @param position 点击的位置
         * @param id       点击的笔记ID
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (view instanceof NotesListItem) {
                NoteItemData item = ((NotesListItem) view).getItemData();
                if (mNotesListAdapter.isInChoiceMode()) {
                    if (item.getType() == Notes.TYPE_NOTE) {
                        position = position - mNotesListView.getHeaderViewsCount();
                        mModeCallBack.onItemCheckedStateChanged(null, position, id,
                                !mNotesListAdapter.isSelectedItem(position));
                    }
                    return;
                }

                switch (mState) {
                    case NOTE_LIST:
                        if (item.getType() == Notes.TYPE_FOLDER
                                || item.getType() == Notes.TYPE_SYSTEM) {
                            openFolder(item);
                        } else if (item.getType() == Notes.TYPE_NOTE) {
                            openNode(item);
                        } else {
                            Log.e(TAG, "Wrong note type in NOTE_LIST");
                        }
                        break;
                    case SUB_FOLDER:
                    case CALL_RECORD_FOLDER:
                        if (item.getType() == Notes.TYPE_NOTE) {
                            openNode(item);
                        } else {
                            Log.e(TAG, "Wrong note type in SUB_FOLDER");
                        }
                        break;
                    default:
                        break;
                }
            }
        }

    }

    /**
     * 启动查询目标文件夹的异步查询
     */
    private void startQueryDestinationFolders() {
        String selection = NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>? AND " + NoteColumns.ID + "<>?";
        selection = (mState == ListEditState.NOTE_LIST) ? selection:
            "(" + selection + ") OR (" + NoteColumns.ID + "=" + Notes.ID_ROOT_FOLDER + ")";

        mBackgroundQueryHandler.startQuery(FOLDER_LIST_QUERY_TOKEN,
                null,
                Notes.CONTENT_NOTE_URI,
                FoldersListAdapter.PROJECTION,
                selection,
                new String[] {
                        String.valueOf(Notes.TYPE_FOLDER),
                        String.valueOf(Notes.ID_TRASH_FOLER),
                        String.valueOf(mCurrentFolderId)
                },
                NoteColumns.MODIFIED_DATE + " DESC");
    }

    /**
     * 处理笔记列表项的长按事件，根据笔记类型启动多选模式或显示上下文菜单
     *
     * @param parent   父适配器视图
     * @param view     被长按的视图
     * @param position 长按的位置
     * @param id       长按的笔记ID
     * @return 是否处理成功
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view instanceof NotesListItem) {
            mFocusNoteDataItem = ((NotesListItem) view).getItemData();
            if (mFocusNoteDataItem.getType() == Notes.TYPE_NOTE && !mNotesListAdapter.isInChoiceMode()) {
                if (mNotesListView.startActionMode(mModeCallBack) != null) {
                    mModeCallBack.onItemCheckedStateChanged(null, position, id, true);
                    mNotesListView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } else {
                    Log.e(TAG, "startActionMode fails");
                }
            } else if (mFocusNoteDataItem.getType() == Notes.TYPE_FOLDER) {
                mNotesListView.setOnCreateContextMenuListener(mFolderOnCreateContextMenuListener);
            }
        }
        return false;
    }
}

