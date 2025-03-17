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

// 声明该类所在的包，表明这是一个用于处理 Google 任务操作失败异常的类
package net.micode.notes.gtask.exception;

/**
 * ActionFailureException 类用于表示在 Google 任务操作过程中发生的失败异常。
 * 该类继承自 RuntimeException，意味着它是一个未检查异常，调用者不需要显式捕获。
 */
public class ActionFailureException extends RuntimeException {
    // 序列化版本号，用于确保在反序列化时类的版本一致性
    private static final long serialVersionUID = 4425249765923293627L;

    /**
     * 默认构造函数，创建一个不包含错误消息和原因的异常实例。
     */
    public ActionFailureException() {
        // 调用父类 RuntimeException 的默认构造函数
        super();
    }

    /**
     * 带有错误消息的构造函数，创建一个包含指定错误消息的异常实例。
     *
     * @param paramString 描述异常的详细信息，用于在日志或调试时提供更多上下文。
     */
    public ActionFailureException(String paramString) {
        // 调用父类 RuntimeException 的构造函数，并传入错误消息
        super(paramString);
    }

    /**
     * 带有错误消息和原因的构造函数，创建一个包含指定错误消息和引发异常的原因的异常实例。
     *
     * @param paramString 描述异常的详细信息，用于在日志或调试时提供更多上下文。
     * @param paramThrowable 引发此异常的原始异常，方便追踪异常的根源。
     */
    public ActionFailureException(String paramString, Throwable paramThrowable) {
        // 调用父类 RuntimeException 的构造函数，并传入错误消息和原因
        super(paramString, paramThrowable);
    }
}
