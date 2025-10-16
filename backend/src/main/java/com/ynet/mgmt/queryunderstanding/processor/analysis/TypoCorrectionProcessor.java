package com.ynet.mgmt.queryunderstanding.processor.analysis;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 错别字纠正处理器
 * 使用 Elasticsearch Term Suggester API 检测并纠正拼写错误
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class TypoCorrectionProcessor extends AbstractQueryProcessor {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 模糊度设置
     */
    private String fuzziness = "AUTO";

    /**
     * 最小相似度阈值（0.0-1.0）
     */
    private double minSimilarity = 0.7;

    /**
     * 最小建议分数
     */
    private double minSuggestScore = 0.5;

    /**
     * 是否启用错别字纠正
     */
    private boolean enableCorrection = true;

    public TypoCorrectionProcessor() {
        this.priority = 80;
        this.timeout = 1000L;
    }

    @Override
    protected void doProcess(QueryContext context) {
        if (!enableCorrection) {
            log.debug("错别字纠正功能已禁用");
            return;
        }

        String query = context.getNormalizedQuery();
        if (query == null || query.isEmpty()) {
            return;
        }

        try {
            // 使用 Term Suggester 获取拼写建议
            String correctedQuery = correctTypos(query);

            if (correctedQuery != null && !correctedQuery.equals(query)) {
                context.setCorrectedQuery(correctedQuery);
                context.putMetadata("typo_correction_applied", true);
                context.putMetadata("original_before_correction", query);
                log.debug("错别字纠正: \"{}\" -> \"{}\"", query, correctedQuery);
            } else {
                log.debug("未检测到错别字，保持原查询: \"{}\"", query);
            }
        } catch (Exception e) {
            log.warn("错别字纠正失败: {}", e.getMessage());
            // 纠正失败不影响后续处理
        }
    }

    /**
     * 使用 Elasticsearch Term Suggester 纠正错别字
     *
     * @param query 原始查询
     * @return 纠正后的查询，如果没有建议则返回 null
     */
    private String correctTypos(String query) throws Exception {
        log.debug("使用 Term Suggester 检测错别字: {}", query);

        try {
            // 分词 - 简单按空格分割
            String[] words = query.split("\\s+");
            StringBuilder correctedQuery = new StringBuilder();
            boolean hasCorrected = false;

            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }

                // 对每个词使用 Term Suggester
                String suggestion = getSuggestionForTerm(word);

                if (suggestion != null && !suggestion.equals(word)) {
                    // 计算相似度
                    double similarity = calculateSimilarity(word, suggestion);

                    if (similarity >= minSimilarity) {
                        log.debug("词 \"{}\" 建议纠正为 \"{}\", 相似度: {}", word, suggestion, similarity);
                        correctedQuery.append(suggestion);
                        hasCorrected = true;
                    } else {
                        log.debug("词 \"{}\" 的建议 \"{}\" 相似度过低({}), 保持原词", word, suggestion, similarity);
                        correctedQuery.append(word);
                    }
                } else {
                    correctedQuery.append(word);
                }

                correctedQuery.append(" ");
            }

            if (hasCorrected) {
                return correctedQuery.toString().trim();
            }

            return null; // 没有纠正

        } catch (Exception e) {
            log.warn("Term Suggester 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 为单个词获取拼写建议
     *
     * @param term 词
     * @return 建议的词，如果没有建议则返回原词
     */
    private String getSuggestionForTerm(String term) throws Exception {
        // Phase 2: 暂时返回原词，避免实际调用ES
        // 在实际部署时需要配置具体的索引名称和字段
        // 示例代码如下:
        /*
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
            .index("function", "information", "product", "activity") // 搜索所有索引
            .suggest(suggest -> suggest
                .suggesters("term-suggestion", ts -> ts
                    .term(t -> t
                        .field("name") // 在name字段上建议
                        .text(term)
                        .suggestMode(SuggestMode.Popular)
                        .maxEdits(2)
                        .minWordLength(2)
                        .prefixLength(1)
                        .minDocFreq(1)
                    )
                )
            )
            .size(0), // 不需要文档结果
            Void.class
        );

        // 解析建议结果
        Map<String, List<Suggestion<Void>>> suggestions = response.suggest();
        if (suggestions != null && suggestions.containsKey("term-suggestion")) {
            List<Suggestion<Void>> termSuggestions = suggestions.get("term-suggestion");
            if (!termSuggestions.isEmpty()) {
                List<TermSuggestOption> options = termSuggestions.get(0).term().options();
                if (!options.isEmpty()) {
                    TermSuggestOption bestOption = options.get(0);
                    if (bestOption.score() >= minSuggestScore) {
                        return bestOption.text();
                    }
                }
            }
        }
        */

        return term; // Phase 2: 返回原词
    }

    /**
     * 计算两个字符串的相似度（Levenshtein 距离）
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度 (0.0-1.0)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * 计算 Levenshtein 距离
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    @Override
    public String getName() {
        return "TypoCorrectionProcessor";
    }

    public void setFuzziness(String fuzziness) {
        this.fuzziness = fuzziness;
    }

    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    public void setEnableCorrection(boolean enableCorrection) {
        this.enableCorrection = enableCorrection;
    }
}
