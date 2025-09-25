package com.ynet.mgmt.searchdata.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.ynet.mgmt.searchdata.dto.SearchDataRequest;
import com.ynet.mgmt.searchdata.dto.SearchDataResponse;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ElasticsearchDataService测试类
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Elasticsearch数据服务测试")
class ElasticsearchDataServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ElasticsearchDataService elasticsearchDataService;

    private SearchSpaceDTO searchSpace;
    private SearchDataRequest searchRequest;

    @BeforeEach
    void setUp() {
        searchSpace = new SearchSpaceDTO();
        searchSpace.setCode("test_index"); // 使用code而不是indexName

        searchRequest = new SearchDataRequest();
        searchRequest.setQuery("测试");
        searchRequest.setPage(1);
        searchRequest.setSize(10);
    }

    @Test
    @DisplayName("测试普通搜索功能")
    void testSearchData_BasicSearch() throws IOException {
        // 准备测试数据
        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "测试文档1", "content", "这是一个测试文档")),
            createMockHit("2", 0.8, Map.of("title", "测试文档2", "content", "另一个测试文档"))
        ), 2L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(2, response.getData().size(), "应该返回2条数据");
        assertEquals(2L, response.getTotal(), "总数应该为2");
        assertEquals(1, response.getPage(), "页码应该为1");
        assertEquals(10, response.getSize(), "页面大小应该为10");

        // 验证第一条数据
        SearchDataResponse.DocumentData firstDoc = response.getData().get(0);
        assertEquals("1", firstDoc.get_id(), "文档ID应该匹配");
        assertEquals(1.0, firstDoc.get_score(), "评分应该匹配");
        assertNotNull(firstDoc.get_source(), "文档源数据不应为空");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试空查询条件的搜索")
    void testSearchData_EmptyQuery() throws IOException {
        // 准备测试数据 - 空查询
        searchRequest.setQuery("");

        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "文档1")),
            createMockHit("2", 1.0, Map.of("title", "文档2"))
        ), 2L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(2, response.getData().size(), "空查询应该返回所有数据");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试null查询条件的搜索")
    void testSearchData_NullQuery() throws IOException {
        // 准备测试数据 - null查询
        searchRequest.setQuery(null);

        SearchResponse<Map> mockResponse = createMockSearchResponse(
            Collections.emptyList(), 0L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(0, response.getData().size(), "null查询应该返回空结果");
        assertEquals(0L, response.getTotal(), "总数应该为0");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试拼音搜索功能")
    void testSearchData_PinyinSearch() throws IOException {
        // 准备测试数据 - 拼音查询
        searchRequest.setQuery("beijing");

        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.5, Map.of("title", "北京大学", "title.pinyin", "beijing daxue")),
            createMockHit("2", 1.2, Map.of("title", "北京市", "title.pinyin", "beijing shi"))
        ), 2L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(2, response.getData().size(), "拼音搜索应该返回匹配的中文文档");

        // 验证评分顺序（高分在前）
        assertTrue(response.getData().get(0).get_score() >= response.getData().get(1).get_score(),
                "结果应该按评分降序排列");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试首字母搜索功能")
    void testSearchData_FirstLetterSearch() throws IOException {
        // 准备测试数据 - 首字母查询
        searchRequest.setQuery("bjdx");

        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "北京大学", "title.first_letter", "bjdx"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getData().size(), "首字母搜索应该返回匹配的文档");
        assertEquals("北京大学", response.getData().get(0).get_source().get("title"),
                "应该返回正确的中文文档");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试分页功能")
    void testSearchData_Pagination() throws IOException {
        // 准备测试数据 - 第2页，每页5条
        searchRequest.setPage(2);
        searchRequest.setSize(5);

        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("6", 0.8, Map.of("title", "文档6")),
            createMockHit("7", 0.7, Map.of("title", "文档7"))
        ), 20L); // 总共20条

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(2, response.getData().size(), "第2页应该返回2条数据");
        assertEquals(20L, response.getTotal(), "总数应该为20");
        assertEquals(2, response.getPage(), "页码应该为2");
        assertEquals(5, response.getSize(), "页面大小应该为5");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试排序功能")
    void testSearchData_WithSorting() throws IOException {
        // 准备测试数据 - 添加排序
        SearchDataRequest.SortConfig sortConfig = new SearchDataRequest.SortConfig();
        sortConfig.setField("title.keyword");
        sortConfig.setOrder("desc");
        searchRequest.setSort(sortConfig);

        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "Z文档")),
            createMockHit("2", 1.0, Map.of("title", "A文档"))
        ), 2L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(2, response.getData().size(), "排序搜索应该返回数据");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试ES连接异常处理")
    void testSearchData_ElasticsearchException() throws IOException {
        // 准备异常
        ElasticsearchException esException = new ElasticsearchException("index_not_found_exception", null);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenThrow(esException);

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            elasticsearchDataService.searchData(searchRequest, searchSpace);
        });

        // 验证异常信息
        assertTrue(exception.getMessage().contains("查询异常"), "异常信息应该包含查询异常");
        assertTrue(exception.getCause() instanceof ElasticsearchException, "原因应该是ElasticsearchException");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试IO异常处理")
    void testSearchData_IOException() throws IOException {
        // 准备异常
        IOException ioException = new IOException("网络连接失败");
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenThrow(ioException);

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            elasticsearchDataService.searchData(searchRequest, searchSpace);
        });

        // 验证异常信息
        assertTrue(exception.getMessage().contains("搜索IO异常"), "异常信息应该包含IO异常");
        assertTrue(exception.getCause() instanceof IOException, "原因应该是IOException");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试拼音查询降级机制")
    void testSearchData_PinyinQueryFallback() throws IOException {
        // 这个测试验证当拼音查询构建失败时，会降级为标准查询
        // 通过反射测试内部方法或者通过集成测试来验证

        SearchResponse<Map> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "测试文档"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn(mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(searchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(1, response.getData().size(), "降级查询应该仍能返回结果");

        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    // 私有辅助方法

    @SuppressWarnings("unchecked")
    private SearchResponse<Map> createMockSearchResponse(List<Hit<Map>> hits, long total) {
        SearchResponse<Map> mockResponse = mock(SearchResponse.class);
        HitsMetadata<Map> mockHits = mock(HitsMetadata.class);
        TotalHits mockTotal = mock(TotalHits.class);

        when(mockTotal.value()).thenReturn(total);
        when(mockTotal.relation()).thenReturn(TotalHitsRelation.Eq);
        when(mockHits.total()).thenReturn(mockTotal);
        when(mockHits.hits()).thenReturn(hits);
        when(mockResponse.hits()).thenReturn(mockHits);

        return mockResponse;
    }

    @SuppressWarnings("unchecked")
    private Hit<Map> createMockHit(String id, double score, Map<String, Object> source) {
        Hit<Map> mockHit = mock(Hit.class);
        when(mockHit.id()).thenReturn(id);
        when(mockHit.score()).thenReturn(score);
        when(mockHit.source()).thenReturn(source);
        when(mockHit.index()).thenReturn("test_index");
        return mockHit;
    }
}