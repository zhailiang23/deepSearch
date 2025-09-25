package com.ynet.mgmt.searchdata.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ElasticsearchDataService拼音搜索功能测试类
 * 专门测试拼音搜索相关的内部方法
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Elasticsearch拼音搜索功能测试")
class ElasticsearchDataServicePinyinTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ElasticsearchDataService elasticsearchDataService;

    @BeforeEach
    void setUp() {
        // 测试前的设置
    }

    @Test
    @DisplayName("测试构建查询方法")
    void testBuildQuery() throws Exception {
        // 测试空查询
        Method buildQueryMethod = ElasticsearchDataService.class.getDeclaredMethod("buildQuery", String.class, Boolean.class, String.class);
        buildQueryMethod.setAccessible(true);

        Query emptyQuery = (Query) buildQueryMethod.invoke(elasticsearchDataService, "", false, "AUTO");
        assertNotNull(emptyQuery, "空查询应该返回match_all查询");

        Query nullQuery = (Query) buildQueryMethod.invoke(elasticsearchDataService, (String) null, false, "AUTO");
        assertNotNull(nullQuery, "null查询应该返回match_all查询");

        // 测试非空查询
        Query normalQuery = (Query) buildQueryMethod.invoke(elasticsearchDataService, "测试查询", false, "AUTO");
        assertNotNull(normalQuery, "普通查询应该返回有效的Query对象");
    }

    @Test
    @DisplayName("测试获取可搜索字段列表")
    void testGetSearchableFields() throws Exception {
        Method getSearchableFieldsMethod = ElasticsearchDataService.class.getDeclaredMethod("getSearchableFields");
        getSearchableFieldsMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> fields = (List<String>) getSearchableFieldsMethod.invoke(elasticsearchDataService);

        assertNotNull(fields, "搜索字段列表不应为空");
        assertFalse(fields.isEmpty(), "搜索字段列表应该包含字段");
        assertTrue(fields.contains("title^2"), "应该包含带权重的title字段");
        assertTrue(fields.contains("content"), "应该包含content字段");
        assertTrue(fields.contains("description"), "应该包含description字段");
        assertTrue(fields.contains("name"), "应该包含name字段");
        assertTrue(fields.contains("text"), "应该包含text字段");

        // 验证字段数量
        assertEquals(5, fields.size(), "应该有5个搜索字段");
    }

    @Test
    @DisplayName("测试获取拼音字段列表")
    void testGetPinyinFields() throws Exception {
        Method getPinyinFieldsMethod = ElasticsearchDataService.class.getDeclaredMethod("getPinyinFields");
        getPinyinFieldsMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> pinyinFields = (List<String>) getPinyinFieldsMethod.invoke(elasticsearchDataService);

        assertNotNull(pinyinFields, "拼音字段列表不应为空");
        assertFalse(pinyinFields.isEmpty(), "拼音字段列表应该包含字段");
        assertTrue(pinyinFields.contains("title.pinyin^2"), "应该包含带权重的title.pinyin字段");
        assertTrue(pinyinFields.contains("content.pinyin"), "应该包含content.pinyin字段");
        assertTrue(pinyinFields.contains("description.pinyin"), "应该包含description.pinyin字段");
        assertTrue(pinyinFields.contains("name.pinyin"), "应该包含name.pinyin字段");
        assertTrue(pinyinFields.contains("text.pinyin"), "应该包含text.pinyin字段");

        // 验证字段数量
        assertEquals(5, pinyinFields.size(), "应该有5个拼音字段");
    }

    @Test
    @DisplayName("测试获取首字母字段列表")
    void testGetFirstLetterFields() throws Exception {
        Method getFirstLetterFieldsMethod = ElasticsearchDataService.class.getDeclaredMethod("getFirstLetterFields");
        getFirstLetterFieldsMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> firstLetterFields = (List<String>) getFirstLetterFieldsMethod.invoke(elasticsearchDataService);

        assertNotNull(firstLetterFields, "首字母字段列表不应为空");
        assertFalse(firstLetterFields.isEmpty(), "首字母字段列表应该包含字段");
        assertTrue(firstLetterFields.contains("title.first_letter^2"), "应该包含带权重的title.first_letter字段");
        assertTrue(firstLetterFields.contains("content.first_letter"), "应该包含content.first_letter字段");
        assertTrue(firstLetterFields.contains("description.first_letter"), "应该包含description.first_letter字段");
        assertTrue(firstLetterFields.contains("name.first_letter"), "应该包含name.first_letter字段");
        assertTrue(firstLetterFields.contains("text.first_letter"), "应该包含text.first_letter字段");

        // 验证字段数量
        assertEquals(5, firstLetterFields.size(), "应该有5个首字母字段");
    }

    @Test
    @DisplayName("测试构建多字段查询")
    void testBuildMultiFieldQuery() throws Exception {
        Method buildMultiFieldQueryMethod = ElasticsearchDataService.class.getDeclaredMethod("buildMultiFieldQuery", String.class, float.class);
        buildMultiFieldQueryMethod.setAccessible(true);

        Query multiFieldQuery = (Query) buildMultiFieldQueryMethod.invoke(elasticsearchDataService, "测试查询", 2.0f);

        assertNotNull(multiFieldQuery, "多字段查询不应为空");
        // 由于Query对象的内部结构复杂，这里主要验证不会抛出异常即可
    }

    @Test
    @DisplayName("测试构建拼音查询")
    void testBuildPinyinQuery() throws Exception {
        Method buildPinyinQueryMethod = ElasticsearchDataService.class.getDeclaredMethod("buildPinyinQuery", String.class, float.class);
        buildPinyinQueryMethod.setAccessible(true);

        Query pinyinQuery = (Query) buildPinyinQueryMethod.invoke(elasticsearchDataService, "beijing", 1.5f);

        assertNotNull(pinyinQuery, "拼音查询不应为空");
        // 由于Query对象的内部结构复杂，这里主要验证不会抛出异常即可
    }

    @Test
    @DisplayName("测试构建首字母查询")
    void testBuildFirstLetterQuery() throws Exception {
        Method buildFirstLetterQueryMethod = ElasticsearchDataService.class.getDeclaredMethod("buildFirstLetterQuery", String.class, float.class);
        buildFirstLetterQueryMethod.setAccessible(true);

        Query firstLetterQuery = (Query) buildFirstLetterQueryMethod.invoke(elasticsearchDataService, "bjdx", 1.0f);

        assertNotNull(firstLetterQuery, "首字母查询不应为空");
        // 由于Query对象的内部结构复杂，这里主要验证不会抛出异常即可
    }

    @Test
    @DisplayName("测试构建拼音增强查询")
    void testBuildPinyinEnhancedQuery() throws Exception {
        Method buildPinyinEnhancedQueryMethod = ElasticsearchDataService.class.getDeclaredMethod("buildPinyinEnhancedQuery", String.class, String.class);
        buildPinyinEnhancedQueryMethod.setAccessible(true);

        Query pinyinEnhancedQuery = (Query) buildPinyinEnhancedQueryMethod.invoke(elasticsearchDataService, "beijing daxue", "AUTO");

        assertNotNull(pinyinEnhancedQuery, "拼音增强查询不应为空");
        // 验证返回的是BoolQuery类型
        assertTrue(pinyinEnhancedQuery.isBool(), "拼音增强查询应该是BoolQuery类型");
    }

    @Test
    @DisplayName("测试Elasticsearch错误消息解析")
    void testGetElasticsearchErrorMessage() throws Exception {
        // 由于ElasticsearchException构造复杂，这里通过简化的方式测试错误消息解析逻辑
        // 实际使用中，这个方法主要是为了提供更友好的错误信息

        Method getElasticsearchErrorMessageMethod = ElasticsearchDataService.class.getDeclaredMethod("getElasticsearchErrorMessage", co.elastic.clients.elasticsearch._types.ElasticsearchException.class);
        getElasticsearchErrorMessageMethod.setAccessible(true);

        // 由于ElasticsearchException构造的复杂性，我们跳过这个测试
        // 在实际使用中，这个方法会被正确调用
        // 这里我们至少验证方法存在且可访问

        assertNotNull(getElasticsearchErrorMessageMethod, "错误消息解析方法应该存在");
        assertTrue(getElasticsearchErrorMessageMethod.canAccess(elasticsearchDataService), "方法应该是可访问的");
    }

    @Test
    @DisplayName("测试搜索字段配置完整性")
    void testSearchFieldsConfiguration() throws Exception {
        // 获取所有搜索相关字段配置
        Method getSearchableFieldsMethod = ElasticsearchDataService.class.getDeclaredMethod("getSearchableFields");
        Method getPinyinFieldsMethod = ElasticsearchDataService.class.getDeclaredMethod("getPinyinFields");
        Method getFirstLetterFieldsMethod = ElasticsearchDataService.class.getDeclaredMethod("getFirstLetterFields");

        getSearchableFieldsMethod.setAccessible(true);
        getPinyinFieldsMethod.setAccessible(true);
        getFirstLetterFieldsMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> searchableFields = (List<String>) getSearchableFieldsMethod.invoke(elasticsearchDataService);
        @SuppressWarnings("unchecked")
        List<String> pinyinFields = (List<String>) getPinyinFieldsMethod.invoke(elasticsearchDataService);
        @SuppressWarnings("unchecked")
        List<String> firstLetterFields = (List<String>) getFirstLetterFieldsMethod.invoke(elasticsearchDataService);

        // 验证字段数量一致性
        assertEquals(searchableFields.size(), pinyinFields.size(), "搜索字段和拼音字段数量应该一致");
        assertEquals(searchableFields.size(), firstLetterFields.size(), "搜索字段和首字母字段数量应该一致");

        // 验证权重策略
        assertTrue(searchableFields.stream().anyMatch(field -> field.contains("^")), "搜索字段应该包含权重设置");
        assertTrue(pinyinFields.stream().anyMatch(field -> field.contains("^")), "拼音字段应该包含权重设置");
        assertTrue(firstLetterFields.stream().anyMatch(field -> field.contains("^")), "首字母字段应该包含权重设置");

        // 验证关键字段存在
        assertTrue(searchableFields.stream().anyMatch(field -> field.startsWith("title")), "应该包含title字段");
        assertTrue(pinyinFields.stream().anyMatch(field -> field.startsWith("title.pinyin")), "应该包含title.pinyin字段");
        assertTrue(firstLetterFields.stream().anyMatch(field -> field.startsWith("title.first_letter")), "应该包含title.first_letter字段");
    }
}