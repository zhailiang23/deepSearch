package com.ynet.mgmt.channel.service.impl;

import com.ynet.mgmt.channel.dto.ChannelDTO;
import com.ynet.mgmt.channel.dto.CreateChannelRequest;
import com.ynet.mgmt.channel.dto.UpdateChannelRequest;
import com.ynet.mgmt.channel.entity.Channel;
import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;
import com.ynet.mgmt.channel.mapper.ChannelMapper;
import com.ynet.mgmt.channel.repository.ChannelRepository;
import com.ynet.mgmt.channel.service.ChannelService;
import com.ynet.mgmt.channel.validator.ChannelValidator;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.common.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Channel service implementation
 * Provides core business logic for channel management
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
    private final ChannelValidator channelValidator;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                             ChannelMapper channelMapper,
                             ChannelValidator channelValidator) {
        this.channelRepository = channelRepository;
        this.channelMapper = channelMapper;
        this.channelValidator = channelValidator;
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public ChannelDTO createChannel(CreateChannelRequest request) {
        log.info("Creating channel: {}", request.getCode());

        // 1. Input validation
        channelValidator.validateCreateRequest(request);

        // 2. Business rule checks
        channelValidator.validateCodeUniqueness(request.getCode(), null);
        channelValidator.validateNameUniqueness(request.getName(), null);

        // 3. Create entity
        Channel channel = channelMapper.toEntity(request);

        // 4. Save to database
        channel = channelRepository.save(channel);

        log.info("Channel created successfully: {} (ID: {})", channel.getCode(), channel.getId());
        return channelMapper.toDTO(channel);
    }

    @Override
    public void deleteChannel(Long id) {
        log.info("Deleting channel: {}", id);

        // 1. Find channel
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // 2. Business rule checks
        channelValidator.validateDeleteOperation(channel);

        // 3. Mark as deleted instead of physical deletion
        channel.markAsDeleted();
        channelRepository.save(channel);

        log.info("Channel deleted successfully: {} ({})", channel.getCode(), id);
    }

    @Override
    public ChannelDTO updateChannel(Long id, UpdateChannelRequest request) {
        log.info("Updating channel: {}", id);

        // 1. Input validation
        channelValidator.validateUpdateRequest(request);

        // 2. Find existing channel
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // 3. Check business rules
        if (request.getName() != null && !Objects.equals(channel.getName(), request.getName())) {
            channelValidator.validateNameUniqueness(request.getName(), id);
        }

        // 4. Update fields
        channelMapper.updateEntity(request, channel);

        // 5. Save updates
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

        Page<Channel> page = channelRepository.findAllByOrderBySortOrderAscCreatedAtDesc(pageable);

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

    // ========== Status Management ==========

    @Override
    public ChannelDTO activateChannel(Long id) {
        log.info("Activating channel: {}", id);

        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // Validate operation
        channelValidator.validateActivateOperation(channel);

        // Execute activation
        channel.activate();
        channel = channelRepository.save(channel);

        log.info("Channel activated successfully: {}", channel.getCode());
        return channelMapper.toDTO(channel);
    }

    @Override
    public ChannelDTO deactivateChannel(Long id) {
        log.info("Deactivating channel: {}", id);

        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // Validate operation
        channelValidator.validateDeactivateOperation(channel);

        // Execute deactivation
        channel.setStatus(ChannelStatus.INACTIVE);
        channel.updateActivity();
        channel = channelRepository.save(channel);

        log.info("Channel deactivated successfully: {}", channel.getCode());
        return channelMapper.toDTO(channel);
    }

    @Override
    public ChannelDTO suspendChannel(Long id) {
        log.info("Suspending channel: {}", id);

        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // Validate operation
        channelValidator.validateSuspendOperation(channel);

        // Execute suspension
        channel.suspend();
        channel = channelRepository.save(channel);

        log.info("Channel suspended successfully: {}", channel.getCode());
        return channelMapper.toDTO(channel);
    }

    @Override
    public ChannelDTO restoreChannel(Long id) {
        log.info("Restoring channel: {}", id);

        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // Validate operation
        channelValidator.validateRestoreOperation(channel);

        // Execute restoration (restore to active status)
        channel.activate();
        channel = channelRepository.save(channel);

        log.info("Channel restored successfully: {}", channel.getCode());
        return channelMapper.toDTO(channel);
    }

    // ========== Query Methods ==========

    @Override
    @Transactional(readOnly = true)
    public List<ChannelDTO> getActiveChannels() {
        log.debug("Getting all active channels list");

        List<Channel> channels = channelRepository.findByStatusOrderBySortOrder(ChannelStatus.ACTIVE);
        return channelMapper.toDTOList(channels);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelDTO> getChannelsByType(ChannelType type) {
        log.debug("Getting channels by type: {}", type);

        List<Channel> channels = channelRepository.findByType(type);
        return channelMapper.toDTOList(channels);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelDTO> getChannelsByStatus(ChannelStatus status) {
        log.debug("Getting channels by status: {}", status);

        List<Channel> channels = channelRepository.findByStatus(status);
        return channelMapper.toDTOList(channels);
    }

    // ========== Validation Methods ==========

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