package com.ynet.mgmt.searchspace.service.impl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

        // 5. 如果启用向量检索，异步创建ES索引
        if (searchSpace.isVectorEnabled()) {
            createElasticsearchIndexAsync(searchSpace);
        }

        log.info("搜索空间创建成功: {} (ID: {})", searchSpace.getCode(), searchSpace.getId());
        return mapper.toDTO(searchSpace);
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

        // 4. 如果启用了向量检索，异步删除ES索引
        if (searchSpace.isVectorEnabled()) {
            deleteElasticsearchIndexAsync(searchSpace);
        }

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

        // 5. 处理向量检索状态变更
        if (request.getVectorEnabled() != null &&
            !Objects.equals(searchSpace.getVectorEnabled(), request.getVectorEnabled())) {

            if (request.getVectorEnabled()) {
                // 启用向量检索
                createElasticsearchIndexAsync(searchSpace);
                searchSpace.enableVector();
            } else {
                // 禁用向量检索
                validator.validateVectorDisable(searchSpace);
                deleteElasticsearchIndexAsync(searchSpace);
                searchSpace.disableVector();
            }
        }

        // 6. 保存更新
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("搜索空间更新成功: {} ({})", searchSpace.getCode(), id);
        return mapper.toDTO(searchSpace);
    }

    @Override
    public SearchSpaceDTO enableVectorSearch(Long id) {
        log.info("启用向量检索: {}", id);

        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        if (searchSpace.isVectorEnabled()) {
            throw new BusinessException(ErrorCode.VECTOR_ALREADY_ENABLED,
                "向量检索已启用: " + searchSpace.getCode());
        }

        // 异步创建ES索引
        createElasticsearchIndexAsync(searchSpace);

        // 更新状态
        searchSpace.enableVector();
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("向量检索启用成功: {}", searchSpace.getCode());
        return mapper.toDTO(searchSpace);
    }

    @Override
    public SearchSpaceDTO disableVectorSearch(Long id) {
        log.info("禁用向量检索: {}", id);

        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        if (!searchSpace.isVectorEnabled()) {
            throw new BusinessException(ErrorCode.VECTOR_ALREADY_DISABLED,
                "向量检索已禁用: " + searchSpace.getCode());
        }

        // 验证是否可以禁用
        validator.validateVectorDisable(searchSpace);

        // 异步删除ES索引
        deleteElasticsearchIndexAsync(searchSpace);

        // 更新状态
        searchSpace.disableVector();
        searchSpace = searchSpaceRepository.save(searchSpace);

        log.info("向量检索禁用成功: {}", searchSpace.getCode());
        return mapper.toDTO(searchSpace);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSpaceDTO getSearchSpace(Long id) {
        SearchSpace searchSpace = searchSpaceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + id));

        SearchSpaceDTO dto = mapper.toDTO(searchSpace);

        // 获取ES索引状态
        if (searchSpace.isVectorEnabled()) {
            dto.setIndexStatus(getElasticsearchIndexStatus(searchSpace.getCode()));
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchSpaceDTO getSearchSpaceByCode(String code) {
        SearchSpace searchSpace = searchSpaceRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("搜索空间不存在: " + code));

        SearchSpaceDTO dto = mapper.toDTO(searchSpace);

        // 获取ES索引状态
        if (searchSpace.isVectorEnabled()) {
            dto.setIndexStatus(getElasticsearchIndexStatus(code));
        }

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
            .map(space -> {
                SearchSpaceDTO dto = mapper.toDTO(space);
                // 获取ES索引状态
                if (space.isVectorEnabled()) {
                    dto.setIndexStatus(getElasticsearchIndexStatus(space.getCode()));
                }
                return dto;
            })
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
        long vectorEnabledSpaces = searchSpaceRepository.countByVectorEnabledTrue();
        long activeSpaces = searchSpaceRepository.countByStatus(SearchSpaceStatus.ACTIVE);

        return SearchSpaceStatistics.builder()
            .totalSpaces(totalSpaces)
            .vectorEnabledSpaces(vectorEnabledSpaces)
            .vectorDisabledSpaces(totalSpaces - vectorEnabledSpaces)
            .activeSpaces(activeSpaces)
            .inactiveSpaces(totalSpaces - activeSpaces)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code) {
        return !searchSpaceRepository.existsByCode(code);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 异步创建Elasticsearch索引
     */
    private void createElasticsearchIndexAsync(SearchSpace searchSpace) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始创建ES索引: {}", searchSpace.getCode());
                elasticsearchManager.createIndex(searchSpace.getIndexName());
                log.info("ES索引创建成功: {}", searchSpace.getCode());
            } catch (Exception e) {
                log.error("ES索引创建失败: {}", searchSpace.getCode(), e);
                // 在实际应用中，这里可以考虑重试机制或者通知机制
            }
        });
    }

    /**
     * 异步删除Elasticsearch索引
     */
    private void deleteElasticsearchIndexAsync(SearchSpace searchSpace) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始删除ES索引: {}", searchSpace.getCode());
                elasticsearchManager.deleteIndex(searchSpace.getIndexName());
                log.info("ES索引删除成功: {}", searchSpace.getCode());
            } catch (Exception e) {
                log.error("ES索引删除失败: {}", searchSpace.getCode(), e);
                // 在实际应用中，这里可以考虑重试机制或者通知机制
            }
        });
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
}