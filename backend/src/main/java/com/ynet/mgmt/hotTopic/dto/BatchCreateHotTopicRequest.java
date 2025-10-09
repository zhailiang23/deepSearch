package com.ynet.mgmt.hotTopic.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量创建热门话题请求
 *
 * @author system
 * @since 1.0.0
 */
public class BatchCreateHotTopicRequest {

    /**
     * 批量创建的话题列表
     */
    @NotNull(message = "话题列表不能为空")
    @NotEmpty(message = "话题列表不能为空")
    private List<TopicItem> topics;

    /**
     * 是否跳过已存在的话题
     */
    private Boolean skipExisting = true;

    // Getters and Setters

    public List<TopicItem> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicItem> topics) {
        this.topics = topics;
    }

    public Boolean getSkipExisting() {
        return skipExisting;
    }

    public void setSkipExisting(Boolean skipExisting) {
        this.skipExisting = skipExisting;
    }

    /**
     * 话题项
     */
    public static class TopicItem {
        /**
         * 话题名称
         */
        @NotNull(message = "话题名称不能为空")
        private String name;

        /**
         * 热度值
         */
        @NotNull(message = "热度值不能为空")
        private Integer popularity;

        /**
         * 是否可见
         */
        private Boolean visible = true;

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getPopularity() {
            return popularity;
        }

        public void setPopularity(Integer popularity) {
            this.popularity = popularity;
        }

        public Boolean getVisible() {
            return visible;
        }

        public void setVisible(Boolean visible) {
            this.visible = visible;
        }
    }
}
