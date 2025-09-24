package com.ynet.mgmt.jsonimport.enums;

/**
 * 导入任务状态枚举
 * 定义了导入任务的各种状态
 */
public enum ImportTaskState {

    /**
     * 待开始 - 任务已创建但未开始执行
     */
    PENDING("pending", "待开始"),

    /**
     * 正在运行 - 任务正在执行中
     */
    RUNNING("running", "正在运行"),

    /**
     * 分析数据 - 正在分析JSON数据结构
     */
    ANALYZING_DATA("analyzing_data", "分析数据"),

    /**
     * 创建索引 - 正在创建Elasticsearch索引
     */
    CREATING_INDEX("creating_index", "创建索引"),

    /**
     * 处理数据 - 正在批量处理和导入数据
     */
    PROCESSING_DATA("processing_data", "处理数据"),

    /**
     * 优化索引 - 正在执行索引优化
     */
    OPTIMIZING_INDEX("optimizing_index", "优化索引"),

    /**
     * 已完成 - 任务成功完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 已失败 - 任务执行失败
     */
    FAILED("failed", "已失败"),

    /**
     * 已取消 - 任务被用户取消
     */
    CANCELLED("cancelled", "已取消"),

    /**
     * 暂停中 - 任务被暂停（保留状态，暂未实现）
     */
    PAUSED("paused", "暂停中");

    private final String code;
    private final String description;

    ImportTaskState(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码查找状态
     */
    public static ImportTaskState fromCode(String code) {
        for (ImportTaskState state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("未知的任务状态代码: " + code);
    }

    /**
     * 检查是否为终态（任务不会再继续执行）
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * 检查是否为运行状态（任务正在执行中）
     */
    public boolean isRunning() {
        return this == RUNNING || this == ANALYZING_DATA || this == CREATING_INDEX ||
               this == PROCESSING_DATA || this == OPTIMIZING_INDEX;
    }

    /**
     * 检查是否可以取消
     */
    public boolean isCancellable() {
        return this == PENDING || isRunning();
    }
}