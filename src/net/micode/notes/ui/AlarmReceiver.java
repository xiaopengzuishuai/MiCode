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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    /**
     * 当接收到广播时，此方法会被调用。
     * 它的主要作用是将接收到的意图（Intent）重定向到 AlarmAlertActivity 并启动该活动。
     * 
     * @param context 广播接收器所在的上下文环境
     * @param intent  接收到的广播意图
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 设置意图的目标类为 AlarmAlertActivity，这样当启动该意图时，会打开 AlarmAlertActivity
        intent.setClass(context, AlarmAlertActivity.class);
        // 为意图添加 FLAG_ACTIVITY_NEW_TASK 标志，用于在新的任务栈中启动活动
        // 这在从广播接收器启动活动时通常是必要的，因为广播接收器没有自己的任务栈
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 使用上下文环境启动该意图，从而启动 AlarmAlertActivity
        context.startActivity(intent);
    }
}
