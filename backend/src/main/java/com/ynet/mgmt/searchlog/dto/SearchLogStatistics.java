package com.ynet.mgmt.searchlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索日志统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索日志统计数据")
public class SearchLogStatistics {

    @Schema(description = "总搜索次数")
    private Long totalSearches;

    @Schema(description = "成功搜索次数")
    private Long successfulSearches;

    @Schema(description = "失败搜索次数")
    private Long failedSearches;

    @Schema(description = "成功率")
    private Double successRate;

    @Schema(description = "平均响应时间(ms)")
    private Double averageResponseTime;

    @Schema(description = "最大响应时间(ms)")
    private Integer maxResponseTime;

    @Schema(description = "最小响应时间(ms)")
    private Integer minResponseTime;

    @Schema(description = "总点击次数")
    private Long totalClicks;

    @Schema(description = "点击率")
    private Double clickRate;

    @Schema(description = "平均点击位置")
    private Double averageClickPosition;

    @Schema(description = "热门查询关键词")
    private List<QueryStatistic> topQueries;

    @Schema(description = "热门搜索空间")
    private List<SearchSpaceStatistic> topSearchSpaces;

    @Schema(description = "用户统计")
    private List<UserStatistic> topUsers;

    @Schema(description = "时间段统计")
    private List<HourlyStatistic> hourlyStatistics;

    /**
     * 查询关键词统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "查询关键词统计")
    public static class QueryStatistic {
        @Schema(description = "查询关键词")
        private String query;

        @Schema(description = "搜索次数")
        private Long searchCount;

        @Schema(description = "平均响应时间(ms)")
        private Double averageResponseTime;

        @Schema(description = "点击次数")
        private Long clickCount;

        @Schema(description = "点击率")
        private Double clickRate;
    }

    /**
     * 搜索空间统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索空间统计")
    public static class SearchSpaceStatistic {
        @Schema(description = "搜索空间ID")
        private String searchSpaceId;

        @Schema(description = "搜索空间名称")
        private String searchSpaceName;

        @Schema(description = "搜索次数")
        private Long searchCount;

        @Schema(description = "平均响应时间(ms)")
        private Double averageResponseTime;

        @Schema(description = "成功率")
        private Double successRate;
    }

    /**
     * 用户统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户统计")
    public static class UserStatistic {
        @Schema(description = "用户ID")
        private String userId;

        @Schema(description = "搜索次数")
        private Long searchCount;

        @Schema(description = "点击次数")
        private Long clickCount;

        @Schema(description = "点击率")
        private Double clickRate;

        @Schema(description = "活跃时间段")
        private String mostActiveHour;
    }

    /**
     * 按小时统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "按小时统计")
    public static class HourlyStatistic {
        @Schema(description = "小时(0-23)")
        private Integer hour;

        @Schema(description = "搜索次数")
        private Long searchCount;

        @Schema(description = "平均响应时间(ms)")
        private Double averageResponseTime;

        @Schema(description = "成功率")
        private Double successRate;
    }
}