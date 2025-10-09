package com.ynet.mgmt.clustering.service;

import com.ynet.mgmt.clustering.dto.ClusterAnalysisRequest;
import com.ynet.mgmt.clustering.dto.ClusterAnalysisResponse;

/**
 * 聚类分析服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface ClusterAnalysisService {

    /**
     * 执行聚类分析
     *
     * @param request 聚类分析请求
     * @return 聚类分析响应
     */
    ClusterAnalysisResponse analyzeClusters(ClusterAnalysisRequest request);
}
