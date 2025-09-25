package com.ynet.mgmt.channel.controller;

import com.ynet.mgmt.channel.dto.*;
import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
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

        return ResponseEntity.ok(ApiResponse.updated(result));
    }

    /**
     * 删除渠道
     */
    @Operation(summary = "删除渠道", description = "软删除指定ID的渠道")
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

        return ResponseEntity.ok(ApiResponse.deleted());
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
    @Operation(summary = "根据代码获取渠道", description = "根据渠道代码获取渠道信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/by-code/{code}")
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannelByCode(
            @Parameter(description = "渠道代码", required = true) @PathVariable String code) {
        logger.debug("根据代码查询渠道: code={}", code);

        ChannelDTO result = channelService.getChannelByCode(code);

        logger.debug("渠道查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 列表查询操作 ==========

    /**
     * 分页查询渠道列表
     */
    @Operation(summary = "分页查询渠道", description = "根据条件分页查询渠道列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ChannelDTO>>> listChannels(
            @Parameter(description = "查询参数") @Valid @ModelAttribute ChannelQueryRequest request) {
        logger.debug("查询渠道列表: keyword={}, page={}, size={}",
                request.getKeyword(), request.getPage(), request.getSize());

        PageResult<ChannelDTO> result = channelService.listChannels(request);

        logger.debug("渠道列表查询成功: totalElements={}, numberOfElements={}",
                result.getTotalElements(), result.getNumberOfElements());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取活跃渠道列表
     */
    @Operation(summary = "获取活跃渠道", description = "获取所有状态为活跃的渠道")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = List.class)))
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getActiveChannels() {
        logger.debug("获取活跃渠道列表");

        List<ChannelDTO> result = channelService.getActiveChannels();

        logger.debug("活跃渠道列表查询成功: count={}", result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 按类型获取渠道
     */
    @Operation(summary = "按类型查询渠道", description = "根据渠道类型查询渠道列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = List.class)))
    })
    @GetMapping("/by-type/{type}")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getChannelsByType(
            @Parameter(description = "渠道类型", required = true) @PathVariable ChannelType type) {
        logger.debug("根据类型查询渠道: type={}", type);

        List<ChannelDTO> result = channelService.getChannelsByType(type);

        logger.debug("按类型渠道列表查询成功: type={}, count={}", type, result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 状态管理操作 ==========

    /**
     * 激活渠道
     */
    @Operation(summary = "激活渠道", description = "将指定渠道设置为激活状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "激活成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ChannelDTO>> activateChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.info("激活渠道: id={}", id);

        ChannelDTO result = channelService.activateChannel(id);

        logger.info("渠道激活成功: id={}, status={}",
                result.getId(), result.getStatus());

        return ResponseEntity.ok(ApiResponse.success("渠道激活成功", result));
    }

    /**
     * 停用渠道
     */
    @Operation(summary = "停用渠道", description = "将指定渠道设置为停用状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "停用成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ChannelDTO>> deactivateChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.info("停用渠道: id={}", id);

        ChannelDTO result = channelService.deactivateChannel(id);

        logger.info("渠道停用成功: id={}, status={}",
                result.getId(), result.getStatus());

        return ResponseEntity.ok(ApiResponse.success("渠道停用成功", result));
    }

    /**
     * 暂停渠道
     */
    @Operation(summary = "暂停渠道", description = "将指定渠道设置为暂停状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "暂停成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<ChannelDTO>> suspendChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.info("暂停渠道: id={}", id);

        ChannelDTO result = channelService.suspendChannel(id);

        logger.info("渠道暂停成功: id={}, status={}",
                result.getId(), result.getStatus());

        return ResponseEntity.ok(ApiResponse.success("渠道暂停成功", result));
    }

    /**
     * 恢复渠道
     */
    @Operation(summary = "恢复渠道", description = "将暂停的渠道恢复到激活状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "恢复成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ChannelDTO>> restoreChannel(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.info("恢复渠道: id={}", id);

        ChannelDTO result = channelService.restoreChannel(id);

        logger.info("渠道恢复成功: id={}, status={}",
                result.getId(), result.getStatus());

        return ResponseEntity.ok(ApiResponse.success("渠道恢复成功", result));
    }

    // ========== 销售管理操作 ==========

    /**
     * 更新销售数据
     */
    @Operation(summary = "更新销售数据", description = "更新渠道的销售金额和业绩数据")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}/sales")
    public ResponseEntity<ApiResponse<ChannelDTO>> updateSales(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id,
            @Parameter(description = "销售数据更新请求", required = true)
            @Valid @RequestBody UpdateSalesRequest request) {
        logger.info("更新渠道销售数据: id={}, amount={}", id, request.getSalesAmount());

        ChannelDTO result = channelService.updateSales(id, request);

        logger.info("渠道销售数据更新成功: id={}, totalSales={}, currentMonthSales={}",
                result.getId(), result.getTotalSales(), result.getCurrentMonthSales());

        return ResponseEntity.ok(ApiResponse.success("销售数据更新成功", result));
    }

    /**
     * 重置月度销售
     */
    @Operation(summary = "重置月度销售", description = "重置指定渠道的当月销售数据")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "重置成功",
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "渠道不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}/reset-monthly-sales")
    public ResponseEntity<ApiResponse<ChannelDTO>> resetMonthlySales(
            @Parameter(description = "渠道ID", required = true) @PathVariable Long id) {
        logger.info("重置月度销售: id={}", id);

        ChannelDTO result = channelService.resetMonthlySales(id);

        logger.info("月度销售重置成功: id={}, currentMonthSales={}",
                result.getId(), result.getCurrentMonthSales());

        return ResponseEntity.ok(ApiResponse.success("月度销售重置成功", result));
    }

    /**
     * 获取销售排行
     */
    @Operation(summary = "获取销售排行", description = "获取销售业绩排行榜前N名渠道")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = List.class)))
    })
    @GetMapping("/top-performers")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getTopPerformingChannels(
            @Parameter(description = "排行榜数量", required = false)
            @RequestParam(defaultValue = "10") int limit) {
        logger.debug("获取销售排行: limit={}", limit);

        List<ChannelDTO> result = channelService.getTopPerformingChannels(limit);

        logger.debug("销售排行查询成功: count={}", result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 统计分析操作 ==========

    /**
     * 获取渠道统计
     */
    @Operation(summary = "获取渠道统计", description = "获取渠道管理的统计数据")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ChannelStatistics.class)))
    })
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ChannelStatistics>> getChannelStatistics() {
        logger.debug("获取渠道统计信息");

        ChannelStatistics result = channelService.getChannelStatistics();

        logger.debug("统计信息获取成功: totalChannels={}, activeChannels={}",
                result.getTotalChannels(), result.getActiveChannels());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 批量操作 ==========

    /**
     * 批量状态变更
     */
    @Operation(summary = "批量状态变更", description = "批量更改多个渠道的状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/batch/status")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> batchUpdateStatus(
            @Parameter(description = "批量状态更新请求", required = true)
            @Valid @RequestBody BatchStatusUpdateRequest request) {
        logger.info("批量状态变更: channelIds={}, targetStatus={}",
                request.getChannelIds(), request.getTargetStatus());

        List<ChannelDTO> result = channelService.batchUpdateStatus(request);

        logger.info("批量状态变更成功: count={}", result.size());

        return ResponseEntity.ok(ApiResponse.success("批量状态变更成功", result));
    }

    /**
     * 批量删除
     */
    @Operation(summary = "批量删除", description = "批量删除多个渠道")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteChannels(
            @Parameter(description = "渠道ID列表", required = true)
            @RequestBody List<Long> channelIds) {
        logger.info("批量删除渠道: channelIds={}", channelIds);

        channelService.batchDeleteChannels(channelIds);

        logger.info("批量删除成功: count={}", channelIds.size());

        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }

    // ========== 验证操作 ==========

    /**
     * 检查代码唯一性
     */
    @Operation(summary = "检查代码唯一性", description = "检查渠道代码是否可用")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查完成")
    })
    @GetMapping("/check-code")
    public ResponseEntity<ApiResponse<Boolean>> checkCodeAvailability(
            @Parameter(description = "渠道代码", required = true) @RequestParam String code,
            @Parameter(description = "排除的渠道ID") @RequestParam(required = false) Long excludeId) {
        logger.debug("检查代码可用性: code={}, excludeId={}", code, excludeId);

        boolean available = excludeId != null
            ? channelService.isCodeAvailable(code, excludeId)
            : channelService.isCodeAvailable(code);

        logger.debug("代码可用性检查完成: code={}, available={}", code, available);

        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("available", available);

        String message = available ? "代码可用" : "代码已被使用";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    /**
     * 检查名称唯一性
     */
    @Operation(summary = "检查名称唯一性", description = "检查渠道名称是否可用")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查完成")
    })
    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<Boolean>> checkNameAvailability(
            @Parameter(description = "渠道名称", required = true) @RequestParam String name,
            @Parameter(description = "排除的渠道ID") @RequestParam(required = false) Long excludeId) {
        logger.debug("检查名称可用性: name={}, excludeId={}", name, excludeId);

        boolean available = excludeId != null
            ? channelService.isNameAvailable(name, excludeId)
            : channelService.isNameAvailable(name);

        logger.debug("名称可用性检查完成: name={}, available={}", name, available);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("available", available);

        String message = available ? "名称可用" : "名称已被使用";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }
}