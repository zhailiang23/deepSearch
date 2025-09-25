package com.ynet.mgmt.channel.service.impl;

import com.ynet.mgmt.channel.dto.*;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // ========== Extended Query Methods ==========

    @Override
    @Transactional(readOnly = true)
    public PageResult<ChannelDTO> listChannels(ChannelQueryRequest request) {
        log.debug("查询渠道列表(带条件): keyword={}, type={}, status={}, page={}, size={}",
                request.getKeyword(), request.getType(), request.getStatus(),
                request.getPage(), request.getSize());

        // 构建排序
        Sort sort = buildSort(request.getSort());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 根据条件查询
        Page<Channel> page;
        if (hasQueryConditions(request)) {
            // TODO: 这里需要使用repository的条件查询方法
            // 临时使用简单的分页查询
            page = channelRepository.findAllByOrderBySortOrderAscCreatedAtDesc(pageable);
        } else {
            page = channelRepository.findAllByOrderBySortOrderAscCreatedAtDesc(pageable);
        }

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

    // ========== Sales Management ==========

    @Override
    public ChannelDTO updateSales(Long id, UpdateSalesRequest request) {
        log.info("更新渠道销售数据: id={}, amount={}", id, request.getSalesAmount());

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // 更新销售数据
        if (request.getAddToTotal() != null && request.getAddToTotal()) {
            BigDecimal currentTotal = channel.getTotalSales() != null ? channel.getTotalSales() : BigDecimal.ZERO;
            channel.setTotalSales(currentTotal.add(request.getSalesAmount()));
        }

        if (request.getAddToMonthly() != null && request.getAddToMonthly()) {
            BigDecimal currentMonthly = channel.getCurrentMonthSales() != null ? channel.getCurrentMonthSales() : BigDecimal.ZERO;
            channel.setCurrentMonthSales(currentMonthly.add(request.getSalesAmount()));
        }

        // 更新活动时间
        channel.updateActivity();

        // 重新计算绩效比例（这里可以添加绩效计算逻辑）
        // TODO: 实现绩效比例计算逻辑

        channel = channelRepository.save(channel);

        log.info("渠道销售数据更新成功: id={}, totalSales={}, currentMonthSales={}",
                channel.getId(), channel.getTotalSales(), channel.getCurrentMonthSales());
        return channelMapper.toDTO(channel);
    }

    @Override
    public ChannelDTO resetMonthlySales(Long id) {
        log.info("重置月度销售: id={}", id);

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));

        // 重置月度销售
        channel.setCurrentMonthSales(BigDecimal.ZERO);
        // TODO: 重新计算绩效比例
        channel.updateActivity();

        channel = channelRepository.save(channel);

        log.info("月度销售重置成功: id={}", id);
        return channelMapper.toDTO(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelDTO> getTopPerformingChannels(int limit) {
        log.debug("获取销售排行: limit={}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("totalSales").descending());
        Page<Channel> page = channelRepository.findByStatusOrderByTotalSalesDesc(ChannelStatus.ACTIVE, pageable);

        return channelMapper.toDTOList(page.getContent());
    }

    // ========== Statistics ==========

    @Override
    @Transactional(readOnly = true)
    public ChannelStatistics getChannelStatistics() {
        log.debug("获取渠道统计信息");

        ChannelStatistics stats = new ChannelStatistics();

        // 统计各种状态的渠道数量
        stats.setTotalChannels((int) channelRepository.count());
        stats.setActiveChannels((int) channelRepository.countByStatus(ChannelStatus.ACTIVE));
        stats.setSuspendedChannels((int) channelRepository.countByStatus(ChannelStatus.SUSPENDED));
        stats.setInactiveChannels((int) channelRepository.countByStatus(ChannelStatus.INACTIVE));

        // 统计各种类型的渠道数量
        stats.setOnlineChannels((int) channelRepository.countByType(ChannelType.ONLINE));
        stats.setOfflineChannels((int) channelRepository.countByType(ChannelType.OFFLINE));
        stats.setDistributorChannels((int) channelRepository.countByType(ChannelType.DISTRIBUTOR));

        // 统计销售数据
        stats.setTotalSales(channelRepository.sumTotalSales());
        stats.setMonthlyTotalSales(channelRepository.sumCurrentMonthSales());

        // 统计达成目标的渠道数量
        stats.setTargetAchievedChannels((int) channelRepository.countByTargetAchievedTrue());

        // 计算平均佣金率
        stats.setAverageCommissionRate(channelRepository.avgCommissionRate());

        // 获取最高单月销售额
        stats.setHighestMonthlySales(channelRepository.maxCurrentMonthSales());

        log.debug("统计信息获取成功: totalChannels={}, activeChannels={}",
                stats.getTotalChannels(), stats.getActiveChannels());

        return stats;
    }

    // ========== Batch Operations ==========

    @Override
    public List<ChannelDTO> batchUpdateStatus(BatchStatusUpdateRequest request) {
        log.info("批量状态变更: channelIds={}, targetStatus={}",
                request.getChannelIds(), request.getTargetStatus());

        List<Channel> channels = channelRepository.findAllById(request.getChannelIds());

        if (channels.size() != request.getChannelIds().size()) {
            throw new EntityNotFoundException("部分渠道不存在");
        }

        // 批量更新状态
        channels.forEach(channel -> {
            channel.setStatus(request.getTargetStatus());
            channel.updateActivity();
        });

        List<Channel> updatedChannels = channelRepository.saveAll(channels);

        log.info("批量状态变更成功: count={}", updatedChannels.size());
        return channelMapper.toDTOList(updatedChannels);
    }

    @Override
    public void batchDeleteChannels(List<Long> channelIds) {
        log.info("批量删除渠道: channelIds={}", channelIds);

        List<Channel> channels = channelRepository.findAllById(channelIds);

        if (channels.size() != channelIds.size()) {
            throw new EntityNotFoundException("部分渠道不存在");
        }

        // 批量软删除
        channels.forEach(Channel::markAsDeleted);
        channelRepository.saveAll(channels);

        log.info("批量删除成功: count={}", channels.size());
    }

    // ========== Helper Methods ==========

    private Sort buildSort(String sortStr) {
        if (sortStr == null || sortStr.trim().isEmpty()) {
            return Sort.by("createdAt").descending();
        }

        String[] parts = sortStr.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }

    private boolean hasQueryConditions(ChannelQueryRequest request) {
        return request.getKeyword() != null
                || request.getType() != null
                || request.getStatus() != null
                || request.getContactName() != null
                || request.getContactPhone() != null
                || request.getContactEmail() != null
                || (request.getActiveOnly() != null && request.getActiveOnly());
    }
}