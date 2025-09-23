package com.ynet.mgmt.searchspace.constants;

/**
 * 搜索空间错误代码常量
 *
 * @author system
 * @since 1.0.0
 */
public final class ErrorCode {

    private ErrorCode() {}

    // 搜索空间相关错误
    public static final String SEARCH_SPACE_NOT_FOUND = "SEARCH_SPACE_NOT_FOUND";
    public static final String SEARCH_SPACE_CODE_EXISTS = "SEARCH_SPACE_CODE_EXISTS";
    public static final String SEARCH_SPACE_NAME_EXISTS = "SEARCH_SPACE_NAME_EXISTS";
    public static final String SEARCH_SPACE_HAS_DOCUMENTS = "SEARCH_SPACE_HAS_DOCUMENTS";
    public static final String SEARCH_SPACE_INVALID_STATUS = "SEARCH_SPACE_INVALID_STATUS";

    // 搜索空间状态相关错误
    public static final String SEARCH_SPACE_ALREADY_ACTIVE = "SEARCH_SPACE_ALREADY_ACTIVE";
    public static final String SEARCH_SPACE_ALREADY_INACTIVE = "SEARCH_SPACE_ALREADY_INACTIVE";
    public static final String SEARCH_SPACE_STATUS_OPERATION_FAILED = "SEARCH_SPACE_STATUS_OPERATION_FAILED";

    // Elasticsearch相关错误
    public static final String ELASTICSEARCH_INDEX_EXISTS = "ELASTICSEARCH_INDEX_EXISTS";
    public static final String ELASTICSEARCH_INDEX_NOT_EXISTS = "ELASTICSEARCH_INDEX_NOT_EXISTS";
    public static final String ELASTICSEARCH_CONNECTION_FAILED = "ELASTICSEARCH_CONNECTION_FAILED";

    // 验证相关错误
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
}