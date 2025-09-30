package com.ynet.mgmt.statistics.service.impl;

import com.ynet.mgmt.statistics.dto.MismatchKeywordRankingDTO;
import com.ynet.mgmt.statistics.dto.RankingQueryRequest;
import com.ynet.mgmt.statistics.dto.RankingQueryResponse;
import com.ynet.mgmt.statistics.enums.TimeRangeEnum;
import com.ynet.mgmt.statistics.service.MismatchAnalysisService;
import com.ynet.mgmt.statistics.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排行榜服务实现
 * 基于MismatchAnalysisService的统计结果，实现Top10不匹配关键词的筛选、排序和格式化功能
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class RankingServiceImpl implements RankingService {

    private static final Logger logger = LoggerFactory.getLogger(RankingServiceImpl.class);

    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final String CACHE_NAME = "rankingCache";

    private final MismatchAnalysisService mismatchAnalysisService;

    public RankingServiceImpl(MismatchAnalysisService mismatchAnalysisService) {
        this.mismatchAnalysisService = mismatchAnalysisService;
    }

    @Override
    public RankingQueryResponse getRanking(RankingQueryRequest request) {
        logger.info("获取排行榜数据 - request: {}", request);

        try {
            // 获取分析结果
            List<MismatchKeywordRankingDTO> rankings = mismatchAnalysisService.analyzeMismatchedKeywords(request);

            // 计算总数（这里需要另外查询，因为analyzeMismatchedKeywords已经分页）
            long totalCount = calculateTotalCount(request);

            // 获取统计信息
            RankingQueryResponse.StatisticsInfo statistics =
                mismatchAnalysisService.calculateMismatchStatistics(request.getTimeRangeEnum());

            // 构造响应
            RankingQueryResponse response = new RankingQueryResponse(
                rankings,
                request.getTimeRange(),
                totalCount,
                request.getPage(),
                request.getSize()
            );
            response.setStatistics(statistics);

            logger.info("排行榜数据获取成功 - 返回{}条记录", rankings.size());
            return response;

        } catch (Exception e) {
            logger.error("获取排行榜数据失败", e);
            throw new RuntimeException("获取排行榜数据失败", e);
        }
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'top10_' + #timeRange.code")
    public List<MismatchKeywordRankingDTO> getTop10MismatchedKeywords(TimeRangeEnum timeRange) {
        logger.info("获取Top10不匹配关键词 - timeRange: {}", timeRange);

        try {
            return mismatchAnalysisService.getTopMismatchedKeywords(timeRange, DEFAULT_TOP_LIMIT);
        } catch (Exception e) {
            logger.error("获取Top10不匹配关键词失败", e);
            throw new RuntimeException("获取Top10不匹配关键词失败", e);
        }
    }

    @Override
    public List<MismatchKeywordRankingDTO> getRealtimeRanking(int limit) {
        logger.info("获取实时排行榜 - limit: {}", limit);

        try {
            // 实时计算，不使用缓存
            TimeRangeEnum timeRange = TimeRangeEnum.LAST_7_DAYS; // 默认7天
            return mismatchAnalysisService.getTopMismatchedKeywords(timeRange, limit);
        } catch (Exception e) {
            logger.error("获取实时排行榜失败", e);
            throw new RuntimeException("获取实时排行榜失败", e);
        }
    }

    @Override
    public RankingQueryResponse triggerRealtimeCalculation(TimeRangeEnum timeRange) {
        logger.info("触发实时计算 - timeRange: {}", timeRange);

        try {
            // 清除相关缓存
            mismatchAnalysisService.refreshCache(timeRange);

            // 重新计算
            RankingQueryRequest request = new RankingQueryRequest(timeRange.getCode(), 1, DEFAULT_TOP_LIMIT);
            return getRanking(request);

        } catch (Exception e) {
            logger.error("实时计算失败", e);
            throw new RuntimeException("实时计算失败", e);
        }
    }

    @Override
    public String getCacheStatus(TimeRangeEnum timeRange) {
        logger.info("获取缓存状态 - timeRange: {}", timeRange);

        try {
            // TODO: 实现缓存状态检查
            // 这里需要访问缓存管理器来获取缓存状态
            return "缓存状态正常 - " + timeRange.getDescription();
        } catch (Exception e) {
            logger.error("获取缓存状态失败", e);
            return "缓存状态异常";
        }
    }

    @Override
    public void warmupCache(TimeRangeEnum timeRange) {
        logger.info("预热缓存 - timeRange: {}", timeRange);

        try {
            // 预热Top10缓存
            getTop10MismatchedKeywords(timeRange);

            // 预热分页数据缓存
            for (int page = 1; page <= 5; page++) { // 预热前5页
                RankingQueryRequest request = new RankingQueryRequest(timeRange.getCode(), page, 20);
                getRanking(request);
            }

            logger.info("缓存预热完成 - timeRange: {}", timeRange);

        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
    }

    /**
     * 监听搜索日志新增事件，自动更新排行榜
     * TODO: 实现Spring Events集成
     */
    // @EventListener
    // public void handleSearchLogEvent(SearchLogEvent event) {
    //     logger.info("处理搜索日志事件 - 触发排行榜更新");
    //
    //     try {
    //         // 清除相关缓存
    //         for (TimeRangeEnum timeRange : TimeRangeEnum.values()) {
    //             mismatchAnalysisService.refreshCache(timeRange);
    //         }
    //     } catch (Exception e) {
    //         logger.error("处理搜索日志事件失败", e);
    //     }
    // }

    // ==================== 私有方法 ====================

    /**
     * 计算总记录数
     *
     * @param request 查询请求
     * @return 总记录数
     */
    private long calculateTotalCount(RankingQueryRequest request) {
        try {
            // 创建一个查询所有记录的请求
            RankingQueryRequest countRequest = new RankingQueryRequest(request.getTimeRange(), 1, Integer.MAX_VALUE);
            countRequest.setKeyword(request.getKeyword());
            countRequest.setMinMismatchRate(request.getMinMismatchRate());
            countRequest.setMaxMismatchRate(request.getMaxMismatchRate());

            List<MismatchKeywordRankingDTO> allResults = mismatchAnalysisService.analyzeMismatchedKeywords(countRequest);
            return allResults.size();

        } catch (Exception e) {
            logger.warn("计算总记录数失败，返回默认值", e);
            return 0L;
        }
    }
}