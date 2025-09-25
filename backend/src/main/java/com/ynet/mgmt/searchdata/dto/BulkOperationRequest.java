package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量操作请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "批量操作请求")
public class BulkOperationRequest {

    @Schema(description = "批量操作列表", required = true)
    @NotEmpty(message = "操作列表不能为空")
    private List<BulkOperation> operations;

    /**
     * 批量操作项
     */
    @Data
    @Schema(description = "批量操作项")
    public static class BulkOperation {

        @Schema(description = "操作类型", required = true, example = "index",
                allowableValues = {"index", "update", "delete"})
        @NotNull(message = "操作类型不能为空")
        private String action;

        @Schema(description = "文档ID", required = true, example = "doc123")
        @NotNull(message = "文档ID不能为空")
        private String id;

        @Schema(description = "索引名称", required = true, example = "user_data")
        @NotNull(message = "索引名称不能为空")
        private String index;

        @Schema(description = "文档内容（仅index和update操作需要）")
        private Map<String, Object> source;

        @Schema(description = "文档版本号（用于乐观锁）")
        private Long version;
    }
}