package com.ynet.mgmt.searchspace.repository;

import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchSpaceRepository测试
 * 测试新增的JSON导入相关查询方法
 *
 * @author system
 * @since 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SearchSpaceRepository测试")
class SearchSpaceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SearchSpaceRepository repository;

    private SearchSpace space1, space2, space3;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        // 创建测试数据
        space1 = new SearchSpace("空间1", "space1");
        space1.setIndexMapping("{\"properties\":{\"title\":{\"type\":\"text\"}}}");
        space1.setDocumentCount(100L);
        space1.setLastImportTime(now.minusDays(2));
        // 设置审计字段
        space1.setCreatedAt(now.minusDays(10));
        space1.setUpdatedAt(now.minusDays(2));
        space1 = entityManager.persistAndFlush(space1);

        space2 = new SearchSpace("空间2", "space2");
        space2.setDocumentCount(200L);
        space2.setLastImportTime(now.minusDays(1));
        // 设置审计字段
        space2.setCreatedAt(now.minusDays(8));
        space2.setUpdatedAt(now.minusDays(1));
        space2 = entityManager.persistAndFlush(space2);

        space3 = new SearchSpace("空间3", "space3");
        space3.setIndexMapping("{\"mappings\":{\"properties\":{\"content\":{\"type\":\"text\"}}}}");
        // 设置审计字段
        space3.setCreatedAt(now.minusDays(5));
        space3.setUpdatedAt(now.minusDays(5));
        space3 = entityManager.persistAndFlush(space3);

        entityManager.clear();
    }

    @Test
    @DisplayName("测试findAllWithIndexMapping - 查找有索引映射的搜索空间")
    void testFindAllWithIndexMapping() {
        List<SearchSpace> spacesWithMapping = repository.findAllWithIndexMapping();

        assertEquals(2, spacesWithMapping.size(), "应该找到2个有索引映射的搜索空间");
        assertTrue(spacesWithMapping.stream().anyMatch(s -> s.getCode().equals("space1")),
                "应该包含space1");
        assertTrue(spacesWithMapping.stream().anyMatch(s -> s.getCode().equals("space3")),
                "应该包含space3");
        assertFalse(spacesWithMapping.stream().anyMatch(s -> s.getCode().equals("space2")),
                "不应该包含space2");
    }

    @Test
    @DisplayName("测试findAllWithImportedDocuments - 查找有导入文档的搜索空间")
    void testFindAllWithImportedDocuments() {
        List<SearchSpace> spacesWithDocuments = repository.findAllWithImportedDocuments();

        assertEquals(2, spacesWithDocuments.size(), "应该找到2个有导入文档的搜索空间");
        assertTrue(spacesWithDocuments.stream().anyMatch(s -> s.getCode().equals("space1")),
                "应该包含space1");
        assertTrue(spacesWithDocuments.stream().anyMatch(s -> s.getCode().equals("space2")),
                "应该包含space2");
        assertFalse(spacesWithDocuments.stream().anyMatch(s -> s.getCode().equals("space3")),
                "不应该包含space3");
    }

    @Test
    @DisplayName("测试countTotalImportedDocuments - 统计总导入文档数量")
    void testCountTotalImportedDocuments() {
        Long totalDocuments = repository.countTotalImportedDocuments();

        assertEquals(300L, totalDocuments, "总文档数量应该为300 (100+200)");
    }

    @Test
    @DisplayName("测试countSpacesWithIndexMapping - 统计有索引映射的搜索空间数量")
    void testCountSpacesWithIndexMapping() {
        Long count = repository.countSpacesWithIndexMapping();

        assertEquals(2L, count, "有索引映射的搜索空间数量应该为2");
    }

    @Test
    @DisplayName("测试countSpacesWithImportedDocuments - 统计有导入文档的搜索空间数量")
    void testCountSpacesWithImportedDocuments() {
        Long count = repository.countSpacesWithImportedDocuments();

        assertEquals(2L, count, "有导入文档的搜索空间数量应该为2");
    }

    @Test
    @DisplayName("测试findLastImportTime - 查找最后导入时间")
    void testFindLastImportTime() {
        Optional<LocalDateTime> lastImportTime = repository.findLastImportTime();

        assertTrue(lastImportTime.isPresent(), "应该找到最后导入时间");
        assertEquals(space2.getLastImportTime(), lastImportTime.get(),
                "最后导入时间应该是space2的导入时间");
    }

    @Test
    @DisplayName("测试findRecentlyImported - 查找最近导入的搜索空间")
    void testFindRecentlyImported() {
        Pageable pageable = PageRequest.of(0, 10);
        List<SearchSpace> recentlyImported = repository.findRecentlyImported(pageable);

        assertEquals(2, recentlyImported.size(), "应该找到2个最近导入的搜索空间");
        assertEquals("space2", recentlyImported.get(0).getCode(),
                "第一个应该是space2（最近导入）");
        assertEquals("space1", recentlyImported.get(1).getCode(),
                "第二个应该是space1");
    }

    @Test
    @DisplayName("测试空数据情况")
    void testEmptyData() {
        // 清空所有数据
        repository.deleteAll();
        entityManager.flush();

        // 测试各种查询在空数据情况下的表现
        assertEquals(0, repository.findAllWithIndexMapping().size(),
                "空数据时有映射的搜索空间数量应该为0");
        assertEquals(0, repository.findAllWithImportedDocuments().size(),
                "空数据时有文档的搜索空间数量应该为0");
        assertEquals(0L, repository.countTotalImportedDocuments(),
                "空数据时总文档数量应该为0");
        assertEquals(0L, repository.countSpacesWithIndexMapping(),
                "空数据时有映射的搜索空间数量应该为0");
        assertEquals(0L, repository.countSpacesWithImportedDocuments(),
                "空数据时有文档的搜索空间数量应该为0");
        assertFalse(repository.findLastImportTime().isPresent(),
                "空数据时应该没有最后导入时间");
        assertEquals(0, repository.findRecentlyImported(PageRequest.of(0, 10)).size(),
                "空数据时最近导入的搜索空间数量应该为0");
    }

    @Test
    @DisplayName("测试边界情况 - 文档数量为0")
    void testBoundaryCase_ZeroDocuments() {
        // 创建文档数量为0的搜索空间
        SearchSpace zeroDocSpace = new SearchSpace("零文档空间", "zero_space");
        zeroDocSpace.setDocumentCount(0L);
        // 设置审计字段
        zeroDocSpace.setCreatedAt(LocalDateTime.now());
        zeroDocSpace.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(zeroDocSpace);

        // 查询有导入文档的搜索空间，不应该包含文档数量为0的空间
        List<SearchSpace> spacesWithDocuments = repository.findAllWithImportedDocuments();
        assertFalse(spacesWithDocuments.stream().anyMatch(s -> s.getCode().equals("zero_space")),
                "文档数量为0的搜索空间不应该被查询出来");
    }

    @Test
    @DisplayName("测试边界情况 - 空索引映射")
    void testBoundaryCase_EmptyMapping() {
        // 创建空索引映射的搜索空间
        SearchSpace emptyMappingSpace = new SearchSpace("空映射空间", "empty_mapping_space");
        emptyMappingSpace.setIndexMapping("");
        // 设置审计字段
        emptyMappingSpace.setCreatedAt(LocalDateTime.now());
        emptyMappingSpace.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(emptyMappingSpace);

        // 查询有索引映射的搜索空间，不应该包含空映射的空间
        List<SearchSpace> spacesWithMapping = repository.findAllWithIndexMapping();
        assertFalse(spacesWithMapping.stream().anyMatch(s -> s.getCode().equals("empty_mapping_space")),
                "空索引映射的搜索空间不应该被查询出来");
    }

    @Test
    @DisplayName("测试数据完整性 - 新增字段的持久化")
    void testDataIntegrity_NewFields() {
        // 创建包含所有新字段的搜索空间
        SearchSpace completeSpace = new SearchSpace("完整空间", "complete_space");
        String mapping = "{\"properties\":{\"field1\":{\"type\":\"keyword\"}}}";
        LocalDateTime importTime = LocalDateTime.now();

        completeSpace.setIndexMapping(mapping);
        completeSpace.setDocumentCount(500L);
        completeSpace.setLastImportTime(importTime);
        // 设置审计字段
        completeSpace.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        completeSpace.setUpdatedAt(LocalDateTime.now().minusMinutes(5));

        SearchSpace saved = repository.saveAndFlush(completeSpace);
        entityManager.clear();

        // 重新查询验证数据完整性
        Optional<SearchSpace> retrieved = repository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "应该能找到保存的搜索空间");

        SearchSpace space = retrieved.get();
        assertEquals(mapping, space.getIndexMapping(), "索引映射应该正确保存");
        assertEquals(500L, space.getDocumentCount(), "文档数量应该正确保存");
        assertEquals(importTime.withNano(0), space.getLastImportTime().withNano(0),
                "最后导入时间应该正确保存（忽略纳秒精度）");
    }

    @Test
    @DisplayName("测试分页查询最近导入")
    void testFindRecentlyImported_Pagination() {
        LocalDateTime now = LocalDateTime.now();
        
        // 添加更多测试数据
        for (int i = 4; i <= 6; i++) {
            SearchSpace space = new SearchSpace("空间" + i, "space" + i);
            space.setDocumentCount(100L);
            space.setLastImportTime(now.minusHours(i));
            // 设置审计字段
            space.setCreatedAt(now.minusDays(i));
            space.setUpdatedAt(now.minusHours(i));
            entityManager.persistAndFlush(space);
        }

        // 测试分页
        List<SearchSpace> firstPage = repository.findRecentlyImported(PageRequest.of(0, 2));
        assertEquals(2, firstPage.size(), "第一页应该有2条记录");

        List<SearchSpace> secondPage = repository.findRecentlyImported(PageRequest.of(1, 2));
        assertEquals(2, secondPage.size(), "第二页应该有2条记录");

        // 验证排序（最近的在前）
        assertTrue(firstPage.get(0).getLastImportTime().isAfter(firstPage.get(1).getLastImportTime()),
                "第一页数据应该按导入时间降序排列");
    }
}