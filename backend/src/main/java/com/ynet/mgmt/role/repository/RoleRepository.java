package com.ynet.mgmt.role.repository;

import com.ynet.mgmt.role.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色数据访问接口
 *
 * @author Claude
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    // ========== 基础查询方法 ==========

    /**
     * 根据角色代码查询角色
     */
    Optional<Role> findByCode(String code);

    /**
     * 根据角色名称查询角色
     */
    Optional<Role> findByName(String name);

    // ========== 搜索方法 ==========

    /**
     * 根据关键字搜索角色
     * 支持按角色名称、代码、描述进行模糊搜索
     */
    @Query("SELECT r FROM Role r WHERE (:keyword IS NULL OR " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Role> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ========== 存在性检查方法 ==========

    /**
     * 检查角色代码是否存在
     */
    boolean existsByCode(String code);

    /**
     * 检查角色名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查角色代码是否存在（排除指定ID）
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * 检查角色名称是否存在（排除指定ID）
     */
    boolean existsByNameAndIdNot(String name, Long id);
}