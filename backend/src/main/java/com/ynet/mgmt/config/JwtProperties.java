package com.ynet.mgmt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * JWT配置属性类
 *
 * 从application.yml中读取JWT相关配置
 * 包括密钥、过期时间、签发者等信息
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /**
     * JWT签名密钥
     * 必须至少256位以确保安全性
     */
    @NotBlank(message = "JWT密钥不能为空")
    private String secret;

    /**
     * 访问令牌过期时间(毫秒)
     * 默认1小时
     */
    @Positive(message = "访问令牌过期时间必须为正数")
    private long accessTokenExpiration = 3600000L;

    /**
     * 刷新令牌过期时间(毫秒)
     * 默认30天
     */
    @Positive(message = "刷新令牌过期时间必须为正数")
    private long refreshTokenExpiration = 2592000000L;

    /**
     * JWT签发者
     */
    @NotBlank(message = "JWT签发者不能为空")
    private String issuer = "deepSearch";

    /**
     * JWT受众
     */
    @NotBlank(message = "JWT受众不能为空")
    private String audience = "deepSearch-users";

    /**
     * JWT签名算法
     */
    private String algorithm = "HS256";

    // Getters and Setters

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return "JwtProperties{" +
                "secret=[PROTECTED]" +
                ", accessTokenExpiration=" + accessTokenExpiration +
                ", refreshTokenExpiration=" + refreshTokenExpiration +
                ", issuer='" + issuer + '\'' +
                ", audience='" + audience + '\'' +
                ", algorithm='" + algorithm + '\'' +
                '}';
    }
}