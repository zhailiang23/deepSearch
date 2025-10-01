package com.ynet.mgmt.hotTopic.repository;

import com.ynet.mgmt.hotTopic.entity.HotTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 热门话题Repository接口
 * 提供热门话题实体的数据访问方法
 *
 * @author system
 * @since 1.0.0
 */
@Repository
public interface HotTopicRepository extends JpaRepository<HotTopic, Long>,
                                           JpaSpecificationExecutor<HotTopic> {

    /**
     * 根据话题名称查找话题
     * @param name 话题名称
     * @return 话题对象Optional
     */
    Optional<HotTopic> findByName(String name);

    /**
     * 根据关键字搜索话题（支持分页）
     * 搜索范围：话题名称
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 话题分页结果
     */
    @Query("SELECT h FROM HotTopic h WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<HotTopic> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据可见性查找话题（支持分页）
     * @param visible 是否可见
     * @param pageable 分页参数
     * @return 话题分页结果
     */
    Page<HotTopic> findByVisible(Boolean visible, Pageable pageable);

    /**
     * 查找可见的热门话题，按热度降序排列
     * @param pageable 分页参数
     * @return 话题分页结果
     */
    @Query("SELECT h FROM HotTopic h WHERE h.visible = true ORDER BY h.popularity DESC")
    Page<HotTopic> findVisibleTopicsOrderByPopularityDesc(Pageable pageable);

    /**
     * 查找热度大于指定值的话题
     * @param minPopularity 最小热度值
     * @param pageable 分页参数
     * @return 话题分页结果
     */
    @Query("SELECT h FROM HotTopic h WHERE h.popularity >= :minPopularity ORDER BY h.popularity DESC")
    Page<HotTopic> findByPopularityGreaterThanEqual(@Param("minPopularity") Integer minPopularity, Pageable pageable);

    /**
     * 获取热度排行榜前N位
     * @param limit 数量限制
     * @return 话题列表
     */
    @Query("SELECT h FROM HotTopic h WHERE h.visible = true ORDER BY h.popularity DESC")
    List<HotTopic> findTopPopularTopics(Pageable pageable);

    /**
     * 检查话题名称是否已存在
     * @param name 话题名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查话题名称是否已被其他话题使用
     * @param name 话题名称
     * @param id 排除的话题ID
     * @return 是否被其他话题使用
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * 统计可见话题数量
     * @return 可见话题数量
     */
    @Query("SELECT COUNT(h) FROM HotTopic h WHERE h.visible = true")
    Long countVisibleTopics();

    /**
     * 统计不可见话题数量
     * @return 不可见话题数量
     */
    @Query("SELECT COUNT(h) FROM HotTopic h WHERE h.visible = false")
    Long countInvisibleTopics();

    /**
     * 获取平均热度
     * @return 平均热度值
     */
    @Query("SELECT AVG(h.popularity) FROM HotTopic h WHERE h.visible = true")
    Double getAveragePopularity();

    /**
     * 查找可见的热门话题，按热度降序排列（返回List）
     * @return 话题列表
     */
    List<HotTopic> findByVisibleTrueOrderByPopularityDesc();
}