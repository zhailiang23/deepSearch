package com.ynet.mgmt.searchspace.service.impl;

import java.time.LocalDateTime;
import java.util.List;
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

    public SearchSpaceServiceImpl(SearchSpaceRepository searchSpaceRepository,
                                 ElasticsearchManager elasticsearchManager,
                                 SearchSpaceMapper mapper,
                                 SearchSpaceValidator validator) {
        this.searchSpaceRepository = searchSpaceRepository;
        this.elasticsearchManager = elasticsearchManager;
        this.mapper = mapper;
        this.validator = validator;
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
                .health("unknown")
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

    // ========== 新增的JSON导入相关业务方法实现 ==========

    @Override
    @Transactional
    public SearchSpaceDTO updateIndexMapping(Long id, String indexMapping) {
        log.info("更新搜索空间索引映射: {}", id);
        
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