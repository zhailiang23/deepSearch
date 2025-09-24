package com.ynet.mgmt.jsonimport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 字段统计信息
 * 包含字段的各种统计分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldStatistics {

    /**
     * 总记录数
     */
    private int totalCount;

    /**
     * 非空值数量
     */
    private int nonNullCount;

    /**
     * 空值数量
     */
    private int nullCount;

    /**
     * 空值比例
     */
    private double nullRatio;

    /**
     * 唯一值数量
     */
    private int uniqueCount;

    /**
     * 重复度（唯一值/总值的比例）
     */
    private double uniqueRatio;

    // 字符串类型统计
    /**
     * 最小字符串长度
     */
    private Integer minLength;

    /**
     * 最大字符串长度
     */
    private Integer maxLength;

    /**
     * 平均字符串长度
     */
    private Double avgLength;

    // 数值类型统计
    /**
     * 最小数值
     */
    private BigDecimal minValue;

    /**
     * 最大数值
     */
    private BigDecimal maxValue;

    /**
     * 平均数值
     */
    private BigDecimal avgValue;

    /**
     * 数值总和
     */
    private BigDecimal sumValue;

    // 日期类型统计
    /**
     * 最早日期
     */
    private LocalDateTime minDate;

    /**
     * 最晚日期
     */
    private LocalDateTime maxDate;

    /**
     * 日期格式列表（按出现频率排序）
     */
    private List<String> dateFormats;

    /**
     * 高频值样本（前N个）
     */
    private List<FrequencyItem> topValues;

    /**
     * 是否包含异常值
     */
    private boolean hasOutliers;

    /**
     * 数据质量评分（0-100）
     */
    private int qualityScore;

    /**
     * 频率项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrequencyItem {
        /**
         * 值
         */
        private String value;

        /**
         * 出现次数
         */
        private int count;

        /**
         * 出现频率
         */
        private double frequency;
    }
}