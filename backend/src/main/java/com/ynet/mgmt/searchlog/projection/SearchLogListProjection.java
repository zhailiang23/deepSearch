package com.ynet.mgmt.searchlog.projection;

import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搜索日志列表投影接口
 * 用于列表查询，排除大字段（responseData、requestParams、errorStackTrace）以提升性能
 *
 * @author Claude Code AI
 * @since 2025-10-09
 */
public interface SearchLogListProjection {

    /**
     * 获取日志ID
     */
    Long getId();

    /**
     * 获取链路追踪ID
     */
    String getTraceId();

    /**
     * 获取用户ID
     */
    Long getUserId();

    /**
     * 获取会话ID
     */
    String getSessionId();

    /**
     * 获取IP地址
     */
    String getIpAddress();

    /**
     * 获取用户代理
     */
    String getUserAgent();

    /**
     * 获取搜索空间ID
     */
    Long getSearchSpaceId();

    /**
     * 获取搜索空间代码
     */
    String getSearchSpaceCode();

    /**
     * 获取搜索空间名称
     */
    String getSearchSpaceName();

    /**
     * 获取搜索关键词
     */
    String getSearchQuery();

    /**
     * 获取搜索模式
     */
    String getSearchMode();

    /**
     * 获取是否启用拼音
     */
    Boolean getEnablePinyin();

    /**
     * 获取页码
     */
    Integer getPageNumber();

    /**
     * 获取页大小
     */
    Integer getPageSize();

    /**
     * 获取结果总数
     */
    Long getTotalResults();

    /**
     * 获取返回结果数
     */
    Integer getReturnedResults();

    /**
     * 获取最大相关性分数
     */
    BigDecimal getMaxScore();

    /**
     * 获取ES查询耗时
     */
    Long getElasticsearchTimeMs();

    /**
     * 获取总处理时间
     */
    Long getTotalTimeMs();

    /**
     * 获取内存使用
     */
    Integer getMemoryUsedMb();

    /**
     * 获取请求状态
     */
    SearchLogStatus getStatus();

    /**
     * 获取错误类型
     */
    String getErrorType();

    /**
     * 获取错误消息
     */
    String getErrorMessage();

    /**
     * 获取创建时间
     */
    LocalDateTime getCreatedAt();

    /**
     * 获取更新时间
     */
    LocalDateTime getUpdatedAt();

    /**
     * 获取创建者
     */
    String getCreatedBy();

    /**
     * 获取更新者
     */
    String getUpdatedBy();
}
