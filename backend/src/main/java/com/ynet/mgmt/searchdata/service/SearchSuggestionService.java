package com.ynet.mgmt.searchdata.service;

import com.ynet.mgmt.searchdata.dto.SearchSuggestionDTO;
import com.ynet.mgmt.searchdata.dto.SearchSuggestionRequest;

import java.util.List;

/**
 * 搜索建议服务接口
 * 提供智能搜索建议功能，融合ES completion、搜索历史和热门话题
 *
 * @author system
 * @since 1.0.0
 */
public interface SearchSuggestionService {

    /**
     * 获取搜索建议
     * <p>
     * 融合多个数据源生成搜索建议：
     * 1. ES Completion Suggester - 基于索引内容的智能建议
     * 2. 搜索历史 - 用户个人和全局搜索历史
     * 3. 热门话题 - 系统管理的热门搜索词
     * <p>
     * 综合评分算法：
     * - ES相关性得分(40%)
     * - 历史权重得分(35%) = 个人历史(70%) + 全局历史(30%)
     * - 热度权重得分(25%)
     *
     * @param request 搜索建议请求
     * @return 搜索建议列表,按得分降序排列
     */
    List<SearchSuggestionDTO> getSuggestions(SearchSuggestionRequest request);
}
