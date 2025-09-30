package com.ynet.mgmt.sensitiveWord.service;

import com.ynet.mgmt.sensitiveWord.cache.SensitiveWordCache;
import com.ynet.mgmt.sensitiveWord.detector.SensitiveWordDetector;
import com.ynet.mgmt.sensitiveWord.exception.SensitiveWordDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 敏感词检测服务
 * 提供敏感词检测的业务逻辑
 *
 * @author system
 * @since 1.0.0
 */
@Service
public class SensitiveWordCheckService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordCheckService.class);

    private final SensitiveWordCache sensitiveWordCache;

    /**
     * 是否启用敏感词检测
     */
    @Value("${sensitive-word.check.enabled:true}")
    private boolean checkEnabled;

    public SensitiveWordCheckService(SensitiveWordCache sensitiveWordCache) {
        this.sensitiveWordCache = sensitiveWordCache;
    }

    /**
     * 检测文本并在包含敏感词时抛出异常
     *
     * @param text 待检测文本
     * @throws SensitiveWordDetectedException 当检测到敏感词时
     */
    public void checkAndThrow(String text) {
        if (!checkEnabled) {
            log.debug("敏感词检测已禁用");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }

        SensitiveWordDetector.DetectionResult result = check(text);
        if (!result.isPassed()) {
            log.warn("检测到敏感词 - 查询: {}, 敏感词: {}", text, result.getDetectedWords());
            throw new SensitiveWordDetectedException(text, result.getDetectedWords());
        }
    }

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return 检测结果
     */
    public SensitiveWordDetector.DetectionResult check(String text) {
        if (!checkEnabled) {
            return SensitiveWordDetector.DetectionResult.pass();
        }

        if (text == null || text.trim().isEmpty()) {
            return SensitiveWordDetector.DetectionResult.pass();
        }

        try {
            SensitiveWordDetector detector = sensitiveWordCache.getDetector();
            SensitiveWordDetector.DetectionResult result = detector.detect(text);

            if (result.hasDetectedWords()) {
                log.debug("检测到敏感词 - 查询: {}, 敏感词: {}", text, result.getDetectedWords());
            }

            return result;
        } catch (Exception e) {
            log.error("敏感词检测失败", e);
            // 检测失败时默认放行，避免影响搜索功能
            return SensitiveWordDetector.DetectionResult.pass();
        }
    }

    /**
     * 检查文本是否包含敏感词（简化版）
     *
     * @param text 待检测文本
     * @return 是否包含敏感词
     */
    public boolean contains(String text) {
        return !check(text).isPassed();
    }

    /**
     * 检查敏感词检测功能是否启用
     *
     * @return 是否启用
     */
    public boolean isCheckEnabled() {
        return checkEnabled;
    }

    /**
     * 获取缓存状态
     *
     * @return 缓存状态
     */
    public SensitiveWordCache.CacheStatus getCacheStatus() {
        return sensitiveWordCache.getStatus();
    }

    /**
     * 手动刷新缓存
     */
    public void refreshCache() {
        sensitiveWordCache.refresh();
    }
}