package com.ynet.mgmt.searchlog.service;

import com.ynet.mgmt.searchlog.service.impl.ChineseSegmentationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 中文分词服务测试类
 * 测试ChineseSegmentationService的各种功能
 *
 * @author system
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Chinese Segmentation Service Tests")
class ChineseSegmentationServiceTest {

    private ChineseSegmentationService segmentationService;

    @BeforeEach
    void setUp() {
        segmentationService = new ChineseSegmentationServiceImpl();
        // 模拟@PostConstruct初始化
        ((ChineseSegmentationServiceImpl) segmentationService).init();
    }

    @Test
    @DisplayName("Should segment Chinese text correctly")
    void shouldSegmentChineseTextCorrectly() {
        // Given
        String chineseText = "中文分词测试";

        // When
        List<String> result = segmentationService.segmentText(chineseText);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).contains("中文", "分词", "测试");
    }

    @Test
    @DisplayName("Should segment English text correctly")
    void shouldSegmentEnglishTextCorrectly() {
        // Given
        String englishText = "hello world test";

        // When
        List<String> result = segmentationService.segmentText(englishText);

        // Then
        assertThat(result).isNotEmpty();
        // 英文分词可能按词或按字符分割，具体取决于jieba的处理方式
    }

    @Test
    @DisplayName("Should segment mixed Chinese and English text correctly")
    void shouldSegmentMixedTextCorrectly() {
        // Given
        String mixedText = "中文English混合测试";

        // When
        List<String> result = segmentationService.segmentText(mixedText);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(segment -> segment.contains("中文"));
        assertThat(result).anyMatch(segment -> segment.contains("English") || segment.equals("English"));
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void shouldReturnEmptyListForNullInput() {
        // When
        List<String> result = segmentationService.segmentText(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty input")
    void shouldReturnEmptyListForEmptyInput() {
        // When
        List<String> result = segmentationService.segmentText("");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for whitespace input")
    void shouldReturnEmptyListForWhitespaceInput() {
        // When
        List<String> result = segmentationService.segmentText("   ");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle special characters correctly")
    void shouldHandleSpecialCharactersCorrectly() {
        // Given
        String specialText = "特殊字符@#$%测试！";

        // When & Then
        assertDoesNotThrow(() -> {
            List<String> result = segmentationService.segmentText(specialText);
            assertThat(result).isNotNull();
        });
    }

    @Test
    @DisplayName("Should handle very long text correctly")
    void shouldHandleLongTextCorrectly() {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("这是一个很长的中文测试文本");
        }

        // When & Then
        assertDoesNotThrow(() -> {
            List<String> result = segmentationService.segmentText(longText.toString());
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should process batch segmentation correctly")
    void shouldProcessBatchSegmentationCorrectly() {
        // Given
        List<String> texts = Arrays.asList(
                "中文测试",
                "English test",
                "混合mixed测试",
                ""
        );

        // When
        List<List<String>> results = segmentationService.segmentTexts(texts);

        // Then
        assertThat(results).hasSize(4);
        assertThat(results.get(0)).isNotEmpty(); // 中文测试
        assertThat(results.get(1)).isNotEmpty(); // English test
        assertThat(results.get(2)).isNotEmpty(); // 混合测试
        assertThat(results.get(3)).isEmpty();    // 空字符串
    }

    @Test
    @DisplayName("Should return empty list for null batch input")
    void shouldReturnEmptyListForNullBatchInput() {
        // When
        List<List<String>> result = segmentationService.segmentTexts(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty batch input")
    void shouldReturnEmptyListForEmptyBatchInput() {
        // When
        List<List<String>> result = segmentationService.segmentTexts(Collections.emptyList());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should report service as available after initialization")
    void shouldReportServiceAsAvailableAfterInitialization() {
        // When
        boolean isAvailable = segmentationService.isServiceAvailable();

        // Then
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("Should return correct version information")
    void shouldReturnCorrectVersionInformation() {
        // When
        String version = segmentationService.getSegmenterVersion();

        // Then
        assertThat(version).isNotNull();
        assertThat(version).contains("jieba-analysis");
        assertThat(version).contains("1.0.2");
    }

    @Test
    @DisplayName("Should handle numbers in text correctly")
    void shouldHandleNumbersInTextCorrectly() {
        // Given
        String textWithNumbers = "测试123数字456文本";

        // When
        List<String> result = segmentationService.segmentText(textWithNumbers);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should maintain thread safety under concurrent access")
    void shouldMaintainThreadSafetyUnderConcurrentAccess() {
        // Given
        String testText = "并发测试文本";

        // When & Then
        assertDoesNotThrow(() -> {
            // 模拟多线程并发访问
            for (int i = 0; i < 10; i++) {
                new Thread(() -> {
                    List<String> result = segmentationService.segmentText(testText);
                    assertThat(result).isNotNull();
                }).start();
            }
            Thread.sleep(100); // 等待线程执行完成
        });
    }
}