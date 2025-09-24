package com.ynet.mgmt.jsonimport.service;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldAnalysisResult;
import com.ynet.mgmt.jsonimport.model.IndexMappingConfig;
import com.ynet.mgmt.jsonimport.model.JsonSchemaAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 索引配置生成服务
 * 根据JSON数据分析结果生成Elasticsearch索引映射配置
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IndexConfigService {

    private static final Map<FieldType, String> TYPE_MAPPING = Map.of(
        FieldType.STRING, "text",
        FieldType.INTEGER, "long",
        FieldType.FLOAT, "double",
        FieldType.BOOLEAN, "boolean",
        FieldType.DATE, "date",
        FieldType.EMAIL, "text",
        FieldType.URL, "text",
        FieldType.UNKNOWN, "text"
    );

    /**
     * 根据JSON分析结果生成索引映射配置
     *
     * @param searchSpaceCode 搜索空间代码
     * @param analysis        JSON分析结果
     * @return 索引映射配置
     */
    public IndexMappingConfig generateIndexConfig(String searchSpaceCode, JsonSchemaAnalysis analysis) {
        log.info("开始生成索引配置: searchSpaceCode={}, totalRecords={}",
                searchSpaceCode, analysis.getTotalRecords());

        String indexName = generateIndexName(searchSpaceCode);

        // 生成索引设置
        IndexMappingConfig.IndexSettings settings = generateIndexSettings(analysis);

        // 生成字段映射
        Map<String, IndexMappingConfig.FieldMapping> fieldMappings = generateFieldMappings(analysis);

        // 生成分析器配置
        Map<String, Object> analyzers = generateAnalyzers();

        IndexMappingConfig config = IndexMappingConfig.builder()
                .indexName(indexName)
                .settings(settings)
                .fieldMappings(fieldMappings)
                .dynamicMapping(true) // 允许动态映射以处理未预见的字段
                .analyzers(analyzers)
                .aliases(List.of(searchSpaceCode + "_current")) // 添加别名便于查询
                .build();

        log.info("索引配置生成完成: indexName={}, fieldsCount={}",
                indexName, fieldMappings.size());

        return config;
    }

    /**
     * 生成索引名称
     */
    private String generateIndexName(String searchSpaceCode) {
        // 直接使用搜索空间的code作为索引名
        return searchSpaceCode;
    }

    /**
     * 生成索引设置
     */
    private IndexMappingConfig.IndexSettings generateIndexSettings(JsonSchemaAnalysis analysis) {
        int totalRecords = analysis.getTotalRecords();

        // 根据数据量调整分片数
        int numberOfShards = calculateOptimalShards(totalRecords);

        return IndexMappingConfig.IndexSettings.builder()
                .numberOfShards(numberOfShards)
                .numberOfReplicas(1) // 默认1个副本
                .refreshInterval("1s") // 实时搜索需求
                .maxResultWindow(100000) // 支持大量结果返回
                .enableSlowLog(true) // 启用慢查询日志
                .build();
    }

    /**
     * 计算最优分片数
     */
    private int calculateOptimalShards(int totalRecords) {
        if (totalRecords < 10000) return 1;
        if (totalRecords < 100000) return 2;
        if (totalRecords < 1000000) return 3;
        return 5; // 最多5个分片，避免过度分片
    }

    /**
     * 生成字段映射配置
     */
    private Map<String, IndexMappingConfig.FieldMapping> generateFieldMappings(JsonSchemaAnalysis analysis) {
        Map<String, IndexMappingConfig.FieldMapping> mappings = new HashMap<>();

        for (FieldAnalysisResult fieldResult : analysis.getFieldAnalysis().values()) {
            IndexMappingConfig.FieldMapping mapping = generateFieldMapping(fieldResult);
            mappings.put(fieldResult.getFieldName(), mapping);
        }

        // 添加系统字段
        addSystemFields(mappings);

        return mappings;
    }

    /**
     * 生成单个字段的映射配置
     */
    private IndexMappingConfig.FieldMapping generateFieldMapping(FieldAnalysisResult fieldResult) {
        String fieldName = fieldResult.getFieldName();
        FieldType fieldType = fieldResult.getInferredType();
        String elasticsearchType = TYPE_MAPPING.get(fieldType);

        IndexMappingConfig.FieldMapping.FieldMappingBuilder builder = IndexMappingConfig.FieldMapping.builder()
                .fieldName(fieldName)
                .elasticsearchType(elasticsearchType)
                .index(true)
                .store(false)
                .docValues(true);

        // 特殊字段处理
        configureSpecialFields(builder, fieldResult);

        // 文本字段特殊配置
        if ("text".equals(elasticsearchType)) {
            configureTextField(builder, fieldResult);
        }

        // 数值字段特殊配置
        if (isNumericType(elasticsearchType)) {
            configureNumericField(builder, fieldResult);
        }

        // 日期字段特殊配置
        if ("date".equals(elasticsearchType)) {
            configureDateField(builder, fieldResult);
        }

        return builder.build();
    }

    /**
     * 配置特殊字段（如ID字段）
     */
    private void configureSpecialFields(IndexMappingConfig.FieldMapping.FieldMappingBuilder builder,
                                      FieldAnalysisResult fieldResult) {
        String fieldName = fieldResult.getFieldName().toLowerCase();

        // ID字段配置
        if (fieldResult.isSuggestAsId() || fieldName.contains("id") || fieldName.equals("_id")) {
            builder.elasticsearchType("keyword");
            builder.index(true);
            builder.docValues(true);
            return;
        }

        // 建议索引的字段
        if (fieldResult.isSuggestIndex()) {
            builder.boost(1.2); // 增加权重
        }

        // 高重要性字段
        if (fieldResult.getImportance() > 80) {
            builder.boost(1.1);
        }
    }

    /**
     * 配置文本字段
     */
    private void configureTextField(IndexMappingConfig.FieldMapping.FieldMappingBuilder builder,
                                  FieldAnalysisResult fieldResult) {
        // 设置分析器
        String analyzer = determineAnalyzer(fieldResult);
        builder.analyzer(analyzer);

        // 添加keyword子字段用于精确匹配和聚合
        Map<String, IndexMappingConfig.FieldMapping> fields = new HashMap<>();
        fields.put("keyword", IndexMappingConfig.FieldMapping.builder()
                .elasticsearchType("keyword")
                .index(true)
                .docValues(true)
                .build());

        builder.fields(fields);

        // 文本字段特殊配置
        IndexMappingConfig.TextFieldConfig textConfig = IndexMappingConfig.TextFieldConfig.builder()
                .enablePositionIncrements(true)
                .termVector(true) // 支持高亮
                .build();

        builder.textConfig(textConfig);
    }

    /**
     * 配置数值字段
     */
    private void configureNumericField(IndexMappingConfig.FieldMapping.FieldMappingBuilder builder,
                                     FieldAnalysisResult fieldResult) {
        IndexMappingConfig.NumericFieldConfig numericConfig = IndexMappingConfig.NumericFieldConfig.builder()
                .coerce(true) // 允许类型转换
                .build();

        builder.numericConfig(numericConfig);
        builder.ignoreMalformed(true); // 忽略格式错误的数值
    }

    /**
     * 配置日期字段
     */
    private void configureDateField(IndexMappingConfig.FieldMapping.FieldMappingBuilder builder,
                                  FieldAnalysisResult fieldResult) {
        // 支持多种日期格式
        builder.format("strict_date_optional_time||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis");
        builder.ignoreMalformed(true);
    }

    /**
     * 确定最适合的分析器
     */
    private String determineAnalyzer(FieldAnalysisResult fieldResult) {
        String fieldName = fieldResult.getFieldName().toLowerCase();

        // 名称类字段使用标准分析器
        if (fieldName.contains("name") || fieldName.contains("title")) {
            return "standard";
        }

        // 内容类字段使用中文分析器（如果有的话）
        if (fieldName.contains("content") || fieldName.contains("description")) {
            return "standard"; // 可以配置为中文分析器如ik_max_word
        }

        // 代码类字段使用keyword分析器
        if (fieldName.contains("code") || fieldName.contains("key")) {
            return "keyword";
        }

        return "standard";
    }

    /**
     * 判断是否为数值类型
     */
    private boolean isNumericType(String elasticsearchType) {
        return "long".equals(elasticsearchType) ||
               "integer".equals(elasticsearchType) ||
               "double".equals(elasticsearchType) ||
               "float".equals(elasticsearchType);
    }

    /**
     * 添加系统字段
     */
    private void addSystemFields(Map<String, IndexMappingConfig.FieldMapping> mappings) {
        // 搜索空间标识字段
        mappings.put("_searchSpaceCode", IndexMappingConfig.FieldMapping.builder()
                .fieldName("_searchSpaceCode")
                .elasticsearchType("keyword")
                .index(true)
                .docValues(true)
                .build());

        // 文档ID字段
        mappings.put("_documentId", IndexMappingConfig.FieldMapping.builder()
                .fieldName("_documentId")
                .elasticsearchType("keyword")
                .index(true)
                .docValues(true)
                .build());

        // 导入时间戳字段
        mappings.put("_importTimestamp", IndexMappingConfig.FieldMapping.builder()
                .fieldName("_importTimestamp")
                .elasticsearchType("date")
                .format("strict_date_time")
                .index(true)
                .docValues(true)
                .build());

        // 数据版本字段
        mappings.put("_dataVersion", IndexMappingConfig.FieldMapping.builder()
                .fieldName("_dataVersion")
                .elasticsearchType("integer")
                .index(true)
                .docValues(true)
                .build());
    }

    /**
     * 生成分析器配置
     */
    private Map<String, Object> generateAnalyzers() {
        Map<String, Object> analyzers = new HashMap<>();

        // 自定义中文分析器配置（如果需要的话）
        analyzers.put("custom_text", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "stop")
        ));

        return analyzers;
    }

    /**
     * 验证索引配置的有效性
     *
     * @param config 索引配置
     * @return 验证结果
     */
    public boolean validateIndexConfig(IndexMappingConfig config) {
        if (config == null || config.getIndexName() == null || config.getSettings() == null) {
            log.error("索引配置无效：配置对象或关键属性为空");
            return false;
        }

        if (config.getFieldMappings() == null || config.getFieldMappings().isEmpty()) {
            log.error("索引配置无效：没有字段映射");
            return false;
        }

        // 验证分片数合理性
        Integer shards = config.getSettings().getNumberOfShards();
        if (shards == null || shards < 1 || shards > 10) {
            log.error("索引配置无效：分片数不合理 [{}]", shards);
            return false;
        }

        log.info("索引配置验证通过：{}", config.getIndexName());
        return true;
    }
}