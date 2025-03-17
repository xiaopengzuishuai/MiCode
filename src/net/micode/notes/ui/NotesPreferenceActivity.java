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

// 导入 Android 账户类，用于表示系统中的账户
import android.accounts.Account;
// 导入 Android 账户管理器类，用于管理系统中的账户
import android.accounts.AccountManager;
// 导入 Android 动作栏类，用于显示应用的标题和操作按钮
import android.app.ActionBar;
// 导入 Android 警告对话框类，用于显示警告信息和提示用户操作
import android.app.AlertDialog;
// 导入 Android 内容值类，用于存储键值对数据
import android.content.ContentValues;
// 导入 Android 上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 对话框接口类，用于处理对话框的事件
import android.content.DialogInterface;
// 导入 Android 意图类，用于在不同组件之间传递消息和启动活动
import android.content.Intent;
// 导入 Android 意图过滤器类，用于过滤意图
import android.content.IntentFilter;
// 导入 Android 共享偏好类，用于存储应用的配置信息
import android.content.SharedPreferences;
// 导入 Android 包类，用于存储活动的状态信息
import android.os.Bundle;
// 导入 Android 偏好类，用于创建偏好设置项
import android.preference.Preference;
// 导入 Android 偏好点击监听器接口，用于处理偏好设置项的点击事件
import android.preference.Preference.OnPreferenceClickListener;
// 导入 Android 偏好活动类，用于创建偏好设置界面
import android.preference.PreferenceActivity;
// 导入 Android 偏好类别类，用于对偏好设置项进行分组
import android.preference.PreferenceCategory;
// 导入 Android 文本工具类，用于处理文本操作
import android.text.TextUtils;
// 导入 Android 日期格式化类，用于格式化日期和时间
import android.text.format.DateFormat;
// 导入 Android 布局加载器类，用于加载布局文件
import android.view.LayoutInflater;
// 导入 Android 菜单类，用于创建菜单
import android.view.Menu;
// 导入 Android 菜单项类，用于表示菜单中的单个项
import android.view.MenuItem;
// 导入 Android 视图类，是所有 UI 组件的基类
import android.view.View;
// 导入 Android 按钮类，用于创建按钮
import android.widget.Button;
// 导入 Android 文本视图类，用于显示文本
import android.widget.TextView;
// 导入 Android 吐司类，用于显示短暂的提示信息
import android.widget.Toast;

// 导入应用的资源类，用于访问应用的资源
import net.micode.notes.R;
// 导入应用的笔记数据类
import net.micode.notes.data.Notes;
// 导入应用的笔记列类，用于定义笔记数据库的列名
import net.micode.notes.data.Notes.NoteColumns;
// 导入应用的 Google 任务同步服务类
import net.micode.notes.gtask.remote.GTaskSyncService;

/**
 * NotesPreferenceActivity 类继承自 PreferenceActivity，用于创建笔记应用的偏好设置界面。
 * 该界面允许用户设置同步账户、查看最后同步时间，并提供同步操作按钮。
 * 同时，它处理账户选择、账户更改确认、同步状态更新等功能。
 */
public class NotesPreferenceActivity extends PreferenceActivity {
    // 定义共享偏好的名称
    public static final String PREFERENCE_NAME = "notes_preferences";
    // 定义同步账户名称的偏好键
    public static final String PREFERENCE_SYNC_ACCOUNT_NAME = "pref_key_account_name";
    // 定义最后同步时间的偏好键
    public static final String PREFERENCE_LAST_SYNC_TIME = "pref_last_sync_time";
    // 定义设置背景颜色的偏好键
    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear";
    // 定义同步账户的偏好键
    private static final String PREFERENCE_SYNC_ACCOUNT_KEY = "pref_sync_account_key";
    // 定义权限过滤器的键
    private static final String AUTHORITIES_FILTER_KEY = "authorities";
    // 同步账户的偏好类别
    private PreferenceCategory mAccountCategory;
    // Google 任务广播接收器
    private GTaskReceiver mReceiver;
    // 原始账户列表
    private Account[] mOriAccounts;
    // 是否添加了新账户的标志
    private boolean mHasAddedAccount;

    /**
     * 活动创建时调用的方法，用于初始化界面和设置广播接收器。
     *
     * @param icicle 保存的活动状态
     */
    @Override
    protected void onCreate(Bundle icicle) {
        // 调用父类的 onCreate 方法
        super.onCreate(icicle);

        // 设置动作栏的返回按钮可用
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // 从资源文件中加载偏好设置
        addPreferencesFromResource(R.xml.preferences);
        // 查找同步账户的偏好类别
        mAccountCategory = (PreferenceCategory) findPreference(PREFERENCE_SYNC_ACCOUNT_KEY);
        // 创建 Google 任务广播接收器
        mReceiver = new GTaskReceiver();
        // 创建意图过滤器
        IntentFilter filter = new IntentFilter();
        // 添加 Google 任务服务的广播动作
        filter.addAction(GTaskSyncService.GTASK_SERVICE_BROADCAST_NAME);
        // 注册广播接收器
        registerReceiver(mReceiver, filter);

        // 初始化原始账户列表为 null
        mOriAccounts = null;
        // 加载设置界面的头部布局
        View header = LayoutInflater.from(this).inflate(R.layout.settings_header, null);
        // 将头部布局添加到列表视图中
        getListView().addHeaderView(header, null, true);
    }

    /**
     * 活动恢复时调用的方法，用于检查是否添加了新账户并刷新界面。
     */
    @Override
    protected void onResume() {
        // 调用父类的 onResume 方法
        super.onResume();

        // 如果添加了新账户
        if (mHasAddedAccount) {
            // 获取当前的 Google 账户列表
            Account[] accounts = getGoogleAccounts();
            // 如果原始账户列表不为空且当前账户数量大于原始账户数量
            if (mOriAccounts != null && accounts.length > mOriAccounts.length) {
                // 遍历当前账户列表
                for (Account accountNew : accounts) {
                    // 标记是否找到新账户
                    boolean found = false;
                    // 遍历原始账户列表
                    for (Account accountOld : mOriAccounts) {
                        // 如果新账户与原始账户名称相同
                        if (TextUtils.equals(accountOld.name, accountNew.name)) {
                            // 标记为已找到
                            found = true;
                            // 跳出内层循环
                            break;
                        }
                    }
                    // 如果未找到新账户
                    if (!found) {
                        // 设置新账户为同步账户
                        setSyncAccount(accountNew.name);
                        // 跳出外层循环
                        break;
                    }
                }
            }
        }

        // 刷新界面
        refreshUI();
    }

    /**
     * 活动销毁时调用的方法，用于注销广播接收器。
     */
    @Override
    protected void onDestroy() {
        // 如果广播接收器不为空
        if (mReceiver != null) {
            // 注销广播接收器
            unregisterReceiver(mReceiver);
        }
        // 调用父类的 onDestroy 方法
        super.onDestroy();
    }

    /**
     * 加载同步账户的偏好设置项。
     */
    private void loadAccountPreference() {
        // 移除同步账户偏好类别中的所有偏好项
        mAccountCategory.removeAll();

        // 创建一个新的偏好设置项
        Preference accountPref = new Preference(this);
        // 获取当前的同步账户名称
        final String defaultAccount = getSyncAccountName(this);
        // 设置偏好项的标题
        accountPref.setTitle(getString(R.string.preferences_account_title));
        // 设置偏好项的摘要
        accountPref.setSummary(getString(R.string.preferences_account_summary));
        // 设置偏好项的点击监听器
        accountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            /**
             * 处理偏好项的点击事件。
             *
             * @param preference 被点击的偏好项
             * @return 处理结果
             */
            public boolean onPreferenceClick(Preference preference) {
                // 如果当前没有正在进行同步操作
                if (!GTaskSyncService.isSyncing()) {
                    // 如果当前没有设置同步账户
                    if (TextUtils.isEmpty(defaultAccount)) {
                        // 显示选择账户的警告对话框
                        showSelectAccountAlertDialog();
                    } else {
                        // 如果已经设置了同步账户，显示更改账户的确认对话框
                        showChangeAccountConfirmAlertDialog();
                    }
                } else {
                    // 如果正在同步，显示提示信息
                    Toast.makeText(NotesPreferenceActivity.this,
                            R.string.preferences_toast_cannot_change_account, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }
        });

        // 将偏好项添加到同步账户偏好类别中
        mAccountCategory.addPreference(accountPref);
    }

    /**
     * 加载同步按钮和最后同步时间的显示。
     */
    private void loadSyncButton() {
        // 查找同步按钮视图
        Button syncButton = (Button) findViewById(R.id.preference_sync_button);
        // 查找最后同步时间的文本视图
        TextView lastSyncTimeView = (TextView) findViewById(R.id.prefenerece_sync_status_textview);

        // 根据同步状态设置按钮文本和点击监听器
        if (GTaskSyncService.isSyncing()) {
            // 如果正在同步，设置按钮文本为取消同步
            syncButton.setText(getString(R.string.preferences_button_sync_cancel));
            // 设置按钮的点击监听器为取消同步
            syncButton.setOnClickListener(new View.OnClickListener() {
                /**
                 * 处理按钮的点击事件，取消同步操作。
                 *
                 * @param v 被点击的视图
                 */
                public void onClick(View v) {
                    GTaskSyncService.cancelSync(NotesPreferenceActivity.this);
                }
            });
        } else {
            // 如果没有同步，设置按钮文本为立即同步
            syncButton.setText(getString(R.string.preferences_button_sync_immediately));
            // 设置按钮的点击监听器为开始同步
            syncButton.setOnClickListener(new View.OnClickListener() {
                /**
                 * 处理按钮的点击事件，开始同步操作。
                 *
                 * @param v 被点击的视图
                 */
                public void onClick(View v) {
                    GTaskSyncService.startSync(NotesPreferenceActivity.this);
                }
            });
        }
        // 根据是否设置了同步账户启用或禁用按钮
        syncButton.setEnabled(!TextUtils.isEmpty(getSyncAccountName(this)));

        // 根据同步状态设置最后同步时间的显示
        if (GTaskSyncService.isSyncing()) {
            // 如果正在同步，显示同步进度信息
            lastSyncTimeView.setText(GTaskSyncService.getProgressString());
            // 显示最后同步时间的文本视图
            lastSyncTimeView.setVisibility(View.VISIBLE);
        } else {
            // 如果没有同步，获取最后同步时间
            long lastSyncTime = getLastSyncTime(this);
            if (lastSyncTime != 0) {
                // 如果最后同步时间不为 0，显示格式化后的最后同步时间
                lastSyncTimeView.setText(getString(R.string.preferences_last_sync_time,
                        DateFormat.format(getString(R.string.preferences_last_sync_time_format),
                                lastSyncTime)));
                // 显示最后同步时间的文本视图
                lastSyncTimeView.setVisibility(View.VISIBLE);
            } else {
                // 如果最后同步时间为 0，隐藏最后同步时间的文本视图
                lastSyncTimeView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 刷新界面，包括同步账户偏好设置和同步按钮的显示。
     */
    private void refreshUI() {
        // 加载同步账户的偏好设置项
        loadAccountPreference();
        // 加载同步按钮和最后同步时间的显示
        loadSyncButton();
    }

    /**
     * 显示选择账户的警告对话框。
     */
    private void showSelectAccountAlertDialog() {
        // 创建警告对话框的构建器
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 加载账户选择对话框的标题布局
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        // 查找标题文本视图
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        // 设置标题文本
        titleTextView.setText(getString(R.string.preferences_dialog_select_account_title));
        // 查找副标题文本视图
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        // 设置副标题文本
        subtitleTextView.setText(getString(R.string.preferences_dialog_select_account_tips));

        // 设置自定义标题视图
        dialogBuilder.setCustomTitle(titleView);
        // 设置确认按钮为空
        dialogBuilder.setPositiveButton(null, null);

        // 获取当前的 Google 账户列表
        Account[] accounts = getGoogleAccounts();
        // 获取当前的同步账户名称
        String defAccount = getSyncAccountName(this);

        // 保存原始账户列表
        mOriAccounts = accounts;
        // 标记未添加新账户
        mHasAddedAccount = false;

        // 如果有可用的账户
        if (accounts.length > 0) {
            // 创建账户名称的字符序列数组
            CharSequence[] items = new CharSequence[accounts.length];
            // 保存账户名称的映射
            final CharSequence[] itemMapping = items;
            // 初始化选中项的索引为 -1
            int checkedItem = -1;
            // 初始化索引为 0
            int index = 0;
            // 遍历账户列表
            for (Account account : accounts) {
                // 如果当前账户与默认账户名称相同
                if (TextUtils.equals(account.name, defAccount)) {
                    // 设置选中项的索引
                    checkedItem = index;
                }
                // 将账户名称添加到字符序列数组中
                items[index++] = account.name;
            }
            // 设置单选列表项
            dialogBuilder.setSingleChoiceItems(items, checkedItem,
                    new DialogInterface.OnClickListener() {
                        /**
                         * 处理单选列表项的点击事件。
                         *
                         * @param dialog 对话框
                         * @param which  被点击的项的索引
                         */
                        public void onClick(DialogInterface dialog, int which) {
                            // 设置选中的账户为同步账户
                            setSyncAccount(itemMapping[which].toString());
                            // 关闭对话框
                            dialog.dismiss();
                            // 刷新界面
                            refreshUI();
                        }
                    });
        }

        // 加载添加账户的文本视图
        View addAccountView = LayoutInflater.from(this).inflate(R.layout.add_account_text, null);
        // 设置对话框的视图
        dialogBuilder.setView(addAccountView);

        // 显示对话框
        final AlertDialog dialog = dialogBuilder.show();
        // 设置添加账户视图的点击监听器
        addAccountView.setOnClickListener(new View.OnClickListener() {
            /**
             * 处理添加账户视图的点击事件。
             *
             * @param v 被点击的视图
             */
            public void onClick(View v) {
                // 标记添加了新账户
                mHasAddedAccount = true;
                // 创建添加账户的意图
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                // 添加权限过滤器
                intent.putExtra(AUTHORITIES_FILTER_KEY, new String[] {
                    "gmail-ls"
                });
                // 启动添加账户的活动并等待结果
                startActivityForResult(intent, -1);
                // 关闭对话框
                dialog.dismiss();
            }
        });
    }

    /**
     * 显示更改账户的确认对话框。
     */
    private void showChangeAccountConfirmAlertDialog() {
        // 创建警告对话框的构建器
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 加载账户选择对话框的标题布局
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        // 查找标题文本视图
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        // 设置标题文本，显示当前的同步账户名称
        titleTextView.setText(getString(R.string.preferences_dialog_change_account_title,
                getSyncAccountName(this)));
        // 查找副标题文本视图
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        // 设置副标题文本，显示更改账户的警告信息
        subtitleTextView.setText(getString(R.string.preferences_dialog_change_account_warn_msg));
        // 设置自定义标题视图
        dialogBuilder.setCustomTitle(titleView);

        // 创建菜单项的字符序列数组
        CharSequence[] menuItemArray = new CharSequence[] {
                getString(R.string.preferences_menu_change_account),
                getString(R.string.preferences_menu_remove_account),
                getString(R.string.preferences_menu_cancel)
        };
        // 设置菜单项的点击监听器
        dialogBuilder.setItems(menuItemArray, new DialogInterface.OnClickListener() {
            /**
             * 处理菜单项的点击事件。
             *
             * @param dialog 对话框
             * @param which  被点击的项的索引
             */
            public void onClick(DialogInterface dialog, int which) {
                // 如果点击了更改账户菜单项
                if (which == 0) {
                    // 显示选择账户的警告对话框
                    showSelectAccountAlertDialog();
                // 如果点击了移除账户菜单项
                } else if (which == 1) {
                    // 移除同步账户
                    removeSyncAccount();
                    // 刷新界面
                    refreshUI();
                }
            }
        });
        // 显示对话框
        dialogBuilder.show();
    }

    /**
     * 获取当前设备上的 Google 账户列表。
     *
     * @return Google 账户列表
     */
    private Account[] getGoogleAccounts() {
        // 获取账户管理器实例
        AccountManager accountManager = AccountManager.get(this);
        // 返回 Google 账户列表
        return accountManager.getAccountsByType("com.google");
    }

    /**
     * 设置同步账户。
     *
     * @param account 要设置的账户名称
     */
    private void setSyncAccount(String account) {
        // 如果当前的同步账户名称与要设置的账户名称不同
        if (!getSyncAccountName(this).equals(account)) {
            // 获取共享偏好设置
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            // 获取共享偏好设置的编辑器
            SharedPreferences.Editor editor = settings.edit();
            // 如果要设置的账户名称不为空
            if (account != null) {
                // 将账户名称保存到共享偏好设置中
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, account);
            } else {
                // 如果如果要设置的账户名称为空，将空字符串保存到共享偏好设置中
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
            }
            // 提交编辑器的更改
            editor.commit();

            // 清除最后同步时间
            setLastSyncTime(this, 0);

            // 清除本地 Google 任务相关信息
            new Thread(new Runnable() {
                /**
                 * 线程执行的任务，清除本地 Google 任务相关信息。
                 */
                public void run() {
                    // 创建内容值对象
                    ContentValues values = new ContentValues();
                    // 设置 Google 任务 ID 为空字符串
                    values.put(NoteColumns.GTASK_ID, "");
                    // 设置同步 ID 为 0
                    values.put(NoteColumns.SYNC_ID, 0);
                    // 更新笔记数据库中的 Google 任务相关信息
                    getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
                }
            }).start();

            // 显示设置账户成功的提示信息
            Toast.makeText(NotesPreferenceActivity.this,
                    getString(R.string.preferences_toast_success_set_accout, account),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 移除同步账户。
     */
    private void removeSyncAccount() {
        // 获取共享偏好设置
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        // 获取共享偏好设置的编辑器
        SharedPreferences.Editor editor = settings.edit();
        // 如果共享偏好设置中包含同步账户名称
        if (settings.contains(PREFERENCE_SYNC_ACCOUNT_NAME)) {
            // 移除同步账户名称
            editor.remove(PREFERENCE_SYNC_ACCOUNT_NAME);
        }
        // 如果共享偏好设置中包含最后同步时间
        if (settings.contains(PREFERENCE_LAST_SYNC_TIME)) {
            // 移除最后同步时间
            editor.remove(PREFERENCE_LAST_SYNC_TIME);
        }
        // 提交编辑器的更改
        editor.commit();

        // 清除本地 Google 任务相关信息
        new Thread(new Runnable() {
            /**
             * 线程执行的任务，清除本地 Google 任务相关信息。
             */
            public void run() {
                // 创建内容值对象
                ContentValues values = new ContentValues();
                // 设置 Google 任务 ID 为空字符串
                values.put(NoteColumns.GTASK_ID, "");
                // 设置同步 ID 为 0
                values.put(NoteColumns.SYNC_ID, 0);
                // 更新笔记数据库中的 Google 任务相关信息
                getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
            }
        }).start();
    }

    /**
     * 获取当前的同步账户名称。
     *
     * @param context 上下文对象
     * @return 当前的同步账户名称
     */
    public static String getSyncAccountName(Context context) {
        // 获取共享偏好设置
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        // 返回同步账户名称，如果不存在则返回空字符串
        return settings.getString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
    }

    /**
     * 设置最后同步时间。
     *
     * @param context 上下文对象
     * @param time    最后同步时间
     */
    public static void setLastSyncTime(Context context, long time) {
        // 获取共享偏好设置
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        // 获取共享偏好设置的编辑器
        SharedPreferences.Editor editor = settings.edit();
        // 将最后同步时间保存到共享偏好设置中
        editor.putLong(PREFERENCE_LAST_SYNC_TIME, time);
        // 提交编辑器的更改
        editor.commit();
    }

    /**
     * 获取最后同步时间。
     *
     * @param context 上下文对象
     * @return 最后同步时间，如果不存在则返回 0
     */
    public static long getLastSyncTime(Context context) {
        // 获取共享偏好设置
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        // 返回最后同步时间，如果不存在则返回 0
        return settings.getLong(PREFERENCE_LAST_SYNC_TIME, 0);
    }

    /**
     * 内部类，用于接收 Google 任务服务的广播。
     */
    private class GTaskReceiver extends BroadcastReceiver {
        /**
         * 处理接收到的广播。
         *
         * @param context 上下文对象
         * @param intent  接收到的意图
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // 刷新界面
            refreshUI();
            // 如果广播中包含正在同步的标志
            if (intent.getBooleanExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_IS_SYNCING, false)) {
                // 查找同步状态的文本视图
                TextView syncStatus = (TextView) findViewById(R.id.prefenerece_sync_status_textview);
                // 设置同步状态的文本为广播中的进度信息
                syncStatus.setText(intent
                        .getStringExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_PROGRESS_MSG));
            }
        }
    }

    /**
     * 处理菜单项的选择事件。
     *
     * @param item 被选择的菜单项
     * @return 处理结果
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // 根据菜单项的 ID 进行不同的处理
        switch (item.getItemId()) {
            // 如果选择了返回按钮
            case android.R.id.home:
                // 创建返回笔记列表活动的意图
                Intent intent = new Intent(this, NotesListActivity.class);
                // 添加清除顶部活动的标志
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // 启动笔记列表活动
                startActivity(intent);
                return true;
            // 默认情况
            default:
                return false;
        }
    }
}
