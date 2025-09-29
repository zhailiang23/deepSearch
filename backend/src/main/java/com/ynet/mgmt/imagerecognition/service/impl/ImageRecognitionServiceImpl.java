package com.ynet.mgmt.imagerecognition.service.impl;

import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;
import com.ynet.mgmt.imagerecognition.dto.ImageRecognitionResponse;
import com.ynet.mgmt.imagerecognition.service.ImageRecognitionService;
import com.ynet.mgmt.imagerecognition.service.SiliconFlowVisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片识别服务实现
 */
@Service
public class ImageRecognitionServiceImpl implements ImageRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(ImageRecognitionServiceImpl.class);

    private final SiliconFlowVisionService visionService;

    public ImageRecognitionServiceImpl(SiliconFlowVisionService visionService) {
        this.visionService = visionService;
    }

    @Override
    public ImageRecognitionResponse recognizeActivity(MultipartFile file) {
        try {
            logger.info("开始处理图片识别请求: {}", file.getOriginalFilename());

            // 处理图片识别
            ActivityInfo activityInfo = processImageRecognition(file);

            logger.info("图片识别成功: {}", activityInfo.getName());
            return ImageRecognitionResponse.success(activityInfo);

        } catch (IllegalArgumentException e) {
            logger.warn("图片识别参数错误: {}", e.getMessage());
            return ImageRecognitionResponse.error("参数错误: " + e.getMessage());

        } catch (Exception e) {
            logger.error("图片识别失败", e);
            return ImageRecognitionResponse.error("图片识别失败: " + e.getMessage());
        }
    }

    @Override
    public ActivityInfo processImageRecognition(MultipartFile file) throws Exception {
        // 基本参数验证
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        logger.info("处理图片识别，文件名: {}, 大小: {} 字节, 类型: {}",
                   file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // 调用硅基流动 Vision API 进行图片识别
            ActivityInfo activityInfo = visionService.recognizeImage(file);

            // 验证识别结果
            validateRecognitionResult(activityInfo);

            // 后处理：清理和标准化数据
            postProcessActivityInfo(activityInfo);

            return activityInfo;

        } catch (Exception e) {
            logger.error("图片识别处理失败: {}", e.getMessage(), e);

            // 根据异常类型重新抛出适当的异常
            if (e instanceof IllegalArgumentException) {
                throw e;
            }

            throw new RuntimeException("图片识别服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 验证识别结果
     */
    private void validateRecognitionResult(ActivityInfo activityInfo) {
        if (activityInfo == null) {
            throw new RuntimeException("识别结果为空");
        }

        // 记录识别结果的完整性
        boolean hasBasicInfo = activityInfo.getName() != null &&
                              !activityInfo.getName().trim().isEmpty() &&
                              !activityInfo.getName().equals("无法识别");

        if (!hasBasicInfo && (activityInfo.getAll() == null || activityInfo.getAll().trim().isEmpty())) {
            throw new RuntimeException("未能从图片中识别到有效信息");
        }

        logger.debug("识别结果验证通过，包含基本信息: {}", hasBasicInfo);
    }

    /**
     * 后处理活动信息
     * 清理和标准化数据
     */
    private void postProcessActivityInfo(ActivityInfo activityInfo) {
        // 清理空白字符
        if (activityInfo.getName() != null) {
            activityInfo.setName(activityInfo.getName().trim());
        }
        if (activityInfo.getDescript() != null) {
            activityInfo.setDescript(activityInfo.getDescript().trim());
        }
        if (activityInfo.getLink() != null) {
            activityInfo.setLink(activityInfo.getLink().trim());
        }
        if (activityInfo.getStartDate() != null) {
            activityInfo.setStartDate(activityInfo.getStartDate().trim());
        }
        if (activityInfo.getEndDate() != null) {
            activityInfo.setEndDate(activityInfo.getEndDate().trim());
        }
        if (activityInfo.getStatus() != null) {
            activityInfo.setStatus(activityInfo.getStatus().trim());
        }
        if (activityInfo.getAll() != null) {
            activityInfo.setAll(activityInfo.getAll().trim());
        }

        // 设置默认值
        if (activityInfo.getName() == null || activityInfo.getName().isEmpty()) {
            activityInfo.setName("未识别到活动名称");
        }
        if (activityInfo.getDescript() == null || activityInfo.getDescript().isEmpty()) {
            activityInfo.setDescript("未识别到活动描述");
        }

        logger.debug("活动信息后处理完成");
    }
}