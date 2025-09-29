package com.ynet.mgmt.searchdata.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
 * ElasticsearchDataService 语义搜索测试类
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Elasticsearch数据服务语义搜索测试")
class ElasticsearchDataServiceSemanticTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ElasticsearchDataService elasticsearchDataService;

    private SearchSpaceDTO searchSpace;
    private SearchDataRequest semanticSearchRequest;

    @BeforeEach
    void setUp() {
        searchSpace = new SearchSpaceDTO();
        searchSpace.setCode("bank_services");

        semanticSearchRequest = new SearchDataRequest();
        semanticSearchRequest.setQuery("我想取钱");
        semanticSearchRequest.setPage(1);
        semanticSearchRequest.setSize(10);
        semanticSearchRequest.setEnableSemanticSearch(true);
        semanticSearchRequest.setSemanticMode("AUTO");
        semanticSearchRequest.setSemanticWeight(0.3);
    }

    @Test
    @DisplayName("测试混合搜索 - 语义+关键词")
    void testSearchData_HybridSearch() throws IOException {
        // 准备向量服务响应
        List<Float> queryVector = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        when(embeddingService.isServiceAvailable()).thenReturn(true);
        when(embeddingService.getTextEmbedding("我想取钱")).thenReturn(queryVector);

        // 准备ES搜索响应
        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.8, Map.of(
                "title", "附近网点查询",
                "content", "查找最近的银行网点和ATM位置",
                "type", "location_service"
            )),
            createMockHit("2", 1.5, Map.of(
                "title", "取款服务",
                "content", "提供24小时取款服务的ATM网点",
                "type", "atm_service"
            )),
            createMockHit("3", 1.2, Map.of(
                "title", "银行卡管理",
                "content", "银行卡挂失、解锁等服务",
                "type", "card_service"
            ))
        ), 3L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals(3, response.getData().size(), "应该返回3条数据");
        assertEquals(3L, response.getTotal(), "总数应该为3");

        // 验证最相关的结果在前面（"附近网点查询"）
        SearchDataResponse.DocumentData firstDoc = response.getData().get(0);
        assertEquals("附近网点查询", firstDoc.get_source().get("title"), "最相关的结果应该在前面");
        assertTrue(firstDoc.get_score() > 1.5, "语义搜索应该提高相关文档的评分");

        // 验证搜索元数据
        assertNotNull(response.getSearchMetadata(), "搜索元数据不应为空");
        assertEquals("HYBRID", response.getSearchMetadata().getSearchMode(), "搜索模式应该是混合模式");
        assertTrue(response.getSearchMetadata().getSemanticEnabled(), "应该启用语义搜索");
        assertEquals(0.3, response.getSearchMetadata().getSemanticWeight(), "语义权重应该匹配");
        assertEquals("hybrid", response.getSearchMetadata().getActualQueryType(), "实际查询类型应该是混合");

        verify(embeddingService, times(1)).isServiceAvailable();
        verify(embeddingService, times(1)).getTextEmbedding("我想取钱");
        verify(elasticsearchClient, times(1)).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("测试纯语义搜索模式")
    void testSearchData_SemanticOnlyMode() throws IOException {
        // 设置为纯语义搜索
        semanticSearchRequest.setSemanticMode("SEMANTIC_FIRST");

        // 准备向量服务响应
        List<Float> queryVector = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);
        when(embeddingService.isServiceAvailable()).thenReturn(true);
        when(embeddingService.getTextEmbedding("我想取钱")).thenReturn(queryVector);

        // 准备ES搜索响应
        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 0.95, Map.of(
                "title", "ATM取款",
                "content", "自动取款机服务指南"
            ))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals("SEMANTIC_FIRST", response.getSearchMetadata().getSearchMode(), "搜索模式应该是语义优先");
        assertEquals("semantic", response.getSearchMetadata().getActualQueryType(), "实际查询类型应该是语义");

        verify(embeddingService, times(1)).getTextEmbedding("我想取钱");
    }

    @Test
    @DisplayName("测试语义搜索服务不可用时的降级")
    void testSearchData_SemanticServiceDownFallback() throws IOException {
        // 模拟语义服务不可用
        when(embeddingService.isServiceAvailable()).thenReturn(false);

        // 准备传统搜索响应
        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of(
                "title", "取钱相关文档",
                "content", "包含取钱关键词的文档"
            ))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertNotNull(response, "响应不应为空");
        assertEquals("AUTO", response.getSearchMetadata().getSearchMode(), "搜索模式应该是自动");
        assertEquals("keyword", response.getSearchMetadata().getActualQueryType(), "应该降级为关键词搜索");
        assertEquals("unavailable", response.getSearchMetadata().getVectorServiceStatus(), "向量服务状态应该是不可用");
        assertFalse(response.getSearchMetadata().getSemanticEnabled(), "语义搜索应该被禁用");
        assertNotNull(response.getSearchMetadata().getAdjustmentReason(), "应该有调整原因说明");

        verify(embeddingService, times(1)).isServiceAvailable();
        verify(embeddingService, never()).getTextEmbedding(anyString());
    }

    @Test
    @DisplayName("测试短查询自动降级为关键词搜索")
    void testSearchData_ShortQueryFallback() throws IOException {
        // 设置短查询
        semanticSearchRequest.setQuery("钱");

        // 即使语义服务可用，短查询也应该使用关键词搜索
        when(embeddingService.isServiceAvailable()).thenReturn(true);

        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "钱相关文档"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertEquals("keyword", response.getSearchMetadata().getActualQueryType(), "短查询应该使用关键词搜索");
        assertEquals(1, response.getSearchMetadata().getQueryLength(), "查询长度应该为1");
        assertNotNull(response.getSearchMetadata().getAdjustmentReason(), "应该有调整原因说明");

        verify(embeddingService, times(1)).isServiceAvailable();
        verify(embeddingService, never()).getTextEmbedding(anyString());
    }

    @Test
    @DisplayName("测试长查询优先使用语义搜索")
    void testSearchData_LongQuerySemanticPreference() throws IOException {
        // 设置长查询
        semanticSearchRequest.setQuery("我想要在附近找一个可以取钱的地方，最好是银行网点或者ATM机");

        when(embeddingService.isServiceAvailable()).thenReturn(true);
        when(embeddingService.getTextEmbedding(anyString()))
            .thenReturn(Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f));

        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.5, Map.of("title", "银行网点查询"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertEquals("hybrid", response.getSearchMetadata().getActualQueryType(), "长查询应该使用混合搜索");
        assertTrue(response.getSearchMetadata().getQueryLength() > 10, "查询长度应该大于10");

        verify(embeddingService, times(1)).getTextEmbedding(anyString());
    }

    @Test
    @DisplayName("测试关键词优先模式")
    void testSearchData_KeywordFirstMode() throws IOException {
        semanticSearchRequest.setSemanticMode("KEYWORD_FIRST");

        when(embeddingService.isServiceAvailable()).thenReturn(true);

        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "取钱服务"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertEquals("KEYWORD_FIRST", response.getSearchMetadata().getSearchMode(), "搜索模式应该是关键词优先");
        assertEquals("keyword", response.getSearchMetadata().getActualQueryType(), "实际查询类型应该是关键词");

        // 关键词优先模式不应该调用语义服务
        verify(embeddingService, never()).getTextEmbedding(anyString());
    }

    @Test
    @DisplayName("测试向量生成失败的处理")
    void testSearchData_VectorGenerationFailure() throws IOException {
        when(embeddingService.isServiceAvailable()).thenReturn(true);
        when(embeddingService.getTextEmbedding("我想取钱")).thenReturn(null);

        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "取钱服务"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertEquals("keyword", response.getSearchMetadata().getActualQueryType(), "向量生成失败应该降级为关键词搜索");
        assertNotNull(response.getSearchMetadata().getAdjustmentReason(), "应该有调整原因说明");

        verify(embeddingService, times(1)).getTextEmbedding("我想取钱");
    }

    @Test
    @DisplayName("测试性能监控指标")
    void testSearchData_PerformanceMetrics() throws IOException {
        when(embeddingService.isServiceAvailable()).thenReturn(true);
        when(embeddingService.getTextEmbedding("我想取钱"))
            .thenReturn(Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f));

        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "测试文档"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证性能指标
        SearchDataResponse.SearchMetadata metadata = response.getSearchMetadata();
        assertNotNull(metadata.getQueryBuildTime(), "查询构建耗时应该被记录");
        assertNotNull(metadata.getVectorGenerationTime(), "向量生成耗时应该被记录");
        assertNotNull(metadata.getElasticsearchTime(), "ES查询耗时应该被记录");
        assertNotNull(metadata.getTotalTime(), "总耗时应该被记录");

        assertTrue(metadata.getQueryBuildTime() >= 0, "查询构建耗时应该大于等于0");
        assertTrue(metadata.getVectorGenerationTime() >= 0, "向量生成耗时应该大于等于0");
        assertTrue(metadata.getElasticsearchTime() >= 0, "ES查询耗时应该大于等于0");
        assertTrue(metadata.getTotalTime() >= 0, "总耗时应该大于等于0");
    }

    @Test
    @DisplayName("测试禁用语义搜索")
    void testSearchData_SemanticSearchDisabled() throws IOException {
        semanticSearchRequest.setEnableSemanticSearch(false);

        SearchResponse<Map<String, Object>> mockResponse = createMockSearchResponse(Arrays.asList(
            createMockHit("1", 1.0, Map.of("title", "取钱服务"))
        ), 1L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
            .thenReturn((SearchResponse) mockResponse);

        // 执行测试
        SearchDataResponse response = elasticsearchDataService.searchData(semanticSearchRequest, searchSpace);

        // 验证结果
        assertFalse(response.getSearchMetadata().getSemanticEnabled(), "语义搜索应该被禁用");
        assertEquals("keyword", response.getSearchMetadata().getActualQueryType(), "应该使用关键词搜索");

        // 不应该调用语义服务
        verify(embeddingService, never()).isServiceAvailable();
        verify(embeddingService, never()).getTextEmbedding(anyString());
    }

    // 私有辅助方法

    @SuppressWarnings("unchecked")
    private SearchResponse<Map<String, Object>> createMockSearchResponse(List<Hit<Map<String, Object>>> hits, long total) {
        SearchResponse<Map<String, Object>> mockResponse = mock(SearchResponse.class);
        HitsMetadata<Map<String, Object>> mockHits = mock(HitsMetadata.class);
        TotalHits mockTotal = mock(TotalHits.class);

        when(mockTotal.value()).thenReturn(total);
        when(mockTotal.relation()).thenReturn(TotalHitsRelation.Eq);
        when(mockHits.total()).thenReturn(mockTotal);
        when(mockHits.hits()).thenReturn(hits);
        when(mockResponse.hits()).thenReturn(mockHits);

        return mockResponse;
    }

    @SuppressWarnings("unchecked")
    private Hit<Map<String, Object>> createMockHit(String id, double score, Map<String, Object> source) {
        Hit<Map<String, Object>> mockHit = mock(Hit.class);
        when(mockHit.id()).thenReturn(id);
        when(mockHit.score()).thenReturn(score);
        when(mockHit.source()).thenReturn(source);
        when(mockHit.index()).thenReturn("bank_services");
        return mockHit;
    }
}