package com.ynet.mgmt.searchlog.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.exception.BusinessException;
import com.ynet.mgmt.searchlog.dto.*;
import com.ynet.mgmt.searchlog.entity.SearchClickLog;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.repository.SearchClickLogRepository;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
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
    private final ObjectMapper objectMapper;

    @Override
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
                    .requestParams("{}") // 暂时使用空JSON，实际可根据需要构建
                    .responseData("{}") // 暂时使用空JSON，实际可根据需要构建
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
    public SearchLogStatistics getSearchStatistics(StatisticsRequest request) {
        try {
            LocalDateTime startTime = request.getStartTime();
            LocalDateTime endTime = request.getEndTime();

            // 总搜索次数
            long totalSearches = searchLogRepository.countByCreatedAtBetween(startTime, endTime);

            // 成功搜索次数
            long successfulSearches = searchLogRepository.countByStatusAndCreatedAtBetween(
                    SearchLogStatus.SUCCESS, startTime, endTime);

            // 失败搜索次数
            long failedSearches = totalSearches - successfulSearches;

            // 成功率
            double successRate = totalSearches > 0 ? (double) successfulSearches / totalSearches : 0.0;

            // 平均响应时间
            Double avgResponseTime = searchLogRepository.getAverageResponseTimeByPeriod(startTime, endTime);

            // 热门查询关键词
            List<Object[]> topQueriesData = searchLogRepository.findTopQueriesByPeriod(
                    startTime, endTime, PageRequest.of(0, request.getTopQueriesLimit()));
            List<SearchLogStatistics.QueryStatistic> topQueries = convertToQueryStatistics(topQueriesData);

            // 热门搜索空间
            List<Object[]> topSearchSpacesData = searchLogRepository.findTopSearchSpacesByPeriod(
                    startTime, endTime, PageRequest.of(0, request.getTopSearchSpacesLimit()));
            List<SearchLogStatistics.SearchSpaceStatistic> topSearchSpaces = convertToSearchSpaceStatistics(topSearchSpacesData);

            // 构建统计结果
            SearchLogStatistics statistics = SearchLogStatistics.builder()
                    .totalSearches(totalSearches)
                    .successfulSearches(successfulSearches)
                    .failedSearches(failedSearches)
                    .successRate(successRate)
                    .averageResponseTime(avgResponseTime != null ? avgResponseTime : 0.0)
                    .topQueries(topQueries)
                    .topSearchSpaces(topSearchSpaces)
                    .build();

            log.debug("获取搜索统计成功: 总搜索={}, 成功率={}", totalSearches, successRate);
            return statistics;

        } catch (Exception e) {
            log.error("获取搜索统计失败: request={}", request, e);
            throw new BusinessException("SEARCH_STATISTICS_ERROR", "获取搜索统计失败: " + e.getMessage());
        }
    }

    @Override
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
                predicates.add(criteriaBuilder.equal(root.get("searchSpaceId"), request.getSearchSpaceId()));
            }

            // 查询关键词筛选
            if (StringUtils.hasText(request.getQuery())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("searchQuery")),
                        "%" + request.getQuery().toLowerCase() + "%"));
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
}