package com.ynet.mgmt.channel.exception;

import com.ynet.mgmt.common.exception.ValidationException;

/**
 * 渠道验证异常
 * 当渠道数据验证失败时抛出
 *
 * @author system
 * @since 1.0.0
 */
public class ChannelValidationException extends ValidationException {

    public ChannelValidationException(String message) {
        super(message);
    }

    public ChannelValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}