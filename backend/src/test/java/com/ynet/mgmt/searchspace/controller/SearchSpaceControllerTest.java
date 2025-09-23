package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchspace.dto.*;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SearchSpaceController 测试类
 * 测试搜索空间控制器的所有API端点
 *
 * @author system
 * @since 1.0.0
 */
@WebMvcTest(value = SearchSpaceController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@DisplayName("SearchSpaceController 控制器测试")
class SearchSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchSpaceService searchSpaceService;

    @Autowired
    private ObjectMapper objectMapper;

    private SearchSpaceDTO mockSearchSpaceDTO;
    private CreateSearchSpaceRequest mockCreateRequest;
    private UpdateSearchSpaceRequest mockUpdateRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockSearchSpaceDTO = createMockSearchSpaceDTO();
        mockCreateRequest = createMockCreateRequest();
        mockUpdateRequest = createMockUpdateRequest();
    }

    @Test
    @DisplayName("创建搜索空间 - 成功")
    void testCreateSearchSpace_Success() throws Exception {
        // Given
        when(searchSpaceService.createSearchSpace(any(CreateSearchSpaceRequest.class)))
                .thenReturn(mockSearchSpaceDTO);

        // When & Then
        mockMvc.perform(post("/api/search-spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(mockSearchSpaceDTO.getId()))
                .andExpect(jsonPath("$.data.name").value(mockSearchSpaceDTO.getName()))
                .andExpect(jsonPath("$.data.code").value(mockSearchSpaceDTO.getCode()));

        verify(searchSpaceService, times(1)).createSearchSpace(any(CreateSearchSpaceRequest.class));
    }

    @Test
    @DisplayName("创建搜索空间 - 参数验证失败")
    void testCreateSearchSpace_ValidationFailure() throws Exception {
        // Given
        CreateSearchSpaceRequest invalidRequest = new CreateSearchSpaceRequest();
        invalidRequest.setName(""); // 空名称
        invalidRequest.setCode("123invalid"); // 无效代码格式

        // When & Then
        mockMvc.perform(post("/api/search-spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("参数验证失败"));

        verify(searchSpaceService, never()).createSearchSpace(any(CreateSearchSpaceRequest.class));
    }

    @Test
    @DisplayName("分页查询搜索空间列表 - 成功")
    void testListSearchSpaces_Success() throws Exception {
        // Given
        PageResult<SearchSpaceDTO> pageResult = PageResult.<SearchSpaceDTO>builder()
                .content(Arrays.asList(mockSearchSpaceDTO))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(searchSpaceService.listSearchSpaces(any(SearchSpaceQueryRequest.class)))
                .thenReturn(pageResult);

        // When & Then
        mockMvc.perform(get("/api/search-spaces")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(mockSearchSpaceDTO.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(searchSpaceService, times(1)).listSearchSpaces(any(SearchSpaceQueryRequest.class));
    }

    @Test
    @DisplayName("根据ID查询搜索空间 - 成功")
    void testGetSearchSpace_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        when(searchSpaceService.getSearchSpace(searchSpaceId))
                .thenReturn(mockSearchSpaceDTO);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(mockSearchSpaceDTO.getId()))
                .andExpect(jsonPath("$.data.name").value(mockSearchSpaceDTO.getName()));

        verify(searchSpaceService, times(1)).getSearchSpace(searchSpaceId);
    }

    @Test
    @DisplayName("根据ID查询搜索空间 - 不存在")
    void testGetSearchSpace_NotFound() throws Exception {
        // Given
        Long searchSpaceId = 999L;
        when(searchSpaceService.getSearchSpace(searchSpaceId))
                .thenThrow(new RuntimeException("搜索空间不存在"));

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}", searchSpaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在"));

        verify(searchSpaceService, times(1)).getSearchSpace(searchSpaceId);
    }

    @Test
    @DisplayName("根据代码查询搜索空间 - 成功")
    void testGetSearchSpaceByCode_Success() throws Exception {
        // Given
        String code = "test_space";
        when(searchSpaceService.getSearchSpaceByCode(code))
                .thenReturn(mockSearchSpaceDTO);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/code/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value(code));

        verify(searchSpaceService, times(1)).getSearchSpaceByCode(code);
    }

    @Test
    @DisplayName("更新搜索空间 - 成功")
    void testUpdateSearchSpace_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        when(searchSpaceService.updateSearchSpace(eq(searchSpaceId), any(UpdateSearchSpaceRequest.class)))
                .thenReturn(mockSearchSpaceDTO);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.id").value(mockSearchSpaceDTO.getId()));

        verify(searchSpaceService, times(1)).updateSearchSpace(eq(searchSpaceId), any(UpdateSearchSpaceRequest.class));
    }

    @Test
    @DisplayName("删除搜索空间 - 成功")
    void testDeleteSearchSpace_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        doNothing().when(searchSpaceService).deleteSearchSpace(searchSpaceId);

        // When & Then
        mockMvc.perform(delete("/api/search-spaces/{id}", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(searchSpaceService, times(1)).deleteSearchSpace(searchSpaceId);
    }

    @Test
    @DisplayName("获取统计信息 - 成功")
    void testGetStatistics_Success() throws Exception {
        // Given
        SearchSpaceStatistics stats = SearchSpaceStatistics.builder()
                .totalSpaces(10L)
                .activeSpaces(8L)
                .inactiveSpaces(2L)
                .maintenanceSpaces(0L)
                .deletedSpaces(0L)
                .build();

        when(searchSpaceService.getStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSpaces").value(10))
                .andExpect(jsonPath("$.data.activeSpaces").value(8));

        verify(searchSpaceService, times(1)).getStatistics();
    }

    @Test
    @DisplayName("启用搜索空间 - 成功")
    void testEnableSearchSpace_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        SearchSpaceDTO enabledDto = createMockSearchSpaceDTO();
        enabledDto.setStatus(SearchSpaceStatus.ACTIVE);

        when(searchSpaceService.enableSearchSpace(searchSpaceId))
                .thenReturn(enabledDto);

        // When & Then
        mockMvc.perform(post("/api/search-spaces/{id}/enable", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("搜索空间启用成功"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(searchSpaceService, times(1)).enableSearchSpace(searchSpaceId);
    }

    @Test
    @DisplayName("禁用搜索空间 - 成功")
    void testDisableSearchSpace_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        SearchSpaceDTO disabledDto = createMockSearchSpaceDTO();
        disabledDto.setStatus(SearchSpaceStatus.INACTIVE);

        when(searchSpaceService.disableSearchSpace(searchSpaceId))
                .thenReturn(disabledDto);

        // When & Then
        mockMvc.perform(post("/api/search-spaces/{id}/disable", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("搜索空间禁用成功"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(searchSpaceService, times(1)).disableSearchSpace(searchSpaceId);
    }

    @Test
    @DisplayName("检查代码可用性 - 可用")
    void testCheckCodeAvailability_Available() throws Exception {
        // Given
        String code = "new_code";
        when(searchSpaceService.isCodeAvailable(code)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/code/{code}/available", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("代码可用"))
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.available").value(true));

        verify(searchSpaceService, times(1)).isCodeAvailable(code);
    }

    @Test
    @DisplayName("检查代码可用性 - 不可用")
    void testCheckCodeAvailability_NotAvailable() throws Exception {
        // Given
        String code = "existing_code";
        when(searchSpaceService.isCodeAvailable(code)).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/code/{code}/available", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("代码已被使用"))
                .andExpect(jsonPath("$.data.available").value(false));

        verify(searchSpaceService, times(1)).isCodeAvailable(code);
    }

    // 辅助方法：创建模拟的SearchSpaceDTO
    private SearchSpaceDTO createMockSearchSpaceDTO() {
        SearchSpaceDTO dto = new SearchSpaceDTO();
        dto.setId(1L);
        dto.setName("测试搜索空间");
        dto.setCode("test_space");
        dto.setDescription("测试用的搜索空间");
        dto.setStatus(SearchSpaceStatus.ACTIVE);
        dto.setVersion(1L);
        return dto;
    }

    // 辅助方法：创建模拟的CreateSearchSpaceRequest
    private CreateSearchSpaceRequest createMockCreateRequest() {
        CreateSearchSpaceRequest request = new CreateSearchSpaceRequest();
        request.setName("测试搜索空间");
        request.setCode("test_space");
        request.setDescription("测试用的搜索空间");
        return request;
    }

    // 辅助方法：创建模拟的UpdateSearchSpaceRequest
    private UpdateSearchSpaceRequest createMockUpdateRequest() {
        UpdateSearchSpaceRequest request = new UpdateSearchSpaceRequest();
        request.setName("更新后的搜索空间");
        request.setDescription("更新后的描述");
        request.setStatus(SearchSpaceStatus.ACTIVE);
        return request;
    }
}