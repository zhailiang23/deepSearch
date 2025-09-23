package com.ynet.mgmt.auth.dto;

import com.ynet.mgmt.user.entity.UserRole;

/**
 * 登录响应DTO
 */
public class LoginResponse {

    private String token;
    private String refreshToken;
    private UserInfo user;

    // 构造函数
    public LoginResponse() {}

    public LoginResponse(String token, String refreshToken, UserInfo user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    /**
     * 用户信息内嵌类
     */
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private UserRole role;

        // 构造函数
        public UserInfo() {}

        public UserInfo(Long id, String username, String email, String fullName, UserRole role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
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

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }
}