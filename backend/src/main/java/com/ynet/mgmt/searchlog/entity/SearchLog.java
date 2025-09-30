package com.ynet.mgmt.searchlog.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Comment;

import com.ynet.mgmt.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 搜索日志实体类
 * 记录所有搜索请求的详细信息，包括请求参数、响应结果、性能指标和错误信息
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@Entity
@Table(name = "search_logs", indexes = {
    @Index(name = "idx_search_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_search_logs_created_at_desc", columnList = "created_at DESC"),
    @Index(name = "idx_search_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_search_logs_session_id", columnList = "session_id"),
    @Index(name = "idx_search_logs_trace_id", columnList = "trace_id"),
    @Index(name = "idx_search_logs_search_space_id", columnList = "search_space_id"),
    @Index(name = "idx_search_logs_status", columnList = "status"),
    @Index(name = "idx_search_logs_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_search_logs_space_created", columnList = "search_space_id, created_at DESC")
})
@Comment("搜索日志主表")
public class SearchLog extends BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 链路追踪ID
     */
    @Column(name = "trace_id", length = 64)
    @Comment("链路追踪ID")
    private String traceId;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    @Comment("用户ID")
    private Long userId;

    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64)
    @Comment("会话ID")
    private String sessionId;

    /**
     * IP地址(支持IPv6)
     */
    @Column(name = "ip_address", length = 45)
    @Comment("IP地址(支持IPv6)")
    private String ipAddress;

    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", length = 1000)
    @Comment("用户代理信息")
    private String userAgent;

    // ========== 搜索请求信息 ==========

    /**
     * 搜索空间ID
     */
    @Column(name = "search_space_id")
    @Comment("搜索空间ID")
    private Long searchSpaceId;

    /**
     * 搜索空间代码
     */
    @Column(name = "search_space_code", length = 50)
    @Comment("搜索空间代码")
    private String searchSpaceCode;

    /**
     * 搜索空间名称
     */
    @Column(name = "search_space_name", length = 100)
    @Comment("搜索空间名称")
    private String searchSpaceName;

    /**
     * 搜索关键词
     */
    @Column(name = "search_query", length = 2000)
    @Comment("搜索关键词")
    @NotBlank(message = "搜索关键词不能为空")
    private String searchQuery;

    /**
     * 搜索模式
     */
    @Column(name = "search_mode", length = 20)
    @Comment("搜索模式")
    private String searchMode;

    /**
     * 是否启用拼音搜索
     */
    @Column(name = "enable_pinyin")
    @Comment("是否启用拼音搜索")
    private Boolean enablePinyin = false;

    /**
     * 页码
     */
    @Column(name = "page_number")
    @Comment("页码")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNumber = 1;

    /**
     * 页大小
     */
    @Column(name = "page_size")
    @Comment("页大小")
    @Min(value = 1, message = "页大小必须大于0")
    @Max(value = 1000, message = "页大小不能超过1000")
    private Integer pageSize = 10;

    /**
     * 请求参数JSON字符串
     */
    @Lob
    @Column(name = "request_params", columnDefinition = "LONGTEXT")
    @Comment("请求参数JSON字符串")
    private String requestParams;

    /**
     * 响应数据JSON字符串
     */
    @Lob
    @Column(name = "response_data", columnDefinition = "LONGTEXT")
    @Comment("响应数据JSON字符串")
    private String responseData;

    // ========== 响应信息 ==========

    /**
     * 结果总数
     */
    @Column(name = "total_results")
    @Comment("结果总数")
    @Min(value = 0, message = "结果总数不能为负数")
    private Long totalResults = 0L;

    /**
     * 返回结果数
     */
    @Column(name = "returned_results")
    @Comment("返回结果数")
    @Min(value = 0, message = "返回结果数不能为负数")
    private Integer returnedResults = 0;

    /**
     * 最大相关性分数
     */
    @Column(name = "max_score", precision = 10, scale = 6)
    @Comment("最大相关性分数")
    private BigDecimal maxScore;

    // ========== 性能指标 ==========

    /**
     * ES查询耗时(毫秒)
     */
    @Column(name = "elasticsearch_time_ms")
    @Comment("ES查询耗时(毫秒)")
    @Min(value = 0, message = "ES查询耗时不能为负数")
    private Long elasticsearchTimeMs;

    /**
     * 总处理时间(毫秒)
     */
    @Column(name = "total_time_ms")
    @Comment("总处理时间(毫秒)")
    @Min(value = 0, message = "总处理时间不能为负数")
    private Long totalTimeMs;

    /**
     * 内存使用(MB)
     */
    @Column(name = "memory_used_mb")
    @Comment("内存使用(MB)")
    @Min(value = 0, message = "内存使用不能为负数")
    private Integer memoryUsedMb;

    // ========== 状态信息 ==========

    /**
     * 请求状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Comment("请求状态")
    @NotNull(message = "请求状态不能为空")
    private SearchLogStatus status = SearchLogStatus.SUCCESS;

    /**
     * 错误类型
     */
    @Column(name = "error_type", length = 100)
    @Comment("错误类型")
    private String errorType;

    /**
     * 错误消息
     */
    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    @Comment("错误消息")
    private String errorMessage;

    /**
     * 错误堆栈跟踪
     */
    @Lob
    @Column(name = "error_stack_trace", columnDefinition = "LONGTEXT")
    @Comment("错误堆栈跟踪")
    private String errorStackTrace;

    // ========== 关联关系 ==========

    /**
     * 搜索点击日志列表（一对多关系）
     */
    @OneToMany(mappedBy = "searchLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Comment("搜索点击日志列表")
    private List<SearchClickLog> clickLogs = new ArrayList<>();

    // ========== 构造函数 ==========

    /**
     * 默认构造函数
     */
    public SearchLog() {
    }

    /**
     * 构造函数
     *
     * @param searchQuery 搜索关键词
     * @param userId     用户ID
     * @param sessionId  会话ID
     */
    public SearchLog(String searchQuery, Long userId, String sessionId) {
        this.searchQuery = searchQuery;
        this.userId = userId;
        this.sessionId = sessionId;
    }

    // ========== 业务方法 ==========

    /**
     * 判断搜索是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccessful() {
        return status != null && status.isSuccessful();
    }

    /**
     * 判断搜索是否有错误
     *
     * @return 是否有错误
     */
    public boolean hasError() {
        return status != null && status.isError();
    }

    /**
     * 判断是否有点击行为
     *
     * @return 是否有点击
     */
    public boolean hasClicks() {
        return clickLogs != null && !clickLogs.isEmpty();
    }

    /**
     * 获取点击次数
     *
     * @return 点击次数
     */
    public int getClickCount() {
        return clickLogs != null ? clickLogs.size() : 0;
    }

    /**
     * 添加点击日志
     *
     * @param clickLog 点击日志
     */
    public void addClickLog(SearchClickLog clickLog) {
        if (clickLogs == null) {
            clickLogs = new ArrayList<>();
        }
        clickLogs.add(clickLog);
        clickLog.setSearchLog(this);
    }

    /**
     * 移除点击日志
     *
     * @param clickLog 点击日志
     */
    public void removeClickLog(SearchClickLog clickLog) {
        if (clickLogs != null) {
            clickLogs.remove(clickLog);
            clickLog.setSearchLog(null);
        }
    }

    /**
     * 判断搜索是否超时
     *
     * @return 是否超时
     */
    public boolean isTimeout() {
        return status == SearchLogStatus.TIMEOUT;
    }

    /**
     * 设置错误信息
     *
     * @param errorType    错误类型
     * @param errorMessage 错误消息
     */
    public void setError(String errorType, String errorMessage) {
        this.status = SearchLogStatus.ERROR;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    /**
     * 设置超时状态
     */
    public void setTimeout() {
        this.status = SearchLogStatus.TIMEOUT;
        this.errorType = "TIMEOUT";
        this.errorMessage = "搜索请求超时";
    }

    // ========== Getter和Setter ==========

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Long getSearchSpaceId() {
        return searchSpaceId;
    }

    public void setSearchSpaceId(Long searchSpaceId) {
        this.searchSpaceId = searchSpaceId;
    }

    public String getSearchSpaceCode() {
        return searchSpaceCode;
    }

    public void setSearchSpaceCode(String searchSpaceCode) {
        this.searchSpaceCode = searchSpaceCode;
    }

    public String getSearchSpaceName() {
        return searchSpaceName;
    }

    public void setSearchSpaceName(String searchSpaceName) {
        this.searchSpaceName = searchSpaceName;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public Boolean getEnablePinyin() {
        return enablePinyin;
    }

    public void setEnablePinyin(Boolean enablePinyin) {
        this.enablePinyin = enablePinyin;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public Long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Long totalResults) {
        this.totalResults = totalResults;
    }

    public Integer getReturnedResults() {
        return returnedResults;
    }

    public void setReturnedResults(Integer returnedResults) {
        this.returnedResults = returnedResults;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Long getElasticsearchTimeMs() {
        return elasticsearchTimeMs;
    }

    public void setElasticsearchTimeMs(Long elasticsearchTimeMs) {
        this.elasticsearchTimeMs = elasticsearchTimeMs;
    }

    public Long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(Long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public Integer getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public void setMemoryUsedMb(Integer memoryUsedMb) {
        this.memoryUsedMb = memoryUsedMb;
    }

    public SearchLogStatus getStatus() {
        return status;
    }

    public void setStatus(SearchLogStatus status) {
        this.status = status;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    public List<SearchClickLog> getClickLogs() {
        return clickLogs;
    }

    public void setClickLogs(List<SearchClickLog> clickLogs) {
        this.clickLogs = clickLogs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // ========== equals, hashCode, toString ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SearchLog searchLog = (SearchLog) o;
        return Objects.equals(traceId, searchLog.traceId) &&
               Objects.equals(searchQuery, searchLog.searchQuery) &&
               Objects.equals(userId, searchLog.userId) &&
               Objects.equals(sessionId, searchLog.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), traceId, searchQuery, userId, sessionId);
    }

    @Override
    public String toString() {
        return "SearchLog{" +
               "id=" + getId() +
               ", traceId='" + traceId + '\'' +
               ", userId=" + userId +
               ", sessionId='" + sessionId + '\'' +
               ", searchQuery='" + searchQuery + '\'' +
               ", searchSpaceId=" + searchSpaceId +
               ", status=" + status +
               ", totalResults=" + totalResults +
               ", clickCount=" + getClickCount() +
               ", createdAt=" + getCreatedAt() +
               '}';
    }
}