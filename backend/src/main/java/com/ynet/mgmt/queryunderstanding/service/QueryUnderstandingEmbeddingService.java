package com.ynet.mgmt.queryunderstanding.service;

import java.util.List;
import java.util.Map;

/**
 * 查询理解Embedding服务接口
 *
 * @author deepSearch
 * @since 2025-01-16
 */
public interface QueryUnderstandingEmbeddingService {

    /**
     * 生成文本的embedding向量
     *
     * @param text 输入文本
     * @return embedding向量
     */
    List<Double> generateEmbedding(String text);

    /**
     * 批量生成embedding向量
     *
     * @param texts 输入文本列表
     * @return 文本到embedding向量的映射
     */
    Map<String, List<Double>> generateEmbeddings(List<String> texts);

    /**
     * 计算两个文本的语义相似度
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度得分 (0-1)
     */
    double calculateSimilarity(String text1, String text2);

    /**
     * 计算两个embedding向量的余弦相似度
     *
     * @param embedding1 向量1
     * @param embedding2 向量2
     * @return 相似度得分 (0-1)
     */
    double cosineSimilarity(List<Double> embedding1, List<Double> embedding2);

    /**
     * 检查服务是否可用
     *
     * @return true表示服务可用
     */
    boolean isServiceAvailable();
}
