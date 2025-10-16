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
}
