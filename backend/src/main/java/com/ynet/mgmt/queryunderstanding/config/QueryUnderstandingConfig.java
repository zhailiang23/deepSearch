package com.ynet.mgmt.queryunderstanding.config;

import com.ynet.mgmt.queryunderstanding.pipeline.QueryUnderstandingPipeline;
import com.ynet.mgmt.queryunderstanding.processor.QueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 查询理解管道配置
 * 自动注册所有处理器到管道
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Configuration
public class QueryUnderstandingConfig {

    @Autowired
    private QueryUnderstandingPipeline pipeline;

    @Autowired(required = false)
    private List<QueryProcessor> processors;

    /**
     * 初始化管道，注册所有处理器
     */
    @PostConstruct
    public void init() {
        if (processors == null || processors.isEmpty()) {
            log.warn("未发现任何查询处理器");
            return;
        }

        log.info("开始初始化查询理解管道，共发现 {} 个处理器", processors.size());
        pipeline.registerProcessors(processors);
        log.info("查询理解管道初始化完成");
    }
}
