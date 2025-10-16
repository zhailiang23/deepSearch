package com.ynet.mgmt.queryunderstanding.processor.optimization;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 消歧处理器
 * 处理多义词，基于上下文信息进行歧义消除
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class DisambiguationProcessor extends AbstractQueryProcessor {

    /**
     * 常见多义词词典
     * key: 多义词, value: 可能的含义列表
     */
    private static final Map<String, List<String>> AMBIGUOUS_WORDS = new HashMap<>();

    static {
        // 初始化常见多义词
        AMBIGUOUS_WORDS.put("苹果", Arrays.asList("水果", "Apple公司", "iPhone手机"));
        AMBIGUOUS_WORDS.put("锁", Arrays.asList("门锁", "车锁", "账号锁定"));
        AMBIGUOUS_WORDS.put("服务", Arrays.asList("客户服务", "系统服务", "公共服务"));
        AMBIGUOUS_WORDS.put("充值", Arrays.asList("手机充值", "游戏充值", "账户充值"));
    }

    @Override
    public String getName() {
        return "DisambiguationProcessor";
    }

    @Override
    public int getPriority() {
        return 35; // 在查询重写(30)之前
    }

    @Override
    protected void doProcess(QueryContext context) {
        String query = context.getCurrentQuery();

        try {
            // 检测查询中的多义词
            List<String> detectedAmbiguousWords = new ArrayList<>();
            for (String word : AMBIGUOUS_WORDS.keySet()) {
                if (query.contains(word)) {
                    detectedAmbiguousWords.add(word);
                }
            }

            if (!detectedAmbiguousWords.isEmpty()) {
                log.debug("检测到多义词: {}", detectedAmbiguousWords);

                // 尝试根据上下文信息消歧
                for (String word : detectedAmbiguousWords) {
                    String resolvedMeaning = resolveAmbiguity(word, context);
                    if (resolvedMeaning != null) {
                        context.putMetadata("disambiguated_" + word, resolvedMeaning);
                        log.info("消歧: \"{}\" -> \"{}\"", word, resolvedMeaning);
                    } else {
                        // 无法消歧，记录所有可能含义
                        context.putMetadata("ambiguous_" + word, AMBIGUOUS_WORDS.get(word));
                        log.debug("\"{}\" 存在歧义，可能含义: {}", word, AMBIGUOUS_WORDS.get(word));
                    }
                }
            }

        } catch (Exception e) {
            log.warn("消歧处理失败: {}", e.getMessage());
        }
    }

    /**
     * 根据上下文信息解析多义词
     *
     * @param word    多义词
     * @param context 查询上下文
     * @return 解析后的含义，如果无法确定则返回null
     */
    private String resolveAmbiguity(String word, QueryContext context) {
        List<String> meanings = AMBIGUOUS_WORDS.get(word);
        if (meanings == null || meanings.isEmpty()) {
            return null;
        }

        String query = context.getCurrentQuery();

        // 策略1: 基于查询中的其他关键词
        if (word.equals("苹果")) {
            if (query.contains("手机") || query.contains("iPhone") || query.contains("iOS")) {
                return "Apple公司";
            } else if (query.contains("水果") || query.contains("吃") || query.contains("新鲜")) {
                return "水果";
            }
        }

        // 策略2: 基于热门话题
        List<String> hotTopics = context.getHotTopics();
        if (hotTopics != null && !hotTopics.isEmpty()) {
            for (String meaning : meanings) {
                for (String topic : hotTopics) {
                    if (topic.contains(meaning) || meaning.contains(topic)) {
                        return meaning;
                    }
                }
            }
        }

        // 策略3: 基于实体类型
        if (context.getEntities() != null && !context.getEntities().isEmpty()) {
            String entityTypes = context.getEntities().toString();
            for (String meaning : meanings) {
                if (entityTypes.contains(meaning)) {
                    return meaning;
                }
            }
        }

        // 无法确定，返回null
        return null;
    }
}
