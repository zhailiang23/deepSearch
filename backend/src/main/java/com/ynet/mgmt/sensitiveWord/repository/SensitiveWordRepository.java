package com.ynet.mgmt.sensitiveWord.repository;

import com.ynet.mgmt.sensitiveWord.entity.SensitiveWord;
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
 * 敏感词Repository接口
 * 提供敏感词实体的数据访问方法
 *
 * @author system
 * @since 1.0.0
 */
@Repository
public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long>,
                                                 JpaSpecificationExecutor<SensitiveWord> {

    /**
     * 根据敏感词名称查找敏感词
     * @param name 敏感词名称
     * @return 敏感词对象Optional
     */
    Optional<SensitiveWord> findByName(String name);

    /**
     * 根据关键字搜索敏感词（支持分页）
     * 搜索范围：敏感词名称
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 敏感词分页结果
     */
    @Query("SELECT s FROM SensitiveWord s WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SensitiveWord> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据启用状态查找敏感词（支持分页）
     * @param enabled 是否启用
     * @param pageable 分页参数
     * @return 敏感词分页结果
     */
    Page<SensitiveWord> findByEnabled(Boolean enabled, Pageable pageable);

    /**
     * 根据危害等级查找敏感词（支持分页）
     * @param harmLevel 危害等级
     * @param pageable 分页参数
     * @return 敏感词分页结果
     */
    Page<SensitiveWord> findByHarmLevel(Integer harmLevel, Pageable pageable);

    /**
     * 根据启用状态和危害等级查找敏感词（支持分页）
     * @param enabled 是否启用
     * @param harmLevel 危害等级
     * @param pageable 分页参数
     * @return 敏感词分页结果
     */
    Page<SensitiveWord> findByEnabledAndHarmLevel(Boolean enabled, Integer harmLevel, Pageable pageable);

    /**
     * 查找危害等级大于等于指定值的敏感词
     * @param minHarmLevel 最小危害等级
     * @param pageable 分页参数
     * @return 敏感词分页结果
     */
    @Query("SELECT s FROM SensitiveWord s WHERE s.harmLevel >= :minHarmLevel ORDER BY s.harmLevel DESC")
    Page<SensitiveWord> findByHarmLevelGreaterThanEqual(@Param("minHarmLevel") Integer minHarmLevel, Pageable pageable);

    /**
     * 获取所有启用的敏感词，按危害等级降序排列
     * @param pageable 分页参数
     * @return 敏感词分页结果
     */
    @Query("SELECT s FROM SensitiveWord s WHERE s.enabled = true ORDER BY s.harmLevel DESC")
    Page<SensitiveWord> findEnabledWordsOrderByHarmLevelDesc(Pageable pageable);

    /**
     * 检查敏感词名称是否已存在
     * @param name 敏感词名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查敏感词名称是否已被其他敏感词使用
     * @param name 敏感词名称
     * @param id 排除的敏感词ID
     * @return 是否被其他敏感词使用
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * 统计启用的敏感词数量
     * @return 启用的敏感词数量
     */
    @Query("SELECT COUNT(s) FROM SensitiveWord s WHERE s.enabled = true")
    Long countEnabledWords();

    /**
     * 统计禁用的敏感词数量
     * @return 禁用的敏感词数量
     */
    @Query("SELECT COUNT(s) FROM SensitiveWord s WHERE s.enabled = false")
    Long countDisabledWords();

    /**
     * 统计指定危害等级的敏感词数量
     * @param harmLevel 危害等级
     * @return 该等级的敏感词数量
     */
    Long countByHarmLevel(Integer harmLevel);

    /**
     * 获取各危害等级的敏感词数量分布
     * @return 等级分布列表
     */
    @Query("SELECT s.harmLevel, COUNT(s) FROM SensitiveWord s GROUP BY s.harmLevel ORDER BY s.harmLevel")
    List<Object[]> getHarmLevelDistribution();
}