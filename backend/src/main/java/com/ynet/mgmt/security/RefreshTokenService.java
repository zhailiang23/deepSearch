package com.ynet.mgmt.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.ynet.mgmt.config.JwtProperties;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌服务
 *
 * 用于管理JWT刷新令牌的生成、存储、验证和撤销
 * 使用Redis存储刷新令牌，确保分布式环境下的一致性
 */
@Service
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "jwt:refresh:";
    private static final String USER_REFRESH_TOKEN_KEY_PREFIX = "jwt:user_refresh:";
    private static final int REFRESH_TOKEN_LENGTH = 32;

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom;

    @Autowired
    public RefreshTokenService(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
                              JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
        this.secureRandom = new SecureRandom();
    }

    /**
     * 为用户生成刷新令牌
     *
     * @param username 用户名
     * @return 生成的刷新令牌
     */
    public String generateRefreshToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 先撤销用户之前的刷新令牌
        revokeUserRefreshToken(username);

        // 生成新的刷新令牌
        String refreshToken = generateRandomToken();
        long expirationTime = System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration();

        // 存储刷新令牌信息
        String tokenKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
        String userTokenKey = USER_REFRESH_TOKEN_KEY_PREFIX + username;

        // 存储令牌到用户的映射
        stringRedisTemplate.opsForValue().set(
            tokenKey,
            username + ":" + expirationTime,
            jwtProperties.getRefreshTokenExpiration(),
            TimeUnit.MILLISECONDS
        );

        // 存储用户到令牌的映射
        stringRedisTemplate.opsForValue().set(
            userTokenKey,
            refreshToken,
            jwtProperties.getRefreshTokenExpiration(),
            TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    /**
     * 验证刷新令牌
     *
     * @param refreshToken 要验证的刷新令牌
     * @return 令牌对应的用户名，如果令牌无效则返回null
     */
    public String validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return null;
        }

        String tokenKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
        String tokenData = stringRedisTemplate.opsForValue().get(tokenKey);

        if (tokenData == null) {
            return null; // 令牌不存在或已过期
        }

        String[] parts = tokenData.split(":");
        if (parts.length != 2) {
            return null; // 数据格式错误
        }

        String username = parts[0];
        long expirationTime = Long.parseLong(parts[1]);

        if (System.currentTimeMillis() > expirationTime) {
            // 令牌已过期，清理数据
            revokeRefreshToken(refreshToken);
            return null;
        }

        return username;
    }

    /**
     * 撤销指定的刷新令牌
     *
     * @param refreshToken 要撤销的刷新令牌
     * @return true 如果成功撤销，false 如果令牌不存在
     */
    public boolean revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return false;
        }

        String tokenKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
        String tokenData = stringRedisTemplate.opsForValue().get(tokenKey);

        if (tokenData != null) {
            String[] parts = tokenData.split(":");
            if (parts.length == 2) {
                String username = parts[0];
                String userTokenKey = USER_REFRESH_TOKEN_KEY_PREFIX + username;
                stringRedisTemplate.delete(userTokenKey);
            }
        }

        return Boolean.TRUE.equals(stringRedisTemplate.delete(tokenKey));
    }

    /**
     * 撤销用户的刷新令牌
     *
     * @param username 用户名
     * @return true 如果成功撤销，false 如果用户没有有效的刷新令牌
     */
    public boolean revokeUserRefreshToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String userTokenKey = USER_REFRESH_TOKEN_KEY_PREFIX + username;
        String refreshToken = stringRedisTemplate.opsForValue().get(userTokenKey);

        if (refreshToken != null) {
            String tokenKey = REFRESH_TOKEN_KEY_PREFIX + refreshToken;
            stringRedisTemplate.delete(tokenKey);
            return Boolean.TRUE.equals(stringRedisTemplate.delete(userTokenKey));
        }

        return false;
    }

    /**
     * 检查用户是否有有效的刷新令牌
     *
     * @param username 用户名
     * @return true 如果用户有有效的刷新令牌，false 否则
     */
    public boolean hasValidRefreshToken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String userTokenKey = USER_REFRESH_TOKEN_KEY_PREFIX + username;
        String refreshToken = stringRedisTemplate.opsForValue().get(userTokenKey);

        return refreshToken != null && validateRefreshToken(refreshToken) != null;
    }

    /**
     * 清空所有刷新令牌(主要用于测试)
     */
    public void clearAllRefreshTokens() {
        stringRedisTemplate.delete(
            stringRedisTemplate.keys(REFRESH_TOKEN_KEY_PREFIX + "*")
        );
        stringRedisTemplate.delete(
            stringRedisTemplate.keys(USER_REFRESH_TOKEN_KEY_PREFIX + "*")
        );
    }

    /**
     * 获取活跃刷新令牌的数量
     *
     * @return 活跃刷新令牌的数量
     */
    public long getActiveRefreshTokenCount() {
        return stringRedisTemplate.keys(REFRESH_TOKEN_KEY_PREFIX + "*").size();
    }

    /**
     * 生成随机令牌
     *
     * @return Base64编码的随机令牌
     */
    private String generateRandomToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}