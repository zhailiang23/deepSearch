package com.ynet.mgmt.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道统计数据
 * 包含渠道管理的统计信息
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "渠道统计数据")
public class ChannelStatistics {

    /**
     * 总渠道数量
     */
    @Schema(description = "总渠道数量", example = "50")
    private Integer totalChannels;

    /**
     * 激活渠道数量
     */
    @Schema(description = "激活渠道数量", example = "45")
    private Integer activeChannels;

    /**
     * 暂停渠道数量
     */
    @Schema(description = "暂停渠道数量", example = "3")
    private Integer suspendedChannels;

    /**
     * 停用渠道数量
     */
    @Schema(description = "停用渠道数量", example = "2")
    private Integer inactiveChannels;

    /**
     * 线上渠道数量
     */
    @Schema(description = "线上渠道数量", example = "30")
    private Integer onlineChannels;

    /**
     * 线下渠道数量
     */
    @Schema(description = "线下渠道数量", example = "15")
    private Integer offlineChannels;

    /**
     * 分销商数量
     */
    @Schema(description = "分销商数量", example = "5")
    private Integer distributorChannels;

    /**
     * 总销售额
     */
    @Schema(description = "总销售额", example = "5000000.00")
    private BigDecimal totalSales;

    /**
     * 当月总销售额
     */
    @Schema(description = "当月总销售额", example = "500000.00")
    private BigDecimal monthlyTotalSales;

    /**
     * 达成月度目标的渠道数量
     */
    @Schema(description = "达成月度目标的渠道数量", example = "35")
    private Integer targetAchievedChannels;

    /**
     * 平均佣金率
     */
    @Schema(description = "平均佣金率", example = "0.08")
    private BigDecimal averageCommissionRate;

    /**
     * 最高单月销售额
     */
    @Schema(description = "最高单月销售额", example = "150000.00")
    private BigDecimal highestMonthlySales;

    /**
     * 统计时间
     */
    @Schema(description = "统计时间")
    private LocalDateTime statisticsTime;

    // 构造函数
    public ChannelStatistics() {
        this.statisticsTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getTotalChannels() {
        return totalChannels;
    }

    public void setTotalChannels(Integer totalChannels) {
        this.totalChannels = totalChannels;
    }

    public Integer getActiveChannels() {
        return activeChannels;
    }

    public void setActiveChannels(Integer activeChannels) {
        this.activeChannels = activeChannels;
    }

    public Integer getSuspendedChannels() {
        return suspendedChannels;
    }

    public void setSuspendedChannels(Integer suspendedChannels) {
        this.suspendedChannels = suspendedChannels;
    }

    public Integer getInactiveChannels() {
        return inactiveChannels;
    }

    public void setInactiveChannels(Integer inactiveChannels) {
        this.inactiveChannels = inactiveChannels;
    }

    public Integer getOnlineChannels() {
        return onlineChannels;
    }

    public void setOnlineChannels(Integer onlineChannels) {
        this.onlineChannels = onlineChannels;
    }

    public Integer getOfflineChannels() {
        return offlineChannels;
    }

    public void setOfflineChannels(Integer offlineChannels) {
        this.offlineChannels = offlineChannels;
    }

    public Integer getDistributorChannels() {
        return distributorChannels;
    }

    public void setDistributorChannels(Integer distributorChannels) {
        this.distributorChannels = distributorChannels;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getMonthlyTotalSales() {
        return monthlyTotalSales;
    }

    public void setMonthlyTotalSales(BigDecimal monthlyTotalSales) {
        this.monthlyTotalSales = monthlyTotalSales;
    }

    public Integer getTargetAchievedChannels() {
        return targetAchievedChannels;
    }

    public void setTargetAchievedChannels(Integer targetAchievedChannels) {
        this.targetAchievedChannels = targetAchievedChannels;
    }

    public BigDecimal getAverageCommissionRate() {
        return averageCommissionRate;
    }

    public void setAverageCommissionRate(BigDecimal averageCommissionRate) {
        this.averageCommissionRate = averageCommissionRate;
    }

    public BigDecimal getHighestMonthlySales() {
        return highestMonthlySales;
    }

    public void setHighestMonthlySales(BigDecimal highestMonthlySales) {
        this.highestMonthlySales = highestMonthlySales;
    }

    public LocalDateTime getStatisticsTime() {
        return statisticsTime;
    }

    public void setStatisticsTime(LocalDateTime statisticsTime) {
        this.statisticsTime = statisticsTime;
    }

    @Override
    public String toString() {
        return "ChannelStatistics{" +
                "totalChannels=" + totalChannels +
                ", activeChannels=" + activeChannels +
                ", suspendedChannels=" + suspendedChannels +
                ", inactiveChannels=" + inactiveChannels +
                ", onlineChannels=" + onlineChannels +
                ", offlineChannels=" + offlineChannels +
                ", distributorChannels=" + distributorChannels +
                ", totalSales=" + totalSales +
                ", monthlyTotalSales=" + monthlyTotalSales +
                ", targetAchievedChannels=" + targetAchievedChannels +
                ", averageCommissionRate=" + averageCommissionRate +
                ", highestMonthlySales=" + highestMonthlySales +
                ", statisticsTime=" + statisticsTime +
                '}';
    }
}