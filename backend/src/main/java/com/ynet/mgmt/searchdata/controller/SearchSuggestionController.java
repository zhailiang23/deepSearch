package com.ynet.mgmt.searchdata.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchdata.dto.SearchSuggestionDTO;
import com.ynet.mgmt.searchdata.dto.SearchSuggestionRequest;
import com.ynet.mgmt.searchdata.service.SearchSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索建议控制器
 * 提供智能搜索建议API
 *
 * @author system
 * @since 1.0.0
 */
@Tag(name = "搜索建议", description = "智能搜索建议相关API")
@RestController
@RequestMapping("/elasticsearch")
public class SearchSuggestionController {

    private static final Logger logger = LoggerFactory.getLogger(SearchSuggestionController.class);

    @Autowired
    private SearchSuggestionService searchSuggestionService;

    /**
     * 获取搜索建议
     * <p>
     * 融合多个数据源生成智能搜索建议：
     * 1. ES Completion Suggester - 基于索引内容的智能建议
     * 2. 搜索历史 - 用户个人和全局搜索历史
     * 3. 热门话题 - 系统管理的热门搜索词
     * <p>
     * 综合评分算法：
     * - ES相关性得分(40%)
     * - 历史权重得分(35%) = 个人历史(70%) + 全局历史(30%)
     * - 热度权重得分(25%)
     *
     * @param request 搜索建议请求
     * @return 搜索建议列表
     */
    @Operation(
        summary = "获取搜索建议",
        description = "根据用户输入获取智能搜索建议，融合ES completion、搜索历史和热门话题"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = SearchSuggestionDTO.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "请求参数无效",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    @PostMapping("/search-suggestions")
    public ResponseEntity<ApiResponse<List<SearchSuggestionDTO>>> getSuggestions(
        @Parameter(description = "搜索建议请求参数", required = true)
        @Valid @RequestBody SearchSuggestionRequest request) {

        logger.info("获取搜索建议: query={}, userId={}, searchSpaceId={}, size={}",
            request.getQuery(), request.getUserId(), request.getSearchSpaceId(), request.getSize());

        try {
            List<SearchSuggestionDTO> suggestions = searchSuggestionService.getSuggestions(request);

            logger.info("搜索建议返回成功: query={}, count={}", request.getQuery(), suggestions.size());

            return ResponseEntity.ok(ApiResponse.success(suggestions));

        } catch (Exception e) {
            logger.error("获取搜索建议失败: query={}", request.getQuery(), e);
            return ResponseEntity.ok(ApiResponse.error("获取搜索建议失败: " + e.getMessage()));
        }
    }
}
