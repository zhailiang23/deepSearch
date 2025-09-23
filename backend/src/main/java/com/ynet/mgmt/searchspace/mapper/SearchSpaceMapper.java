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
        dto.setVectorEnabled(entity.getVectorEnabled());
        dto.setStatus(entity.getStatus());
        dto.setVersion(entity.getVersion());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

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
        entity.setVectorEnabled(request.getVectorEnabled() != null ? request.getVectorEnabled() : false);

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
        entity.setVectorEnabled(dto.getVectorEnabled());
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
        if (source.getVectorEnabled() != null) {
            target.setVectorEnabled(source.getVectorEnabled());
        }
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
    }
}