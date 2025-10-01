package com.ynet.mgmt.searchlog.projection;

import java.time.LocalDateTime;

/**
 * 搜索日志轻量级投影接口
 * 用于搜索建议功能，只包含必要字段，避免加载大字段（如 responseData）
 *
 * @author Claude Code AI
 * @since 2025-10-01
 */
public interface SearchLogMinimal {

    /**
     * 获取搜索查询词
     */
    String getSearchQuery();

    /**
     * 获取创建时间
     */
    LocalDateTime getCreatedAt();
}
