package com.ynet.mgmt.queryunderstanding.dto;

import lombok.Data;

/**
 * 查询理解请求 DTO
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Data
public class QueryUnderstandingRequest {

    /**
     * 原始查询文本
     */
    private String query;

    /**
     * 是否返回详细信息（包含所有中间步骤）
     */
    private Boolean includeDetails = false;
}
