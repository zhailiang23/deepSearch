package com.ynet.mgmt.hotTopic.service;

import com.ynet.mgmt.hotTopic.dto.*;
import com.ynet.mgmt.common.dto.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 热门话题业务服务接口
 * 提供热门话题管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
public interface HotTopicService {

    // ========== 基本CRUD操作 ==========

    /**
     * 创建热门话题
     * @param request 创建请求
     * @return 创建的话题
     */
    HotTopicDTO createHotTopic(CreateHotTopicRequest request);

    /**
     * 删除热门话题
     * @param id 话题ID
     */
    void deleteHotTopic(Long id);

    /**
     * 更新热门话题
     * @param id 话题ID
     * @param request 更新请求
     * @return 更新后的话题
     */
    HotTopicDTO updateHotTopic(Long id, UpdateHotTopicRequest request);

    /**
     * 根据ID获取热门话题
     * @param id 话题ID
     * @return 话题信息
     */
    HotTopicDTO getHotTopic(Long id);

    /**
     * 分页查询热门话题列表
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<HotTopicDTO> listHotTopics(Pageable pageable);

    /**
     * 关键字搜索热门话题（分页）
     * @param keyword 搜索关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<HotTopicDTO> searchHotTopics(String keyword, Pageable pageable);

    /**
     * 获取所有热门话题列表
     * @return 话题列表
     */
    List<HotTopicDTO> getAllHotTopics();

    // ========== 可见性相关操作 ==========

    /**
     * 根据可见性分页查询热门话题
     * @param visible 是否可见
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<HotTopicDTO> listHotTopicsByVisible(Boolean visible, Pageable pageable);

    /**
     * 获取可见的热门话题，按热度降序排列
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<HotTopicDTO> getVisibleTopicsOrderByPopularity(Pageable pageable);

    /**
     * 切换话题可见性
     * @param id 话题ID
     * @return 更新后的话题
     */
    HotTopicDTO toggleVisibility(Long id);

    // ========== 热度相关操作 ==========

    /**
     * 获取热度大于指定值的话题
     * @param minPopularity 最小热度值
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<HotTopicDTO> getTopicsByMinPopularity(Integer minPopularity, Pageable pageable);

    /**
     * 获取热度排行榜前N位
     * @param limit 数量限制
     * @return 话题列表
     */
    List<HotTopicDTO> getTopPopularTopics(int limit);

    /**
     * 更新话题热度
     * @param id 话题ID
     * @param popularity 新的热度值
     * @return 更新后的话题
     */
    HotTopicDTO updatePopularity(Long id, Integer popularity);

    // ========== 验证方法 ==========

    /**
     * 检查名称是否可用
     * @param name 话题名称
     * @return 是否可用
     */
    boolean isNameAvailable(String name);

    /**
     * 检查名称是否可用（排除指定ID）
     * @param name 话题名称
     * @param excludeId 排除的话题ID
     * @return 是否可用
     */
    boolean isNameAvailable(String name, Long excludeId);

    // ========== 统计方法 ==========

    /**
     * 统计可见话题数量
     * @return 可见话题数量
     */
    Long countVisibleTopics();

    /**
     * 统计不可见话题数量
     * @return 不可见话题数量
     */
    Long countInvisibleTopics();

    /**
     * 获取平均热度
     * @return 平均热度值
     */
    Double getAveragePopularity();
}