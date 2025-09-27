package com.ynet.mgmt.common.exception;

/**
 * 服务异常
 * 用于服务层抛出的业务相关异常
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
public class ServiceException extends RuntimeException {

    /**
     * 构造函数
     *
     * @param message 异常消息
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     *
     * @param cause 异常原因
     */
    public ServiceException(Throwable cause) {
        super(cause);
    }
}