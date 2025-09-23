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

    // 向量检索相关错误
    public static final String VECTOR_ALREADY_ENABLED = "VECTOR_ALREADY_ENABLED";
    public static final String VECTOR_ALREADY_DISABLED = "VECTOR_ALREADY_DISABLED";
    public static final String VECTOR_OPERATION_FAILED = "VECTOR_OPERATION_FAILED";

    // Elasticsearch相关错误
    public static final String ELASTICSEARCH_INDEX_EXISTS = "ELASTICSEARCH_INDEX_EXISTS";
    public static final String ELASTICSEARCH_INDEX_NOT_EXISTS = "ELASTICSEARCH_INDEX_NOT_EXISTS";
    public static final String ELASTICSEARCH_CONNECTION_FAILED = "ELASTICSEARCH_CONNECTION_FAILED";

    // 验证相关错误
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_PARAMETER = "INVALID_PARAMETER";
}