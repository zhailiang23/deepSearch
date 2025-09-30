package com.ynet.mgmt.user.service;

import com.ynet.mgmt.user.dto.*;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.user.entity.UserRole;
import com.ynet.mgmt.user.entity.UserStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 用户业务服务接口
 * 提供用户管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
public interface UserService {

    // ========== 基本CRUD操作 ==========

    /**
     * 创建用户
     * @param request 创建请求
     * @return 创建的用户
     */
    UserDTO createUser(CreateUserRequest request);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 更新用户
     * @param id 用户ID
     * @param request 更新请求
     * @return 更新后的用户
     */
    UserDTO updateUser(Long id, UpdateUserRequest request);

    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户信息
     */
    UserDTO getUser(Long id);

    /**
     * 分页查询用户列表
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<UserDTO> listUsers(Pageable pageable);

    /**
     * 关键字搜索用户（分页）
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<UserDTO> searchUsers(String keyword, Pageable pageable);

    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    List<UserDTO> getAllUsers();

    // ========== 状态相关操作 ==========

    /**
     * 根据状态分页查询用户
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<UserDTO> listUsersByStatus(UserStatus status, Pageable pageable);

    /**
     * 切换用户状态(启用/禁用)
     * @param id 用户ID
     * @return 更新后的用户
     */
    UserDTO toggleStatus(Long id);

    // ========== 角色相关操作 ==========

    /**
     * 根据角色分页查询用户
     * @param role 用户角色
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<UserDTO> listUsersByRole(UserRole role, Pageable pageable);

    // ========== 验证方法 ==========

    /**
     * 检查用户名是否可用
     * @param username 用户名
     * @return 是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查用户名是否可用（排除指定ID）
     * @param username 用户名
     * @param excludeId 排除的用户ID
     * @return 是否可用
     */
    boolean isUsernameAvailable(String username, Long excludeId);

    /**
     * 检查邮箱是否可用
     * @param email 邮箱
     * @return 是否可用
     */
    boolean isEmailAvailable(String email);

    /**
     * 检查邮箱是否可用（排除指定ID）
     * @param email 邮箱
     * @param excludeId 排除的用户ID
     * @return 是否可用
     */
    boolean isEmailAvailable(String email, Long excludeId);

    // ========== 统计方法 ==========

    /**
     * 统计激活的用户数量
     * @return 激活的用户数量
     */
    Long countActiveUsers();

    /**
     * 统计锁定的用户数量
     * @return 锁定的用户数量
     */
    Long countLockedUsers();

    /**
     * 统计指定角色的用户数量
     * @param role 用户角色
     * @return 该角色的用户数量
     */
    Long countByRole(UserRole role);
}