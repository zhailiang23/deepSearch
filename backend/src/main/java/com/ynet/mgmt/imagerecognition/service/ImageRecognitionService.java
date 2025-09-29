package com.ynet.mgmt.imagerecognition.service;

import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;
import com.ynet.mgmt.imagerecognition.dto.ImageRecognitionResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片识别服务接口
 */
public interface ImageRecognitionService {

    /**
     * 识别图片中的活动信息
     *
     * @param file 上传的图片文件
     * @return 图片识别响应
     */
    ImageRecognitionResponse recognizeActivity(MultipartFile file);

    /**
     * 处理图片识别请求
     * 包含完整的业务逻辑处理：验证、识别、异常处理等
     *
     * @param file 上传的图片文件
     * @return 活动信息
     * @throws Exception 处理过程中的异常
     */
    ActivityInfo processImageRecognition(MultipartFile file) throws Exception;
}