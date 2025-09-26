package com.ynet.mgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * JPA配置类
 * 配置JPA相关功能，包括审计和Repository扫描
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.ynet.mgmt"
})
@EnableJpaAuditing
public class JpaConfig {

    /**
     * 配置JPA审计的当前用户提供者
     * 后续与认证系统集成时将替换为实际的用户信息
     *
     * @return 当前用户提供者
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system"); // 后续与认证系统集成
    }
}