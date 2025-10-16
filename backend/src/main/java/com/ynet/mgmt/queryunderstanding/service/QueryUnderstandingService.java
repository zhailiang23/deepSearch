package com.ynet.mgmt.queryunderstanding.service;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;

/**
 * 查询理解服务接口
 *
 * @author deepSearch
 * @since 2025-01-16
 */
public interface QueryUnderstandingService {

    /**
     * 理解和处理查询
     *
     * @param query 原始查询
     * @return 查询上下文
     */
    QueryContext understandQuery(String query);
}
