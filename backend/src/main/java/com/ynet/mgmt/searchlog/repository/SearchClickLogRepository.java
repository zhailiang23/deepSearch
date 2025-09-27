package com.ynet.mgmt.searchlog.repository;

import com.ynet.mgmt.searchlog.entity.SearchClickLog;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索点击日志Repository接口
 * 提供搜索点击日志的数据访问操作，包括基本CRUD、统计分析、行为分析等功能
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@Repository
public interface SearchClickLogRepository extends JpaRepository<SearchClickLog, Long>, JpaSpecificationExecutor<SearchClickLog> {

    // ========== 基本查询方法 ==========

    /**
     * 根据搜索日志查找所有点击记录
     *
     * @param searchLog 搜索日志
     * @return 点击记录列表
     */
    List<SearchClickLog> findBySearchLog(SearchLog searchLog);

    /**
     * 根据搜索日志ID查找所有点击记录
     *
     * @param searchLogId 搜索日志ID
     * @return 点击记录列表
     */
    List<SearchClickLog> findBySearchLogId(Long searchLogId);

    /**
     * 根据搜索日志ID和点击顺序查找点击记录（按点击顺序排序）
     *
     * @param searchLogId 搜索日志ID
     * @return 点击记录列表
     */
    List<SearchClickLog> findBySearchLogIdOrderByClickSequenceAsc(Long searchLogId);

    /**
     * 根据用户ID查找点击记录
     *
     * @param userId   用户ID
     * @param pageable 分页信息
     * @return 点击记录分页列表
     */
    Page<SearchClickLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和时间范围查找点击记录
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 点击记录分页列表
     */
    Page<SearchClickLog> findByUserIdAndClickTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据文档ID查找点击记录
     *
     * @param documentId 文档ID
     * @param pageable   分页信息
     * @return 点击记录分页列表
     */
    Page<SearchClickLog> findByDocumentId(String documentId, Pageable pageable);

    /**
     * 根据文档索引查找点击记录
     *
     * @param documentIndex 文档索引
     * @param pageable      分页信息
     * @return 点击记录分页列表
     */
    Page<SearchClickLog> findByDocumentIndex(String documentIndex, Pageable pageable);

    /**
     * 根据点击位置查找点击记录
     *
     * @param clickPosition 点击位置
     * @param pageable      分页信息
     * @return 点击记录分页列表
     */
    Page<SearchClickLog> findByClickPosition(Integer clickPosition, Pageable pageable);

    /**
     * 根据时间范围查找点击记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 点击记录分页列表
     */
    Page<SearchClickLog> findByClickTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // ========== 统计查询方法 ==========

    /**
     * 统计搜索日志的点击次数
     *
     * @param searchLogId 搜索日志ID
     * @return 点击次数
     */
    long countBySearchLogId(Long searchLogId);

    /**
     * 统计用户的点击次数
     *
     * @param userId 用户ID
     * @return 点击次数
     */
    long countByUserId(Long userId);

    /**
     * 统计文档的点击次数
     *
     * @param documentId 文档ID
     * @return 点击次数
     */
    long countByDocumentId(String documentId);

    /**
     * 统计时间范围内的点击次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 点击次数
     */
    long countByClickTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定位置的点击次数
     *
     * @param clickPosition 点击位置
     * @return 点击次数
     */
    long countByClickPosition(Integer clickPosition);

    // ========== 存在性查询方法 ==========

    /**
     * 检查搜索日志是否有点击记录
     *
     * @param searchLogId 搜索日志ID
     * @return 是否有点击记录
     */
    boolean existsBySearchLogId(Long searchLogId);

    /**
     * 检查用户是否有点击记录
     *
     * @param userId 用户ID
     * @return 是否有点击记录
     */
    boolean existsByUserId(Long userId);

    /**
     * 检查文档是否被点击过
     *
     * @param documentId 文档ID
     * @return 是否被点击过
     */
    boolean existsByDocumentId(String documentId);

    // ========== 自定义查询方法 ==========

    /**
     * 查找热门文档（按点击次数排序）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     限制数量
     * @return 热门文档列表（文档ID、标题、点击次数）
     */
    @Query(value = "SELECT document_id, document_title, COUNT(*) as click_count FROM search_click_logs " +
                   "WHERE click_time BETWEEN :startTime AND :endTime " +
                   "GROUP BY document_id, document_title ORDER BY click_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findPopularDocuments(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("limit") int limit);

    /**
     * 查找点击位置分布统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 点击位置分布（位置、点击次数）
     */
    @Query("SELECT c.clickPosition, COUNT(c) FROM SearchClickLog c " +
           "WHERE c.clickTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.clickPosition ORDER BY c.clickPosition ASC")
    List<Object[]> findClickPositionDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找用户点击行为分析
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 用户点击行为统计（总点击次数、平均点击位置、首次点击比例）
     */
    @Query("SELECT COUNT(c), AVG(c.clickPosition), " +
           "COUNT(CASE WHEN c.clickSequence = 1 THEN 1 END) * 100.0 / COUNT(c) FROM SearchClickLog c " +
           "WHERE c.userId = :userId AND c.clickTime BETWEEN :startTime AND :endTime")
    Object[] findUserClickBehavior(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找搜索日志的点击序列
     *
     * @param searchLogId 搜索日志ID
     * @return 点击序列（按点击顺序排序）
     */
    @Query("SELECT c.clickSequence, c.clickPosition, c.documentId, c.documentTitle, c.clickTime FROM SearchClickLog c " +
           "WHERE c.searchLog.id = :searchLogId ORDER BY c.clickSequence ASC")
    List<Object[]> findClickSequenceBySearchLogId(@Param("searchLogId") Long searchLogId);

    /**
     * 查找多次点击的搜索日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页信息
     * @return 多次点击的搜索日志ID列表
     */
    @Query("SELECT c.searchLog.id, COUNT(c) FROM SearchClickLog c " +
           "WHERE c.clickTime BETWEEN :startTime AND :endTime " +
           "GROUP BY c.searchLog.id HAVING COUNT(c) > 1 ORDER BY COUNT(c) DESC")
    Page<Object[]> findMultipleClickSearchLogs(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    /**
     * 查找点击时间分布（按小时统计）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 点击时间分布（小时、点击次数）
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM click_time) as hour, COUNT(*) as click_count FROM search_click_logs " +
                   "WHERE click_time BETWEEN :startTime AND :endTime " +
                   "GROUP BY EXTRACT(HOUR FROM click_time) ORDER BY hour", nativeQuery = true)
    List<Object[]> findClickTimeDistribution(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找快速点击记录（点击时间与搜索时间间隔短）
     *
     * @param maxDelaySeconds 最大延迟秒数
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @param pageable        分页信息
     * @return 快速点击记录列表
     */
    @Query(value = "SELECT c.* FROM search_click_logs c " +
                   "INNER JOIN search_logs s ON c.search_log_id = s.id " +
                   "WHERE c.click_time BETWEEN :startTime AND :endTime " +
                   "AND EXTRACT(EPOCH FROM (c.click_time - s.created_at)) <= :maxDelaySeconds " +
                   "ORDER BY c.click_time DESC", nativeQuery = true)
    Page<SearchClickLog> findQuickClickLogs(@Param("maxDelaySeconds") Long maxDelaySeconds,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           Pageable pageable);

    /**
     * 查找无点击搜索的统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 无点击搜索统计（总搜索数、有点击搜索数、无点击比例）
     */
    @Query(value = "SELECT " +
                   "COUNT(DISTINCT s.id) as total_searches, " +
                   "COUNT(DISTINCT c.search_log_id) as clicked_searches, " +
                   "(COUNT(DISTINCT s.id) - COUNT(DISTINCT c.search_log_id)) * 100.0 / COUNT(DISTINCT s.id) as no_click_rate " +
                   "FROM search_logs s " +
                   "LEFT JOIN search_click_logs c ON s.id = c.search_log_id " +
                   "WHERE s.created_at BETWEEN :startTime AND :endTime", nativeQuery = true)
    Object[] findNoClickStatistics(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找用户最近的点击记录
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return 最近点击记录列表
     */
    @Query("SELECT c FROM SearchClickLog c WHERE c.userId = :userId ORDER BY c.clickTime DESC")
    List<SearchClickLog> findRecentClicksByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 删除指定时间之前的点击日志
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Query("DELETE FROM SearchClickLog c WHERE c.clickTime < :beforeTime")
    int deleteByClickTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);
}