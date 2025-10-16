package com.ynet.mgmt.queryunderstanding.context;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询上下文
 * 在查询理解管道中传递的核心数据结构
 * 存储查询的各个处理阶段的信息
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
public class QueryContext {

    /**
     * 原始查询文本
     */
    private String originalQuery;

    /**
     * 标准化后的查询（去除特殊字符、多余空白等）
     */
    private String normalizedQuery;

    /**
     * 纠正错别字后的查询
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
     * 从查询中识别出的实体列表
     */
    private List<Entity> entities = new ArrayList<>();

    /**
     * 识别出的用户意图
     */
    private IntentType intent;

    /**
     * 意图识别的置信度（0.0-1.0）
     */
    private Double intentConfidence;

    /**
     * 同义词列表
     */
    private List<String> synonyms = new ArrayList<>();

    /**
     * 语义相关词列表
     */
    private List<String> relatedTerms = new ArrayList<>();

    /**
     * 检测到的短语列表
     */
    private List<String> detectedPhrases = new ArrayList<>();

    /**
     * 关联的热门话题
     */
    private List<String> hotTopics = new ArrayList<>();

    /**
     * 查询元数据（用于存储各种额外信息）
     */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 最终构建的 Elasticsearch 查询
     */
    private Query elasticsearchQuery;

    /**
     * 各处理器的执行耗时（毫秒）
     * key: 处理器名称, value: 耗时
     */
    private Map<String, Long> processorTimings = new HashMap<>();

    /**
     * 管道开始时间（用于计算总耗时）
     */
    private long startTime;

    /**
     * 是否跳过复杂处理（针对简单查询）
     */
    private boolean skipComplexProcessing = false;

    /**
     * 构造函数
     * @param originalQuery 原始查询
     */
    public QueryContext(String originalQuery) {
        this.originalQuery = originalQuery;
        this.normalizedQuery = originalQuery;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 记录处理器执行时间
     * @param processorName 处理器名称
     * @param duration 耗时（毫秒）
     */
    public void recordProcessorTiming(String processorName, long duration) {
        this.processorTimings.put(processorName, duration);
    }

    /**
     * 获取总处理时间
     * @return 总耗时（毫秒）
     */
    public long getTotalProcessingTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 添加实体
     * @param entity 实体
     */
    public void addEntity(Entity entity) {
        this.entities.add(entity);
    }

    /**
     * 添加同义词
     * @param synonym 同义词
     */
    public void addSynonym(String synonym) {
        if (!this.synonyms.contains(synonym)) {
            this.synonyms.add(synonym);
        }
    }

    /**
     * 添加相关词
     * @param term 相关词
     */
    public void addRelatedTerm(String term) {
        if (!this.relatedTerms.contains(term)) {
            this.relatedTerms.add(term);
        }
    }

    /**
     * 添加短语
     * @param phrase 短语
     */
    public void addDetectedPhrase(String phrase) {
        if (!this.detectedPhrases.contains(phrase)) {
            this.detectedPhrases.add(phrase);
        }
    }

    /**
     * 添加热门话题
     * @param topic 热门话题
     */
    public void addHotTopic(String topic) {
        if (!this.hotTopics.contains(topic)) {
            this.hotTopics.add(topic);
        }
    }

    /**
     * 设置元数据
     * @param key 键
     * @param value 值
     */
    public void putMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * 获取元数据
     * @param key 键
     * @return 值
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    /**
     * 获取当前有效的查询文本
     * 优先级：rewrittenQuery > expandedQuery > correctedQuery > normalizedQuery > originalQuery
     * @return 当前有效查询
     */
    public String getCurrentQuery() {
        if (rewrittenQuery != null && !rewrittenQuery.isEmpty()) {
            return rewrittenQuery;
        }
        if (expandedQuery != null && !expandedQuery.isEmpty()) {
            return expandedQuery;
        }
        if (correctedQuery != null && !correctedQuery.isEmpty()) {
            return correctedQuery;
        }
        if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
            return normalizedQuery;
        }
        return originalQuery;
    }
}
