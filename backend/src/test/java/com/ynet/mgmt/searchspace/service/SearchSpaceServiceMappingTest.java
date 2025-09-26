package com.ynet.mgmt.searchspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import com.ynet.mgmt.searchspace.exception.SearchSpaceException;
import com.ynet.mgmt.searchspace.mapper.SearchSpaceMapper;
import com.ynet.mgmt.searchspace.repository.SearchSpaceRepository;
import com.ynet.mgmt.searchspace.service.impl.SearchSpaceServiceImpl;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SearchSpaceService 索引映射相关方法测试类
 * 测试 getIndexMapping 和 updateIndexMapping 方法的各种场景
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSpaceService 索引映射方法测试")
class SearchSpaceServiceMappingTest {

    @Mock
    private SearchSpaceRepository searchSpaceRepository;

    @Mock
    private ElasticsearchManager elasticsearchManager;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient indicesClient;

    @Mock
    private SearchSpaceMapper mapper;

    @InjectMocks
    private SearchSpaceServiceImpl searchSpaceService;

    private SearchSpace mockSearchSpace;
    private String mockMappingJson;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        mockSearchSpace = createMockSearchSpace();
        mockMappingJson = createMockMappingJson();

        // 设置 ElasticsearchClient 的 indices() 方法返回 mock 的 IndicesClient
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
    }

    // ========== getIndexMapping 方法测试 ==========

    @Test
    @DisplayName("获取索引映射配置 - 成功")
    void testGetIndexMapping_Success() throws Exception {
        // Given
        Long spaceId = 1L;
        String indexName = "test_space_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(true);

        // Mock Elasticsearch response
        GetMappingResponse mockResponse = createMockGetMappingResponse();
        when(indicesClient.getMapping(any(GetMappingRequest.class)))
                .thenReturn(mockResponse);

        // When
        String result = searchSpaceService.getIndexMapping(spaceId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).contains("properties");

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, times(1)).findActualIndexName(mockSearchSpace.getCode());
        verify(elasticsearchManager, times(1)).indexExists(indexName);
        verify(indicesClient, times(1)).getMapping(any(GetMappingRequest.class));
    }

    @Test
    @DisplayName("获取索引映射配置 - 搜索空间不存在")
    void testGetIndexMapping_SearchSpaceNotFound() {
        // Given
        Long spaceId = 999L;
        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.getIndexMapping(spaceId))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("搜索空间不存在: " + spaceId)
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("SEARCH_SPACE_NOT_FOUND");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, never()).findActualIndexName(any());
    }

    @Test
    @DisplayName("获取索引映射配置 - 索引不存在")
    void testGetIndexMapping_IndexNotFound() {
        // Given
        Long spaceId = 1L;
        String indexName = "non_existent_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.getIndexMapping(spaceId))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("搜索空间对应的索引不存在")
                .hasMessageContaining(indexName)
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("INDEX_NOT_FOUND");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, times(1)).findActualIndexName(mockSearchSpace.getCode());
        verify(elasticsearchManager, times(1)).indexExists(indexName);
        verify(indicesClient, never()).getMapping(any(GetMappingRequest.class));
    }

    @Test
    @DisplayName("获取索引映射配置 - Elasticsearch连接失败")
    void testGetIndexMapping_ElasticsearchConnectionFailed() throws Exception {
        // Given
        Long spaceId = 1L;
        String indexName = "test_space_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(true);
        when(indicesClient.getMapping(any(GetMappingRequest.class)))
                .thenThrow(new IOException("Connection refused"));

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.getIndexMapping(spaceId))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("Elasticsearch连接失败")
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("ELASTICSEARCH_CONNECTION_FAILED");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                    assertThat(searchSpaceEx.getOperation()).isEqualTo("GET_MAPPING");
                });

        verify(indicesClient, times(1)).getMapping(any(GetMappingRequest.class));
    }

    @Test
    @DisplayName("获取索引映射配置 - 索引映射为空")
    void testGetIndexMapping_EmptyMapping() throws Exception {
        // Given
        Long spaceId = 1L;
        String indexName = "test_space_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(true);

        // Mock empty response
        GetMappingResponse emptyResponse = mock(GetMappingResponse.class);
        when(emptyResponse.result()).thenReturn(Map.of());
        when(indicesClient.getMapping(any(GetMappingRequest.class)))
                .thenReturn(emptyResponse);

        // When
        String result = searchSpaceService.getIndexMapping(spaceId);

        // Then
        assertThat(result).isEqualTo("{}");

        verify(indicesClient, times(1)).getMapping(any(GetMappingRequest.class));
    }

    // ========== updateIndexMapping 方法测试 ==========

    @Test
    @DisplayName("更新索引映射配置 - 成功")
    void testUpdateIndexMapping_Success() throws Exception {
        // Given
        Long spaceId = 1L;
        String indexName = "test_space_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(true);

        // Mock successful Elasticsearch response
        PutMappingResponse mockResponse = mock(PutMappingResponse.class);
        when(mockResponse.acknowledged()).thenReturn(true);
        when(indicesClient.putMapping(any(PutMappingRequest.class)))
                .thenReturn(mockResponse);

        when(searchSpaceRepository.save(any(SearchSpace.class)))
                .thenReturn(mockSearchSpace);

        // When
        assertThatCode(() -> searchSpaceService.updateIndexMapping(spaceId, mockMappingJson))
                .doesNotThrowAnyException();

        // Then
        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, times(1)).findActualIndexName(mockSearchSpace.getCode());
        verify(elasticsearchManager, times(1)).indexExists(indexName);
        verify(indicesClient, times(1)).putMapping(any(PutMappingRequest.class));
        verify(searchSpaceRepository, times(1)).save(argThat(space ->
                space.getIndexMapping().equals(mockMappingJson)));
    }

    @Test
    @DisplayName("更新索引映射配置 - 搜索空间不存在")
    void testUpdateIndexMapping_SearchSpaceNotFound() {
        // Given
        Long spaceId = 999L;
        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, mockMappingJson))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("搜索空间不存在: " + spaceId)
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("SEARCH_SPACE_NOT_FOUND");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, never()).findActualIndexName(any());
    }

    @Test
    @DisplayName("更新索引映射配置 - JSON为空")
    void testUpdateIndexMapping_EmptyJson() {
        // Given
        Long spaceId = 1L;
        String emptyJson = "";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, emptyJson))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("映射配置不能为空")
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("MAPPING_VALIDATION_FAILED");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, never()).findActualIndexName(any());
    }

    @Test
    @DisplayName("更新索引映射配置 - JSON为null")
    void testUpdateIndexMapping_NullJson() {
        // Given
        Long spaceId = 1L;

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, null))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("映射配置不能为空")
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("MAPPING_VALIDATION_FAILED");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, never()).findActualIndexName(any());
    }

    @Test
    @DisplayName("更新索引映射配置 - JSON格式错误")
    void testUpdateIndexMapping_InvalidJson() {
        // Given
        Long spaceId = 1L;
        String invalidJson = "{invalid json format";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, invalidJson))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("JSON格式错误")
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("MAPPING_VALIDATION_FAILED");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, never()).findActualIndexName(any());
    }

    @Test
    @DisplayName("更新索引映射配置 - 索引不存在")
    void testUpdateIndexMapping_IndexNotFound() {
        // Given
        Long spaceId = 1L;
        String indexName = "non_existent_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, mockMappingJson))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("搜索空间对应的索引不存在")
                .hasMessageContaining(indexName)
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("INDEX_NOT_FOUND");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(searchSpaceRepository, times(1)).findById(spaceId);
        verify(elasticsearchManager, times(1)).findActualIndexName(mockSearchSpace.getCode());
        verify(elasticsearchManager, times(1)).indexExists(indexName);
        verify(indicesClient, never()).putMapping(any(PutMappingRequest.class));
    }

    @Test
    @DisplayName("更新索引映射配置 - Elasticsearch连接失败")
    void testUpdateIndexMapping_ElasticsearchConnectionFailed() throws Exception {
        // Given
        Long spaceId = 1L;
        String indexName = "test_space_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(true);
        when(indicesClient.putMapping(any(PutMappingRequest.class)))
                .thenThrow(new IOException("Connection refused"));

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, mockMappingJson))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("Elasticsearch连接失败")
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("ELASTICSEARCH_CONNECTION_FAILED");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                    assertThat(searchSpaceEx.getOperation()).isEqualTo("UPDATE_MAPPING");
                });

        verify(indicesClient, times(1)).putMapping(any(PutMappingRequest.class));
        verify(searchSpaceRepository, never()).save(any(SearchSpace.class));
    }

    @Test
    @DisplayName("更新索引映射配置 - Elasticsearch未确认更新")
    void testUpdateIndexMapping_ElasticsearchNotAcknowledged() throws Exception {
        // Given
        Long spaceId = 1L;
        String indexName = "test_space_index";

        when(searchSpaceRepository.findById(spaceId))
                .thenReturn(Optional.of(mockSearchSpace));
        when(elasticsearchManager.findActualIndexName(mockSearchSpace.getCode()))
                .thenReturn(indexName);
        when(elasticsearchManager.indexExists(indexName))
                .thenReturn(true);

        // Mock unacknowledged response
        PutMappingResponse mockResponse = mock(PutMappingResponse.class);
        when(mockResponse.acknowledged()).thenReturn(false);
        when(indicesClient.putMapping(any(PutMappingRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> searchSpaceService.updateIndexMapping(spaceId, mockMappingJson))
                .isInstanceOf(SearchSpaceException.class)
                .hasMessageContaining("更新索引映射失败")
                .hasMessageContaining(indexName)
                .satisfies(ex -> {
                    SearchSpaceException searchSpaceEx = (SearchSpaceException) ex;
                    assertThat(searchSpaceEx.getErrorCode()).isEqualTo("MAPPING_UPDATE_FAILED");
                    assertThat(searchSpaceEx.getSpaceId()).isEqualTo(spaceId);
                });

        verify(indicesClient, times(1)).putMapping(any(PutMappingRequest.class));
        verify(searchSpaceRepository, never()).save(any(SearchSpace.class));
    }

    // ========== 辅助方法 ==========

    private SearchSpace createMockSearchSpace() {
        SearchSpace searchSpace = new SearchSpace();
        searchSpace.setId(1L);
        searchSpace.setName("测试搜索空间");
        searchSpace.setCode("test_space");
        searchSpace.setDescription("测试用的搜索空间");
        searchSpace.setStatus(SearchSpaceStatus.ACTIVE);
        searchSpace.setVersion(1L);
        searchSpace.setCreatedAt(LocalDateTime.now());
        searchSpace.setUpdatedAt(LocalDateTime.now());
        return searchSpace;
    }

    private String createMockMappingJson() {
        return """
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
    }

    private GetMappingResponse createMockGetMappingResponse() throws Exception {
        // 创建 mock TypeMapping
        TypeMapping mockTypeMapping = mock(TypeMapping.class);

        // 创建 mock IndexMappingRecord
        IndexMappingRecord mockIndexMappingRecord = mock(IndexMappingRecord.class);
        when(mockIndexMappingRecord.mappings()).thenReturn(mockTypeMapping);

        // 创建映射结果
        Map<String, IndexMappingRecord> mappingResult = new HashMap<>();
        mappingResult.put("test_space_index", mockIndexMappingRecord);

        // 创建 mock GetMappingResponse
        GetMappingResponse mockResponse = mock(GetMappingResponse.class);
        when(mockResponse.result()).thenReturn(mappingResult);

        return mockResponse;
    }
}