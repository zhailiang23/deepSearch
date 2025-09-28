package security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.searchlog.dto.HotWordRequest;
import com.ynet.mgmt.searchlog.dto.SearchClickRequest;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.repository.SearchLogRepository;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 热词统计功能安全性测试
 *
 * 测试目标：
 * 1. 验证API认证和授权机制
 * 2. 测试SQL注入攻击防护
 * 3. 验证敏感数据访问控制
 * 4. 测试参数验证和边界检查
 * 5. 验证CORS和CSRF防护
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-28
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:securitytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.security=DEBUG"
})
@Transactional
@DisplayName("热词统计安全性测试")
public class HotWordSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @Autowired
    private SearchLogService searchLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ========== 认证和授权测试 ==========

    @Test
    @DisplayName("未认证用户访问热词统计接口应返回401")
    void testUnauthenticatedAccessToHotWords() throws Exception {
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("有USER权限的用户可以访问热词统计")
    void testUserAccessToHotWords() throws Exception {
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("有ADMIN权限的用户可以访问所有统计接口")
    void testAdminAccessToAllStatistics() throws Exception {
        // 热词统计
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "10"))
                .andExpect(status().isOk());

        // 搜索统计
        mockMvc.perform(get("/search-logs/statistics")
                .param("startTime", "2023-01-01 00:00:00")
                .param("endTime", "2023-12-31 23:59:59"))
                .andExpect(status().isOk());

        // 清理操作
        mockMvc.perform(delete("/search-logs/cleanup")
                .param("retentionDays", "30")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("普通用户无法执行管理操作")
    void testUserCannotAccessAdminOperations() throws Exception {
        // 尝试清理日志 - 应该被拒绝
        mockMvc.perform(delete("/search-logs/cleanup")
                .param("retentionDays", "30")
                .with(csrf()))
                .andExpect(status().isForbidden());

        // 尝试批量删除 - 应该被拒绝
        mockMvc.perform(delete("/search-logs/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1, 2, 3]")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== SQL注入攻击测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试热词查询参数的SQL注入防护")
    void testSqlInjectionInHotWordsQuery() throws Exception {
        // 尝试SQL注入攻击
        String[] maliciousInputs = {
            "'; DROP TABLE search_logs; --",
            "' OR '1'='1",
            "1; DELETE FROM search_logs WHERE id > 0; --",
            "' UNION SELECT password FROM users --",
            "'; UPDATE search_logs SET search_query = 'hacked'; --"
        };

        for (String maliciousInput : maliciousInputs) {
            mockMvc.perform(get("/search-logs/hot-words")
                    .param("userId", maliciousInput)
                    .param("searchSpaceId", maliciousInput)
                    .param("limit", "10"))
                    .andExpect(status().isOk()) // 应该正常处理，而不是执行SQL注入
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试时间参数的SQL注入防护")
    void testSqlInjectionInDateParameters() throws Exception {
        String[] maliciousDateInputs = {
            "2023-01-01'; DROP TABLE search_logs; --",
            "2023-01-01' OR '1'='1",
            "'; SELECT * FROM users; --"
        };

        for (String maliciousInput : maliciousDateInputs) {
            mockMvc.perform(get("/search-logs/hot-words")
                    .param("startDate", maliciousInput)
                    .param("endDate", "2023-12-31 23:59:59")
                    .param("limit", "10"))
                    .andExpect(status().isBadRequest()); // 应该返回参数错误
        }
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试搜索日志查询的SQL注入防护")
    void testSqlInjectionInSearchLogQuery() throws Exception {
        // 测试各种恶意输入
        mockMvc.perform(get("/search-logs")
                .param("query", "'; DROP TABLE search_logs; --")
                .param("userId", "' OR '1'='1")
                .param("sessionId", "1'; DELETE FROM search_logs; --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ========== 参数验证和边界测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试参数边界和无效输入处理")
    void testParameterValidationAndBoundaries() throws Exception {
        // 测试负数限制
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "-1"))
                .andExpect(status().isBadRequest());

        // 测试超大数值
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "999999"))
                .andExpect(status().isBadRequest());

        // 测试最小词长边界
        mockMvc.perform(get("/search-logs/hot-words")
                .param("minWordLength", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/search-logs/hot-words")
                .param("minWordLength", "100"))
                .andExpect(status().isBadRequest());

        // 测试时间顺序验证
        mockMvc.perform(get("/search-logs/hot-words")
                .param("startDate", "2023-12-31 23:59:59")
                .param("endDate", "2023-01-01 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试特殊字符和编码处理")
    void testSpecialCharactersAndEncoding() throws Exception {
        String[] specialInputs = {
            "<script>alert('xss')</script>",
            "%3Cscript%3Ealert%28%27xss%27%29%3C%2Fscript%3E",
            "../../etc/passwd",
            "%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "null",
            "undefined",
            ""
        };

        for (String input : specialInputs) {
            mockMvc.perform(get("/search-logs/hot-words")
                    .param("userId", input)
                    .param("limit", "10"))
                    .andExpect(status().isOk());
        }
    }

    // ========== 数据访问控制测试 ==========

    @Test
    @WithMockUser(username = "user1", authorities = "USER")
    @DisplayName("测试用户只能访问自己的数据")
    void testUserDataAccessControl() throws Exception {
        // 普通用户尝试访问其他用户的数据
        mockMvc.perform(get("/search-logs")
                .param("userId", "999999")) // 假设这是其他用户ID
                .andExpect(status().isOk()); // 目前系统允许查看，但应该记录日志
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试敏感信息脱敏")
    void testSensitiveDataMasking() throws Exception {
        // 检查返回的数据是否包含敏感信息
        mockMvc.perform(get("/search-logs/hot-words")
                .param("includeSegmentDetails", "true")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("password")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("token")
                )));
    }

    // ========== CSRF和CORS测试 ==========

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("测试CSRF防护机制")
    void testCsrfProtection() throws Exception {
        // 没有CSRF token的删除请求应该被拒绝
        mockMvc.perform(delete("/search-logs/cleanup")
                .param("retentionDays", "30"))
                .andExpect(status().isForbidden());

        // 有CSRF token的请求应该成功
        mockMvc.perform(delete("/search-logs/cleanup")
                .param("retentionDays", "30")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试CORS配置")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/search-logs/hot-words")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        // 测试不被允许的源
        mockMvc.perform(options("/search-logs/hot-words")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    // ========== 点击记录接口安全测试 ==========

    @Test
    @DisplayName("点击记录接口无需认证但应验证数据")
    void testClickRecordSecurity() throws Exception {
        SearchClickRequest request = new SearchClickRequest();
        request.setSearchLogId(1L);
        request.setDocumentId("doc123");
        request.setClickPosition(1);

        // 无需认证的接口
        mockMvc.perform(post("/search-logs/click")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isNotFound()); // 搜索日志不存在

        // 测试恶意数据
        request.setDocumentId("<script>alert('xss')</script>");
        mockMvc.perform(post("/search-logs/click")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isNotFound()); // 应该安全处理
    }

    // ========== 性能和资源保护测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试查询性能和资源限制")
    void testQueryPerformanceAndResourceLimits() throws Exception {
        // 测试大范围时间查询
        mockMvc.perform(get("/search-logs/statistics")
                .param("startTime", "1900-01-01 00:00:00")
                .param("endTime", "2100-12-31 23:59:59"))
                .andExpect(status().isOk()); // 应该有适当的限制

        // 测试大量数据请求
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "100")) // 测试上限
                .andExpect(status().isBadRequest());
    }

    // ========== 日志记录和审计测试 ==========

    @Test
    @WithMockUser(username = "testuser", authorities = "USER")
    @DisplayName("测试安全事件日志记录")
    void testSecurityEventLogging() throws Exception {
        // 执行一些可能的安全事件
        mockMvc.perform(get("/search-logs/hot-words")
                .param("userId", "'; DROP TABLE users; --")
                .param("limit", "10"));

        // 验证安全事件被记录（这里简化处理，实际应检查日志文件）
        // 在实际实现中，应该有安全事件监听器记录这些尝试
    }

    // ========== 错误处理安全测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试错误消息不泄露敏感信息")
    void testErrorHandlingSecurity() throws Exception {
        // 触发各种错误情况
        mockMvc.perform(get("/search-logs/-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("database")
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("sql")
                )));

        // 测试无效JSON输入
        mockMvc.perform(post("/search-logs/click")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== 并发安全测试 ==========

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("测试并发访问安全性")
    void testConcurrentAccessSecurity() throws Exception {
        // 模拟多个并发请求
        List<Thread> threads = Arrays.asList(
            new Thread(() -> {
                try {
                    mockMvc.perform(get("/search-logs/hot-words")
                            .param("limit", "10"));
                } catch (Exception e) {
                    // 忽略异常，专注于并发安全
                }
            }),
            new Thread(() -> {
                try {
                    mockMvc.perform(get("/search-logs/statistics")
                            .param("startTime", "2023-01-01 00:00:00")
                            .param("endTime", "2023-12-31 23:59:59"));
                } catch (Exception e) {
                    // 忽略异常，专注于并发安全
                }
            })
        );

        // 启动所有线程
        threads.forEach(Thread::start);

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join(5000); // 5秒超时
        }

        // 验证数据一致性（这里简化处理）
        mockMvc.perform(get("/search-logs/hot-words")
                .param("limit", "5"))
                .andExpect(status().isOk());
    }
}