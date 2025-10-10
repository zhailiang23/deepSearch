package com.ynet.mgmt.user.dto;

import com.ynet.mgmt.user.entity.UserStatus;

/**
 * 用户统计数据类
 * 用于展示用户按自定义角色和状态的统计信息
 */
public class UserStatistics {

    private Long roleId;
    private String roleName;
    private UserStatus status;
    private Long count;

    public UserStatistics() {}

    public UserStatistics(Long roleId, String roleName, UserStatus status, Long count) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.status = status;
        this.count = count;
    }

    // Getters and Setters
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
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
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", status=" + status +
                ", count=" + count +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserStatistics that = (UserStatistics) o;

        if (roleId != null ? !roleId.equals(that.roleId) : that.roleId != null) return false;
        if (roleName != null ? !roleName.equals(that.roleName) : that.roleName != null) return false;
        if (status != that.status) return false;
        return count != null ? count.equals(that.count) : that.count == null;
    }

    @Override
    public int hashCode() {
        int result = roleId != null ? roleId.hashCode() : 0;
        result = 31 * result + (roleName != null ? roleName.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (count != null ? count.hashCode() : 0);
        return result;
    }
}