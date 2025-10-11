package com.ynet.mgmt.searchdata.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchdata.dto.*;
import com.ynet.mgmt.searchdata.service.ElasticsearchDataService;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch数据管理控制器
 * 提供ES数据的搜索、更新、删除等操作API
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "Elasticsearch数据管理", description = "ES数据搜索、编辑、删除相关API")
@RestController
@RequestMapping("/elasticsearch")
public class ElasticsearchDataController {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDataController.class);

    private final ElasticsearchDataService elasticsearchDataService;
    private final SearchSpaceService searchSpaceService;

    @Autowired
    public ElasticsearchDataController(ElasticsearchDataService elasticsearchDataService,
                                     SearchSpaceService searchSpaceService) {
        this.elasticsearchDataService = elasticsearchDataService;
        this.searchSpaceService = searchSpaceService;
    }

    /**
     * 搜索ES数据
     *
     * 功能特性：
     * - 支持拼音搜索：可通过拼音查找中文内容（如：用"zhang"搜索"张三"）
     * - 多层级搜索：原字段精确匹配 + 拼音匹配 + 首字母匹配
     * - 智能权重：不同匹配策略具有不同的相关性权重
     * - 可配置搜索模式：AUTO（平衡）、STRICT（严格）、FUZZY（模糊）
     * - 语义搜索：基于向量的语义理解，例如"我想取钱"可以找到"附近网点"
     * - 混合搜索：结合关键词搜索和语义搜索，提供更精准的结果
     * - 智能降级：语义服务不可用时自动降级为传统关键词搜索
     *
     * @param request 搜索请求参数
     * @return 搜索结果
     */
    @Operation(summary = "搜索ES数据",
               description = "根据搜索空间和查询条件搜索Elasticsearch中的数据。支持拼音搜索、语义搜索、混合搜索等智能匹配功能。可根据查询内容自动选择最佳搜索策略。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "搜索成功",
                    content = @Content(schema = @Schema(implementation = SearchDataResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数无效",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<SearchDataResponse>> searchData(
            @Parameter(description = "搜索请求参数", required = true)
            @Valid @RequestBody SearchDataRequest request) {

        // 判断是单空间搜索还是多空间搜索
        boolean isMultiSpaceSearch = request.getSearchSpaceIds() != null && !request.getSearchSpaceIds().isEmpty();

        logger.info("搜索ES数据: searchSpaceId={}, searchSpaceIds={}, query={}, page={}, size={}, pinyin={}, pinyinMode={}, semantic={}, semanticMode={}, semanticWeight={}, multiSpace={}",
                request.getSearchSpaceId(), request.getSearchSpaceIds(), request.getQuery(), request.getPage(), request.getSize(),
                request.getEnablePinyinSearch(), request.getPinyinMode(),
                request.getEnableSemanticSearch(), request.getSemanticMode(), request.getSemanticWeight(), isMultiSpaceSearch);

        try {
            // 获取当前用户角色（从token中）
            String userRole = getCurrentUserRole();
            logger.debug("当前用户角色: {}", userRole);

            SearchDataResponse response;

            if (isMultiSpaceSearch) {
                // 多空间搜索
                response = elasticsearchDataService.searchMultipleSpaces(request, userRole);
            } else {
                // 单空间搜索（原有逻辑）
                if (request.getSearchSpaceId() == null || request.getSearchSpaceId().trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.badRequest("搜索空间ID不能为空"));
                }

                // 验证搜索空间是否存在
                SearchSpaceDTO searchSpace = searchSpaceService.getSearchSpace(Long.valueOf(request.getSearchSpaceId()));

                // 执行搜索
                response = elasticsearchDataService.searchData(request, searchSpace, userRole);
            }

            logger.info("搜索完成: searchSpaceId={}, total={}, returned={}, mode={}, actualType={}, totalTime={}ms",
                    request.getSearchSpaceId(), response.getTotal(), response.getData().size(),
                    response.getSearchMetadata() != null ? response.getSearchMetadata().getSearchMode() : "unknown",
                    response.getSearchMetadata() != null ? response.getSearchMetadata().getActualQueryType() : "unknown",
                    response.getSearchMetadata() != null ? response.getSearchMetadata().getTotalTime() : 0);

            return ResponseEntity.ok(ApiResponse.success("搜索成功", response));

        } catch (NumberFormatException e) {
            logger.warn("搜索空间ID格式无效: searchSpaceId={}", request.getSearchSpaceId());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("搜索空间ID格式无效"));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && (message.contains("不存在") || message.contains("未找到"))) {
                logger.warn("搜索空间不存在: searchSpaceId={}", request.getSearchSpaceId());
                return ResponseEntity.notFound().build();
            }

            logger.error("搜索数据失败: searchSpaceId={}", request.getSearchSpaceId(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("搜索失败: " + message));

        } catch (Exception e) {
            logger.error("搜索数据异常: searchSpaceId={}", request.getSearchSpaceId(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("系统异常，请稍后重试"));
        }
    }

    /**
     * 获取索引映射
     *
     * @param searchSpaceId 搜索空间ID
     * @return 索引映射信息
     */
    @Operation(summary = "获取索引映射", description = "获取搜索空间对应的Elasticsearch索引映射配置")
    @GetMapping("/mapping/{searchSpaceId}")
    public ResponseEntity<ApiResponse<IndexMappingResponse>> getIndexMapping(
            @Parameter(description = "搜索空间ID", required = true)
            @PathVariable String searchSpaceId) {

        logger.info("获取索引映射: searchSpaceId={}", searchSpaceId);

        try {
            SearchSpaceDTO searchSpace = searchSpaceService.getSearchSpace(Long.valueOf(searchSpaceId));
            IndexMappingResponse response = elasticsearchDataService.getIndexMapping(searchSpace);

            logger.info("索引映射获取成功: searchSpaceId={}, index={}",
                    searchSpaceId, response.getIndex());

            return ResponseEntity.ok(ApiResponse.success("获取映射成功", response));

        } catch (NumberFormatException e) {
            logger.warn("搜索空间ID格式无效: searchSpaceId={}", searchSpaceId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("搜索空间ID格式无效"));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && (message.contains("不存在") || message.contains("未找到"))) {
                logger.warn("搜索空间不存在: searchSpaceId={}", searchSpaceId);
                return ResponseEntity.notFound().build();
            }

            logger.error("获取索引映射失败: searchSpaceId={}", searchSpaceId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("获取映射失败: " + message));

        } catch (Exception e) {
            logger.error("获取索引映射异常: searchSpaceId={}", searchSpaceId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("系统异常，请稍后重试"));
        }
    }

    /**
     * 更新文档
     *
     * @param id 文档ID
     * @param request 更新请求
     * @return 更新结果
     */
    @Operation(summary = "更新文档", description = "更新Elasticsearch中的单个文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(schema = @Schema(implementation = UpdateDocumentResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "文档不存在",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "版本冲突",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    @PutMapping("/document/{id}/channels")
    public ResponseEntity<ApiResponse<UpdateDocumentResponse>> updateDocumentChannels(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "索引名称", required = true) @RequestParam String index,
            @Parameter(description = "渠道白名单", required = true) @RequestBody List<String> channels) {

        logger.info("更新文档渠道配置: id={}, index={}, channels={}", id, index, channels);

        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("request_channel_white_list", channels);

            UpdateDocumentResponse response = elasticsearchDataService.updateDocumentFields(id, index, fields);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            logger.error("更新文档渠道配置失败: id={}, index={}", id, index, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新文档渠道配置失败: " + e.getMessage()));
        }
    }

    /**
     * 更新文档角色白名单
     *
     * @param id 文档ID
     * @param index 索引名称
     * @param roles 角色白名单
     * @return 更新结果
     */
    @Operation(summary = "更新文档角色白名单", description = "更新文档的角色访问控制白名单")
    @PutMapping("/document/{id}/roles")
    public ResponseEntity<ApiResponse<UpdateDocumentResponse>> updateDocumentRoles(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "索引名称", required = true) @RequestParam String index,
            @Parameter(description = "角色白名单", required = true) @RequestBody List<String> roles) {

        logger.info("更新文档角色配置: id={}, index={}, roles={}", id, index, roles);

        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("request_role_white_list", roles);

            UpdateDocumentResponse response = elasticsearchDataService.updateDocumentFields(id, index, fields);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            logger.error("更新文档角色配置失败: id={}, index={}", id, index, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("更新文档角色配置失败: " + e.getMessage()));
        }
    }

    @PutMapping("/document/{id}")
    public ResponseEntity<ApiResponse<UpdateDocumentResponse>> updateDocument(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "更新请求", required = true)
            @Valid @RequestBody UpdateDocumentRequest request) {

        logger.info("更新文档: id={}, index={}, version={}",
                id, request.getIndex(), request.getVersion());

        try {
            UpdateDocumentResponse response = elasticsearchDataService.updateDocument(id, request);

            logger.info("文档更新成功: id={}, index={}, version={}, result={}",
                    id, request.getIndex(), response.getVersion(), response.getResult());

            return ResponseEntity.ok(ApiResponse.success("文档更新成功", response));

        } catch (RuntimeException e) {
            String message = e.getMessage();

            if (message != null && message.contains("version_conflict")) {
                logger.warn("文档版本冲突: id={}, index={}", id, request.getIndex());
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("文档已被其他用户修改，请刷新后重试"));
            }

            if (message != null && (message.contains("not_found") || message.contains("不存在"))) {
                logger.warn("文档不存在: id={}, index={}", id, request.getIndex());
                return ResponseEntity.notFound().build();
            }

            logger.error("更新文档失败: id={}, index={}", id, request.getIndex(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("更新失败: " + message));

        } catch (Exception e) {
            logger.error("更新文档异常: id={}, index={}", id, request.getIndex(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("系统异常，请稍后重试"));
        }
    }

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @param request 删除请求
     * @return 删除结果
     */
    @Operation(summary = "删除文档", description = "从Elasticsearch中删除单个文档")
    @DeleteMapping("/document/{id}")
    public ResponseEntity<ApiResponse<DeleteDocumentResponse>> deleteDocument(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "删除请求参数", required = true)
            @Valid @RequestBody DeleteDocumentRequest request) {

        logger.info("删除文档: id={}, index={}", id, request.getIndex());

        try {
            DeleteDocumentResponse response = elasticsearchDataService.deleteDocument(id, request);

            logger.info("文档删除成功: id={}, index={}, result={}",
                    id, request.getIndex(), response.getResult());

            return ResponseEntity.ok(ApiResponse.success("文档删除成功", response));

        } catch (RuntimeException e) {
            String message = e.getMessage();

            if (message != null && (message.contains("not_found") || message.contains("不存在"))) {
                logger.warn("文档不存在: id={}, index={}", id, request.getIndex());
                return ResponseEntity.notFound().build();
            }

            logger.error("删除文档失败: id={}, index={}", id, request.getIndex(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("删除失败: " + message));

        } catch (Exception e) {
            logger.error("删除文档异常: id={}, index={}", id, request.getIndex(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("系统异常，请稍后重试"));
        }
    }

    /**
     * 获取文档详情
     *
     * @param id 文档ID
     * @param index 索引名称
     * @param includeVectors 是否包含向量字段（用于编辑）
     * @return 文档详情
     */
    @Operation(summary = "获取文档详情", description = "根据ID获取Elasticsearch中的文档详情。可通过includeVectors参数控制是否返回向量字段（编辑时需要）")
    @GetMapping("/document/{id}")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocument(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "索引名称", required = true) @RequestParam String index,
            @Parameter(description = "是否包含向量字段", required = false) @RequestParam(defaultValue = "false") boolean includeVectors) {

        logger.info("获取文档详情: id={}, index={}, includeVectors={}", id, index, includeVectors);

        try {
            DocumentDetailResponse response = elasticsearchDataService.getDocument(id, index, includeVectors);

            logger.info("文档获取成功: id={}, index={}, includeVectors={}", id, index, includeVectors);

            return ResponseEntity.ok(ApiResponse.success("获取文档成功", response));

        } catch (RuntimeException e) {
            String message = e.getMessage();

            if (message != null && (message.contains("not_found") || message.contains("不存在"))) {
                logger.warn("文档不存在: id={}, index={}", id, index);
                return ResponseEntity.notFound().build();
            }

            logger.error("获取文档失败: id={}, index={}", id, index, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("获取文档失败: " + message));

        } catch (Exception e) {
            logger.error("获取文档异常: id={}, index={}", id, index, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("系统异常，请稍后重试"));
        }
    }

    /**
     * 批量操作文档
     *
     * @param request 批量操作请求
     * @return 批量操作结果
     */
    @Operation(summary = "批量操作文档", description = "批量执行文档的索引、更新、删除操作")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkOperation(
            @Parameter(description = "批量操作请求", required = true)
            @Valid @RequestBody BulkOperationRequest request) {

        logger.info("批量操作文档: operations={}", request.getOperations().size());

        try {
            BulkOperationResponse response = elasticsearchDataService.bulkOperation(request);

            logger.info("批量操作完成: operations={}, took={}ms, errors={}",
                    request.getOperations().size(), response.getTook(), response.hasErrors());

            return ResponseEntity.ok(ApiResponse.success("批量操作完成", response));

        } catch (Exception e) {
            logger.error("批量操作异常: operations={}", request.getOperations().size(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("批量操作失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前用户角色
     * 从Spring Security上下文中获取当前认证用户的角色
     *
     * @return 用户角色代码（如"ADMIN"、"USER"），如果未认证则返回null
     */
    private String getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

                // 从权限中提取角色
                return authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.substring(5)) // 移除"ROLE_"前缀
                    .findFirst()
                    .orElse(null);
            }
        } catch (Exception e) {
            logger.debug("获取用户角色失败", e);
        }
        return null;
    }

    /**
     * 简化的搜索接口
     * 用于搜索数据管理页面的基本关键词搜索
     */
    @Operation(summary = "简化的数据搜索", description = "只针对索引可检索字段进行简单的关键词匹配搜索")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "搜索成功",
            content = @Content(schema = @Schema(implementation = SimpleSearchResponse.class))
        )
    })
    @PostMapping("/simple-search")
    public ResponseEntity<ApiResponse<SimpleSearchResponse>> simpleSearch(
            @Valid @RequestBody SimpleSearchRequest request) {

        logger.info("收到简化搜索请求: searchSpaceId={}, keyword={}",
            request.getSearchSpaceId(), request.getKeyword());

        try {
            SimpleSearchResponse response = elasticsearchDataService.simpleSearch(request);

            return ResponseEntity.ok(ApiResponse.success(
                String.format("搜索成功，共找到%d条结果", response.getTotal()),
                response
            ));

        } catch (Exception e) {
            logger.error("简化搜索失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 设置文档为推荐
     *
     * @param id 文档ID
     * @param index 索引名称
     * @return 操作结果
     */
    @Operation(summary = "设置文档为推荐", description = "将文档设置为推荐，推荐的文档会在搜索结果中排在最前面")
    @PutMapping("/document/{id}/recommend")
    public ResponseEntity<ApiResponse<UpdateDocumentResponse>> recommendDocument(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "索引名称", required = true) @RequestParam String index) {

        logger.info("设置文档为推荐: id={}, index={}", id, index);

        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("recommend", 1);

            UpdateDocumentResponse response = elasticsearchDataService.updateDocumentFields(id, index, fields);

            logger.info("文档推荐设置成功: id={}, index={}", id, index);
            return ResponseEntity.ok(ApiResponse.success("推荐设置成功", response));

        } catch (RuntimeException e) {
            logger.error("设置文档推荐失败: id={}, index={}", id, index, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("推荐设置失败: " + e.getMessage()));
        }
    }

    /**
     * 取消文档推荐
     *
     * @param id 文档ID
     * @param index 索引名称
     * @return 操作结果
     */
    @Operation(summary = "取消文档推荐", description = "取消文档的推荐状态，文档将按正常相关性排序")
    @DeleteMapping("/document/{id}/recommend")
    public ResponseEntity<ApiResponse<UpdateDocumentResponse>> unrecommendDocument(
            @Parameter(description = "文档ID", required = true) @PathVariable String id,
            @Parameter(description = "索引名称", required = true) @RequestParam String index) {

        logger.info("取消文档推荐: id={}, index={}", id, index);

        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("recommend", 0);

            UpdateDocumentResponse response = elasticsearchDataService.updateDocumentFields(id, index, fields);

            logger.info("取消文档推荐成功: id={}, index={}", id, index);
            return ResponseEntity.ok(ApiResponse.success("取消推荐成功", response));

        } catch (RuntimeException e) {
            logger.error("取消文档推荐失败: id={}, index={}", id, index, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("取消推荐失败: " + e.getMessage()));
        }
    }
}