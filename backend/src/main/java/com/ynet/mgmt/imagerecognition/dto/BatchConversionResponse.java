package com.ynet.mgmt.imagerecognition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量转换响应 DTO
 */
@Schema(description = "批量图片转换响应")
public class BatchConversionResponse {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "总图片数量")
    private int totalCount;

    @Schema(description = "成功识别数量")
    private int successCount;

    @Schema(description = "失败数量")
    private int failureCount;

    @Schema(description = "插入到索引的数量")
    private int insertedCount;

    @Schema(description = "跳过的数量（已存在）")
    private int skippedCount;

    @Schema(description = "识别结果列表")
    private List<ImageRecognitionResult> results;

    public BatchConversionResponse() {
        this.results = new ArrayList<>();
    }

    public static BatchConversionResponse success(List<ImageRecognitionResult> results) {
        BatchConversionResponse response = new BatchConversionResponse();
        response.setSuccess(true);
        response.setMessage("批量识别完成");
        response.setResults(results);
        response.setTotalCount(results.size());
        response.setSuccessCount((int) results.stream().filter(ImageRecognitionResult::isSuccess).count());
        response.setFailureCount((int) results.stream().filter(r -> !r.isSuccess()).count());
        return response;
    }

    public static BatchConversionResponse error(String message) {
        BatchConversionResponse response = new BatchConversionResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<ImageRecognitionResult> getResults() {
        return results;
    }

    public void setResults(List<ImageRecognitionResult> results) {
        this.results = results;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
}
