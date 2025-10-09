package com.ynet.mgmt.clustering.dto;

import lombok.Data;
import java.util.List;

/**
 * 聚类分析响应
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ClusterAnalysisResponse {

    /**
     * 聚类话题列表
     */
    private List<ClusterTopicDTO> clusters;

    /**
     * 散点图数据
     */
    private List<ScatterPointDTO> scatterData;

    /**
     * 噪声点数量
     */
    private Integer noiseCount;

    /**
     * 总文本数
     */
    private Integer totalTexts;

    /**
     * 有效簇数量
     */
    private Integer validClusters;

    /**
     * 时间范围描述
     */
    private String timeRangeDesc;
}
