package com.ynet.mgmt.searchdata.enums;

/**
 * 语义搜索模式枚举
 * 定义不同的语义搜索策略
 *
 * @author system
 * @since 1.0.0
 */
public enum SemanticSearchMode {

    /**
     * 自动模式
     * 根据查询内容自动选择最佳搜索策略
     * 短查询偏向关键词搜索，长查询偏向语义搜索
     */
    AUTO("自动模式", "根据查询内容自动选择最佳搜索策略"),

    /**
     * 关键词优先模式
     * 以关键词搜索为主，语义搜索为辅
     * 关键词搜索权重更高
     */
    KEYWORD_FIRST("关键词优先", "以关键词搜索为主，语义搜索为辅"),

    /**
     * 语义优先模式
     * 以语义搜索为主，关键词搜索为辅
     * 语义搜索权重更高，适合意图搜索
     */
    SEMANTIC_FIRST("语义优先", "以语义搜索为主，关键词搜索为辅"),

    /**
     * 混合模式
     * 关键词搜索和语义搜索权重平衡
     * 适合大多数搜索场景
     */
    HYBRID("混合模式", "关键词搜索和语义搜索权重平衡"),

    /**
     * 仅关键词模式
     * 只使用关键词搜索，不使用语义搜索
     * 适合精确匹配场景
     */
    KEYWORD_ONLY("仅关键词", "只使用关键词搜索，不使用语义搜索"),

    /**
     * 仅语义模式
     * 只使用语义搜索，不使用关键词搜索
     * 适合概念和意图搜索场景
     */
    SEMANTIC_ONLY("仅语义", "只使用语义搜索，不使用关键词搜索");

    private final String displayName;
    private final String description;

    SemanticSearchMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据模式获取语义搜索权重
     *
     * @return 语义搜索权重 (0.0 - 1.0)
     */
    public double getSemanticWeight() {
        switch (this) {
            case KEYWORD_FIRST:
                return 0.2;
            case SEMANTIC_FIRST:
                return 0.8;
            case HYBRID:
                return 0.5;
            case KEYWORD_ONLY:
                return 0.0;
            case SEMANTIC_ONLY:
                return 1.0;
            case AUTO:
            default:
                return 0.3; // 默认权重
        }
    }

    /**
     * 根据查询内容长度自动确定语义搜索权重
     * 用于 AUTO 模式的智能权重分配
     *
     * @param queryLength 查询文本长度
     * @return 语义搜索权重
     */
    public double getAutoSemanticWeight(int queryLength) {
        if (this != AUTO) {
            return getSemanticWeight();
        }

        // 根据查询长度动态调整权重
        if (queryLength <= 2) {
            // 很短的查询，偏向关键词搜索
            return 0.1;
        } else if (queryLength <= 5) {
            // 短查询，关键词搜索为主
            return 0.2;
        } else if (queryLength <= 10) {
            // 中等长度，平衡搜索
            return 0.4;
        } else {
            // 长查询，偏向语义搜索
            return 0.6;
        }
    }

    /**
     * 是否启用关键词搜索
     */
    public boolean isKeywordEnabled() {
        return this != SEMANTIC_ONLY;
    }

    /**
     * 是否启用语义搜索
     */
    public boolean isSemanticEnabled() {
        return this != KEYWORD_ONLY;
    }
}