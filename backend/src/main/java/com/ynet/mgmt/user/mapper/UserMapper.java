package com.ynet.mgmt.user.mapper;

import com.ynet.mgmt.user.dto.UserDTO;
import com.ynet.mgmt.user.dto.CreateUserRequest;
import com.ynet.mgmt.user.dto.UpdateUserRequest;
import com.ynet.mgmt.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户实体与DTO转换器
 * 提供用户实体与数据传输对象之间的转换方法
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class UserMapper {

    /**
     * 实体转DTO
     *
     * @param entity 用户实体
     * @return 用户DTO
     */
    public UserDTO toDTO(User entity) {
        if (entity == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setStatus(entity.getStatus());
        dto.setFailedLoginAttempts(entity.getFailedLoginAttempts());
        dto.setLastLoginAt(entity.getLastLoginAt());
        dto.setLastLoginIp(entity.getLastLoginIp());
        dto.setEmailVerified(entity.getEmailVerified());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        // 设置自定义角色信息
        if (entity.getCustomRole() != null) {
            dto.setCustomRoleId(entity.getCustomRole().getId());
            dto.setCustomRoleName(entity.getCustomRole().getName());
            dto.setCustomRoleCode(entity.getCustomRole().getCode());
        }

        return dto;
    }

    /**
     * 实体列表转DTO列表
     *
     * @param entities 用户实体列表
     * @return 用户DTO列表
     */
    public List<UserDTO> toDTOList(List<User> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建请求转实体
     * 注意:密码需要在service层加密后设置
     *
     * @param request 创建请求
     * @return 用户实体
     */
    public User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        User entity = new User();
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        // 密码会在service层处理
        entity.setFullName(request.getFullName());
        entity.setPhone(request.getPhone());
        entity.setStatus(request.getStatus());
        // customRole会在service层处理

        return entity;
    }

    /**
     * 更新实体字段（仅更新非空字段）
     *
     * @param request 更新请求
     * @param target 目标实体
     */
    public void updateEntity(UpdateUserRequest request, User target) {
        if (request == null || target == null) {
            return;
        }

        if (request.getEmail() != null) {
            target.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            target.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            target.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            target.setStatus(request.getStatus());
        }
        // customRoleId会在service层处理
    }
}