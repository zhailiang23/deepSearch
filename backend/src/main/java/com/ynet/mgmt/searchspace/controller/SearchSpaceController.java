package com.ynet.mgmt.searchspace.controller;

import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchspace.dto.*;
import com.ynet.mgmt.searchspace.dto.ApiResponseonse;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponseonses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索空间控制器
 * 提供搜索空间管理的REST API接口
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "搜索空间管理", description = "搜索空间管理相关API")
@RestController
@RequestMapping("/api/search-spaces")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002"})
public class SearchSpaceController {

    private static final Logger logger = LoggerFactory.getLogger(SearchSpaceController.class);

    private final SearchSpaceService searchSpaceService;

    @Autowired
    public SearchSpaceController(SearchSpaceService searchSpaceService) {
        this.searchSpaceService = searchSpaceService;
    }

    /**
     * 创建搜索空间
     *
     * @param createRequest 创建请求
     * @return 创建的搜索空间信息
     */
    @Operation(summary = "创建搜索空间", description = "创建新的搜索空间，包括名称、代码等基本信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponseonse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponseonse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SearchSpaceDTO>> createSearchSpace(
            @Parameter(description = "创建搜索空间请求", required = true)
            @Valid @RequestBody CreateSearchSpaceRequest createRequest) {
        logger.info("创建搜索空间: name={}, code={}", createRequest.getName(), createRequest.getCode());

        SearchSpaceDTO result = searchSpaceService.createSearchSpace(createRequest);

        logger.info("搜索空间创建成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result));
    }

    /**
     * 分页查询搜索空间列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Operation(summary = "分页查询搜索空间列表", description = "根据关键词、分页参数查询搜索空间列表")
    @ApiResponseonses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<SearchSpaceDTO>>> listSearchSpaces(
            @Parameter(description = "查询请求参数") @Valid SearchSpaceQueryRequest request) {
        logger.debug("查询搜索空间列表: keyword={}, page={}, size={}",
                request.getKeyword(), request.getPage(), request.getSize());

        PageResult<SearchSpaceDTO> result = searchSpaceService.listSearchSpaces(request);

        logger.debug("搜索空间列表查询成功: totalElements={}, numberOfElements={}",
                result.getTotalElements(), result.getNumberOfElements());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 根据ID查询搜索空间
     *
     * @param id 搜索空间ID
     * @return 搜索空间信息
     */
    @Operation(summary = "根据ID查询搜索空间", description = "根据搜索空间ID获取详细信息")
    @ApiResponseonses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SearchSpaceDTO>> getSearchSpace(
            @Parameter(description = "搜索空间ID", required = true) @PathVariable Long id) {
        logger.debug("根据ID查询搜索空间: id={}", id);

        SearchSpaceDTO result = searchSpaceService.getSearchSpace(id);

        logger.debug("搜索空间查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 根据代码查询搜索空间
     *
     * @param code 搜索空间代码
     * @return 搜索空间信息
     */
    @Operation(summary = "根据代码查询搜索空间", description = "根据搜索空间代码获取详细信息")
    @ApiResponseonses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<SearchSpaceDTO>> getSearchSpaceByCode(
            @Parameter(description = "搜索空间代码", required = true) @PathVariable String code) {
        logger.debug("根据代码查询搜索空间: code={}", code);

        SearchSpaceDTO result = searchSpaceService.getSearchSpaceByCode(code);

        logger.debug("搜索空间查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新搜索空间
     *
     * @param id 搜索空间ID
     * @param updateRequest 更新请求
     * @return 更新后的搜索空间信息
     */
    @Operation(summary = "更新搜索空间", description = "更新搜索空间的名称、描述等信息")
    @ApiResponseonses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponseonseonse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SearchSpaceDTO>> updateSearchSpace(
            @Parameter(description = "搜索空间ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新请求", required = true) @Valid @RequestBody UpdateSearchSpaceRequest updateRequest) {
        logger.info("更新搜索空间: id={}, name={}", id, updateRequest.getName());

        SearchSpaceDTO result = searchSpaceService.updateSearchSpace(id, updateRequest);

        logger.info("搜索空间更新成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(ApiResponse.updated(result));
    }

    /**
     * 删除搜索空间
     *
     * @param id 搜索空间ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSearchSpace(@PathVariable Long id) {
        logger.info("删除搜索空间: id={}", id);

        searchSpaceService.deleteSearchSpace(id);

        logger.info("搜索空间删除成功: id={}", id);

        return ResponseEntity.ok(ApiResponse.deleted());
    }

    /**
     * 获取搜索空间统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SearchSpaceStatistics>> getStatistics() {
        logger.debug("获取搜索空间统计信息");

        SearchSpaceStatistics result = searchSpaceService.getStatistics();

        logger.debug("统计信息获取成功: totalCount={}, activeCount={}",
                result.getTotalCount(), result.getActiveCount());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 启用向量检索
     *
     * @param id 搜索空间ID
     * @return 更新后的搜索空间信息
     */
    @PostMapping("/{id}/vector/enable")
    public ResponseEntity<ApiResponse<SearchSpaceDTO>> enableVectorSearch(@PathVariable Long id) {
        logger.info("启用向量检索: id={}", id);

        SearchSpaceDTO result = searchSpaceService.enableVectorSearch(id);

        logger.info("向量检索启用成功: id={}, vectorEnabled={}",
                result.getId(), result.getVectorEnabled());

        return ResponseEntity.ok(ApiResponse.success("向量检索启用成功", result));
    }

    /**
     * 禁用向量检索
     *
     * @param id 搜索空间ID
     * @return 更新后的搜索空间信息
     */
    @PostMapping("/{id}/vector/disable")
    public ResponseEntity<ApiResponse<SearchSpaceDTO>> disableVectorSearch(@PathVariable Long id) {
        logger.info("禁用向量检索: id={}", id);

        SearchSpaceDTO result = searchSpaceService.disableVectorSearch(id);

        logger.info("向量检索禁用成功: id={}, vectorEnabled={}",
                result.getId(), result.getVectorEnabled());

        return ResponseEntity.ok(ApiResponse.success("向量检索禁用成功", result));
    }

    /**
     * 检查代码是否可用
     *
     * @param code 搜索空间代码
     * @return 可用性检查结果
     */
    @GetMapping("/code/{code}/available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCodeAvailability(@PathVariable String code) {
        logger.debug("检查代码可用性: code={}", code);

        boolean available = searchSpaceService.isCodeAvailable(code);

        logger.debug("代码可用性检查完成: code={}, available={}", code, available);

        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("available", available);

        String message = available ? "代码可用" : "代码已被使用";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

}