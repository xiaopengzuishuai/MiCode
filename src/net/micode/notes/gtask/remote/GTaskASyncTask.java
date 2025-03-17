
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

// 声明该类所在的包，表明这是一个处理 Google 任务异步任务的包
package net.micode.notes.gtask.remote;

// 导入 Android 系统的通知相关类，用于显示同步状态的通知
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
// 导入 Android 系统的上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 系统的意图类，用于启动活动和发送广播
import android.content.Intent;
// 导入 Android 系统的异步任务类，用于在后台执行耗时操作
import android.os.AsyncTask;

// 导入应用的资源类，用于获取字符串和图标资源
import net.micode.notes.R;
// 导入应用的笔记列表活动类，用于在同步成功时打开笔记列表
import net.micode.notes.ui.NotesListActivity;
// 导入应用的笔记偏好设置活动类，用于在同步失败或取消时打开设置页面
import net.micode.notes.ui.NotesPreferenceActivity;

/**
 * GTaskASyncTask 类继承自 AsyncTask，用于在后台执行 Google 任务的同步操作。
 * 该类处理同步的开始、进度更新和结束，并通过通知向用户显示同步状态。
 */
public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {
    // 定义同步通知的唯一标识符
    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235;

    /**
     * OnCompleteListener 接口定义了一个回调方法，用于在同步任务完成时通知调用者。
     */
    public interface OnCompleteListener {
        /**
         * 当同步任务完成时调用此方法。
         */
        void onComplete();
    }

    // 上下文对象，用于获取系统服务和资源
    private Context mContext;
    // 通知管理器，用于显示和管理同步通知
    private NotificationManager mNotifiManager;
    // Google 任务管理器，负责实际的同步操作
    private GTaskManager mTaskManager;
    // 同步完成监听器，用于在同步完成时回调
    private OnCompleteListener mOnCompleteListener;

    /**
     * 构造函数，初始化 GTaskASyncTask 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param listener 同步完成监听器，用于在同步完成时回调
     */
    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        // 获取通知管理器系统服务
        mNotifiManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // 获取 Google 任务管理器实例
        mTaskManager = GTaskManager.getInstance();
    }

    /**
     * 取消同步操作。
     */
    public void cancelSync() {
        mTaskManager.cancelSync();
    }

    /**
     * 发布同步进度消息。
     *
     * @param message 同步进度消息
     */
    public void publishProgess(String message) {
        // 调用 AsyncTask 的 publishProgress 方法发布进度消息
        publishProgress(new String[] {
            message
        });
    }

    /**
     * 显示同步状态通知。
     *
     * @param tickerId 通知的滚动消息资源 ID
     * @param content 通知的内容
     */
    private void showNotification(int tickerId, String content) {
        // 创建通知对象
        Notification notification = new Notification(R.drawable.notification, mContext
                .getString(tickerId), System.currentTimeMillis());
        // 设置通知的默认灯光效果
        notification.defaults = Notification.DEFAULT_LIGHTS;
        // 设置通知自动取消
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        // 定义点击通知时的待定意图
        PendingIntent pendingIntent;
        if (tickerId != R.string.ticker_success) {
            // 如果不是同步成功通知，点击通知打开笔记偏好设置活动
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesPreferenceActivity.class), 0);

        } else {
            // 如果是同步成功通知，点击通知打开笔记列表活动
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext,
                    NotesListActivity.class), 0);
        }
        // 设置通知的最新事件信息
        notification.setLatestEventInfo(mContext, mContext.getString(R.string.app_name), content,
                pendingIntent);
        // 发送通知
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);
    }

    /**
     * 在后台线程中执行同步操作。
     *
     * @param unused 未使用的参数
     * @return 同步操作的结果状态码
     */
    @Override
    protected Integer doInBackground(Void... unused) {
        // 发布登录进度消息
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity
                .getSyncAccountName(mContext)));
        // 调用 Google 任务管理器的同步方法进行同步操作
        return mTaskManager.sync(mContext, this);
    }

    /**
     * 在主线程中更新同步进度。
     *
     * @param progress 同步进度消息
     */
    @Override
    protected void onProgressUpdate(String... progress) {
        // 显示同步中的通知
        showNotification(R.string.ticker_syncing, progress[0]);
        if (mContext instanceof GTaskSyncService) {
            // 如果上下文是 GTaskSyncService 类型，发送广播通知进度消息
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]);
        }
    }

    /**
     * 在主线程中处理同步完成后的操作。
     *
     * @param result 同步操作的结果状态码
     */
    @Override
    protected void onPostExecute(Integer result) {
        if (result == GTaskManager.STATE_SUCCESS) {
            // 如果同步成功，显示成功通知并记录最后同步时间
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount()));
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis());
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) {
            // 如果是网络错误，显示网络错误通知
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network));
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) {
            // 如果是内部错误，显示内部错误通知
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal));
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) {
            // 如果同步被取消，显示取消通知
            showNotification(R.string.ticker_cancel, mContext
                    .getString(R.string.error_sync_cancelled));
        }
        if (mOnCompleteListener != null) {
            // 如果有同步完成监听器，在新线程中调用其 onComplete 方法
            new Thread(new Runnable() {

                public void run() {
                    mOnCompleteListener.onComplete();
                }
            }).start();
        }
    }
}
