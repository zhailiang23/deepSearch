package com.ynet.mgmt.queryunderstanding.dto;

import lombok.Data;

/**
 * SiliconFlow Embedding API请求
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
public class EmbeddingApiRequest {

    /**
     * 模型名称
     * 例如: "BAAI/bge-large-zh-v1.5"
     */
    private String model;

    /**
     * 输入文本
     */
    private String input;

    /**
     * 编码格式
     * 默认为 "float"
     */
    private String encodingFormat = "float";
}
