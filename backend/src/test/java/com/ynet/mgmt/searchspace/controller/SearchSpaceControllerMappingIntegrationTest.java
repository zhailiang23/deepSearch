package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.exception.SearchSpaceException;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchSpaceController Mapping 功能集成测试
 * 测试前后端 mapping 配置交互的完整流程
 */
@WebMvcTest(SearchSpaceController.class)
@ActiveProfiles("test")
class SearchSpaceControllerMappingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchSpaceService searchSpaceService;

    private SearchSpaceDTO testSearchSpace;
    private String validMappingJson;
    private String complexMappingJson;

    @BeforeEach
    void setUp() {
        testSearchSpace = new SearchSpaceDTO();
        testSearchSpace.setId(1L);
        testSearchSpace.setName("测试搜索空间");
        testSearchSpace.setCode("test_space");
        // testSearchSpace.setIndexName("test_index"); // 注释掉不存在的方法

        validMappingJson = """
                {
                  "properties": {
                    "title": {
                      "type": "text",
                      "analyzer": "ik_max_word",
                      "fields": {
                        "keyword": {
                          "type": "keyword",
                          "ignore_above": 256
                        }
                      }
                    },
                    "content": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    },
                    "createTime": {
                      "type": "date",
                      "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
                    },
                    "category": {
                      "type": "keyword"
                    }
                  }
                }
                """;

        complexMappingJson = """
                {
                  "properties": {
                    "title": {
                      "type": "text",
                      "analyzer": "ik_max_word",
                      "fields": {
                        "keyword": {
                          "type": "keyword",
                          "ignore_above": 256
                        },
                        "pinyin": {
                          "type": "text",
                          "analyzer": "pinyin"
                        }
                      }
                    },
                    "content": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    },
                    "tags": {
                      "type": "keyword"
                    },
                    "metadata": {
                      "type": "object",
                      "properties": {
                        "source": {
                          "type": "keyword"
                        },
                        "author": {
                          "type": "text",
                          "analyzer": "ik_max_word"
                        },
                        "publishDate": {
                          "type": "date"
                        }
                      }
                    },
                    "location": {
                      "type": "geo_point"
                    },
                    "score": {
                      "type": "float"
                    }
                  }
                }
                """;
    }

    @Test
    @DisplayName("完整 Mapping 配置流程测试 - 获取→更新→验证")
    void testCompleteMappingWorkflow() throws Exception {
        Long searchSpaceId = 1L;

        // 1. 首次获取空映射配置
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn("{}");

        MvcResult getResult1 = mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("{}"))
                .andReturn();

        verify(searchSpaceService, times(1)).getIndexMapping(searchSpaceId);

        // 2. 更新为有效的映射配置
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), anyString())).thenReturn(validMappingJson);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMappingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("映射配置更新成功"));

        verify(searchSpaceService).updateIndexMapping(searchSpaceId, validMappingJson);

        // 3. 再次获取映射配置验证更新成功
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(validMappingJson);

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data").value(validMappingJson));

        verify(searchSpaceService, times(2)).getIndexMapping(searchSpaceId);
    }

    @Test
    @DisplayName("复杂 Mapping 配置更新流程测试")
    void testComplexMappingUpdateWorkflow() throws Exception {
        Long searchSpaceId = 1L;

        // 1. 从简单配置开始
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(validMappingJson);

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(validMappingJson));

        // 2. 更新为复杂配置
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), anyString())).thenReturn(complexMappingJson);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(complexMappingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(searchSpaceService).updateIndexMapping(searchSpaceId, complexMappingJson);

        // 3. 验证复杂配置已生效
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(complexMappingJson);

        MvcResult result = mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("pinyin"), "复杂映射配置应包含拼音字段");
        assertTrue(responseBody.contains("geo_point"), "复杂映射配置应包含地理位置字段");
        assertTrue(responseBody.contains("metadata"), "复杂映射配置应包含元数据对象");
    }

    @Test
    @DisplayName("并发 Mapping 操作流程测试")
    void testConcurrentMappingOperations() throws Exception {
        Long searchSpaceId = 1L;
        String mapping1 = "{\"properties\": {\"field1\": {\"type\": \"text\"}}}";
        String mapping2 = "{\"properties\": {\"field2\": {\"type\": \"keyword\"}}}";

        // 模拟并发更新操作
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(mapping1))).thenReturn(mapping1);
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(mapping2))).thenReturn(mapping2);

        // 第一个更新请求
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapping1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 第二个更新请求
        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapping2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证两次更新都被调用
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, mapping1);
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, mapping2);
    }

    @Test
    @DisplayName("映射配置错误处理完整流程测试")
    void testMappingErrorHandlingWorkflow() throws Exception {
        Long searchSpaceId = 1L;
        String invalidMapping = "{\"invalid\": \"mapping\", \"syntax\": error}"; // 语法错误的JSON

        // 1. 尝试更新无效映射配置
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), anyString()))
                .thenThrow(new SearchSpaceException("映射配置验证失败：无效的JSON格式"));

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidMapping))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("映射配置验证失败：无效的JSON格式"));

        verify(searchSpaceService).updateIndexMapping(searchSpaceId, invalidMapping);

        // 2. 验证原始映射配置未被修改
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(validMappingJson);

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(validMappingJson));

        // 3. 修正后的映射配置应该成功更新
        String correctedMapping = "{\"properties\": {\"corrected\": {\"type\": \"text\"}}}";
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(correctedMapping)))
                .thenReturn(correctedMapping);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctedMapping))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(searchSpaceService).updateIndexMapping(searchSpaceId, correctedMapping);
    }

    @Test
    @DisplayName("不存在的搜索空间映射操作流程测试")
    void testNonExistentSearchSpaceMappingWorkflow() throws Exception {
        Long nonExistentId = 999L;

        // 1. 尝试获取不存在搜索空间的映射配置
        when(searchSpaceService.getIndexMapping(nonExistentId))
                .thenThrow(new SearchSpaceException("搜索空间不存在"));

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在"));

        // 2. 尝试更新不存在搜索空间的映射配置
        when(searchSpaceService.updateIndexMapping(eq(nonExistentId), anyString()))
                .thenThrow(new SearchSpaceException("搜索空间不存在"));

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMappingJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在"));

        verify(searchSpaceService).getIndexMapping(nonExistentId);
        verify(searchSpaceService).updateIndexMapping(nonExistentId, validMappingJson);
    }

    @Test
    @DisplayName("大型映射配置处理流程测试")
    void testLargeMappingConfigurationWorkflow() throws Exception {
        Long searchSpaceId = 1L;

        // 构建大型映射配置（包含大量字段）
        StringBuilder largeMappingBuilder = new StringBuilder("{\"properties\": {");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) largeMappingBuilder.append(",");
            largeMappingBuilder.append(String.format(
                    "\"field_%d\": {\"type\": \"text\", \"analyzer\": \"ik_max_word\"}", i));
        }
        largeMappingBuilder.append("}}");
        String largeMapping = largeMappingBuilder.toString();

        // 1. 更新大型映射配置
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(largeMapping)))
                .thenReturn(largeMapping);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(largeMapping))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(searchSpaceService).updateIndexMapping(searchSpaceId, largeMapping);

        // 2. 获取大型映射配置
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(largeMapping);

        MvcResult result = mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.length() > 10000, "大型映射配置响应体应该很大");
        assertTrue(responseBody.contains("field_0"), "应包含第一个字段");
        assertTrue(responseBody.contains("field_999"), "应包含最后一个字段");
    }

    @Test
    @DisplayName("特殊字符映射配置处理流程测试")
    void testSpecialCharacterMappingWorkflow() throws Exception {
        Long searchSpaceId = 1L;

        String specialCharMapping = """
                {
                  "properties": {
                    "title-中文": {
                      "type": "text",
                      "analyzer": "ik_max_word"
                    },
                    "field.with.dots": {
                      "type": "keyword"
                    },
                    "field_with_emoji_🚀": {
                      "type": "text"
                    },
                    "field@special#chars": {
                      "type": "keyword"
                    },
                    "数值字段": {
                      "type": "integer"
                    }
                  }
                }
                """;

        // 1. 更新包含特殊字符的映射配置
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(specialCharMapping)))
                .thenReturn(specialCharMapping);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialCharMapping))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. 验证特殊字符配置已保存
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(specialCharMapping);

        MvcResult result = mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("title-中文"), "应包含中文字段名");
        assertTrue(responseBody.contains("field.with.dots"), "应包含带点的字段名");
        assertTrue(responseBody.contains("field_with_emoji_🚀"), "应包含emoji字段名");
        assertTrue(responseBody.contains("field@special#chars"), "应包含特殊符号字段名");
        assertTrue(responseBody.contains("数值字段"), "应包含中文数值字段名");
    }

    @Test
    @DisplayName("映射配置版本控制流程测试")
    void testMappingVersionControlWorkflow() throws Exception {
        Long searchSpaceId = 1L;

        String version1 = "{\"properties\": {\"title\": {\"type\": \"text\"}}}";
        String version2 = "{\"properties\": {\"title\": {\"type\": \"text\"}, \"content\": {\"type\": \"text\"}}}";
        String version3 = "{\"properties\": {\"title\": {\"type\": \"text\"}, \"content\": {\"type\": \"text\"}, \"tags\": {\"type\": \"keyword\"}}}";

        // 版本 1
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(version1))).thenReturn(version1);
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(version1);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(version1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(version1));

        // 版本 2
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(version2))).thenReturn(version2);
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(version2);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(version2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(version2));

        // 版本 3
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(version3))).thenReturn(version3);
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(version3);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(version3))
                .andExpect(status().isOk());

        MvcResult finalResult = mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andReturn();

        String finalMapping = finalResult.getResponse().getContentAsString();
        assertTrue(finalMapping.contains("title"), "最终版本应包含title");
        assertTrue(finalMapping.contains("content"), "最终版本应包含content");
        assertTrue(finalMapping.contains("tags"), "最终版本应包含tags");

        // 验证所有版本的更新都被调用
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, version1);
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, version2);
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, version3);
    }

    @Test
    @DisplayName("空映射配置到完整配置的迁移流程测试")
    void testEmptyToFullMappingMigrationWorkflow() throws Exception {
        Long searchSpaceId = 1L;

        // 1. 初始状态：空映射
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn("");

        mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(""));

        // 2. 添加基础字段
        String basicMapping = "{\"properties\": {\"id\": {\"type\": \"long\"}}}";
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(basicMapping))).thenReturn(basicMapping);
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(basicMapping);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basicMapping))
                .andExpect(status().isOk());

        // 3. 扩展为完整配置
        when(searchSpaceService.updateIndexMapping(eq(searchSpaceId), eq(validMappingJson))).thenReturn(validMappingJson);
        when(searchSpaceService.getIndexMapping(searchSpaceId)).thenReturn(validMappingJson);

        mockMvc.perform(put("/api/search-spaces/{id}/mapping", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMappingJson))
                .andExpect(status().isOk());

        // 4. 验证最终完整配置
        MvcResult result = mockMvc.perform(get("/api/search-spaces/{id}/mapping", searchSpaceId))
                .andExpect(status().isOk())
                .andReturn();

        String finalMapping = result.getResponse().getContentAsString();
        assertTrue(finalMapping.contains("title"), "完整配置应包含title字段");
        assertTrue(finalMapping.contains("content"), "完整配置应包含content字段");
        assertTrue(finalMapping.contains("createTime"), "完整配置应包含createTime字段");
        assertTrue(finalMapping.contains("category"), "完整配置应包含category字段");

        // 验证迁移过程中的所有调用
        verify(searchSpaceService, times(3)).getIndexMapping(searchSpaceId);
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, basicMapping);
        verify(searchSpaceService).updateIndexMapping(searchSpaceId, validMappingJson);
    }
}