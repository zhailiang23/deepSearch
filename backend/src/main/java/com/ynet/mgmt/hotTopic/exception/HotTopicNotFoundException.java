package com.ynet.mgmt.hotTopic.exception;

import com.ynet.mgmt.common.exception.EntityNotFoundException;

/**
 * 热门话题未找到异常
 * 当查询的热门话题不存在时抛出
 *
 * @author system
 * @since 1.0.0
 */
public class HotTopicNotFoundException extends EntityNotFoundException {

    public HotTopicNotFoundException(String message) {
        super(message);
    }

    public HotTopicNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public HotTopicNotFoundException(Long topicId) {
        super("热门话题不存在: ID=" + topicId);
    }

    public HotTopicNotFoundException(String field, String value) {
        super("热门话题不存在: " + field + "=" + value);
    }
}