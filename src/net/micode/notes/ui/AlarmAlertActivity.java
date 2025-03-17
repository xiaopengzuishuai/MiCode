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

// 导入 Android 活动相关类
import android.app.Activity;
// 导入 Android 对话框构建器类
import android.app.AlertDialog;
// 导入 Android 上下文类
import android.content.Context;
// 导入 Android 对话框接口
import android.content.DialogInterface;
// 导入 Android 对话框点击监听器接口
import android.content.DialogInterface.OnClickListener;
// 导入 Android 对话框关闭监听器接口
import android.content.DialogInterface.OnDismissListener;
// 导入 Android 意图类
import android.content.Intent;
// 导入 Android 音频管理类
import android.media.AudioManager;
// 导入 Android 媒体播放器类
import android.media.MediaPlayer;
// 导入 Android 铃声管理器类
import android.media.RingtoneManager;
// 导入 Android URI 类
import android.net.Uri;
// 导入 Android 包数据类
import android.os.Bundle;
// 导入 Android 电源管理类
import android.os.PowerManager;
// 导入 Android 设置类
import android.provider.Settings;
// 导入 Android 窗口类
import android.view.Window;
// 导入 Android 窗口管理器类
import android.view.WindowManager;

// 导入应用资源类
import net.micode.notes.R;
// 导入应用笔记数据类
import net.micode.notes.data.Notes;
// 导入应用数据工具类
import net.micode.notes.tool.DataUtils;

// 导入输入输出异常类
import java.io.IOException;

/**
 * AlarmAlertActivity 类用于处理闹钟提醒功能，当闹钟触发时，该活动会显示一个对话框，
 * 同时播放闹钟声音，并提供相应的用户交互操作。
 */
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    // 用于存储当前笔记的 ID
    private long mNoteId;
    // 用于存储笔记的摘要信息
    private String mSnippet;
    // 定义笔记摘要的最大显示长度
    private static final int SNIPPET_PREW_MAX_LEN = 60;
    // 用于播放闹钟声音的媒体播放器对象
    MediaPlayer mPlayer;

    /**
     * 当活动创建时调用此方法，进行初始化操作，包括设置窗口属性、获取笔记信息、
     * 显示对话框和播放闹钟声音等。
     *
     * @param savedInstanceState 如果活动是重新创建的，则包含之前保存的状态信息，否则为 null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类的 onCreate 方法
        super.onCreate(savedInstanceState);
        // 请求不显示活动的标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 获取当前活动的窗口对象
        final Window win = getWindow();
        // 设置窗口标志，使活动在设备锁定时也能显示
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 检查屏幕是否处于开启状态
        if (!isScreenOn()) {
            // 设置窗口标志，保持屏幕开启、唤醒屏幕、允许屏幕开启时锁定设备以及调整布局
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }

        // 获取启动该活动的意图对象
        Intent intent = getIntent();

        try {
            // 从意图的数据中提取笔记的 ID
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            // 通过笔记 ID 从内容解析器中获取笔记的摘要信息
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            // 如果摘要信息长度超过最大显示长度，则进行截断处理
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,
                    SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)
                    : mSnippet;
        } catch (IllegalArgumentException e) {
            // 打印异常堆栈信息
            e.printStackTrace();
            // 异常发生时结束当前活动
            return;
        }

        // 创建一个新的媒体播放器对象
        mPlayer = new MediaPlayer();
        // 检查笔记是否在数据库中可见
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            // 显示操作对话框
            showActionDialog();
            // 播放闹钟声音
            playAlarmSound();
        } else {
            // 笔记不可见时结束当前活动
            finish();
        }
    }

    /**
     * 检查屏幕是否处于开启状态。
     *
     * @return 如果屏幕开启返回 true，否则返回 false
     */
    private boolean isScreenOn() {
        // 获取电源管理服务
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // 返回屏幕是否开启的状态
        return pm.isScreenOn();
    }

    /**
     * 播放闹钟声音，设置音频流类型、数据源、准备媒体播放器并开始播放。
     */
    private void playAlarmSound() {
        // 获取默认的闹钟铃声 URI
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);

        // 获取静音模式下受影响的音频流设置
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        // 根据静音模式设置音频流类型
        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
        try {
            // 设置媒体播放器的数据源
            mPlayer.setDataSource(this, url);
            // 准备媒体播放器
            mPlayer.prepare();
            // 设置媒体播放器循环播放
            mPlayer.setLooping(true);
            // 开始播放音频
            mPlayer.start();
        } catch (IllegalArgumentException e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        } catch (SecurityException e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        } catch (IOException e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 显示操作对话框，设置对话框的标题、消息、按钮等，并为对话框添加关闭监听器。
     */
    private void showActionDialog() {
        // 创建一个对话框构建器对象
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        // 设置对话框的标题
        dialog.setTitle(R.string.app_name);
        // 设置对话框的消息内容
        dialog.setMessage(mSnippet);
        // 设置对话框的确定按钮，并指定点击监听器
        dialog.setPositiveButton(R.string.notealert_ok, this);
        // 如果屏幕处于开启状态，设置对话框的否定按钮，并指定点击监听器
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }
        // 显示对话框并设置关闭监听器
        dialog.show().setOnDismissListener(this);
    }

    /**
     * 处理对话框按钮点击事件的回调方法
     * 
     * @param dialog 被点击的对话框对象
     * @param which  被点击的按钮标识
     */
    public void onClick(DialogInterface dialog, int which) {
        // 根据被点击的按钮标识进行不同的处理
        switch (which) {
            // 如果点击的是对话框的负按钮（通常是“取消”或自定义的否定操作按钮）
            case DialogInterface.BUTTON_NEGATIVE:
                // 创建一个新的 Intent，用于启动 NoteEditActivity
                Intent intent = new Intent(this, NoteEditActivity.class);
                // 设置 Intent 的动作是查看操作
                intent.setAction(Intent.ACTION_VIEW);
                // 向 Intent 中添加额外的数据，将当前笔记的 ID 传递给 NoteEditActivity
                intent.putExtra(Intent.EXTRA_UID, mNoteId);
                // 启动 NoteEditActivity
                startActivity(intent);
                // 跳出 switch 语句
                break;
            // 如果点击的是其他按钮，不做任何处理
            default:
                break;
        }
    }

    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound();
        finish();
    }

    private void stopAlarmSound() {
        // 停止播放闹钟声音，并释放 MediaPlayer 资源
        if (mPlayer != null) {
            // 停止播放
            mPlayer.stop();
            // 释放 MediaPlayer 占用的资源
            mPlayer.release();
            // 将 mPlayer 置为 null，避免后续误操作
            mPlayer = null;
        }
    } // 结束 stopAlarmSound 方法

} // 结束 AlarmAlertActivity 类