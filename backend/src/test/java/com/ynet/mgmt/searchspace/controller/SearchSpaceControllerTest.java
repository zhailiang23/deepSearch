package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchspace.dto.*;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import com.ynet.mgmt.searchspace.exception.SearchSpaceException;
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

    // 其他依赖项的Mock，只是为了满足Controller构造函数
    @MockBean
    private com.ynet.mgmt.searchspace.service.FileValidationService fileValidationService;

    @MockBean
    private com.ynet.mgmt.searchspace.service.FileStorageService fileStorageService;

    @MockBean
    private com.ynet.mgmt.searchspace.service.ElasticsearchManager elasticsearchManager;

    @MockBean
    private com.ynet.mgmt.jsonimport.service.DataImportService dataImportService;

    @MockBean
    private com.ynet.mgmt.jsonimport.service.JsonAnalysisService jsonAnalysisService;

    @MockBean
    private com.ynet.mgmt.jsonimport.service.IndexConfigService indexConfigService;

    @MockBean
    private co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;

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

    // ========== 索引映射配置相关测试 ==========

    @Test
    @DisplayName("获取索引映射配置 - 成功")
    void testGetIndexMapping_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String mockMappingJson = """
                {
                  "properties": {
                    "title": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    },
                    "content": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    },
                    "createTime": {
                      "type": "date",
                      "format": "yyyy-MM-dd HH:mm:ss"
                    }
                  }
                }
                """;

        when(searchSpaceService.getIndexMapping(searchSpaceId))
                .thenReturn(mockMappingJson);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取mapping配置成功"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isString());

        verify(searchSpaceService, times(1)).getIndexMapping(searchSpaceId);
    }

    @Test
    @DisplayName("获取索引映射配置 - 搜索空间不存在")
    void testGetIndexMapping_SearchSpaceNotFound() throws Exception {
        // Given
        Long searchSpaceId = 999L;
        SearchSpaceException exception = SearchSpaceException.searchSpaceNotFound(searchSpaceId);

        when(searchSpaceService.getIndexMapping(searchSpaceId))
                .thenThrow(exception);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在: " + searchSpaceId));

        verify(searchSpaceService, times(1)).getIndexMapping(searchSpaceId);
    }

    @Test
    @DisplayName("获取索引映射配置 - 索引不存在")
    void testGetIndexMapping_IndexNotFound() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        SearchSpaceException exception = SearchSpaceException.indexNotFound(searchSpaceId, "test_index");

        when(searchSpaceService.getIndexMapping(searchSpaceId))
                .thenThrow(exception);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间对应的索引不存在: test_index (搜索空间ID: " + searchSpaceId + ")"));

        verify(searchSpaceService, times(1)).getIndexMapping(searchSpaceId);
    }

    @Test
    @DisplayName("获取索引映射配置 - Elasticsearch连接失败")
    void testGetIndexMapping_ElasticsearchConnectionFailed() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        SearchSpaceException exception = SearchSpaceException.elasticsearchConnectionFailed(
                searchSpaceId, "GET_MAPPING", new RuntimeException("Connection refused"));

        when(searchSpaceService.getIndexMapping(searchSpaceId))
                .thenThrow(exception);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Elasticsearch服务不可用"));

        verify(searchSpaceService, times(1)).getIndexMapping(searchSpaceId);
    }

    @Test
    @DisplayName("获取索引映射配置 - 未知异常")
    void testGetIndexMapping_UnknownException() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        RuntimeException exception = new RuntimeException("Unexpected error");

        when(searchSpaceService.getIndexMapping(searchSpaceId))
                .thenThrow(exception);

        // When & Then
        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取mapping配置失败: Unexpected error"));

        verify(searchSpaceService, times(1)).getIndexMapping(searchSpaceId);
    }

    @Test
    @DisplayName("更新索引映射配置 - 成功")
    void testUpdateIndexMapping_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String mappingJson = """
                {
                  "properties": {
                    "title": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    },
                    "content": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    }
                  }
                }
                """;

        doNothing().when(searchSpaceService).updateIndexMapping(searchSpaceId, mappingJson);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson))
                .andExpect(status().isNoContent());

        verify(searchSpaceService, times(1)).updateIndexMapping(searchSpaceId, mappingJson);
    }

    @Test
    @DisplayName("更新索引映射配置 - JSON为空")
    void testUpdateIndexMapping_EmptyJson() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String emptyJson = "";

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("mapping配置不能为空"));

        verify(searchSpaceService, never()).updateIndexMapping(anyLong(), anyString());
    }

    @Test
    @DisplayName("更新索引映射配置 - JSON为null")
    void testUpdateIndexMapping_NullJson() throws Exception {
        // Given
        Long searchSpaceId = 1L;

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("mapping配置不能为空"));

        verify(searchSpaceService, never()).updateIndexMapping(anyLong(), anyString());
    }

    @Test
    @DisplayName("更新索引映射配置 - 搜索空间不存在")
    void testUpdateIndexMapping_SearchSpaceNotFound() throws Exception {
        // Given
        Long searchSpaceId = 999L;
        String mappingJson = "{\"properties\": {\"title\": {\"type\": \"text\"}}}";
        SearchSpaceException exception = SearchSpaceException.searchSpaceNotFound(searchSpaceId);

        doThrow(exception).when(searchSpaceService).updateIndexMapping(searchSpaceId, mappingJson);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在: " + searchSpaceId));

        verify(searchSpaceService, times(1)).updateIndexMapping(searchSpaceId, mappingJson);
    }

    @Test
    @DisplayName("更新索引映射配置 - 映射验证失败")
    void testUpdateIndexMapping_MappingValidationFailed() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String invalidMappingJson = "{\"invalid\": \"mapping\"}";
        SearchSpaceException exception = SearchSpaceException.mappingValidationFailed(
                searchSpaceId, "Invalid field type");

        doThrow(exception).when(searchSpaceService).updateIndexMapping(searchSpaceId, invalidMappingJson);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidMappingJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("映射配置验证失败: Invalid field type (搜索空间ID: " + searchSpaceId + ")"));

        verify(searchSpaceService, times(1)).updateIndexMapping(searchSpaceId, invalidMappingJson);
    }

    @Test
    @DisplayName("更新索引映射配置 - Elasticsearch连接失败")
    void testUpdateIndexMapping_ElasticsearchConnectionFailed() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String mappingJson = "{\"properties\": {\"title\": {\"type\": \"text\"}}}";
        SearchSpaceException exception = SearchSpaceException.elasticsearchConnectionFailed(
                searchSpaceId, "UPDATE_MAPPING", new RuntimeException("Connection refused"));

        doThrow(exception).when(searchSpaceService).updateIndexMapping(searchSpaceId, mappingJson);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Elasticsearch服务不可用"));

        verify(searchSpaceService, times(1)).updateIndexMapping(searchSpaceId, mappingJson);
    }

    @Test
    @DisplayName("更新索引映射配置 - 映射更新失败")
    void testUpdateIndexMapping_MappingUpdateFailed() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String mappingJson = "{\"properties\": {\"title\": {\"type\": \"text\"}}}";
        SearchSpaceException exception = SearchSpaceException.mappingUpdateFailed(
                searchSpaceId, "test_index", new RuntimeException("Update failed"));

        doThrow(exception).when(searchSpaceService).updateIndexMapping(searchSpaceId, mappingJson);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("映射更新失败: 更新索引映射失败: test_index (搜索空间ID: " + searchSpaceId + ")"));

        verify(searchSpaceService, times(1)).updateIndexMapping(searchSpaceId, mappingJson);
    }

    @Test
    @DisplayName("更新索引映射配置 - 未知异常")
    void testUpdateIndexMapping_UnknownException() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String mappingJson = "{\"properties\": {\"title\": {\"type\": \"text\"}}}";
        RuntimeException exception = new RuntimeException("Unexpected error");

        doThrow(exception).when(searchSpaceService).updateIndexMapping(searchSpaceId, mappingJson);

        // When & Then
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mappingJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("更新mapping配置失败: Unexpected error"));

        verify(searchSpaceService, times(1)).updateIndexMapping(searchSpaceId, mappingJson);
    }
}