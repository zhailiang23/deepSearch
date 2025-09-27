package com.ynet.mgmt.searchlog.dto;

import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索日志查询请求DTO
 * 支持多条件筛选的动态查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索日志查询请求")
public class SearchLogQueryRequest {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "搜索空间ID")
    private String searchSpaceId;

    @Schema(description = "查询关键词")
    private String query;

    @Schema(description = "状态")
    private SearchLogStatus status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "最小响应时间(ms)")
    private Integer minResponseTime;

    @Schema(description = "最大响应时间(ms)")
    private Integer maxResponseTime;

    @Schema(description = "用户IP")
    private String userIp;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "搜索空间代码")
    private String searchSpaceCode;

    @Schema(description = "链路追踪ID")
    private String traceId;
}