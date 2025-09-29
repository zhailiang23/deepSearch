package com.ynet.mgmt.imagerecognition.dto;

import com.ynet.mgmt.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 图片识别响应 DTO
 * 继承自通用的 ApiResponse，包含活动信息数据
 */
@Schema(description = "图片识别响应")
public class ImageRecognitionResponse extends ApiResponse<ActivityInfo> {

    public ImageRecognitionResponse() {
        super();
    }

    public ImageRecognitionResponse(boolean success, String message, ActivityInfo data) {
        super(success, message, data);
    }

    public ImageRecognitionResponse(ActivityInfo data) {
        super(true, "图片识别成功", data);
    }

    /**
     * 创建成功响应
     */
    public static ImageRecognitionResponse success(ActivityInfo data) {
        return new ImageRecognitionResponse(data);
    }

    /**
     * 创建失败响应
     */
    public static ImageRecognitionResponse error(String message) {
        return new ImageRecognitionResponse(false, message, null);
    }
}