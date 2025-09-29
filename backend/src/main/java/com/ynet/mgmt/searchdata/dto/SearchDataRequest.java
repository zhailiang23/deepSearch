package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 搜索数据请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "搜索数据请求参数")
public class SearchDataRequest {

    @Schema(description = "搜索空间ID", required = true, example = "1")
    @NotBlank(message = "搜索空间ID不能为空")
    private String searchSpaceId;

    @Schema(description = "搜索查询语句", example = "user AND active:true")
    private String query;

    @Schema(description = "页码", example = "1", minimum = "1")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20", minimum = "1")
    @Min(value = 1, message = "每页大小必须大于等于1")
    private Integer size = 20;

    @Schema(description = "排序配置")
    private SortConfig sort;

    @Schema(description = "过滤条件")
    private List<FilterConfig> filters;

    @Schema(description = "启用拼音搜索", example = "true", defaultValue = "true")
    private Boolean enablePinyinSearch = true;

    @Schema(description = "拼音搜索模式", example = "AUTO",
            allowableValues = {"AUTO", "STRICT", "FUZZY"},
            defaultValue = "AUTO")
    private String pinyinMode = "AUTO";

    @Schema(description = "启用语义搜索", example = "true", defaultValue = "true")
    private Boolean enableSemanticSearch = true;

    @Schema(description = "语义搜索模式", example = "AUTO",
            allowableValues = {"AUTO", "KEYWORD_FIRST", "SEMANTIC_FIRST", "HYBRID", "KEYWORD_ONLY", "SEMANTIC_ONLY"},
            defaultValue = "AUTO")
    private String semanticMode = "AUTO";

    @Schema(description = "语义搜索权重", example = "0.3",
            minimum = "0.0", maximum = "1.0",
            defaultValue = "0.3")
    private Double semanticWeight;

    /**
     * 排序配置
     */
    @Data
    @Schema(description = "排序配置")
    public static class SortConfig {

        @Schema(description = "排序字段", required = true, example = "created_at")
        @NotBlank(message = "排序字段不能为空")
        private String field;

        @Schema(description = "排序顺序", required = true, example = "desc", allowableValues = {"asc", "desc"})
        @NotBlank(message = "排序顺序不能为空")
        private String order;
    }

    /**
     * 过滤配置
     */
    @Data
    @Schema(description = "过滤配置")
    public static class FilterConfig {

        @Schema(description = "过滤字段", required = true, example = "status")
        @NotBlank(message = "过滤字段不能为空")
        private String field;

        @Schema(description = "过滤值", required = true, example = "active")
        private Object value;

        @Schema(description = "操作符", required = true, example = "eq",
                allowableValues = {"eq", "contains", "startsWith", "endsWith", "range", "in"})
        @NotBlank(message = "操作符不能为空")
        private String operator;
    }
}