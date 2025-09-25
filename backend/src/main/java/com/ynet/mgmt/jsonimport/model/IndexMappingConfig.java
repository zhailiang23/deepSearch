package com.ynet.mgmt.jsonimport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Elasticsearch索引映射配置
 * 根据JSON数据分析结果生成的索引配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexMappingConfig {

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 索引设置
     */
    private IndexSettings settings;

    /**
     * 字段映射配置
     */
    private Map<String, FieldMapping> fieldMappings;

    /**
     * 索引别名
     */
    private List<String> aliases;

    /**
     * 是否启用动态映射
     */
    private Boolean dynamicMapping;

    /**
     * 自定义分析器配置
     */
    private Map<String, Object> analyzers;

    /**
     * 索引设置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexSettings {

        /**
         * 主分片数
         */
        private Integer numberOfShards;

        /**
         * 副本数
         */
        private Integer numberOfReplicas;

        /**
         * 刷新间隔
         */
        private String refreshInterval;

        /**
         * 最大结果窗口
         */
        private Integer maxResultWindow;

        /**
         * 是否启用慢查询日志
         */
        private Boolean enableSlowLog;
    }

    /**
     * 字段映射配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldMapping {

        /**
         * 字段名称
         */
        private String fieldName;

        /**
         * Elasticsearch字段类型
         */
        private String elasticsearchType;

        /**
         * 分析器名称
         */
        private String analyzer;

        /**
         * 搜索分析器名称
         */
        private String searchAnalyzer;

        /**
         * 是否建立索引
         */
        private Boolean index;

        /**
         * 是否存储原始值
         */
        private Boolean store;

        /**
         * 子字段配置（如keyword字段）
         */
        private Map<String, FieldMapping> fields;

        /**
         * 字段格式（用于日期等特殊类型）
         */
        private String format;

        /**
         * 是否启用doc_values
         */
        private Boolean docValues;

        /**
         * 字段权重
         */
        private Double boost;

        /**
         * 空值处理
         */
        private String nullValue;

        /**
         * 忽略格式错误
         */
        private Boolean ignoreMalformed;

        /**
         * 文本字段的特殊配置
         */
        private TextFieldConfig textConfig;

        /**
         * 数值字段的特殊配置
         */
        private NumericFieldConfig numericConfig;
    }

    /**
     * 文本字段配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextFieldConfig {

        /**
         * 是否启用位置增量
         */
        private Boolean enablePositionIncrements;

        /**
         * 字段长度限制
         */
        private Integer fielddata;

        /**
         * 是否支持高亮
         */
        private Boolean termVector;
    }

    /**
     * 数值字段配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NumericFieldConfig {

        /**
         * 强制转换
         */
        private Boolean coerce;

        /**
         * 缩放因子（用于scaled_float）
         */
        private Integer scalingFactor;
    }

    /**
     * 生成Elasticsearch映射JSON
     * 支持分析器配置和完整的索引设置
     */
    public Map<String, Object> toElasticsearchMapping() {
        Map<String, Object> mapping = new HashMap<>();

        // 构建设置部分
        Map<String, Object> settingsMap = new HashMap<>();

        // 基础索引设置
        settingsMap.put("number_of_shards", settings.getNumberOfShards());
        settingsMap.put("number_of_replicas", settings.getNumberOfReplicas());
        settingsMap.put("refresh_interval", settings.getRefreshInterval());

        if (settings.getMaxResultWindow() != null) {
            settingsMap.put("max_result_window", settings.getMaxResultWindow());
        }

        // 添加分析器配置
        if (analyzers != null && !analyzers.isEmpty()) {
            settingsMap.put("analysis", analyzers);
        }

        mapping.put("settings", settingsMap);

        // 构建映射部分
        Map<String, Object> mappingsMap = new HashMap<>();
        mappingsMap.put("dynamic", dynamicMapping != null ? dynamicMapping : true);
        mappingsMap.put("properties", buildPropertiesMap());

        mapping.put("mappings", mappingsMap);

        return mapping;
    }

    /**
     * 构建properties映射
     */
    private Map<String, Object> buildPropertiesMap() {
        return fieldMappings.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> buildFieldMap(entry.getValue())
            ));
    }

    /**
     * 构建单个字段的映射配置
     */
    private Map<String, Object> buildFieldMap(FieldMapping fieldMapping) {
        Map<String, Object> fieldMap = new HashMap<>();

        fieldMap.put("type", fieldMapping.getElasticsearchType());

        if (fieldMapping.getAnalyzer() != null) {
            fieldMap.put("analyzer", fieldMapping.getAnalyzer());
        }

        if (fieldMapping.getSearchAnalyzer() != null) {
            fieldMap.put("search_analyzer", fieldMapping.getSearchAnalyzer());
        }

        if (fieldMapping.getIndex() != null) {
            fieldMap.put("index", fieldMapping.getIndex());
        }

        if (fieldMapping.getStore() != null) {
            fieldMap.put("store", fieldMapping.getStore());
        }

        if (fieldMapping.getDocValues() != null) {
            fieldMap.put("doc_values", fieldMapping.getDocValues());
        }

        if (fieldMapping.getBoost() != null) {
            fieldMap.put("boost", fieldMapping.getBoost());
        }

        if (fieldMapping.getFormat() != null) {
            fieldMap.put("format", fieldMapping.getFormat());
        }

        if (fieldMapping.getIgnoreMalformed() != null) {
            fieldMap.put("ignore_malformed", fieldMapping.getIgnoreMalformed());
        }

        if (fieldMapping.getNullValue() != null) {
            fieldMap.put("null_value", fieldMapping.getNullValue());
        }

        // 处理文本字段的特殊配置
        if (fieldMapping.getTextConfig() != null) {
            TextFieldConfig textConfig = fieldMapping.getTextConfig();
            if (textConfig.getEnablePositionIncrements() != null) {
                fieldMap.put("position_increment_gap", textConfig.getEnablePositionIncrements() ? 100 : 0);
            }
            if (textConfig.getTermVector() != null && textConfig.getTermVector()) {
                fieldMap.put("term_vector", "with_positions_offsets");
            }
            if (textConfig.getFielddata() != null) {
                fieldMap.put("fielddata", textConfig.getFielddata());
            }
        }

        // 处理数值字段的特殊配置
        if (fieldMapping.getNumericConfig() != null) {
            NumericFieldConfig numericConfig = fieldMapping.getNumericConfig();
            if (numericConfig.getCoerce() != null) {
                fieldMap.put("coerce", numericConfig.getCoerce());
            }
            if (numericConfig.getScalingFactor() != null) {
                fieldMap.put("scaling_factor", numericConfig.getScalingFactor());
            }
        }

        // 处理子字段
        if (fieldMapping.getFields() != null && !fieldMapping.getFields().isEmpty()) {
            fieldMap.put("fields", fieldMapping.getFields().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> buildFieldMap(entry.getValue())
                ))
            );
        }

        return fieldMap;
    }
}