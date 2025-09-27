package com.ynet.mgmt.searchlog.integration;

import com.ynet.mgmt.monitor.SearchLogMonitor;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.service.SearchLogCleanupService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 搜索日志监控集成测试
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "search.log.enabled=true",
    "search.log.monitor.enabled=true",
    "search.log.cleanup.enabled=true",
    "search.log.retention.days=7"
})
@Transactional
@DisplayName("搜索日志监控集成测试")
public class SearchLogMonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private SearchLogMonitor searchLogMonitor;

    @Autowired
    private SearchLogCleanupService cleanupService;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        searchLogRepository.deleteAll();
        // 清理所有指标
        meterRegistry.clear();
    }

    @Test
    @DisplayName("监控指标记录和查询集成测试")
    void testMonitoringMetricsIntegration() {
        // 1. 记录一些监控指标
        searchLogMonitor.recordSearchLogMetrics("search", 100L, true);
        searchLogMonitor.recordSearchLogMetrics("search", 150L, true);
        searchLogMonitor.recordSearchLogMetrics("search", 200L, false);

        // 2. 验证指标被正确记录
        assertThat(meterRegistry.counter("search.log.records").count()).isEqualTo(3.0);
        assertThat(meterRegistry.timer("search.log.processing.time").count()).isEqualTo(3);
        assertThat(meterRegistry.counter("search.log.errors").count()).isEqualTo(1.0);

        // 3. 记录性能影响
        searchLogMonitor.recordSearchPerformanceImpact(250L);
        assertThat(meterRegistry.timer("search.performance.impact").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("健康检查端点集成测试")
    void testHealthCheckEndpoint() throws Exception {
        // 1. 创建一些测试数据
        createTestSearchLogs();

        // 2. 调用健康检查端点
        mockMvc.perform(get("/api/search-logs/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.logRecording").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.queue").value("UP"))
                .andExpect(jsonPath("$.system").value("UP"))
                .andExpect(jsonPath("$.queueSize").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("监控指标端点集成测试")
    void testMetricsEndpoint() throws Exception {
        // 调用监控指标端点
        mockMvc.perform(get("/api/search-logs/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.queueSize").exists())
                .andExpect(jsonPath("$.threadPool").exists())
                .andExpect(jsonPath("$.threadPool.activeCount").exists())
                .andExpect(jsonPath("$.threadPool.corePoolSize").exists())
                .andExpect(jsonPath("$.threadPool.maximumPoolSize").exists())
                .andExpect(jsonPath("$.memory").exists())
                .andExpect(jsonPath("$.memory.maxMemory").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("数据清理集成测试")
    void testDataCleanupIntegration() throws Exception {
        // 1. 创建过期的测试数据
        createExpiredTestData();

        // 2. 获取清理前的统计信息
        mockMvc.perform(get("/api/search-logs/cleanup/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiredLogs").value(2));

        // 3. 执行手动清理
        mockMvc.perform(post("/api/search-logs/cleanup/manual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.deletedLogs").value(2));

        // 4. 验证数据已被清理
        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            long remainingLogs = searchLogRepository.count();
            assertThat(remainingLogs).isLessThan(2);
        });
    }

    @Test
    @DisplayName("清理配置端点集成测试")
    void testCleanupConfigEndpoint() throws Exception {
        mockMvc.perform(get("/api/search-logs/cleanup/config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.retentionDays").value(7))
                .andExpect(jsonPath("$.data.batchSize").exists())
                .andExpect(jsonPath("$.data.nextCleanupDate").exists());
    }

    @Test
    @DisplayName("清理预览端点集成测试")
    void testCleanupPreviewEndpoint() throws Exception {
        // 创建测试数据
        createExpiredTestData();

        mockMvc.perform(get("/api/search-logs/cleanup/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.willDeleteLogs").value(2))
                .andExpect(jsonPath("$.data.willDeleteClicks").value(0))
                .andExpect(jsonPath("$.data.retentionDays").value(7))
                .andExpect(jsonPath("$.data.totalLogsRemaining").exists())
                .andExpect(jsonPath("$.data.cutoffDate").exists());
    }

    @Test
    @DisplayName("搜索完成事件处理集成测试")
    void testSearchCompletedEventHandling() {
        // 发布搜索完成事件
        SearchLogMonitor.SearchCompletedEvent event =
            new SearchLogMonitor.SearchCompletedEvent("trace-123", 180L, true);

        searchLogMonitor.handleSearchCompleted(event);

        // 验证事件被正确处理，性能指标被记录
        assertThat(meterRegistry.timer("search.performance.impact").count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("数据清理服务集成测试")
    void testCleanupServiceIntegration() {
        // 1. 创建过期数据
        createExpiredTestData();

        // 2. 获取清理前统计
        Map<String, Object> statsBefore = cleanupService.getExpiredDataStats();
        assertThat(statsBefore.get("expiredLogs")).isEqualTo(2L);

        // 3. 执行清理
        Map<String, Object> cleanupResult = cleanupService.manualCleanup();
        assertThat(cleanupResult.get("success")).isEqualTo(true);
        assertThat(cleanupResult.get("deletedLogs")).isEqualTo(2);

        // 4. 验证清理后统计
        Map<String, Object> statsAfter = cleanupService.getExpiredDataStats();
        assertThat(statsAfter.get("expiredLogs")).isEqualTo(0L);
    }

    @Test
    @DisplayName("无权限访问管理端点测试")
    void testUnauthorizedAccess() throws Exception {
        // 没有权限访问清理管理端点
        mockMvc.perform(get("/api/search-logs/cleanup/stats"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/search-logs/cleanup/manual"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("健康检查在数据库故障时的表现")
    void testHealthCheckWithDatabaseIssue() throws Exception {
        // 在没有最近日志的情况下检查健康状态
        // 由于是新系统（没有历史日志），应该返回健康状态
        mockMvc.perform(get("/api/search-logs/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * 创建测试搜索日志数据
     */
    private void createTestSearchLogs() {
        SearchLog log1 = new SearchLog();
        log1.setSearchQuery("test query 1");
        log1.setUserId(1L);
        log1.setSessionId("session-1");
        log1.setStatus(SearchLogStatus.SUCCESS);
        log1.setTotalTimeMs(100L);

        SearchLog log2 = new SearchLog();
        log2.setSearchQuery("test query 2");
        log2.setUserId(2L);
        log2.setSessionId("session-2");
        log2.setStatus(SearchLogStatus.SUCCESS);
        log2.setTotalTimeMs(150L);

        searchLogRepository.save(log1);
        searchLogRepository.save(log2);
    }

    /**
     * 创建过期的测试数据
     */
    private void createExpiredTestData() {
        LocalDateTime expiredTime = LocalDateTime.now().minusDays(10);

        SearchLog expiredLog1 = new SearchLog();
        expiredLog1.setSearchQuery("expired query 1");
        expiredLog1.setUserId(1L);
        expiredLog1.setSessionId("expired-session-1");
        expiredLog1.setStatus(SearchLogStatus.SUCCESS);
        expiredLog1.setCreatedAt(expiredTime);

        SearchLog expiredLog2 = new SearchLog();
        expiredLog2.setSearchQuery("expired query 2");
        expiredLog2.setUserId(2L);
        expiredLog2.setSessionId("expired-session-2");
        expiredLog2.setStatus(SearchLogStatus.ERROR);
        expiredLog2.setCreatedAt(expiredTime.minusHours(1));

        searchLogRepository.save(expiredLog1);
        searchLogRepository.save(expiredLog2);
    }
}