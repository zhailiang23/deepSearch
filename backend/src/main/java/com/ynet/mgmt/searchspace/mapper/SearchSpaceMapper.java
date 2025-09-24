package com.ynet.mgmt.searchspace.mapper;

import com.ynet.mgmt.searchspace.dto.CreateSearchSpaceRequest;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索空间实体与DTO转换器
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class SearchSpaceMapper {

    /**
     * 实体转DTO
     *
     * @param entity 搜索空间实体
     * @return 搜索空间DTO
     */
    public SearchSpaceDTO toDTO(SearchSpace entity) {
        if (entity == null) {
            return null;
        }

        SearchSpaceDTO dto = new SearchSpaceDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setIndexMapping(entity.getIndexMapping());
        dto.setDocumentCount(entity.getDocumentCount());
        dto.setLastImportTime(entity.getLastImportTime());
        dto.setStatus(entity.getStatus());
        dto.setVersion(entity.getVersion());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /**
     * 实体转DTO（包含索引状态）
     *
     * @param entity 搜索空间实体
     * @param indexStatus 索引状态
     * @return 搜索空间DTO
     */
    public SearchSpaceDTO toDTO(SearchSpace entity, com.ynet.mgmt.searchspace.model.IndexStatus indexStatus) {
        SearchSpaceDTO dto = toDTO(entity);
        if (dto != null) {
            dto.setIndexStatus(indexStatus);
        }
        return dto;
    }

    /**
     * 实体列表转DTO列表
     *
     * @param entities 搜索空间实体列表
     * @return 搜索空间DTO列表
     */
    public List<SearchSpaceDTO> toDTOList(List<SearchSpace> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建请求转实体
     *
     * @param request 创建请求
     * @return 搜索空间实体
     */
    public SearchSpace toEntity(CreateSearchSpaceRequest request) {
        if (request == null) {
            return null;
        }

        SearchSpace entity = new SearchSpace();
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());
        // 新字段在创建时保持默认值
        // indexMapping = null
        // documentCount = 0L (实体中已设置默认值)
        // lastImportTime = null

        return entity;
    }

    /**
     * DTO转实体（用于更新）
     *
     * @param dto 搜索空间DTO
     * @return 搜索空间实体
     */
    public SearchSpace toEntity(SearchSpaceDTO dto) {
        if (dto == null) {
            return null;
        }

        SearchSpace entity = new SearchSpace();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setDescription(dto.getDescription());
        entity.setIndexMapping(dto.getIndexMapping());
        entity.setDocumentCount(dto.getDocumentCount());
        entity.setLastImportTime(dto.getLastImportTime());
        entity.setStatus(dto.getStatus());
        entity.setVersion(dto.getVersion());

        return entity;
    }

    /**
     * 部分更新实体
     * 仅更新非空字段
     *
     * @param source 源实体
     * @param target 目标实体
     */
    public void updateEntity(SearchSpace source, SearchSpace target) {
        if (source == null || target == null) {
            return;
        }

        if (source.getName() != null) {
            target.setName(source.getName());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
        // 注意：对于新增的字段，通常不在通用的部分更新方法中处理
        // indexMapping、documentCount、lastImportTime 应通过专门的业务方法更新
    }

    /**
     * 复制实体（用于深拷贝）
     * 包含所有字段的完整复制
     *
     * @param source 源实体
     * @return 复制的实体
     */
    public SearchSpace copyEntity(SearchSpace source) {
        if (source == null) {
            return null;
        }

        SearchSpace target = new SearchSpace();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setCode(source.getCode());
        target.setDescription(source.getDescription());
        target.setIndexMapping(source.getIndexMapping());
        target.setDocumentCount(source.getDocumentCount());
        target.setLastImportTime(source.getLastImportTime());
        target.setStatus(source.getStatus());
        target.setVersion(source.getVersion());

        return target;
    }
}