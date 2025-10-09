package com.ynet.mgmt.clustering.dto;

import lombok.Data;
import java.util.List;

/**
 * 聚类话题 DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ClusterTopicDTO {

    /**
     * 簇 ID
     */
    private Integer clusterId;

    /**
     * 话题名称
     */
    private String topic;

    /**
     * 业务标签
     */
    private List<String> tags;

    /**
     * 代表性问题
     */
    private List<String> examples;

    /**
     * 簇大小
     */
    private Integer size;
}
