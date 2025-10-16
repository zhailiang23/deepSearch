package com.ynet.mgmt.queryunderstanding.service;

import com.ynet.mgmt.queryunderstanding.context.Entity;
import com.ynet.mgmt.queryunderstanding.context.IntentType;

import java.util.List;

/**
 * 查询理解LLM服务接口
 *
 * @author deepSearch
 * @since 2025-01-16
 */
public interface QueryUnderstandingLlmService {

    /**
     * 识别查询意图
     *
     * @param query 查询文本
     * @return 意图类型
     */
    IntentType recognizeIntent(String query);

    /**
     * 提取查询中的实体
     *
     * @param query 查询文本
     * @return 实体列表
     */
    List<Entity> extractEntities(String query);

    /**
     * 基于意图重写查询
     *
     * @param query 原始查询
     * @param intent 识别出的意图
     * @param entities 提取的实体
     * @return 重写后的查询
     */
    String rewriteQuery(String query, IntentType intent, List<Entity> entities);

    /**
     * 检查服务是否可用
     *
     * @return true 如果服务可用
     */
    boolean isServiceAvailable();
}
