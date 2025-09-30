package com.ynet.mgmt.sensitiveWord.service;

import com.ynet.mgmt.sensitiveWord.dto.*;
import com.ynet.mgmt.common.dto.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 敏感词业务服务接口
 * 提供敏感词管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
public interface SensitiveWordService {

    // ========== 基本CRUD操作 ==========

    /**
     * 创建敏感词
     * @param request 创建请求
     * @return 创建的敏感词
     */
    SensitiveWordDTO createSensitiveWord(CreateSensitiveWordRequest request);

    /**
     * 删除敏感词
     * @param id 敏感词ID
     */
    void deleteSensitiveWord(Long id);

    /**
     * 更新敏感词
     * @param id 敏感词ID
     * @param request 更新请求
     * @return 更新后的敏感词
     */
    SensitiveWordDTO updateSensitiveWord(Long id, UpdateSensitiveWordRequest request);

    /**
     * 根据ID获取敏感词
     * @param id 敏感词ID
     * @return 敏感词信息
     */
    SensitiveWordDTO getSensitiveWord(Long id);

    /**
     * 分页查询敏感词列表
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<SensitiveWordDTO> listSensitiveWords(Pageable pageable);

    /**
     * 关键字搜索敏感词（分页）
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<SensitiveWordDTO> searchSensitiveWords(String keyword, Pageable pageable);

    /**
     * 获取所有敏感词列表
     * @return 敏感词列表
     */
    List<SensitiveWordDTO> getAllSensitiveWords();

    // ========== 启用状态相关操作 ==========

    /**
     * 根据启用状态分页查询敏感词
     * @param enabled 是否启用
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<SensitiveWordDTO> listSensitiveWordsByEnabled(Boolean enabled, Pageable pageable);

    /**
     * 切换敏感词启用状态
     * @param id 敏感词ID
     * @return 更新后的敏感词
     */
    SensitiveWordDTO toggleStatus(Long id);

    // ========== 危害等级相关操作 ==========

    /**
     * 根据危害等级分页查询敏感词
     * @param harmLevel 危害等级
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<SensitiveWordDTO> listSensitiveWordsByHarmLevel(Integer harmLevel, Pageable pageable);

    /**
     * 获取危害等级大于等于指定值的敏感词
     * @param minHarmLevel 最小危害等级
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<SensitiveWordDTO> getSensitiveWordsByMinHarmLevel(Integer minHarmLevel, Pageable pageable);

    /**
     * 更新敏感词危害等级
     * @param id 敏感词ID
     * @param harmLevel 新的危害等级
     * @return 更新后的敏感词
     */
    SensitiveWordDTO updateHarmLevel(Long id, Integer harmLevel);

    // ========== 验证方法 ==========

    /**
     * 检查名称是否可用
     * @param name 敏感词名称
     * @return 是否可用
     */
    boolean isNameAvailable(String name);

    /**
     * 检查名称是否可用（排除指定ID）
     * @param name 敏感词名称
     * @param excludeId 排除的敏感词ID
     * @return 是否可用
     */
    boolean isNameAvailable(String name, Long excludeId);

    // ========== 统计方法 ==========

    /**
     * 统计启用的敏感词数量
     * @return 启用的敏感词数量
     */
    Long countEnabledWords();

    /**
     * 统计禁用的敏感词数量
     * @return 禁用的敏感词数量
     */
    Long countDisabledWords();

    /**
     * 统计指定危害等级的敏感词数量
     * @param harmLevel 危害等级
     * @return 该等级的敏感词数量
     */
    Long countByHarmLevel(Integer harmLevel);

    /**
     * 获取各危害等级的敏感词数量分布
     * @return 等级分布Map，key为等级，value为数量
     */
    Map<Integer, Long> getHarmLevelDistribution();
}