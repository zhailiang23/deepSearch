package com.ynet.mgmt.searchspace.service.impl;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.common.exception.BusinessException;
import com.ynet.mgmt.common.exception.EntityNotFoundException;
import com.ynet.mgmt.searchspace.constants.ErrorCode;
import com.ynet.mgmt.searchspace.dto.*;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import com.ynet.mgmt.searchspace.mapper.SearchSpaceMapper;
import com.ynet.mgmt.searchspace.model.IndexStatus;
import com.ynet.mgmt.searchspace.repository.SearchSpaceRepository;
import com.ynet.mgmt.searchspace.service.ElasticsearchManager;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import com.ynet.mgmt.searchspace.validator.SearchSpaceValidator;
import com.ynet.mgmt.searchspace.exception.SearchSpaceException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import jakarta.json.Json;

/**
 * 搜索空间业务服务实现
 * 提供搜索空间管理的核心业务逻辑
 *
 * @author system
 * @since 1.0.0
 */
@Service
@Transactional
public class SearchSpaceServiceImpl implements SearchSpaceService {

    private static final Logger log = LoggerFactory.getLogger(SearchSpaceServiceImpl.class);

    private final SearchSpaceRepository searchSpaceRepository;
    private final ElasticsearchManager elasticsearchManager;
    private final SearchSpaceMapper mapper;
    private final SearchSpaceValidator validator;
    private final ElasticsearchClient elasticsearchClient;

    public SearchSpaceServiceImpl(SearchSpaceRepository searchSpaceRepository,
                                 ElasticsearchManager elasticsearchManager,
                                 SearchSpaceMapper mapper,
                                 SearchSpaceValidator validator,
                                 ElasticsearchClient elasticsearchClient) {
        this.searchSpaceRepository = searchSpaceRepository;
        this.elasticsearchManager = elasticsearchManager;
        this.mapper = mapper;
        this.validator = validator;
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public SearchSpaceDTO createSearchSpace(CreateSearchSpaceRequest request) {
        log.info("创建搜索空间: {}", request.getCode());

        // 1. 输入验证
        validator.validateCreateRequest(request);

        // 2. 业务规则检查
        validator.validateCodeUniqueness(request.getCode(), null);

        // 3. 创建实体
        SearchSpace searchSpace = mapper.toEntity(request);

        // 4. 保存到数据库
        searchSpace = searchSpaceRepository.save(searchSpace);

        // 5. 步创建ES索引
        createElasticsearchIndex(searchSpace);

        log.info("搜索空间创建成功: {} (ID: {})", searchSpace.getCode(), searchSpace.getId());
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    public void deleteSearchSpace(Long id) {
        log.info("删除搜索空间: {}", id);

        // 1. 查询搜索空间
        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        // 2. 业务规则检查
        validator.validateDeleteOperation(searchSpace);

        // 3. 检查依赖关系（如果有文档在使用此搜索空间）
        if (hasDocumentsInSpace(searchSpace.getCode())) {
            throw new BusinessException(ErrorCode.SEARCH_SPACE_HAS_DOCUMENTS,
                "搜索空间中存在文档，无法删除: " + searchSpace.getCode());
        }

        // 4. 异步删除ES索引
        deleteElasticsearchIndex(searchSpace);

        // 5. 删除数据库记录
        searchSpaceRepository.delete(searchSpace);

        log.info("搜索空间删除成功: {} ({})", searchSpace.getCode(), id);
    }

    @Override
    public SearchSpaceDTO updateSearchSpace(Long id, UpdateSearchSpaceRequest request) {
        log.info("更新搜索空间: {}", id);

        // 1. 输入验证
        validator.validateUpdateRequest(request);

        // 2. 查询现有搜索空间
        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        // 3. 检查名称唯一性（如果有更新）
        if (request.getName() != null && !Objects.equals(searchSpace.getName(), request.getName())) {
            validator.validateNameUniqueness(request.getName(), id);
        }

        // 4. 更新字段
        if (request.getName() != null) {
            searchSpace.setName(request.getName());
        }
        if (request.getDescription() != null) {
            searchSpace.setDescription(request.getDescription());
        }

        // 5. 处理状态变更
        if (request.getStatus() != null && !Objects.equals(searchSpace.getStatus(), request.getStatus())) {
            searchSpace.setStatus(request.getStatus());
        }

        // 6. 保存更新
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("搜索空间更新成功: {} ({})", searchSpace.getCode(), id);
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    public SearchSpaceDTO enableSearchSpace(Long id) {
        log.info("启用搜索空间: {}", id);

        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        if (searchSpace.isActive()) {
            throw new BusinessException(ErrorCode.SEARCH_SPACE_ALREADY_ACTIVE,
                "搜索空间已启用: " + searchSpace.getCode());
        }

        // 更新状态
        searchSpace.activate();
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("搜索空间启用成功: {}", searchSpace.getCode());
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    public SearchSpaceDTO disableSearchSpace(Long id) {
        log.info("禁用搜索空间: {}", id);

        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        if (!searchSpace.isActive()) {
            throw new BusinessException(ErrorCode.SEARCH_SPACE_ALREADY_INACTIVE,
                "搜索空间已禁用: " + searchSpace.getCode());
        }

        // 验证是否可以禁用
        validator.validateDisableOperation(searchSpace);

        // 更新状态
        searchSpace.deactivate();
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("搜索空间禁用成功: {}", searchSpace.getCode());
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSpaceDTO getSearchSpace(Long id) {
        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        // 获取ES索引状态并转换为DTO
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSpaceDTO getSearchSpaceByCode(String code) {
        SearchSpace searchSpace = searchSpaceRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + code));

        // 获取ES索引状态并转换为DTO
        SearchSpaceDTO dto = mapper.toDTO(searchSpace, getElasticsearchIndexStatus(code));

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SearchSpaceDTO> listSearchSpaces(SearchSpaceQueryRequest request) {
        log.debug("查询搜索空间列表: {}", request);

        // 构建分页参数
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy())
        );

        // 执行查询
        Page<SearchSpace> page = searchSpaceRepository.findByKeyword(request.getKeyword(), pageable);

        // 转换结果
        List<SearchSpaceDTO> dtos = page.getContent().stream()
            .map(space -> mapper.toDTO(space, getElasticsearchIndexStatus(space.getCode())))
            .collect(Collectors.toList());

        return PageResult.<SearchSpaceDTO>builder()
            .content(dtos)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSpaceStatistics getStatistics() {
        long totalSpaces = searchSpaceRepository.count();
        long activeSpaces = searchSpaceRepository.countByStatus(SearchSpaceStatus.ACTIVE);
        long inactiveSpaces = searchSpaceRepository.countByStatus(SearchSpaceStatus.INACTIVE);
        long maintenanceSpaces = searchSpaceRepository.countByStatus(SearchSpaceStatus.MAINTENANCE);
        long deletedSpaces = searchSpaceRepository.countByStatus(SearchSpaceStatus.DELETED);

        return SearchSpaceStatistics.builder()
            .totalSpaces(totalSpaces)
            .activeSpaces(activeSpaces)
            .inactiveSpaces(inactiveSpaces)
            .maintenanceSpaces(maintenanceSpaces)
            .deletedSpaces(deletedSpaces)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code) {
        return !searchSpaceRepository.existsByCode(code);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 创建Elasticsearch索引
     */
    private void createElasticsearchIndex(SearchSpace searchSpace) {
        try {
            log.info("开始创建ES索引: {}", searchSpace.getCode());
            elasticsearchManager.createIndex(searchSpace.getIndexName());
            log.info("ES索引创建成功: {}", searchSpace.getCode());
        } catch (Exception e) {
            log.error("ES索引创建失败: {}", searchSpace.getCode(), e);
            // 在实际应用中，这里可以考虑重试机制或者通知机制
        }
    }

    /**
     * 删除Elasticsearch索引
     */
    private void deleteElasticsearchIndex(SearchSpace searchSpace) {
        try {
            log.info("开始删除ES索引: {}", searchSpace.getCode());
            elasticsearchManager.deleteIndex(searchSpace.getIndexName());
            log.info("ES索引删除成功: {}", searchSpace.getCode());
        } catch (Exception e) {
            log.error("ES索引删除失败: {}", searchSpace.getCode(), e);
            // 在实际应用中，这里可以考虑重试机制或者通知机制
        }
    }

    /**
     * 获取Elasticsearch索引状态
     */
    private IndexStatus getElasticsearchIndexStatus(String indexName) {
        try {
            return elasticsearchManager.getIndexStatus(indexName);
        } catch (Exception e) {
            log.warn("获取ES索引状态失败: {}", indexName, e);
            return IndexStatus.builder()
                .name(indexName)
                .exists(false)
                .health("error")
                .error(e.getMessage())
                .build();
        }
    }

    /**
     * 检查搜索空间中是否有文档
     * TODO: 在文档管理模块实现后完善此方法
     */
    private boolean hasDocumentsInSpace(String spaceCode) {
        // 暂时返回false，待文档模块实现后更新
        log.debug("检查搜索空间 {} 中是否有文档 - 暂时返回false", spaceCode);
        return false;
    }

    // ========== 新增的索引映射管理方法实现 ==========

    @Override
    @Transactional(readOnly = true)
    public String getIndexMapping(Long spaceId) {
        log.info("获取搜索空间的索引映射配置: spaceId={}", spaceId);

        try {
            // 1. 查询搜索空间是否存在
            SearchSpace searchSpace = searchSpaceRepository.findById(spaceId)
                .orElseThrow(() -> SearchSpaceException.searchSpaceNotFound(spaceId));

            // 2. 获取实际的索引名称
            String indexName = elasticsearchManager.findActualIndexName(searchSpace.getCode());
            log.debug("搜索空间 {} 对应的实际索引名: {}", searchSpace.getCode(), indexName);

            // 3. 检查索引是否存在
            if (!elasticsearchManager.indexExists(indexName)) {
                throw SearchSpaceException.indexNotFound(spaceId, indexName);
            }

            // 4. 调用 Elasticsearch 获取映射
            GetMappingRequest request = GetMappingRequest.of(builder -> builder.index(indexName));
            GetMappingResponse response = elasticsearchClient.indices().getMapping(request);

            // 5. 解析响应并转换为JSON字符串
            var indexMappings = response.result();
            if (indexMappings.isEmpty()) {
                log.warn("索引 {} 没有映射配置", indexName);
                return "{}";
            }

            // 获取第一个（也是唯一的）索引的映射
            var mappingEntry = indexMappings.entrySet().iterator().next();
            var indexMappingRecord = mappingEntry.getValue();

            // 获取TypeMapping对象，这包含了实际的映射信息
            var typeMapping = indexMappingRecord.mappings();
            log.debug("TypeMapping获取成功: indexName={}", indexName);

            // 尝试多种序列化方法
            String mappingJson;
            try {
                // 方法1：手动构建映射JSON
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> mappingMap = new HashMap<>();

                // 获取properties
                var properties = typeMapping.properties();
                if (properties != null && !properties.isEmpty()) {
                    Map<String, Object> propertiesMap = new HashMap<>();
                    properties.forEach((fieldName, property) -> {
                        try {
                            // 获取字段的基本信息
                            Map<String, Object> fieldMap = new HashMap<>();

                            // 尝试获取字段类型信息
                            if (property._kind() != null) {
                                fieldMap.put("_kind", property._kind().toString());
                            }

                            // 根据属性类型获取具体信息
                            switch (property._kind()) {
                                case Text:
                                    var textProperty = property.text();
                                    fieldMap.put("type", "text");
                                    if (textProperty.analyzer() != null) {
                                        fieldMap.put("analyzer", textProperty.analyzer());
                                    }
                                    break;
                                case Keyword:
                                    fieldMap.put("type", "keyword");
                                    break;
                                case Long:
                                    fieldMap.put("type", "long");
                                    break;
                                case Integer:
                                    fieldMap.put("type", "integer");
                                    break;
                                case Date:
                                    var dateProperty = property.date();
                                    fieldMap.put("type", "date");
                                    if (dateProperty.format() != null) {
                                        fieldMap.put("format", dateProperty.format());
                                    }
                                    break;
                                case Boolean:
                                    fieldMap.put("type", "boolean");
                                    break;
                                case Object:
                                    var objectProperty = property.object();
                                    fieldMap.put("type", "object");
                                    if (objectProperty.properties() != null && !objectProperty.properties().isEmpty()) {
                                        // 递归处理子属性
                                        Map<String, Object> subProperties = new HashMap<>();
                                        objectProperty.properties().forEach((subFieldName, subProperty) -> {
                                            Map<String, Object> subFieldMap = new HashMap<>();
                                            subFieldMap.put("type", subProperty._kind().toString().toLowerCase());
                                            subProperties.put(subFieldName, subFieldMap);
                                        });
                                        fieldMap.put("properties", subProperties);
                                    }
                                    break;
                                default:
                                    fieldMap.put("type", property._kind().toString().toLowerCase());
                            }

                            propertiesMap.put(fieldName, fieldMap);
                        } catch (Exception ex) {
                            log.warn("处理字段 {} 失败: {}", fieldName, ex.getMessage());
                            propertiesMap.put(fieldName, Map.of("type", "unknown", "error", ex.getMessage()));
                        }
                    });
                    mappingMap.put("properties", propertiesMap);
                }

                // 添加其他映射元数据
                if (typeMapping.meta() != null) {
                    mappingMap.put("_meta", typeMapping.meta());
                }
                if (typeMapping.source() != null && typeMapping.source().enabled() != null) {
                    Map<String, Object> sourceMap = new HashMap<>();
                    sourceMap.put("enabled", typeMapping.source().enabled());
                    mappingMap.put("_source", sourceMap);
                }

                mappingJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappingMap);
                log.debug("手动构建映射成功，包含 {} 个字段", properties != null ? properties.size() : 0);

            } catch (Exception e) {
                log.error("映射序列化失败: {}", e.getMessage(), e);
                // 备用方案：返回错误信息
                mappingJson = String.format("{ \"error\": \"映射序列化失败\", \"message\": \"%s\", \"properties\": {} }", e.getMessage().replace("\"", "\\\""));
            }

            log.info("获取索引映射配置成功: spaceId={}, indexName={}, mappingSize={}",
                spaceId, indexName, mappingJson.length());

            return mappingJson;

        } catch (SearchSpaceException e) {
            // 重新抛出业务异常
            throw e;
        } catch (JsonProcessingException e) {
            log.error("JSON处理失败: spaceId={}", spaceId, e);
            throw SearchSpaceException.mappingRetrievalFailed(spaceId, "unknown", e);
        } catch (Exception e) {
            log.error("获取索引映射配置失败: spaceId={}", spaceId, e);
            throw SearchSpaceException.elasticsearchConnectionFailed(spaceId, "GET_MAPPING", e);
        }
    }

    @Override
    @Transactional
    public void updateIndexMapping(Long spaceId, String mappingJson) {
        log.info("更新搜索空间的索引映射配置: spaceId={}, mappingSize={}",
                spaceId, mappingJson != null ? mappingJson.length() : 0);

        try {
            // 1. 查询搜索空间是否存在
            SearchSpace searchSpace = searchSpaceRepository.findById(spaceId)
                .orElseThrow(() -> SearchSpaceException.searchSpaceNotFound(spaceId));

            // 2. 验证映射JSON格式
            if (mappingJson == null || mappingJson.trim().isEmpty()) {
                throw SearchSpaceException.mappingValidationFailed(spaceId, "映射配置不能为空");
            }

            validateMappingJson(mappingJson, spaceId);

            // 3. 获取实际的索引名称
            String indexName = elasticsearchManager.findActualIndexName(searchSpace.getCode());
            log.debug("搜索空间 {} 对应的实际索引名: {}", searchSpace.getCode(), indexName);

            // 4. 检查索引是否存在
            if (!elasticsearchManager.indexExists(indexName)) {
                throw SearchSpaceException.indexNotFound(spaceId, indexName);
            }

            // 5. 解析映射JSON并构建更新请求
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode mappingNode = objectMapper.readTree(mappingJson);

            // 6. 调用 Elasticsearch 更新映射
            PutMappingRequest request = PutMappingRequest.of(builder -> {
                builder.index(indexName);

                // 从JSON构建TypeMapping
                try {
                    // 将JsonNode转换为字符串，然后使用StringReader和JsonParser
                    String mappingJsonStr = objectMapper.writeValueAsString(mappingNode);

                    // 使用JsonpMapper解析映射
                    JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
                    java.io.StringReader reader = new java.io.StringReader(mappingJsonStr);
                    jakarta.json.stream.JsonParser parser = jakarta.json.Json.createParser(reader);
                    TypeMapping typeMapping = jsonpMapper.deserialize(parser, TypeMapping.class);

                    builder.properties(typeMapping.properties());

                    parser.close();
                    reader.close();

                } catch (Exception e) {
                    log.error("解析映射配置失败: spaceId={}, mappingJson={}", spaceId, mappingJson, e);
                    throw new RuntimeException("映射配置格式错误", e);
                }

                return builder;
            });

            PutMappingResponse response = elasticsearchClient.indices().putMapping(request);

            if (!response.acknowledged()) {
                throw SearchSpaceException.mappingUpdateFailed(spaceId, indexName,
                    new RuntimeException("Elasticsearch未确认映射更新操作"));
            }

            // 7. 更新数据库中的映射信息（可选，用于缓存）
            searchSpace.setIndexMapping(mappingJson);
            searchSpaceRepository.save(searchSpace);

            log.info("更新索引映射配置成功: spaceId={}, indexName={}", spaceId, indexName);

        } catch (SearchSpaceException e) {
            // 重新抛出业务异常
            throw e;
        } catch (JsonProcessingException e) {
            log.error("JSON处理失败: spaceId={}, mappingJson={}", spaceId, mappingJson, e);
            throw SearchSpaceException.mappingValidationFailed(spaceId, "JSON格式错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("更新索引映射配置失败: spaceId={}, mappingJson={}", spaceId, mappingJson, e);
            throw SearchSpaceException.elasticsearchConnectionFailed(spaceId, "UPDATE_MAPPING", e);
        }
    }

    /**
     * 验证映射JSON的有效性
     */
    private void validateMappingJson(String mappingJson, Long spaceId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(mappingJson);

            // 基本的映射结构验证
            if (rootNode.has("properties")) {
                JsonNode properties = rootNode.get("properties");
                if (!properties.isObject()) {
                    throw SearchSpaceException.mappingValidationFailed(spaceId, "properties字段必须是对象类型");
                }

                // 可以添加更多具体的字段验证逻辑
                log.debug("映射JSON验证通过: spaceId={}, propertiesCount={}",
                    spaceId, properties.size());
            } else {
                log.warn("映射JSON缺少properties字段: spaceId={}", spaceId);
            }

        } catch (JsonProcessingException e) {
            throw SearchSpaceException.mappingValidationFailed(spaceId, "JSON格式错误: " + e.getMessage());
        }
    }

    // ========== 新增的JSON导入相关业务方法实现 ==========

    /**
     * 更新搜索空间的索引映射配置（保留原有方法以维持兼容性）
     *
     * @deprecated 使用 {@link #updateIndexMapping(Long, String)} 替代
     */
    @Deprecated
    public SearchSpaceDTO updateIndexMappingCompat(Long id, String indexMapping) {
        log.info("更新搜索空间索引映射(兼容方法): {}", id);

        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        // 使用实体的业务方法设置映射配置
        searchSpace.setIndexMapping(indexMapping);
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("搜索空间索引映射更新成功: {} (ID: {})", searchSpace.getCode(), id);
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    @Transactional
    public SearchSpaceDTO updateImportStats(Long id, long additionalCount) {
        log.info("更新搜索空间导入统计: {} (+{})", id, additionalCount);
        
        if (additionalCount < 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "导入数量不能为负数");
        }
        
        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));
        
        // 使用实体的业务方法更新统计
        searchSpace.updateImportStats(additionalCount);
        searchSpace = searchSpaceRepository.save(searchSpace);
        
        log.info("搜索空间导入统计更新成功: {} (ID: {}) - 文档总数: {}", 
                searchSpace.getCode(), id, searchSpace.getDocumentCount());
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSpaceDTO> listSearchSpacesWithMapping() {
        log.debug("查询有索引映射配置的搜索空间列表");
        
        List<SearchSpace> searchSpaces = searchSpaceRepository.findAllWithIndexMapping();
        return searchSpaces.stream()
            .map(space -> mapper.toDTO(space, getElasticsearchIndexStatus(space.getCode())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchSpaceDTO> listSearchSpacesWithDocuments() {
        log.debug("查询有导入文档的搜索空间列表");
        
        List<SearchSpace> searchSpaces = searchSpaceRepository.findAllWithImportedDocuments();
        return searchSpaces.stream()
            .map(space -> mapper.toDTO(space, getElasticsearchIndexStatus(space.getCode())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ImportStatistics getImportStatistics() {
        log.debug("获取导入统计信息");
        
        Long totalSpacesWithMapping = searchSpaceRepository.countSpacesWithIndexMapping();
        Long totalSpacesWithDocuments = searchSpaceRepository.countSpacesWithImportedDocuments();
        Long totalImportedDocuments = searchSpaceRepository.countTotalImportedDocuments();
        LocalDateTime lastImportTime = searchSpaceRepository.findLastImportTime().orElse(null);
        
        return new ImportStatistics(totalSpacesWithMapping, totalSpacesWithDocuments,
                totalImportedDocuments, lastImportTime);
    }

    @Override
    @Transactional
    public SearchSpaceDTO resetImportStats(Long id) {
        log.info("重置搜索空间导入统计: {}", id);
        
        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));
        
        searchSpace.setDocumentCount(0L);
        searchSpace.setLastImportTime(null);
        searchSpace = searchSpaceRepository.save(searchSpace);
        
        log.info("搜索空间导入统计重置成功: {} (ID: {})", searchSpace.getCode(), id);
        return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
    }

    @Override
    @Transactional
    public List<SearchSpaceDTO> batchUpdateImportStats(List<ImportStatsUpdate> importUpdates) {
        log.info("批量更新搜索空间导入统计: {} 个更新", importUpdates.size());
        
        if (importUpdates == null || importUpdates.isEmpty()) {
            return List.of();
        }
        
        List<SearchSpaceDTO> results = importUpdates.stream()
            .map(update -> {
                if (update.getAdditionalCount() < 0) {
                    throw new BusinessException(ErrorCode.INVALID_PARAMETER, 
                        "搜索空间 " + update.getSearchSpaceId() + " 的导入数量不能为负数");
                }
                
                SearchSpace searchSpace = searchSpaceRepository.findById(update.getSearchSpaceId())
                    .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + update.getSearchSpaceId()));
                
                searchSpace.updateImportStats(update.getAdditionalCount());
                searchSpace = searchSpaceRepository.save(searchSpace);
                
                return mapper.toDTO(searchSpace, getElasticsearchIndexStatus(searchSpace.getCode()));
            })
            .collect(Collectors.toList());
        
        log.info("批量更新搜索空间导入统计完成: {} 个成功", results.size());
        return results;
    }
}