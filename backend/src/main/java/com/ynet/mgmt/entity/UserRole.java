package com.ynet.mgmt.entity;

/**
 * 用户角色枚举
 * 定义系统中用户的不同角色级别
 */
public enum UserRole {
    /**
     * 管理员角色 - 具有系统完整管理权限
     */
    ADMIN("管理员", "ROLE_ADMIN"),

    /**
     * 普通用户角色 - 基础用户权限
     */
    USER("普通用户", "ROLE_USER");

    private final String displayName;
    private final String authority;

    UserRole(String displayName, String authority) {
        this.displayName = displayName;
        this.authority = authority;
    }

    /**
     * 获取角色显示名称
     * @return 角色的中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取Spring Security权限标识
     * @return Spring Security使用的权限字符串
     */
    public String getAuthority() {
        return authority;
    }
}