package com.ynet.mgmt.searchspace.dto;

/**
 * 搜索空间统计信息
 *
 * @author system
 * @since 1.0.0
 */
public class SearchSpaceStatistics {

    private long totalSpaces;
    private long vectorEnabledSpaces;
    private long vectorDisabledSpaces;
    private long activeSpaces;
    private long inactiveSpaces;

    // 构造函数
    public SearchSpaceStatistics() {}

    public SearchSpaceStatistics(long totalSpaces, long vectorEnabledSpaces, long vectorDisabledSpaces) {
        this.totalSpaces = totalSpaces;
        this.vectorEnabledSpaces = vectorEnabledSpaces;
        this.vectorDisabledSpaces = vectorDisabledSpaces;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SearchSpaceStatistics statistics = new SearchSpaceStatistics();

        public Builder totalSpaces(long totalSpaces) {
            statistics.totalSpaces = totalSpaces;
            return this;
        }

        public Builder vectorEnabledSpaces(long vectorEnabledSpaces) {
            statistics.vectorEnabledSpaces = vectorEnabledSpaces;
            return this;
        }

        public Builder vectorDisabledSpaces(long vectorDisabledSpaces) {
            statistics.vectorDisabledSpaces = vectorDisabledSpaces;
            return this;
        }

        public Builder activeSpaces(long activeSpaces) {
            statistics.activeSpaces = activeSpaces;
            return this;
        }

        public Builder inactiveSpaces(long inactiveSpaces) {
            statistics.inactiveSpaces = inactiveSpaces;
            return this;
        }

        public SearchSpaceStatistics build() {
            return statistics;
        }
    }

    // 业务方法
    public double getVectorEnabledPercentage() {
        if (totalSpaces == 0) return 0.0;
        return (double) vectorEnabledSpaces / totalSpaces * 100;
    }

    public double getActivePercentage() {
        if (totalSpaces == 0) return 0.0;
        return (double) activeSpaces / totalSpaces * 100;
    }

    // Getters and Setters
    public long getTotalSpaces() {
        return totalSpaces;
    }

    public void setTotalSpaces(long totalSpaces) {
        this.totalSpaces = totalSpaces;
    }

    public long getVectorEnabledSpaces() {
        return vectorEnabledSpaces;
    }

    public void setVectorEnabledSpaces(long vectorEnabledSpaces) {
        this.vectorEnabledSpaces = vectorEnabledSpaces;
    }

    public long getVectorDisabledSpaces() {
        return vectorDisabledSpaces;
    }

    public void setVectorDisabledSpaces(long vectorDisabledSpaces) {
        this.vectorDisabledSpaces = vectorDisabledSpaces;
    }

    public long getActiveSpaces() {
        return activeSpaces;
    }

    public void setActiveSpaces(long activeSpaces) {
        this.activeSpaces = activeSpaces;
    }

    public long getInactiveSpaces() {
        return inactiveSpaces;
    }

    public void setInactiveSpaces(long inactiveSpaces) {
        this.inactiveSpaces = inactiveSpaces;
    }

    @Override
    public String toString() {
        return "SearchSpaceStatistics{" +
                "totalSpaces=" + totalSpaces +
                ", vectorEnabledSpaces=" + vectorEnabledSpaces +
                ", vectorDisabledSpaces=" + vectorDisabledSpaces +
                ", activeSpaces=" + activeSpaces +
                ", inactiveSpaces=" + inactiveSpaces +
                '}';
    }
}