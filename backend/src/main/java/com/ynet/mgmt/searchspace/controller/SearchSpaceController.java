package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchspace.dto.*;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import com.ynet.mgmt.searchspace.service.FileValidationService;
import com.ynet.mgmt.searchspace.service.FileStorageService;
import com.ynet.mgmt.searchspace.service.ElasticsearchManager;
import com.ynet.mgmt.jsonimport.service.DataImportService;
import com.ynet.mgmt.jsonimport.service.JsonAnalysisService;
import com.ynet.mgmt.jsonimport.service.IndexConfigService;
import com.ynet.mgmt.jsonimport.dto.ImportExecuteRequest;
import com.ynet.mgmt.jsonimport.dto.ImportTaskStatus;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 搜索空间控制器
 * 提供搜索空间管理的REST API接口
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "搜索空间管理", description = "搜索空间管理相关API")
@RestController
@RequestMapping("/search-spaces")
public class SearchSpaceController {

    private static final Logger logger = LoggerFactory.getLogger(SearchSpaceController.class);

    private final SearchSpaceService searchSpaceService;
    private final FileValidationService fileValidationService;
    private final FileStorageService fileStorageService;
    private final ElasticsearchManager elasticsearchManager;
    private final DataImportService dataImportService;
    private final JsonAnalysisService jsonAnalysisService;
    private final IndexConfigService indexConfigService;
    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public SearchSpaceController(SearchSpaceService searchSpaceService, 
                                FileValidationService fileValidationService,
                                FileStorageService fileStorageService,
                                ElasticsearchManager elasticsearchManager,
                                DataImportService dataImportService,
                                JsonAnalysisService jsonAnalysisService,
                                IndexConfigService indexConfigService,
                                ElasticsearchClient elasticsearchClient) {
        this.searchSpaceService = searchSpaceService;
        this.fileValidationService = fileValidationService;
        this.fileStorageService = fileStorageService;
        this.elasticsearchManager = elasticsearchManager;
        this.dataImportService = dataImportService;
        this.jsonAnalysisService = jsonAnalysisService;
        this.indexConfigService = indexConfigService;
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 创建搜索空间
     *
     * @param createRequest 创建请求
     * @return 创建的搜索空间信息
     */
    @Operation(summary = "创建搜索空间", description = "创建新的搜索空间，包括名称、代码等基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = SearchSpaceDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceDTO>> createSearchSpace(
            @Parameter(description = "创建搜索空间请求", required = true)
            @Valid @RequestBody CreateSearchSpaceRequest createRequest) {
        logger.info("创建搜索空间: name={}, code={}", createRequest.getName(), createRequest.getCode());

        SearchSpaceDTO result = searchSpaceService.createSearchSpace(createRequest);

        logger.info("搜索空间创建成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(com.ynet.mgmt.common.dto.ApiResponse.created(result));
    }

    /**
     * 分页查询搜索空间列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @Operation(summary = "分页查询搜索空间列表", description = "根据关键词、分页参数查询搜索空间列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<PageResult<SearchSpaceDTO>>> listSearchSpaces(
            @Parameter(description = "查询请求参数") @Valid SearchSpaceQueryRequest request) {
        logger.debug("查询搜索空间列表: keyword={}, page={}, size={}",
                request.getKeyword(), request.getPage(), request.getSize());

        PageResult<SearchSpaceDTO> result = searchSpaceService.listSearchSpaces(request);

        logger.debug("搜索空间列表查询成功: totalElements={}, numberOfElements={}",
                result.getTotalElements(), result.getNumberOfElements());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success(result));
    }

    /**
     * 根据ID查询搜索空间
     *
     * @param id 搜索空间ID
     * @return 搜索空间信息
     */
    @Operation(summary = "根据ID查询搜索空间", description = "根据搜索空间ID获取详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = SearchSpaceDTO.class))),
            @ApiResponse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceDTO>> getSearchSpace(
            @Parameter(description = "搜索空间ID", required = true) @PathVariable Long id) {
        logger.debug("根据ID查询搜索空间: id={}", id);

        SearchSpaceDTO result = searchSpaceService.getSearchSpace(id);

        logger.debug("搜索空间查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success(result));
    }

    /**
     * 根据代码查询搜索空间
     *
     * @param code 搜索空间代码
     * @return 搜索空间信息
     */
    @Operation(summary = "根据代码查询搜索空间", description = "根据搜索空间代码获取详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = SearchSpaceDTO.class))),
            @ApiResponse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceDTO>> getSearchSpaceByCode(
            @Parameter(description = "搜索空间代码", required = true) @PathVariable String code) {
        logger.debug("根据代码查询搜索空间: code={}", code);

        SearchSpaceDTO result = searchSpaceService.getSearchSpaceByCode(code);

        logger.debug("搜索空间查询成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success(result));
    }

    /**
     * 更新搜索空间
     *
     * @param id 搜索空间ID
     * @param updateRequest 更新请求
     * @return 更新后的搜索空间信息
     */
    @Operation(summary = "更新搜索空间", description = "更新搜索空间的名称、描述等信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = SearchSpaceDTO.class))),
            @ApiResponse(responseCode = "400", description = "参数验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceDTO>> updateSearchSpace(
            @Parameter(description = "搜索空间ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新请求", required = true) @Valid @RequestBody UpdateSearchSpaceRequest updateRequest) {
        logger.info("更新搜索空间: id={}, name={}", id, updateRequest.getName());

        SearchSpaceDTO result = searchSpaceService.updateSearchSpace(id, updateRequest);

        logger.info("搜索空间更新成功: id={}, name={}, code={}",
                result.getId(), result.getName(), result.getCode());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.updated(result));
    }

    /**
     * 删除搜索空间
     *
     * @param id 搜索空间ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<Void>> deleteSearchSpace(@PathVariable Long id) {
        logger.info("删除搜索空间: id={}", id);

        searchSpaceService.deleteSearchSpace(id);

        logger.info("搜索空间删除成功: id={}", id);

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.deleted());
    }

    /**
     * 获取搜索空间统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceStatistics>> getStatistics() {
        logger.debug("获取搜索空间统计信息");

        SearchSpaceStatistics result = searchSpaceService.getStatistics();

        logger.debug("统计信息获取成功: totalCount={}, activeCount={}",
                result.getTotalSpaces(), result.getActiveSpaces());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success(result));
    }

    /**
     * 启用搜索空间
     *
     * @param id 搜索空间ID
     * @return 更新后的搜索空间信息
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceDTO>> enableSearchSpace(@PathVariable Long id) {
        logger.info("启用搜索空间: id={}", id);

        SearchSpaceDTO result = searchSpaceService.enableSearchSpace(id);

        logger.info("搜索空间启用成功: id={}, status={}",
                result.getId(), result.getStatus());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success("搜索空间启用成功", result));
    }

    /**
     * 禁用搜索空间
     *
     * @param id 搜索空间ID
     * @return 更新后的搜索空间信息
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<SearchSpaceDTO>> disableSearchSpace(@PathVariable Long id) {
        logger.info("禁用搜索空间: id={}", id);

        SearchSpaceDTO result = searchSpaceService.disableSearchSpace(id);

        logger.info("搜索空间禁用成功: id={}, status={}",
                result.getId(), result.getStatus());

        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success("搜索空间禁用成功", result));
    }

    /**
     * 检查代码是否可用
     *
     * @param code 搜索空间代码
     * @return 可用性检查结果
     */
    @GetMapping("/code/{code}/available")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<Map<String, Object>>> checkCodeAvailability(@PathVariable String code) {
        logger.debug("检查代码可用性: code={}", code);

        boolean available = searchSpaceService.isCodeAvailable(code);

        logger.debug("代码可用性检查完成: code={}, available={}", code, available);

        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("available", available);

        String message = available ? "代码可用" : "代码已被使用";
        return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success(message, data));
    }

    /**
     * 同步导入JSON数据（简化版）
     * 将文件上传和数据导入合并为一个同步操作
     */
    @Operation(
            summary = "同步导入JSON数据", 
            description = "一次性完成JSON文件上传、验证、解析和数据导入"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导入成功",
                    content = @Content(schema = @Schema(implementation = com.ynet.mgmt.common.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "文件验证失败",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "搜索空间不存在",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "413", description = "文件过大",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/{id}/import-sync")
    public ResponseEntity<com.ynet.mgmt.common.dto.ApiResponse<ImportSyncResponse>> importJsonFileSync(
            @Parameter(description = "搜索空间ID", required = true) @PathVariable Long id,
            @Parameter(description = "JSON文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "导入模式：APPEND(追加) 或 REPLACE(替换)", required = false) 
            @RequestParam(value = "mode", defaultValue = "APPEND") String mode,
            @Parameter(description = "批处理大小", required = false) 
            @RequestParam(value = "batchSize", defaultValue = "1000") Integer batchSize,
            @Parameter(description = "错误处理：STOP_ON_ERROR 或 SKIP_ERROR", required = false)
            @RequestParam(value = "errorHandling", defaultValue = "SKIP_ERROR") String errorHandling) {
        
        logger.info("开始同步JSON文件导入: searchSpaceId={}, fileName={}, fileSize={}, mode={}", 
                id, file.getOriginalFilename(), file.getSize(), mode);

        long startTimeMs = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // 1. 验证搜索空间是否存在
            SearchSpaceDTO searchSpace = searchSpaceService.getSearchSpace(id);
            logger.debug("搜索空间验证通过: id={}, name={}", searchSpace.getId(), searchSpace.getName());

            // 2. 验证文件
            FileValidationResult validationResult = fileValidationService.validateJsonFile(file);
            if (!validationResult.isValid()) {
                logger.warn("文件验证失败: searchSpaceId={}, fileName={}, error={}", 
                        id, file.getOriginalFilename(), validationResult.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body(com.ynet.mgmt.common.dto.ApiResponse.badRequest(validationResult.getErrorMessage()));
            }

            // 3. 解析JSON文件内容
            String jsonContent;
            try {
                jsonContent = new String(file.getBytes(), "UTF-8");
            } catch (IOException e) {
                logger.error("读取文件内容失败: searchSpaceId={}, fileName={}", id, file.getOriginalFilename(), e);
                return ResponseEntity.badRequest()
                        .body(com.ynet.mgmt.common.dto.ApiResponse.badRequest("读取文件内容失败"));
            }

            // 4. 同步执行导入处理
            ImportSyncResponse response = performSyncImport(
                    searchSpace, jsonContent, validationResult, mode, batchSize, errorHandling, startTime);

            long duration = System.currentTimeMillis() - startTimeMs;
            logger.info("同步JSON导入完成: searchSpaceId={}, fileName={}, duration={}ms, success={}, imported={}, errors={}", 
                    id, file.getOriginalFilename(), duration, response.isSuccess(), 
                    response.getSuccessRecords(), response.getFailedRecords());

            if (response.isSuccess()) {
                return ResponseEntity.ok(com.ynet.mgmt.common.dto.ApiResponse.success("导入完成", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(com.ynet.mgmt.common.dto.ApiResponse.badRequest(response.getMessage()));
            }

        } catch (RuntimeException e) {
            String message = e.getMessage();
            if (message != null && (message.contains("不存在") || message.contains("未找到"))) {
                logger.warn("搜索空间不存在: searchSpaceId={}", id);
                return ResponseEntity.notFound().build();
            }
            
            logger.error("同步导入过程中发生未知错误: searchSpaceId={}, fileName={}", 
                    id, file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.ynet.mgmt.common.dto.ApiResponse.error("导入失败: " + message));
            
        } catch (Exception e) {
            logger.error("同步导入异常: searchSpaceId={}, fileName={}", 
                    id, file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.ynet.mgmt.common.dto.ApiResponse.error("系统异常，请稍后重试"));
        }
    }

    /**
     * 执行同步导入处理
     * 完全复用DataImportService的逻辑，将ImportTaskStatus转换为ImportSyncResponse
     */
    private ImportSyncResponse performSyncImport(
            SearchSpaceDTO searchSpace, 
            String jsonContent, 
            FileValidationResult validationResult,
            String mode,
            Integer batchSize, 
            String errorHandling,
            LocalDateTime startTime) {
        
        try {
            logger.info("开始执行DataImportService同步导入: searchSpace={}, mode={}", searchSpace.getCode(), mode);
            
            // 调用DataImportService的同步导入方法
            ImportTaskStatus taskStatus = dataImportService.executeImportFromJsonContent(
                    searchSpace.getId(), 
                    jsonContent, 
                    mode, 
                    batchSize, 
                    errorHandling
            );
            
            // 将ImportTaskStatus转换为ImportSyncResponse
            return convertTaskStatusToSyncResponse(taskStatus, startTime);
            
        } catch (Exception e) {
            logger.error("DataImportService同步导入失败", e);
            return ImportSyncResponse.failure("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 将ImportTaskStatus转换为ImportSyncResponse
     */
    private ImportSyncResponse convertTaskStatusToSyncResponse(ImportTaskStatus taskStatus, LocalDateTime startTime) {
        LocalDateTime endTime = taskStatus.getEndTime() != null ? taskStatus.getEndTime() : LocalDateTime.now();
        long processingTime = java.time.Duration.between(startTime, endTime).toMillis();
        
        boolean success = taskStatus.getState() == com.ynet.mgmt.jsonimport.enums.ImportTaskState.COMPLETED;
        Integer totalRecords = taskStatus.getTotalRecords();
        Integer successRecords = taskStatus.getSuccessCount();
        Integer failedRecords = taskStatus.getErrorCount();
        Integer skippedRecords = 0; // ImportTaskStatus中没有跳过记录的概念
        
        String message;
        if (success) {
            message = String.format("导入完成：成功 %d 条，失败 %d 条", 
                    successRecords != null ? successRecords : 0, 
                    failedRecords != null ? failedRecords : 0);
        } else {
            message = taskStatus.getErrorMessage() != null ? taskStatus.getErrorMessage() : "导入失败";
        }
        
        // 构建错误和警告信息
        List<String> errors = null;
        List<String> warnings = null;
        
        if (taskStatus.getErrorDetails() != null && !taskStatus.getErrorDetails().isEmpty()) {
            errors = new ArrayList<>(taskStatus.getErrorDetails());
        }
        
        // 如果有部分失败，添加警告信息
        if (success && failedRecords != null && failedRecords > 0) {
            warnings = List.of("部分记录导入失败，请检查数据格式");
        }
        
        logger.info("转换任务状态完成: success={}, totalRecords={}, successRecords={}, failedRecords={}, processingTime={}ms", 
                success, totalRecords, successRecords, failedRecords, processingTime);
        
        return new ImportSyncResponse(
                success,
                totalRecords,
                successRecords,
                failedRecords,
                skippedRecords,
                startTime,
                endTime,
                processingTime,
                errors,
                warnings,
                message
        );
    }

}