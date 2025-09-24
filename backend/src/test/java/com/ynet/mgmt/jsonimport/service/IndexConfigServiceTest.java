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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IndexConfigService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class IndexConfigServiceTest {

    @InjectMocks
    private IndexConfigService indexConfigService;

    private JsonSchemaAnalysis sampleAnalysis;

    @BeforeEach
    void setUp() {
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
        assertTrue(config.getIndexName().startsWith("searchspace_test_space_"));

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
}