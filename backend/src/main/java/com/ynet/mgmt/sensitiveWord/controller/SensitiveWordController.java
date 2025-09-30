package com.ynet.mgmt.sensitiveWord.controller;

import com.ynet.mgmt.sensitiveWord.dto.*;
import com.ynet.mgmt.sensitiveWord.service.SensitiveWordService;
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
 * 敏感词管理控制器
 * 提供敏感词管理的REST API接口
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "敏感词管理", description = "敏感词管理相关API")
@RestController
@RequestMapping("/sensitive-words")
@Validated
@CrossOrigin(origins = "*")
public class SensitiveWordController {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordController.class);

    private final SensitiveWordService sensitiveWordService;

    public SensitiveWordController(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    // ========== 基础CRUD操作 ==========

    /**
     * 创建敏感词
     */
    @Operation(summary = "创建敏感词", description = "创建新的敏感词")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = SensitiveWordDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<SensitiveWordDTO>> createSensitiveWord(
            @Parameter(description = "创建敏感词请求", required = true)
            @Valid @RequestBody CreateSensitiveWordRequest request) {
        logger.info("创建敏感词: name={}, harmLevel={}", request.getName(), request.getHarmLevel());

        SensitiveWordDTO result = sensitiveWordService.createSensitiveWord(request);

        logger.info("敏感词创建成功: id={}, name={}, harmLevel={}",
                result.getId(), result.getName(), result.getHarmLevel());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * 更新敏感词
     */
    @Operation(summary = "更新敏感词", description = "更新指定ID的敏感词信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = SensitiveWordDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "敏感词不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SensitiveWordDTO>> updateSensitiveWord(
            @Parameter(description = "敏感词ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新敏感词请求", required = true)
            @Valid @RequestBody UpdateSensitiveWordRequest request) {
        logger.info("更新敏感词: id={}, name={}", id, request.getName());

        SensitiveWordDTO result = sensitiveWordService.updateSensitiveWord(id, request);

        logger.info("敏感词更新成功: id={}, name={}, harmLevel={}",
                result.getId(), result.getName(), result.getHarmLevel());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除敏感词
     */
    @Operation(summary = "删除敏感词", description = "删除指定ID的敏感词")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "敏感词不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deleteSensitiveWord(
            @Parameter(description = "敏感词ID", required = true) @PathVariable Long id) {
        logger.info("删除敏感词: id={}", id);

        sensitiveWordService.deleteSensitiveWord(id);

        logger.info("敏感词删除成功: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * 获取单个敏感词
     */
    @Operation(summary = "获取敏感词详情", description = "根据ID获取敏感词详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = SensitiveWordDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "敏感词不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SensitiveWordDTO>> getSensitiveWord(
            @Parameter(description = "敏感词ID", required = true) @PathVariable Long id) {
        logger.debug("根据ID查询敏感词: id={}", id);

        SensitiveWordDTO result = sensitiveWordService.getSensitiveWord(id);

        logger.debug("敏感词查询成功: id={}, name={}, harmLevel={}",
                result.getId(), result.getName(), result.getHarmLevel());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 分页查询敏感词列表
     */
    @Operation(summary = "分页查询敏感词列表", description = "分页查询所有敏感词信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<SensitiveWordDTO>>> listSensitiveWords(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "启用状态过滤") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "危害等级过滤") @RequestParam(required = false) Integer harmLevel,
            @PageableDefault(size = 20, sort = "harmLevel", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("分页查询敏感词列表: keyword={}, enabled={}, harmLevel={}, page={}, size={}",
                keyword, enabled, harmLevel, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<SensitiveWordDTO> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = sensitiveWordService.searchSensitiveWords(keyword.trim(), pageable);
        } else if (enabled != null && harmLevel != null) {
            // 暂不支持复合查询,先按启用状态查询
            result = sensitiveWordService.listSensitiveWordsByEnabled(enabled, pageable);
        } else if (enabled != null) {
            result = sensitiveWordService.listSensitiveWordsByEnabled(enabled, pageable);
        } else if (harmLevel != null) {
            result = sensitiveWordService.listSensitiveWordsByHarmLevel(harmLevel, pageable);
        } else {
            result = sensitiveWordService.listSensitiveWords(pageable);
        }

        logger.debug("敏感词列表查询成功: keyword={}, enabled={}, harmLevel={}, totalElements={}, totalPages={}",
                keyword, enabled, harmLevel, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 搜索敏感词
     */
    @Operation(summary = "搜索敏感词", description = "根据关键字搜索敏感词")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResult<SensitiveWordDTO>>> searchSensitiveWords(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "harmLevel", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("搜索敏感词: keyword={}, page={}, size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<SensitiveWordDTO> result = sensitiveWordService.searchSensitiveWords(keyword, pageable);

        logger.debug("敏感词搜索成功: keyword={}, totalElements={}, totalPages={}",
                keyword, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有敏感词列表
     */
    @Operation(summary = "获取所有敏感词列表", description = "获取所有敏感词的简单列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SensitiveWordDTO>>> getAllSensitiveWords() {
        logger.debug("查询所有敏感词列表");

        List<SensitiveWordDTO> result = sensitiveWordService.getAllSensitiveWords();

        logger.debug("所有敏感词列表查询成功: size={}", result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 特殊操作 ==========

    /**
     * 切换敏感词启用状态
     */
    @Operation(summary = "切换敏感词启用状态", description = "切换指定敏感词的启用状态")
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<SensitiveWordDTO>> toggleStatus(
            @Parameter(description = "敏感词ID", required = true) @PathVariable Long id) {
        logger.info("切换敏感词启用状态: id={}", id);

        SensitiveWordDTO result = sensitiveWordService.toggleStatus(id);

        logger.info("敏感词状态切换成功: id={}, enabled={}", id, result.getEnabled());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新敏感词危害等级
     */
    @Operation(summary = "更新敏感词危害等级", description = "更新指定敏感词的危害等级")
    @PatchMapping("/{id}/harm-level")
    public ResponseEntity<ApiResponse<SensitiveWordDTO>> updateHarmLevel(
            @Parameter(description = "敏感词ID", required = true) @PathVariable Long id,
            @Parameter(description = "新的危害等级", required = true) @RequestParam Integer harmLevel) {
        logger.info("更新敏感词危害等级: id={}, harmLevel={}", id, harmLevel);

        SensitiveWordDTO result = sensitiveWordService.updateHarmLevel(id, harmLevel);

        logger.info("敏感词危害等级更新成功: id={}, harmLevel={}", id, harmLevel);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 验证接口 ==========

    /**
     * 检查敏感词名称是否可用
     */
    @Operation(summary = "检查敏感词名称是否可用", description = "验证敏感词名称是否已被使用")
    @GetMapping("/name-available")
    public ResponseEntity<ApiResponse<Boolean>> checkNameAvailable(
            @Parameter(description = "敏感词名称", required = true) @RequestParam String name,
            @Parameter(description = "排除的敏感词ID") @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId != null ?
            sensitiveWordService.isNameAvailable(name, excludeId) :
            sensitiveWordService.isNameAvailable(name);

        return ResponseEntity.ok(ApiResponse.success(available));
    }

    // ========== 统计接口 ==========

    /**
     * 获取敏感词统计信息
     */
    @Operation(summary = "获取敏感词统计信息", description = "获取启用、禁用敏感词数量和各等级分布")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getStatistics() {
        logger.debug("获取敏感词统计信息");

        Long enabledCount = sensitiveWordService.countEnabledWords();
        Long disabledCount = sensitiveWordService.countDisabledWords();
        Map<Integer, Long> levelDistribution = sensitiveWordService.getHarmLevelDistribution();

        // 构建统计结果
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("enabledWords", enabledCount);
        statistics.put("disabledWords", disabledCount);
        statistics.put("totalWords", enabledCount + disabledCount);
        statistics.put("harmLevelDistribution", levelDistribution);

        logger.debug("敏感词统计信息查询成功: enabled={}, disabled={}, total={}",
                enabledCount, disabledCount, enabledCount + disabledCount);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}