package com.ynet.mgmt.channel.dto;

import com.ynet.mgmt.channel.entity.ChannelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量状态更新请求参数
 * 用于批量更改多个渠道的状态
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "批量状态更新请求参数")
public class BatchStatusUpdateRequest {

    /**
     * 渠道ID列表
     */
    @Schema(description = "渠道ID列表", example = "[1, 2, 3]")
    @NotNull(message = "渠道ID列表不能为空")
    @NotEmpty(message = "渠道ID列表不能为空")
    private List<Long> channelIds;

    /**
     * 目标状态
     */
    @Schema(description = "目标状态")
    @NotNull(message = "目标状态不能为空")
    private ChannelStatus targetStatus;

    /**
     * 操作原因说明
     */
    @Schema(description = "操作原因说明", example = "批量停用过期渠道")
    private String reason;

    // 构造函数
    public BatchStatusUpdateRequest() {}

    public BatchStatusUpdateRequest(List<Long> channelIds, ChannelStatus targetStatus) {
        this.channelIds = channelIds;
        this.targetStatus = targetStatus;
    }

    public BatchStatusUpdateRequest(List<Long> channelIds, ChannelStatus targetStatus, String reason) {
        this.channelIds = channelIds;
        this.targetStatus = targetStatus;
        this.reason = reason;
    }

    // Getters and Setters
    public List<Long> getChannelIds() {
        return channelIds;
    }

    public void setChannelIds(List<Long> channelIds) {
        this.channelIds = channelIds;
    }

    public ChannelStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(ChannelStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "BatchStatusUpdateRequest{" +
                "channelIds=" + channelIds +
                ", targetStatus=" + targetStatus +
                ", reason='" + reason + '\'' +
                '}';
    }
}