package com.ynet.mgmt.searchlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;

/**
 * 热词统计请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "热词统计请求")
public class HotWordRequest {

    @Schema(description = "开始时间", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "结束时间", example = "2024-01-31T23:59:59")
    private LocalDateTime endDate;

    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 100, message = "返回数量不能大于100")
    @Schema(description = "返回热词数量限制", example = "10", minimum = "1", maximum = "100")
    @Builder.Default
    private Integer limit = 10;

    @Schema(description = "用户ID筛选")
    private String userId;

    @Schema(description = "搜索空间ID筛选")
    private String searchSpaceId;

    @Schema(description = "是否包含分词详情")
    @Builder.Default
    private Boolean includeSegmentDetails = false;

    @Min(value = 1, message = "最小词长不能小于1")
    @Max(value = 20, message = "最小词长不能大于20")
    @Schema(description = "最小词长", example = "2", minimum = "1", maximum = "20")
    @Builder.Default
    private Integer minWordLength = 2;

    @Schema(description = "是否排除停用词")
    @Builder.Default
    private Boolean excludeStopWords = true;
}