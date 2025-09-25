package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 索引映射响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "索引映射响应")
public class IndexMappingResponse {

    @Schema(description = "索引名称", example = "user_data")
    private String index;

    @Schema(description = "映射配置")
    private MappingInfo mapping;

    /**
     * 映射配置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "映射配置信息")
    public static class MappingInfo {

        @Schema(description = "映射属性")
        private Map<String, Object> mappings;
    }
}