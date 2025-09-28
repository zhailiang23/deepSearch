package com.ynet.mgmt.searchlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 热词统计汇总DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热词统计汇总")
public class HotWordStatistics {

    @Schema(description = "统计时间范围开始")
    private LocalDateTime startDate;

    @Schema(description = "统计时间范围结束")
    private LocalDateTime endDate;

    @Schema(description = "热词列表")
    private List<HotWordResponse> hotWords;

    @Schema(description = "总词汇数量")
    private Integer totalWordsCount;

    @Schema(description = "有效词汇数量(排除停用词)")
    private Integer validWordsCount;

    @Schema(description = "词汇覆盖的搜索查询总数")
    private Long totalSearchQueries;

    @Schema(description = "平均词长")
    private Double averageWordLength;

    @Schema(description = "分词处理的查询总数")
    private Long processedQueriesCount;

    @Schema(description = "分词成功率")
    private Double segmentationSuccessRate;

    @Schema(description = "按词长分布统计")
    private Map<Integer, Long> wordLengthDistribution;

    @Schema(description = "按时间段分布的热词统计")
    private List<HourlyHotWordStatistic> hourlyStatistics;

    @Schema(description = "词性分布统计")
    private Map<String, Long> partOfSpeechDistribution;

    @Schema(description = "最活跃的搜索空间")
    private List<SearchSpaceHotWords> topSearchSpaces;

    /**
     * 按小时热词统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "按小时热词统计")
    public static class HourlyHotWordStatistic {

        @Schema(description = "小时(0-23)")
        private Integer hour;

        @Schema(description = "该时段热词数量")
        private Integer hotWordsCount;

        @Schema(description = "该时段搜索查询数量")
        private Long queriesCount;

        @Schema(description = "该时段最热门的词汇")
        private List<String> topWords;

        @Schema(description = "该时段词汇多样性指数")
        private Double diversityIndex;
    }

    /**
     * 搜索空间热词统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索空间热词统计")
    public static class SearchSpaceHotWords {

        @Schema(description = "搜索空间ID")
        private String searchSpaceId;

        @Schema(description = "搜索空间名称")
        private String searchSpaceName;

        @Schema(description = "该空间的热词数量")
        private Integer hotWordsCount;

        @Schema(description = "该空间的搜索查询数量")
        private Long queriesCount;

        @Schema(description = "该空间最热门的词汇")
        private List<HotWordResponse> topHotWords;

        @Schema(description = "该空间词汇特征性指数")
        private Double uniquenessIndex;
    }
}