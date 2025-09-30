package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 简化的搜索响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "简化的搜索响应")
public class SimpleSearchResponse {

    @Schema(description = "搜索结果列表")
    private List<SearchResultItem> results;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Integer page;

    @Schema(description = "每页大小")
    private Integer size;

    /**
     * 搜索结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索结果项")
    public static class SearchResultItem {

        @Schema(description = "文档ID")
        private String id;

        @Schema(description = "相关性分数")
        private Double score;

        @Schema(description = "索引名称")
        private String index;

        @Schema(description = "文档源数据")
        private Map<String, Object> source;
    }
}