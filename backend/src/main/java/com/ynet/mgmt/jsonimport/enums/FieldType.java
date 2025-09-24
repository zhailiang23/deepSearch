package com.ynet.mgmt.jsonimport.enums;

/**
 * JSON字段类型枚举
 * 用于表示分析过程中识别的不同数据类型
 */
public enum FieldType {
    /**
     * 字符串类型 - 默认文本类型
     */
    STRING("string", "字符串"),

    /**
     * 整数类型 - 包括Integer和Long
     */
    INTEGER("integer", "整数"),

    /**
     * 浮点数类型 - 包括Float和Double
     */
    FLOAT("float", "浮点数"),

    /**
     * 布尔类型
     */
    BOOLEAN("boolean", "布尔值"),

    /**
     * 日期时间类型
     */
    DATE("date", "日期时间"),

    /**
     * 邮箱格式字符串
     */
    EMAIL("email", "邮箱"),

    /**
     * URL格式字符串
     */
    URL("url", "链接"),

    /**
     * 未知类型 - 无法推断时的默认值
     */
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String description;

    FieldType(String code, String description) {
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
     * 根据代码获取字段类型
     */
    public static FieldType fromCode(String code) {
        for (FieldType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 是否为数值类型
     */
    public boolean isNumeric() {
        return this == INTEGER || this == FLOAT;
    }

    /**
     * 是否为时间相关类型
     */
    public boolean isTemporal() {
        return this == DATE;
    }

    /**
     * 是否为格式化字符串类型
     */
    public boolean isFormattedString() {
        return this == EMAIL || this == URL;
    }
}