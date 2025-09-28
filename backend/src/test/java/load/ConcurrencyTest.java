package load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchlog.dto.HotWordRequest;
import com.ynet.mgmt.searchlog.dto.HotWordResponse;
import com.ynet.mgmt.searchlog.dto.SearchClickRequest;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 并发性能测试
 *
 * 测试目标：
 * 1. 验证高并发场景下的数据一致性
 * 2. 测试热词统计在并发访问下的性能
 * 3. 验证数据库连接池和事务管理
 * 4. 测试内存使用和资源泄漏
 * 5. 验证API在高负载下的响应时间
 * 6. 测试并发读写的线程安全性
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-28
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:loadtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.hikari.maximum-pool-size=20",
    "spring.datasource.hikari.minimum-idle=5",
    "spring.jpa.properties.hibernate.jdbc.batch_size=25",
    "logging.level.com.ynet.mgmt=INFO"
})
@DisplayName("并发性能测试")
public class ConcurrencyTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private SearchLogService searchLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private final LocalDateTime baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 清理测试数据
        searchLogRepository.deleteAll();
    }

    // ========== 并发读取测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试高并发热词查询的性能和一致性")
    void testConcurrentHotWordQueries() throws InterruptedException {
        // 准备测试数据
        prepareTestData(1000);

        int threadCount = 20;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long startTime = System.currentTimeMillis();

                        try {
                            mockMvc.perform(get("/search-logs/hot-words")
                                    .param("limit", "10")
                                    .param("startDate", "2023-01-01 00:00:00")
                                    .param("endDate", "2023-01-02 00:00:00"))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.success").value(true));

                            long endTime = System.currentTimeMillis();
                            responseTimes.add(endTime - startTime);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("并发查询异常: " + e.getMessage());
                        }

                        // 短暂休息避免过载
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    System.err.println("线程执行异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // 验证结果
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan(threadCount * requestsPerThread * 0.95); // 至少95%成功
        assertThat(errorCount.get()).isLessThan(threadCount * requestsPerThread * 0.05); // 错误率低于5%

        // 性能验证
        double averageResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        System.out.printf("并发查询性能统计:%n");
        System.out.printf("- 总请求数: %d%n", threadCount * requestsPerThread);
        System.out.printf("- 成功数: %d%n", successCount.get());
        System.out.printf("- 错误数: %d%n", errorCount.get());
        System.out.printf("- 平均响应时间: %.2f ms%n", averageResponseTime);

        assertThat(averageResponseTime).isLessThan(5000); // 平均响应时间应小于5秒
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试并发搜索日志查询的性能")
    void testConcurrentSearchLogQueries() throws InterruptedException {
        // 准备测试数据
        prepareTestData(500);

        int threadCount = 15;
        int requestsPerThread = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long startTime = System.currentTimeMillis();

                        mockMvc.perform(get("/search-logs")
                                .param("userId", String.valueOf(threadId % 10))
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk());

                        long endTime = System.currentTimeMillis();
                        totalResponseTime.addAndGet(endTime - startTime);
                        successCount.incrementAndGet();

                        Thread.sleep(30);
                    }
                } catch (Exception e) {
                    System.err.println("搜索日志查询异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount * requestsPerThread);

        double averageResponseTime = totalResponseTime.get() / (double) successCount.get();
        System.out.printf("搜索日志并发查询平均响应时间: %.2f ms%n", averageResponseTime);
        assertThat(averageResponseTime).isLessThan(3000);
    }

    // ========== 并发写入测试 ==========

    @Test
    @DisplayName("测试并发点击记录的数据一致性")
    void testConcurrentClickRecording() throws InterruptedException {
        // 先创建一些搜索日志
        List<SearchLog> searchLogs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SearchLog log = createTestSearchLog("并发测试-" + i, (long) i, baseTime.plusMinutes(i));
            searchLogs.add(searchLogRepository.save(log));
        }

        int threadCount = 10;
        int clicksPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < clicksPerThread; j++) {
                        SearchClickRequest request = new SearchClickRequest();
                        request.setSearchLogId(searchLogs.get(threadId % searchLogs.size()).getId());
                        request.setDocumentId("doc-" + threadId + "-" + j);
                        request.setClickPosition(j + 1);

                        try {
                            mockMvc.perform(post("/search-logs/click")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                                    .with(csrf()))
                                    .andExpect(status().isOk());

                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("点击记录异常: " + e.getMessage());
                        }

                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    System.err.println("点击记录线程异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        System.out.printf("点击记录并发测试: 成功 %d, 错误 %d%n", successCount.get(), errorCount.get());
        assertThat(successCount.get()).isGreaterThan(threadCount * clicksPerThread * 0.9);
    }

    // ========== 混合并发测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试读写混合并发场景")
    void testMixedConcurrentOperations() throws InterruptedException {
        // 准备基础数据
        prepareTestData(200);

        int readerThreads = 8;
        int writerThreads = 4;
        int operationsPerThread = 5;

        ExecutorService executor = Executors.newFixedThreadPool(readerThreads + writerThreads);
        CountDownLatch latch = new CountDownLatch(readerThreads + writerThreads);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger writeSuccess = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        // 启动读线程
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            // 随机查询不同类型的数据
                            if (j % 3 == 0) {
                                // 热词查询
                                mockMvc.perform(get("/search-logs/hot-words")
                                        .param("limit", "5"))
                                        .andExpect(status().isOk());
                            } else if (j % 3 == 1) {
                                // 统计查询
                                mockMvc.perform(get("/search-logs/statistics")
                                        .param("startTime", "2023-01-01 00:00:00")
                                        .param("endTime", "2023-01-01 12:00:00"))
                                        .andExpect(status().isOk());
                            } else {
                                // 日志查询
                                mockMvc.perform(get("/search-logs")
                                        .param("page", "0")
                                        .param("size", "10"))
                                        .andExpect(status().isOk());
                            }
                            readSuccess.incrementAndGet();
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    System.err.println("读线程异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 启动写线程（创建新的搜索日志）
        for (int i = 0; i < writerThreads; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            SearchLog log = createTestSearchLog(
                                "并发写入-" + writerId + "-" + j,
                                (long) writerId,
                                baseTime.plusMinutes(100 + writerId * operationsPerThread + j)
                            );
                            searchLogRepository.save(log);
                            writeSuccess.incrementAndGet();
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                        Thread.sleep(150);
                    }
                } catch (Exception e) {
                    System.err.println("写线程异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        System.out.printf("混合并发测试结果: 读成功 %d, 写成功 %d, 错误 %d%n",
                readSuccess.get(), writeSuccess.get(), errors.get());

        assertThat(readSuccess.get()).isGreaterThan(readerThreads * operationsPerThread * 0.9);
        assertThat(writeSuccess.get()).isGreaterThan(writerThreads * operationsPerThread * 0.9);
        assertThat(errors.get()).isLessThan((readerThreads + writerThreads) * operationsPerThread * 0.1);
    }

    // ========== 压力测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("热词查询压力测试")
    void testHotWordQueryStressTest() throws InterruptedException {
        // 准备大量测试数据
        prepareTestData(2000);

        int threadCount = 25;
        int requestsPerThread = 20;
        long testDurationMs = 30000; // 30秒

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待开始信号

                    while (System.currentTimeMillis() - startTime < testDurationMs) {
                        long requestStart = System.currentTimeMillis();
                        totalRequests.incrementAndGet();

                        try {
                            mockMvc.perform(get("/search-logs/hot-words")
                                    .param("limit", String.valueOf(5 + (int)(Math.random() * 10)))
                                    .param("minWordLength", String.valueOf(1 + (int)(Math.random() * 3))))
                                    .andExpect(status().isOk());

                            long requestEnd = System.currentTimeMillis();
                            responseTimes.add(requestEnd - requestStart);
                            successfulRequests.incrementAndGet();

                        } catch (Exception e) {
                            // 压力测试中忽略个别错误
                        }

                        // 随机延迟
                        Thread.sleep((long)(Math.random() * 200));
                    }
                } catch (Exception e) {
                    System.err.println("压力测试线程异常: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 开始测试
        boolean completed = endLatch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // 计算性能指标
        double successRate = (double) successfulRequests.get() / totalRequests.get() * 100;
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        long maxResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        double throughput = successfulRequests.get() / (testDurationMs / 1000.0);

        System.out.printf("压力测试结果:%n");
        System.out.printf("- 测试时长: %d 秒%n", testDurationMs / 1000);
        System.out.printf("- 总请求数: %d%n", totalRequests.get());
        System.out.printf("- 成功请求数: %d%n", successfulRequests.get());
        System.out.printf("- 成功率: %.2f%%%n", successRate);
        System.out.printf("- 平均响应时间: %.2f ms%n", avgResponseTime);
        System.out.printf("- 最大响应时间: %d ms%n", maxResponseTime);
        System.out.printf("- 吞吐量: %.2f 请求/秒%n", throughput);

        // 验证性能要求
        assertThat(successRate).isGreaterThan(95.0); // 成功率大于95%
        assertThat(avgResponseTime).isLessThan(3000); // 平均响应时间小于3秒
        assertThat(maxResponseTime).isLessThan(10000); // 最大响应时间小于10秒
        assertThat(throughput).isGreaterThan(1.0); // 吞吐量大于1请求/秒
    }

    // ========== 内存和资源测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试内存使用和资源泄漏")
    void testMemoryUsageAndResourceLeaks() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.printf("初始内存使用: %d MB%n", initialMemory / 1024 / 1024);

        // 执行大量查询操作
        for (int batch = 0; batch < 5; batch++) {
            prepareTestData(100);

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(10);

            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 20; j++) {
                            HotWordRequest request = HotWordRequest.builder()
                                    .startDate(baseTime.minusDays(1))
                                    .endDate(baseTime.plusDays(1))
                                    .limit(50)
                                    .build();
                            searchLogService.getHotWords(request);
                        }
                    } catch (Exception e) {
                        System.err.println("内存测试异常: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // 强制垃圾回收
            System.gc();
            Thread.sleep(1000);

            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("批次 %d 后内存使用: %d MB%n", batch + 1, currentMemory / 1024 / 1024);

            // 清理数据
            searchLogRepository.deleteAll();
        }

        // 最终内存检查
        System.gc();
        Thread.sleep(2000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        System.out.printf("最终内存使用: %d MB%n", finalMemory / 1024 / 1024);
        System.out.printf("内存增长: %d MB%n", memoryIncrease / 1024 / 1024);

        // 验证没有严重的内存泄漏（允许一定的内存增长）
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // 小于100MB增长
    }

    // ========== 数据库连接池测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试数据库连接池在高并发下的表现")
    void testDatabaseConnectionPoolUnderLoad() throws InterruptedException {
        int threadCount = 30; // 超过连接池大小
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger connectionErrors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            // 执行数据库操作
                            SearchLog log = createTestSearchLog(
                                "连接池测试-" + threadId + "-" + j,
                                (long) threadId,
                                baseTime.plusMinutes(threadId * operationsPerThread + j)
                            );
                            searchLogRepository.save(log);

                            // 立即查询验证
                            searchLogRepository.findById(log.getId());
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            if (e.getMessage().contains("connection") || e.getMessage().contains("pool")) {
                                connectionErrors.incrementAndGet();
                            }
                            System.err.println("连接池测试异常: " + e.getMessage());
                        }

                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    System.err.println("连接池测试线程异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        System.out.printf("连接池测试结果: 成功 %d, 连接错误 %d%n", successCount.get(), connectionErrors.get());

        // 验证连接池能够处理高并发
        assertThat(successCount.get()).isGreaterThan(threadCount * operationsPerThread * 0.8);
        assertThat(connectionErrors.get()).isLessThan(threadCount * operationsPerThread * 0.1);
    }

    // ========== 辅助方法 ==========

    /**
     * 准备测试数据
     */
    private void prepareTestData(int count) {
        List<SearchLog> logs = new ArrayList<>();
        String[] queries = {"Java", "Spring", "Python", "JavaScript", "React", "Vue", "Angular", "Node.js", "MySQL", "Redis"};

        for (int i = 0; i < count; i++) {
            String query = queries[i % queries.length] + " 教程 " + (i / queries.length + 1);
            SearchLog log = createTestSearchLog(query, (long) (i % 50), baseTime.plusMinutes(i));
            logs.add(log);

            // 批量保存以提高性能
            if (logs.size() >= 50) {
                searchLogRepository.saveAll(logs);
                logs.clear();
            }
        }

        if (!logs.isEmpty()) {
            searchLogRepository.saveAll(logs);
        }
    }

    private SearchLog createTestSearchLog(String query, Long userId, LocalDateTime createdAt) {
        SearchLog log = new SearchLog();
        log.setSearchQuery(query);
        log.setUserId(userId);
        log.setSearchSpaceId(1L);
        log.setSearchSpaceCode("test-space");
        log.setStatus(SearchLogStatus.SUCCESS);
        log.setTotalResults((long) (Math.random() * 1000));
        log.setTotalTimeMs((long) (Math.random() * 500));
        log.setUserIp("127.0.0.1");
        log.setSessionId("session-" + userId);
        log.setUserAgent("Test Agent");
        log.setTraceId("trace-" + System.nanoTime());
        log.setCreatedAt(createdAt);
        log.setUpdatedAt(createdAt);
        return log;
    }
}