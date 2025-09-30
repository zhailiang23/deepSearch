package com.ynet.mgmt.role.service.impl;

import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.role.dto.CreateRoleRequest;
import com.ynet.mgmt.role.dto.RoleDTO;
import com.ynet.mgmt.role.dto.UpdateRoleRequest;
import com.ynet.mgmt.role.entity.Role;
import com.ynet.mgmt.role.exception.DuplicateRoleException;
import com.ynet.mgmt.role.exception.RoleNotFoundException;
import com.ynet.mgmt.role.mapper.RoleMapper;
import com.ynet.mgmt.role.repository.RoleRepository;
import com.ynet.mgmt.role.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色管理服务实现类
 *
 * @author Claude
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    // ========== 基础CRUD操作 ==========

    @Override
    public RoleDTO createRole(CreateRoleRequest request) {
        logger.info("创建角色：{}", request.getName());

        // 验证代码唯一性
        if (roleRepository.existsByCode(request.getCode())) {
            throw new DuplicateRoleException("代码", request.getCode());
        }

        // 验证名称唯一性
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateRoleException("名称", request.getName());
        }

        // 创建实体
        Role role = roleMapper.toEntity(request);
        Role savedRole = roleRepository.save(role);

        logger.info("角色创建成功，ID：{}", savedRole.getId());
        return roleMapper.toDTO(savedRole);
    }

    @Override
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        logger.info("更新角色，ID：{}", id);

        // 查找角色
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        // 验证名称唯一性（如果要更新名称）
        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new DuplicateRoleException("名称", request.getName());
            }
        }

        // 更新实体
        roleMapper.updateEntity(role, request);
        Role savedRole = roleRepository.save(role);

        logger.info("角色更新成功，ID：{}", savedRole.getId());
        return roleMapper.toDTO(savedRole);
    }

    @Override
    public void deleteRole(Long id) {
        logger.info("删除角色，ID：{}", id);

        // 验证角色是否存在
        if (!roleRepository.existsById(id)) {
            throw new RoleNotFoundException(id);
        }

        roleRepository.deleteById(id);
        logger.info("角色删除成功，ID：{}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return roleMapper.toDTO(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDTO getRoleByCode(String code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new RoleNotFoundException("代码", code));
        return roleMapper.toDTO(role);
    }

    // ========== 查询操作 ==========

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleDTO> listRoles(Pageable pageable) {
        Page<Role> rolePage = roleRepository.findAll(pageable);

        return PageResult.<RoleDTO>builder()
                .content(rolePage.getContent().stream()
                        .map(roleMapper::toDTO)
                        .toList())
                .page(rolePage.getNumber())
                .size(rolePage.getSize())
                .totalElements(rolePage.getTotalElements())
                .totalPages(rolePage.getTotalPages())
                .first(rolePage.isFirst())
                .last(rolePage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleDTO> searchRoles(String keyword, Pageable pageable) {
        Page<Role> rolePage = roleRepository.findByKeyword(keyword, pageable);

        return PageResult.<RoleDTO>builder()
                .content(rolePage.getContent().stream()
                        .map(roleMapper::toDTO)
                        .toList())
                .page(rolePage.getNumber())
                .size(rolePage.getSize())
                .totalElements(rolePage.getTotalElements())
                .totalPages(rolePage.getTotalPages())
                .first(rolePage.isFirst())
                .last(rolePage.isLast())
                .build();
    }

    // ========== 验证操作 ==========

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code, Long excludeId) {
        if (excludeId == null) {
            return !roleRepository.existsByCode(code);
        }
        return !roleRepository.existsByCodeAndIdNot(code, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name, Long excludeId) {
        if (excludeId == null) {
            return !roleRepository.existsByName(name);
        }
        return !roleRepository.existsByNameAndIdNot(name, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<RoleDTO> getAllRoles() {
        logger.info("获取所有角色列表");
        return roleRepository.findAll().stream()
                .map(roleMapper::toDTO)
                .toList();
    }
}