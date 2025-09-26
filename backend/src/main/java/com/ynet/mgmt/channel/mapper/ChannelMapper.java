package com.ynet.mgmt.channel.mapper;

import com.ynet.mgmt.channel.dto.ChannelDTO;
import com.ynet.mgmt.channel.dto.CreateChannelRequest;
import com.ynet.mgmt.channel.dto.UpdateChannelRequest;
import com.ynet.mgmt.channel.entity.Channel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 渠道实体与DTO转换器
 * 提供渠道实体与数据传输对象之间的转换方法
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class ChannelMapper {

    /**
     * 实体转DTO
     *
     * @param entity 渠道实体
     * @return 渠道DTO
     */
    public ChannelDTO toDTO(Channel entity) {
        if (entity == null) {
            return null;
        }

        ChannelDTO dto = new ChannelDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        return dto;
    }

    /**
     * 实体列表转DTO列表
     *
     * @param entities 渠道实体列表
     * @return 渠道DTO列表
     */
    public List<ChannelDTO> toDTOList(List<Channel> entities) {
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
     * @return 渠道实体
     */
    public Channel toEntity(CreateChannelRequest request) {
        if (request == null) {
            return null;
        }

        Channel entity = new Channel();
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());

        return entity;
    }

    /**
     * 更新实体字段（仅更新非空字段）
     *
     * @param request 更新请求
     * @param target 目标实体
     */
    public void updateEntity(UpdateChannelRequest request, Channel target) {
        if (request == null || target == null) {
            return;
        }

        if (request.getName() != null) {
            target.setName(request.getName());
        }
        if (request.getDescription() != null) {
            target.setDescription(request.getDescription());
        }
    }

    /**
     * DTO转实体（用于更新操作）
     *
     * @param dto 渠道DTO
     * @return 渠道实体
     */
    public Channel toEntity(ChannelDTO dto) {
        if (dto == null) {
            return null;
        }

        Channel entity = new Channel();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setDescription(dto.getDescription());

        return entity;
    }

    /**
     * 复制实体（用于深拷贝）
     * 包含所有字段的完整复制
     *
     * @param source 源实体
     * @return 复制的实体
     */
    public Channel copyEntity(Channel source) {
        if (source == null) {
            return null;
        }

        Channel target = new Channel();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setCode(source.getCode());
        target.setDescription(source.getDescription());

        return target;
    }
}