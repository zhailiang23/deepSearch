package com.ynet.mgmt.user.service.impl;

import com.ynet.mgmt.user.dto.*;
import com.ynet.mgmt.user.entity.User;
import com.ynet.mgmt.user.entity.UserStatus;
import com.ynet.mgmt.user.exception.DuplicateUserException;
import com.ynet.mgmt.user.exception.UserNotFoundException;
import com.ynet.mgmt.user.mapper.UserMapper;
import com.ynet.mgmt.user.repository.UserRepository;
import com.ynet.mgmt.user.service.UserService;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.role.repository.RoleRepository;
import com.ynet.mgmt.role.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户业务服务实现类
 * 实现用户管理的核心业务逻辑
 *
 * @author system
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public UserServiceImpl(UserRepository repository,
                          UserMapper mapper,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    // ========== 基本CRUD操作 ==========

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        logger.info("创建用户: username={}, email={}", request.getUsername(), request.getEmail());

        // 检查用户名是否已存在
        if (repository.existsByUsername(request.getUsername())) {
            logger.warn("用户名已存在: username={}", request.getUsername());
            throw DuplicateUserException.byUsername(request.getUsername());
        }

        // 检查邮箱是否已存在
        if (repository.existsByEmail(request.getEmail())) {
            logger.warn("邮箱已存在: email={}", request.getEmail());
            throw DuplicateUserException.byEmail(request.getEmail());
        }

        // 转换并设置加密密码
        User entity = mapper.toEntity(request);
        entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // 设置自定义角色(必须)
        Role customRole = roleRepository.findById(request.getCustomRoleId())
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: id=" + request.getCustomRoleId()));
        entity.setCustomRole(customRole);
        logger.debug("设置用户自定义角色: roleId={}, roleName={}", customRole.getId(), customRole.getName());

        User saved = repository.save(entity);

        logger.info("用户创建成功: id={}, username={}", saved.getId(), saved.getUsername());

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        logger.info("删除用户: id={}", id);

        if (!repository.existsById(id)) {
            logger.warn("用户不存在: id={}", id);
            throw new UserNotFoundException(id);
        }

        repository.deleteById(id);
        logger.info("用户删除成功: id={}", id);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        logger.info("更新用户: id={}", id);

        // 查找用户
        User entity = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // 如果更新邮箱,检查是否与其他用户重复
        if (request.getEmail() != null && !request.getEmail().equals(entity.getEmail())) {
            if (repository.existsByEmailAndIdNot(request.getEmail(), id)) {
                logger.warn("邮箱已被其他用户使用: email={}", request.getEmail());
                throw DuplicateUserException.byEmail(request.getEmail());
            }
        }

        // 更新字段
        mapper.updateEntity(request, entity);

        // 更新自定义角色
        if (request.getCustomRoleId() != null) {
            Role customRole = roleRepository.findById(request.getCustomRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("角色不存在: id=" + request.getCustomRoleId()));
            entity.setCustomRole(customRole);
            logger.debug("更新用户自定义角色: roleId={}, roleName={}", customRole.getId(), customRole.getName());
        }

        User updated = repository.save(entity);

        logger.info("用户更新成功: id={}, username={}", updated.getId(), updated.getUsername());

        return mapper.toDTO(updated);
    }

    @Override
    public UserDTO getUser(Long id) {
        logger.debug("根据ID查询用户: id={}", id);

        User entity = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return mapper.toDTO(entity);
    }

    @Override
    public PageResult<UserDTO> listUsers(Pageable pageable) {
        logger.debug("分页查询用户列表: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<User> page = repository.findAll(pageable);
        List<UserDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<UserDTO>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public PageResult<UserDTO> searchUsers(String keyword, Pageable pageable) {
        logger.debug("搜索用户: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> page = repository.findByKeyword(keyword, pageable);
        List<UserDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<UserDTO>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public List<UserDTO> getAllUsers() {
        logger.debug("查询所有用户列表");

        List<User> users = repository.findAll();
        return mapper.toDTOList(users);
    }

    // ========== 状态相关操作 ==========

    @Override
    public PageResult<UserDTO> listUsersByStatus(UserStatus status, Pageable pageable) {
        logger.debug("根据状态查询用户: status={}, page={}, size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> page = repository.findAll(
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("status"), status),
                pageable
        );
        List<UserDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<UserDTO>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public UserDTO toggleStatus(Long id) {
        logger.info("切换用户状态: id={}", id);

        User entity = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // 切换状态
        if (UserStatus.ACTIVE.equals(entity.getStatus())) {
            entity.setStatus(UserStatus.DISABLED);
        } else {
            entity.setStatus(UserStatus.ACTIVE);
        }

        User updated = repository.save(entity);

        logger.info("用户状态切换成功: id={}, status={}", id, updated.getStatus());

        return mapper.toDTO(updated);
    }

    // ========== 验证方法 ==========

    @Override
    public boolean isUsernameAvailable(String username) {
        return !repository.existsByUsername(username);
    }

    @Override
    public boolean isUsernameAvailable(String username, Long excludeId) {
        return !repository.existsByUsernameAndIdNot(username, excludeId);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return !repository.existsByEmail(email);
    }

    @Override
    public boolean isEmailAvailable(String email, Long excludeId) {
        return !repository.existsByEmailAndIdNot(email, excludeId);
    }

    // ========== 统计方法 ==========

    @Override
    public Long countActiveUsers() {
        return repository.countByStatus(UserStatus.ACTIVE);
    }

    @Override
    public Long countLockedUsers() {
        return repository.countByStatus(UserStatus.LOCKED);
    }

    @Override
    public List<UserStatistics> getUserStatisticsByRole() {
        return repository.getUserStatisticsByRole();
    }
}