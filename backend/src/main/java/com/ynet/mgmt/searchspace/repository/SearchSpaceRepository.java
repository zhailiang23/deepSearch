package com.ynet.mgmt.searchspace.repository;

import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 搜索空间Repository接口
 * 提供搜索空间的数据访问方法
 *
 * @author system
 * @since 1.0.0
 */
@Repository
public interface SearchSpaceRepository extends JpaRepository<SearchSpace, Long> {

    /**
     * 根据代码查找搜索空间
     * @param code 搜索空间代码
     * @return 搜索空间实体
     */
    Optional<SearchSpace> findByCode(String code);

    /**
     * 检查代码是否存在
     * @param code 搜索空间代码
     * @return true if 代码已存在
     */
    boolean existsByCode(String code);

    /**
     * 根据名称查找搜索空间
     * @param name 搜索空间名称
     * @return 搜索空间实体
     */
    Optional<SearchSpace> findByName(String name);

    /**
     * 检查名称是否存在
     * @param name 搜索空间名称
     * @return true if 名称已存在
     */
    boolean existsByName(String name);

    /**
     * 按名称或代码模糊查询
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT s FROM SearchSpace s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SearchSpace> findByKeyword(@Param("keyword") String keyword, Pageable pageable);



    /**
     * 根据状态查询搜索空间
     * @param status 搜索空间状态
     * @return 指定状态的搜索空间列表
     */
    List<SearchSpace> findByStatus(SearchSpaceStatus status);

    /**
     * 根据状态分页查询搜索空间
     * @param status 搜索空间状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SearchSpace> findByStatus(SearchSpaceStatus status, Pageable pageable);

    /**
     * 查询活跃状态的搜索空间
     * @return 活跃状态的搜索空间列表
     */
    default List<SearchSpace> findActiveSearchSpaces() {
        return findByStatus(SearchSpaceStatus.ACTIVE);
    }

    /**
     * 分页查询活跃状态的搜索空间
     * @param pageable 分页参数
     * @return 分页结果
     */
    default Page<SearchSpace> findActiveSearchSpaces(Pageable pageable) {
        return findByStatus(SearchSpaceStatus.ACTIVE, pageable);
    }

    /**
     * 查询可搜索的搜索空间（活跃和维护状态）
     * @return 可搜索的搜索空间列表
     */
    @Query("SELECT s FROM SearchSpace s WHERE s.status IN ('ACTIVE', 'MAINTENANCE')")
    List<SearchSpace> findSearchableSpaces();

    /**
     * 分页查询可搜索的搜索空间
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT s FROM SearchSpace s WHERE s.status IN ('ACTIVE', 'MAINTENANCE')")
    Page<SearchSpace> findSearchableSpaces(Pageable pageable);

    /**
     * 统计总数
     * @return 搜索空间总数
     */
    @Query("SELECT COUNT(s) FROM SearchSpace s")
    long countAll();

    /**
     * 按状态统计数量
     * @param status 搜索空间状态
     * @return 指定状态的搜索空间数量
     */
    long countByStatus(SearchSpaceStatus status);


    /**
     * 根据名称或代码模糊查询（排除指定ID）
     * 用于更新时检查重复
     * @param keyword 搜索关键词
     * @param excludeId 排除的ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT s FROM SearchSpace s WHERE " +
           "s.id != :excludeId AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SearchSpace> findByKeywordExcludingId(@Param("keyword") String keyword,
                                               @Param("excludeId") Long excludeId,
                                               Pageable pageable);

    /**
     * 检查代码是否存在（排除指定ID）
     * @param code 搜索空间代码
     * @param excludeId 排除的ID
     * @return true if 代码已存在
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SearchSpace s WHERE s.code = :code AND s.id != :excludeId")
    boolean existsByCodeExcludingId(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * 检查名称是否存在（排除指定ID）
     * @param name 搜索空间名称
     * @param excludeId 排除的ID
     * @return true if 名称已存在
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SearchSpace s WHERE s.name = :name AND s.id != :excludeId")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("excludeId") Long excludeId);

    // ========== 新增的JSON导入相关查询方法 ==========

    /**
     * 查找有索引映射配置的搜索空间
     * @return 有映射配置的搜索空间列表
     */
    @Query("SELECT s FROM SearchSpace s WHERE s.indexMapping IS NOT NULL AND s.indexMapping != ''")
    List<SearchSpace> findAllWithIndexMapping();

    /**
     * 查找有导入文档的搜索空间
     * @return 有导入文档的搜索空间列表
     */
    @Query("SELECT s FROM SearchSpace s WHERE s.documentCount > 0")
    List<SearchSpace> findAllWithImportedDocuments();

    /**
     * 统计总导入文档数量
     * @return 所有搜索空间的文档总数
     */
    @Query("SELECT COALESCE(SUM(s.documentCount), 0) FROM SearchSpace s")
    Long countTotalImportedDocuments();

    /**
     * 查找最近导入的搜索空间
     * @param pageable 分页参数
     * @return 最近导入的搜索空间列表
     */
    @Query("SELECT s FROM SearchSpace s WHERE s.lastImportTime IS NOT NULL ORDER BY s.lastImportTime DESC")
    List<SearchSpace> findRecentlyImported(Pageable pageable);

    /**
     * 统计有索引映射配置的搜索空间数量
     * @return 有映射配置的搜索空间数量
     */
    @Query("SELECT COUNT(s) FROM SearchSpace s WHERE s.indexMapping IS NOT NULL AND s.indexMapping != ''")
    Long countSpacesWithIndexMapping();

    /**
     * 统计有导入文档的搜索空间数量
     * @return 有导入文档的搜索空间数量
     */
    @Query("SELECT COUNT(s) FROM SearchSpace s WHERE s.documentCount > 0")
    Long countSpacesWithImportedDocuments();

    /**
     * 查找最后一次导入时间
     * @return 最后一次导入时间
     */
    @Query("SELECT MAX(s.lastImportTime) FROM SearchSpace s WHERE s.lastImportTime IS NOT NULL")
    Optional<java.time.LocalDateTime> findLastImportTime();
}