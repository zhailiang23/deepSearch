package com.ynet.mgmt.queryunderstanding.processor.nlp;

import com.ynet.mgmt.queryunderstanding.context.Entity;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingLlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 实体抽取处理器
 * 使用LLM从查询中提取命名实体
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class EntityExtractionProcessor extends AbstractQueryProcessor {

    @Autowired
    private QueryUnderstandingLlmService llmService;

    public EntityExtractionProcessor() {
        this.priority = 40; // 在意图识别后执行
        this.timeout = 3000L; // LLM调用需要更长时间
    }

    @Override
    protected void doProcess(QueryContext context) {
        // 检查LLM服务是否可用
        if (!llmService.isServiceAvailable()) {
            log.debug("LLM服务不可用，跳过实体抽取");
            return;
        }

        String query = context.getCurrentQuery();
        if (query == null || query.isEmpty()) {
            return;
        }

        try {
            // 调用LLM提取实体
            List<Entity> entities = llmService.extractEntities(query);

            // 设置实体列表
            context.setEntities(entities);

            log.debug("实体抽取完成: query=\"{}\", 提取到 {} 个实体",
                query, entities.size());

            if (!entities.isEmpty()) {
                log.debug("实体详情: {}", entities);
            }

        } catch (Exception e) {
            log.warn("实体抽取失败: {}", e.getMessage());
            // 失败时不影响后续处理
        }
    }

    @Override
    public String getName() {
        return "EntityExtractionProcessor";
    }
}
