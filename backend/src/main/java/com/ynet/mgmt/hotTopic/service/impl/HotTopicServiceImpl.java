package com.ynet.mgmt.hotTopic.service.impl;

import com.ynet.mgmt.hotTopic.dto.*;
import com.ynet.mgmt.hotTopic.entity.HotTopic;
import com.ynet.mgmt.hotTopic.exception.DuplicateHotTopicException;
import com.ynet.mgmt.hotTopic.exception.HotTopicNotFoundException;
import com.ynet.mgmt.hotTopic.mapper.HotTopicMapper;
import com.ynet.mgmt.hotTopic.repository.HotTopicRepository;
import com.ynet.mgmt.hotTopic.service.HotTopicService;
import com.ynet.mgmt.common.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 热门话题服务实现类
 * 提供热门话题管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
@Service
@Transactional
public class HotTopicServiceImpl implements HotTopicService {

    private static final Logger log = LoggerFactory.getLogger(HotTopicServiceImpl.class);

    private final HotTopicRepository hotTopicRepository;
    private final HotTopicMapper hotTopicMapper;

    public HotTopicServiceImpl(HotTopicRepository hotTopicRepository,
                              HotTopicMapper hotTopicMapper) {
        this.hotTopicRepository = hotTopicRepository;
        this.hotTopicMapper = hotTopicMapper;
    }

    @Override
    public HotTopicDTO createHotTopic(CreateHotTopicRequest request) {
        log.info("Creating hot topic: {}", request.getName());

        // 验证名称的唯一性
        if (!isNameAvailable(request.getName())) {
            throw new DuplicateHotTopicException("名称", request.getName());
        }

        // 创建实体
        HotTopic hotTopic = hotTopicMapper.toEntity(request);

        // 保存到数据库
        hotTopic = hotTopicRepository.save(hotTopic);

        log.info("Hot topic created successfully: {} (ID: {})", hotTopic.getName(), hotTopic.getId());
        return hotTopicMapper.toDTO(hotTopic);
    }

    @Override
    public void deleteHotTopic(Long id) {
        log.info("Deleting hot topic: {}", id);

        HotTopic hotTopic = hotTopicRepository.findById(id)
            .orElseThrow(() -> new HotTopicNotFoundException(id));

        hotTopicRepository.delete(hotTopic);

        log.info("Hot topic deleted successfully: {} ({})", hotTopic.getName(), id);
    }

    @Override
    public HotTopicDTO updateHotTopic(Long id, UpdateHotTopicRequest request) {
        log.info("Updating hot topic: {}", id);

        HotTopic hotTopic = hotTopicRepository.findById(id)
            .orElseThrow(() -> new HotTopicNotFoundException(id));

        // 验证名称的唯一性（排除当前ID）
        if (!isNameAvailable(request.getName(), id)) {
            throw new DuplicateHotTopicException("名称", request.getName());
        }

        // 更新实体
        hotTopicMapper.updateEntity(request, hotTopic);

        // 保存到数据库
        hotTopic = hotTopicRepository.save(hotTopic);

        log.info("Hot topic updated successfully: {} (ID: {})", hotTopic.getName(), hotTopic.getId());
        return hotTopicMapper.toDTO(hotTopic);
    }

    @Override
    @Transactional(readOnly = true)
    public HotTopicDTO getHotTopic(Long id) {
        log.debug("Getting hot topic: {}", id);

        HotTopic hotTopic = hotTopicRepository.findById(id)
            .orElseThrow(() -> new HotTopicNotFoundException(id));

        return hotTopicMapper.toDTO(hotTopic);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<HotTopicDTO> listHotTopics(Pageable pageable) {
        log.debug("Listing hot topics: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<HotTopic> page = hotTopicRepository.findAll(pageable);
        List<HotTopicDTO> content = hotTopicMapper.toDTOList(page.getContent());

        return PageResult.<HotTopicDTO>builder()
            .content(content)
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
    public PageResult<HotTopicDTO> searchHotTopics(String keyword, Pageable pageable) {
        log.debug("Searching hot topics: keyword={}, page={}, size={}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<HotTopic> page = hotTopicRepository.findByKeyword(keyword, pageable);
        List<HotTopicDTO> content = hotTopicMapper.toDTOList(page.getContent());

        return PageResult.<HotTopicDTO>builder()
            .content(content)
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
    public List<HotTopicDTO> getAllHotTopics() {
        log.debug("Getting all hot topics");

        List<HotTopic> hotTopics = hotTopicRepository.findAll();
        return hotTopicMapper.toDTOList(hotTopics);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<HotTopicDTO> listHotTopicsByVisible(Boolean visible, Pageable pageable) {
        log.debug("Listing hot topics by visible: visible={}, page={}, size={}", visible, pageable.getPageNumber(), pageable.getPageSize());

        Page<HotTopic> page = hotTopicRepository.findByVisible(visible, pageable);
        List<HotTopicDTO> content = hotTopicMapper.toDTOList(page.getContent());

        return PageResult.<HotTopicDTO>builder()
            .content(content)
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
    public PageResult<HotTopicDTO> getVisibleTopicsOrderByPopularity(Pageable pageable) {
        log.debug("Getting visible topics ordered by popularity: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<HotTopic> page = hotTopicRepository.findVisibleTopicsOrderByPopularityDesc(pageable);
        List<HotTopicDTO> content = hotTopicMapper.toDTOList(page.getContent());

        return PageResult.<HotTopicDTO>builder()
            .content(content)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }

    @Override
    public HotTopicDTO toggleVisibility(Long id) {
        log.info("Toggling visibility for hot topic: {}", id);

        HotTopic hotTopic = hotTopicRepository.findById(id)
            .orElseThrow(() -> new HotTopicNotFoundException(id));

        hotTopic.setVisible(!hotTopic.getVisible());
        hotTopic = hotTopicRepository.save(hotTopic);

        log.info("Hot topic visibility toggled: {} -> {}", hotTopic.getName(), hotTopic.getVisible());
        return hotTopicMapper.toDTO(hotTopic);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<HotTopicDTO> getTopicsByMinPopularity(Integer minPopularity, Pageable pageable) {
        log.debug("Getting topics by min popularity: minPopularity={}, page={}, size={}", minPopularity, pageable.getPageNumber(), pageable.getPageSize());

        Page<HotTopic> page = hotTopicRepository.findByPopularityGreaterThanEqual(minPopularity, pageable);
        List<HotTopicDTO> content = hotTopicMapper.toDTOList(page.getContent());

        return PageResult.<HotTopicDTO>builder()
            .content(content)
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
    public List<HotTopicDTO> getTopPopularTopics(int limit) {
        log.debug("Getting top popular topics: limit={}", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<HotTopic> hotTopics = hotTopicRepository.findTopPopularTopics(pageable);
        return hotTopicMapper.toDTOList(hotTopics);
    }

    @Override
    public HotTopicDTO updatePopularity(Long id, Integer popularity) {
        log.info("Updating popularity for hot topic: {} -> {}", id, popularity);

        HotTopic hotTopic = hotTopicRepository.findById(id)
            .orElseThrow(() -> new HotTopicNotFoundException(id));

        hotTopic.setPopularity(popularity);
        hotTopic = hotTopicRepository.save(hotTopic);

        log.info("Hot topic popularity updated: {} -> {}", hotTopic.getName(), popularity);
        return hotTopicMapper.toDTO(hotTopic);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name) {
        return !hotTopicRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name, Long excludeId) {
        return !hotTopicRepository.existsByNameAndIdNot(name, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countVisibleTopics() {
        return hotTopicRepository.countVisibleTopics();
    }

    @Override
    @Transactional(readOnly = true)
    public Long countInvisibleTopics() {
        return hotTopicRepository.countInvisibleTopics();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAveragePopularity() {
        return hotTopicRepository.getAveragePopularity();
    }
}