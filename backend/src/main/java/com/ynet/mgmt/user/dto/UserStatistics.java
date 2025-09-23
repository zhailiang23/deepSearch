package com.ynet.mgmt.user.dto;

import com.ynet.mgmt.user.entity.UserRole;
import com.ynet.mgmt.user.entity.UserStatus;

/**
 * 用户统计数据类
 * 用于展示用户按角色和状态的统计信息
 */
public class UserStatistics {

    private UserRole role;
    private UserStatus status;
    private Long count;

    public UserStatistics() {}

    public UserStatistics(UserRole role, UserStatus status, Long count) {
        this.role = role;
        this.status = status;
        this.count = count;
    }

    // Getters and Setters
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "UserStatistics{" +
                "role=" + role +
                ", status=" + status +
                ", count=" + count +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserStatistics that = (UserStatistics) o;

        if (role != that.role) return false;
        if (status != that.status) return false;
        return count != null ? count.equals(that.count) : that.count == null;
    }

    @Override
    public int hashCode() {
        int result = role != null ? role.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (count != null ? count.hashCode() : 0);
        return result;
    }
}