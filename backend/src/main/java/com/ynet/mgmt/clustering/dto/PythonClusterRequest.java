package com.ynet.mgmt.clustering.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Python 聚类服务请求（内部使用）
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
public class PythonClusterRequest {

    /**
     * 待聚类的文本列表
     */
    private List<String> texts;

    /**
     * DBSCAN eps 参数
     */
    private Double eps;

    /**
     * DBSCAN min_samples 参数
     */
    @JsonProperty("min_samples")
    private Integer minSamples;

    /**
     * 距离度量方式
     */
    private String metric;

    /**
     * 硅基流动 Embedding 配置
     */
    @JsonProperty("siliconflow_embedding")
    private Map<String, Object> siliconflowEmbedding;

    /**
     * 硅基流动 LLM 配置
     */
    @JsonProperty("siliconflow_llm")
    private Map<String, Object> siliconflowLlm;
}
