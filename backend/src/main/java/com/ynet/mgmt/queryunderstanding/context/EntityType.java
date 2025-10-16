package com.ynet.mgmt.queryunderstanding.context;

/**
 * 实体类型枚举
 * 定义系统可识别的各种实体类型
 *
 * @author deepSearch
 * @since 2025-01-16
 */
public enum EntityType {

    /**
     * 人名
     */
    PERSON("人名"),

    /**
     * 地名/位置
     */
    LOCATION("地名"),

    /**
     * 机构/组织名称
     */
    ORGANIZATION("机构"),

    /**
     * 产品/服务名称
     */
    PRODUCT("产品"),

    /**
     * 日期/时间
     */
    DATE_TIME("时间"),

    /**
     * 金额/数字
     */
    MONEY("金额"),

    /**
     * 其他
     */
    OTHER("其他");

    private final String description;

    EntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
