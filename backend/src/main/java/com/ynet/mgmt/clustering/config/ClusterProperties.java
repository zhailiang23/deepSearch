package com.ynet.mgmt.clustering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 聚类分析配置属性
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {

    private PythonConfig python = new PythonConfig();
    private SiliconFlowConfig siliconflow = new SiliconFlowConfig();

    public PythonConfig getPython() {
        return python;
    }

    public void setPython(PythonConfig python) {
        this.python = python;
    }

    public SiliconFlowConfig getSiliconflow() {
        return siliconflow;
    }

    public void setSiliconflow(SiliconFlowConfig siliconflow) {
        this.siliconflow = siliconflow;
    }

    /**
     * Python 服务配置
     */
    public static class PythonConfig {
        private String serviceUrl;
        private Long timeout;
        private Integer maxTexts;

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }

        public Integer getMaxTexts() {
            return maxTexts;
        }

        public void setMaxTexts(Integer maxTexts) {
            this.maxTexts = maxTexts;
        }
    }

    /**
     * 硅基流动配置
     */
    public static class SiliconFlowConfig {
        private EmbeddingConfig embedding = new EmbeddingConfig();
        private LlmConfig llm = new LlmConfig();

        public EmbeddingConfig getEmbedding() {
            return embedding;
        }

        public void setEmbedding(EmbeddingConfig embedding) {
            this.embedding = embedding;
        }

        public LlmConfig getLlm() {
            return llm;
        }

        public void setLlm(LlmConfig llm) {
            this.llm = llm;
        }
    }

    /**
     * Embedding API 配置
     */
    public static class EmbeddingConfig {
        private String apiKey;
        private String apiUrl;
        private String model;
        private Long timeout;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * LLM API 配置
     */
    public static class LlmConfig {
        private String apiKey;
        private String apiUrl;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Long timeout;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }
    }
}
