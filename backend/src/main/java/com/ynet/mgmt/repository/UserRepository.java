package com.ynet.mgmt.repository;

import com.ynet.mgmt.entity.User;
import com.ynet.mgmt.entity.UserRole;
import com.ynet.mgmt.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户Repository接口
 * 提供用户实体的数据访问方法
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>,
                                      JpaSpecificationExecutor<User>,
                                      UserRepositoryCustom {

    // 基础查询方法

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱地址
     * @return 用户对象Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户
     * @param username 用户名
     * @param email 邮箱地址
     * @return 用户对象Optional
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    // 状态和角色查询

    /**
     * 根据用户状态查找用户列表
     * @param status 用户状态
     * @return 用户列表
     */
    List<User> findByStatus(UserStatus status);

    /**
     * 根据用户角色查找用户列表
     * @param role 用户角色
     * @return 用户列表
     */
    List<User> findByRole(UserRole role);

    /**
     * 根据状态和角色查找用户分页列表
     * @param status 用户状态
     * @param role 用户角色
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByStatusAndRole(UserStatus status, UserRole role, Pageable pageable);

    // 关键字搜索查询

    /**
     * 根据关键字搜索用户（支持分页）
     * 搜索范围：用户名、邮箱、姓名
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 复合条件查询用户
     * @param status 用户状态（可为空）
     * @param role 用户角色（可为空）
     * @param keyword 搜索关键字（可为空）
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:keyword IS NULL OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findByStatusAndRoleAndKeyword(
        @Param("status") UserStatus status,
        @Param("role") UserRole role,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    // 统计查询

    /**
     * 统计指定状态的用户数量
     * @param status 用户状态
     * @return 用户数量
     */
    long countByStatus(UserStatus status);

    /**
     * 统计指定角色的用户数量
     * @param role 用户角色
     * @return 用户数量
     */
    long countByRole(UserRole role);

    /**
     * 统计指定状态和角色的用户数量
     * @param status 用户状态
     * @param role 用户角色
     * @return 用户数量
     */
    long countByStatusAndRole(UserStatus status, UserRole role);

    // 时间范围查询

    /**
     * 查找指定时间范围内创建的用户
     * @param start 开始时间
     * @param end 结束时间
     * @return 用户列表
     */
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查找指定时间范围内有登录记录的用户
     * @param start 开始时间
     * @param end 结束时间
     * @return 用户列表
     */
    List<User> findByLastLoginAtBetween(LocalDateTime start, LocalDateTime end);

    // 安全相关查询

    /**
     * 查找登录失败次数超过指定值的用户
     * @param attempts 失败次数阈值
     * @return 用户列表
     */
    List<User> findByFailedLoginAttemptsGreaterThan(Integer attempts);

    /**
     * 查找账号锁定时间在指定时间之后的用户
     * @param now 当前时间
     * @return 用户列表
     */
    List<User> findByAccountLockedUntilAfter(LocalDateTime now);

    // 邮箱验证相关

    /**
     * 根据邮箱验证令牌查找用户
     * @param token 验证令牌
     * @return 用户对象Optional
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * 查找未验证邮箱的用户
     * @return 用户列表
     */
    List<User> findByEmailVerifiedFalse();

    // 批量操作

    /**
     * 批量更新用户状态
     * @param status 新状态
     * @param ids 用户ID列表
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
    int updateStatusByIds(@Param("status") UserStatus status, @Param("ids") List<Long> ids);

    /**
     * 重置指定状态用户的登录失败次数
     * @param status 用户状态
     * @return 影响的记录数
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLockedUntil = null WHERE u.status = :status")
    int resetFailedLoginAttempts(@Param("status") UserStatus status);

    // 存在性检查

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱地址
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户名是否已被其他用户使用
     * @param username 用户名
     * @param id 排除的用户ID
     * @return 是否被其他用户使用
     */
    boolean existsByUsernameAndIdNot(String username, Long id);

    /**
     * 检查邮箱是否已被其他用户使用
     * @param email 邮箱地址
     * @param id 排除的用户ID
     * @return 是否被其他用户使用
     */
    boolean existsByEmailAndIdNot(String email, Long id);
}