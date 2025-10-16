package com.ynet.mgmt.queryunderstanding.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.dto.QueryUnderstandingRequest;
import com.ynet.mgmt.queryunderstanding.dto.QueryUnderstandingResponse;
import com.ynet.mgmt.queryunderstanding.service.QueryUnderstandingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 查询理解 API 控制器
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@RestController
@RequestMapping("/query-understanding")
public class QueryUnderstandingController {

    @Autowired
    private QueryUnderstandingService queryUnderstandingService;

    /**
     * 理解查询
     *
     * @param request 查询理解请求
     * @return 查询理解结果
     */
    @PostMapping("/understand")
    public ApiResponse<QueryUnderstandingResponse> understandQuery(
            @RequestBody QueryUnderstandingRequest request) {

        log.info("收到查询理解请求: {}", request.getQuery());

        // 执行查询理解
        QueryContext context = queryUnderstandingService.understandQuery(request.getQuery());

        // 构建响应
        QueryUnderstandingResponse.QueryUnderstandingResponseBuilder builder =
                QueryUnderstandingResponse.builder()
                        .originalQuery(context.getOriginalQuery())
                        .normalizedQuery(context.getNormalizedQuery())
                        .correctedQuery(context.getCorrectedQuery())
                        .expandedQuery(context.getExpandedQuery())
                        .rewrittenQuery(context.getRewrittenQuery())
                        .finalQuery(context.getCurrentQuery())
                        .totalProcessingTime(context.getTotalProcessingTime());

        // 如果请求详细信息，返回所有数据
        if (Boolean.TRUE.equals(request.getIncludeDetails())) {
            builder.entities(context.getEntities())
                    .intent(context.getIntent())
                    .intentConfidence(context.getIntentConfidence())
                    .synonyms(context.getSynonyms())
                    .relatedTerms(context.getRelatedTerms())
                    .detectedPhrases(context.getDetectedPhrases())
                    .hotTopics(context.getHotTopics())
                    .processorTimings(context.getProcessorTimings())
                    .metadata(context.getMetadata());
        }

        QueryUnderstandingResponse response = builder.build();

        log.info("查询理解完成，耗时: {} ms", response.getTotalProcessingTime());
        return ApiResponse.success(response);
    }

    /**
     * 测试端点 - 快速测试查询理解
     *
     * @param query 查询文本
     * @return 处理后的查询
     */
    @GetMapping("/test")
    public ApiResponse<String> testQuery(@RequestParam String query) {
        log.info("收到测试请求: {}", query);
        QueryContext context = queryUnderstandingService.understandQuery(query);
        return ApiResponse.success(context.getCurrentQuery());
    }
}
