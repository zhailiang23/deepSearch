package com.ynet.mgmt.searchspace.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImportStatistics DTO测试
 *
 * @author system
 * @since 1.0.0
 */
@DisplayName("ImportStatistics DTO测试")
class ImportStatisticsTest {

    @Test
    @DisplayName("测试构造函数和getter方法")
    void testConstructorAndGetters() {
        LocalDateTime importTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        ImportStatistics statistics = new ImportStatistics(2L, 3L, 500L, importTime);

        assertEquals(2L, statistics.getTotalSpacesWithMapping(), "有映射的空间数量应该正确");
        assertEquals(3L, statistics.getTotalSpacesWithDocuments(), "有文档的空间数量应该正确");
        assertEquals(500L, statistics.getTotalImportedDocuments(), "总文档数量应该正确");
        assertEquals(importTime, statistics.getLastImportTime(), "最后导入时间应该正确");
    }

    @Test
    @DisplayName("测试默认构造函数")
    void testDefaultConstructor() {
        ImportStatistics statistics = new ImportStatistics();

        assertNull(statistics.getTotalSpacesWithMapping(), "默认情况下有映射的空间数量应该为null");
        assertNull(statistics.getTotalSpacesWithDocuments(), "默认情况下有文档的空间数量应该为null");
        assertNull(statistics.getTotalImportedDocuments(), "默认情况下总文档数量应该为null");
        assertNull(statistics.getLastImportTime(), "默认情况下最后导入时间应该为null");
    }

    @Test
    @DisplayName("测试setter方法")
    void testSetters() {
        ImportStatistics statistics = new ImportStatistics();
        LocalDateTime importTime = LocalDateTime.now();

        statistics.setTotalSpacesWithMapping(5L);
        statistics.setTotalSpacesWithDocuments(8L);
        statistics.setTotalImportedDocuments(1000L);
        statistics.setLastImportTime(importTime);

        assertEquals(5L, statistics.getTotalSpacesWithMapping(), "有映射的空间数量应该被正确设置");
        assertEquals(8L, statistics.getTotalSpacesWithDocuments(), "有文档的空间数量应该被正确设置");
        assertEquals(1000L, statistics.getTotalImportedDocuments(), "总文档数量应该被正确设置");
        assertEquals(importTime, statistics.getLastImportTime(), "最后导入时间应该被正确设置");
    }

    @Test
    @DisplayName("测试hasAnyImports方法 - 有导入")
    void testHasAnyImports_WithImports() {
        ImportStatistics statistics = new ImportStatistics();
        statistics.setTotalImportedDocuments(100L);

        assertTrue(statistics.hasAnyImports(), "当有导入文档时应该返回true");
    }

    @Test
    @DisplayName("测试hasAnyImports方法 - 无导入")
    void testHasAnyImports_WithoutImports() {
        ImportStatistics statistics = new ImportStatistics();

        // null文档数量
        statistics.setTotalImportedDocuments(null);
        assertFalse(statistics.hasAnyImports(), "当文档数量为null时应该返回false");

        // 0文档数量
        statistics.setTotalImportedDocuments(0L);
        assertFalse(statistics.hasAnyImports(), "当文档数量为0时应该返回false");
    }

    @Test
    @DisplayName("测试hasAnyMappings方法 - 有映射")
    void testHasAnyMappings_WithMappings() {
        ImportStatistics statistics = new ImportStatistics();
        statistics.setTotalSpacesWithMapping(3L);

        assertTrue(statistics.hasAnyMappings(), "当有映射配置的空间时应该返回true");
    }

    @Test
    @DisplayName("测试hasAnyMappings方法 - 无映射")
    void testHasAnyMappings_WithoutMappings() {
        ImportStatistics statistics = new ImportStatistics();

        // null映射数量
        statistics.setTotalSpacesWithMapping(null);
        assertFalse(statistics.hasAnyMappings(), "当映射数量为null时应该返回false");

        // 0映射数量
        statistics.setTotalSpacesWithMapping(0L);
        assertFalse(statistics.hasAnyMappings(), "当映射数量为0时应该返回false");
    }

    @Test
    @DisplayName("测试toString方法")
    void testToString() {
        LocalDateTime importTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        ImportStatistics statistics = new ImportStatistics(2L, 3L, 500L, importTime);

        String toString = statistics.toString();

        assertTrue(toString.contains("totalSpacesWithMapping=2"), "toString应该包含有映射的空间数量");
        assertTrue(toString.contains("totalSpacesWithDocuments=3"), "toString应该包含有文档的空间数量");
        assertTrue(toString.contains("totalImportedDocuments=500"), "toString应该包含总文档数量");
        assertTrue(toString.contains("lastImportTime=" + importTime), "toString应该包含最后导入时间");
    }

    @Test
    @DisplayName("测试边界情况 - 负数值")
    void testBoundaryValues_NegativeNumbers() {
        ImportStatistics statistics = new ImportStatistics();

        statistics.setTotalSpacesWithMapping(-1L);
        statistics.setTotalSpacesWithDocuments(-1L);
        statistics.setTotalImportedDocuments(-1L);

        // 业务方法应该能处理负数（虽然实际中不应该出现）
        assertFalse(statistics.hasAnyImports(), "负数文档数量应该被认为是无导入");
        assertFalse(statistics.hasAnyMappings(), "负数映射数量应该被认为是无映射");
    }

    @Test
    @DisplayName("测试完整的统计场景")
    void testCompleteStatisticsScenario() {
        LocalDateTime importTime = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
        ImportStatistics statistics = new ImportStatistics();

        // 模拟统计数据
        statistics.setTotalSpacesWithMapping(5L);        // 5个空间有映射配置
        statistics.setTotalSpacesWithDocuments(3L);      // 3个空间有导入文档
        statistics.setTotalImportedDocuments(1500L);     // 总共导入了1500个文档
        statistics.setLastImportTime(importTime);        // 最后导入时间

        // 验证业务逻辑
        assertTrue(statistics.hasAnyMappings(), "应该有映射配置");
        assertTrue(statistics.hasAnyImports(), "应该有导入文档");

        // 验证统计结果是合理的（有映射的空间数可能大于有文档的空间数）
        assertTrue(statistics.getTotalSpacesWithMapping() >= statistics.getTotalSpacesWithDocuments(),
                "有映射配置的空间数应该大于等于有文档的空间数");
        assertTrue(statistics.getTotalImportedDocuments() > 0,
                "总文档数应该大于0");
        assertNotNull(statistics.getLastImportTime(),
                "应该有最后导入时间");
    }
}