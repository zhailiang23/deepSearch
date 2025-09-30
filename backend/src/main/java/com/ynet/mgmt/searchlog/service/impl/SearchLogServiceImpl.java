package com.ynet.mgmt.searchlog.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.exception.BusinessException;
import com.ynet.mgmt.config.CacheConfig;
import com.ynet.mgmt.searchlog.dto.*;
import com.ynet.mgmt.searchlog.entity.SearchClickLog;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.repository.SearchClickLogRepository;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.service.ChineseSegmentationService;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 搜索日志服务实现类
 * 提供搜索日志的完整业务逻辑实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SearchLogServiceImpl implements SearchLogService {

    private final SearchLogRepository searchLogRepository;
    private final SearchClickLogRepository searchClickLogRepository;
    private final ChineseSegmentationService segmentationService;
    private final ObjectMapper objectMapper;

    @Override
    @CacheEvict(value = {CacheConfig.STATISTICS_CACHE, CacheConfig.HOT_WORDS_CACHE}, allEntries = true)
    public SearchLog saveSearchLog(SearchLog searchLog) {
        try {
            // 设置默认值
            if (searchLog.getStatus() == null) {
                searchLog.setStatus(SearchLogStatus.SUCCESS);
            }
            if (searchLog.getTotalTimeMs() == null) {
                searchLog.setTotalTimeMs(0L);
            }

            // 保存日志
            SearchLog saved = searchLogRepository.save(searchLog);
            log.debug("保存搜索日志成功: id={}, userId={}, query={}",
                    saved.getId(), saved.getUserId(), saved.getSearchQuery());

            return saved;

        } catch (Exception e) {
            log.error("保存搜索日志失败: userId={}, query={}",
                    searchLog.getUserId(), searchLog.getSearchQuery(), e);
            throw new BusinessException("SEARCH_LOG_SAVE_ERROR", "保存搜索日志失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchLog> getSearchLogs(SearchLogQueryRequest request, Pageable pageable) {
        try {
            // 构建查询条件
            Specification<SearchLog> spec = buildSearchLogSpecification(request);

            // 执行分页查询
            Page<SearchLog> result = searchLogRepository.findAll(spec, pageable);

            result.forEach(log -> log.getClickLogs().iterator());

            log.debug("查询搜索日志: page={}, size={}, total={}",
                    pageable.getPageNumber(), pageable.getPageSize(), result.getTotalElements());

            return result;

        } catch (Exception e) {
            log.error("查询搜索日志失败: request={}", request, e);
            throw new BusinessException("SEARCH_LOG_QUERY_ERROR", "查询搜索日志失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchLogDetailResponse getSearchLogDetail(Long id) {
        try {
            // 查找搜索日志
            SearchLog searchLog = searchLogRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("搜索日志不存在: " + id));

            // 构建详情响应
            SearchLogDetailResponse response = SearchLogDetailResponse.builder()
                    .id(searchLog.getId())
                    .userId(searchLog.getUserId() != null ? searchLog.getUserId().toString() : null)
                    .userIp(maskIp(searchLog.getIpAddress()))
                    .searchSpaceId(searchLog.getSearchSpaceId() != null ? searchLog.getSearchSpaceId().toString() : null)
                    .searchSpaceCode(searchLog.getSearchSpaceCode())
                    .query(searchLog.getSearchQuery())
                    .resultCount(searchLog.getTotalResults() != null ? searchLog.getTotalResults().intValue() : 0)
                    .responseTime(searchLog.getTotalTimeMs() != null ? searchLog.getTotalTimeMs().intValue() : 0)
                    .status(searchLog.getStatus())
                    .createdAt(searchLog.getCreatedAt())
                    .sessionId(searchLog.getSessionId())
                    .traceId(searchLog.getTraceId())
                    .requestParams(searchLog.getRequestParams())
                    .responseData(searchLog.getResponseData())
                    .errorMessage(searchLog.getErrorMessage())
                    .userAgent(searchLog.getUserAgent())
                    .build();

            // 添加点击记录信息
            if (searchLog.getClickLogs() != null && !searchLog.getClickLogs().isEmpty()) {
                List<SearchLogDetailResponse.ClickLogInfo> clickLogs = searchLog.getClickLogs().stream()
                        .map(this::convertToClickLogInfo)
                        .collect(Collectors.toList());
                response.setClickLogs(clickLogs);
            }

            log.debug("获取搜索日志详情成功: id={}", id);
            return response;

        } catch (EntityNotFoundException e) {
            log.warn("搜索日志不存在: id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("获取搜索日志详情失败: id={}", id, e);
            throw new BusinessException("SEARCH_LOG_DETAIL_ERROR", "获取搜索日志详情失败: " + e.getMessage());
        }
    }

    @Override
    public void recordClickAction(SearchClickRequest request) {
        try {
            // 验证搜索日志是否存在
            SearchLog searchLog = searchLogRepository.findById(request.getSearchLogId())
                    .orElseThrow(() -> new EntityNotFoundException("搜索日志不存在: " + request.getSearchLogId()));

            // 创建点击记录
            SearchClickLog clickLog = new SearchClickLog();
            clickLog.setSearchLog(searchLog);
            clickLog.setDocumentId(request.getDocumentId());
            clickLog.setDocumentTitle(request.getDocumentTitle());
            clickLog.setClickPosition(request.getClickPosition());
            clickLog.setClickTime(LocalDateTime.now());

            // 设置用户信息
            if (request.getUserId() != null) {
                try {
                    clickLog.setUserId(Long.valueOf(request.getUserId()));
                } catch (NumberFormatException e) {
                    log.debug("用户ID格式错误: {}", request.getUserId());
                }
            }
            clickLog.setIpAddress(request.getUserIp());

            // 自动计算点击顺序
            int clickSequence = (int) (searchClickLogRepository.countBySearchLogId(searchLog.getId()) + 1);
            clickLog.setClickSequence(clickSequence);

            // 保存点击记录
            searchClickLogRepository.save(clickLog);

            log.debug("记录点击行为成功: searchLogId={}, documentId={}, position={}",
                    request.getSearchLogId(), request.getDocumentId(), request.getClickPosition());

        } catch (EntityNotFoundException e) {
            log.warn("记录点击行为失败 - 搜索日志不存在: {}", request.getSearchLogId());
            throw e;
        } catch (Exception e) {
            log.error("记录点击行为失败: request={}", request, e);
            throw new BusinessException("SEARCH_CLICK_RECORD_ERROR", "记录点击行为失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.STATISTICS_CACHE, keyGenerator = "customKeyGenerator")
    public SearchLogStatistics getSearchStatistics(StatisticsRequest request) {
        try {
            LocalDateTime startTime = request.getStartTime();
            LocalDateTime endTime = request.getEndTime();

            log.debug("开始获取搜索统计数据，时间范围: {} - {}", startTime, endTime);

            // 使用批量查询优化，减少数据库交互次数
            Object[] batchStats = searchLogRepository.findBatchStatistics(startTime, endTime);

            long totalSearches = ((Number) batchStats[0]).longValue();
            long successfulSearches = ((Number) batchStats[1]).longValue();
            long failedSearches = totalSearches - successfulSearches;
            double successRate = totalSearches > 0 ? (double) successfulSearches / totalSearches : 0.0;
            Double avgResponseTime = batchStats[2] != null ? ((Number) batchStats[2]).doubleValue() : 0.0;

            // 热门查询关键词（并行查询）
            List<Object[]> topQueriesData = searchLogRepository.findTopQueriesByPeriod(
                    startTime, endTime, PageRequest.of(0, request.getTopQueriesLimit()));
            List<SearchLogStatistics.QueryStatistic> topQueries = convertToQueryStatistics(topQueriesData);

            // 热门搜索空间（并行查询）
            List<Object[]> topSearchSpacesData = searchLogRepository.findTopSearchSpacesByPeriod(
                    startTime, endTime, PageRequest.of(0, request.getTopSearchSpacesLimit()));
            List<SearchLogStatistics.SearchSpaceStatistic> topSearchSpaces = convertToSearchSpaceStatistics(topSearchSpacesData);

            // 构建统计结果
            SearchLogStatistics statistics = SearchLogStatistics.builder()
                    .totalSearches(totalSearches)
                    .successfulSearches(successfulSearches)
                    .failedSearches(failedSearches)
                    .successRate(successRate)
                    .averageResponseTime(avgResponseTime)
                    .topQueries(topQueries)
                    .topSearchSpaces(topSearchSpaces)
                    .build();

            log.debug("获取搜索统计成功: 总搜索={}, 成功率={:.2f}%, 平均响应时间={:.2f}ms",
                    totalSearches, successRate * 100, avgResponseTime);
            return statistics;

        } catch (Exception e) {
            log.error("获取搜索统计失败: request={}", request, e);
            throw new BusinessException("SEARCH_STATISTICS_ERROR", "获取搜索统计失败: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(value = {CacheConfig.STATISTICS_CACHE, CacheConfig.HOT_WORDS_CACHE}, allEntries = true)
    public long cleanupExpiredLogs(int retentionDays) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);

            // 先删除关联的点击记录
            long deletedClicks = searchClickLogRepository.deleteBySearchLogCreatedAtBefore(cutoffTime);

            // 再删除搜索日志
            long deletedLogs = searchLogRepository.deleteByCreatedAtBefore(cutoffTime);

            log.info("清理过期日志完成: 删除搜索日志{}条, 点击记录{}条", deletedLogs, deletedClicks);
            return deletedLogs;

        } catch (Exception e) {
            log.error("清理过期日志失败: retentionDays={}", retentionDays, e);
            throw new BusinessException("SEARCH_LOG_CLEANUP_ERROR", "清理过期日志失败: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(value = {CacheConfig.STATISTICS_CACHE, CacheConfig.HOT_WORDS_CACHE}, allEntries = true)
    public long batchDeleteLogs(List<Long> logIds) {
        try {
            if (logIds == null || logIds.isEmpty()) {
                return 0;
            }

            // 先删除关联的点击记录
            long deletedClicks = searchClickLogRepository.deleteBySearchLogIdIn(logIds);

            // 再删除搜索日志
            searchLogRepository.deleteAllById(logIds);
            long deletedLogs = logIds.size();

            log.info("批量删除日志完成: 删除搜索日志{}条, 点击记录{}条", deletedLogs, deletedClicks);
            return deletedLogs;

        } catch (Exception e) {
            log.error("批量删除日志失败: logIds={}", logIds, e);
            throw new BusinessException("SEARCH_LOG_BATCH_DELETE_ERROR", "批量删除日志失败: " + e.getMessage());
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 构建搜索日志查询条件
     */
    private Specification<SearchLog> buildSearchLogSpecification(SearchLogQueryRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 用户ID筛选
            if (StringUtils.hasText(request.getUserId())) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), request.getUserId()));
            }

            // 搜索空间ID筛选
            if (StringUtils.hasText(request.getSearchSpaceId())) {
                try {
                    Long searchSpaceId = Long.valueOf(request.getSearchSpaceId());
                    predicates.add(criteriaBuilder.equal(root.get("searchSpaceId"), searchSpaceId));
                } catch (NumberFormatException e) {
                    log.warn("无效的搜索空间ID格式: {}", request.getSearchSpaceId());
                    // 跳过无效的搜索空间ID筛选
                }
            }

            // 查询关键词筛选
            if (StringUtils.hasText(request.getQuery())) {
                // 现在searchQuery是普通字符串字段，可以直接使用LIKE查询
                predicates.add(criteriaBuilder.like(
                        root.get("searchQuery"),
                        "%" + request.getQuery() + "%"));
            }

            // 状态筛选
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            // 时间范围筛选
            if (request.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), request.getStartTime()));
            }

            if (request.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), request.getEndTime()));
            }

            // 响应时间范围筛选
            if (request.getMinResponseTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("totalTimeMs"), request.getMinResponseTime()));
            }

            if (request.getMaxResponseTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("totalTimeMs"), request.getMaxResponseTime()));
            }

            // 用户IP筛选
            if (StringUtils.hasText(request.getUserIp())) {
                predicates.add(criteriaBuilder.like(root.get("ipAddress"), request.getUserIp() + "%"));
            }

            // 会话ID筛选
            if (StringUtils.hasText(request.getSessionId())) {
                predicates.add(criteriaBuilder.equal(root.get("sessionId"), request.getSessionId()));
            }

            // 搜索空间代码筛选
            if (StringUtils.hasText(request.getSearchSpaceCode())) {
                predicates.add(criteriaBuilder.equal(root.get("searchSpaceCode"), request.getSearchSpaceCode()));
            }

            // 链路追踪ID筛选
            if (StringUtils.hasText(request.getTraceId())) {
                predicates.add(criteriaBuilder.equal(root.get("traceId"), request.getTraceId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * IP地址脱敏处理
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        if (ip.contains(".")) {
            // IPv4: 192.168.1.1 -> 192.168.*.1
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*." + parts[3];
            }
        }

        return ip; // 保持原样或其他脱敏策略
    }

    /**
     * 转换点击记录信息
     */
    private SearchLogDetailResponse.ClickLogInfo convertToClickLogInfo(SearchClickLog clickLog) {
        return SearchLogDetailResponse.ClickLogInfo.builder()
                .id(clickLog.getId())
                .documentId(clickLog.getDocumentId())
                .documentTitle(clickLog.getDocumentTitle())
                .documentUrl("") // SearchClickLog实体中没有URL字段，使用空字符串
                .clickPosition(clickLog.getClickPosition())
                .clickTime(clickLog.getClickTime())
                .clickType("") // SearchClickLog实体中没有Type字段，使用空字符串
                .build();
    }

    /**
     * 转换查询统计数据
     */
    private List<SearchLogStatistics.QueryStatistic> convertToQueryStatistics(List<Object[]> data) {
        return data.stream()
                .map(row -> SearchLogStatistics.QueryStatistic.builder()
                        .query((String) row[0])
                        .searchCount((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 转换搜索空间统计数据
     */
    private List<SearchLogStatistics.SearchSpaceStatistic> convertToSearchSpaceStatistics(List<Object[]> data) {
        return data.stream()
                .map(row -> SearchLogStatistics.SearchSpaceStatistic.builder()
                        .searchSpaceId((String) row[0])
                        .searchSpaceName((String) row[1])
                        .searchCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.HOT_WORDS_CACHE, keyGenerator = "customKeyGenerator")
    public List<HotWordResponse> getHotWords(HotWordRequest request) {
        try {
            log.debug("开始获取热词统计: request={}", request);

            // 设置默认时间范围
            LocalDateTime startTime = request.getStartDate();
            LocalDateTime endTime = request.getEndDate();
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }

            // 性能优化：首先尝试使用数据库聚合查询（适用于简单场景）
            if (request.getMinWordLength() <= 2 && !request.getExcludeStopWords()) {
                return getHotWordsFromDatabaseAggregation(request, startTime, endTime);
            }

            // 复杂场景：使用分页加载和内存处理
            return getHotWordsWithPaginatedProcessing(request, startTime, endTime);

        } catch (Exception e) {
            log.error("获取热词统计失败: request={}", request, e);
            throw new BusinessException("HOT_WORD_STATISTICS_ERROR", "获取热词统计失败: " + e.getMessage());
        }
    }

    /**
     * 使用数据库聚合查询获取热词（性能最优）
     */
    private List<HotWordResponse> getHotWordsFromDatabaseAggregation(HotWordRequest request,
                                                                   LocalDateTime startTime,
                                                                   LocalDateTime endTime) {
        log.debug("使用数据库聚合查询获取热词");

        List<Object[]> dbResults = searchLogRepository.findHotQueriesOptimized(
                startTime, endTime, PageRequest.of(0, request.getLimit() * 2)); // 多取一些用于后续过滤

        return dbResults.stream()
                .map(row -> {
                    String query = (String) row[0];
                    Long count = ((Number) row[1]).longValue();
                    LocalDateTime firstOccurrence = (LocalDateTime) row[2];
                    LocalDateTime lastOccurrence = (LocalDateTime) row[3];

                    return HotWordResponse.builder()
                            .word(query)
                            .count(count)
                            .percentage(calculatePercentage(count, count)) // 简化计算
                            .wordLength(query.length())
                            .relatedQueriesCount(1L) // 简化计算
                            .firstOccurrence(firstOccurrence)
                            .lastOccurrence(lastOccurrence)
                            .build();
                })
                .filter(response -> response.getWord().length() >= request.getMinWordLength())
                .limit(request.getLimit())
                .collect(Collectors.toList());
    }

    /**
     * 使用分页处理获取热词（支持复杂过滤）
     */
    private List<HotWordResponse> getHotWordsWithPaginatedProcessing(HotWordRequest request,
                                                                   LocalDateTime startTime,
                                                                   LocalDateTime endTime) {
        log.debug("使用分页处理获取热词");

        Map<String, WordStatistics> wordStatsMap = new HashMap<>();
        int pageSize = 1000; // 每页处理1000条记录
        int pageNumber = 0;
        Page<String> queryPage;

        // 分页加载搜索查询
        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            if (StringUtils.hasText(request.getUserId()) || StringUtils.hasText(request.getSearchSpaceId())) {
                // 带过滤条件的查询
                Long userId = StringUtils.hasText(request.getUserId()) ?
                        Long.valueOf(request.getUserId()) : null;
                Long searchSpaceId = StringUtils.hasText(request.getSearchSpaceId()) ?
                        Long.valueOf(request.getSearchSpaceId()) : null;

                queryPage = searchLogRepository.findSearchQueriesForHotWordsWithFilter(
                        startTime, endTime, userId, searchSpaceId, pageable);
            } else {
                // 普通查询
                queryPage = searchLogRepository.findSearchQueriesForHotWords(
                        startTime, endTime, pageable);
            }

            if (!queryPage.getContent().isEmpty()) {
                // 批量分词处理当前页
                List<String> queries = queryPage.getContent();
                List<List<String>> segmentedQueries = segmentationService.segmentTexts(queries);

                // 统计当前页的词频
                for (int i = 0; i < segmentedQueries.size(); i++) {
                    List<String> words = segmentedQueries.get(i);
                    String originalQuery = queries.get(i);

                    for (String word : words) {
                        if (isValidWord(word, request)) {
                            WordStatistics stats = wordStatsMap.computeIfAbsent(word, k -> new WordStatistics(k));
                            stats.incrementCount();
                            stats.addRelatedQuery(originalQuery);
                            stats.updateOccurrenceTimes(LocalDateTime.now()); // 简化时间处理
                        }
                    }
                }
            }

            pageNumber++;
            log.debug("处理第{}页，本页{}条记录，累计词汇{}个", pageNumber, queryPage.getContent().size(), wordStatsMap.size());

        } while (queryPage.hasNext() && pageNumber < 100); // 限制最大页数防止内存溢出

        // 转换为响应对象并排序
        List<HotWordResponse> hotWords = wordStatsMap.values().stream()
                .map(stats -> buildHotWordResponse(stats, wordStatsMap.values().size()))
                .sorted((w1, w2) -> Long.compare(w2.getCount(), w1.getCount()))
                .limit(request.getLimit())
                .collect(Collectors.toList());

        log.debug("分页热词统计完成，返回{}个热词", hotWords.size());
        return hotWords;
    }

    // ========== 热词统计私有辅助方法 ==========

    /**
     * 构建热词查询条件
     */
    private Specification<SearchLog> buildHotWordSpecification(HotWordRequest request,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 时间范围筛选
            predicates.add(criteriaBuilder.between(root.get("createdAt"), startTime, endTime));

            // 只统计成功的搜索
            predicates.add(criteriaBuilder.equal(root.get("status"), SearchLogStatus.SUCCESS));

            // 用户ID筛选
            if (StringUtils.hasText(request.getUserId())) {
                try {
                    Long userId = Long.valueOf(request.getUserId());
                    predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID格式: {}", request.getUserId());
                }
            }

            // 搜索空间ID筛选
            if (StringUtils.hasText(request.getSearchSpaceId())) {
                try {
                    Long searchSpaceId = Long.valueOf(request.getSearchSpaceId());
                    predicates.add(criteriaBuilder.equal(root.get("searchSpaceId"), searchSpaceId));
                } catch (NumberFormatException e) {
                    log.warn("无效的搜索空间ID格式: {}", request.getSearchSpaceId());
                }
            }

            // 排除空查询
            predicates.add(criteriaBuilder.isNotNull(root.get("searchQuery")));
            predicates.add(criteriaBuilder.notEqual(root.get("searchQuery"), ""));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 计算词汇统计信息
     */
    private Map<String, WordStatistics> calculateWordStatistics(List<List<String>> segmentedQueries,
                                                               List<SearchLog> searchLogs,
                                                               HotWordRequest request) {
        Map<String, WordStatistics> wordStatsMap = new HashMap<>();

        // 建立查询索引映射
        Map<String, SearchLog> queryToLogMap = searchLogs.stream()
                .filter(log -> log.getSearchQuery() != null)
                .collect(Collectors.toMap(
                        SearchLog::getSearchQuery,
                        Function.identity(),
                        (existing, replacement) -> existing));

        for (int i = 0; i < segmentedQueries.size() && i < searchLogs.size(); i++) {
            List<String> words = segmentedQueries.get(i);
            SearchLog searchLog = searchLogs.get(i);

            for (String word : words) {
                if (isValidWord(word, request)) {
                    WordStatistics stats = wordStatsMap.computeIfAbsent(word, k -> new WordStatistics(k));
                    stats.incrementCount();
                    stats.addRelatedQuery(searchLog.getSearchQuery());
                    stats.updateOccurrenceTimes(searchLog.getCreatedAt());
                }
            }
        }

        return wordStatsMap;
    }

    /**
     * 判断词汇是否有效
     */
    private boolean isValidWord(String word, HotWordRequest request) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        // 长度过滤
        if (word.length() < request.getMinWordLength()) {
            return false;
        }

        // 停用词过滤
        if (request.getExcludeStopWords()) {
            return !isStopWord(word);
        }

        return true;
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        // 简单的停用词判断
        Set<String> stopWords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这",
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * 构建热词响应对象
     */
    private HotWordResponse buildHotWordResponse(WordStatistics stats, int totalWords) {
        HotWordResponse.HotWordResponseBuilder builder = HotWordResponse.builder()
                .word(stats.getWord())
                .count(stats.getCount())
                .percentage(calculatePercentage(stats.getCount(), stats.getTotalOccurrences()))
                .wordLength(stats.getWord().length())
                .relatedQueriesCount((long) stats.getRelatedQueries().size())
                .firstOccurrence(stats.getFirstOccurrence())
                .lastOccurrence(stats.getLastOccurrence());

        return builder.build();
    }

    /**
     * 计算百分比
     */
    private Double calculatePercentage(long count, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(((double) count / total) * 10000.0) / 100.0; // 保留两位小数
    }

    /**
     * 词汇统计信息内部类
     */
    private static class WordStatistics {
        private final String word;
        private long count = 0;
        private final Set<String> relatedQueries = new HashSet<>();
        private LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        private long totalOccurrences = 0;

        public WordStatistics(String word) {
            this.word = word;
        }

        public void incrementCount() {
            this.count++;
            this.totalOccurrences++;
        }

        public void addRelatedQuery(String query) {
            if (query != null && !query.trim().isEmpty()) {
                this.relatedQueries.add(query);
            }
        }

        public void updateOccurrenceTimes(LocalDateTime time) {
            if (time != null) {
                if (this.firstOccurrence == null || time.isBefore(this.firstOccurrence)) {
                    this.firstOccurrence = time;
                }
                if (this.lastOccurrence == null || time.isAfter(this.lastOccurrence)) {
                    this.lastOccurrence = time;
                }
            }
        }

        // Getters
        public String getWord() { return word; }
        public long getCount() { return count; }
        public Set<String> getRelatedQueries() { return relatedQueries; }
        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public long getTotalOccurrences() { return totalOccurrences; }
    }
}