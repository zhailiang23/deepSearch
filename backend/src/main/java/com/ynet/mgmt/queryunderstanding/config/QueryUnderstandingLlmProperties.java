package com.ynet.mgmt.queryunderstanding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 查询理解LLM配置属性
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
@Component
@ConfigurationProperties(prefix = "query-understanding.llm")
public class QueryUnderstandingLlmProperties {

    /**
     * 是否启用LLM增强
     */
    private Boolean enabled = false;

    /**
     * SiliconFlow API配置
     */
    private SiliconFlowConfig siliconflow = new SiliconFlowConfig();

    /**
     * Embedding API配置
     */
    private EmbeddingConfig embedding = new EmbeddingConfig();

    @Data
    public static class SiliconFlowConfig {
        /**
         * API Key
         */
        private String apiKey;

        /**
         * API URL
         */
        private String apiUrl = "https://api.siliconflow.cn/v1/chat/completions";

        /**
         * 模型名称
         */
        private String model = "Qwen/Qwen2.5-7B-Instruct";

        /**
         * 温度参数
         */
        private Double temperature = 0.3;

        /**
         * 最大生成token数
         */
        private Integer maxTokens = 500;

        /**
         * Top-P参数
         */
        private Double topP = 0.9;

        /**
         * 超时时间(毫秒)
         */
        private Long timeout = 10000L;
    }

    @Data
    public static class EmbeddingConfig {
        /**
         * API Key (如果为空则使用siliconflow.apiKey)
         */
        private String apiKey;

        /**
         * API URL
         */
        private String apiUrl = "https://api.siliconflow.cn/v1/embeddings";

        /**
         * 模型名称
         */
        private String model = "BAAI/bge-large-zh-v1.5";

        /**
         * 编码格式
         */
        private String encodingFormat = "float";

        /**
         * 超时时间(毫秒)
         */
        private Long timeout = 10000L;

        /**
         * 语义相似度阈值
         */
        private Double similarityThreshold = 0.7;
    }
}
