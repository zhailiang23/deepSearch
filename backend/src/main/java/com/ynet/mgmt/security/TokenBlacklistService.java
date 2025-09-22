package com.ynet.mgmt.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.ynet.mgmt.config.JwtProperties;

import java.util.concurrent.TimeUnit;

/**
 * 令牌黑名单服务
 *
 * 用于管理被撤销的JWT令牌，防止已注销的令牌被重复使用
 * 使用Redis存储黑名单，设置过期时间自动清理
 */
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final JwtProperties jwtProperties;

    @Autowired
    public TokenBlacklistService(RedisTemplate<String, String> stringRedisTemplate,
                                JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 将令牌添加到黑名单
     *
     * @param token 要加入黑名单的令牌
     * @param expirationTimeInMillis 令牌的原始过期时间(毫秒)
     */
    public void addToBlacklist(String token, long expirationTimeInMillis) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("令牌不能为空");
        }

        String key = BLACKLIST_KEY_PREFIX + token;

        // 计算距离过期还有多长时间
        long currentTime = System.currentTimeMillis();
        long timeToExpiration = expirationTimeInMillis - currentTime;

        // 只有当令牌还没过期时才需要加入黑名单
        if (timeToExpiration > 0) {
            // 在Redis中存储，设置过期时间为令牌剩余时间
            stringRedisTemplate.opsForValue().set(
                key,
                "blacklisted",
                timeToExpiration,
                TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 将令牌添加到黑名单(使用默认过期时间)
     *
     * @param token 要加入黑名单的令牌
     */
    public void addToBlacklist(String token) {
        addToBlacklist(token, System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 检查令牌是否在黑名单中
     *
     * @param token 要检查的令牌
     * @return true 如果令牌在黑名单中，false 否则
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String key = BLACKLIST_KEY_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 从黑名单中移除令牌(主要用于测试)
     *
     * @param token 要移除的令牌
     * @return true 如果成功移除，false 如果令牌不在黑名单中
     */
    public boolean removeFromBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String key = BLACKLIST_KEY_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    /**
     * 清空所有黑名单令牌(主要用于测试)
     */
    public void clearAllBlacklist() {
        stringRedisTemplate.delete(
            stringRedisTemplate.keys(BLACKLIST_KEY_PREFIX + "*")
        );
    }

    /**
     * 获取黑名单中令牌的数量
     *
     * @return 黑名单中令牌的数量
     */
    public long getBlacklistSize() {
        return stringRedisTemplate.keys(BLACKLIST_KEY_PREFIX + "*").size();
    }
}