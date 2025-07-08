package org.example;

/**
 * 自定义异常，专门用于表示在反编译过程中发生的错误。
 */
public class DecompilationException extends Exception {

    /**
     * 构造函数
     * @param message 描述失败原因的详细消息
     * @param cause   原始异常
     */
    public DecompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     * @param message 详细消息
     */
    public DecompilationException(String message) {
        super(message);
    }
}