package com.ynet.mgmt.searchlog.service;

import com.ynet.mgmt.searchlog.dto.*;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 搜索日志服务接口
 * 提供搜索日志的增删改查、分页查询、条件筛选、点击行为记录、统计分析等功能
 */
public interface SearchLogService {

    /**
     * 保存搜索日志
     *
     * @param searchLog 搜索日志实体
     * @return 保存后的搜索日志
     */
    SearchLog saveSearchLog(SearchLog searchLog);

    /**
     * 分页查询搜索日志
     *
     * @param request 查询请求参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SearchLog> getSearchLogs(SearchLogQueryRequest request, Pageable pageable);

    /**
     * 根据ID获取搜索日志详情
     *
     * @param id 搜索日志ID
     * @return 搜索日志详情
     */
    SearchLogDetailResponse getSearchLogDetail(Long id);

    /**
     * 记录点击行为
     *
     * @param request 点击记录请求
     */
    void recordClickAction(SearchClickRequest request);

    /**
     * 获取搜索统计数据
     *
     * @param request 统计请求参数
     * @return 统计数据
     */
    SearchLogStatistics getSearchStatistics(StatisticsRequest request);

    /**
     * 清理过期日志数据
     *
     * @param retentionDays 保留天数
     * @return 清理的记录数
     */
    long cleanupExpiredLogs(int retentionDays);

    /**
     * 批量删除日志
     *
     * @param logIds 日志ID列表
     * @return 删除的记录数
     */
    long batchDeleteLogs(List<Long> logIds);
}