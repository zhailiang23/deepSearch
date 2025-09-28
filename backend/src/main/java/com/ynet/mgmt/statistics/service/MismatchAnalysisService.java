package com.ynet.mgmt.statistics.service;

import com.ynet.mgmt.statistics.dto.MismatchKeywordRankingDTO;
import com.ynet.mgmt.statistics.dto.RankingQueryRequest;
import com.ynet.mgmt.statistics.dto.RankingQueryResponse;
import com.ynet.mgmt.statistics.enums.TimeRangeEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 搜索关键词不匹配度分析服务接口
 * 提供搜索关键词不匹配度的核心计算逻辑，采用分层权重算法
 *
 * @author system
 * @since 1.0.0
 */
public interface MismatchAnalysisService {

    /**
     * 计算指定时间范围内的搜索关键词不匹配度统计
     *
     * @param timeRange 时间范围枚举
     * @return 不匹配度统计结果
     */
    RankingQueryResponse.StatisticsInfo calculateMismatchStatistics(TimeRangeEnum timeRange);

    /**
     * 计算自定义时间范围内的搜索关键词不匹配度统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 不匹配度统计结果
     */
    RankingQueryResponse.StatisticsInfo calculateMismatchStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取不匹配关键词详细分析结果
     *
     * @param request 查询参数
     * @return 不匹配关键词分析结果列表
     */
    List<MismatchKeywordRankingDTO> analyzeMismatchedKeywords(RankingQueryRequest request);

    /**
     * 批量计算不匹配度，用于大量搜索日志的高效处理
     *
     * @param searchLogIds 搜索日志ID列表
     * @return 关键词到不匹配度分数的映射
     */
    Map<String, Double> batchCalculateMismatchScores(List<Long> searchLogIds);

    /**
     * 获取热门不匹配关键词排行榜
     *
     * @param timeRange 时间范围
     * @param limit 返回数量限制
     * @return 不匹配关键词排行榜
     */
    List<MismatchKeywordRankingDTO> getTopMismatchedKeywords(TimeRangeEnum timeRange, int limit);

    /**
     * 获取关键词详细信息
     *
     * @param keyword 关键词
     * @param timeRange 时间范围
     * @return 关键词详细信息
     */
    MismatchKeywordRankingDTO getKeywordDetail(String keyword, TimeRangeEnum timeRange);

    /**
     * 刷新指定时间范围的缓存
     *
     * @param timeRange 时间范围枚举
     */
    void refreshCache(TimeRangeEnum timeRange);

    /**
     * 清除所有统计缓存
     */
    void clearAllCache();

    /**
     * 检查服务是否可用
     *
     * @return true表示服务可用，false表示服务不可用
     */
    boolean isServiceAvailable();
}