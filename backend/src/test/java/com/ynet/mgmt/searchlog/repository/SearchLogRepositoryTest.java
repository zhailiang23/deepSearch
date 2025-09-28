package com.ynet.mgmt.searchlog.repository;

import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchLogRepository测试
 * 测试热词统计相关的查询方法
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-28
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SearchLogRepository热词统计功能测试")
class SearchLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SearchLogRepository repository;

    private SearchLog successLog1, successLog2, successLog3, errorLog, emptyQueryLog;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now();

        // 创建成功搜索日志
        successLog1 = createSearchLog("Java编程", SearchLogStatus.SUCCESS, baseTime.minusHours(1));
        successLog2 = createSearchLog("Spring Boot", SearchLogStatus.SUCCESS, baseTime.minusHours(2));
        successLog3 = createSearchLog("Java编程", SearchLogStatus.SUCCESS, baseTime.minusHours(3)); // 重复查询

        // 创建错误状态的搜索日志（不应该被热词统计包含）
        errorLog = createSearchLog("错误查询", SearchLogStatus.ERROR, baseTime.minusHours(4));

        // 创建包含空白但有内容的查询词（测试TRIM功能）
        emptyQueryLog = createSearchLog(" 测试空白 ", SearchLogStatus.SUCCESS, baseTime.minusHours(5));

        // 持久化所有测试数据
        successLog1 = entityManager.persistAndFlush(successLog1);
        successLog2 = entityManager.persistAndFlush(successLog2);
        successLog3 = entityManager.persistAndFlush(successLog3);
        errorLog = entityManager.persistAndFlush(errorLog);
        emptyQueryLog = entityManager.persistAndFlush(emptyQueryLog);

        entityManager.clear();
    }

    private SearchLog createSearchLog(String query, SearchLogStatus status, LocalDateTime createdAt) {
        SearchLog log = new SearchLog();
        log.setSearchQuery(query);
        log.setStatus(status);
        log.setUserId(1L);
        log.setSessionId("session-" + System.currentTimeMillis());
        log.setTraceId("trace-" + System.currentTimeMillis());
        log.setSearchSpaceId(1L);
        log.setTotalResults(10L);
        log.setReturnedResults(10);
        log.setTotalTimeMs(100L);

        // 设置审计字段
        log.setCreatedAt(createdAt);
        log.setUpdatedAt(createdAt);

        return log;
    }

    @Test
    @DisplayName("测试findSuccessfulSearchQueriesByTimeRange - 查找指定时间范围内成功搜索的查询词")
    void testFindSuccessfulSearchQueriesByTimeRange() {
        // 测试查找所有成功搜索的查询词（不限时间）
        List<String> allQueries = repository.findSuccessfulSearchQueriesByTimeRange(null, null);

        assertEquals(4, allQueries.size(), "应该找到4个成功的搜索查询词");
        assertTrue(allQueries.contains("Java编程"), "应该包含Java编程");
        assertTrue(allQueries.contains("Spring Boot"), "应该包含Spring Boot");
        assertTrue(allQueries.contains(" 测试空白 "), "应该包含测试空白查询词");
        assertFalse(allQueries.contains("错误查询"), "不应该包含错误状态的查询");

        // 验证重复查询词会被多次返回（用于词频统计）
        long javaCount = allQueries.stream().filter(q -> "Java编程".equals(q)).count();
        assertEquals(2, javaCount, "Java编程应该出现2次");
    }

    @Test
    @DisplayName("测试findSuccessfulSearchQueriesByTimeRange - 按时间范围过滤")
    void testFindSuccessfulSearchQueriesByTimeRangeWithTimeFilter() {
        LocalDateTime startTime = baseTime.minusHours(2).minusMinutes(30);
        LocalDateTime endTime = baseTime.minusMinutes(30);

        List<String> filteredQueries = repository.findSuccessfulSearchQueriesByTimeRange(startTime, endTime);

        assertEquals(2, filteredQueries.size(), "指定时间范围内应该找到2个查询词");
        assertTrue(filteredQueries.contains("Java编程"), "应该包含Java编程");
        assertTrue(filteredQueries.contains("Spring Boot"), "应该包含Spring Boot");

        // 验证时间范围外的查询不被包含（超过2小时30分钟前的查询）
        // 由于我们的时间范围是从2.5小时前到30分钟前，3小时前的Java编程查询不应该被包含
        long javaCountInRange = filteredQueries.stream().filter(q -> "Java编程".equals(q)).count();
        assertEquals(1, javaCountInRange, "时间范围内应该只包含1个Java编程查询");
    }

    @Test
    @DisplayName("测试findSuccessfulSearchQueriesByTimeRange - 只指定开始时间")
    void testFindSuccessfulSearchQueriesByTimeRangeWithStartTimeOnly() {
        LocalDateTime startTime = baseTime.minusHours(2).minusMinutes(30);

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(startTime, null);

        assertEquals(2, queries.size(), "从指定开始时间到现在应该找到2个查询词");
        assertTrue(queries.contains("Java编程"), "应该包含Java编程");
        assertTrue(queries.contains("Spring Boot"), "应该包含Spring Boot");
    }

    @Test
    @DisplayName("测试findSuccessfulSearchQueriesByTimeRange - 只指定结束时间")
    void testFindSuccessfulSearchQueriesByTimeRangeWithEndTimeOnly() {
        LocalDateTime endTime = baseTime.minusHours(2).minusMinutes(30);

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(null, endTime);

        assertEquals(2, queries.size(), "从开始到指定结束时间应该找到2个查询词");
        assertTrue(queries.contains("Java编程"), "应该包含Java编程");
        assertTrue(queries.contains(" 测试空白 "), "应该包含测试空白");
    }

    @Test
    @DisplayName("测试countSuccessfulSearchesByTimeRange - 统计指定时间范围内成功搜索的总数")
    void testCountSuccessfulSearchesByTimeRange() {
        // 测试统计所有成功搜索（不限时间）
        long totalCount = repository.countSuccessfulSearchesByTimeRange(null, null);

        assertEquals(4L, totalCount, "总的成功搜索数量应该为4");

        // 测试指定时间范围的统计
        LocalDateTime startTime = baseTime.minusHours(2).minusMinutes(30);
        LocalDateTime endTime = baseTime.minusMinutes(30);

        long filteredCount = repository.countSuccessfulSearchesByTimeRange(startTime, endTime);

        assertEquals(2L, filteredCount, "指定时间范围内的成功搜索数量应该为2");
    }

    @Test
    @DisplayName("测试countSuccessfulSearchesByTimeRange - 只指定开始时间")
    void testCountSuccessfulSearchesByTimeRangeWithStartTimeOnly() {
        LocalDateTime startTime = baseTime.minusHours(1).minusMinutes(30);

        long count = repository.countSuccessfulSearchesByTimeRange(startTime, null);

        assertEquals(1L, count, "从指定开始时间到现在的成功搜索数量应该为1");
    }

    @Test
    @DisplayName("测试countSuccessfulSearchesByTimeRange - 只指定结束时间")
    void testCountSuccessfulSearchesByTimeRangeWithEndTimeOnly() {
        LocalDateTime endTime = baseTime.minusHours(1).minusMinutes(30);

        long count = repository.countSuccessfulSearchesByTimeRange(null, endTime);

        assertEquals(3L, count, "从开始到指定结束时间的成功搜索数量应该为3");
    }

    @Test
    @DisplayName("测试边界情况 - 空数据时的查询")
    void testBoundaryCase_EmptyData() {
        // 清空所有数据
        repository.deleteAll();
        entityManager.flush();

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(null, null);
        assertEquals(0, queries.size(), "空数据时查询词列表应该为空");

        long count = repository.countSuccessfulSearchesByTimeRange(null, null);
        assertEquals(0L, count, "空数据时统计数量应该为0");
    }

    @Test
    @DisplayName("测试边界情况 - 没有匹配时间范围的数据")
    void testBoundaryCase_NoMatchingTimeRange() {
        LocalDateTime futureStart = baseTime.plusHours(1);
        LocalDateTime futureEnd = baseTime.plusHours(2);

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(futureStart, futureEnd);
        assertEquals(0, queries.size(), "未来时间范围内应该没有查询词");

        long count = repository.countSuccessfulSearchesByTimeRange(futureStart, futureEnd);
        assertEquals(0L, count, "未来时间范围内的统计数量应该为0");
    }

    @Test
    @DisplayName("测试边界情况 - 只有错误状态的搜索日志")
    void testBoundaryCase_OnlyErrorStatus() {
        // 清空成功的搜索日志，只保留错误日志
        repository.delete(successLog1);
        repository.delete(successLog2);
        repository.delete(successLog3);
        repository.delete(emptyQueryLog);
        entityManager.flush();

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(null, null);
        assertEquals(0, queries.size(), "只有错误状态时查询词列表应该为空");

        long count = repository.countSuccessfulSearchesByTimeRange(null, null);
        assertEquals(0L, count, "只有错误状态时统计数量应该为0");
    }

    @Test
    @DisplayName("测试边界情况 - 查询词包含特殊字符")
    void testBoundaryCase_SpecialCharactersInQuery() {
        // 创建包含特殊字符的搜索日志
        SearchLog specialCharLog = createSearchLog("Java & Spring框架", SearchLogStatus.SUCCESS, baseTime);
        SearchLog unicodeLog = createSearchLog("🚀 微服务架构", SearchLogStatus.SUCCESS, baseTime);

        entityManager.persistAndFlush(specialCharLog);
        entityManager.persistAndFlush(unicodeLog);

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(baseTime.minusMinutes(1), baseTime.plusMinutes(1));

        assertTrue(queries.contains("Java & Spring框架"), "应该正确处理包含特殊字符的查询词");
        assertTrue(queries.contains("🚀 微服务架构"), "应该正确处理包含Unicode字符的查询词");
    }

    @Test
    @DisplayName("测试边界情况 - 查询词长度边界")
    void testBoundaryCase_QueryLength() {
        // 创建很短和很长的查询词
        SearchLog shortQueryLog = createSearchLog("a", SearchLogStatus.SUCCESS, baseTime);
        String longQuery = "a".repeat(1000); // 1000个字符的查询词
        SearchLog longQueryLog = createSearchLog(longQuery, SearchLogStatus.SUCCESS, baseTime);

        entityManager.persistAndFlush(shortQueryLog);
        entityManager.persistAndFlush(longQueryLog);

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(baseTime.minusMinutes(1), baseTime.plusMinutes(1));

        assertTrue(queries.contains("a"), "应该正确处理单字符查询词");
        assertTrue(queries.contains(longQuery), "应该正确处理长查询词");
    }

    @Test
    @DisplayName("测试数据完整性 - 验证查询条件准确性")
    void testDataIntegrity_QueryConditions() {
        // 创建各种状态的搜索日志来验证查询条件
        SearchLog timeoutLog = createSearchLog("超时查询", SearchLogStatus.TIMEOUT, baseTime);

        entityManager.persistAndFlush(timeoutLog);

        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(null, null);

        // 验证只包含SUCCESS状态的记录
        assertFalse(queries.contains("超时查询"), "不应该包含TIMEOUT状态的查询");

        // 验证统计数量的准确性
        long expectedCount = queries.size();
        long actualCount = repository.countSuccessfulSearchesByTimeRange(null, null);
        assertEquals(expectedCount, actualCount, "查询词数量和统计数量应该一致");
    }

    @Test
    @DisplayName("测试性能 - 大量数据查询")
    void testPerformance_LargeDataSet() {
        // 创建大量测试数据
        for (int i = 0; i < 100; i++) {
            SearchLog log = createSearchLog("测试查询" + (i % 10), SearchLogStatus.SUCCESS,
                    baseTime.minusMinutes(i));
            entityManager.persist(log);

            if (i % 20 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();

        // 测试查询性能（这里主要验证功能，实际性能测试需要更大的数据集）
        long startTime = System.currentTimeMillis();
        List<String> queries = repository.findSuccessfulSearchQueriesByTimeRange(null, null);
        long queryTime = System.currentTimeMillis() - startTime;

        assertTrue(queries.size() > 100, "应该查询到大量数据");
        assertTrue(queryTime < 1000, "查询时间应该在合理范围内（1秒内）");

        // 测试统计查询性能
        startTime = System.currentTimeMillis();
        long count = repository.countSuccessfulSearchesByTimeRange(null, null);
        long countTime = System.currentTimeMillis() - startTime;

        assertTrue(count > 100, "统计数量应该正确");
        assertTrue(countTime < 1000, "统计查询时间应该在合理范围内（1秒内）");
    }
}