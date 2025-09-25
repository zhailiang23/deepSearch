package com.ynet.mgmt.channel.dto;

import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 渠道查询请求参数
 * 支持分页、排序、筛选等查询条件
 *
 * @author system
 * @since 1.0.0
 */
@Schema(description = "渠道查询请求参数")
public class ChannelQueryRequest {

    /**
     * 页码，从0开始
     */
    @Schema(description = "页码，从0开始", example = "0")
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", example = "20")
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Integer size = 20;

    /**
     * 排序字段，格式：字段名,方向 例如：name,asc
     */
    @Schema(description = "排序字段，格式：字段名,方向", example = "name,asc")
    private String sort = "createdAt,desc";

    /**
     * 关键词搜索（支持渠道名称、代码搜索）
     */
    @Schema(description = "关键词搜索（支持渠道名称、代码搜索）", example = "线上渠道")
    private String keyword;

    /**
     * 渠道类型筛选
     */
    @Schema(description = "渠道类型筛选")
    private ChannelType type;

    /**
     * 渠道状态筛选
     */
    @Schema(description = "渠道状态筛选")
    private ChannelStatus status;

    /**
     * 联系人姓名搜索
     */
    @Schema(description = "联系人姓名搜索", example = "张三")
    private String contactName;

    /**
     * 联系人电话搜索
     */
    @Schema(description = "联系人电话搜索", example = "138")
    private String contactPhone;

    /**
     * 联系人邮箱搜索
     */
    @Schema(description = "联系人邮箱搜索", example = "@gmail.com")
    private String contactEmail;

    /**
     * 是否只查询激活状态的渠道
     */
    @Schema(description = "是否只查询激活状态的渠道", example = "true")
    private Boolean activeOnly = false;

    // 构造函数
    public ChannelQueryRequest() {}

    // Getters and Setters
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
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

    public Boolean getActiveOnly() {
        return activeOnly;
    }

    public void setActiveOnly(Boolean activeOnly) {
        this.activeOnly = activeOnly;
    }

    @Override
    public String toString() {
        return "ChannelQueryRequest{" +
                "page=" + page +
                ", size=" + size +
                ", sort='" + sort + '\'' +
                ", keyword='" + keyword + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", contactName='" + contactName + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", activeOnly=" + activeOnly +
                '}';
    }
}