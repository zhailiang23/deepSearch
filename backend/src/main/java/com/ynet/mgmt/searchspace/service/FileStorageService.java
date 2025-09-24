package com.ynet.mgmt.searchspace.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 文件存储服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface FileStorageService {

    /**
     * 保存临时文件
     *
     * @param file 上传的文件
     * @param searchSpaceId 搜索空间ID
     * @return 保存的文件路径
     * @throws IOException 文件操作异常
     */
    Path saveTemporaryFile(MultipartFile file, Long searchSpaceId) throws IOException;

    /**
     * 生成文件名
     *
     * @param searchSpaceId 搜索空间ID
     * @param originalName 原始文件名
     * @return 生成的文件名
     */
    String generateFileName(Long searchSpaceId, String originalName);

    /**
     * 清理过期文件
     *
     * @return 清理的文件数量
     */
    int cleanupExpiredFiles();

    /**
     * 获取临时文件路径
     *
     * @param fileName 文件名
     * @return 文件路径
     */
    Path getTemporaryFilePath(String fileName);

    /**
     * 根据任务ID和搜索空间ID获取临时文件路径
     *
     * @param taskId 任务ID
     * @param searchSpaceId 搜索空间ID
     * @return 文件路径
     */
    Path getTemporaryFilePath(String taskId, Long searchSpaceId);

    /**
     * 删除临时文件
     *
     * @param filePath 文件路径
     * @return 删除是否成功
     */
    boolean deleteTemporaryFile(Path filePath);

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    boolean exists(Path filePath);

    /**
     * 初始化临时目录
     */
    void initializeTemporaryDirectory();
}