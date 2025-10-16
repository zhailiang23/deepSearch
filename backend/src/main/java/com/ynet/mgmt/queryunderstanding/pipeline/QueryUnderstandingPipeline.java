package com.ynet.mgmt.queryunderstanding.pipeline;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.QueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 查询理解管道
 * 采用责任链模式，按优先级顺序执行所有已注册的处理器
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class QueryUnderstandingPipeline {

    /**
     * 注册的处理器列表（按优先级排序）
     */
    private final List<QueryProcessor> processors = new ArrayList<>();

    /**
     * 管道总超时时间（毫秒）
     */
    private long totalTimeout = 5000L;

    /**
     * 简单查询阈值（字符数）
     */
    private int simpleQueryThreshold = 2;

    /**
     * 是否启用智能跳过
     */
    private boolean smartSkipEnabled = true;

    /**
     * 注册处理器
     *
     * @param processor 查询处理器
     */
    public void registerProcessor(QueryProcessor processor) {
        processors.add(processor);
        // 按优先级降序排列（优先级高的先执行）
        processors.sort(Comparator.comparingInt(QueryProcessor::getPriority).reversed());
        log.info("注册处理器: {}, 优先级: {}", processor.getName(), processor.getPriority());
    }

    /**
     * 注册多个处理器
     *
     * @param processorList 处理器列表
     */
    public void registerProcessors(List<QueryProcessor> processorList) {
        processorList.forEach(this::registerProcessor);
    }

    /**
     * 执行查询理解管道
     *
     * @param query 原始查询
     * @return 处理后的查询上下文
     */
    public QueryContext execute(String query) {
        log.info("开始执行查询理解管道，原始查询: {}", query);

        // 创建查询上下文
        QueryContext context = new QueryContext(query);

        // 智能跳过策略：检查是否为简单查询
        if (smartSkipEnabled && isSimpleQuery(query)) {
            log.debug("检测到简单查询，启用智能跳过策略");
            context.setSkipComplexProcessing(true);
        }

        // 按优先级执行所有处理器
        for (QueryProcessor processor : processors) {
            // 检查是否超过总超时时间
            if (context.getTotalProcessingTime() > totalTimeout) {
                log.warn("管道执行超时，已执行时间: {} ms，超时阈值: {} ms",
                        context.getTotalProcessingTime(), totalTimeout);
                break;
            }

            processor.process(context);
        }

        long totalTime = context.getTotalProcessingTime();
        log.info("查询理解管道执行完成，总耗时: {} ms，处理后查询: {}",
                totalTime, context.getCurrentQuery());

        return context;
    }

    /**
     * 判断是否为简单查询
     *
     * @param query 查询文本
     * @return true 表示简单查询
     */
    private boolean isSimpleQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        // 去除空白后字符数少于阈值视为简单查询
        return query.trim().length() < simpleQueryThreshold;
    }

    /**
     * 获取已注册的处理器列表
     *
     * @return 处理器列表
     */
    public List<QueryProcessor> getProcessors() {
        return new ArrayList<>(processors);
    }

    /**
     * 清空所有处理器
     */
    public void clearProcessors() {
        processors.clear();
        log.info("已清空所有处理器");
    }

    /**
     * 设置总超时时间
     *
     * @param totalTimeout 超时时间（毫秒）
     */
    public void setTotalTimeout(long totalTimeout) {
        this.totalTimeout = totalTimeout;
    }

    /**
     * 设置简单查询阈值
     *
     * @param simpleQueryThreshold 阈值（字符数）
     */
    public void setSimpleQueryThreshold(int simpleQueryThreshold) {
        this.simpleQueryThreshold = simpleQueryThreshold;
    }

    /**
     * 设置是否启用智能跳过
     *
     * @param smartSkipEnabled true 表示启用
     */
    public void setSmartSkipEnabled(boolean smartSkipEnabled) {
        this.smartSkipEnabled = smartSkipEnabled;
    }

    /**
     * 获取处理器数量
     *
     * @return 处理器数量
     */
    public int getProcessorCount() {
        return processors.size();
    }
}
