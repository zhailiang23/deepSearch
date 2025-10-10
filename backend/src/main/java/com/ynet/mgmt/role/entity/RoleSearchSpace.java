package com.ynet.mgmt.role.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * 角色-搜索空间关联实体
 * 用于管理角色对搜索空间的访问权限
 *
 * @author Claude
 */
@Entity
@Table(name = "role_search_spaces", indexes = {
    @Index(name = "idx_role_id", columnList = "role_id"),
    @Index(name = "idx_search_space_id", columnList = "search_space_id"),
    @Index(name = "unique_role_search_space", columnList = "role_id,search_space_id", unique = true)
})
public class RoleSearchSpace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 角色ID
     */
    @Column(name = "role_id", nullable = false)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 搜索空间ID
     */
    @Column(name = "search_space_id", nullable = false)
    @NotNull(message = "搜索空间ID不能为空")
    private Long searchSpaceId;

    @Version
    private Long version;

    // ========== 构造方法 ==========

    public RoleSearchSpace() {}

    public RoleSearchSpace(Long roleId, Long searchSpaceId) {
        this.roleId = roleId;
        this.searchSpaceId = searchSpaceId;
    }

    // ========== 业务方法 ==========

    /**
     * 检查关联是否有效
     */
    public boolean isValid() {
        return roleId != null && searchSpaceId != null;
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

    public Long getSearchSpaceId() {
        return searchSpaceId;
    }

    public void setSearchSpaceId(Long searchSpaceId) {
        this.searchSpaceId = searchSpaceId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // ========== Object Methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleSearchSpace that = (RoleSearchSpace) o;
        if (roleId != null && that.roleId != null && searchSpaceId != null && that.searchSpaceId != null) {
            return roleId.equals(that.roleId) && searchSpaceId.equals(that.searchSpaceId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = roleId != null ? roleId.hashCode() : 0;
        result = 31 * result + (searchSpaceId != null ? searchSpaceId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RoleSearchSpace{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", searchSpaceId=" + searchSpaceId +
                '}';
    }
}
