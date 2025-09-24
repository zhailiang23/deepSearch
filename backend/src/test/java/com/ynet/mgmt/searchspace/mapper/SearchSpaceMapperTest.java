package com.ynet.mgmt.searchspace.mapper;

import com.ynet.mgmt.searchspace.dto.CreateSearchSpaceRequest;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.entity.SearchSpaceStatus;
import com.ynet.mgmt.searchspace.model.IndexStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchSpaceMapper测试
 * 测试新增字段的映射功能
 *
 * @author system
 * @since 1.0.0
 */
@DisplayName("SearchSpaceMapper测试")
class SearchSpaceMapperTest {

    private SearchSpaceMapper mapper;
    private SearchSpace testEntity;
    private SearchSpaceDTO testDTO;

    @BeforeEach
    void setUp() {
        mapper = new SearchSpaceMapper();

        // 创建测试实体（包含所有新字段）
        testEntity = new SearchSpace("测试空间", "test_space");
        testEntity.setId(1L);
        testEntity.setDescription("测试描述");
        testEntity.setIndexMapping("{\"properties\":{\"title\":{\"type\":\"text\"}}}");
        testEntity.setDocumentCount(100L);
        testEntity.setLastImportTime(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        testEntity.setStatus(SearchSpaceStatus.ACTIVE);
        testEntity.setVersion(1L);
        testEntity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 9, 0, 0));
        testEntity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 10, 0, 0));

        // 创建测试DTO
        testDTO = new SearchSpaceDTO();
        testDTO.setId(2L);
        testDTO.setName("DTO测试空间");
        testDTO.setCode("dto_test_space");
        testDTO.setDescription("DTO测试描述");
        testDTO.setIndexMapping("{\"mappings\":{\"properties\":{\"content\":{\"type\":\"keyword\"}}}}");
        testDTO.setDocumentCount(200L);
        testDTO.setLastImportTime(LocalDateTime.of(2024, 2, 1, 14, 20, 0));
        testDTO.setStatus(SearchSpaceStatus.MAINTENANCE);
        testDTO.setVersion(2L);
    }

    @Test
    @DisplayName("测试实体转DTO - 包含新字段")
    void testEntityToDTO_WithNewFields() {
        SearchSpaceDTO dto = mapper.toDTO(testEntity);

        assertNotNull(dto, "DTO不应该为null");
        assertEquals(testEntity.getId(), dto.getId());
        assertEquals(testEntity.getName(), dto.getName());
        assertEquals(testEntity.getCode(), dto.getCode());
        assertEquals(testEntity.getDescription(), dto.getDescription());

        // 测试新字段映射
        assertEquals(testEntity.getIndexMapping(), dto.getIndexMapping(), "索引映射应该正确映射");
        assertEquals(testEntity.getDocumentCount(), dto.getDocumentCount(), "文档数量应该正确映射");
        assertEquals(testEntity.getLastImportTime(), dto.getLastImportTime(), "最后导入时间应该正确映射");

        assertEquals(testEntity.getStatus(), dto.getStatus());
        assertEquals(testEntity.getVersion(), dto.getVersion());
        assertEquals(testEntity.getCreatedAt(), dto.getCreatedAt());
        assertEquals(testEntity.getUpdatedAt(), dto.getUpdatedAt());
    }

    @Test
    @DisplayName("测试实体转DTO - null实体")
    void testEntityToDTO_NullEntity() {
        SearchSpaceDTO dto = mapper.toDTO(null);
        assertNull(dto, "null实体应该返回null DTO");
    }

    @Test
    @DisplayName("测试实体转DTO - 包含索引状态")
    void testEntityToDTO_WithIndexStatus() {
        IndexStatus indexStatus = IndexStatus.builder()
                .name("test_space")
                .exists(true)
                .health("green")
                .docsCount(150L)
                .build();

        SearchSpaceDTO dto = mapper.toDTO(testEntity, indexStatus);

        assertNotNull(dto, "DTO不应该为null");
        assertEquals(testEntity.getIndexMapping(), dto.getIndexMapping(), "新字段应该正确映射");
        assertEquals(indexStatus, dto.getIndexStatus(), "索引状态应该被设置");
    }

    @Test
    @DisplayName("测试DTO转实体 - 包含新字段")
    void testDTOToEntity_WithNewFields() {
        SearchSpace entity = mapper.toEntity(testDTO);

        assertNotNull(entity, "实体不应该为null");
        assertEquals(testDTO.getId(), entity.getId());
        assertEquals(testDTO.getName(), entity.getName());
        assertEquals(testDTO.getCode(), entity.getCode());
        assertEquals(testDTO.getDescription(), entity.getDescription());

        // 测试新字段映射
        assertEquals(testDTO.getIndexMapping(), entity.getIndexMapping(), "索引映射应该正确映射");
        assertEquals(testDTO.getDocumentCount(), entity.getDocumentCount(), "文档数量应该正确映射");
        assertEquals(testDTO.getLastImportTime(), entity.getLastImportTime(), "最后导入时间应该正确映射");

        assertEquals(testDTO.getStatus(), entity.getStatus());
        assertEquals(testDTO.getVersion(), entity.getVersion());
    }

    @Test
    @DisplayName("测试DTO转实体 - null DTO")
    void testDTOToEntity_NullDTO() {
        SearchSpace entity = mapper.toEntity((SearchSpaceDTO) null);
        assertNull(entity, "null DTO应该返回null实体");
    }

    @Test
    @DisplayName("测试创建请求转实体 - 新字段默认值")
    void testCreateRequestToEntity_NewFieldDefaults() {
        CreateSearchSpaceRequest request = new CreateSearchSpaceRequest();
        request.setName("新建空间");
        request.setCode("new_space");
        request.setDescription("新建描述");

        SearchSpace entity = mapper.toEntity(request);

        assertNotNull(entity, "实体不应该为null");
        assertEquals(request.getName(), entity.getName());
        assertEquals(request.getCode(), entity.getCode());
        assertEquals(request.getDescription(), entity.getDescription());

        // 测试新字段的默认值
        assertNull(entity.getIndexMapping(), "索引映射默认应该为null");
        assertEquals(0L, entity.getDocumentCount(), "文档数量默认应该为0");
        assertNull(entity.getLastImportTime(), "最后导入时间默认应该为null");
    }

    @Test
    @DisplayName("测试创建请求转实体 - null请求")
    void testCreateRequestToEntity_NullRequest() {
        SearchSpace entity = mapper.toEntity((CreateSearchSpaceRequest) null);
        assertNull(entity, "null请求应该返回null实体");
    }

    @Test
    @DisplayName("测试实体列表转DTO列表")
    void testEntityListToDTOList() {
        SearchSpace entity2 = new SearchSpace("空间2", "space2");
        entity2.setDocumentCount(50L);

        List<SearchSpace> entities = Arrays.asList(testEntity, entity2);
        List<SearchSpaceDTO> dtos = mapper.toDTOList(entities);

        assertNotNull(dtos, "DTO列表不应该为null");
        assertEquals(2, dtos.size(), "DTO列表大小应该正确");

        assertEquals(testEntity.getIndexMapping(), dtos.get(0).getIndexMapping(),
                "第一个DTO的索引映射应该正确");
        assertEquals(entity2.getDocumentCount(), dtos.get(1).getDocumentCount(),
                "第二个DTO的文档数量应该正确");
    }

    @Test
    @DisplayName("测试实体列表转DTO列表 - null列表")
    void testEntityListToDTOList_NullList() {
        List<SearchSpaceDTO> dtos = mapper.toDTOList(null);
        assertNull(dtos, "null列表应该返回null");
    }

    @Test
    @DisplayName("测试更新实体 - 不包含新字段")
    void testUpdateEntity_ExcludesNewFields() {
        SearchSpace source = new SearchSpace();
        source.setName("新名称");
        source.setDescription("新描述");
        source.setStatus(SearchSpaceStatus.INACTIVE);
        // 设置新字段（但不应该被更新）
        source.setIndexMapping("{\"new\":\"mapping\"}");
        source.setDocumentCount(999L);
        source.setLastImportTime(LocalDateTime.now());

        SearchSpace target = new SearchSpace("原名称", "original_code");
        target.setDescription("原描述");
        target.setIndexMapping("{\"original\":\"mapping\"}");
        target.setDocumentCount(100L);
        target.setLastImportTime(LocalDateTime.of(2024, 1, 1, 0, 0));

        mapper.updateEntity(source, target);

        // 基本字段应该被更新
        assertEquals("新名称", target.getName(), "名称应该被更新");
        assertEquals("新描述", target.getDescription(), "描述应该被更新");
        assertEquals(SearchSpaceStatus.INACTIVE, target.getStatus(), "状态应该被更新");

        // 新字段不应该被更新
        assertEquals("{\"original\":\"mapping\"}", target.getIndexMapping(),
                "索引映射不应该被部分更新方法修改");
        assertEquals(100L, target.getDocumentCount(),
                "文档数量不应该被部分更新方法修改");
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), target.getLastImportTime(),
                "最后导入时间不应该被部分更新方法修改");
    }

    @Test
    @DisplayName("测试复制实体 - 包含所有新字段")
    void testCopyEntity_IncludesAllNewFields() {
        SearchSpace copied = mapper.copyEntity(testEntity);

        assertNotNull(copied, "复制的实体不应该为null");
        assertNotSame(testEntity, copied, "复制的实体应该是新的实例");

        // 验证所有字段都被正确复制
        assertEquals(testEntity.getId(), copied.getId());
        assertEquals(testEntity.getName(), copied.getName());
        assertEquals(testEntity.getCode(), copied.getCode());
        assertEquals(testEntity.getDescription(), copied.getDescription());
        assertEquals(testEntity.getIndexMapping(), copied.getIndexMapping(), "索引映射应该被复制");
        assertEquals(testEntity.getDocumentCount(), copied.getDocumentCount(), "文档数量应该被复制");
        assertEquals(testEntity.getLastImportTime(), copied.getLastImportTime(), "最后导入时间应该被复制");
        assertEquals(testEntity.getStatus(), copied.getStatus());
        assertEquals(testEntity.getVersion(), copied.getVersion());
    }

    @Test
    @DisplayName("测试复制实体 - null实体")
    void testCopyEntity_NullEntity() {
        SearchSpace copied = mapper.copyEntity(null);
        assertNull(copied, "null实体应该返回null");
    }

    @Test
    @DisplayName("测试边界情况 - 空字符串和null值")
    void testBoundaryValues() {
        SearchSpace entity = new SearchSpace();
        entity.setName("");
        entity.setCode("");
        entity.setDescription("");
        // 使用实体的业务方法设置空字符串映射，会被转换为null
        entity.setIndexMapping("");
        entity.setDocumentCount(null);
        entity.setLastImportTime(null);

        SearchSpaceDTO dto = mapper.toDTO(entity);

        assertEquals("", dto.getName(), "空字符串应该保持不变");
        // entity.setIndexMapping("")会被业务逻辑转换为null，所以dto中也是null
        assertNull(dto.getIndexMapping(), "空索引映射经过业务逻辑处理后应该为null");
        assertNull(dto.getDocumentCount(), "null文档数量应该保持不变");
        assertNull(dto.getLastImportTime(), "null导入时间应该保持不变");
    }

    @Test
    @DisplayName("测试完整的转换循环")
    void testCompleteConversionCycle() {
        // 实体 -> DTO -> 实体
        SearchSpaceDTO dto = mapper.toDTO(testEntity);
        SearchSpace convertedEntity = mapper.toEntity(dto);

        // 验证关键字段没有丢失
        assertEquals(testEntity.getName(), convertedEntity.getName());
        assertEquals(testEntity.getCode(), convertedEntity.getCode());
        assertEquals(testEntity.getIndexMapping(), convertedEntity.getIndexMapping(),
                "索引映射在转换循环中应该保持不变");
        assertEquals(testEntity.getDocumentCount(), convertedEntity.getDocumentCount(),
                "文档数量在转换循环中应该保持不变");
        assertEquals(testEntity.getLastImportTime(), convertedEntity.getLastImportTime(),
                "最后导入时间在转换循环中应该保持不变");
    }
}