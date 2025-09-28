package com.ynet.mgmt.statistics.service;

import com.ynet.mgmt.statistics.dto.RankingQueryRequest;
import com.ynet.mgmt.statistics.dto.RankingQueryResponse;
import com.ynet.mgmt.statistics.dto.MismatchKeywordRankingDTO;
import com.ynet.mgmt.statistics.enums.TimeRangeEnum;

import java.util.List;

/**
 * 排行榜服务接口
 * 基于MismatchAnalysisService的统计结果，实现Top10不匹配关键词的筛选、排序和格式化功能
 *
 * @author system
 * @since 1.0.0
 */
public interface RankingService {

    /**
     * 获取关键词不匹配度排行榜
     *
     * @param request 查询请求参数
     * @return 排行榜响应数据
     */
    RankingQueryResponse getRanking(RankingQueryRequest request);

    /**
     * 获取Top10不匹配关键词
     *
     * @param timeRange 时间范围
     * @return Top10关键词列表
     */
    List<MismatchKeywordRankingDTO> getTop10MismatchedKeywords(TimeRangeEnum timeRange);

    /**
     * 获取实时排行榜
     *
     * @param limit 返回数量限制
     * @return 实时排行榜
     */
    List<MismatchKeywordRankingDTO> getRealtimeRanking(int limit);

    /**
     * 触发实时计算并更新排行榜
     *
     * @param timeRange 时间范围
     * @return 更新后的排行榜
     */
    RankingQueryResponse triggerRealtimeCalculation(TimeRangeEnum timeRange);

    /**
     * 获取排行榜缓存状态
     *
     * @param timeRange 时间范围
     * @return 缓存状态信息
     */
    String getCacheStatus(TimeRangeEnum timeRange);

    /**
     * 预热排行榜缓存
     *
     * @param timeRange 时间范围
     */
    void warmupCache(TimeRangeEnum timeRange);
}