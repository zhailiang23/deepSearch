package com.ynet.mgmt.clustering.controller;

import com.ynet.mgmt.clustering.dto.ClusterAnalysisRequest;
import com.ynet.mgmt.clustering.dto.ClusterAnalysisResponse;
import com.ynet.mgmt.clustering.exception.ClusterAnalysisException;
import com.ynet.mgmt.clustering.service.ClusterAnalysisService;
import com.ynet.mgmt.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 聚类分析控制器
 *
 * @author system
 * @since 1.0.0
 */
@RestController
@RequestMapping("/clustering")
@Validated
public class ClusterAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAnalysisController.class);

    private final ClusterAnalysisService clusterAnalysisService;

    public ClusterAnalysisController(ClusterAnalysisService clusterAnalysisService) {
        this.clusterAnalysisService = clusterAnalysisService;
    }

    /**
     * 执行聚类分析
     *
     * @param request 聚类分析请求
     * @return 聚类分析响应
     */
    @PostMapping("/analyze")
    public ApiResponse<ClusterAnalysisResponse> analyze(@Valid @RequestBody ClusterAnalysisRequest request) {
        try {
            logger.info("收到聚类分析请求: {}", request);

            // 真实调用Python聚类服务
            ClusterAnalysisResponse response = clusterAnalysisService.analyzeClusters(request);
            return ApiResponse.success(response);
        } catch (ClusterAnalysisException e) {
            logger.error("聚类分析失败: {}", e.getMessage());
            return ApiResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("聚类分析系统错误", e);
            return ApiResponse.error("系统错误: " + e.getMessage());
        }
    }

    /**
     * 创建模拟响应数据（用于前端测试）
     */
    private ClusterAnalysisResponse createMockResponse(ClusterAnalysisRequest request) {
        ClusterAnalysisResponse response = new ClusterAnalysisResponse();

        // 设置基本统计
        response.setTotalTexts(1000);
        response.setValidClusters(5);
        response.setNoiseCount(50);
        response.setTimeRangeDesc(getTimeRangeDesc(request.getTimeRange()));

        // 创建模拟聚类话题
        java.util.List<com.ynet.mgmt.clustering.dto.ClusterTopicDTO> clusters = new java.util.ArrayList<>();

        // 话题1：转账功能
        com.ynet.mgmt.clustering.dto.ClusterTopicDTO topic1 = new com.ynet.mgmt.clustering.dto.ClusterTopicDTO();
        topic1.setClusterId(0);
        topic1.setTopic("转账汇款功能咨询");
        topic1.setTags(java.util.Arrays.asList("转账", "汇款", "限额"));
        topic1.setExamples(java.util.Arrays.asList("如何转账", "转账限额是多少", "跨行转账手续费"));
        topic1.setSize(250);
        clusters.add(topic1);

        // 话题2：信用卡服务
        com.ynet.mgmt.clustering.dto.ClusterTopicDTO topic2 = new com.ynet.mgmt.clustering.dto.ClusterTopicDTO();
        topic2.setClusterId(1);
        topic2.setTopic("信用卡办理与使用");
        topic2.setTags(java.util.Arrays.asList("信用卡", "申请", "额度"));
        topic2.setExamples(java.util.Arrays.asList("如何申请信用卡", "信用卡额度", "信用卡还款"));
        topic2.setSize(180);
        clusters.add(topic2);

        // 话题3：理财产品
        com.ynet.mgmt.clustering.dto.ClusterTopicDTO topic3 = new com.ynet.mgmt.clustering.dto.ClusterTopicDTO();
        topic3.setClusterId(2);
        topic3.setTopic("理财产品购买咨询");
        topic3.setTags(java.util.Arrays.asList("理财", "收益", "风险"));
        topic3.setExamples(java.util.Arrays.asList("理财产品收益", "购买理财", "理财风险"));
        topic3.setSize(220);
        clusters.add(topic3);

        // 话题4：手机银行
        com.ynet.mgmt.clustering.dto.ClusterTopicDTO topic4 = new com.ynet.mgmt.clustering.dto.ClusterTopicDTO();
        topic4.setClusterId(3);
        topic4.setTopic("手机银行APP使用");
        topic4.setTags(java.util.Arrays.asList("APP", "登录", "功能"));
        topic4.setExamples(java.util.Arrays.asList("手机银行登录", "APP功能", "密码修改"));
        topic4.setSize(150);
        clusters.add(topic4);

        // 话题5：账户查询
        com.ynet.mgmt.clustering.dto.ClusterTopicDTO topic5 = new com.ynet.mgmt.clustering.dto.ClusterTopicDTO();
        topic5.setClusterId(4);
        topic5.setTopic("账户余额与明细查询");
        topic5.setTags(java.util.Arrays.asList("查询", "余额", "明细"));
        topic5.setExamples(java.util.Arrays.asList("查询余额", "交易明细", "账单查询"));
        topic5.setSize(150);
        clusters.add(topic5);

        response.setClusters(clusters);

        // 创建模拟散点图数据
        java.util.List<com.ynet.mgmt.clustering.dto.ScatterPointDTO> scatterData = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random(42);

        // 为每个簇生成散点数据
        String[][] examples = {
            {"转账", "汇款", "转账限额", "跨行转账"},
            {"信用卡", "办卡", "信用卡额度", "还款"},
            {"理财", "收益", "购买理财", "理财产品"},
            {"手机银行", "APP", "登录", "密码"},
            {"查询", "余额", "明细", "账单"}
        };

        for (int clusterId = 0; clusterId < 5; clusterId++) {
            // 每个簇的中心点
            double centerX = random.nextDouble() * 20 - 10;
            double centerY = random.nextDouble() * 20 - 10;

            // 为每个簇生成散点
            int pointCount = clusters.get(clusterId).getSize();
            for (int i = 0; i < pointCount; i++) {
                com.ynet.mgmt.clustering.dto.ScatterPointDTO point = new com.ynet.mgmt.clustering.dto.ScatterPointDTO();
                point.setX(centerX + random.nextGaussian() * 2);
                point.setY(centerY + random.nextGaussian() * 2);
                point.setCluster(clusterId);
                point.setText(examples[clusterId][random.nextInt(examples[clusterId].length)]);
                scatterData.add(point);
            }
        }

        // 添加噪声点
        for (int i = 0; i < 50; i++) {
            com.ynet.mgmt.clustering.dto.ScatterPointDTO point = new com.ynet.mgmt.clustering.dto.ScatterPointDTO();
            point.setX(random.nextDouble() * 20 - 10);
            point.setY(random.nextDouble() * 20 - 10);
            point.setCluster(-1);
            point.setText("其他查询");
            scatterData.add(point);
        }

        response.setScatterData(scatterData);

        return response;
    }

    /**
     * 获取时间范围描述
     */
    private String getTimeRangeDesc(String timeRange) {
        return switch (timeRange) {
            case "7d" -> "最近 7 天";
            case "30d" -> "最近 30 天";
            case "90d" -> "最近 90 天";
            default -> "未知时间范围";
        };
    }
}
