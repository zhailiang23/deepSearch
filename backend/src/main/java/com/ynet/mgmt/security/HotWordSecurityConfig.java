package com.ynet.mgmt.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 热词统计功能安全配置
 *
 * 提供以下安全功能：
 * 1. API访问频率限制
 * 2. 参数安全验证
 * 3. SQL注入防护
 * 4. 敏感数据访问控制
 * 5. 审计日志记录
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-28
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class HotWordSecurityConfig {

    /**
     * 速率限制器Bean
     */
    @Bean
    public RateLimiter hotWordApiRateLimiter() {
        return new RateLimiter();
    }

    /**
     * 参数验证器Bean
     */
    @Bean
    public ParameterValidator hotWordParameterValidator() {
        return new ParameterValidator();
    }

    /**
     * 安全审计器Bean
     */
    @Bean
    public SecurityAuditor hotWordSecurityAuditor() {
        return new SecurityAuditor();
    }

    /**
     * 数据访问控制器Bean
     */
    @Bean
    public DataAccessController hotWordDataAccessController() {
        return new DataAccessController();
    }

    /**
     * API访问频率限制器
     */
    @Component
    public static class RateLimiter {
        private final ConcurrentHashMap<String, UserRateLimit> userLimits = new ConcurrentHashMap<>();

        @Value("${hotword.security.rate-limit.requests-per-minute:60}")
        private int requestsPerMinute;

        @Value("${hotword.security.rate-limit.burst-capacity:10}")
        private int burstCapacity;

        /**
         * 检查用户是否超过访问频率限制
         *
         * @param userId 用户ID
         * @param clientIp 客户端IP
         * @return 是否允许访问
         */
        public boolean isAllowed(String userId, String clientIp) {
            String key = userId != null ? "user:" + userId : "ip:" + clientIp;
            UserRateLimit limit = userLimits.computeIfAbsent(key, k -> new UserRateLimit());

            LocalDateTime now = LocalDateTime.now();

            // 清理过期的计数
            if (limit.windowStart.isBefore(now.minusMinutes(1))) {
                limit.reset(now);
            }

            // 检查是否超过限制
            if (limit.requestCount.get() >= requestsPerMinute) {
                // 记录超限事件
                logRateLimitViolation(key, limit.requestCount.get());
                return false;
            }

            // 检查突发流量限制
            if (limit.burstCount.get() >= burstCapacity) {
                if (limit.lastBurstReset.isBefore(now.minusSeconds(10))) {
                    limit.burstCount.set(0);
                    limit.lastBurstReset = now;
                } else {
                    logBurstLimitViolation(key, limit.burstCount.get());
                    return false;
                }
            }

            limit.requestCount.incrementAndGet();
            limit.burstCount.incrementAndGet();
            return true;
        }

        private void logRateLimitViolation(String key, int requestCount) {
            System.err.printf("[SECURITY] Rate limit exceeded for %s: %d requests/minute%n", key, requestCount);
        }

        private void logBurstLimitViolation(String key, int burstCount) {
            System.err.printf("[SECURITY] Burst limit exceeded for %s: %d requests in 10 seconds%n", key, burstCount);
        }

        private static class UserRateLimit {
            AtomicInteger requestCount = new AtomicInteger(0);
            AtomicInteger burstCount = new AtomicInteger(0);
            LocalDateTime windowStart = LocalDateTime.now();
            LocalDateTime lastBurstReset = LocalDateTime.now();

            void reset(LocalDateTime now) {
                requestCount.set(0);
                windowStart = now;
            }
        }
    }

    /**
     * 参数安全验证器
     */
    @Component
    public static class ParameterValidator {

        // SQL注入检测模式
        private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|declare|sp_|xp_).*"
        );

        // XSS攻击检测模式
        private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i).*(<script|javascript:|onload=|onerror=|<iframe|<object|<embed).*"
        );

        // 路径遍历检测模式
        private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            ".*(\\.\\.[\\/\\\\]|%2e%2e[\\/\\\\]|\\.\\.%2f|\\.\\.%5c).*"
        );

        /**
         * 验证字符串参数安全性
         *
         * @param value 参数值
         * @param paramName 参数名称
         * @return 是否安全
         */
        public boolean isSecureString(String value, String paramName) {
            if (value == null) {
                return true;
            }

            // 检测SQL注入
            if (SQL_INJECTION_PATTERN.matcher(value).matches()) {
                logSecurityViolation("SQL_INJECTION", paramName, value);
                return false;
            }

            // 检测XSS攻击
            if (XSS_PATTERN.matcher(value).matches()) {
                logSecurityViolation("XSS_ATTACK", paramName, value);
                return false;
            }

            // 检测路径遍历
            if (PATH_TRAVERSAL_PATTERN.matcher(value).matches()) {
                logSecurityViolation("PATH_TRAVERSAL", paramName, value);
                return false;
            }

            return true;
        }

        /**
         * 验证数字参数范围
         *
         * @param value 参数值
         * @param min 最小值
         * @param max 最大值
         * @param paramName 参数名称
         * @return 是否在有效范围内
         */
        public boolean isValidRange(Integer value, int min, int max, String paramName) {
            if (value == null) {
                return true;
            }

            if (value < min || value > max) {
                logSecurityViolation("INVALID_RANGE", paramName,
                    String.format("Value: %d, Expected: %d-%d", value, min, max));
                return false;
            }

            return true;
        }

        /**
         * 验证时间范围参数
         *
         * @param startTime 开始时间
         * @param endTime 结束时间
         * @param maxDays 最大允许的天数范围
         * @return 是否有效
         */
        public boolean isValidTimeRange(LocalDateTime startTime, LocalDateTime endTime, int maxDays) {
            if (startTime == null || endTime == null) {
                return true;
            }

            // 检查时间顺序
            if (startTime.isAfter(endTime)) {
                logSecurityViolation("INVALID_TIME_ORDER", "timeRange",
                    String.format("Start: %s, End: %s", startTime, endTime));
                return false;
            }

            // 检查时间范围
            long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);
            if (daysBetween > maxDays) {
                logSecurityViolation("TIME_RANGE_TOO_LARGE", "timeRange",
                    String.format("Days: %d, Max: %d", daysBetween, maxDays));
                return false;
            }

            // 检查是否查询未来时间
            if (endTime.isAfter(LocalDateTime.now().plusDays(1))) {
                logSecurityViolation("FUTURE_TIME_QUERY", "timeRange",
                    String.format("End time: %s", endTime));
                return false;
            }

            return true;
        }

        private void logSecurityViolation(String violationType, String paramName, String value) {
            System.err.printf("[SECURITY] %s detected in parameter '%s': %s%n",
                violationType, paramName, value);
        }
    }

    /**
     * 安全审计记录器
     */
    @Component
    public static class SecurityAuditor {

        /**
         * 记录API访问事件
         *
         * @param userId 用户ID
         * @param endpoint API端点
         * @param clientIp 客户端IP
         * @param userAgent 用户代理
         * @param parameters 请求参数
         */
        public void logApiAccess(String userId, String endpoint, String clientIp,
                                String userAgent, String parameters) {
            System.out.printf("[AUDIT] API Access - User: %s, Endpoint: %s, IP: %s, Agent: %s, Params: %s%n",
                userId, endpoint, clientIp, userAgent, parameters);
        }

        /**
         * 记录数据访问事件
         *
         * @param userId 用户ID
         * @param operation 操作类型
         * @param dataType 数据类型
         * @param recordCount 记录数量
         */
        public void logDataAccess(String userId, String operation, String dataType, int recordCount) {
            System.out.printf("[AUDIT] Data Access - User: %s, Operation: %s, Type: %s, Count: %d%n",
                userId, operation, dataType, recordCount);
        }

        /**
         * 记录安全异常事件
         *
         * @param userId 用户ID
         * @param eventType 事件类型
         * @param details 详细信息
         * @param clientIp 客户端IP
         */
        public void logSecurityEvent(String userId, String eventType, String details, String clientIp) {
            System.err.printf("[SECURITY] Security Event - User: %s, Type: %s, Details: %s, IP: %s%n",
                userId, eventType, details, clientIp);
        }

        /**
         * 记录性能异常事件
         *
         * @param userId 用户ID
         * @param operation 操作
         * @param duration 耗时（毫秒）
         * @param threshold 阈值（毫秒）
         */
        public void logPerformanceEvent(String userId, String operation, long duration, long threshold) {
            System.out.printf("[PERFORMANCE] Slow Operation - User: %s, Operation: %s, Duration: %dms, Threshold: %dms%n",
                userId, operation, duration, threshold);
        }
    }

    /**
     * 数据访问控制器
     */
    @Component
    public static class DataAccessController {

        @Value("${hotword.security.max-query-results:1000}")
        private int maxQueryResults;

        @Value("${hotword.security.sensitive-data-access.enabled:true}")
        private boolean sensitiveDataAccessEnabled;

        /**
         * 检查用户是否有权限访问指定数据
         *
         * @param userId 当前用户ID
         * @param targetUserId 目标用户ID
         * @param userRole 用户角色
         * @return 是否有权限
         */
        public boolean canAccessUserData(String userId, String targetUserId, String userRole) {
            // 管理员可以访问所有数据
            if ("ADMIN".equals(userRole) || "SUPER_ADMIN".equals(userRole)) {
                return true;
            }

            // 普通用户只能访问自己的数据
            if (userId != null && userId.equals(targetUserId)) {
                return true;
            }

            // 记录未授权访问尝试
            System.err.printf("[SECURITY] Unauthorized data access attempt - User: %s, Target: %s, Role: %s%n",
                userId, targetUserId, userRole);
            return false;
        }

        /**
         * 检查查询结果数量是否超过限制
         *
         * @param requestedLimit 请求的限制数量
         * @param userRole 用户角色
         * @return 实际允许的限制数量
         */
        public int getEffectiveLimit(int requestedLimit, String userRole) {
            int maxAllowed = "ADMIN".equals(userRole) ? maxQueryResults * 2 : maxQueryResults;

            if (requestedLimit > maxAllowed) {
                System.out.printf("[SECURITY] Query limit reduced from %d to %d for role: %s%n",
                    requestedLimit, maxAllowed, userRole);
                return maxAllowed;
            }

            return requestedLimit;
        }

        /**
         * 过滤敏感数据字段
         *
         * @param data 原始数据
         * @param userRole 用户角色
         * @return 过滤后的数据
         */
        public Object filterSensitiveData(Object data, String userRole) {
            if (!sensitiveDataAccessEnabled || "ADMIN".equals(userRole)) {
                return data;
            }

            // 这里可以实现具体的数据脱敏逻辑
            // 例如：隐藏IP地址、用户ID等敏感信息
            return data;
        }
    }

    /**
     * 自定义安全注解
     */
    public static class HotWordSecurity {

        /**
         * 热词查询权限验证
         */
        @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
        public @interface RequireHotWordAccess {}

        /**
         * 管理员权限验证
         */
        @PreAuthorize("hasRole('ADMIN')")
        public @interface RequireAdminAccess {}

        /**
         * 数据管理权限验证
         */
        @PreAuthorize("hasRole('ADMIN') or hasRole('DATA_MANAGER')")
        public @interface RequireDataManagement {}
    }

    /**
     * 安全工具类
     */
    public static class SecurityUtils {

        /**
         * 获取客户端真实IP地址
         *
         * @param request HTTP请求
         * @return 客户端IP地址
         */
        public static String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }

            String remoteAddr = request.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : "unknown";
        }

        /**
         * 清理用户输入，防止XSS攻击
         *
         * @param input 用户输入
         * @return 清理后的字符串
         */
        public static String sanitizeInput(String input) {
            if (input == null) {
                return null;
            }

            return input
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
        }

        /**
         * 脱敏处理敏感信息
         *
         * @param value 原始值
         * @param maskChar 脱敏字符
         * @param visibleChars 保留可见字符数
         * @return 脱敏后的值
         */
        public static String maskSensitiveData(String value, char maskChar, int visibleChars) {
            if (value == null || value.length() <= visibleChars) {
                return value;
            }

            StringBuilder masked = new StringBuilder();
            masked.append(value, 0, visibleChars);
            for (int i = visibleChars; i < value.length(); i++) {
                masked.append(maskChar);
            }
            return masked.toString();
        }
    }
}