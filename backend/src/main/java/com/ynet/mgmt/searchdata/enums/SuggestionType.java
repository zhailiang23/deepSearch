package com.ynet.mgmt.searchdata.enums;

/**
 * 搜索建议类型枚举
 * 用于标识建议的来源类型
 *
 * @author system
 * @since 1.0.0
 */
public enum SuggestionType {
    /**
     * 基于ES Completion Suggester的建议
     */
    ES_COMPLETION("ES建议", "suggest"),

    /**
     * 基于搜索历史的建议
     */
    HISTORY("历史搜索", "history"),

    /**
     * 基于热门话题的建议
     */
    HOT_TOPIC("热门搜索", "hot");

    private final String displayName;
    private final String icon;

    SuggestionType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
