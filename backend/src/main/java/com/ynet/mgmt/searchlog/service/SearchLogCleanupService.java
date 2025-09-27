package com.ynet.mgmt.searchlog.service;

import com.ynet.mgmt.common.exception.ServiceException;
import com.ynet.mgmt.searchlog.repository.SearchClickLogRepository;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索日志清理服务
 * 提供数据清理、归档和合规处理功能
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@Service
@Transactional
public class SearchLogCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(SearchLogCleanupService.class);

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private SearchClickLogRepository clickLogRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Value("${search.log.retention.days:30}")
    private int retentionDays;

    @Value("${search.log.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${search.log.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    /**
     * 定时删除过期的搜索日志
     * 默认每天凌晨2点执行
     */
    @Scheduled(cron = "${search.log.cleanup.schedule:0 0 2 * * ?}")
    public void deleteExpiredLogs() {
        if (!cleanupEnabled) {
            logger.info("搜索日志清理功能已禁用，跳过清理任务");
            return;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        logger.info("开始清理 {} 之前的搜索日志数据，保留天数: {}", cutoffDate, retentionDays);

        try {
            long startTime = System.currentTimeMillis();

            // 先删除关联的点击日志
            int deletedClicks = deleteExpiredClickLogs(cutoffDate);
            logger.info("清理过期点击日志: {} 条", deletedClicks);

            // 再删除搜索日志
            int deletedLogs = deleteExpiredSearchLogs(cutoffDate);
            logger.info("清理过期搜索日志: {} 条", deletedLogs);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 记录清理统计
            recordCleanupStats(deletedLogs, deletedClicks, duration);

            logger.info("搜索日志清理完成，耗时: {}ms，搜索日志: {}条，点击日志: {}条",
                       duration, deletedLogs, deletedClicks);

        } catch (Exception e) {
            logger.error("清理过期日志失败", e);
            throw new ServiceException("数据清理失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除过期点击日志
     *
     * @param cutoffDate 截止日期
     * @return 删除的总数
     */
    private int deleteExpiredClickLogs(LocalDateTime cutoffDate) {
        int totalDeleted = 0;
        int batchDeleted;

        logger.debug("开始批量删除过期点击日志，截止日期: {}, 批次大小: {}", cutoffDate, batchSize);

        do {
            try {
                batchDeleted = clickLogRepository.deleteExpiredClickLogsBatch(cutoffDate, batchSize);
                totalDeleted += batchDeleted;

                if (batchDeleted > 0) {
                    logger.debug("批量删除点击日志: {} 条", batchDeleted);

                    // 避免长时间占用连接和锁，短暂休眠
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("点击日志清理被中断");
                break;
            } catch (Exception e) {
                logger.error("批量删除点击日志失败", e);
                throw new ServiceException("批量删除点击日志失败: " + e.getMessage());
            }
        } while (batchDeleted > 0);

        return totalDeleted;
    }

    /**
     * 批量删除过期搜索日志
     *
     * @param cutoffDate 截止日期
     * @return 删除的总数
     */
    private int deleteExpiredSearchLogs(LocalDateTime cutoffDate) {
        int totalDeleted = 0;
        int batchDeleted;

        logger.debug("开始批量删除过期搜索日志，截止日期: {}, 批次大小: {}", cutoffDate, batchSize);

        do {
            try {
                batchDeleted = searchLogRepository.deleteExpiredSearchLogsBatch(cutoffDate, batchSize);
                totalDeleted += batchDeleted;

                if (batchDeleted > 0) {
                    logger.debug("批量删除搜索日志: {} 条", batchDeleted);

                    // 避免长时间占用连接和锁，短暂休眠
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("搜索日志清理被中断");
                break;
            } catch (Exception e) {
                logger.error("批量删除搜索日志失败", e);
                throw new ServiceException("批量删除搜索日志失败: " + e.getMessage());
            }
        } while (batchDeleted > 0);

        return totalDeleted;
    }

    /**
     * 记录清理统计
     *
     * @param deletedLogs   删除的搜索日志数
     * @param deletedClicks 删除的点击日志数
     * @param duration      清理耗时
     */
    private void recordCleanupStats(int deletedLogs, int deletedClicks, long duration) {
        logger.info("数据清理完成 - 搜索日志: {}, 点击日志: {}, 耗时: {}ms",
                   deletedLogs, deletedClicks, duration);

        // 发送清理完成事件
        DataCleanupCompletedEvent event = new DataCleanupCompletedEvent(
                deletedLogs, deletedClicks, duration, LocalDateTime.now()
        );
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 获取过期数据统计
     *
     * @return 过期数据统计信息
     */
    public Map<String, Object> getExpiredDataStats() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            long expiredLogs = searchLogRepository.countExpiredLogs(cutoffDate);
            long expiredClicks = clickLogRepository.countExpiredClicks(cutoffDate);
            long totalLogs = searchLogRepository.count();
            long totalClicks = clickLogRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("retentionDays", retentionDays);
            stats.put("cutoffDate", cutoffDate.toString());
            stats.put("expiredLogs", expiredLogs);
            stats.put("expiredClicks", expiredClicks);
            stats.put("totalLogs", totalLogs);
            stats.put("totalClicks", totalClicks);
            stats.put("expiredLogsPercent", totalLogs > 0 ? (double) expiredLogs / totalLogs * 100 : 0);
            stats.put("expiredClicksPercent", totalClicks > 0 ? (double) expiredClicks / totalClicks * 100 : 0);
            stats.put("cleanupEnabled", cleanupEnabled);
            stats.put("batchSize", batchSize);

            return stats;

        } catch (Exception e) {
            logger.error("获取过期数据统计失败", e);
            throw new ServiceException("获取过期数据统计失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发数据清理
     *
     * @return 清理结果
     */
    public Map<String, Object> manualCleanup() {
        if (!cleanupEnabled) {
            throw new ServiceException("搜索日志清理功能已禁用");
        }

        logger.info("手动触发搜索日志数据清理");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        long startTime = System.currentTimeMillis();

        try {
            // 先删除关联的点击日志
            int deletedClicks = deleteExpiredClickLogs(cutoffDate);

            // 再删除搜索日志
            int deletedLogs = deleteExpiredSearchLogs(cutoffDate);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 记录清理统计
            recordCleanupStats(deletedLogs, deletedClicks, duration);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("deletedLogs", deletedLogs);
            result.put("deletedClicks", deletedClicks);
            result.put("duration", duration);
            result.put("cutoffDate", cutoffDate.toString());
            result.put("timestamp", LocalDateTime.now().toString());

            return result;

        } catch (Exception e) {
            logger.error("手动清理失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now().toString());
            return result;
        }
    }

    /**
     * 获取清理配置信息
     *
     * @return 配置信息
     */
    public Map<String, Object> getCleanupConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", cleanupEnabled);
        config.put("retentionDays", retentionDays);
        config.put("batchSize", batchSize);
        config.put("nextCleanupDate", calculateNextCleanupDate());

        return config;
    }

    /**
     * 计算下次清理时间
     *
     * @return 下次清理时间
     */
    private String calculateNextCleanupDate() {
        // 简单计算：下一个凌晨2点
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextCleanup = now.toLocalDate().atTime(2, 0).plusDays(1);
        if (now.getHour() < 2) {
            nextCleanup = now.toLocalDate().atTime(2, 0);
        }
        return nextCleanup.toString();
    }

    /**
     * 数据清理完成事件
     */
    public static class DataCleanupCompletedEvent {
        private final int deletedLogs;
        private final int deletedClicks;
        private final long duration;
        private final LocalDateTime timestamp;

        public DataCleanupCompletedEvent(int deletedLogs, int deletedClicks, long duration, LocalDateTime timestamp) {
            this.deletedLogs = deletedLogs;
            this.deletedClicks = deletedClicks;
            this.duration = duration;
            this.timestamp = timestamp;
        }

        public int getDeletedLogs() {
            return deletedLogs;
        }

        public int getDeletedClicks() {
            return deletedClicks;
        }

        public long getDuration() {
            return duration;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}