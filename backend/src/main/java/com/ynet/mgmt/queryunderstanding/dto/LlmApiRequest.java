package com.ynet.mgmt.queryunderstanding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM API 请求DTO
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmApiRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大token数
     */
    private Integer maxTokens;

    /**
     * Top-P参数
     */
    private Double topP;

    /**
     * 消息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色: system, user, assistant
         */
        private String role;

        /**
         * 内容
         */
        private String content;
    }
}
