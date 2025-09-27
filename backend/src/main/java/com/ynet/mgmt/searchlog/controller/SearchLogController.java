package com.ynet.mgmt.searchlog.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchlog.dto.*;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索日志管理控制器
 * 提供搜索日志查询、统计、点击行为记录等API
 *
 * @author Claude
 */
@Slf4j
@RestController
@RequestMapping("/api/search-logs")
@Tag(name = "搜索日志管理", description = "搜索日志查询、统计和分析相关API")
public class SearchLogController {

    private final SearchLogService searchLogService;

    public SearchLogController(SearchLogService searchLogService) {
        this.searchLogService = searchLogService;
    }

    // ========== 查询操作 ==========

    @GetMapping
    @Operation(summary = "分页查询搜索日志",
               description = "支持多维度筛选的搜索日志分页查询，包括用户ID、搜索空间、关键词、时间范围等条件")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = PageResult.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<PageResult<SearchLog>>> getSearchLogs(
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "搜索空间ID") @RequestParam(required = false) String searchSpaceId,
            @Parameter(description = "搜索空间代码") @RequestParam(required = false) String searchSpaceCode,
            @Parameter(description = "查询关键词") @RequestParam(required = false) String query,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始时间 (格式: yyyy-MM-dd HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间 (格式: yyyy-MM-dd HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "最小响应时间(ms)") @RequestParam(required = false) Integer minResponseTime,
            @Parameter(description = "最大响应时间(ms)") @RequestParam(required = false) Integer maxResponseTime,
            @Parameter(description = "用户IP") @RequestParam(required = false) String userIp,
            @Parameter(description = "会话ID") @RequestParam(required = false) String sessionId,
            @Parameter(description = "链路追踪ID") @RequestParam(required = false) String traceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        try {
            // 构建查询请求
            SearchLogQueryRequest request = SearchLogQueryRequest.builder()
                    .userId(userId)
                    .searchSpaceId(searchSpaceId)
                    .searchSpaceCode(searchSpaceCode)
                    .query(query)
                    .status(status != null ? Enum.valueOf(com.ynet.mgmt.searchlog.entity.SearchLogStatus.class, status.toUpperCase()) : null)
                    .startTime(startTime)
                    .endTime(endTime)
                    .minResponseTime(minResponseTime)
                    .maxResponseTime(maxResponseTime)
                    .userIp(userIp)
                    .sessionId(sessionId)
                    .traceId(traceId)
                    .build();

            Page<SearchLog> pageResult = searchLogService.getSearchLogs(request, pageable);

            // 转换为PageResult
            PageResult<SearchLog> result = PageResult.<SearchLog>builder()
                    .content(pageResult.getContent())
                    .page(pageResult.getNumber())
                    .size(pageResult.getSize())
                    .totalElements(pageResult.getTotalElements())
                    .totalPages(pageResult.getTotalPages())
                    .first(pageResult.isFirst())
                    .last(pageResult.isLast())
                    .build();

            log.info("查询搜索日志成功，共{}条记录", result.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IllegalArgumentException e) {
            log.warn("查询搜索日志参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("查询参数错误: " + e.getMessage()));
        } catch (Exception e) {
            log.error("查询搜索日志失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("查询搜索日志失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取搜索日志详情",
               description = "根据日志ID获取详细的搜索日志信息，包括请求参数、响应数据、点击记录等")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
            content = @Content(schema = @Schema(implementation = SearchLogDetailResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "日志不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<SearchLogDetailResponse>> getSearchLogDetail(
            @Parameter(description = "搜索日志ID", required = true) @PathVariable Long id) {

        try {
            SearchLogDetailResponse detail = searchLogService.getSearchLogDetail(id);
            log.info("获取搜索日志详情成功，ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success(detail));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("不存在")) {
                log.warn("搜索日志不存在，ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            log.error("获取搜索日志详情失败，ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取搜索日志详情失败: " + e.getMessage()));
        }
    }

    // ========== 点击行为记录 ==========

    @PostMapping("/click")
    @Operation(summary = "记录搜索结果点击行为",
               description = "记录用户点击搜索结果的行为，用于统计分析和优化搜索体验")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "记录成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "搜索日志不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<Void>> recordClickAction(
            @Valid @RequestBody SearchClickRequest request,
            HttpServletRequest httpRequest) {

        try {
            // 补充IP和User Agent信息
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            request.setUserIp(clientIp);
            request.setUserAgent(userAgent);

            searchLogService.recordClickAction(request);

            log.info("记录点击行为成功，搜索日志ID: {}, 文档ID: {}, 位置: {}",
                     request.getSearchLogId(), request.getDocumentId(), request.getClickPosition());
            return ResponseEntity.ok(ApiResponse.success());

        } catch (IllegalArgumentException e) {
            log.warn("记录点击行为参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("请求参数错误: " + e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("不存在")) {
                log.warn("搜索日志不存在，无法记录点击行为，ID: {}", request.getSearchLogId());
                return ResponseEntity.notFound().build();
            }
            log.error("记录点击行为失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("记录点击行为失败: " + e.getMessage()));
        }
    }

    // ========== 统计分析 ==========

    @GetMapping("/statistics")
    @Operation(summary = "获取搜索日志统计数据",
               description = "根据时间范围和筛选条件获取搜索日志的统计分析数据，包括搜索次数、成功率、响应时间、热门查询等")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功",
            content = @Content(schema = @Schema(implementation = SearchLogStatistics.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<SearchLogStatistics>> getSearchStatistics(
            @Parameter(description = "开始时间 (格式: yyyy-MM-dd HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间 (格式: yyyy-MM-dd HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @Parameter(description = "用户ID筛选") @RequestParam(required = false) String userId,
            @Parameter(description = "搜索空间ID筛选") @RequestParam(required = false) String searchSpaceId,
            @Parameter(description = "包含详细统计") @RequestParam(defaultValue = "false") Boolean includeDetails,
            @Parameter(description = "热门查询数量限制") @RequestParam(defaultValue = "10") Integer topQueriesLimit,
            @Parameter(description = "热门搜索空间数量限制") @RequestParam(defaultValue = "10") Integer topSearchSpacesLimit,
            @Parameter(description = "热门用户数量限制") @RequestParam(defaultValue = "10") Integer topUsersLimit) {

        try {
            // 参数验证
            if (startTime.isAfter(endTime)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("开始时间不能晚于结束时间"));
            }

            StatisticsRequest request = StatisticsRequest.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .userId(userId)
                    .searchSpaceId(searchSpaceId)
                    .includeDetails(includeDetails)
                    .topQueriesLimit(topQueriesLimit)
                    .topSearchSpacesLimit(topSearchSpacesLimit)
                    .topUsersLimit(topUsersLimit)
                    .build();

            SearchLogStatistics statistics = searchLogService.getSearchStatistics(request);

            log.info("获取搜索统计数据成功，时间范围: {} 到 {}, 总搜索次数: {}",
                     startTime, endTime, statistics.getTotalSearches());
            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (IllegalArgumentException e) {
            log.warn("获取搜索统计参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("请求参数错误: " + e.getMessage()));
        } catch (Exception e) {
            log.error("获取搜索统计数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取搜索统计数据失败: " + e.getMessage()));
        }
    }

    // ========== 管理操作 ==========

    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期日志数据",
               description = "根据保留天数清理过期的搜索日志数据，释放存储空间")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "清理成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<Long>> cleanupExpiredLogs(
            @Parameter(description = "保留天数", required = true) @RequestParam Integer retentionDays) {

        try {
            if (retentionDays <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("保留天数必须大于0"));
            }

            long deletedCount = searchLogService.cleanupExpiredLogs(retentionDays);

            log.info("清理过期日志完成，保留{}天，删除{}条记录", retentionDays, deletedCount);
            return ResponseEntity.ok(ApiResponse.success("清理完成，删除 " + deletedCount + " 条记录", deletedCount));

        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("清理过期日志失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除搜索日志",
               description = "根据日志ID列表批量删除搜索日志记录")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ApiResponse<Long>> batchDeleteLogs(
            @Parameter(description = "要删除的日志ID列表", required = true)
            @RequestBody List<Long> logIds) {

        try {
            if (logIds == null || logIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("日志ID列表不能为空"));
            }

            long deletedCount = searchLogService.batchDeleteLogs(logIds);

            log.info("批量删除搜索日志完成，请求删除{}条，实际删除{}条", logIds.size(), deletedCount);
            return ResponseEntity.ok(ApiResponse.success("删除完成，删除 " + deletedCount + " 条记录", deletedCount));

        } catch (Exception e) {
            log.error("批量删除搜索日志失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("批量删除搜索日志失败: " + e.getMessage()));
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}