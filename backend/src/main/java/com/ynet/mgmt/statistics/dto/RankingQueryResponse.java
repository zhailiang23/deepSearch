package com.ynet.mgmt.statistics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜查询响应数据
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "排行榜查询响应数据")
public class RankingQueryResponse {

    @Schema(description = "排行榜数据列表")
    private List<MismatchKeywordRankingDTO> ranking;

    @Schema(description = "查询时间范围", example = "7d")
    private String timeRange;

    @Schema(description = "总记录数", example = "100")
    private Long totalCount;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页大小", example = "20")
    private Integer size;

    @Schema(description = "是否有下一页", example = "true")
    private Boolean hasNext;

    @Schema(description = "是否有上一页", example = "false")
    private Boolean hasPrev;

    @Schema(description = "总页数", example = "5")
    private Integer totalPages;

    @Schema(description = "统计信息")
    private StatisticsInfo statistics;

    @Schema(description = "查询时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime queryTime;

    public RankingQueryResponse() {
        this.queryTime = LocalDateTime.now();
    }

    public RankingQueryResponse(List<MismatchKeywordRankingDTO> ranking, String timeRange,
                               Long totalCount, Integer page, Integer size) {
        this.ranking = ranking;
        this.timeRange = timeRange;
        this.totalCount = totalCount;
        this.page = page;
        this.size = size;
        this.queryTime = LocalDateTime.now();
        this.calculatePagination();
    }

    /**
     * 计算分页信息
     */
    private void calculatePagination() {
        if (totalCount != null && size != null && size > 0) {
            this.totalPages = (int) Math.ceil((double) totalCount / size);
            this.hasNext = page != null && page < totalPages;
            this.hasPrev = page != null && page > 1;
        } else {
            this.totalPages = 0;
            this.hasNext = false;
            this.hasPrev = false;
        }
    }

    /**
     * 统计信息内部类
     */
    @Schema(description = "统计信息")
    public static class StatisticsInfo {

        @Schema(description = "总关键词数", example = "150")
        private Integer totalKeywords;

        @Schema(description = "平均不匹配率", example = "0.35")
        private Double avgMismatchRate;

        @Schema(description = "最高不匹配率", example = "0.95")
        private Double maxMismatchRate;

        @Schema(description = "最低不匹配率", example = "0.05")
        private Double minMismatchRate;

        @Schema(description = "数据更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastUpdated;

        public StatisticsInfo() {
        }

        public StatisticsInfo(Integer totalKeywords, Double avgMismatchRate,
                             Double maxMismatchRate, Double minMismatchRate) {
            this.totalKeywords = totalKeywords;
            this.avgMismatchRate = avgMismatchRate;
            this.maxMismatchRate = maxMismatchRate;
            this.minMismatchRate = minMismatchRate;
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters and Setters for StatisticsInfo
        public Integer getTotalKeywords() {
            return totalKeywords;
        }

        public void setTotalKeywords(Integer totalKeywords) {
            this.totalKeywords = totalKeywords;
        }

        public Double getAvgMismatchRate() {
            return avgMismatchRate;
        }

        public void setAvgMismatchRate(Double avgMismatchRate) {
            this.avgMismatchRate = avgMismatchRate;
        }

        public Double getMaxMismatchRate() {
            return maxMismatchRate;
        }

        public void setMaxMismatchRate(Double maxMismatchRate) {
            this.maxMismatchRate = maxMismatchRate;
        }

        public Double getMinMismatchRate() {
            return minMismatchRate;
        }

        public void setMinMismatchRate(Double minMismatchRate) {
            this.minMismatchRate = minMismatchRate;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
    }

    // Getters and Setters
    public List<MismatchKeywordRankingDTO> getRanking() {
        return ranking;
    }

    public void setRanking(List<MismatchKeywordRankingDTO> ranking) {
        this.ranking = ranking;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
        this.calculatePagination();
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
        this.calculatePagination();
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
        this.calculatePagination();
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrev() {
        return hasPrev;
    }

    public void setHasPrev(Boolean hasPrev) {
        this.hasPrev = hasPrev;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public StatisticsInfo getStatistics() {
        return statistics;
    }

    public void setStatistics(StatisticsInfo statistics) {
        this.statistics = statistics;
    }

    public LocalDateTime getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(LocalDateTime queryTime) {
        this.queryTime = queryTime;
    }
}