package com.ynet.mgmt.monitor;

import com.ynet.mgmt.common.exception.ServiceException;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 搜索日志监控组件
 * 提供系统监控、性能指标收集和健康检查功能
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@Component
@RestController
@ConditionalOnProperty(value = "search.log.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class SearchLogMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SearchLogMonitor.class);

    private final MeterRegistry meterRegistry;
    private final Counter searchLogRecords;
    private final Timer searchLogProcessingTime;
    private final Counter searchLogErrors;
    private final Timer searchPerformanceImpact;
    private final SearchLogRepository searchLogRepository;
    private final ThreadPoolTaskExecutor asyncTaskExecutor;

    @Autowired
    public SearchLogMonitor(MeterRegistry meterRegistry,
                           SearchLogRepository searchLogRepository,
                           ThreadPoolTaskExecutor asyncTaskExecutor) {
        this.meterRegistry = meterRegistry;
        this.searchLogRepository = searchLogRepository;
        this.asyncTaskExecutor = asyncTaskExecutor;

        // 初始化指标
        this.searchLogRecords = Counter.builder("search.log.records")
                .description("搜索日志记录总数")
                .register(meterRegistry);

        this.searchLogProcessingTime = Timer.builder("search.log.processing.time")
                .description("搜索日志处理时间")
                .register(meterRegistry);

        // 注册Gauge而不保存引用，因为它是自动更新的
        Gauge.builder("search.log.queue.size", this, SearchLogMonitor::getQueueSize)
                .description("搜索日志队列大小")
                .register(meterRegistry);

        this.searchLogErrors = Counter.builder("search.log.errors")
                .description("搜索日志错误数量")
                .register(meterRegistry);

        this.searchPerformanceImpact = Timer.builder("search.performance.impact")
                .description("搜索性能影响监控")
                .register(meterRegistry);

        logger.info("搜索日志监控组件已初始化");
    }

    /**
     * 记录搜索日志处理指标
     *
     * @param operation 操作类型
     * @param duration  处理时长（毫秒）
     * @param success   是否成功
     */
    public void recordSearchLogMetrics(String operation, long duration, boolean success) {
        // 记录处理计数
        Counter.builder("search.log.records")
                .tags("operation", operation, "success", String.valueOf(success))
                .register(meterRegistry)
                .increment();

        // 记录处理时间
        searchLogProcessingTime.record(duration, TimeUnit.MILLISECONDS);

        // 记录错误
        if (!success) {
            Counter.builder("search.log.errors")
                    .tags("operation", operation)
                    .register(meterRegistry)
                    .increment();
            logger.warn("搜索日志操作失败: operation={}, duration={}ms", operation, duration);
        }

        logger.debug("搜索日志指标记录: operation={}, duration={}ms, success={}",
                    operation, duration, success);
    }

    /**
     * 记录搜索性能影响
     *
     * @param searchDuration 搜索耗时（毫秒）
     */
    public void recordSearchPerformanceImpact(long searchDuration) {
        searchPerformanceImpact.record(searchDuration, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取任务队列大小
     *
     * @return 队列大小
     */
    private double getQueueSize() {
        try {
            ThreadPoolExecutor executor = asyncTaskExecutor.getThreadPoolExecutor();
            return executor != null ? executor.getQueue().size() : 0;
        } catch (Exception e) {
            logger.warn("获取队列大小失败", e);
            return 0;
        }
    }

    /**
     * 搜索完成事件监听器
     * 用于监控搜索性能影响
     */
    @EventListener
    public void handleSearchCompleted(SearchCompletedEvent event) {
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(searchPerformanceImpact);

            recordSearchPerformanceImpact(event.getDuration());

            logger.debug("搜索完成事件处理: traceId={}, duration={}ms",
                        event.getTraceId(), event.getDuration());
        } catch (Exception e) {
            logger.error("处理搜索完成事件失败", e);
        }
    }

    /**
     * 健康检查端点
     *
     * @return 健康状态信息
     */
    @GetMapping("/api/search-logs/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 检查日志记录是否正常
            boolean logRecordingHealthy = checkLogRecordingHealth();

            // 检查数据库连接
            boolean databaseHealthy = checkDatabaseHealth();

            // 检查队列状态
            double queueSize = getQueueSize();
            boolean queueHealthy = queueSize < 1000; // 队列大小正常阈值

            // 检查系统负载
            boolean systemHealthy = checkSystemHealth();

            // 组装健康状态
            health.put("logRecording", logRecordingHealthy ? "UP" : "DOWN");
            health.put("database", databaseHealthy ? "UP" : "DOWN");
            health.put("queue", queueHealthy ? "UP" : "DOWN");
            health.put("system", systemHealthy ? "UP" : "DOWN");
            health.put("queueSize", queueSize);
            health.put("timestamp", LocalDateTime.now().toString());

            boolean overallHealthy = logRecordingHealthy && databaseHealthy && queueHealthy && systemHealthy;
            health.put("status", overallHealthy ? "UP" : "DOWN");

            logger.debug("健康检查完成: status={}, queueSize={}",
                        overallHealthy ? "UP" : "DOWN", queueSize);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("健康检查失败", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 检查日志记录健康状态
     *
     * @return 是否健康
     */
    private boolean checkLogRecordingHealth() {
        try {
            // 检查最近5分钟是否有日志记录
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            long recentLogs = searchLogRepository.countByCreatedAtBetween(
                fiveMinutesAgo, LocalDateTime.now()
            );

            // 如果没有最近日志，检查历史是否有日志（防止新系统误报）
            if (recentLogs == 0) {
                long totalLogs = searchLogRepository.count();
                return totalLogs == 0; // 如果总数为0说明是新系统，返回健康
            }

            return recentLogs > 0;
        } catch (Exception e) {
            logger.error("检查日志记录健康状态失败", e);
            return false;
        }
    }

    /**
     * 检查数据库健康状态
     *
     * @return 是否健康
     */
    private boolean checkDatabaseHealth() {
        try {
            searchLogRepository.count();
            return true;
        } catch (Exception e) {
            logger.error("数据库健康检查失败", e);
            return false;
        }
    }

    /**
     * 检查系统健康状态
     *
     * @return 是否健康
     */
    private boolean checkSystemHealth() {
        try {
            // 检查内存使用率
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            // 内存使用率超过90%认为不健康
            boolean memoryHealthy = memoryUsagePercent < 90.0;

            // 检查线程池状态
            ThreadPoolExecutor executor = asyncTaskExecutor.getThreadPoolExecutor();
            boolean threadPoolHealthy = true;
            if (executor != null) {
                int activeCount = executor.getActiveCount();
                int maxPoolSize = executor.getMaximumPoolSize();
                // 活跃线程数接近最大值认为不健康
                threadPoolHealthy = (double) activeCount / maxPoolSize < 0.9;
            }

            logger.debug("系统健康检查: memoryUsage={}%, threadPoolHealthy={}",
                        String.format("%.2f", memoryUsagePercent), threadPoolHealthy);

            return memoryHealthy && threadPoolHealthy;

        } catch (Exception e) {
            logger.error("系统健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取监控指标
     *
     * @return 监控指标信息
     */
    @GetMapping("/api/search-logs/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // 队列信息
            metrics.put("queueSize", getQueueSize());

            // 线程池信息
            ThreadPoolExecutor executor = asyncTaskExecutor.getThreadPoolExecutor();
            if (executor != null) {
                Map<String, Object> threadPool = new HashMap<>();
                threadPool.put("activeCount", executor.getActiveCount());
                threadPool.put("corePoolSize", executor.getCorePoolSize());
                threadPool.put("maximumPoolSize", executor.getMaximumPoolSize());
                threadPool.put("completedTaskCount", executor.getCompletedTaskCount());
                metrics.put("threadPool", threadPool);
            }

            // 内存信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("maxMemory", runtime.maxMemory());
            memory.put("totalMemory", runtime.totalMemory());
            memory.put("freeMemory", runtime.freeMemory());
            memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            metrics.put("memory", memory);

            // 时间戳
            metrics.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            logger.error("获取监控指标失败", e);
            throw new ServiceException("获取监控指标失败: " + e.getMessage());
        }
    }

    /**
     * 搜索完成事件
     * 用于监控搜索性能
     */
    public static class SearchCompletedEvent {
        private final String traceId;
        private final long duration;
        private final boolean success;

        public SearchCompletedEvent(String traceId, long duration, boolean success) {
            this.traceId = traceId;
            this.duration = duration;
            this.success = success;
        }

        public String getTraceId() {
            return traceId;
        }

        public long getDuration() {
            return duration;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}