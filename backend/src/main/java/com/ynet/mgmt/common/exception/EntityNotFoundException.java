package com.ynet.mgmt.common.exception;

/**
 * 实体未找到异常
 *
 * @author system
 * @since 1.0.0
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}