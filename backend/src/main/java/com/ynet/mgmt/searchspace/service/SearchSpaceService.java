package com.ynet.mgmt.searchspace.service;

import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchspace.dto.*;

/**
 * 搜索空间业务服务接口
 * 提供搜索空间管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
public interface SearchSpaceService {

    /**
     * 创建搜索空间
     * @param createRequest 创建请求
     * @return 创建的搜索空间
     */
    SearchSpaceDTO createSearchSpace(CreateSearchSpaceRequest createRequest);

    /**
     * 删除搜索空间
     * @param id 搜索空间ID
     */
    void deleteSearchSpace(Long id);

    /**
     * 更新搜索空间
     * @param id 搜索空间ID
     * @param updateRequest 更新请求
     * @return 更新后的搜索空间
     */
    SearchSpaceDTO updateSearchSpace(Long id, UpdateSearchSpaceRequest updateRequest);

    /**
     * 启用向量检索
     * @param id 搜索空间ID
     * @return 更新后的搜索空间
     */
    SearchSpaceDTO enableVectorSearch(Long id);

    /**
     * 禁用向量检索
     * @param id 搜索空间ID
     * @return 更新后的搜索空间
     */
    SearchSpaceDTO disableVectorSearch(Long id);

    /**
     * 根据ID查询搜索空间
     * @param id 搜索空间ID
     * @return 搜索空间信息
     */
    SearchSpaceDTO getSearchSpace(Long id);

    /**
     * 根据代码查询搜索空间
     * @param code 搜索空间代码
     * @return 搜索空间信息
     */
    SearchSpaceDTO getSearchSpaceByCode(String code);

    /**
     * 分页查询搜索空间列表
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<SearchSpaceDTO> listSearchSpaces(SearchSpaceQueryRequest request);

    /**
     * 获取搜索空间统计信息
     * @return 统计信息
     */
    SearchSpaceStatistics getStatistics();

    /**
     * 检查代码是否可用
     * @param code 搜索空间代码
     * @return 是否可用
     */
    boolean isCodeAvailable(String code);
}