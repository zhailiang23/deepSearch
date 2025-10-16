package com.ynet.mgmt.queryunderstanding.processor;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 查询处理器抽象基类
 * 提供通用的处理器功能实现
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
public abstract class AbstractQueryProcessor implements QueryProcessor {

    /**
     * 是否启用
     */
    protected boolean enabled = true;

    /**
     * 优先级
     */
    protected int priority;

    /**
     * 超时时间（毫秒）
     */
    protected long timeout = 1000L;

    /**
     * 处理查询上下文（带性能监控和异常处理）
     *
     * @param context 查询上下文
     */
    @Override
    public void process(QueryContext context) {
        if (!isEnabled()) {
            log.debug("处理器 {} 已禁用，跳过处理", getName());
            return;
        }

        // 如果启用了智能跳过，并且此处理器应该被跳过
        if (context.isSkipComplexProcessing() && shouldSkipForSimpleQuery()) {
            log.debug("简单查询跳过处理器 {}", getName());
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            log.debug("开始执行处理器: {}", getName());
            doProcess(context);
            long duration = System.currentTimeMillis() - startTime;
            context.recordProcessorTiming(getName(), duration);
            log.debug("处理器 {} 执行完成，耗时: {} ms", getName(), duration);

            // 检查是否超时
            if (duration > timeout) {
                log.warn("处理器 {} 执行超时，耗时: {} ms，超时阈值: {} ms",
                        getName(), duration, timeout);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            context.recordProcessorTiming(getName(), duration);
            log.error("处理器 {} 执行失败，耗时: {} ms", getName(), duration, e);
            // 处理失败不中断整个管道，继续执行
            onProcessError(context, e);
        }
    }

    /**
     * 实际的处理逻辑（由子类实现）
     *
     * @param context 查询上下文
     */
    protected abstract void doProcess(QueryContext context);

    /**
     * 处理器名称（由子类实现）
     *
     * @return 处理器名称
     */
    @Override
    public abstract String getName();

    /**
     * 是否对简单查询跳过此处理器
     * 某些复杂处理器（如 NER、意图分类）可以对简单查询跳过
     *
     * @return true 表示可以跳过
     */
    protected boolean shouldSkipForSimpleQuery() {
        return false;
    }

    /**
     * 处理错误的回调方法
     * 子类可以覆盖此方法来实现自定义的错误处理逻辑
     *
     * @param context 查询上下文
     * @param e 异常
     */
    protected void onProcessError(QueryContext context, Exception e) {
        // 默认实现：将错误信息记录到元数据中
        context.putMetadata(getName() + "_error", e.getMessage());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
