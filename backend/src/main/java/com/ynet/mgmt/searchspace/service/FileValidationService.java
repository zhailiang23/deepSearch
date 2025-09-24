package com.ynet.mgmt.searchspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ynet.mgmt.searchspace.dto.FileValidationResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件验证服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface FileValidationService {

    /**
     * 验证JSON文件
     *
     * @param file 上传的文件
     * @return 验证结果
     */
    FileValidationResult validateJsonFile(MultipartFile file);

    /**
     * 解析JSON数组
     *
     * @param inputStream 输入流
     * @return JSON节点
     * @throws Exception 解析异常
     */
    JsonNode parseJsonArray(InputStream inputStream) throws Exception;

    /**
     * 统计记录数量
     *
     * @param jsonArray JSON数组节点
     * @return 记录数量
     */
    Integer countRecords(JsonNode jsonArray);

    /**
     * 验证文件大小
     *
     * @param size 文件大小
     * @param maxSize 最大允许大小
     * @return 验证是否通过
     */
    boolean validateFileSize(long size, long maxSize);

    /**
     * 验证文件扩展名
     *
     * @param fileName 文件名
     * @return 验证是否通过
     */
    boolean validateFileExtension(String fileName);

    /**
     * 验证MIME类型
     *
     * @param contentType 内容类型
     * @return 验证是否通过
     */
    boolean validateMimeType(String contentType);
}