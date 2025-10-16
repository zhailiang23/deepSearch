package com.ynet.mgmt.hotTopic.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * 热门话题实体类
 * 管理热门话题的基本信息
 *
 * @author system
 * @since 1.0.0
 */
@Entity
@Table(name = "hot_topics", indexes = {
    @Index(name = "idx_hot_topic_name", columnList = "name"),
    @Index(name = "idx_hot_topic_popularity", columnList = "popularity"),
    @Index(name = "idx_hot_topic_visible", columnList = "visible"),
    @Index(name = "idx_hot_topic_pinyin", columnList = "pinyin"),
    @Index(name = "idx_hot_topic_pinyin_first_letter", columnList = "pinyin_first_letter")
})
public class HotTopic extends BaseEntity {

    /**
     * 热门话题ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 话题名称
     */
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "话题名称不能为空")
    @Size(max = 100, message = "话题名称长度不能超过100字符")
    private String name;

    /**
     * 热度值
     */
    @Column(name = "popularity", nullable = false)
    @NotNull(message = "热度值不能为空")
    @Min(value = 0, message = "热度值不能小于0")
    @Max(value = Integer.MAX_VALUE, message = "热度值超出范围")
    private Integer popularity;

    /**
     * 是否可见
     */
    @Column(name = "visible", nullable = false)
    @NotNull(message = "可见性设置不能为空")
    private Boolean visible;

    /**
     * 话题名称的拼音全拼（用于拼音搜索）
     * 例如："取现" -> "quxian"
     */
    @Column(name = "pinyin", length = 200)
    @Size(max = 200, message = "拼音长度不能超过200字符")
    private String pinyin;

    /**
     * 话题名称的拼音首字母（用于首字母搜索）
     * 例如："取现" -> "qx"
     */
    @Column(name = "pinyin_first_letter", length = 50)
    @Size(max = 50, message = "拼音首字母长度不能超过50字符")
    private String pinyinFirstLetter;

    // 构造函数

    public HotTopic() {}

    public HotTopic(String name, Integer popularity, Boolean visible) {
        this.name = name;
        this.popularity = popularity;
        this.visible = visible;
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

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getPinyinFirstLetter() {
        return pinyinFirstLetter;
    }

    public void setPinyinFirstLetter(String pinyinFirstLetter) {
        this.pinyinFirstLetter = pinyinFirstLetter;
    }

    // 业务方法

    /**
     * 检查话题是否可见
     */
    public boolean isVisible() {
        return Boolean.TRUE.equals(visible);
    }

    /**
     * 检查是否为高热度话题（热度值大于1000）
     */
    public boolean isHighPopularity() {
        return popularity != null && popularity > 1000;
    }

    // equals, hashCode 和 toString 方法

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HotTopic hotTopic = (HotTopic) o;

        if (!Objects.equals(id, hotTopic.id)) return false;
        return Objects.equals(name, hotTopic.name);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HotTopic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", popularity=" + popularity +
                ", visible=" + visible +
                '}';
    }
}