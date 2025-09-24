package com.ynet.mgmt.jsonimport.service;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldAnalysisResult;
import com.ynet.mgmt.jsonimport.model.JsonSchemaAnalysis;
import com.ynet.mgmt.jsonimport.util.FieldTypeInferrer;
import com.ynet.mgmt.jsonimport.util.StatisticsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JSON分析服务测试")
class JsonAnalysisServiceTest {

    @Mock
    private FieldTypeInferrer fieldTypeInferrer;

    @Mock
    private StatisticsCalculator statisticsCalculator;

    private JsonAnalysisService jsonAnalysisService;

    @BeforeEach
    void setUp() {
        jsonAnalysisService = new JsonAnalysisService(fieldTypeInferrer, statisticsCalculator);
    }

    @Test
    @DisplayName("分析简单JSON数据")
    void testAnalyzeSimpleJsonData() {
        // 准备测试数据
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("id", "1", "name", "张三", "age", "25", "active", "true"),
                Map.of("id", "2", "name", "李四", "age", "30", "active", "false"),
                Map.of("id", "3", "name", "王五", "age", "28", "active", "true")
        );

        // Mock类型推断结果
        FieldTypeInferrer.TypeInferenceResult idTypeResult = new FieldTypeInferrer.TypeInferenceResult(
                FieldType.INTEGER, 0.95, Collections.emptyList());
        FieldTypeInferrer.TypeInferenceResult nameTypeResult = new FieldTypeInferrer.TypeInferenceResult(
                FieldType.STRING, 1.0, Collections.emptyList());
        FieldTypeInferrer.TypeInferenceResult ageTypeResult = new FieldTypeInferrer.TypeInferenceResult(
                FieldType.INTEGER, 0.9, Collections.emptyList());
        FieldTypeInferrer.TypeInferenceResult activeTypeResult = new FieldTypeInferrer.TypeInferenceResult(
                FieldType.BOOLEAN, 0.85, Collections.emptyList());

        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(idTypeResult)
                .thenReturn(nameTypeResult)
                .thenReturn(ageTypeResult)
                .thenReturn(activeTypeResult);

        // Mock统计计算结果
        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        // 执行测试
        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(jsonData);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.getTotalRecords());
        assertEquals(4, result.getTotalFields());
        assertNotNull(result.getFieldAnalysis());
        assertEquals(4, result.getFieldAnalysis().size());

        // 验证字段存在
        assertTrue(result.getFieldAnalysis().containsKey("id"));
        assertTrue(result.getFieldAnalysis().containsKey("name"));
        assertTrue(result.getFieldAnalysis().containsKey("age"));
        assertTrue(result.getFieldAnalysis().containsKey("active"));

        assertNotNull(result.getAnalyzedAt());
        assertNotNull(result.getSourceFileHash());
        assertTrue(result.getAnalysisDurationMs() >= 0);
        assertNotNull(result.getConsistencyReport());
    }

    @Test
    @DisplayName("分析空数据")
    void testAnalyzeEmptyData() {
        List<Map<String, Object>> emptyData = Collections.emptyList();

        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(emptyData);

        assertNotNull(result);
        assertEquals(0, result.getTotalRecords());
        assertEquals(0, result.getTotalFields());
        assertTrue(result.getFieldAnalysis().isEmpty());
        assertEquals(0, result.getOverallQualityScore());
        assertNull(result.getRecommendedIdField());
        assertTrue(result.getRecommendedIndexFields().isEmpty());
        assertFalse(result.getConsistencyReport().isReadyForElasticsearch());
    }

    @Test
    @DisplayName("分析null数据")
    void testAnalyzeNullData() {
        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(null);

        assertNotNull(result);
        assertEquals(0, result.getTotalRecords());
        assertEquals(0, result.getTotalFields());
        assertTrue(result.getFieldAnalysis().isEmpty());
    }

    @Test
    @DisplayName("分析包含null记录的数据")
    void testAnalyzeDataWithNullRecords() {
        List<Map<String, Object>> dataWithNulls = Arrays.asList(
                Map.of("id", "1", "name", "张三"),
                null, // null记录
                Map.of("id", "2", "name", "李四"),
                Collections.emptyMap() // 空记录
        );

        // Mock返回值
        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 1.0, Collections.emptyList()));

        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(dataWithNulls);

        assertNotNull(result);
        assertEquals(4, result.getTotalRecords());
        // 应该只有id和name字段
        assertEquals(2, result.getTotalFields());
    }

    @Test
    @DisplayName("测试字段分析功能")
    void testAnalyzeField() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("email", "user1@example.com"),
                Map.of("email", "user2@test.org"),
                Map.of("email", "admin@company.net")
        );

        // Mock类型推断
        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.EMAIL, 0.95, Collections.emptyList()));

        // Mock统计计算
        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        FieldAnalysisResult result = jsonAnalysisService.analyzeField("email", jsonData);

        assertNotNull(result);
        assertEquals("email", result.getFieldName());
        assertEquals(FieldType.EMAIL, result.getInferredType());
        assertEquals(0.95, result.getConfidence());
        assertNotNull(result.getStatistics());
        assertNotNull(result.getSampleValues());
        assertNotNull(result.getIssues());
    }

    @Test
    @DisplayName("测试字段值分析功能")
    void testAnalyzeFieldValues() {
        List<Object> values = Arrays.asList("123", "456", "789", "999");

        // Mock返回值
        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.INTEGER, 0.9, Collections.emptyList()));

        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        FieldAnalysisResult result = jsonAnalysisService.analyzeFieldValues("numberField", values);

        assertNotNull(result);
        assertEquals("numberField", result.getFieldName());
        assertEquals(FieldType.INTEGER, result.getInferredType());
        assertEquals(0.9, result.getConfidence());
    }

    @Test
    @DisplayName("测试ID字段推荐")
    void testIdFieldRecommendation() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("id", "1", "user_id", "u001", "name", "张三"),
                Map.of("id", "2", "user_id", "u002", "name", "李四"),
                Map.of("id", "3", "user_id", "u003", "name", "王五")
        );

        // Mock ID字段的类型推断（高唯一性，高置信度）
        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 0.95, Collections.emptyList()));

        // Mock统计结果（高唯一性，低空值率）
        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockIdFieldStatistics());

        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(jsonData);

        // 应该推荐其中一个ID字段
        assertNotNull(result.getRecommendedIdField());
        assertTrue(result.getRecommendedIdField().contains("id"));
    }

    @Test
    @DisplayName("测试索引字段推荐")
    void testIndexFieldRecommendation() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("id", "1", "status", "active", "category", "A"),
                Map.of("id", "2", "status", "inactive", "category", "B"),
                Map.of("id", "3", "status", "active", "category", "A")
        );

        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 0.9, Collections.emptyList()));

        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockIndexFieldStatistics());

        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(jsonData);

        assertFalse(result.getRecommendedIndexFields().isEmpty());
        // status和category应该被推荐为索引字段
        assertTrue(result.getRecommendedIndexFields().contains("status") ||
                  result.getRecommendedIndexFields().contains("category"));
    }

    @Test
    @DisplayName("测试数据一致性报告")
    void testDataConsistencyReport() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("name", "张三", "age", "25"),
                Map.of("name", "李四", "age", "invalid"), // 无效年龄
                Map.of("name", "王五") // 缺少age字段
        );

        // Mock低置信度的类型推断
        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 0.6, Collections.emptyList()));

        // Mock包含问题的统计结果
        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockProblematicStatistics());

        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(jsonData);

        assertNotNull(result.getConsistencyReport());
        assertTrue(result.getConsistencyReport().getCompletenessScore() < 100);
        assertTrue(result.getConsistencyReport().getConsistencyScore() < 100);
        assertFalse(result.getConsistencyReport().getAnomalies().isEmpty());
        assertFalse(result.getConsistencyReport().getRecommendations().isEmpty());
        assertFalse(result.getConsistencyReport().isReadyForElasticsearch());
    }

    @Test
    @DisplayName("测试缓存功能")
    void testCacheFunctionality() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("id", "1", "name", "张三")
        );

        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 1.0, Collections.emptyList()));

        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        // 第一次分析
        JsonSchemaAnalysis result1 = jsonAnalysisService.analyzeJsonArray(jsonData);
        assertNotNull(result1);

        // 第二次分析同样的数据，应该使用缓存
        JsonSchemaAnalysis result2 = jsonAnalysisService.analyzeJsonArray(jsonData);
        assertNotNull(result2);

        // 哈希值应该相同
        assertEquals(result1.getSourceFileHash(), result2.getSourceFileHash());

        // 验证缓存统计
        Map<String, Object> cacheStats = jsonAnalysisService.getCacheStatistics();
        assertTrue((Integer) cacheStats.get("cacheSize") > 0);
    }

    @Test
    @DisplayName("测试缓存清理")
    void testCacheClear() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("id", "1", "name", "张三")
        );

        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 1.0, Collections.emptyList()));

        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        // 进行分析，产生缓存
        jsonAnalysisService.analyzeJsonArray(jsonData);

        // 验证缓存存在
        Map<String, Object> cacheStats = jsonAnalysisService.getCacheStatistics();
        assertTrue((Integer) cacheStats.get("cacheSize") > 0);

        // 清理缓存
        jsonAnalysisService.clearCache();

        // 验证缓存已清空
        cacheStats = jsonAnalysisService.getCacheStatistics();
        assertEquals(0, cacheStats.get("cacheSize"));
    }

    @Test
    @DisplayName("测试大数据集性能")
    void testLargeDataSetPerformance() {
        // 创建1000条记录
        List<Map<String, Object>> largeDataSet = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            largeDataSet.add(Map.of(
                    "id", String.valueOf(i),
                    "name", "用户" + i,
                    "age", String.valueOf(20 + (i % 50)),
                    "email", "user" + i + "@example.com"
            ));
        }

        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenReturn(new FieldTypeInferrer.TypeInferenceResult(
                        FieldType.STRING, 1.0, Collections.emptyList()));

        when(statisticsCalculator.calculateStatistics(any(), anyList(), any()))
                .thenReturn(createMockStatistics());

        long startTime = System.currentTimeMillis();
        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(largeDataSet);
        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(result);
        assertEquals(1000, result.getTotalRecords());
        assertTrue(duration < 10000, "分析1000条记录应该在10秒内完成，实际耗时: " + duration + "ms");
    }

    @Test
    @DisplayName("测试异常处理")
    void testExceptionHandling() {
        List<Map<String, Object>> jsonData = Arrays.asList(
                Map.of("field", "value")
        );

        // Mock抛出异常
        when(fieldTypeInferrer.inferFieldType(anyList()))
                .thenThrow(new RuntimeException("类型推断失败"));

        // 应该优雅处理异常，不抛出异常而是创建默认结果
        JsonSchemaAnalysis result = jsonAnalysisService.analyzeJsonArray(jsonData);
        
        // 验证异常被处理，创建了默认的字段分析结果
        assertNotNull(result);
        assertEquals(1, result.getTotalRecords());
        assertEquals(1, result.getTotalFields());
        assertTrue(result.getFieldAnalysis().containsKey("field"));
        
        // 验证默认字段分析结果的特征
        FieldAnalysisResult fieldResult = result.getFieldAnalysis().get("field");
        assertEquals("field", fieldResult.getFieldName());
        assertFalse(fieldResult.getIssues().isEmpty()); // 应该包含错误信息
    }

    // 辅助方法：创建Mock统计结果
    private com.ynet.mgmt.jsonimport.model.FieldStatistics createMockStatistics() {
        return com.ynet.mgmt.jsonimport.model.FieldStatistics.builder()
                .totalCount(3)
                .nonNullCount(3)
                .nullCount(0)
                .nullRatio(0.0)
                .uniqueCount(3)
                .uniqueRatio(1.0)
                .qualityScore(90)
                .hasOutliers(false)
                .topValues(Collections.emptyList())
                .build();
    }

    // 创建ID字段的Mock统计结果
    private com.ynet.mgmt.jsonimport.model.FieldStatistics createMockIdFieldStatistics() {
        return com.ynet.mgmt.jsonimport.model.FieldStatistics.builder()
                .totalCount(3)
                .nonNullCount(3)
                .nullCount(0)
                .nullRatio(0.0)
                .uniqueCount(3)
                .uniqueRatio(1.0) // 100%唯一性
                .qualityScore(95)
                .hasOutliers(false)
                .topValues(Collections.emptyList())
                .build();
    }

    // 创建索引字段的Mock统计结果
    private com.ynet.mgmt.jsonimport.model.FieldStatistics createMockIndexFieldStatistics() {
        return com.ynet.mgmt.jsonimport.model.FieldStatistics.builder()
                .totalCount(3)
                .nonNullCount(3)
                .nullCount(0)
                .nullRatio(0.0)
                .uniqueCount(2)
                .uniqueRatio(0.67) // 适中的唯一性
                .qualityScore(85)
                .hasOutliers(false)
                .topValues(Collections.emptyList())
                .build();
    }

    // 创建有问题的Mock统计结果
    private com.ynet.mgmt.jsonimport.model.FieldStatistics createMockProblematicStatistics() {
        return com.ynet.mgmt.jsonimport.model.FieldStatistics.builder()
                .totalCount(3)
                .nonNullCount(2)
                .nullCount(1)
                .nullRatio(0.33) // 33%空值率
                .uniqueCount(2)
                .uniqueRatio(1.0)
                .qualityScore(60) // 较低的质量评分
                .hasOutliers(true)
                .topValues(Collections.emptyList())
                .build();
    }
}