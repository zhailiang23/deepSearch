package com.ynet.mgmt.user.controller;

import com.ynet.mgmt.user.dto.*;
import com.ynet.mgmt.user.entity.UserRole;
import com.ynet.mgmt.user.entity.UserStatus;
import com.ynet.mgmt.user.service.UserService;
import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供用户管理的REST API接口
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "用户管理", description = "用户管理相关API")
@RestController
@RequestMapping("/users")
@Validated
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ========== 基础CRUD操作 ==========

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "创建新的用户")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<UserDTO>> createUser(
            @Parameter(description = "创建用户请求", required = true)
            @Valid @RequestBody CreateUserRequest request) {
        logger.info("创建用户: username={}, email={}", request.getUsername(), request.getEmail());

        UserDTO result = userService.createUser(request);

        logger.info("用户创建成功: id={}, username={}", result.getId(), result.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "更新指定ID的用户信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新用户请求", required = true)
            @Valid @RequestBody UpdateUserRequest request) {
        logger.info("更新用户: id={}", id);

        UserDTO result = userService.updateUser(id, request);

        logger.info("用户更新成功: id={}, username={}", result.getId(), result.getUsername());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "删除指定ID的用户")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        logger.info("删除用户: id={}", id);

        userService.deleteUser(id);

        logger.info("用户删除成功: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * 获取单个用户
     */
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        logger.debug("根据ID查询用户: id={}", id);

        UserDTO result = userService.getUser(id);

        logger.debug("用户查询成功: id={}, username={}", result.getId(), result.getUsername());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表", description = "分页查询所有用户信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<UserDTO>>> listUsers(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态过滤") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "角色过滤") @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("分页查询用户列表: keyword={}, status={}, role={}, page={}, size={}",
                keyword, status, role, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<UserDTO> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = userService.searchUsers(keyword.trim(), pageable);
        } else if (status != null) {
            result = userService.listUsersByStatus(status, pageable);
        } else if (role != null) {
            result = userService.listUsersByRole(role, pageable);
        } else {
            result = userService.listUsers(pageable);
        }

        logger.debug("用户列表查询成功: keyword={}, status={}, role={}, totalElements={}, totalPages={}",
                keyword, status, role, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 搜索用户
     */
    @Operation(summary = "搜索用户", description = "根据关键字搜索用户")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResult<UserDTO>>> searchUsers(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("搜索用户: keyword={}, page={}, size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<UserDTO> result = userService.searchUsers(keyword, pageable);

        logger.debug("用户搜索成功: keyword={}, totalElements={}, totalPages={}",
                keyword, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有用户列表
     */
    @Operation(summary = "获取所有用户列表", description = "获取所有用户的简单列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        logger.debug("查询所有用户列表");

        List<UserDTO> result = userService.getAllUsers();

        logger.debug("所有用户列表查询成功: size={}", result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 特殊操作 ==========

    /**
     * 切换用户状态
     */
    @Operation(summary = "切换用户状态", description = "切换指定用户的状态")
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserDTO>> toggleStatus(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        logger.info("切换用户状态: id={}", id);

        UserDTO result = userService.toggleStatus(id);

        logger.info("用户状态切换成功: id={}, status={}", id, result.getStatus());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 验证接口 ==========

    /**
     * 检查用户名是否可用
     */
    @Operation(summary = "检查用户名是否可用", description = "验证用户名是否已被使用")
    @GetMapping("/username-available")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailable(
            @Parameter(description = "用户名", required = true) @RequestParam String username,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId != null ?
                userService.isUsernameAvailable(username, excludeId) :
                userService.isUsernameAvailable(username);

        return ResponseEntity.ok(ApiResponse.success(available));
    }

    /**
     * 检查邮箱是否可用
     */
    @Operation(summary = "检查邮箱是否可用", description = "验证邮箱是否已被使用")
    @GetMapping("/email-available")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailable(
            @Parameter(description = "邮箱", required = true) @RequestParam String email,
            @Parameter(description = "排除的用户ID") @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId != null ?
                userService.isEmailAvailable(email, excludeId) :
                userService.isEmailAvailable(email);

        return ResponseEntity.ok(ApiResponse.success(available));
    }

    // ========== 统计接口 ==========

    /**
     * 获取用户统计信息
     */
    @Operation(summary = "获取用户统计信息", description = "获取用户各状态和角色的数量统计")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getStatistics() {
        logger.debug("获取用户统计信息");

        Long activeCount = userService.countActiveUsers();
        Long lockedCount = userService.countLockedUsers();
        Long adminCount = userService.countByRole(UserRole.ADMIN);
        Long userCount = userService.countByRole(UserRole.USER);

        // 构建统计结果
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("activeUsers", activeCount);
        statistics.put("lockedUsers", lockedCount);
        statistics.put("totalUsers", activeCount + lockedCount);
        statistics.put("adminUsers", adminCount);
        statistics.put("normalUsers", userCount);

        logger.debug("用户统计信息查询成功: active={}, locked={}, total={}",
                activeCount, lockedCount, activeCount + lockedCount);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}