package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 简化的搜索请求
 * 仅用于搜索数据管理页面的基本关键词搜索
 */
@Data
@Schema(description = "简化的搜索请求")
public class SimpleSearchRequest {

    @NotBlank(message = "搜索空间ID不能为空")
    @Schema(description = "搜索空间ID", example = "1", required = true)
    private String searchSpaceId;

    @Schema(description = "搜索关键词", example = "中国银行")
    private String keyword;

    @Schema(description = "页码（从1开始）", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;
}