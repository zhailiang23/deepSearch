package com.ynet.mgmt.searchlog.service;

import java.util.List;

/**
 * 中文分词服务接口
 * 提供中文和英文文本分词功能，为热词统计提供基础数据
 *
 * @author system
 * @since 1.0.0
 */
public interface ChineseSegmentationService {

    /**
     * 对输入文本进行分词处理
     * 支持中文和英文混合文本的分词
     *
     * @param text 待分词的文本，可以包含中文、英文或混合内容
     * @return 分词结果列表，空字符串或null返回空列表
     * @throws IllegalArgumentException 当输入参数不合法时抛出
     */
    List<String> segmentText(String text);

    /**
     * 批量分词处理
     * 对多个文本进行批量分词，提高处理效率
     *
     * @param texts 待分词的文本列表
     * @return 分词结果列表的列表，顺序与输入对应
     */
    List<List<String>> segmentTexts(List<String> texts);

    /**
     * 检查分词服务是否可用
     * 用于健康检查和服务状态监控
     *
     * @return true表示服务可用，false表示服务不可用
     */
    boolean isServiceAvailable();

    /**
     * 获取分词器版本信息
     * 用于版本追踪和调试
     *
     * @return 分词器版本字符串
     */
    String getSegmenterVersion();
}