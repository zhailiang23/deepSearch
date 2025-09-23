package com.ynet.mgmt.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一API响应格式
 *
 * @author system
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String code;
    private Long timestamp;

    // 构造函数
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(boolean success, String message, T data, String code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    // 成功响应静态方法
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "操作成功", null, "200");
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data, "200");
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, "200");
    }

    public static <T> ApiResponse<T> success(String message, T data, String code) {
        return new ApiResponse<>(true, message, data, code);
    }

    // 失败响应静态方法
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, "500");
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, message, null, code);
    }

    public static <T> ApiResponse<T> error(String message, T data, String code) {
        return new ApiResponse<>(false, message, data, code);
    }

    // 业务便捷方法
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "创建成功", data, "201");
    }

    public static <T> ApiResponse<T> updated(T data) {
        return new ApiResponse<>(true, "更新成功", data, "200");
    }

    public static <T> ApiResponse<T> deleted() {
        return new ApiResponse<>(true, "删除成功", null, "200");
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, message, null, "404");
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, message, null, "400");
    }

    public static <T> ApiResponse<T> validationError(String message, T errors) {
        return new ApiResponse<>(false, message, errors, "400");
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}