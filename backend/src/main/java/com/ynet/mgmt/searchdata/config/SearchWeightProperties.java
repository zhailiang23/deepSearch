package com.ynet.mgmt.searchdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索权重配置类
 * 集中管理所有搜索相关的权重参数
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "search.weight")
public class SearchWeightProperties {

    /**
     * 字段权重配置 - 基于字段名称的boost值
     */
    private FieldWeight field = new FieldWeight();

    /**
     * 多字段搜索权重配置 - buildMultiFieldQuery 方法中的权重
     */
    private MultiField multiField = new MultiField();

    /**
     * 拼音搜索权重配置 - 不同拼音模式下的权重
     */
    private PinyinWeight pinyin = new PinyinWeight();

    /**
     * 字段权重配置
     */
    @Data
    public static class FieldWeight {
        /**
         * 标题类字段权重 (title, 标题)
         */
        private float title = 3.0f;

        /**
         * 名称类字段权重 (name, 名称)
         */
        private float name = 2.5f;

        /**
         * 内容类字段权重 (content, description, 内容, 描述)
         */
        private float content = 2.0f;

        /**
         * 文本类字段权重 (text, 文本)
         */
        private float text = 1.5f;

        /**
         * 类型分类字段权重 (type, category, 类型, 分类)
         */
        private float category = 1.2f;

        /**
         * 默认字段权重
         */
        private float defaultWeight = 1.0f;

        /**
         * keyword子字段权重
         */
        private float keyword = 0.8f;
    }

    /**
     * 多字段搜索权重配置
     */
    @Data
    public static class MultiField {
        /**
         * 完全短语匹配权重倍数
         * 最终权重 = boost * phraseMatch
         */
        private float phraseMatch = 1.5f;

        /**
         * 所有词都匹配权重倍数
         * 最终权重 = boost * allWordsMatch
         */
        private float allWordsMatch = 1.2f;

        /**
         * 任意词匹配权重倍数 (基础权重)
         * 最终权重 = boost * anyWordMatch
         */
        private float anyWordMatch = 1.0f;
    }

    /**
     * 拼音搜索权重配置
     */
    @Data
    public static class PinyinWeight {
        /**
         * STRICT 模式 - 关键字查询权重
         */
        private float strictKeyword = 1.0f;

        /**
         * STRICT 模式 - 拼音查询权重
         */
        private float strictPinyin = 0.8f;

        /**
         * STRICT 模式 - 首字母查询权重
         */
        private float strictFirstLetter = 0.5f;

        /**
         * FUZZY 模式 - 关键字查询权重
         */
        private float fuzzyKeyword = 1.0f;

        /**
         * FUZZY 模式 - 拼音查询权重
         */
        private float fuzzyPinyin = 0.8f;

        /**
         * FUZZY 模式 - 首字母查询权重
         */
        private float fuzzyFirstLetter = 0.6f;

        /**
         * AUTO 模式 - 关键字查询权重
         */
        private float autoKeyword = 1.0f;

        /**
         * AUTO 模式 - 拼音查询权重
         */
        private float autoPinyin = 0.8f;

        /**
         * AUTO 模式 - 首字母查询权重
         */
        private float autoFirstLetter = 0.6f;

        /**
         * 拼音字段权重 (*.pinyin)
         */
        private float pinyinField = 1.2f;

        /**
         * 中文拼音字段权重 (*.chinese_pinyin)
         */
        private float chinesePinyinField = 1.0f;

        /**
         * 首字母字段权重 (*.first_letter)
         */
        private float firstLetterField = 1.0f;
    }
}
