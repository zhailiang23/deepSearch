package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.service.FileStorageService;
import com.ynet.mgmt.searchspace.service.FileValidationService;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SearchSpaceController文件导入功能集成测试
 *
 * @author system
 * @since 1.0.0
 */
@WebMvcTest(SearchSpaceController.class)
@DisplayName("搜索空间控制器文件导入测试")
class SearchSpaceControllerImportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchSpaceService searchSpaceService;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("JSON文件导入 - 成功")
    void testImportJsonFile_Success() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 1L;
        String jsonContent = "[{\"id\":1,\"name\":\"test1\"},{\"id\":2,\"name\":\"test2\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                MediaType.APPLICATION_JSON_VALUE,
                jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(searchSpaceId);
        searchSpace.setName("Test Search Space");

        com.ynet.mgmt.searchspace.dto.FileValidationResult validationResult =
                com.ynet.mgmt.searchspace.dto.FileValidationResult.success(2, (long) jsonContent.length());

        // 配置模拟对象
        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(searchSpace);
        when(fileValidationService.validateJsonFile(any())).thenReturn(validationResult);
        when(fileStorageService.saveTemporaryFile(any(), eq(searchSpaceId)))
                .thenReturn(Paths.get("/tmp/test/1_20240101_120000_000_test.json"));

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("文件上传成功"))
                .andExpect(jsonPath("$.data.fileName").value("test.json"))
                .andExpect(jsonPath("$.data.fileSize").value(jsonContent.length()))
                .andExpect(jsonPath("$.data.recordCount").value(2))
                .andExpect(jsonPath("$.data.taskId").exists())
                .andExpect(jsonPath("$.data.uploadTime").exists())
                .andExpect(jsonPath("$.data.message").value("文件上传成功，系统正在处理中"));

        // 验证服务调用
        verify(searchSpaceService).getSearchSpace(searchSpaceId);
        verify(fileValidationService).validateJsonFile(any());
        verify(fileStorageService).saveTemporaryFile(any(), eq(searchSpaceId));
    }

    @Test
    @DisplayName("JSON文件导入 - 搜索空间不存在")
    void testImportJsonFile_SearchSpaceNotFound() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 999L;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                MediaType.APPLICATION_JSON_VALUE,
                "[{\"id\":1}]".getBytes(StandardCharsets.UTF_8)
        );

        // 配置模拟对象
        when(searchSpaceService.getSearchSpace(searchSpaceId))
                .thenThrow(new RuntimeException("搜索空间不存在"));

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在"));

        // 验证服务调用
        verify(searchSpaceService).getSearchSpace(searchSpaceId);
        verify(fileValidationService, never()).validateJsonFile(any());
        verify(fileStorageService, never()).saveTemporaryFile(any(), any());
    }

    @Test
    @DisplayName("JSON文件导入 - 文件验证失败")
    void testImportJsonFile_ValidationFailed() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "[{\"id\":1}]".getBytes(StandardCharsets.UTF_8)
        );

        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(searchSpaceId);
        searchSpace.setName("Test Search Space");

        com.ynet.mgmt.searchspace.dto.FileValidationResult validationResult =
                com.ynet.mgmt.searchspace.dto.FileValidationResult.failure("只支持JSON文件格式");

        // 配置模拟对象
        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(searchSpace);
        when(fileValidationService.validateJsonFile(any())).thenReturn(validationResult);

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("只支持JSON文件格式"));

        // 验证服务调用
        verify(searchSpaceService).getSearchSpace(searchSpaceId);
        verify(fileValidationService).validateJsonFile(any());
        verify(fileStorageService, never()).saveTemporaryFile(any(), any());
    }

    @Test
    @DisplayName("JSON文件导入 - 文件存储失败")
    void testImportJsonFile_StorageFailed() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 1L;
        String jsonContent = "[{\"id\":1,\"name\":\"test\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                MediaType.APPLICATION_JSON_VALUE,
                jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(searchSpaceId);
        searchSpace.setName("Test Search Space");

        com.ynet.mgmt.searchspace.dto.FileValidationResult validationResult =
                com.ynet.mgmt.searchspace.dto.FileValidationResult.success(1, (long) jsonContent.length());

        // 配置模拟对象
        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(searchSpace);
        when(fileValidationService.validateJsonFile(any())).thenReturn(validationResult);
        when(fileStorageService.saveTemporaryFile(any(), eq(searchSpaceId)))
                .thenThrow(new IOException("磁盘空间不足"));

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("系统异常，请稍后重试"));

        // 验证服务调用
        verify(searchSpaceService).getSearchSpace(searchSpaceId);
        verify(fileValidationService).validateJsonFile(any());
        verify(fileStorageService).saveTemporaryFile(any(), eq(searchSpaceId));
    }

    @Test
    @DisplayName("JSON文件导入 - 缺少文件参数")
    void testImportJsonFile_MissingFile() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 1L;

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 验证没有调用服务
        verify(searchSpaceService, never()).getSearchSpace(any());
        verify(fileValidationService, never()).validateJsonFile(any());
        verify(fileStorageService, never()).saveTemporaryFile(any(), any());
    }

    @Test
    @DisplayName("JSON文件导入 - 无效的搜索空间ID")
    void testImportJsonFile_InvalidSearchSpaceId() throws Exception {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                MediaType.APPLICATION_JSON_VALUE,
                "[{\"id\":1}]".getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", "invalid")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 验证没有调用服务
        verify(searchSpaceService, never()).getSearchSpace(any());
        verify(fileValidationService, never()).validateJsonFile(any());
        verify(fileStorageService, never()).saveTemporaryFile(any(), any());
    }

    @Test
    @DisplayName("文件大小超限异常处理")
    void testMaxUploadSizeExceededException() throws Exception {
        // 这个测试需要在实际的Spring Boot环境中运行，因为文件大小限制是在框架级别处理的
        // 这里主要测试异常处理器的逻辑

        Long searchSpaceId = 1L;

        // 模拟MaxUploadSizeExceededException
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(20 * 1024 * 1024);

        SearchSpaceExceptionHandler exceptionHandler = new SearchSpaceExceptionHandler();
        var response = exceptionHandler.handleMaxUploadSizeExceededException(exception);

        assertEquals(413, response.getStatusCodeValue());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("文件大小不能超过"));
    }

    @Test
    @DisplayName("测试有效的JSON数组内容")
    void testImportJsonFile_ValidJsonArray() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 1L;
        String complexJsonContent = """
                [
                    {
                        "id": 1,
                        "title": "测试文档1",
                        "content": "这是第一个测试文档的内容",
                        "tags": ["测试", "文档"],
                        "metadata": {
                            "author": "张三",
                            "date": "2024-01-01"
                        }
                    },
                    {
                        "id": 2,
                        "title": "测试文档2",
                        "content": "这是第二个测试文档的内容",
                        "tags": ["测试", "样例"],
                        "metadata": {
                            "author": "李四",
                            "date": "2024-01-02"
                        }
                    }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "complex-data.json",
                MediaType.APPLICATION_JSON_VALUE,
                complexJsonContent.getBytes(StandardCharsets.UTF_8)
        );

        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(searchSpaceId);
        searchSpace.setName("Test Search Space");

        com.ynet.mgmt.searchspace.dto.FileValidationResult validationResult =
                com.ynet.mgmt.searchspace.dto.FileValidationResult.success(2, (long) complexJsonContent.length());

        // 配置模拟对象
        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(searchSpace);
        when(fileValidationService.validateJsonFile(any())).thenReturn(validationResult);
        when(fileStorageService.saveTemporaryFile(any(), eq(searchSpaceId)))
                .thenReturn(Paths.get("/tmp/test/1_20240101_120000_000_complex-data.json"));

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("complex-data.json"))
                .andExpect(jsonPath("$.data.recordCount").value(2))
                .andExpect(jsonPath("$.data.taskId").exists());
    }

    @Test
    @DisplayName("测试空JSON数组验证失败")
    void testImportJsonFile_EmptyJsonArray() throws Exception {
        // 准备测试数据
        Long searchSpaceId = 1L;
        String emptyJsonArray = "[]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.json",
                MediaType.APPLICATION_JSON_VALUE,
                emptyJsonArray.getBytes(StandardCharsets.UTF_8)
        );

        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(searchSpaceId);
        searchSpace.setName("Test Search Space");

        com.ynet.mgmt.searchspace.dto.FileValidationResult validationResult =
                com.ynet.mgmt.searchspace.dto.FileValidationResult.failure("JSON数组不能为空，至少包含1个对象");

        // 配置模拟对象
        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(searchSpace);
        when(fileValidationService.validateJsonFile(any())).thenReturn(validationResult);

        // 执行测试
        mockMvc.perform(multipart("/search-spaces/{id}/import", searchSpaceId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("JSON数组不能为空，至少包含1个对象"));
    }
}