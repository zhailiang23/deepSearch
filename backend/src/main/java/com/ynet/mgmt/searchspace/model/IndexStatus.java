package com.ynet.mgmt.searchspace.model;

/**
 * 索引状态信息模型
 * 用于返回Elasticsearch索引的状态和统计信息
 *
 * @author system
 * @since 1.0.0
 */
public class IndexStatus {

    /**
     * 索引名称
     */
    private String name;

    /**
     * 索引是否存在
     */
    private boolean exists;

    /**
     * 索引健康状态 (green, yellow, red)
     */
    private String health;

    /**
     * 文档数量
     */
    private long docsCount;

    /**
     * 存储大小
     */
    private String storeSize;

    /**
     * 主分片数量
     */
    private Integer primaryShards;

    /**
     * 副本数量
     */
    private Integer replicas;

    /**
     * 错误信息（如果有）
     */
    private String error;

    /**
     * 默认构造函数
     */
    public IndexStatus() {}

    /**
     * 带参数的构造函数
     */
    public IndexStatus(String name, boolean exists) {
        this.name = name;
        this.exists = exists;
    }

    /**
     * 构建器模式创建实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器类
     */
    public static class Builder {
        private IndexStatus indexStatus = new IndexStatus();

        public Builder name(String name) {
            indexStatus.name = name;
            return this;
        }

        public Builder exists(boolean exists) {
            indexStatus.exists = exists;
            return this;
        }

        public Builder health(String health) {
            indexStatus.health = health;
            return this;
        }

        public Builder docsCount(long docsCount) {
            indexStatus.docsCount = docsCount;
            return this;
        }

        public Builder storeSize(String storeSize) {
            indexStatus.storeSize = storeSize;
            return this;
        }

        public Builder primaryShards(Integer primaryShards) {
            indexStatus.primaryShards = primaryShards;
            return this;
        }

        public Builder replicas(Integer replicas) {
            indexStatus.replicas = replicas;
            return this;
        }

        public Builder error(String error) {
            indexStatus.error = error;
            return this;
        }

        public IndexStatus build() {
            return indexStatus;
        }
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }

    public long getDocsCount() {
        return docsCount;
    }

    public void setDocsCount(long docsCount) {
        this.docsCount = docsCount;
    }

    public String getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(String storeSize) {
        this.storeSize = storeSize;
    }

    public Integer getPrimaryShards() {
        return primaryShards;
    }

    public void setPrimaryShards(Integer primaryShards) {
        this.primaryShards = primaryShards;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * 检查索引是否健康
     */
    public boolean isHealthy() {
        return exists && "green".equalsIgnoreCase(health);
    }

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "IndexStatus{" +
                "name='" + name + '\'' +
                ", exists=" + exists +
                ", health='" + health + '\'' +
                ", docsCount=" + docsCount +
                ", storeSize='" + storeSize + '\'' +
                ", primaryShards=" + primaryShards +
                ", replicas=" + replicas +
                ", error='" + error + '\'' +
                '}';
    }
}