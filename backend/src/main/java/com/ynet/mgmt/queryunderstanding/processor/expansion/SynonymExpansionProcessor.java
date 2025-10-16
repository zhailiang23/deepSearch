package com.ynet.mgmt.queryunderstanding.processor.expansion;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 同义词扩展处理器
 * 为查询词添加同义词，提高召回率
 * Phase 1: 使用内置同义词词典
 * 后续可集成 Elasticsearch Synonym Graph Filter
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class SynonymExpansionProcessor extends AbstractQueryProcessor {

    /**
     * 最大同义词数量
     */
    private int maxSynonyms = 5;

    /**
     * 内置同义词词典（简化版，实际应从数据库或配置加载）
     */
    private final Map<String, List<String>> synonymDict = new HashMap<>();

    public SynonymExpansionProcessor() {
        this.priority = 60;
        this.timeout = 800L;
        initializeSynonymDict();
    }

    /**
     * 初始化同义词词典
     */
    private void initializeSynonymDict() {
        // 示例同义词
        synonymDict.put("生活", Arrays.asList("生活服务", "日常"));
        synonymDict.put("缴费", Arrays.asList("交费", "支付", "付费"));
        synonymDict.put("查询", Arrays.asList("搜索", "检索", "查找"));
        synonymDict.put("办理", Arrays.asList("处理", "操作"));

        log.debug("同义词词典初始化完成，包含 {} 个词条", synonymDict.size());
    }

    @Override
    protected void doProcess(QueryContext context) {
        String query = context.getCurrentQuery();
        if (query == null || query.isEmpty()) {
            return;
        }

        // 分词（简单按空格分词，实际应使用中文分词器）
        String[] terms = query.split("\\s+");
        List<String> synonyms = new ArrayList<>();

        for (String term : terms) {
            List<String> termSynonyms = findSynonyms(term);
            if (termSynonyms != null && !termSynonyms.isEmpty()) {
                synonyms.addAll(termSynonyms);
                log.debug("为词 \"{}\" 找到同义词: {}", term, termSynonyms);
            }
        }

        // 限制同义词数量
        if (synonyms.size() > maxSynonyms) {
            synonyms = synonyms.subList(0, maxSynonyms);
        }

        // 添加到上下文
        for (String synonym : synonyms) {
            context.addSynonym(synonym);
        }

        if (!synonyms.isEmpty()) {
            log.debug("同义词扩展完成，共找到 {} 个同义词", synonyms.size());
        }
    }

    /**
     * 查找同义词
     *
     * @param term 原词
     * @return 同义词列表
     */
    private List<String> findSynonyms(String term) {
        // 精确匹配
        if (synonymDict.containsKey(term)) {
            return new ArrayList<>(synonymDict.get(term));
        }

        // 模糊匹配（包含关系）
        List<String> fuzzyMatches = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : synonymDict.entrySet()) {
            if (term.contains(entry.getKey()) || entry.getKey().contains(term)) {
                fuzzyMatches.addAll(entry.getValue());
            }
        }

        return fuzzyMatches;
    }

    @Override
    public String getName() {
        return "SynonymExpansionProcessor";
    }

    /**
     * 添加同义词映射
     *
     * @param word 原词
     * @param synonyms 同义词列表
     */
    public void addSynonymMapping(String word, List<String> synonyms) {
        synonymDict.put(word, new ArrayList<>(synonyms));
        log.debug("添加同义词映射: {} -> {}", word, synonyms);
    }

    /**
     * 批量添加同义词映射
     *
     * @param mappings 同义词映射
     */
    public void addSynonymMappings(Map<String, List<String>> mappings) {
        synonymDict.putAll(mappings);
        log.debug("批量添加同义词映射，共 {} 条", mappings.size());
    }

    /**
     * 清空同义词词典
     */
    public void clearSynonymDict() {
        synonymDict.clear();
        log.debug("同义词词典已清空");
    }

    /**
     * 获取同义词词典大小
     *
     * @return 词典大小
     */
    public int getSynonymDictSize() {
        return synonymDict.size();
    }

    public void setMaxSynonyms(int maxSynonyms) {
        this.maxSynonyms = maxSynonyms;
    }
}
