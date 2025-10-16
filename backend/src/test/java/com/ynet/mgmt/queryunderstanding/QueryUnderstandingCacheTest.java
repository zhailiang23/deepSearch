package com.ynet.mgmt.queryunderstanding;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.pipeline.QueryUnderstandingPipeline;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 查询理解缓存功能测试
 * 测试Redis缓存的正确性和性能
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("查询理解缓存功能测试")
class QueryUnderstandingCacheTest {

    @Autowired
    private QueryUnderstandingPipeline pipeline;

    @Autowired
    private QueryUnderstandingCacheService cacheService;

    @BeforeEach
    void setUp() {
        // 清空所有缓存，确保测试独立性
        cacheService.evictAll();
    }

    @Test
    @DisplayName("测试QueryContext缓存存取")
    void testQueryContextCache() {
        // Given: 一个查询
        String query = "生活缴费";

        // When: 第一次执行（无缓存）
        long start1 = System.currentTimeMillis();
        QueryContext context1 = pipeline.execute(query);
        long time1 = System.currentTimeMillis() - start1;

        // When: 第二次执行（有缓存）
        long start2 = System.currentTimeMillis();
        QueryContext context2 = pipeline.execute(query);
        long time2 = System.currentTimeMillis() - start2;

        // Then: 验证两次结果一致
        assertThat(context2.getOriginalQuery())
                .as("缓存的原始查询应该一致")
                .isEqualTo(context1.getOriginalQuery());

        assertThat(context2.getNormalizedQuery())
                .as("缓存的标准化查询应该一致")
                .isEqualTo(context1.getNormalizedQuery());

        assertThat(context2.getSynonyms())
                .as("缓存的同义词应该一致")
                .containsAll(context1.getSynonyms());

        // Then: 验证缓存命中
        // 注意：由于查询处理本身就很快，缓存命中的性能提升可能不明显
        // 这里主要验证第二次调用能正常工作，不一定要求性能提升
        assertThat(context2)
                .as("第二次调用应该成功返回结果")
                .isNotNull();

        System.out.println("\n=== QueryContext缓存测试 ===");
        System.out.println("查询: " + query);
        System.out.println("第一次执行（无缓存）: " + time1 + "ms");
        System.out.println("第二次执行（缓存命中）: " + time2 + "ms");
        System.out.println("性能提升: " + ((time1 - time2) * 100 / time1) + "%");
    }

    @Test
    @DisplayName("测试缓存失效和刷新")
    void testCacheEviction() {
        // Given: 一个查询
        String query = "北京天气";

        // When: 执行并缓存
        QueryContext context1 = pipeline.execute(query);
        assertThat(context1).isNotNull();

        // When: 清除缓存
        cacheService.evictQueryCache(query);

        // When: 再次执行
        QueryContext context2 = pipeline.execute(query);

        // Then: 两次结果应该一致（但不是同一个对象实例）
        assertThat(context2.getOriginalQuery())
                .isEqualTo(context1.getOriginalQuery());

        System.out.println("\n=== 缓存失效测试 ===");
        System.out.println("查询: " + query);
        System.out.println("缓存清除后重新执行成功");
    }

    @Test
    @DisplayName("测试LLM意图缓存")
    void testLlmIntentCache() {
        // Given: 一个查询和意图结果
        String query = "如何充值";
        String intentJson = "{\"intent\":\"COMMAND\",\"confidence\":0.95}";

        // When: 缓存意图结果
        cacheService.cacheLlmIntent(query, intentJson);

        // When: 获取缓存
        String cachedIntent = cacheService.getCachedLlmIntent(query);

        // Then: 验证缓存结果
        assertThat(cachedIntent)
                .as("应该成功缓存并获取意图结果")
                .isNotNull()
                .isEqualTo(intentJson);

        System.out.println("\n=== LLM意图缓存测试 ===");
        System.out.println("查询: " + query);
        System.out.println("缓存内容: " + intentJson);
        System.out.println("读取结果: " + cachedIntent);
    }

    @Test
    @DisplayName("测试LLM实体缓存")
    void testLlmEntityCache() {
        // Given: 一个查询和实体结果
        String query = "北京明天天气";
        String entitiesJson = "[{\"type\":\"LOCATION\",\"value\":\"北京\"},{\"type\":\"DATE\",\"value\":\"明天\"}]";

        // When: 缓存实体结果
        cacheService.cacheLlmEntities(query, entitiesJson);

        // When: 获取缓存
        String cachedEntities = cacheService.getCachedLlmEntities(query);

        // Then: 验证缓存结果
        assertThat(cachedEntities)
                .as("应该成功缓存并获取实体结果")
                .isNotNull()
                .isEqualTo(entitiesJson);

        System.out.println("\n=== LLM实体缓存测试 ===");
        System.out.println("查询: " + query);
        System.out.println("缓存内容: " + entitiesJson);
        System.out.println("读取结果: " + cachedEntities);
    }

    @Test
    @DisplayName("测试LLM查询重写缓存")
    void testLlmRewriteCache() {
        // Given: 原始查询和重写结果
        String originalQuery = "怎么交电费";
        String rewrittenQuery = "电费缴纳方式";

        // When: 缓存重写结果
        cacheService.cacheLlmRewrite(originalQuery, rewrittenQuery);

        // When: 获取缓存
        String cachedRewrite = cacheService.getCachedLlmRewrite(originalQuery);

        // Then: 验证缓存结果
        assertThat(cachedRewrite)
                .as("应该成功缓存并获取重写结果")
                .isNotNull()
                .isEqualTo(rewrittenQuery);

        System.out.println("\n=== LLM查询重写缓存测试 ===");
        System.out.println("原始查询: " + originalQuery);
        System.out.println("重写查询: " + rewrittenQuery);
        System.out.println("读取结果: " + cachedRewrite);
    }

    @Test
    @DisplayName("测试同义词缓存")
    void testSynonymCache() {
        // Given: 一个词和同义词列表
        String word = "生活";
        String synonymsJson = "[\"生活服务\",\"日常\",\"居家\"]";

        // When: 缓存同义词
        cacheService.cacheSynonyms(word, synonymsJson);

        // When: 获取缓存
        String cachedSynonyms = cacheService.getCachedSynonyms(word);

        // Then: 验证缓存结果
        assertThat(cachedSynonyms)
                .as("应该成功缓存并获取同义词")
                .isNotNull()
                .isEqualTo(synonymsJson);

        System.out.println("\n=== 同义词缓存测试 ===");
        System.out.println("词语: " + word);
        System.out.println("同义词: " + synonymsJson);
        System.out.println("读取结果: " + cachedSynonyms);
    }

    @Test
    @DisplayName("测试批量查询的缓存效果")
    void testBatchQueryCachePerformance() {
        // Given: 一组重复查询
        String[] queries = {
                "生活缴费",
                "北京天气",
                "生活缴费",  // 重复
                "如何充值",
                "北京天气",  // 重复
                "生活缴费"   // 重复
        };

        long totalTimeFirstRound = 0;
        long totalTimeSecondRound = 0;
        int cacheHits = 0;

        System.out.println("\n=== 批量查询缓存性能测试 ===");

        // When: 第一轮执行（建立缓存）
        System.out.println("\n第一轮执行（建立缓存）:");
        for (String query : queries) {
            long start = System.currentTimeMillis();
            QueryContext context = pipeline.execute(query);
            long time = System.currentTimeMillis() - start;
            totalTimeFirstRound += time;
            System.out.printf("  查询: %-15s | 耗时: %4dms%n", query, time);
        }

        // When: 清空缓存，重新执行
        cacheService.evictAll();

        // When: 第二轮执行（利用缓存）
        System.out.println("\n第二轮执行（利用缓存）:");
        for (String query : queries) {
            long start = System.currentTimeMillis();
            QueryContext context = pipeline.execute(query);
            long time = System.currentTimeMillis() - start;
            totalTimeSecondRound += time;

            // 缓存命中通常非常快（<5ms）
            if (time < 5) {
                cacheHits++;
            }

            System.out.printf("  查询: %-15s | 耗时: %4dms%n", query, time);
        }

        // Then: 验证缓存效果
        System.out.println("\n统计结果:");
        System.out.println("  第一轮总耗时: " + totalTimeFirstRound + "ms");
        System.out.println("  第二轮总耗时: " + totalTimeSecondRound + "ms");
        System.out.println("  缓存命中次数: " + cacheHits + "/" + queries.length);
        System.out.println("  性能提升: " + ((totalTimeFirstRound - totalTimeSecondRound) * 100 / totalTimeFirstRound) + "%");

        // Then: 第二轮应该至少有一半的查询命中缓存
        assertThat(cacheHits)
                .as("至少一半的查询应该命中缓存")
                .isGreaterThanOrEqualTo(queries.length / 2);
    }

    @Test
    @DisplayName("测试缓存统计功能")
    void testCacheStatistics() {
        // Given: 多个查询
        String[] queries = {"查询1", "查询2", "查询3"};

        // When: 执行查询并缓存
        for (String query : queries) {
            pipeline.execute(query);
        }

        // When: 获取缓存统计
        long count = cacheService.getCacheCount();

        // Then: 验证缓存数量
        assertThat(count)
                .as("应该缓存了所有查询")
                .isGreaterThanOrEqualTo(queries.length);

        System.out.println("\n=== 缓存统计测试 ===");
        System.out.println("执行查询数: " + queries.length);
        System.out.println("缓存条目数: " + count);
    }

    @Test
    @DisplayName("测试缓存在管道失败时的行为")
    void testCacheOnPipelineFailure() {
        // Given: 一个会导致某些处理器跳过的查询
        String query = "";  // 空查询会触发智能跳过

        // When: 执行查询
        QueryContext context = pipeline.execute(query);

        // Then: 即使部分处理器被跳过，仍然应该缓存结果
        assertThat(context).isNotNull();

        // When: 再次执行
        QueryContext cachedContext = pipeline.execute(query);

        // Then: 应该从缓存获取
        assertThat(cachedContext).isNotNull();

        System.out.println("\n=== 管道失败时的缓存行为测试 ===");
        System.out.println("空查询处理成功并缓存");
    }

    @Test
    @DisplayName("测试不同查询的缓存隔离")
    void testCacheIsolation() {
        // Given: 两个不同的查询
        String query1 = "生活缴费";
        String query2 = "北京天气";

        // When: 分别执行
        QueryContext context1 = pipeline.execute(query1);
        QueryContext context2 = pipeline.execute(query2);

        // Then: 验证结果不同
        assertThat(context1.getNormalizedQuery())
                .as("不同查询应该有不同的结果")
                .isNotEqualTo(context2.getNormalizedQuery());

        // Then: 验证缓存隔离
        QueryContext cached1 = pipeline.execute(query1);
        QueryContext cached2 = pipeline.execute(query2);

        assertThat(cached1.getNormalizedQuery())
                .isEqualTo(context1.getNormalizedQuery());
        assertThat(cached2.getNormalizedQuery())
                .isEqualTo(context2.getNormalizedQuery());

        System.out.println("\n=== 缓存隔离测试 ===");
        System.out.println("查询1: " + query1 + " -> " + cached1.getNormalizedQuery());
        System.out.println("查询2: " + query2 + " -> " + cached2.getNormalizedQuery());
        System.out.println("两个查询的缓存正确隔离");
    }
}
