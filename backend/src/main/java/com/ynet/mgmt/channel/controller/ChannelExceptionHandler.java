package com.ynet.mgmt.channel.controller;

import com.ynet.mgmt.channel.exception.*;
import com.ynet.mgmt.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 渠道管理全局异常处理器
 * 处理渠道相关的各种异常情况
 *
 * @author system
 * @since 1.0.0
 */
@RestControllerAdvice("com.ynet.mgmt.channel")
public class ChannelExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChannelExceptionHandler.class);

    /**
     * 处理渠道未找到异常
     */
    @ExceptionHandler(ChannelNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<Void>> handleChannelNotFound(ChannelNotFoundException e) {
        logger.warn("渠道未找到: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(e.getMessage()));
    }

    /**
     * 处理渠道重复异常
     */
    @ExceptionHandler(DuplicateChannelException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateChannel(DuplicateChannelException e) {
        logger.warn("渠道重复: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage(), "409"));
    }

    /**
     * 处理渠道状态异常
     */
    @ExceptionHandler(ChannelStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleChannelStatus(ChannelStatusException e) {
        logger.warn("渠道状态错误: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest(e.getMessage()));
    }

    /**
     * 处理渠道验证异常
     */
    @ExceptionHandler(ChannelValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleChannelValidation(ChannelValidationException e) {
        logger.warn("渠道验证失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest(e.getMessage()));
    }

    /**
     * 处理参数验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException e) {
        logger.warn("参数验证失败: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("参数验证失败", errors));
    }

    /**
     * 处理一般的业务异常
     */
    @ExceptionHandler(com.ynet.mgmt.common.exception.BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            com.ynet.mgmt.common.exception.BusinessException e) {
        logger.warn("业务异常: code={}, message={}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
    }

    /**
     * 处理实体未找到异常
     */
    @ExceptionHandler(com.ynet.mgmt.common.exception.EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(
            com.ynet.mgmt.common.exception.EntityNotFoundException e) {
        logger.warn("实体未找到: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(e.getMessage()));
    }

    /**
     * 处理验证异常
     */
    @ExceptionHandler(com.ynet.mgmt.common.exception.ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            com.ynet.mgmt.common.exception.ValidationException e) {
        logger.warn("数据验证异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest(e.getMessage()));
    }

    /**
     * 处理IllegalArgumentException异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("参数错误: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("参数错误: " + e.getMessage()));
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        logger.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统异常，请稍后重试", "500"));
    }
}