package com.ynet.mgmt.searchdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 搜索建议请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "搜索建议请求")
public class SearchSuggestionRequest {

    /**
     * 用户输入的查询词
     */
    @Schema(description = "用户输入的查询词", example = "中国", required = true)
    @NotBlank(message = "查询词不能为空")
    @Size(max = 100, message = "查询词长度不能超过100")
    private String query;

    /**
     * 搜索空间ID(可选,用于上下文过滤)
     * @deprecated 使用 searchSpaceIds 替代
     */
    @Schema(description = "搜索空间ID（已废弃，使用searchSpaceIds）", example = "1", deprecated = true)
    @Deprecated
    private Long searchSpaceId;

    /**
     * 搜索空间ID列表(可选,支持多索引搜索)
     */
    @Schema(description = "搜索空间ID列表", example = "[1, 2, 3]")
    private List<Long> searchSpaceIds;

    /**
     * 用户ID(可选,用于个性化建议)
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 返回建议数量
     */
    @Schema(description = "返回建议数量", example = "10", defaultValue = "10")
    @Min(value = 1, message = "返回数量至少为1")
    @Max(value = 20, message = "返回数量最多为20")
    private Integer size = 10;

    /**
     * 是否启用模糊匹配
     */
    @Schema(description = "是否启用模糊匹配", example = "true", defaultValue = "true")
    private Boolean enableFuzzy = true;

    // Constructors
    public SearchSuggestionRequest() {
    }

    public SearchSuggestionRequest(String query) {
        this.query = query;
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getSearchSpaceId() {
        return searchSpaceId;
    }

    public void setSearchSpaceId(Long searchSpaceId) {
        this.searchSpaceId = searchSpaceId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getEnableFuzzy() {
        return enableFuzzy;
    }

    public void setEnableFuzzy(Boolean enableFuzzy) {
        this.enableFuzzy = enableFuzzy;
    }

    public List<Long> getSearchSpaceIds() {
        return searchSpaceIds;
    }

    public void setSearchSpaceIds(List<Long> searchSpaceIds) {
        this.searchSpaceIds = searchSpaceIds;
    }

    @Override
    public String toString() {
        return "SearchSuggestionRequest{" +
                "query='" + query + '\'' +
                ", searchSpaceId=" + searchSpaceId +
                ", searchSpaceIds=" + searchSpaceIds +
                ", userId=" + userId +
                ", size=" + size +
                ", enableFuzzy=" + enableFuzzy +
                '}';
    }
}
