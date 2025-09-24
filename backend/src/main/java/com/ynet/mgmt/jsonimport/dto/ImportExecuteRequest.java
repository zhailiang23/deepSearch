package com.ynet.mgmt.jsonimport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 导入执行请求DTO
 * 用于前端提交导入任务的配置参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportExecuteRequest {

    /**
     * 任务ID（文件上传时生成）
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 搜索空间ID
     */
    private Long searchSpaceId;

    /**
     * 导入模式
     */
    @NotNull(message = "导入模式不能为空")
    private ImportMode mode;

    /**
     * 批处理大小
     */
    @Min(value = 100, message = "批处理大小不能小于100")
    @Max(value = 5000, message = "批处理大小不能大于5000")
    private Integer batchSize;

    /**
     * 错误处理策略
     */
    @NotNull(message = "错误处理策略不能为空")
    private ErrorHandlingStrategy errorHandling;

    /**
     * 是否启用索引优化
     */
    private Boolean enableIndexOptimization;

    /**
     * 自定义映射配置（JSON格式）
     */
    private String customMappingConfig;

    /**
     * 导入模式枚举
     */
    public enum ImportMode {
        /**
         * 追加模式：将数据添加到现有索引中
         */
        APPEND("append", "追加"),

        /**
         * 替换模式：先清空索引再导入新数据
         */
        REPLACE("replace", "替换");

        private final String code;
        private final String description;

        ImportMode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 错误处理策略枚举
     */
    public enum ErrorHandlingStrategy {
        /**
         * 遇到错误时停止导入
         */
        STOP_ON_ERROR("stop", "遇到错误时停止导入"),

        /**
         * 跳过错误记录继续导入
         */
        SKIP_ERROR("skip", "跳过错误记录继续导入");

        private final String code;
        private final String description;

        ErrorHandlingStrategy(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}