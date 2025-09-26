package com.ynet.mgmt.channel.exception;

import com.ynet.mgmt.common.exception.EntityNotFoundException;

/**
 * 渠道未找到异常
 * 当查询的渠道不存在时抛出
 *
 * @author system
 * @since 1.0.0
 */
public class ChannelNotFoundException extends EntityNotFoundException {

    public ChannelNotFoundException(String message) {
        super(message);
    }

    public ChannelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelNotFoundException(Long channelId) {
        super("渠道不存在: ID=" + channelId);
    }

    public ChannelNotFoundException(String field, String value) {
        super("渠道不存在: " + field + "=" + value);
    }
}