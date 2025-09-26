package com.ynet.mgmt.channel.repository;

import com.ynet.mgmt.channel.entity.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * 根据关键字搜索渠道（支持分页）
     * 搜索范围：渠道名称、代码、描述
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 渠道分页结果
     */
    @Query("SELECT c FROM Channel c WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Channel> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

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