package com.ynet.mgmt.queryunderstanding.service.impl;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.pipeline.QueryUnderstandingPipeline;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询理解服务实现
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Service
public class QueryUnderstandingServiceImpl implements QueryUnderstandingService {

    @Autowired
    private QueryUnderstandingPipeline pipeline;

    @Override
    public QueryContext understandQuery(String query) {
        log.info("开始处理查询理解请求: {}", query);

        if (query == null || query.trim().isEmpty()) {
            log.warn("查询为空");
            return new QueryContext("");
        }

        // 执行管道处理
        QueryContext context = pipeline.execute(query);

        log.info("查询理解完成，耗时: {} ms", context.getTotalProcessingTime());
        return context;
    }
}
