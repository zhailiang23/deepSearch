package com.ynet.mgmt.imagerecognition.service.impl;

import com.ynet.mgmt.imagerecognition.dto.PromptConfigDTO;
import com.ynet.mgmt.imagerecognition.entity.PromptConfig;
import com.ynet.mgmt.imagerecognition.repository.PromptConfigRepository;
import com.ynet.mgmt.imagerecognition.service.PromptConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词配置服务实现
 */
@Service
public class PromptConfigServiceImpl implements PromptConfigService {

    private static final Logger logger = LoggerFactory.getLogger(PromptConfigServiceImpl.class);

    /**
     * 默认图片识别配置键名
     */
    public static final String DEFAULT_IMAGE_RECOGNITION_KEY = "image_recognition_default";

    /**
     * 默认提示词模板
     */
    private static final String DEFAULT_PROMPT_TEMPLATE =
        "你是一个专业的图片文字识别助手。请仔细识别图片中的所有文字内容,并严格按照以下JSON格式返回结果。\n\n" +
        "**重要要求:**\n" +
        "1. 必须返回有效的JSON格式数据\n" +
        "2. 不要添加任何解释性文字\n" +
        "3. 不要使用markdown代码块标记\n" +
        "4. 直接返回纯JSON对象\n\n" +
        "**JSON格式要求:**\n" +
        "{\n" +
        "  \"name\": \"从图片中识别到的活动名称,如果无法识别则填写'未识别到活动名称'\",\n" +
        "  \"descript\": \"活动描述,如果无法直接识别则根据活动名称和其他文字信息生成简洁描述(不超过100字)\",\n" +
        "  \"link\": \"活动链接地址,如果无法识别则填写空字符串\",\n" +
        "  \"startDate\": \"活动开始时间,格式YYYY-MM-DD,如果无法识别则填写空字符串\",\n" +
        "  \"endDate\": \"活动结束时间,格式YYYY-MM-DD,如果无法识别则填写空字符串\",\n" +
        "  \"status\": \"活动状态(如:进行中、即将开始、已结束、已取消),如果无法识别则填写'未知'\",\n" +
        "  \"all\": \"图片中识别到的所有文字内容,保持原始格式和换行\"\n" +
        "}\n\n" +
        "请严格按照上述JSON格式返回识别结果:";

    private final PromptConfigRepository repository;

    public PromptConfigServiceImpl(PromptConfigRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        initializeDefaultConfig();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromptConfigDTO> getAllConfigs() {
        return repository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromptConfigDTO getConfigById(Long id) {
        PromptConfig config = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在: ID=" + id));
        return convertToDTO(config);
    }

    @Override
    @Transactional(readOnly = true)
    public PromptConfigDTO getConfigByKey(String configKey) {
        PromptConfig config = repository.findByConfigKey(configKey)
            .orElseThrow(() -> new RuntimeException("配置不存在: KEY=" + configKey));
        return convertToDTO(config);
    }

    @Override
    @Transactional(readOnly = true)
    public String getEnabledPromptContent(String configKey) {
        return repository.findByConfigKeyAndEnabled(configKey, true)
            .map(PromptConfig::getPromptContent)
            .orElse(null);
    }

    @Override
    @Transactional
    public PromptConfigDTO createConfig(PromptConfigDTO dto) {
        // 检查配置键名是否已存在
        if (repository.existsByConfigKey(dto.getConfigKey())) {
            throw new RuntimeException("配置键名已存在: " + dto.getConfigKey());
        }

        PromptConfig config = convertToEntity(dto);
        PromptConfig saved = repository.save(config);
        logger.info("创建提示词配置成功: KEY={}", saved.getConfigKey());
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public PromptConfigDTO updateConfig(Long id, PromptConfigDTO dto) {
        PromptConfig config = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("配置不存在: ID=" + id));

        // 如果修改了配置键名,检查新键名是否已被其他配置使用
        if (!config.getConfigKey().equals(dto.getConfigKey())) {
            if (repository.existsByConfigKey(dto.getConfigKey())) {
                throw new RuntimeException("配置键名已存在: " + dto.getConfigKey());
            }
            config.setConfigKey(dto.getConfigKey());
        }

        config.setConfigName(dto.getConfigName());
        config.setPromptContent(dto.getPromptContent());
        config.setDescription(dto.getDescription());
        config.setEnabled(dto.getEnabled());

        PromptConfig updated = repository.save(config);
        logger.info("更新提示词配置成功: ID={}, KEY={}", id, updated.getConfigKey());
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("配置不存在: ID=" + id);
        }
        repository.deleteById(id);
        logger.info("删除提示词配置成功: ID={}", id);
    }

    @Override
    @Transactional
    public void initializeDefaultConfig() {
        if (!repository.existsByConfigKey(DEFAULT_IMAGE_RECOGNITION_KEY)) {
            PromptConfig config = new PromptConfig();
            config.setConfigKey(DEFAULT_IMAGE_RECOGNITION_KEY);
            config.setConfigName("图片识别默认提示词");
            config.setPromptContent(DEFAULT_PROMPT_TEMPLATE);
            config.setDescription("用于批量图片识别的默认提示词模板");
            config.setEnabled(true);

            repository.save(config);
            logger.info("初始化默认提示词配置成功");
        }
    }

    /**
     * 将实体转换为 DTO
     */
    private PromptConfigDTO convertToDTO(PromptConfig entity) {
        PromptConfigDTO dto = new PromptConfigDTO();
        dto.setId(entity.getId());
        dto.setConfigKey(entity.getConfigKey());
        dto.setConfigName(entity.getConfigName());
        dto.setPromptContent(entity.getPromptContent());
        dto.setDescription(entity.getDescription());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }

    /**
     * 将 DTO 转换为实体
     */
    private PromptConfig convertToEntity(PromptConfigDTO dto) {
        PromptConfig entity = new PromptConfig();
        entity.setConfigKey(dto.getConfigKey());
        entity.setConfigName(dto.getConfigName());
        entity.setPromptContent(dto.getPromptContent());
        entity.setDescription(dto.getDescription());
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        return entity;
    }
}
