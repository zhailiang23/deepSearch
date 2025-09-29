package com.ynet.mgmt.hotTopic.exception;

import com.ynet.mgmt.common.exception.BusinessException;

/**
 * 重复热门话题异常
 * 当创建或更新话题时名称已存在时抛出
 *
 * @author system
 * @since 1.0.0
 */
public class DuplicateHotTopicException extends BusinessException {

    public DuplicateHotTopicException(String message) {
        super("DUPLICATE_HOT_TOPIC", message);
    }

    public DuplicateHotTopicException(String message, Throwable cause) {
        super("DUPLICATE_HOT_TOPIC", message, cause);
    }

    public DuplicateHotTopicException(String field, String value) {
        super("DUPLICATE_HOT_TOPIC", "热门话题" + field + "已存在: " + value);
    }
}