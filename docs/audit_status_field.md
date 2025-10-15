# 审核状态字段 (audit_status) 说明文档

## 概述

本文档记录了为 Elasticsearch 索引添加审核状态字段的实现细节。

## 背景

为了支持文档的审核管理功能，需要在所有索引中添加一个系统级的状态字段，用于标识文档的审核状态。

## 字段设计

### 字段名称
- **字段名**: `audit_status`
- **字段类型**: `keyword`
- **说明**: 使用 `audit_status` 而非 `status`，避免与业务字段（如 activity 索引中的活动状态）冲突

### 状态值

| 状态值 | 说明 | 英文 |
|--------|------|------|
| available | 可用（已审核通过） | Available |
| unavailable | 不可用（已禁用） | Unavailable |
| under_review | 审核中 | Under Review |

### 默认值
- 新创建的文档默认状态为 `available`（可用）
- 已有文档批量更新后的状态也为 `available`

## 实现内容

### 1. 代码修改

#### 文件: `IndexConfigService.java`
**位置**: `backend/src/main/java/com/ynet/mgmt/jsonimport/service/IndexConfigService.java`

**修改内容**: 在 `addSystemFields` 方法中添加 `audit_status` 字段配置

```java
// 审核状态字段 - 文档的审核状态（系统字段）
// 值: "available"(可用), "unavailable"(不可用), "under_review"(审核中)
// 默认值: "available"
// 注意：使用 audit_status 而非 status，避免与业务字段冲突
mappings.put("audit_status", IndexMappingConfig.FieldMapping.builder()
        .fieldName("audit_status")
        .elasticsearchType("keyword")
        .index(true)
        .docValues(true)
        .build());
```

**影响范围**:
- 所有通过 `IndexConfigService.generateIndexConfig` 方法创建的新索引都会自动包含 `audit_status` 字段

### 2. 批量更新脚本

#### 文件: `add_status_field.sh`
**位置**: `scripts/add_status_field.sh`

**功能**:
1. 为已有索引添加 `audit_status` 字段映射
2. 将所有文档的 `audit_status` 字段值设置为 `available`
3. 验证更新结果

**使用方法**:
```bash
# 执行脚本
./scripts/add_status_field.sh

# 可选：指定 Elasticsearch 连接信息
ES_HOST=localhost ES_PORT=9200 ./scripts/add_status_field.sh
```

## 更新结果

### 索引列表
已成功更新以下索引：

| 索引名称 | 文档数量 | 状态 |
|----------|----------|------|
| function | 248 | ✓ 成功 |
| information | 246 | ✓ 成功 |
| product | 83 | ✓ 成功 |
| activity | 34 | ✓ 成功 |

### 验证结果

所有索引的所有文档都已成功添加 `audit_status` 字段，并设置为 `"available"`：

- **function**: 248/248 文档
- **information**: 246/246 文档
- **product**: 83/83 文档
- **activity**: 34/34 文档

## 字段映射详情

所有索引的 `audit_status` 字段映射配置如下：

```json
{
  "audit_status": {
    "type": "keyword",
    "index": true,
    "doc_values": true
  }
}
```

## 特殊说明

### activity 索引
activity 索引原本已存在一个 `status` 字段（类型为 `text`），用于存储活动的业务状态（如"已结束"、"进行中"等）。

为避免冲突，审核状态字段使用了 `audit_status` 作为字段名，两个字段可以共存：
- `status`: 业务状态字段（活动状态）
- `audit_status`: 系统审核状态字段

示例文档：
```json
{
  "status": "已结束",           // 业务字段：活动状态
  "audit_status": "available", // 系统字段：审核状态
  ...
}
```

## 后续使用

### 查询示例

1. **查询所有可用文档**:
```bash
curl -X GET "localhost:9200/function/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "term": {
        "audit_status": "available"
      }
    }
  }'
```

2. **查询审核中的文档**:
```bash
curl -X GET "localhost:9200/function/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "term": {
        "audit_status": "under_review"
      }
    }
  }'
```

3. **更新文档状态**:
```bash
curl -X POST "localhost:9200/function/_update/document-id" \
  -H 'Content-Type: application/json' \
  -d '{
    "doc": {
      "audit_status": "unavailable"
    }
  }'
```

### Java API 使用

在 Java 代码中使用 Elasticsearch Java Client：

```java
// 查询可用文档
SearchResponse<Document> response = esClient.search(s -> s
    .index("function")
    .query(q -> q
        .term(t -> t
            .field("audit_status")
            .value("available")
        )
    ),
    Document.class
);

// 更新文档状态
esClient.update(u -> u
    .index("function")
    .id(documentId)
    .doc(Map.of("audit_status", "under_review")),
    Document.class
);
```

## 注意事项

1. **向后兼容**: 已有文档都被设置为 `available` 状态，不影响现有功能
2. **新索引自动包含**: 通过 `IndexConfigService` 创建的新索引会自动包含此字段
3. **字段名冲突**: 如果业务字段已使用 `status`，审核状态使用 `audit_status`
4. **性能影响**: `keyword` 类型的字段对查询性能影响很小

## 版本历史

- **2025-01-15**: 初始版本，添加 `audit_status` 字段到所有索引
- 修改人：Claude Code
- 相关提交：待提交

## 相关文件

- `backend/src/main/java/com/ynet/mgmt/jsonimport/service/IndexConfigService.java`
- `scripts/add_status_field.sh`
- `docs/audit_status_field.md` (本文档)
