package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文档详情响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档详情响应")
public class DocumentDetailResponse {

    @Schema(description = "文档ID", example = "doc123")
    private String _id;

    @Schema(description = "索引名称", example = "user_data")
    private String _index;

    @Schema(description = "文档类型")
    private String _type;

    @Schema(description = "文档版本(seq_no)", example = "1")
    private Long _version;

    @Schema(description = "主分片term", example = "1")
    private Long _primary_term;

    @Schema(description = "是否找到文档", example = "true")
    private Boolean found;

    @Schema(description = "文档内容")
    private Map<String, Object> _source;
}