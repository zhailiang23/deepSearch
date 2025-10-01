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
            List<SearchSuggestionDTO> result = mergeSuggestions(suggestions, request.getSize());

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

            // 过滤以查询词开头的话题（前缀匹配）
            List<HotTopic> matchedTopics = hotTopics.stream()
                .filter(topic -> topic.getName().toLowerCase().startsWith(request.getQuery().toLowerCase()))
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
     */
    private List<SearchSuggestionDTO> getESCompletionSuggestions(SearchSuggestionRequest request) {
        try {
            // 获取索引名称列表
            List<String> indexNames = getIndexNames(request);
            if (indexNames == null || indexNames.isEmpty()) {
                logger.debug("未指定搜索空间，跳过ES Completion建议");
                return Collections.emptyList();
            }

            logger.debug("查询ES Completion建议: indices={}, query={}", indexNames, request.getQuery());

            // 获取 completion 字段（固定返回 name）
            List<String> completionFields = getCompletionFields(indexNames);
            if (completionFields.isEmpty()) {
                logger.debug("索引中没有找到 completion 字段");
                return Collections.emptyList();
            }

            logger.debug("使用 completion 字段: {}", completionFields);

            // 构建 completion suggest 查询
            SearchRequest searchRequest = SearchRequest.of(s -> {
                var requestBuilder = s.index(indexNames).size(0);

                // 为每个 completion 字段创建一个 suggester
                requestBuilder.suggest(suggester -> {
                    for (String field : completionFields) {
                        suggester.suggesters(field.replace(".", "_"), suggest -> suggest
                            .prefix(request.getQuery())
                            .completion(completion -> {
                                var builder = completion
                                    .field(field)
                                    .size(request.getSize())
                                    .skipDuplicates(true);  // 跳过重复建议

                                // 如果启用模糊匹配
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

            // 处理建议结果 - 遍历所有 suggester
            List<SearchSuggestionDTO> suggestions = new ArrayList<>();

            if (response.suggest() != null) {
                // 遍历所有字段的 suggester 结果
                for (Map.Entry<String, List<Suggestion<Map>>> entry : response.suggest().entrySet()) {
                    String suggesterName = entry.getKey();
                    List<Suggestion<Map>> suggestionList = entry.getValue();

                    logger.debug("处理 suggester: {}", suggesterName);

                    for (Suggestion<Map> suggestion : suggestionList) {
                        if (suggestion.isCompletion() && suggestion.completion() != null) {
                            for (CompletionSuggestOption<Map> option : suggestion.completion().options()) {
                                // ES Completion 得分: score * 40.0
                                // ES的score通常是0-1之间的浮点数，需要归一化
                                Double optionScore = option.score();
                                double score = (optionScore != null ? optionScore.doubleValue() : 1.0) * 40.0;

                                SearchSuggestionDTO dto = new SearchSuggestionDTO(
                                    option.text(),
                                    score,
                                    SuggestionType.ES_COMPLETION
                                );
                                dto.addMetadata("esScore", optionScore);
                                dto.addMetadata("field", suggesterName);
                                dto.addMetadata("indices", indexNames);
                                suggestions.add(dto);

                                logger.debug("找到建议: text={}, score={}, field={}",
                                    option.text(), score, suggesterName);
                            }
                        }
                    }
                }
            }

            logger.debug("ES Completion建议查询完成: indices={}, count={}", indexNames, suggestions.size());
            return suggestions;

        } catch (ElasticsearchException e) {
            logger.warn("ES Completion建议查询失败（ES异常）: {}", e.getMessage());
            return Collections.emptyList();
        } catch (IOException e) {
            logger.error("ES Completion建议查询失败（IO异常）", e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("ES Completion建议查询失败（未知异常）", e);
            return Collections.emptyList();
        }
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
     * @param indexNames 索引名称列表
     * @return completion 字段路径列表
     */
    private List<String> getCompletionFields(List<String> indexNames) {
        // 生成缓存key
        String cacheKey = String.join(",", indexNames);

        // 尝试从缓存获取
        List<String> cached = completionFieldsCache.getIfPresent(cacheKey);
        if (cached != null) {
            logger.debug("从缓存获取 completion 字段: {}", cached);
            return cached;
        }

        // 缓存未命中，查询 ES
        Set<String> completionFields = new HashSet<>();

        try {
            for (String indexName : indexNames) {
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

                } catch (Exception e) {
                    logger.warn("获取索引 {} 的 mapping 失败: {}", indexName, e.getMessage());
                }
            }

            logger.debug("从索引 {} 中找到 completion 字段: {}", indexNames, completionFields);

        } catch (Exception e) {
            logger.error("获取 completion 字段失败", e);
        }

        List<String> result = new ArrayList<>(completionFields);

        // 存入缓存
        completionFieldsCache.put(cacheKey, result);

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
     * 合并建议列表：去重、按得分排序、限制数量
     */
    private List<SearchSuggestionDTO> mergeSuggestions(
            List<SearchSuggestionDTO> suggestions,
            int maxSize) {

        // 按文本去重，保留得分最高的
        Map<String, SearchSuggestionDTO> uniqueSuggestions = suggestions.stream()
            .collect(Collectors.toMap(
                SearchSuggestionDTO::getText,
                dto -> dto,
                (dto1, dto2) -> dto1.getScore() > dto2.getScore() ? dto1 : dto2
            ));

        // 按得分降序排序并限制数量
        return uniqueSuggestions.values().stream()
            .sorted(Comparator.comparing(SearchSuggestionDTO::getScore).reversed())
            .limit(maxSize)
            .collect(Collectors.toList());
    }
}
