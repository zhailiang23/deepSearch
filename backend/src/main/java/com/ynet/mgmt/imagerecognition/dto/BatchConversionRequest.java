package com.ynet.mgmt.imagerecognition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 批量转换请求 DTO
 */
@Schema(description = "批量图片转换请求")
public class BatchConversionRequest {

    @Schema(description = "数据库 IP 地址", example = "localhost", required = true)
    @NotBlank(message = "数据库 IP 地址不能为空")
    private String dbHost;

    @Schema(description = "数据库端口", example = "3306", required = true)
    @NotNull(message = "数据库端口不能为空")
    private Integer dbPort;

    @Schema(description = "数据库名称", example = "my_database", required = true)
    @NotBlank(message = "数据库名称不能为空")
    private String dbName;

    @Schema(description = "数据库用户名", example = "root", required = true)
    @NotBlank(message = "数据库用户名不能为空")
    private String dbUsername;

    @Schema(description = "数据库密码", example = "password", required = true)
    @NotBlank(message = "数据库密码不能为空")
    private String dbPassword;

    @Schema(description = "数据表名称", example = "images", required = true)
    @NotBlank(message = "数据表名称不能为空")
    private String tableName;

    @Schema(description = "图片地址列名", example = "image_path", required = true)
    @NotBlank(message = "图片地址列名不能为空")
    private String imagePathColumn;

    @Schema(description = "图片链接列名（可选，用于 URL 图片）", example = "image_url")
    private String imageUrlColumn;

    @Schema(description = "主键列名（用于更新或关联）", example = "id")
    private String primaryKeyColumn = "id";

    @Schema(description = "目标 Elasticsearch 索引名称", example = "activity")
    private String indexName;

    // Getters and Setters

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public Integer getDbPort() {
        return dbPort;
    }

    public void setDbPort(Integer dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getImagePathColumn() {
        return imagePathColumn;
    }

    public void setImagePathColumn(String imagePathColumn) {
        this.imagePathColumn = imagePathColumn;
    }

    public String getImageUrlColumn() {
        return imageUrlColumn;
    }

    public void setImageUrlColumn(String imageUrlColumn) {
        this.imageUrlColumn = imageUrlColumn;
    }

    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public void setPrimaryKeyColumn(String primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
