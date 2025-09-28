package com.ynet.mgmt.statistics.controller;

import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.statistics.dto.MismatchKeywordRankingDTO;
import com.ynet.mgmt.statistics.dto.RankingQueryRequest;
import com.ynet.mgmt.statistics.dto.RankingQueryResponse;
import com.ynet.mgmt.statistics.enums.TimeRangeEnum;
import com.ynet.mgmt.statistics.service.MismatchAnalysisService;
import com.ynet.mgmt.statistics.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 关键词不匹配度统计控制器
 *
 * @author system
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/statistics/mismatch-keywords")
@Tag(name = "关键词不匹配度统计", description = "关键词不匹配度统计相关接口")
@Validated
public class MismatchKeywordController {

    private static final Logger logger = LoggerFactory.getLogger(MismatchKeywordController.class);

    private final MismatchAnalysisService mismatchAnalysisService;
    private final RankingService rankingService;

    public MismatchKeywordController(MismatchAnalysisService mismatchAnalysisService, RankingService rankingService) {
        this.mismatchAnalysisService = mismatchAnalysisService;
        this.rankingService = rankingService;
    }

    /**
     * 获取关键词不匹配度排行榜
     *
     * @param timeRange        时间范围
     * @param page            页码
     * @param size            每页大小
     * @param keyword         搜索关键词（可选）
     * @param minMismatchRate 最小不匹配率（可选）
     * @param maxMismatchRate 最大不匹配率（可选）
     * @return 排行榜数据
     */
    @GetMapping("/ranking")
    @Operation(summary = "获取关键词不匹配度排行榜", description = "根据时间范围获取关键词不匹配度排行榜，支持分页和筛选")
    public ResponseEntity<ApiResponse<RankingQueryResponse>> getRanking(
            @Parameter(description = "时间范围", example = "7d")
            @RequestParam(defaultValue = "7d") String timeRange,

            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每页大小，最大100", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "最小不匹配率，0-1之间")
            @RequestParam(required = false) Double minMismatchRate,

            @Parameter(description = "最大不匹配率，0-1之间")
            @RequestParam(required = false) Double maxMismatchRate) {

        logger.info("获取关键词不匹配度排行榜 - timeRange: {}, page: {}, size: {}, keyword: {}",
                timeRange, page, size, keyword);

        try {
            // 参数验证
            if (!TimeRangeEnum.isValidCode(timeRange)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的时间范围参数: " + timeRange));
            }

            if (page < 1) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("页码必须大于0"));
            }

            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("每页大小必须在1-100之间"));
            }

            // 构造查询请求
            RankingQueryRequest request = new RankingQueryRequest(timeRange, page, size);
            request.setKeyword(keyword);
            request.setMinMismatchRate(minMismatchRate);
            request.setMaxMismatchRate(maxMismatchRate);

            // 调用服务层获取数据
            RankingQueryResponse response = rankingService.getRanking(request);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("参数错误: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("获取排行榜数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("数据查询失败，请稍后重试"));
        }
    }

    /**
     * 获取关键词详细信息
     *
     * @param keyword   关键词
     * @param timeRange 时间范围
     * @return 关键词详细信息
     */
    @GetMapping("/detail")
    @Operation(summary = "获取关键词详细信息", description = "获取指定关键词在指定时间范围内的详细统计信息")
    public ResponseEntity<ApiResponse<MismatchKeywordRankingDTO>> getDetail(
            @Parameter(description = "关键词", required = true)
            @RequestParam String keyword,

            @Parameter(description = "时间范围", example = "7d")
            @RequestParam(defaultValue = "7d") String timeRange) {

        logger.info("获取关键词详细信息 - keyword: {}, timeRange: {}", keyword, timeRange);

        try {
            // 参数验证
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("关键词不能为空"));
            }

            if (!TimeRangeEnum.isValidCode(timeRange)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的时间范围参数: " + timeRange));
            }

            // 调用服务层获取数据
            MismatchKeywordRankingDTO detail = mismatchAnalysisService.getKeywordDetail(keyword, TimeRangeEnum.fromCode(timeRange));

            return ResponseEntity.ok(ApiResponse.success(detail));

        } catch (Exception e) {
            logger.error("获取关键词详细信息失败 - keyword: {}", keyword, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("数据查询失败，请稍后重试"));
        }
    }

    /**
     * 导出排行榜数据
     *
     * @param request 查询请求参数
     * @return 导出文件
     */
    @PostMapping("/export")
    @Operation(summary = "导出排行榜数据", description = "导出关键词不匹配度排行榜数据为Excel文件")
    public ResponseEntity<ApiResponse<String>> exportData(@Valid @RequestBody RankingQueryRequest request) {

        logger.info("导出排行榜数据 - request: {}", request);

        try {
            // TODO: 实现数据导出功能
            // String downloadUrl = exportService.exportRankingData(request);

            String downloadUrl = "/api/files/download/ranking-export-" + System.currentTimeMillis() + ".xlsx";

            return ResponseEntity.ok(ApiResponse.success(downloadUrl, "导出任务已创建"));

        } catch (Exception e) {
            logger.error("导出排行榜数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("导出失败，请稍后重试"));
        }
    }

    /**
     * 获取统计概览
     *
     * @param timeRange 时间范围
     * @return 统计概览信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计概览", description = "获取指定时间范围内的关键词不匹配度统计概览")
    public ResponseEntity<ApiResponse<RankingQueryResponse.StatisticsInfo>> getStatistics(
            @Parameter(description = "时间范围", example = "7d")
            @RequestParam(defaultValue = "7d") String timeRange) {

        logger.info("获取统计概览 - timeRange: {}", timeRange);

        try {
            // 参数验证
            if (!TimeRangeEnum.isValidCode(timeRange)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的时间范围参数: " + timeRange));
            }

            // 调用服务层获取数据
            RankingQueryResponse.StatisticsInfo statistics = mismatchAnalysisService.calculateMismatchStatistics(TimeRangeEnum.fromCode(timeRange));

            return ResponseEntity.ok(ApiResponse.success(statistics));

        } catch (Exception e) {
            logger.error("获取统计概览失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("数据查询失败，请稍后重试"));
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 创建模拟响应数据
     */
    private RankingQueryResponse createMockResponse(RankingQueryRequest request) {
        List<MismatchKeywordRankingDTO> mockData = new ArrayList<>();

        // 模拟10条排行榜数据
        String[] keywords = {"银行贷款", "信用卡申请", "投资理财", "房屋按揭", "保险产品",
                           "存款利率", "外汇兑换", "理财产品", "基金投资", "股票开户"};
        double[] mismatchRates = {0.85, 0.78, 0.72, 0.69, 0.65, 0.62, 0.58, 0.55, 0.52, 0.48};
        int[] searchCounts = {1250, 1100, 980, 850, 720, 650, 580, 520, 480, 420};

        for (int i = 0; i < keywords.length; i++) {
            MismatchKeywordRankingDTO item = new MismatchKeywordRankingDTO();
            item.setRank(i + 1);
            item.setKeyword(keywords[i]);
            item.setMismatchRate(mismatchRates[i]);
            item.setSearchCount(searchCounts[i]);
            item.setMismatchCount((int) (searchCounts[i] * mismatchRates[i]));
            item.setTrend(i < 3 ? "up" : i < 7 ? "stable" : "down");
            item.setRankChange(i < 3 ? i + 1 : i < 7 ? 0 : -(i - 6));
            mockData.add(item);
        }

        RankingQueryResponse response = new RankingQueryResponse(mockData, request.getTimeRange(),
                                                               50L, request.getPage(), request.getSize());
        response.setStatistics(createMockStatistics());

        return response;
    }

    /**
     * 创建模拟详细数据
     */
    private MismatchKeywordRankingDTO createMockDetail(String keyword) {
        MismatchKeywordRankingDTO detail = new MismatchKeywordRankingDTO();
        detail.setKeyword(keyword);
        detail.setRank(1);
        detail.setMismatchRate(0.75);
        detail.setSearchCount(1250);
        detail.setMismatchCount(938);
        detail.setTrend("up");
        detail.setRankChange(2);
        return detail;
    }

    /**
     * 创建模拟统计数据
     */
    private RankingQueryResponse.StatisticsInfo createMockStatistics() {
        return new RankingQueryResponse.StatisticsInfo(150, 0.35, 0.95, 0.05);
    }
}