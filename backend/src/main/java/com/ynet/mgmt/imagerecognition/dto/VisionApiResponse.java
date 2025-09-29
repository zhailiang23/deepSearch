package com.ynet.mgmt.imagerecognition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 硅基流动 Vision API 响应 DTO
 */
@Schema(description = "Vision API 响应")
public class VisionApiResponse {

    @Schema(description = "响应 ID")
    @JsonProperty("id")
    private String id;

    @Schema(description = "对象类型")
    @JsonProperty("object")
    private String object;

    @Schema(description = "创建时间")
    @JsonProperty("created")
    private Long created;

    @Schema(description = "模型名称")
    @JsonProperty("model")
    private String model;

    @Schema(description = "选择列表")
    @JsonProperty("choices")
    private List<Choice> choices;

    @Schema(description = "使用情况")
    @JsonProperty("usage")
    private Usage usage;

    public VisionApiResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * 选择类
     */
    @Schema(description = "选择")
    public static class Choice {

        @Schema(description = "索引")
        @JsonProperty("index")
        private Integer index;

        @Schema(description = "消息")
        @JsonProperty("message")
        private Message message;

        @Schema(description = "完成原因")
        @JsonProperty("finish_reason")
        private String finishReason;

        public Choice() {
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    /**
     * 消息类
     */
    @Schema(description = "消息")
    public static class Message {

        @Schema(description = "角色")
        @JsonProperty("role")
        private String role;

        @Schema(description = "内容")
        @JsonProperty("content")
        private String content;

        public Message() {
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 使用情况类
     */
    @Schema(description = "使用情况")
    public static class Usage {

        @Schema(description = "提示 token 数")
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @Schema(description = "完成 token 数")
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @Schema(description = "总 token 数")
        @JsonProperty("total_tokens")
        private Integer totalTokens;

        public Usage() {
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}