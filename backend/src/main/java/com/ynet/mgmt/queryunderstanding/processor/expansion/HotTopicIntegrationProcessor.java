package com.ynet.mgmt.queryunderstanding.processor.expansion;

import com.ynet.mgmt.hotTopic.entity.HotTopic;
import com.ynet.mgmt.hotTopic.repository.HotTopicRepository;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 热门话题集成处理器
 * 将查询与热门话题进行关联，增强搜索相关性
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class HotTopicIntegrationProcessor extends AbstractQueryProcessor {

    @Autowired
    private HotTopicRepository hotTopicRepository;

    @Override
    public String getName() {
        return "HotTopicIntegrationProcessor";
    }

    @Override
    public int getPriority() {
        return 50; // 在同义词扩展(60)之后
    }

    @Override
    protected void doProcess(QueryContext context) {
        String query = context.getCurrentQuery();

        try {
            // 查询所有可见的热门话题
            List<HotTopic> hotTopics = hotTopicRepository.findByVisibleTrueOrderByPopularityDesc();

            // 查找与查询相关的热门话题
            List<String> relatedHotTopics = new ArrayList<>();
            for (HotTopic hotTopic : hotTopics) {
                if (isRelated(query, hotTopic)) {
                    relatedHotTopics.add(hotTopic.getName());

                    // 记录元数据
                    context.putMetadata("hot_topic_" + hotTopic.getName(), hotTopic.getPopularity());

                    log.debug("检测到热门话题: {} (热度: {})", hotTopic.getName(), hotTopic.getPopularity());
                }
            }

            // 将热门话题添加到上下文
            if (!relatedHotTopics.isEmpty()) {
                List<String> existingHotTopics = context.getHotTopics();
                if (existingHotTopics == null) {
                    existingHotTopics = new ArrayList<>();
                }
                existingHotTopics.addAll(relatedHotTopics);
                context.setHotTopics(existingHotTopics);

                log.info("为查询 \"{}\" 找到 {} 个相关热门话题: {}",
                        query, relatedHotTopics.size(), relatedHotTopics);
            }

        } catch (Exception e) {
            log.warn("热门话题集成失败: {}", e.getMessage());
        }
    }

    /**
     * 判断查询是否与热门话题相关
     *
     * @param query    查询文本
     * @param hotTopic 热门话题
     * @return true 如果相关
     */
    private boolean isRelated(String query, HotTopic hotTopic) {
        String topicName = hotTopic.getName();

        // 策略1: 直接包含
        if (query.contains(topicName) || topicName.contains(query)) {
            return true;
        }

        // 策略2: 拼音匹配
        if (hotTopic.getPinyin() != null && hotTopic.getPinyin().contains(query.toLowerCase())) {
            return true;
        }

        // 策略3: 拼音首字母匹配
        if (hotTopic.getPinyinFirstLetter() != null &&
            hotTopic.getPinyinFirstLetter().contains(query.toLowerCase())) {
            return true;
        }

        return false;
    }
}
