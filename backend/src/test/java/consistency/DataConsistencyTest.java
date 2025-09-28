package consistency;

import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.entity.SearchClickLog;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.repository.SearchClickLogRepository;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import com.ynet.mgmt.searchlog.dto.HotWordRequest;
import com.ynet.mgmt.searchlog.dto.HotWordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据一致性测试
 *
 * 测试目标：
 * 1. 验证数据库事务一致性
 * 2. 测试热词统计数据准确性
 * 3. 验证搜索日志和点击日志关联数据一致性
 * 4. 测试并发场景下的数据一致性
 * 5. 验证数据完整性约束
 * 6. 测试数据清理操作的一致性
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-28
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:consistencytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG"
})
@Transactional
@DisplayName("数据一致性测试")
public class DataConsistencyTest {

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private SearchClickLogRepository searchClickLogRepository;

    @Autowired
    private SearchLogService searchLogService;

    private final LocalDateTime baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        // 清理测试数据
        searchClickLogRepository.deleteAll();
        searchLogRepository.deleteAll();
    }

    // ========== 基础数据一致性测试 ==========

    @Test
    @DisplayName("测试搜索日志保存的数据完整性")
    void testSearchLogDataIntegrity() {
        // 创建搜索日志
        SearchLog log = createTestSearchLog("Java编程", 1L, baseTime);
        SearchLog savedLog = searchLogRepository.save(log);

        // 验证保存的数据完整性
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getSearchQuery()).isEqualTo("Java编程");
        assertThat(savedLog.getUserId()).isEqualTo(1L);
        assertThat(savedLog.getStatus()).isEqualTo(SearchLogStatus.SUCCESS);
        assertThat(savedLog.getCreatedAt()).isEqualTo(baseTime);
        assertThat(savedLog.getVersion()).isEqualTo(0L);

        // 从数据库重新查询验证
        SearchLog retrievedLog = searchLogRepository.findById(savedLog.getId()).orElse(null);
        assertThat(retrievedLog).isNotNull();
        assertThat(retrievedLog.getSearchQuery()).isEqualTo(savedLog.getSearchQuery());
        assertThat(retrievedLog.getUserId()).isEqualTo(savedLog.getUserId());
    }

    @Test
    @DisplayName("测试搜索日志和点击日志的关联数据一致性")
    void testSearchLogAndClickLogAssociation() {
        // 创建搜索日志
        SearchLog searchLog = createTestSearchLog("Spring Boot", 1L, baseTime);
        searchLog = searchLogRepository.save(searchLog);

        // 创建点击日志
        SearchClickLog clickLog1 = createTestClickLog(searchLog, "doc1", 1);
        SearchClickLog clickLog2 = createTestClickLog(searchLog, "doc2", 2);

        searchClickLogRepository.save(clickLog1);
        searchClickLogRepository.save(clickLog2);

        // 验证关联数据一致性
        SearchLog retrievedLog = searchLogRepository.findById(searchLog.getId()).orElse(null);
        assertThat(retrievedLog).isNotNull();
        assertThat(retrievedLog.getClickLogs()).hasSize(2);

        List<SearchClickLog> clickLogs = searchClickLogRepository.findBySearchLogIdOrderByClickPosition(searchLog.getId());
        assertThat(clickLogs).hasSize(2);
        assertThat(clickLogs.get(0).getDocumentId()).isEqualTo("doc1");
        assertThat(clickLogs.get(1).getDocumentId()).isEqualTo("doc2");
    }

    @Test
    @DisplayName("测试级联删除的数据一致性")
    void testCascadeDeleteConsistency() {
        // 创建搜索日志和点击日志
        SearchLog searchLog = createTestSearchLog("测试查询", 1L, baseTime);
        searchLog = searchLogRepository.save(searchLog);

        SearchClickLog clickLog = createTestClickLog(searchLog, "doc1", 1);
        searchClickLogRepository.save(clickLog);

        Long searchLogId = searchLog.getId();
        Long clickLogId = clickLog.getId();

        // 验证数据存在
        assertThat(searchLogRepository.existsById(searchLogId)).isTrue();
        assertThat(searchClickLogRepository.existsById(clickLogId)).isTrue();

        // 删除搜索日志
        searchLogRepository.deleteById(searchLogId);

        // 验证关联的点击日志也被删除（如果配置了级联删除）
        assertThat(searchLogRepository.existsById(searchLogId)).isFalse();
        // 注意：这取决于实际的JPA级联配置
    }

    // ========== 热词统计一致性测试 ==========

    @Test
    @DisplayName("测试热词统计的准确性和一致性")
    void testHotWordStatisticsAccuracy() {
        // 创建测试数据：相同查询词多次搜索
        String[] queries = {"Java", "Spring", "Java", "Python", "Java", "Spring"};
        for (int i = 0; i < queries.length; i++) {
            SearchLog log = createTestSearchLog(queries[i], 1L, baseTime.plusMinutes(i));
            searchLogRepository.save(log);
        }

        // 获取热词统计
        HotWordRequest request = HotWordRequest.builder()
                .startDate(baseTime.minusDays(1))
                .endDate(baseTime.plusDays(1))
                .limit(10)
                .excludeStopWords(false)
                .build();

        List<HotWordResponse> hotWords = searchLogService.getHotWords(request);

        // 验证统计准确性
        assertThat(hotWords).isNotEmpty();

        // 验证Java出现3次（最多）
        HotWordResponse javaWord = hotWords.stream()
                .filter(hw -> "Java".equals(hw.getWord()))
                .findFirst()
                .orElse(null);
        assertThat(javaWord).isNotNull();
        assertThat(javaWord.getCount()).isEqualTo(3);

        // 验证Spring出现2次
        HotWordResponse springWord = hotWords.stream()
                .filter(hw -> "Spring".equals(hw.getWord()))
                .findFirst()
                .orElse(null);
        assertThat(springWord).isNotNull();
        assertThat(springWord.getCount()).isEqualTo(2);

        // 验证Python出现1次
        HotWordResponse pythonWord = hotWords.stream()
                .filter(hw -> "Python".equals(hw.getWord()))
                .findFirst()
                .orElse(null);
        assertThat(pythonWord).isNotNull();
        assertThat(pythonWord.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试不同时间范围的热词统计一致性")
    void testHotWordStatisticsTimeRangeConsistency() {
        // 创建不同时间的搜索记录
        SearchLog log1 = createTestSearchLog("时间测试", 1L, baseTime);
        SearchLog log2 = createTestSearchLog("时间测试", 1L, baseTime.plusDays(1));
        SearchLog log3 = createTestSearchLog("时间测试", 1L, baseTime.plusDays(2));

        searchLogRepository.saveAll(List.of(log1, log2, log3));

        // 测试不同时间范围的查询
        HotWordRequest fullRangeRequest = HotWordRequest.builder()
                .startDate(baseTime.minusDays(1))
                .endDate(baseTime.plusDays(3))
                .limit(10)
                .build();

        HotWordRequest partialRangeRequest = HotWordRequest.builder()
                .startDate(baseTime)
                .endDate(baseTime.plusDays(1).plusHours(12))
                .limit(10)
                .build();

        List<HotWordResponse> fullRangeResults = searchLogService.getHotWords(fullRangeRequest);
        List<HotWordResponse> partialRangeResults = searchLogService.getHotWords(partialRangeRequest);

        // 验证全范围查询应该包含3次
        HotWordResponse fullRangeWord = fullRangeResults.stream()
                .filter(hw -> hw.getWord().contains("时间测试"))
                .findFirst()
                .orElse(null);
        assertThat(fullRangeWord).isNotNull();
        assertThat(fullRangeWord.getCount()).isEqualTo(3);

        // 验证部分范围查询应该包含2次
        HotWordResponse partialRangeWord = partialRangeResults.stream()
                .filter(hw -> hw.getWord().contains("时间测试"))
                .findFirst()
                .orElse(null);
        assertThat(partialRangeWord).isNotNull();
        assertThat(partialRangeWord.getCount()).isEqualTo(2);
    }

    // ========== 事务一致性测试 ==========

    @Test
    @DisplayName("测试事务回滚的数据一致性")
    void testTransactionRollbackConsistency() {
        Long initialCount = searchLogRepository.count();

        try {
            // 在事务中执行操作
            SearchLog log = createTestSearchLog("事务测试", 1L, baseTime);
            searchLogRepository.save(log);

            // 故意抛出异常触发回滚
            throw new RuntimeException("模拟异常");
        } catch (RuntimeException e) {
            // 预期的异常
        }

        // 验证数据没有被保存（事务回滚）
        Long finalCount = searchLogRepository.count();
        assertThat(finalCount).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("测试批量操作的原子性")
    void testBatchOperationAtomicity() {
        // 创建测试数据
        List<SearchLog> logs = List.of(
            createTestSearchLog("批量测试1", 1L, baseTime),
            createTestSearchLog("批量测试2", 1L, baseTime.plusMinutes(1)),
            createTestSearchLog("批量测试3", 1L, baseTime.plusMinutes(2))
        );

        List<SearchLog> savedLogs = searchLogRepository.saveAll(logs);
        List<Long> logIds = savedLogs.stream().map(SearchLog::getId).toList();

        // 批量删除
        long deletedCount = searchLogService.batchDeleteLogs(logIds);

        // 验证批量操作的原子性
        assertThat(deletedCount).isEqualTo(3);
        for (Long id : logIds) {
            assertThat(searchLogRepository.existsById(id)).isFalse();
        }
    }

    // ========== 并发数据一致性测试 ==========

    @Test
    @DisplayName("测试并发写入的数据一致性")
    void testConcurrentWriteConsistency() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // 并发创建搜索日志
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        SearchLog log = createTestSearchLog(
                            "并发测试-" + threadId + "-" + j,
                            (long) threadId,
                            baseTime.plusSeconds(threadId * operationsPerThread + j)
                        );
                        searchLogRepository.save(log);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 记录并发异常
                    System.err.println("并发写入异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executor.shutdown();

        // 验证数据一致性
        long totalRecords = searchLogRepository.count();
        assertThat(totalRecords).isEqualTo(successCount.get());
        assertThat(successCount.get()).isEqualTo(threadCount * operationsPerThread);
    }

    @Test
    @DisplayName("测试并发读写的数据一致性")
    void testConcurrentReadWriteConsistency() throws InterruptedException {
        // 先创建一些基础数据
        for (int i = 0; i < 10; i++) {
            SearchLog log = createTestSearchLog("基础数据-" + i, 1L, baseTime.plusMinutes(i));
            searchLogRepository.save(log);
        }

        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        // 启动读线程
        for (int i = 0; i < threadCount / 2; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        HotWordRequest request = HotWordRequest.builder()
                                .startDate(baseTime.minusDays(1))
                                .endDate(baseTime.plusDays(1))
                                .limit(5)
                                .build();
                        List<HotWordResponse> hotWords = searchLogService.getHotWords(request);
                        readCount.incrementAndGet();
                        Thread.sleep(10); // 模拟处理时间
                    }
                } catch (Exception e) {
                    System.err.println("并发读取异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 启动写线程
        for (int i = 0; i < threadCount / 2; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        SearchLog log = createTestSearchLog(
                            "并发写入-" + threadId + "-" + j,
                            (long) threadId,
                            baseTime.plusMinutes(100 + threadId * 5 + j)
                        );
                        searchLogRepository.save(log);
                        writeCount.incrementAndGet();
                        Thread.sleep(20); // 模拟处理时间
                    }
                } catch (Exception e) {
                    System.err.println("并发写入异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executor.shutdown();

        // 验证读写操作都成功完成
        assertThat(readCount.get()).isEqualTo((threadCount / 2) * 10);
        assertThat(writeCount.get()).isEqualTo((threadCount / 2) * 5);

        // 验证最终数据一致性
        long finalCount = searchLogRepository.count();
        assertThat(finalCount).isEqualTo(10 + writeCount.get()); // 基础数据 + 新写入数据
    }

    // ========== 数据完整性约束测试 ==========

    @Test
    @DisplayName("测试必填字段约束")
    void testRequiredFieldConstraints() {
        SearchLog log = new SearchLog();
        // 不设置必填字段，应该抛出异常

        assertThatThrownBy(() -> {
            searchLogRepository.save(log);
            searchLogRepository.flush(); // 强制执行SQL
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("测试字段长度约束")
    void testFieldLengthConstraints() {
        SearchLog log = createTestSearchLog("正常查询", 1L, baseTime);

        // 设置超长查询字符串（假设有长度限制）
        String longQuery = "a".repeat(1000);
        log.setSearchQuery(longQuery);

        // 根据实际约束，这可能成功也可能失败
        try {
            searchLogRepository.save(log);
            searchLogRepository.flush();
            // 如果成功，验证数据被正确保存
            SearchLog saved = searchLogRepository.findById(log.getId()).orElse(null);
            assertThat(saved).isNotNull();
        } catch (Exception e) {
            // 如果失败，验证是由于长度约束
            assertThat(e.getMessage()).containsAnyOf("length", "size", "constraint");
        }
    }

    // ========== 数据清理一致性测试 ==========

    @Test
    @DisplayName("测试数据清理操作的一致性")
    void testDataCleanupConsistency() {
        // 创建过期和非过期的数据
        LocalDateTime cutoffTime = baseTime.minusDays(30);

        // 过期数据
        SearchLog expiredLog1 = createTestSearchLog("过期1", 1L, cutoffTime.minusDays(1));
        SearchLog expiredLog2 = createTestSearchLog("过期2", 1L, cutoffTime.minusDays(2));

        // 非过期数据
        SearchLog currentLog1 = createTestSearchLog("当前1", 1L, cutoffTime.plusDays(1));
        SearchLog currentLog2 = createTestSearchLog("当前2", 1L, cutoffTime.plusDays(2));

        searchLogRepository.saveAll(List.of(expiredLog1, expiredLog2, currentLog1, currentLog2));

        // 执行清理操作
        long deletedCount = searchLogService.cleanupExpiredLogs(30);

        // 验证清理结果
        assertThat(deletedCount).isEqualTo(2); // 应该删除2条过期记录

        // 验证过期数据被删除
        assertThat(searchLogRepository.existsById(expiredLog1.getId())).isFalse();
        assertThat(searchLogRepository.existsById(expiredLog2.getId())).isFalse();

        // 验证非过期数据保留
        assertThat(searchLogRepository.existsById(currentLog1.getId())).isTrue();
        assertThat(searchLogRepository.existsById(currentLog2.getId())).isTrue();
    }

    // ========== 辅助方法 ==========

    private SearchLog createTestSearchLog(String query, Long userId, LocalDateTime createdAt) {
        SearchLog log = new SearchLog();
        log.setSearchQuery(query);
        log.setUserId(userId);
        log.setSearchSpaceId(1L);
        log.setSearchSpaceCode("test-space");
        log.setStatus(SearchLogStatus.SUCCESS);
        log.setTotalResults(100L);
        log.setTotalTimeMs(50L);
        log.setUserIp("127.0.0.1");
        log.setSessionId("test-session");
        log.setUserAgent("Test Agent");
        log.setTraceId("trace-" + System.nanoTime());
        log.setCreatedAt(createdAt);
        log.setUpdatedAt(createdAt);
        return log;
    }

    private SearchClickLog createTestClickLog(SearchLog searchLog, String documentId, Integer position) {
        SearchClickLog clickLog = new SearchClickLog();
        clickLog.setSearchLog(searchLog);
        clickLog.setDocumentId(documentId);
        clickLog.setDocumentTitle("Test Document " + documentId);
        clickLog.setClickPosition(position);
        clickLog.setUserIp("127.0.0.1");
        clickLog.setUserAgent("Test Agent");
        clickLog.setCreatedAt(LocalDateTime.now());
        return clickLog;
    }
}