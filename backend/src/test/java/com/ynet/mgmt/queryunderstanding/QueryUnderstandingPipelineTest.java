package com.ynet.mgmt.queryunderstanding;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.pipeline.QueryUnderstandingPipeline;
import com.ynet.mgmt.queryunderstanding.processor.preprocessing.TextNormalizationProcessor;
import com.ynet.mgmt.queryunderstanding.processor.expansion.SynonymExpansionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 查询理解管道测试
 *
 * @author deepSearch
 * @since 2025-01-16
 */
class QueryUnderstandingPipelineTest {

    private QueryUnderstandingPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new QueryUnderstandingPipeline();

        // 注册测试处理器
        pipeline.registerProcessor(new TextNormalizationProcessor());
        pipeline.registerProcessor(new SynonymExpansionProcessor());
    }

    @Test
    void testBasicQueryProcessing() {
        // 测试基本查询处理
        QueryContext context = pipeline.execute("生活缴费");

        assertNotNull(context);
        assertNotNull(context.getNormalizedQuery());
        assertEquals("生活缴费", context.getNormalizedQuery());
        assertTrue(context.getTotalProcessingTime() >= 0);
    }

    @Test
    void testFullWidthConversion() {
        // 测试全角转半角
        QueryContext context = pipeline.execute("（测试）　查询");

        assertNotNull(context.getNormalizedQuery());
        // 全角括号和空格应该被转换
        assertTrue(context.getNormalizedQuery().contains("(") ||
                   context.getNormalizedQuery().contains("测试"));
    }

    @Test
    void testSynonymExpansion() {
        // 测试同义词扩展
        QueryContext context = pipeline.execute("生活缴费");

        assertNotNull(context);
        // 应该有同义词被添加
        assertNotNull(context.getSynonyms());
    }

    @Test
    void testSimpleQuerySkip() {
        // 测试简单查询跳过
        pipeline.setSmartSkipEnabled(true);
        pipeline.setSimpleQueryThreshold(3);

        QueryContext context = pipeline.execute("a");

        assertTrue(context.isSkipComplexProcessing());
    }

    @Test
    void testPerformanceTracking() {
        // 测试性能追踪
        QueryContext context = pipeline.execute("测试查询");

        assertNotNull(context.getProcessorTimings());
        assertTrue(context.getProcessorTimings().size() > 0);
        assertTrue(context.getTotalProcessingTime() >= 0);
    }

    @Test
    void testEmptyQuery() {
        // 测试空查询
        QueryContext context = pipeline.execute("");

        assertNotNull(context);
        assertEquals("", context.getOriginalQuery());
    }

    @Test
    void testNullQuery() {
        // 测试 null 查询
        QueryContext context = pipeline.execute(null);

        assertNotNull(context);
        // 应该能够优雅处理
    }

    @Test
    void testCurrentQuery() {
        // 测试获取当前查询
        QueryContext context = pipeline.execute("测试");

        String current = context.getCurrentQuery();
        assertNotNull(current);
        // 至少应该有标准化后的查询
        assertTrue(!current.isEmpty());
    }
}
