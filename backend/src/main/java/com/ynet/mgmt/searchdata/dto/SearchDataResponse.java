package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 搜索数据响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索数据响应")
public class SearchDataResponse {

    @Schema(description = "数据列表")
    private List<DocumentData> data;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer page;

    @Schema(description = "每页大小", example = "20")
    private Integer size;

    @Schema(description = "索引映射信息")
    private IndexMappingInfo mapping;

    /**
     * 文档数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "文档数据")
    public static class DocumentData {

        @Schema(description = "文档ID", example = "doc123")
        private String _id;

        @Schema(description = "相关性评分", example = "1.2345")
        private Double _score;

        @Schema(description = "文档内容")
        private Map<String, Object> _source;

        @Schema(description = "索引名称", example = "user_data")
        private String _index;

        @Schema(description = "文档类型")
        private String _type;

        @Schema(description = "文档版本", example = "1")
        private Long _version;
    }

    /**
     * 索引映射信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "索引映射信息")
    public static class IndexMappingInfo {

        @Schema(description = "映射属性")
        private Map<String, Object> mappings;
    }
}