package com.ynet.mgmt.imagerecognition.controller;

import com.ynet.mgmt.imagerecognition.dto.ImageRecognitionResponse;
import com.ynet.mgmt.imagerecognition.service.ImageRecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片识别控制器
 */
@RestController
@RequestMapping("/image")
@Tag(name = "图片识别", description = "图片识别相关接口")
public class ImageRecognitionController {

    private static final Logger logger = LoggerFactory.getLogger(ImageRecognitionController.class);

    private final ImageRecognitionService imageRecognitionService;

    public ImageRecognitionController(ImageRecognitionService imageRecognitionService) {
        this.imageRecognitionService = imageRecognitionService;
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "识别图片中的活动信息", description = "上传 JPG 或 PNG 图片，识别其中的活动相关文字信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "识别成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ImageRecognitionResponse> recognizeActivity(
            @Parameter(description = "图片文件 (JPG/PNG 格式，最大 10MB)", required = true)
            @RequestParam("file") MultipartFile file) {

        logger.info("收到图片识别请求: {}", file != null ? file.getOriginalFilename() : "null");

        try {
            // 基本参数验证
            if (file == null || file.isEmpty()) {
                logger.warn("图片文件为空");
                return ResponseEntity.badRequest()
                    .body(ImageRecognitionResponse.error("图片文件不能为空"));
            }

            // 调用服务进行图片识别
            ImageRecognitionResponse response = imageRecognitionService.recognizeActivity(file);

            // 根据识别结果返回相应的 HTTP 状态码
            if (response.isSuccess()) {
                logger.info("图片识别成功: {}", file.getOriginalFilename());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("图片识别失败: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("处理图片识别请求时发生异常", e);
            return ResponseEntity.internalServerError()
                .body(ImageRecognitionResponse.error("服务器内部错误，请稍后重试"));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查图片识别服务是否正常运行")
    @ApiResponse(responseCode = "200", description = "服务正常")
    public ResponseEntity<String> healthCheck() {
        logger.debug("图片识别服务健康检查");
        return ResponseEntity.ok("Image Recognition Service is running");
    }
}