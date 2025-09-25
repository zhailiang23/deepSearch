package com.ynet.mgmt.channel.entity;

/**
 * 渠道状态枚举
 * 定义渠道运营的不同状态
 *
 * @author system
 * @since 1.0.0
 */
public enum ChannelStatus {
    /**
     * 激活状态 - 渠道正常运营中
     */
    ACTIVE("激活", "渠道正常运营中"),

    /**
     * 未激活状态 - 渠道暂未开始运营
     */
    INACTIVE("未激活", "渠道暂未开始运营"),

    /**
     * 暂停状态 - 渠道暂停运营
     */
    SUSPENDED("暂停", "渠道暂停运营"),

    /**
     * 已删除状态 - 渠道已被删除
     */
    DELETED("已删除", "渠道已被删除");

    private final String displayName;
    private final String description;

    ChannelStatus(String displayName, String description) {
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
     * 检查是否为活动状态
     * @return 是否为 ACTIVE 状态
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 检查是否为已删除状态
     * @return 是否为 DELETED 状态
     */
    public boolean isDeleted() {
        return this == DELETED;
    }

    /**
     * 检查是否为暂停状态
     * @return 是否为 SUSPENDED 状态
     */
    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    /**
     * 检查是否为未激活状态
     * @return 是否为 INACTIVE 状态
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }
}