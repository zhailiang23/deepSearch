package com.ynet.mgmt.searchlog.service.impl;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.ynet.mgmt.searchlog.service.ChineseSegmentationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文分词服务实现类
 * 基于jieba-analysis实现中文分词功能
 * 采用单例模式和懒加载优化性能
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
public class ChineseSegmentationServiceImpl implements ChineseSegmentationService {

    private JiebaSegmenter segmenter;
    private volatile boolean serviceAvailable = false;
    private static final String JIEBA_VERSION = "1.0.2";

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing Chinese segmentation service...");
            // 使用搜索引擎模式，适合搜索引擎建立倒排索引的分词
            this.segmenter = new JiebaSegmenter();
            this.serviceAvailable = true;
            log.info("Chinese segmentation service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Chinese segmentation service", e);
            this.serviceAvailable = false;
        }
    }

    @Override
    public List<String> segmentText(String text) {
        // 参数验证
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 服务可用性检查
        if (!serviceAvailable || segmenter == null) {
            log.warn("Segmentation service is not available");
            return Collections.emptyList();
        }

        try {
            // 使用搜索引擎模式进行分词
            List<String> segments = segmenter.sentenceProcess(text.trim());

            // 过滤空白和单字符结果（可选）
            return segments.stream()
                    .filter(segment -> segment != null && segment.trim().length() > 0)
                    .map(String::trim)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during text segmentation for text: {}", text, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<List<String>> segmentTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<String>> results = new ArrayList<>();
        for (String text : texts) {
            results.add(segmentText(text));
        }
        return results;
    }

    @Override
    public boolean isServiceAvailable() {
        return serviceAvailable && segmenter != null;
    }

    @Override
    public String getSegmenterVersion() {
        return "jieba-analysis-" + JIEBA_VERSION;
    }
}