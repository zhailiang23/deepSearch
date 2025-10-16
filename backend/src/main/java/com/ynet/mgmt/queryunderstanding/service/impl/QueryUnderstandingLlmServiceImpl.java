package com.ynet.mgmt.queryunderstanding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.queryunderstanding.config.QueryUnderstandingLlmProperties;
import com.ynet.mgmt.queryunderstanding.context.Entity;
import com.ynet.mgmt.queryunderstanding.context.EntityType;
import com.ynet.mgmt.queryunderstanding.context.IntentType;
import com.ynet.mgmt.queryunderstanding.dto.LlmApiRequest;
import com.ynet.mgmt.queryunderstanding.dto.LlmApiResponse;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingCacheService;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingLlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询理解LLM服务实现
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Service
public class QueryUnderstandingLlmServiceImpl implements QueryUnderstandingLlmService {

    private final QueryUnderstandingLlmProperties properties;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private QueryUnderstandingCacheService cacheService;

    /**
     * 意图识别提示词
     */
    private static final String INTENT_RECOGNITION_PROMPT =
        "你是银行手机APP的查询意图分析助手。请分析用户的查询意图,并只返回以下类型之一:\n" +
        "- QUERY: 用户想要查询或搜索银行服务、产品、功能等信息\n" +
        "- COMMAND: 用户想要执行银行业务操作(如转账、缴费、开户等)\n" +
        "- QUESTION: 用户在询问银行相关问题,需要解释性回答\n\n" +
        "银行业务场景包括:转账汇款、理财投资、信用卡、贷款、生活缴费、账户查询、网点服务等。\n\n" +
        "用户查询: {query}\n\n" +
        "请只返回意图类型(QUERY/COMMAND/QUESTION),不要有其他内容。";

    /**
     * 实体抽取提示词
     */
    private static final String ENTITY_EXTRACTION_PROMPT =
        "你是银行手机APP的实体识别助手。请从用户查询中识别银行业务相关实体:\n" +
        "- ACCOUNT_TYPE: 账户类型(储蓄卡、信用卡、理财账户、工资卡等)\n" +
        "- TRANSACTION_TYPE: 交易类型(转账、缴费、充值、提现、还款等)\n" +
        "- SERVICE: 服务类型(网点、ATM、在线客服、预约服务等)\n" +
        "- PRODUCT: 金融产品(贷款、理财、基金、保险、定期存款等)\n" +
        "- MONEY: 金额(如:1000元、5万)\n" +
        "- DATE: 日期(如:今天、本月、2024年1月)\n" +
        "- TIME: 时间(如:上午、工作时间)\n" +
        "- LOCATION: 地点(网点位置、城市、地区)\n" +
        "- ORGANIZATION: 机构(收款单位、缴费单位)\n\n" +
        "用户查询: {query}\n\n" +
        "请以JSON格式返回实体列表,格式: [{\"type\":\"ACCOUNT_TYPE\",\"text\":\"信用卡\",\"value\":\"信用卡\"}]\n" +
        "如果没有识别到实体,请返回空数组: []";

    /**
     * 查询重写提示词
     */
    private static final String QUERY_REWRITE_PROMPT =
        "你是银行手机APP的查询优化助手。基于用户意图和识别的实体,生成更精确的银行业务搜索查询。\n\n" +
        "优化原则:\n" +
        "1. 补充银行业务术语(如:将'还钱'改为'信用卡还款')\n" +
        "2. 明确业务场景(如:将'转账'改为'个人转账汇款')\n" +
        "3. 保留关键实体信息(金额、日期、账户类型等)\n" +
        "4. 使用银行标准服务名称(如:'理财产品'而非'赚钱')\n\n" +
        "原始查询: {query}\n" +
        "意图类型: {intent}\n" +
        "识别实体: {entities}\n\n" +
        "重要规则:\n" +
        "- 如果原始查询已经足够清晰明确,直接返回原查询\n" +
        "- 如果查询含义不明确或无法优化,直接返回原查询\n" +
        "- 只返回优化后的查询文本,不要返回任何解释、说明或提示信息\n" +
        "- 不要返回类似'请提供更多信息'、'查询不明确'等提示文字\n" +
        "- 返回的查询应该简短精炼,不超过30个字";

    public QueryUnderstandingLlmServiceImpl(QueryUnderstandingLlmProperties properties,
                                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            // 使用HttpComponents连接池提升性能 (如果可用)
            try {
                Class.forName("org.apache.http.client.HttpClient");
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(properties.getSiliconflow().getTimeout().intValue());
                factory.setConnectionRequestTimeout(properties.getSiliconflow().getTimeout().intValue());
                this.restTemplate = new RestTemplate(factory);
                log.info("使用HttpComponents连接池配置RestTemplate");
            } catch (Throwable e) {
                // 如果HttpComponents不可用，回退到SimpleClientHttpRequestFactory
                log.info("HttpComponents不可用({}), 使用SimpleClientHttpRequestFactory", e.getClass().getSimpleName());
                org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(properties.getSiliconflow().getTimeout().intValue());
                factory.setReadTimeout(properties.getSiliconflow().getTimeout().intValue());
                this.restTemplate = new RestTemplate(factory);
            }
        } catch (Exception e) {
            log.warn("无法配置RestTemplate超时，将使用默认RestTemplate", e);
            this.restTemplate = new RestTemplate();
        }

        log.info("初始化查询理解LLM服务: enabled={}, model={}",
            properties.getEnabled(), properties.getSiliconflow().getModel());
    }

    @Override
    public IntentType recognizeIntent(String query) {
        if (!isServiceAvailable() || !StringUtils.hasText(query)) {
            return IntentType.QUERY; // 默认为查询意图
        }

        // 尝试从缓存获取
        if (cacheService != null) {
            String cachedIntent = cacheService.getCachedLlmIntent(query);
            if (cachedIntent != null) {
                try {
                    // 从JSON中提取intent值
                    if (cachedIntent.contains("\"intent\"")) {
                        String intentValue = cachedIntent.split("\"intent\"\\s*:\\s*\"")[1].split("\"")[0];
                        return IntentType.valueOf(intentValue);
                    }
                } catch (Exception e) {
                    log.warn("解析缓存的意图失败: {}", e.getMessage());
                }
            }
        }

        try {
            String prompt = INTENT_RECOGNITION_PROMPT.replace("{query}", query);
            String response = callLlm(prompt);

            if (StringUtils.hasText(response)) {
                response = response.trim().toUpperCase();
                try {
                    IntentType intent = IntentType.valueOf(response);

                    // 缓存结果
                    if (cacheService != null) {
                        String cacheValue = String.format("{\"intent\":\"%s\"}", intent.name());
                        cacheService.cacheLlmIntent(query, cacheValue);
                    }

                    return intent;
                } catch (IllegalArgumentException e) {
                    log.warn("无法识别意图类型: {}, 使用默认QUERY", response);
                }
            }
        } catch (Exception e) {
            log.warn("LLM意图识别失败: {}", e.getMessage());
        }

        return IntentType.QUERY;
    }

    @Override
    public List<Entity> extractEntities(String query) {
        List<Entity> entities = new ArrayList<>();

        if (!isServiceAvailable() || !StringUtils.hasText(query)) {
            return entities;
        }

        // 尝试从缓存获取
        if (cacheService != null) {
            String cachedEntities = cacheService.getCachedLlmEntities(query);
            if (cachedEntities != null) {
                try {
                    Entity[] entityArray = objectMapper.readValue(cachedEntities, Entity[].class);
                    entities.addAll(Arrays.asList(entityArray));
                    log.debug("从缓存获取到 {} 个实体", entities.size());
                    return entities;
                } catch (Exception e) {
                    log.warn("解析缓存的实体失败: {}", e.getMessage());
                }
            }
        }

        try {
            String prompt = ENTITY_EXTRACTION_PROMPT.replace("{query}", query);
            String response = callLlm(prompt);

            if (StringUtils.hasText(response)) {
                // 尝试解析JSON
                response = extractJsonFromResponse(response);
                Entity[] entityArray = objectMapper.readValue(response, Entity[].class);
                entities.addAll(Arrays.asList(entityArray));

                log.debug("从查询 \"{}\" 中提取到 {} 个实体", query, entities.size());

                // 缓存结果
                if (cacheService != null) {
                    cacheService.cacheLlmEntities(query, response);
                }
            }
        } catch (Exception e) {
            log.warn("LLM实体抽取失败: {}", e.getMessage());
        }

        return entities;
    }

    @Override
    public String rewriteQuery(String query, IntentType intent, List<Entity> entities) {
        if (!isServiceAvailable() || !StringUtils.hasText(query)) {
            return query;
        }

        // 尝试从缓存获取
        if (cacheService != null) {
            String cachedRewrite = cacheService.getCachedLlmRewrite(query);
            if (cachedRewrite != null) {
                log.debug("从缓存获取重写查询: \"{}\" -> \"{}\"", query, cachedRewrite);
                return cachedRewrite;
            }
        }

        try {
            String entitiesStr = entities.isEmpty() ? "无" : entities.toString();
            String prompt = QUERY_REWRITE_PROMPT
                .replace("{query}", query)
                .replace("{intent}", intent.toString())
                .replace("{entities}", entitiesStr);

            String rewrittenQuery = callLlm(prompt);

            if (StringUtils.hasText(rewrittenQuery)) {
                rewrittenQuery = rewrittenQuery.trim();

                // 验证返回的是否是有效的查询重写
                if (!isValidQueryRewrite(rewrittenQuery)) {
                    log.debug("LLM返回的不是有效的查询重写,保持原查询: \"{}\"", rewrittenQuery);
                    return query;
                }

                log.debug("查询重写: \"{}\" -> \"{}\"", query, rewrittenQuery);

                // 缓存结果
                if (cacheService != null) {
                    cacheService.cacheLlmRewrite(query, rewrittenQuery);
                }

                return rewrittenQuery;
            }
        } catch (Exception e) {
            log.warn("LLM查询重写失败: {}", e.getMessage());
        }

        return query;
    }

    @Override
    public boolean isServiceAvailable() {
        return properties.getEnabled() != null && properties.getEnabled()
            && StringUtils.hasText(properties.getSiliconflow().getApiKey());
    }

    /**
     * 调用LLM API
     *
     * @param prompt 提示词
     * @return LLM响应文本
     */
    private String callLlm(String prompt) throws Exception {
        // 构建请求
        LlmApiRequest request = new LlmApiRequest();
        request.setModel(properties.getSiliconflow().getModel());
        request.setTemperature(properties.getSiliconflow().getTemperature());
        request.setMaxTokens(properties.getSiliconflow().getMaxTokens());
        request.setTopP(properties.getSiliconflow().getTopP());

        // 添加消息
        List<LlmApiRequest.Message> messages = new ArrayList<>();
        messages.add(new LlmApiRequest.Message("user", prompt));
        request.setMessages(messages);

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getSiliconflow().getApiKey());

        // 发送请求
        HttpEntity<LlmApiRequest> requestEntity = new HttpEntity<>(request, headers);
        ResponseEntity<LlmApiResponse> responseEntity = restTemplate.exchange(
            properties.getSiliconflow().getApiUrl(),
            HttpMethod.POST,
            requestEntity,
            LlmApiResponse.class
        );

        LlmApiResponse response = responseEntity.getBody();
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("LLM API 返回空响应");
        }

        LlmApiResponse.Choice choice = response.getChoices().get(0);
        if (choice.getMessage() == null || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw new RuntimeException("LLM API 响应中没有内容");
        }

        return choice.getMessage().getContent();
    }

    /**
     * 从响应中提取JSON内容
     * 处理LLM可能返回markdown代码块的情况
     *
     * @param response 原始响应
     * @return 提取的JSON字符串
     */
    private String extractJsonFromResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return "[]";
        }

        // 移除markdown代码块标记
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        response = response.trim();

        // 如果不是JSON格式,返回空数组
        if (!response.startsWith("[") && !response.startsWith("{")) {
            return "[]";
        }

        return response;
    }

    /**
     * 验证LLM返回的是否是有效的查询重写
     * 排除提示性文字、说明文字等非查询内容
     *
     * @param rewrittenQuery LLM返回的文本
     * @return true表示是有效的查询重写
     */
    private boolean isValidQueryRewrite(String rewrittenQuery) {
        if (!StringUtils.hasText(rewrittenQuery)) {
            return false;
        }

        // 检查是否包含提示性关键词
        String[] invalidKeywords = {
            "请提供", "请输入", "请说明", "请明确",
            "不明确", "无法", "不能", "无效",
            "抱歉", "对不起", "很抱歉",
            "查询信息", "具体业务", "关键信息",
            "需要更多", "缺少", "不足"
        };

        String lowerQuery = rewrittenQuery.toLowerCase();
        for (String keyword : invalidKeywords) {
            if (lowerQuery.contains(keyword.toLowerCase())) {
                return false;
            }
        }

        // 检查长度是否合理(超过50个字符可能是说明文字)
        if (rewrittenQuery.length() > 50) {
            return false;
        }

        // 检查是否包含句号、问号等结束符号(说明文字通常有标点)
        if (rewrittenQuery.contains("。") || rewrittenQuery.contains("?") ||
            rewrittenQuery.contains("!") || rewrittenQuery.contains("?")) {
            return false;
        }

        return true;
    }
}
