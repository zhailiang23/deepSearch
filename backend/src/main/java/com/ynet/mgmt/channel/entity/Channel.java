package com.ynet.mgmt.channel.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 渠道实体类
 * 管理销售渠道的基本信息、业务数据和运营状态
 *
 * @author system
 * @since 1.0.0
 */
@Entity
@Table(name = "channels", indexes = {
    @Index(name = "idx_channel_code", columnList = "code", unique = true),
    @Index(name = "idx_channel_name", columnList = "name"),
    @Index(name = "idx_channel_status", columnList = "status"),
    @Index(name = "idx_channel_type", columnList = "type"),
    @Index(name = "idx_channel_sort_order", columnList = "sort_order"),
    @Index(name = "idx_channel_last_activity", columnList = "last_activity_at")
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

    /**
     * 渠道类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull(message = "渠道类型不能为空")
    private ChannelType type;

    /**
     * 渠道状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "渠道状态不能为空")
    private ChannelStatus status = ChannelStatus.INACTIVE;

    /**
     * 联系人姓名
     */
    @Column(name = "contact_name", length = 100)
    @Size(max = 100, message = "联系人姓名长度不能超过100字符")
    private String contactName;

    /**
     * 联系人电话
     */
    @Column(name = "contact_phone", length = 20)
    @Pattern(regexp = "^[0-9+\\-\\s()]{0,20}$", message = "联系电话格式不正确")
    private String contactPhone;

    /**
     * 联系人邮箱
     */
    @Column(name = "contact_email", length = 100)
    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过100字符")
    private String contactEmail;

    /**
     * 渠道地址
     */
    @Column(name = "address", length = 255)
    @Size(max = 255, message = "地址长度不能超过255字符")
    private String address;

    /**
     * 佣金率（小数形式，如0.05表示5%）
     */
    @Column(name = "commission_rate", precision = 5, scale = 4)
    @DecimalMin(value = "0.0000", message = "佣金率不能为负数")
    @DecimalMax(value = "1.0000", message = "佣金率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "佣金率格式不正确")
    private BigDecimal commissionRate = BigDecimal.ZERO;

    /**
     * 月度销售目标
     */
    @Column(name = "monthly_target", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "月度目标不能为负数")
    @Digits(integer = 13, fraction = 2, message = "月度目标金额格式不正确")
    private BigDecimal monthlyTarget = BigDecimal.ZERO;

    /**
     * 总销售额
     */
    @Column(name = "total_sales", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "总销售额不能为负数")
    @Digits(integer = 13, fraction = 2, message = "总销售额格式不正确")
    private BigDecimal totalSales = BigDecimal.ZERO;

    /**
     * 当月销售额
     */
    @Column(name = "current_month_sales", precision = 15, scale = 2)
    @DecimalMin(value = "0.00", message = "当月销售额不能为负数")
    @Digits(integer = 13, fraction = 2, message = "当月销售额格式不正确")
    private BigDecimal currentMonthSales = BigDecimal.ZERO;

    /**
     * 最后活动时间
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order", nullable = false)
    @Min(value = 0, message = "排序顺序不能为负数")
    private Integer sortOrder = 0;

    /**
     * 乐观锁版本字段
     */
    @Version
    private Long version;

    // 构造函数

    public Channel() {}

    public Channel(String name, String code, ChannelType type) {
        this.name = name;
        this.code = code;
        this.type = type;
    }

    // 业务逻辑方法

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

    /**
     * 检查渠道是否为线上类型
     * @return true if 渠道类型为ONLINE
     */
    public boolean isOnline() {
        return ChannelType.ONLINE.equals(this.type);
    }

    /**
     * 检查渠道是否为线下类型
     * @return true if 渠道类型为OFFLINE
     */
    public boolean isOffline() {
        return ChannelType.OFFLINE.equals(this.type);
    }

    /**
     * 检查渠道是否为混合类型
     * @return true if 渠道类型为HYBRID
     */
    public boolean isHybrid() {
        return ChannelType.HYBRID.equals(this.type);
    }

    /**
     * 激活渠道
     */
    public void activate() {
        this.status = ChannelStatus.ACTIVE;
        updateActivity();
    }

    /**
     * 暂停渠道
     */
    public void suspend() {
        this.status = ChannelStatus.SUSPENDED;
        updateActivity();
    }

    /**
     * 标记渠道为删除状态
     */
    public void markAsDeleted() {
        this.status = ChannelStatus.DELETED;
        updateActivity();
    }

    /**
     * 更新最后活动时间
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * 更新销售数据
     * @param amount 销售金额
     */
    public void updateSales(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalSales = this.totalSales.add(amount);
            this.currentMonthSales = this.currentMonthSales.add(amount);
            updateActivity();
        }
    }

    /**
     * 计算佣金金额
     * @param salesAmount 销售金额
     * @return 佣金金额
     */
    public BigDecimal getCommissionAmount(BigDecimal salesAmount) {
        if (salesAmount == null || salesAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (commissionRate == null) {
            return BigDecimal.ZERO;
        }
        return salesAmount.multiply(commissionRate);
    }

    /**
     * 计算月度目标完成度
     * @return 完成度（百分比）
     */
    public BigDecimal getMonthlyTargetCompletion() {
        if (monthlyTarget == null || monthlyTarget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (currentMonthSales == null) {
            return BigDecimal.ZERO;
        }
        return currentMonthSales.divide(monthlyTarget, 4, RoundingMode.HALF_UP);
    }

    /**
     * 检查是否完成月度目标
     * @return true if 当月销售额达到或超过月度目标
     */
    public boolean isTargetAchieved() {
        if (monthlyTarget == null || monthlyTarget.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        return currentMonthSales != null && currentMonthSales.compareTo(monthlyTarget) >= 0;
    }

    /**
     * 重置当月销售额
     */
    public void resetCurrentMonthSales() {
        this.currentMonthSales = BigDecimal.ZERO;
        updateActivity();
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
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public void setStatus(ChannelStatus status) {
        this.status = status;
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
                ", type=" + type +
                ", status=" + status +
                ", totalSales=" + totalSales +
                ", currentMonthSales=" + currentMonthSales +
                '}';
    }
}