package com.ynet.mgmt.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新销售数据请求参数
 * 用于更新渠道的销售金额和业绩数据
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "更新销售数据请求参数")
public class UpdateSalesRequest {

    /**
     * 销售金额
     */
    @Schema(description = "销售金额", example = "50000.00")
    @NotNull(message = "销售金额不能为空")
    @Positive(message = "销售金额必须为正数")
    private BigDecimal salesAmount;

    /**
     * 销售描述
     */
    @Schema(description = "销售描述", example = "线上商城月度销售")
    @NotBlank(message = "销售描述不能为空")
    private String description;

    /**
     * 销售时间
     */
    @Schema(description = "销售时间", example = "2025-09-26T10:30:00")
    private LocalDateTime salesDate;

    /**
     * 是否累加到总销售额
     */
    @Schema(description = "是否累加到总销售额", example = "true")
    private Boolean addToTotal = true;

    /**
     * 是否累加到当月销售额
     */
    @Schema(description = "是否累加到当月销售额", example = "true")
    private Boolean addToMonthly = true;

    // 构造函数
    public UpdateSalesRequest() {}

    public UpdateSalesRequest(BigDecimal salesAmount, String description) {
        this.salesAmount = salesAmount;
        this.description = description;
    }

    public UpdateSalesRequest(BigDecimal salesAmount, String description, LocalDateTime salesDate) {
        this.salesAmount = salesAmount;
        this.description = description;
        this.salesDate = salesDate;
    }

    // Getters and Setters
    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getSalesDate() {
        return salesDate;
    }

    public void setSalesDate(LocalDateTime salesDate) {
        this.salesDate = salesDate;
    }

    public Boolean getAddToTotal() {
        return addToTotal;
    }

    public void setAddToTotal(Boolean addToTotal) {
        this.addToTotal = addToTotal;
    }

    public Boolean getAddToMonthly() {
        return addToMonthly;
    }

    public void setAddToMonthly(Boolean addToMonthly) {
        this.addToMonthly = addToMonthly;
    }

    @Override
    public String toString() {
        return "UpdateSalesRequest{" +
                "salesAmount=" + salesAmount +
                ", description='" + description + '\'' +
                ", salesDate=" + salesDate +
                ", addToTotal=" + addToTotal +
                ", addToMonthly=" + addToMonthly +
                '}';
    }
}