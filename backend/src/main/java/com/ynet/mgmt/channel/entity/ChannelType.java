package com.ynet.mgmt.channel.entity;

/**
 * 渠道类型枚举
 * 定义渠道的不同经营类型
 *
 * @author system
 * @since 1.0.0
 */
public enum ChannelType {
    /**
     * 线上渠道 - 网络销售渠道
     */
    ONLINE("线上渠道", "网络销售渠道"),

    /**
     * 线下渠道 - 实体销售渠道
     */
    OFFLINE("线下渠道", "实体销售渠道"),

    /**
     * 混合渠道 - 线上线下结合渠道
     */
    HYBRID("混合渠道", "线上线下结合渠道"),

    /**
     * 分销商渠道 - 第三方分销商渠道
     */
    DISTRIBUTOR("分销商渠道", "第三方分销商渠道");

    private final String displayName;
    private final String description;

    ChannelType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 获取类型显示名称
     * @return 类型的中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取类型描述
     * @return 类型的详细描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 检查是否为线上渠道
     * @return 是否为 ONLINE 类型
     */
    public boolean isOnline() {
        return this == ONLINE;
    }

    /**
     * 检查是否为线下渠道
     * @return 是否为 OFFLINE 类型
     */
    public boolean isOffline() {
        return this == OFFLINE;
    }

    /**
     * 检查是否为混合渠道
     * @return 是否为 HYBRID 类型
     */
    public boolean isHybrid() {
        return this == HYBRID;
    }
}