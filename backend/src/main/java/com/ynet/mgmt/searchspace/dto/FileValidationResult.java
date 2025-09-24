package com.ynet.mgmt.searchspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件验证结果DTO
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "文件验证结果")
public class FileValidationResult {

    @Schema(description = "验证是否通过", example = "true")
    private boolean valid;

    @Schema(description = "错误信息", example = "文件格式不正确")
    private String errorMessage;

    @Schema(description = "记录数量", example = "100")
    private Integer recordCount;

    @Schema(description = "文件大小（字节）", example = "1048576")
    private Long fileSize;

    public FileValidationResult() {
    }

    public FileValidationResult(boolean valid, String errorMessage, Integer recordCount, Long fileSize) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.recordCount = recordCount;
        this.fileSize = fileSize;
    }

    public static FileValidationResult success(Integer recordCount, Long fileSize) {
        return new FileValidationResult(true, null, recordCount, fileSize);
    }

    public static FileValidationResult failure(String errorMessage) {
        return new FileValidationResult(false, errorMessage, null, null);
    }

    public static FileValidationResult failure(String errorMessage, Long fileSize) {
        return new FileValidationResult(false, errorMessage, null, fileSize);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "FileValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                ", recordCount=" + recordCount +
                ", fileSize=" + fileSize +
                '}';
    }
}