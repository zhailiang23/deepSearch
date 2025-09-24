package com.ynet.mgmt.searchspace.dto;

import java.time.LocalDateTime;

/**
 * 搜索空间导入统计信息DTO
 * 用于提供搜索空间JSON导入功能的统计数据
 *
 * @author system
 * @since 1.0.0
 */
public class ImportStatistics {

    /**
     * 有映射配置的搜索空间数量
     */
    private Long totalSpacesWithMapping;

    /**
     * 有导入文档的搜索空间数量
     */
    private Long totalSpacesWithDocuments;

    /**
     * 总导入文档数量
     */
    private Long totalImportedDocuments;

    /**
     * 最后导入时间
     */
    private LocalDateTime lastImportTime;

    // 构造函数
    public ImportStatistics() {}

    public ImportStatistics(Long totalSpacesWithMapping, Long totalSpacesWithDocuments,
                          Long totalImportedDocuments, LocalDateTime lastImportTime) {
        this.totalSpacesWithMapping = totalSpacesWithMapping;
        this.totalSpacesWithDocuments = totalSpacesWithDocuments;
        this.totalImportedDocuments = totalImportedDocuments;
        this.lastImportTime = lastImportTime;
    }

    // 业务方法

    /**
     * 检查是否有任何导入活动
     * @return true if 有导入的文档
     */
    public boolean hasAnyImports() {
        return totalImportedDocuments != null && totalImportedDocuments > 0;
    }

    /**
     * 检查是否有映射配置
     * @return true if 有搜索空间配置了映射
     */
    public boolean hasAnyMappings() {
        return totalSpacesWithMapping != null && totalSpacesWithMapping > 0;
    }

    // Getters and Setters
    public Long getTotalSpacesWithMapping() {
        return totalSpacesWithMapping;
    }

    public void setTotalSpacesWithMapping(Long totalSpacesWithMapping) {
        this.totalSpacesWithMapping = totalSpacesWithMapping;
    }

    public Long getTotalSpacesWithDocuments() {
        return totalSpacesWithDocuments;
    }

    public void setTotalSpacesWithDocuments(Long totalSpacesWithDocuments) {
        this.totalSpacesWithDocuments = totalSpacesWithDocuments;
    }

    public Long getTotalImportedDocuments() {
        return totalImportedDocuments;
    }

    public void setTotalImportedDocuments(Long totalImportedDocuments) {
        this.totalImportedDocuments = totalImportedDocuments;
    }

    public LocalDateTime getLastImportTime() {
        return lastImportTime;
    }

    public void setLastImportTime(LocalDateTime lastImportTime) {
        this.lastImportTime = lastImportTime;
    }

    @Override
    public String toString() {
        return "ImportStatistics{" +
                "totalSpacesWithMapping=" + totalSpacesWithMapping +
                ", totalSpacesWithDocuments=" + totalSpacesWithDocuments +
                ", totalImportedDocuments=" + totalImportedDocuments +
                ", lastImportTime=" + lastImportTime +
                '}';
    }
}