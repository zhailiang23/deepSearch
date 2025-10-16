package com.ynet.mgmt.queryunderstanding.dto;

import lombok.Data;

import java.util.List;

/**
 * LLM API 响应DTO
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
public class LlmApiResponse {

    /**
     * 响应ID
     */
    private String id;

    /**
     * 对象类型
     */
    private String object;

    /**
     * 创建时间
     */
    private Long created;

    /**
     * 模型
     */
    private String model;

    /**
     * 选择列表
     */
    private List<Choice> choices;

    /**
     * 使用统计
     */
    private Usage usage;

    /**
     * 选择
     */
    @Data
    public static class Choice {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 消息
         */
        private Message message;

        /**
         * 结束原因
         */
        private String finishReason;
    }

    /**
     * 消息
     */
    @Data
    public static class Message {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容
         */
        private String content;
    }

    /**
     * 使用统计
     */
    @Data
    public static class Usage {
        /**
         * 提示token数
         */
        private Integer promptTokens;

        /**
         * 完成token数
         */
        private Integer completionTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}
