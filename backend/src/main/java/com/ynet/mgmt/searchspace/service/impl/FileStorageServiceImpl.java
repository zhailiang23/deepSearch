package com.ynet.mgmt.searchspace.service.impl;

import com.ynet.mgmt.searchspace.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * 文件存储服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Value("${app.import.temp-dir:${java.io.tmpdir}/deepsearch/imports}")
    private String tempDirectory;

    @Value("${app.import.file-expiry-hours:24}")
    private int fileExpiryHours;

    private Path tempDirPath;

    @PostConstruct
    public void init() {
        initializeTemporaryDirectory();
    }

    @Override
    public void initializeTemporaryDirectory() {
        try {
            tempDirPath = Paths.get(tempDirectory);
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
                logger.info("创建临时目录: {}", tempDirPath.toAbsolutePath());
            }
            logger.debug("临时目录已就绪: {}", tempDirPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("无法创建临时目录: {}, error={}", tempDirectory, e.getMessage(), e);
            throw new RuntimeException("临时目录初始化失败", e);
        }
    }

    @Override
    public Path saveTemporaryFile(MultipartFile file, Long searchSpaceId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (searchSpaceId == null) {
            throw new IllegalArgumentException("搜索空间ID不能为空");
        }

        String fileName = generateFileName(searchSpaceId, file.getOriginalFilename());
        Path filePath = tempDirPath.resolve(fileName);

        try {
            // 确保父目录存在
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 保存文件
            file.transferTo(filePath.toFile());

            logger.info("文件保存成功: originalName={}, savedPath={}, size={}",
                    file.getOriginalFilename(), filePath.toAbsolutePath(), file.getSize());

            return filePath;

        } catch (IOException e) {
            logger.error("文件保存失败: originalName={}, targetPath={}, error={}",
                    file.getOriginalFilename(), filePath.toAbsolutePath(), e.getMessage(), e);
            throw new IOException("文件保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateFileName(Long searchSpaceId, String originalName) {
        if (searchSpaceId == null) {
            throw new IllegalArgumentException("搜索空间ID不能为空");
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String safeName = getSafeFileName(originalName);

        return String.format("%d_%s_%s", searchSpaceId, timestamp, safeName);
    }

    @Override
    public int cleanupExpiredFiles() {
        if (!Files.exists(tempDirPath)) {
            logger.debug("临时目录不存在，跳过清理: {}", tempDirPath.toAbsolutePath());
            return 0;
        }

        int cleanedCount = 0;
        Instant cutoffTime = Instant.now().minusSeconds(fileExpiryHours * 3600L);

        try (Stream<Path> files = Files.walk(tempDirPath)) {
            for (Path file : files.filter(Files::isRegularFile).toArray(Path[]::new)) {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(file);
                    if (lastModified.toInstant().isBefore(cutoffTime)) {
                        boolean deleted = deleteTemporaryFile(file);
                        if (deleted) {
                            cleanedCount++;
                            logger.debug("清理过期文件: {}", file.toAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    logger.warn("检查文件修改时间失败: {}, error={}", file.toAbsolutePath(), e.getMessage());
                }
            }

            if (cleanedCount > 0) {
                logger.info("清理过期文件完成: count={}, expiryHours={}", cleanedCount, fileExpiryHours);
            }

        } catch (IOException e) {
            logger.error("清理过期文件失败: tempDir={}, error={}", tempDirPath.toAbsolutePath(), e.getMessage(), e);
        }

        return cleanedCount;
    }

    @Override
    public Path getTemporaryFilePath(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return tempDirPath.resolve(fileName);
    }

    @Override
    public boolean deleteTemporaryFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return false;
        }

        try {
            Files.delete(filePath);
            logger.debug("文件删除成功: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.warn("文件删除失败: {}, error={}", filePath.toAbsolutePath(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Path filePath) {
        return filePath != null && Files.exists(filePath);
    }

    @Override
    public Path getTemporaryFilePath(String taskId, Long searchSpaceId) {
        if (!StringUtils.hasText(taskId) || searchSpaceId == null) {
            throw new IllegalArgumentException("任务ID和搜索空间ID不能为空");
        }

        // 根据taskId查找对应的文件
        // 文件名格式: {searchSpaceId}_{timestamp}_{originalName}
        // 我们需要找到以searchSpaceId开头的文件
        try (Stream<Path> files = Files.list(tempDirPath)) {
            String prefix = searchSpaceId + "_";
            return files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith(prefix);
                    })
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("未找到任务对应的临时文件: taskId=" + taskId + ", searchSpaceId=" + searchSpaceId));
        } catch (IOException e) {
            logger.error("查找临时文件失败: taskId={}, searchSpaceId={}, error={}", taskId, searchSpaceId, e.getMessage(), e);
            throw new RuntimeException("查找临时文件失败", e);
        }
    }

    /**
     * 获取安全的文件名，移除不安全字符
     *
     * @param originalName 原始文件名
     * @return 安全的文件名
     */
    private String getSafeFileName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "unnamed.json";
        }

        // 移除路径分隔符和其他不安全字符
        String safeName = originalName.replaceAll("[/\\\\:*?\"<>|]", "_");

        // 确保文件名不超过255个字符（大多数文件系统的限制）
        if (safeName.length() > 255) {
            String extension = "";
            int lastDotIndex = safeName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = safeName.substring(lastDotIndex);
                safeName = safeName.substring(0, lastDotIndex);
            }

            safeName = safeName.substring(0, Math.min(safeName.length(), 255 - extension.length())) + extension;
        }

        // 如果文件名为空或只包含扩展名，使用默认名称
        if (safeName.trim().isEmpty() || safeName.startsWith(".")) {
            safeName = "upload_" + safeName;
        }

        return safeName;
    }
}