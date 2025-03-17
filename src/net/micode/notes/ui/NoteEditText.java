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

// 导入 Android 上下文类，用于获取系统服务和资源
import android.content.Context;
// 导入 Android 矩形类，用于表示矩形区域
import android.graphics.Rect;
// 导入 Android 文本布局类，用于处理文本的布局
import android.text.Layout;
// 导入 Android 文本选择类，用于操作文本的选择范围
import android.text.Selection;
// 导入 Android 带样式的文本类，用于处理带样式的文本
import android.text.Spanned;
// 导入 Android 文本工具类，提供一些文本处理的实用方法
import android.text.TextUtils;
// 导入 Android 文本样式类，用于表示 URL 样式
import android.text.style.URLSpan;
// 导入 Android 属性集类，用于获取视图的属性
import android.util.AttributeSet;
// 导入 Android 日志类，用于记录日志信息
import android.util.Log;
// 导入 Android 上下文菜单类，用于创建上下文菜单
import android.view.ContextMenu;
// 导入 Android 按键事件类，用于处理按键事件
import android.view.KeyEvent;
// 导入 Android 菜单项类，用于表示菜单中的单个项目
import android.view.MenuItem;
// 导入 Android 菜单项点击监听器接口，用于处理菜单项的点击事件
import android.view.MenuItem.OnMenuItemClickListener;
// 导入 Android 触摸事件类，用于处理触摸事件
import android.view.MotionEvent;
// 导入 Android 编辑文本类，用于创建可编辑的文本框
import android.widget.EditText;

// 导入应用的资源类，用于访问应用的资源，如字符串、布局等
import net.micode.notes.R;

// 导入 Java 中的哈希映射类，用于存储键值对
import java.util.HashMap;
// 导入 Java 中的映射接口，定义了映射的基本操作
import java.util.Map;

/**
 * NoteEditText 类继承自 EditText，用于创建一个自定义的可编辑文本框。
 * 该类处理了文本的触摸事件、按键事件、焦点变化事件和上下文菜单事件，
 * 并提供了文本变化的回调接口，方便外部处理文本的删除、添加和变化。
 */
public class NoteEditText extends EditText {
    // 定义日志标签，用于在日志中标识该类的日志信息
    private static final String TAG = "NoteEditText";
    // 记录当前编辑文本框的索引
    private int mIndex;
    // 记录删除操作前的文本选择起始位置
    private int mSelectionStartBeforeDelete;

    // 定义电话链接的协议前缀
    private static final String SCHEME_TEL = "tel:" ;
    // 定义 HTTP 链接的协议前缀
    private static final String SCHEME_HTTP = "http:" ;
    // 定义邮件链接的协议前缀
    private static final String SCHEME_EMAIL = "mailto:" ;

    // 定义一个静态的映射，用于存储协议前缀和对应的资源 ID
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        // 将电话链接协议前缀和对应的资源 ID 存入映射
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        // 将 HTTP 链接协议前缀和对应的资源 ID 存入映射
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        // 将邮件链接协议前缀和对应的资源 ID 存入映射
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
    }

    /**
     * 定义一个内部接口，用于在文本发生变化时回调。
     * 该接口包含三个方法，分别处理文本的删除、添加和变化事件。
     */
    public interface OnTextViewChangeListener {
        /**
         * 当按下删除键（{@link KeyEvent#KEYCODE_DEL}）且文本为空时，删除当前编辑文本框。
         *
         * @param index 当前编辑文本框的索引
         * @param text  当前编辑文本框的文本内容
         */
        void onEditTextDelete(int index, String text);

        /**
         * 当按下回车键（{@link KeyEvent#KEYCODE_ENTER}）时，在当前编辑文本框后添加一个新的编辑文本框。
         *
         * @param index 当前编辑文本框的索引
         * @param text  当前编辑文本框的文本内容
         */
        void onEditTextEnter(int index, String text);

        /**
         * 当文本发生变化时，隐藏或显示菜单项选项。
         *
         * @param index 当前编辑文本框的索引
         * @param hasText 文本是否有内容
         */
        void onTextChange(int index, boolean hasText);
    }

    // 文本变化的回调监听器
    private OnTextViewChangeListener mOnTextViewChangeListener;

    /**
     * 构造函数，用于创建 NoteEditText 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     */
    public NoteEditText(Context context) {
        // 调用父类的构造函数
        super(context, null);
        // 初始化索引为 0
        mIndex = 0;
    }

    /**
     * 设置当前编辑文本框的索引。
     *
     * @param index 要设置的索引值
     */
    public void setIndex(int index) {
        mIndex = index;
    }

    /**
     * 设置文本变化的回调监听器。
     *
     * @param listener 实现了 OnTextViewChangeListener 接口的监听器对象
     */
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }

    /**
     * 构造函数，用于创建 NoteEditText 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param attrs 视图的属性集
     */
    public NoteEditText(Context context, AttributeSet attrs) {
        // 调用父类的构造函数，使用默认的编辑文本样式
        super(context, attrs, android.R.attr.editTextStyle);
    }

    /**
     * 构造函数，用于创建 NoteEditText 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param attrs 视图的属性集
     * @param defStyle 视图的默认样式
     */
    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        // 调用父类的构造函数
        super(context, attrs, defStyle);
        // TODO: 这里是自动生成的构造函数代码，可能需要根据实际需求进行完善
    }

    /**
     * 处理触摸事件的回调方法。
     * 当用户触摸编辑文本框时，根据触摸位置设置文本的选择范围。
     *
     * @param event 触摸事件对象
     * @return 是否处理了该事件
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 根据触摸事件的动作类型进行处理
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 获取触摸点的 x 坐标
                int x = (int) event.getX();
                // 获取触摸点的 y 坐标
                int y = (int) event.getY();
                // 减去左内边距，得到相对于文本内容的 x 坐标
                x -= getTotalPaddingLeft();
                // 减去上内边距，得到相对于文本内容的 y 坐标
                y -= getTotalPaddingTop();
                // 加上滚动的 x 偏移量，得到实际的 x 坐标
                x += getScrollX();
                // 加上滚动的 y 偏移量，得到实际的 y 坐标
                y += getScrollY();

                // 获取文本的布局对象
                Layout layout = getLayout();
                // 根据 y 坐标获取触摸点所在的行
                int line = layout.getLineForVertical(y);
                // 根据行和 x 坐标获取触摸点在文本中的偏移量
                int off = layout.getOffsetForHorizontal(line, x);
                // 设置文本的选择范围为触摸点的偏移量
                Selection.setSelection(getText(), off);
                break;
        }

        // 调用父类的触摸事件处理方法
        return super.onTouchEvent(event);
    }

    /**
     * 处理按键按下事件的回调方法。
     * 当用户按下按键时，根据按键类型进行相应的处理。
     *
     * @param keyCode 按键的代码
     * @param event 按键事件对象
     * @return 是否处理了该事件
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 根据按键代码进行处理
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                // 如果设置了文本变化的回调监听器
                if (mOnTextViewChangeListener != null) {
                    // 返回 false，表示不处理该事件
                    return false;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                // 记录删除操作前的文本选择起始位置
                mSelectionStartBeforeDelete = getSelectionStart();
                break;
            default:
                break;
        }
        // 调用父类的按键按下事件处理方法
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 处理按键抬起事件的回调方法。
     * 当用户抬起按键时，根据按键类型进行相应的处理。
     *
     * @param keyCode 按键的代码
     * @param event 按键事件对象
     * @return 是否处理了该事件
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // 根据按键代码进行处理
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL:
                // 如果设置了文本变化的回调监听器
                if (mOnTextViewChangeListener != null) {
                    // 如果删除操作前的选择起始位置为 0 且当前索引不为 0
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        // 调用回调监听器的文本删除方法
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        // 返回 true，表示处理了该事件
                        return true;
                    }
                } else {
                    // 记录日志，提示未设置文本变化的回调监听器
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                // 如果设置了文本变化的回调监听器
                if (mOnTextViewChangeListener != null) {
                    // 获取当前文本的选择起始位置
                    int selectionStart = getSelectionStart();
                    // 获取从选择起始位置到文本末尾的内容
                    String text = getText().subSequence(selectionStart, length()).toString();
                    // 设置文本为从开头到选择起始位置的内容
                    setText(getText().subSequence(0, selectionStart));
                    // 调用回调监听器的文本添加方法
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                } else {
                    // 记录日志，提示未设置文本变化的回调监听器
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            default:
                break;
        }
        // 调用父类的按键抬起事件处理方法
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 处理焦点变化事件的回调方法。
     * 当编辑文本框的焦点发生变化时，根据焦点状态和文本内容调用回调监听器。
     *
     * @param focused 是否获得焦点
     * @param direction 焦点变化的方向
     * @param previouslyFocusedRect 之前聚焦的矩形区域
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // 如果设置了文本变化的回调监听器
        if (mOnTextViewChangeListener != null) {
            // 如果失去焦点且文本为空
            if (!focused && TextUtils.isEmpty(getText())) {
                // 调用回调监听器的文本变化方法，提示文本无内容
                mOnTextViewChangeListener.onTextChange(mIndex, false);
            } else {
                // 调用回调监听器的文本变化方法，提示文本有内容
                mOnTextViewChangeListener.onTextChange(mIndex, true);
            }
        }
        // 调用父类的焦点变化事件处理方法
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * 创建上下文菜单的回调方法。
     * 当用户长按编辑文本框时，根据选中的 URL 链接添加相应的菜单项。
     *
     * @param menu 上下文菜单对象
     */
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        // 检查文本是否为带样式的文本
        if (getText() instanceof Spanned) {
            // 获取当前文本的选择起始位置
            int selStart = getSelectionStart();
            // 获取当前文本的选择结束位置
            int selEnd = getSelectionEnd();

            // 获取选择范围的最小值
            int min = Math.min(selStart, selEnd);
            // 获取选择范围的最大值
            int max = Math.max(selStart, selEnd);

            // 获取选择范围内的所有 URL 样式
            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            // 如果选择范围内只有一个 URL 样式
            if (urls.length == 1) {
                // 初始化默认的资源 ID 为 0
                int defaultResId = 0;
                // 遍历协议前缀映射
                for(String schema: sSchemaActionResMap.keySet()) {
                    // 如果 URL 链接包含当前协议前缀
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        // 获取对应的资源 ID
                        defaultResId = sSchemaActionResMap.get(schema);
                        // 跳出循环
                        break;
                    }
                }

                // 如果未找到匹配的资源 ID
                if (defaultResId == 0) {
                    // 使用默认的其他链接资源 ID
                    defaultResId = R.string.note_link_other;
                }

                // 在上下文菜单中添加一个菜单项，设置标题为资源 ID 对应的字符串
                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            /**
                             * 处理菜单项点击事件的回调方法。
                             * 当用户点击菜单项时，打开对应的 URL 链接。
                             *
                             * @param item 被点击的菜单项
                             * @return 是否处理了该事件
                             */
                            public boolean onMenuItemClick(MenuItem item) {
                                // 调用 URL 样式的点击方法，打开链接
                                urls[0].onClick(NoteEditText.this);
                                // 返回 true，表示处理了该事件
                                return true;
                            }
                        });
            }
        }
        // 调用父类的上下文菜单创建方法
        super.onCreateContextMenu(menu);
    }
}
