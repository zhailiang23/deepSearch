package com.ynet.mgmt.jsonimport.model;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字段分析结果
 * 包含单个字段的完整分析信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldAnalysisResult {

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 推断的字段类型
     */
    private FieldType inferredType;

    /**
     * 类型推断置信度（0.0-1.0）
     */
    private double confidence;

    /**
     * 字段统计信息
     */
    private FieldStatistics statistics;

    /**
     * 样本值列表（用于展示）
     */
    private List<String> sampleValues;

    /**
     * 检测到的问题列表
     */
    private List<String> issues;

    /**
     * 是否建议作为主键
     */
    private boolean suggestAsId;

    /**
     * 是否建议建立索引
     */
    private boolean suggestIndex;

    /**
     * 字段重要性评分（0-100）
     */
    private int importance;

    /**
     * 是否包含中文内容
     */
    private boolean hasChineseContent;

    /**
     * 中文内容占比（0.0-1.0）
     */
    private double chineseContentRatio;

    /**
     * 包含中文内容的样本值
     */
    private List<String> chineseContentSamples;

    /**
     * 类型推断过程中的候选类型及其置信度
     */
    private List<TypeCandidate> typeCandidates;

    /**
     * 类型候选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCandidate {
        /**
         * 候选类型
         */
        private FieldType type;

        /**
         * 置信度
         */
        private double confidence;

        /**
         * 匹配的样本数量
         */
        private int matchCount;

        /**
         * 推断依据
         */
        private String reason;
    }
}