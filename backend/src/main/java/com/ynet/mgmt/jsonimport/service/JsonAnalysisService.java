package com.ynet.mgmt.jsonimport.service;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldAnalysisResult;
import com.ynet.mgmt.jsonimport.model.FieldStatistics;
import com.ynet.mgmt.jsonimport.model.JsonSchemaAnalysis;
import com.ynet.mgmt.jsonimport.util.FieldTypeInferrer;
import com.ynet.mgmt.jsonimport.util.StatisticsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * JSON结构分析服务
 * 提供JSON数据的智能分析功能，包括字段类型推断和统计分析
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JsonAnalysisService {

    private final FieldTypeInferrer fieldTypeInferrer;
    private final StatisticsCalculator statisticsCalculator;

    // 分析结果缓存（基于文件哈希）
    private final Map<String, JsonSchemaAnalysis> analysisCache = new ConcurrentHashMap<>();

    private static final int SAMPLE_SIZE_FOR_TYPE_INFERENCE = 1000; // 类型推断的样本大小
    private static final int MAX_SAMPLE_VALUES = 20; // 样本值显示数量

    /**
     * 分析JSON数组数据
     *
     * @param jsonData JSON数据列表，每个元素是一个扁平的Map
     * @return 分析结果
     */
    public JsonSchemaAnalysis analyzeJsonArray(List<Map<String, Object>> jsonData) {
        long startTime = System.currentTimeMillis();

        if (jsonData == null || jsonData.isEmpty()) {
            return createEmptyAnalysis(startTime);
        }

        log.info("开始分析JSON数据，记录数: {}", jsonData.size());

        // 生成数据哈希用于缓存
        String dataHash = generateDataHash(jsonData);
        if (analysisCache.containsKey(dataHash)) {
            log.info("使用缓存的分析结果，哈希: {}", dataHash);
            return analysisCache.get(dataHash);
        }

        try {
            // 提取所有字段
            Set<String> allFields = extractAllFields(jsonData);
            log.info("检测到字段数量: {}", allFields.size());

            // 分析每个字段
            Map<String, FieldAnalysisResult> fieldAnalysisMap = new HashMap<>();
            for (String fieldName : allFields) {
                try {
                    FieldAnalysisResult fieldResult = analyzeField(fieldName, jsonData);
                    fieldAnalysisMap.put(fieldName, fieldResult);
                    log.debug("字段 {} 分析完成，类型: {}", fieldName, fieldResult.getInferredType());
                } catch (Exception e) {
                    log.error("分析字段 {} 时出错: {}", fieldName, e.getMessage(), e);
                    // 创建默认的分析结果
                    fieldAnalysisMap.put(fieldName, createDefaultFieldAnalysis(fieldName));
                }
            }

            // 生成整体分析结果
            JsonSchemaAnalysis analysis = buildAnalysisResult(
                    jsonData.size(),
                    fieldAnalysisMap,
                    dataHash,
                    startTime
            );

            // 缓存结果
            analysisCache.put(dataHash, analysis);

            log.info("JSON分析完成，耗时: {}ms", analysis.getAnalysisDurationMs());
            return analysis;

        } catch (Exception e) {
            log.error("JSON分析过程中发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("JSON分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分析单个字段
     *
     * @param fieldName 字段名
     * @param jsonData  JSON数据
     * @return 字段分析结果
     */
    public FieldAnalysisResult analyzeField(String fieldName, List<Map<String, Object>> jsonData) {
        // 提取字段值，过滤null记录
        List<Object> fieldValues = jsonData.stream()
                .filter(Objects::nonNull) // 过滤null记录
                .map(record -> record.get(fieldName))
                .toList();

        return analyzeFieldValues(fieldName, fieldValues);
    }

    /**
     * 分析字段值列表
     *
     * @param fieldName 字段名
     * @param values    字段值列表
     * @return 字段分析结果
     */
    public FieldAnalysisResult analyzeFieldValues(String fieldName, List<Object> values) {
        if (values == null || values.isEmpty()) {
            return createDefaultFieldAnalysis(fieldName);
        }

        // 类型推断（使用采样提高性能）
        List<Object> sampleForInference = values.size() > SAMPLE_SIZE_FOR_TYPE_INFERENCE
                ? values.stream().limit(SAMPLE_SIZE_FOR_TYPE_INFERENCE).toList()
                : values;

        FieldTypeInferrer.TypeInferenceResult typeResult = fieldTypeInferrer.inferFieldType(sampleForInference);

        // 统计计算
        FieldStatistics statistics = statisticsCalculator.calculateStatistics(
                fieldName, values, typeResult.getType()
        );

        // 提取样本值
        List<String> sampleValues = extractSampleValues(values);

        // 分析中文内容
        ChineseContentAnalysis chineseAnalysis = analyzeChineseContent(values);

        // 检测问题
        List<String> issues = detectFieldIssues(fieldName, values, typeResult, statistics);

        // 生成建议（考虑中文内容）
        boolean suggestAsId = suggestAsIdField(fieldName, statistics, typeResult.getType());
        boolean suggestIndex = suggestIndexField(fieldName, statistics, typeResult.getType(), chineseAnalysis);
        int importance = calculateFieldImportance(fieldName, statistics, typeResult.getType(), chineseAnalysis);

        return FieldAnalysisResult.builder()
                .fieldName(fieldName)
                .inferredType(typeResult.getType())
                .confidence(typeResult.getConfidence())
                .statistics(statistics)
                .sampleValues(sampleValues)
                .issues(issues)
                .suggestAsId(suggestAsId)
                .suggestIndex(suggestIndex)
                .importance(importance)
                .hasChineseContent(chineseAnalysis.isHasChineseContent())
                .chineseContentRatio(chineseAnalysis.getChineseContentRatio())
                .chineseContentSamples(chineseAnalysis.getChineseContentSamples())
                .typeCandidates(typeResult.getCandidates())
                .build();
    }

    /**
     * 提取所有字段名
     */
    private Set<String> extractAllFields(List<Map<String, Object>> jsonData) {
        Set<String> allFields = new HashSet<>();
        for (Map<String, Object> record : jsonData) {
            if (record != null) {
                allFields.addAll(record.keySet());
            }
        }
        return allFields;
    }

    /**
     * 生成数据哈希
     */
    private String generateDataHash(List<Map<String, Object>> jsonData) {
        // 使用数据的前几条记录和字段信息生成哈希
        StringBuilder sb = new StringBuilder();

        // 添加记录数量
        sb.append("count:").append(jsonData.size()).append(";");

        // 添加字段信息
        Set<String> fields = extractAllFields(jsonData);
        sb.append("fields:").append(String.join(",", fields.stream().sorted().toList())).append(";");

        // 添加前几条记录的哈希
        int sampleSize = Math.min(5, jsonData.size());
        for (int i = 0; i < sampleSize; i++) {
            Map<String, Object> record = jsonData.get(i);
            if (record != null) {
                sb.append("record").append(i).append(":").append(record.toString()).append(";");
            } else {
                sb.append("record").append(i).append(":null;");
            }
        }

        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 提取样本值
     */
    private List<String> extractSampleValues(List<Object> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.trim().isEmpty())
                .distinct()
                .limit(MAX_SAMPLE_VALUES)
                .toList();
    }

    /**
     * 检测字段问题
     */
    private List<String> detectFieldIssues(String fieldName, List<Object> values,
                                            FieldTypeInferrer.TypeInferenceResult typeResult,
                                            FieldStatistics statistics) {
        List<String> issues = new ArrayList<>();

        // 置信度检查
        if (typeResult.getConfidence() < 0.7) {
            issues.add("类型推断置信度较低 (" + String.format("%.2f", typeResult.getConfidence()) + ")");
        }

        // 空值检查
        if (statistics.getNullRatio() > 0.5) {
            issues.add("空值比例过高 (" + String.format("%.1f%%", statistics.getNullRatio() * 100) + ")");
        }

        // 数据量检查
        if (statistics.getNonNullCount() < 10) {
            issues.add("有效数据量过少 (" + statistics.getNonNullCount() + " 条)");
        }

        // 唯一性检查
        if (statistics.getUniqueRatio() < 0.01) {
            issues.add("数据重复度过高，几乎没有唯一值");
        }

        // 异常值检查
        if (statistics.isHasOutliers()) {
            issues.add("检测到异常值");
        }

        // 字段名检查
        if (fieldName.contains(" ") || fieldName.contains("-")) {
            issues.add("字段名包含空格或特殊字符，建议规范化");
        }

        return issues;
    }

    /**
     * 建议作为ID字段
     */
    private boolean suggestAsIdField(String fieldName, FieldStatistics statistics, FieldType fieldType) {
        // 字段名暗示
        String lowerFieldName = fieldName.toLowerCase();
        boolean nameIndicatesId = lowerFieldName.equals("id") ||
                                 lowerFieldName.equals("_id") ||
                                 lowerFieldName.endsWith("_id") ||
                                 lowerFieldName.equals("uuid") ||
                                 lowerFieldName.equals("key");

        // 统计特征
        boolean highUniqueness = statistics.getUniqueRatio() > 0.95;
        boolean lowNullRatio = statistics.getNullRatio() < 0.01;
        boolean suitableType = fieldType == FieldType.STRING || fieldType == FieldType.INTEGER;

        return nameIndicatesId && highUniqueness && lowNullRatio && suitableType;
    }

    /**
     * 建议建立索引（考虑中文内容）
     */
    private boolean suggestIndexField(String fieldName, FieldStatistics statistics, FieldType fieldType, ChineseContentAnalysis chineseAnalysis) {
        // 高查询价值字段
        String lowerFieldName = fieldName.toLowerCase();
        boolean highQueryValue = lowerFieldName.contains("id") ||
                                lowerFieldName.contains("name") ||
                                lowerFieldName.contains("code") ||
                                lowerFieldName.contains("status") ||
                                lowerFieldName.contains("type") ||
                                lowerFieldName.contains("category");

        // 统计特征
        boolean goodCardinality = statistics.getUniqueRatio() > 0.1 && statistics.getUniqueRatio() < 0.9;
        boolean lowNullRatio = statistics.getNullRatio() < 0.3;
        boolean indexableType = fieldType != FieldType.UNKNOWN;

        // 中文内容特征 - 如果字段包含足够比例的中文内容，提高索引建议的权重
        boolean hasSignificantChineseContent = chineseAnalysis.isHasChineseContent() &&
                                              chineseAnalysis.getChineseContentRatio() >= 0.3;

        // 如果有中文内容，更积极地建议建立索引（因为需要拼音搜索支持）
        if (hasSignificantChineseContent) {
            // 对于中文内容，降低其他条件的要求
            return (highQueryValue || goodCardinality || statistics.getUniqueRatio() > 0.05) &&
                   statistics.getNullRatio() < 0.5 &&
                   indexableType;
        }

        return (highQueryValue || goodCardinality) && lowNullRatio && indexableType;
    }

    /**
     * 计算字段重要性（考虑中文内容）
     */
    private int calculateFieldImportance(String fieldName, FieldStatistics statistics, FieldType fieldType, ChineseContentAnalysis chineseAnalysis) {
        int importance = 50; // 基础分

        // 字段名重要性
        String lowerFieldName = fieldName.toLowerCase();
        if (lowerFieldName.contains("id")) importance += 30;
        if (lowerFieldName.contains("name") || lowerFieldName.contains("title")) importance += 20;
        if (lowerFieldName.contains("time") || lowerFieldName.contains("date")) importance += 15;
        if (lowerFieldName.contains("status") || lowerFieldName.contains("state")) importance += 10;

        // 数据完整性
        if (statistics.getNullRatio() < 0.05) importance += 15;
        else if (statistics.getNullRatio() > 0.5) importance -= 20;

        // 数据多样性
        if (statistics.getUniqueRatio() > 0.1 && statistics.getUniqueRatio() < 0.95) importance += 10;

        // 类型稳定性
        if (fieldType != FieldType.UNKNOWN && fieldType != FieldType.STRING) importance += 5;

        // 数据质量
        importance += statistics.getQualityScore() / 10;

        // 中文内容特征加分
        if (chineseAnalysis.isHasChineseContent()) {
            // 中文内容比例越高，重要性越高
            int chineseBonus = (int) (chineseAnalysis.getChineseContentRatio() * 20);
            importance += chineseBonus;

            // 如果字段名暗示是文本类字段且包含中文，额外加分
            if (lowerFieldName.contains("name") || lowerFieldName.contains("title") ||
                lowerFieldName.contains("content") || lowerFieldName.contains("description") ||
                lowerFieldName.contains("text") || lowerFieldName.contains("comment")) {
                importance += 10;
            }

            log.debug("字段 {} 包含中文内容，占比 {:.2f}，获得重要性加分 {}",
                     fieldName, chineseAnalysis.getChineseContentRatio(), chineseBonus + 10);
        }

        return Math.max(0, Math.min(100, importance));
    }

    /**
     * 构建分析结果
     */
    private JsonSchemaAnalysis buildAnalysisResult(int totalRecords,
                                                   Map<String, FieldAnalysisResult> fieldAnalysisMap,
                                                   String dataHash,
                                                   long startTime) {
        long endTime = System.currentTimeMillis();

        // 计算整体质量评分
        int overallQualityScore = fieldAnalysisMap.values().stream()
                .mapToInt(f -> f.getStatistics().getQualityScore())
                .sum() / Math.max(1, fieldAnalysisMap.size());

        // 推荐ID字段
        String recommendedIdField = fieldAnalysisMap.values().stream()
                .filter(FieldAnalysisResult::isSuggestAsId)
                .max(Comparator.comparingInt(FieldAnalysisResult::getImportance))
                .map(FieldAnalysisResult::getFieldName)
                .orElse(null);

        // 推荐索引字段
        List<String> recommendedIndexFields = fieldAnalysisMap.values().stream()
                .filter(FieldAnalysisResult::isSuggestIndex)
                .sorted(Comparator.comparingInt(FieldAnalysisResult::getImportance).reversed())
                .map(FieldAnalysisResult::getFieldName)
                .toList();

        // 数据一致性报告
        JsonSchemaAnalysis.DataConsistencyReport consistencyReport = buildConsistencyReport(fieldAnalysisMap);

        return JsonSchemaAnalysis.builder()
                .totalRecords(totalRecords)
                .totalFields(fieldAnalysisMap.size())
                .fieldAnalysis(fieldAnalysisMap)
                .analyzedAt(LocalDateTime.now())
                .sourceFileHash(dataHash)
                .analysisDurationMs(endTime - startTime)
                .overallQualityScore(overallQualityScore)
                .recommendedIdField(recommendedIdField)
                .recommendedIndexFields(recommendedIndexFields)
                .consistencyReport(consistencyReport)
                .build();
    }

    /**
     * 构建数据一致性报告
     */
    private JsonSchemaAnalysis.DataConsistencyReport buildConsistencyReport(
            Map<String, FieldAnalysisResult> fieldAnalysisMap) {

        // 计算完整性评分
        double avgCompleteness = fieldAnalysisMap.values().stream()
                .mapToDouble(f -> 1.0 - f.getStatistics().getNullRatio())
                .average()
                .orElse(0.0);
        int completenessScore = (int) (avgCompleteness * 100);

        // 计算一致性评分
        double avgConfidence = fieldAnalysisMap.values().stream()
                .mapToDouble(FieldAnalysisResult::getConfidence)
                .average()
                .orElse(0.0);
        int consistencyScore = (int) (avgConfidence * 100);

        // 收集异常情况
        List<String> anomalies = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        for (FieldAnalysisResult field : fieldAnalysisMap.values()) {
            anomalies.addAll(field.getIssues());

            if (field.getStatistics().getNullRatio() > 0.3) {
                recommendations.add("字段 " + field.getFieldName() + " 空值比例较高，考虑数据清洗");
            }
            if (field.getConfidence() < 0.7) {
                recommendations.add("字段 " + field.getFieldName() + " 类型不明确，可能需要数据标准化");
            }
        }

        // 判断是否适合导入Elasticsearch
        boolean readyForElasticsearch = completenessScore >= 70 &&
                                       consistencyScore >= 70 &&
                                       anomalies.size() < fieldAnalysisMap.size() / 2;

        return JsonSchemaAnalysis.DataConsistencyReport.builder()
                .completenessScore(completenessScore)
                .consistencyScore(consistencyScore)
                .anomalies(anomalies.stream().distinct().limit(20).toList())
                .recommendations(recommendations.stream().distinct().limit(10).toList())
                .readyForElasticsearch(readyForElasticsearch)
                .build();
    }

    /**
     * 创建默认字段分析结果
     */
    private FieldAnalysisResult createDefaultFieldAnalysis(String fieldName) {
        return FieldAnalysisResult.builder()
                .fieldName(fieldName)
                .inferredType(FieldType.UNKNOWN)
                .confidence(0.0)
                .statistics(FieldStatistics.builder()
                        .totalCount(0)
                        .nonNullCount(0)
                        .nullCount(0)
                        .nullRatio(1.0)
                        .uniqueCount(0)
                        .uniqueRatio(0.0)
                        .qualityScore(0)
                        .build())
                .sampleValues(Collections.emptyList())
                .issues(List.of("字段无有效数据"))
                .suggestAsId(false)
                .suggestIndex(false)
                .importance(0)
                .hasChineseContent(false)
                .chineseContentRatio(0.0)
                .chineseContentSamples(Collections.emptyList())
                .typeCandidates(Collections.emptyList())
                .build();
    }

    /**
     * 创建空分析结果
     */
    private JsonSchemaAnalysis createEmptyAnalysis(long startTime) {
        return JsonSchemaAnalysis.builder()
                .totalRecords(0)
                .totalFields(0)
                .fieldAnalysis(Collections.emptyMap())
                .analyzedAt(LocalDateTime.now())
                .sourceFileHash("")
                .analysisDurationMs(System.currentTimeMillis() - startTime)
                .overallQualityScore(0)
                .recommendedIdField(null)
                .recommendedIndexFields(Collections.emptyList())
                .consistencyReport(JsonSchemaAnalysis.DataConsistencyReport.builder()
                        .completenessScore(0)
                        .consistencyScore(0)
                        .anomalies(List.of("数据为空"))
                        .recommendations(List.of("请提供有效的JSON数据"))
                        .readyForElasticsearch(false)
                        .build())
                .build();
    }

    /**
     * 清除分析缓存
     */
    public void clearCache() {
        analysisCache.clear();
        log.info("分析缓存已清空");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", analysisCache.size());
        stats.put("cacheKeys", analysisCache.keySet());
        return stats;
    }

    /**
     * 分析字段值中的中文内容
     *
     * @param values 字段值列表
     * @return 中文内容分析结果
     */
    private ChineseContentAnalysis analyzeChineseContent(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return new ChineseContentAnalysis(false, 0.0, Collections.emptyList());
        }

        List<String> chineseContentSamples = new ArrayList<>();
        int totalNonNullValues = 0;
        int valuesWithChinese = 0;

        for (Object value : values) {
            if (value != null) {
                totalNonNullValues++;
                String stringValue = value.toString().trim();

                if (containsChinese(stringValue)) {
                    valuesWithChinese++;
                    // 收集包含中文的样本值（限制数量）
                    if (chineseContentSamples.size() < MAX_SAMPLE_VALUES) {
                        chineseContentSamples.add(stringValue);
                    }
                }
            }
        }

        boolean hasChineseContent = valuesWithChinese > 0;
        double chineseContentRatio = totalNonNullValues > 0 ? (double) valuesWithChinese / totalNonNullValues : 0.0;

        log.debug("中文内容分析完成: 总值={}, 含中文值={}, 占比={:.2f}",
                 totalNonNullValues, valuesWithChinese, chineseContentRatio);

        return new ChineseContentAnalysis(hasChineseContent, chineseContentRatio, chineseContentSamples);
    }

    /**
     * 判断字符串是否包含中文字符
     *
     * @param text 待检测的字符串
     * @return 是否包含中文
     */
    private boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 检查是否包含中文字符（CJK统一表意文字）
        return text.chars().anyMatch(ch ->
            (ch >= 0x4E00 && ch <= 0x9FFF) ||     // CJK统一表意文字
            (ch >= 0x3400 && ch <= 0x4DBF) ||     // CJK扩展A
            (ch >= 0x20000 && ch <= 0x2A6DF) ||   // CJK扩展B
            (ch >= 0x2A700 && ch <= 0x2B73F) ||   // CJK扩展C
            (ch >= 0x2B740 && ch <= 0x2B81F) ||   // CJK扩展D
            (ch >= 0x2B820 && ch <= 0x2CEAF)      // CJK扩展E
        );
    }

    /**
     * 中文内容分析结果的内部类
     */
    private static class ChineseContentAnalysis {
        private final boolean hasChineseContent;
        private final double chineseContentRatio;
        private final List<String> chineseContentSamples;

        public ChineseContentAnalysis(boolean hasChineseContent, double chineseContentRatio,
                                     List<String> chineseContentSamples) {
            this.hasChineseContent = hasChineseContent;
            this.chineseContentRatio = chineseContentRatio;
            this.chineseContentSamples = chineseContentSamples != null ? chineseContentSamples : Collections.emptyList();
        }

        public boolean isHasChineseContent() { return hasChineseContent; }
        public double getChineseContentRatio() { return chineseContentRatio; }
        public List<String> getChineseContentSamples() { return chineseContentSamples; }
    }
}