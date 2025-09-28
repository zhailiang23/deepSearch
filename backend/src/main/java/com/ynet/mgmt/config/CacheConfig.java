package com.ynet.mgmt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置类
 * 配置多级缓存策略，优化搜索日志统计和热词查询性能
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.default-ttl:PT30M}")
    private Duration defaultTtl;

    @Value("${app.cache.hot-words-ttl:PT1H}")
    private Duration hotWordsTtl;

    @Value("${app.cache.statistics-ttl:PT15M}")
    private Duration statisticsTtl;

    @Value("${app.cache.search-spaces-ttl:PT2H}")
    private Duration searchSpacesTtl;

    // 缓存名称常量
    public static final String HOT_WORDS_CACHE = "hotWords";
    public static final String STATISTICS_CACHE = "statistics";
    public static final String SEARCH_SPACES_CACHE = "searchSpaces";
    public static final String TOP_QUERIES_CACHE = "topQueries";
    public static final String USER_BEHAVIOR_CACHE = "userBehavior";

    /**
     * 配置Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存空值

        // 特定缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 热词缓存配置 - 缓存时间较长，因为热词变化相对较慢
        cacheConfigurations.put(HOT_WORDS_CACHE,
                defaultCacheConfig.entryTtl(hotWordsTtl));

        // 统计数据缓存配置 - 缓存时间较短，需要保证数据相对实时
        cacheConfigurations.put(STATISTICS_CACHE,
                defaultCacheConfig.entryTtl(statisticsTtl));

        // 搜索空间缓存配置 - 缓存时间较长，搜索空间配置变化不频繁
        cacheConfigurations.put(SEARCH_SPACES_CACHE,
                defaultCacheConfig.entryTtl(searchSpacesTtl));

        // 热门查询缓存配置 - 中等缓存时间
        cacheConfigurations.put(TOP_QUERIES_CACHE,
                defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

        // 用户行为缓存配置 - 短期缓存
        cacheConfigurations.put(USER_BEHAVIOR_CACHE,
                defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 缓存键生成器 - 用于生成自定义缓存键
     */
    @Bean("customKeyGenerator")
    public org.springframework.cache.interceptor.KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(":");
            key.append(method.getName()).append(":");

            for (Object param : params) {
                if (param != null) {
                    key.append(param.toString()).append(":");
                }
            }

            // 移除最后一个冒号
            if (key.length() > 0 && key.charAt(key.length() - 1) == ':') {
                key.deleteCharAt(key.length() - 1);
            }

            return key.toString();
        };
    }
}