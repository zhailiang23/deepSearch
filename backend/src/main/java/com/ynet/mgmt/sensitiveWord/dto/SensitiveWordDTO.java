package com.ynet.mgmt.sensitiveWord.dto;

import java.time.LocalDateTime;

/**
 * 敏感词数据传输对象
 * 用于向外部提供敏感词信息
 *
 * @author system
 * @since 1.0.0
 */
public class SensitiveWordDTO {

    /**
     * 敏感词ID
     */
    private Long id;

    /**
     * 敏感词名称
     */
    private String name;

    /**
     * 危害等级（1-5级）
     */
    private Integer harmLevel;

    /**
     * 是否启用
     */
    private Boolean enabled;

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
    public SensitiveWordDTO() {}

    public SensitiveWordDTO(Long id, String name, Integer harmLevel, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.harmLevel = harmLevel;
        this.enabled = enabled;
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

    public Integer getHarmLevel() {
        return harmLevel;
    }

    public void setHarmLevel(Integer harmLevel) {
        this.harmLevel = harmLevel;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
        return "SensitiveWordDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", harmLevel=" + harmLevel +
                ", enabled=" + enabled +
                '}';
    }
}