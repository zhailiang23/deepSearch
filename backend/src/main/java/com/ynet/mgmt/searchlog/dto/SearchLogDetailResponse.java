package com.ynet.mgmt.searchlog.dto;

import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索日志详情响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索日志详情")
public class SearchLogDetailResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户IP（脱敏）")
    private String userIp;

    @Schema(description = "搜索空间ID")
    private String searchSpaceId;

    @Schema(description = "搜索空间代码")
    private String searchSpaceCode;

    @Schema(description = "查询关键词")
    private String query;

    @Schema(description = "结果数量")
    private Integer resultCount;

    @Schema(description = "响应时间(ms)")
    private Integer responseTime;

    @Schema(description = "状态")
    private SearchLogStatus status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "请求参数（JSON格式）")
    private String requestParams;

    @Schema(description = "响应数据（JSON格式）")
    private String responseData;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "搜索空间信息")
    private SearchSpaceInfo searchSpace;

    @Schema(description = "性能指标")
    private PerformanceMetrics performance;

    @Schema(description = "点击记录列表")
    private List<ClickLogInfo> clickLogs;

    /**
     * 搜索空间信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索空间信息")
    public static class SearchSpaceInfo {
        @Schema(description = "搜索空间名称")
        private String name;

        @Schema(description = "搜索空间描述")
        private String description;

        @Schema(description = "索引数量")
        private Integer indexCount;
    }

    /**
     * 性能指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "性能指标")
    public static class PerformanceMetrics {
        @Schema(description = "查询执行时间(ms)")
        private Integer queryTime;

        @Schema(description = "序列化时间(ms)")
        private Integer serializationTime;

        @Schema(description = "总处理时间(ms)")
        private Integer totalTime;

        @Schema(description = "是否超时")
        private Boolean isTimeout;
    }

    /**
     * 点击记录信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "点击记录信息")
    public static class ClickLogInfo {
        @Schema(description = "点击记录ID")
        private Long id;

        @Schema(description = "文档ID")
        private String documentId;

        @Schema(description = "文档标题")
        private String documentTitle;

        @Schema(description = "文档URL")
        private String documentUrl;

        @Schema(description = "点击位置")
        private Integer clickPosition;

        @Schema(description = "点击时间")
        private LocalDateTime clickTime;

        @Schema(description = "点击类型")
        private String clickType;
    }
}