package com.ynet.mgmt.monitor;

import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * SearchLogMonitor测试类
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("搜索日志监控组件测试")
class SearchLogMonitorTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    private MeterRegistry meterRegistry;
    private SearchLogMonitor searchLogMonitor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        // 模拟ThreadPoolTaskExecutor
        when(asyncTaskExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(new java.util.concurrent.LinkedBlockingQueue<>());
        when(threadPoolExecutor.getActiveCount()).thenReturn(2);
        when(threadPoolExecutor.getCorePoolSize()).thenReturn(4);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(10);
        when(threadPoolExecutor.getCompletedTaskCount()).thenReturn(100L);

        searchLogMonitor = new SearchLogMonitor(meterRegistry, searchLogRepository, asyncTaskExecutor);
    }

    @Test
    @DisplayName("记录搜索日志指标 - 成功情况")
    void testRecordSearchLogMetrics_Success() {
        // 执行
        searchLogMonitor.recordSearchLogMetrics("search", 100L, true);

        // 验证指标是否正确记录
        assertThat(meterRegistry.counter("search.log.records").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("search.log.processing.time").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("search.log.errors").count()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("记录搜索日志指标 - 失败情况")
    void testRecordSearchLogMetrics_Failure() {
        // 执行
        searchLogMonitor.recordSearchLogMetrics("search", 200L, false);

        // 验证指标是否正确记录
        assertThat(meterRegistry.counter("search.log.records").count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer("search.log.processing.time").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("search.log.errors").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("记录搜索性能影响")
    void testRecordSearchPerformanceImpact() {
        // 执行
        searchLogMonitor.recordSearchPerformanceImpact(150L);

        // 验证指标是否正确记录
        assertThat(meterRegistry.timer("search.performance.impact").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("处理搜索完成事件")
    void testHandleSearchCompleted() {
        // 准备
        SearchLogMonitor.SearchCompletedEvent event =
            new SearchLogMonitor.SearchCompletedEvent("trace-123", 120L, true);

        // 执行
        searchLogMonitor.handleSearchCompleted(event);

        // 验证性能影响指标被记录
        assertThat(meterRegistry.timer("search.performance.impact").count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("健康检查 - 所有组件健康")
    void testHealthCheck_AllHealthy() {
        // 准备
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        when(searchLogRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(10L);
        when(searchLogRepository.count()).thenReturn(100L);

        // 执行
        ResponseEntity<Map<String, Object>> response = searchLogMonitor.healthCheck();

        // 验证
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("UP");
        assertThat(body.get("logRecording")).isEqualTo("UP");
        assertThat(body.get("database")).isEqualTo("UP");
        assertThat(body.get("queue")).isEqualTo("UP");
        assertThat(body.get("system")).isEqualTo("UP");
    }

    @Test
    @DisplayName("健康检查 - 数据库异常")
    void testHealthCheck_DatabaseUnhealthy() {
        // 准备
        when(searchLogRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // 执行
        ResponseEntity<Map<String, Object>> response = searchLogMonitor.healthCheck();

        // 验证
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("健康检查 - 日志记录不活跃")
    void testHealthCheck_LogRecordingInactive() {
        // 准备 - 最近5分钟没有日志记录
        when(searchLogRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(searchLogRepository.count()).thenReturn(100L); // 历史有日志，说明系统不是新的

        // 执行
        ResponseEntity<Map<String, Object>> response = searchLogMonitor.healthCheck();

        // 验证
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("DOWN");
        assertThat(body.get("logRecording")).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("健康检查 - 新系统无日志记录")
    void testHealthCheck_NewSystemNoLogs() {
        // 准备 - 新系统，完全没有日志
        when(searchLogRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(0L);
        when(searchLogRepository.count()).thenReturn(0L); // 历史也没有日志，说明是新系统

        // 执行
        ResponseEntity<Map<String, Object>> response = searchLogMonitor.healthCheck();

        // 验证 - 新系统应该认为是健康的
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("logRecording")).isEqualTo("UP");
    }

    @Test
    @DisplayName("获取监控指标")
    void testGetMetrics() {
        // 执行
        ResponseEntity<Map<String, Object>> response = searchLogMonitor.getMetrics();

        // 验证
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKeys("queueSize", "threadPool", "memory", "timestamp");

        @SuppressWarnings("unchecked")
        Map<String, Object> threadPool = (Map<String, Object>) body.get("threadPool");
        assertThat(threadPool).containsKeys("activeCount", "corePoolSize", "maximumPoolSize", "completedTaskCount");

        @SuppressWarnings("unchecked")
        Map<String, Object> memory = (Map<String, Object>) body.get("memory");
        assertThat(memory).containsKeys("maxMemory", "totalMemory", "freeMemory", "usedMemory");
    }

    @Test
    @DisplayName("搜索完成事件属性测试")
    void testSearchCompletedEvent() {
        // 创建事件
        SearchLogMonitor.SearchCompletedEvent event =
            new SearchLogMonitor.SearchCompletedEvent("trace-456", 300L, false);

        // 验证属性
        assertThat(event.getTraceId()).isEqualTo("trace-456");
        assertThat(event.getDuration()).isEqualTo(300L);
        assertThat(event.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("队列大小监控 - 异常情况")
    void testQueueSizeMonitoring_Exception() {
        // 准备 - 模拟获取队列大小时发生异常
        when(asyncTaskExecutor.getThreadPoolExecutor()).thenThrow(new RuntimeException("Thread pool error"));

        // 重新创建监控器来触发Gauge注册
        SearchLogMonitor monitorWithError = new SearchLogMonitor(meterRegistry, searchLogRepository, asyncTaskExecutor);

        // 验证 - 应该返回0而不是抛出异常
        Gauge queueGauge = meterRegistry.find("search.log.queue.size").gauge();
        assertThat(queueGauge.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("多次记录指标累积测试")
    void testMultipleMetricsRecording() {
        // 记录多次指标
        searchLogMonitor.recordSearchLogMetrics("search", 100L, true);
        searchLogMonitor.recordSearchLogMetrics("search", 150L, true);
        searchLogMonitor.recordSearchLogMetrics("search", 200L, false);

        // 验证累积效果
        assertThat(meterRegistry.counter("search.log.records").count()).isEqualTo(3.0);
        assertThat(meterRegistry.timer("search.log.processing.time").count()).isEqualTo(3);
        assertThat(meterRegistry.counter("search.log.errors").count()).isEqualTo(1.0);
    }
}