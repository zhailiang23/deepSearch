package com.ynet.mgmt.imagerecognition.controller;

import com.ynet.mgmt.imagerecognition.dto.BatchConversionRequest;
import com.ynet.mgmt.imagerecognition.dto.BatchConversionResponse;
import com.ynet.mgmt.imagerecognition.service.BatchConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 批量数据转换控制器
 */
@RestController
@RequestMapping("/image/batch")
@Tag(name = "批量数据转换", description = "批量图片识别和数据转换相关接口")
public class BatchConversionController {

    private static final Logger logger = LoggerFactory.getLogger(BatchConversionController.class);

    private final BatchConversionService batchConversionService;

    public BatchConversionController(BatchConversionService batchConversionService) {
        this.batchConversionService = batchConversionService;
    }

    @PostMapping("/convert")
    @Operation(summary = "批量识别数据库中的图片", description = "连接指定数据库，读取图片并批量识别其中的文本内容")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量识别完成"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<BatchConversionResponse> batchConvert(
            @Valid @RequestBody BatchConversionRequest request) {

        logger.info("收到批量转换请求: 数据库={}:{}/{}, 表={}",
                    request.getDbHost(), request.getDbPort(), request.getDbName(), request.getTableName());

        try {
            BatchConversionResponse response = batchConversionService.batchRecognizeImages(request);

            if (response.isSuccess()) {
                logger.info("批量转换成功: 总数={}, 成功={}, 失败={}",
                           response.getTotalCount(), response.getSuccessCount(), response.getFailureCount());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("批量转换失败: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            logger.error("处理批量转换请求时发生异常", e);
            return ResponseEntity.internalServerError()
                .body(BatchConversionResponse.error("服务器内部错误: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查批量转换服务是否正常运行")
    @ApiResponse(responseCode = "200", description = "服务正常")
    public ResponseEntity<String> healthCheck() {
        logger.debug("批量转换服务健康检查");
        return ResponseEntity.ok("Batch Conversion Service is running");
    }
}
