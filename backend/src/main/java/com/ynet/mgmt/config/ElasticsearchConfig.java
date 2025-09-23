package com.ynet.mgmt.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch配置类
 * 负责创建和配置Elasticsearch客户端
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    /**
     * 创建ElasticsearchClient Bean
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchProperties properties) {
        log.info("正在配置Elasticsearch客户端，连接地址: {}", properties.getUris());

        try {
            // 解析第一个URI作为主节点
            String firstUri = properties.getUris().get(0);
            HttpHost httpHost = HttpHost.create(firstUri);

            RestClientBuilder builder = RestClient.builder(httpHost);

            // 配置认证
            configureAuthentication(builder, properties);

            // 配置超时
            configureTimeouts(builder, properties);

            // 构建RestClient
            RestClient restClient = builder.build();

            // 创建传输层
            ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
            );

            // 创建客户端
            ElasticsearchClient client = new ElasticsearchClient(transport);

            log.info("Elasticsearch客户端配置完成");
            return client;

        } catch (Exception e) {
            log.error("配置Elasticsearch客户端失败", e);
            throw new RuntimeException("无法创建Elasticsearch客户端", e);
        }
    }

    /**
     * 配置认证信息
     */
    private void configureAuthentication(RestClientBuilder builder, ElasticsearchProperties properties) {
        if (properties.hasUsernamePassword()) {
            log.info("配置用户名密码认证");
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword())
            );

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        } else if (properties.hasApiKey()) {
            log.info("配置API密钥认证");
            builder.setDefaultHeaders(new org.apache.http.Header[]{
                new org.apache.http.message.BasicHeader("Authorization", "ApiKey " + properties.getApiKey())
            });
        } else {
            log.info("未配置认证信息，使用无认证连接");
        }
    }

    /**
     * 配置超时设置
     */
    private void configureTimeouts(RestClientBuilder builder, ElasticsearchProperties properties) {
        log.info("配置超时设置 - 连接超时: {}ms, 读取超时: {}ms",
                properties.getConnectionTimeout().toMillis(),
                properties.getReadTimeout().toMillis());

        builder.setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout(Math.toIntExact(properties.getConnectionTimeout().toMillis()))
                .setSocketTimeout(Math.toIntExact(properties.getReadTimeout().toMillis()))
        );
    }
}