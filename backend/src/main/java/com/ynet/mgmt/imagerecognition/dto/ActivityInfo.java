package com.ynet.mgmt.imagerecognition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 活动信息实体类
 * 用于存储从图片中识别出的活动信息
 */
@Schema(description = "活动信息")
public class ActivityInfo {

    @Schema(description = "活动名称")
    @JsonProperty("name")
    private String name;

    @Schema(description = "活动描述")
    @JsonProperty("descript")
    private String descript;

    @Schema(description = "活动链接")
    @JsonProperty("link")
    private String link;

    @Schema(description = "开始时间")
    @JsonProperty("startDate")
    private String startDate;

    @Schema(description = "结束时间")
    @JsonProperty("endDate")
    private String endDate;

    @Schema(description = "活动状态")
    @JsonProperty("status")
    private String status;

    @Schema(description = "从图片中识别到的所有文字信息")
    @JsonProperty("all")
    private String all;

    public ActivityInfo() {
    }

    public ActivityInfo(String name, String descript, String link, String startDate, String endDate, String status, String all) {
        this.name = name;
        this.descript = descript;
        this.link = link;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.all = all;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescript() {
        return descript;
    }

    public void setDescript(String descript) {
        this.descript = descript;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAll() {
        return all;
    }

    public void setAll(String all) {
        this.all = all;
    }

    @Override
    public String toString() {
        return "ActivityInfo{" +
                "name='" + name + '\'' +
                ", descript='" + descript + '\'' +
                ", link='" + link + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", status='" + status + '\'' +
                ", all='" + all + '\'' +
                '}';
    }
}