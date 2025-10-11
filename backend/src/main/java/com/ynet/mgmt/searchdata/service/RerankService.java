package com.ynet.mgmt.searchdata.service;

import com.ynet.mgmt.searchdata.dto.SearchDataResponse;

import java.util.List;

/**
 * 重排序服务接口
 * 用于对搜索结果进行语义相关性重排序
 *
 * @author system
 * @since 1.0.0
 */
public interface RerankService {

    /**
     * 对搜索结果文档进行重排序
     *
     * @param query 查询文本
     * @param documents 待重排序的文档列表
     * @param topN 返回前N条结果,如果为null则返回所有
     * @return 按相关性评分重新排序后的文档列表
     */
    List<SearchDataResponse.DocumentData> rerankDocuments(
            String query,
            List<SearchDataResponse.DocumentData> documents,
            Integer topN
    );

    /**
     * 检查服务是否可用
     *
     * @return 服务可用性状态
     */
    boolean isServiceAvailable();

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取服务类型
     *
     * @return 服务类型（local/remote）
     */
    String getServiceType();
}
