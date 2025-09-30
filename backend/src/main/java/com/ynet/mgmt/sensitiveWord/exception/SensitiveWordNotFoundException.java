package com.ynet.mgmt.sensitiveWord.exception;

/**
 * 敏感词未找到异常
 * 当尝试访问不存在的敏感词时抛出此异常
 *
 * @author system
 * @since 1.0.0
 */
public class SensitiveWordNotFoundException extends RuntimeException {

    /**
     * 默认构造函数
     */
    public SensitiveWordNotFoundException() {
        super("敏感词未找到");
    }

    /**
     * 带消息的构造函数
     *
     * @param message 异常消息
     */
    public SensitiveWordNotFoundException(String message) {
        super(message);
    }

    /**
     * 根据ID构造异常
     *
     * @param id 敏感词ID
     */
    public SensitiveWordNotFoundException(Long id) {
        super(String.format("敏感词未找到: id=%d", id));
    }

    /**
     * 根据名称构造异常
     *
     * @param name 敏感词名称
     */
    public static SensitiveWordNotFoundException byName(String name) {
        return new SensitiveWordNotFoundException(String.format("敏感词未找到: name=%s", name));
    }

    /**
     * 带消息和原因的构造函数
     *
     * @param message 异常消息
     * @param cause 原因
     */
    public SensitiveWordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}