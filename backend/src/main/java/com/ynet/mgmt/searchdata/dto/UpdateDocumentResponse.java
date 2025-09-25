package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新文档响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新文档响应")
public class UpdateDocumentResponse {

    @Schema(description = "文档ID", example = "doc123")
    private String id;

    @Schema(description = "索引名称", example = "user_data")
    private String index;

    @Schema(description = "文档版本", example = "2")
    private Long version;

    @Schema(description = "操作结果", example = "updated", allowableValues = {"created", "updated", "noop"})
    private String result;
}