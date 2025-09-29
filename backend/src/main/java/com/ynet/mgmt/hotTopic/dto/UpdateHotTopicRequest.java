package com.ynet.mgmt.hotTopic.dto;

import jakarta.validation.constraints.*;

/**
 * 更新热门话题请求DTO
 * 用于接收更新热门话题时的请求参数
 *
 * @author system
 * @since 1.0.0
 */
public class UpdateHotTopicRequest {

    /**
     * 话题名称
     */
    @NotBlank(message = "话题名称不能为空")
    @Size(max = 100, message = "话题名称长度不能超过100字符")
    private String name;

    /**
     * 热度值
     */
    @NotNull(message = "热度值不能为空")
    @Min(value = 0, message = "热度值不能小于0")
    @Max(value = Integer.MAX_VALUE, message = "热度值超出范围")
    private Integer popularity;

    /**
     * 是否可见
     */
    @NotNull(message = "可见性设置不能为空")
    private Boolean visible;

    // 构造函数
    public UpdateHotTopicRequest() {}

    public UpdateHotTopicRequest(String name, Integer popularity, Boolean visible) {
        this.name = name;
        this.popularity = popularity;
        this.visible = visible;
    }

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

    @Override
    public String toString() {
        return "UpdateHotTopicRequest{" +
                "name='" + name + '\'' +
                ", popularity=" + popularity +
                ", visible=" + visible +
                '}';
    }
}