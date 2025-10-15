package com.ynet.mgmt.imagerecognition.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 图片识别提示词配置实体类
 * 用于存储和管理图片识别时使用的提示词模板
 */
@Entity
@Table(name = "prompt_config", indexes = {
    @Index(name = "idx_config_key", columnList = "config_key")
})
public class PromptConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键名（唯一标识）
     */
    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    @NotBlank(message = "配置键名不能为空")
    @Size(max = 100, message = "配置键名长度不能超过100字符")
    private String configKey;

    /**
     * 配置名称
     */
    @Column(name = "config_name", nullable = false, length = 200)
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 200, message = "配置名称长度不能超过200字符")
    private String configName;

    /**
     * 提示词内容
     */
    @Column(name = "prompt_content", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "提示词内容不能为空")
    private String promptContent;

    /**
     * 配置描述
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "配置描述长度不能超过500字符")
    private String description;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

    // 构造函数
    public PromptConfig() {}

    public PromptConfig(String configKey, String configName, String promptContent) {
        this.configKey = configKey;
        this.configName = configName;
        this.promptContent = promptContent;
    }

    // 业务方法

    /**
     * 检查配置是否启用
     * @return true if 配置已启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getPromptContent() {
        return promptContent;
    }

    public void setPromptContent(String promptContent) {
        this.promptContent = promptContent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

        PromptConfig that = (PromptConfig) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return configKey != null ? configKey.equals(that.configKey) : that.configKey == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (configKey != null ? configKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PromptConfig{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", configName='" + configName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
