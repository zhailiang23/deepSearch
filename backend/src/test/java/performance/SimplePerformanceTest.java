package performance;

import com.ynet.mgmt.searchlog.dto.HotWordRequest;
import com.ynet.mgmt.searchlog.dto.HotWordResponse;
import com.ynet.mgmt.searchlog.dto.SearchLogStatistics;
import com.ynet.mgmt.searchlog.dto.StatisticsRequest;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 简化的性能测试类
 * 直接测试Service层性能，避免复杂的Web层集成
 */
@Slf4j
@SpringBootTest(classes = com.ynet.mgmt.MgmtApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SimplePerformanceTest {

    @Autowired
    private SearchLogService searchLogService;

    @Autowired
    private SearchLogRepository searchLogRepository;

    // 性能目标
    private static final long MAX_SERVICE_RESPONSE_TIME_MS = 300;
    private static final int CONCURRENT_THREADS = 5;

    private List<Long> testDataIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createSimpleTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    /**
     * 创建简单测试数据
     */
    @Transactional
    void createSimpleTestData() {
        log.info("创建简单性能测试数据...");
        testDataIds.clear();

        String[] queries = {"Java", "Spring", "测试", "性能", "优化", "数据库", "缓存", "索引"};
        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);

        for (int i = 0; i < 100; i++) {
            SearchLog log = new SearchLog();
            log.setUserId((long) (i % 10 + 1));
            log.setSearchSpaceId((long) (i % 5 + 1));
            log.setSearchSpaceCode("SPACE_" + (i % 5 + 1));
            log.setSearchQuery(queries[i % queries.length] + " 测试 " + i);
            log.setStatus(SearchLogStatus.SUCCESS);
            log.setTotalResults((long) (Math.random() * 100));
            log.setTotalTimeMs((long) (Math.random() * 500 + 50));
            log.setIpAddress("192.168.1." + (i % 100 + 1));
            log.setSessionId("session_" + (i % 20));
            log.setTraceId("trace_" + i);
            log.setCreatedAt(baseTime.plusHours(i % 168)); // 一周内分布

            SearchLog saved = searchLogRepository.save(log);
            testDataIds.add(saved.getId());
        }

        log.info("创建了 {} 条简单测试数据", testDataIds.size());
    }

    /**
     * 清理测试数据
     */
    @Transactional
    void cleanupTestData() {
        if (!testDataIds.isEmpty()) {
            searchLogRepository.deleteAllById(testDataIds);
            log.info("清理了 {} 条测试数据", testDataIds.size());
        }
    }

    /**
     * 测试热词服务单次请求性能
     */
    @Test
    @Order(1)
    void testHotWordsServicePerformance() {
        log.info("=== 测试热词服务性能 ===");

        HotWordRequest request = HotWordRequest.builder()
                .limit(10)
                .minWordLength(1)
                .excludeStopWords(false)
                .startDate(LocalDateTime.now().minusDays(7))
                .endDate(LocalDateTime.now())
                .build();

        // 预热调用
        try {
            searchLogService.getHotWords(request);
        } catch (Exception e) {
            log.warn("预热调用失败: {}", e.getMessage());
        }

        // 正式测试
        List<Long> responseTimes = new ArrayList<>();
        int testRounds = 5;

        for (int i = 0; i < testRounds; i++) {
            long startTime = System.currentTimeMillis();

            try {
                List<HotWordResponse> result = searchLogService.getHotWords(request);
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;

                responseTimes.add(responseTime);
                log.debug("第{}次热词查询: {}ms, 返回{}个热词", i + 1, responseTime, result.size());

            } catch (Exception e) {
                log.error("热词查询失败", e);
                Assertions.fail("热词查询不应该失败: " + e.getMessage());
            }
        }

        // 分析性能
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        log.info("热词服务性能: 平均={}ms, 最大={}ms, 最小={}ms", avgTime, maxTime, minTime);

        Assertions.assertTrue(avgTime <= MAX_SERVICE_RESPONSE_TIME_MS,
                String.format("热词服务平均响应时间 %.2f ms 超过目标 %d ms", avgTime, MAX_SERVICE_RESPONSE_TIME_MS));
    }

    /**
     * 测试统计服务性能
     */
    @Test
    @Order(2)
    void testStatisticsServicePerformance() {
        log.info("=== 测试统计服务性能 ===");

        StatisticsRequest request = StatisticsRequest.builder()
                .startTime(LocalDateTime.now().minusDays(7))
                .endTime(LocalDateTime.now())
                .topQueriesLimit(5)
                .topSearchSpacesLimit(5)
                .build();

        // 预热调用
        try {
            searchLogService.getSearchStatistics(request);
        } catch (Exception e) {
            log.warn("统计服务预热失败: {}", e.getMessage());
        }

        // 正式测试
        List<Long> responseTimes = new ArrayList<>();
        int testRounds = 5;

        for (int i = 0; i < testRounds; i++) {
            long startTime = System.currentTimeMillis();

            try {
                SearchLogStatistics result = searchLogService.getSearchStatistics(request);
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;

                responseTimes.add(responseTime);
                log.debug("第{}次统计查询: {}ms, 总搜索={}", i + 1, responseTime, result.getTotalSearches());

            } catch (Exception e) {
                log.error("统计查询失败", e);
                Assertions.fail("统计查询不应该失败: " + e.getMessage());
            }
        }

        // 分析性能
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        log.info("统计服务性能: 平均={}ms, 最大={}ms", avgTime, maxTime);

        Assertions.assertTrue(avgTime <= MAX_SERVICE_RESPONSE_TIME_MS,
                String.format("统计服务平均响应时间 %.2f ms 超过目标 %d ms", avgTime, MAX_SERVICE_RESPONSE_TIME_MS));
    }

    /**
     * 测试简单并发性能
     */
    @Test
    @Order(3)
    void testSimpleConcurrentPerformance() throws InterruptedException, ExecutionException {
        log.info("=== 测试简单并发性能（{}线程）===", CONCURRENT_THREADS);

        HotWordRequest request = HotWordRequest.builder()
                .limit(5)
                .minWordLength(1)
                .excludeStopWords(false)
                .startDate(LocalDateTime.now().minusDays(3))
                .endDate(LocalDateTime.now())
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<Future<Long>> futures = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        // 提交并发任务
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            Future<Long> future = executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    List<HotWordResponse> result = searchLogService.getHotWords(request);
                    return System.currentTimeMillis() - startTime;
                } catch (Exception e) {
                    log.error("并发测试失败", e);
                    return -1L;
                }
            });
            futures.add(future);
        }

        // 收集结果
        List<Long> responseTimes = new ArrayList<>();
        int errorCount = 0;

        for (Future<Long> future : futures) {
            Long responseTime = future.get();
            if (responseTime > 0) {
                responseTimes.add(responseTime);
            } else {
                errorCount++;
            }
        }

        long testEndTime = System.currentTimeMillis();
        long totalTime = testEndTime - testStartTime;

        executor.shutdown();

        // 分析并发性能
        if (!responseTimes.isEmpty()) {
            double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            double successRate = (double) responseTimes.size() / CONCURRENT_THREADS * 100;

            log.info("并发性能结果:");
            log.info("- 总线程数: {}", CONCURRENT_THREADS);
            log.info("- 成功线程数: {}", responseTimes.size());
            log.info("- 错误线程数: {}", errorCount);
            log.info("- 成功率: {:.1f}%", successRate);
            log.info("- 平均响应时间: {:.1f}ms", avgTime);
            log.info("- 总测试时间: {}ms", totalTime);

            Assertions.assertTrue(successRate >= 80.0, "并发测试成功率应该 >= 80%");
            Assertions.assertTrue(avgTime <= MAX_SERVICE_RESPONSE_TIME_MS * 2,
                    "并发测试平均响应时间不应过长");
        } else {
            Assertions.fail("所有并发请求都失败了");
        }
    }

    /**
     * 测试数据库查询性能
     */
    @Test
    @Order(4)
    void testDatabaseQueryPerformance() {
        log.info("=== 测试数据库查询性能 ===");

        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        // 测试计数查询
        long queryStart = System.currentTimeMillis();
        long count = searchLogRepository.countByCreatedAtBetween(startTime, endTime);
        long queryTime = System.currentTimeMillis() - queryStart;

        log.info("计数查询: {}ms, 结果={}", queryTime, count);

        // 测试批量统计查询
        queryStart = System.currentTimeMillis();
        Object[] stats = searchLogRepository.findBatchStatistics(startTime, endTime);
        queryTime = System.currentTimeMillis() - queryStart;

        log.info("批量统计查询: {}ms", queryTime);

        // 测试热门查询
        queryStart = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(0, 10);
        var topQueries = searchLogRepository.findHotQueriesOptimized(startTime, endTime, pageable);
        queryTime = System.currentTimeMillis() - queryStart;

        log.info("热门查询优化版: {}ms, 结果数={}", queryTime, topQueries.size());

        // 所有查询都应该在合理时间内完成
        Assertions.assertTrue(queryTime <= 100, "数据库查询时间应该在100ms以内");
    }

    /**
     * 性能测试总结
     */
    @Test
    @Order(99)
    void performanceTestSummary() {
        log.info("=== 简单性能测试总结 ===");
        log.info("已完成服务层性能测试:");
        log.info("1. 热词服务单次请求性能");
        log.info("2. 统计服务性能");
        log.info("3. 简单并发性能测试");
        log.info("4. 数据库查询性能");
        log.info("性能目标: 服务响应时间 < {}ms", MAX_SERVICE_RESPONSE_TIME_MS);
        log.info("========================");
    }
}