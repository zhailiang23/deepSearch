package com.ynet.mgmt.channel.constants;

/**
 * 渠道模块错误码常量
 *
 * @author system
 * @since 1.0.0
 */
public final class ErrorCode {

    private ErrorCode() {}

    // 渠道基本错误
    public static final String CHANNEL_NOT_FOUND = "CHANNEL_NOT_FOUND";
    public static final String CHANNEL_CODE_ALREADY_EXISTS = "CHANNEL_CODE_ALREADY_EXISTS";
    public static final String CHANNEL_NAME_ALREADY_EXISTS = "CHANNEL_NAME_ALREADY_EXISTS";
    public static final String CHANNEL_INVALID_STATUS = "CHANNEL_INVALID_STATUS";

    // 渠道状态错误
    public static final String CHANNEL_ALREADY_ACTIVE = "CHANNEL_ALREADY_ACTIVE";
    public static final String CHANNEL_ALREADY_INACTIVE = "CHANNEL_ALREADY_INACTIVE";
    public static final String CHANNEL_ALREADY_SUSPENDED = "CHANNEL_ALREADY_SUSPENDED";
    public static final String CHANNEL_ALREADY_DELETED = "CHANNEL_ALREADY_DELETED";
    public static final String CHANNEL_NOT_SUSPENDED = "CHANNEL_NOT_SUSPENDED";
    public static final String CHANNEL_STATUS_OPERATION_FAILED = "CHANNEL_STATUS_OPERATION_FAILED";

    // 渠道业务错误
    public static final String CHANNEL_HAS_ACTIVE_ORDERS = "CHANNEL_HAS_ACTIVE_ORDERS";
    public static final String CHANNEL_CANNOT_DELETE_WITH_SALES = "CHANNEL_CANNOT_DELETE_WITH_SALES";
    public static final String CHANNEL_COMMISSION_RATE_INVALID = "CHANNEL_COMMISSION_RATE_INVALID";

    // 参数验证错误
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
    public static final String INVALID_CHANNEL_TYPE = "INVALID_CHANNEL_TYPE";
}