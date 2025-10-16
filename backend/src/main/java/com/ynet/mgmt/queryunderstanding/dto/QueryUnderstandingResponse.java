package com.ynet.mgmt.queryunderstanding.dto;

import com.ynet.mgmt.queryunderstanding.context.Entity;
import com.ynet.mgmt.queryunderstanding.context.IntentType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 查询理解响应 DTO
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
@Builder
public class QueryUnderstandingResponse {

    /**
     * 原始查询
     */
    private String originalQuery;

    /**
     * 标准化后的查询
     */
    private String normalizedQuery;

    /**
     * 纠正后的查询
     */
    private String correctedQuery;

    /**
     * 扩展后的查询（包含同义词等）
     */
    private String expandedQuery;

    /**
     * 重写后的查询（基于意图和实体优化）
     */
    private String rewrittenQuery;

    /**
     * 最终的查询（用于搜索）
     */
    private String finalQuery;

    /**
     * 识别的实体
     */
    private List<Entity> entities;

    /**
     * 识别的意图
     */
    private IntentType intent;

    /**
     * 意图置信度
     */
    private Double intentConfidence;

    /**
     * 同义词
     */
    private List<String> synonyms;

    /**
     * 相关词
     */
    private List<String> relatedTerms;

    /**
     * 检测到的短语
     */
    private List<String> detectedPhrases;

    /**
     * 热门话题
     */
    private List<String> hotTopics;

    /**
     * 各处理器耗时（毫秒）
     */
    private Map<String, Long> processorTimings;

    /**
     * 总处理时间（毫秒）
     */
    private Long totalProcessingTime;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
