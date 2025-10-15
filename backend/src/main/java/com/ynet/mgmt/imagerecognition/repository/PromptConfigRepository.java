package com.ynet.mgmt.imagerecognition.repository;

import com.ynet.mgmt.imagerecognition.entity.PromptConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 提示词配置数据访问接口
 */
@Repository
public interface PromptConfigRepository extends JpaRepository<PromptConfig, Long> {

    /**
     * 根据配置键名查找配置
     * @param configKey 配置键名
     * @return 配置对象（如果存在）
     */
    Optional<PromptConfig> findByConfigKey(String configKey);

    /**
     * 根据配置键名和启用状态查找配置
     * @param configKey 配置键名
     * @param enabled 是否启用
     * @return 配置对象（如果存在）
     */
    Optional<PromptConfig> findByConfigKeyAndEnabled(String configKey, Boolean enabled);

    /**
     * 检查配置键名是否存在
     * @param configKey 配置键名
     * @return 存在返回 true，否则返回 false
     */
    boolean existsByConfigKey(String configKey);
}
