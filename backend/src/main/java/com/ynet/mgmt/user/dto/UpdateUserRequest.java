package com.ynet.mgmt.user.dto;

import com.ynet.mgmt.user.entity.UserStatus;
import jakarta.validation.constraints.*;

/**
 * 更新用户请求DTO
 *
 * @author system
 * @since 1.0.0
 */
public class UpdateUserRequest {

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

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
     * 自定义角色ID
     */
    private Long customRoleId;

    /**
     * 状态
     */
    private UserStatus status;

    // 构造函数
    public UpdateUserRequest() {}

    public UpdateUserRequest(String email, Long customRoleId, UserStatus status) {
        this.email = email;
        this.customRoleId = customRoleId;
        this.status = status;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Long getCustomRoleId() {
        return customRoleId;
    }

    public void setCustomRoleId(Long customRoleId) {
        this.customRoleId = customRoleId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UpdateUserRequest{" +
                "email='" + email + '\'' +
                ", customRoleId=" + customRoleId +
                ", status=" + status +
                '}';
    }
}