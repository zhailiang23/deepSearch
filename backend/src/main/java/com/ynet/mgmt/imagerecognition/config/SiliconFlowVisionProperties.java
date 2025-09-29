package com.ynet.mgmt.imagerecognition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 硅基流动 Vision API 配置属性
 */
@Configuration
@ConfigurationProperties(prefix = "siliconflow.vision")
public class SiliconFlowVisionProperties {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API URL
     */
    private String apiUrl;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 温度参数
     */
    private Double temperature = 0.1;

    /**
     * 最大 tokens 数
     */
    private Integer maxTokens = 2048;

    /**
     * Top-p 参数
     */
    private Double topP = 0.8;

    /**
     * 是否启用流式响应
     */
    private Boolean stream = false;

    /**
     * 超时时间（毫秒）
     */
    private Long timeout = 60000L;

    /**
     * 图片配置
     */
    private ImageConfig image = new ImageConfig();

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

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public ImageConfig getImage() {
        return image;
    }

    public void setImage(ImageConfig image) {
        this.image = image;
    }

    /**
     * 图片配置类
     */
    public static class ImageConfig {

        /**
         * 最大文件大小（字节）
         */
        private Long maxFileSize = 10485760L; // 10MB

        /**
         * 支持的文件格式
         */
        private String supportedFormats = "jpg,jpeg,png";

        /**
         * 最大宽度
         */
        private Integer maxWidth = 3584;

        /**
         * 最大高度
         */
        private Integer maxHeight = 3584;

        /**
         * 最小宽度
         */
        private Integer minWidth = 56;

        /**
         * 最小高度
         */
        private Integer minHeight = 56;

        public Long getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(Long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public String getSupportedFormats() {
            return supportedFormats;
        }

        public void setSupportedFormats(String supportedFormats) {
            this.supportedFormats = supportedFormats;
        }

        public Integer getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(Integer maxWidth) {
            this.maxWidth = maxWidth;
        }

        public Integer getMaxHeight() {
            return maxHeight;
        }

        public void setMaxHeight(Integer maxHeight) {
            this.maxHeight = maxHeight;
        }

        public Integer getMinWidth() {
            return minWidth;
        }

        public void setMinWidth(Integer minWidth) {
            this.minWidth = minWidth;
        }

        public Integer getMinHeight() {
            return minHeight;
        }

        public void setMinHeight(Integer minHeight) {
            this.minHeight = minHeight;
        }
    }
}