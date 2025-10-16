package com.ynet.mgmt.queryunderstanding;

import com.ynet.mgmt.queryunderstanding.context.IntentType;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.pipeline.QueryUnderstandingPipeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 查询理解管道集成测试
 * 测试整个管道的端到端功能
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("查询理解管道集成测试")
class QueryUnderstandingIntegrationTest {

    @Autowired
    private QueryUnderstandingPipeline pipeline;

    @Test
    @DisplayName("测试基础管道处理 - Phase 1功能")
    void testBasicPipelineProcessing() {
        // Given: 一个包含全角字符和多余空白的查询
        String query = "  （生活）　缴费  ";

        // When: 执行管道处理
        QueryContext context = pipeline.execute(query);

        // Then: 验证文本标准化
        assertThat(context.getNormalizedQuery())
                .as("文本标准化应该去除多余空白并转换全角字符")
                .isEqualTo("(生活) 缴费");

        // Then: 验证同义词扩展
        assertThat(context.getSynonyms())
                .as("应该包含同义词")
                .isNotEmpty()
                .contains("生活服务", "日常", "交费", "支付", "付费");

        // Then: 验证当前查询
        String currentQuery = context.getCurrentQuery();
        assertThat(currentQuery)
                .as("当前查询不应为空")
                .isNotEmpty();

        // Then: 验证处理耗时
        assertThat(context.getTotalProcessingTime())
                .as("管道处理应该在合理时间内完成")
                .isLessThan(5000L); // 小于5秒

        // Then: 验证各处理器执行记录
        assertThat(context.getProcessorTimings())
                .as("应该记录各处理器的执行时间")
                .isNotEmpty();

        // 输出调试信息
        System.out.println("\n=== 基础管道测试结果 ===");
        System.out.println("原始查询: " + query);
        System.out.println("标准化后: " + context.getNormalizedQuery());
        System.out.println("同义词: " + context.getSynonyms());
        System.out.println("当前查询: " + currentQuery);
        System.out.println("总耗时: " + context.getTotalProcessingTime() + "ms");
        System.out.println("各处理器耗时: " + context.getProcessorTimings());
    }

    @Test
    @DisplayName("测试简单查询的智能跳过")
    void testSmartSkipForSimpleQuery() {
        // Given: 一个简单查询
        String query = "a";

        // When: 执行管道处理
        QueryContext context = pipeline.execute(query);

        // Then: 验证智能跳过标记
        assertThat(context.isSkipComplexProcessing())
                .as("简单查询应该启用智能跳过")
                .isTrue();

        // Then: 验证处理耗时更短
        assertThat(context.getTotalProcessingTime())
                .as("简单查询处理应该更快")
                .isLessThan(100L); // 小于100ms

        System.out.println("\n=== 简单查询测试结果 ===");
        System.out.println("查询: " + query);
        System.out.println("智能跳过: " + context.isSkipComplexProcessing());
        System.out.println("总耗时: " + context.getTotalProcessingTime() + "ms");
    }

    @Test
    @DisplayName("测试Phase 2功能 - LLM关闭时的降级处理")
    void testPhase2WithLlmDisabled() {
        // Given: LLM功能默认关闭的情况
        String query = "北京天气怎么样";

        // When: 执行管道处理
        QueryContext context = pipeline.execute(query);

        // Then: 验证基础处理仍然工作
        assertThat(context.getNormalizedQuery())
                .as("文本标准化应该正常工作")
                .isEqualTo("北京天气怎么样");

        // Then: 验证没有意图和实体（因为LLM关闭）
        assertThat(context.getIntent())
                .as("LLM关闭时不应该有意图识别结果")
                .isNull();

        assertThat(context.getEntities())
                .as("LLM关闭时不应该有实体")
                .isEmpty();

        // Then: 验证没有查询重写（因为没有意图）
        assertThat(context.getRewrittenQuery())
                .as("LLM关闭时不应该有查询重写")
                .isNull();

        System.out.println("\n=== Phase 2降级测试结果 ===");
        System.out.println("查询: " + query);
        System.out.println("标准化后: " + context.getNormalizedQuery());
        System.out.println("意图: " + context.getIntent());
        System.out.println("实体: " + context.getEntities());
        System.out.println("重写查询: " + context.getRewrittenQuery());
        System.out.println("总耗时: " + context.getTotalProcessingTime() + "ms");
    }

    @Test
    @DisplayName("测试getCurrentQuery优先级")
    void testCurrentQueryPriority() {
        // Given: 一个普通查询
        String query = "生活缴费";

        // When: 执行管道处理
        QueryContext context = pipeline.execute(query);

        // Then: getCurrentQuery应该返回处理链中最高优先级的查询
        String currentQuery = context.getCurrentQuery();

        // 优先级: rewrittenQuery > expandedQuery > correctedQuery > normalizedQuery > originalQuery
        if (context.getRewrittenQuery() != null) {
            assertThat(currentQuery).isEqualTo(context.getRewrittenQuery());
        } else if (context.getExpandedQuery() != null) {
            assertThat(currentQuery).isEqualTo(context.getExpandedQuery());
        } else if (context.getCorrectedQuery() != null) {
            assertThat(currentQuery).isEqualTo(context.getCorrectedQuery());
        } else if (context.getNormalizedQuery() != null) {
            assertThat(currentQuery).isEqualTo(context.getNormalizedQuery());
        } else {
            assertThat(currentQuery).isEqualTo(context.getOriginalQuery());
        }

        System.out.println("\n=== getCurrentQuery优先级测试 ===");
        System.out.println("原始查询: " + context.getOriginalQuery());
        System.out.println("标准化查询: " + context.getNormalizedQuery());
        System.out.println("纠错查询: " + context.getCorrectedQuery());
        System.out.println("扩展查询: " + context.getExpandedQuery());
        System.out.println("重写查询: " + context.getRewrittenQuery());
        System.out.println("当前查询: " + currentQuery);
    }

    @Test
    @DisplayName("测试管道性能 - 批量查询")
    void testPipelinePerformance() {
        // Given: 一组典型查询
        String[] queries = {
                "生活缴费",
                "北京天气",
                "如何充值",
                "产品价格",
                "在线客服"
        };

        // When & Then: 执行批量处理并统计性能
        long totalTime = 0;
        int count = 0;

        System.out.println("\n=== 批量查询性能测试 ===");
        for (String query : queries) {
            QueryContext context = pipeline.execute(query);
            long time = context.getTotalProcessingTime();
            totalTime += time;
            count++;

            System.out.printf("查询: %-15s | 耗时: %4dms | 同义词数: %d%n",
                    query, time, context.getSynonyms().size());

            // 每个查询应该在合理时间内完成
            assertThat(time)
                    .as("查询 '" + query + "' 处理时间应该小于5秒")
                    .isLessThan(5000L);
        }

        // 计算平均耗时
        long avgTime = totalTime / count;
        System.out.println("平均耗时: " + avgTime + "ms");

        // 平均耗时应该在合理范围内
        assertThat(avgTime)
                .as("平均处理时间应该小于3秒")
                .isLessThan(3000L);
    }

    @Test
    @DisplayName("测试空查询和异常情况")
    void testEdgeCases() {
        // Test 1: 空字符串
        QueryContext context1 = pipeline.execute("");
        assertThat(context1.getOriginalQuery()).isEmpty();
        System.out.println("\n空字符串测试通过");

        // Test 2: 只有空白字符
        QueryContext context2 = pipeline.execute("   ");
        assertThat(context2.getNormalizedQuery())
                .as("只有空白字符应该被标准化为空")
                .isEmpty();
        System.out.println("纯空白字符测试通过");

        // Test 3: 特殊字符
        QueryContext context3 = pipeline.execute("@#$%^&*()");
        assertThat(context3.getNormalizedQuery()).isNotNull();
        System.out.println("特殊字符测试通过");

        // Test 4: 超长查询
        String longQuery = "这是一个超长的查询".repeat(50);
        QueryContext context4 = pipeline.execute(longQuery);
        assertThat(context4.getNormalizedQuery()).isNotNull();
        assertThat(context4.getTotalProcessingTime())
                .as("超长查询也应该在合理时间内处理完成")
                .isLessThan(10000L);
        System.out.println("超长查询测试通过，耗时: " + context4.getTotalProcessingTime() + "ms");
    }

    @Test
    @DisplayName("测试管道可观测性")
    void testPipelineObservability() {
        // Given: 一个查询
        String query = "生活缴费";

        // When: 执行管道处理
        QueryContext context = pipeline.execute(query);

        // Then: 验证所有可观测性指标
        assertThat(context.getProcessorTimings())
                .as("应该记录所有处理器的执行时间")
                .isNotEmpty();

        assertThat(context.getStartTime())
                .as("应该记录开始时间")
                .isGreaterThan(0);

        assertThat(context.getTotalProcessingTime())
                .as("总处理时间应该大于0")
                .isGreaterThan(0);

        // 输出所有处理器的执行时间
        System.out.println("\n=== 管道可观测性测试 ===");
        System.out.println("查询: " + query);
        System.out.println("开始时间: " + context.getStartTime());
        System.out.println("总耗时: " + context.getTotalProcessingTime() + "ms");
        System.out.println("\n各处理器耗时:");
        context.getProcessorTimings().forEach((processor, time) ->
                System.out.printf("  %-30s: %4d ms%n", processor, time)
        );
    }
}
