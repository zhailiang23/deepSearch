package com.ynet.mgmt.clustering.dto;

import lombok.Data;

/**
 * 散点数据 DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ScatterPointDTO {

    /**
     * X 坐标
     */
    private Double x;

    /**
     * Y 坐标
     */
    private Double y;

    /**
     * 簇 ID (-1 表示噪声点)
     */
    private Integer cluster;

    /**
     * 文本内容
     */
    private String text;
}
