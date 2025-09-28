package com.ynet.mgmt.statistics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 关键词不匹配度排行榜数据传输对象
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "关键词不匹配度排行榜条目")
public class MismatchKeywordRankingDTO {

    @Schema(description = "排名", example = "1")
    private Integer rank;

    @Schema(description = "关键词", example = "银行贷款")
    private String keyword;

    @Schema(description = "不匹配率", example = "0.75")
    private Double mismatchRate;

    @Schema(description = "搜索次数", example = "1250")
    private Integer searchCount;

    @Schema(description = "不匹配次数", example = "938")
    private Integer mismatchCount;

    @Schema(description = "最后搜索时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSearchTime;

    @Schema(description = "趋势", example = "up", allowableValues = {"up", "down", "stable"})
    private String trend;

    @Schema(description = "排名变化", example = "+2")
    private Integer rankChange;

    public MismatchKeywordRankingDTO() {
    }

    public MismatchKeywordRankingDTO(Integer rank, String keyword, Double mismatchRate,
                                    Integer searchCount, Integer mismatchCount,
                                    LocalDateTime lastSearchTime) {
        this.rank = rank;
        this.keyword = keyword;
        this.mismatchRate = mismatchRate;
        this.searchCount = searchCount;
        this.mismatchCount = mismatchCount;
        this.lastSearchTime = lastSearchTime;
    }

    // Getters and Setters
    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Double getMismatchRate() {
        return mismatchRate;
    }

    public void setMismatchRate(Double mismatchRate) {
        this.mismatchRate = mismatchRate;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(Integer searchCount) {
        this.searchCount = searchCount;
    }

    public Integer getMismatchCount() {
        return mismatchCount;
    }

    public void setMismatchCount(Integer mismatchCount) {
        this.mismatchCount = mismatchCount;
    }

    public LocalDateTime getLastSearchTime() {
        return lastSearchTime;
    }

    public void setLastSearchTime(LocalDateTime lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public Integer getRankChange() {
        return rankChange;
    }

    public void setRankChange(Integer rankChange) {
        this.rankChange = rankChange;
    }

    @Override
    public String toString() {
        return "MismatchKeywordRankingDTO{" +
                "rank=" + rank +
                ", keyword='" + keyword + '\'' +
                ", mismatchRate=" + mismatchRate +
                ", searchCount=" + searchCount +
                ", mismatchCount=" + mismatchCount +
                ", lastSearchTime=" + lastSearchTime +
                ", trend='" + trend + '\'' +
                ", rankChange=" + rankChange +
                '}';
    }
}