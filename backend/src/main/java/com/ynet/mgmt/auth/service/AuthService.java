package com.ynet.mgmt.auth.service;

import com.ynet.mgmt.auth.dto.LoginRequest;
import com.ynet.mgmt.auth.dto.LoginResponse;
import com.ynet.mgmt.user.entity.User;
import com.ynet.mgmt.user.entity.UserStatus;
import com.ynet.mgmt.user.repository.UserRepository;
import com.ynet.mgmt.auth.security.JwtTokenManager;
import com.ynet.mgmt.auth.security.RefreshTokenService;
import com.ynet.mgmt.auth.security.TokenBlacklistService;
import com.ynet.mgmt.auth.security.IpTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 认证服务
 */
@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenManager jwtTokenManager;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final IpTrackingService ipTrackingService;

    @Autowired
    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenManager jwtTokenManager,
                      RefreshTokenService refreshTokenService,
                      TokenBlacklistService tokenBlacklistService,
                      IpTrackingService ipTrackingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenManager = jwtTokenManager;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.ipTrackingService = ipTrackingService;
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest loginRequest, String ipAddress) {
        logger.info("登录尝试: 用户={}, IP={}", loginRequest.getUsername(), ipAddress);

        // 查找用户
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 检查用户状态
        validateUserStatus(user);

        // 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress);
            throw new RuntimeException("用户名或密码错误");
        }

        // 登录成功处理
        handleSuccessfulLogin(user, ipAddress);

        // 生成Token
        String accessToken = jwtTokenManager.generateAccessToken(user.getUsername());
        String refreshToken = refreshTokenService.generateRefreshToken(user.getUsername());

        // 构建响应
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCustomRole().getCode()
        );

        logger.info("用户登录成功: 用户={}, IP={}", user.getUsername(), ipAddress);
        return new LoginResponse(accessToken, refreshToken, userInfo);
    }

    /**
     * 刷新Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        logger.debug("刷新Token请求");

        // 验证刷新Token
        String username = refreshTokenService.validateRefreshToken(refreshToken);
        if (username == null) {
            throw new RuntimeException("刷新Token无效或已过期");
        }

        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户状态
        validateUserStatus(user);

        // 生成新的访问Token
        String newAccessToken = jwtTokenManager.generateAccessToken(user.getUsername());

        // 构建用户信息
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCustomRole().getCode()
        );

        logger.debug("Token刷新成功: 用户={}", username);
        return new LoginResponse(newAccessToken, refreshToken, userInfo);
    }

    /**
     * 登出
     */
    public void logout(String token, String username) {
        logger.info("用户登出: 用户={}", username);

        try {
            // 将Token加入黑名单
            if (token != null) {
                tokenBlacklistService.addToBlacklist(token);
            }

            // 撤销用户的刷新Token
            if (username != null) {
                refreshTokenService.revokeUserRefreshToken(username);
            }

            logger.info("用户登出成功: 用户={}", username);
        } catch (Exception e) {
            logger.error("登出处理出错: 用户={}, 错误={}", username, e.getMessage());
            // 即使出错也不抛异常，确保登出操作成功
        }
    }

    /**
     * 获取用户信息
     */
    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCustomRole().getCode()
        );
    }

    /**
     * 验证用户状态
     */
    private void validateUserStatus(User user) {
        if (!user.isActive()) {
            if (user.getStatus() == UserStatus.PENDING) {
                throw new RuntimeException("账户未激活");
            } else if (user.getStatus() == UserStatus.DISABLED) {
                throw new RuntimeException("账户已被暂停");
            }
        }

        if (user.isLocked()) {
            throw new RuntimeException("账户已被锁定，请稍后再试");
        }
    }

    /**
     * 处理登录失败
     */
    private void handleFailedLogin(User user, String ipAddress) {
        user.incrementFailedLoginAttempts();

        // 记录失败的IP
        try {
            ipTrackingService.recordLoginAttempt(ipAddress, user.getUsername(), false);
        } catch (Exception e) {
            logger.debug("记录失败IP时出错: {}", e.getMessage());
        }

        // 检查是否需要锁定账户
        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.lockAccount(ACCOUNT_LOCK_DURATION);
            logger.warn("账户因登录失败次数过多被锁定: 用户={}, IP={}, 失败次数={}",
                    user.getUsername(), ipAddress, user.getFailedLoginAttempts());
        }

        userRepository.save(user);

        logger.warn("登录失败: 用户={}, IP={}, 失败次数={}",
                user.getUsername(), ipAddress, user.getFailedLoginAttempts());
    }

    /**
     * 处理登录成功
     */
    private void handleSuccessfulLogin(User user, String ipAddress) {
        // 更新登录信息
        user.updateLastLogin(ipAddress);

        // 记录成功的IP
        try {
            ipTrackingService.recordLoginAttempt(ipAddress, user.getUsername(), true);
        } catch (Exception e) {
            logger.debug("记录成功IP时出错: {}", e.getMessage());
        }

        userRepository.save(user);
    }
}