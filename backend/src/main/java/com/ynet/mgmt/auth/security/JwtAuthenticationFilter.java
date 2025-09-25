package com.ynet.mgmt.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * 从HTTP请求中提取JWT令牌，验证令牌有效性，并设置Spring Security上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenManager jwtTokenManager;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenManager jwtTokenManager, UserDetailsService userDetailsService) {
        this.jwtTokenManager = jwtTokenManager;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        System.out.println("=== JWT Filter 被调用 ===");
        System.out.println("请求URI: " + request.getRequestURI());
        System.out.println("请求方法: " + request.getMethod());

        try {
            // 提取JWT令牌
            String token = getTokenFromRequest(request);
            logger.debug("JWT过滤器处理请求: URI={}, Token存在={}", request.getRequestURI(), token != null);

            if (StringUtils.hasText(token) && jwtTokenManager.validateToken(token)) {
                // 从令牌中获取用户名
                String username = jwtTokenManager.getUsernameFromToken(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 加载用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("JWT认证成功: 用户={}", username);
                }
            } else {
                logger.debug("JWT令牌无效或不存在: token存在={}, 有效性={}",
                    token != null, token != null && jwtTokenManager.validateToken(token));
            }
        } catch (Exception e) {
            logger.error("JWT认证过程中发生错误: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求中提取JWT令牌
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}