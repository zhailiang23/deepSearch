package com.ynet.mgmt.clustering.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.clustering.config.ClusterProperties;
import com.ynet.mgmt.clustering.dto.ClusterAnalysisResponse;
import com.ynet.mgmt.clustering.dto.ClusterTopicDTO;
import com.ynet.mgmt.clustering.dto.PythonClusterRequest;
import com.ynet.mgmt.clustering.dto.ScatterPointDTO;
import com.ynet.mgmt.clustering.exception.ClusterAnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python 聚类服务客户端
 * 负责与 Python FastAPI 服务通信
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class PythonClusterClient {

    private static final Logger logger = LoggerFactory.getLogger(PythonClusterClient.class);

    private final RestTemplate restTemplate;
    private final ClusterProperties clusterProperties;
    private final ObjectMapper objectMapper;

    public PythonClusterClient(RestTemplateBuilder restTemplateBuilder,
                               ClusterProperties clusterProperties,
                               ObjectMapper objectMapper) {
        this.clusterProperties = clusterProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(clusterProperties.getPython().getTimeout()))
                .setReadTimeout(Duration.ofMillis(clusterProperties.getPython().getTimeout()))
                .build();
    }

    /**
     * 执行聚类分析
     *
     * @param texts      待聚类文本列表
     * @param eps        DBSCAN eps 参数
     * @param minSamples DBSCAN min_samples 参数
     * @param metric     距离度量方式
     * @return 聚类分析响应
     */
    public ClusterAnalysisResponse performClustering(List<String> texts, Double eps,
                                                     Integer minSamples, String metric) {
        try {
            // 检查文本数量限制
            if (texts.size() > clusterProperties.getPython().getMaxTexts()) {
                throw new ClusterAnalysisException(
                        String.format("文本数量 %d 超过最大限制 %d",
                                texts.size(), clusterProperties.getPython().getMaxTexts()));
            }

            // 构建请求
            PythonClusterRequest request = PythonClusterRequest.builder()
                    .texts(texts)
                    .eps(eps)
                    .minSamples(minSamples)
                    .metric(metric)
                    .siliconflowEmbedding(buildEmbeddingConfig())
                    .siliconflowLlm(buildLlmConfig())
                    .build();

            // 发送 HTTP 请求
            String url = clusterProperties.getPython().getServiceUrl() + "/api/cluster";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PythonClusterRequest> httpEntity = new HttpEntity<>(request, headers);

            logger.info("调用 Python 聚类服务: url={}, 文本数={}, eps={}, minSamples={}, metric={}",
                    url, texts.size(), eps, minSamples, metric);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, httpEntity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ClusterAnalysisException("Python 聚类服务返回异常: " + response.getStatusCode());
            }

            // 解析响应
            return parseResponse(response.getBody());

        } catch (RestClientException e) {
            logger.error("调用 Python 聚类服务失败", e);
            throw new ClusterAnalysisException("调用 Python 聚类服务失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("聚类分析失败", e);
            throw new ClusterAnalysisException("聚类分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 Embedding 配置
     */
    private Map<String, Object> buildEmbeddingConfig() {
        Map<String, Object> config = new HashMap<>();
        ClusterProperties.EmbeddingConfig embeddingConfig = clusterProperties.getSiliconflow().getEmbedding();
        config.put("api_key", embeddingConfig.getApiKey());
        config.put("api_url", embeddingConfig.getApiUrl());
        config.put("model", embeddingConfig.getModel());
        config.put("timeout", embeddingConfig.getTimeout());
        return config;
    }

    /**
     * 构建 LLM 配置
     */
    private Map<String, Object> buildLlmConfig() {
        Map<String, Object> config = new HashMap<>();
        ClusterProperties.LlmConfig llmConfig = clusterProperties.getSiliconflow().getLlm();
        config.put("api_key", llmConfig.getApiKey());
        config.put("api_url", llmConfig.getApiUrl());
        config.put("model", llmConfig.getModel());
        config.put("temperature", llmConfig.getTemperature());
        config.put("max_tokens", llmConfig.getMaxTokens());
        config.put("timeout", llmConfig.getTimeout());
        return config;
    }

    /**
     * 解析 Python 服务响应
     */
    @SuppressWarnings("unchecked")
    private ClusterAnalysisResponse parseResponse(Map<String, Object> responseBody) {
        try {
            ClusterAnalysisResponse response = new ClusterAnalysisResponse();

            // 解析聚类话题列表
            List<Map<String, Object>> clustersData = (List<Map<String, Object>>) responseBody.get("clusters");
            if (clustersData != null) {
                List<ClusterTopicDTO> clusters = clustersData.stream()
                        .map(this::parseClusterTopic)
                        .toList();
                response.setClusters(clusters);
            }

            // 解析散点图数据
            List<Map<String, Object>> scatterData = (List<Map<String, Object>>) responseBody.get("scatter_data");
            if (scatterData != null) {
                List<ScatterPointDTO> scatterPoints = scatterData.stream()
                        .map(this::parseScatterPoint)
                        .toList();
                response.setScatterData(scatterPoints);
            }

            // 解析统计数据
            response.setNoiseCount((Integer) responseBody.get("noise_count"));
            response.setTotalTexts((Integer) responseBody.get("total_texts"));
            response.setValidClusters((Integer) responseBody.get("valid_clusters"));

            return response;
        } catch (Exception e) {
            logger.error("解析 Python 响应失败", e);
            throw new ClusterAnalysisException("解析 Python 响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析聚类话题
     */
    @SuppressWarnings("unchecked")
    private ClusterTopicDTO parseClusterTopic(Map<String, Object> data) {
        ClusterTopicDTO dto = new ClusterTopicDTO();
        dto.setClusterId((Integer) data.get("cluster_id"));
        dto.setTopic((String) data.get("topic"));
        dto.setTags((List<String>) data.get("tags"));
        dto.setExamples((List<String>) data.get("examples"));
        dto.setSize((Integer) data.get("size"));
        return dto;
    }

    /**
     * 解析散点数据
     */
    private ScatterPointDTO parseScatterPoint(Map<String, Object> data) {
        ScatterPointDTO dto = new ScatterPointDTO();
        dto.setX(((Number) data.get("x")).doubleValue());
        dto.setY(((Number) data.get("y")).doubleValue());
        dto.setCluster((Integer) data.get("cluster"));
        dto.setText((String) data.get("text"));
        return dto;
    }
}
