package com.ynet.mgmt.repository;

import com.ynet.mgmt.dto.UserSearchCriteria;
import com.ynet.mgmt.dto.UserStatistics;
import com.ynet.mgmt.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.util.List;

/**
 * 用户Repository自定义接口
 * 定义复杂查询和统计方法
 */
public interface UserRepositoryCustom {

    /**
     * 根据复杂条件查询用户
     * 支持关键字、状态、角色、创建时间等多维度查询
     * @param criteria 查询条件
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<User> findByComplexCriteria(UserSearchCriteria criteria, Pageable pageable);

    /**
     * 获取用户统计数据
     * 按角色和状态分组统计用户数量
     * @return 统计结果列表
     */
    List<UserStatistics> getUserStatisticsByRole();

    /**
     * 查找指定时间内有登录记录的活跃用户
     * @param duration 时间范围
     * @return 活跃用户列表
     */
    List<User> findActiveUsersWithRecentLogin(Duration duration);

    /**
     * 获取最近注册的用户列表
     * @param limit 限制数量
     * @return 最近注册的用户
     */
    List<User> findRecentlyRegisteredUsers(int limit);

    /**
     * 查找需要密码重置的用户
     * 密码超过指定天数未更改的用户
     * @param days 天数阈值
     * @return 需要密码重置的用户列表
     */
    List<User> findUsersRequiringPasswordReset(int days);
}