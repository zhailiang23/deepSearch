package com.ynet.mgmt.sensitiveWord.mapper;

import com.ynet.mgmt.sensitiveWord.dto.SensitiveWordDTO;
import com.ynet.mgmt.sensitiveWord.dto.CreateSensitiveWordRequest;
import com.ynet.mgmt.sensitiveWord.dto.UpdateSensitiveWordRequest;
import com.ynet.mgmt.sensitiveWord.entity.SensitiveWord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词实体与DTO转换器
 * 提供敏感词实体与数据传输对象之间的转换方法
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class SensitiveWordMapper {

    /**
     * 实体转DTO
     *
     * @param entity 敏感词实体
     * @return 敏感词DTO
     */
    public SensitiveWordDTO toDTO(SensitiveWord entity) {
        if (entity == null) {
            return null;
        }

        SensitiveWordDTO dto = new SensitiveWordDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setHarmLevel(entity.getHarmLevel());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        return dto;
    }

    /**
     * 实体列表转DTO列表
     *
     * @param entities 敏感词实体列表
     * @return 敏感词DTO列表
     */
    public List<SensitiveWordDTO> toDTOList(List<SensitiveWord> entities) {
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
     * @return 敏感词实体
     */
    public SensitiveWord toEntity(CreateSensitiveWordRequest request) {
        if (request == null) {
            return null;
        }

        SensitiveWord entity = new SensitiveWord();
        entity.setName(request.getName());
        entity.setHarmLevel(request.getHarmLevel());
        entity.setEnabled(request.getEnabled());

        return entity;
    }

    /**
     * 更新实体字段（仅更新非空字段）
     *
     * @param request 更新请求
     * @param target 目标实体
     */
    public void updateEntity(UpdateSensitiveWordRequest request, SensitiveWord target) {
        if (request == null || target == null) {
            return;
        }

        if (request.getName() != null) {
            target.setName(request.getName());
        }
        if (request.getHarmLevel() != null) {
            target.setHarmLevel(request.getHarmLevel());
        }
        if (request.getEnabled() != null) {
            target.setEnabled(request.getEnabled());
        }
    }

    /**
     * DTO转实体（用于更新操作）
     *
     * @param dto 敏感词DTO
     * @return 敏感词实体
     */
    public SensitiveWord toEntity(SensitiveWordDTO dto) {
        if (dto == null) {
            return null;
        }

        SensitiveWord entity = new SensitiveWord();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setHarmLevel(dto.getHarmLevel());
        entity.setEnabled(dto.getEnabled());

        return entity;
    }

    /**
     * 复制实体（用于深拷贝）
     * 包含所有字段的完整复制
     *
     * @param source 源实体
     * @return 复制的实体
     */
    public SensitiveWord copyEntity(SensitiveWord source) {
        if (source == null) {
            return null;
        }

        SensitiveWord target = new SensitiveWord();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setHarmLevel(source.getHarmLevel());
        target.setEnabled(source.getEnabled());

        return target;
    }
}