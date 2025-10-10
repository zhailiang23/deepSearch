package com.ynet.mgmt.user.dto;

import com.ynet.mgmt.user.entity.UserStatus;
import java.time.LocalDateTime;

/**
 * 用户搜索条件类
 * 用于用户列表查询的动态条件构建
 */
public class UserSearchCriteria {

    private String keyword;
    private UserStatus status;
    private Long customRoleId;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private Boolean emailVerified;

    public UserSearchCriteria() {}

    public UserSearchCriteria(String keyword, UserStatus status, Long customRoleId,
                            LocalDateTime createdAfter, LocalDateTime createdBefore,
                            Boolean emailVerified) {
        this.keyword = keyword;
        this.status = status;
        this.customRoleId = customRoleId;
        this.createdAfter = createdAfter;
        this.createdBefore = createdBefore;
        this.emailVerified = emailVerified;
    }

    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String keyword;
        private UserStatus status;
        private Long customRoleId;
        private LocalDateTime createdAfter;
        private LocalDateTime createdBefore;
        private Boolean emailVerified;

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder customRoleId(Long customRoleId) {
            this.customRoleId = customRoleId;
            return this;
        }

        public Builder createdAfter(LocalDateTime createdAfter) {
            this.createdAfter = createdAfter;
            return this;
        }

        public Builder createdBefore(LocalDateTime createdBefore) {
            this.createdBefore = createdBefore;
            return this;
        }

        public Builder emailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserSearchCriteria build() {
            return new UserSearchCriteria(keyword, status, customRoleId, createdAfter, createdBefore, emailVerified);
        }
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Long getCustomRoleId() {
        return customRoleId;
    }

    public void setCustomRoleId(Long customRoleId) {
        this.customRoleId = customRoleId;
    }

    public LocalDateTime getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(LocalDateTime createdAfter) {
        this.createdAfter = createdAfter;
    }

    public LocalDateTime getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(LocalDateTime createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public String toString() {
        return "UserSearchCriteria{" +
                "keyword='" + keyword + '\'' +
                ", status=" + status +
                ", customRoleId=" + customRoleId +
                ", createdAfter=" + createdAfter +
                ", createdBefore=" + createdBefore +
                ", emailVerified=" + emailVerified +
                '}';
    }
}