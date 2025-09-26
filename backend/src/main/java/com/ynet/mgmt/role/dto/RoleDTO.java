package com.ynet.mgmt.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 角色数据传输对象
 *
 * @author Claude
 */
@Schema(description = "角色数据传输对象")
public class RoleDTO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色代码")
    private String code;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "更新者")
    private String updatedBy;

    // ========== 构造方法 ==========

    public RoleDTO() {}

    public RoleDTO(Long id, String name, String code, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}