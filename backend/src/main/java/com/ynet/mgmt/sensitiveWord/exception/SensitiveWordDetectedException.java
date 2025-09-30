package com.ynet.mgmt.sensitiveWord.exception;

import java.util.Collections;
import java.util.List;

/**
 * 敏感词检测异常
 * 当用户查询包含敏感词时抛出此异常
 *
 * @author system
 * @since 1.0.0
 */
public class SensitiveWordDetectedException extends RuntimeException {

    /**
     * 检测到的敏感词列表
     */
    private final List<String> detectedWords;

    /**
     * 原始查询文本
     */
    private final String query;

    /**
     * 构造函数
     *
     * @param message 异常信息
     * @param query 原始查询文本
     * @param detectedWords 检测到的敏感词列表
     */
    public SensitiveWordDetectedException(String message, String query, List<String> detectedWords) {
        super(message);
        this.query = query;
        this.detectedWords = detectedWords != null ? Collections.unmodifiableList(detectedWords) : Collections.emptyList();
    }

    /**
     * 构造函数（默认消息）
     *
     * @param query 原始查询文本
     * @param detectedWords 检测到的敏感词列表
     */
    public SensitiveWordDetectedException(String query, List<String> detectedWords) {
        this("查询内容包含敏感词，无法执行搜索", query, detectedWords);
    }

    /**
     * 获取检测到的敏感词列表
     *
     * @return 敏感词列表
     */
    public List<String> getDetectedWords() {
        return detectedWords;
    }

    /**
     * 获取原始查询文本
     *
     * @return 查询文本
     */
    public String getQuery() {
        return query;
    }

    /**
     * 获取检测到的敏感词数量
     *
     * @return 敏感词数量
     */
    public int getDetectedWordCount() {
        return detectedWords.size();
    }

    @Override
    public String toString() {
        return "SensitiveWordDetectedException{" +
                "message='" + getMessage() + '\'' +
                ", query='" + query + '\'' +
                ", detectedWords=" + detectedWords +
                '}';
    }
}