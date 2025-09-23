package com.ynet.mgmt.common.dto;

import java.util.List;

/**
 * 分页结果封装
 *
 * @author system
 * @since 1.0.0
 */
public class PageResult<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    // 构造函数
    public PageResult() {}

    public PageResult(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = page == 0;
        this.last = page >= totalPages - 1;
    }

    // Builder模式
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private PageResult<T> result = new PageResult<>();

        public Builder<T> content(List<T> content) {
            result.content = content;
            return this;
        }

        public Builder<T> page(int page) {
            result.page = page;
            return this;
        }

        public Builder<T> size(int size) {
            result.size = size;
            return this;
        }

        public Builder<T> totalElements(long totalElements) {
            result.totalElements = totalElements;
            return this;
        }

        public Builder<T> totalPages(int totalPages) {
            result.totalPages = totalPages;
            return this;
        }

        public Builder<T> first(boolean first) {
            result.first = first;
            return this;
        }

        public Builder<T> last(boolean last) {
            result.last = last;
            return this;
        }

        public PageResult<T> build() {
            return result;
        }
    }

    // 业务方法
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    public boolean hasNext() {
        return !last;
    }

    public boolean hasPrevious() {
        return !first;
    }

    public int getNumberOfElements() {
        return content != null ? content.size() : 0;
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
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

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", numberOfElements=" + getNumberOfElements() +
                '}';
    }
}