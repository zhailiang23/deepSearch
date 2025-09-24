package com.ynet.mgmt.searchspace.service;

import com.ynet.mgmt.searchspace.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileStorageService测试类
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文件存储服务测试")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl();

        // 设置临时目录
        ReflectionTestUtils.setField(fileStorageService, "tempDirectory", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "fileExpiryHours", 24);
        ReflectionTestUtils.setField(fileStorageService, "tempDirPath", tempDir);

        // 初始化临时目录
        fileStorageService.initializeTemporaryDirectory();
    }

    @Test
    @DisplayName("保存临时文件 - 正常情况")
    void testSaveTemporaryFile_Success() throws IOException {
        // 准备测试数据
        String content = "[{\"id\":1,\"name\":\"test\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                content.getBytes(StandardCharsets.UTF_8)
        );
        Long searchSpaceId = 1L;

        // 执行测试
        Path savedPath = fileStorageService.saveTemporaryFile(file, searchSpaceId);

        // 验证结果
        assertNotNull(savedPath, "保存路径不应为空");
        assertTrue(Files.exists(savedPath), "文件应该存在");
        assertEquals(content, Files.readString(savedPath, StandardCharsets.UTF_8), "文件内容应该匹配");
        assertTrue(savedPath.getFileName().toString().startsWith("1_"), "文件名应该以搜索空间ID开头");
        assertTrue(savedPath.getFileName().toString().endsWith("_test.json"), "文件名应该以原始文件名结尾");
    }

    @Test
    @DisplayName("保存临时文件 - 空文件")
    void testSaveTemporaryFile_EmptyFile() {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", new byte[0]);
        Long searchSpaceId = 1L;

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.saveTemporaryFile(file, searchSpaceId);
        });

        assertEquals("文件不能为空", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("保存临时文件 - null文件")
    void testSaveTemporaryFile_NullFile() {
        // 准备测试数据
        Long searchSpaceId = 1L;

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.saveTemporaryFile(null, searchSpaceId);
        });

        assertEquals("文件不能为空", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("保存临时文件 - null搜索空间ID")
    void testSaveTemporaryFile_NullSearchSpaceId() {
        // 准备测试数据
        String content = "[{\"id\":1,\"name\":\"test\"}]";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.saveTemporaryFile(file, null);
        });

        assertEquals("搜索空间ID不能为空", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("生成文件名 - 正常情况")
    void testGenerateFileName_Success() {
        // 准备测试数据
        Long searchSpaceId = 123L;
        String originalName = "test-data.json";

        // 执行测试
        String fileName = fileStorageService.generateFileName(searchSpaceId, originalName);

        // 验证结果
        assertNotNull(fileName, "文件名不应为空");
        assertTrue(fileName.startsWith("123_"), "文件名应该以搜索空间ID开头");
        assertTrue(fileName.endsWith("_test-data.json"), "文件名应该以原始文件名结尾");
        assertTrue(fileName.matches("\\d+_\\d{8}_\\d{6}_\\d{3}_.*"), "文件名应该包含时间戳");
    }

    @Test
    @DisplayName("生成文件名 - 不安全字符处理")
    void testGenerateFileName_UnsafeCharacters() {
        // 准备测试数据
        Long searchSpaceId = 123L;
        String originalName = "test/file\\name:with*unsafe?chars\"<>|.json";

        // 执行测试
        String fileName = fileStorageService.generateFileName(searchSpaceId, originalName);

        // 验证结果
        assertNotNull(fileName, "文件名不应为空");
        assertTrue(fileName.startsWith("123_"), "文件名应该以搜索空间ID开头");
        assertFalse(fileName.contains("/"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains("\\"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains(":"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains("*"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains("?"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains("\""), "文件名不应包含不安全字符");
        assertFalse(fileName.contains("<"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains(">"), "文件名不应包含不安全字符");
        assertFalse(fileName.contains("|"), "文件名不应包含不安全字符");
        assertTrue(fileName.endsWith(".json"), "文件名应该保留扩展名");
    }

    @Test
    @DisplayName("生成文件名 - 空原始文件名")
    void testGenerateFileName_EmptyOriginalName() {
        // 准备测试数据
        Long searchSpaceId = 123L;
        String originalName = "";

        // 执行测试
        String fileName = fileStorageService.generateFileName(searchSpaceId, originalName);

        // 验证结果
        assertNotNull(fileName, "文件名不应为空");
        assertTrue(fileName.startsWith("123_"), "文件名应该以搜索空间ID开头");
        assertTrue(fileName.endsWith("_unnamed.json"), "文件名应该使用默认名称");
    }

    @Test
    @DisplayName("生成文件名 - null搜索空间ID")
    void testGenerateFileName_NullSearchSpaceId() {
        // 准备测试数据
        String originalName = "test.json";

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.generateFileName(null, originalName);
        });

        assertEquals("搜索空间ID不能为空", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("清理过期文件")
    void testCleanupExpiredFiles() throws IOException {
        // 准备测试数据 - 创建一些过期和未过期的文件
        Path expiredFile = tempDir.resolve("expired.json");
        Path recentFile = tempDir.resolve("recent.json");

        Files.write(expiredFile, "expired content".getBytes(StandardCharsets.UTF_8));
        Files.write(recentFile, "recent content".getBytes(StandardCharsets.UTF_8));

        // 设置过期文件的修改时间为25小时前
        Instant expiredTime = Instant.now().minus(25, ChronoUnit.HOURS);
        Files.setLastModifiedTime(expiredFile, FileTime.from(expiredTime));

        // 执行测试
        int cleanedCount = fileStorageService.cleanupExpiredFiles();

        // 验证结果
        assertEquals(1, cleanedCount, "应该清理1个过期文件");
        assertFalse(Files.exists(expiredFile), "过期文件应该被删除");
        assertTrue(Files.exists(recentFile), "最近的文件应该保留");
    }

    @Test
    @DisplayName("获取临时文件路径")
    void testGetTemporaryFilePath() {
        // 准备测试数据
        String fileName = "test.json";

        // 执行测试
        Path filePath = fileStorageService.getTemporaryFilePath(fileName);

        // 验证结果
        assertNotNull(filePath, "文件路径不应为空");
        assertEquals(tempDir.resolve(fileName), filePath, "文件路径应该匹配");
    }

    @Test
    @DisplayName("获取临时文件路径 - 空文件名")
    void testGetTemporaryFilePath_EmptyFileName() {
        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.getTemporaryFilePath("");
        });

        assertEquals("文件名不能为空", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("删除临时文件 - 成功")
    void testDeleteTemporaryFile_Success() throws IOException {
        // 准备测试数据
        Path testFile = tempDir.resolve("test.json");
        Files.write(testFile, "test content".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(testFile), "文件应该存在");

        // 执行测试
        boolean result = fileStorageService.deleteTemporaryFile(testFile);

        // 验证结果
        assertTrue(result, "删除操作应该成功");
        assertFalse(Files.exists(testFile), "文件应该被删除");
    }

    @Test
    @DisplayName("删除临时文件 - 文件不存在")
    void testDeleteTemporaryFile_FileNotExists() {
        // 准备测试数据
        Path nonExistentFile = tempDir.resolve("nonexistent.json");

        // 执行测试
        boolean result = fileStorageService.deleteTemporaryFile(nonExistentFile);

        // 验证结果
        assertFalse(result, "删除不存在的文件应该返回false");
    }

    @Test
    @DisplayName("删除临时文件 - null路径")
    void testDeleteTemporaryFile_NullPath() {
        // 执行测试
        boolean result = fileStorageService.deleteTemporaryFile(null);

        // 验证结果
        assertFalse(result, "删除null路径应该返回false");
    }

    @Test
    @DisplayName("检查文件是否存在 - 存在")
    void testExists_FileExists() throws IOException {
        // 准备测试数据
        Path testFile = tempDir.resolve("test.json");
        Files.write(testFile, "test content".getBytes(StandardCharsets.UTF_8));

        // 执行测试
        boolean result = fileStorageService.exists(testFile);

        // 验证结果
        assertTrue(result, "存在的文件检查应该返回true");
    }

    @Test
    @DisplayName("检查文件是否存在 - 不存在")
    void testExists_FileNotExists() {
        // 准备测试数据
        Path nonExistentFile = tempDir.resolve("nonexistent.json");

        // 执行测试
        boolean result = fileStorageService.exists(nonExistentFile);

        // 验证结果
        assertFalse(result, "不存在的文件检查应该返回false");
    }

    @Test
    @DisplayName("检查文件是否存在 - null路径")
    void testExists_NullPath() {
        // 执行测试
        boolean result = fileStorageService.exists(null);

        // 验证结果
        assertFalse(result, "null路径检查应该返回false");
    }

    @Test
    @DisplayName("初始化临时目录")
    void testInitializeTemporaryDirectory() {
        // 验证临时目录已经在setUp中初始化
        assertTrue(Files.exists(tempDir), "临时目录应该存在");
        assertTrue(Files.isDirectory(tempDir), "路径应该是目录");
    }
}