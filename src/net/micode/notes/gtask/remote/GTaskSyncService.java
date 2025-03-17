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
 * GTaskSyncService 类用于管理 Google 任务的同步服务，包括启动、取消同步以及广播同步状态。
 */
package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * 该服务负责处理 Google 任务的同步操作，包括启动和取消同步，并通过广播通知同步状态。
 */
public class GTaskSyncService extends Service {
    /**
     * 用于在 Intent 中传递同步操作类型的键名。
     */
    public final static String ACTION_STRING_NAME = "sync_action_type";

    /**
     * 表示启动同步操作的常量。
     */
    public final static int ACTION_START_SYNC = 0;

    /**
     * 表示取消同步操作的常量。
     */
    public final static int ACTION_CANCEL_SYNC = 1;

    /**
     * 表示无效操作的常量。
     */
    public final static int ACTION_INVALID = 2;

    /**
     * 同步服务广播的名称，用于发送同步状态更新。
     */
    public final static String GTASK_SERVICE_BROADCAST_NAME = "net.micode.notes.gtask.remote.gtask_sync_service";

    /**
     * 广播中用于表示是否正在同步的键名。
     */
    public final static String GTASK_SERVICE_BROADCAST_IS_SYNCING = "isSyncing";

    /**
     * 广播中用于传递同步进度消息的键名。
     */
    public final static String GTASK_SERVICE_BROADCAST_PROGRESS_MSG = "progressMsg";

    /**
     * 静态的异步任务实例，用于执行同步操作。
     */
    private static GTaskASyncTask mSyncTask = null;

    /**
     * 静态的同步进度消息。
     */
    private static String mSyncProgress = "";

    /**
     * 启动同步操作。
     * 如果当前没有正在进行的同步任务，则创建一个新的异步任务并开始执行。
     */
    private void startSync() {
        if (mSyncTask == null) {
            // 创建一个新的异步任务实例，并传入当前服务和完成监听器
            mSyncTask = new GTaskASyncTask(this, new GTaskASyncTask.OnCompleteListener() {
                /**
                 * 当同步任务完成时调用的方法。
                 * 清空同步任务实例，发送广播通知同步完成，并停止服务。
                 */
                public void onComplete() {
                    mSyncTask = null;
                    // 发送广播，更新同步状态
                    sendBroadcast("");
                    // 停止当前服务
                    stopSelf();
                }
            });
            // 发送广播，更新同步状态
            sendBroadcast("");
            // 执行异步任务
            mSyncTask.execute();
        }
    }

    /**
     * 取消同步操作。
     * 如果当前有正在进行的同步任务，则调用其取消方法。
     */
    private void cancelSync() {
        if (mSyncTask != null) {
            // 取消当前的同步任务
            mSyncTask.cancelSync();
        }
    }

    /**
     * 服务创建时调用的方法。
     * 初始化同步任务实例为空。
     */
    @Override
    public void onCreate() {
        mSyncTask = null;
    }

    /**
     * 服务启动时调用的方法，处理传入的 Intent。
     * 根据 Intent 中的操作类型，启动或取消同步任务。
     *
     * @param intent  启动服务的 Intent
     * @param flags   启动标志
     * @param startId 启动 ID
     * @return 服务的启动模式
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 获取 Intent 中的额外数据
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_STRING_NAME)) {
            // 根据同步操作类型执行相应的操作
            switch (bundle.getInt(ACTION_STRING_NAME, ACTION_INVALID)) {
                case ACTION_START_SYNC:
                    // 启动同步操作
                    startSync();
                    break;
                case ACTION_CANCEL_SYNC:
                    // 取消同步操作
                    cancelSync();
                    break;
                default:
                    break;
            }
            // 返回服务启动模式为 START_STICKY
            return START_STICKY;
        }
        // 调用父类的 onStartCommand 方法
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 当系统内存不足时调用的方法。
     * 如果当前有正在进行的同步任务，则取消该任务。
     */
    @Override
    public void onLowMemory() {
        if (mSyncTask != null) {
            // 取消当前的同步任务
            mSyncTask.cancelSync();
        }
    }

    /**
     * 绑定服务时调用的方法。
     * 此服务不支持绑定，返回 null。
     *
     * @param intent 绑定服务的 Intent
     * @return 绑定的 IBinder 对象，此处返回 null
     */
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 发送广播，通知同步状态和进度消息。
     *
     * @param msg 同步进度消息
     */
    public void sendBroadcast(String msg) {
        // 更新同步进度消息
        mSyncProgress = msg;
        // 创建一个新的 Intent 用于广播
        Intent intent = new Intent(GTASK_SERVICE_BROADCAST_NAME);
        // 在 Intent 中添加是否正在同步的信息
        intent.putExtra(GTASK_SERVICE_BROADCAST_IS_SYNCING, mSyncTask != null);
        // 在 Intent 中添加同步进度消息
        intent.putExtra(GTASK_SERVICE_BROADCAST_PROGRESS_MSG, msg);
        // 发送广播
        sendBroadcast(intent);
    }

    /**
     * 静态方法，用于从活动中启动同步服务。
     *
     * @param activity 调用此方法的活动
     */
    public static void startSync(Activity activity) {
        // 设置 GTaskManager 的活动上下文
        GTaskManager.getInstance().setActivityContext(activity);
        // 创建一个新的 Intent，用于启动 GTaskSyncService
        Intent intent = new Intent(activity, GTaskSyncService.class);
        // 在 Intent 中添加同步操作类型为启动同步
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_START_SYNC);
        // 启动服务
        activity.startService(intent);
    }

    /**
     * 静态方法，用于从上下文中取消同步服务。
     *
     * @param context 调用此方法的上下文
     */
    public static void cancelSync(Context context) {
        // 创建一个新的 Intent，用于启动 GTaskSyncService
        Intent intent = new Intent(context, GTaskSyncService.class);
        // 在 Intent 中添加同步操作类型为取消同步
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_CANCEL_SYNC);
        // 启动服务
        context.startService(intent);
    }

    /**
     * 静态方法，用于检查是否正在进行同步操作。
     *
     * @return 如果正在同步返回 true，否则返回 false
     */
    public static boolean isSyncing() {
        return mSyncTask != null;
    }

    /**
     * 静态方法，用于获取当前的同步进度消息。
     *
     * @return 当前的同步进度消息
     */
    public static String getProgressString() {
        return mSyncProgress;
    }
}
