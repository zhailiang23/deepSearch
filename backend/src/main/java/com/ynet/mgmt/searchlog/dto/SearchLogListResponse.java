package com.ynet.mgmt.searchlog.dto;

import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搜索日志列表响应DTO
 * 用于列表查询，排除大字段（responseData、requestParams、errorStackTrace）以提升性能
 *
 * @author Claude Code AI
 * @since 2025-10-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索日志列表项")
public class SearchLogListResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "搜索空间ID")
    private Long searchSpaceId;

    @Schema(description = "搜索空间代码")
    private String searchSpaceCode;

    @Schema(description = "搜索空间名称")
    private String searchSpaceName;

    @Schema(description = "搜索关键词")
    private String searchQuery;

    @Schema(description = "搜索模式")
    private String searchMode;

    @Schema(description = "是否启用拼音")
    private Boolean enablePinyin;

    @Schema(description = "页码")
    private Integer pageNumber;

    @Schema(description = "页大小")
    private Integer pageSize;

    @Schema(description = "结果总数")
    private Long totalResults;

    @Schema(description = "返回结果数")
    private Integer returnedResults;

    @Schema(description = "最大相关性分数")
    private BigDecimal maxScore;

    @Schema(description = "ES查询耗时(ms)")
    private Long elasticsearchTimeMs;

    @Schema(description = "总处理时间(ms)")
    private Long totalTimeMs;

    @Schema(description = "内存使用(MB)")
    private Integer memoryUsedMb;

    @Schema(description = "请求状态")
    private SearchLogStatus status;

    @Schema(description = "错误类型")
    private String errorType;

    @Schema(description = "错误消息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "更新者")
    private String updatedBy;
}
