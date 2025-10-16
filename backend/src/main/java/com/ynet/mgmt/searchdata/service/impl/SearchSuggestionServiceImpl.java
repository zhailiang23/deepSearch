package com.ynet.mgmt.searchdata.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ynet.mgmt.hotTopic.entity.HotTopic;
import com.ynet.mgmt.hotTopic.repository.HotTopicRepository;
import com.ynet.mgmt.searchdata.dto.SearchSuggestionDTO;
import com.ynet.mgmt.searchdata.dto.SearchSuggestionRequest;
import com.ynet.mgmt.searchdata.enums.SuggestionType;
import com.ynet.mgmt.searchdata.service.SearchSuggestionService;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索建议服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class SearchSuggestionServiceImpl implements SearchSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(SearchSuggestionServiceImpl.class);

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private HotTopicRepository hotTopicRepository;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private SearchSpaceService searchSpaceService;

    // Caffeine 本地缓存，用于缓存 completion 字段映射
    private final Cache<String, List<String>> completionFieldsCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    @Override
    public List<SearchSuggestionDTO> getSuggestions(SearchSuggestionRequest request) {
        logger.info("获取搜索建议: query={}, userId={}, searchSpaceId={}",
            request.getQuery(), request.getUserId(), request.getSearchSpaceId());

        try {
            // 使用 CompletableFuture 并行执行4个数据源查询
            java.util.concurrent.CompletableFuture<List<SearchSuggestionDTO>> personalHistoryFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> getPersonalHistorySuggestions(request));

            java.util.concurrent.CompletableFuture<List<SearchSuggestionDTO>> globalHistoryFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> getGlobalHistorySuggestions(request));

            java.util.concurrent.CompletableFuture<List<SearchSuggestionDTO>> hotTopicsFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> getHotTopicSuggestions(request));

            java.util.concurrent.CompletableFuture<List<SearchSuggestionDTO>> esCompletionsFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> getESCompletionSuggestions(request));

            // 等待所有查询完成，设置300ms超时
            java.util.concurrent.CompletableFuture<Void> allFutures = java.util.concurrent.CompletableFuture.allOf(
                personalHistoryFuture, globalHistoryFuture, hotTopicsFuture, esCompletionsFuture
            );

            try {
                allFutures.get(300, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                logger.warn("搜索建议查询超时: query={}, 使用部分结果", request.getQuery());
            }

            // 合并结果（即使超时也返回已完成的结果）
            List<SearchSuggestionDTO> suggestions = new ArrayList<>();
            suggestions.addAll(personalHistoryFuture.getNow(Collections.emptyList()));
            suggestions.addAll(globalHistoryFuture.getNow(Collections.emptyList()));
            suggestions.addAll(hotTopicsFuture.getNow(Collections.emptyList()));
            suggestions.addAll(esCompletionsFuture.getNow(Collections.emptyList()));

            // 去重、排序、限制数量
            List<SearchSuggestionDTO> result = mergeSuggestions(suggestions, request.getQuery(), request.getSize());

            logger.info("搜索建议生成成功: query={}, count={}", request.getQuery(), result.size());
            return result;

        } catch (Exception e) {
            logger.error("获取搜索建议失败: query={}", request.getQuery(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取个人搜索历史建议
     */
    private List<SearchSuggestionDTO> getPersonalHistorySuggestions(SearchSuggestionRequest request) {
        if (request.getUserId() == null) {
            return Collections.emptyList();
        }

        try {
            // 查询最近7天的搜索记录（优化：减少扫描范围）
            LocalDateTime startTime = LocalDateTime.now().minusDays(7);
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);

            // 使用轻量级查询（不加载 responseData 大字段）
            List<Object[]> logs;
            if (request.getSearchSpaceId() != null) {
                logs = searchLogRepository.findMinimalByUserIdAndSearchSpaceIdAndQueryAndCreatedAtAfter(
                    request.getUserId(),
                    request.getSearchSpaceId(),
                    request.getQuery(),
                    startTime,
                    pageable
                );
            } else {
                logs = searchLogRepository.findMinimalByUserIdAndQueryAndCreatedAtAfter(
                    request.getUserId(),
                    request.getQuery(),
                    startTime,
                    pageable
                );
            }

            // 按查询词分组统计
            Map<String, List<LocalDateTime>> groupedLogs = new HashMap<>();
            for (Object[] log : logs) {
                String searchQuery = (String) log[0];
                LocalDateTime createdAt = (LocalDateTime) log[1];
                groupedLogs.computeIfAbsent(searchQuery, k -> new ArrayList<>()).add(createdAt);
            }

            // 转换为建议并计算得分
            return groupedLogs.entrySet().stream()
                .map(entry -> {
                    String query = entry.getKey();
                    List<LocalDateTime> queryTimes = entry.getValue();

                    // 计算最后搜索时间距今天数
                    long daysAgo = ChronoUnit.DAYS.between(
                        queryTimes.stream().max(LocalDateTime::compareTo).orElse(LocalDateTime.now()),
                        LocalDateTime.now()
                    );

                    // 个人历史得分: frequency * e^(-0.1 * days) * 0.35 * 0.7
                    double frequency = queryTimes.size();
                    double timeDecay = Math.exp(-0.1 * daysAgo);
                    double score = (frequency * timeDecay / 10.0) * 35.0 * 0.7;

                    SearchSuggestionDTO dto = new SearchSuggestionDTO(query, score, SuggestionType.HISTORY);
                    dto.addMetadata("searchCount", frequency);
                    dto.addMetadata("lastSearchDays", daysAgo);
                    dto.addMetadata("source", "personal");
                    return dto;
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("获取个人历史建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取全局搜索历史建议
     */
    private List<SearchSuggestionDTO> getGlobalHistorySuggestions(SearchSuggestionRequest request) {
        try {
            // 查询最近7天的全局搜索记录（优化：减少扫描范围）
            LocalDateTime startTime = LocalDateTime.now().minusDays(7);
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);

            // 使用轻量级查询（不加载 responseData 大字段）
            List<Object[]> logs;
            if (request.getSearchSpaceId() != null) {
                logs = searchLogRepository.findMinimalBySearchSpaceIdAndQueryAndCreatedAtAfter(
                    request.getSearchSpaceId(),
                    request.getQuery(),
                    startTime,
                    pageable
                );
            } else {
                logs = searchLogRepository.findMinimalByQueryAndCreatedAtAfter(
                    request.getQuery(),
                    startTime,
                    pageable
                );
            }

            // 按查询词分组统计
            Map<String, List<LocalDateTime>> groupedLogs = new HashMap<>();
            for (Object[] log : logs) {
                String searchQuery = (String) log[0];
                LocalDateTime createdAt = (LocalDateTime) log[1];
                groupedLogs.computeIfAbsent(searchQuery, k -> new ArrayList<>()).add(createdAt);
            }

            // 转换为建议并计算得分
            return groupedLogs.entrySet().stream()
                .map(entry -> {
                    String query = entry.getKey();
                    List<LocalDateTime> queryTimes = entry.getValue();

                    // 计算最后搜索时间距今天数
                    long daysAgo = ChronoUnit.DAYS.between(
                        queryTimes.stream().max(LocalDateTime::compareTo).orElse(LocalDateTime.now()),
                        LocalDateTime.now()
                    );

                    // 全局历史得分: frequency * e^(-0.1 * days) * 0.35 * 0.3
                    double frequency = queryTimes.size();
                    double timeDecay = Math.exp(-0.1 * daysAgo);
                    double score = (frequency * timeDecay / 100.0) * 35.0 * 0.3;

                    SearchSuggestionDTO dto = new SearchSuggestionDTO(query, score, SuggestionType.HISTORY);
                    dto.addMetadata("searchCount", frequency);
                    dto.addMetadata("lastSearchDays", daysAgo);
                    dto.addMetadata("source", "global");
                    return dto;
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("获取全局历史建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取热门话题建议
     */
    private List<SearchSuggestionDTO> getHotTopicSuggestions(SearchSuggestionRequest request) {
        try {
            // 查询可见的热门话题
            List<HotTopic> hotTopics = hotTopicRepository.findByVisibleTrueOrderByPopularityDesc();

            String query = request.getQuery().toLowerCase();

            // 过滤匹配的话题（支持中文前缀、拼音前缀和首字母前缀匹配）
            List<HotTopic> matchedTopics = hotTopics.stream()
                .filter(topic -> {
                    String name = topic.getName().toLowerCase();
                    String pinyin = topic.getPinyin();
                    String firstLetter = topic.getPinyinFirstLetter();

                    // 中文前缀匹配
                    if (name.startsWith(query)) {
                        return true;
                    }

                    // 拼音前缀匹配（如果拼音字段存在）
                    if (pinyin != null && pinyin.toLowerCase().startsWith(query)) {
                        return true;
                    }

                    // 首字母前缀匹配（如果首字母字段存在）
                    if (firstLetter != null && firstLetter.toLowerCase().startsWith(query)) {
                        return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());

            if (matchedTopics.isEmpty()) {
                return Collections.emptyList();
            }

            // 获取最大热度值用于标准化
            int maxPopularity = hotTopics.stream()
                .mapToInt(HotTopic::getPopularity)
                .max()
                .orElse(1);

            // 转换为建议并计算得分
            return matchedTopics.stream()
                .map(topic -> {
                    // 热度得分: (popularity / max_popularity) * 25
                    double normalizedPopularity = (double) topic.getPopularity() / maxPopularity;
                    double score = normalizedPopularity * 25.0;

                    SearchSuggestionDTO dto = new SearchSuggestionDTO(
                        topic.getName(),
                        score,
                        SuggestionType.HOT_TOPIC
                    );
                    dto.addMetadata("popularity", topic.getPopularity());
                    dto.addMetadata("topicId", topic.getId());
                    return dto;
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("获取热门话题建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取ES Completion建议
     * 混合策略：使用completion suggester（中文）+ prefix query（拼音）
     */
    private List<SearchSuggestionDTO> getESCompletionSuggestions(SearchSuggestionRequest request) {
        try {
            // 获取索引名称列表
            List<String> indexNames = getIndexNames(request);
            if (indexNames == null || indexNames.isEmpty()) {
                logger.debug("未指定搜索空间，跳过ES Completion建议");
                return Collections.emptyList();
            }

            logger.debug("查询ES建议: indices={}, query={}", indexNames, request.getQuery());

            List<SearchSuggestionDTO> allSuggestions = new ArrayList<>();

            for (String indexName : indexNames) {
                try {
                    // 策略1：使用completion suggester（支持中文前缀匹配）
                    allSuggestions.addAll(getCompletionSuggesterResults(indexName, request));

                    // 策略2：使用prefix query在chinese_pinyin字段上查询（支持拼音匹配）
                    allSuggestions.addAll(getPinyinPrefixResults(indexName, request));

                } catch (Exception e) {
                    logger.warn("索引 {} 的建议查询失败: {}", indexName, e.getMessage());
                }
            }

            logger.debug("ES建议查询完成: indices={}, count={}", indexNames, allSuggestions.size());
            return allSuggestions;

        } catch (Exception e) {
            logger.error("ES建议查询失败（未知异常）", e);
            return Collections.emptyList();
        }
    }

    /**
     * 使用completion suggester获取建议
     */
    @SuppressWarnings("unchecked")
    private List<SearchSuggestionDTO> getCompletionSuggesterResults(String indexName, SearchSuggestionRequest request) {
        List<SearchSuggestionDTO> suggestions = new ArrayList<>();

        try {
            // 获取该索引的 completion 字段
            List<String> completionFields = getCompletionFields(indexName);
            if (completionFields.isEmpty()) {
                logger.debug("索引 {} 中没有找到 completion 字段", indexName);
                return suggestions;
            }

            logger.debug("索引 {} 使用 completion 字段: {}", indexName, completionFields);

            // 构建 completion suggest 查询
            SearchRequest searchRequest = SearchRequest.of(s -> {
                var requestBuilder = s.index(indexName).size(0);

                // 为每个 completion 字段创建一个 suggester
                requestBuilder.suggest(suggester -> {
                    for (String field : completionFields) {
                        suggester.suggesters(field.replace(".", "_"), suggest -> suggest
                            .prefix(request.getQuery())
                            .completion(completion -> {
                                var builder = completion
                                    .field(field)
                                    .size(request.getSize())
                                    .skipDuplicates(true);

                                if (request.getEnableFuzzy() != null && request.getEnableFuzzy()) {
                                    builder.fuzzy(f -> f
                                        .fuzziness("AUTO")
                                        .transpositions(true)
                                        .minLength(3)
                                    );
                                }

                                return builder;
                            })
                        );
                    }
                    return suggester;
                });

                return requestBuilder;
            });

            // 执行查询
            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            // 处理建议结果
            if (response.suggest() != null) {
                for (Map.Entry<String, List<Suggestion<Map>>> entry : response.suggest().entrySet()) {
                    for (Suggestion<Map> suggestion : entry.getValue()) {
                        if (suggestion.isCompletion() && suggestion.completion() != null) {
                            for (CompletionSuggestOption<Map> option : suggestion.completion().options()) {
                                Double optionScore = option.score();
                                double score = (optionScore != null ? optionScore.doubleValue() : 1.0) * 40.0;

                                SearchSuggestionDTO dto = new SearchSuggestionDTO(
                                    option.text(),
                                    score,
                                    SuggestionType.ES_COMPLETION
                                );
                                dto.addMetadata("esScore", optionScore);
                                dto.addMetadata("method", "completion");
                                dto.addMetadata("index", indexName);
                                suggestions.add(dto);

                                logger.debug("Completion建议: text={}, score={}", option.text(), score);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Completion suggester查询失败: index={}, error={}", indexName, e.getMessage());
        }

        return suggestions;
    }

    /**
     * 使用match query在拼音字段上查询
     * 注意:拼音分析器是按字分词的,所以使用match query + operator:and来匹配多个拼音tokens
     */
    @SuppressWarnings("unchecked")
    private List<SearchSuggestionDTO> getPinyinPrefixResults(String indexName, SearchSuggestionRequest request) {
        List<SearchSuggestionDTO> suggestions = new ArrayList<>();

        try {
            // 构建match query查询拼音字段
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .size(request.getSize())
                .query(q -> q
                    .bool(b -> b
                        .should(sh -> sh
                            .match(m -> m
                                .field("name.chinese_pinyin")
                                .query(request.getQuery().toLowerCase())
                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                .boost(2.0f)
                            )
                        )
                        .should(sh -> sh
                            .match(m -> m
                                .field("descript.chinese_pinyin")
                                .query(request.getQuery().toLowerCase())
                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                .boost(1.0f)
                            )
                        )
                        .should(sh -> sh
                            .match(m -> m
                                .field("category.chinese_pinyin")
                                .query(request.getQuery().toLowerCase())
                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                .boost(1.5f)
                            )
                        )
                        .minimumShouldMatch("1")
                    )
                )
                .source(src -> src
                    .filter(f -> f
                        .includes("name", "descript", "category")
                    )
                )
            );

            // 执行查询
            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            // 处理查询结果
            if (response.hits() != null && response.hits().hits() != null) {
                for (var hit : response.hits().hits()) {
                    Map source = hit.source();
                    if (source != null && source.containsKey("name")) {
                        String text = source.get("name").toString();
                        Double hitScore = hit.score();
                        double score = (hitScore != null ? hitScore : 1.0) * 40.0;

                        SearchSuggestionDTO dto = new SearchSuggestionDTO(
                            text,
                            score,
                            SuggestionType.ES_COMPLETION
                        );
                        dto.addMetadata("esScore", hitScore);
                        dto.addMetadata("method", "pinyin_match");
                        dto.addMetadata("index", indexName);
                        suggestions.add(dto);

                        logger.debug("拼音匹配建议: text={}, score={}", text, score);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("拼音match查询失败: index={}, error={}", indexName, e.getMessage());
        }

        return suggestions;
    }

    /**
     * 获取索引名称列表（支持向后兼容）
     */
    private List<String> getIndexNames(SearchSuggestionRequest request) {
        List<String> indexNames = new ArrayList<>();

        try {
            // 优先使用新的 searchSpaceIds
            if (request.getSearchSpaceIds() != null && !request.getSearchSpaceIds().isEmpty()) {
                for (Long spaceId : request.getSearchSpaceIds()) {
                    try {
                        SearchSpaceDTO searchSpace = searchSpaceService.getSearchSpace(spaceId);
                        indexNames.add(searchSpace.getIndexName());
                    } catch (Exception e) {
                        logger.warn("获取搜索空间索引名失败: searchSpaceId={}", spaceId, e);
                    }
                }
            }
            // 向后兼容：如果没有 searchSpaceIds，使用旧的 searchSpaceId
            else if (request.getSearchSpaceId() != null) {
                SearchSpaceDTO searchSpace = searchSpaceService.getSearchSpace(request.getSearchSpaceId());
                indexNames.add(searchSpace.getIndexName());
            }
        } catch (Exception e) {
            logger.warn("获取索引名称列表失败", e);
        }

        return indexNames;
    }

    /**
     * 动态获取索引中的 completion 字段
     * 使用 Caffeine 本地缓存避免频繁查询 ES mapping
     *
     * @param indexName 索引名称
     * @return completion 字段路径列表
     */
    private List<String> getCompletionFields(String indexName) {
        // 尝试从缓存获取
        List<String> cached = completionFieldsCache.getIfPresent(indexName);
        if (cached != null) {
            logger.debug("从缓存获取 completion 字段: {}", cached);
            return cached;
        }

        // 缓存未命中，查询 ES
        Set<String> completionFields = new HashSet<>();

        try {
            // 获取索引的 mapping
            var getMappingRequest = co.elastic.clients.elasticsearch.indices.GetMappingRequest.of(r -> r
                .index(indexName)
            );

            var mappingResponse = elasticsearchClient.indices().getMapping(getMappingRequest);

            // 遍历索引的 mapping
            mappingResponse.result().forEach((index, indexMappingRecord) -> {
                var properties = indexMappingRecord.mappings().properties();

                // 递归查找 completion 类型的字段
                findCompletionFields(properties, "", completionFields);
            });

            logger.debug("从索引 {} 中找到 completion 字段: {}", indexName, completionFields);

        } catch (Exception e) {
            logger.warn("获取索引 {} 的 mapping 失败: {}", indexName, e.getMessage());
        }

        List<String> result = new ArrayList<>(completionFields);

        // 存入缓存
        completionFieldsCache.put(indexName, result);

        return result;
    }

    /**
     * 递归查找 completion 类型的字段
     *
     * @param properties 字段属性映射
     * @param prefix     字段路径前缀
     * @param result     结果集合
     */
    private void findCompletionFields(
            Map<String, co.elastic.clients.elasticsearch._types.mapping.Property> properties,
            String prefix,
            Set<String> result) {

        if (properties == null) {
            return;
        }

        for (Map.Entry<String, co.elastic.clients.elasticsearch._types.mapping.Property> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            co.elastic.clients.elasticsearch._types.mapping.Property property = entry.getValue();

            String fullPath = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;

            // 检查是否为 completion 类型
            if (property.isCompletion()) {
                result.add(fullPath);
                logger.debug("找到 completion 字段: {}", fullPath);
            }

            // 递归处理子字段（multifields）
            if (property.isText() && property.text().fields() != null) {
                findCompletionFields(property.text().fields(), fullPath, result);
            } else if (property.isKeyword() && property.keyword().fields() != null) {
                findCompletionFields(property.keyword().fields(), fullPath, result);
            } else if (property.isObject() && property.object().properties() != null) {
                findCompletionFields(property.object().properties(), fullPath, result);
            }
        }
    }

    /**
     * 合并建议列表：去重、按前缀匹配和得分排序、限制数量
     */
    private List<SearchSuggestionDTO> mergeSuggestions(
            List<SearchSuggestionDTO> suggestions,
            String query,
            int maxSize) {

        // 按文本去重，保留得分最高的
        Map<String, SearchSuggestionDTO> uniqueSuggestions = suggestions.stream()
            .collect(Collectors.toMap(
                SearchSuggestionDTO::getText,
                dto -> dto,
                (dto1, dto2) -> dto1.getScore() > dto2.getScore() ? dto1 : dto2
            ));

        // 排序逻辑：
        // 1. 优先按是否以查询词开头排序（前缀匹配优先）
        // 2. 相同前缀匹配状态下，按得分降序排序
        String lowerQuery = query.toLowerCase();
        return uniqueSuggestions.values().stream()
            .sorted(Comparator
                // 先按是否前缀匹配排序（true 排在前面）
                .comparing((SearchSuggestionDTO dto) -> dto.getText().toLowerCase().startsWith(lowerQuery))
                .reversed()
                // 再按得分降序排序
                .thenComparing(SearchSuggestionDTO::getScore, Comparator.reverseOrder())
            )
            .limit(maxSize)
            .collect(Collectors.toList());
    }
}
