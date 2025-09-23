package com.ynet.mgmt.auth.security;

import com.ynet.mgmt.config.SecurityPolicyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * IP地址跟踪和监控服务
 *
 * 提供IP级别的访问控制、速率限制和威胁检测功能
 * 基于Redis实现高性能的IP统计和黑名单管理
 */
@Service
public class IpTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(IpTrackingService.class);

    // Redis Key 前缀
    private static final String IP_REQUEST_PREFIX = "ip:req:";
    private static final String IP_LOGIN_PREFIX = "ip:login:";
    private static final String IP_BLACKLIST_PREFIX = "ip:blacklist:";
    private static final String IP_WHITELIST_PREFIX = "ip:whitelist:";
    private static final String IP_STATS_PREFIX = "ip:stats:";
    private static final String IP_THREAT_PREFIX = "ip:threat:";

    // 时间窗口标识
    private static final String MINUTE_WINDOW = ":min:";
    private static final String HOUR_WINDOW = ":hour:";
    private static final String DAY_WINDOW = ":day:";

    private final SecurityPolicyProperties securityProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    // Lua脚本用于原子性操作
    private final DefaultRedisScript<Long> incrementScript;

    @Autowired
    public IpTrackingService(SecurityPolicyProperties securityProperties,
                           RedisTemplate<String, Object> redisTemplate) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
        this.incrementScript = createIncrementScript();
    }

    /**
     * 记录IP请求
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @return 是否允许请求
     */
    public boolean recordRequest(String ipAddress, String userAgent) {
        if (!securityProperties.isEnableIpControl()) {
            return true;
        }

        try {
            // 检查白名单
            if (isWhitelisted(ipAddress)) {
                return true;
            }

            // 检查黑名单
            if (isBlacklisted(ipAddress)) {
                logger.warn("Request blocked from blacklisted IP: {}", ipAddress);
                return false;
            }

            // 记录请求统计
            String currentMinute = getCurrentTimeWindow("minute");
            String requestKey = IP_REQUEST_PREFIX + ipAddress + MINUTE_WINDOW + currentMinute;

            // 使用Lua脚本原子性增加计数并检查限制
            Long currentCount = incrementWithTTL(requestKey, 60);

            // 检查是否超过速率限制
            if (currentCount > securityProperties.getIpRateLimitPerMinute()) {
                // 临时加入黑名单
                addToTempBlacklist(ipAddress, "RATE_LIMIT_EXCEEDED");
                logger.warn("IP {} exceeded rate limit: {} requests/minute", ipAddress, currentCount);
                return false;
            }

            // 记录用户代理信息
            recordUserAgent(ipAddress, userAgent);

            // 检测可疑活动
            if (securityProperties.isEnableSuspiciousActivityDetection()) {
                detectSuspiciousActivity(ipAddress, userAgent);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error recording request for IP: {}", ipAddress, e);
            return true; // 出错时允许请求，避免误伤
        }
    }

    /**
     * 记录登录尝试
     * @param ipAddress IP地址
     * @param username 用户名
     * @param success 是否成功
     */
    public void recordLoginAttempt(String ipAddress, String username, boolean success) {
        if (!securityProperties.isEnableIpControl()) {
            return;
        }

        try {
            String currentHour = getCurrentTimeWindow("hour");
            String loginKey = IP_LOGIN_PREFIX + ipAddress + HOUR_WINDOW + currentHour;

            // 增加登录尝试计数
            Long currentCount = incrementWithTTL(loginKey, 3600);

            // 记录详细统计
            recordLoginStats(ipAddress, username, success);

            // 检查登录尝试限制
            if (!success && currentCount > securityProperties.getIpLoginAttemptsPerHour()) {
                addToTempBlacklist(ipAddress, "LOGIN_ATTEMPTS_EXCEEDED");
                logger.warn("IP {} exceeded login attempts: {} attempts/hour", ipAddress, currentCount);
            }

            // 检测多账户登录尝试
            if (securityProperties.isEnableSuspiciousActivityDetection()) {
                detectMultiUserAttempts(ipAddress, username);
            }

        } catch (Exception e) {
            logger.error("Error recording login attempt for IP: {}", ipAddress, e);
        }
    }

    /**
     * 检查IP是否被黑名单
     * @param ipAddress IP地址
     * @return 是否被黑名单
     */
    public boolean isBlacklisted(String ipAddress) {
        String key = IP_BLACKLIST_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 检查IP是否在白名单
     * @param ipAddress IP地址
     * @return 是否在白名单
     */
    public boolean isWhitelisted(String ipAddress) {
        String key = IP_WHITELIST_PREFIX + ipAddress;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 手动添加IP到黑名单
     * @param ipAddress IP地址
     * @param reason 原因
     * @param durationMinutes 持续时间(分钟)
     */
    public void addToBlacklist(String ipAddress, String reason, int durationMinutes) {
        String key = IP_BLACKLIST_PREFIX + ipAddress;
        BlacklistEntry entry = new BlacklistEntry(ipAddress, reason, LocalDateTime.now());

        redisTemplate.opsForValue().set(key, entry);
        if (durationMinutes > 0) {
            redisTemplate.expire(key, durationMinutes, TimeUnit.MINUTES);
        }

        logger.info("IP {} added to blacklist: {}, duration: {} minutes", ipAddress, reason, durationMinutes);
    }

    /**
     * 添加IP到临时黑名单
     * @param ipAddress IP地址
     * @param reason 原因
     */
    public void addToTempBlacklist(String ipAddress, String reason) {
        addToBlacklist(ipAddress, reason, securityProperties.getIpBlacklistDurationMinutes());
    }

    /**
     * 添加IP到白名单
     * @param ipAddress IP地址
     * @param reason 原因
     */
    public void addToWhitelist(String ipAddress, String reason) {
        String key = IP_WHITELIST_PREFIX + ipAddress;
        WhitelistEntry entry = new WhitelistEntry(ipAddress, reason, LocalDateTime.now());

        redisTemplate.opsForValue().set(key, entry);
        logger.info("IP {} added to whitelist: {}", ipAddress, reason);
    }

    /**
     * 从黑名单移除IP
     * @param ipAddress IP地址
     * @return 是否成功移除
     */
    public boolean removeFromBlacklist(String ipAddress) {
        String key = IP_BLACKLIST_PREFIX + ipAddress;
        boolean removed = Boolean.TRUE.equals(redisTemplate.delete(key));
        if (removed) {
            logger.info("IP {} removed from blacklist", ipAddress);
        }
        return removed;
    }

    /**
     * 从白名单移除IP
     * @param ipAddress IP地址
     * @return 是否成功移除
     */
    public boolean removeFromWhitelist(String ipAddress) {
        String key = IP_WHITELIST_PREFIX + ipAddress;
        boolean removed = Boolean.TRUE.equals(redisTemplate.delete(key));
        if (removed) {
            logger.info("IP {} removed from whitelist", ipAddress);
        }
        return removed;
    }

    /**
     * 获取IP统计信息
     * @param ipAddress IP地址
     * @return 统计信息
     */
    public IpStatistics getIpStatistics(String ipAddress) {
        try {
            String currentMinute = getCurrentTimeWindow("minute");
            String currentHour = getCurrentTimeWindow("hour");
            String currentDay = getCurrentTimeWindow("day");

            // 获取各时间窗口的请求数
            Integer requestsLastMinute = (Integer) redisTemplate.opsForValue()
                .get(IP_REQUEST_PREFIX + ipAddress + MINUTE_WINDOW + currentMinute);
            Integer requestsLastHour = (Integer) redisTemplate.opsForValue()
                .get(IP_REQUEST_PREFIX + ipAddress + HOUR_WINDOW + currentHour);
            Integer requestsLastDay = (Integer) redisTemplate.opsForValue()
                .get(IP_REQUEST_PREFIX + ipAddress + DAY_WINDOW + currentDay);

            // 获取登录统计
            Integer loginAttemptsLastHour = (Integer) redisTemplate.opsForValue()
                .get(IP_LOGIN_PREFIX + ipAddress + HOUR_WINDOW + currentHour);

            // 获取威胁评分
            Double threatScore = getThreatScore(ipAddress);

            return new IpStatistics(
                ipAddress,
                requestsLastMinute != null ? requestsLastMinute : 0,
                requestsLastHour != null ? requestsLastHour : 0,
                requestsLastDay != null ? requestsLastDay : 0,
                loginAttemptsLastHour != null ? loginAttemptsLastHour : 0,
                isBlacklisted(ipAddress),
                isWhitelisted(ipAddress),
                threatScore
            );

        } catch (Exception e) {
            logger.error("Error getting statistics for IP: {}", ipAddress, e);
            return new IpStatistics(ipAddress, 0, 0, 0, 0, false, false, 0.0);
        }
    }

    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        try {
            String pattern = IP_STATS_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                // 批量删除过期的统计数据
                redisTemplate.delete(keys);
                logger.info("Cleaned up {} expired IP statistics entries", keys.size());
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired data", e);
        }
    }

    /**
     * 获取当前时间窗口标识
     * @param windowType 窗口类型 (minute/hour/day)
     * @return 时间窗口标识
     */
    private String getCurrentTimeWindow(String windowType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (windowType) {
            case "minute" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            case "hour" -> now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            case "day" -> now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            default -> throw new IllegalArgumentException("Invalid window type: " + windowType);
        };
    }

    /**
     * 原子性增加计数并设置TTL
     * @param key Redis键
     * @param ttlSeconds TTL秒数
     * @return 增加后的值
     */
    private Long incrementWithTTL(String key, int ttlSeconds) {
        return redisTemplate.execute(incrementScript,
            Collections.singletonList(key), ttlSeconds);
    }

    /**
     * 记录用户代理信息
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     */
    private void recordUserAgent(String ipAddress, String userAgent) {
        if (!securityProperties.isEnableUserAgentDetection() || userAgent == null) {
            return;
        }

        String key = IP_STATS_PREFIX + ipAddress + ":ua";
        redisTemplate.opsForSet().add(key, userAgent);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    /**
     * 记录登录统计
     * @param ipAddress IP地址
     * @param username 用户名
     * @param success 是否成功
     */
    private void recordLoginStats(String ipAddress, String username, boolean success) {
        String key = IP_STATS_PREFIX + ipAddress + ":users";
        redisTemplate.opsForSet().add(key, username);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);

        if (!success) {
            String failKey = IP_STATS_PREFIX + ipAddress + ":failures";
            incrementWithTTL(failKey, 3600);
        }
    }

    /**
     * 检测可疑活动
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     */
    private void detectSuspiciousActivity(String ipAddress, String userAgent) {
        // 检测用户代理变化
        if (securityProperties.isEnableUserAgentDetection()) {
            detectUserAgentChanges(ipAddress, userAgent);
        }

        // 检测异常时间段活动
        detectOffHoursActivity(ipAddress);
    }

    /**
     * 检测用户代理变化
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     */
    private void detectUserAgentChanges(String ipAddress, String userAgent) {
        String key = IP_STATS_PREFIX + ipAddress + ":ua";
        Set<Object> userAgents = redisTemplate.opsForSet().members(key);

        if (userAgents != null && userAgents.size() > securityProperties.getSuspiciousActivityThreshold()) {
            increaseThreatScore(ipAddress, 0.3, "MULTIPLE_USER_AGENTS");
        }
    }

    /**
     * 检测多用户登录尝试
     * @param ipAddress IP地址
     * @param username 用户名
     */
    private void detectMultiUserAttempts(String ipAddress, String username) {
        String key = IP_STATS_PREFIX + ipAddress + ":users";
        Set<Object> users = redisTemplate.opsForSet().members(key);

        if (users != null && users.size() > securityProperties.getSuspiciousActivityThreshold()) {
            increaseThreatScore(ipAddress, 0.5, "MULTIPLE_USER_ATTEMPTS");
        }
    }

    /**
     * 检测非工作时间活动
     * @param ipAddress IP地址
     */
    private void detectOffHoursActivity(String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        // 假设工作时间是 8:00 - 22:00
        if (hour < 8 || hour > 22) {
            increaseThreatScore(ipAddress, 0.2, "OFF_HOURS_ACTIVITY");
        }
    }

    /**
     * 增加威胁评分
     * @param ipAddress IP地址
     * @param score 评分增量
     * @param reason 原因
     */
    private void increaseThreatScore(String ipAddress, double score, String reason) {
        String key = IP_THREAT_PREFIX + ipAddress;
        redisTemplate.opsForValue().increment(key, score);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);

        Double currentScore = getThreatScore(ipAddress);
        if (currentScore > 1.0) {
            addToTempBlacklist(ipAddress, "HIGH_THREAT_SCORE");
            logger.warn("IP {} blocked due to high threat score: {}, reason: {}",
                ipAddress, currentScore, reason);
        }
    }

    /**
     * 获取威胁评分
     * @param ipAddress IP地址
     * @return 威胁评分
     */
    private Double getThreatScore(String ipAddress) {
        String key = IP_THREAT_PREFIX + ipAddress;
        Object score = redisTemplate.opsForValue().get(key);
        return score != null ? (Double) score : 0.0;
    }

    /**
     * 创建Lua脚本用于原子性增加计数
     * @return Lua脚本
     */
    private DefaultRedisScript<Long> createIncrementScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local key = KEYS[1] " +
            "local ttl = ARGV[1] " +
            "local current = redis.call('incr', key) " +
            "if current == 1 then " +
            "    redis.call('expire', key, ttl) " +
            "end " +
            "return current"
        );
        script.setResultType(Long.class);
        return script;
    }

    // 内部数据类

    /**
     * 黑名单条目
     */
    public static class BlacklistEntry {
        private final String ipAddress;
        private final String reason;
        private final LocalDateTime addedAt;

        public BlacklistEntry(String ipAddress, String reason, LocalDateTime addedAt) {
            this.ipAddress = ipAddress;
            this.reason = reason;
            this.addedAt = addedAt;
        }

        // Getters
        public String getIpAddress() { return ipAddress; }
        public String getReason() { return reason; }
        public LocalDateTime getAddedAt() { return addedAt; }
    }

    /**
     * 白名单条目
     */
    public static class WhitelistEntry {
        private final String ipAddress;
        private final String reason;
        private final LocalDateTime addedAt;

        public WhitelistEntry(String ipAddress, String reason, LocalDateTime addedAt) {
            this.ipAddress = ipAddress;
            this.reason = reason;
            this.addedAt = addedAt;
        }

        // Getters
        public String getIpAddress() { return ipAddress; }
        public String getReason() { return reason; }
        public LocalDateTime getAddedAt() { return addedAt; }
    }

    /**
     * IP统计信息
     */
    public static class IpStatistics {
        private final String ipAddress;
        private final int requestsLastMinute;
        private final int requestsLastHour;
        private final int requestsLastDay;
        private final int loginAttemptsLastHour;
        private final boolean blacklisted;
        private final boolean whitelisted;
        private final double threatScore;

        public IpStatistics(String ipAddress, int requestsLastMinute, int requestsLastHour,
                          int requestsLastDay, int loginAttemptsLastHour, boolean blacklisted,
                          boolean whitelisted, double threatScore) {
            this.ipAddress = ipAddress;
            this.requestsLastMinute = requestsLastMinute;
            this.requestsLastHour = requestsLastHour;
            this.requestsLastDay = requestsLastDay;
            this.loginAttemptsLastHour = loginAttemptsLastHour;
            this.blacklisted = blacklisted;
            this.whitelisted = whitelisted;
            this.threatScore = threatScore;
        }

        // Getters
        public String getIpAddress() { return ipAddress; }
        public int getRequestsLastMinute() { return requestsLastMinute; }
        public int getRequestsLastHour() { return requestsLastHour; }
        public int getRequestsLastDay() { return requestsLastDay; }
        public int getLoginAttemptsLastHour() { return loginAttemptsLastHour; }
        public boolean isBlacklisted() { return blacklisted; }
        public boolean isWhitelisted() { return whitelisted; }
        public double getThreatScore() { return threatScore; }
    }
}