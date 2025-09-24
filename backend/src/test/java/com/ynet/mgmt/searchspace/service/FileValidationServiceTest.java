package com.ynet.mgmt.searchspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchspace.dto.FileValidationResult;
import com.ynet.mgmt.searchspace.service.impl.FileValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileValidationService测试类
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文件验证服务测试")
class FileValidationServiceTest {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationServiceImpl();

        // 设置配置值
        ReflectionTestUtils.setField(fileValidationService, "maxFileSize", 20971520L); // 20MB
        ReflectionTestUtils.setField(fileValidationService, "allowedExtensions", Arrays.asList("json"));
    }

    @Test
    @DisplayName("验证有效的JSON文件")
    void testValidateJsonFile_ValidFile() {
        // 准备测试数据
        String jsonContent = "[{\"id\":1,\"name\":\"test1\"},{\"id\":2,\"name\":\"test2\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertTrue(result.isValid(), "文件验证应该通过");
        assertNull(result.getErrorMessage(), "错误消息应该为空");
        assertEquals(2, result.getRecordCount(), "记录数量应该为2");
        assertEquals(jsonContent.length(), result.getFileSize(), "文件大小应该匹配");
    }

    @Test
    @DisplayName("验证空文件")
    void testValidateJsonFile_EmptyFile() {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", new byte[0]);

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "空文件验证应该失败");
        assertEquals("文件不能为空", result.getErrorMessage(), "错误消息应该匹配");
    }

    @Test
    @DisplayName("验证文件大小超限")
    void testValidateJsonFile_FileTooLarge() {
        // 准备测试数据 - 创建一个超过20MB的文件
        byte[] largeContent = new byte[25 * 1024 * 1024]; // 25MB
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", largeContent);

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "大文件验证应该失败");
        assertTrue(result.getErrorMessage().contains("文件大小不能超过"), "错误消息应该包含大小限制信息");
        assertEquals(largeContent.length, result.getFileSize(), "文件大小应该匹配");
    }

    @Test
    @DisplayName("验证非JSON扩展名")
    void testValidateJsonFile_InvalidExtension() {
        // 准备测试数据
        String content = "[{\"id\":1,\"name\":\"test\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "application/json",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "非JSON扩展名验证应该失败");
        assertEquals("只支持JSON文件格式", result.getErrorMessage(), "错误消息应该匹配");
    }

    @Test
    @DisplayName("验证无效的MIME类型")
    void testValidateJsonFile_InvalidMimeType() {
        // 准备测试数据
        String content = "[{\"id\":1,\"name\":\"test\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "image/png",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "无效MIME类型验证应该失败");
        assertEquals("文件格式必须为JSON", result.getErrorMessage(), "错误消息应该匹配");
    }

    @Test
    @DisplayName("验证无效的JSON格式")
    void testValidateJsonFile_InvalidJson() {
        // 准备测试数据 - 无效的JSON
        String invalidJson = "{invalid json content";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                invalidJson.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "无效JSON验证应该失败");
        assertTrue(result.getErrorMessage().contains("JSON格式不正确"), "错误消息应该包含JSON格式错误信息");
    }

    @Test
    @DisplayName("验证非数组JSON")
    void testValidateJsonFile_NotArrayJson() {
        // 准备测试数据 - 对象而非数组
        String objectJson = "{\"id\":1,\"name\":\"test\"}";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                objectJson.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "非数组JSON验证应该失败");
        assertEquals("JSON文件的根节点必须是数组格式", result.getErrorMessage(), "错误消息应该匹配");
    }

    @Test
    @DisplayName("验证空数组JSON")
    void testValidateJsonFile_EmptyArrayJson() {
        // 准备测试数据 - 空数组
        String emptyArray = "[]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                emptyArray.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试
        FileValidationResult result = fileValidationService.validateJsonFile(file);

        // 验证结果
        assertFalse(result.isValid(), "空数组验证应该失败");
        assertEquals("JSON数组不能为空，至少包含1个对象", result.getErrorMessage(), "错误消息应该匹配");
    }

    @Test
    @DisplayName("解析JSON数组")
    void testParseJsonArray() throws Exception {
        // 准备测试数据
        String jsonContent = "[{\"id\":1,\"name\":\"test1\"},{\"id\":2,\"name\":\"test2\"}]";
        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));

        // 执行测试
        JsonNode result = fileValidationService.parseJsonArray(inputStream);

        // 验证结果
        assertNotNull(result, "解析结果不应为空");
        assertTrue(result.isArray(), "解析结果应该是数组");
        assertEquals(2, result.size(), "数组大小应该为2");
    }

    @Test
    @DisplayName("统计记录数量")
    void testCountRecords() throws Exception {
        // 准备测试数据
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode arrayNode = objectMapper.readTree("[{\"id\":1},{\"id\":2},{\"id\":3}]");

        // 执行测试
        Integer count = fileValidationService.countRecords(arrayNode);

        // 验证结果
        assertEquals(3, count, "记录数量应该为3");
    }

    @Test
    @DisplayName("统计非数组节点的记录数量")
    void testCountRecords_NotArray() throws Exception {
        // 准备测试数据
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode objectNode = objectMapper.readTree("{\"id\":1}");

        // 执行测试
        Integer count = fileValidationService.countRecords(objectNode);

        // 验证结果
        assertEquals(0, count, "非数组节点记录数量应该为0");
    }

    @Test
    @DisplayName("验证文件大小 - 正常情况")
    void testValidateFileSize_Valid() {
        // 执行测试
        boolean result = fileValidationService.validateFileSize(1024L, 2048L);

        // 验证结果
        assertTrue(result, "正常文件大小验证应该通过");
    }

    @Test
    @DisplayName("验证文件大小 - 超限情况")
    void testValidateFileSize_TooLarge() {
        // 执行测试
        boolean result = fileValidationService.validateFileSize(2048L, 1024L);

        // 验证结果
        assertFalse(result, "超限文件大小验证应该失败");
    }

    @Test
    @DisplayName("验证文件大小 - 零大小")
    void testValidateFileSize_ZeroSize() {
        // 执行测试
        boolean result = fileValidationService.validateFileSize(0L, 1024L);

        // 验证结果
        assertFalse(result, "零大小文件验证应该失败");
    }

    @Test
    @DisplayName("验证文件扩展名 - 有效扩展名")
    void testValidateFileExtension_Valid() {
        // 执行测试
        boolean result = fileValidationService.validateFileExtension("test.json");

        // 验证结果
        assertTrue(result, "有效扩展名验证应该通过");
    }

    @Test
    @DisplayName("验证文件扩展名 - 大小写不敏感")
    void testValidateFileExtension_CaseInsensitive() {
        // 执行测试
        boolean result = fileValidationService.validateFileExtension("test.JSON");

        // 验证结果
        assertTrue(result, "大小写不敏感验证应该通过");
    }

    @Test
    @DisplayName("验证文件扩展名 - 无效扩展名")
    void testValidateFileExtension_Invalid() {
        // 执行测试
        boolean result = fileValidationService.validateFileExtension("test.txt");

        // 验证结果
        assertFalse(result, "无效扩展名验证应该失败");
    }

    @Test
    @DisplayName("验证文件扩展名 - 空文件名")
    void testValidateFileExtension_EmptyFileName() {
        // 执行测试
        boolean result = fileValidationService.validateFileExtension("");

        // 验证结果
        assertFalse(result, "空文件名验证应该失败");
    }

    @Test
    @DisplayName("验证文件扩展名 - null文件名")
    void testValidateFileExtension_NullFileName() {
        // 执行测试
        boolean result = fileValidationService.validateFileExtension(null);

        // 验证结果
        assertFalse(result, "null文件名验证应该失败");
    }

    @Test
    @DisplayName("验证MIME类型 - application/json")
    void testValidateMimeType_ApplicationJson() {
        // 执行测试
        boolean result = fileValidationService.validateMimeType("application/json");

        // 验证结果
        assertTrue(result, "application/json验证应该通过");
    }

    @Test
    @DisplayName("验证MIME类型 - text/json")
    void testValidateMimeType_TextJson() {
        // 执行测试
        boolean result = fileValidationService.validateMimeType("text/json");

        // 验证结果
        assertTrue(result, "text/json验证应该通过");
    }

    @Test
    @DisplayName("验证MIME类型 - text/plain")
    void testValidateMimeType_TextPlain() {
        // 执行测试
        boolean result = fileValidationService.validateMimeType("text/plain");

        // 验证结果
        assertTrue(result, "text/plain验证应该通过");
    }

    @Test
    @DisplayName("验证MIME类型 - 无效类型")
    void testValidateMimeType_Invalid() {
        // 执行测试
        boolean result = fileValidationService.validateMimeType("image/png");

        // 验证结果
        assertFalse(result, "image/png验证应该失败");
    }

    @Test
    @DisplayName("验证MIME类型 - 空类型")
    void testValidateMimeType_Empty() {
        // 执行测试
        boolean result = fileValidationService.validateMimeType("");

        // 验证结果
        assertFalse(result, "空MIME类型验证应该失败");
    }

    @Test
    @DisplayName("验证MIME类型 - null类型")
    void testValidateMimeType_Null() {
        // 执行测试
        boolean result = fileValidationService.validateMimeType(null);

        // 验证结果
        assertFalse(result, "null MIME类型验证应该失败");
    }
}