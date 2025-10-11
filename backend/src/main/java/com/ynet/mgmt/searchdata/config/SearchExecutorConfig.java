package com.ynet.mgmt.searchdata.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 搜索执行器配置
 * 为多索引并发搜索提供专用线程池
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SearchExecutorConfig {

    /**
     * 创建搜索专用线程池
     * 核心线程数：10，最大线程数：50
     * 空闲线程存活时间：60秒
     * 队列容量：100
     * 拒绝策略：CallerRunsPolicy（调用者线程执行）
     *
     * @return ExecutorService 搜索执行器
     */
    @Bean(name = "searchExecutor")
    public ExecutorService searchExecutor() {
        int corePoolSize = 10;
        int maxPoolSize = 50;
        long keepAliveTime = 60L;
        int queueCapacity = 100;

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private int threadCount = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("search-executor-" + threadCount++);
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        log.info("搜索执行器线程池已创建: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }
}
