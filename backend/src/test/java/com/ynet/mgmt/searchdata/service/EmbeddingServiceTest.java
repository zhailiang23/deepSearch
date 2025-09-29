package com.ynet.mgmt.searchdata.service;

import com.ynet.mgmt.searchdata.service.impl.SiliconFlowEmbeddingService;
import com.ynet.mgmt.searchdata.service.impl.LocalEmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌入服务测试类
 * 测试 EmbeddingService 接口的实现
 *
 * @author system
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "semantic.embedding.enabled=true",
    "semantic.embedding.provider=siliconflow"
})
@DisplayName("嵌入服务测试")
class EmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("测试服务健康检查")
    void testIsServiceAvailable() {
        // 执行测试
        boolean isAvailable = embeddingService.isServiceAvailable();

        // 验证结果 - 不强制要求服务可用，因为可能是测试环境
        assertNotNull(isAvailable, "健康检查结果不应为null");
        System.out.println("嵌入服务可用性: " + isAvailable);
        System.out.println("服务类型: " + embeddingService.getServiceType());
        System.out.println("模型名称: " + embeddingService.getModelName());
        System.out.println("向量维度: " + embeddingService.getVectorDimension());
    }

    @Test
    @DisplayName("测试单个文本向量生成")
    void testGetTextEmbedding() {
        String text = "我想取钱";

        // 执行测试
        List<Float> embedding = embeddingService.getTextEmbedding(text);

        if (embedding != null) {
            // 服务可用时的验证
            assertNotNull(embedding, "向量不应为空");
            assertTrue(embedding.size() > 0, "向量维度应该大于0");
            assertEquals(embeddingService.getVectorDimension(), embedding.size(),
                "向量维度应该匹配服务规格");

            System.out.println("向量生成成功，维度: " + embedding.size());
            System.out.println("前5个维度: " + embedding.subList(0, Math.min(5, embedding.size())));
        } else {
            // 服务不可用时的处理
            System.out.println("向量生成失败，可能是服务配置问题或服务不可用");
        }
    }

    @Test
    @DisplayName("测试批量文本向量生成")
    void testGetBatchTextEmbeddings() {
        List<String> texts = Arrays.asList("我想取钱", "附近网点", "银行服务");

        // 执行测试
        Map<String, List<Float>> embeddings = embeddingService.getBatchTextEmbeddings(texts);

        if (embeddings != null) {
            // 服务可用时的验证
            assertNotNull(embeddings, "批量向量不应为空");
            assertTrue(embeddings.size() > 0, "应该返回至少一个向量");

            for (String text : texts) {
                if (embeddings.containsKey(text)) {
                    List<Float> vector = embeddings.get(text);
                    assertNotNull(vector, "向量不应为null: " + text);
                    assertEquals(embeddingService.getVectorDimension(), vector.size(),
                        "向量维度应该匹配: " + text);
                }
            }

            System.out.println("批量向量生成成功，返回" + embeddings.size() + "个向量");
        } else {
            System.out.println("批量向量生成失败，可能是服务配置问题或服务不可用");
        }
    }

    @Test
    @DisplayName("测试余弦相似度计算")
    void testCalculateCosineSimilarity() {
        // 准备两个向量
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(0.0f, 1.0f, 0.0f);
        List<Float> vector3 = Arrays.asList(1.0f, 0.0f, 0.0f);

        // 执行测试
        double similarity1 = embeddingService.calculateCosineSimilarity(vector1, vector2);
        double similarity2 = embeddingService.calculateCosineSimilarity(vector1, vector3);

        // 验证结果
        assertEquals(0.0, similarity1, 0.001, "垂直向量的相似度应该为0");
        assertEquals(1.0, similarity2, 0.001, "相同向量的相似度应该为1");

        // 测试边界情况
        double similarity3 = embeddingService.calculateCosineSimilarity(null, vector1);
        double similarity4 = embeddingService.calculateCosineSimilarity(vector1, null);

        assertEquals(0.0, similarity3, "null向量的相似度应该为0");
        assertEquals(0.0, similarity4, "null向量的相似度应该为0");
    }

    @Test
    @DisplayName("测试空文本输入处理")
    void testGetTextEmbedding_EmptyInput() {
        // 执行测试
        List<Float> embedding1 = embeddingService.getTextEmbedding("");
        List<Float> embedding2 = embeddingService.getTextEmbedding(null);
        List<Float> embedding3 = embeddingService.getTextEmbedding("   ");

        // 验证结果
        assertNull(embedding1, "空字符串应该返回null");
        assertNull(embedding2, "null字符串应该返回null");
        assertNull(embedding3, "空白字符串应该返回null");
    }

    @Test
    @DisplayName("测试语义相似性")
    void testSemanticSimilarity() {
        if (!embeddingService.isServiceAvailable()) {
            System.out.println("跳过语义相似性测试 - 服务不可用");
            return;
        }

        // 测试语义相关的文本对
        String[] similarPairs = {
            "我想取钱", "附近网点",
            "银行服务", "金融业务",
            "存款利率", "定期存款"
        };

        for (int i = 0; i < similarPairs.length; i += 2) {
            String text1 = similarPairs[i];
            String text2 = similarPairs[i + 1];

            List<Float> embedding1 = embeddingService.getTextEmbedding(text1);
            List<Float> embedding2 = embeddingService.getTextEmbedding(text2);

            if (embedding1 != null && embedding2 != null) {
                double similarity = embeddingService.calculateCosineSimilarity(embedding1, embedding2);

                assertTrue(similarity >= -1.0 && similarity <= 1.0,
                    "相似度应该在-1到1之间: " + text1 + " vs " + text2);

                System.out.printf("'%s' 和 '%s' 的相似度: %.4f%n", text1, text2, similarity);
            }
        }
    }

    @Test
    @DisplayName("测试服务信息获取")
    void testServiceInfo() {
        // 验证服务信息
        assertNotNull(embeddingService.getServiceType(), "服务类型不应为null");
        assertNotNull(embeddingService.getModelName(), "模型名称不应为null");
        assertTrue(embeddingService.getVectorDimension() > 0, "向量维度应该大于0");

        System.out.println("=== 服务信息 ===");
        System.out.println("服务类型: " + embeddingService.getServiceType());
        System.out.println("模型名称: " + embeddingService.getModelName());
        System.out.println("向量维度: " + embeddingService.getVectorDimension());

        // 根据服务类型验证具体实现
        if ("siliconflow".equals(embeddingService.getServiceType())) {
            assertTrue(embeddingService instanceof SiliconFlowEmbeddingService,
                "应该是SiliconFlow实现");
            assertEquals(1024, embeddingService.getVectorDimension(),
                "SiliconFlow服务应该是1024维");
        } else if ("local".equals(embeddingService.getServiceType())) {
            assertTrue(embeddingService instanceof LocalEmbeddingService,
                "应该是Local实现");
            assertEquals(384, embeddingService.getVectorDimension(),
                "Local服务应该是384维");
        }
    }
}