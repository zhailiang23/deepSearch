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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    // TODO: 注入 ChineseSegmentationService
    // private final ChineseSegmentationService segmentationService;

    public MismatchAnalysisServiceImpl(SearchLogRepository searchLogRepository) {
        this.searchLogRepository = searchLogRepository;
    }

    @Override
    @Cacheable(value = "mismatchStatistics", key = "#timeRange.code")
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
            // 查询时间范围内的搜索日志
            List<SearchLog> searchLogs = searchLogRepository.findByCreatedAtBetween(startTime, endTime);

            if (searchLogs.isEmpty()) {
                return new RankingQueryResponse.StatisticsInfo(0, 0.0, 0.0, 0.0);
            }

            // 按关键词分组并计算不匹配度
            Map<String, List<SearchLog>> keywordGroups = searchLogs.stream()
                    .collect(Collectors.groupingBy(SearchLog::getSearchQuery));

            List<Double> mismatchRates = new ArrayList<>();
            int totalKeywords = keywordGroups.size();

            for (Map.Entry<String, List<SearchLog>> entry : keywordGroups.entrySet()) {
                String keyword = entry.getKey();
                List<SearchLog> logs = entry.getValue();

                double mismatchRate = calculateKeywordMismatchRate(keyword, logs);
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

            return new RankingQueryResponse.StatisticsInfo(totalKeywords, avgMismatchRate, maxMismatchRate, minMismatchRate);

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

            // 查询搜索日志
            List<SearchLog> searchLogs = searchLogRepository.findByCreatedAtBetween(startTime, endTime);

            // 按关键词分组
            Map<String, List<SearchLog>> keywordGroups = searchLogs.stream()
                    .collect(Collectors.groupingBy(SearchLog::getSearchQuery));

            // 计算每个关键词的不匹配度
            List<MismatchKeywordRankingDTO> results = new ArrayList<>();
            int rank = 1;

            for (Map.Entry<String, List<SearchLog>> entry : keywordGroups.entrySet()) {
                String keyword = entry.getKey();
                List<SearchLog> logs = entry.getValue();

                // 关键词筛选
                if (request.hasKeywordFilter() && !keyword.contains(request.getKeyword())) {
                    continue;
                }

                double mismatchRate = calculateKeywordMismatchRate(keyword, logs);

                // 不匹配率范围筛选
                if (request.hasMismatchRateFilter()) {
                    if (request.getMinMismatchRate() != null && mismatchRate < request.getMinMismatchRate()) {
                        continue;
                    }
                    if (request.getMaxMismatchRate() != null && mismatchRate > request.getMaxMismatchRate()) {
                        continue;
                    }
                }

                MismatchKeywordRankingDTO dto = createRankingDTO(keyword, logs, mismatchRate, rank++);
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
            // 查询搜索日志
            List<SearchLog> searchLogs = searchLogRepository.findAllById(searchLogIds);

            // 按关键词分组计算
            Map<String, List<SearchLog>> keywordGroups = searchLogs.stream()
                    .collect(Collectors.groupingBy(SearchLog::getSearchQuery));

            Map<String, Double> mismatchScores = new HashMap<>();
            for (Map.Entry<String, List<SearchLog>> entry : keywordGroups.entrySet()) {
                String keyword = entry.getKey();
                List<SearchLog> logs = entry.getValue();
                double mismatchRate = calculateKeywordMismatchRate(keyword, logs);
                mismatchScores.put(keyword, mismatchRate);
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

            // 查询指定关键词的搜索日志
            List<SearchLog> searchLogs = searchLogRepository.findBySearchQueryAndCreatedAtBetween(
                    keyword, startTime, endTime);

            if (searchLogs.isEmpty()) {
                return null;
            }

            double mismatchRate = calculateKeywordMismatchRate(keyword, searchLogs);
            return createRankingDTO(keyword, searchLogs, mismatchRate, 1);

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
     * 计算关键词不匹配度
     * 分层权重算法：无结果权重1.0，少结果权重0.6，正常结果权重0.0
     *
     * @param keyword 关键词
     * @param searchLogs 搜索日志列表
     * @return 不匹配度（0-1之间）
     */
    private double calculateKeywordMismatchRate(String keyword, List<SearchLog> searchLogs) {
        if (searchLogs.isEmpty()) {
            return 0.0;
        }

        // TODO: 集成分词服务进行关键词标准化
        // String normalizedKeyword = segmentationService.segmentAndNormalize(keyword);

        double totalWeight = 0.0;
        double mismatchWeight = 0.0;

        for (SearchLog log : searchLogs) {
            int resultCount = log.getTotalResults() != null ? log.getTotalResults() : 0;
            double weight = calculateResultWeight(resultCount);
            totalWeight += weight;

            if (isMismatch(resultCount)) {
                mismatchWeight += weight;
            }
        }

        return totalWeight > 0 ? mismatchWeight / totalWeight : 0.0;
    }

    /**
     * 计算结果权重
     *
     * @param resultCount 搜索结果数量
     * @return 权重值
     */
    private double calculateResultWeight(int resultCount) {
        if (resultCount == 0) {
            return NO_RESULT_WEIGHT;
        } else if (resultCount <= LOW_RESULT_THRESHOLD) {
            return LOW_RESULT_WEIGHT;
        } else {
            return NORMAL_RESULT_WEIGHT;
        }
    }

    /**
     * 判断是否为不匹配
     *
     * @param resultCount 搜索结果数量
     * @return true如果是不匹配
     */
    private boolean isMismatch(int resultCount) {
        return resultCount <= LOW_RESULT_THRESHOLD;
    }

    /**
     * 创建排行榜DTO
     *
     * @param keyword 关键词
     * @param searchLogs 搜索日志
     * @param mismatchRate 不匹配率
     * @param rank 排名
     * @return 排行榜DTO
     */
    private MismatchKeywordRankingDTO createRankingDTO(String keyword, List<SearchLog> searchLogs,
                                                      double mismatchRate, int rank) {
        int searchCount = searchLogs.size();
        long mismatchCount = searchLogs.stream()
                .mapToInt(log -> log.getTotalResults() != null ? log.getTotalResults() : 0)
                .filter(count -> count <= LOW_RESULT_THRESHOLD)
                .count();

        LocalDateTime lastSearchTime = searchLogs.stream()
                .map(SearchLog::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        MismatchKeywordRankingDTO dto = new MismatchKeywordRankingDTO(
                rank, keyword, mismatchRate, searchCount, (int) mismatchCount, lastSearchTime);

        // TODO: 计算趋势和排名变化
        dto.setTrend(calculateTrend(mismatchRate));
        dto.setRankChange(0); // 暂时设为0，需要历史数据对比

        return dto;
    }

    /**
     * 计算趋势
     *
     * @param mismatchRate 不匹配率
     * @return 趋势
     */
    private String calculateTrend(double mismatchRate) {
        if (mismatchRate > 0.7) {
            return "up";
        } else if (mismatchRate < 0.3) {
            return "down";
        } else {
            return "stable";
        }
    }
}