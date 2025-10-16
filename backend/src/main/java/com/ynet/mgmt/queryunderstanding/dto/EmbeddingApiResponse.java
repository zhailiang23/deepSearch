package com.ynet.mgmt.queryunderstanding.dto;

import lombok.Data;

import java.util.List;

/**
 * SiliconFlow Embedding API响应
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
public class EmbeddingApiResponse {

    /**
     * 模型名称
     */
    private String model;

    /**
     * Embedding数据列表
     */
    private List<EmbeddingData> data;

    /**
     * Token使用统计
     */
    private Usage usage;

    @Data
    public static class EmbeddingData {
        /**
         * 对象类型，通常为 "embedding"
         */
        private String object;

        /**
         * Embedding向量
         */
        private List<Double> embedding;

        /**
         * 索引位置
         */
        private Integer index;
    }

    @Data
    public static class Usage {
        /**
         * 提示词token数
         */
        private Integer promptTokens;

        /**
         * 完成token数
         */
        private Integer completionTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}
