package com.ynet.mgmt.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建角色请求对象
 *
 * @author Claude
 */
@Schema(description = "创建角色请求对象")
public class CreateRoleRequest {

    @Schema(description = "角色名称", required = true)
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 100, message = "角色名称长度必须在2-100个字符之间")
    private String name;

    @Schema(description = "角色代码", required = true)
    @NotBlank(message = "角色代码不能为空")
    @Size(min = 2, max = 50, message = "角色代码长度必须在2-50个字符之间")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色代码必须以大写字母开头，只能包含大写字母、数字和下划线")
    private String code;

    @Schema(description = "角色描述")
    @Size(max = 500, message = "角色描述长度不能超过500个字符")
    private String description;

    // ========== 构造方法 ==========

    public CreateRoleRequest() {}

    public CreateRoleRequest(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public CreateRoleRequest(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    // ========== Getter & Setter ==========

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
}