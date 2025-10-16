package com.ynet.mgmt.queryunderstanding.processor.expansion;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义扩展处理器
 * 基于embedding向量计算语义相似度，进行智能查询扩展
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class SemanticExpansionProcessor extends AbstractQueryProcessor {

    @Autowired
    private QueryUnderstandingEmbeddingService embeddingService;

    @Autowired(required = false)
    private SynonymExpansionProcessor synonymExpansionProcessor;

    /**
     * 最大扩展词数量
     */
    private static final int MAX_EXPANSION_TERMS = 5;

    @Override
    public String getName() {
        return "SemanticExpansionProcessor";
    }

    @Override
    public int getPriority() {
        return 55; // 在同义词扩展(60)之后，热门话题集成(50)之前
    }

    @Override
    protected void doProcess(QueryContext context) {
        String query = context.getCurrentQuery();

        // 如果embedding服务不可用，跳过处理
        if (!embeddingService.isServiceAvailable()) {
            log.debug("Embedding服务不可用，跳过语义扩展");
            return;
        }

        try {
            // 生成查询的embedding向量
            List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding.isEmpty()) {
                log.debug("无法生成查询的embedding向量");
                return;
            }

            // 构建候选词列表（来自内置词典和已识别的同义词）
            Set<String> candidateTerms = new HashSet<>();

            // 添加已识别的同义词作为候选
            if (context.getSynonyms() != null && !context.getSynonyms().isEmpty()) {
                candidateTerms.addAll(context.getSynonyms());
            }

            // 添加内置词典中的词作为候选
            candidateTerms.addAll(getBuiltInCandidates());

            if (candidateTerms.isEmpty()) {
                log.debug("候选词列表为空，无法进行语义扩展");
                return;
            }

            // 计算候选词与查询的语义相似度
            List<SemanticMatch> semanticMatches = new ArrayList<>();

            for (String candidate : candidateTerms) {
                // 跳过已经在查询中的词
                if (query.contains(candidate)) {
                    continue;
                }

                try {
                    double similarity = embeddingService.calculateSimilarity(query, candidate);

                    // 只保留相似度超过阈值的词（0.7）
                    if (similarity >= 0.7) {
                        semanticMatches.add(new SemanticMatch(candidate, similarity));
                        log.debug("发现语义相似词: {} (相似度: {:.4f})", candidate, similarity);
                    }
                } catch (Exception e) {
                    log.warn("计算词语 {} 的相似度失败: {}", candidate, e.getMessage());
                }
            }

            // 按相似度降序排序，取前N个
            semanticMatches.sort(Comparator.comparingDouble(SemanticMatch::getSimilarity).reversed());
            List<String> expansionTerms = semanticMatches.stream()
                .limit(MAX_EXPANSION_TERMS)
                .map(SemanticMatch::getTerm)
                .collect(Collectors.toList());

            if (!expansionTerms.isEmpty()) {
                // 将语义扩展词添加到上下文
                for (String term : expansionTerms) {
                    context.addRelatedTerm(term);
                }

                // 记录语义扩展信息
                for (SemanticMatch match : semanticMatches) {
                    if (expansionTerms.contains(match.getTerm())) {
                        context.putMetadata("semantic_similarity_" + match.getTerm(),
                            String.format("%.4f", match.getSimilarity()));
                    }
                }

                log.info("为查询 \"{}\" 添加了 {} 个语义扩展词: {}",
                    query, expansionTerms.size(), expansionTerms);
            }

        } catch (Exception e) {
            log.warn("语义扩展处理失败: {}", e.getMessage());
        }
    }

    /**
     * 获取内置候选词
     *
     * @return 候选词集合
     */
    private Set<String> getBuiltInCandidates() {
        Set<String> candidates = new HashSet<>();
        // 添加一些常见的候选词
        candidates.addAll(Arrays.asList(
            "生活", "缴费", "查询", "办理", "服务", "交费", "支付", "付费",
            "搜索", "检索", "查找", "处理", "操作", "日常", "生活服务"
        ));
        return candidates;
    }

    /**
     * 语义匹配结果
     */
    private static class SemanticMatch {
        private final String term;
        private final double similarity;

        public SemanticMatch(String term, double similarity) {
            this.term = term;
            this.similarity = similarity;
        }

        public String getTerm() {
            return term;
        }

        public double getSimilarity() {
            return similarity;
        }
    }
}
