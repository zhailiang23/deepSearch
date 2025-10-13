package com.ynet.mgmt.imagerecognition.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;
import com.ynet.mgmt.imagerecognition.service.ActivityIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 活动索引服务实现
 */
@Service
public class ActivityIndexServiceImpl implements ActivityIndexService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityIndexServiceImpl.class);

    private final ElasticsearchClient elasticsearchClient;

    public ActivityIndexServiceImpl(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public boolean insertActivity(String id, ActivityInfo activityInfo, String indexName, String imagePath) {
        try {
            // 1. 检查文档是否已存在
            if (documentExists(id, indexName)) {
                logger.info("文档已存在，跳过插入: id={}, index={}", id, indexName);
                return false;
            }

            // 2. 构建文档
            Map<String, Object> document = buildDocumentFromActivityInfo(activityInfo, imagePath);

            // 3. 插入文档
            IndexRequest<Map<String, Object>> request = IndexRequest.of(builder -> builder
                    .index(indexName)
                    .id(id)
                    .document(document)
            );

            IndexResponse response = elasticsearchClient.index(request);

            logger.info("活动信息插入成功: id={}, index={}, result={}",
                    id, indexName, response.result());
            return true;

        } catch (Exception e) {
            logger.error("插入活动信息失败: id={}, index={}, error={}",
                    id, indexName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean documentExists(String id, String indexName) {
        try {
            GetRequest request = GetRequest.of(builder -> builder
                    .index(indexName)
                    .id(id)
            );

            @SuppressWarnings("unchecked")
            GetResponse<Map<String, Object>> response = (GetResponse<Map<String, Object>>) (GetResponse<?>) elasticsearchClient.get(request, Map.class);
            return response.found();

        } catch (Exception e) {
            // 如果索引不存在或其他错误，返回false
            logger.debug("检查文档存在性失败: id={}, index={}, error={}",
                    id, indexName, e.getMessage());
            return false;
        }
    }

    /**
     * 从ActivityInfo构建Elasticsearch文档
     */
    private Map<String, Object> buildDocumentFromActivityInfo(ActivityInfo activityInfo, String imagePath) {
        Map<String, Object> document = new HashMap<>();

        // 活动信息字段
        document.put("name", activityInfo.getName());
        document.put("descript", activityInfo.getDescript());
        document.put("link", activityInfo.getLink());

        // 处理日期字段：空字符串转为null,避免Elasticsearch解析错误
        String startDate = activityInfo.getStartDate();
        document.put("startDate", (startDate != null && !startDate.isEmpty()) ? startDate : null);

        String endDate = activityInfo.getEndDate();
        document.put("endDate", (endDate != null && !endDate.isEmpty()) ? endDate : null);

        document.put("status", activityInfo.getStatus());
        document.put("all", activityInfo.getAll());

        // 元数据字段
        document.put("imagePath", imagePath);
        document.put("importTimestamp", LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT));

        return document;
    }
}
