package com.ynet.mgmt.sensitiveWord.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * 敏感词实体类
 * 管理敏感词的基本信息
 *
 * @author system
 * @since 1.0.0
 */
@Entity
@Table(name = "sensitive_words", indexes = {
    @Index(name = "idx_sensitive_word_name", columnList = "name"),
    @Index(name = "idx_sensitive_word_harm_level", columnList = "harm_level"),
    @Index(name = "idx_sensitive_word_enabled", columnList = "enabled")
})
public class SensitiveWord extends BaseEntity {

    /**
     * 敏感词ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 敏感词名称
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "敏感词名称不能为空")
    @Size(max = 100, message = "敏感词名称长度不能超过100字符")
    private String name;

    /**
     * 危害等级（1-5级）
     */
    @Column(name = "harm_level", nullable = false)
    @NotNull(message = "危害等级不能为空")
    @Min(value = 1, message = "危害等级不能小于1")
    @Max(value = 5, message = "危害等级不能大于5")
    private Integer harmLevel;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    // 构造函数

    public SensitiveWord() {}

    public SensitiveWord(String name, Integer harmLevel, Boolean enabled) {
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

    // 业务方法

    /**
     * 检查敏感词是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * 检查是否为高危害等级（等级大于3）
     */
    public boolean isHighHarm() {
        return harmLevel != null && harmLevel > 3;
    }

    /**
     * 检查是否为低危害等级（等级为1）
     */
    public boolean isLowHarm() {
        return harmLevel != null && harmLevel == 1;
    }

    // equals, hashCode 和 toString 方法

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensitiveWord that = (SensitiveWord) o;

        if (!Objects.equals(id, that.id)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SensitiveWord{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", harmLevel=" + harmLevel +
                ", enabled=" + enabled +
                '}';
    }
}