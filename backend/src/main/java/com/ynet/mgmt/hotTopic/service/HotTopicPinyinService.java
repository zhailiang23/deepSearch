package com.ynet.mgmt.hotTopic.service;

import com.ynet.mgmt.common.utils.PinyinUtils;
import com.ynet.mgmt.hotTopic.entity.HotTopic;
import com.ynet.mgmt.hotTopic.repository.HotTopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 热门话题拼音服务
 * 负责热门话题的拼音字段生成和更新
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class HotTopicPinyinService {

    private static final Logger logger = LoggerFactory.getLogger(HotTopicPinyinService.class);

    @Autowired
    private HotTopicRepository hotTopicRepository;

    /**
     * 批量初始化所有热门话题的拼音字段
     * 仅更新拼音字段为空的记录
     *
     * @return 更新的记录数
     */
    @Transactional
    public int initializeAllPinyin() {
        logger.info("开始批量初始化热门话题拼音数据");

        // 查询所有热门话题
        List<HotTopic> allTopics = hotTopicRepository.findAll();
        int updateCount = 0;

        for (HotTopic topic : allTopics) {
            // 只更新拼音字段为空的记录
            if (!StringUtils.hasText(topic.getPinyin()) || !StringUtils.hasText(topic.getPinyinFirstLetter())) {
                boolean updated = updateTopicPinyin(topic);
                if (updated) {
                    updateCount++;
                }
            }
        }

        logger.info("热门话题拼音数据初始化完成，共更新 {} 条记录", updateCount);
        return updateCount;
    }

    /**
     * 强制更新所有热门话题的拼音字段
     * 无论拼音字段是否为空都会更新
     *
     * @return 更新的记录数
     */
    @Transactional
    public int forceUpdateAllPinyin() {
        logger.info("开始强制更新所有热门话题拼音数据");

        List<HotTopic> allTopics = hotTopicRepository.findAll();
        int updateCount = 0;

        for (HotTopic topic : allTopics) {
            boolean updated = updateTopicPinyin(topic);
            if (updated) {
                updateCount++;
            }
        }

        logger.info("热门话题拼音数据强制更新完成，共更新 {} 条记录", updateCount);
        return updateCount;
    }

    /**
     * 为单个热门话题更新拼音字段
     *
     * @param topic 热门话题
     * @return true 如果更新成功，false 否则
     */
    @Transactional
    public boolean updateTopicPinyin(HotTopic topic) {
        if (topic == null || !StringUtils.hasText(topic.getName())) {
            return false;
        }

        try {
            String name = topic.getName();

            // 生成拼音全拼
            String pinyin = PinyinUtils.toPinyin(name);
            topic.setPinyin(pinyin);

            // 生成拼音首字母
            String pinyinFirstLetter = PinyinUtils.toPinyinFirstLetter(name);
            topic.setPinyinFirstLetter(pinyinFirstLetter);

            // 添加详细日志用于调试
            logger.info("拼音转换详情: name='{}' [bytes: {}], pinyin='{}' [bytes: {}], firstLetter='{}' [bytes: {}]",
                    name, name.getBytes().length,
                    pinyin, pinyin.getBytes().length,
                    pinyinFirstLetter, pinyinFirstLetter.getBytes().length);

            // 保存到数据库
            hotTopicRepository.save(topic);

            logger.debug("更新热门话题拼音: name={}, pinyin={}, firstLetter={}",
                    name, pinyin, pinyinFirstLetter);
            return true;

        } catch (Exception e) {
            logger.error("更新热门话题拼音失败: id={}, name={}", topic.getId(), topic.getName(), e);
            return false;
        }
    }

    /**
     * 为新创建的热门话题自动生成拼音字段
     * 此方法应在创建热门话题时调用
     *
     * @param topic 新创建的热门话题
     */
    public void generatePinyinForNewTopic(HotTopic topic) {
        if (topic == null || !StringUtils.hasText(topic.getName())) {
            return;
        }

        String name = topic.getName();
        topic.setPinyin(PinyinUtils.toPinyin(name));
        topic.setPinyinFirstLetter(PinyinUtils.toPinyinFirstLetter(name));

        logger.debug("为新话题生成拼音: name={}, pinyin={}, firstLetter={}",
                name, topic.getPinyin(), topic.getPinyinFirstLetter());
    }
}
