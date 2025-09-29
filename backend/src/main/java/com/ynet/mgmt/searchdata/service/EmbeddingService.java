package com.ynet.mgmt.searchdata.service;

import java.util.List;
import java.util.Map;

/**
 * 嵌入服务接口
 * 支持多种嵌入模型的统一接口
 *
 * @author system
 * @since 1.0.0
 */
public interface EmbeddingService {

    /**
     * 将单个文本转换为向量表示
     *
     * @param text 待转换的文本
     * @return 文本的向量表示，如果服务不可用则返回null
     */
    List<Float> getTextEmbedding(String text);

    /**
     * 批量将文本转换为向量表示
     *
     * @param texts 待转换的文本列表
     * @return 文本到向量的映射，如果服务不可用则返回null
     */
    Map<String, List<Float>> getBatchTextEmbeddings(List<String> texts);

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 余弦相似度值（-1到1之间）
     */
    double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2);

    /**
     * 检查服务是否可用
     *
     * @return 服务可用性状态
     */
    boolean isServiceAvailable();

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    int getVectorDimension();

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取服务类型
     *
     * @return 服务类型（local/remote）
     */
    String getServiceType();
}