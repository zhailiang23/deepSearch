package com.ynet.mgmt.role.service;

import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.role.dto.CreateRoleRequest;
import com.ynet.mgmt.role.dto.RoleDTO;
import com.ynet.mgmt.role.dto.UpdateRoleRequest;
import org.springframework.data.domain.Pageable;

/**
 * 角色管理服务接口
 *
 * @author Claude
 */
public interface RoleService {

    // ========== 基础CRUD操作 ==========

    /**
     * 创建角色
     */
    RoleDTO createRole(CreateRoleRequest request);

    /**
     * 更新角色
     */
    RoleDTO updateRole(Long id, UpdateRoleRequest request);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 根据ID获取角色
     */
    RoleDTO getRoleById(Long id);

    /**
     * 根据代码获取角色
     */
    RoleDTO getRoleByCode(String code);

    // ========== 查询操作 ==========

    /**
     * 分页查询角色列表
     */
    PageResult<RoleDTO> listRoles(Pageable pageable);

    /**
     * 搜索角色
     */
    PageResult<RoleDTO> searchRoles(String keyword, Pageable pageable);

    // ========== 验证操作 ==========

    /**
     * 检查角色代码可用性
     */
    boolean isCodeAvailable(String code, Long excludeId);

    /**
     * 检查角色名称可用性
     */
    boolean isNameAvailable(String name, Long excludeId);

    /**
     * 获取所有角色（不分页）
     */
    java.util.List<RoleDTO> getAllRoles();
}