package com.ynet.mgmt.channel.exception;

import com.ynet.mgmt.common.exception.BusinessException;

/**
 * 渠道重复异常
 * 当试图创建重复的渠道时抛出
 *
 * @author system
 * @since 1.0.0
 */
public class DuplicateChannelException extends BusinessException {

    public DuplicateChannelException(String message) {
        super("DUPLICATE_CHANNEL", message);
    }

    public DuplicateChannelException(String message, Throwable cause) {
        super("DUPLICATE_CHANNEL", message, cause);
    }

    public DuplicateChannelException(String field, String value) {
        super("DUPLICATE_CHANNEL", "渠道" + field + "已存在: " + value);
    }
}