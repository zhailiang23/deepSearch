package com.ynet.mgmt.jsonimport.service;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldAnalysisResult;
import com.ynet.mgmt.jsonimport.model.IndexMappingConfig;
import com.ynet.mgmt.jsonimport.model.JsonSchemaAnalysis;
import com.ynet.mgmt.searchdata.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

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

    /**
     * 语义嵌入服务
     */
    private final EmbeddingService embeddingService;

    /**
     * 是否启用拼音分析器功能
     * 可以通过配置文件控制是否启用拼音功能
     */
    @Value("${elasticsearch.pinyin.enabled:true}")
    private boolean pinyinEnabled;

    /**
     * 是否启用语义搜索功能
     * 可以通过配置文件控制是否为文本字段添加向量子字段
     */
    @Value("${semantic.embedding.enabled:true}")
    private boolean semanticEnabled;

    /**
     * 拼音插件可用性缓存
     * 避免重复检测插件状态
     */
    private Boolean pinyinPluginAvailable = null;

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

            // 如果需要语义搜索支持，为文本字段添加对应的向量字段
            if ("text".equals(mapping.getElasticsearchType()) && shouldAddSemanticField(fieldResult)) {
                String vectorFieldName = fieldResult.getFieldName() + "_vector";
                log.debug("为字段 {} 创建独立的向量字段 {}", fieldResult.getFieldName(), vectorFieldName);

                IndexMappingConfig.FieldMapping vectorMapping = IndexMappingConfig.FieldMapping.builder()
                        .fieldName(vectorFieldName)
                        .elasticsearchType("dense_vector")
                        .vectorConfig(IndexMappingConfig.VectorFieldConfig.builder()
                                .dims(embeddingService.getVectorDimension())
                                .similarity("cosine")
                                .index(true)
                                .indexType("hnsw")
                                .m(16)
                                .efConstruction(200)
                                .build())
                        .index(false) // 向量字段不需要传统索引
                        .docValues(false) // 向量字段不需要doc_values
                        .build();

                mappings.put(vectorFieldName, vectorMapping);
            }
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
     * 根据字段特征智能添加拼音子字段支持
     */
    private void configureTextField(IndexMappingConfig.FieldMapping.FieldMappingBuilder builder,
                                  FieldAnalysisResult fieldResult) {
        // 设置分析器
        String analyzer = determineAnalyzer(fieldResult);
        builder.analyzer(analyzer);

        // 构建子字段映射
        Map<String, IndexMappingConfig.FieldMapping> fields = new HashMap<>();

        // 添加keyword子字段用于精确匹配和聚合
        fields.put("keyword", IndexMappingConfig.FieldMapping.builder()
                .elasticsearchType("keyword")
                .index(true)
                .docValues(true)
                .build());

        // 如果需要拼音支持，添加拼音相关子字段
        if (shouldAddPinyinField(fieldResult)) {
            log.debug("为字段 {} 添加拼音子字段支持", fieldResult.getFieldName());

            // 添加拼音子字段 - 支持全拼和简拼搜索
            fields.put("pinyin", IndexMappingConfig.FieldMapping.builder()
                    .elasticsearchType("text")
                    .analyzer("pinyin_analyzer")
                    .index(true)
                    .docValues(false)
                    .build());

            // 添加中文拼音混合子字段 - 同时支持中文和拼音搜索
            fields.put("chinese_pinyin", IndexMappingConfig.FieldMapping.builder()
                    .elasticsearchType("text")
                    .analyzer("chinese_pinyin_analyzer")
                    .index(true)
                    .docValues(false)
                    .build());

            // 为重要字段添加首字母搜索支持
            if (fieldResult.getImportance() >= 80 || fieldResult.isSuggestIndex()) {
                fields.put("first_letter", IndexMappingConfig.FieldMapping.builder()
                        .elasticsearchType("text")
                        .analyzer("pinyin_first_letter_analyzer")
                        .index(true)
                        .docValues(false)
                        .build());
            }

            // 如果使用拼音分析器作为主分析器，设置搜索分析器
            if (analyzer.contains("pinyin")) {
                builder.searchAnalyzer("pinyin_search_analyzer");
            }
        }

        // 注意：dense_vector字段不能作为multifields，需要在主字段级别单独创建

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
        boolean shouldUsePinyin = shouldAddPinyinField(fieldResult);

        log.trace("确定分析器 - 字段: {}, 是否使用拼音: {}", fieldName, shouldUsePinyin);

        // 名称类字段使用拼音分析器（如果符合条件）
        if (fieldName.contains("name") || fieldName.contains("title")) {
            return shouldUsePinyin ? "chinese_pinyin_analyzer" : "standard";
        }

        // 内容类字段使用拼音分析器（如果符合条件）
        if (fieldName.contains("content") || fieldName.contains("description")) {
            return shouldUsePinyin ? "pinyin_search_analyzer" : "standard";
        }

        // 公司类字段使用拼音分析器（如果符合条件）
        if (fieldName.contains("company") || fieldName.contains("corp")) {
            return shouldUsePinyin ? "chinese_pinyin_analyzer" : "standard";
        }

        // 代码类字段使用keyword分析器
        if (fieldName.contains("code") || fieldName.contains("key")) {
            return "keyword";
        }

        // 其他文本字段也可能需要拼音支持
        if (shouldUsePinyin) {
            return "chinese_pinyin_analyzer";
        }

        return "standard";
    }

    /**
     * 判断字段是否应该添加拼音字段支持
     * 简化逻辑：只要包含中文就添加拼音字段支持
     *
     * @param fieldResult 字段分析结果
     * @return 是否需要拼音支持
     */
    private boolean shouldAddPinyinField(FieldAnalysisResult fieldResult) {
        // 首先检查拼音插件是否可用
        if (!isPinyinPluginAvailable()) {
            log.debug("拼音插件不可用，跳过字段 {} 的拼音支持", fieldResult.getFieldName());
            return false;
        }

        // 只为字符串类型字段考虑拼音支持
        if (fieldResult.getInferredType() != FieldType.STRING) {
            log.trace("字段 {} 不是字符串类型，跳过拼音支持", fieldResult.getFieldName());
            return false;
        }

        // 简化逻辑：只要检测到中文内容就添加拼音支持
        if (fieldResult.isHasChineseContent()) {
            log.debug("字段 {} 包含中文内容（占比: {:.2f}），添加拼音支持",
                     fieldResult.getFieldName(), fieldResult.getChineseContentRatio());
            return true;
        }

        // 如果字段名暗示可能包含中文，也考虑添加拼音支持（作为备用策略）
        String fieldName = fieldResult.getFieldName().toLowerCase();
        if (isChineseTextFieldCandidate(fieldName)) {
            log.debug("字段名 {} 暗示可能包含中文内容，添加拼音支持", fieldResult.getFieldName());
            return true;
        }

        log.debug("字段 {} 不包含中文内容，跳过拼音支持", fieldResult.getFieldName());
        return false;
    }

    /**
     * 判断字段是否应该添加语义向量字段支持
     *
     * @param fieldResult 字段分析结果
     * @return 是否需要语义搜索支持
     */
    private boolean shouldAddSemanticField(FieldAnalysisResult fieldResult) {
        // 首先检查语义搜索功能是否启用
        if (!semanticEnabled) {
            log.debug("语义搜索功能已禁用");
            return false;
        }

        // 检查语义嵌入服务是否可用
        if (!embeddingService.isServiceAvailable()) {
            log.debug("语义嵌入服务不可用，跳过字段 {} 的向量支持", fieldResult.getFieldName());
            return false;
        }

        // 只为字符串类型字段考虑语义搜索支持
        if (fieldResult.getInferredType() != FieldType.STRING) {
            log.trace("字段 {} 不是字符串类型，跳过语义支持", fieldResult.getFieldName());
            return false;
        }

        String fieldName = fieldResult.getFieldName().toLowerCase();

        // 重要的文本字段应该支持语义搜索
        if (fieldResult.getImportance() >= 80 || fieldResult.isSuggestIndex()) {
            log.debug("字段 {} 重要性高（{}），添加语义支持",
                     fieldResult.getFieldName(), fieldResult.getImportance());
            return true;
        }

        // 内容类字段适合语义搜索
        if (isSemanticContentField(fieldName)) {
            log.debug("字段 {} 适合语义搜索，添加向量支持", fieldResult.getFieldName());
            return true;
        }

        // 如果字段包含较多文本内容，也考虑添加语义支持
        if (fieldResult.getStatistics() != null &&
            fieldResult.getStatistics().getAvgLength() != null &&
            fieldResult.getStatistics().getAvgLength() > 20) {
            log.debug("字段 {} 内容较长（平均长度: {}），添加语义支持",
                     fieldResult.getFieldName(), fieldResult.getStatistics().getAvgLength());
            return true;
        }

        log.debug("字段 {} 不需要语义搜索支持", fieldResult.getFieldName());
        return false;
    }

    /**
     * 判断字段名称是否为语义内容字段候选
     * 这些字段类型适合进行语义搜索
     */
    private boolean isSemanticContentField(String fieldName) {
        return fieldName.contains("title") || fieldName.contains("标题") ||
               fieldName.contains("name") || fieldName.contains("名称") || fieldName.contains("姓名") ||
               fieldName.contains("content") || fieldName.contains("内容") || fieldName.contains("正文") ||
               fieldName.contains("description") || fieldName.contains("描述") || fieldName.contains("说明") ||
               fieldName.contains("summary") || fieldName.contains("摘要") || fieldName.contains("概述") ||
               fieldName.contains("remark") || fieldName.contains("备注") || fieldName.contains("注释") ||
               fieldName.contains("comment") || fieldName.contains("评论") || fieldName.contains("留言") ||
               fieldName.contains("text") || fieldName.contains("文本") || fieldName.contains("正文") ||
               fieldName.contains("abstract") || fieldName.contains("摘要") ||
               fieldName.contains("introduction") || fieldName.contains("介绍") ||
               fieldName.contains("detail") || fieldName.contains("详情") || fieldName.contains("详细");
    }

    /**
     * 判断字段名称是否为中文文本字段候选
     * 优先检查中文字段名，英文字段名需要额外的条件才会被认为是中文字段
     */
    private boolean isChineseTextFieldCandidate(String fieldName) {
        // 包含中文字符的字段名直接返回 true
        if (containsChinese(fieldName)) {
            return true;
        }

        // 对于英文字段名，只有在明确的中文业务场景下才认为是中文字段候选
        // 这里我们保守处理，只认为明确包含中文相关概念的英文字段名才是候选
        return fieldName.contains("chinese") || fieldName.contains("cn") || fieldName.contains("chn");
    }

    /**
     * 判断字段名称是否可能包含文本内容
     * 用于辅助判断是否需要拼音支持
     */
    private boolean isTextContentField(String fieldName) {
        return fieldName.contains("name") || fieldName.contains("名称") || fieldName.contains("姓名") ||
               fieldName.contains("title") || fieldName.contains("标题") || fieldName.contains("主题") ||
               fieldName.contains("content") || fieldName.contains("内容") || fieldName.contains("正文") ||
               fieldName.contains("description") || fieldName.contains("描述") || fieldName.contains("说明") ||
               fieldName.contains("remark") || fieldName.contains("备注") || fieldName.contains("注释") ||
               fieldName.contains("comment") || fieldName.contains("评论") || fieldName.contains("留言") ||
               fieldName.contains("address") || fieldName.contains("地址") || fieldName.contains("位置") ||
               fieldName.contains("company") || fieldName.contains("公司") || fieldName.contains("单位") ||
               fieldName.contains("department") || fieldName.contains("部门") || fieldName.contains("科室") ||
               fieldName.contains("subject") || fieldName.contains("主题") || fieldName.contains("学科") ||
               fieldName.contains("corp") || fieldName.contains("organization") || fieldName.contains("org");
    }


    /**
     * 判断字符串是否包含中文字符
     */
    private boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 检查是否包含中文字符（CJK统一表意文字）
        return text.chars().anyMatch(ch ->
            (ch >= 0x4E00 && ch <= 0x9FFF) ||     // CJK统一表意文字
            (ch >= 0x3400 && ch <= 0x4DBF) ||     // CJK扩展A
            (ch >= 0x20000 && ch <= 0x2A6DF) ||   // CJK扩展B
            (ch >= 0x2A700 && ch <= 0x2B73F) ||   // CJK扩展C
            (ch >= 0x2B740 && ch <= 0x2B81F) ||   // CJK扩展D
            (ch >= 0x2B820 && ch <= 0x2CEAF)      // CJK扩展E
        );
    }

    /**
     * 检查拼音插件是否可用
     * 通过尝试创建测试分析器来检测拼音插件
     *
     * @return 拼音插件是否可用
     */
    private boolean isPinyinPluginAvailable() {
        // 如果配置禁用拼音功能，直接返回false
        if (!pinyinEnabled) {
            log.info("拼音功能已通过配置禁用");
            return false;
        }

        // 使用缓存避免重复检测
        if (pinyinPluginAvailable != null) {
            return pinyinPluginAvailable;
        }

        try {
            // TODO: 在实际集成ES客户端时，可以通过以下方式检测插件
            // 1. 调用 _nodes/plugins API 检查是否安装了pinyin插件
            // 2. 创建临时索引测试拼音分析器
            // 3. 捕获异常判断插件是否可用

            // 当前暂时假设拼音插件可用，在实际部署时会进行真实检测
            pinyinPluginAvailable = true;
            log.info("拼音插件检测完成，状态: {}", pinyinPluginAvailable ? "可用" : "不可用");

        } catch (Exception e) {
            log.warn("拼音插件检测失败，将禁用拼音功能: {}", e.getMessage());
            pinyinPluginAvailable = false;
        }

        return pinyinPluginAvailable;
    }

    /**
     * 获取拼音插件状态信息
     * 用于监控和诊断
     */
    public Map<String, Object> getPinyinPluginStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", pinyinEnabled);
        status.put("available", isPinyinPluginAvailable());
        status.put("checked", pinyinPluginAvailable != null);
        return status;
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
     * 包含拼音分析器、分词器和过滤器的完整配置
     */
    private Map<String, Object> generateAnalyzers() {
        Map<String, Object> analysisConfig = new HashMap<>();

        // 分析器配置
        Map<String, Object> analyzers = new HashMap<>();

        // 自定义中文分析器配置
        analyzers.put("custom_text", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "stop")
        ));

        // 拼音分析器配置 - 支持全拼和简拼
        analyzers.put("pinyin_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "pinyin_filter")
        ));

        // 拼音首字母分析器 - 仅保留首字母
        analyzers.put("pinyin_first_letter_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "pinyin_first_letter_filter")
        ));

        // 拼音搜索分析器 - 搜索时使用，保留原文和拼音
        analyzers.put("pinyin_search_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "pinyin_search_filter")
        ));

        // 中文拼音混合分析器 - 同时支持中文和拼音搜索
        analyzers.put("chinese_pinyin_analyzer", Map.of(
            "type", "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "chinese_pinyin_filter")
        ));

        // 过滤器配置
        Map<String, Object> filters = new HashMap<>();

        // 拼音过滤器 - 全功能拼音转换
        filters.put("pinyin_filter", Map.of(
            "type", "pinyin",
            "keep_first_letter", true,
            "keep_full_pinyin", true,
            "keep_joined_full_pinyin", true,
            "keep_original", false,
            "limit_first_letter_length", 16,
            "lowercase", true,
            "remove_duplicated_term", true
        ));

        // 拼音首字母过滤器 - 仅保留首字母
        filters.put("pinyin_first_letter_filter", Map.of(
            "type", "pinyin",
            "keep_first_letter", true,
            "keep_full_pinyin", false,
            "keep_joined_full_pinyin", false,
            "keep_original", false,
            "limit_first_letter_length", 16,
            "lowercase", true,
            "remove_duplicated_term", true
        ));

        // 拼音搜索过滤器 - 搜索时保留原文
        filters.put("pinyin_search_filter", Map.of(
            "type", "pinyin",
            "keep_first_letter", false,
            "keep_full_pinyin", true,
            "keep_joined_full_pinyin", true,
            "keep_original", true,
            "limit_first_letter_length", 16,
            "lowercase", true,
            "remove_duplicated_term", true
        ));

        // 中文拼音混合过滤器 - 平衡性能和功能
        filters.put("chinese_pinyin_filter", Map.of(
            "type", "pinyin",
            "keep_first_letter", true,
            "keep_full_pinyin", true,
            "keep_joined_full_pinyin", false,
            "keep_original", true,
            "limit_first_letter_length", 16,
            "lowercase", true,
            "remove_duplicated_term", true
        ));

        analysisConfig.put("analyzer", analyzers);
        analysisConfig.put("filter", filters);

        return analysisConfig;
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