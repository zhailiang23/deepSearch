package com.ynet.mgmt.searchdata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchdata.dto.SearchDataRequest;
import com.ynet.mgmt.searchdata.dto.SearchDataResponse;
import com.ynet.mgmt.searchdata.service.ElasticsearchDataService;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 拼音搜索API功能测试
 *
 * 测试目标：
 * - 验证API层正确传递拼音搜索参数
 * - 验证不同拼音搜索模式的API调用
 * - 验证API响应格式
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(ElasticsearchDataController.class)
@DisplayName("拼音搜索API功能测试")
public class ElasticsearchDataControllerPinyinTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ElasticsearchDataService elasticsearchDataService;

    @MockBean
    private SearchSpaceService searchSpaceService;

    private SearchSpaceDTO mockSearchSpace;
    private SearchDataResponse mockSearchResponse;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        mockSearchSpace = new SearchSpaceDTO();
        mockSearchSpace.setId(1L);
        mockSearchSpace.setName("测试搜索空间");
        mockSearchSpace.setCode("test_index");

        mockSearchResponse = SearchDataResponse.builder()
                .data(Arrays.asList(
                        SearchDataResponse.DocumentData.builder()
                                ._id("1")
                                ._score(1.0)
                                ._index("test_index")
                                ._source(java.util.Map.of("title", "张三", "content", "这是张三的信息"))
                                .build()
                ))
                .total(1L)
                .page(1)
                .size(20)
                .build();
    }

    @Test
    @DisplayName("默认拼音搜索功能测试")
    void testDefaultPinyinSearchEnabled() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备请求体
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhang");
        request.setPage(1);
        request.setSize(20);
        // 不设置拼音参数，使用默认值

        // 执行请求并验证
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("搜索成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.data[0]._source.title").value("张三"));

        // 验证服务层调用参数
        ArgumentCaptor<SearchDataRequest> requestCaptor = ArgumentCaptor.forClass(SearchDataRequest.class);
        verify(elasticsearchDataService).searchData(requestCaptor.capture(), eq(mockSearchSpace), eq(null));

        SearchDataRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getEnablePinyinSearch()).isTrue(); // 默认启用
        assertThat(capturedRequest.getPinyinMode()).isEqualTo("AUTO"); // 默认模式
    }

    @Test
    @DisplayName("启用拼音搜索 - AUTO模式")
    void testPinyinSearchAutoMode() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备请求体 - 明确设置拼音参数
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhang");
        request.setPage(1);
        request.setSize(20);
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("AUTO");

        // 执行请求
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证参数传递
        ArgumentCaptor<SearchDataRequest> requestCaptor = ArgumentCaptor.forClass(SearchDataRequest.class);
        verify(elasticsearchDataService).searchData(requestCaptor.capture(), eq(mockSearchSpace), eq(null));

        SearchDataRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getEnablePinyinSearch()).isTrue();
        assertThat(capturedRequest.getPinyinMode()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("启用拼音搜索 - STRICT模式")
    void testPinyinSearchStrictMode() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备请求体 - STRICT模式
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhang");
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("STRICT");

        // 执行请求
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证参数传递
        ArgumentCaptor<SearchDataRequest> requestCaptor = ArgumentCaptor.forClass(SearchDataRequest.class);
        verify(elasticsearchDataService).searchData(requestCaptor.capture(), eq(mockSearchSpace), eq(null));

        SearchDataRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getPinyinMode()).isEqualTo("STRICT");
    }

    @Test
    @DisplayName("启用拼音搜索 - FUZZY模式")
    void testPinyinSearchFuzzyMode() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备请求体 - FUZZY模式
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("z"); // 首字母搜索
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("FUZZY");

        // 执行请求
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证参数传递
        ArgumentCaptor<SearchDataRequest> requestCaptor = ArgumentCaptor.forClass(SearchDataRequest.class);
        verify(elasticsearchDataService).searchData(requestCaptor.capture(), eq(mockSearchSpace), eq(null));

        SearchDataRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getPinyinMode()).isEqualTo("FUZZY");
    }

    @Test
    @DisplayName("禁用拼音搜索功能测试")
    void testPinyinSearchDisabled() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备请求体 - 禁用拼音搜索
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhang");
        request.setEnablePinyinSearch(false); // 禁用拼音搜索

        // 执行请求
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证参数传递
        ArgumentCaptor<SearchDataRequest> requestCaptor = ArgumentCaptor.forClass(SearchDataRequest.class);
        verify(elasticsearchDataService).searchData(requestCaptor.capture(), eq(mockSearchSpace), eq(null));

        SearchDataRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getEnablePinyinSearch()).isFalse();
    }

    @Test
    @DisplayName("复杂拼音搜索查询测试")
    void testComplexPinyinQuery() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备复杂查询请求
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhangsan beijing"); // 多词拼音查询
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("AUTO");

        // 添加排序
        SearchDataRequest.SortConfig sort = new SearchDataRequest.SortConfig();
        sort.setField("_score");
        sort.setOrder("desc");
        request.setSort(sort);

        // 执行请求
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证调用
        verify(elasticsearchDataService).searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null));
    }

    @Test
    @DisplayName("无效拼音模式测试")
    void testInvalidPinyinMode() throws Exception {
        // 准备mock
        given(searchSpaceService.getSearchSpace(1L)).willReturn(mockSearchSpace);
        given(elasticsearchDataService.searchData(any(SearchDataRequest.class), eq(mockSearchSpace), eq(null)))
                .willReturn(mockSearchResponse);

        // 准备请求体 - 无效的拼音模式
        SearchDataRequest request = new SearchDataRequest();
        request.setSearchSpaceId("1");
        request.setQuery("zhang");
        request.setEnablePinyinSearch(true);
        request.setPinyinMode("INVALID_MODE"); // 无效模式，应该降级到AUTO

        // 执行请求
        mockMvc.perform(post("/elasticsearch/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 验证参数传递（服务层会处理无效模式）
        ArgumentCaptor<SearchDataRequest> requestCaptor = ArgumentCaptor.forClass(SearchDataRequest.class);
        verify(elasticsearchDataService).searchData(requestCaptor.capture(), eq(mockSearchSpace), eq(null));

        SearchDataRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getEnablePinyinSearch()).isTrue();
        assertThat(capturedRequest.getPinyinMode()).isEqualTo("INVALID_MODE"); // API层原样传递，服务层负责处理
    }
}