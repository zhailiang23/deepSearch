package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 删除文档请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "删除文档请求")
public class DeleteDocumentRequest {

    @Schema(description = "索引名称", required = true, example = "user_data")
    @NotBlank(message = "索引名称不能为空")
    private String index;

    @Schema(description = "文档版本号（用于乐观锁）", example = "1")
    private Long version;
}