package com.ynet.mgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * 为数据导入等长时间运行的任务提供专用线程池
 */
@Configuration
@EnableAsync
public class AsyncTaskConfig {

    /**
     * 数据导入任务执行器
     * 配置专用的线程池来处理数据导入任务
     */
    @Bean("dataImportTaskExecutor")
    public Executor dataImportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：同时允许的导入任务数
        executor.setCorePoolSize(2);

        // 最大线程数：高峰期最多的导入任务数
        executor.setMaxPoolSize(5);

        // 队列容量：等待执行的任务队列大小
        executor.setQueueCapacity(10);

        // 线程空闲时间：线程空闲超过此时间将被回收
        executor.setKeepAliveSeconds(60);

        // 线程名前缀
        executor.setThreadNamePrefix("DataImport-");

        // 拒绝策略：当任务过多时的处理策略
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Elasticsearch任务执行器
     * 用于处理ES相关的异步操作
     */
    @Bean("elasticsearchTaskExecutor")
    public Executor elasticsearchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ES-Task-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 搜索日志记录任务执行器
     * 用于异步记录搜索日志，确保不影响搜索性能
     */
    @Bean("searchLogExecutor")
    public Executor searchLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("SearchLog-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}