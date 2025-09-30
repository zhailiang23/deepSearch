package com.ynet.mgmt.sensitiveWord.cache;

import com.ynet.mgmt.sensitiveWord.detector.SensitiveWordDetector;
import com.ynet.mgmt.sensitiveWord.entity.SensitiveWord;
import com.ynet.mgmt.sensitiveWord.repository.SensitiveWordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 敏感词缓存管理器
 * 负责敏感词检测器的缓存和刷新
 *
 * 特点：
 * - 启动时自动加载
 * - 定时刷新（可配置）
 * - 支持手动刷新
 * - 线程安全
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class SensitiveWordCache {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordCache.class);

    private final SensitiveWordRepository sensitiveWordRepository;

    /**
     * 使用 AtomicReference 确保线程安全的读写
     */
    private final AtomicReference<SensitiveWordDetector> detectorRef = new AtomicReference<>();

    /**
     * 缓存状态
     */
    private volatile boolean initialized = false;

    @Autowired
    public SensitiveWordCache(SensitiveWordRepository sensitiveWordRepository) {
        this.sensitiveWordRepository = sensitiveWordRepository;
    }

    /**
     * 应用启动时初始化缓存
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化敏感词缓存...");
        try {
            refresh();
            initialized = true;
            log.info("敏感词缓存初始化完成");
        } catch (Exception e) {
            log.error("敏感词缓存初始化失败", e);
            // 即使失败也创建一个空的检测器，避免空指针
            detectorRef.set(new SensitiveWordDetector(List.of()));
            initialized = true;
        }
    }

    /**
     * 定时刷新缓存（每5分钟执行一次）
     * fixedRate: 固定频率，单位毫秒
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void scheduledRefresh() {
        if (!initialized) {
            log.warn("敏感词缓存未初始化，跳过定时刷新");
            return;
        }

        log.debug("开始定时刷新敏感词缓存...");
        try {
            refresh();
            log.debug("敏感词缓存定时刷新完成");
        } catch (Exception e) {
            log.error("敏感词缓存定时刷新失败", e);
        }
    }

    /**
     * 手动刷新缓存
     * 在敏感词增删改时调用
     */
    public synchronized void refresh() {
        log.info("开始刷新敏感词缓存...");
        long startTime = System.currentTimeMillis();

        try {
            // 从数据库加载所有启用的敏感词
            List<SensitiveWord> enabledWords = sensitiveWordRepository.findByEnabled(true, org.springframework.data.domain.Pageable.unpaged())
                    .getContent();

            // 提取敏感词名称列表
            List<String> wordNames = enabledWords.stream()
                    .map(SensitiveWord::getName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .collect(Collectors.toList());

            // 创建新的检测器
            SensitiveWordDetector newDetector = new SensitiveWordDetector(wordNames);

            // 原子性地更新检测器
            detectorRef.set(newDetector);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("敏感词缓存刷新完成，加载了 {} 个启用的敏感词，耗时 {}ms",
                    wordNames.size(), elapsed);

        } catch (Exception e) {
            log.error("刷新敏感词缓存失败", e);
            throw new RuntimeException("刷新敏感词缓存失败", e);
        }
    }

    /**
     * 获取当前的检测器
     *
     * @return 敏感词检测器
     */
    public SensitiveWordDetector getDetector() {
        SensitiveWordDetector detector = detectorRef.get();
        if (detector == null) {
            log.warn("敏感词检测器未初始化，返回空检测器");
            // 返回空检测器，避免空指针
            return new SensitiveWordDetector(List.of());
        }
        return detector;
    }

    /**
     * 检查缓存是否已初始化
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取缓存中的敏感词数量
     *
     * @return 敏感词数量
     */
    public int getWordCount() {
        SensitiveWordDetector detector = detectorRef.get();
        return detector != null ? detector.getWordCount() : 0;
    }

    /**
     * 获取缓存状态信息
     *
     * @return 状态信息
     */
    public CacheStatus getStatus() {
        return new CacheStatus(
                initialized,
                getWordCount(),
                detectorRef.get() != null
        );
    }

    /**
     * 缓存状态
     */
    public static class CacheStatus {
        private final boolean initialized;
        private final int wordCount;
        private final boolean detectorAvailable;

        public CacheStatus(boolean initialized, int wordCount, boolean detectorAvailable) {
            this.initialized = initialized;
            this.wordCount = wordCount;
            this.detectorAvailable = detectorAvailable;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public int getWordCount() {
            return wordCount;
        }

        public boolean isDetectorAvailable() {
            return detectorAvailable;
        }

        @Override
        public String toString() {
            return "CacheStatus{" +
                    "initialized=" + initialized +
                    ", wordCount=" + wordCount +
                    ", detectorAvailable=" + detectorAvailable +
                    '}';
        }
    }
}