package com.ynet.mgmt.searchlog.service;

import com.ynet.mgmt.common.exception.ServiceException;
import com.ynet.mgmt.searchlog.repository.SearchClickLogRepository;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * SearchLogCleanupService测试类
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("搜索日志清理服务测试")
class SearchLogCleanupServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private SearchClickLogRepository clickLogRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SearchLogCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new SearchLogCleanupService();
        ReflectionTestUtils.setField(cleanupService, "searchLogRepository", searchLogRepository);
        ReflectionTestUtils.setField(cleanupService, "clickLogRepository", clickLogRepository);
        ReflectionTestUtils.setField(cleanupService, "applicationEventPublisher", applicationEventPublisher);
        ReflectionTestUtils.setField(cleanupService, "retentionDays", 30);
        ReflectionTestUtils.setField(cleanupService, "batchSize", 1000);
        ReflectionTestUtils.setField(cleanupService, "cleanupEnabled", true);
    }

    @Test
    @DisplayName("删除过期日志 - 成功清理")
    void testDeleteExpiredLogs_Success() {
        // 准备
        when(clickLogRepository.deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenReturn(50, 30, 0); // 第一批50条，第二批30条，第三批0条
        when(searchLogRepository.deleteExpiredSearchLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenReturn(20, 10, 0); // 第一批20条，第二批10条，第三批0条

        // 执行
        cleanupService.deleteExpiredLogs();

        // 验证
        verify(clickLogRepository, times(3)).deleteExpiredClickLogsBatch(any(LocalDateTime.class), eq(1000));
        verify(searchLogRepository, times(3)).deleteExpiredSearchLogsBatch(any(LocalDateTime.class), eq(1000));
        verify(applicationEventPublisher).publishEvent(any(SearchLogCleanupService.DataCleanupCompletedEvent.class));
    }

    @Test
    @DisplayName("删除过期日志 - 清理功能已禁用")
    void testDeleteExpiredLogs_Disabled() {
        // 准备 - 禁用清理功能
        ReflectionTestUtils.setField(cleanupService, "cleanupEnabled", false);

        // 执行
        cleanupService.deleteExpiredLogs();

        // 验证 - 不应该执行任何删除操作
        verify(clickLogRepository, never()).deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt());
        verify(searchLogRepository, never()).deleteExpiredSearchLogsBatch(any(LocalDateTime.class), anyInt());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("删除过期日志 - 数据库异常")
    void testDeleteExpiredLogs_DatabaseException() {
        // 准备
        when(clickLogRepository.deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // 执行和验证
        assertThatThrownBy(() -> cleanupService.deleteExpiredLogs())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("数据清理失败");
    }

    @Test
    @DisplayName("获取过期数据统计")
    void testGetExpiredDataStats() {
        // 准备
        when(searchLogRepository.countExpiredLogs(any(LocalDateTime.class))).thenReturn(100L);
        when(clickLogRepository.countExpiredClicks(any(LocalDateTime.class))).thenReturn(300L);
        when(searchLogRepository.count()).thenReturn(500L);
        when(clickLogRepository.count()).thenReturn(1500L);

        // 执行
        Map<String, Object> stats = cleanupService.getExpiredDataStats();

        // 验证
        assertThat(stats).containsKeys(
            "retentionDays", "cutoffDate", "expiredLogs", "expiredClicks",
            "totalLogs", "totalClicks", "expiredLogsPercent", "expiredClicksPercent",
            "cleanupEnabled", "batchSize"
        );

        assertThat(stats.get("retentionDays")).isEqualTo(30);
        assertThat(stats.get("expiredLogs")).isEqualTo(100L);
        assertThat(stats.get("expiredClicks")).isEqualTo(300L);
        assertThat(stats.get("totalLogs")).isEqualTo(500L);
        assertThat(stats.get("totalClicks")).isEqualTo(1500L);
        assertThat(stats.get("expiredLogsPercent")).isEqualTo(20.0); // 100/500 * 100
        assertThat(stats.get("expiredClicksPercent")).isEqualTo(20.0); // 300/1500 * 100
        assertThat(stats.get("cleanupEnabled")).isEqualTo(true);
        assertThat(stats.get("batchSize")).isEqualTo(1000);
    }

    @Test
    @DisplayName("获取过期数据统计 - 空数据库")
    void testGetExpiredDataStats_EmptyDatabase() {
        // 准备
        when(searchLogRepository.countExpiredLogs(any(LocalDateTime.class))).thenReturn(0L);
        when(clickLogRepository.countExpiredClicks(any(LocalDateTime.class))).thenReturn(0L);
        when(searchLogRepository.count()).thenReturn(0L);
        when(clickLogRepository.count()).thenReturn(0L);

        // 执行
        Map<String, Object> stats = cleanupService.getExpiredDataStats();

        // 验证
        assertThat(stats.get("expiredLogs")).isEqualTo(0L);
        assertThat(stats.get("expiredClicks")).isEqualTo(0L);
        assertThat(stats.get("totalLogs")).isEqualTo(0L);
        assertThat(stats.get("totalClicks")).isEqualTo(0L);
        assertThat(stats.get("expiredLogsPercent")).isEqualTo(0.0);
        assertThat(stats.get("expiredClicksPercent")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("手动清理 - 成功")
    void testManualCleanup_Success() {
        // 准备
        when(clickLogRepository.deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenReturn(25, 0); // 第一批25条，第二批0条
        when(searchLogRepository.deleteExpiredSearchLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenReturn(15, 0); // 第一批15条，第二批0条

        // 执行
        Map<String, Object> result = cleanupService.manualCleanup();

        // 验证
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("deletedLogs")).isEqualTo(15);
        assertThat(result.get("deletedClicks")).isEqualTo(25);
        assertThat(result).containsKeys("duration", "cutoffDate", "timestamp");

        verify(applicationEventPublisher).publishEvent(any(SearchLogCleanupService.DataCleanupCompletedEvent.class));
    }

    @Test
    @DisplayName("手动清理 - 清理功能已禁用")
    void testManualCleanup_Disabled() {
        // 准备 - 禁用清理功能
        ReflectionTestUtils.setField(cleanupService, "cleanupEnabled", false);

        // 执行和验证
        assertThatThrownBy(() -> cleanupService.manualCleanup())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("搜索日志清理功能已禁用");
    }

    @Test
    @DisplayName("手动清理 - 异常情况")
    void testManualCleanup_Exception() {
        // 准备
        when(clickLogRepository.deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenThrow(new RuntimeException("Database error"));

        // 执行
        Map<String, Object> result = cleanupService.manualCleanup();

        // 验证
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("Database error");
        assertThat(result).containsKey("timestamp");
    }

    @Test
    @DisplayName("获取清理配置")
    void testGetCleanupConfig() {
        // 执行
        Map<String, Object> config = cleanupService.getCleanupConfig();

        // 验证
        assertThat(config).containsKeys("enabled", "retentionDays", "batchSize", "nextCleanupDate");
        assertThat(config.get("enabled")).isEqualTo(true);
        assertThat(config.get("retentionDays")).isEqualTo(30);
        assertThat(config.get("batchSize")).isEqualTo(1000);
        assertThat(config.get("nextCleanupDate")).isNotNull();
    }

    @Test
    @DisplayName("数据清理完成事件属性测试")
    void testDataCleanupCompletedEvent() {
        // 创建事件
        LocalDateTime timestamp = LocalDateTime.now();
        SearchLogCleanupService.DataCleanupCompletedEvent event =
            new SearchLogCleanupService.DataCleanupCompletedEvent(50, 150, 5000L, timestamp);

        // 验证属性
        assertThat(event.getDeletedLogs()).isEqualTo(50);
        assertThat(event.getDeletedClicks()).isEqualTo(150);
        assertThat(event.getDuration()).isEqualTo(5000L);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("批量删除逻辑测试 - 中断处理")
    void testBatchDeletionWithInterruption() {
        // 准备 - 模拟删除过程中的中断
        when(clickLogRepository.deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt()))
            .thenReturn(100, 100) // 前两批正常
            .thenAnswer(invocation -> {
                Thread.currentThread().interrupt(); // 模拟中断
                return 50;
            });

        // 执行 - 应该能正常处理中断而不抛出异常
        cleanupService.deleteExpiredLogs();

        // 验证 - 至少执行了部分删除操作
        verify(clickLogRepository, atLeast(2)).deleteExpiredClickLogsBatch(any(LocalDateTime.class), anyInt());
    }

    @Test
    @DisplayName("获取过期数据统计 - 数据库异常")
    void testGetExpiredDataStats_DatabaseException() {
        // 准备
        when(searchLogRepository.countExpiredLogs(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // 执行和验证
        assertThatThrownBy(() -> cleanupService.getExpiredDataStats())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("获取过期数据统计失败");
    }

    @Test
    @DisplayName("不同保留天数配置测试")
    void testDifferentRetentionDays() {
        // 准备 - 设置不同的保留天数
        ReflectionTestUtils.setField(cleanupService, "retentionDays", 7);

        when(searchLogRepository.countExpiredLogs(any(LocalDateTime.class))).thenReturn(200L);
        when(clickLogRepository.countExpiredClicks(any(LocalDateTime.class))).thenReturn(600L);
        when(searchLogRepository.count()).thenReturn(1000L);
        when(clickLogRepository.count()).thenReturn(3000L);

        // 执行
        Map<String, Object> stats = cleanupService.getExpiredDataStats();

        // 验证
        assertThat(stats.get("retentionDays")).isEqualTo(7);
        assertThat(stats.get("expiredLogsPercent")).isEqualTo(20.0); // 200/1000 * 100
    }
}