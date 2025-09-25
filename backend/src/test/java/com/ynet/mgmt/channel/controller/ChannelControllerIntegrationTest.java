package com.ynet.mgmt.channel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.channel.dto.CreateChannelRequest;
import com.ynet.mgmt.channel.entity.ChannelType;
import com.ynet.mgmt.channel.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChannelController集成测试
 * 测试基础的REST API端点功能
 *
 * @author system
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class ChannelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChannelRepository channelRepository;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        channelRepository.deleteAll();
    }

    @Test
    void testCreateChannel() throws Exception {
        CreateChannelRequest request = new CreateChannelRequest();
        request.setName("测试渠道");
        request.setCode("TEST_CHANNEL_001");
        request.setDescription("这是一个测试渠道");
        request.setType(ChannelType.ONLINE);
        request.setContactPerson("张三");
        request.setContactPhone("13800138000");
        request.setContactEmail("zhangsan@test.com");
        request.setCommissionRate(new BigDecimal("0.05"));
        request.setMonthlyTarget(new BigDecimal("100000"));

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("测试渠道"))
                .andExpect(jsonPath("$.data.code").value("TEST_CHANNEL_001"));
    }

    @Test
    void testCreateChannelWithInvalidData() throws Exception {
        CreateChannelRequest request = new CreateChannelRequest();
        // 故意不设置必需的字段

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testListChannels() throws Exception {
        // 先创建一个测试渠道
        CreateChannelRequest request = new CreateChannelRequest();
        request.setName("测试渠道");
        request.setCode("TEST_CHANNEL_LIST_001");
        request.setDescription("测试渠道描述");
        request.setType(ChannelType.ONLINE);
        request.setContactPerson("联系人");
        request.setContactPhone("13801380001");
        request.setContactEmail("contact@test.com");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 获取渠道列表
        mockMvc.perform(get("/api/channels")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetActiveChannels() throws Exception {
        // 创建一个激活状态的渠道
        CreateChannelRequest request = new CreateChannelRequest();
        request.setName("激活渠道");
        request.setCode("ACTIVE_CHANNEL_001");
        request.setDescription("这是一个激活的渠道");
        request.setType(ChannelType.ONLINE);
        request.setContactPerson("激活联系人");
        request.setContactPhone("13800138005");
        request.setContactEmail("active@test.com");

        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 获取活跃渠道列表
        mockMvc.perform(get("/api/channels/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testCheckCodeAvailability() throws Exception {
        // 检查不存在的代码
        mockMvc.perform(get("/api/channels/check-code")
                        .param("code", "NON_EXISTING_CODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testGetChannelStatistics() throws Exception {
        // 获取统计信息（即使没有数据也应该能正常返回）
        mockMvc.perform(get("/api/channels/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}