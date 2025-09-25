package com.ynet.mgmt.channel.constants;

/**
 *  Sïã8Ï
 *
 * @author system
 * @since 1.0.0
 */
public final class ErrorCode {

    private ErrorCode() {}

    //  Sú@ï
    public static final String CHANNEL_NOT_FOUND = "CHANNEL_NOT_FOUND";
    public static final String CHANNEL_CODE_EXISTS = "CHANNEL_CODE_EXISTS";
    public static final String CHANNEL_NAME_EXISTS = "CHANNEL_NAME_EXISTS";
    public static final String CHANNEL_INVALID_STATUS = "CHANNEL_INVALID_STATUS";

    //  S¶øsï
    public static final String CHANNEL_ALREADY_ACTIVE = "CHANNEL_ALREADY_ACTIVE";
    public static final String CHANNEL_ALREADY_INACTIVE = "CHANNEL_ALREADY_INACTIVE";
    public static final String CHANNEL_ALREADY_SUSPENDED = "CHANNEL_ALREADY_SUSPENDED";
    public static final String CHANNEL_ALREADY_DELETED = "CHANNEL_ALREADY_DELETED";
    public static final String CHANNEL_STATUS_OPERATION_FAILED = "CHANNEL_STATUS_OPERATION_FAILED";

    //  S¡Äï
    public static final String CHANNEL_HAS_ACTIVE_ORDERS = "CHANNEL_HAS_ACTIVE_ORDERS";
    public static final String CHANNEL_CANNOT_DELETE_WITH_SALES = "CHANNEL_CANNOT_DELETE_WITH_SALES";
    public static final String CHANNEL_COMMISSION_RATE_INVALID = "CHANNEL_COMMISSION_RATE_INVALID";

    // ŒÁøsï
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
    public static final String INVALID_CHANNEL_TYPE = "INVALID_CHANNEL_TYPE";
}