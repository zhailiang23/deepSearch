package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 批量操作响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量操作响应")
public class BulkOperationResponse {

    @Schema(description = "执行耗时（毫秒）", example = "120")
    private Long took;

    @Schema(description = "是否有错误", example = "false")
    private Boolean errors;

    public boolean hasErrors() {
        return errors != null && errors;
    }

    @Schema(description = "操作结果列表")
    private List<Map<String, Object>> items;
}