package com.ynet.mgmt.searchspace.service;

import com.ynet.mgmt.common.dto.PageResult;
import com.ynet.mgmt.searchspace.dto.*;

import java.util.List;

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
     * 启用搜索空间
     * @param id 搜索空间ID
     * @return 更新后的搜索空间
     */
    SearchSpaceDTO enableSearchSpace(Long id);

    /**
     * 禁用搜索空间
     * @param id 搜索空间ID
     * @return 更新后的搜索空间
     */
    SearchSpaceDTO disableSearchSpace(Long id);

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

    // ========== 新增的索引映射管理方法 ==========

    /**
     * 获取搜索空间的索引映射配置
     * @param spaceId 搜索空间ID
     * @return 索引映射配置（JSON字符串）
     * @throws com.ynet.mgmt.searchspace.exception.SearchSpaceException 当操作失败时
     */
    String getIndexMapping(Long spaceId);

    /**
     * 更新搜索空间的索引映射配置
     * @param spaceId 搜索空间ID
     * @param mappingJson 新的映射配置（JSON字符串）
     * @throws com.ynet.mgmt.searchspace.exception.SearchSpaceException 当操作失败时
     */
    void updateIndexMapping(Long spaceId, String mappingJson);

    // ========== 新增的JSON导入相关业务方法 ==========

    /**
     * 更新搜索空间的导入统计
     * @param id 搜索空间ID
     * @param additionalCount 新增文档数量
     * @return 更新后的搜索空间DTO
     */
    SearchSpaceDTO updateImportStats(Long id, long additionalCount);

    /**
     * 获取有索引映射的搜索空间列表
     * @return 搜索空间DTO列表
     */
    List<SearchSpaceDTO> listSearchSpacesWithMapping();

    /**
     * 获取有导入文档的搜索空间列表
     * @return 搜索空间DTO列表
     */
    List<SearchSpaceDTO> listSearchSpacesWithDocuments();

    /**
     * 获取导入统计信息
     * @return 导入相关统计数据
     */
    ImportStatistics getImportStatistics();

    /**
     * 重置搜索空间的导入统计
     * @param id 搜索空间ID
     * @return 更新后的搜索空间DTO
     */
    SearchSpaceDTO resetImportStats(Long id);

    /**
     * 批量更新搜索空间的导入统计
     * @param importUpdates 批量更新列表，包含ID和新增数量
     * @return 更新后的搜索空间DTO列表
     */
    List<SearchSpaceDTO> batchUpdateImportStats(List<ImportStatsUpdate> importUpdates);

    /**
     * 导入统计更新DTO
     */
    class ImportStatsUpdate {
        private Long searchSpaceId;
        private long additionalCount;

        public ImportStatsUpdate() {}

        public ImportStatsUpdate(Long searchSpaceId, long additionalCount) {
            this.searchSpaceId = searchSpaceId;
            this.additionalCount = additionalCount;
        }

        public Long getSearchSpaceId() {
            return searchSpaceId;
        }

        public void setSearchSpaceId(Long searchSpaceId) {
            this.searchSpaceId = searchSpaceId;
        }

        public long getAdditionalCount() {
            return additionalCount;
        }

        public void setAdditionalCount(long additionalCount) {
            this.additionalCount = additionalCount;
        }
    }
}