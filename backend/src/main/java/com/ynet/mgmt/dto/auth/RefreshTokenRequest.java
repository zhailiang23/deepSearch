package com.ynet.mgmt.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新Token请求DTO
 */
public class RefreshTokenRequest {

    @NotBlank(message = "刷新Token不能为空")
    private String refreshToken;

    // 构造函数
    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{refreshToken='[PROTECTED]'}";
    }
}