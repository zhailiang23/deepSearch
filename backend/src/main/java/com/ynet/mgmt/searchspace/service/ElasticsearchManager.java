package com.ynet.mgmt.searchspace.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.ynet.mgmt.config.ElasticsearchProperties;
import com.ynet.mgmt.searchspace.exception.ElasticsearchException;
import com.ynet.mgmt.searchspace.model.ClusterHealth;
import com.ynet.mgmt.searchspace.model.IndexStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Elasticsearch管理器
 * 提供索引管理、健康检查、状态查询等核心功能
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class ElasticsearchManager {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchManager.class);

    private final ElasticsearchClient client;
    private final ElasticsearchProperties properties;

    public ElasticsearchManager(ElasticsearchClient client, ElasticsearchProperties properties) {
        this.client = client;
        this.properties = properties;
        log.info("ElasticsearchManager 初始化完成");
    }

    /**
     * 创建索引
     *
     * @param indexName 索引名称
     * @throws ElasticsearchException 如果创建失败
     */
    public void createIndex(String indexName) {
        log.info("开始创建索引: {}", indexName);

        try {
            // 检查索引是否已存在
            if (indexExists(indexName)) {
                log.warn("索引 {} 已存在，跳过创建", indexName);
                return;
            }

            // 创建索引请求
            CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(indexName)
                .settings(indexSettings -> indexSettings
                    .numberOfShards(String.valueOf(properties.getIndex().getDefaultSettings().getNumberOfShards()))
                    .numberOfReplicas(String.valueOf(properties.getIndex().getDefaultSettings().getNumberOfReplicas()))
                    .refreshInterval(Time.of(time -> time
                        .time(properties.getIndex().getDefaultSettings().getRefreshInterval().getSeconds() + "s")
                    ))
                )
                .mappings(typeMapping -> typeMapping
                    .properties("title", property -> property
                        .text(textProperty -> textProperty
                            .analyzer("standard")
                            .fields("keyword", keywordField -> keywordField
                                .keyword(keywordProperty -> keywordProperty)
                            )
                        )
                    )
                    .properties("content", property -> property
                        .text(textProperty -> textProperty.analyzer("standard"))
                    )
                    .properties("timestamp", property -> property
                        .date(dateProperty -> dateProperty.format("strict_date_time"))
                    )
                    .properties("searchSpaceCode", property -> property
                        .keyword(keywordProperty -> keywordProperty)
                    )
                    .properties("documentId", property -> property
                        .keyword(keywordProperty -> keywordProperty)
                    )
                )
            );

            CreateIndexResponse response = client.indices().create(request);
            if (response.acknowledged()) {
                log.info("索引 {} 创建成功", indexName);
            } else {
                throw ElasticsearchException.indexCreationFailed(indexName,
                    new RuntimeException("索引创建未被确认"));
            }

        } catch (ElasticsearchException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建索引 {} 失败", indexName, e);
            throw ElasticsearchException.indexCreationFailed(indexName, e);
        }
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @throws ElasticsearchException 如果删除失败
     */
    public void deleteIndex(String indexName) {
        log.info("开始删除索引: {}", indexName);

        try {
            if (!indexExists(indexName)) {
                log.warn("索引 {} 不存在，跳过删除", indexName);
                return;
            }

            DeleteIndexRequest request = DeleteIndexRequest.of(builder -> builder
                .index(indexName)
            );

            DeleteIndexResponse response = client.indices().delete(request);
            if (response.acknowledged()) {
                log.info("索引 {} 删除成功", indexName);
            } else {
                throw ElasticsearchException.indexDeletionFailed(indexName,
                    new RuntimeException("索引删除未被确认"));
            }

        } catch (ElasticsearchException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除索引 {} 失败", indexName, e);
            throw ElasticsearchException.indexDeletionFailed(indexName, e);
        }
    }

    /**
     * 检查索引是否存在
     *
     * @param indexName 索引名称
     * @return true 如果索引存在
     */
    public boolean indexExists(String indexName) {
        try {
            ExistsRequest request = ExistsRequest.of(builder -> builder
                .index(indexName)
            );

            BooleanResponse response = client.indices().exists(request);
            boolean exists = response.value();
            log.debug("索引 {} 存在性检查结果: {}", indexName, exists);
            return exists;

        } catch (Exception e) {
            log.error("检查索引 {} 是否存在时发生错误", indexName, e);
            return false;
        }
    }

    /**
     * 获取索引状态
     *
     * @param indexName 索引名称
     * @return 索引状态信息
     */
    public IndexStatus getIndexStatus(String indexName) {
        log.debug("获取索引状态: {}", indexName);

        try {
            if (!indexExists(indexName)) {
                return IndexStatus.builder()
                    .name(indexName)
                    .exists(false)
                    .health("not_exists")
                    .build();
            }

            // 获取索引基本信息
            GetIndexRequest getRequest = GetIndexRequest.of(builder -> builder
                .index(indexName)
            );
            GetIndexResponse getResponse = client.indices().get(getRequest);

            // 获取索引健康状态
            String health;
            try {
                HealthRequest healthRequest = HealthRequest.of(builder -> builder
                    .index(indexName)
                    .timeout(Time.of(time -> time.time("10s")))
                );
                HealthResponse healthResponse = client.cluster().health(healthRequest);
                health = healthResponse.status().jsonValue();
            } catch (Exception e) {
                log.warn("获取索引 {} 健康状态失败，使用默认值", indexName, e);
                health = "unknown";
            }

            // 获取文档数量
            long docsCount = getDocumentCount(indexName);

            // 获取存储大小
            String storeSize = getStoreSize(indexName);

            // 获取分片信息
            var indexInfo = getResponse.get(indexName);
            Integer primaryShards = null;
            Integer replicas = null;
            if (indexInfo != null && indexInfo.settings() != null) {
                var settings = indexInfo.settings().index();
                if (settings != null) {
                    try {
                        primaryShards = Integer.parseInt(settings.numberOfShards());
                        replicas = Integer.parseInt(settings.numberOfReplicas());
                    } catch (Exception e) {
                        log.warn("解析索引 {} 分片配置失败", indexName, e);
                    }
                }
            }

            return IndexStatus.builder()
                .name(indexName)
                .exists(true)
                .health(health)
                .docsCount(docsCount)
                .storeSize(storeSize)
                .primaryShards(primaryShards)
                .replicas(replicas)
                .build();

        } catch (Exception e) {
            log.error("获取索引 {} 状态失败", indexName, e);
            return IndexStatus.builder()
                .name(indexName)
                .exists(false)
                .health("error")
                .error(e.getMessage())
                .build();
        }
    }

    /**
     * 获取索引健康状态
     *
     * @param indexName 索引名称
     * @return 健康状态 (green, yellow, red, unknown)
     */
    public String getIndexHealth(String indexName) {
        try {
            HealthRequest request = HealthRequest.of(builder -> builder
                .index(indexName)
                .timeout(Time.of(time -> time.time("10s")))
            );
            HealthResponse response = client.cluster().health(request);
            return response.status().jsonValue();
        } catch (Exception e) {
            log.error("获取索引 {} 健康状态失败", indexName, e);
            return "unknown";
        }
    }

    /**
     * 检查ES集群健康状态
     *
     * @return 集群健康状态信息
     */
    public ClusterHealth getClusterHealth() {
        log.debug("获取集群健康状态");

        try {
            HealthRequest request = HealthRequest.of(builder -> builder
                .timeout(Time.of(time -> time.time("30s")))
            );
            HealthResponse response = client.cluster().health(request);

            return ClusterHealth.builder()
                .status(response.status().jsonValue())
                .clusterName(response.clusterName())
                .numberOfNodes(response.numberOfNodes())
                .numberOfDataNodes(response.numberOfDataNodes())
                .activePrimaryShards(response.activePrimaryShards())
                .activeShards(response.activeShards())
                .relocatingShards(response.relocatingShards())
                .initializingShards(response.initializingShards())
                .unassignedShards(response.unassignedShards())
                .build();

        } catch (Exception e) {
            log.error("获取集群健康状态失败", e);
            return ClusterHealth.builder()
                .status("unknown")
                .error(e.getMessage())
                .build();
        }
    }

    /**
     * 检查Elasticsearch连接是否正常
     *
     * @return true 如果连接正常
     */
    public boolean isConnectionHealthy() {
        try {
            ClusterHealth health = getClusterHealth();
            return !health.hasError() && health.isAvailable();
        } catch (Exception e) {
            log.error("检查ES连接健康状态失败", e);
            return false;
        }
    }

    /**
     * 获取索引文档数量
     */
    public long getDocumentCount(String indexName) {
        try {
            CountRequest request = CountRequest.of(builder -> builder
                .index(indexName)
            );
            CountResponse response = client.count(request);
            return response.count();
        } catch (Exception e) {
            log.warn("获取索引 {} 文档数量失败", indexName, e);
            return 0;
        }
    }

    /**
     * 获取索引存储大小
     */
    private String getStoreSize(String indexName) {
        try {
            // 暂时返回固定值，避免 Stats API 的复杂性
            // TODO: 在后续版本中实现完整的 Stats API 调用
            log.debug("获取索引 {} 存储大小 - 暂时返回默认值", indexName);
            return "unknown";
        } catch (Exception e) {
            log.warn("获取索引 {} 存储大小失败", indexName, e);
            return "unknown";
        }
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ========== 异步操作方法 ==========

    /**
     * 异步创建索引
     *
     * @param indexName 索引名称
     * @return CompletableFuture<Void>
     */
    @Async("elasticsearchTaskExecutor")
    public CompletableFuture<Void> createIndexAsync(String indexName) {
        log.info("异步创建索引: {}", indexName);
        try {
            createIndex(indexName);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步删除索引
     *
     * @param indexName 索引名称
     * @return CompletableFuture<Void>
     */
    @Async("elasticsearchTaskExecutor")
    public CompletableFuture<Void> deleteIndexAsync(String indexName) {
        log.info("异步删除索引: {}", indexName);
        try {
            deleteIndex(indexName);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步获取索引状态
     *
     * @param indexName 索引名称
     * @return CompletableFuture<IndexStatus>
     */
    @Async("elasticsearchTaskExecutor")
    public CompletableFuture<IndexStatus> getIndexStatusAsync(String indexName) {
        log.debug("异步获取索引状态: {}", indexName);
        try {
            IndexStatus status = getIndexStatus(indexName);
            return CompletableFuture.completedFuture(status);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步获取集群健康状态
     *
     * @return CompletableFuture<ClusterHealth>
     */
    @Async("elasticsearchTaskExecutor")
    public CompletableFuture<ClusterHealth> getClusterHealthAsync() {
        log.debug("异步获取集群健康状态");
        try {
            ClusterHealth health = getClusterHealth();
            return CompletableFuture.completedFuture(health);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步检查索引是否存在
     *
     * @param indexName 索引名称
     * @return CompletableFuture<Boolean>
     */
    @Async("elasticsearchTaskExecutor")
    public CompletableFuture<Boolean> indexExistsAsync(String indexName) {
        log.debug("异步检查索引是否存在: {}", indexName);
        try {
            boolean exists = indexExists(indexName);
            return CompletableFuture.completedFuture(exists);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 异步检查连接健康状态
     *
     * @return CompletableFuture<Boolean>
     */
    @Async("elasticsearchTaskExecutor")
    public CompletableFuture<Boolean> isConnectionHealthyAsync() {
        log.debug("异步检查连接健康状态");
        try {
            boolean healthy = isConnectionHealthy();
            return CompletableFuture.completedFuture(healthy);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 查找搜索空间的实际索引名称
     * 优先返回包含数据的索引，如果有多个则返回最新的
     *
     * @param searchSpaceCode 搜索空间代码
     * @return 实际的索引名称，如果没有找到则返回基础索引名
     */
    public String findActualIndexName(String searchSpaceCode) {
        try {
            // 搜索模式：searchspace_{code}_* 或者直接是 code
            String pattern = "searchspace_" + searchSpaceCode + "_*";
            
            // 使用 _cat/indices API 查找匹配的索引
            // 注意：这里我们直接构造请求，因为 Java client 可能不直接支持 _cat API
            String catIndicesUrl = "/_cat/indices/" + pattern + "?h=index,docs.count&format=json";
            
            // 先尝试查找带时间戳的索引
            var indicesWithPattern = getIndicesMatching("searchspace_" + searchSpaceCode + "_*");
            if (!indicesWithPattern.isEmpty()) {
                // 如果有多个索引，选择文档数量最多的，如果文档数量相同则选择最新的（按名称排序）
                String bestIndex = indicesWithPattern.stream()
                    .filter(indexName -> getDocumentCount(indexName) > 0) // 优先选择有数据的索引
                    .findFirst() // 如果有数据的索引，取第一个
                    .orElse(indicesWithPattern.get(indicesWithPattern.size() - 1)); // 否则取最新的
                log.debug("为搜索空间 {} 找到实际索引: {}", searchSpaceCode, bestIndex);
                return bestIndex;
            }
            
            // 如果没有找到带时间戳的索引，检查基础索引是否存在
            if (indexExists(searchSpaceCode)) {
                log.debug("为搜索空间 {} 使用基础索引: {}", searchSpaceCode, searchSpaceCode);
                return searchSpaceCode;
            }
            
            // 都没有找到，返回基础索引名（让调用方处理不存在的情况）
            log.debug("为搜索空间 {} 未找到实际索引，返回基础索引名: {}", searchSpaceCode, searchSpaceCode);
            return searchSpaceCode;
            
        } catch (Exception e) {
            log.error("查找搜索空间 {} 的实际索引时发生错误", searchSpaceCode, e);
            return searchSpaceCode; // 发生错误时返回基础索引名
        }
    }

    /**
     * 获取匹配模式的索引列表
     *
     * @param pattern 索引模式 (支持通配符 *)
     * @return 匹配的索引名称列表，按时间戳倒序排列（最新的在前）
     */
    private List<String> getIndicesMatching(String pattern) {
        try {
            // 使用 GetIndex API 查找匹配的索引
            GetIndexRequest request = GetIndexRequest.of(builder -> builder
                .index(pattern)
            );
            
            GetIndexResponse response = client.indices().get(request);
            
            // 获取所有匹配的索引名并按时间戳排序（最新的在前）
            return response.result().keySet().stream()
                .sorted((a, b) -> {
                    // 提取时间戳进行比较，如果无法提取则按字典序
                    try {
                        String timestampA = a.substring(a.lastIndexOf('_') + 1);
                        String timestampB = b.substring(b.lastIndexOf('_') + 1);
                        return Long.compare(Long.parseLong(timestampB), Long.parseLong(timestampA)); // 降序
                    } catch (Exception e) {
                        return b.compareTo(a); // 降序字典排序
                    }
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.debug("获取匹配索引时发生错误: {}", pattern, e);
            return new ArrayList<>();
        }
    }

    // 在现有的getIndexStatus方法前添加重载方法
    /**
     * 获取搜索空间的实际索引状态
     * 会自动查找搜索空间对应的实际索引（包括带时间戳的索引）
     *
     * @param searchSpaceCode 搜索空间代码
     * @return 索引状态信息
     */
    public IndexStatus getSearchSpaceIndexStatus(String searchSpaceCode) {
        String actualIndexName = findActualIndexName(searchSpaceCode);
        IndexStatus status = getIndexStatus(actualIndexName);
        
        // 如果查询的是实际索引而不是基础索引，更新显示名称以反映这一点
        if (!actualIndexName.equals(searchSpaceCode)) {
            log.debug("搜索空间 {} 使用实际索引 {} (包含 {} 个文档)", 
                searchSpaceCode, actualIndexName, status.getDocsCount());
        }
        
        return status;
    }
}