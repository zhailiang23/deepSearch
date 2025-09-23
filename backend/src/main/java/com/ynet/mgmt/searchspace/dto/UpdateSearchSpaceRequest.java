package com.ynet.mgmt.searchspace.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新搜索空间请求
 *
 * @author system
 * @since 1.0.0
 */
public class UpdateSearchSpaceRequest {

    @Size(max = 50, message = "搜索空间名称长度不能超过50字符")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    private Boolean vectorEnabled;

    // 构造函数
    public UpdateSearchSpaceRequest() {}

    public UpdateSearchSpaceRequest(String name) {
        this.name = name;
    }

    public UpdateSearchSpaceRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UpdateSearchSpaceRequest(String name, String description, Boolean vectorEnabled) {
        this.name = name;
        this.description = description;
        this.vectorEnabled = vectorEnabled;
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

    public Boolean getVectorEnabled() {
        return vectorEnabled;
    }

    public void setVectorEnabled(Boolean vectorEnabled) {
        this.vectorEnabled = vectorEnabled;
    }

    @Override
    public String toString() {
        return "UpdateSearchSpaceRequest{" +
                "name='" + name + '\'' +
                ", vectorEnabled=" + vectorEnabled +
                '}';
    }
}