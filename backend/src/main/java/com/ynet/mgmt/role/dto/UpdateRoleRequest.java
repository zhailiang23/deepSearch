package com.ynet.mgmt.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 更新角色请求对象
 *
 * @author Claude
 */
@Schema(description = "更新角色请求对象")
public class UpdateRoleRequest {

    @Schema(description = "角色名称")
    @Size(min = 2, max = 100, message = "角色名称长度必须在2-100个字符之间")
    private String name;

    @Schema(description = "角色描述")
    @Size(max = 500, message = "角色描述长度不能超过500个字符")
    private String description;

    // ========== 构造方法 ==========

    public UpdateRoleRequest() {}

    public UpdateRoleRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // ========== Getter & Setter ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}