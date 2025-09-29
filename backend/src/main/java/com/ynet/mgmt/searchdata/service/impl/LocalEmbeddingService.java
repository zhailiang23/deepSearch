package com.ynet.mgmt.searchdata.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchdata.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地嵌入服务实现
 * 基于本地部署的sentence-transformers模型提供文本向量化服务
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service("localEmbeddingService")
@ConditionalOnProperty(name = "semantic.embedding.provider", havingValue = "local")
public class LocalEmbeddingService implements EmbeddingService {

    /**
     * 本地模型名称
     */
    private static final String MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2";

    /**
     * 向量维度（MiniLM模型为384维）
     */
    private static final int VECTOR_DIMENSION = 384;

    @Value("${semantic.embedding.local.service.url:http://localhost:8001}")
    private String embeddingServiceUrl;

    @Value("${semantic.embedding.enabled:true}")
    private boolean semanticEnabled;

    @Value("${semantic.embedding.timeout:5000}")
    private int connectionTimeout;

    @Value("${semantic.embedding.cache.expire.hours:24}")
    private int cacheExpireHours;

    @Value("${semantic.embedding.cache.max.size:1000}")
    private int maxCacheSize;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 向量缓存，避免重复计算相同文本的向量
     */
    private final Map<String, CachedVector> vectorCache = new ConcurrentHashMap<>();

    /**
     * 服务可用性状态缓存
     */
    private Boolean serviceAvailable = null;
    private LocalDateTime lastHealthCheck = null;
    private final Duration healthCheckInterval = Duration.ofMinutes(5);

    /**
     * 缓存的向量数据
     */
    private static class CachedVector {
        private final List<Float> vector;
        private final LocalDateTime createdAt;

        public CachedVector(List<Float> vector) {
            this.vector = vector;
            this.createdAt = LocalDateTime.now();
        }

        public List<Float> getVector() { return vector; }
        public LocalDateTime getCreatedAt() { return createdAt; }

        public boolean isExpired(Duration expireDuration) {
            return LocalDateTime.now().isAfter(createdAt.plus(expireDuration));
        }
    }

    @PostConstruct
    public void init() {
        log.info("初始化本地嵌入服务: url={}, model={}, dimension={}, enabled={}",
                embeddingServiceUrl, MODEL_NAME, VECTOR_DIMENSION, semanticEnabled);

        if (semanticEnabled) {
            // 异步检查服务可用性
            checkServiceAvailabilityAsync();
        }
    }

    @Override
    public List<Float> getTextEmbedding(String text) {
        if (!semanticEnabled) {
            log.debug("语义搜索功能已禁用");
            return null;
        }

        if (text == null || text.trim().isEmpty()) {
            log.warn("文本为空，无法生成向量");
            return null;
        }

        // 规范化文本作为缓存键
        String normalizedText = normalizeText(text);

        // 检查缓存
        CachedVector cached = vectorCache.get(normalizedText);
        if (cached != null && !cached.isExpired(Duration.ofHours(cacheExpireHours))) {
            log.debug("从缓存获取向量: text={}", normalizedText.substring(0, Math.min(50, normalizedText.length())));
            return cached.getVector();
        }

        // 检查服务可用性
        if (!isServiceAvailable()) {
            log.warn("本地向量服务不可用，跳过向量生成: text={}", normalizedText.substring(0, Math.min(50, normalizedText.length())));
            return null;
        }

        try {
            List<Float> vector = callLocalEmbeddingService(normalizedText);
            if (vector != null && vector.size() == VECTOR_DIMENSION) {
                // 缓存向量结果
                if (vectorCache.size() >= maxCacheSize) {
                    clearOldCache();
                }
                vectorCache.put(normalizedText, new CachedVector(vector));
                log.debug("向量生成成功: text={}, dimension={}",
                    normalizedText.substring(0, Math.min(50, normalizedText.length())), vector.size());
                return vector;
            } else {
                log.warn("向量生成失败或维度不匹配: expected={}, actual={}",
                    VECTOR_DIMENSION, vector != null ? vector.size() : 0);
                return null;
            }

        } catch (Exception e) {
            log.error("调用本地嵌入服务失败: text={}", normalizedText.substring(0, Math.min(50, normalizedText.length())), e);
            return null;
        }
    }

    @Override
    public Map<String, List<Float>> getBatchTextEmbeddings(List<String> texts) {
        if (!semanticEnabled || texts == null || texts.isEmpty()) {
            return null;
        }

        // 规范化文本
        List<String> normalizedTexts = texts.stream()
                .filter(Objects::nonNull)
                .filter(text -> !text.trim().isEmpty())
                .map(this::normalizeText)
                .distinct()
                .toList();

        if (normalizedTexts.isEmpty()) {
            return null;
        }

        // 检查服务可用性
        if (!isServiceAvailable()) {
            log.warn("本地向量服务不可用，跳过批量向量生成");
            return null;
        }

        try {
            return callLocalBatchEmbeddingService(normalizedTexts);
        } catch (Exception e) {
            log.error("批量向量生成失败", e);
            return null;
        }
    }

    @Override
    public double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);
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
        // 如果最近检查过且在缓存时间内，返回缓存结果
        if (serviceAvailable != null && lastHealthCheck != null &&
            LocalDateTime.now().isBefore(lastHealthCheck.plus(healthCheckInterval))) {
            return serviceAvailable;
        }

        // 执行健康检查
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                embeddingServiceUrl + "/health", JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode body = response.getBody();
                serviceAvailable = "healthy".equals(body.path("status").asText()) &&
                                 body.path("model_loaded").asBoolean();
            } else {
                serviceAvailable = false;
            }

            lastHealthCheck = LocalDateTime.now();
            log.info("本地嵌入服务健康检查: available={}", serviceAvailable);
            return serviceAvailable;

        } catch (Exception e) {
            serviceAvailable = false;
            lastHealthCheck = LocalDateTime.now();
            log.warn("本地嵌入服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public int getVectorDimension() {
        return VECTOR_DIMENSION;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public String getServiceType() {
        return "local";
    }

    /**
     * 调用本地嵌入服务 - 单个文本
     */
    private List<Float> callLocalEmbeddingService(String text) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", text);
        requestBody.put("model", MODEL_NAME);
        requestBody.put("normalize", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
            embeddingServiceUrl + "/embeddings", request, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode embeddingNode = response.getBody().get("embedding");
            if (embeddingNode != null && embeddingNode.isArray()) {
                List<Float> vector = new ArrayList<>();
                for (JsonNode valueNode : embeddingNode) {
                    vector.add(valueNode.floatValue());
                }
                return vector;
            }
        }

        throw new RuntimeException("本地嵌入服务调用失败: " + response.getStatusCode());
    }

    /**
     * 调用本地嵌入服务 - 批量文本
     */
    private Map<String, List<Float>> callLocalBatchEmbeddingService(List<String> texts) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("texts", texts);
        requestBody.put("model", MODEL_NAME);
        requestBody.put("normalize", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
            embeddingServiceUrl + "/embeddings/batch", request, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode embeddingsNode = response.getBody().get("embeddings");
            if (embeddingsNode != null && embeddingsNode.isArray()) {
                Map<String, List<Float>> result = new HashMap<>();
                for (int i = 0; i < embeddingsNode.size() && i < texts.size(); i++) {
                    JsonNode embeddingArray = embeddingsNode.get(i);
                    if (embeddingArray.isArray()) {
                        List<Float> vector = new ArrayList<>();
                        for (JsonNode valueNode : embeddingArray) {
                            vector.add(valueNode.floatValue());
                        }
                        result.put(texts.get(i), vector);
                    }
                }
                return result;
            }
        }

        throw new RuntimeException("本地批量嵌入服务调用失败: " + response.getStatusCode());
    }

    /**
     * 规范化文本
     */
    private String normalizeText(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    /**
     * 清理旧缓存
     */
    private void clearOldCache() {
        Duration expireDuration = Duration.ofHours(cacheExpireHours);
        vectorCache.entrySet().removeIf(entry ->
            entry.getValue().isExpired(expireDuration));

        // 如果清理后仍然太多，随机移除一些
        if (vectorCache.size() >= maxCacheSize) {
            List<String> keys = new ArrayList<>(vectorCache.keySet());
            Collections.shuffle(keys);
            for (int i = 0; i < keys.size() / 2; i++) {
                vectorCache.remove(keys.get(i));
            }
        }
    }

    /**
     * 异步检查服务可用性
     */
    private void checkServiceAvailabilityAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 延迟1秒再检查
                isServiceAvailable();
            } catch (Exception e) {
                log.warn("异步健康检查失败", e);
            }
        }).start();
    }
}