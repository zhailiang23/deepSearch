package com.ynet.mgmt.sensitiveWord.service.impl;

import com.ynet.mgmt.sensitiveWord.dto.*;
import com.ynet.mgmt.sensitiveWord.entity.SensitiveWord;
import com.ynet.mgmt.sensitiveWord.exception.DuplicateSensitiveWordException;
import com.ynet.mgmt.sensitiveWord.exception.SensitiveWordNotFoundException;
import com.ynet.mgmt.sensitiveWord.mapper.SensitiveWordMapper;
import com.ynet.mgmt.sensitiveWord.repository.SensitiveWordRepository;
import com.ynet.mgmt.sensitiveWord.service.SensitiveWordService;
import com.ynet.mgmt.sensitiveWord.cache.SensitiveWordCache;
import com.ynet.mgmt.common.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词业务服务实现类
 * 实现敏感词管理的核心业务逻辑
 *
 * @author system
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveWordServiceImpl.class);

    private final SensitiveWordRepository repository;
    private final SensitiveWordMapper mapper;
    private final SensitiveWordCache sensitiveWordCache;

    public SensitiveWordServiceImpl(SensitiveWordRepository repository,
                                    SensitiveWordMapper mapper,
                                    SensitiveWordCache sensitiveWordCache) {
        this.repository = repository;
        this.mapper = mapper;
        this.sensitiveWordCache = sensitiveWordCache;
    }

    // ========== 基本CRUD操作 ==========

    @Override
    @Transactional
    public SensitiveWordDTO createSensitiveWord(CreateSensitiveWordRequest request) {
        logger.info("创建敏感词: name={}, harmLevel={}", request.getName(), request.getHarmLevel());

        // 检查名称是否已存在
        if (repository.existsByName(request.getName())) {
            logger.warn("敏感词名称已存在: name={}", request.getName());
            throw DuplicateSensitiveWordException.byName(request.getName());
        }

        // 转换并保存
        SensitiveWord entity = mapper.toEntity(request);
        SensitiveWord saved = repository.save(entity);

        logger.info("敏感词创建成功: id={}, name={}", saved.getId(), saved.getName());

        // 刷新缓存
        sensitiveWordCache.refresh();
        logger.debug("敏感词缓存已刷新");

        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteSensitiveWord(Long id) {
        logger.info("删除敏感词: id={}", id);

        if (!repository.existsById(id)) {
            logger.warn("敏感词不存在: id={}", id);
            throw new SensitiveWordNotFoundException(id);
        }

        repository.deleteById(id);
        logger.info("敏感词删除成功: id={}", id);

        // 刷新缓存
        sensitiveWordCache.refresh();
        logger.debug("敏感词缓存已刷新");
    }

    @Override
    @Transactional
    public SensitiveWordDTO updateSensitiveWord(Long id, UpdateSensitiveWordRequest request) {
        logger.info("更新敏感词: id={}, name={}", id, request.getName());

        // 查找敏感词
        SensitiveWord entity = repository.findById(id)
                .orElseThrow(() -> new SensitiveWordNotFoundException(id));

        // 检查名称是否被其他敏感词使用
        if (!entity.getName().equals(request.getName()) &&
            repository.existsByNameAndIdNot(request.getName(), id)) {
            logger.warn("敏感词名称已被其他敏感词使用: name={}", request.getName());
            throw DuplicateSensitiveWordException.byName(request.getName());
        }

        // 更新实体
        mapper.updateEntity(request, entity);
        SensitiveWord updated = repository.save(entity);

        logger.info("敏感词更新成功: id={}, name={}", updated.getId(), updated.getName());

        // 刷新缓存
        sensitiveWordCache.refresh();
        logger.debug("敏感词缓存已刷新");

        return mapper.toDTO(updated);
    }

    @Override
    public SensitiveWordDTO getSensitiveWord(Long id) {
        logger.debug("根据ID查询敏感词: id={}", id);

        SensitiveWord entity = repository.findById(id)
                .orElseThrow(() -> new SensitiveWordNotFoundException(id));

        return mapper.toDTO(entity);
    }

    @Override
    public PageResult<SensitiveWordDTO> listSensitiveWords(Pageable pageable) {
        logger.debug("分页查询敏感词列表: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<SensitiveWord> page = repository.findAll(pageable);
        List<SensitiveWordDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public PageResult<SensitiveWordDTO> searchSensitiveWords(String keyword, Pageable pageable) {
        logger.debug("搜索敏感词: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<SensitiveWord> page = repository.findByKeyword(keyword, pageable);
        List<SensitiveWordDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public List<SensitiveWordDTO> getAllSensitiveWords() {
        logger.debug("查询所有敏感词列表");

        List<SensitiveWord> entities = repository.findAll();
        return mapper.toDTOList(entities);
    }

    // ========== 启用状态相关操作 ==========

    @Override
    public PageResult<SensitiveWordDTO> listSensitiveWordsByEnabled(Boolean enabled, Pageable pageable) {
        logger.debug("根据启用状态查询敏感词: enabled={}, page={}, size={}",
                enabled, pageable.getPageNumber(), pageable.getPageSize());

        Page<SensitiveWord> page = repository.findByEnabled(enabled, pageable);
        List<SensitiveWordDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public SensitiveWordDTO toggleStatus(Long id) {
        logger.info("切换敏感词启用状态: id={}", id);

        SensitiveWord entity = repository.findById(id)
                .orElseThrow(() -> new SensitiveWordNotFoundException(id));

        entity.setEnabled(!entity.isEnabled());
        SensitiveWord updated = repository.save(entity);

        logger.info("敏感词状态切换成功: id={}, enabled={}", id, updated.getEnabled());

        // 刷新缓存（启用/禁用状态变化会影响检测）
        sensitiveWordCache.refresh();
        logger.debug("敏感词缓存已刷新");

        return mapper.toDTO(updated);
    }

    // ========== 危害等级相关操作 ==========

    @Override
    public PageResult<SensitiveWordDTO> listSensitiveWordsByHarmLevel(Integer harmLevel, Pageable pageable) {
        logger.debug("根据危害等级查询敏感词: harmLevel={}, page={}, size={}",
                harmLevel, pageable.getPageNumber(), pageable.getPageSize());

        Page<SensitiveWord> page = repository.findByHarmLevel(harmLevel, pageable);
        List<SensitiveWordDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public PageResult<SensitiveWordDTO> getSensitiveWordsByMinHarmLevel(Integer minHarmLevel, Pageable pageable) {
        logger.debug("查询危害等级大于等于指定值的敏感词: minHarmLevel={}, page={}, size={}",
                minHarmLevel, pageable.getPageNumber(), pageable.getPageSize());

        Page<SensitiveWord> page = repository.findByHarmLevelGreaterThanEqual(minHarmLevel, pageable);
        List<SensitiveWordDTO> content = mapper.toDTOList(page.getContent());

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public SensitiveWordDTO updateHarmLevel(Long id, Integer harmLevel) {
        logger.info("更新敏感词危害等级: id={}, harmLevel={}", id, harmLevel);

        SensitiveWord entity = repository.findById(id)
                .orElseThrow(() -> new SensitiveWordNotFoundException(id));

        entity.setHarmLevel(harmLevel);
        SensitiveWord updated = repository.save(entity);

        logger.info("敏感词危害等级更新成功: id={}, harmLevel={}", id, harmLevel);
        return mapper.toDTO(updated);
    }

    // ========== 验证方法 ==========

    @Override
    public boolean isNameAvailable(String name) {
        return !repository.existsByName(name);
    }

    @Override
    public boolean isNameAvailable(String name, Long excludeId) {
        return !repository.existsByNameAndIdNot(name, excludeId);
    }

    // ========== 统计方法 ==========

    @Override
    public Long countEnabledWords() {
        return repository.countEnabledWords();
    }

    @Override
    public Long countDisabledWords() {
        return repository.countDisabledWords();
    }

    @Override
    public Long countByHarmLevel(Integer harmLevel) {
        return repository.countByHarmLevel(harmLevel);
    }

    @Override
    public Map<Integer, Long> getHarmLevelDistribution() {
        List<Object[]> results = repository.getHarmLevelDistribution();
        Map<Integer, Long> distribution = new HashMap<>();

        for (Object[] result : results) {
            Integer level = (Integer) result[0];
            Long count = (Long) result[1];
            distribution.put(level, count);
        }

        return distribution;
    }
}