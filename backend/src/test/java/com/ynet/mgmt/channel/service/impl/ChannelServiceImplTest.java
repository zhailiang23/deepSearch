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
import com.ynet.mgmt.common.exception.BusinessException;
import com.ynet.mgmt.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChannelService业务逻辑测试类
 * 测试渠道管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("渠道服务实现测试")
class ChannelServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMapper channelMapper;

    @Mock
    private ChannelValidator channelValidator;

    private ChannelService channelService;

    // 测试数据
    private Channel testChannel;
    private ChannelDTO testChannelDTO;
    private CreateChannelRequest createRequest;
    private UpdateChannelRequest updateRequest;

    @BeforeEach
    void setUp() {
        channelService = new ChannelServiceImpl(channelRepository, channelMapper, channelValidator);
        setupTestData();
    }

    private void setupTestData() {
        // 创建测试渠道实体
        testChannel = new Channel();
        testChannel.setId(1L);
        testChannel.setName("测试渠道");
        testChannel.setCode("TEST_CHANNEL");
        testChannel.setDescription("测试用渠道");
        testChannel.setType(ChannelType.ONLINE);
        testChannel.setStatus(ChannelStatus.INACTIVE);
        testChannel.setContactName("张三");
        testChannel.setContactPhone("13800138000");
        testChannel.setContactEmail("test@example.com");
        testChannel.setCommissionRate(new BigDecimal("0.05"));
        testChannel.setMonthlyTarget(new BigDecimal("100000.00"));
        testChannel.setSortOrder(0);

        // 创建测试DTO
        testChannelDTO = new ChannelDTO();
        testChannelDTO.setId(1L);
        testChannelDTO.setName("测试渠道");
        testChannelDTO.setCode("TEST_CHANNEL");
        testChannelDTO.setDescription("测试用渠道");
        testChannelDTO.setType(ChannelType.ONLINE);
        testChannelDTO.setStatus(ChannelStatus.INACTIVE);
        testChannelDTO.setContactName("张三");
        testChannelDTO.setContactPhone("13800138000");
        testChannelDTO.setContactEmail("test@example.com");
        testChannelDTO.setCommissionRate(new BigDecimal("0.05"));
        testChannelDTO.setMonthlyTarget(new BigDecimal("100000.00"));
        testChannelDTO.setSortOrder(0);

        // 创建创建请求
        createRequest = new CreateChannelRequest();
        createRequest.setName("新渠道");
        createRequest.setCode("NEW_CHANNEL");
        createRequest.setDescription("新创建的渠道");
        createRequest.setType(ChannelType.OFFLINE);
        createRequest.setContactPerson("李四");
        createRequest.setContactPhone("13900139000");
        createRequest.setContactEmail("new@example.com");
        createRequest.setCommissionRate(new BigDecimal("0.03"));
        createRequest.setMonthlyTarget(new BigDecimal("50000.00"));

        // 创建更新请求
        updateRequest = new UpdateChannelRequest();
        updateRequest.setName("更新后渠道");
        updateRequest.setDescription("更新后的渠道描述");
        updateRequest.setCommissionRate(new BigDecimal("0.06"));
    }

    // ========== 创建渠道测试 ==========

    @Test
    @DisplayName("创建渠道 - 成功")
    void testCreateChannel_Success() {
        // Given
        Channel savedChannel = new Channel();
        savedChannel.setId(2L);
        savedChannel.setName(createRequest.getName());
        savedChannel.setCode(createRequest.getCode());

        ChannelDTO expectedDTO = new ChannelDTO();
        expectedDTO.setId(2L);
        expectedDTO.setName(createRequest.getName());
        expectedDTO.setCode(createRequest.getCode());

        when(channelMapper.toEntity(createRequest)).thenReturn(testChannel);
        when(channelRepository.save(testChannel)).thenReturn(savedChannel);
        when(channelMapper.toDTO(savedChannel)).thenReturn(expectedDTO);

        // When
        ChannelDTO result = channelService.createChannel(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(expectedDTO.getId(), result.getId());
        assertEquals(expectedDTO.getName(), result.getName());
        assertEquals(expectedDTO.getCode(), result.getCode());

        verify(channelValidator).validateCreateRequest(createRequest);
        verify(channelValidator).validateCodeUniqueness(createRequest.getCode(), null);
        verify(channelValidator).validateNameUniqueness(createRequest.getName(), null);
        verify(channelRepository).save(testChannel);
    }

    @Test
    @DisplayName("创建渠道 - 代码已存在")
    void testCreateChannel_CodeExists() {
        // Given
        doThrow(new BusinessException("CHANNEL_CODE_ALREADY_EXISTS", "渠道代码已存在"))
            .when(channelValidator).validateCodeUniqueness(createRequest.getCode(), null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> channelService.createChannel(createRequest));
        assertEquals("CHANNEL_CODE_ALREADY_EXISTS", exception.getErrorCode());
    }

    // ========== 查询渠道测试 ==========

    @Test
    @DisplayName("根据ID获取渠道 - 成功")
    void testGetChannel_Success() {
        // Given
        Long channelId = 1L;
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.getChannel(channelId);

        // Then
        assertNotNull(result);
        assertEquals(testChannelDTO.getId(), result.getId());
        assertEquals(testChannelDTO.getName(), result.getName());
        assertEquals(testChannelDTO.getCode(), result.getCode());
    }

    @Test
    @DisplayName("根据ID获取渠道 - 不存在")
    void testGetChannel_NotFound() {
        // Given
        Long channelId = 999L;
        when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
            () -> channelService.getChannel(channelId));
        assertTrue(exception.getMessage().contains("Channel not found"));
    }

    @Test
    @DisplayName("根据代码获取渠道 - 成功")
    void testGetChannelByCode_Success() {
        // Given
        String channelCode = "TEST_CHANNEL";
        when(channelRepository.findByCode(channelCode)).thenReturn(Optional.of(testChannel));
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.getChannelByCode(channelCode);

        // Then
        assertNotNull(result);
        assertEquals(testChannelDTO.getCode(), result.getCode());
    }

    // ========== 更新渠道测试 ==========

    @Test
    @DisplayName("更新渠道 - 成功")
    void testUpdateChannel_Success() {
        // Given
        Long channelId = 1L;
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(testChannel)).thenReturn(testChannel);
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.updateChannel(channelId, updateRequest);

        // Then
        assertNotNull(result);
        verify(channelValidator).validateUpdateRequest(updateRequest);
        verify(channelMapper).updateEntity(updateRequest, testChannel);
        verify(channelRepository).save(testChannel);
    }

    @Test
    @DisplayName("更新渠道 - 名称冲突")
    void testUpdateChannel_NameConflict() {
        // Given
        Long channelId = 1L;
        updateRequest.setName("冲突的名称");

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        doThrow(new BusinessException("CHANNEL_NAME_ALREADY_EXISTS", "渠道名称已存在"))
            .when(channelValidator).validateNameUniqueness("冲突的名称", channelId);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> channelService.updateChannel(channelId, updateRequest));
        assertEquals("CHANNEL_NAME_ALREADY_EXISTS", exception.getErrorCode());
    }

    // ========== 删除渠道测试 ==========

    @Test
    @DisplayName("删除渠道 - 成功")
    void testDeleteChannel_Success() {
        // Given
        Long channelId = 1L;
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(testChannel)).thenReturn(testChannel);

        // When
        channelService.deleteChannel(channelId);

        // Then
        verify(channelValidator).validateDeleteOperation(testChannel);
        verify(channelRepository).save(testChannel);
        assertEquals(ChannelStatus.DELETED, testChannel.getStatus());
    }

    // ========== 状态管理测试 ==========

    @Test
    @DisplayName("激活渠道 - 成功")
    void testActivateChannel_Success() {
        // Given
        Long channelId = 1L;
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(testChannel)).thenReturn(testChannel);
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.activateChannel(channelId);

        // Then
        assertNotNull(result);
        verify(channelValidator).validateActivateOperation(testChannel);
        verify(channelRepository).save(testChannel);
        assertEquals(ChannelStatus.ACTIVE, testChannel.getStatus());
    }

    @Test
    @DisplayName("停用渠道 - 成功")
    void testDeactivateChannel_Success() {
        // Given
        Long channelId = 1L;
        testChannel.setStatus(ChannelStatus.ACTIVE); // 先设置为激活状态

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(testChannel)).thenReturn(testChannel);
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.deactivateChannel(channelId);

        // Then
        assertNotNull(result);
        verify(channelValidator).validateDeactivateOperation(testChannel);
        verify(channelRepository).save(testChannel);
        assertEquals(ChannelStatus.INACTIVE, testChannel.getStatus());
    }

    @Test
    @DisplayName("暂停渠道 - 成功")
    void testSuspendChannel_Success() {
        // Given
        Long channelId = 1L;
        testChannel.setStatus(ChannelStatus.ACTIVE);

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(testChannel)).thenReturn(testChannel);
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.suspendChannel(channelId);

        // Then
        assertNotNull(result);
        verify(channelValidator).validateSuspendOperation(testChannel);
        verify(channelRepository).save(testChannel);
        assertEquals(ChannelStatus.SUSPENDED, testChannel.getStatus());
    }

    @Test
    @DisplayName("恢复渠道 - 成功")
    void testRestoreChannel_Success() {
        // Given
        Long channelId = 1L;
        testChannel.setStatus(ChannelStatus.SUSPENDED);

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(testChannel)).thenReturn(testChannel);
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        ChannelDTO result = channelService.restoreChannel(channelId);

        // Then
        assertNotNull(result);
        verify(channelValidator).validateRestoreOperation(testChannel);
        verify(channelRepository).save(testChannel);
        assertEquals(ChannelStatus.ACTIVE, testChannel.getStatus());
    }

    // ========== 查询方法测试 ==========

    @Test
    @DisplayName("获取激活渠道列表")
    void testGetActiveChannels() {
        // Given
        List<Channel> activeChannels = Arrays.asList(testChannel);
        List<ChannelDTO> expectedDTOs = Arrays.asList(testChannelDTO);

        when(channelRepository.findByStatusOrderBySortOrder(ChannelStatus.ACTIVE))
            .thenReturn(activeChannels);
        when(channelMapper.toDTOList(activeChannels)).thenReturn(expectedDTOs);

        // When
        List<ChannelDTO> result = channelService.getActiveChannels();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChannelDTO, result.get(0));
    }

    @Test
    @DisplayName("根据类型获取渠道列表")
    void testGetChannelsByType() {
        // Given
        List<Channel> channels = Arrays.asList(testChannel);
        List<ChannelDTO> expectedDTOs = Arrays.asList(testChannelDTO);

        when(channelRepository.findByType(ChannelType.ONLINE)).thenReturn(channels);
        when(channelMapper.toDTOList(channels)).thenReturn(expectedDTOs);

        // When
        List<ChannelDTO> result = channelService.getChannelsByType(ChannelType.ONLINE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChannelDTO, result.get(0));
    }

    @Test
    @DisplayName("根据状态获取渠道列表")
    void testGetChannelsByStatus() {
        // Given
        List<Channel> channels = Arrays.asList(testChannel);
        List<ChannelDTO> expectedDTOs = Arrays.asList(testChannelDTO);

        when(channelRepository.findByStatus(ChannelStatus.INACTIVE)).thenReturn(channels);
        when(channelMapper.toDTOList(channels)).thenReturn(expectedDTOs);

        // When
        List<ChannelDTO> result = channelService.getChannelsByStatus(ChannelStatus.INACTIVE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChannelDTO, result.get(0));
    }

    // ========== 分页查询测试 ==========

    @Test
    @DisplayName("分页查询渠道列表")
    void testListChannels() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Channel> channels = Arrays.asList(testChannel);
        Page<Channel> page = new PageImpl<>(channels, pageable, 1);
        List<ChannelDTO> dtos = Arrays.asList(testChannelDTO);

        when(channelRepository.findAllByOrderBySortOrderAscCreatedAtDesc(pageable)).thenReturn(page);
        when(channelMapper.toDTO(testChannel)).thenReturn(testChannelDTO);

        // When
        PageResult<ChannelDTO> result = channelService.listChannels(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    // ========== 验证方法测试 ==========

    @Test
    @DisplayName("检查代码可用性 - 可用")
    void testIsCodeAvailable_Available() {
        // Given
        String code = "NEW_CODE";
        when(channelRepository.existsByCode(code)).thenReturn(false);

        // When
        boolean result = channelService.isCodeAvailable(code);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查代码可用性 - 不可用")
    void testIsCodeAvailable_NotAvailable() {
        // Given
        String code = "EXISTING_CODE";
        when(channelRepository.existsByCode(code)).thenReturn(true);

        // When
        boolean result = channelService.isCodeAvailable(code);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("检查名称可用性（排除指定ID）- 可用")
    void testIsNameAvailable_WithExcludeId_Available() {
        // Given
        String name = "新名称";
        Long excludeId = 1L;
        when(channelRepository.existsByNameAndIdNot(name, excludeId)).thenReturn(false);

        // When
        boolean result = channelService.isNameAvailable(name, excludeId);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查名称可用性（排除指定ID）- 不可用")
    void testIsNameAvailable_WithExcludeId_NotAvailable() {
        // Given
        String name = "已存在名称";
        Long excludeId = 1L;
        when(channelRepository.existsByNameAndIdNot(name, excludeId)).thenReturn(true);

        // When
        boolean result = channelService.isNameAvailable(name, excludeId);

        // Then
        assertFalse(result);
    }
}