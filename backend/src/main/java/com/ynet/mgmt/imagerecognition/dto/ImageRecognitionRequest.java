package com.ynet.mgmt.imagerecognition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片识别请求 DTO
 */
@Schema(description = "图片识别请求")
public class ImageRecognitionRequest {

    @Schema(description = "上传的图片文件", required = true)
    private MultipartFile file;

    public ImageRecognitionRequest() {
    }

    public ImageRecognitionRequest(MultipartFile file) {
        this.file = file;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "ImageRecognitionRequest{" +
                "file=" + (file != null ? file.getOriginalFilename() : "null") +
                '}';
    }
}