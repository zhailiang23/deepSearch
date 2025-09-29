package com.ynet.mgmt.imagerecognition.service;

import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;
import com.ynet.mgmt.imagerecognition.dto.VisionApiRequest;
import com.ynet.mgmt.imagerecognition.dto.VisionApiResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 硅基流动 Vision API 服务接口
 */
public interface SiliconFlowVisionService {

    /**
     * 调用 Vision API 识别图片中的活动信息
     *
     * @param file 图片文件
     * @return 活动信息
     * @throws Exception 调用 API 时的异常
     */
    ActivityInfo recognizeImage(MultipartFile file) throws Exception;

    /**
     * 调用 Vision API
     *
     * @param request Vision API 请求
     * @return Vision API 响应
     * @throws Exception 调用 API 时的异常
     */
    VisionApiResponse callVisionApi(VisionApiRequest request) throws Exception;

    /**
     * 将图片文件转换为 Base64 编码
     *
     * @param file 图片文件
     * @return Base64 编码的图片数据
     * @throws Exception 转换时的异常
     */
    String encodeImageToBase64(MultipartFile file) throws Exception;

    /**
     * 验证图片文件
     *
     * @param file 图片文件
     * @throws Exception 验证失败时的异常
     */
    void validateImageFile(MultipartFile file) throws Exception;
}