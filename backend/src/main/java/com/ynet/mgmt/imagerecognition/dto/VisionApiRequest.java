package com.ynet.mgmt.imagerecognition.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 硅基流动 Vision API 请求 DTO
 */
@Schema(description = "Vision API 请求")
public class VisionApiRequest {

    @Schema(description = "模型名称")
    @JsonProperty("model")
    private String model;

    @Schema(description = "消息列表")
    @JsonProperty("messages")
    private List<Message> messages;

    @Schema(description = "温度参数，控制输出随机性")
    @JsonProperty("temperature")
    private Double temperature;

    @Schema(description = "最大生成 token 数")
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @Schema(description = "核采样参数")
    @JsonProperty("top_p")
    private Double topP;

    @Schema(description = "是否启用流式响应")
    @JsonProperty("stream")
    private Boolean stream;

    @Schema(description = "响应格式")
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    public VisionApiRequest() {
    }

    public VisionApiRequest(String model, List<Message> messages, Double temperature, Integer maxTokens, Double topP, Boolean stream) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.stream = stream;
    }

    public VisionApiRequest(String model, List<Message> messages, Double temperature, Integer maxTokens, Double topP, Boolean stream, ResponseFormat responseFormat) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.stream = stream;
        this.responseFormat = responseFormat;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    /**
     * 消息类
     */
    @Schema(description = "消息")
    public static class Message {

        @Schema(description = "角色")
        @JsonProperty("role")
        private String role;

        @Schema(description = "内容列表")
        @JsonProperty("content")
        private List<Content> content;

        public Message() {
        }

        public Message(String role, List<Content> content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Content> getContent() {
            return content;
        }

        public void setContent(List<Content> content) {
            this.content = content;
        }
    }

    /**
     * 内容类
     */
    @Schema(description = "内容")
    public static class Content {

        @Schema(description = "内容类型")
        @JsonProperty("type")
        private String type;

        @Schema(description = "文本内容")
        @JsonProperty("text")
        private String text;

        @Schema(description = "图片 URL")
        @JsonProperty("image_url")
        private ImageUrl imageUrl;

        public Content() {
        }

        public Content(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public Content(String type, ImageUrl imageUrl) {
            this.type = type;
            this.imageUrl = imageUrl;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public ImageUrl getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(ImageUrl imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    /**
     * 图片 URL 类
     */
    @Schema(description = "图片 URL")
    public static class ImageUrl {

        @Schema(description = "图片 URL 或 Base64 编码")
        @JsonProperty("url")
        private String url;

        public ImageUrl() {
        }

        public ImageUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * 响应格式类
     */
    @Schema(description = "响应格式")
    public static class ResponseFormat {

        @Schema(description = "响应格式类型")
        @JsonProperty("type")
        private String type;

        public ResponseFormat() {
        }

        public ResponseFormat(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}