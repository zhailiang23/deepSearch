package com.ynet.mgmt.user.entity;

/**
 * 用户状态枚举
 * 定义用户账号的不同状态
 */
public enum UserStatus {
    /**
     * 激活状态 - 用户账号正常可用
     */
    ACTIVE("激活", "用户账号正常可用"),

    /**
     * 禁用状态 - 用户账号被管理员禁用
     */
    DISABLED("禁用", "用户账号被管理员禁用"),

    /**
     * 锁定状态 - 用户账号因多次登录失败被锁定
     */
    LOCKED("锁定", "用户账号因多次登录失败被锁定"),

    /**
     * 待验证状态 - 用户注册后等待邮箱验证
     */
    PENDING("待验证", "用户注册后等待邮箱验证");

    private final String displayName;
    private final String description;

    UserStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 获取状态显示名称
     * @return 状态的中文显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取状态描述
     * @return 状态的详细描述
     */
    public String getDescription() {
        return description;
    }
}