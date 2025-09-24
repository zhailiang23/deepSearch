package com.ynet.mgmt.jsonimport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JSON Schema分析结果
 * 包含整个JSON数据集的分析结果
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JsonSchemaAnalysis {

    /**
     * 总记录数
     */
    private int totalRecords;

    /**
     * 总字段数
     */
    private int totalFields;

    /**
     * 各字段的分析结果
     * key: 字段名, value: 字段分析结果
     */
    private Map<String, FieldAnalysisResult> fieldAnalysis;

    /**
     * 分析完成时间
     */
    private LocalDateTime analyzedAt;

    /**
     * 源文件MD5哈希值（用于缓存）
     */
    private String sourceFileHash;

    /**
     * 分析耗时（毫秒）
     */
    private long analysisDurationMs;

    /**
     * 分析质量评分（0-100）
     */
    private int overallQualityScore;

    /**
     * 推荐的主键字段
     */
    private String recommendedIdField;

    /**
     * 推荐建立索引的字段列表
     */
    private java.util.List<String> recommendedIndexFields;

    /**
     * 数据一致性检查结果
     */
    private DataConsistencyReport consistencyReport;

    /**
     * 数据一致性报告
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataConsistencyReport {
        /**
         * 字段完整性评分
         */
        private int completenessScore;

        /**
         * 数据类型一致性评分
         */
        private int consistencyScore;

        /**
         * 检测到的异常情况
         */
        private java.util.List<String> anomalies;

        /**
         * 数据质量建议
         */
        private java.util.List<String> recommendations;

        /**
         * 是否适合导入Elasticsearch
         */
        private boolean readyForElasticsearch;
    }
}