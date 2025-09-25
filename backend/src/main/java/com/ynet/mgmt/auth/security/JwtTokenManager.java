package com.ynet.mgmt.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.ynet.mgmt.config.JwtProperties;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT令牌管理器
 *
 * 统一管理JWT令牌的生成、验证、解析等功能
 * 整合令牌黑名单和刷新令牌服务
 */
@Component
public class JwtTokenManager {

    private final JwtProperties jwtProperties;
    private final TokenBlacklistService blacklistService;
    private final RefreshTokenService refreshTokenService;
    private final Key signingKey;

    @Autowired
    public JwtTokenManager(JwtProperties jwtProperties,
                          TokenBlacklistService blacklistService,
                          RefreshTokenService refreshTokenService) {
        this.jwtProperties = jwtProperties;
        this.blacklistService = blacklistService;
        this.refreshTokenService = refreshTokenService;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * 为用户生成访问令牌
     *
     * @param userDetails 用户详情
     * @return 生成的JWT访问令牌
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 为用户生成访问令牌(使用用户名)
     *
     * @param username 用户名
     * @return 生成的JWT访问令牌
     */
    public String generateAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 为用户生成刷新令牌
     *
     * @param username 用户名
     * @return 生成的刷新令牌
     */
    public String generateRefreshToken(String username) {
        return refreshTokenService.generateRefreshToken(username);
    }

    /**
     * 验证JWT令牌
     *
     * @param token JWT令牌
     * @return true 如果令牌有效，false 否则
     */
    public boolean validateToken(String token) {
        try {
            // 首先检查令牌是否在黑名单中
            if (blacklistService.isBlacklisted(token)) {
                System.out.println("JWT验证失败: token在黑名单中");
                return false;
            }

            // 解析令牌验证签名和过期时间
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);

            System.out.println("JWT验证成功");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("JWT验证失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 令牌对应的用户名，如果无效则返回null
     */
    public String validateRefreshToken(String refreshToken) {
        return refreshTokenService.validateRefreshToken(refreshToken);
    }

    /**
     * 从JWT令牌中提取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从JWT令牌中提取过期时间
     *
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 检查JWT令牌是否已过期
     *
     * @param token JWT令牌
     * @return true 如果已过期，false 否则
     */
    public boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 撤销访问令牌(添加到黑名单)
     *
     * @param token 要撤销的访问令牌
     */
    public void revokeAccessToken(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            blacklistService.addToBlacklist(token, expirationDate.getTime());
        } catch (Exception e) {
            // 如果无法解析令牌，使用默认过期时间
            blacklistService.addToBlacklist(token);
        }
    }

    /**
     * 撤销刷新令牌
     *
     * @param refreshToken 要撤销的刷新令牌
     * @return true 如果成功撤销，false 否则
     */
    public boolean revokeRefreshToken(String refreshToken) {
        return refreshTokenService.revokeRefreshToken(refreshToken);
    }

    /**
     * 撤销用户的所有令牌
     *
     * @param username 用户名
     */
    public void revokeAllUserTokens(String username) {
        refreshTokenService.revokeUserRefreshToken(username);
    }

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌，如果刷新令牌无效则返回null
     */
    public String refreshAccessToken(String refreshToken) {
        String username = validateRefreshToken(refreshToken);
        if (username != null) {
            return generateAccessToken(username);
        }
        return null;
    }

    /**
     * 从令牌中提取特定声明
     *
     * @param token JWT令牌
     * @param claimsResolver 声明解析器
     * @param <T> 声明类型
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中提取所有声明
     *
     * @param token JWT令牌
     * @return 所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * 创建JWT令牌
     *
     * @param claims 声明
     * @param subject 主题(通常是用户名)
     * @param expirationTime 过期时间(毫秒)
     * @return 生成的JWT令牌
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuer(jwtProperties.getIssuer())
            .setAudience(jwtProperties.getAudience())
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }
}