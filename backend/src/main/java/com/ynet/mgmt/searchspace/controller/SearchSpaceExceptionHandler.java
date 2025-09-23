package com.ynet.mgmt.searchspace.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchspace.exception.ElasticsearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索空间异常处理器
 * 统一处理搜索空间相关的异常
 *
 * @author system
 * @since 1.0.0
 */
@RestControllerAdvice(basePackages = "com.ynet.mgmt.searchspace.controller")
public class SearchSpaceExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SearchSpaceExceptionHandler.class);

    /**
     * 处理参数验证异常
     *
     * @param ex 参数验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("参数验证失败: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(ApiResponse.validationError("参数验证失败", fieldErrors));
    }

    /**
     * 处理 Elasticsearch 异常
     *
     * @param ex Elasticsearch 异常
     * @return 错误响应
     */
    @ExceptionHandler(ElasticsearchException.class)
    public ResponseEntity<ApiResponse<Void>> handleElasticsearchException(ElasticsearchException ex) {
        logger.error("Elasticsearch操作失败: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("搜索引擎操作失败: " + ex.getMessage(), "500"));
    }

    /**
     * 处理非法参数异常
     *
     * @param ex 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("非法参数: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
    }

    /**
     * 处理数据不存在异常
     *
     * @param ex 运行时异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        // 根据异常消息判断是否为数据不存在
        if (message != null && (message.contains("不存在") || message.contains("未找到") || message.contains("not found"))) {
            logger.warn("数据不存在: {}", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(message));
        }

        // 其他运行时异常
        logger.error("运行时异常: {}", message, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("操作失败: " + message, "500"));
    }

    /**
     * 处理通用异常
     *
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("系统异常: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统异常，请稍后重试", "500"));
    }
}