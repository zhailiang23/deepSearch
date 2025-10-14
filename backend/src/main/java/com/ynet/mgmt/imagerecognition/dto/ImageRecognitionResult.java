package com.ynet.mgmt.imagerecognition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单张图片识别结果 DTO
 */
@Schema(description = "单张图片识别结果")
public class ImageRecognitionResult {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "图片路径或 URL")
    private String imagePath;

    @Schema(description = "图片缩略图 Base64（用于前端展示）")
    private String thumbnailBase64;

    @Schema(description = "识别的文本内容")
    private String recognizedText;

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "活动描述")
    private String descript;

    @Schema(description = "活动链接")
    private String link;

    @Schema(description = "活动开始时间")
    private String startDate;

    @Schema(description = "活动结束时间")
    private String endDate;

    @Schema(description = "活动状态")
    private String status;

    @Schema(description = "是否识别成功")
    private boolean success;

    @Schema(description = "错误信息（如果失败）")
    private String errorMessage;

    public ImageRecognitionResult() {
    }

    public ImageRecognitionResult(String id, String imagePath) {
        this.id = id;
        this.imagePath = imagePath;
    }

    public static ImageRecognitionResult success(String id, String imagePath, String thumbnailBase64, String recognizedText) {
        ImageRecognitionResult result = new ImageRecognitionResult(id, imagePath);
        result.setSuccess(true);
        result.setThumbnailBase64(thumbnailBase64);
        result.setRecognizedText(recognizedText);
        return result;
    }

    public static ImageRecognitionResult failure(String id, String imagePath, String errorMessage) {
        ImageRecognitionResult result = new ImageRecognitionResult(id, imagePath);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getThumbnailBase64() {
        return thumbnailBase64;
    }

    public void setThumbnailBase64(String thumbnailBase64) {
        this.thumbnailBase64 = thumbnailBase64;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
}
