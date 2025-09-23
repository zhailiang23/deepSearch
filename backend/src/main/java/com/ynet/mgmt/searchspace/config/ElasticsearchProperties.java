package com.ynet.mgmt.searchspace.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Elasticsearch配置属性类
 * 管理Elasticsearch客户端的连接配置、超时设置和索引默认配置
 *
 * @author system
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "elasticsearch")
@Validated
public class ElasticsearchProperties {

    /**
     * Elasticsearch集群节点URI列表
     */
    @NotEmpty(message = "Elasticsearch URI列表不能为空")
    private List<String> uris = List.of("http://localhost:9200");

    /**
     * 连接用户名（可选）
     */
    private String username;

    /**
     * 连接密码（可选）
     */
    private String password;

    /**
     * API密钥（可选，与用户名密码二选一）
     */
    private String apiKey;

    /**
     * 连接超时时间
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * 读取超时时间
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * 是否启用SSL
     */
    private boolean sslEnabled = false;

    /**
     * SSL证书指纹（可选）
     */
    private String sslFingerprint;

    /**
     * 索引相关配置
     */
    @Valid
    private Index index = new Index();

    /**
     * 索引配置内部类
     */
    public static class Index {
        /**
         * 默认索引设置
         */
        @Valid
        private DefaultSettings defaultSettings = new DefaultSettings();

        /**
         * 默认索引设置内部类
         */
        public static class DefaultSettings {
            /**
             * 主分片数量
             */
            @Min(value = 1, message = "主分片数量必须大于0")
            private Integer numberOfShards = 1;

            /**
             * 副本数量
             */
            @Min(value = 0, message = "副本数量不能小于0")
            private Integer numberOfReplicas = 0;

            /**
             * 索引刷新间隔
             */
            private Duration refreshInterval = Duration.ofSeconds(1);

            // Getters and Setters
            public Integer getNumberOfShards() {
                return numberOfShards;
            }

            public void setNumberOfShards(Integer numberOfShards) {
                this.numberOfShards = numberOfShards;
            }

            public Integer getNumberOfReplicas() {
                return numberOfReplicas;
            }

            public void setNumberOfReplicas(Integer numberOfReplicas) {
                this.numberOfReplicas = numberOfReplicas;
            }

            public Duration getRefreshInterval() {
                return refreshInterval;
            }

            public void setRefreshInterval(Duration refreshInterval) {
                this.refreshInterval = refreshInterval;
            }
        }

        // Getters and Setters
        public DefaultSettings getDefaultSettings() {
            return defaultSettings;
        }

        public void setDefaultSettings(DefaultSettings defaultSettings) {
            this.defaultSettings = defaultSettings;
        }
    }

    // Getters and Setters
    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getSslFingerprint() {
        return sslFingerprint;
    }

    public void setSslFingerprint(String sslFingerprint) {
        this.sslFingerprint = sslFingerprint;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    /**
     * 检查是否配置了用户名密码认证
     */
    public boolean hasUsernamePassword() {
        return username != null && password != null && !username.trim().isEmpty() && !password.trim().isEmpty();
    }

    /**
     * 检查是否配置了API密钥认证
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * 检查是否有任何形式的认证配置
     */
    public boolean hasAuthentication() {
        return hasUsernamePassword() || hasApiKey();
    }
}