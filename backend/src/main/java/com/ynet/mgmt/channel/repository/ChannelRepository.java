package com.ynet.mgmt.channel.repository;

import com.ynet.mgmt.channel.entity.Channel;
import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 渠道Repository接口
 * 提供渠道实体的数据访问方法
 *
 * @author system
 * @since 1.0.0
 */
@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long>,
                                         JpaSpecificationExecutor<Channel> {

    // 基础查询方法

    /**
     * 根据渠道代码查找渠道
     * @param code 渠道代码
     * @return 渠道对象Optional
     */
    Optional<Channel> findByCode(String code);

    /**
     * 根据渠道名称查找渠道
     * @param name 渠道名称
     * @return 渠道对象Optional
     */
    Optional<Channel> findByName(String name);

    /**
     * 根据状态查找渠道列表
     * @param status 渠道状态
     * @return 渠道列表
     */
    List<Channel> findByStatus(ChannelStatus status);

    /**
     * 根据类型查找渠道列表
     * @param type 渠道类型
     * @return 渠道列表
     */
    List<Channel> findByType(ChannelType type);

    // 组合查询方法

    /**
     * 根据状态和类型查找渠道列表
     * @param status 渠道状态
     * @param type 渠道类型
     * @return 渠道列表
     */
    List<Channel> findByStatusAndType(ChannelStatus status, ChannelType type);

    /**
     * 根据状态列表查找渠道
     * @param statuses 状态列表
     * @return 渠道列表
     */
    List<Channel> findByStatusIn(List<ChannelStatus> statuses);

    /**
     * 根据状态和类型查找渠道分页列表
     * @param status 渠道状态
     * @param type 渠道类型
     * @param pageable 分页参数
     * @return 渠道分页结果
     */
    Page<Channel> findByStatusAndType(ChannelStatus status, ChannelType type, Pageable pageable);

    // 排序查询方法

    /**
     * 根据状态查找渠道并按排序顺序排序
     * @param status 渠道状态
     * @return 渠道列表
     */
    List<Channel> findByStatusOrderBySortOrder(ChannelStatus status);

    /**
     * 查找所有渠道并按排序顺序排序
     * @return 渠道列表
     */
    List<Channel> findAllByOrderBySortOrder();

    /**
     * 按排序顺序和创建时间排序查找渠道
     * @param pageable 分页参数
     * @return 渠道分页结果
     */
    Page<Channel> findAllByOrderBySortOrderAscCreatedAtDesc(Pageable pageable);

    // 销售相关查询

    /**
     * 查找总销售额大于指定金额的渠道
     * @param amount 金额阈值
     * @return 渠道列表
     */
    List<Channel> findByTotalSalesGreaterThan(BigDecimal amount);

    /**
     * 查找当月销售额大于等于指定金额的渠道
     * @param target 目标金额
     * @return 渠道列表
     */
    List<Channel> findByCurrentMonthSalesGreaterThanEqual(BigDecimal target);

    /**
     * 查找已达到月度目标的渠道
     * @return 渠道列表
     */
    @Query("SELECT c FROM Channel c WHERE c.currentMonthSales >= c.monthlyTarget AND c.monthlyTarget > 0")
    List<Channel> findChannelsWithTargetAchieved();

    /**
     * 查找未达到月度目标的活跃渠道
     * @return 渠道列表
     */
    @Query("SELECT c FROM Channel c WHERE c.status = 'ACTIVE' AND " +
           "(c.currentMonthSales < c.monthlyTarget OR c.currentMonthSales IS NULL) AND c.monthlyTarget > 0")
    List<Channel> findActiveChannelsWithTargetNotAchieved();

    // 时间范围查询

    /**
     * 查找最后活动时间在指定时间之后的渠道
     * @param date 时间阈值
     * @return 渠道列表
     */
    List<Channel> findByLastActivityAtAfter(LocalDateTime date);

    /**
     * 查找在指定时间范围内创建的渠道
     * @param start 开始时间
     * @param end 结束时间
     * @return 渠道列表
     */
    List<Channel> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查找指定天数内无活动的渠道
     * @param date 时间阈值
     * @return 渠道列表
     */
    @Query("SELECT c FROM Channel c WHERE c.lastActivityAt < :date OR c.lastActivityAt IS NULL")
    List<Channel> findInactiveChannelsBeforeDate(@Param("date") LocalDateTime date);

    // 关键字搜索查询

    /**
     * 根据关键字搜索渠道（支持分页）
     * 搜索范围：渠道名称、代码、描述、联系人姓名
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 渠道分页结果
     */
    @Query("SELECT c FROM Channel c WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Channel> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 复合条件查询渠道
     * @param status 渠道状态（可为空）
     * @param type 渠道类型（可为空）
     * @param keyword 搜索关键字（可为空）
     * @param pageable 分页参数
     * @return 渠道分页结果
     */
    @Query("SELECT c FROM Channel c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:keyword IS NULL OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Channel> findByStatusAndTypeAndKeyword(
        @Param("status") ChannelStatus status,
        @Param("type") ChannelType type,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    // 统计查询方法

    /**
     * 统计指定状态的渠道数量
     * @param status 渠道状态
     * @return 渠道数量
     */
    long countByStatus(ChannelStatus status);

    /**
     * 统计指定类型的渠道数量
     * @param type 渠道类型
     * @return 渠道数量
     */
    long countByType(ChannelType type);

    /**
     * 统计指定状态和类型的渠道数量
     * @param status 渠道状态
     * @param type 渠道类型
     * @return 渠道数量
     */
    long countByStatusAndType(ChannelStatus status, ChannelType type);

    /**
     * 获取所有活跃渠道的总销售额
     * @return 总销售额
     */
    @Query("SELECT COALESCE(SUM(c.totalSales), 0) FROM Channel c WHERE c.status = 'ACTIVE'")
    BigDecimal getTotalActiveSales();

    /**
     * 获取所有活跃渠道的当月销售额
     * @return 当月总销售额
     */
    @Query("SELECT COALESCE(SUM(c.currentMonthSales), 0) FROM Channel c WHERE c.status = 'ACTIVE'")
    BigDecimal getTotalActiveCurrentMonthSales();

    /**
     * 获取指定类型渠道的总销售额
     * @param type 渠道类型
     * @return 总销售额
     */
    @Query("SELECT COALESCE(SUM(c.totalSales), 0) FROM Channel c WHERE c.type = :type AND c.status = 'ACTIVE'")
    BigDecimal getTotalSalesByType(@Param("type") ChannelType type);

    // 自定义复杂查询

    /**
     * 查找指定日期后有活动的活跃渠道，按总销售额降序排列
     * @param date 日期阈值
     * @return 渠道列表
     */
    @Query("SELECT c FROM Channel c WHERE c.status = 'ACTIVE' " +
           "AND c.lastActivityAt > :date ORDER BY c.totalSales DESC")
    List<Channel> findActiveChannelsAfterDate(@Param("date") LocalDateTime date);

    /**
     * 查找佣金率在指定范围内的渠道
     * @param minRate 最小佣金率
     * @param maxRate 最大佣金率
     * @return 渠道列表
     */
    @Query("SELECT c FROM Channel c WHERE c.commissionRate BETWEEN :minRate AND :maxRate")
    List<Channel> findByCommissionRateBetween(@Param("minRate") BigDecimal minRate,
                                            @Param("maxRate") BigDecimal maxRate);

    /**
     * 查找销售业绩排名前N的渠道
     * @param pageable 分页参数（用于限制数量）
     * @return 渠道列表
     */
    @Query("SELECT c FROM Channel c WHERE c.status = 'ACTIVE' ORDER BY c.currentMonthSales DESC")
    List<Channel> findTopPerformingChannels(Pageable pageable);

    // 批量操作

    /**
     * 批量更新渠道状态
     * @param status 新状态
     * @param ids 渠道ID列表
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE Channel c SET c.status = :status, c.lastActivityAt = CURRENT_TIMESTAMP WHERE c.id IN :ids")
    int updateStatusByIds(@Param("status") ChannelStatus status, @Param("ids") List<Long> ids);

    /**
     * 重置所有渠道的当月销售额
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE Channel c SET c.currentMonthSales = 0, c.lastActivityAt = CURRENT_TIMESTAMP")
    int resetAllCurrentMonthSales();

    /**
     * 重置指定状态渠道的当月销售额
     * @param status 渠道状态
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE Channel c SET c.currentMonthSales = 0, c.lastActivityAt = CURRENT_TIMESTAMP WHERE c.status = :status")
    int resetCurrentMonthSalesByStatus(@Param("status") ChannelStatus status);

    // 存在性检查

    /**
     * 检查渠道代码是否已存在
     * @param code 渠道代码
     * @return 是否存在
     */
    boolean existsByCode(String code);

    /**
     * 检查渠道名称是否已存在
     * @param name 渠道名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查渠道代码是否已被其他渠道使用
     * @param code 渠道代码
     * @param id 排除的渠道ID
     * @return 是否被其他渠道使用
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 检查渠道名称是否已被其他渠道使用
     * @param name 渠道名称
     * @param id 排除的渠道ID
     * @return 是否被其他渠道使用
     */
    boolean existsByNameAndIdNot(String name, Long id);
}