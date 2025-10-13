package com.ynet.mgmt.imagerecognition.service;

import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;

/**
 * 活动索引服务接口
 * 负责将活动信息插入到Elasticsearch索引
 */
public interface ActivityIndexService {

    /**
     * 将活动信息插入到指定的索引中
     * 如果文档ID已存在则跳过
     *
     * @param id 文档ID
     * @param activityInfo 活动信息
     * @param indexName 索引名称
     * @param imagePath 图片路径(用于记录)
     * @return true 如果插入成功, false 如果文档已存在或插入失败
     */
    boolean insertActivity(String id, ActivityInfo activityInfo, String indexName, String imagePath);

    /**
     * 检查文档是否已存在
     *
     * @param id 文档ID
     * @param indexName 索引名称
     * @return true 如果文档存在
     */
    boolean documentExists(String id, String indexName);
}
