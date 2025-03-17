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
// 导入 Android 菜单类，用于创建和管理菜单
import android.view.Menu;
// 导入 Android 菜单项类，用于表示菜单中的单个项目
import android.view.MenuItem;
// 导入 Android 视图类，是所有 UI 组件的基类
import android.view.View;
// 导入 Android 视图点击监听器接口，用于处理视图的点击事件
import android.view.View.OnClickListener;
// 导入 Android 按钮类，用于创建可点击的按钮
import android.widget.Button;
// 导入 Android 弹出菜单类，用于创建下拉菜单
import android.widget.PopupMenu;
// 导入 Android 弹出菜单项点击监听器接口，用于处理菜单项的点击事件
import android.widget.PopupMenu.OnMenuItemClickListener;

// 导入应用的资源类，用于访问应用的资源，如字符串、布局等
import net.micode.notes.R;

/**
 * DropdownMenu 类用于创建一个下拉菜单，通过点击按钮来显示。
 * 该类封装了 PopupMenu 的使用，提供了设置菜单项点击监听器、查找菜单项和设置按钮标题等功能。
 */
public class DropdownMenu {
    // 用于触发下拉菜单显示的按钮
    private Button mButton;
    // 下拉菜单的 PopupMenu 实例
    private PopupMenu mPopupMenu;
    // 下拉菜单的 Menu 实例，用于管理菜单项
    private Menu mMenu;

    /**
     * 构造函数，用于创建 DropdownMenu 实例。
     *
     * @param context 上下文对象，用于获取系统服务和资源
     * @param button  用于触发下拉菜单显示的按钮
     * @param menuId  菜单布局的资源 ID
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        // 保存传入的按钮实例
        mButton = button;
        // 设置按钮的背景资源为下拉图标
        mButton.setBackgroundResource(R.drawable.dropdown_icon);
        // 创建一个新的 PopupMenu 实例，关联到传入的按钮
        mPopupMenu = new PopupMenu(context, mButton);
        // 获取 PopupMenu 的 Menu 实例
        mMenu = mPopupMenu.getMenu();
        // 使用菜单布局资源 ID 填充菜单
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
        // 为按钮设置点击监听器，当点击按钮时显示下拉菜单
        mButton.setOnClickListener(new OnClickListener() {
            /**
             * 处理按钮点击事件的回调方法。
             * 当按钮被点击时，显示下拉菜单。
             *
             * @param v 被点击的视图对象
             */
            public void onClick(View v) {
                // 显示下拉菜单
                mPopupMenu.show();
            }
        });
    }

    /**
     * 设置下拉菜单项的点击监听器。
     *
     * @param listener 实现了 OnMenuItemClickListener 接口的监听器对象
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        // 检查 PopupMenu 实例是否不为空
        if (mPopupMenu != null) {
            // 为 PopupMenu 设置菜单项点击监听器
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }

    /**
     * 根据菜单项的 ID 查找菜单项。
     *
     * @param id 菜单项的 ID
     * @return 找到的菜单项实例，如果未找到则返回 null
     */
    public MenuItem findItem(int id) {
        // 在菜单中查找指定 ID 的菜单项
        return mMenu.findItem(id);
    }

    /**
     * 设置按钮的标题。
     *
     * @param title 按钮的标题文本
     */
    public void setTitle(CharSequence title) {
        // 设置按钮的文本为传入的标题
        mButton.setText(title);
    }
}
