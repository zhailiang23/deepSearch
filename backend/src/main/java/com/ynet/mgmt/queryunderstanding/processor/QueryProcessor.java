package com.ynet.mgmt.queryunderstanding.processor;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;

/**
 * 查询处理器接口
 * 所有查询处理器都需要实现此接口
 * 采用责任链模式，每个处理器处理查询上下文的特定方面
 *
 * @author deepSearch
 * @since 2025-01-16
 */
public interface QueryProcessor {

    /**
     * 处理查询上下文
     * 每个处理器应该修改或增强 QueryContext 中的信息
     *
     * @param context 查询上下文
     */
    void process(QueryContext context);

    /**
     * 获取处理器名称
     * 用于日志记录和性能追踪
     *
     * @return 处理器名称
     */
    String getName();

    /**
     * 检查处理器是否启用
     * 可以通过配置动态启用/禁用处理器
     *
     * @return true 表示启用，false 表示禁用
     */
    boolean isEnabled();

    /**
     * 获取处理器优先级
     * 数字越大优先级越高，越先执行
     *
     * @return 优先级值
     */
    int getPriority();

    /**
     * 获取处理器超时时间（毫秒）
     * 如果处理超过此时间，应该终止处理
     *
     * @return 超时时间（毫秒）
     */
    default long getTimeout() {
        return 1000L; // 默认 1 秒
    }
}
