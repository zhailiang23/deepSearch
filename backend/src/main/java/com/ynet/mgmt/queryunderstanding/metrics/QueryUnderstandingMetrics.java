package com.ynet.mgmt.queryunderstanding.metrics;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 查询理解管道性能指标
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
@Component
public class QueryUnderstandingMetrics {

    /**
     * 总查询数
     */
    private final LongAdder totalQueries = new LongAdder();

    /**
     * 缓存命中数
     */
    private final LongAdder cacheHits = new LongAdder();

    /**
     * 缓存未命中数
     */
    private final LongAdder cacheMisses = new LongAdder();

    /**
     * 简单查询数（触发智能跳过）
     */
    private final LongAdder simpleQueries = new LongAdder();

    /**
     * 复杂查询数
     */
    private final LongAdder complexQueries = new LongAdder();

    /**
     * 超时查询数
     */
    private final LongAdder timeoutQueries = new LongAdder();

    /**
     * 错误查询数
     */
    private final LongAdder errorQueries = new LongAdder();

    /**
     * 各处理器执行次数
     */
    private final Map<String, LongAdder> processorExecutionCount = new ConcurrentHashMap<>();

    /**
     * 各处理器总耗时（毫秒）
     */
    private final Map<String, LongAdder> processorTotalTime = new ConcurrentHashMap<>();

    /**
     * 各处理器错误次数
     */
    private final Map<String, LongAdder> processorErrorCount = new ConcurrentHashMap<>();

    /**
     * 管道总耗时（毫秒）
     */
    private final LongAdder pipelineTotalTime = new LongAdder();

    /**
     * 最近一次查询时间
     */
    private final AtomicLong lastQueryTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 记录查询
     */
    public void recordQuery() {
        totalQueries.increment();
        lastQueryTime.set(System.currentTimeMillis());
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    /**
     * 记录简单查询
     */
    public void recordSimpleQuery() {
        simpleQueries.increment();
    }

    /**
     * 记录复杂查询
     */
    public void recordComplexQuery() {
        complexQueries.increment();
    }

    /**
     * 记录超时查询
     */
    public void recordTimeout() {
        timeoutQueries.increment();
    }

    /**
     * 记录错误查询
     */
    public void recordError() {
        errorQueries.increment();
    }

    /**
     * 记录处理器执行
     *
     * @param processorName 处理器名称
     * @param duration 耗时（毫秒）
     */
    public void recordProcessorExecution(String processorName, long duration) {
        processorExecutionCount.computeIfAbsent(processorName, k -> new LongAdder()).increment();
        processorTotalTime.computeIfAbsent(processorName, k -> new LongAdder()).add(duration);
    }

    /**
     * 记录处理器错误
     *
     * @param processorName 处理器名称
     */
    public void recordProcessorError(String processorName) {
        processorErrorCount.computeIfAbsent(processorName, k -> new LongAdder()).increment();
    }

    /**
     * 记录管道总耗时
     *
     * @param duration 耗时（毫秒）
     */
    public void recordPipelineExecution(long duration) {
        pipelineTotalTime.add(duration);
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率（0-1）
     */
    public double getCacheHitRate() {
        long hits = cacheHits.sum();
        long total = hits + cacheMisses.sum();
        return total == 0 ? 0.0 : (double) hits / total;
    }

    /**
     * 获取平均管道耗时
     *
     * @return 平均耗时（毫秒）
     */
    public double getAveragePipelineTime() {
        long total = totalQueries.sum();
        return total == 0 ? 0.0 : (double) pipelineTotalTime.sum() / total;
    }

    /**
     * 获取处理器平均耗时
     *
     * @param processorName 处理器名称
     * @return 平均耗时（毫秒）
     */
    public double getAverageProcessorTime(String processorName) {
        LongAdder count = processorExecutionCount.get(processorName);
        LongAdder time = processorTotalTime.get(processorName);

        if (count == null || time == null) {
            return 0.0;
        }

        long totalCount = count.sum();
        return totalCount == 0 ? 0.0 : (double) time.sum() / totalCount;
    }

    /**
     * 获取处理器错误率
     *
     * @param processorName 处理器名称
     * @return 错误率（0-1）
     */
    public double getProcessorErrorRate(String processorName) {
        LongAdder count = processorExecutionCount.get(processorName);
        LongAdder errors = processorErrorCount.get(processorName);

        if (count == null || errors == null) {
            return 0.0;
        }

        long totalCount = count.sum();
        return totalCount == 0 ? 0.0 : (double) errors.sum() / totalCount;
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalQueries.reset();
        cacheHits.reset();
        cacheMisses.reset();
        simpleQueries.reset();
        complexQueries.reset();
        timeoutQueries.reset();
        errorQueries.reset();
        pipelineTotalTime.reset();
        processorExecutionCount.clear();
        processorTotalTime.clear();
        processorErrorCount.clear();
    }

    /**
     * 获取指标摘要
     *
     * @return 指标摘要
     */
    public MetricsSummary getSummary() {
        MetricsSummary summary = new MetricsSummary();
        summary.setTotalQueries(totalQueries.sum());
        summary.setCacheHits(cacheHits.sum());
        summary.setCacheMisses(cacheMisses.sum());
        summary.setCacheHitRate(getCacheHitRate());
        summary.setSimpleQueries(simpleQueries.sum());
        summary.setComplexQueries(complexQueries.sum());
        summary.setTimeoutQueries(timeoutQueries.sum());
        summary.setErrorQueries(errorQueries.sum());
        summary.setAveragePipelineTime(getAveragePipelineTime());
        summary.setLastQueryTime(lastQueryTime.get());
        return summary;
    }

    /**
     * 指标摘要
     */
    @Data
    public static class MetricsSummary {
        private long totalQueries;
        private long cacheHits;
        private long cacheMisses;
        private double cacheHitRate;
        private long simpleQueries;
        private long complexQueries;
        private long timeoutQueries;
        private long errorQueries;
        private double averagePipelineTime;
        private long lastQueryTime;
    }
}
