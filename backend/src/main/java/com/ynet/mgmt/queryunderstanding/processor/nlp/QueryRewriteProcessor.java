package com.ynet.mgmt.queryunderstanding.processor.nlp;

import com.ynet.mgmt.queryunderstanding.context.Entity;
import com.ynet.mgmt.queryunderstanding.context.IntentType;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingLlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询重写处理器
 * 基于识别的意图和提取的实体重写查询，提升搜索效果
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class QueryRewriteProcessor extends AbstractQueryProcessor {

    @Autowired
    private QueryUnderstandingLlmService llmService;

    /**
     * 是否启用查询重写
     */
    private boolean enableRewrite = true;

    public QueryRewriteProcessor() {
        this.priority = 30; // 在所有分析后执行
        this.timeout = 3000L; // LLM调用需要更长时间
    }

    @Override
    protected void doProcess(QueryContext context) {
        if (!enableRewrite) {
            log.debug("查询重写功能已禁用");
            return;
        }

        // 检查LLM服务是否可用
        if (!llmService.isServiceAvailable()) {
            log.debug("LLM服务不可用，跳过查询重写");
            return;
        }

        String query = context.getCurrentQuery();
        if (query == null || query.isEmpty()) {
            return;
        }

        // 获取意图和实体
        IntentType intent = context.getIntent();
        List<Entity> entities = context.getEntities();

        // 如果没有意图信息，跳过重写
        if (intent == null) {
            log.debug("没有意图信息，跳过查询重写");
            return;
        }

        try {
            // 调用LLM重写查询
            String rewrittenQuery = llmService.rewriteQuery(query, intent, entities);

            // 如果重写后的查询与原查询不同，记录重写
            if (rewrittenQuery != null && !rewrittenQuery.equals(query)) {
                context.setRewrittenQuery(rewrittenQuery);
                context.putMetadata("query_rewrite_applied", true);
                context.putMetadata("original_before_rewrite", query);

                log.debug("查询重写完成: \"{}\" -> \"{}\"", query, rewrittenQuery);
                log.debug("重写依据: intent={}, entities={}", intent, entities);
            } else {
                log.debug("查询无需重写: \"{}\"", query);
            }

        } catch (Exception e) {
            log.warn("查询重写失败: {}", e.getMessage());
            // 失败时不影响后续处理
        }
    }

    @Override
    public String getName() {
        return "QueryRewriteProcessor";
    }

    /**
     * 设置是否启用查询重写
     *
     * @param enableRewrite true 表示启用
     */
    public void setEnableRewrite(boolean enableRewrite) {
        this.enableRewrite = enableRewrite;
    }
}
