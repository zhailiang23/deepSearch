package com.ynet.mgmt.hotTopic.controller;

import com.ynet.mgmt.hotTopic.dto.*;
import com.ynet.mgmt.hotTopic.service.HotTopicService;
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
 * 热门话题管理控制器
 * 提供热门话题管理的REST API接口
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "热门话题管理", description = "热门话题管理相关API")
@RestController
@RequestMapping("/hot-topics")
@Validated
@CrossOrigin(origins = "*")
public class HotTopicController {

    private static final Logger logger = LoggerFactory.getLogger(HotTopicController.class);

    private final HotTopicService hotTopicService;

    public HotTopicController(HotTopicService hotTopicService) {
        this.hotTopicService = hotTopicService;
    }

    // ========== 基础CRUD操作 ==========

    /**
     * 创建热门话题
     */
    @Operation(summary = "创建热门话题", description = "创建新的热门话题")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = HotTopicDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<HotTopicDTO>> createHotTopic(
            @Parameter(description = "创建热门话题请求", required = true)
            @Valid @RequestBody CreateHotTopicRequest request) {
        logger.info("创建热门话题: name={}, popularity={}", request.getName(), request.getPopularity());

        HotTopicDTO result = hotTopicService.createHotTopic(request);

        logger.info("热门话题创建成功: id={}, name={}, popularity={}",
                result.getId(), result.getName(), result.getPopularity());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * 批量创建热门话题
     */
    @Operation(summary = "批量创建热门话题", description = "批量创建多个热门话题")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "批量创建成功",
                    content = @Content(schema = @Schema(implementation = BatchCreateHotTopicResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<BatchCreateHotTopicResponse>> batchCreateHotTopics(
            @Parameter(description = "批量创建热门话题请求", required = true)
            @Valid @RequestBody BatchCreateHotTopicRequest request) {
        logger.info("批量创建热门话题: count={}", request.getTopics().size());

        BatchCreateHotTopicResponse result = hotTopicService.batchCreateHotTopics(request);

        logger.info("批量创建完成: success={}, skipped={}, failed={}",
                result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * 更新热门话题
     */
    @Operation(summary = "更新热门话题", description = "更新指定ID的热门话题信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = HotTopicDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "话题不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotTopicDTO>> updateHotTopic(
            @Parameter(description = "话题ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新热门话题请求", required = true)
            @Valid @RequestBody UpdateHotTopicRequest request) {
        logger.info("更新热门话题: id={}, name={}", id, request.getName());

        HotTopicDTO result = hotTopicService.updateHotTopic(id, request);

        logger.info("热门话题更新成功: id={}, name={}, popularity={}",
                result.getId(), result.getName(), result.getPopularity());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 删除热门话题
     */
    @Operation(summary = "删除热门话题", description = "删除指定ID的热门话题")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "话题不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> deleteHotTopic(
            @Parameter(description = "话题ID", required = true) @PathVariable Long id) {
        logger.info("删除热门话题: id={}", id);

        hotTopicService.deleteHotTopic(id);

        logger.info("热门话题删除成功: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * 获取单个热门话题
     */
    @Operation(summary = "获取热门话题详情", description = "根据ID获取热门话题详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = HotTopicDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "话题不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotTopicDTO>> getHotTopic(
            @Parameter(description = "话题ID", required = true) @PathVariable Long id) {
        logger.debug("根据ID查询热门话题: id={}", id);

        HotTopicDTO result = hotTopicService.getHotTopic(id);

        logger.debug("热门话题查询成功: id={}, name={}, popularity={}",
                result.getId(), result.getName(), result.getPopularity());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 分页查询热门话题列表
     */
    @Operation(summary = "分页查询热门话题列表", description = "分页查询所有热门话题信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<HotTopicDTO>>> listHotTopics(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "可见性过滤") @RequestParam(required = false) Boolean visible,
            @PageableDefault(size = 20, sort = "popularity", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("分页查询热门话题列表: keyword={}, visible={}, page={}, size={}",
                keyword, visible, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<HotTopicDTO> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = hotTopicService.searchHotTopics(keyword.trim(), pageable);
        } else if (visible != null) {
            result = hotTopicService.listHotTopicsByVisible(visible, pageable);
        } else {
            result = hotTopicService.listHotTopics(pageable);
        }

        logger.debug("热门话题列表查询成功: keyword={}, visible={}, totalElements={}, totalPages={}",
                keyword, visible, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 搜索热门话题
     */
    @Operation(summary = "搜索热门话题", description = "根据关键字搜索热门话题")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResult<HotTopicDTO>>> searchHotTopics(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "popularity", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("搜索热门话题: keyword={}, page={}, size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        PageResult<HotTopicDTO> result = hotTopicService.searchHotTopics(keyword, pageable);

        logger.debug("热门话题搜索成功: keyword={}, totalElements={}, totalPages={}",
                keyword, result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取所有热门话题列表
     */
    @Operation(summary = "获取所有热门话题列表", description = "获取所有热门话题的简单列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<HotTopicDTO>>> getAllHotTopics() {
        logger.debug("查询所有热门话题列表");

        List<HotTopicDTO> result = hotTopicService.getAllHotTopics();

        logger.debug("所有热门话题列表查询成功: size={}", result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 特殊操作 ==========

    /**
     * 切换话题可见性
     */
    @Operation(summary = "切换话题可见性", description = "切换指定话题的可见性状态")
    @PostMapping("/{id}/toggle-visibility")
    public ResponseEntity<ApiResponse<HotTopicDTO>> toggleVisibility(
            @Parameter(description = "话题ID", required = true) @PathVariable Long id) {
        logger.info("切换话题可见性: id={}", id);

        HotTopicDTO result = hotTopicService.toggleVisibility(id);

        logger.info("话题可见性切换成功: id={}, visible={}", id, result.getVisible());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新话题热度
     */
    @Operation(summary = "更新话题热度", description = "更新指定话题的热度值")
    @PatchMapping("/{id}/popularity")
    public ResponseEntity<ApiResponse<HotTopicDTO>> updatePopularity(
            @Parameter(description = "话题ID", required = true) @PathVariable Long id,
            @Parameter(description = "新的热度值", required = true) @RequestParam Integer popularity) {
        logger.info("更新话题热度: id={}, popularity={}", id, popularity);

        HotTopicDTO result = hotTopicService.updatePopularity(id, popularity);

        logger.info("话题热度更新成功: id={}, popularity={}", id, popularity);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取热门话题排行榜
     */
    @Operation(summary = "获取热门话题排行榜", description = "获取热度排行榜前N位")
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<HotTopicDTO>>> getTopPopularTopics(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        logger.debug("获取热门话题排行榜: limit={}", limit);

        List<HotTopicDTO> result = hotTopicService.getTopPopularTopics(limit);

        logger.debug("热门话题排行榜查询成功: limit={}, size={}", limit, result.size());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ========== 验证接口 ==========

    /**
     * 检查话题名称是否可用
     */
    @Operation(summary = "检查话题名称是否可用", description = "验证话题名称是否已被使用")
    @GetMapping("/name-available")
    public ResponseEntity<ApiResponse<Boolean>> checkNameAvailable(
            @Parameter(description = "话题名称", required = true) @RequestParam String name,
            @Parameter(description = "排除的话题ID") @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId != null ?
            hotTopicService.isNameAvailable(name, excludeId) :
            hotTopicService.isNameAvailable(name);

        return ResponseEntity.ok(ApiResponse.success(available));
    }

    // ========== 统计接口 ==========

    /**
     * 获取话题统计信息
     */
    @Operation(summary = "获取话题统计信息", description = "获取可见、不可见话题数量和平均热度")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getStatistics() {
        logger.debug("获取话题统计信息");

        Long visibleCount = hotTopicService.countVisibleTopics();
        Long invisibleCount = hotTopicService.countInvisibleTopics();
        Double averagePopularity = hotTopicService.getAveragePopularity();

        class Statistics {
            public final Long visibleTopics = visibleCount;
            public final Long invisibleTopics = invisibleCount;
            public final Long totalTopics = visibleCount + invisibleCount;
            public final Double avgPopularity = averagePopularity != null ? averagePopularity : 0.0;
        }

        Statistics statistics = new Statistics();

        logger.debug("话题统计信息查询成功: visible={}, invisible={}, average={}",
                visibleCount, invisibleCount, averagePopularity);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}