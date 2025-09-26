package com.ynet.mgmt.role.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.role.dto.CreateRoleRequest;
import com.ynet.mgmt.role.dto.RoleDTO;
import com.ynet.mgmt.role.dto.UpdateRoleRequest;
import com.ynet.mgmt.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理控制器
 *
 * @author Claude
 */
@RestController
@RequestMapping("/roles")
@Tag(name = "角色管理", description = "角色管理相关API")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // ========== 基础CRUD操作 ==========

    @PostMapping
    @Operation(summary = "创建角色", description = "创建新角色")
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDTO result = roleService.createRole(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "根据ID更新角色信息")
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        RoleDTO result = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "根据ID删除角色")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", description = "根据ID获取角色详情")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleById(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        RoleDTO result = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "根据代码获取角色", description = "根据角色代码获取角色详情")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleByCode(
            @Parameter(description = "角色代码") @PathVariable String code) {
        RoleDTO result = roleService.getRoleByCode(code);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 查询操作 ==========

    @GetMapping
    @Operation(summary = "分页查询角色列表", description = "分页查询角色列表，支持关键字搜索")
    public ResponseEntity<ApiResponse<PageResult<RoleDTO>>> listRoles(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResult<RoleDTO> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = roleService.searchRoles(keyword.trim(), pageable);
        } else {
            result = roleService.listRoles(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 验证操作 ==========

    @GetMapping("/check-code")
    @Operation(summary = "检查角色代码可用性", description = "检查角色代码是否可用")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeAvailability(
            @Parameter(description = "角色代码", required = true) @RequestParam String code,
            @Parameter(description = "排除的角色ID") @RequestParam(required = false) Long excludeId) {
        boolean available = roleService.isCodeAvailable(code, excludeId);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    @GetMapping("/check-name")
    @Operation(summary = "检查角色名称可用性", description = "检查角色名称是否可用")
    public ResponseEntity<ApiResponse<Boolean>> checkNameAvailability(
            @Parameter(description = "角色名称", required = true) @RequestParam String name,
            @Parameter(description = "排除的角色ID") @RequestParam(required = false) Long excludeId) {
        boolean available = roleService.isNameAvailable(name, excludeId);
        return ResponseEntity.ok(ApiResponse.success(available));
    }
}