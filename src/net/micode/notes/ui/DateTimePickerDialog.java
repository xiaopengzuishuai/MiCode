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

// 导入 java.util.Calendar 类，用于处理日期和时间
import java.util.Calendar;

// 导入应用的资源类，用于访问应用的资源，如字符串、布局等
import net.micode.notes.R;
// 导入自定义的日期时间选择器类
import net.micode.notes.ui.DateTimePicker;
// 导入自定义的日期时间改变监听器接口
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

// 导入 Android 对话框类，用于创建对话框
import android.app.AlertDialog;
// 导入 Android 上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 对话框接口
import android.content.DialogInterface;
// 导入 Android 对话框点击监听器接口
import android.content.DialogInterface.OnClickListener;
// 导入 Android 日期格式化工具类
import android.text.format.DateFormat;
// 导入 Android 日期工具类，用于日期和时间的格式化
import android.text.format.DateUtils;

/**
 * DateTimePickerDialog 类继承自 AlertDialog，用于创建一个自定义的日期时间选择对话框。
 * 该对话框包含一个日期时间选择器，允许用户选择特定的日期和时间。
 * 当用户选择完成后，会触发相应的回调事件。
 */
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {
    // 用于存储当前选择的日期和时间
    private Calendar mDate = Calendar.getInstance();
    // 标记是否使用 24 小时制显示时间
    private boolean mIs24HourView;
    // 日期时间选择完成的回调监听器
    private OnDateTimeSetListener mOnDateTimeSetListener;
    // 自定义的日期时间选择器控件
    private DateTimePicker mDateTimePicker;

    /**
     * 定义一个内部接口，用于在日期时间选择完成时回调。
     */
    public interface OnDateTimeSetListener {
        /**
         * 当日期时间选择完成时调用此方法。
         *
         * @param dialog 日期时间选择对话框实例
         * @param date   选择的日期和时间的毫秒数
         */
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    /**
     * 构造函数，用于创建 DateTimePickerDialog 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param date    初始显示的日期和时间的毫秒数
     */
    public DateTimePickerDialog(Context context, long date) {
        // 调用父类的构造函数
        super(context);
        // 创建一个新的日期时间选择器实例
        mDateTimePicker = new DateTimePicker(context);
        // 将日期时间选择器设置为对话框的视图
        setView(mDateTimePicker);
        // 为日期时间选择器设置日期时间改变监听器
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            /**
             * 当日期时间发生改变时调用此方法。
             *
             * @param view      日期时间选择器视图
             * @param year      选择的年份
             * @param month     选择的月份（0 - 11）
             * @param dayOfMonth 选择的日期
             * @param hourOfDay  选择的小时（0 - 23）
             * @param minute     选择的分钟
             */
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                                          int dayOfMonth, int hourOfDay, int minute) {
                // 更新 mDate 中的年份
                mDate.set(Calendar.YEAR, year);
                // 更新 mDate 中的月份
                mDate.set(Calendar.MONTH, month);
                // 更新 mDate 中的日期
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                // 更新 mDate 中的小时
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                // 更新 mDate 中的分钟
                mDate.set(Calendar.MINUTE, minute);
                // 更新对话框的标题，显示当前选择的日期和时间
                updateTitle(mDate.getTimeInMillis());
            }
        });
        // 设置 mDate 的时间为传入的日期和时间
        mDate.setTimeInMillis(date);
        // 将秒数设置为 0
        mDate.set(Calendar.SECOND, 0);
        // 设置日期时间选择器的当前日期和时间
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis());
        // 设置对话框的确定按钮，并指定点击监听器为当前类
        setButton(context.getString(R.string.datetime_dialog_ok), this);
        // 设置对话框的取消按钮，不指定点击监听器
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener) null);
        // 根据系统设置确定是否使用 24 小时制显示时间
        set24HourView(DateFormat.is24HourFormat(this.getContext()));
        // 更新对话框的标题，显示初始的日期和时间
        updateTitle(mDate.getTimeInMillis());
    }

    /**
     * 设置是否使用 24 小时制显示时间。
     *
     * @param is24HourView true 表示使用 24 小时制，false 表示使用 12 小时制
     */
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
    }

    /**
     * 设置日期时间选择完成的回调监听器。
     *
     * @param callBack 实现了 OnDateTimeSetListener 接口的回调对象
     */
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    /**
     * 更新对话框的标题，显示当前选择的日期和时间。
     *
     * @param date 选择的日期和时间的毫秒数
     */
    private void updateTitle(long date) {
        // 定义日期时间格式化标志，显示年、月、日和时间
        int flag =
                DateUtils.FORMAT_SHOW_YEAR |
                        DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_TIME;
        // 根据是否使用 24 小时制添加相应的标志
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_24HOUR;
        // 设置对话框的标题为格式化后的日期时间
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag));
    }

    /**
     * 处理对话框按钮点击事件的回调方法。
     * 当用户点击确定按钮时，调用回调监听器的 OnDateTimeSet 方法。
     *
     * @param arg0 对话框对象
     * @param arg1 被点击的按钮标识
     */
    public void onClick(DialogInterface arg0, int arg1) {
        // 检查回调监听器是否已设置
        if (mOnDateTimeSetListener != null) {
            // 调用回调监听器的 OnDateTimeSet 方法，传递当前对话框和选择的日期时间
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }

}