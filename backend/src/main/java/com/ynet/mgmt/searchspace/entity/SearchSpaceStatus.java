package com.ynet.mgmt.searchspace.entity;

/**
 * 搜索空间状态枚举
 * 定义搜索空间的不同状态
 *
 * @author system
 * @since 1.0.0
 */
public enum SearchSpaceStatus {
    /**
     * 活跃状态 - 搜索空间正常可用
     */
    ACTIVE("活跃", "搜索空间正常可用"),

    /**
     * 非活跃状态 - 搜索空间暂时停用
     */
    INACTIVE("非活跃", "搜索空间暂时停用"),

    /**
     * 维护状态 - 搜索空间正在维护中
     */
    MAINTENANCE("维护中", "搜索空间正在维护中"),

    /**
     * 已删除状态 - 搜索空间已被删除
     */
    DELETED("已删除", "搜索空间已被删除");

    private final String displayName;
    private final String description;

    SearchSpaceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 获取状态显示名称
     * @return 状态的中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取状态描述
     * @return 状态的详细描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 检查是否为活跃状态
     * @return true if 状态为ACTIVE
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 检查是否可用于搜索
     * @return true if 状态为ACTIVE或MAINTENANCE
     */
    public boolean isSearchable() {
        return this == ACTIVE || this == MAINTENANCE;
    }
}