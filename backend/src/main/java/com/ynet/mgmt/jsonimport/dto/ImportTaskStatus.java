package com.ynet.mgmt.jsonimport.dto;

import com.ynet.mgmt.jsonimport.enums.ImportTaskState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导入任务状态DTO
 * 用于前端查询导入任务进度和状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTaskStatus {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 搜索空间ID
     */
    private Long searchSpaceId;

    /**
     * 任务状态
     */
    private ImportTaskState state;

    /**
     * 总记录数
     */
    private Integer totalRecords;

    /**
     * 已处理记录数
     */
    private Integer processedRecords;

    /**
     * 成功导入记录数
     */
    private Integer successCount;

    /**
     * 错误记录数
     */
    private Integer errorCount;

    /**
     * 当前批次
     */
    private Integer currentBatch;

    /**
     * 总批次数
     */
    private Integer totalBatches;

    /**
     * 进度百分比
     */
    private Double progressPercentage;

    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;

    /**
     * 任务完成时间
     */
    private LocalDateTime endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误详情列表（最多显示前20个错误）
     */
    private List<String> errorDetails;

    /**
     * 当前处理状态描述
     */
    private String statusMessage;

    /**
     * 估计剩余时间（秒）
     */
    private Long estimatedRemainingTime;

    /**
     * 导入结果摘要
     */
    private ImportResultSummary resultSummary;

    /**
     * 导入结果摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResultSummary {

        /**
         * 导入是否成功
         */
        private Boolean success;

        /**
         * 总耗时（毫秒）
         */
        private Long durationMs;

        /**
         * 创建的索引名称
         */
        private String createdIndexName;

        /**
         * 实际导入的文档数
         */
        private Integer indexedDocuments;

        /**
         * 平均处理速度（记录/秒）
         */
        private Double averageSpeed;

        /**
         * 索引大小（字节）
         */
        private Long indexSize;
    }

    /**
     * 计算进度百分比
     */
    public void updateProgressPercentage() {
        if (totalRecords != null && totalRecords > 0) {
            this.progressPercentage = (processedRecords != null ? processedRecords : 0) * 100.0 / totalRecords;
        } else {
            this.progressPercentage = 0.0;
        }
    }

    /**
     * 检查任务是否完成（成功或失败）
     */
    public boolean isCompleted() {
        return state == ImportTaskState.COMPLETED || state == ImportTaskState.FAILED;
    }

    /**
     * 检查任务是否成功
     */
    public boolean isSuccessful() {
        return state == ImportTaskState.COMPLETED && (errorCount == null || errorCount == 0);
    }

    /**
     * 检查任务是否失败
     */
    public boolean isFailed() {
        return state == ImportTaskState.FAILED;
    }

    /**
     * 检查任务是否正在运行
     */
    public boolean isRunning() {
        return state == ImportTaskState.RUNNING || state == ImportTaskState.PROCESSING_DATA;
    }
}