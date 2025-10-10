package com.ynet.mgmt.role.dto;

import java.time.LocalDateTime;

/**
 * 角色-搜索空间关联DTO
 *
 * @author Claude
 */
public class RoleSearchSpaceDTO {

    private Long id;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private Long searchSpaceId;
    private String searchSpaceName;
    private String searchSpaceCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== 构造方法 ==========

    public RoleSearchSpaceDTO() {}

    public RoleSearchSpaceDTO(Long id, Long roleId, String roleName, String roleCode,
                              Long searchSpaceId, String searchSpaceName, String searchSpaceCode) {
        this.id = id;
        this.roleId = roleId;
        this.roleName = roleName;
        this.roleCode = roleCode;
        this.searchSpaceId = searchSpaceId;
        this.searchSpaceName = searchSpaceName;
        this.searchSpaceCode = searchSpaceCode;
    }

    // ========== Getter & Setter ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Long getSearchSpaceId() {
        return searchSpaceId;
    }

    public void setSearchSpaceId(Long searchSpaceId) {
        this.searchSpaceId = searchSpaceId;
    }

    public String getSearchSpaceName() {
        return searchSpaceName;
    }

    public void setSearchSpaceName(String searchSpaceName) {
        this.searchSpaceName = searchSpaceName;
    }

    public String getSearchSpaceCode() {
        return searchSpaceCode;
    }

    public void setSearchSpaceCode(String searchSpaceCode) {
        this.searchSpaceCode = searchSpaceCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "RoleSearchSpaceDTO{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", roleCode='" + roleCode + '\'' +
                ", searchSpaceId=" + searchSpaceId +
                ", searchSpaceName='" + searchSpaceName + '\'' +
                ", searchSpaceCode='" + searchSpaceCode + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
