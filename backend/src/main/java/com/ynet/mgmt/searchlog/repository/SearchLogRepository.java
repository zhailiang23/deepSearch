package com.ynet.mgmt.searchlog.repository;

import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 搜索日志Repository接口
 * 提供搜索日志的数据访问操作，包括基本CRUD、分页查询、统计分析等功能
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long>, JpaSpecificationExecutor<SearchLog> {

    // ========== 基本查询方法 ==========

    /**
     * 根据链路追踪ID查找搜索日志
     *
     * @param traceId 链路追踪ID
     * @return 搜索日志
     */
    Optional<SearchLog> findByTraceId(String traceId);

    /**
     * 根据用户ID查找搜索日志列表
     *
     * @param userId   用户ID
     * @param pageable 分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和时间范围查找搜索日志
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据会话ID查找搜索日志列表
     *
     * @param sessionId 会话ID
     * @param pageable  分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findBySessionId(String sessionId, Pageable pageable);

    /**
     * 根据搜索空间ID查找搜索日志
     *
     * @param searchSpaceId 搜索空间ID
     * @param pageable      分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findBySearchSpaceId(Long searchSpaceId, Pageable pageable);

    /**
     * 根据状态查找搜索日志
     *
     * @param status   搜索状态
     * @param pageable 分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findByStatus(SearchLogStatus status, Pageable pageable);

    /**
     * 根据时间范围查找搜索日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== 复合查询方法 ==========

    /**
     * 根据搜索空间和时间范围查找搜索日志
     *
     * @param searchSpaceId 搜索空间ID
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param pageable      分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findBySearchSpaceIdAndCreatedAtBetween(Long searchSpaceId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据状态和时间范围查找搜索日志
     *
     * @param status    搜索状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 搜索日志分页列表
     */
    Page<SearchLog> findByStatusAndCreatedAtBetween(SearchLogStatus status, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== 统计查询方法 ==========

    /**
     * 统计用户搜索次数
     *
     * @param userId 用户ID
     * @return 搜索次数
     */
    long countByUserId(Long userId);

    /**
     * 统计指定状态和时间范围内的搜索次数
     *
     * @param status    搜索状态
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 搜索次数
     */
    long countByStatusAndCreatedAtBetween(SearchLogStatus status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计时间范围内的搜索次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 搜索次数
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定状态的搜索次数
     *
     * @param status 搜索状态
     * @return 搜索次数
     */
    long countByStatus(SearchLogStatus status);

    /**
     * 统计搜索空间的使用次数
     *
     * @param searchSpaceId 搜索空间ID
     * @return 使用次数
     */
    long countBySearchSpaceId(Long searchSpaceId);

    /**
     * 计算指定时间范围内的平均响应时间
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 平均响应时间（毫秒）
     */
    @Query("SELECT AVG(s.totalTimeMs) FROM SearchLog s WHERE s.status = 'SUCCESS' AND s.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageResponseTimeByPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定时间范围内的热门查询关键词
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 热门查询列表（查询词，搜索次数）
     */
    @Query("SELECT s.searchQuery, COUNT(s) FROM SearchLog s WHERE s.createdAt BETWEEN :startTime AND :endTime GROUP BY s.searchQuery ORDER BY COUNT(s) DESC")
    List<Object[]> findTopQueriesByPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 查找指定时间范围内成功搜索的查询词用于热词统计
     * 只返回状态为SUCCESS的搜索日志的查询词，排除非空和有效查询
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功搜索的查询词列表
     */
    @Query("SELECT s.searchQuery FROM SearchLog s WHERE s.status = 'SUCCESS' " +
           "AND (:startTime IS NULL OR s.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR s.createdAt <= :endTime) " +
           "AND s.searchQuery IS NOT NULL AND LENGTH(TRIM(s.searchQuery)) > 0")
    List<String> findSuccessfulSearchQueriesByTimeRange(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内成功搜索的总数
     * 用于计算热词百分比
     *
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 成功搜索总数
     */
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.status = 'SUCCESS' " +
           "AND (:startTime IS NULL OR s.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR s.createdAt <= :endTime) " +
           "AND s.searchQuery IS NOT NULL AND LENGTH(TRIM(s.searchQuery)) > 0")
    long countSuccessfulSearchesByTimeRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定时间范围内的热门搜索空间
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 热门搜索空间列表（搜索空间ID，搜索次数）
     */
    @Query("SELECT s.searchSpaceId, COUNT(s) FROM SearchLog s WHERE s.createdAt BETWEEN :startTime AND :endTime GROUP BY s.searchSpaceId ORDER BY COUNT(s) DESC")
    List<Object[]> findTopSearchSpacesByPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    // ========== 存在性查询方法 ==========

    /**
     * 检查链路追踪ID是否存在
     *
     * @param traceId 链路追踪ID
     * @return 是否存在
     */
    boolean existsByTraceId(String traceId);

    /**
     * 检查用户是否有搜索记录
     *
     * @param userId 用户ID
     * @return 是否有搜索记录
     */
    boolean existsByUserId(Long userId);

    // ========== 自定义查询方法 ==========

    /**
     * 查找热门搜索关键词
     *
     * @param startTime 开始时间
     * @param pageable  分页参数（用于限制数量）
     * @return 热门关键词列表（关键词和搜索次数）
     */
    @Query(value = "SELECT search_query, COUNT(*) as search_count FROM search_logs " +
                   "WHERE status = 'SUCCESS' AND created_at >= :startTime " +
                   "GROUP BY search_query ORDER BY search_count DESC", nativeQuery = true)
    List<Object[]> findPopularSearchQueries(@Param("startTime") LocalDateTime startTime, Pageable pageable);

    /**
     * 查找搜索性能统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 性能统计信息（平均响应时间、最大响应时间、最小响应时间）
     */
    @Query("SELECT AVG(s.totalTimeMs), MAX(s.totalTimeMs), MIN(s.totalTimeMs) FROM SearchLog s " +
           "WHERE s.status = 'SUCCESS' AND s.createdAt BETWEEN :startTime AND :endTime")
    Object[] findPerformanceStatistics(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找搜索空间使用统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 搜索空间使用统计列表（搜索空间ID、名称、使用次数）
     */
    @Query("SELECT s.searchSpaceId, s.searchSpaceCode, COUNT(s) FROM SearchLog s " +
           "WHERE s.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY s.searchSpaceId, s.searchSpaceCode ORDER BY COUNT(s) DESC")
    List<Object[]> findSearchSpaceUsageStatistics(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找错误统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 错误统计列表（错误类型、错误次数）
     */
    @Query("SELECT s.errorMessage, COUNT(s) FROM SearchLog s " +
           "WHERE s.status IN ('ERROR', 'TIMEOUT') AND s.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY s.errorMessage ORDER BY COUNT(s) DESC")
    List<Object[]> findErrorStatistics(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找用户搜索行为分析
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 用户搜索行为统计（搜索次数、平均结果数、有点击的搜索次数）
     */
    @Query("SELECT COUNT(s), AVG(s.totalResults), " +
           "COUNT(CASE WHEN SIZE(s.clickLogs) > 0 THEN 1 END) FROM SearchLog s " +
           "WHERE s.userId = :userId AND s.createdAt BETWEEN :startTime AND :endTime")
    Object[] findUserSearchBehavior(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找慢搜索日志
     *
     * @param thresholdMs 时间阈值（毫秒）
     * @param pageable    分页信息
     * @return 慢搜索日志列表
     */
    @Query("SELECT s FROM SearchLog s WHERE s.totalTimeMs > :thresholdMs ORDER BY s.totalTimeMs DESC")
    Page<SearchLog> findSlowSearchLogs(@Param("thresholdMs") Long thresholdMs, Pageable pageable);

    /**
     * 查找无结果搜索
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 无结果搜索列表
     */
    @Query("SELECT s FROM SearchLog s WHERE s.totalResults = 0 AND s.createdAt BETWEEN :startTime AND :endTime ORDER BY s.createdAt DESC")
    Page<SearchLog> findNoResultSearchLogs(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 查找有点击行为的搜索日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 有点击的搜索日志列表
     */
    @Query("SELECT DISTINCT s FROM SearchLog s JOIN s.clickLogs c WHERE s.createdAt BETWEEN :startTime AND :endTime ORDER BY s.createdAt DESC")
    Page<SearchLog> findSearchLogsWithClicks(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 删除指定时间之前的搜索日志
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM SearchLog s WHERE s.createdAt < :beforeTime")
    int deleteByCreatedAtBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 批量删除过期的搜索日志
     *
     * @param cutoffDate 截止日期
     * @param batchSize  批次大小
     * @return 删除的记录数
     */
    @Modifying
    @Query(value = "DELETE FROM search_logs WHERE ctid IN (" +
                   "SELECT ctid FROM search_logs WHERE created_at < :cutoffDate LIMIT CAST(:batchSize AS INTEGER))",
           nativeQuery = true)
    int deleteExpiredSearchLogsBatch(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("batchSize") int batchSize);

    /**
     * 统计过期日志数量
     *
     * @param cutoffDate 截止日期
     * @return 过期日志数量
     */
    long countByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * 统计过期日志数量
     *
     * @param cutoffDate 截止日期
     * @return 过期日志数量
     */
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.createdAt < :cutoffDate")
    long countExpiredLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 统计最近时间内的日志数量
     *
     * @param afterTime 时间点
     * @return 日志数量
     */
    long countByCreatedAtAfter(LocalDateTime afterTime);

    // ========== 性能优化查询方法 ==========

    /**
     * 分页查询热词统计原始数据
     * 优化版本：避免加载所有数据到内存
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 搜索查询分页列表
     */
    @Query("SELECT s.searchQuery FROM SearchLog s WHERE s.status = 'SUCCESS' " +
           "AND s.createdAt BETWEEN :startTime AND :endTime " +
           "AND s.searchQuery IS NOT NULL AND s.searchQuery != ''")
    Page<String> findSearchQueriesForHotWords(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             Pageable pageable);

    /**
     * 获取热词统计的优化查询
     * 使用数据库聚合功能，避免内存计算
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数（用于限制数量）
     * @return 查询词频统计
     */
    @Query(value = "SELECT search_query, COUNT(*) as query_count, " +
                   "MIN(created_at) as first_occurrence, MAX(created_at) as last_occurrence " +
                   "FROM search_logs " +
                   "WHERE status = 'SUCCESS' AND created_at BETWEEN :startTime AND :endTime " +
                   "AND search_query IS NOT NULL AND search_query != '' " +
                   "GROUP BY search_query " +
                   "ORDER BY query_count DESC", nativeQuery = true)
    List<Object[]> findHotQueriesOptimized(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          Pageable pageable);

    /**
     * 批量统计查询
     * 一次查询获取多个统计指标，减少数据库交互
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据数组 [总数, 成功数, 平均响应时间, 最大响应时间, 最小响应时间]
     */
    @Query("SELECT COUNT(s), " +
           "SUM(CASE WHEN s.status = 'SUCCESS' THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN s.status = 'SUCCESS' THEN s.totalTimeMs ELSE NULL END), " +
           "MAX(CASE WHEN s.status = 'SUCCESS' THEN s.totalTimeMs ELSE NULL END), " +
           "MIN(CASE WHEN s.status = 'SUCCESS' THEN s.totalTimeMs ELSE NULL END) " +
           "FROM SearchLog s WHERE s.createdAt BETWEEN :startTime AND :endTime")
    Object[] findBatchStatistics(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 条件查询热词数据（带过滤条件）
     *
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @param userId         用户ID（可选）
     * @param searchSpaceId  搜索空间ID（可选）
     * @param pageable       分页参数
     * @return 搜索查询分页列表
     */
    @Query("SELECT s.searchQuery FROM SearchLog s WHERE s.status = 'SUCCESS' " +
           "AND s.createdAt BETWEEN :startTime AND :endTime " +
           "AND s.searchQuery IS NOT NULL AND s.searchQuery != '' " +
           "AND (:userId IS NULL OR s.userId = :userId) " +
           "AND (:searchSpaceId IS NULL OR s.searchSpaceId = :searchSpaceId)")
    Page<String> findSearchQueriesForHotWordsWithFilter(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       @Param("userId") Long userId,
                                                       @Param("searchSpaceId") Long searchSpaceId,
                                                       Pageable pageable);

    /**
     * 获取用户搜索行为分析的优化查询
     *
     * @param userIds   用户ID列表
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 用户行为统计
     */
    @Query("SELECT s.userId, COUNT(s), AVG(s.totalResults), " +
           "COUNT(CASE WHEN SIZE(s.clickLogs) > 0 THEN 1 END) " +
           "FROM SearchLog s WHERE s.userId IN :userIds " +
           "AND s.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY s.userId")
    List<Object[]> findUserSearchBehaviorBatch(@Param("userIds") List<Long> userIds,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内有搜索记录的活跃用户ID
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 活跃用户ID列表
     */
    @Query("SELECT DISTINCT s.userId FROM SearchLog s " +
           "WHERE s.userId IS NOT NULL " +
           "AND s.createdAt BETWEEN :startTime AND :endTime")
    List<Long> findActiveUserIds(@Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询时间范围内的搜索查询字符串和结果数（用于分词后的关键词不匹配度分析）
     * 返回格式：[searchQuery, totalResults]
     * 不查询 response_data 等大字段，性能优化
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 查询字符串和结果数列表
     */
    @Query("SELECT s.searchQuery, s.totalResults " +
           "FROM SearchLog s " +
           "WHERE s.createdAt BETWEEN :startTime AND :endTime")
    List<Object[]> findQueryAndResultsInTimeRange(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
}