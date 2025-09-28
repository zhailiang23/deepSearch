package com.ynet.mgmt.statistics.enums;

/**
 * 时间范围枚举
 * 用于关键词不匹配度统计的时间范围选择
 *
 * @author system
 * @since 1.0.0
 */
public enum TimeRangeEnum {

    /**
     * 最近7天
     */
    LAST_7_DAYS("7d", "最近7天", 7),

    /**
     * 最近30天
     */
    LAST_30_DAYS("30d", "最近30天", 30),

    /**
     * 最近90天
     */
    LAST_90_DAYS("90d", "最近90天", 90);

    private final String code;
    private final String description;
    private final int days;

    TimeRangeEnum(String code, String description, int days) {
        this.code = code;
        this.description = description;
        this.days = days;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getDays() {
        return days;
    }

    /**
     * 根据代码获取时间范围枚举
     *
     * @param code 时间范围代码
     * @return 时间范围枚举
     * @throws IllegalArgumentException 如果代码无效
     */
    public static TimeRangeEnum fromCode(String code) {
        for (TimeRangeEnum timeRange : values()) {
            if (timeRange.getCode().equals(code)) {
                return timeRange;
            }
        }
        throw new IllegalArgumentException("Invalid time range code: " + code);
    }

    /**
     * 检查代码是否有效
     *
     * @param code 时间范围代码
     * @return true如果代码有效，否则false
     */
    public static boolean isValidCode(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}