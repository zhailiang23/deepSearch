package com.ynet.mgmt.channel.controller;

import com.ynet.mgmt.channel.dto.*;
import com.ynet.mgmt.channel.service.ChannelService;
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

import java.util.List;

/**
 * 渠道管理控制器
 * 提供渠道管理的REST API接口
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "渠道管理", description = "渠道管理相关API")
@RestController
@RequestMapping("/channels")
@Validated
@CrossOrigin(origins = "*")
public class ChannelController {

    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    // ========== 基础CRUD操作 ==========

    /**
     * 创建渠道
     */
    @Operation(summary = "创建渠道", description = "创建新的销售渠道")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<ChannelDTO>> createChannel(
            @Parameter(description = "创建渠道请求", required = true)
            @Valid @RequestBody CreateChannelRequest request) {
        logger.info("创建渠道: name={}, code={}", request.getName(), request.getCode());

        ChannelDTO result = channelService.createChannel(request);

        logger.info("渠道创建成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * 更新渠道
     */
    @Operation(summary = "更新渠道", description = "更新指定ID的渠道信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelDTO>> updateChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新渠道请求", required = true)
            @Valid @RequestBody UpdateChannelRequest request) {
        logger.info("更新渠道: id={}, name={}", id, request.getName());

        ChannelDTO result = channelService.updateChannel(id, request);

        logger.info("渠道更新成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除渠道
     */
    @Operation(summary = "删除渠道", description = "删除指定ID的渠道")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.info("删除渠道: id={}", id);

        channelService.deleteChannel(id);

        logger.info("渠道删除成功: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * 获取单个渠道
     */
    @Operation(summary = "获取渠道详情", description = "根据ID获取渠道详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.debug("根据ID查询渠道: id={}", id);

        ChannelDTO result = channelService.getChannel(id);

        logger.debug("渠道查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 根据代码获取渠道
     */
    @Operation(summary = "根据代码获取渠道", description = "根据代码获取渠道详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannelByCode(
            @Parameter(description = "渠道代码", required = true) @PathVariable String code) {
        logger.debug("根据代码查询渠道: code={}", code);

        ChannelDTO result = channelService.getChannelByCode(code);

        logger.debug("渠道查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 分页查询渠道列表
     */
    @Operation(summary = "分页查询渠道列表", description = "分页查询所有渠道信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ChannelDTO>>> listChannels(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("分页查询渠道列表: keyword={}, page={}, size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<ChannelDTO> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = channelService.searchChannels(keyword.trim(), pageable);
        } else {
            result = channelService.listChannels(pageable);
        }

        logger.debug("渠道列表查询成功: keyword={}, totalElements={}, totalPages={}",
                keyword, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 搜索渠道
     */
    @Operation(summary = "搜索渠道", description = "根据关键字搜索渠道")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResult<ChannelDTO>>> searchChannels(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("搜索渠道: keyword={}, page={}, size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<ChannelDTO> result = channelService.searchChannels(keyword, pageable);

        logger.debug("渠道搜索成功: keyword={}, totalElements={}, totalPages={}",
                keyword, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有渠道列表
     */
    @Operation(summary = "获取所有渠道列表", description = "获取所有渠道的简单列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getAllChannels() {
        logger.debug("查询所有渠道列表");

        List<ChannelDTO> result = channelService.getAllChannels();

        logger.debug("所有渠道列表查询成功: size={}", result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 验证接口 ==========

    /**
     * 检查渠道代码是否可用
     */
    @Operation(summary = "检查渠道代码是否可用", description = "验证渠道代码是否已被使用")
    @GetMapping("/code-available")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeAvailable(
            @Parameter(description = "渠道代码", required = true) @RequestParam String code,
            @Parameter(description = "排除的渠道ID") @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId != null ?
            channelService.isCodeAvailable(code, excludeId) :
            channelService.isCodeAvailable(code);

        return ResponseEntity.ok(ApiResponse.success(available));
    }

    /**
     * 检查渠道名称是否可用
     */
    @Operation(summary = "检查渠道名称是否可用", description = "验证渠道名称是否已被使用")
    @GetMapping("/name-available")
    public ResponseEntity<ApiResponse<Boolean>> checkNameAvailable(
            @Parameter(description = "渠道名称", required = true) @RequestParam String name,
            @Parameter(description = "排除的渠道ID") @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId != null ?
            channelService.isNameAvailable(name, excludeId) :
            channelService.isNameAvailable(name);

        return ResponseEntity.ok(ApiResponse.success(available));
    }
}