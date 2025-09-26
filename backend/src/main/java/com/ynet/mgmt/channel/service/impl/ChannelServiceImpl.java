package com.ynet.mgmt.channel.service.impl;

import com.ynet.mgmt.channel.dto.*;
import com.ynet.mgmt.channel.entity.Channel;
import com.ynet.mgmt.channel.mapper.ChannelMapper;
import com.ynet.mgmt.channel.repository.ChannelRepository;
import com.ynet.mgmt.channel.service.ChannelService;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.common.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 渠道服务实现类
 * 提供渠道管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
@Service
@Transactional
public class ChannelServiceImpl implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ChannelRepository channelRepository;
    private final ChannelMapper channelMapper;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                             ChannelMapper channelMapper) {
        this.channelRepository = channelRepository;
        this.channelMapper = channelMapper;
    }

    @Override
    public ChannelDTO createChannel(CreateChannelRequest request) {
        log.info("Creating channel: {}", request.getCode());

        // 验证代码和名称的唯一性
        if (!isCodeAvailable(request.getCode())) {
            throw new IllegalArgumentException("渠道代码已存在: " + request.getCode());
        }

        if (!isNameAvailable(request.getName())) {
            throw new IllegalArgumentException("渠道名称已存在: " + request.getName());
        }

        // 创建实体
        Channel channel = channelMapper.toEntity(request);

        // 保存到数据库
        channel = channelRepository.save(channel);

        log.info("Channel created successfully: {} (ID: {})", channel.getCode(), channel.getId());
        return channelMapper.toDTO(channel);
    }

    @Override
    public void deleteChannel(Long id) {
        log.info("Deleting channel: {}", id);

        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        channelRepository.delete(channel);

        log.info("Channel deleted successfully: {} ({})", channel.getCode(), id);
    }

    @Override
    public ChannelDTO updateChannel(Long id, UpdateChannelRequest request) {
        log.info("Updating channel: {}", id);

        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // 验证名称唯一性（如果修改了名称）
        if (request.getName() != null && !request.getName().equals(channel.getName())) {
            if (!isNameAvailable(request.getName(), id)) {
                throw new IllegalArgumentException("渠道名称已存在: " + request.getName());
            }
        }

        // 更新字段
        channelMapper.updateEntity(request, channel);

        // 保存更新
        channel = channelRepository.save(channel);

        log.info("Channel updated successfully: {} ({})", channel.getCode(), id);
        return channelMapper.toDTO(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelDTO getChannel(Long id) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));
        return channelMapper.toDTO(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelDTO getChannelByCode(String code) {
        Channel channel = channelRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + code));
        return channelMapper.toDTO(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ChannelDTO> listChannels(Pageable pageable) {
        log.debug("Querying channel list with pagination");

        Page<Channel> page = channelRepository.findAll(pageable);

        List<ChannelDTO> dtos = page.getContent().stream()
            .map(channelMapper::toDTO)
            .collect(Collectors.toList());

        return PageResult.<ChannelDTO>builder()
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
    public PageResult<ChannelDTO> searchChannels(String keyword, Pageable pageable) {
        log.debug("Searching channels with keyword: {}", keyword);

        Page<Channel> page = channelRepository.findByKeyword(keyword, pageable);

        List<ChannelDTO> dtos = page.getContent().stream()
            .map(channelMapper::toDTO)
            .collect(Collectors.toList());

        return PageResult.<ChannelDTO>builder()
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
    public List<ChannelDTO> getAllChannels() {
        log.debug("Querying all channels");

        List<Channel> channels = channelRepository.findAll();
        return channels.stream()
            .map(channelMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code) {
        return !channelRepository.existsByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name) {
        return !channelRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code, Long excludeId) {
        return !channelRepository.existsByCodeAndIdNot(code, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name, Long excludeId) {
        return !channelRepository.existsByNameAndIdNot(name, excludeId);
    }
}