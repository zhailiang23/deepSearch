package com.ynet.mgmt.queryunderstanding.processor.nlp;

import com.ynet.mgmt.queryunderstanding.context.IntentType;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingLlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 意图识别处理器
 * 使用LLM识别用户查询的意图类型
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class IntentRecognitionProcessor extends AbstractQueryProcessor {

    @Autowired
    private QueryUnderstandingLlmService llmService;

    public IntentRecognitionProcessor() {
        this.priority = 50; // 在同义词扩展后执行
        this.timeout = 3000L; // LLM调用需要更长时间
    }

    @Override
    protected void doProcess(QueryContext context) {
        // 检查LLM服务是否可用
        if (!llmService.isServiceAvailable()) {
            log.debug("LLM服务不可用，跳过意图识别");
            return;
        }

        String query = context.getCurrentQuery();
        if (query == null || query.isEmpty()) {
            return;
        }

        try {
            // 调用LLM识别意图
            IntentType intent = llmService.recognizeIntent(query);

            // 设置意图
            context.setIntent(intent);

            // 根据意图类型设置置信度
            double confidence = calculateConfidence(intent, query);
            context.setIntentConfidence(confidence);

            log.debug("意图识别完成: query=\"{}\", intent={}, confidence={}",
                query, intent, confidence);

        } catch (Exception e) {
            log.warn("意图识别失败: {}", e.getMessage());
            // 失败时设置默认值
            context.setIntent(IntentType.QUERY);
            context.setIntentConfidence(0.5);
        }
    }

    /**
     * 计算意图识别的置信度
     * 基于查询特征和识别结果
     *
     * @param intent 识别的意图
     * @param query 查询文本
     * @return 置信度 (0.0-1.0)
     */
    private double calculateConfidence(IntentType intent, String query) {
        // 简化的置信度计算
        // 实际应用中可以基于更多特征计算

        double confidence = 0.7; // 基准置信度

        // 如果包含问号，QUESTION意图置信度更高
        if (query.contains("?") || query.contains("？")) {
            if (intent == IntentType.QUESTION) {
                confidence = 0.9;
            }
        }

        // 如果包含操作动词，COMMAND意图置信度更高
        String[] commandKeywords = {"打开", "关闭", "删除", "创建", "修改", "设置"};
        for (String keyword : commandKeywords) {
            if (query.contains(keyword)) {
                if (intent == IntentType.COMMAND) {
                    confidence = 0.85;
                }
                break;
            }
        }

        return confidence;
    }

    @Override
    public String getName() {
        return "IntentRecognitionProcessor";
    }
}
