package com.ynet.mgmt.queryunderstanding.context;

/**
 * 意图类型枚举
 * 定义用户查询的各种可能意图
 *
 * @author deepSearch
 * @since 2025-01-16
 */
public enum IntentType {

    /**
     * 信息查询 - 用户想要获取信息
     */
    INFORMATION_QUERY("信息查询"),

    /**
     * 导航 - 用户想要找到特定页面或功能
     */
    NAVIGATION("导航"),

    /**
     * 操作指令 - 用户想要执行某个操作
     */
    ACTION("操作指令"),

    /**
     * 比较 - 用户想要比较多个项目
     */
    COMPARISON("比较"),

    /**
     * 定义/解释 - 用户想要了解某个概念的定义
     */
    DEFINITION("定义"),

    /**
     * 列表/枚举 - 用户想要获取一个列表
     */
    LIST("列表"),

    /**
     * 未知意图
     */
    UNKNOWN("未知");

    private final String description;

    IntentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
