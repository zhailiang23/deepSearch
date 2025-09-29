package com.ynet.mgmt.jsonimport.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.jsonimport.dto.ImportExecuteRequest;
import com.ynet.mgmt.jsonimport.dto.ImportTaskStatus;
import com.ynet.mgmt.jsonimport.enums.ImportTaskState;
import com.ynet.mgmt.jsonimport.model.IndexMappingConfig;
import com.ynet.mgmt.jsonimport.model.JsonSchemaAnalysis;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.service.ElasticsearchManager;
import com.ynet.mgmt.searchspace.service.FileStorageService;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import com.ynet.mgmt.searchdata.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据导入服务
 * 整合JSON分析、索引配置、数据导入的完整工作流程
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataImportService {

    private final JsonAnalysisService jsonAnalysisService;
    private final IndexConfigService indexConfigService;
    private final ElasticsearchManager elasticsearchManager;
    private final ElasticsearchClient elasticsearchClient;
    private final SearchSpaceService searchSpaceService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;

    // 任务状态缓存
    private final Map<String, ImportTaskStatus> taskStatusCache = new ConcurrentHashMap<>();

    // 任务取消标志
    private final Set<String> cancelledTasks = ConcurrentHashMap.newKeySet();

    /**
     * 同步执行JSON数据导入
     * 复用异步导入的所有逻辑，但直接返回结果而不是使用CompletableFuture
     *
     * @param searchSpaceId 搜索空间ID
     * @param request       导入请求
     * @return 任务状态
     */
    public ImportTaskStatus executeImportSync(Long searchSpaceId, ImportExecuteRequest request) {
        String taskId = request.getTaskId();

        log.info("开始执行同步数据导入: searchSpaceId={}, taskId={}, mode={}",
                searchSpaceId, taskId, request.getMode());

        // 初始化任务状态
        ImportTaskStatus status = initializeTaskStatus(searchSpaceId, taskId);
        updateTaskStatus(status);

        try {
            // 验证搜索空间
            SearchSpace searchSpace = validateSearchSpace(searchSpaceId);

            // 第1步：分析JSON数据
            status.setState(ImportTaskState.ANALYZING_DATA);
            status.setStatusMessage("正在分析JSON数据结构...");
            updateTaskStatus(status);

            JsonSchemaAnalysis analysis = analyzeJsonData(taskId, searchSpaceId);
            status.setTotalRecords(analysis.getTotalRecords());
            status.setTotalBatches((int) Math.ceil((double) analysis.getTotalRecords() / request.getBatchSize()));

            // 检查是否被取消
            checkCancellation(taskId);

            // 第2步：生成索引配置
            status.setState(ImportTaskState.CREATING_INDEX);
            status.setStatusMessage("正在生成索引配置和创建索引...");
            updateTaskStatus(status);

            IndexMappingConfig indexConfig = generateIndexConfig(searchSpace.getCode(), analysis);
            createElasticsearchIndex(indexConfig, request.getMode());

            // 检查是否被取消
            checkCancellation(taskId);

            // 第3步：导入数据
            status.setState(ImportTaskState.PROCESSING_DATA);
            status.setStatusMessage("正在批量导入数据...");
            updateTaskStatus(status);

            ImportTaskStatus.ImportResultSummary resultSummary = importData(
                    taskId, searchSpaceId, indexConfig, analysis, request, status);

            // 第4步：优化索引（可选）
            if (Boolean.TRUE.equals(request.getEnableIndexOptimization())) {
                status.setState(ImportTaskState.OPTIMIZING_INDEX);
                status.setStatusMessage("正在优化索引...");
                updateTaskStatus(status);
                optimizeIndex(indexConfig.getIndexName());
            }

            // 完成导入
            status.setState(ImportTaskState.COMPLETED);
            status.setEndTime(LocalDateTime.now());
            status.setStatusMessage("数据导入完成");
            status.setResultSummary(resultSummary);
            status.updateProgressPercentage();
            updateTaskStatus(status);
            
            // 更新搜索空间的文档统计
            if (status.getSuccessCount() != null && status.getSuccessCount() > 0) {
                try {
                    searchSpaceService.updateImportStats(searchSpaceId, status.getSuccessCount());
                    log.info("更新搜索空间文档统计: searchSpaceId={}, successCount={}", 
                            searchSpaceId, status.getSuccessCount());
                } catch (Exception e) {
                    log.warn("更新搜索空间文档统计失败: searchSpaceId={}, error={}", 
                            searchSpaceId, e.getMessage());
                }
            }

            log.info("同步数据导入完成: taskId={}, totalRecords={}, successCount={}",
                    taskId, analysis.getTotalRecords(), status.getSuccessCount());

            return status;

        } catch (Exception e) {
            log.error("同步数据导入失败: taskId={}, error={}", taskId, e.getMessage(), e);

            status.setState(ImportTaskState.FAILED);
            status.setEndTime(LocalDateTime.now());
            status.setErrorMessage(e.getMessage());
            status.setStatusMessage("导入失败: " + e.getMessage());
            updateTaskStatus(status);

            throw new RuntimeException("数据导入失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            cleanupTemporaryFiles(taskId, searchSpaceId);
        }
    }

    /**
     * 基于JSON内容创建临时文件并执行同步导入
     * 为SearchSpaceController的同步导入提供便捷方法
     */
    public ImportTaskStatus executeImportFromJsonContent(Long searchSpaceId, String jsonContent, 
            String mode, Integer batchSize, String errorHandling) throws IOException {
        
        // 生成临时任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 创建临时文件
        String fileName = "sync-import-" + taskId + ".json";
        Path tempFilePath = fileStorageService.getTemporaryFilePath(fileName);
        
        // 确保临时目录存在
        if (!Files.exists(tempFilePath.getParent())) {
            Files.createDirectories(tempFilePath.getParent());
        }
        
        // 写入文件内容
        Files.write(tempFilePath, jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        log.debug("创建临时文件用于同步导入: {}", tempFilePath);
        
        // 构建导入请求
        ImportExecuteRequest request = ImportExecuteRequest.builder()
                .taskId(taskId)
                .mode(ImportExecuteRequest.ImportMode.valueOf(mode.toUpperCase()))
                .batchSize(batchSize)
                .errorHandling(ImportExecuteRequest.ErrorHandlingStrategy.valueOf(errorHandling.toUpperCase()))
                .enableIndexOptimization(false) // 同步导入默认不开启索引优化
                .build();
        
        try {
            return executeImportSync(searchSpaceId, request);
        } finally {
            // 确保清理临时文件
            try {
                Files.deleteIfExists(tempFilePath);
                log.debug("清理临时文件: {}", tempFilePath);
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 执行JSON数据导入
     *
     * @param searchSpaceId 搜索空间ID
     * @param request       导入请求
     * @return 任务状态
     */
    @Async("dataImportTaskExecutor")
    public CompletableFuture<ImportTaskStatus> executeImport(Long searchSpaceId, ImportExecuteRequest request) {
        String taskId = request.getTaskId();

        log.info("开始执行数据导入: searchSpaceId={}, taskId={}, mode={}",
                searchSpaceId, taskId, request.getMode());

        // 初始化任务状态
        ImportTaskStatus status = initializeTaskStatus(searchSpaceId, taskId);
        updateTaskStatus(status);

        try {
            // 验证搜索空间
            SearchSpace searchSpace = validateSearchSpace(searchSpaceId);

            // 第1步：分析JSON数据
            status.setState(ImportTaskState.ANALYZING_DATA);
            status.setStatusMessage("正在分析JSON数据结构...");
            updateTaskStatus(status);

            JsonSchemaAnalysis analysis = analyzeJsonData(taskId, searchSpaceId);
            status.setTotalRecords(analysis.getTotalRecords());
            status.setTotalBatches((int) Math.ceil((double) analysis.getTotalRecords() / request.getBatchSize()));

            // 检查是否被取消
            checkCancellation(taskId);

            // 第2步：生成索引配置
            status.setState(ImportTaskState.CREATING_INDEX);
            status.setStatusMessage("正在生成索引配置和创建索引...");
            updateTaskStatus(status);

            IndexMappingConfig indexConfig = generateIndexConfig(searchSpace.getCode(), analysis);
            createElasticsearchIndex(indexConfig, request.getMode());

            // 检查是否被取消
            checkCancellation(taskId);

            // 第3步：导入数据
            status.setState(ImportTaskState.PROCESSING_DATA);
            status.setStatusMessage("正在批量导入数据...");
            updateTaskStatus(status);

            ImportTaskStatus.ImportResultSummary resultSummary = importData(
                    taskId, searchSpaceId, indexConfig, analysis, request, status);

            // 第4步：优化索引（可选）
            if (Boolean.TRUE.equals(request.getEnableIndexOptimization())) {
                status.setState(ImportTaskState.OPTIMIZING_INDEX);
                status.setStatusMessage("正在优化索引...");
                updateTaskStatus(status);
                optimizeIndex(indexConfig.getIndexName());
            }

            // 完成导入
            status.setState(ImportTaskState.COMPLETED);
            status.setEndTime(LocalDateTime.now());
            status.setStatusMessage("数据导入完成");
            status.setResultSummary(resultSummary);
            status.updateProgressPercentage();
            updateTaskStatus(status);

            log.info("数据导入完成: taskId={}, totalRecords={}, successCount={}",
                    taskId, analysis.getTotalRecords(), status.getSuccessCount());

            return CompletableFuture.completedFuture(status);

        } catch (Exception e) {
            log.error("数据导入失败: taskId={}, error={}", taskId, e.getMessage(), e);

            status.setState(ImportTaskState.FAILED);
            status.setEndTime(LocalDateTime.now());
            status.setErrorMessage(e.getMessage());
            status.setStatusMessage("导入失败: " + e.getMessage());
            updateTaskStatus(status);

            return CompletableFuture.failedFuture(e);
        } finally {
            // 清理临时文件
            cleanupTemporaryFiles(taskId, searchSpaceId);
        }
    }

    /**
     * 获取导入任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    public ImportTaskStatus getImportStatus(String taskId) {
        return taskStatusCache.get(taskId);
    }

    /**
     * 取消导入任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public boolean cancelImport(String taskId) {
        ImportTaskStatus status = taskStatusCache.get(taskId);
        if (status == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return false;
        }

        if (!status.getState().isCancellable()) {
            log.warn("任务不能取消: taskId={}, state={}", taskId, status.getState());
            return false;
        }

        log.info("取消导入任务: taskId={}", taskId);
        cancelledTasks.add(taskId);

        status.setState(ImportTaskState.CANCELLED);
        status.setEndTime(LocalDateTime.now());
        status.setStatusMessage("任务已被取消");
        updateTaskStatus(status);

        return true;
    }

    /**
     * 初始化任务状态
     */
    private ImportTaskStatus initializeTaskStatus(Long searchSpaceId, String taskId) {
        return ImportTaskStatus.builder()
                .taskId(taskId)
                .searchSpaceId(searchSpaceId)
                .state(ImportTaskState.PENDING)
                .totalRecords(0)
                .processedRecords(0)
                .successCount(0)
                .errorCount(0)
                .currentBatch(0)
                .totalBatches(0)
                .progressPercentage(0.0)
                .startTime(LocalDateTime.now())
                .statusMessage("准备开始导入...")
                .errorDetails(new ArrayList<>())
                .build();
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(ImportTaskStatus status) {
        taskStatusCache.put(status.getTaskId(), status);
    }

    /**
     * 验证搜索空间
     */
    private SearchSpace validateSearchSpace(Long searchSpaceId) {
        // 这里需要根据实际的SearchSpaceService实现来调用
        // 假设SearchSpaceService有getSearchSpaceEntity方法
        try {
            // 临时解决方案：通过DTO转换获取实体信息
            var searchSpaceDTO = searchSpaceService.getSearchSpace(searchSpaceId);
            if (searchSpaceDTO == null) {
                throw new RuntimeException("搜索空间不存在: " + searchSpaceId);
            }

            // 创建临时SearchSpace对象用于获取必要信息
            SearchSpace searchSpace = new SearchSpace();
            searchSpace.setId(searchSpaceDTO.getId());
            searchSpace.setCode(searchSpaceDTO.getCode());
            searchSpace.setName(searchSpaceDTO.getName());
            return searchSpace;

        } catch (Exception e) {
            throw new RuntimeException("搜索空间验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分析JSON数据 - 重载方法支持直接文件名
     */
    private JsonSchemaAnalysis analyzeJsonData(String taskId, Long searchSpaceId) throws IOException {
        // 尝试通过taskId构建文件名
        String fileName = "sync-import-" + taskId + ".json";
        Path filePath = fileStorageService.getTemporaryFilePath(fileName);
        
        // 如果文件不存在，尝试旧的方式
        if (!Files.exists(filePath)) {
            filePath = fileStorageService.getTemporaryFilePath(taskId, searchSpaceId);
        }
        
        if (!Files.exists(filePath)) {
            throw new RuntimeException("临时文件不存在: " + filePath);
        }

        String jsonContent = Files.readString(filePath);
        List<Map<String, Object>> jsonData = parseJsonData(jsonContent);

        return jsonAnalysisService.analyzeJsonArray(jsonData);
    }

    /**
     * 解析JSON数据
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonData(String jsonContent) throws IOException {
        Object parsed = objectMapper.readValue(jsonContent, Object.class);

        if (parsed instanceof List) {
            return (List<Map<String, Object>>) parsed;
        } else if (parsed instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) parsed;
            // 尝试找到数组字段
            for (Object value : dataMap.values()) {
                if (value instanceof List) {
                    return (List<Map<String, Object>>) value;
                }
            }
            // 如果没有找到数组，将单个对象包装成数组
            return List.of(dataMap);
        } else {
            throw new RuntimeException("不支持的JSON数据格式");
        }
    }

    /**
     * 生成索引配置
     */
    private IndexMappingConfig generateIndexConfig(String searchSpaceCode, JsonSchemaAnalysis analysis) {
        IndexMappingConfig config = indexConfigService.generateIndexConfig(searchSpaceCode, analysis);

        if (!indexConfigService.validateIndexConfig(config)) {
            throw new RuntimeException("生成的索引配置无效");
        }

        return config;
    }

    /**
     * 创建Elasticsearch索引
     */
    private void createElasticsearchIndex(IndexMappingConfig config, ImportExecuteRequest.ImportMode mode) throws IOException {
        String indexName = config.getIndexName();
        boolean indexExists = elasticsearchManager.indexExists(indexName);

        log.info("处理索引创建: indexName={}, mode={}, exists={}", indexName, mode, indexExists);

        if (indexExists) {
            if (mode == ImportExecuteRequest.ImportMode.REPLACE) {
                // 替换模式：删除现有索引后重新创建
                log.info("替换模式：删除现有索引 {}", indexName);
                elasticsearchManager.deleteIndex(indexName);
            } else if (mode == ImportExecuteRequest.ImportMode.APPEND) {
                // 追加模式：直接使用现有索引
                log.info("追加模式：使用现有索引 {}", indexName);
                return;
            }
        }

        // 创建新索引（索引不存在或者是替换模式）
        log.info("创建新索引: {}", indexName);

        // 获取完整的Elasticsearch映射配置（包含分析器）
        Map<String, Object> mappingConfig = config.toElasticsearchMapping();

        CreateIndexRequest request = CreateIndexRequest.of(builder -> {
            // 基础设置
            builder.index(indexName);

            // 应用完整的settings配置（包含分析器）
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) mappingConfig.get("settings");
            if (settings != null) {
                builder.settings(s -> {
                    // 应用基础索引设置
                    s.numberOfShards(String.valueOf(config.getSettings().getNumberOfShards()))
                     .numberOfReplicas(String.valueOf(config.getSettings().getNumberOfReplicas()))
                     .refreshInterval(t -> t.time(config.getSettings().getRefreshInterval()));

                    // 应用分析器配置（如果存在）
                    if (settings.containsKey("analysis")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> analysis = (Map<String, Object>) settings.get("analysis");

                        // 直接将analysis作为JsonValue传递给ES客户端
                        try {
                            s.withJson(new java.io.StringReader(
                                objectMapper.writeValueAsString(Map.of("analysis", analysis))
                            ));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                            log.warn("Failed to serialize analysis configuration: {}", e.getMessage());
                        }
                    }

                    return s;
                });
            }

            // 应用字段映射
            builder.mappings(m -> m
                .dynamic(co.elastic.clients.elasticsearch._types.mapping.DynamicMapping.True)
                .properties(buildPropertiesFromConfig(config.getFieldMappings()))
            );

            return builder;
        });

        elasticsearchClient.indices().create(request);
        log.info("Elasticsearch索引创建成功（包含拼音分析器配置）: {}", indexName);
    }

    /**
     * 从配置构建Elasticsearch属性映射
     */
    private Map<String, co.elastic.clients.elasticsearch._types.mapping.Property> buildPropertiesFromConfig(
            Map<String, IndexMappingConfig.FieldMapping> fieldMappings) {

        Map<String, co.elastic.clients.elasticsearch._types.mapping.Property> properties = new HashMap<>();

        for (Map.Entry<String, IndexMappingConfig.FieldMapping> entry : fieldMappings.entrySet()) {
            String fieldName = entry.getKey();
            IndexMappingConfig.FieldMapping mapping = entry.getValue();

            co.elastic.clients.elasticsearch._types.mapping.Property property = buildPropertyFromMapping(mapping);
            properties.put(fieldName, property);
        }

        return properties;
    }

    /**
     * 从字段映射构建Elasticsearch属性
     */
    private co.elastic.clients.elasticsearch._types.mapping.Property buildPropertyFromMapping(
            IndexMappingConfig.FieldMapping mapping) {

        return switch (mapping.getElasticsearchType()) {
            case "text" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .text(t -> {
                        t.analyzer(mapping.getAnalyzer());

                        // 添加搜索分析器支持
                        if (mapping.getSearchAnalyzer() != null) {
                            t.searchAnalyzer(mapping.getSearchAnalyzer());
                        }

                        if (mapping.getFields() != null) {
                            Map<String, co.elastic.clients.elasticsearch._types.mapping.Property> fields = new HashMap<>();
                            for (Map.Entry<String, IndexMappingConfig.FieldMapping> fieldEntry : mapping.getFields().entrySet()) {
                                fields.put(fieldEntry.getKey(), buildPropertyFromMapping(fieldEntry.getValue()));
                            }
                            t.fields(fields);
                        }
                        return t;
                    })
            );
            case "keyword" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .keyword(k -> k.index(mapping.getIndex()))
            );
            case "long" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .long_(l -> l.index(mapping.getIndex()))
            );
            case "double" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .double_(d -> d.index(mapping.getIndex()))
            );
            case "boolean" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .boolean_(b -> b.index(mapping.getIndex()))
            );
            case "date" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .date(d -> d.format(mapping.getFormat()).index(mapping.getIndex()))
            );
            case "object" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .object(o -> o.enabled(true))
            );
            case "dense_vector" -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .denseVector(dv -> {
                        if (mapping.getVectorConfig() != null) {
                            IndexMappingConfig.VectorFieldConfig vectorConfig = mapping.getVectorConfig();
                            dv.dims(vectorConfig.getDims());

                            if (vectorConfig.getSimilarity() != null) {
                                // 设置相似度算法
                                dv.similarity(vectorConfig.getSimilarity());
                            }

                            if (vectorConfig.getIndex() != null) {
                                dv.index(vectorConfig.getIndex());
                            }

                            // 如果是HNSW索引，添加索引选项
                            if ("hnsw".equals(vectorConfig.getIndexType()) && vectorConfig.getIndex() != null && vectorConfig.getIndex()) {
                                dv.indexOptions(indexOpts -> {
                                    indexOpts.type("hnsw");
                                    if (vectorConfig.getM() != null) {
                                        indexOpts.m(vectorConfig.getM());
                                    }
                                    if (vectorConfig.getEfConstruction() != null) {
                                        indexOpts.efConstruction(vectorConfig.getEfConstruction());
                                    }
                                    return indexOpts;
                                });
                            }
                        }
                        return dv;
                    })
            );
            default -> co.elastic.clients.elasticsearch._types.mapping.Property.of(p -> p
                    .text(t -> t.analyzer("standard"))
            );
        };
    }

    /**
     * 导入数据
     */
    private ImportTaskStatus.ImportResultSummary importData(
            String taskId, Long searchSpaceId, IndexMappingConfig indexConfig,
            JsonSchemaAnalysis analysis, ImportExecuteRequest request, ImportTaskStatus status) throws IOException {

        long startTime = System.currentTimeMillis();
        String indexName = indexConfig.getIndexName();
        int batchSize = request.getBatchSize();
        List<String> errorDetails = new ArrayList<>();

        // 读取JSON数据 - 修复临时文件路径
        String fileName = "sync-import-" + taskId + ".json";
        Path filePath = fileStorageService.getTemporaryFilePath(fileName);
        
        // 如果新格式文件不存在，尝试旧格式
        if (!Files.exists(filePath)) {
            filePath = fileStorageService.getTemporaryFilePath(taskId, searchSpaceId);
        }
        
        if (!Files.exists(filePath)) {
            throw new RuntimeException("未找到任务对应的临时文件: " + filePath);
        }

        String jsonContent = Files.readString(filePath);
        List<Map<String, Object>> jsonData = parseJsonData(jsonContent);

        // 分批导入
        int totalRecords = jsonData.size();
        int totalBatches = (int) Math.ceil((double) totalRecords / batchSize);
        int successCount = 0;
        int errorCount = 0;

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            // 检查是否被取消
            checkCancellation(taskId);

            int startIndex = batchIndex * batchSize;
            int endIndex = Math.min(startIndex + batchSize, totalRecords);
            List<Map<String, Object>> batchData = jsonData.subList(startIndex, endIndex);

            try {
                // 执行批量导入
                BulkResponse response = performBulkIndex(indexName, batchData, searchSpaceId, indexConfig);

                // 处理响应结果
                if (response.errors()) {
                    log.warn("批次 {} 存在错误，详细分析批量响应", batchIndex + 1);
                    
                    // 详细分析每个响应项
                    int batchSuccessCount = 0;
                    int batchErrorCount = 0;
                    
                    for (int itemIndex = 0; itemIndex < response.items().size(); itemIndex++) {
                        var item = response.items().get(itemIndex);
                        
                        if (item.error() != null) {
                            batchErrorCount++;
                            log.error("文档 {} 导入失败: 错误类型={}, 错误原因={}, 错误详情={}", 
                                itemIndex, 
                                item.error().type(),
                                item.error().reason(),
                                item.error().toString());
                        } else {
                            batchSuccessCount++;
                            log.debug("文档 {} 导入成功", itemIndex);
                        }
                    }
                    
                    log.info("批次 {} 详细结果: 成功={}, 失败={}", batchIndex + 1, batchSuccessCount, batchErrorCount);
                    
                    errorCount += batchErrorCount;
                    successCount += batchSuccessCount;
                    
                    // 记录详细错误信息
                    if (batchErrorCount > 0) {
                        String errorMsg = String.format("批次 %d 有 %d 个文档导入失败", batchIndex + 1, batchErrorCount);
                        errorDetails.add(errorMsg);
                    }
                    
                    if (request.getErrorHandling() == ImportExecuteRequest.ErrorHandlingStrategy.STOP_ON_ERROR) {
                        throw new RuntimeException("遇到错误停止导入，批次: " + (batchIndex + 1));
                    }
                } else {
                    successCount += batchData.size();
                    log.info("批次 {} 全部成功: 导入 {} 个文档", batchIndex + 1, batchData.size());
                }

                // 更新进度
                status.setCurrentBatch(batchIndex + 1);
                status.setProcessedRecords(endIndex);
                status.setSuccessCount(successCount);
                status.setErrorCount(errorCount);
                status.updateProgressPercentage();
                status.setStatusMessage(String.format("正在导入数据... (%d/%d 批次)", batchIndex + 1, totalBatches));
                updateTaskStatus(status);

                log.debug("批次 {}/{} 导入完成: 成功={}, 失败={}", batchIndex + 1, totalBatches, batchData.size() - errorCount, errorCount);

            } catch (Exception e) {
                String error = "批次 " + (batchIndex + 1) + " 导入失败: " + e.getMessage();
                errorDetails.add(error);
                errorCount += batchData.size();

                if (request.getErrorHandling() == ImportExecuteRequest.ErrorHandlingStrategy.STOP_ON_ERROR) {
                    throw new RuntimeException(error, e);
                }

                log.error("批次 {} 导入失败", batchIndex + 1, e);
            }
        }

        // 更新最终错误详情
        status.setErrorDetails(errorDetails.stream().limit(20).collect(Collectors.toList()));

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        return ImportTaskStatus.ImportResultSummary.builder()
                .success(errorCount == 0 || request.getErrorHandling() == ImportExecuteRequest.ErrorHandlingStrategy.SKIP_ERROR)
                .durationMs(durationMs)
                .createdIndexName(indexName)
                .indexedDocuments(successCount)
                .averageSpeed(successCount / (durationMs / 1000.0))
                .indexSize(0L) // TODO: 获取实际索引大小
                .build();
    }

    /**
     * 执行批量索引操作
     */
    private BulkResponse performBulkIndex(String indexName, List<Map<String, Object>> batchData, Long searchSpaceId, IndexMappingConfig indexConfig) throws IOException {
        List<BulkOperation> operations = new ArrayList<>();

        for (int i = 0; i < batchData.size(); i++) {
            Map<String, Object> document = new HashMap<>(batchData.get(i));

            // 添加系统字段
            document.put("_searchSpaceId", searchSpaceId);
            document.put("_documentId", UUID.randomUUID().toString());
            // 修复：使用符合Elasticsearch strict_date_time格式的时间戳
            document.put("_importTimestamp", LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT));
            document.put("_dataVersion", 1);

            // 生成向量字段
            generateVectorFields(document, indexConfig);

            IndexOperation<Map<String, Object>> indexOp = IndexOperation.of(io -> io
                    .index(indexName)
                    .id(UUID.randomUUID().toString())
                    .document(document)
            );

            operations.add(BulkOperation.of(bo -> bo.index(indexOp)));
        }

        BulkRequest request = BulkRequest.of(br -> br
                .index(indexName)
                .operations(operations)
        );

        return elasticsearchClient.bulk(request);
    }

    /**
     * 优化索引
     */
    private void optimizeIndex(String indexName) {
        try {
            log.info("开始优化索引: {}", indexName);
            // TODO: 实现索引优化逻辑
            // 例如：强制合并段、刷新索引等
            log.info("索引优化完成: {}", indexName);
        } catch (Exception e) {
            log.warn("索引优化失败: {}", indexName, e);
            // 优化失败不影响导入结果
        }
    }

    /**
     * 检查任务是否被取消
     */
    private void checkCancellation(String taskId) {
        if (cancelledTasks.contains(taskId)) {
            cancelledTasks.remove(taskId);
            throw new RuntimeException("任务已被取消");
        }
    }

    /**
     * 为文档生成向量字段
     */
    private void generateVectorFields(Map<String, Object> document, IndexMappingConfig indexConfig) {
        if (!embeddingService.isServiceAvailable()) {
            log.debug("嵌入服务不可用，跳过向量生成");
            return;
        }

        try {
            // 遍历索引映射配置，查找需要生成向量的文本字段
            for (Map.Entry<String, IndexMappingConfig.FieldMapping> entry : indexConfig.getFieldMappings().entrySet()) {
                String fieldName = entry.getKey();
                IndexMappingConfig.FieldMapping mapping = entry.getValue();

                // 检查是否为文本字段且需要生成向量
                if ("text".equals(mapping.getElasticsearchType()) && shouldGenerateVector(fieldName, mapping)) {
                    Object fieldValue = document.get(fieldName);
                    if (fieldValue != null && !fieldValue.toString().trim().isEmpty()) {
                        String text = fieldValue.toString();

                        // 生成向量字段名
                        String vectorFieldName = fieldName + "_vector";

                        // 检查索引配置中是否确实有对应的向量字段定义
                        if (indexConfig.getFieldMappings().containsKey(vectorFieldName)) {
                            try {
                                // 生成嵌入向量
                                List<Float> embedding = embeddingService.getTextEmbedding(text);
                                if (embedding != null && !embedding.isEmpty()) {
                                    // 转换为double数组以符合Elasticsearch要求
                                    double[] embeddingArray = embedding.stream()
                                            .mapToDouble(Float::doubleValue)
                                            .toArray();
                                    document.put(vectorFieldName, embeddingArray);
                                    log.debug("为字段 {} 生成向量: {} 维", fieldName, embeddingArray.length);
                                } else {
                                    log.warn("字段 {} 向量生成失败：返回空向量", fieldName);
                                }
                            } catch (Exception e) {
                                log.error("为字段 {} 生成向量时发生错误: {}", fieldName, e.getMessage(), e);
                            }
                        } else {
                            log.debug("索引配置中不存在向量字段: {}", vectorFieldName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成向量字段时发生错误: {}", e.getMessage(), e);
            // 向量生成失败不应该影响数据导入，只记录错误
        }
    }

    /**
     * 判断是否应该为该字段生成向量
     */
    private boolean shouldGenerateVector(String fieldName, IndexMappingConfig.FieldMapping mapping) {
        // 只为文本类型字段生成向量，且排除系统字段
        return "text".equals(mapping.getElasticsearchType())
               && !fieldName.startsWith("_")
               && !fieldName.endsWith("_vector");
    }

    /**
     * 清理临时文件
     */
    private void cleanupTemporaryFiles(String taskId, Long searchSpaceId) {
        try {
            // 尝试清理新格式的临时文件
            String fileName = "sync-import-" + taskId + ".json";
            Path newFormatPath = fileStorageService.getTemporaryFilePath(fileName);
            if (Files.exists(newFormatPath)) {
                Files.delete(newFormatPath);
                log.info("临时文件清理完成: {}", newFormatPath);
            }
            
            // 同时尝试清理旧格式的临时文件
            Path oldFormatPath = fileStorageService.getTemporaryFilePath(taskId, searchSpaceId);
            if (Files.exists(oldFormatPath)) {
                Files.delete(oldFormatPath);
                log.info("旧格式临时文件清理完成: {}", oldFormatPath);
            }
        } catch (Exception e) {
            log.warn("清理临时文件失败: taskId={}, searchSpaceId={}", taskId, searchSpaceId, e);
        }
    }
}