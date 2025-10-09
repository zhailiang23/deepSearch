package com.ynet.mgmt.hotTopic.dto;

import java.util.List;

/**
 * 批量创建热门话题响应
 *
 * @author system
 * @since 1.0.0
 */
public class BatchCreateHotTopicResponse {

    /**
     * 成功创建的话题数量
     */
    private Integer successCount;

    /**
     * 跳过的话题数量（已存在）
     */
    private Integer skippedCount;

    /**
     * 失败的话题数量
     */
    private Integer failedCount;

    /**
     * 成功创建的话题列表
     */
    private List<HotTopicDTO> createdTopics;

    /**
     * 跳过的话题名称列表
     */
    private List<String> skippedTopics;

    /**
     * 失败的话题名称列表
     */
    private List<String> failedTopics;

    // Getters and Setters

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<HotTopicDTO> getCreatedTopics() {
        return createdTopics;
    }

    public void setCreatedTopics(List<HotTopicDTO> createdTopics) {
        this.createdTopics = createdTopics;
    }

    public List<String> getSkippedTopics() {
        return skippedTopics;
    }

    public void setSkippedTopics(List<String> skippedTopics) {
        this.skippedTopics = skippedTopics;
    }

    public List<String> getFailedTopics() {
        return failedTopics;
    }

    public void setFailedTopics(List<String> failedTopics) {
        this.failedTopics = failedTopics;
    }
}
