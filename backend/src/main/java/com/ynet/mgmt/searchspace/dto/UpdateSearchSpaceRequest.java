package com.ynet.mgmt.searchspace.dto;

import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
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

    private SearchSpaceStatus status;

    // 构造函数
    public UpdateSearchSpaceRequest() {}

    public UpdateSearchSpaceRequest(String name) {
        this.name = name;
    }

    public UpdateSearchSpaceRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public UpdateSearchSpaceRequest(String name, String description, SearchSpaceStatus status) {
        this.name = name;
        this.description = description;
        this.status = status;
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

    public SearchSpaceStatus getStatus() {
        return status;
    }

    public void setStatus(SearchSpaceStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UpdateSearchSpaceRequest{" +
                "name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}