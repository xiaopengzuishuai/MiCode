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

// 导入用于获取日期格式符号的类
import java.text.DateFormatSymbols;
// 导入用于处理日期和时间的类
import java.util.Calendar;

// 导入应用资源类
import net.micode.notes.R;

// 导入 Android 上下文类
import android.content.Context;
// 导入用于格式化日期的类
import android.text.format.DateFormat;
// 导入视图类
import android.view.View;
// 导入帧布局类
import android.widget.FrameLayout;
// 导入数字选择器类
import android.widget.NumberPicker;

/**
 * DateTimePicker 类继承自 FrameLayout，用于创建一个日期时间选择器。
 * 该选择器允许用户选择日期和时间，并支持 12 小时制和 24 小时制。
 */
public class DateTimePicker extends FrameLayout {

    // 默认启用状态
    private static final boolean DEFAULT_ENABLE_STATE = true;

    // 半天的小时数
    private static final int HOURS_IN_HALF_DAY = 12;
    // 一整天的小时数
    private static final int HOURS_IN_ALL_DAY = 24;
    // 一周的天数
    private static final int DAYS_IN_ALL_WEEK = 7;
    // 日期选择器的最小值
    private static final int DATE_SPINNER_MIN_VAL = 0;
    // 日期选择器的最大值
    private static final int DATE_SPINNER_MAX_VAL = DAYS_IN_ALL_WEEK - 1;
    // 24 小时制下小时选择器的最小值
    private static final int HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW = 0;
    // 24 小时制下小时选择器的最大值
    private static final int HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW = 23;
    // 12 小时制下小时选择器的最小值
    private static final int HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW = 1;
    // 12 小时制下小时选择器的最大值
    private static final int HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW = 12;
    // 分钟选择器的最小值
    private static final int MINUT_SPINNER_MIN_VAL = 0;
    // 分钟选择器的最大值
    private static final int MINUT_SPINNER_MAX_VAL = 59;
    // 上午/下午选择器的最小值
    private static final int AMPM_SPINNER_MIN_VAL = 0;
    // 上午/下午选择器的最大值
    private static final int AMPM_SPINNER_MAX_VAL = 1;

    // 日期选择器
    private final NumberPicker mDateSpinner;
    // 小时选择器
    private final NumberPicker mHourSpinner;
    // 分钟选择器
    private final NumberPicker mMinuteSpinner;
    // 上午/下午选择器
    private final NumberPicker mAmPmSpinner;
    // 当前日期和时间的日历对象
    private Calendar mDate;

    // 日期显示值数组
    private String[] mDateDisplayValues = new String[DAYS_IN_ALL_WEEK];

    // 是否为上午
    private boolean mIsAm;

    // 是否为 24 小时制
    private boolean mIs24HourView;

    // 是否启用
    private boolean mIsEnabled = DEFAULT_ENABLE_STATE;

    // 是否正在初始化
    private boolean mInitialising;

    // 日期时间改变监听器
    private OnDateTimeChangedListener mOnDateTimeChangedListener;

    /**
     * 日期选择器值改变监听器
     */
    private NumberPicker.OnValueChangeListener mOnDateChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 根据新值和旧值的差值调整日期
            mDate.add(Calendar.DAY_OF_YEAR, newVal - oldVal);
            // 更新日期选择器的显示
            updateDateControl();
            // 触发日期时间改变事件
            onDateTimeChanged();
        }
    };

    /**
     * 小时选择器值改变监听器
     */
    private NumberPicker.OnValueChangeListener mOnHourChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 标记日期是否改变
            boolean isDateChanged = false;
            // 创建一个新的日历对象
            Calendar cal = Calendar.getInstance();
            if (!mIs24HourView) {
                // 12 小时制下的逻辑
                if (!mIsAm && oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY) {
                    // 从下午 11 点到上午 12 点，日期加一天
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    isDateChanged = true;
                } else if (mIsAm && oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                    // 从上午 12 点到下午 11 点，日期减一天
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    isDateChanged = true;
                }
                if (oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY ||
                        oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                    // 切换上午/下午状态
                    mIsAm = !mIsAm;
                    // 更新上午/下午选择器的显示
                    updateAmPmControl();
                }
            } else {
                // 24 小时制下的逻辑
                if (oldVal == HOURS_IN_ALL_DAY - 1 && newVal == 0) {
                    // 从 23 点到 0 点，日期加一天
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    isDateChanged = true;
                } else if (oldVal == 0 && newVal == HOURS_IN_ALL_DAY - 1) {
                    // 从 0 点到 23 点，日期减一天
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    isDateChanged = true;
                }
            }
            // 计算新的小时值
            int newHour = mHourSpinner.getValue() % HOURS_IN_HALF_DAY + (mIsAm ? 0 : HOURS_IN_HALF_DAY);
            // 设置新的小时值到日历对象
            mDate.set(Calendar.HOUR_OF_DAY, newHour);
            // 触发日期时间改变事件
            onDateTimeChanged();
            if (isDateChanged) {
                // 如果日期改变，更新年、月、日
                setCurrentYear(cal.get(Calendar.YEAR));
                setCurrentMonth(cal.get(Calendar.MONTH));
                setCurrentDay(cal.get(Calendar.DAY_OF_MONTH));
            }
        }
    };

    /**
     * 分钟选择器值改变监听器
     */
    private NumberPicker.OnValueChangeListener mOnMinuteChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 获取分钟选择器的最小值
            int minValue = mMinuteSpinner.getMinValue();
            // 获取分钟选择器的最大值
            int maxValue = mMinuteSpinner.getMaxValue();
            // 偏移量
            int offset = 0;
            if (oldVal == maxValue && newVal == minValue) {
                // 从 59 分钟到 0 分钟，小时加一
                offset += 1;
            } else if (oldVal == minValue && newVal == maxValue) {
                // 从 0 分钟到 59 分钟，小时减一
                offset -= 1;
            }
            if (offset != 0) {
                // 如果偏移量不为 0，调整小时数
                mDate.add(Calendar.HOUR_OF_DAY, offset);
                // 更新小时选择器的值
                mHourSpinner.setValue(getCurrentHour());
                // 更新日期选择器的显示
                updateDateControl();
                // 获取当前小时数
                int newHour = getCurrentHourOfDay();
                if (newHour >= HOURS_IN_HALF_DAY) {
                    // 如果小时数大于等于 12，设置为下午
                    mIsAm = false;
                    // 更新上午/下午选择器的显示
                    updateAmPmControl();
                } else {
                    // 如果小时数小于 12，设置为上午
                    mIsAm = true;
                    // 更新上午/下午选择器的显示
                    updateAmPmControl();
                }
            }
            // 设置新的分钟值到日历对象
            mDate.set(Calendar.MINUTE, newVal);
            // 触发日期时间改变事件
            onDateTimeChanged();
        }
    };

    /**
     * 上午/下午选择器值改变监听器
     */
    private NumberPicker.OnValueChangeListener mOnAmPmChangedListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 切换上午/下午状态
            mIsAm = !mIsAm;
            if (mIsAm) {
                // 如果切换到上午，小时数减 12
                mDate.add(Calendar.HOUR_OF_DAY, -HOURS_IN_HALF_DAY);
            } else {
                // 如果切换到下午，小时数加 12
                mDate.add(Calendar.HOUR_OF_DAY, HOURS_IN_HALF_DAY);
            }
            // 更新上午/下午选择器的显示
            updateAmPmControl();
            // 触发日期时间改变事件
            onDateTimeChanged();
        }
    };

    /**
     * 日期时间改变监听器接口
     */
    public interface OnDateTimeChangedListener {
        /**
         * 当日期时间改变时调用
         * @param view 日期时间选择器视图
         * @param year 年份
         * @param month 月份
         * @param dayOfMonth 日期
         * @param hourOfDay 小时
         * @param minute 分钟
         */
        void onDateTimeChanged(DateTimePicker view, int year, int month,
                int dayOfMonth, int hourOfDay, int minute);
    }

    /**
     * 构造函数，使用当前时间初始化
     * @param context 上下文对象
     */
    public DateTimePicker(Context context) {
        this(context, System.currentTimeMillis());
    }

    /**
     * 构造函数，使用指定时间初始化
     * @param context 上下文对象
     * @param date 日期时间的毫秒值
     */
    public DateTimePicker(Context context, long date) {
        this(context, date, DateFormat.is24HourFormat(context));
    }

    /**
     * 构造函数，使用指定时间和是否为 24 小时制初始化
     * @param context 上下文对象
     * @param date 日期时间的毫秒值
     * @param is24HourView 是否为 24 小时制
     */
    public DateTimePicker(Context context, long date, boolean is24HourView) {
        // 调用父类构造函数
        super(context);
        // 初始化日历对象
        mDate = Calendar.getInstance();
        // 标记为正在初始化
        mInitialising = true;
        // 判断当前是否为上午
        mIsAm = getCurrentHourOfDay() >= HOURS_IN_HALF_DAY;
        // 加载布局文件
        inflate(context, R.layout.datetime_picker, this);

        // 初始化日期选择器
        mDateSpinner = (NumberPicker) findViewById(R.id.date);
        mDateSpinner.setMinValue(DATE_SPINNER_MIN_VAL);
        mDateSpinner.setMaxValue(DATE_SPINNER_MAX_VAL);
        mDateSpinner.setOnValueChangedListener(mOnDateChangedListener);

        // 初始化小时选择器
        mHourSpinner = (NumberPicker) findViewById(R.id.hour);
        mHourSpinner.setOnValueChangedListener(mOnHourChangedListener);
        // 初始化分钟选择器
        mMinuteSpinner =  (NumberPicker) findViewById(R.id.minute);
        mMinuteSpinner.setMinValue(MINUT_SPINNER_MIN_VAL);
        mMinuteSpinner.setMaxValue(MINUT_SPINNER_MAX_VAL);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setOnValueChangedListener(mOnMinuteChangedListener);

        // 获取上午/下午的显示字符串
        String[] stringsForAmPm = new DateFormatSymbols().getAmPmStrings();
        // 初始化上午/下午选择器
        mAmPmSpinner = (NumberPicker) findViewById(R.id.amPm);
        mAmPmSpinner.setMinValue(AMPM_SPINNER_MIN_VAL);
        mAmPmSpinner.setMaxValue(AMPM_SPINNER_MAX_VAL);
        mAmPmSpinner.setDisplayedValues(stringsForAmPm);
        mAmPmSpinner.setOnValueChangedListener(mOnAmPmChangedListener);

        // 更新日期选择器的显示
        updateDateControl();
        // 更新小时选择器的显示
        updateHourControl();
        // 更新上午/下午选择器的显示
        updateAmPmControl();

        // 设置是否为 24 小时制
        set24HourView(is24HourView);

        // 设置当前日期和时间
        setCurrentDate(date);

        // 设置启用状态
        setEnabled(isEnabled());

        // 设置内容描述
        mInitialising = false;
    }

    /**
     * 设置启用状态
     * @param enabled 是否启用
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        // 调用父类的设置启用状态方法
        super.setEnabled(enabled);
        // 设置日期选择器的启用状态
        mDateSpinner.setEnabled(enabled);
        // 设置分钟选择器的启用状态
        mMinuteSpinner.setEnabled(enabled);
        // 设置小时选择器的启用状态
        mHourSpinner.setEnabled(enabled);
        // 设置上午/下午选择器的启用状态
        mAmPmSpinner.setEnabled(enabled);
        // 更新启用状态标志
        mIsEnabled = enabled;
    }

    /**
     * 获取启用状态
     * @return 是否启用
     */
    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * 获取当前日期和时间的毫秒值
     * @return 当前日期和时间的毫秒值
     */
    public long getCurrentDateInTimeMillis() {
        return mDate.getTimeInMillis();
    }

    /**
     * 设置当前日期和时间
     * @param date 当前日期和时间的毫秒值
     */
    public void setCurrentDate(long date) {
        // 创建一个新的日历对象
        Calendar cal = Calendar.getInstance();
        // 设置日历对象的时间
        cal.setTimeInMillis(date);
        // 设置当前日期和时间的年、月、日、小时、分钟
        setCurrentDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    /**
     * 设置当前日期和时间
     * @param year 年份
     * @param month 月份
     * @param dayOfMonth 日期
     * @param hourOfDay 小时
     * @param minute 分钟
     */
    public void setCurrentDate(int year, int month,
            int dayOfMonth, int hourOfDay, int minute) {
        // 设置当前年份
        setCurrentYear(year);
        // 设置当前月份
        setCurrentMonth(month);
        // 设置当前日期
        setCurrentDay(dayOfMonth);
        // 设置当前小时
        setCurrentHour(hourOfDay);
        // 设置当前分钟
        setCurrentMinute(minute);
    }

    /**
     * 获取当前年份
     * @return 当前年份
     */
    public int getCurrentYear() {
        return mDate.get(Calendar.YEAR);
    }

    /**
     * 设置当前年份
     * @param year 当前年份
     */
    public void setCurrentYear(int year) {
        if (!mInitialising && year == getCurrentYear()) {
            return;
        }
        // 设置日历对象的年份
        mDate.set(Calendar.YEAR, year);
        // 更新日期选择器的显示
        updateDateControl();
        // 触发日期时间改变事件
        onDateTimeChanged();
    }

    /**
     * 获取当前月份
     * @return 当前月份
     */
    public int getCurrentMonth() {
        return mDate.get(Calendar.MONTH);
    }

    /**
     * 设置当前月份
     * @param month 当前月份
     */
    public void setCurrentMonth(int month) {
        if (!mInitialising && month == getCurrentMonth()) {
            return;
        }
        // 设置日历对象的月份
        mDate.set(Calendar.MONTH, month);
        // 更新日期选择器的显示
        updateDateControl();
        // 触发日期时间改变事件
        onDateTimeChanged();
    }

    /**
     * 获取当前日期
     * @return 当前日期
     */
    public int getCurrentDay() {
        return mDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 设置当前日期
     * @param dayOfMonth 当前日期
     */
    public void setCurrentDay(int dayOfMonth) {
        if (!mInitialising && dayOfMonth == getCurrentDay()) {
            return;
        }
        // 设置日历对象的日期
        mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        // 更新日期选择器的显示
        updateDateControl();
        // 触发日期时间改变事件
        onDateTimeChanged();
    }

    /**
     * 获取当前小时（24 小时制）
     * @return 当前小时（24 小时制）
     */
    public int getCurrentHourOfDay() {
        return mDate.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取当前小时（根据是否为 24 小时制）
     * @return 当前小时
     */
    private int getCurrentHour() {
        if (mIs24HourView){
            // 24 小时制下直接返回当前小时
            return getCurrentHourOfDay();
        } else {
            // 12 小时制下的逻辑
            int hour = getCurrentHourOfDay();
            if (hour > HOURS_IN_HALF_DAY) {
                // 如果小时数大于 12，减去 12
                return hour - HOURS_IN_HALF_DAY;
            } else {
                // 如果小时数为 0，返回 12，否则返回小时数
                return hour == 0 ? HOURS_IN_HALF_DAY : hour;
            }
        }
    }

    /**
     * 设置当前小时（24 小时制）
     * @param hourOfDay 当前小时（24 小时制）
     */
    public void setCurrentHour(int hourOfDay) {
        if (!mInitialising && hourOfDay == getCurrentHourOfDay()) {
            return;
        }
        // 设置日历对象的小时数
        mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        if (!mIs24HourView) {
            if (hourOfDay >= HOURS_IN_HALF_DAY) {
                // 如果小时数大于等于 12，设置为下午
                mIsAm = false;
                if (hourOfDay > HOURS_IN_HALF_DAY) {
                    // 如果小时数大于 12，减去 12
                    hourOfDay -= HOURS_IN_HALF_DAY;
                }
            } else {
                // 如果小时数小于 12，设置为上午
                mIsAm = true;
                if (hourOfDay == 0) {
                    // 如果小时数为 0，设置为 12
                    hourOfDay = HOURS_IN_HALF_DAY;
                }
            }
            // 更新上午/下午选择器的显示
            updateAmPmControl();
        }
        // 设置小时选择器的值
        mHourSpinner.setValue(hourOfDay);
        // 触发日期时间改变事件
        onDateTimeChanged();
    }

    /**
     * 获取当前分钟
     * @return 当前分钟
     */
    public int getCurrentMinute() {
        return mDate.get(Calendar.MINUTE);
    }

    /**
     * 设置当前分钟
     * @param minute 当前分钟
     */
    public void setCurrentMinute(int minute) {
        if (!mInitialising && minute == getCurrentMinute()) {
            return;
        }
        // 设置分钟选择器的值
        mMinuteSpinner.setValue(minute);
        // 设置日历对象的分钟数
        mDate.set(Calendar.MINUTE, minute);
        // 触发日期时间改变事件
        onDateTimeChanged();
    }

    /**
     * 判断是否为 24 小时制
     * @return 是否为 24 小时制
     */
    public boolean is24HourView () {
        return mIs24HourView;
    }

    /**
     * 设置是否为 24 小时制
     * @param is24HourView 是否为 24 小时制
     */
    public void set24HourView(boolean is24HourView) {
        if (mIs24HourView == is24HourView) {
            return;
        }
        // 更新 24 小时制标志
        mIs24HourView = is24HourView;
        // 根据是否为 24 小时制显示或隐藏上午/下午选择器
        mAmPmSpinner.setVisibility(is24HourView ? View.GONE : View.VISIBLE);
        // 获取当前小时数
        int hour = getCurrentHourOfDay();
        // 更新小时选择器的显示
        updateHourControl();
        // 设置当前小时数
        setCurrentHour(hour);
        // 更新上午/下午选择器的显示
        updateAmPmControl();
    }

    /**
     * 更新日期选择器的显示
     */
    private void updateDateControl() {
        // 创建一个新的日历对象
        Calendar cal = Calendar.getInstance();
        // 设置日历对象的时间
        cal.setTimeInMillis(mDate.getTimeInMillis());
        // 日期减去一周的一半加一天
        cal.add(Calendar.DAY_OF_YEAR, -DAYS_IN_ALL_WEEK / 2 - 1);
        // 清空日期选择器的显示值
        mDateSpinner.setDisplayedValues(null);
        for (int i = 0; i < DAYS_IN_ALL_WEEK; ++i) {
            // 日期加一天
            cal.add(Calendar.DAY_OF_YEAR, 1);
            // 格式化日期并存储到显示值数组中
            mDateDisplayValues[i] = (String) DateFormat.format("MM.dd EEEE", cal);
        }
        // 设置日期选择器的显示值
        mDateSpinner.setDisplayedValues(mDateDisplayValues);
        // 设置日期选择器的当前值
        mDateSpinner.setValue(DAYS_IN_ALL_WEEK / 2);
        // 使日期选择器无效，触发重绘
        mDateSpinner.invalidate();
    }

    /**
     * 更新上午/下午选择器的显示
     */
    private void updateAmPmControl() {
        if (mIs24HourView) {
            // 24 小时制下隐藏上午/下午选择器
            mAmPmSpinner.setVisibility(View.GONE);
        } else {
            // 12 小时制下设置上午/下午选择器的值
            int index = mIsAm ? Calendar.AM : Calendar.PM;
            mAmPmSpinner.setValue(index);
            // 显示上午/下午选择器
            mAmPmSpinner.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 更新小时选择器的显示
     */
    private void updateHourControl() {
        if (mIs24HourView) {
            // 24 小时制下设置小时选择器的范围
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW);
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW);
        } else {
            // 12 小时制下设置小时选择器的范围
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW);
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW);
        }
    }

    /**
     * 设置日期时间改变监听器
     * @param callback 日期时间改变监听器
     */
    public void setOnDateTimeChangedListener(OnDateTimeChangedListener callback) {
        mOnDateTimeChangedListener = callback;
    }

    /**
     * 触发日期时间改变事件
     */
    private void onDateTimeChanged() {
        if (mOnDateTimeChangedListener != null) {
            // 调用监听器的日期时间改变方法
            mOnDateTimeChangedListener.onDateTimeChanged(this, getCurrentYear(),
                    getCurrentMonth(), getCurrentDay(), getCurrentHourOfDay(), getCurrentMinute());
        }
    }
}
