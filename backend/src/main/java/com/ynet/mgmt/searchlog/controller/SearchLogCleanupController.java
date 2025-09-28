package com.ynet.mgmt.searchlog.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchlog.service.SearchLogCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 搜索日志清理控制器
 * 提供数据清理管理和监控接口
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@RestController
@RequestMapping("/search-logs/cleanup")
@Tag(name = "搜索日志清理管理", description = "搜索日志数据清理和维护接口")
public class SearchLogCleanupController {

    private static final Logger logger = LoggerFactory.getLogger(SearchLogCleanupController.class);

    @Autowired
    private SearchLogCleanupService cleanupService;

    /**
     * 获取过期数据统计
     *
     * @return 过期数据统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取过期数据统计", description = "查看当前过期的搜索日志和点击日志统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getExpiredDataStats() {
        try {
            Map<String, Object> stats = cleanupService.getExpiredDataStats();
            logger.debug("获取过期数据统计: {}", stats);

            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            logger.error("获取过期数据统计失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取过期数据统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取清理配置信息
     *
     * @return 清理配置信息
     */
    @GetMapping("/config")
    @Operation(summary = "获取清理配置", description = "查看当前的数据清理配置参数")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getCleanupConfig() {
        try {
            Map<String, Object> config = cleanupService.getCleanupConfig();
            logger.debug("获取清理配置: {}", config);

            return ResponseEntity.ok(ApiResponse.success(config));

        } catch (Exception e) {
            logger.error("获取清理配置失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取清理配置失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发数据清理
     *
     * @return 清理结果
     */
    @PostMapping("/manual")
    @Operation(summary = "手动清理数据", description = "立即执行过期数据清理任务")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> manualCleanup() {
        try {
            logger.info("管理员手动触发搜索日志数据清理");

            Map<String, Object> result = cleanupService.manualCleanup();

            if ((Boolean) result.get("success")) {
                logger.info("手动清理成功: {}", result);
                return ResponseEntity.ok(ApiResponse.success("数据清理完成", result));
            } else {
                logger.error("手动清理失败: {}", result);
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("数据清理失败: " + result.get("error")));
            }

        } catch (Exception e) {
            logger.error("手动触发清理失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("手动触发清理失败: " + e.getMessage()));
        }
    }

    /**
     * 预览清理影响
     *
     * @return 清理预览信息
     */
    @GetMapping("/preview")
    @Operation(summary = "预览清理影响", description = "查看即将清理的数据统计，不执行实际清理")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> previewCleanup() {
        try {
            Map<String, Object> stats = cleanupService.getExpiredDataStats();

            // 构建预览信息
            Map<String, Object> preview = Map.of(
                "willDeleteLogs", stats.get("expiredLogs"),
                "willDeleteClicks", stats.get("expiredClicks"),
                "retentionDays", stats.get("retentionDays"),
                "cutoffDate", stats.get("cutoffDate"),
                "totalLogsRemaining", (Long) stats.get("totalLogs") - (Long) stats.get("expiredLogs"),
                "totalClicksRemaining", (Long) stats.get("totalClicks") - (Long) stats.get("expiredClicks")
            );

            logger.debug("清理预览: {}", preview);

            return ResponseEntity.ok(ApiResponse.success(preview));

        } catch (Exception e) {
            logger.error("获取清理预览失败", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取清理预览失败: " + e.getMessage()));
        }
    }
}