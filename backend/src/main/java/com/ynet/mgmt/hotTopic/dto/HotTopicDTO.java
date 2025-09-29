package com.ynet.mgmt.hotTopic.dto;

import java.time.LocalDateTime;

/**
 * 热门话题数据传输对象
 * 用于向外部提供热门话题信息
 *
 * @author system
 * @since 1.0.0
 */
public class HotTopicDTO {

    /**
     * 话题ID
     */
    private Long id;

    /**
     * 话题名称
     */
    private String name;

    /**
     * 热度值
     */
    private Integer popularity;

    /**
     * 是否可见
     */
    private Boolean visible;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 更新者
     */
    private String updatedBy;

    // 构造函数
    public HotTopicDTO() {}

    public HotTopicDTO(Long id, String name, Integer popularity, Boolean visible) {
        this.id = id;
        this.name = name;
        this.popularity = popularity;
        this.visible = visible;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "HotTopicDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", popularity=" + popularity +
                ", visible=" + visible +
                '}';
    }
}