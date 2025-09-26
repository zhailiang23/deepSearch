package com.ynet.mgmt.role.mapper;

import com.ynet.mgmt.role.dto.CreateRoleRequest;
import com.ynet.mgmt.role.dto.RoleDTO;
import com.ynet.mgmt.role.dto.UpdateRoleRequest;
import com.ynet.mgmt.role.entity.Role;
import org.springframework.stereotype.Component;

/**
 * 角色映射器
 *
 * @author Claude
 */
@Component
public class RoleMapper {

    /**
     * 实体转DTO
     */
    public RoleDTO toDTO(Role role) {
        if (role == null) {
            return null;
        }

        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setCode(role.getCode());
        dto.setDescription(role.getDescription());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        dto.setCreatedBy(role.getCreatedBy());
        dto.setUpdatedBy(role.getUpdatedBy());
        return dto;
    }

    /**
     * 创建请求转实体
     */
    public Role toEntity(CreateRoleRequest request) {
        if (request == null) {
            return null;
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        return role;
    }

    /**
     * 更新请求应用到实体
     */
    public void updateEntity(Role role, UpdateRoleRequest request) {
        if (role == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
    }
}