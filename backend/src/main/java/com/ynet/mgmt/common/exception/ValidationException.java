package com.ynet.mgmt.common.exception;

/**
 * 数据验证异常
 *
 * @author system
 * @since 1.0.0
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}