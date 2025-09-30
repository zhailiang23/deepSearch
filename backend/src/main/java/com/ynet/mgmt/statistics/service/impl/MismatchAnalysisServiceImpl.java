package com.ynet.mgmt.statistics.service.impl;

import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.statistics.dto.MismatchKeywordRankingDTO;
import com.ynet.mgmt.statistics.dto.RankingQueryRequest;
import com.ynet.mgmt.statistics.dto.RankingQueryResponse;
import com.ynet.mgmt.statistics.enums.TimeRangeEnum;
import com.ynet.mgmt.statistics.service.MismatchAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 搜索关键词不匹配度分析服务实现
 * 实现分层权重算法：无结果权重1.0，少结果权重0.6
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class MismatchAnalysisServiceImpl implements MismatchAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(MismatchAnalysisServiceImpl.class);

    /**
     * 分层权重配置
     */
    private static final double NO_RESULT_WEIGHT = 1.0;      // 无结果权重
    private static final double LOW_RESULT_WEIGHT = 0.6;     // 少结果权重
    private static final double NORMAL_RESULT_WEIGHT = 0.0;  // 正常结果权重
    private static final int LOW_RESULT_THRESHOLD = 5;       // 少结果阈值

    private final SearchLogRepository searchLogRepository;
    private final com.ynet.mgmt.searchlog.service.ChineseSegmentationService segmentationService;

    public MismatchAnalysisServiceImpl(SearchLogRepository searchLogRepository,
                                       com.ynet.mgmt.searchlog.service.ChineseSegmentationService segmentationService) {
        this.searchLogRepository = searchLogRepository;
        this.segmentationService = segmentationService;
    }

    @Override
    public RankingQueryResponse.StatisticsInfo calculateMismatchStatistics(TimeRangeEnum timeRange) {
        logger.info("计算不匹配度统计 - timeRange: {}", timeRange);

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(timeRange.getDays());

        return calculateMismatchStatistics(startTime, endTime);
    }

    @Override
    public RankingQueryResponse.StatisticsInfo calculateMismatchStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("计算不匹配度统计 - startTime: {}, endTime: {}", startTime, endTime);

        try {
            // 获取关键词统计数据
            Map<String, KeywordStats> keywordStatsMap = buildKeywordStatistics(startTime, endTime);

            if (keywordStatsMap.isEmpty()) {
                return new RankingQueryResponse.StatisticsInfo(0, 0.0, 0.0, 0.0);
            }

            // 计算每个关键词的不匹配度
            List<Double> mismatchRates = new ArrayList<>();

            for (KeywordStats stats : keywordStatsMap.values()) {
                double mismatchRate = calculateKeywordMismatchRateFromStats(stats);
                mismatchRates.add(mismatchRate);
            }

            // 计算统计指标
            double avgMismatchRate = mismatchRates.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            double maxMismatchRate = mismatchRates.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0.0);

            double minMismatchRate = mismatchRates.stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0.0);

            return new RankingQueryResponse.StatisticsInfo(keywordStatsMap.size(), avgMismatchRate, maxMismatchRate, minMismatchRate);

        } catch (Exception e) {
            logger.error("计算不匹配度统计失败", e);
            throw new RuntimeException("计算不匹配度统计失败", e);
        }
    }

    @Override
    @Cacheable(value = "mismatchKeywords", key = "#request.timeRange + '_' + #request.page + '_' + #request.size")
    public List<MismatchKeywordRankingDTO> analyzeMismatchedKeywords(RankingQueryRequest request) {
        logger.info("分析不匹配关键词 - request: {}", request);

        try {
            TimeRangeEnum timeRange = request.getTimeRangeEnum();
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(timeRange.getDays());

            // 构建关键词统计数据（已分词并聚合）
            Map<String, KeywordStats> keywordStatsMap = buildKeywordStatistics(startTime, endTime);

            // 计算每个关键词的不匹配度并创建 DTO
            List<MismatchKeywordRankingDTO> results = new ArrayList<>();

            for (Map.Entry<String, KeywordStats> entry : keywordStatsMap.entrySet()) {
                String keyword = entry.getKey();
                KeywordStats stats = entry.getValue();

                // 关键词筛选
                if (request.hasKeywordFilter() && !keyword.contains(request.getKeyword())) {
                    continue;
                }

                double mismatchRate = calculateKeywordMismatchRateFromStats(stats);

                // 不匹配率范围筛选
                if (request.hasMismatchRateFilter()) {
                    if (request.getMinMismatchRate() != null && mismatchRate < request.getMinMismatchRate()) {
                        continue;
                    }
                    if (request.getMaxMismatchRate() != null && mismatchRate > request.getMaxMismatchRate()) {
                        continue;
                    }
                }

                MismatchKeywordRankingDTO dto = new MismatchKeywordRankingDTO(
                        0, keyword, mismatchRate, stats.searchCount, stats.mismatchCount, stats.lastSearchTime);
                results.add(dto);
            }

            // 按不匹配度降序排序
            results.sort((a, b) -> Double.compare(b.getMismatchRate(), a.getMismatchRate()));

            // 更新排名
            for (int i = 0; i < results.size(); i++) {
                results.get(i).setRank(i + 1);
            }

            // 分页处理
            int start = (request.getPage() - 1) * request.getSize();
            int end = Math.min(start + request.getSize(), results.size());

            if (start >= results.size()) {
                return new ArrayList<>();
            }

            return results.subList(start, end);

        } catch (Exception e) {
            logger.error("分析不匹配关键词失败", e);
            throw new RuntimeException("分析不匹配关键词失败", e);
        }
    }

    @Override
    public Map<String, Double> batchCalculateMismatchScores(List<Long> searchLogIds) {
        logger.info("批量计算不匹配度分数 - searchLogIds size: {}", searchLogIds.size());

        try {
            // 查询搜索日志（只查询需要的字段）
            List<SearchLog> searchLogs = searchLogRepository.findAllById(searchLogIds);

            // 按关键词聚合统计
            Map<String, KeywordStats> keywordStatsMap = new HashMap<>();

            for (SearchLog log : searchLogs) {
                String searchQuery = log.getSearchQuery();
                if (searchQuery == null || searchQuery.isBlank()) {
                    continue;
                }

                // 对查询字符串进行分词
                List<String> keywords = segmentationService.segmentText(searchQuery);

                for (String keyword : keywords) {
                    if (keyword == null || keyword.isBlank() || keyword.length() < 2) {
                        continue;
                    }

                    KeywordStats stats = keywordStatsMap.computeIfAbsent(keyword, k -> new KeywordStats());
                    stats.searchCount++;

                    int resultCount = (log.getTotalResults() != null) ? log.getTotalResults().intValue() : 0;
                    if (resultCount == 0) {
                        stats.noResultCount++;
                    } else if (resultCount <= LOW_RESULT_THRESHOLD) {
                        stats.lowResultCount++;
                    }
                }
            }

            // 计算每个关键词的不匹配度
            Map<String, Double> mismatchScores = new HashMap<>();
            for (Map.Entry<String, KeywordStats> entry : keywordStatsMap.entrySet()) {
                double mismatchRate = calculateKeywordMismatchRateFromStats(entry.getValue());
                mismatchScores.put(entry.getKey(), mismatchRate);
            }

            return mismatchScores;

        } catch (Exception e) {
            logger.error("批量计算不匹配度分数失败", e);
            throw new RuntimeException("批量计算不匹配度分数失败", e);
        }
    }

    @Override
    @Cacheable(value = "topMismatchKeywords", key = "#timeRange.code + '_' + #limit")
    public List<MismatchKeywordRankingDTO> getTopMismatchedKeywords(TimeRangeEnum timeRange, int limit) {
        logger.info("获取Top不匹配关键词 - timeRange: {}, limit: {}", timeRange, limit);

        RankingQueryRequest request = new RankingQueryRequest(timeRange.getCode(), 1, limit);
        return analyzeMismatchedKeywords(request);
    }

    @Override
    @Cacheable(value = "keywordDetail", key = "#keyword + '_' + #timeRange.code")
    public MismatchKeywordRankingDTO getKeywordDetail(String keyword, TimeRangeEnum timeRange) {
        logger.info("获取关键词详细信息 - keyword: {}, timeRange: {}", keyword, timeRange);

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(timeRange.getDays());

            // 构建关键词统计数据
            Map<String, KeywordStats> keywordStatsMap = buildKeywordStatistics(startTime, endTime);

            // 从结果中找到指定关键词
            KeywordStats stats = keywordStatsMap.get(keyword);
            if (stats == null) {
                return null;
            }

            double mismatchRate = calculateKeywordMismatchRateFromStats(stats);

            MismatchKeywordRankingDTO dto = new MismatchKeywordRankingDTO(
                    1, keyword, mismatchRate, stats.searchCount, stats.mismatchCount, stats.lastSearchTime);
            return dto;

        } catch (Exception e) {
            logger.error("获取关键词详细信息失败 - keyword: {}", keyword, e);
            throw new RuntimeException("获取关键词详细信息失败", e);
        }
    }

    @Override
    @CacheEvict(value = {"mismatchStatistics", "mismatchKeywords", "topMismatchKeywords", "keywordDetail"},
                condition = "#timeRange != null")
    public void refreshCache(TimeRangeEnum timeRange) {
        logger.info("刷新缓存 - timeRange: {}", timeRange);
    }

    @Override
    @CacheEvict(value = {"mismatchStatistics", "mismatchKeywords", "topMismatchKeywords", "keywordDetail"},
                allEntries = true)
    public void clearAllCache() {
        logger.info("清除所有缓存");
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // 简单的健康检查：查询数据库连接是否正常
            searchLogRepository.count();
            return true;
        } catch (Exception e) {
            logger.error("服务不可用", e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 构建关键词统计数据（核心方法）
     * 1. 查询时间范围内的所有搜索日志
     * 2. 对每条日志的 searchQuery 进行分词
     * 3. 按关键词聚合统计数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 关键词统计数据 Map
     */
    private Map<String, KeywordStats> buildKeywordStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        // 查询搜索日志（只查询 searchQuery 和 totalResults，不查询 response_data）
        List<Object[]> queryResults = searchLogRepository.findQueryAndResultsInTimeRange(startTime, endTime);

        logger.info("查询到 {} 条搜索日志", queryResults.size());

        // 按关键词聚合统计
        Map<String, KeywordStats> keywordStatsMap = new HashMap<>();

        for (Object[] row : queryResults) {
            String searchQuery = (String) row[0];
            Long totalResults = (Long) row[1];

            if (searchQuery == null || searchQuery.isBlank()) {
                continue;
            }

            // 对查询字符串进行分词
            List<String> keywords = segmentationService.segmentText(searchQuery);

            // 统计每个关键词
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank() || keyword.length() < 2) {
                    // 过滤掉单字或空白关键词
                    continue;
                }

                KeywordStats stats = keywordStatsMap.computeIfAbsent(keyword, k -> new KeywordStats());
                stats.searchCount++;

                // 根据结果数判断是否为不匹配
                int resultCount = (totalResults != null) ? totalResults.intValue() : 0;
                if (resultCount == 0) {
                    stats.noResultCount++;
                } else if (resultCount <= LOW_RESULT_THRESHOLD) {
                    stats.lowResultCount++;
                }

                stats.lastSearchTime = LocalDateTime.now(); // 简化处理，实际应该从日志获取
            }
        }

        logger.info("统计到 {} 个唯一关键词", keywordStatsMap.size());

        return keywordStatsMap;
    }

    /**
     * 根据关键词统计数据计算不匹配率
     * 使用分层权重算法：
     * - 无结果（0条）：权重 1.0（完全不匹配）
     * - 少结果（1-5条）：权重 0.6（部分不匹配）
     * - 正常结果（>5条）：权重 0.0（匹配良好）
     *
     * 不匹配率 = 加权不匹配分数 / 总出现次数
     *
     * @param stats 关键词统计数据
     * @return 不匹配率（0-1之间）
     */
    private double calculateKeywordMismatchRateFromStats(KeywordStats stats) {
        if (stats.searchCount == 0) {
            return 0.0;
        }

        // 计算加权不匹配分数
        // 无结果按1.0计算，少结果按0.6计算
        double weightedMismatchScore = stats.noResultCount * NO_RESULT_WEIGHT +
                                      stats.lowResultCount * LOW_RESULT_WEIGHT;

        // 不匹配率 = 加权不匹配分数 / 总出现次数
        // 例如：出现56次，其中1次无结果 -> 1.0 / 56 = 1.8%
        // 例如：出现3次，其中2次少结果 -> 1.2 / 3 = 40%
        double mismatchRate = weightedMismatchScore / stats.searchCount;

        stats.mismatchCount = stats.noResultCount + stats.lowResultCount;

        return mismatchRate;
    }

    /**
     * 关键词统计数据内部类
     */
    private static class KeywordStats {
        int searchCount = 0;          // 搜索次数
        int noResultCount = 0;        // 无结果次数
        int lowResultCount = 0;       // 少结果次数
        int mismatchCount = 0;        // 不匹配次数（无结果+少结果）
        LocalDateTime lastSearchTime; // 最后搜索时间
    }
}