package com.ynet.mgmt.channel.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * 渠道实体类
 * 管理销售渠道的基本信息
 *
 * @author system
 * @since 1.0.0
 */
@Entity
@Table(name = "channels", indexes = {
    @Index(name = "idx_channel_code", columnList = "code", unique = true),
    @Index(name = "idx_channel_name", columnList = "name")
})
public class Channel extends BaseEntity {

    /**
     * 渠道ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 渠道名称
     */
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 100, message = "渠道名称长度不能超过100字符")
    private String name;

    /**
     * 渠道代码（唯一标识）
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    @NotBlank(message = "渠道代码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{2,50}$", message = "渠道代码只能包含字母、数字和下划线，长度2-50字符")
    private String code;

    /**
     * 渠道描述
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    // 构造函数

    public Channel() {}

    public Channel(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Channel(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
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

    // equals, hashCode 和 toString 方法

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;

        if (!Objects.equals(id, channel.id)) return false;
        return Objects.equals(code, channel.code);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (code != null ? code.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}