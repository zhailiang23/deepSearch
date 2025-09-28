# 热词统计API文档

## 概述

热词统计功能是搜索日志管理系统的核心分析模块，通过分析用户搜索行为，提取出现频率最高的关键词，帮助了解用户搜索趋势和热点内容。

## API端点

### 基础信息

- **基础URL**: `/api/search-logs`
- **认证方式**: JWT Token认证
- **内容类型**: `application/json`

## API列表

### 1. 获取热词统计

#### 端点信息
```
GET /api/search-logs/hot-words
```

#### 描述
分析搜索日志中的关键词，统计出现频率最高的词汇，支持时间范围筛选和参数配置。

#### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| startDate | string | 否 | - | 开始时间，格式：yyyy-MM-dd HH:mm:ss |
| endDate | string | 否 | - | 结束时间，格式：yyyy-MM-dd HH:mm:ss |
| limit | integer | 否 | 10 | 返回热词数量限制（1-100） |
| userId | string | 否 | - | 用户ID筛选 |
| searchSpaceId | string | 否 | - | 搜索空间ID筛选 |
| includeSegmentDetails | boolean | 否 | false | 是否包含分词详情 |
| minWordLength | integer | 否 | 2 | 最小词长（1-20） |
| excludeStopWords | boolean | 否 | true | 是否排除停用词 |

#### 请求示例

```bash
# 获取最近7天前10个热词
GET /api/search-logs/hot-words?startDate=2024-01-01 00:00:00&endDate=2024-01-07 23:59:59&limit=10

# 获取特定用户的热词统计
GET /api/search-logs/hot-words?userId=user123&limit=20&includeSegmentDetails=true

# 获取特定搜索空间的热词，排除停用词
GET /api/search-logs/hot-words?searchSpaceId=space456&excludeStopWords=true&minWordLength=3
```

#### 响应格式

**成功响应 (200 OK)**

```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "word": "人工智能",
      "count": 150,
      "percentage": 15.5,
      "wordLength": 4,
      "relatedQueriesCount": 45,
      "lastOccurrence": "2024-01-07T15:30:00",
      "firstOccurrence": "2024-01-01T09:15:00",
      "segmentDetails": {
        "partOfSpeech": "n",
        "segmentWeight": 0.85,
        "isStopWord": false,
        "relatedWords": ["AI", "机器学习", "深度学习"],
        "frequencyDensity": 0.12
      }
    },
    {
      "word": "机器学习",
      "count": 120,
      "percentage": 12.3,
      "wordLength": 4,
      "relatedQueriesCount": 38,
      "lastOccurrence": "2024-01-07T14:20:00",
      "firstOccurrence": "2024-01-01T10:30:00",
      "segmentDetails": null
    }
  ],
  "timestamp": "2024-01-07T16:00:00"
}
```

**错误响应示例**

```json
# 400 - 参数错误
{
  "success": false,
  "message": "请求参数错误: 返回数量限制必须在1-100之间",
  "timestamp": "2024-01-07T16:00:00"
}

# 400 - 时间范围错误
{
  "success": false,
  "message": "请求参数错误: 开始时间不能晚于结束时间",
  "timestamp": "2024-01-07T16:00:00"
}

# 500 - 服务器错误
{
  "success": false,
  "message": "获取热词统计失败: 数据库连接异常",
  "timestamp": "2024-01-07T16:00:00"
}
```

### 2. 获取搜索日志统计

#### 端点信息
```
GET /api/search-logs/statistics
```

#### 描述
获取搜索日志的综合统计数据，包括搜索次数、成功率、响应时间、热门查询等。

#### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| startTime | string | 是 | - | 开始时间，格式：yyyy-MM-dd HH:mm:ss |
| endTime | string | 是 | - | 结束时间，格式：yyyy-MM-dd HH:mm:ss |
| userId | string | 否 | - | 用户ID筛选 |
| searchSpaceId | string | 否 | - | 搜索空间ID筛选 |
| includeDetails | boolean | 否 | false | 包含详细统计 |
| topQueriesLimit | integer | 否 | 10 | 热门查询数量限制 |
| topSearchSpacesLimit | integer | 否 | 10 | 热门搜索空间数量限制 |
| topUsersLimit | integer | 否 | 10 | 热门用户数量限制 |

#### 请求示例

```bash
# 获取基础统计数据
GET /api/search-logs/statistics?startTime=2024-01-01 00:00:00&endTime=2024-01-07 23:59:59

# 获取详细统计数据
GET /api/search-logs/statistics?startTime=2024-01-01 00:00:00&endTime=2024-01-07 23:59:59&includeDetails=true&topQueriesLimit=20
```

#### 响应格式

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "totalSearches": 1500,
    "successfulSearches": 1350,
    "failedSearches": 150,
    "successRate": 90.0,
    "averageResponseTime": 245.5,
    "totalUsers": 85,
    "totalSearchSpaces": 12,
    "totalQueries": 980,
    "periodStart": "2024-01-01T00:00:00",
    "periodEnd": "2024-01-07T23:59:59",
    "topQueries": [
      {
        "query": "人工智能",
        "count": 150,
        "percentage": 10.0
      }
    ],
    "topSearchSpaces": [
      {
        "searchSpaceId": "space123",
        "searchSpaceName": "技术文档",
        "count": 500,
        "percentage": 33.3
      }
    ],
    "topUsers": [
      {
        "userId": "user123",
        "count": 200,
        "percentage": 13.3
      }
    ],
    "hourlyStatistics": [
      {
        "hour": 9,
        "count": 120,
        "percentage": 8.0
      }
    ]
  },
  "timestamp": "2024-01-07T16:00:00"
}
```

### 3. 分页查询搜索日志

#### 端点信息
```
GET /api/search-logs
```

#### 描述
支持多维度筛选的搜索日志分页查询，包括用户ID、搜索空间、关键词、时间范围等条件。

#### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| userId | string | 否 | - | 用户ID |
| searchSpaceId | string | 否 | - | 搜索空间ID |
| searchSpaceCode | string | 否 | - | 搜索空间代码 |
| query | string | 否 | - | 查询关键词 |
| status | string | 否 | - | 状态(SUCCESS/FAILED/TIMEOUT) |
| startTime | string | 否 | - | 开始时间 |
| endTime | string | 否 | - | 结束时间 |
| minResponseTime | integer | 否 | - | 最小响应时间(ms) |
| maxResponseTime | integer | 否 | - | 最大响应时间(ms) |
| userIp | string | 否 | - | 用户IP |
| sessionId | string | 否 | - | 会话ID |
| traceId | string | 否 | - | 链路追踪ID |
| page | integer | 否 | 0 | 页码(从0开始) |
| size | integer | 否 | 20 | 页大小 |
| sort | string | 否 | createdAt,desc | 排序字段和方向 |

#### 请求示例

```bash
# 查询最近的搜索日志
GET /api/search-logs?page=0&size=20&sort=createdAt,desc

# 查询特定用户的搜索日志
GET /api/search-logs?userId=user123&startTime=2024-01-01 00:00:00&endTime=2024-01-07 23:59:59

# 查询失败的搜索记录
GET /api/search-logs?status=FAILED&page=0&size=50
```

### 4. 获取搜索日志详情

#### 端点信息
```
GET /api/search-logs/{id}
```

#### 描述
根据日志ID获取详细的搜索日志信息，包括请求参数、响应数据、点击记录等。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 搜索日志ID |

#### 请求示例

```bash
GET /api/search-logs/12345
```

### 5. 记录搜索结果点击行为

#### 端点信息
```
POST /api/search-logs/click
```

#### 描述
记录用户点击搜索结果的行为，用于统计分析和优化搜索体验。

#### 请求体

```json
{
  "searchLogId": 12345,
  "documentId": "doc_001",
  "clickPosition": 1,
  "documentTitle": "人工智能技术发展趋势",
  "clickTime": "2024-01-07T15:30:00"
}
```

#### 响应格式

```json
{
  "success": true,
  "message": "记录成功",
  "timestamp": "2024-01-07T15:30:00"
}
```

## 错误码说明

| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未授权访问 |
| 403 | Forbidden | 权限不足 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

## 数据模型

### HotWordResponse

热词统计响应数据结构：

```json
{
  "word": "string",                    // 热词
  "count": "number",                   // 出现次数
  "percentage": "number",              // 出现频率百分比
  "wordLength": "number",              // 词长
  "relatedQueriesCount": "number",     // 关联的搜索查询数量
  "lastOccurrence": "datetime",        // 最近出现时间
  "firstOccurrence": "datetime",       // 首次出现时间
  "segmentDetails": {                  // 分词详情（可选）
    "partOfSpeech": "string",          // 词性
    "segmentWeight": "number",         // 分词权重
    "isStopWord": "boolean",           // 是否为停用词
    "relatedWords": ["string"],        // 相关词汇
    "frequencyDensity": "number"       // 词频密度
  }
}
```

### SearchLogStatistics

搜索日志统计数据结构：

```json
{
  "totalSearches": "number",           // 总搜索次数
  "successfulSearches": "number",      // 成功搜索次数
  "failedSearches": "number",          // 失败搜索次数
  "successRate": "number",             // 成功率
  "averageResponseTime": "number",     // 平均响应时间
  "totalUsers": "number",              // 总用户数
  "totalSearchSpaces": "number",       // 总搜索空间数
  "totalQueries": "number",            // 总查询数
  "periodStart": "datetime",           // 统计期间开始时间
  "periodEnd": "datetime",             // 统计期间结束时间
  "topQueries": [],                    // 热门查询
  "topSearchSpaces": [],               // 热门搜索空间
  "topUsers": [],                      // 热门用户
  "hourlyStatistics": []               // 小时统计
}
```

## 使用注意事项

1. **时间格式**: 所有时间参数都使用 `yyyy-MM-dd HH:mm:ss` 格式
2. **分页参数**: 页码从0开始，默认每页20条记录
3. **排序参数**: 格式为 `字段名,方向`，如 `createdAt,desc`
4. **限制参数**: 热词数量限制范围为1-100，词长限制范围为1-20
5. **认证**: 所有API都需要有效的JWT Token
6. **频率限制**: API调用频率限制为每分钟100次请求

## 示例代码

### JavaScript

```javascript
// 获取热词统计
async function getHotWords() {
  const response = await fetch('/api/search-logs/hot-words?limit=10&excludeStopWords=true', {
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    }
  });

  const result = await response.json();
  if (result.success) {
    console.log('热词列表:', result.data);
  }
}

// 记录点击行为
async function recordClick(searchLogId, documentId, position) {
  const response = await fetch('/api/search-logs/click', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      searchLogId: searchLogId,
      documentId: documentId,
      clickPosition: position,
      clickTime: new Date().toISOString()
    })
  });

  const result = await response.json();
  return result.success;
}
```

### Python

```python
import requests
from datetime import datetime

class HotWordAPI:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }

    def get_hot_words(self, limit=10, start_date=None, end_date=None):
        """获取热词统计"""
        params = {'limit': limit}
        if start_date:
            params['startDate'] = start_date.strftime('%Y-%m-%d %H:%M:%S')
        if end_date:
            params['endDate'] = end_date.strftime('%Y-%m-%d %H:%M:%S')

        response = requests.get(
            f'{self.base_url}/api/search-logs/hot-words',
            params=params,
            headers=self.headers
        )

        return response.json()

    def get_statistics(self, start_time, end_time):
        """获取搜索统计"""
        params = {
            'startTime': start_time.strftime('%Y-%m-%d %H:%M:%S'),
            'endTime': end_time.strftime('%Y-%m-%d %H:%M:%S')
        }

        response = requests.get(
            f'{self.base_url}/api/search-logs/statistics',
            params=params,
            headers=self.headers
        )

        return response.json()
```

### Java

```java
// Spring RestTemplate 示例
@Service
public class HotWordService {

    @Autowired
    private RestTemplate restTemplate;

    public List<HotWordResponse> getHotWords(int limit) {
        String url = "/api/search-logs/hot-words?limit=" + limit;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<List<HotWordResponse>>> response =
            restTemplate.exchange(url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<ApiResponse<List<HotWordResponse>>>() {});

        return response.getBody().getData();
    }
}
```

## 更新日志

### v1.0.0 (2024-01-07)
- 初始版本发布
- 提供热词统计API
- 支持搜索日志查询和统计
- 实现点击行为记录功能