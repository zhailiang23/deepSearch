package com.ynet.mgmt.clustering.service.impl;

import com.ynet.mgmt.clustering.client.PythonClusterClient;
import com.ynet.mgmt.clustering.dto.ClusterAnalysisRequest;
import com.ynet.mgmt.clustering.dto.ClusterAnalysisResponse;
import com.ynet.mgmt.clustering.exception.ClusterAnalysisException;
import com.ynet.mgmt.clustering.service.ClusterAnalysisService;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聚类分析服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class ClusterAnalysisServiceImpl implements ClusterAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAnalysisServiceImpl.class);

    private final SearchLogRepository searchLogRepository;
    private final PythonClusterClient pythonClusterClient;

    public ClusterAnalysisServiceImpl(SearchLogRepository searchLogRepository,
                                      PythonClusterClient pythonClusterClient) {
        this.searchLogRepository = searchLogRepository;
        this.pythonClusterClient = pythonClusterClient;
    }

    @Override
    public ClusterAnalysisResponse analyzeClusters(ClusterAnalysisRequest request) {
        try {
            // 1. 计算时间范围
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(request.getTimeRange());

            logger.info("开始聚类分析: timeRange={}, startTime={}, endTime={}, eps={}, minSamples={}, metric={}",
                    request.getTimeRange(), startTime, endTime, request.getEps(),
                    request.getMinSamples(), request.getMetric());

            // 2. 从数据库查询成功搜索的查询词
            List<String> queries = searchLogRepository.findSuccessfulSearchQueriesByTimeRange(startTime, endTime);

            if (queries.isEmpty()) {
                logger.warn("指定时间范围内没有搜索记录");
                throw new ClusterAnalysisException("指定时间范围内没有搜索记录");
            }

            // 3. 去重
            List<String> uniqueQueries = queries.stream()
                    .distinct()
                    .collect(Collectors.toList());

            logger.info("查询到 {} 条搜索记录,去重后 {} 条", queries.size(), uniqueQueries.size());

            // 4. 调用 Python 聚类服务
            ClusterAnalysisResponse response = pythonClusterClient.performClustering(
                    uniqueQueries,
                    request.getEps(),
                    request.getMinSamples(),
                    request.getMetric()
            );

            // 5. 设置时间范围描述
            response.setTimeRangeDesc(getTimeRangeDescription(request.getTimeRange()));

            logger.info("聚类分析完成: validClusters={}, noiseCount={}, totalTexts={}",
                    response.getValidClusters(), response.getNoiseCount(), response.getTotalTexts());

            return response;

        } catch (ClusterAnalysisException e) {
            throw e;
        } catch (Exception e) {
            logger.error("聚类分析失败", e);
            throw new ClusterAnalysisException("聚类分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据时间范围代码计算开始时间
     */
    private LocalDateTime calculateStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "7d" -> now.minusDays(7);
            case "30d" -> now.minusDays(30);
            case "90d" -> now.minusDays(90);
            default -> throw new ClusterAnalysisException("无效的时间范围: " + timeRange);
        };
    }

    /**
     * 获取时间范围描述
     */
    private String getTimeRangeDescription(String timeRange) {
        return switch (timeRange) {
            case "7d" -> "最近 7 天";
            case "30d" -> "最近 30 天";
            case "90d" -> "最近 90 天";
            default -> "未知时间范围";
        };
    }
}
