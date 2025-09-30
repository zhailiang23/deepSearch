package com.ynet.mgmt.user.dto;

import com.ynet.mgmt.user.entity.UserRole;
import com.ynet.mgmt.user.entity.UserStatus;
import jakarta.validation.constraints.*;

/**
 * 创建用户请求DTO
 *
 * @author system
 * @since 1.0.0
 */
public class CreateUserRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "用户名只能包含字母、数字和下划线,长度3-50字符")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50字符之间")
    private String password;

    /**
     * 姓名
     */
    @Size(max = 100, message = "姓名长度不能超过100字符")
    private String fullName;

    /**
     * 电话
     */
    @Pattern(regexp = "^[0-9+\\-\\s()]{0,20}$", message = "电话号码格式不正确")
    private String phone;

    /**
     * 角色
     */
    @NotNull(message = "角色不能为空")
    private UserRole role;

    /**
     * 状态
     */
    @NotNull(message = "状态不能为空")
    private UserStatus status;

    // 构造函数
    public CreateUserRequest() {}

    public CreateUserRequest(String username, String email, String password, UserRole role, UserStatus status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

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

    @Override
    public String toString() {
        return "CreateUserRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}