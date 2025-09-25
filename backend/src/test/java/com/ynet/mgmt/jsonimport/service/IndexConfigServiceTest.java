package com.ynet.mgmt.jsonimport.service;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldAnalysisResult;
import com.ynet.mgmt.jsonimport.model.FieldStatistics;
import com.ynet.mgmt.jsonimport.model.IndexMappingConfig;
import com.ynet.mgmt.jsonimport.model.JsonSchemaAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IndexConfigService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "elasticsearch.pinyin.enabled=true"
})
class IndexConfigServiceTest {

    @InjectMocks
    private IndexConfigService indexConfigService;

    private JsonSchemaAnalysis sampleAnalysis;

    @BeforeEach
    void setUp() {
        // 启用拼音功能用于测试
        ReflectionTestUtils.setField(indexConfigService, "pinyinEnabled", true);

        // 重置插件可用性缓存，确保每次测试都重新检测
        ReflectionTestUtils.setField(indexConfigService, "pinyinPluginAvailable", null);
        // 创建测试用的JSON分析结果
        FieldAnalysisResult idField = FieldAnalysisResult.builder()
                .fieldName("id")
                .inferredType(FieldType.INTEGER)
                .confidence(1.0)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(1000)
                        .nullCount(0)
                        .nullRatio(0.0)
                        .uniqueCount(1000)
                        .uniqueRatio(1.0)
                        .qualityScore(100)
                        .build())
                .suggestAsId(true)
                .suggestIndex(true)
                .importance(100)
                .build();

        FieldAnalysisResult nameField = FieldAnalysisResult.builder()
                .fieldName("name")
                .inferredType(FieldType.STRING)
                .confidence(0.95)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(980)
                        .nullCount(20)
                        .nullRatio(0.02)
                        .uniqueCount(950)
                        .uniqueRatio(0.95)
                        .qualityScore(90)
                        .build())
                .suggestAsId(false)
                .suggestIndex(true)
                .importance(85)
                .build();

        FieldAnalysisResult descriptionField = FieldAnalysisResult.builder()
                .fieldName("description")
                .inferredType(FieldType.STRING)
                .confidence(0.9)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(800)
                        .nullCount(200)
                        .nullRatio(0.2)
                        .uniqueCount(750)
                        .uniqueRatio(0.75)
                        .qualityScore(75)
                        .build())
                .suggestAsId(false)
                .suggestIndex(false)
                .importance(60)
                .build();

        Map<String, FieldAnalysisResult> fieldAnalysis = Map.of(
                "id", idField,
                "name", nameField,
                "description", descriptionField
        );

        sampleAnalysis = JsonSchemaAnalysis.builder()
                .totalRecords(1000)
                .totalFields(3)
                .fieldAnalysis(fieldAnalysis)
                .analyzedAt(LocalDateTime.now())
                .sourceFileHash("test-hash")
                .analysisDurationMs(1000L)
                .overallQualityScore(85)
                .recommendedIdField("id")
                .recommendedIndexFields(List.of("id", "name"))
                .build();
    }

    @Test
    void testGenerateIndexConfig_SmallDataset() {
        // Given
        String searchSpaceCode = "test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, sampleAnalysis);

        // Then
        assertNotNull(config);
        assertNotNull(config.getIndexName());
        assertEquals("test_space", config.getIndexName());

        // 验证索引设置
        assertNotNull(config.getSettings());
        assertEquals(1, config.getSettings().getNumberOfShards()); // 小数据集应该是1个分片
        assertEquals(1, config.getSettings().getNumberOfReplicas());
        assertEquals("1s", config.getSettings().getRefreshInterval());

        // 验证字段映射
        assertNotNull(config.getFieldMappings());
        assertTrue(config.getFieldMappings().containsKey("id"));
        assertTrue(config.getFieldMappings().containsKey("name"));
        assertTrue(config.getFieldMappings().containsKey("description"));

        // 验证系统字段
        assertTrue(config.getFieldMappings().containsKey("_searchSpaceCode"));
        assertTrue(config.getFieldMappings().containsKey("_documentId"));
        assertTrue(config.getFieldMappings().containsKey("_importTimestamp"));
        assertTrue(config.getFieldMappings().containsKey("_dataVersion"));
    }

    @Test
    void testGenerateIndexConfig_LargeDataset() {
        // Given
        String searchSpaceCode = "large_space";
        // 创建大数据集的分析结果
        JsonSchemaAnalysis largeAnalysis = sampleAnalysis.toBuilder()
                .totalRecords(500000) // 50万条记录
                .build();

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, largeAnalysis);

        // Then
        assertNotNull(config);
        // 大数据集应该有更多分片
        assertTrue(config.getSettings().getNumberOfShards() > 1);
        assertTrue(config.getSettings().getNumberOfShards() <= 5); // 不超过5个分片
    }

    @Test
    void testGenerateIndexConfig_IdFieldMapping() {
        // Given
        String searchSpaceCode = "test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, sampleAnalysis);

        // Then
        IndexMappingConfig.FieldMapping idMapping = config.getFieldMappings().get("id");
        assertNotNull(idMapping);
        // ID字段应该被识别为keyword类型（即使原始类型是integer）
        assertEquals("keyword", idMapping.getElasticsearchType());
        assertTrue(idMapping.getIndex());
        assertTrue(idMapping.getDocValues());
    }

    @Test
    void testGenerateIndexConfig_TextFieldMapping() {
        // Given
        String searchSpaceCode = "test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, sampleAnalysis);

        // Then
        IndexMappingConfig.FieldMapping nameMapping = config.getFieldMappings().get("name");
        assertNotNull(nameMapping);
        assertEquals("text", nameMapping.getElasticsearchType());
        // 由于原测试的name字段没有样本数据，且虽然重要性85>70但字段名只是"name"不包含中文，所以使用标准分析器
        assertEquals("standard", nameMapping.getAnalyzer());

        // 应该有keyword子字段
        assertNotNull(nameMapping.getFields());
        assertTrue(nameMapping.getFields().containsKey("keyword"));
        assertEquals("keyword", nameMapping.getFields().get("keyword").getElasticsearchType());
    }

    @Test
    void testValidateIndexConfig_ValidConfig() {
        // Given
        IndexMappingConfig validConfig = IndexMappingConfig.builder()
                .indexName("test_index")
                .settings(IndexMappingConfig.IndexSettings.builder()
                        .numberOfShards(1)
                        .numberOfReplicas(1)
                        .refreshInterval("1s")
                        .build())
                .fieldMappings(Map.of(
                        "test_field", IndexMappingConfig.FieldMapping.builder()
                                .fieldName("test_field")
                                .elasticsearchType("text")
                                .build()
                ))
                .build();

        // When
        boolean isValid = indexConfigService.validateIndexConfig(validConfig);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateIndexConfig_InvalidConfig_NullConfig() {
        // When
        boolean isValid = indexConfigService.validateIndexConfig(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateIndexConfig_InvalidConfig_NoIndexName() {
        // Given
        IndexMappingConfig invalidConfig = IndexMappingConfig.builder()
                .indexName(null) // 无效：没有索引名
                .settings(IndexMappingConfig.IndexSettings.builder()
                        .numberOfShards(1)
                        .build())
                .fieldMappings(Map.of())
                .build();

        // When
        boolean isValid = indexConfigService.validateIndexConfig(invalidConfig);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateIndexConfig_InvalidConfig_InvalidShardCount() {
        // Given
        IndexMappingConfig invalidConfig = IndexMappingConfig.builder()
                .indexName("test_index")
                .settings(IndexMappingConfig.IndexSettings.builder()
                        .numberOfShards(15) // 无效：超过最大分片数
                        .build())
                .fieldMappings(Map.of(
                        "test_field", IndexMappingConfig.FieldMapping.builder()
                                .fieldName("test_field")
                                .elasticsearchType("text")
                                .build()
                ))
                .build();

        // When
        boolean isValid = indexConfigService.validateIndexConfig(invalidConfig);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testCalculateOptimalShards() {
        // 使用反射测试私有方法，或者提取为受保护的方法
        // 这里我们通过创建不同大小的数据集来间接测试

        // 小数据集 (< 10,000)
        JsonSchemaAnalysis smallData = sampleAnalysis.toBuilder().totalRecords(5000).build();
        IndexMappingConfig smallConfig = indexConfigService.generateIndexConfig("test", smallData);
        assertEquals(1, smallConfig.getSettings().getNumberOfShards());

        // 中等数据集 (10,000 - 100,000)
        JsonSchemaAnalysis mediumData = sampleAnalysis.toBuilder().totalRecords(50000).build();
        IndexMappingConfig mediumConfig = indexConfigService.generateIndexConfig("test", mediumData);
        assertEquals(2, mediumConfig.getSettings().getNumberOfShards());

        // 大数据集 (100,000 - 1,000,000)
        JsonSchemaAnalysis largeData = sampleAnalysis.toBuilder().totalRecords(500000).build();
        IndexMappingConfig largeConfig = indexConfigService.generateIndexConfig("test", largeData);
        assertEquals(3, largeConfig.getSettings().getNumberOfShards());

        // 超大数据集 (> 1,000,000)
        JsonSchemaAnalysis veryLargeData = sampleAnalysis.toBuilder().totalRecords(2000000).build();
        IndexMappingConfig veryLargeConfig = indexConfigService.generateIndexConfig("test", veryLargeData);
        assertEquals(5, veryLargeConfig.getSettings().getNumberOfShards());
    }

    @Test
    void testIndexMappingToElasticsearchMapping() {
        // Given
        String searchSpaceCode = "test_space";
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, sampleAnalysis);

        // When
        Map<String, Object> esMapping = config.toElasticsearchMapping();

        // Then
        assertNotNull(esMapping);
        assertTrue(esMapping.containsKey("settings"));
        assertTrue(esMapping.containsKey("mappings"));

        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) esMapping.get("settings");
        assertEquals(1, settings.get("number_of_shards"));
        assertEquals(1, settings.get("number_of_replicas"));
        assertEquals("1s", settings.get("refresh_interval"));

        @SuppressWarnings("unchecked")
        Map<String, Object> mappings = (Map<String, Object>) esMapping.get("mappings");
        assertTrue((Boolean) mappings.get("dynamic"));
        assertTrue(mappings.containsKey("properties"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) mappings.get("properties");
        assertTrue(properties.containsKey("id"));
        assertTrue(properties.containsKey("name"));
        assertTrue(properties.containsKey("description"));
    }

    @Test
    void testSystemFieldsGeneration() {
        // Given
        String searchSpaceCode = "test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, sampleAnalysis);

        // Then
        Map<String, IndexMappingConfig.FieldMapping> mappings = config.getFieldMappings();

        // 验证系统字段
        IndexMappingConfig.FieldMapping searchSpaceCodeField = mappings.get("_searchSpaceCode");
        assertNotNull(searchSpaceCodeField);
        assertEquals("keyword", searchSpaceCodeField.getElasticsearchType());

        IndexMappingConfig.FieldMapping documentIdField = mappings.get("_documentId");
        assertNotNull(documentIdField);
        assertEquals("keyword", documentIdField.getElasticsearchType());

        IndexMappingConfig.FieldMapping timestampField = mappings.get("_importTimestamp");
        assertNotNull(timestampField);
        assertEquals("date", timestampField.getElasticsearchType());

        IndexMappingConfig.FieldMapping versionField = mappings.get("_dataVersion");
        assertNotNull(versionField);
        assertEquals("integer", versionField.getElasticsearchType());
    }

    @Test
    void testPinyinFieldMapping_ChineseNameField() {
        // Given - 创建包含中文内容的字段
        FieldAnalysisResult chineseNameField = FieldAnalysisResult.builder()
                .fieldName("name")
                .inferredType(FieldType.STRING)
                .confidence(0.95)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(980)
                        .nullCount(20)
                        .nullRatio(0.02)
                        .uniqueCount(950)
                        .uniqueRatio(0.95)
                        .qualityScore(90)
                        .build())
                .sampleValues(List.of("张三", "李四", "王五", "赵六", "陈七"))
                .suggestAsId(false)
                .suggestIndex(true)
                .importance(85)
                .build();

        Map<String, FieldAnalysisResult> fieldAnalysis = Map.of(
                "name", chineseNameField
        );

        JsonSchemaAnalysis analysisWithChinese = sampleAnalysis.toBuilder()
                .fieldAnalysis(fieldAnalysis)
                .build();

        String searchSpaceCode = "chinese_test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, analysisWithChinese);

        // Then
        IndexMappingConfig.FieldMapping nameMapping = config.getFieldMappings().get("name");
        assertNotNull(nameMapping);
        assertEquals("text", nameMapping.getElasticsearchType());

        // 验证使用了拼音分析器
        assertEquals("chinese_pinyin_analyzer", nameMapping.getAnalyzer());
        assertEquals("pinyin_search_analyzer", nameMapping.getSearchAnalyzer());

        // 验证拼音子字段
        assertNotNull(nameMapping.getFields());
        assertTrue(nameMapping.getFields().containsKey("pinyin"));
        assertTrue(nameMapping.getFields().containsKey("chinese_pinyin"));
        assertTrue(nameMapping.getFields().containsKey("first_letter")); // 高重要性字段应该有首字母字段

        // 验证拼音子字段配置
        IndexMappingConfig.FieldMapping pinyinField = nameMapping.getFields().get("pinyin");
        assertEquals("text", pinyinField.getElasticsearchType());
        assertEquals("pinyin_analyzer", pinyinField.getAnalyzer());

        IndexMappingConfig.FieldMapping chinesePinyinField = nameMapping.getFields().get("chinese_pinyin");
        assertEquals("text", chinesePinyinField.getElasticsearchType());
        assertEquals("chinese_pinyin_analyzer", chinesePinyinField.getAnalyzer());

        IndexMappingConfig.FieldMapping firstLetterField = nameMapping.getFields().get("first_letter");
        assertEquals("text", firstLetterField.getElasticsearchType());
        assertEquals("pinyin_first_letter_analyzer", firstLetterField.getAnalyzer());
    }

    @Test
    void testPinyinFieldMapping_EnglishNameField() {
        // Given - 创建包含英文内容的字段
        FieldAnalysisResult englishNameField = FieldAnalysisResult.builder()
                .fieldName("name")
                .inferredType(FieldType.STRING)
                .confidence(0.95)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(980)
                        .nullCount(20)
                        .nullRatio(0.02)
                        .uniqueCount(950)
                        .uniqueRatio(0.95)
                        .qualityScore(90)
                        .build())
                .sampleValues(List.of("John", "Jane", "Bob", "Alice", "Charlie"))
                .suggestAsId(false)
                .suggestIndex(true)
                .importance(85)
                .build();

        Map<String, FieldAnalysisResult> fieldAnalysis = Map.of(
                "name", englishNameField
        );

        JsonSchemaAnalysis analysisWithEnglish = sampleAnalysis.toBuilder()
                .fieldAnalysis(fieldAnalysis)
                .build();

        String searchSpaceCode = "english_test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, analysisWithEnglish);

        // Then
        IndexMappingConfig.FieldMapping nameMapping = config.getFieldMappings().get("name");
        assertNotNull(nameMapping);
        assertEquals("text", nameMapping.getElasticsearchType());

        // 验证使用了标准分析器（不是拼音分析器）
        assertEquals("standard", nameMapping.getAnalyzer());
        assertNull(nameMapping.getSearchAnalyzer());

        // 验证没有拼音子字段
        assertNotNull(nameMapping.getFields());
        assertFalse(nameMapping.getFields().containsKey("pinyin"));
        assertFalse(nameMapping.getFields().containsKey("chinese_pinyin"));
        assertFalse(nameMapping.getFields().containsKey("first_letter"));

        // 应该仍然有keyword子字段
        assertTrue(nameMapping.getFields().containsKey("keyword"));
    }

    @Test
    void testPinyinFieldMapping_MixedContentField() {
        // Given - 创建包含中英文混合内容的字段
        FieldAnalysisResult mixedContentField = FieldAnalysisResult.builder()
                .fieldName("title")
                .inferredType(FieldType.STRING)
                .confidence(0.9)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(950)
                        .nullCount(50)
                        .nullRatio(0.05)
                        .uniqueCount(900)
                        .uniqueRatio(0.9)
                        .qualityScore(85)
                        .build())
                .sampleValues(List.of("产品介绍", "Product Introduction", "用户手册", "User Manual", "技术文档"))
                .suggestAsId(false)
                .suggestIndex(true)
                .importance(75)
                .build();

        Map<String, FieldAnalysisResult> fieldAnalysis = Map.of(
                "title", mixedContentField
        );

        JsonSchemaAnalysis analysisWithMixed = sampleAnalysis.toBuilder()
                .fieldAnalysis(fieldAnalysis)
                .build();

        String searchSpaceCode = "mixed_test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, analysisWithMixed);

        // Then
        IndexMappingConfig.FieldMapping titleMapping = config.getFieldMappings().get("title");
        assertNotNull(titleMapping);
        assertEquals("text", titleMapping.getElasticsearchType());

        // 验证使用了拼音分析器（因为有中文内容）
        assertEquals("chinese_pinyin_analyzer", titleMapping.getAnalyzer());
        assertEquals("pinyin_search_analyzer", titleMapping.getSearchAnalyzer());

        // 验证拼音子字段
        assertNotNull(titleMapping.getFields());
        assertTrue(titleMapping.getFields().containsKey("pinyin"));
        assertTrue(titleMapping.getFields().containsKey("chinese_pinyin"));
    }

    @Test
    void testPinyinAnalyzersGeneration() {
        // Given
        String searchSpaceCode = "test_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, sampleAnalysis);

        // Then
        assertNotNull(config.getAnalyzers());

        // 验证分析器存在
        @SuppressWarnings("unchecked")
        Map<String, Object> analyzers = (Map<String, Object>) config.getAnalyzers().get("analyzer");
        assertNotNull(analyzers);
        assertTrue(analyzers.containsKey("pinyin_analyzer"));
        assertTrue(analyzers.containsKey("pinyin_first_letter_analyzer"));
        assertTrue(analyzers.containsKey("pinyin_search_analyzer"));
        assertTrue(analyzers.containsKey("chinese_pinyin_analyzer"));

        // 验证过滤器存在
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) config.getAnalyzers().get("filter");
        assertNotNull(filters);
        assertTrue(filters.containsKey("pinyin_filter"));
        assertTrue(filters.containsKey("pinyin_first_letter_filter"));
        assertTrue(filters.containsKey("pinyin_search_filter"));
        assertTrue(filters.containsKey("chinese_pinyin_filter"));
    }

    @Test
    void testElasticsearchMappingWithPinyinAnalyzers() {
        // Given
        FieldAnalysisResult chineseField = FieldAnalysisResult.builder()
                .fieldName("company")
                .inferredType(FieldType.STRING)
                .confidence(0.9)
                .statistics(FieldStatistics.builder()
                        .totalCount(1000)
                        .nonNullCount(950)
                        .nullCount(50)
                        .build())
                .sampleValues(List.of("北京科技有限公司", "上海贸易公司", "深圳软件公司"))
                .suggestAsId(false)
                .suggestIndex(true)
                .importance(80)
                .build();

        Map<String, FieldAnalysisResult> fieldAnalysis = Map.of(
                "company", chineseField
        );

        JsonSchemaAnalysis analysisWithChinese = sampleAnalysis.toBuilder()
                .fieldAnalysis(fieldAnalysis)
                .build();

        String searchSpaceCode = "chinese_company_space";
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, analysisWithChinese);

        // When
        Map<String, Object> esMapping = config.toElasticsearchMapping();

        // Then
        assertNotNull(esMapping);

        // 验证设置中包含分析器配置
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) esMapping.get("settings");
        assertTrue(settings.containsKey("analysis"));

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) settings.get("analysis");
        assertTrue(analysis.containsKey("analyzer"));
        assertTrue(analysis.containsKey("filter"));

        // 验证映射中包含拼音字段配置
        @SuppressWarnings("unchecked")
        Map<String, Object> mappings = (Map<String, Object>) esMapping.get("mappings");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) mappings.get("properties");
        assertTrue(properties.containsKey("company"));

        @SuppressWarnings("unchecked")
        Map<String, Object> companyField = (Map<String, Object>) properties.get("company");
        assertEquals("chinese_pinyin_analyzer", companyField.get("analyzer"));
        assertEquals("pinyin_search_analyzer", companyField.get("search_analyzer"));

        // 验证子字段
        assertTrue(companyField.containsKey("fields"));
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) companyField.get("fields");
        assertTrue(fields.containsKey("pinyin"));
        assertTrue(fields.containsKey("chinese_pinyin"));
        assertTrue(fields.containsKey("first_letter"));
    }

    @Test
    void testPinyinPluginStatus() {
        // When
        Map<String, Object> status = indexConfigService.getPinyinPluginStatus();

        // Then
        assertNotNull(status);
        assertTrue(status.containsKey("enabled"));
        assertTrue(status.containsKey("available"));
        assertTrue(status.containsKey("checked"));

        // 验证状态值类型
        assertInstanceOf(Boolean.class, status.get("enabled"));
        assertInstanceOf(Boolean.class, status.get("available"));
        assertInstanceOf(Boolean.class, status.get("checked"));
    }

    @Test
    void testLowImportanceFieldWithoutPinyin() {
        // Given - 创建低重要性的中文字段
        FieldAnalysisResult lowImportanceField = FieldAnalysisResult.builder()
                .fieldName("备注")  // 中文字段名，但重要性低
                .inferredType(FieldType.STRING)
                .confidence(0.8)
                .statistics(FieldStatistics.builder().build())
                .sampleValues(List.of("test remark", "another remark"))  // 使用英文样本数据，确保不触发拼音
                .suggestAsId(false)
                .suggestIndex(false)  // 不建议索引
                .importance(40)  // 低重要性
                .build();

        Map<String, FieldAnalysisResult> fieldAnalysis = Map.of(
                "备注", lowImportanceField
        );

        JsonSchemaAnalysis analysisWithLowImportance = sampleAnalysis.toBuilder()
                .fieldAnalysis(fieldAnalysis)
                .build();

        String searchSpaceCode = "low_importance_space";

        // When
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, analysisWithLowImportance);

        // Then
        IndexMappingConfig.FieldMapping remarkMapping = config.getFieldMappings().get("备注");
        assertNotNull(remarkMapping);

        // 由于样本数据是英文，即使字段名是中文，也不应该使用拼音分析器
        assertEquals("standard", remarkMapping.getAnalyzer());
        assertNull(remarkMapping.getSearchAnalyzer());

        // 不应该有拼音子字段
        if (remarkMapping.getFields() != null) {
            assertFalse(remarkMapping.getFields().containsKey("pinyin"));
            assertFalse(remarkMapping.getFields().containsKey("chinese_pinyin"));
            assertFalse(remarkMapping.getFields().containsKey("first_letter"));
        }
    }
}