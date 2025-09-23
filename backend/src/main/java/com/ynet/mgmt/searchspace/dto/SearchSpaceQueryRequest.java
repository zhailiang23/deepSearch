package com.ynet.mgmt.searchspace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 搜索空间查询请求
 *
 * @author system
 * @since 1.0.0
 */
public class SearchSpaceQueryRequest {

    private String keyword;

    @Min(value = 0, message = "页码不能小于0")
    private int page = 0;

    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private int size = 10;

    @Pattern(regexp = "^(id|name|code|createdAt|updatedAt)$", message = "排序字段只能是: id, name, code, createdAt, updatedAt")
    private String sortBy = "createdAt";

    @Pattern(regexp = "^(ASC|DESC)$", message = "排序方向只能是: ASC, DESC")
    private String sortDirection = "DESC";

    // 构造函数
    public SearchSpaceQueryRequest() {}

    public SearchSpaceQueryRequest(String keyword) {
        this.keyword = keyword;
    }

    public SearchSpaceQueryRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public SearchSpaceQueryRequest(String keyword, int page, int size) {
        this.keyword = keyword;
        this.page = page;
        this.size = size;
    }

    public SearchSpaceQueryRequest(String keyword, int page, int size, String sortBy, String sortDirection) {
        this.keyword = keyword;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    @Override
    public String toString() {
        return "SearchSpaceQueryRequest{" +
                "keyword='" + keyword + '\'' +
                ", page=" + page +
                ", size=" + size +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                '}';
    }
}