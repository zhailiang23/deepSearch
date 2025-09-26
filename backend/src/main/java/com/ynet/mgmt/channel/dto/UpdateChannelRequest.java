package com.ynet.mgmt.channel.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新渠道请求DTO
 * 用于接收更新渠道时的请求参数
 *
 * @author system
 * @since 1.0.0
 */
public class UpdateChannelRequest {

    /**
     * 渠道名称
     */
    @Size(max = 100, message = "渠道名称长度不能超过100字符")
    private String name;

    /**
     * 渠道描述
     */
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    // 构造函数
    public UpdateChannelRequest() {}

    public UpdateChannelRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "UpdateChannelRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}