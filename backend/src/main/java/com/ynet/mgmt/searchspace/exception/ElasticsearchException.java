package com.ynet.mgmt.searchspace.exception;

/**
 * Elasticsearch操作异常类
 * 用于包装Elasticsearch相关的异常信息
 *
 * @author system
 * @since 1.0.0
 */
public class ElasticsearchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 默认构造函数
     */
    public ElasticsearchException() {
        super();
    }

    /**
     * 带消息的构造函数
     */
    public ElasticsearchException(String message) {
        super(message);
    }

    /**
     * 带消息和原因的构造函数
     */
    public ElasticsearchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 带原因的构造函数
     */
    public ElasticsearchException(Throwable cause) {
        super(cause);
    }

    /**
     * 完整构造函数
     */
    public ElasticsearchException(String message, String errorCode, String operation, String indexName) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
        this.indexName = indexName;
    }

    /**
     * 完整构造函数（带原因）
     */
    public ElasticsearchException(String message, Throwable cause, String errorCode, String operation, String indexName) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
        this.indexName = indexName;
    }

    // 静态工厂方法

    /**
     * 创建索引创建失败异常
     */
    public static ElasticsearchException indexCreationFailed(String indexName, Throwable cause) {
        return new ElasticsearchException(
            "创建索引失败: " + indexName,
            cause,
            "INDEX_CREATION_FAILED",
            "CREATE_INDEX",
            indexName
        );
    }

    /**
     * 创建索引删除失败异常
     */
    public static ElasticsearchException indexDeletionFailed(String indexName, Throwable cause) {
        return new ElasticsearchException(
            "删除索引失败: " + indexName,
            cause,
            "INDEX_DELETION_FAILED",
            "DELETE_INDEX",
            indexName
        );
    }

    /**
     * 创建索引不存在异常
     */
    public static ElasticsearchException indexNotFound(String indexName) {
        return new ElasticsearchException(
            "索引不存在: " + indexName,
            "INDEX_NOT_FOUND",
            "CHECK_INDEX",
            indexName
        );
    }

    /**
     * 创建连接失败异常
     */
    public static ElasticsearchException connectionFailed(Throwable cause) {
        return new ElasticsearchException(
            "Elasticsearch连接失败",
            cause,
            "CONNECTION_FAILED",
            "CONNECT",
            null
        );
    }

    /**
     * 创建操作超时异常
     */
    public static ElasticsearchException operationTimeout(String operation, String indexName) {
        return new ElasticsearchException(
            "操作超时: " + operation + (indexName != null ? " on index " + indexName : ""),
            "OPERATION_TIMEOUT",
            operation,
            indexName
        );
    }

    /**
     * 创建集群不可用异常
     */
    public static ElasticsearchException clusterUnavailable(Throwable cause) {
        return new ElasticsearchException(
            "Elasticsearch集群不可用",
            cause,
            "CLUSTER_UNAVAILABLE",
            "HEALTH_CHECK",
            null
        );
    }

    // Getters and Setters

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * 获取详细的错误信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("ElasticsearchException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (errorCode != null) {
            sb.append(", errorCode='").append(errorCode).append('\'');
        }
        if (operation != null) {
            sb.append(", operation='").append(operation).append('\'');
        }
        if (indexName != null) {
            sb.append(", indexName='").append(indexName).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDetailedMessage();
    }
}