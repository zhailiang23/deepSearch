package com.ynet.mgmt.queryunderstanding.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实体信息
 * 用于存储从查询中识别出的命名实体
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entity {

    /**
     * 实体文本
     */
    private String text;

    /**
     * 实体类型
     */
    private EntityType type;

    /**
     * 置信度（0.0-1.0）
     */
    private Double confidence;

    /**
     * 实体在原始查询中的起始位置
     */
    private Integer startPosition;

    /**
     * 实体在原始查询中的结束位置
     */
    private Integer endPosition;

    /**
     * 实体的规范化形式（如果有）
     */
    private String normalizedForm;
}
