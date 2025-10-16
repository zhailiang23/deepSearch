package com.ynet.mgmt.queryunderstanding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.queryunderstanding.config.QueryUnderstandingLlmProperties;
import com.ynet.mgmt.queryunderstanding.dto.EmbeddingApiRequest;
import com.ynet.mgmt.queryunderstanding.dto.EmbeddingApiResponse;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingCacheService;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 查询理解Embedding服务实现
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Service
public class QueryUnderstandingEmbeddingServiceImpl implements QueryUnderstandingEmbeddingService {

    private final QueryUnderstandingLlmProperties properties;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private QueryUnderstandingCacheService cacheService;

    public QueryUnderstandingEmbeddingServiceImpl(QueryUnderstandingLlmProperties properties,
                                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            // 使用HttpComponents连接池提升性能 (如果可用)
            try {
                Class.forName("org.apache.http.client.HttpClient");
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(properties.getEmbedding().getTimeout().intValue());
                factory.setConnectionRequestTimeout(properties.getEmbedding().getTimeout().intValue());
                this.restTemplate = new RestTemplate(factory);
                log.info("使用HttpComponents连接池配置Embedding RestTemplate");
            } catch (Throwable e) {
                // 如果HttpComponents不可用，回退到SimpleClientHttpRequestFactory
                log.info("HttpComponents不可用({}), 使用SimpleClientHttpRequestFactory", e.getClass().getSimpleName());
                org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(properties.getEmbedding().getTimeout().intValue());
                factory.setReadTimeout(properties.getEmbedding().getTimeout().intValue());
                this.restTemplate = new RestTemplate(factory);
            }
        } catch (Exception e) {
            log.warn("无法配置Embedding RestTemplate超时，将使用默认RestTemplate", e);
            this.restTemplate = new RestTemplate();
        }

        log.info("初始化查询理解Embedding服务: enabled={}, model={}",
            properties.getEnabled(), properties.getEmbedding().getModel());
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        if (!isServiceAvailable() || !StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        // 尝试从缓存获取
        if (cacheService != null) {
            try {
                String cachedEmbedding = cacheService.getCachedEmbedding(text);
                if (cachedEmbedding != null) {
                    Double[] embeddingArray = objectMapper.readValue(cachedEmbedding, Double[].class);
                    log.debug("从缓存获取到embedding向量，维度: {}", embeddingArray.length);
                    return Arrays.asList(embeddingArray);
                }
            } catch (Exception e) {
                log.warn("解析缓存的embedding失败: {}", e.getMessage());
            }
        }

        try {
            List<Double> embedding = callEmbeddingApi(text);

            // 缓存结果
            if (cacheService != null && !embedding.isEmpty()) {
                String embeddingJson = objectMapper.writeValueAsString(embedding);
                cacheService.cacheEmbedding(text, embeddingJson);
            }

            return embedding;
        } catch (Exception e) {
            log.warn("生成embedding失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, List<Double>> generateEmbeddings(List<String> texts) {
        Map<String, List<Double>> result = new HashMap<>();

        if (texts == null || texts.isEmpty()) {
            return result;
        }

        for (String text : texts) {
            if (StringUtils.hasText(text)) {
                List<Double> embedding = generateEmbedding(text);
                if (!embedding.isEmpty()) {
                    result.put(text, embedding);
                }
            }
        }

        return result;
    }

    @Override
    public double calculateSimilarity(String text1, String text2) {
        if (!isServiceAvailable() || !StringUtils.hasText(text1) || !StringUtils.hasText(text2)) {
            return 0.0;
        }

        try {
            List<Double> embedding1 = generateEmbedding(text1);
            List<Double> embedding2 = generateEmbedding(text2);

            if (embedding1.isEmpty() || embedding2.isEmpty()) {
                return 0.0;
            }

            return cosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            log.warn("计算文本相似度失败: {}", e.getMessage());
            return 0.0;
        }
    }

    @Override
    public double cosineSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1 == null || embedding2 == null ||
            embedding1.isEmpty() || embedding2.isEmpty() ||
            embedding1.size() != embedding2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            double v1 = embedding1.get(i);
            double v2 = embedding2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public boolean isServiceAvailable() {
        return properties.getEnabled() != null && properties.getEnabled() &&
            StringUtils.hasText(getApiKey());
    }

    /**
     * 调用Embedding API
     *
     * @param text 输入文本
     * @return embedding向量
     */
    private List<Double> callEmbeddingApi(String text) throws Exception {
        // 构建请求
        EmbeddingApiRequest request = new EmbeddingApiRequest();
        request.setModel(properties.getEmbedding().getModel());
        request.setInput(text);
        request.setEncodingFormat(properties.getEmbedding().getEncodingFormat());

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getApiKey());

        // 发送请求
        HttpEntity<EmbeddingApiRequest> requestEntity = new HttpEntity<>(request, headers);
        ResponseEntity<EmbeddingApiResponse> responseEntity = restTemplate.exchange(
            properties.getEmbedding().getApiUrl(),
            HttpMethod.POST,
            requestEntity,
            EmbeddingApiResponse.class
        );

        EmbeddingApiResponse response = responseEntity.getBody();
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new RuntimeException("Embedding API 返回空响应");
        }

        EmbeddingApiResponse.EmbeddingData embeddingData = response.getData().get(0);
        if (embeddingData.getEmbedding() == null || embeddingData.getEmbedding().isEmpty()) {
            throw new RuntimeException("Embedding API 响应中没有向量数据");
        }

        log.debug("生成embedding向量成功，维度: {}", embeddingData.getEmbedding().size());
        return embeddingData.getEmbedding();
    }

    /**
     * 获取API Key
     * 优先使用embedding配置的apiKey，如果为空则使用siliconflow的apiKey
     */
    private String getApiKey() {
        String apiKey = properties.getEmbedding().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            apiKey = properties.getSiliconflow().getApiKey();
        }
        return apiKey;
    }
}
