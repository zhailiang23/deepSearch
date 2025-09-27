package com.ynet.mgmt.searchlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

/**
 * 点击行为记录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "点击行为记录请求")
public class SearchClickRequest {

    @NotNull(message = "搜索日志ID不能为空")
    @Schema(description = "搜索日志ID", required = true)
    private Long searchLogId;

    @NotBlank(message = "文档ID不能为空")
    @Schema(description = "点击的文档ID", required = true)
    private String documentId;

    @Schema(description = "文档标题")
    private String documentTitle;

    @Schema(description = "文档URL")
    private String documentUrl;

    @NotNull(message = "点击位置不能为空")
    @Min(value = 1, message = "点击位置必须大于0")
    @Schema(description = "点击位置(在搜索结果中的排序)", required = true)
    private Integer clickPosition;

    @Schema(description = "用户ID", hidden = true)
    private String userId;

    @Schema(description = "用户IP", hidden = true)
    private String userIp;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "点击类型")
    private String clickType;

    @Schema(description = "修饰键状态")
    private String modifierKeys;
}