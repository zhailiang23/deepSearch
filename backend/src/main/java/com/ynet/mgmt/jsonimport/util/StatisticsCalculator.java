package com.ynet.mgmt.jsonimport.util;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计计算工具类
 * 实现各种字段统计信息的计算
 */
@Component
@Slf4j
public class StatisticsCalculator {

    private static final int TOP_VALUES_LIMIT = 10; // 高频值的最大数量
    private static final int SAMPLE_SIZE = 1000; // 样本大小限制

    /**
     * 计算字段统计信息
     *
     * @param fieldName 字段名
     * @param values    字段值列表
     * @param fieldType 推断的字段类型
     * @return 统计信息
     */
    public FieldStatistics calculateStatistics(String fieldName, List<Object> values, FieldType fieldType) {
        if (values == null || values.isEmpty()) {
            return FieldStatistics.builder()
                    .totalCount(0)
                    .nonNullCount(0)
                    .nullCount(0)
                    .nullRatio(0.0)
                    .uniqueCount(0)
                    .uniqueRatio(0.0)
                    .qualityScore(0)
                    .build();
        }

        // 基础统计
        int totalCount = values.size();
        List<Object> nonNullValues = values.stream()
                .filter(Objects::nonNull)
                .filter(v -> !v.toString().trim().isEmpty())
                .toList();

        int nonNullCount = nonNullValues.size();
        int nullCount = totalCount - nonNullCount;
        double nullRatio = totalCount > 0 ? (double) nullCount / totalCount : 0.0;

        // 唯一值统计
        Set<String> uniqueValues = nonNullValues.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        int uniqueCount = uniqueValues.size();
        double uniqueRatio = nonNullCount > 0 ? (double) uniqueCount / nonNullCount : 0.0;

        // 构建基础统计信息
        FieldStatistics.FieldStatisticsBuilder builder = FieldStatistics.builder()
                .totalCount(totalCount)
                .nonNullCount(nonNullCount)
                .nullCount(nullCount)
                .nullRatio(nullRatio)
                .uniqueCount(uniqueCount)
                .uniqueRatio(uniqueRatio);

        // 根据字段类型计算特定统计信息
        switch (fieldType) {
            case STRING, EMAIL, URL -> calculateStringStatistics(builder, nonNullValues);
            case INTEGER, FLOAT -> calculateNumericStatistics(builder, nonNullValues, fieldType);
            case DATE -> calculateDateStatistics(builder, nonNullValues);
            case BOOLEAN -> calculateBooleanStatistics(builder, nonNullValues);
        }

        // 计算高频值
        List<FieldStatistics.FrequencyItem> topValues = calculateTopValues(nonNullValues);
        builder.topValues(topValues);

        // 计算数据质量评分
        int qualityScore = calculateQualityScore(nullRatio, uniqueRatio, nonNullCount, fieldType);
        builder.qualityScore(qualityScore);

        // 检测异常值
        boolean hasOutliers = detectOutliers(nonNullValues, fieldType);
        builder.hasOutliers(hasOutliers);

        return builder.build();
    }

    /**
     * 计算字符串类型统计
     */
    private void calculateStringStatistics(FieldStatistics.FieldStatisticsBuilder builder, List<Object> values) {
        if (values.isEmpty()) {
            return;
        }

        List<Integer> lengths = values.stream()
                .map(v -> v.toString().length())
                .toList();

        int minLength = lengths.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxLength = lengths.stream().mapToInt(Integer::intValue).max().orElse(0);
        double avgLength = lengths.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        builder.minLength(minLength)
                .maxLength(maxLength)
                .avgLength(avgLength);
    }

    /**
     * 计算数值类型统计
     */
    private void calculateNumericStatistics(FieldStatistics.FieldStatisticsBuilder builder,
                                            List<Object> values, FieldType fieldType) {
        List<BigDecimal> numbers = new ArrayList<>();

        for (Object value : values) {
            try {
                BigDecimal number = new BigDecimal(value.toString().trim());
                numbers.add(number);
            } catch (NumberFormatException e) {
                log.debug("无法解析数值: {}", value);
            }
        }

        if (numbers.isEmpty()) {
            return;
        }

        // 计算统计值
        BigDecimal minValue = numbers.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxValue = numbers.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal sumValue = numbers.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgValue = sumValue.divide(new BigDecimal(numbers.size()), 6, RoundingMode.HALF_UP);

        builder.minValue(minValue)
                .maxValue(maxValue)
                .sumValue(sumValue)
                .avgValue(avgValue);
    }

    /**
     * 计算日期类型统计
     */
    private void calculateDateStatistics(FieldStatistics.FieldStatisticsBuilder builder, List<Object> values) {
        List<LocalDateTime> dates = new ArrayList<>();
        Set<String> detectedFormats = new HashSet<>();

        // 常见日期格式
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );

        for (Object value : values) {
            String str = value.toString().trim();

            // 检查时间戳格式
            try {
                long timestamp = Long.parseLong(str);
                if (timestamp > 0 && timestamp < 2524608000000L) {
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(
                            timestamp / 1000, 0, java.time.ZoneOffset.UTC
                    );
                    dates.add(dateTime);
                    detectedFormats.add("timestamp");
                    continue;
                }
            } catch (NumberFormatException e) {
                // 不是时间戳，继续尝试其他格式
            }

            // 尝试各种日期格式
            for (DateTimeFormatter formatter : formatters) {
                try {
                    if (str.contains("T") || str.contains(" ")) {
                        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
                        dates.add(dateTime);
                        detectedFormats.add(formatter.toString());
                        break;
                    } else {
                        LocalDateTime dateTime = LocalDateTime.parse(str + "T00:00:00",
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        dates.add(dateTime);
                        detectedFormats.add("date-only");
                        break;
                    }
                } catch (DateTimeParseException e) {
                    // 继续尝试下一个格式
                }
            }
        }

        if (!dates.isEmpty()) {
            LocalDateTime minDate = dates.stream().min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime maxDate = dates.stream().max(LocalDateTime::compareTo).orElse(null);

            builder.minDate(minDate)
                    .maxDate(maxDate)
                    .dateFormats(new ArrayList<>(detectedFormats));
        }
    }

    /**
     * 计算布尔类型统计（复用字符串统计）
     */
    private void calculateBooleanStatistics(FieldStatistics.FieldStatisticsBuilder builder, List<Object> values) {
        // 布尔类型主要关注分布，使用字符串长度统计
        calculateStringStatistics(builder, values);
    }

    /**
     * 计算高频值
     */
    private List<FieldStatistics.FrequencyItem> calculateTopValues(List<Object> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用采样来提高性能
        List<Object> sampleValues = values.size() > SAMPLE_SIZE
                ? values.stream().limit(SAMPLE_SIZE).toList()
                : values;

        Map<String, Integer> frequency = new HashMap<>();
        for (Object value : sampleValues) {
            String strValue = value.toString();
            frequency.put(strValue, frequency.getOrDefault(strValue, 0) + 1);
        }

        return frequency.entrySet().stream()
                .map(entry -> {
                    double freq = (double) entry.getValue() / sampleValues.size();
                    return FieldStatistics.FrequencyItem.builder()
                            .value(entry.getKey())
                            .count(entry.getValue())
                            .frequency(freq)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .limit(TOP_VALUES_LIMIT)
                .toList();
    }

    /**
     * 计算数据质量评分
     */
    private int calculateQualityScore(double nullRatio, double uniqueRatio, int nonNullCount, FieldType fieldType) {
        int score = 100;

        // 空值率扣分
        if (nullRatio > 0.5) {
            score -= 30;
        } else if (nullRatio > 0.2) {
            score -= 15;
        } else if (nullRatio > 0.05) {
            score -= 5;
        }

        // 数据量扣分
        if (nonNullCount < 10) {
            score -= 20;
        } else if (nonNullCount < 100) {
            score -= 10;
        }

        // 唯一性评分（根据字段类型调整）
        switch (fieldType) {
            case STRING, EMAIL, URL -> {
                // 字符串类型，适中的唯一性比较好
                if (uniqueRatio < 0.1 || uniqueRatio > 0.9) {
                    score -= 10;
                }
            }
            case INTEGER, FLOAT -> {
                // 数值类型，高唯一性通常更好
                if (uniqueRatio < 0.3) {
                    score -= 15;
                }
            }
            case BOOLEAN -> {
                // 布尔类型，唯一性应该很低
                if (uniqueRatio > 0.1) {
                    score -= 20;
                }
            }
            case DATE -> {
                // 日期类型，中等唯一性
                if (uniqueRatio < 0.2) {
                    score -= 10;
                }
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 检测异常值
     */
    private boolean detectOutliers(List<Object> values, FieldType fieldType) {
        if (values.isEmpty()) {
            return false;
        }

        switch (fieldType) {
            case INTEGER, FLOAT -> {
                return detectNumericOutliers(values);
            }
            case STRING, EMAIL, URL -> {
                return detectStringOutliers(values);
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * 检测数值异常值
     */
    private boolean detectNumericOutliers(List<Object> values) {
        List<BigDecimal> numbers = new ArrayList<>();

        for (Object value : values) {
            try {
                BigDecimal number = new BigDecimal(value.toString().trim());
                numbers.add(number);
            } catch (NumberFormatException e) {
                // 解析失败本身就是一种异常
                return true;
            }
        }

        if (numbers.size() < 3) {
            return false;
        }

        // 使用四分位数方法检测异常值
        numbers.sort(BigDecimal::compareTo);
        int size = numbers.size();
        int q1Index = size / 4;
        int q3Index = size * 3 / 4;

        if (q1Index >= size || q3Index >= size) {
            return false;
        }

        BigDecimal q1 = numbers.get(q1Index);
        BigDecimal q3 = numbers.get(q3Index);
        BigDecimal iqr = q3.subtract(q1);
        BigDecimal lowerBound = q1.subtract(iqr.multiply(new BigDecimal("1.5")));
        BigDecimal upperBound = q3.add(iqr.multiply(new BigDecimal("1.5")));

        return numbers.stream().anyMatch(n ->
                n.compareTo(lowerBound) < 0 || n.compareTo(upperBound) > 0
        );
    }

    /**
     * 检测字符串异常值
     */
    private boolean detectStringOutliers(List<Object> values) {
        List<Integer> lengths = values.stream()
                .map(v -> v.toString().length())
                .sorted()
                .toList();

        if (lengths.size() < 3) {
            return false;
        }

        // 检查长度分布是否有明显异常
        double avgLength = lengths.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int maxLength = lengths.stream().mapToInt(Integer::intValue).max().orElse(0);

        // 如果最大长度超过平均长度的5倍，认为有异常值
        return maxLength > avgLength * 5;
    }
}