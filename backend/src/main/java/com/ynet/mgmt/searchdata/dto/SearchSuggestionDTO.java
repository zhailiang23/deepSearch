package com.ynet.mgmt.searchdata.dto;

import com.ynet.mgmt.searchdata.enums.SuggestionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索建议响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "搜索建议响应")
public class SearchSuggestionDTO {

    /**
     * 建议文本
     */
    @Schema(description = "建议文本", example = "中国银行")
    private String text;

    /**
     * 综合得分(0-100)
     */
    @Schema(description = "综合得分", example = "85.5")
    private Double score;

    /**
     * 建议类型
     */
    @Schema(description = "建议类型", example = "HOT_TOPIC")
    private SuggestionType type;

    /**
     * 图标标识
     */
    @Schema(description = "图标标识", example = "hot")
    private String icon;

    /**
     * 额外元数据
     */
    @Schema(description = "额外元数据")
    private Map<String, Object> metadata;

    // Constructors
    public SearchSuggestionDTO() {
        this.metadata = new HashMap<>();
    }

    public SearchSuggestionDTO(String text, Double score, SuggestionType type) {
        this.text = text;
        this.score = score;
        this.type = type;
        this.icon = type != null ? type.getIcon() : null;
        this.metadata = new HashMap<>();
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public SuggestionType getType() {
        return type;
    }

    public void setType(SuggestionType type) {
        this.type = type;
        this.icon = type != null ? type.getIcon() : null;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "SearchSuggestionDTO{" +
                "text='" + text + '\'' +
                ", score=" + score +
                ", type=" + type +
                ", icon='" + icon + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
