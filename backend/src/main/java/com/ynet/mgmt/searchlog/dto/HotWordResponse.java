package com.ynet.mgmt.searchlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 热词统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热词统计响应")
public class HotWordResponse {

    @Schema(description = "热词", example = "人工智能")
    private String word;

    @Schema(description = "出现次数", example = "150")
    private Long count;

    @Schema(description = "出现频率百分比", example = "15.5")
    private Double percentage;

    @Schema(description = "词长", example = "4")
    private Integer wordLength;

    @Schema(description = "关联的搜索查询数量", example = "45")
    private Long relatedQueriesCount;

    @Schema(description = "最近出现时间")
    private java.time.LocalDateTime lastOccurrence;

    @Schema(description = "首次出现时间")
    private java.time.LocalDateTime firstOccurrence;

    @Schema(description = "分词详情")
    private SegmentDetails segmentDetails;

    /**
     * 分词详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分词详情")
    public static class SegmentDetails {

        @Schema(description = "词性", example = "n")
        private String partOfSpeech;

        @Schema(description = "分词权重", example = "0.85")
        private Double segmentWeight;

        @Schema(description = "是否为停用词")
        private Boolean isStopWord;

        @Schema(description = "相关词汇")
        private List<String> relatedWords;

        @Schema(description = "词频密度", example = "0.12")
        private Double frequencyDensity;
    }
}