package com.ynet.mgmt.sensitiveWord.exception;

/**
 * 重复敏感词异常
 * 当尝试创建已存在的敏感词时抛出此异常
 *
 * @author system
 * @since 1.0.0
 */
public class DuplicateSensitiveWordException extends RuntimeException {

    /**
     * 默认构造函数
     */
    public DuplicateSensitiveWordException() {
        super("敏感词已存在");
    }

    /**
     * 带消息的构造函数
     *
     * @param message 异常消息
     */
    public DuplicateSensitiveWordException(String message) {
        super(message);
    }

    /**
     * 根据名称构造异常
     *
     * @param name 敏感词名称
     * @return 异常实例
     */
    public static DuplicateSensitiveWordException byName(String name) {
        return new DuplicateSensitiveWordException(String.format("敏感词已存在: name=%s", name));
    }

    /**
     * 带消息和原因的构造函数
     *
     * @param message 异常消息
     * @param cause 原因
     */
    public DuplicateSensitiveWordException(String message, Throwable cause) {
        super(message, cause);
    }
}