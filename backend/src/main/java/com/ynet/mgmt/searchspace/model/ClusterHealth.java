package com.ynet.mgmt.searchspace.model;

/**
 * Elasticsearch集群健康状态模型
 * 用于返回集群的整体健康状态和统计信息
 *
 * @author system
 * @since 1.0.0
 */
public class ClusterHealth {

    /**
     * 集群状态 (green, yellow, red)
     */
    private String status;

    /**
     * 节点总数
     */
    private Integer numberOfNodes;

    /**
     * 数据节点数量
     */
    private Integer numberOfDataNodes;

    /**
     * 活跃主分片数量
     */
    private Integer activePrimaryShards;

    /**
     * 活跃分片总数
     */
    private Integer activeShards;

    /**
     * 正在迁移的分片数量
     */
    private Integer relocatingShards;

    /**
     * 正在初始化的分片数量
     */
    private Integer initializingShards;

    /**
     * 未分配的分片数量
     */
    private Integer unassignedShards;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 错误信息（如果有）
     */
    private String error;

    /**
     * 默认构造函数
     */
    public ClusterHealth() {}

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
        private ClusterHealth clusterHealth = new ClusterHealth();

        public Builder status(String status) {
            clusterHealth.status = status;
            return this;
        }

        public Builder numberOfNodes(Integer numberOfNodes) {
            clusterHealth.numberOfNodes = numberOfNodes;
            return this;
        }

        public Builder numberOfDataNodes(Integer numberOfDataNodes) {
            clusterHealth.numberOfDataNodes = numberOfDataNodes;
            return this;
        }

        public Builder activePrimaryShards(Integer activePrimaryShards) {
            clusterHealth.activePrimaryShards = activePrimaryShards;
            return this;
        }

        public Builder activeShards(Integer activeShards) {
            clusterHealth.activeShards = activeShards;
            return this;
        }

        public Builder relocatingShards(Integer relocatingShards) {
            clusterHealth.relocatingShards = relocatingShards;
            return this;
        }

        public Builder initializingShards(Integer initializingShards) {
            clusterHealth.initializingShards = initializingShards;
            return this;
        }

        public Builder unassignedShards(Integer unassignedShards) {
            clusterHealth.unassignedShards = unassignedShards;
            return this;
        }

        public Builder clusterName(String clusterName) {
            clusterHealth.clusterName = clusterName;
            return this;
        }

        public Builder error(String error) {
            clusterHealth.error = error;
            return this;
        }

        public ClusterHealth build() {
            return clusterHealth;
        }
    }

    // Getters and Setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNumberOfNodes(Integer numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public Integer getNumberOfDataNodes() {
        return numberOfDataNodes;
    }

    public void setNumberOfDataNodes(Integer numberOfDataNodes) {
        this.numberOfDataNodes = numberOfDataNodes;
    }

    public Integer getActivePrimaryShards() {
        return activePrimaryShards;
    }

    public void setActivePrimaryShards(Integer activePrimaryShards) {
        this.activePrimaryShards = activePrimaryShards;
    }

    public Integer getActiveShards() {
        return activeShards;
    }

    public void setActiveShards(Integer activeShards) {
        this.activeShards = activeShards;
    }

    public Integer getRelocatingShards() {
        return relocatingShards;
    }

    public void setRelocatingShards(Integer relocatingShards) {
        this.relocatingShards = relocatingShards;
    }

    public Integer getInitializingShards() {
        return initializingShards;
    }

    public void setInitializingShards(Integer initializingShards) {
        this.initializingShards = initializingShards;
    }

    public Integer getUnassignedShards() {
        return unassignedShards;
    }

    public void setUnassignedShards(Integer unassignedShards) {
        this.unassignedShards = unassignedShards;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * 检查集群是否健康
     */
    public boolean isHealthy() {
        return "green".equalsIgnoreCase(status);
    }

    /**
     * 检查集群是否可用
     */
    public boolean isAvailable() {
        return "green".equalsIgnoreCase(status) || "yellow".equalsIgnoreCase(status);
    }

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }

    /**
     * 检查是否有未分配的分片
     */
    public boolean hasUnassignedShards() {
        return unassignedShards != null && unassignedShards > 0;
    }

    @Override
    public String toString() {
        return "ClusterHealth{" +
                "status='" + status + '\'' +
                ", numberOfNodes=" + numberOfNodes +
                ", numberOfDataNodes=" + numberOfDataNodes +
                ", activePrimaryShards=" + activePrimaryShards +
                ", activeShards=" + activeShards +
                ", relocatingShards=" + relocatingShards +
                ", initializingShards=" + initializingShards +
                ", unassignedShards=" + unassignedShards +
                ", clusterName='" + clusterName + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}