package com.ynet.mgmt.role.repository;

import com.ynet.mgmt.role.entity.RoleSearchSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色-搜索空间关联Repository
 *
 * @author Claude
 */
@Repository
public interface RoleSearchSpaceRepository extends JpaRepository<RoleSearchSpace, Long> {

    /**
     * 查询角色的所有搜索空间关联
     *
     * @param roleId 角色ID
     * @return 关联列表
     */
    List<RoleSearchSpace> findByRoleId(Long roleId);

    /**
     * 查询搜索空间的所有角色关联
     *
     * @param searchSpaceId 搜索空间ID
     * @return 关联列表
     */
    List<RoleSearchSpace> findBySearchSpaceId(Long searchSpaceId);

    /**
     * 删除角色的所有关联
     *
     * @param roleId 角色ID
     */
    @Modifying
    @Query("DELETE FROM RoleSearchSpace rss WHERE rss.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除搜索空间的所有关联
     *
     * @param searchSpaceId 搜索空间ID
     */
    @Modifying
    @Query("DELETE FROM RoleSearchSpace rss WHERE rss.searchSpaceId = :searchSpaceId")
    void deleteBySearchSpaceId(@Param("searchSpaceId") Long searchSpaceId);

    /**
     * 删除角色的指定关联（不在列表中的）
     *
     * @param roleId 角色ID
     * @param searchSpaceIds 要保留的搜索空间ID列表
     */
    @Modifying
    @Query("DELETE FROM RoleSearchSpace rss WHERE rss.roleId = :roleId AND rss.searchSpaceId NOT IN :searchSpaceIds")
    void deleteByRoleIdAndSearchSpaceIdNotIn(@Param("roleId") Long roleId, @Param("searchSpaceIds") List<Long> searchSpaceIds);

    /**
     * 检查角色是否有某个搜索空间的权限
     *
     * @param roleId 角色ID
     * @param searchSpaceId 搜索空间ID
     * @return 是否存在关联
     */
    boolean existsByRoleIdAndSearchSpaceId(Long roleId, Long searchSpaceId);

    /**
     * 查询角色关联的所有搜索空间ID
     *
     * @param roleId 角色ID
     * @return 搜索空间ID列表
     */
    @Query("SELECT rss.searchSpaceId FROM RoleSearchSpace rss WHERE rss.roleId = :roleId")
    List<Long> findSearchSpaceIdsByRoleId(@Param("roleId") Long roleId);
}
