package com.ynet.mgmt.searchspace.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * 搜索空间实体类
 * 管理搜索空间的基本信息、状态和向量检索配置
 *
 * @author system
 * @since 1.0.0
 */
@Entity
@Table(name = "search_spaces",
       indexes = {
           @Index(name = "idx_search_space_code", columnList = "code", unique = true),
           @Index(name = "idx_search_space_name", columnList = "name"),
           @Index(name = "idx_search_space_status", columnList = "status"),
           @Index(name = "idx_search_space_created_at", columnList = "created_at")
       })
public class SearchSpace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "搜索空间名称不能为空")
    @Size(max = 50, message = "搜索空间名称长度不能超过50字符")
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @NotBlank(message = "搜索空间代码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "代码只能包含小写字母、数字和下划线，且必须以字母开头")
    @Size(max = 100, message = "代码长度不能超过100字符")
    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @Size(max = 500, message = "描述长度不能超过500字符")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private SearchSpaceStatus status = SearchSpaceStatus.ACTIVE;

    @Version
    private Long version;

    // 构造函数
    public SearchSpace() {}

    public SearchSpace(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public SearchSpace(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    // 业务方法

    /**
     * 检查搜索空间是否处于活跃状态
     * @return true if 搜索空间状态为ACTIVE
     */
    public boolean isActive() {
        return SearchSpaceStatus.ACTIVE.equals(this.status);
    }

    /**
     * 检查搜索空间是否可用于搜索
     * @return true if 搜索空间状态允许搜索
     */
    public boolean isSearchable() {
        return this.status != null && this.status.isSearchable();
    }


    /**
     * 获取Elasticsearch索引名称
     * @return 基于code生成的索引名称
     */
    public String getIndexName() {
        return this.code;
    }



    /**
     * 激活搜索空间
     */
    public void activate() {
        this.status = SearchSpaceStatus.ACTIVE;
    }

    /**
     * 停用搜索空间
     */
    public void deactivate() {
        this.status = SearchSpaceStatus.INACTIVE;
    }

    /**
     * 设置为维护状态
     */
    public void setMaintenance() {
        this.status = SearchSpaceStatus.MAINTENANCE;
    }

    /**
     * 标记为已删除
     */
    public void markAsDeleted() {
        this.status = SearchSpaceStatus.DELETED;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchSpace that = (SearchSpace) o;

        // 如果ID都存在，只比较ID
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }

        // 如果ID不存在，比较code
        return code != null ? code.equals(that.code) : that.code == null;
    }

    @Override
    public int hashCode() {
        // 如果ID存在，只使用ID计算hashCode
        if (id != null) {
            return id.hashCode();
        }
        // 否则使用code计算hashCode
        return code != null ? code.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SearchSpace{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", status=" + status +
                '}';
    }
}