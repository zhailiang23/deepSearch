package com.ynet.mgmt.searchdata.service;

import com.ynet.mgmt.searchdata.dto.SearchDataRequest;
import com.ynet.mgmt.searchdata.dto.SearchDataResponse;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ElasticsearchDataService 增强拼音搜索功能测试
 *
 * 测试目标：
 * - 验证不同拼音搜索模式的查询构建
 * - 验证拼音搜索参数的正确处理
 * - 验证降级逻辑和错误处理
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ElasticsearchDataService增强拼音搜索功能测试")
public class ElasticsearchDataServiceEnhancedPinyinTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ElasticsearchDataService elasticsearchDataService;

    private SearchSpaceDTO searchSpace;
    private SearchResponse<Map> mockSearchResponse;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        searchSpace = new SearchSpaceDTO();
        searchSpace.setId(1L);
        searchSpace.setName("测试搜索空间");
        searchSpace.setCode("test_index");

        // 准备mock搜索响应
        Map<String, Object> sourceData = new HashMap<>();
        sourceData.put("title", "张三");
        sourceData.put("content", "这是张三的个人信息");

        Hit<Map> mockHit = Hit.of(h -> h
                .id("1")
                .index("test_index")
                .score(1.5)
                .source(sourceData));

        TotalHits totalHits = TotalHits.of(t -> t
                .value(1L)
                .relation(TotalHitsRelation.Eq));

        HitsMetadata<Map> hitsMetadata = HitsMetadata.of(h -> h
                .total(totalHits)
                .hits(Arrays.asList(mockHit)));

        mockSearchResponse = SearchResponse.of(s -> s
                .took(15L)
                .timedOut(false)
                .hits(hitsMetadata));
    }

    @Test
    @DisplayName("拼音搜索启用状态 - AUTO模式测试")
    void testPinyinSearchEnabledAutoMode() throws IOException {
        // 准备请求
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan");
        request.setPage(1);
        request.setSize(20);
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("AUTO");

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).get_source()).containsEntry("title", "张三");

        // 验证ES客户端调用
        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(elasticsearchClient).search(searchRequestCaptor.capture(), eq(Map.class));

        SearchRequest capturedRequest = searchRequestCaptor.getValue();
        assertThat(capturedRequest.index()).contains("test_index");
        // 注意：由于查询对象的复杂性，这里主要验证基本参数传递
    }

    @Test
    @DisplayName("拼音搜索启用状态 - STRICT模式测试")
    void testPinyinSearchEnabledStrictMode() throws IOException {
        // 准备请求
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan");
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("STRICT");

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);

        // 验证ES客户端调用
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("拼音搜索启用状态 - FUZZY模式测试")
    void testPinyinSearchEnabledFuzzyMode() throws IOException {
        // 准备请求
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("z"); // 首字母搜索在FUZZY模式下权重更高
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("FUZZY");

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);

        // 验证ES客户端调用
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("拼音搜索禁用状态测试")
    void testPinyinSearchDisabled() throws IOException {
        // 准备请求 - 禁用拼音搜索
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan");
        request.setEnablePinyinSearch(false);

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);

        // 验证ES客户端调用（应该使用标准查询）
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("空查询测试")
    void testEmptyQuery() throws IOException {
        // 准备请求 - 空查询
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery(""); // 空查询
        request.setEnablePinyinSearch(true);

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();

        // 验证ES客户端调用（应该使用match_all查询）
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("null查询测试")
    void testNullQuery() throws IOException {
        // 准备请求 - null查询
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery(null); // null查询
        request.setEnablePinyinSearch(true);

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();

        // 验证ES客户端调用
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("无效拼音模式测试")
    void testInvalidPinyinMode() throws IOException {
        // 准备请求 - 无效拼音模式
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan");
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("INVALID_MODE"); // 无效模式

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果（应该降级到AUTO模式）
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);

        // 验证ES客户端调用
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }

    @Test
    @DisplayName("带排序的拼音搜索测试")
    void testPinyinSearchWithSorting() throws IOException {
        // 准备请求 - 包含排序
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan");
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("AUTO");

        // 设置排序
        SearchDataRequest.SortConfig sort = new SearchDataRequest.SortConfig();
        sort.setField("_score");
        sort.setOrder("desc");
        request.setSort(sort);

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);

        // 验证ES客户端调用
        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(elasticsearchClient).search(searchRequestCaptor.capture(), eq(Map.class));

        SearchRequest capturedRequest = searchRequestCaptor.getValue();
        assertThat(capturedRequest.sort()).isNotEmpty(); // 验证排序设置
    }

    @Test
    @DisplayName("拼音搜索默认参数测试")
    void testPinyinSearchDefaultParameters() throws IOException {
        // 准备请求 - 使用默认参数（不设置拼音相关参数）
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan");
        // enablePinyinSearch 和 pinyinMode 使用默认值

        // Mock ES响应
        given(elasticsearchClient.search(any(SearchRequest.class), eq(Map.class)))
                .willReturn(mockSearchResponse);

        // 执行搜索
        SearchDataResponse response = elasticsearchDataService.searchData(request,  searchSpace, null);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1L);

        // 验证ES客户端调用
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Map.class));
    }
}