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
import com.ynet.mgmt.role.repository.RoleSearchSpaceRepository;
import com.ynet.mgmt.role.entity.RoleSearchSpace;
import com.ynet.mgmt.role.service.RoleService;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.repository.SearchSpaceRepository;
import com.ynet.mgmt.searchspace.mapper.SearchSpaceMapper;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    private final RoleSearchSpaceRepository roleSearchSpaceRepository;
    private final SearchSpaceRepository searchSpaceRepository;
    private final SearchSpaceMapper searchSpaceMapper;

    public RoleServiceImpl(RoleRepository roleRepository,
                          RoleMapper roleMapper,
                          RoleSearchSpaceRepository roleSearchSpaceRepository,
                          SearchSpaceRepository searchSpaceRepository,
                          SearchSpaceMapper searchSpaceMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
        this.roleSearchSpaceRepository = roleSearchSpaceRepository;
        this.searchSpaceRepository = searchSpaceRepository;
        this.searchSpaceMapper = searchSpaceMapper;
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

    // ========== 搜索空间配置操作 ==========

    @Override
    public void configureSearchSpaces(Long roleId, List<Long> searchSpaceIds) {
        logger.info("配置角色搜索空间权限，角色ID：{}，搜索空间数量：{}", roleId, searchSpaceIds.size());

        // 验证角色是否存在
        if (!roleRepository.existsById(roleId)) {
            throw new RoleNotFoundException(roleId);
        }

        // 获取现有的关联
        List<RoleSearchSpace> existingAssociations = roleSearchSpaceRepository.findByRoleId(roleId);
        List<Long> existingSearchSpaceIds = existingAssociations.stream()
                .map(RoleSearchSpace::getSearchSpaceId)
                .toList();

        // 找出需要新增的关联
        List<Long> toAdd = searchSpaceIds.stream()
                .filter(id -> !existingSearchSpaceIds.contains(id))
                .toList();

        // 找出需要删除的关联
        List<Long> toRemove = existingSearchSpaceIds.stream()
                .filter(id -> !searchSpaceIds.contains(id))
                .toList();

        // 删除不再需要的关联
        if (!toRemove.isEmpty()) {
            roleSearchSpaceRepository.deleteByRoleIdAndSearchSpaceIdNotIn(roleId, searchSpaceIds);
            logger.info("删除角色搜索空间关联，数量：{}", toRemove.size());
        }

        // 添加新的关联
        if (!toAdd.isEmpty()) {
            List<RoleSearchSpace> newAssociations = toAdd.stream()
                    .map(searchSpaceId -> new RoleSearchSpace(roleId, searchSpaceId))
                    .toList();
            roleSearchSpaceRepository.saveAll(newAssociations);
            logger.info("添加角色搜索空间关联，数量：{}", toAdd.size());
        }

        logger.info("角色搜索空间权限配置完成，角色ID：{}", roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSpaceDTO> getRoleSearchSpaces(Long roleId) {
        logger.info("获取角色关联的搜索空间，角色ID：{}", roleId);

        // 验证角色是否存在
        if (!roleRepository.existsById(roleId)) {
            throw new RoleNotFoundException(roleId);
        }

        // 获取角色关联的搜索空间ID列表
        List<Long> searchSpaceIds = roleSearchSpaceRepository.findSearchSpaceIdsByRoleId(roleId);

        if (searchSpaceIds.isEmpty()) {
            logger.info("角色未关联任何搜索空间，角色ID：{}", roleId);
            return List.of();
        }

        // 查询搜索空间详情
        List<SearchSpace> searchSpaces = searchSpaceRepository.findAllById(searchSpaceIds);

        logger.info("查询到角色关联的搜索空间数量：{}，角色ID：{}", searchSpaces.size(), roleId);

        return searchSpaces.stream()
                .map(searchSpaceMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSpaceDTO> getAvailableSearchSpaces(Long roleId) {
        logger.info("获取可配置的搜索空间列表，角色ID：{}", roleId);

        // 获取所有活跃的搜索空间
        List<SearchSpace> allSearchSpaces = searchSpaceRepository.findAll();

        logger.info("查询到可配置的搜索空间数量：{}", allSearchSpaces.size());

        return allSearchSpaces.stream()
                .map(searchSpaceMapper::toDTO)
                .toList();
    }
}