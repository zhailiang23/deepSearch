package com.ynet.mgmt.searchlog.entity;

/**
 * 搜索日志状态枚举
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
public enum SearchLogStatus {

    /**
     * 搜索成功
     */
    SUCCESS("成功"),

    /**
     * 搜索出错
     */
    ERROR("错误"),

    /**
     * 搜索超时
     */
    TIMEOUT("超时");

    private final String description;

    /**
     * 构造函数
     *
     * @param description 状态描述
     */
    SearchLogStatus(String description) {
        this.description = description;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为成功状态
     *
     * @return 是否成功
     */
    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为错误状态
     *
     * @return 是否错误
     */
    public boolean isError() {
        return this == ERROR || this == TIMEOUT;
    }
}