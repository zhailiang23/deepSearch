package com.ynet.mgmt.clustering.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 聚类分析请求
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ClusterAnalysisRequest {

    /**
     * 时间范围代码 (7d, 30d, 90d)
     */
    @NotBlank(message = "时间范围不能为空")
    @Pattern(regexp = "^(7d|30d|90d)$", message = "时间范围必须是 7d、30d 或 90d")
    private String timeRange;

    /**
     * DBSCAN eps 参数
     */
    @NotNull(message = "eps 参数不能为空")
    @DecimalMin(value = "0.1", message = "eps 必须大于等于 0.1")
    @DecimalMax(value = "1.0", message = "eps 必须小于等于 1.0")
    private Double eps = 0.4;

    /**
     * DBSCAN min_samples 参数
     */
    @NotNull(message = "min_samples 参数不能为空")
    @Min(value = 2, message = "min_samples 必须大于等于 2")
    @Max(value = 10, message = "min_samples 必须小于等于 10")
    private Integer minSamples = 3;

    /**
     * 距离度量方式
     */
    @NotBlank(message = "距离度量方式不能为空")
    @Pattern(regexp = "^(cosine|euclidean)$", message = "距离度量方式必须是 cosine 或 euclidean")
    private String metric = "cosine";
}
