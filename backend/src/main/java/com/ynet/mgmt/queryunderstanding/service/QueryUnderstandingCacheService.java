package com.ynet.mgmt.queryunderstanding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 查询理解缓存服务
 * 使用Redis缓存查询理解结果,提升性能
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Service
public class QueryUnderstandingCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 缓存键前缀
     */
    private static final String CACHE_PREFIX = "query_understanding:";
    private static final String LLM_INTENT_PREFIX = "llm:intent:";
    private static final String LLM_ENTITY_PREFIX = "llm:entity:";
    private static final String LLM_REWRITE_PREFIX = "llm:rewrite:";
    private static final String SYNONYM_PREFIX = "synonym:";

    /**
     * 默认缓存过期时间
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final Duration LLM_TTL = Duration.ofHours(1);
    private static final Duration SYNONYM_TTL = Duration.ofHours(24);

    /**
     * 缓存查询理解结果
     *
     * @param query   原始查询
     * @param context 查询上下文
     */
    public void cacheQueryContext(String query, QueryContext context) {
        try {
            String key = CACHE_PREFIX + query;

            // 序列化QueryContext为JSON
            String jsonValue = objectMapper.writeValueAsString(context);

            redisTemplate.opsForValue().set(key, jsonValue, DEFAULT_TTL.toMillis(), TimeUnit.MILLISECONDS);

            log.debug("缓存查询理解结果: query={}, ttl={}min", query, DEFAULT_TTL.toMinutes());
        } catch (Exception e) {
            log.warn("缓存查询理解结果失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的查询理解结果
     *
     * @param query 原始查询
     * @return 查询上下文,如果不存在则返回null
     */
    public QueryContext getCachedQueryContext(String query) {
        try {
            String key = CACHE_PREFIX + query;
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                // 反序列化JSON为QueryContext
                QueryContext context = objectMapper.readValue(value.toString(), QueryContext.class);
                log.debug("命中查询理解缓存: query={}", query);
                return context;
            }
        } catch (Exception e) {
            log.warn("获取缓存的查询理解结果失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 缓存意图识别结果
     *
     * @param query  查询文本
     * @param intent 意图类型(JSON字符串)
     */
    public void cacheLlmIntent(String query, String intent) {
        try {
            String key = CACHE_PREFIX + LLM_INTENT_PREFIX + query;
            redisTemplate.opsForValue().set(key, intent, LLM_TTL.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("缓存意图识别结果: query={}", query);
        } catch (Exception e) {
            log.warn("缓存意图识别结果失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的意图识别结果
     *
     * @param query 查询文本
     * @return 意图类型(JSON字符串),如果不存在则返回null
     */
    public String getCachedLlmIntent(String query) {
        try {
            String key = CACHE_PREFIX + LLM_INTENT_PREFIX + query;
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                log.debug("命中意图识别缓存: query={}", query);
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("获取缓存的意图识别结果失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 缓存实体抽取结果
     *
     * @param query    查询文本
     * @param entities 实体列表(JSON字符串)
     */
    public void cacheLlmEntities(String query, String entities) {
        try {
            String key = CACHE_PREFIX + LLM_ENTITY_PREFIX + query;
            redisTemplate.opsForValue().set(key, entities, LLM_TTL.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("缓存实体抽取结果: query={}", query);
        } catch (Exception e) {
            log.warn("缓存实体抽取结果失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的实体抽取结果
     *
     * @param query 查询文本
     * @return 实体列表(JSON字符串),如果不存在则返回null
     */
    public String getCachedLlmEntities(String query) {
        try {
            String key = CACHE_PREFIX + LLM_ENTITY_PREFIX + query;
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                log.debug("命中实体抽取缓存: query={}", query);
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("获取缓存的实体抽取结果失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 缓存查询重写结果
     *
     * @param originalQuery  原始查询
     * @param rewrittenQuery 重写后的查询
     */
    public void cacheLlmRewrite(String originalQuery, String rewrittenQuery) {
        try {
            String key = CACHE_PREFIX + LLM_REWRITE_PREFIX + originalQuery;
            redisTemplate.opsForValue().set(key, rewrittenQuery, LLM_TTL.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("缓存查询重写结果: originalQuery={}", originalQuery);
        } catch (Exception e) {
            log.warn("缓存查询重写结果失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的查询重写结果
     *
     * @param originalQuery 原始查询
     * @return 重写后的查询,如果不存在则返回null
     */
    public String getCachedLlmRewrite(String originalQuery) {
        try {
            String key = CACHE_PREFIX + LLM_REWRITE_PREFIX + originalQuery;
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                log.debug("命中查询重写缓存: originalQuery={}", originalQuery);
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("获取缓存的查询重写结果失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 缓存同义词
     *
     * @param word     词语
     * @param synonyms 同义词列表(JSON字符串)
     */
    public void cacheSynonyms(String word, String synonyms) {
        try {
            String key = CACHE_PREFIX + SYNONYM_PREFIX + word;
            redisTemplate.opsForValue().set(key, synonyms, SYNONYM_TTL.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("缓存同义词: word={}", word);
        } catch (Exception e) {
            log.warn("缓存同义词失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存的同义词
     *
     * @param word 词语
     * @return 同义词列表(JSON字符串),如果不存在则返回null
     */
    public String getCachedSynonyms(String word) {
        try {
            String key = CACHE_PREFIX + SYNONYM_PREFIX + word;
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                log.debug("命中同义词缓存: word={}", word);
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("获取缓存的同义词失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 清除指定查询的缓存
     *
     * @param query 查询文本
     */
    public void evictQueryCache(String query) {
        try {
            String key = CACHE_PREFIX + query;
            redisTemplate.delete(key);
            log.debug("清除查询缓存: query={}", query);
        } catch (Exception e) {
            log.warn("清除查询缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除所有查询理解缓存
     */
    public void evictAll() {
        try {
            // 使用scan命令查找所有匹配的key并删除
            redisTemplate.keys(CACHE_PREFIX + "*").forEach(key -> {
                redisTemplate.delete(key);
            });
            log.info("已清除所有查询理解缓存");
        } catch (Exception e) {
            log.warn("清除所有缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存数量
     */
    public long getCacheCount() {
        try {
            return redisTemplate.keys(CACHE_PREFIX + "*").size();
        } catch (Exception e) {
            log.warn("获取缓存统计信息失败: {}", e.getMessage());
            return 0;
        }
    }
}
