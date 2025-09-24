package com.ynet.mgmt.searchspace.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchspace.dto.FileValidationResult;
import com.ynet.mgmt.searchspace.service.FileValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 文件验证服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class FileValidationServiceImpl implements FileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(FileValidationServiceImpl.class);

    @Value("${app.import.max-file-size:20971520}")
    private long maxFileSize;

    @Value("#{'${app.import.allowed-extensions:json}'.split(',')}")
    private List<String> allowedExtensions;

    private final ObjectMapper objectMapper;

    public FileValidationServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public FileValidationResult validateJsonFile(MultipartFile file) {
        logger.debug("开始验证JSON文件: fileName={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 检查文件是否为空
        if (file.isEmpty()) {
            logger.warn("文件为空");
            return FileValidationResult.failure("文件不能为空");
        }

        // 验证文件大小
        if (!validateFileSize(file.getSize(), maxFileSize)) {
            logger.warn("文件大小超限: size={}, maxSize={}", file.getSize(), maxFileSize);
            return FileValidationResult.failure("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB", file.getSize());
        }

        // 验证文件扩展名
        if (!validateFileExtension(file.getOriginalFilename())) {
            logger.warn("文件扩展名不符合要求: fileName={}", file.getOriginalFilename());
            return FileValidationResult.failure("只支持JSON文件格式", file.getSize());
        }

        // 验证MIME类型
        if (!validateMimeType(file.getContentType())) {
            logger.warn("MIME类型不符合要求: contentType={}", file.getContentType());
            return FileValidationResult.failure("文件格式必须为JSON", file.getSize());
        }

        try {
            // 解析JSON内容
            JsonNode jsonNode;
            try (InputStream inputStream = file.getInputStream()) {
                jsonNode = parseJsonArray(inputStream);
            }

            // 验证是否为数组
            if (!jsonNode.isArray()) {
                logger.warn("JSON根节点不是数组: fileName={}", file.getOriginalFilename());
                return FileValidationResult.failure("JSON文件的根节点必须是数组格式", file.getSize());
            }

            // 统计记录数量
            Integer recordCount = countRecords(jsonNode);
            if (recordCount == 0) {
                logger.warn("JSON数组为空: fileName={}", file.getOriginalFilename());
                return FileValidationResult.failure("JSON数组不能为空，至少包含1个对象", file.getSize());
            }

            logger.info("JSON文件验证成功: fileName={}, recordCount={}, fileSize={}",
                    file.getOriginalFilename(), recordCount, file.getSize());

            return FileValidationResult.success(recordCount, file.getSize());

        } catch (IOException e) {
            logger.error("文件读取失败: fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            return FileValidationResult.failure("文件读取失败: " + e.getMessage(), file.getSize());
        } catch (Exception e) {
            logger.error("JSON解析失败: fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            return FileValidationResult.failure("JSON格式不正确: " + e.getMessage(), file.getSize());
        }
    }

    @Override
    public JsonNode parseJsonArray(InputStream inputStream) throws Exception {
        return objectMapper.readTree(inputStream);
    }

    @Override
    public Integer countRecords(JsonNode jsonArray) {
        if (jsonArray == null || !jsonArray.isArray()) {
            return 0;
        }
        return jsonArray.size();
    }

    @Override
    public boolean validateFileSize(long size, long maxSize) {
        return size > 0 && size <= maxSize;
    }

    @Override
    public boolean validateFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }

        String extension = getFileExtension(fileName);
        return StringUtils.hasText(extension) && allowedExtensions.contains(extension.toLowerCase());
    }

    @Override
    public boolean validateMimeType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }

        List<String> validMimeTypes = Arrays.asList(
                "application/json",
                "text/json",
                "text/plain"  // 有些浏览器可能发送text/plain
        );

        return validMimeTypes.stream().anyMatch(contentType::startsWith);
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（不包含点）
     */
    private String getFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1);
    }
}