package com.ynet.mgmt.user.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 管理系统用户的基本信息、状态和安全相关数据
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "用户名只能包含字母、数字和下划线，长度3-50字符")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank(message = "密码不能为空")
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    @Size(max = 100, message = "姓名长度不能超过100字符")
    private String fullName;

    @Column(name = "phone", length = 20)
    @Pattern(regexp = "^[0-9+\\-\\s()]{0,20}$", message = "电话号码格式不正确")
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "custom_role_id", nullable = false)
    private com.ynet.mgmt.role.entity.Role customRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Version
    private Long version;

    // 构造函数
    public User() {}

    public User(String username, String email, String passwordHash, com.ynet.mgmt.role.entity.Role customRole) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.customRole = customRole;
    }

    // 业务方法

    /**
     * 检查用户是否处于激活状态
     * @return true if 用户状态为ACTIVE
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    /**
     * 检查用户账号是否被锁定
     * @return true if 用户状态为LOCKED或锁定时间未到期
     */
    public boolean isLocked() {
        return UserStatus.LOCKED.equals(this.status) ||
               (accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now()));
    }

    /**
     * 增加登录失败次数
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * 重置登录失败次数和锁定状态
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    /**
     * 锁定用户账号指定时长
     * @param lockDuration 锁定时长
     */
    public void lockAccount(Duration lockDuration) {
        this.status = UserStatus.LOCKED;
        this.accountLockedUntil = LocalDateTime.now().plus(lockDuration);
    }

    /**
     * 更新最后登录信息
     * @param ipAddress 登录IP地址
     */
    public void updateLastLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        resetFailedLoginAttempts();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public com.ynet.mgmt.role.entity.Role getCustomRole() {
        return customRole;
    }

    public void setCustomRole(com.ynet.mgmt.role.entity.Role customRole) {
        this.customRole = customRole;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public LocalDateTime getAccountLockedUntil() {
        return accountLockedUntil;
    }

    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (id != null ? !id.equals(user.id) : user.id != null) return false;
        return username != null ? username.equals(user.username) : user.username == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", customRole=" + (customRole != null ? customRole.getCode() : null) +
                ", status=" + status +
                '}';
    }
}