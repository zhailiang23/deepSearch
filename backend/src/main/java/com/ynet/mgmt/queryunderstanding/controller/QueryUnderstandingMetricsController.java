package com.ynet.mgmt.queryunderstanding.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.queryunderstanding.metrics.QueryUnderstandingMetrics;
import com.ynet.mgmt.queryunderstanding.pipeline.QueryUnderstandingPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询理解指标Controller
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@RestController
@RequestMapping("/query-understanding/metrics")
public class QueryUnderstandingMetricsController {

    @Autowired(required = false)
    private QueryUnderstandingMetrics metrics;

    @Autowired(required = false)
    private QueryUnderstandingPipeline pipeline;

    /**
     * 获取指标摘要
     *
     * @return 指标摘要
     */
    @GetMapping("/summary")
    public ApiResponse<QueryUnderstandingMetrics.MetricsSummary> getSummary() {
        if (metrics == null) {
            return ApiResponse.error("查询理解指标服务未启用");
        }

        try {
            QueryUnderstandingMetrics.MetricsSummary summary = metrics.getSummary();
            return ApiResponse.success(summary);
        } catch (Exception e) {
            log.error("获取指标摘要失败", e);
            return ApiResponse.error("获取指标摘要失败: " + e.getMessage());
        }
    }

    /**
     * 获取处理器指标
     *
     * @return 处理器指标列表
     */
    @GetMapping("/processors")
    public ApiResponse<Map<String, Map<String, Object>>> getProcessorMetrics() {
        if (metrics == null || pipeline == null) {
            return ApiResponse.error("查询理解服务未启用");
        }

        try {
            Map<String, Map<String, Object>> processorMetrics = new HashMap<>();

            pipeline.getProcessors().forEach(processor -> {
                String name = processor.getName();
                Map<String, Object> metric = new HashMap<>();
                metric.put("name", name);
                metric.put("priority", processor.getPriority());
                metric.put("enabled", processor.isEnabled());
                metric.put("averageTime", metrics.getAverageProcessorTime(name));
                metric.put("errorRate", metrics.getProcessorErrorRate(name));
                processorMetrics.put(name, metric);
            });

            return ApiResponse.success(processorMetrics);
        } catch (Exception e) {
            log.error("获取处理器指标失败", e);
            return ApiResponse.error("获取处理器指标失败: " + e.getMessage());
        }
    }

    /**
     * 重置指标
     *
     * @return 操作结果
     */
    @PostMapping("/reset")
    public ApiResponse<String> resetMetrics() {
        if (metrics == null) {
            return ApiResponse.error("查询理解指标服务未启用");
        }

        try {
            metrics.reset();
            log.info("查询理解指标已重置");
            return ApiResponse.success("指标已重置");
        } catch (Exception e) {
            log.error("重置指标失败", e);
            return ApiResponse.error("重置指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取管道配置
     *
     * @return 管道配置
     */
    @GetMapping("/pipeline/config")
    public ApiResponse<Map<String, Object>> getPipelineConfig() {
        if (pipeline == null) {
            return ApiResponse.error("查询理解管道未启用");
        }

        try {
            Map<String, Object> config = new HashMap<>();
            config.put("processorCount", pipeline.getProcessorCount());
            config.put("processors", pipeline.getProcessors().stream()
                    .map(p -> Map.of(
                            "name", p.getName(),
                            "priority", p.getPriority(),
                            "enabled", p.isEnabled()
                    ))
                    .toList()
            );
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取管道配置失败", e);
            return ApiResponse.error("获取管道配置失败: " + e.getMessage());
        }
    }
}
