package com.ynet.mgmt.channel.dto;

import com.ynet.mgmt.channel.entity.ChannelType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 更新渠道请求DTO
 * 用于接收更新渠道时的请求参数
 *
 * @author system
 * @since 1.0.0
 */
public class UpdateChannelRequest {

    /**
     * 渠道名称
     */
    @Size(max = 100, message = "渠道名称长度不能超过100字符")
    private String name;

    /**
     * 渠道描述
     */
    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    /**
     * 渠道类型
     */
    private ChannelType type;

    /**
     * 联系人姓名
     */
    @Size(max = 100, message = "联系人姓名长度不能超过100字符")
    private String contactPerson;

    /**
     * 联系人电话
     */
    @Pattern(regexp = "^[0-9+\\-\\s()]{0,20}$", message = "联系电话格式不正确")
    private String contactPhone;

    /**
     * 联系人邮箱
     */
    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过100字符")
    private String contactEmail;

    /**
     * 渠道地址
     */
    @Size(max = 255, message = "地址长度不能超过255字符")
    private String address;

    /**
     * 网站地址
     */
    @Size(max = 255, message = "网站地址长度不能超过255字符")
    private String website;

    /**
     * 佣金率（小数形式，如0.05表示5%）
     */
    @DecimalMin(value = "0.0000", message = "佣金率不能为负数")
    @DecimalMax(value = "1.0000", message = "佣金率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "佣金率格式不正确")
    private BigDecimal commissionRate;

    /**
     * 月度销售目标
     */
    @DecimalMin(value = "0.00", message = "月度目标不能为负数")
    @Digits(integer = 13, fraction = 2, message = "月度目标金额格式不正确")
    private BigDecimal monthlyTarget;

    /**
     * 排序顺序
     */
    @Min(value = 0, message = "排序顺序不能为负数")
    private Integer sortOrder;

    /**
     * 版本号（用于乐观锁）
     */
    private Long version;

    // 构造函数
    public UpdateChannelRequest() {}

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getMonthlyTarget() {
        return monthlyTarget;
    }

    public void setMonthlyTarget(BigDecimal monthlyTarget) {
        this.monthlyTarget = monthlyTarget;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "UpdateChannelRequest{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", contactPerson='" + contactPerson + '\'' +
                ", monthlyTarget=" + monthlyTarget +
                ", version=" + version +
                '}';
    }
}