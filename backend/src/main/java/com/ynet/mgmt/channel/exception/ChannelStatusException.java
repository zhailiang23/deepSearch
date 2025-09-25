package com.ynet.mgmt.channel.exception;

import com.ynet.mgmt.common.exception.BusinessException;

/**
 * 渠道状态异常
 * 当渠道状态操作不合法时抛出
 *
 * @author system
 * @since 1.0.0
 */
public class ChannelStatusException extends BusinessException {

    public ChannelStatusException(String message) {
        super("CHANNEL_STATUS_ERROR", message);
    }

    public ChannelStatusException(String message, Throwable cause) {
        super("CHANNEL_STATUS_ERROR", message, cause);
    }
}