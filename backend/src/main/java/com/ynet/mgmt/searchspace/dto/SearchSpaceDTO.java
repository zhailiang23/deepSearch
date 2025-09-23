package com.ynet.mgmt.searchspace.dto;

import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import com.ynet.mgmt.searchspace.model.IndexStatus;

import java.time.LocalDateTime;

/**
 * 搜索空间DTO
 * 用于对外暴露搜索空间信息
 *
 * @author system
 * @since 1.0.0
 */
public class SearchSpaceDTO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private SearchSpaceStatus status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ES索引状态信息
    private IndexStatus indexStatus;

    // 构造函数
    public SearchSpaceDTO() {}

    public SearchSpaceDTO(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    // 业务判断方法
    public boolean isActive() {
        return SearchSpaceStatus.ACTIVE.equals(this.status);
    }

    public boolean isSearchable() {
        return this.status != null && this.status.isSearchable();
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(IndexStatus indexStatus) {
        this.indexStatus = indexStatus;
    }

    @Override
    public String toString() {
        return "SearchSpaceDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", status=" + status +
                '}';
    }
}