package com.ynet.mgmt.imagerecognition.service;

import com.ynet.mgmt.imagerecognition.dto.PromptConfigDTO;

import java.util.List;

/**
 * 提示词配置服务接口
 */
public interface PromptConfigService {

    /**
     * 获取所有提示词配置
     * @return 配置列表
     */
    List<PromptConfigDTO> getAllConfigs();

    /**
     * 根据ID获取配置
     * @param id 配置ID
     * @return 配置对象
     */
    PromptConfigDTO getConfigById(Long id);

    /**
     * 根据配置键名获取配置
     * @param configKey 配置键名
     * @return 配置对象
     */
    PromptConfigDTO getConfigByKey(String configKey);

    /**
     * 根据配置键名获取启用的提示词内容
     * @param configKey 配置键名
     * @return 提示词内容，如果未找到或未启用则返回 null
     */
    String getEnabledPromptContent(String configKey);

    /**
     * 创建新配置
     * @param dto 配置数据
     * @return 创建的配置
     */
    PromptConfigDTO createConfig(PromptConfigDTO dto);

    /**
     * 更新配置
     * @param id 配置ID
     * @param dto 配置数据
     * @return 更新后的配置
     */
    PromptConfigDTO updateConfig(Long id, PromptConfigDTO dto);

    /**
     * 删除配置
     * @param id 配置ID
     */
    void deleteConfig(Long id);

    /**
     * 初始化默认配置
     * 如果数据库中不存在默认配置,则创建默认配置
     */
    void initializeDefaultConfig();
}
