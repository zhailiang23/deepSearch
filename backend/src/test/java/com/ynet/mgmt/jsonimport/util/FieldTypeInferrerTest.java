package com.ynet.mgmt.jsonimport.util;

import com.ynet.mgmt.jsonimport.enums.FieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("字段类型推断器测试")
class FieldTypeInferrerTest {

    private FieldTypeInferrer fieldTypeInferrer;

    @BeforeEach
    void setUp() {
        fieldTypeInferrer = new FieldTypeInferrer();
    }

    @Test
    @DisplayName("推断整数类型")
    void testInferIntegerType() {
        List<Object> values = Arrays.asList("123", "456", "789", "0", "-100");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.INTEGER, result.getType());
        assertTrue(result.getConfidence() >= 0.8, "整数类型置信度应该很高");
        assertFalse(result.getCandidates().isEmpty());
    }

    @Test
    @DisplayName("推断浮点数类型")
    void testInferFloatType() {
        List<Object> values = Arrays.asList("123.45", "456.78", "789.12", "0.0", "-100.5");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.FLOAT, result.getType());
        assertTrue(result.getConfidence() >= 0.8, "浮点数类型置信度应该很高");
    }

    @Test
    @DisplayName("推断布尔类型")
    void testInferBooleanType() {
        List<Object> values = Arrays.asList("true", "false", "true", "false", "1", "0");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.BOOLEAN, result.getType());
        assertTrue(result.getConfidence() > 0.5, "布尔类型应该被识别");
    }

    @Test
    @DisplayName("推断邮箱类型")
    void testInferEmailType() {
        List<Object> values = Arrays.asList(
                "user@example.com",
                "admin@test.org",
                "support@company.net",
                "info@domain.co.uk"
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.EMAIL, result.getType());
        assertTrue(result.getConfidence() >= 0.9, "邮箱类型需要高置信度");
    }

    @Test
    @DisplayName("推断URL类型")
    void testInferUrlType() {
        List<Object> values = Arrays.asList(
                "https://www.example.com",
                "http://test.org/path",
                "https://api.service.com/v1",
                "ftp://files.company.net"
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.URL, result.getType());
        assertTrue(result.getConfidence() >= 0.9, "URL类型需要高置信度");
    }

    @Test
    @DisplayName("推断日期类型")
    void testInferDateType() {
        List<Object> values = Arrays.asList(
                "2023-12-01",
                "2023-12-02",
                "2023-12-03T10:30:00",
                "2023/12/04 15:45:30"
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.DATE, result.getType());
        assertTrue(result.getConfidence() >= 0.7, "日期类型应该被识别");
    }

    @Test
    @DisplayName("推断时间戳类型")
    void testInferTimestampType() {
        List<Object> values = Arrays.asList(
                "1640995200", // 2022-01-01的时间戳
                "1641081600", // 2022-01-02的时间戳
                "1641168000", // 2022-01-03的时间戳
                "1641254400"  // 2022-01-04的时间戳
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        // 时间戳可能被识别为DATE或INTEGER，都是合理的
        assertTrue(result.getType() == FieldType.DATE || result.getType() == FieldType.INTEGER);
    }

    @Test
    @DisplayName("混合类型数据 - 应该选择置信度最高的")
    void testMixedTypeData() {
        List<Object> values = Arrays.asList(
                "123", "456", "789", // 整数
                "not_a_number", "text", // 字符串
                "999" // 整数
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        // 应该选择字符串类型，因为不是所有值都能解析为数字
        assertTrue(result.getType() == FieldType.STRING || result.getType() == FieldType.INTEGER);
        assertFalse(result.getCandidates().isEmpty());
    }

    @Test
    @DisplayName("空值处理")
    void testNullValues() {
        List<Object> values = Arrays.asList(null, "", "  ", "123", "456");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        // 应该基于非空值推断类型
        assertEquals(FieldType.INTEGER, result.getType());
    }

    @Test
    @DisplayName("全空数据")
    void testAllNullValues() {
        List<Object> values = Arrays.asList(null, "", "  ");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.STRING, result.getType());
        assertTrue(result.getConfidence() < 0.5);
    }

    @Test
    @DisplayName("空列表处理")
    void testEmptyList() {
        List<Object> values = Collections.emptyList();

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.UNKNOWN, result.getType());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    @DisplayName("布尔值的各种表示形式")
    void testVariousBooleanFormats() {
        List<Object> values = Arrays.asList(
                "true", "false", "TRUE", "FALSE",
                "1", "0", "yes", "no", "Y", "N",
                "on", "off", "enabled", "disabled"
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.BOOLEAN, result.getType());
    }

    @Test
    @DisplayName("科学计数法数字")
    void testScientificNotation() {
        List<Object> values = Arrays.asList(
                "1.23E+10", "4.56E-5", "7.89e12", "1.0E0"
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.FLOAT, result.getType());
    }

    @Test
    @DisplayName("负数处理")
    void testNegativeNumbers() {
        List<Object> values = Arrays.asList("-123", "-456.78", "-0", "-999");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertTrue(result.getType() == FieldType.INTEGER || result.getType() == FieldType.FLOAT);
    }

    @Test
    @DisplayName("类型推断候选项验证")
    void testTypeCandidates() {
        List<Object> values = Arrays.asList("123", "456", "789");

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertFalse(result.getCandidates().isEmpty());

        // 验证候选项按置信度降序排列
        for (int i = 0; i < result.getCandidates().size() - 1; i++) {
            assertTrue(result.getCandidates().get(i).getConfidence()
                      >= result.getCandidates().get(i + 1).getConfidence());
        }

        // 验证最佳候选项与结果类型一致
        assertEquals(result.getType(), result.getCandidates().get(0).getType());
    }

    @Test
    @DisplayName("边界情况 - 特殊字符串")
    void testSpecialStrings() {
        List<Object> values = Arrays.asList(
                "null", "undefined", "NaN", "Infinity", "-Infinity"
        );

        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);

        assertEquals(FieldType.STRING, result.getType());
    }

    @Test
    @DisplayName("大数据量性能测试")
    void testLargeDataSet() {
        // 创建1000个整数值
        List<Object> values = java.util.stream.IntStream.range(1, 1001)
                .boxed()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList());

        long startTime = System.currentTimeMillis();
        FieldTypeInferrer.TypeInferenceResult result = fieldTypeInferrer.inferFieldType(values);
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(FieldType.INTEGER, result.getType());
        assertTrue(duration < 1000, "推断1000个值应该在1秒内完成，实际耗时: " + duration + "ms");
    }
}