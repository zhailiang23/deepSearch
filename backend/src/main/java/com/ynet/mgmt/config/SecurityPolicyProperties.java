package com.ynet.mgmt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

/**
 * 安全策略配置属性类
 *
 * 从application.yml中读取安全相关配置
 * 包括登录尝试限制、账户锁定策略、IP访问控制等
 */
@Component
@ConfigurationProperties(prefix = "security.policy")
@Validated
public class SecurityPolicyProperties {

    /**
     * 最大登录失败次数
     * 超过此次数将锁定账户
     */
    @Min(value = 1, message = "最大登录失败次数必须大于0")
    private int maxFailedAttempts = 5;

    /**
     * 账户锁定时长(分钟)
     * 账户被锁定后需要等待的时间
     */
    @Positive(message = "账户锁定时长必须为正数")
    private int lockoutDurationMinutes = 30;

    /**
     * 失败尝试重置时间(小时)
     * 在此时间内没有失败尝试将重置失败计数
     */
    @Positive(message = "失败尝试重置时间必须为正数")
    private int attemptResetHours = 24;

    /**
     * IP访问速率限制
     * 单个IP每分钟最大请求数
     */
    @Min(value = 1, message = "IP访问速率限制必须大于0")
    private int ipRateLimitPerMinute = 100;

    /**
     * IP登录尝试限制
     * 单个IP每小时最大登录尝试次数
     */
    @Min(value = 1, message = "IP登录尝试限制必须大于0")
    private int ipLoginAttemptsPerHour = 20;

    /**
     * IP黑名单时长(分钟)
     * IP被标记为可疑后的黑名单时长
     */
    @Positive(message = "IP黑名单时长必须为正数")
    private int ipBlacklistDurationMinutes = 60;

    /**
     * 是否启用IP访问控制
     */
    @NotNull
    private boolean enableIpControl = true;

    /**
     * 是否启用逐步延迟
     * 失败次数越多，延迟越长
     */
    @NotNull
    private boolean enableProgressiveDelay = true;

    /**
     * 基础延迟时间(秒)
     * 第一次失败后的延迟时间
     */
    @Min(value = 0, message = "基础延迟时间不能为负数")
    private int baseDelaySeconds = 1;

    /**
     * 最大延迟时间(秒)
     * 逐步延迟的上限
     */
    @Positive(message = "最大延迟时间必须为正数")
    private int maxDelaySeconds = 60;

    /**
     * 是否启用可疑活动检测
     */
    @NotNull
    private boolean enableSuspiciousActivityDetection = true;

    /**
     * 可疑活动检测阈值
     * 触发可疑活动检测的最小事件数
     */
    @Min(value = 1, message = "可疑活动检测阈值必须大于0")
    private int suspiciousActivityThreshold = 3;

    /**
     * 地理位置检测启用标志
     */
    @NotNull
    private boolean enableGeolocationDetection = false;

    /**
     * 用户代理变化检测启用标志
     */
    @NotNull
    private boolean enableUserAgentDetection = true;

    /**
     * 威胁情报集成启用标志
     */
    @NotNull
    private boolean enableThreatIntelligence = false;

    // 构造函数
    public SecurityPolicyProperties() {}

    // 业务方法

    /**
     * 获取账户锁定时长
     * @return Duration对象
     */
    public Duration getLockoutDuration() {
        return Duration.ofMinutes(lockoutDurationMinutes);
    }

    /**
     * 获取失败尝试重置时长
     * @return Duration对象
     */
    public Duration getAttemptResetDuration() {
        return Duration.ofHours(attemptResetHours);
    }

    /**
     * 获取IP黑名单时长
     * @return Duration对象
     */
    public Duration getIpBlacklistDuration() {
        return Duration.ofMinutes(ipBlacklistDurationMinutes);
    }

    /**
     * 计算逐步延迟时间
     * @param failedAttempts 失败次数
     * @return 延迟秒数
     */
    public int calculateDelaySeconds(int failedAttempts) {
        if (!enableProgressiveDelay || failedAttempts <= 0) {
            return 0;
        }

        // 指数延迟: baseDelay * 2^(attempts-1)
        int delaySeconds = baseDelaySeconds * (int) Math.pow(2, failedAttempts - 1);
        return Math.min(delaySeconds, maxDelaySeconds);
    }

    /**
     * 判断是否为高风险阈值
     * @param eventCount 事件数量
     * @return 是否超过阈值
     */
    public boolean isHighRiskActivity(int eventCount) {
        return eventCount >= suspiciousActivityThreshold;
    }

    // Getters and Setters

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public int getAttemptResetHours() {
        return attemptResetHours;
    }

    public void setAttemptResetHours(int attemptResetHours) {
        this.attemptResetHours = attemptResetHours;
    }

    public int getIpRateLimitPerMinute() {
        return ipRateLimitPerMinute;
    }

    public void setIpRateLimitPerMinute(int ipRateLimitPerMinute) {
        this.ipRateLimitPerMinute = ipRateLimitPerMinute;
    }

    public int getIpLoginAttemptsPerHour() {
        return ipLoginAttemptsPerHour;
    }

    public void setIpLoginAttemptsPerHour(int ipLoginAttemptsPerHour) {
        this.ipLoginAttemptsPerHour = ipLoginAttemptsPerHour;
    }

    public int getIpBlacklistDurationMinutes() {
        return ipBlacklistDurationMinutes;
    }

    public void setIpBlacklistDurationMinutes(int ipBlacklistDurationMinutes) {
        this.ipBlacklistDurationMinutes = ipBlacklistDurationMinutes;
    }

    public boolean isEnableIpControl() {
        return enableIpControl;
    }

    public void setEnableIpControl(boolean enableIpControl) {
        this.enableIpControl = enableIpControl;
    }

    public boolean isEnableProgressiveDelay() {
        return enableProgressiveDelay;
    }

    public void setEnableProgressiveDelay(boolean enableProgressiveDelay) {
        this.enableProgressiveDelay = enableProgressiveDelay;
    }

    public int getBaseDelaySeconds() {
        return baseDelaySeconds;
    }

    public void setBaseDelaySeconds(int baseDelaySeconds) {
        this.baseDelaySeconds = baseDelaySeconds;
    }

    public int getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(int maxDelaySeconds) {
        this.maxDelaySeconds = maxDelaySeconds;
    }

    public boolean isEnableSuspiciousActivityDetection() {
        return enableSuspiciousActivityDetection;
    }

    public void setEnableSuspiciousActivityDetection(boolean enableSuspiciousActivityDetection) {
        this.enableSuspiciousActivityDetection = enableSuspiciousActivityDetection;
    }

    public int getSuspiciousActivityThreshold() {
        return suspiciousActivityThreshold;
    }

    public void setSuspiciousActivityThreshold(int suspiciousActivityThreshold) {
        this.suspiciousActivityThreshold = suspiciousActivityThreshold;
    }

    public boolean isEnableGeolocationDetection() {
        return enableGeolocationDetection;
    }

    public void setEnableGeolocationDetection(boolean enableGeolocationDetection) {
        this.enableGeolocationDetection = enableGeolocationDetection;
    }

    public boolean isEnableUserAgentDetection() {
        return enableUserAgentDetection;
    }

    public void setEnableUserAgentDetection(boolean enableUserAgentDetection) {
        this.enableUserAgentDetection = enableUserAgentDetection;
    }

    public boolean isEnableThreatIntelligence() {
        return enableThreatIntelligence;
    }

    public void setEnableThreatIntelligence(boolean enableThreatIntelligence) {
        this.enableThreatIntelligence = enableThreatIntelligence;
    }

    @Override
    public String toString() {
        return "SecurityPolicyProperties{" +
                "maxFailedAttempts=" + maxFailedAttempts +
                ", lockoutDurationMinutes=" + lockoutDurationMinutes +
                ", attemptResetHours=" + attemptResetHours +
                ", ipRateLimitPerMinute=" + ipRateLimitPerMinute +
                ", ipLoginAttemptsPerHour=" + ipLoginAttemptsPerHour +
                ", ipBlacklistDurationMinutes=" + ipBlacklistDurationMinutes +
                ", enableIpControl=" + enableIpControl +
                ", enableProgressiveDelay=" + enableProgressiveDelay +
                ", baseDelaySeconds=" + baseDelaySeconds +
                ", maxDelaySeconds=" + maxDelaySeconds +
                ", enableSuspiciousActivityDetection=" + enableSuspiciousActivityDetection +
                ", suspiciousActivityThreshold=" + suspiciousActivityThreshold +
                ", enableGeolocationDetection=" + enableGeolocationDetection +
                ", enableUserAgentDetection=" + enableUserAgentDetection +
                ", enableThreatIntelligence=" + enableThreatIntelligence +
                '}';
    }
}