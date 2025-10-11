package com.ynet.mgmt.searchdata.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchdata.dto.SearchDataResponse;
import com.ynet.mgmt.searchdata.service.RerankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硅基流动重排序服务实现
 * 基于硅基流动Rerank API提供语义重排序服务
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service("siliconFlowRerankService")
@ConditionalOnProperty(name = "rerank.provider", havingValue = "siliconflow", matchIfMissing = true)
public class SiliconFlowRerankService implements RerankService {

    /**
     * 硅基流动Rerank API端点
     */
    private static final String API_ENDPOINT = "https://api.siliconflow.cn/v1/rerank";

    /**
     * 模型名称
     */
    private static final String MODEL_NAME = "BAAI/bge-reranker-v2-m3";

    @Value("${rerank.siliconflow.api-key:${semantic.embedding.siliconflow.api-key}}")
    private String apiKey;

    @Value("${rerank.enabled:true}")
    private boolean rerankEnabled;

    @Value("${rerank.siliconflow.timeout:5000}")
    private int connectionTimeout;

    @Value("${rerank.siliconflow.default-top-n:50}")
    private int defaultTopN;

    @Value("${rerank.siliconflow.return-documents:true}")
    private boolean returnDocuments;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 服务可用性状态缓存
     */
    private Boolean serviceAvailable = null;
    private LocalDateTime lastHealthCheck = null;
    private final Duration healthCheckInterval = Duration.ofMinutes(5);

    @PostConstruct
    public void init() {
        // 初始化RestTemplate并配置超时
        try {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connectionTimeout);
            factory.setReadTimeout(connectionTimeout);
            this.restTemplate = new RestTemplate(factory);
        } catch (Exception e) {
            log.warn("无法配置RestTemplate超时，将使用默认RestTemplate", e);
            this.restTemplate = new RestTemplate();
        }

        log.info("初始化硅基流动重排序服务: model={}, enabled={}, timeout={}ms, defaultTopN={}",
                MODEL_NAME, rerankEnabled, connectionTimeout, defaultTopN);

        if (rerankEnabled) {
            // 异步检查服务可用性
            checkServiceAvailabilityAsync();
        }
    }

    @Override
    public List<SearchDataResponse.DocumentData> rerankDocuments(
            String query,
            List<SearchDataResponse.DocumentData> documents,
            Integer topN
    ) {
        if (!rerankEnabled) {
            log.debug("重排序功能已禁用");
            return documents;
        }

        if (query == null || query.trim().isEmpty()) {
            log.warn("查询文本为空，无法重排序");
            return documents;
        }

        if (documents == null || documents.isEmpty()) {
            log.debug("文档列表为空，无需重排序");
            return documents;
        }

        // 检查服务可用性
        if (!isServiceAvailable()) {
            log.warn("硅基流动重排序服务不可用，返回原始结果");
            return documents;
        }

        try {
            long startTime = System.currentTimeMillis();

            // 确定重排序数量
            int actualTopN = topN != null ? topN : defaultTopN;
            // 只对前actualTopN条结果进行重排序（节省成本）
            int documentsToRerank = Math.min(documents.size(), actualTopN * 2); // 重排序2倍数量以保证top-N质量

            List<SearchDataResponse.DocumentData> toRerank = documents.subList(0, documentsToRerank);
            List<SearchDataResponse.DocumentData> remaining = documents.size() > documentsToRerank ?
                    documents.subList(documentsToRerank, documents.size()) : new ArrayList<>();

            // 调用Rerank API
            List<RerankResult> rerankResults = callSiliconFlowRerankAPI(query, toRerank, actualTopN);

            if (rerankResults == null || rerankResults.isEmpty()) {
                log.warn("重排序返回空结果，返回原始顺序");
                return documents;
            }

            // 根据重排序结果重新组织文档
            List<SearchDataResponse.DocumentData> rerankedDocuments = new ArrayList<>();

            // 添加重排序后的top-N文档
            for (RerankResult result : rerankResults) {
                if (result.index < toRerank.size()) {
                    SearchDataResponse.DocumentData doc = toRerank.get(result.index);
                    // 更新文档评分为rerank评分
                    doc.set_score(result.relevanceScore);
                    rerankedDocuments.add(doc);
                }
            }

            // 添加剩余文档（保持原顺序）
            rerankedDocuments.addAll(remaining);

            long duration = System.currentTimeMillis() - startTime;
            log.info("重排序完成: query={}, original={}, reranked={}, topN={}, took={}ms",
                    query.substring(0, Math.min(50, query.length())),
                    documents.size(),
                    rerankedDocuments.size(),
                    actualTopN,
                    duration);

            return rerankedDocuments;

        } catch (Exception e) {
            log.error("重排序失败，返回原始结果: query={}, error={}",
                    query.substring(0, Math.min(50, query.length())), e.getMessage(), e);
            return documents;
        }
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
            // 使用简单的测试查询
            List<String> testDocs = Arrays.asList("测试文档1", "测试文档2");
            List<RerankResult> results = callSiliconFlowRerankAPI("测试", testDocs, 2);
            serviceAvailable = results != null && !results.isEmpty();
            lastHealthCheck = LocalDateTime.now();

            log.info("硅基流动重排序服务健康检查: available={}", serviceAvailable);
            return serviceAvailable;

        } catch (Exception e) {
            serviceAvailable = false;
            lastHealthCheck = LocalDateTime.now();
            log.warn("硅基流动重排序服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public String getServiceType() {
        return "remote";
    }

    /**
     * 调用硅基流动Rerank API
     *
     * @param query 查询文本
     * @param documents 文档列表（可以是字符串或DocumentData）
     * @param topN 返回前N条
     * @return 重排序结果列表
     */
    private <T> List<RerankResult> callSiliconFlowRerankAPI(String query, List<T> documents, int topN) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        // 提取文档文本
        List<String> documentTexts = documents.stream()
                .map(this::extractDocumentText)
                .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("query", query);
        requestBody.put("documents", documentTexts);
        requestBody.put("top_n", topN);
        requestBody.put("return_documents", returnDocuments);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(API_ENDPOINT, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseRerankResponse(response.getBody());
        } else {
            throw new RuntimeException("硅基流动Rerank API调用失败: " + response.getStatusCode());
        }
    }

    /**
     * 从文档对象中提取文本内容
     */
    private <T> String extractDocumentText(T document) {
        if (document instanceof String) {
            return (String) document;
        } else if (document instanceof SearchDataResponse.DocumentData) {
            SearchDataResponse.DocumentData doc = (SearchDataResponse.DocumentData) document;
            Map<String, Object> source = doc.get_source();
            if (source == null) {
                return "";
            }

            // 尝试多种字段组合提取文本
            StringBuilder text = new StringBuilder();

            // 优先级：title > name > content > description
            if (source.containsKey("title")) {
                text.append(source.get("title")).append(" ");
            }
            if (source.containsKey("name")) {
                text.append(source.get("name")).append(" ");
            }
            if (source.containsKey("content")) {
                Object content = source.get("content");
                if (content != null) {
                    String contentStr = content.toString();
                    // 限制内容长度，避免超过API限制
                    if (contentStr.length() > 2000) {
                        contentStr = contentStr.substring(0, 2000);
                    }
                    text.append(contentStr).append(" ");
                }
            }
            if (source.containsKey("description")) {
                text.append(source.get("description")).append(" ");
            }

            String result = text.toString().trim();
            return result.isEmpty() ? doc.get_id() : result;
        }
        return document.toString();
    }

    /**
     * 解析重排序响应
     */
    private List<RerankResult> parseRerankResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode resultsNode = rootNode.get("results");

        if (resultsNode == null || !resultsNode.isArray()) {
            throw new RuntimeException("无效的Rerank API响应格式");
        }

        List<RerankResult> results = new ArrayList<>();

        for (JsonNode resultNode : resultsNode) {
            int index = resultNode.get("index").asInt();
            double relevanceScore = resultNode.get("relevance_score").asDouble();

            results.add(new RerankResult(index, relevanceScore));
        }

        return results;
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

    /**
     * 重排序结果
     */
    private static class RerankResult {
        private final int index;
        private final double relevanceScore;

        public RerankResult(int index, double relevanceScore) {
            this.index = index;
            this.relevanceScore = relevanceScore;
        }
    }
}
