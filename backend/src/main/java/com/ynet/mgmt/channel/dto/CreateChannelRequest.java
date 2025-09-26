package com.ynet.mgmt.channel.dto;

import jakarta.validation.constraints.*;

/**
 * 创建渠道请求DTO
 * 用于接收创建渠道时的请求参数
 *
 * @author system
 * @since 1.0.0
 */
public class CreateChannelRequest {

    /**
     * 渠道名称
     */
    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 100, message = "渠道名称长度不能超过100字符")
    private String name;

    /**
     * 渠道代码（唯一标识）
     */
    @NotBlank(message = "渠道代码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{2,50}$", message = "渠道代码只能包含字母、数字和下划线，长度2-50字符")
    private String code;

    /**
     * 渠道描述
     */
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    // 构造函数
    public CreateChannelRequest() {}

    public CreateChannelRequest(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public CreateChannelRequest(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CreateChannelRequest{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}