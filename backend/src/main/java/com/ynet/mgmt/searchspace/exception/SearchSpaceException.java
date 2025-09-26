package com.ynet.mgmt.searchspace.exception;

/**
 * 搜索空间操作异常类
 * 用于搜索空间映射管理相关的异常处理
 *
 * @author system
 * @since 1.0.0
 */
public class SearchSpaceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 搜索空间ID
     */
    private Long spaceId;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 默认构造函数
     */
    public SearchSpaceException() {
        super();
    }

    /**
     * 带消息的构造函数
     */
    public SearchSpaceException(String message) {
        super(message);
    }

    /**
     * 带消息和原因的构造函数
     */
    public SearchSpaceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带原因的构造函数
     */
    public SearchSpaceException(Throwable cause) {
        super(cause);
    }

    /**
     * 完整构造函数
     */
    public SearchSpaceException(String message, String errorCode, String operation, Long spaceId) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
        this.spaceId = spaceId;
    }

    /**
     * 完整构造函数（带原因）
     */
    public SearchSpaceException(String message, Throwable cause, String errorCode, String operation, Long spaceId) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
        this.spaceId = spaceId;
    }

    // 静态工厂方法

    /**
     * 创建索引不存在异常
     */
    public static SearchSpaceException indexNotFound(Long spaceId, String indexName) {
        return new SearchSpaceException(
            "搜索空间对应的索引不存在: " + indexName + " (搜索空间ID: " + spaceId + ")",
            "INDEX_NOT_FOUND",
            "GET_MAPPING",
            spaceId
        );
    }

    /**
     * 创建映射验证失败异常
     */
    public static SearchSpaceException mappingValidationFailed(Long spaceId, String reason) {
        return new SearchSpaceException(
            "映射配置验证失败: " + reason + " (搜索空间ID: " + spaceId + ")",
            "MAPPING_VALIDATION_FAILED",
            "UPDATE_MAPPING",
            spaceId
        );
    }

    /**
     * 创建Elasticsearch连接异常
     */
    public static SearchSpaceException elasticsearchConnectionFailed(Long spaceId, String operation, Throwable cause) {
        return new SearchSpaceException(
            "Elasticsearch连接失败，操作: " + operation + " (搜索空间ID: " + spaceId + ")",
            cause,
            "ELASTICSEARCH_CONNECTION_FAILED",
            operation,
            spaceId
        );
    }

    /**
     * 创建映射更新失败异常
     */
    public static SearchSpaceException mappingUpdateFailed(Long spaceId, String indexName, Throwable cause) {
        return new SearchSpaceException(
            "更新索引映射失败: " + indexName + " (搜索空间ID: " + spaceId + ")",
            cause,
            "MAPPING_UPDATE_FAILED",
            "UPDATE_MAPPING",
            spaceId
        );
    }

    /**
     * 创建映射获取失败异常
     */
    public static SearchSpaceException mappingRetrievalFailed(Long spaceId, String indexName, Throwable cause) {
        return new SearchSpaceException(
            "获取索引映射失败: " + indexName + " (搜索空间ID: " + spaceId + ")",
            cause,
            "MAPPING_RETRIEVAL_FAILED",
            "GET_MAPPING",
            spaceId
        );
    }

    /**
     * 创建搜索空间未找到异常
     */
    public static SearchSpaceException searchSpaceNotFound(Long spaceId) {
        return new SearchSpaceException(
            "搜索空间不存在: " + spaceId,
            "SEARCH_SPACE_NOT_FOUND",
            "FIND_SPACE",
            spaceId
        );
    }

    // Getters and Setters

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * 获取详细的错误信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("SearchSpaceException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (errorCode != null) {
            sb.append(", errorCode='").append(errorCode).append('\'');
        }
        if (operation != null) {
            sb.append(", operation='").append(operation).append('\'');
        }
        if (spaceId != null) {
            sb.append(", spaceId=").append(spaceId);
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDetailedMessage();
    }
}