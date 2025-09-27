package com.ynet.mgmt.searchlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 统计分析请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计分析请求")
public class StatisticsRequest {

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间", required = true)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间", required = true)
    private LocalDateTime endTime;

    @Schema(description = "用户ID筛选")
    private String userId;

    @Schema(description = "搜索空间ID筛选")
    private String searchSpaceId;

    @Schema(description = "包含详细统计")
    @Builder.Default
    private Boolean includeDetails = false;

    @Schema(description = "热门查询数量限制")
    @Builder.Default
    private Integer topQueriesLimit = 10;

    @Schema(description = "热门搜索空间数量限制")
    @Builder.Default
    private Integer topSearchSpacesLimit = 10;

    @Schema(description = "热门用户数量限制")
    @Builder.Default
    private Integer topUsersLimit = 10;
}