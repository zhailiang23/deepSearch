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

    @Schema(description = "搜索日志ID", example = "123")
    private Long searchLogId;

    @Schema(description = "搜索元数据")
    private SearchMetadata searchMetadata;

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

        @Schema(description = "高亮信息")
        private Map<String, List<String>> highlight;
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

    /**
     * 搜索元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索元数据")
    public static class SearchMetadata {

        @Schema(description = "使用的搜索模式", example = "HYBRID")
        private String searchMode;

        @Schema(description = "是否启用语义搜索", example = "true")
        private Boolean semanticEnabled;

        @Schema(description = "语义搜索权重", example = "0.3")
        private Double semanticWeight;

        @Schema(description = "是否启用拼音搜索", example = "true")
        private Boolean pinyinEnabled;

        @Schema(description = "拼音搜索模式", example = "AUTO")
        private String pinyinMode;

        @Schema(description = "查询构建耗时(毫秒)", example = "15")
        private Long queryBuildTime;

        @Schema(description = "向量生成耗时(毫秒)", example = "50")
        private Long vectorGenerationTime;

        @Schema(description = "ES查询耗时(毫秒)", example = "120")
        private Long elasticsearchTime;

        @Schema(description = "总耗时(毫秒)", example = "185")
        private Long totalTime;

        @Schema(description = "向量服务状态", example = "available")
        private String vectorServiceStatus;

        @Schema(description = "实际使用的查询类型", example = "hybrid",
                allowableValues = {"keyword", "semantic", "hybrid"})
        private String actualQueryType;

        @Schema(description = "查询文本长度", example = "12")
        private Integer queryLength;

        @Schema(description = "搜索策略自动调整说明")
        private String adjustmentReason;
    }
}