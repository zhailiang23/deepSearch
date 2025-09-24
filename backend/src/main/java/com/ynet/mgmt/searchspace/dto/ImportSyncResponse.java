package com.ynet.mgmt.searchspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步导入响应DTO
 * 用于搜索空间JSON文件同步导入功能的响应结果
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "同步导入响应结果")
public class ImportSyncResponse {

    @Schema(description = "导入是否成功", example = "true")
    private boolean success;

    @Schema(description = "总记录数", example = "100")
    private Integer totalRecords;

    @Schema(description = "成功导入的记录数", example = "95")
    private Integer successRecords;

    @Schema(description = "失败的记录数", example = "5")
    private Integer failedRecords;

    @Schema(description = "跳过的记录数", example = "0")
    private Integer skippedRecords;

    @Schema(description = "导入开始时间")
    private LocalDateTime startTime;

    @Schema(description = "导入结束时间")
    private LocalDateTime endTime;

    @Schema(description = "处理耗时（毫秒）", example = "1500")
    private Long processingTimeMs;

    @Schema(description = "错误信息列表")
    private List<String> errors;

    @Schema(description = "警告信息列表")
    private List<String> warnings;

    @Schema(description = "详细信息", example = "导入完成，95条记录成功处理")
    private String message;

    public ImportSyncResponse() {
    }

    public ImportSyncResponse(boolean success, Integer totalRecords, Integer successRecords,
                             Integer failedRecords, Integer skippedRecords,
                             LocalDateTime startTime, LocalDateTime endTime,
                             Long processingTimeMs, List<String> errors,
                             List<String> warnings, String message) {
        this.success = success;
        this.totalRecords = totalRecords;
        this.successRecords = successRecords;
        this.failedRecords = failedRecords;
        this.skippedRecords = skippedRecords;
        this.startTime = startTime;
        this.endTime = endTime;
        this.processingTimeMs = processingTimeMs;
        this.errors = errors;
        this.warnings = warnings;
        this.message = message;
    }

    // 静态工厂方法
    public static ImportSyncResponse success(Integer totalRecords, Integer successRecords,
                                           Integer failedRecords, Integer skippedRecords,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           Long processingTimeMs, String message) {
        return new ImportSyncResponse(true, totalRecords, successRecords, failedRecords,
                                    skippedRecords, startTime, endTime, processingTimeMs,
                                    null, null, message);
    }

    public static ImportSyncResponse failure(String errorMessage) {
        ImportSyncResponse response = new ImportSyncResponse();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setErrors(List.of(errorMessage));
        return response;
    }

    // 业务方法
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public double getSuccessRate() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (double) (successRecords != null ? successRecords : 0) / totalRecords * 100;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getSuccessRecords() {
        return successRecords;
    }

    public void setSuccessRecords(Integer successRecords) {
        this.successRecords = successRecords;
    }

    public Integer getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(Integer failedRecords) {
        this.failedRecords = failedRecords;
    }

    public Integer getSkippedRecords() {
        return skippedRecords;
    }

    public void setSkippedRecords(Integer skippedRecords) {
        this.skippedRecords = skippedRecords;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ImportSyncResponse{" +
                "success=" + success +
                ", totalRecords=" + totalRecords +
                ", successRecords=" + successRecords +
                ", failedRecords=" + failedRecords +
                ", skippedRecords=" + skippedRecords +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", processingTimeMs=" + processingTimeMs +
                ", errors=" + errors +
                ", warnings=" + warnings +
                ", message='" + message + '\'' +
                '}';
    }
}