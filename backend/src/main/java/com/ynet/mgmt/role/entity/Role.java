package com.ynet.mgmt.role.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 角色实体
 *
 * @author Claude
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_code", columnList = "code", unique = true),
    @Index(name = "idx_role_name", columnList = "name")
})
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 角色名称
     */
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 100, message = "角色名称长度必须在2-100个字符之间")
    private String name;

    /**
     * 角色代码
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "角色代码不能为空")
    @Size(min = 2, max = 50, message = "角色代码长度必须在2-50个字符之间")
    private String code;

    /**
     * 角色描述
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "角色描述长度不能超过500个字符")
    private String description;

    // ========== 构造方法 ==========

    public Role() {}

    public Role(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Role(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    // ========== 业务方法 ==========

    /**
     * 检查角色名称是否有效
     */
    public boolean hasValidName() {
        return name != null && !name.trim().isEmpty() && name.length() >= 2;
    }

    /**
     * 检查角色代码是否有效
     */
    public boolean hasValidCode() {
        return code != null && code.matches("^[A-Z][A-Z0-9_]*$");
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (description != null && !description.trim().isEmpty()) {
            return name + " (" + description + ")";
        }
        return name;
    }

    // ========== Getter & Setter ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ========== Object Methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return code != null ? code.equals(role.code) : role.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}