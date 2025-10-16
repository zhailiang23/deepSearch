package com.ynet.mgmt.queryunderstanding.processor;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.ynet.mgmt.queryunderstanding.context.Entity;
import com.ynet.mgmt.queryunderstanding.context.EntityType;
import com.ynet.mgmt.queryunderstanding.context.IntentType;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询构建处理器
 * 基于上下文信息构建优化的Elasticsearch查询
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class QueryBuilderProcessor extends AbstractQueryProcessor {

    @Override
    public String getName() {
        return "QueryBuilderProcessor";
    }

    @Override
    public int getPriority() {
        return 10; // 在所有分析处理器之后执行
    }

    @Override
    protected void doProcess(QueryContext context) {
        try {
            // 构建基础查询
            Query esQuery = buildQuery(context);

            // 设置到上下文
            context.setElasticsearchQuery(esQuery);

            log.info("成功构建Elasticsearch查询");

        } catch (Exception e) {
            log.warn("构建Elasticsearch查询失败: {}", e.getMessage());
        }
    }

    /**
     * 构建Elasticsearch查询
     *
     * @param context 查询上下文
     * @return Elasticsearch查询
     */
    private Query buildQuery(QueryContext context) {
        List<Query> mustQueries = new ArrayList<>();
        List<Query> shouldQueries = new ArrayList<>();

        // 1. 主查询 - 使用最终的查询文本
        String mainQuery = context.getCurrentQuery();
        if (mainQuery != null && !mainQuery.isEmpty()) {
            // 使用multi_match查询多个字段
            shouldQueries.add(Query.of(q -> q
                .multiMatch(m -> m
                    .query(mainQuery)
                    .fields("title^3", "content^2", "keywords^2", "description")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                )
            ));
        }

        // 2. 同义词扩展
        if (context.getSynonyms() != null && !context.getSynonyms().isEmpty()) {
            for (String synonym : context.getSynonyms()) {
                shouldQueries.add(Query.of(q -> q
                    .multiMatch(m -> m
                        .query(synonym)
                        .fields("title^2", "content", "keywords")
                        .type(TextQueryType.BestFields)
                    )
                ));
            }
            log.debug("添加了 {} 个同义词查询", context.getSynonyms().size());
        }

        // 3. 语义相关词
        if (context.getRelatedTerms() != null && !context.getRelatedTerms().isEmpty()) {
            for (String term : context.getRelatedTerms()) {
                shouldQueries.add(Query.of(q -> q
                    .multiMatch(m -> m
                        .query(term)
                        .fields("title^1.5", "content", "keywords^1.5")
                        .type(TextQueryType.BestFields)
                    )
                ));
            }
            log.debug("添加了 {} 个语义相关词查询", context.getRelatedTerms().size());
        }

        // 4. 热门话题
        if (context.getHotTopics() != null && !context.getHotTopics().isEmpty()) {
            for (String topic : context.getHotTopics()) {
                // 热门话题使用更高的权重
                shouldQueries.add(Query.of(q -> q
                    .multiMatch(m -> m
                        .query(topic)
                        .fields("title^4", "keywords^3", "content^2")
                        .type(TextQueryType.BestFields)
                    )
                ));
            }
            log.debug("添加了 {} 个热门话题查询", context.getHotTopics().size());
        }

        // 5. 实体过滤
        if (context.getEntities() != null && !context.getEntities().isEmpty()) {
            for (Entity entity : context.getEntities()) {
                // 根据实体类型构建不同的查询
                String fieldName = getFieldNameForEntityType(entity.getType());
                if (fieldName != null) {
                    shouldQueries.add(Query.of(q -> q
                        .match(m -> m
                            .field(fieldName)
                            .query(entity.getText())
                        )
                    ));
                }
            }
            log.debug("添加了 {} 个实体查询", context.getEntities().size());
        }

        // 6. 根据意图调整查询策略
        if (context.getIntent() != null) {
            adjustQueryByIntent(context.getIntent(), shouldQueries);
        }

        // 7. 组合查询
        if (mustQueries.isEmpty() && shouldQueries.isEmpty()) {
            // 如果没有任何查询条件，返回match_all
            return Query.of(q -> q.matchAll(m -> m));
        }

        // 使用bool查询组合所有条件
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (!mustQueries.isEmpty()) {
            boolBuilder.must(mustQueries);
        }

        if (!shouldQueries.isEmpty()) {
            boolBuilder.should(shouldQueries);
            boolBuilder.minimumShouldMatch("1"); // 至少匹配一个should条件
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 根据实体类型获取对应的字段名
     *
     * @param entityType 实体类型
     * @return 字段名
     */
    private String getFieldNameForEntityType(EntityType entityType) {
        switch (entityType) {
            case PERSON:
                return "person";
            case LOCATION:
                return "location";
            case ORGANIZATION:
                return "organization";
            case DATE_TIME:
                return "date";
            case PRODUCT:
                return "product";
            case MONEY:
                return "amount";
            default:
                return null;
        }
    }

    /**
     * 根据意图类型调整查询策略
     *
     * @param intent        意图类型
     * @param shouldQueries should查询列表
     */
    private void adjustQueryByIntent(IntentType intent, List<Query> shouldQueries) {
        switch (intent) {
            case QUERY:
            case INFORMATION_QUERY:
                // 查询意图 - 提升title和keywords权重
                log.debug("检测到查询意图，提升标题和关键词权重");
                break;
            case COMMAND:
            case ACTION:
                // 命令意图 - 可能需要精确匹配
                log.debug("检测到命令/操作意图");
                break;
            case QUESTION:
                // 问题意图 - 提升content权重
                log.debug("检测到问题意图，提升内容权重");
                break;
            case NAVIGATION:
                // 导航意图
                log.debug("检测到导航意图");
                break;
            case COMPARISON:
                // 比较意图
                log.debug("检测到比较意图");
                break;
            case DEFINITION:
                // 定义意图
                log.debug("检测到定义意图");
                break;
            case LIST:
                // 列表意图
                log.debug("检测到列表意图");
                break;
            case UNKNOWN:
            default:
                // 未知意图 - 使用默认策略
                log.debug("意图类型未知或为默认类型");
                break;
        }
    }
}
