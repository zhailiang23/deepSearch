package com.ynet.mgmt.searchspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 文件上传响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "文件上传响应")
public class FileUploadResponse {

    @Schema(description = "处理任务ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String taskId;

    @Schema(description = "原始文件名", example = "data.json")
    private String fileName;

    @Schema(description = "文件大小（字节）", example = "1048576")
    private Long fileSize;

    @Schema(description = "JSON记录数量", example = "100")
    private Integer recordCount;

    @Schema(description = "上传时间", example = "2024-01-15T10:30:00")
    private LocalDateTime uploadTime;

    @Schema(description = "处理消息", example = "文件上传成功，正在处理中")
    private String message;

    public FileUploadResponse() {
        this.uploadTime = LocalDateTime.now();
    }

    public FileUploadResponse(String taskId, String fileName, Long fileSize, Integer recordCount, String message) {
        this.taskId = taskId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.recordCount = recordCount;
        this.message = message;
        this.uploadTime = LocalDateTime.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "FileUploadResponse{" +
                "taskId='" + taskId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", recordCount=" + recordCount +
                ", uploadTime=" + uploadTime +
                ", message='" + message + '\'' +
                '}';
    }
}