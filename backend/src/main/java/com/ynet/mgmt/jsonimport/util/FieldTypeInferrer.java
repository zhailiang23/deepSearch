package com.ynet.mgmt.jsonimport.util;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import com.ynet.mgmt.jsonimport.model.FieldAnalysisResult.TypeCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 字段类型推断工具类
 * 实现基于样本数据的智能类型推断算法
 */
@Component
@Slf4j
public class FieldTypeInferrer {

    // 邮箱格式正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    // URL格式正则
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            Pattern.CASE_INSENSITIVE
    );

    // 布尔值的各种表示形式
    private static final Set<String> BOOLEAN_TRUE_VALUES = Set.of(
            "true", "1", "yes", "y", "on", "enabled", "active"
    );

    private static final Set<String> BOOLEAN_FALSE_VALUES = Set.of(
            "false", "0", "no", "n", "off", "disabled", "inactive"
    );

    // 常见日期时间格式
    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy")
    );

    /**
     * 推断字段类型
     *
     * @param values 字段的所有值
     * @return 推断结果，包含类型和置信度
     */
    public TypeInferenceResult inferFieldType(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return new TypeInferenceResult(FieldType.UNKNOWN, 0.0, Collections.emptyList());
        }

        // 过滤空值
        List<Object> nonNullValues = values.stream()
                .filter(Objects::nonNull)
                .filter(v -> !v.toString().trim().isEmpty())
                .toList();

        if (nonNullValues.isEmpty()) {
            return new TypeInferenceResult(FieldType.STRING, 0.1, Collections.emptyList());
        }

        // 生成各类型的候选结果
        Map<FieldType, TypeCandidate> candidates = new HashMap<>();

        // 测试各种类型
        candidates.put(FieldType.BOOLEAN, testBoolean(nonNullValues));
        candidates.put(FieldType.INTEGER, testInteger(nonNullValues));
        candidates.put(FieldType.FLOAT, testFloat(nonNullValues));
        candidates.put(FieldType.DATE, testDate(nonNullValues));
        candidates.put(FieldType.EMAIL, testEmail(nonNullValues));
        candidates.put(FieldType.URL, testUrl(nonNullValues));

        // 移除置信度为0的候选项
        candidates.entrySet().removeIf(entry -> entry.getValue().getConfidence() == 0.0);

        // 如果没有合适的候选项，默认为字符串
        if (candidates.isEmpty()) {
            return new TypeInferenceResult(
                    FieldType.STRING,
                    1.0,
                    List.of(TypeCandidate.builder()
                            .type(FieldType.STRING)
                            .confidence(1.0)
                            .matchCount(nonNullValues.size())
                            .reason("默认类型")
                            .build())
            );
        }

        // 选择置信度最高的类型
        TypeCandidate bestCandidate = candidates.values().stream()
                .max(Comparator.comparingDouble(TypeCandidate::getConfidence))
                .orElse(null);

        // 构建候选列表（按置信度降序）
        List<TypeCandidate> candidateList = candidates.values().stream()
                .sorted(Comparator.comparingDouble(TypeCandidate::getConfidence).reversed())
                .toList();

        return new TypeInferenceResult(
                bestCandidate.getType(),
                bestCandidate.getConfidence(),
                candidateList
        );
    }

    /**
     * 测试布尔类型
     */
    private TypeCandidate testBoolean(List<Object> values) {
        int matchCount = 0;

        for (Object value : values) {
            String str = value.toString().toLowerCase().trim();
            if (BOOLEAN_TRUE_VALUES.contains(str) || BOOLEAN_FALSE_VALUES.contains(str)) {
                matchCount++;
            }
        }

        double confidence = (double) matchCount / values.size();
        return TypeCandidate.builder()
                .type(FieldType.BOOLEAN)
                .confidence(confidence)
                .matchCount(matchCount)
                .reason(String.format("匹配布尔值格式: %d/%d", matchCount, values.size()))
                .build();
    }

    /**
     * 测试整数类型
     */
    private TypeCandidate testInteger(List<Object> values) {
        int matchCount = 0;

        for (Object value : values) {
            try {
                String str = value.toString().trim();
                // 排除小数点
                if (str.contains(".")) {
                    continue;
                }
                Long.parseLong(str);
                matchCount++;
            } catch (NumberFormatException e) {
                // 解析失败，继续下一个
            }
        }

        double confidence = (double) matchCount / values.size();
        // 整数类型需要较高的匹配率才认为可信
        if (confidence < 0.8) {
            confidence = 0.0;
        }

        return TypeCandidate.builder()
                .type(FieldType.INTEGER)
                .confidence(confidence)
                .matchCount(matchCount)
                .reason(String.format("匹配整数格式: %d/%d", matchCount, values.size()))
                .build();
    }

    /**
     * 测试浮点数类型
     */
    private TypeCandidate testFloat(List<Object> values) {
        int matchCount = 0;

        for (Object value : values) {
            try {
                String str = value.toString().trim();
                new BigDecimal(str);
                matchCount++;
            } catch (NumberFormatException e) {
                // 解析失败，继续下一个
            }
        }

        double confidence = (double) matchCount / values.size();
        // 如果都能解析为数字，但已经有更高置信度的整数类型，则降低浮点数置信度
        if (confidence < 0.8) {
            confidence = 0.0;
        }

        return TypeCandidate.builder()
                .type(FieldType.FLOAT)
                .confidence(confidence)
                .matchCount(matchCount)
                .reason(String.format("匹配数值格式: %d/%d", matchCount, values.size()))
                .build();
    }

    /**
     * 测试日期类型
     */
    private TypeCandidate testDate(List<Object> values) {
        int matchCount = 0;
        List<String> detectedFormats = new ArrayList<>();

        for (Object value : values) {
            String str = value.toString().trim();

            // 首先检查是否为时间戳
            if (isTimestamp(str)) {
                matchCount++;
                continue;
            }

            // 尝试各种日期格式
            boolean parsed = false;
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    formatter.parse(str);
                    matchCount++;
                    detectedFormats.add(formatter.toString());
                    parsed = true;
                    break;
                } catch (DateTimeParseException e) {
                    // 继续尝试下一个格式
                }
            }
        }

        double confidence = (double) matchCount / values.size();
        // 日期类型需要较高的匹配率
        if (confidence < 0.7) {
            confidence = 0.0;
        }

        String reason = String.format("匹配日期格式: %d/%d", matchCount, values.size());
        if (!detectedFormats.isEmpty()) {
            reason += " (格式: " + String.join(", ", detectedFormats) + ")";
        }

        return TypeCandidate.builder()
                .type(FieldType.DATE)
                .confidence(confidence)
                .matchCount(matchCount)
                .reason(reason)
                .build();
    }

    /**
     * 测试邮箱类型
     */
    private TypeCandidate testEmail(List<Object> values) {
        int matchCount = 0;

        for (Object value : values) {
            String str = value.toString().trim();
            if (EMAIL_PATTERN.matcher(str).matches()) {
                matchCount++;
            }
        }

        double confidence = (double) matchCount / values.size();
        // 邮箱类型需要非常高的匹配率
        if (confidence < 0.9) {
            confidence = 0.0;
        }

        return TypeCandidate.builder()
                .type(FieldType.EMAIL)
                .confidence(confidence)
                .matchCount(matchCount)
                .reason(String.format("匹配邮箱格式: %d/%d", matchCount, values.size()))
                .build();
    }

    /**
     * 测试URL类型
     */
    private TypeCandidate testUrl(List<Object> values) {
        int matchCount = 0;

        for (Object value : values) {
            String str = value.toString().trim();
            if (URL_PATTERN.matcher(str).matches()) {
                matchCount++;
            }
        }

        double confidence = (double) matchCount / values.size();
        // URL类型需要非常高的匹配率
        if (confidence < 0.9) {
            confidence = 0.0;
        }

        return TypeCandidate.builder()
                .type(FieldType.URL)
                .confidence(confidence)
                .matchCount(matchCount)
                .reason(String.format("匹配URL格式: %d/%d", matchCount, values.size()))
                .build();
    }

    /**
     * 检查字符串是否为时间戳
     */
    private boolean isTimestamp(String str) {
        try {
            long timestamp = Long.parseLong(str);
            // 检查时间戳范围是否合理（1970-2050年之间）
            return timestamp > 0 && timestamp < 2524608000000L; // 2050年的毫秒时间戳
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 类型推断结果
     */
    public static class TypeInferenceResult {
        private final FieldType type;
        private final double confidence;
        private final List<TypeCandidate> candidates;

        public TypeInferenceResult(FieldType type, double confidence, List<TypeCandidate> candidates) {
            this.type = type;
            this.confidence = confidence;
            this.candidates = candidates;
        }

        public FieldType getType() {
            return type;
        }

        public double getConfidence() {
            return confidence;
        }

        public List<TypeCandidate> getCandidates() {
            return candidates;
        }
    }
}