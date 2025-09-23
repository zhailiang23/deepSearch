package com.ynet.mgmt.searchspace.dto;

/**
 * 搜索空间统计信息
 *
 * @author system
 * @since 1.0.0
 */
public class SearchSpaceStatistics {

    private long totalSpaces;
    private long activeSpaces;
    private long inactiveSpaces;
    private long maintenanceSpaces;
    private long deletedSpaces;

    // 构造函数
    public SearchSpaceStatistics() {}

    public SearchSpaceStatistics(long totalSpaces, long activeSpaces, long inactiveSpaces) {
        this.totalSpaces = totalSpaces;
        this.activeSpaces = activeSpaces;
        this.inactiveSpaces = inactiveSpaces;
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

        public Builder maintenanceSpaces(long maintenanceSpaces) {
            statistics.maintenanceSpaces = maintenanceSpaces;
            return this;
        }

        public Builder deletedSpaces(long deletedSpaces) {
            statistics.deletedSpaces = deletedSpaces;
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

    public long getMaintenanceSpaces() {
        return maintenanceSpaces;
    }

    public void setMaintenanceSpaces(long maintenanceSpaces) {
        this.maintenanceSpaces = maintenanceSpaces;
    }

    public long getDeletedSpaces() {
        return deletedSpaces;
    }

    public void setDeletedSpaces(long deletedSpaces) {
        this.deletedSpaces = deletedSpaces;
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
                ", maintenanceSpaces=" + maintenanceSpaces +
                ", deletedSpaces=" + deletedSpaces +
                ", activeSpaces=" + activeSpaces +
                ", inactiveSpaces=" + inactiveSpaces +
                '}';
    }
}