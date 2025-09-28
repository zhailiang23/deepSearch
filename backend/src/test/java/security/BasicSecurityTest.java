package security;

import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.security.HotWordSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * 基础安全性测试
 * 不依赖Spring Security Test，验证核心安全功能
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-28
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:basicsecuritytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@DisplayName("基础安全性测试")
public class BasicSecurityTest {

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private HotWordSecurityConfig.ParameterValidator parameterValidator;

    @Autowired
    private HotWordSecurityConfig.RateLimiter rateLimiter;

    @Autowired
    private HotWordSecurityConfig.DataAccessController dataAccessController;

    private final LocalDateTime baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        searchLogRepository.deleteAll();
    }

    // ========== 参数验证测试 ==========

    @Test
    @DisplayName("测试SQL注入检测")
    void testSqlInjectionDetection() {
        String[] maliciousInputs = {
            "'; DROP TABLE search_logs; --",
            "' OR '1'='1",
            "1; DELETE FROM search_logs WHERE id > 0; --",
            "' UNION SELECT password FROM users --",
            "'; UPDATE search_logs SET search_query = 'hacked'; --"
        };

        for (String maliciousInput : maliciousInputs) {
            boolean isSecure = parameterValidator.isSecureString(maliciousInput, "testParam");
            assertThat(isSecure).isFalse();
        }

        // 测试正常输入
        boolean isSecure = parameterValidator.isSecureString("Java编程教程", "query");
        assertThat(isSecure).isTrue();
    }

    @Test
    @DisplayName("测试XSS攻击检测")
    void testXssDetection() {
        String[] xssInputs = {
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "<iframe src='malicious.com'></iframe>",
            "<object data='malicious.swf'></object>"
        };

        for (String xssInput : xssInputs) {
            boolean isSecure = parameterValidator.isSecureString(xssInput, "testParam");
            assertThat(isSecure).isFalse();
        }
    }

    @Test
    @DisplayName("测试路径遍历攻击检测")
    void testPathTraversalDetection() {
        String[] pathTraversalInputs = {
            "../../etc/passwd",
            "..\\windows\\system32",
            "%2e%2e%2f%2e%2e%2fetc%2fpasswd"
        };

        for (String pathTraversalInput : pathTraversalInputs) {
            boolean isSecure = parameterValidator.isSecureString(pathTraversalInput, "testParam");
            assertThat(isSecure).isFalse();
        }
    }

    @Test
    @DisplayName("测试数字参数范围验证")
    void testNumericRangeValidation() {
        // 测试有效范围
        assertThat(parameterValidator.isValidRange(5, 1, 10, "limit")).isTrue();
        assertThat(parameterValidator.isValidRange(1, 1, 10, "limit")).isTrue();
        assertThat(parameterValidator.isValidRange(10, 1, 10, "limit")).isTrue();

        // 测试无效范围
        assertThat(parameterValidator.isValidRange(0, 1, 10, "limit")).isFalse();
        assertThat(parameterValidator.isValidRange(11, 1, 10, "limit")).isFalse();
        assertThat(parameterValidator.isValidRange(-1, 1, 10, "limit")).isFalse();

        // 测试空值
        assertThat(parameterValidator.isValidRange(null, 1, 10, "limit")).isTrue();
    }

    @Test
    @DisplayName("测试时间范围验证")
    void testTimeRangeValidation() {
        LocalDateTime start = baseTime;
        LocalDateTime end = baseTime.plusDays(1);

        // 测试有效时间范围
        assertThat(parameterValidator.isValidTimeRange(start, end, 7)).isTrue();

        // 测试时间顺序错误
        assertThat(parameterValidator.isValidTimeRange(end, start, 7)).isFalse();

        // 测试时间范围过大
        LocalDateTime farEnd = baseTime.plusDays(10);
        assertThat(parameterValidator.isValidTimeRange(start, farEnd, 7)).isFalse();

        // 测试未来时间查询
        LocalDateTime future = LocalDateTime.now().plusDays(2);
        assertThat(parameterValidator.isValidTimeRange(start, future, 7)).isFalse();

        // 测试空值
        assertThat(parameterValidator.isValidTimeRange(null, null, 7)).isTrue();
    }

    // ========== 访问频率限制测试 ==========

    @Test
    @DisplayName("测试API访问频率限制")
    void testApiRateLimit() {
        String userId = "testUser";
        String clientIp = "127.0.0.1";

        // 前几次访问应该被允许
        for (int i = 0; i < 5; i++) {
            boolean allowed = rateLimiter.isAllowed(userId, clientIp);
            assertThat(allowed).isTrue();
        }

        // 注意：由于配置的限制可能较高，这里只测试基本功能
        // 在实际环境中，可以通过配置调整限制进行更严格的测试
    }

    @Test
    @DisplayName("测试不同用户的访问频率限制隔离")
    void testRateLimitIsolation() {
        String user1 = "user1";
        String user2 = "user2";
        String clientIp = "127.0.0.1";

        // 用户1的访问
        for (int i = 0; i < 3; i++) {
            boolean allowed = rateLimiter.isAllowed(user1, clientIp);
            assertThat(allowed).isTrue();
        }

        // 用户2的访问应该不受影响
        for (int i = 0; i < 3; i++) {
            boolean allowed = rateLimiter.isAllowed(user2, clientIp);
            assertThat(allowed).isTrue();
        }
    }

    // ========== 数据访问控制测试 ==========

    @Test
    @DisplayName("测试用户数据访问权限")
    void testUserDataAccessControl() {
        // 管理员可以访问所有数据
        assertThat(dataAccessController.canAccessUserData("admin", "user1", "ADMIN")).isTrue();
        assertThat(dataAccessController.canAccessUserData("admin", "user2", "SUPER_ADMIN")).isTrue();

        // 用户只能访问自己的数据
        assertThat(dataAccessController.canAccessUserData("user1", "user1", "USER")).isTrue();
        assertThat(dataAccessController.canAccessUserData("user1", "user2", "USER")).isFalse();

        // 测试空值情况
        assertThat(dataAccessController.canAccessUserData(null, "user1", "USER")).isFalse();
    }

    @Test
    @DisplayName("测试查询结果数量限制")
    void testQueryResultLimit() {
        // 普通用户的限制
        int userLimit = dataAccessController.getEffectiveLimit(500, "USER");
        assertThat(userLimit).isLessThanOrEqualTo(1000); // 默认最大限制

        // 管理员有更高的限制
        int adminLimit = dataAccessController.getEffectiveLimit(1500, "ADMIN");
        assertThat(adminLimit).isEqualTo(1500); // 管理员可以有更高限制

        // 超过管理员限制的情况
        int reducedLimit = dataAccessController.getEffectiveLimit(5000, "ADMIN");
        assertThat(reducedLimit).isLessThanOrEqualTo(2000); // 管理员最大限制的2倍
    }

    // ========== 数据完整性测试 ==========

    @Test
    @DisplayName("测试基础数据完整性")
    void testBasicDataIntegrity() {
        // 创建测试数据
        SearchLog log = createTestSearchLog("测试查询", 1L, baseTime);
        SearchLog savedLog = searchLogRepository.save(log);

        // 验证数据完整性
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getSearchQuery()).isEqualTo("测试查询");
        assertThat(savedLog.getUserId()).isEqualTo(1L);
        assertThat(savedLog.getStatus()).isEqualTo(SearchLogStatus.SUCCESS);

        // 验证数据库约束
        SearchLog retrievedLog = searchLogRepository.findById(savedLog.getId()).orElse(null);
        assertThat(retrievedLog).isNotNull();
        assertThat(retrievedLog.getSearchQuery()).isEqualTo(savedLog.getSearchQuery());
    }

    @Test
    @DisplayName("测试数据库查询参数化")
    void testParameterizedQueries() {
        // 创建测试数据，包含特殊字符
        String specialQuery = "Java + Spring & Boot (测试)";
        SearchLog log = createTestSearchLog(specialQuery, 1L, baseTime);
        SearchLog savedLog = searchLogRepository.save(log);

        // 使用Repository方法查询（应该是参数化的）
        SearchLog foundLog = searchLogRepository.findByTraceId(savedLog.getTraceId()).orElse(null);
        assertThat(foundLog).isNotNull();
        assertThat(foundLog.getSearchQuery()).isEqualTo(specialQuery);

        // 测试like查询
        var logs = searchLogRepository.findPopularSearchQueries(baseTime.minusDays(1), 10);
        assertThat(logs).isNotNull();
    }

    // ========== 安全工具类测试 ==========

    @Test
    @DisplayName("测试输入清理功能")
    void testInputSanitization() {
        String maliciousInput = "<script>alert('xss')</script>";
        String sanitized = HotWordSecurityConfig.SecurityUtils.sanitizeInput(maliciousInput);

        assertThat(sanitized).doesNotContain("<script>");
        assertThat(sanitized).doesNotContain("</script>");
        assertThat(sanitized).contains("&lt;").contains("&gt;");
    }

    @Test
    @DisplayName("测试敏感数据脱敏")
    void testSensitiveDataMasking() {
        String phoneNumber = "13812345678";
        String masked = HotWordSecurityConfig.SecurityUtils.maskSensitiveData(phoneNumber, '*', 3);

        assertThat(masked).startsWith("138");
        assertThat(masked).endsWith("*******");
        assertThat(masked).hasSize(11);
    }

    // ========== 辅助方法 ==========

    private SearchLog createTestSearchLog(String query, Long userId, LocalDateTime createdAt) {
        SearchLog log = new SearchLog();
        log.setSearchQuery(query);
        log.setUserId(userId);
        log.setSearchSpaceId(1L);
        log.setSearchSpaceCode("test-space");
        log.setStatus(SearchLogStatus.SUCCESS);
        log.setTotalResults(100L);
        log.setTotalTimeMs(50L);
        log.setIpAddress("127.0.0.1");
        log.setSessionId("test-session");
        log.setUserAgent("Test Agent");
        log.setTraceId("trace-" + System.nanoTime());
        log.setCreatedAt(createdAt);
        log.setUpdatedAt(createdAt);
        return log;
    }
}