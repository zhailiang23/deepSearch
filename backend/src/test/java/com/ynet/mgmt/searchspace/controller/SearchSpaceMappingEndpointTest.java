package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.service.*;
import com.ynet.mgmt.jsonimport.service.*;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SearchSpaceController Mapping端点单元测试
 * 专门测试新增的mapping相关API端点
 */
public class SearchSpaceMappingEndpointTest {

    @Mock
    private SearchSpaceService searchSpaceService;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ElasticsearchManager elasticsearchManager;

    @Mock
    private DataImportService dataImportService;

    @Mock
    private JsonAnalysisService jsonAnalysisService;

    @Mock
    private IndexConfigService indexConfigService;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    private SearchSpaceController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SearchSpaceController(
                searchSpaceService,
                fileValidationService,
                fileStorageService,
                elasticsearchManager,
                dataImportService,
                jsonAnalysisService,
                indexConfigService,
                elasticsearchClient
        );
    }

    @Test
    void testGetIndexMapping_Success() {
        // Given
        Long searchSpaceId = 1L;
        SearchSpaceDTO mockSearchSpace = new SearchSpaceDTO();
        mockSearchSpace.setId(searchSpaceId);
        mockSearchSpace.setName("测试搜索空间");
        mockSearchSpace.setCode("test-space");

        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(mockSearchSpace);

        // When
        ResponseEntity<ApiResponse<String>> response = controller.getIndexMapping(searchSpaceId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("获取mapping配置成功", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());

        // 验证返回的mapping是有效的JSON
        String mappingJson = response.getBody().getData();
        assertDoesNotThrow(() -> {
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(mappingJson);
        });
    }

    @Test
    void testGetIndexMapping_SearchSpaceNotFound() {
        // Given
        Long searchSpaceId = 999L;
        when(searchSpaceService.getSearchSpace(searchSpaceId))
                .thenThrow(new RuntimeException("搜索空间不存在"));

        // When
        ResponseEntity<ApiResponse<String>> response = controller.getIndexMapping(searchSpaceId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("搜索空间不存在", response.getBody().getMessage());
    }

    @Test
    void testUpdateIndexMapping_Success() {
        // Given
        Long searchSpaceId = 1L;
        String validMappingJson = "{\"properties\":{\"title\":{\"type\":\"text\"}}}";

        SearchSpaceDTO mockSearchSpace = new SearchSpaceDTO();
        mockSearchSpace.setId(searchSpaceId);
        mockSearchSpace.setName("测试搜索空间");
        mockSearchSpace.setCode("test-space");

        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(mockSearchSpace);

        // When
        ResponseEntity<ApiResponse<Void>> response = controller.updateIndexMapping(searchSpaceId, validMappingJson);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testUpdateIndexMapping_InvalidJson() {
        // Given
        Long searchSpaceId = 1L;
        String invalidMappingJson = "{invalid json}";

        SearchSpaceDTO mockSearchSpace = new SearchSpaceDTO();
        mockSearchSpace.setId(searchSpaceId);
        mockSearchSpace.setName("测试搜索空间");
        mockSearchSpace.setCode("test-space");

        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(mockSearchSpace);

        // When
        ResponseEntity<ApiResponse<Void>> response = controller.updateIndexMapping(searchSpaceId, invalidMappingJson);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("JSON格式错误"));
    }

    @Test
    void testUpdateIndexMapping_EmptyMapping() {
        // Given
        Long searchSpaceId = 1L;
        String emptyMappingJson = "";

        SearchSpaceDTO mockSearchSpace = new SearchSpaceDTO();
        mockSearchSpace.setId(searchSpaceId);
        mockSearchSpace.setName("测试搜索空间");
        mockSearchSpace.setCode("test-space");

        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(mockSearchSpace);

        // When
        ResponseEntity<ApiResponse<Void>> response = controller.updateIndexMapping(searchSpaceId, emptyMappingJson);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("mapping配置不能为空", response.getBody().getMessage());
    }

    @Test
    void testUpdateIndexMapping_SearchSpaceNotFound() {
        // Given
        Long searchSpaceId = 999L;
        String validMappingJson = "{\"properties\":{\"title\":{\"type\":\"text\"}}}";

        when(searchSpaceService.getSearchSpace(searchSpaceId))
                .thenThrow(new RuntimeException("搜索空间不存在"));

        // When
        ResponseEntity<ApiResponse<Void>> response = controller.updateIndexMapping(searchSpaceId, validMappingJson);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("搜索空间不存在", response.getBody().getMessage());
    }

    @Test
    void testGenerateMockMapping() throws JsonProcessingException {
        // Given
        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(1L);
        searchSpace.setName("测试搜索空间");
        searchSpace.setCode("test-space");

        when(searchSpaceService.getSearchSpace(1L)).thenReturn(searchSpace);

        // When
        ResponseEntity<ApiResponse<String>> response = controller.getIndexMapping(1L);

        // Then
        assertNotNull(response.getBody());
        String mappingJson = response.getBody().getData();

        // 验证生成的JSON是有效的
        ObjectMapper mapper = new ObjectMapper();
        var mappingNode = mapper.readTree(mappingJson);

        // 验证基础结构
        assertTrue(mappingNode.has("settings"));
        assertTrue(mappingNode.has("mappings"));
        assertTrue(mappingNode.get("mappings").has("properties"));

        // 验证基础字段存在
        var properties = mappingNode.get("mappings").get("properties");
        assertTrue(properties.has("id"));
        assertTrue(properties.has("title"));
        assertTrue(properties.has("content"));
        assertTrue(properties.has("created_at"));
        assertTrue(properties.has("updated_at"));
    }
}