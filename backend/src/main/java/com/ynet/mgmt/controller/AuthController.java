package com.ynet.mgmt.controller;

import com.ynet.mgmt.dto.auth.LoginRequest;
import com.ynet.mgmt.dto.auth.LoginResponse;
import com.ynet.mgmt.dto.auth.RefreshTokenRequest;
import com.ynet.mgmt.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(request);
            LoginResponse response = authService.login(loginRequest, ipAddress);

            logger.info("登录成功: 用户={}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("登录失败: 用户={}, 错误={}", loginRequest.getUsername(), e.getMessage());

            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        try {
            LoginResponse response = authService.refreshToken(refreshRequest.getRefreshToken());

            logger.debug("Token刷新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Token刷新失败: 错误={}", e.getMessage());

            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        try {
            String token = extractTokenFromRequest(request);
            String username = authentication != null ? authentication.getName() : null;

            authService.logout(token, username);

            logger.info("登出成功: 用户={}", username);

            Map<String, String> response = new HashMap<>();
            response.put("message", "登出成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("登出失败: 错误={}", e.getMessage());

            // 即使出错也返回成功，避免前端处理复杂
            Map<String, String> response = new HashMap<>();
            response.put("message", "登出成功");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "未认证");
                return ResponseEntity.status(401).body(error);
            }

            LoginResponse.UserInfo userInfo = authService.getUserProfile(authentication.getName());

            logger.debug("获取用户信息成功: 用户={}", authentication.getName());
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("获取用户信息失败: 用户={}, 错误={}",
                    authentication != null ? authentication.getName() : "unknown", e.getMessage());

            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth");
        return ResponseEntity.ok(response);
    }

    /**
     * 从请求中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}