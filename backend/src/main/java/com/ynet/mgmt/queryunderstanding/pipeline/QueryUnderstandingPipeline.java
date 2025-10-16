package com.ynet.mgmt.queryunderstanding.pipeline;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.metrics.QueryUnderstandingMetrics;
import com.ynet.mgmt.queryunderstanding.processor.QueryProcessor;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired(required = false)
    private QueryUnderstandingCacheService cacheService;

    @Autowired(required = false)
    private QueryUnderstandingMetrics metrics;

    @Value("${query-understanding.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${query-understanding.async.enabled:true}")
    private boolean asyncEnabled;

    @Value("${query-understanding.async.threads:4}")
    private int asyncThreads;

    /**
     * 注册的处理器列表（按优先级排序）
     */
    private final List<QueryProcessor> processors = new ArrayList<>();

    /**
     * 异步执行线程池
     */
    private ExecutorService executorService;

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
     * 初始化线程池
     */
    @PostConstruct
    public void init() {
        if (asyncEnabled) {
            executorService = Executors.newFixedThreadPool(asyncThreads);
            log.info("异步处理已启用，线程池大小: {}", asyncThreads);
        }
    }

    /**
     * 销毁线程池
     */
    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("线程池已关闭");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

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

        // 记录查询
        if (metrics != null) {
            metrics.recordQuery();
        }

        // 尝试从缓存获取
        if (cacheEnabled && cacheService != null) {
            QueryContext cachedContext = cacheService.getCachedQueryContext(query);
            if (cachedContext != null) {
                log.info("从缓存获取查询理解结果，查询: {}", query);
                if (metrics != null) {
                    metrics.recordCacheHit();
                }
                return cachedContext;
            }
            if (metrics != null) {
                metrics.recordCacheMiss();
            }
        }

        // 创建查询上下文
        QueryContext context = new QueryContext(query);

        // 智能跳过策略：检查是否为简单查询
        boolean isSimple = smartSkipEnabled && isSimpleQuery(query);
        if (isSimple) {
            log.debug("检测到简单查询，启用智能跳过策略");
            context.setSkipComplexProcessing(true);
            if (metrics != null) {
                metrics.recordSimpleQuery();
            }
        } else {
            if (metrics != null) {
                metrics.recordComplexQuery();
            }
        }

        try {
            // 根据是否启用异步执行不同的处理逻辑
            if (asyncEnabled && executorService != null) {
                executeWithAsync(context);
            } else {
                executeSequential(context);
            }

            long totalTime = context.getTotalProcessingTime();
            log.info("查询理解管道执行完成，总耗时: {} ms，处理后查询: {}",
                    totalTime, context.getCurrentQuery());

            // 记录管道执行时间
            if (metrics != null) {
                metrics.recordPipelineExecution(totalTime);
            }

            // 缓存结果
            if (cacheEnabled && cacheService != null) {
                cacheService.cacheQueryContext(query, context);
            }

            return context;
        } catch (Exception e) {
            log.error("查询理解管道执行失败: {}", e.getMessage(), e);
            if (metrics != null) {
                metrics.recordError();
            }
            throw e;
        }
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

    /**
     * 顺序执行所有处理器
     *
     * @param context 查询上下文
     */
    private void executeSequential(QueryContext context) {
        for (QueryProcessor processor : processors) {
            // 检查是否超过总超时时间
            if (context.getTotalProcessingTime() > totalTimeout) {
                log.warn("管道执行超时，已执行时间: {} ms，超时阈值: {} ms",
                        context.getTotalProcessingTime(), totalTimeout);
                if (metrics != null) {
                    metrics.recordTimeout();
                }
                break;
            }

            long startTime = System.currentTimeMillis();
            try {
                processor.process(context);
                long duration = System.currentTimeMillis() - startTime;
                if (metrics != null) {
                    metrics.recordProcessorExecution(processor.getName(), duration);
                }
            } catch (Exception e) {
                log.error("处理器 {} 执行失败: {}", processor.getName(), e.getMessage(), e);
                if (metrics != null) {
                    metrics.recordProcessorError(processor.getName());
                }
            }
        }
    }

    /**
     * 异步并行执行处理器
     * 将相同优先级的处理器分组并行执行，不同优先级的处理器按顺序执行
     *
     * @param context 查询上下文
     */
    private void executeWithAsync(QueryContext context) {
        // 按优先级分组处理器
        Map<Integer, List<QueryProcessor>> processorGroups = processors.stream()
                .collect(Collectors.groupingBy(QueryProcessor::getPriority));

        // 按优先级降序排列分组
        List<Integer> priorities = new ArrayList<>(processorGroups.keySet());
        priorities.sort(Collections.reverseOrder());

        // 按优先级组顺序执行
        for (Integer priority : priorities) {
            // 检查是否超过总超时时间
            if (context.getTotalProcessingTime() > totalTimeout) {
                log.warn("管道执行超时，已执行时间: {} ms，超时阈值: {} ms",
                        context.getTotalProcessingTime(), totalTimeout);
                if (metrics != null) {
                    metrics.recordTimeout();
                }
                break;
            }

            List<QueryProcessor> group = processorGroups.get(priority);

            if (group.size() == 1) {
                // 单个处理器直接执行
                QueryProcessor processor = group.get(0);
                executeProcessor(processor, context);
            } else {
                // 多个处理器并行执行
                List<CompletableFuture<Void>> futures = group.stream()
                        .map(processor -> CompletableFuture.runAsync(
                                () -> executeProcessor(processor, context),
                                executorService
                        ))
                        .collect(Collectors.toList());

                // 等待所有同优先级的处理器完成
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                } catch (Exception e) {
                    log.error("处理器组执行失败: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 执行单个处理器
     *
     * @param processor 处理器
     * @param context 查询上下文
     */
    private void executeProcessor(QueryProcessor processor, QueryContext context) {
        long startTime = System.currentTimeMillis();
        try {
            processor.process(context);
            long duration = System.currentTimeMillis() - startTime;
            if (metrics != null) {
                metrics.recordProcessorExecution(processor.getName(), duration);
            }
            log.debug("处理器 {} 执行完成，耗时: {} ms", processor.getName(), duration);
        } catch (Exception e) {
            log.error("处理器 {} 执行失败: {}", processor.getName(), e.getMessage(), e);
            if (metrics != null) {
                metrics.recordProcessorError(processor.getName());
            }
        }
    }
}
