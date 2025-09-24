package com.ynet.mgmt.searchspace.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchSpace实体类测试
 * 测试新增的业务方法和字段功能
 *
 * @author system
 * @since 1.0.0
 */
@DisplayName("SearchSpace实体测试")
class SearchSpaceTest {

    private SearchSpace searchSpace;

    @BeforeEach
    void setUp() {
        searchSpace = new SearchSpace("测试搜索空间", "test_space");
    }

    @Test
    @DisplayName("测试hasIndexMapping方法 - 有映射配置")
    void testHasIndexMapping_WithMapping() {
        // 设置映射配置
        searchSpace.setIndexMapping("{\"properties\":{\"title\":{\"type\":\"text\"}}}");

        assertTrue(searchSpace.hasIndexMapping(), "应该返回true当有映射配置时");
    }

    @Test
    @DisplayName("测试hasIndexMapping方法 - 无映射配置")
    void testHasIndexMapping_WithoutMapping() {
        // null映射
        searchSpace.setIndexMapping(null);
        assertFalse(searchSpace.hasIndexMapping(), "应该返回false当映射为null时");

        // 空字符串映射
        searchSpace.setIndexMapping("");
        assertFalse(searchSpace.hasIndexMapping(), "应该返回false当映射为空字符串时");

        // 空白字符串映射
        searchSpace.setIndexMapping("   ");
        assertFalse(searchSpace.hasIndexMapping(), "应该返回false当映射为空白字符串时");
    }

    @Test
    @DisplayName("测试hasImportedDocuments方法 - 有导入文档")
    void testHasImportedDocuments_WithDocuments() {
        // 设置文档数量
        searchSpace.setDocumentCount(100L);

        assertTrue(searchSpace.hasImportedDocuments(), "应该返回true当有导入文档时");
    }

    @Test
    @DisplayName("测试hasImportedDocuments方法 - 无导入文档")
    void testHasImportedDocuments_WithoutDocuments() {
        // null文档数量
        searchSpace.setDocumentCount(null);
        assertFalse(searchSpace.hasImportedDocuments(), "应该返回false当文档数量为null时");

        // 0文档数量
        searchSpace.setDocumentCount(0L);
        assertFalse(searchSpace.hasImportedDocuments(), "应该返回false当文档数量为0时");

        // 负数文档数量（理论上不应该出现，但测试边界情况）
        searchSpace.setDocumentCount(-1L);
        assertFalse(searchSpace.hasImportedDocuments(), "应该返回false当文档数量为负数时");
    }

    @Test
    @DisplayName("测试updateImportStats方法 - 正常更新")
    void testUpdateImportStats_Normal() {
        LocalDateTime beforeUpdate = LocalDateTime.now();

        // 初始状态 - documentCount在实体中有默认值0L
        assertEquals(0L, searchSpace.getDocumentCount(), "默认文档数量应该为0");
        assertNull(searchSpace.getLastImportTime(), "默认导入时间应该为null");

        // 第一次导入
        searchSpace.updateImportStats(50);

        assertEquals(50L, searchSpace.getDocumentCount(), "第一次导入后文档数量应该为50");
        assertNotNull(searchSpace.getLastImportTime(), "导入时间应该被设置");
        assertTrue(searchSpace.getLastImportTime().isAfter(beforeUpdate), "导入时间应该在更新操作之后");

        // 第二次导入
        LocalDateTime firstImportTime = searchSpace.getLastImportTime();
        try {
            Thread.sleep(10); // 确保时间差
        } catch (InterruptedException e) {
            // ignore
        }
        searchSpace.updateImportStats(30);

        assertEquals(80L, searchSpace.getDocumentCount(), "第二次导入后文档数量应该为80");
        assertTrue(searchSpace.getLastImportTime().isAfter(firstImportTime), "导入时间应该被更新");
    }

    @Test
    @DisplayName("测试updateImportStats方法 - 从已有数量更新")
    void testUpdateImportStats_FromExistingCount() {
        // 设置初始数量
        searchSpace.setDocumentCount(100L);

        // 更新导入统计
        searchSpace.updateImportStats(25);

        assertEquals(125L, searchSpace.getDocumentCount(), "文档数量应该累加");
        assertNotNull(searchSpace.getLastImportTime(), "导入时间应该被设置");
    }

    @Test
    @DisplayName("测试setIndexMapping方法 - 处理空白字符")
    void testSetIndexMapping_TrimWhitespace() {
        String mappingWithSpaces = "  {\"properties\":{\"title\":{\"type\":\"text\"}}}  ";
        String expectedMapping = "{\"properties\":{\"title\":{\"type\":\"text\"}}}";

        searchSpace.setIndexMapping(mappingWithSpaces);

        assertEquals(expectedMapping, searchSpace.getIndexMapping(), "映射配置应该去除前后空白");
    }

    @Test
    @DisplayName("测试setIndexMapping方法 - 处理空值")
    void testSetIndexMapping_HandleNullAndEmpty() {
        // null值
        searchSpace.setIndexMapping(null);
        assertNull(searchSpace.getIndexMapping(), "null映射应该保持null");

        // 空字符串
        searchSpace.setIndexMapping("");
        assertNull(searchSpace.getIndexMapping(), "空字符串映射应该被设置为null");

        // 空白字符串
        searchSpace.setIndexMapping("   ");
        assertNull(searchSpace.getIndexMapping(), "空白字符串映射应该被设置为null");
    }

    @Test
    @DisplayName("测试默认值设置")
    void testDefaultValues() {
        SearchSpace newSpace = new SearchSpace();

        assertEquals(0L, newSpace.getDocumentCount(), "文档数量默认值应该为0");
        assertEquals(SearchSpaceStatus.ACTIVE, newSpace.getStatus(), "状态默认值应该为ACTIVE");
        assertNull(newSpace.getIndexMapping(), "索引映射默认值应该为null");
        assertNull(newSpace.getLastImportTime(), "最后导入时间默认值应该为null");
    }

    @Test
    @DisplayName("测试toString方法包含新字段")
    void testToString_IncludesNewFields() {
        searchSpace.setId(1L);
        searchSpace.setDocumentCount(100L);

        String toStringResult = searchSpace.toString();

        assertTrue(toStringResult.contains("documentCount=100"), "toString应该包含documentCount字段");
        assertTrue(toStringResult.contains("id=1"), "toString应该包含id字段");
        assertTrue(toStringResult.contains("test_space"), "toString应该包含code字段");
    }

    @Test
    @DisplayName("测试实体的完整业务流程")
    void testCompleteBusinessFlow() {
        // 1. 创建搜索空间
        assertEquals("test_space", searchSpace.getIndexName(), "索引名称应该基于code生成");

        // 2. 设置索引映射
        String mapping = "{\"mappings\":{\"properties\":{\"content\":{\"type\":\"text\"}}}}";
        searchSpace.setIndexMapping(mapping);
        assertTrue(searchSpace.hasIndexMapping(), "应该有索引映射");

        // 3. 导入文档
        assertFalse(searchSpace.hasImportedDocuments(), "初始时应该没有导入文档");
        searchSpace.updateImportStats(1000);
        assertTrue(searchSpace.hasImportedDocuments(), "导入后应该有导入文档");

        // 4. 继续导入
        searchSpace.updateImportStats(500);
        assertEquals(1500L, searchSpace.getDocumentCount(), "文档数量应该正确累加");

        // 5. 验证状态
        assertTrue(searchSpace.isActive(), "搜索空间应该是活跃的");
        assertTrue(searchSpace.isSearchable(), "搜索空间应该是可搜索的");
    }
}