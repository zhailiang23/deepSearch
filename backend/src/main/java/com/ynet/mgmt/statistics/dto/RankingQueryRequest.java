package com.ynet.mgmt.statistics.dto;

import com.ynet.mgmt.statistics.enums.TimeRangeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 排行榜查询请求参数
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "排行榜查询请求参数")
public class RankingQueryRequest {

    @Schema(description = "时间范围", example = "7d", allowableValues = {"7d", "30d", "90d"})
    @NotNull(message = "时间范围不能为空")
    private String timeRange;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20;

    @Schema(description = "搜索关键词", example = "银行")
    private String keyword;

    @Schema(description = "最小不匹配率", example = "0.1")
    @Min(value = 0, message = "最小不匹配率不能小于0")
    @Max(value = 1, message = "最小不匹配率不能大于1")
    private Double minMismatchRate;

    @Schema(description = "最大不匹配率", example = "0.9")
    @Min(value = 0, message = "最大不匹配率不能小于0")
    @Max(value = 1, message = "最大不匹配率不能大于1")
    private Double maxMismatchRate;

    public RankingQueryRequest() {
    }

    public RankingQueryRequest(String timeRange) {
        this.timeRange = timeRange;
    }

    public RankingQueryRequest(String timeRange, Integer page, Integer size) {
        this.timeRange = timeRange;
        this.page = page;
        this.size = size;
    }

    /**
     * 获取时间范围枚举
     *
     * @return 时间范围枚举
     */
    public TimeRangeEnum getTimeRangeEnum() {
        return TimeRangeEnum.fromCode(timeRange);
    }

    /**
     * 检查是否有关键词筛选
     *
     * @return true如果有关键词筛选
     */
    public boolean hasKeywordFilter() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 检查是否有不匹配率范围筛选
     *
     * @return true如果有不匹配率范围筛选
     */
    public boolean hasMismatchRateFilter() {
        return minMismatchRate != null || maxMismatchRate != null;
    }

    // Getters and Setters
    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Double getMinMismatchRate() {
        return minMismatchRate;
    }

    public void setMinMismatchRate(Double minMismatchRate) {
        this.minMismatchRate = minMismatchRate;
    }

    public Double getMaxMismatchRate() {
        return maxMismatchRate;
    }

    public void setMaxMismatchRate(Double maxMismatchRate) {
        this.maxMismatchRate = maxMismatchRate;
    }

    @Override
    public String toString() {
        return "RankingQueryRequest{" +
                "timeRange='" + timeRange + '\'' +
                ", page=" + page +
                ", size=" + size +
                ", keyword='" + keyword + '\'' +
                ", minMismatchRate=" + minMismatchRate +
                ", maxMismatchRate=" + maxMismatchRate +
                '}';
    }
}