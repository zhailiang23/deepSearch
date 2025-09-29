package com.ynet.mgmt.hotTopic.mapper;

import com.ynet.mgmt.hotTopic.dto.HotTopicDTO;
import com.ynet.mgmt.hotTopic.dto.CreateHotTopicRequest;
import com.ynet.mgmt.hotTopic.dto.UpdateHotTopicRequest;
import com.ynet.mgmt.hotTopic.entity.HotTopic;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 热门话题实体与DTO转换器
 * 提供热门话题实体与数据传输对象之间的转换方法
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class HotTopicMapper {

    /**
     * 实体转DTO
     *
     * @param entity 热门话题实体
     * @return 热门话题DTO
     */
    public HotTopicDTO toDTO(HotTopic entity) {
        if (entity == null) {
            return null;
        }

        HotTopicDTO dto = new HotTopicDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPopularity(entity.getPopularity());
        dto.setVisible(entity.getVisible());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        return dto;
    }

    /**
     * 实体列表转DTO列表
     *
     * @param entities 热门话题实体列表
     * @return 热门话题DTO列表
     */
    public List<HotTopicDTO> toDTOList(List<HotTopic> entities) {
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
     * @return 热门话题实体
     */
    public HotTopic toEntity(CreateHotTopicRequest request) {
        if (request == null) {
            return null;
        }

        HotTopic entity = new HotTopic();
        entity.setName(request.getName());
        entity.setPopularity(request.getPopularity());
        entity.setVisible(request.getVisible());

        return entity;
    }

    /**
     * 更新实体字段（仅更新非空字段）
     *
     * @param request 更新请求
     * @param target 目标实体
     */
    public void updateEntity(UpdateHotTopicRequest request, HotTopic target) {
        if (request == null || target == null) {
            return;
        }

        if (request.getName() != null) {
            target.setName(request.getName());
        }
        if (request.getPopularity() != null) {
            target.setPopularity(request.getPopularity());
        }
        if (request.getVisible() != null) {
            target.setVisible(request.getVisible());
        }
    }

    /**
     * DTO转实体（用于更新操作）
     *
     * @param dto 热门话题DTO
     * @return 热门话题实体
     */
    public HotTopic toEntity(HotTopicDTO dto) {
        if (dto == null) {
            return null;
        }

        HotTopic entity = new HotTopic();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setPopularity(dto.getPopularity());
        entity.setVisible(dto.getVisible());

        return entity;
    }

    /**
     * 复制实体（用于深拷贝）
     * 包含所有字段的完整复制
     *
     * @param source 源实体
     * @return 复制的实体
     */
    public HotTopic copyEntity(HotTopic source) {
        if (source == null) {
            return null;
        }

        HotTopic target = new HotTopic();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setPopularity(source.getPopularity());
        target.setVisible(source.getVisible());

        return target;
    }
}