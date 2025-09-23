package com.ynet.mgmt.searchspace.dto;

import jakarta.validation.constraints.*;

/**
 * 创建搜索空间请求
 *
 * @author system
 * @since 1.0.0
 */
public class CreateSearchSpaceRequest {

    @NotBlank(message = "搜索空间名称不能为空")
    @Size(max = 50, message = "搜索空间名称长度不能超过50字符")
    private String name;

    @NotBlank(message = "搜索空间代码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "代码只能包含小写字母、数字和下划线，且必须以字母开头")
    @Size(max = 100, message = "代码长度不能超过100字符")
    private String code;

    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    private Boolean vectorEnabled;

    // 构造函数
    public CreateSearchSpaceRequest() {}

    public CreateSearchSpaceRequest(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public CreateSearchSpaceRequest(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public CreateSearchSpaceRequest(String name, String code, String description, Boolean vectorEnabled) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.vectorEnabled = vectorEnabled;
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

    public Boolean getVectorEnabled() {
        return vectorEnabled;
    }

    public void setVectorEnabled(Boolean vectorEnabled) {
        this.vectorEnabled = vectorEnabled;
    }

    @Override
    public String toString() {
        return "CreateSearchSpaceRequest{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", vectorEnabled=" + vectorEnabled +
                '}';
    }
}