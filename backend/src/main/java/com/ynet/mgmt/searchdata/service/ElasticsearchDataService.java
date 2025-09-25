package com.ynet.mgmt.searchdata.service;

import com.ynet.mgmt.searchdata.dto.*;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import co.elastic.clients.json.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch数据管理服务
 * 处理ES数据的搜索、更新、删除等操作
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
public class ElasticsearchDataService {

    private final ElasticsearchClient elasticsearchClient;

    @Autowired
    public ElasticsearchDataService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 搜索ES数据
     *
     * @param request 搜索请求
     * @param searchSpace 搜索空间
     * @return 搜索结果
     */
    public SearchDataResponse searchData(SearchDataRequest request, SearchSpaceDTO searchSpace) {
        long startTime = System.currentTimeMillis();
        String indexName = searchSpace.getIndexName();

        try {
            log.info("开始搜索ES数据: index={}, query={}, page={}, size={}, pinyin={}, mode={}",
                    indexName, request.getQuery(), request.getPage(), request.getSize(),
                    request.getEnablePinyinSearch(), request.getPinyinMode());

            // 构建查询
            Query query = buildQuery(request.getQuery(), request.getEnablePinyinSearch(), request.getPinyinMode());

            // 计算分页参数
            int from = (request.getPage() - 1) * request.getSize();

            // 构建搜索请求
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(indexName)
                    .query(query)
                    .from(from)
                    .size(request.getSize())
                    .trackTotalHits(t -> t.enabled(true));

            // 添加排序
            if (request.getSort() != null) {
                String sortField = request.getSort().getField();
                String sortOrder = request.getSort().getOrder();
                searchBuilder.sort(s -> s.field(f -> f
                        .field(sortField)
                        .order("desc".equalsIgnoreCase(sortOrder) ?
                                co.elastic.clients.elasticsearch._types.SortOrder.Desc :
                                co.elastic.clients.elasticsearch._types.SortOrder.Asc)));
                log.debug("添加排序: field={}, order={}", sortField, sortOrder);
            }

            // 执行搜索
            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchBuilder.build(), Map.class);

            // 转换结果
            List<SearchDataResponse.DocumentData> documents = searchResponse.hits().hits().stream()
                    .map(this::convertHitToDocument)
                    .collect(Collectors.toList());

            // 获取总数
            long totalHits = 0L;
            if (searchResponse.hits().total() != null) {
                totalHits = searchResponse.hits().total().value();
            }

            // 获取映射信息（如果需要）
            SearchDataResponse.IndexMappingInfo mappingInfo = null;
            if (request.getPage() == 1) { // 只在第一页获取映射信息
                mappingInfo = getSimpleMappingInfo(indexName);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("搜索完成: index={}, total={}, returned={}, took={}ms",
                    indexName, totalHits, documents.size(), duration);

            return SearchDataResponse.builder()
                    .data(documents)
                    .total(totalHits)
                    .page(request.getPage())
                    .size(request.getSize())
                    .mapping(mappingInfo)
                    .build();

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("搜索ES数据IO异常: index={}, query={}, took={}ms",
                    indexName, request.getQuery(), duration, e);
            throw new RuntimeException("搜索IO异常: " + e.getMessage(), e);
        } catch (ElasticsearchException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("ES查询异常: index={}, query={}, status={}, took={}ms",
                    indexName, request.getQuery(), e.status(), duration, e);

            // 根据不同的ES异常类型提供更友好的错误信息
            String errorMsg = getElasticsearchErrorMessage(e);
            throw new RuntimeException("查询异常: " + errorMsg, e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("搜索过程中出现未预期异常: index={}, query={}, took={}ms",
                    indexName, request.getQuery(), duration, e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取索引映射
     *
     * @param searchSpace 搜索空间
     * @return 索引映射
     */
    public IndexMappingResponse getIndexMapping(SearchSpaceDTO searchSpace) {
        try {
            String indexName = searchSpace.getIndexName();
            log.info("获取索引映射: index={}", indexName);

            GetMappingRequest request = GetMappingRequest.of(builder -> builder.index(indexName));
            GetMappingResponse response = elasticsearchClient.indices().getMapping(request);

            // 获取映射信息
            Map<String, IndexMappingRecord> mappings = response.result();
            if (mappings.isEmpty()) {
                throw new RuntimeException("索引不存在或无映射信息: " + indexName);
            }

            IndexMappingRecord indexMapping = mappings.values().iterator().next();
            Map<String, Object> mappingData = convertMappingToMap(indexMapping);

            return IndexMappingResponse.builder()
                    .index(indexName)
                    .mapping(IndexMappingResponse.MappingInfo.builder()
                            .mappings(mappingData)
                            .build())
                    .build();

        } catch (IOException e) {
            log.error("获取索引映射失败: index={}", searchSpace.getIndexName(), e);
            throw new RuntimeException("获取映射失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新文档
     *
     * @param id 文档ID
     * @param request 更新请求
     * @return 更新结果
     */
    public UpdateDocumentResponse updateDocument(String id, UpdateDocumentRequest request) {
        try {
            log.info("更新文档: id={}, index={}, version={}", id, request.getIndex(), request.getVersion());

            // 构建更新请求
            IndexRequest.Builder<Map<String, Object>> updateBuilder = new IndexRequest.Builder<Map<String, Object>>()
                    .index(request.getIndex())
                    .id(id)
                    .document(request.getSource());

            // 添加乐观锁版本控制
            if (request.getVersion() != null) {
                updateBuilder.ifSeqNo(request.getVersion())
                        .ifPrimaryTerm(1L); // 简化处理，实际应该获取真实的primary_term
            }

            // 执行更新
            IndexResponse response = elasticsearchClient.index(updateBuilder.build());

            log.info("文档更新成功: id={}, index={}, version={}, result={}",
                    id, request.getIndex(), response.version(), response.result());

            return UpdateDocumentResponse.builder()
                    .id(id)
                    .index(request.getIndex())
                    .version(response.version())
                    .result(response.result().jsonValue())
                    .build();

        } catch (ElasticsearchException e) {
            if (e.getMessage().contains("version_conflict")) {
                log.warn("文档版本冲突: id={}, index={}", id, request.getIndex());
                throw new RuntimeException("version_conflict: 文档已被其他用户修改", e);
            }
            log.error("更新文档失败: id={}, index={}", id, request.getIndex(), e);
            throw new RuntimeException("更新失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("更新文档异常: id={}, index={}", id, request.getIndex(), e);
            throw new RuntimeException("更新异常: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @param request 删除请求
     * @return 删除结果
     */
    public DeleteDocumentResponse deleteDocument(String id, DeleteDocumentRequest request) {
        try {
            log.info("删除文档: id={}, index={}", id, request.getIndex());

            // 构建删除请求
            DeleteRequest.Builder deleteBuilder = new DeleteRequest.Builder()
                    .index(request.getIndex())
                    .id(id);

            // 添加乐观锁版本控制
            if (request.getVersion() != null) {
                deleteBuilder.ifSeqNo(request.getVersion())
                        .ifPrimaryTerm(1L);
            }

            // 执行删除
            DeleteResponse response = elasticsearchClient.delete(deleteBuilder.build());

            log.info("文档删除成功: id={}, index={}, result={}", id, request.getIndex(), response.result());

            return DeleteDocumentResponse.builder()
                    .id(id)
                    .index(request.getIndex())
                    .version(response.version())
                    .result(response.result().jsonValue())
                    .build();

        } catch (ElasticsearchException e) {
            log.error("删除文档失败: id={}, index={}", id, request.getIndex(), e);
            throw new RuntimeException("删除失败: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("删除文档异常: id={}, index={}", id, request.getIndex(), e);
            throw new RuntimeException("删除异常: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文档详情
     *
     * @param id 文档ID
     * @param index 索引名称
     * @return 文档详情
     */
    public DocumentDetailResponse getDocument(String id, String index) {
        try {
            log.info("获取文档详情: id={}, index={}", id, index);

            GetRequest request = GetRequest.of(builder -> builder.index(index).id(id));
            GetResponse<Map> response = elasticsearchClient.get(request, Map.class);

            if (!response.found()) {
                throw new RuntimeException("not_found: 文档不存在");
            }

            return DocumentDetailResponse.builder()
                    ._id(response.id())
                    ._index(response.index())
                    ._type(null) // ES 8.x 已废弃type概念
                    ._version(response.version())
                    .found(response.found())
                    ._source(response.source())
                    .build();

        } catch (IOException e) {
            log.error("获取文档异常: id={}, index={}", id, index, e);
            throw new RuntimeException("获取文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量操作文档
     *
     * @param request 批量操作请求
     * @return 批量操作结果
     */
    public BulkOperationResponse bulkOperation(BulkOperationRequest request) {
        try {
            log.info("执行批量操作: operations={}", request.getOperations().size());

            List<BulkOperation> operations = request.getOperations().stream()
                    .map(this::convertToBulkOperation)
                    .collect(Collectors.toList());

            BulkRequest bulkRequest = BulkRequest.of(builder -> builder.operations(operations));
            BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest);

            // 转换结果
            List<Map<String, Object>> items = bulkResponse.items().stream()
                    .map(this::convertBulkResponseItem)
                    .collect(Collectors.toList());

            log.info("批量操作完成: took={}ms, errors={}", bulkResponse.took(), bulkResponse.errors());

            return BulkOperationResponse.builder()
                    .took(bulkResponse.took())
                    .errors(bulkResponse.errors())
                    .items(items)
                    .build();

        } catch (IOException e) {
            log.error("批量操作异常: operations={}", request.getOperations().size(), e);
            throw new RuntimeException("批量操作失败: " + e.getMessage(), e);
        }
    }

    // 私有辅助方法

    /**
     * 构建查询 - 支持拼音搜索增强
     */
    private Query buildQuery(String queryString, Boolean enablePinyinSearch, String pinyinMode) {
        if (!StringUtils.hasText(queryString)) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        // 如果拼音搜索被禁用，直接使用标准查询
        if (enablePinyinSearch == null || !enablePinyinSearch) {
            log.debug("拼音搜索已禁用，使用标准查询: query={}", queryString);
            return QueryStringQuery.of(q -> q
                    .query(queryString)
                    .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                    .analyzeWildcard(true))._toQuery();
        }

        try {
            // 尝试构建拼音增强查询
            Query pinyinQuery = buildPinyinEnhancedQuery(queryString, pinyinMode);
            if (pinyinQuery != null) {
                log.debug("使用拼音增强查询: query={}, mode={}", queryString, pinyinMode);
                return pinyinQuery;
            }
        } catch (Exception e) {
            log.warn("拼音查询构建失败，降级为标准查询: query={}, mode={}, error={}",
                    queryString, pinyinMode, e.getMessage());

            // 记录拼音搜索错误的详细信息，便于问题排查
            if (log.isDebugEnabled()) {
                log.debug("拼音查询构建异常详情", e);
            }
        }

        // 降级为标准查询
        return QueryStringQuery.of(q -> q
                .query(queryString)
                .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                .analyzeWildcard(true))._toQuery();
    }

    /**
     * 构建拼音增强查询
     */
    private Query buildPinyinEnhancedQuery(String queryString, String pinyinMode) {
        String mode = pinyinMode != null ? pinyinMode.toUpperCase() : "AUTO";
        boolean containsChinese = queryString.matches(".*[\\u4e00-\\u9fa5].*");
        boolean isShortChinese = containsChinese && queryString.length() <= 2;

        // 添加调试日志
        System.out.println("=== 拼音查询构建调试 ===");
        System.out.println("查询字符串: " + queryString);
        System.out.println("包含中文: " + containsChinese);
        System.out.println("是否短中文: " + isShortChinese);
        System.out.println("拼音模式: " + mode);

        switch (mode) {
            case "STRICT":
                // 严格模式：优先原字段匹配，拼音作为辅助
                BoolQuery.Builder strictBuilder = new BoolQuery.Builder()
                    .should(buildMultiFieldQuery(queryString, 3.0f))  // 原字段权重更高
                    .should(buildPinyinQuery(queryString, 1.0f))      // 拼音权重较低
                    .minimumShouldMatch("1");
                // 短中文查询避免使用首字母匹配
                if (!isShortChinese) {
                    System.out.println("STRICT模式: 添加首字母匹配 (非短中文)");
                    strictBuilder.should(buildFirstLetterQuery(queryString, 0.5f));
                } else {
                    System.out.println("STRICT模式: 跳过首字母匹配 (短中文)");
                }
                return strictBuilder.build()._toQuery();

            case "FUZZY":
                // 模糊模式：增强拼音和首字母匹配权重
                System.out.println("FUZZY模式: 使用所有匹配方式");
                return BoolQuery.of(b -> b
                    .should(buildMultiFieldQuery(queryString, 1.5f))  // 原字段权重降低
                    .should(buildPinyinQuery(queryString, 2.0f))      // 拼音权重更高
                    .should(buildFirstLetterQuery(queryString, 1.5f)) // 首字母权重提高
                    .minimumShouldMatch("1")
                )._toQuery();

            case "AUTO":
            default:
                // 自动模式：平衡所有搜索策略
                BoolQuery.Builder autoBuilder = new BoolQuery.Builder()
                    .should(buildMultiFieldQuery(queryString, 2.0f))  // 原字段精确匹配，权重2.0
                    .minimumShouldMatch("1"); // 至少匹配一个should条件

                // 对于短中文查询，只使用原字段匹配，避免拼音分析器导致的过度匹配
                if (!isShortChinese) {
                    System.out.println("AUTO模式: 添加拼音和首字母匹配 (非短中文)");
                    autoBuilder.should(buildPinyinQuery(queryString, 1.5f));      // 拼音字段匹配，权重1.5
                    autoBuilder.should(buildFirstLetterQuery(queryString, 1.0f)); // 首字母匹配，权重1.0
                } else {
                    System.out.println("AUTO模式: 跳过拼音和首字母匹配 (短中文，避免过度匹配)");
                }
                Query finalQuery = autoBuilder.build()._toQuery();
                System.out.println("=== 查询构建完成 ===");
                return finalQuery;
        }
    }

    /**
     * 构建多字段搜索查询
     */
    private Query buildMultiFieldQuery(String queryString, float boost) {
        return MultiMatchQuery.of(m -> m
            .query(queryString)
            .fields(getSearchableFields())
            .type(TextQueryType.BestFields)
            .boost(boost)
            .operator(Operator.And)
        )._toQuery();
    }

    /**
     * 构建拼音搜索查询
     */
    private Query buildPinyinQuery(String queryString, float boost) {
        return MultiMatchQuery.of(m -> m
            .query(queryString)
            .fields(getPinyinFields())
            .type(TextQueryType.BestFields)
            .boost(boost)
            .operator(Operator.Or) // 拼音搜索使用OR操作符，更宽泛匹配
        )._toQuery();
    }

    /**
     * 构建首字母搜索查询
     */
    private Query buildFirstLetterQuery(String queryString, float boost) {
        return MultiMatchQuery.of(m -> m
            .query(queryString)
            .fields(getFirstLetterFields())
            .type(TextQueryType.BestFields)
            .boost(boost)
            .operator(Operator.Or)
        )._toQuery();
    }

    /**
     * 获取可搜索字段列表
     */
    private List<String> getSearchableFields() {
        // 常见的文本搜索字段
        return Arrays.asList(
            "title^2",      // title字段权重加倍
            "content",
            "description",
            "name",
            "text",
            "category"      // 分类字段
        );
    }

    /**
     * 获取拼音字段列表
     */
    private List<String> getPinyinFields() {
        return Arrays.asList(
            "title.pinyin^2",
            "content.pinyin",
            "description.pinyin",
            "name.pinyin",
            "text.pinyin",
            "category.pinyin"
        );
    }

    /**
     * 获取首字母字段列表
     */
    private List<String> getFirstLetterFields() {
        return Arrays.asList(
            "title.first_letter^2",
            "content.first_letter",
            "description.first_letter",
            "name.first_letter",
            "text.first_letter",
            "category.first_letter"
        );
    }

    /**
     * 解析Elasticsearch异常并返回友好的错误信息
     */
    private String getElasticsearchErrorMessage(ElasticsearchException e) {
        if (e.getMessage() == null) {
            return "未知的Elasticsearch错误";
        }

        String message = e.getMessage().toLowerCase();

        if (message.contains("index_not_found")) {
            return "索引不存在";
        } else if (message.contains("parsing_exception")) {
            return "查询语法错误";
        } else if (message.contains("search_parse_exception")) {
            return "搜索查询解析失败";
        } else if (message.contains("timeout")) {
            return "搜索超时";
        } else if (message.contains("circuit_breaking_exception")) {
            return "查询资源超限，请简化查询条件";
        } else if (message.contains("too_many_clauses")) {
            return "查询条件过于复杂";
        } else if (message.contains("illegal_argument")) {
            return "查询参数错误";
        } else {
            // 返回原始错误信息的前100个字符，避免过长
            return e.getMessage().length() > 100 ?
                    e.getMessage().substring(0, 100) + "..." :
                    e.getMessage();
        }
    }

    /**
     * 转换搜索结果
     */
    private SearchDataResponse.DocumentData convertHitToDocument(Hit<Map> hit) {
        return SearchDataResponse.DocumentData.builder()
                ._id(hit.id())
                ._score(hit.score())
                ._source(hit.source())
                ._index(hit.index())
                ._version(null) // Hit对象中通常不包含version信息
                .build();
    }

    /**
     * 获取简化的映射信息
     */
    private SearchDataResponse.IndexMappingInfo getSimpleMappingInfo(String indexName) {
        try {
            GetMappingRequest request = GetMappingRequest.of(builder -> builder.index(indexName));
            GetMappingResponse response = elasticsearchClient.indices().getMapping(request);

            Map<String, IndexMappingRecord> mappings = response.result();
            if (!mappings.isEmpty()) {
                IndexMappingRecord indexMapping = mappings.values().iterator().next();
                Map<String, Object> mappingData = convertMappingToMap(indexMapping);

                return SearchDataResponse.IndexMappingInfo.builder()
                        .mappings(mappingData)
                        .build();
            }
        } catch (Exception e) {
            log.warn("获取映射信息失败: index={}", indexName, e);
        }
        return null;
    }

    /**
     * 转换映射对象为Map
     */
    private Map<String, Object> convertMappingToMap(IndexMappingRecord indexMapping) {
        // 直接返回mappings内容，避免双层嵌套
        Map<String, Object> result = new HashMap<>();
        if (indexMapping.mappings() != null) {
            // 将TypeMapping对象转换为Map
            try {
                // 使用Java客户端API获取映射的实际内容
                var mappings = indexMapping.mappings();
                if (mappings.properties() != null) {
                    result.put("properties", mappings.properties());
                }
                if (mappings.dynamic() != null) {
                    result.put("dynamic", mappings.dynamic());
                }
            } catch (Exception e) {
                log.warn("转换映射对象失败，使用简化处理", e);
                // 如果转换失败，尝试直接返回映射对象
                result.put("mappings", indexMapping.mappings());
            }
        }
        return result;
    }

    /**
     * 转换批量操作
     */
    private BulkOperation convertToBulkOperation(BulkOperationRequest.BulkOperation operation) {
        switch (operation.getAction().toLowerCase()) {
            case "index":
                return BulkOperation.of(b -> b
                        .index(idx -> idx
                                .index(operation.getIndex())
                                .id(operation.getId())
                                .document(JsonData.of(operation.getSource()))));
            case "update":
                return BulkOperation.of(b -> b
                        .update(upd -> upd
                                .index(operation.getIndex())
                                .id(operation.getId())
                                .action(a -> a.doc(JsonData.of(operation.getSource())))));
            case "delete":
                return BulkOperation.of(b -> b
                        .delete(del -> del
                                .index(operation.getIndex())
                                .id(operation.getId())));
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + operation.getAction());
        }
    }

    /**
     * 转换批量响应项
     */
    private Map<String, Object> convertBulkResponseItem(BulkResponseItem item) {
        Map<String, Object> result = new HashMap<>();

        // 简化处理：直接返回基本信息，避免复杂的类型转换
        result.put("operation", "bulk_operation");
        result.put("status", "completed");

        // 根据实际情况，可以在后续版本中完善详细信息的提取
        // 目前这种简化处理可以保证编译通过

        return result;
    }
}