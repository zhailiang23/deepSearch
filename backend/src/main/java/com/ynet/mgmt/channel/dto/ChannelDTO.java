package com.ynet.mgmt.channel.dto;

import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道数据传输对象
 * 用于向外部提供渠道信息
 *
 * @author system
 * @since 1.0.0
 */
public class ChannelDTO {

    /**
     * 渠道ID
     */
    private Long id;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 渠道代码（唯一标识）
     */
    private String code;

    /**
     * 渠道描述
     */
    private String description;

    /**
     * 渠道类型
     */
    private ChannelType type;

    /**
     * 渠道类型显示名称
     */
    private String typeDisplayName;

    /**
     * 渠道状态
     */
    private ChannelStatus status;

    /**
     * 渠道状态显示名称
     */
    private String statusDisplayName;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人电话
     */
    private String contactPhone;

    /**
     * 联系人邮箱
     */
    private String contactEmail;

    /**
     * 渠道地址
     */
    private String address;

    /**
     * 网站地址
     */
    private String website;

    /**
     * 佣金率（小数形式，如0.05表示5%）
     */
    private BigDecimal commissionRate;

    /**
     * 月度销售目标
     */
    private BigDecimal monthlyTarget;

    /**
     * 总销售额
     */
    private BigDecimal totalSales;

    /**
     * 当月销售额
     */
    private BigDecimal currentMonthSales;

    /**
     * 月度目标完成率
     */
    private BigDecimal performanceRatio;

    /**
     * 是否达到月度目标
     */
    private Boolean targetAchieved;

    /**
     * 最后活动时间
     */
    private LocalDateTime lastActivityAt;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 版本号（用于乐观锁）
     */
    private Long version;

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
    public ChannelDTO() {}

    // 业务方法
    /**
     * 检查渠道是否处于激活状态
     * @return true if 渠道状态为ACTIVE
     */
    public boolean isActive() {
        return ChannelStatus.ACTIVE.equals(this.status);
    }

    /**
     * 检查渠道是否被暂停
     * @return true if 渠道状态为SUSPENDED
     */
    public boolean isSuspended() {
        return ChannelStatus.SUSPENDED.equals(this.status);
    }

    /**
     * 检查渠道是否已删除
     * @return true if 渠道状态为DELETED
     */
    public boolean isDeleted() {
        return ChannelStatus.DELETED.equals(this.status);
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

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
        this.typeDisplayName = type != null ? type.getDisplayName() : null;
    }

    public String getTypeDisplayName() {
        return typeDisplayName;
    }

    public void setTypeDisplayName(String typeDisplayName) {
        this.typeDisplayName = typeDisplayName;
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public void setStatus(ChannelStatus status) {
        this.status = status;
        this.statusDisplayName = status != null ? status.getDisplayName() : null;
    }

    public String getStatusDisplayName() {
        return statusDisplayName;
    }

    public void setStatusDisplayName(String statusDisplayName) {
        this.statusDisplayName = statusDisplayName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getCurrentMonthSales() {
        return currentMonthSales;
    }

    public void setCurrentMonthSales(BigDecimal currentMonthSales) {
        this.currentMonthSales = currentMonthSales;
    }

    public BigDecimal getPerformanceRatio() {
        return performanceRatio;
    }

    public void setPerformanceRatio(BigDecimal performanceRatio) {
        this.performanceRatio = performanceRatio;
    }

    public Boolean getTargetAchieved() {
        return targetAchieved;
    }

    public void setTargetAchieved(Boolean targetAchieved) {
        this.targetAchieved = targetAchieved;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
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
        return "ChannelDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", totalSales=" + totalSales +
                ", currentMonthSales=" + currentMonthSales +
                ", performanceRatio=" + performanceRatio +
                '}';
    }
}