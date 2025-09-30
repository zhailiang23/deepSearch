package com.ynet.mgmt.sensitiveWord.dto;

import jakarta.validation.constraints.*;

/**
 * 创建敏感词请求DTO
 *
 * @author system
 * @since 1.0.0
 */
public class CreateSensitiveWordRequest {

    /**
     * 敏感词名称
     */
    @NotBlank(message = "敏感词名称不能为空")
    @Size(max = 100, message = "敏感词名称长度不能超过100字符")
    private String name;

    /**
     * 危害等级（1-5级）
     */
    @NotNull(message = "危害等级不能为空")
    @Min(value = 1, message = "危害等级不能小于1")
    @Max(value = 5, message = "危害等级不能大于5")
    private Integer harmLevel;

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    // 构造函数
    public CreateSensitiveWordRequest() {}

    public CreateSensitiveWordRequest(String name, Integer harmLevel, Boolean enabled) {
        this.name = name;
        this.harmLevel = harmLevel;
        this.enabled = enabled;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CreateSensitiveWordRequest{" +
                "name='" + name + '\'' +
                ", harmLevel=" + harmLevel +
                ", enabled=" + enabled +
                '}';
    }
}