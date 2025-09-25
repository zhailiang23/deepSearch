# Task 42 Analysis: 后端API实现和ES集成

## Issue: [#42](https://github.com/zhailiang/deepSearch/issues/42) - 后端API实现和ES集成

**估算工时**: 12小时
**复杂度**: 中等
**依赖关系**: 无依赖，为其他任务提供基础API支持

## 工作分解

### Stream A: ES配置和连接 (3小时)
**负责范围**:
- Elasticsearch配置类实现
- ES客户端连接管理

**具体工作**:
1. 创建 `ElasticsearchConfig.java` 配置类
2. 配置ES客户端连接参数
3. 实现连接池和健康检查
4. 添加ES配置属性到 application.yml

**关键文件**:
- `backend/src/main/java/com/ynet/mgmt/config/ElasticsearchConfig.java`
- `backend/src/main/resources/application.yml`

**验收标准**:
- ES客户端成功连接
- 健康检查通过
- 配置参数可外部化

### Stream B: 数据传输对象设计 (2小时)
**负责范围**:
- API请求和响应DTO设计
- 数据验证和序列化

**具体工作**:
1. 创建 `SearchRequestDto.java` - 搜索请求DTO
2. 创建 `SearchResultDto.java` - 搜索结果DTO
3. 创建 `DocumentUpdateDto.java` - 文档更新DTO
4. 添加Bean Validation注解
5. 实现JSON序列化配置

**关键文件**:
- `backend/src/main/java/com/ynet/mgmt/dto/SearchRequestDto.java`
- `backend/src/main/java/com/ynet/mgmt/dto/SearchResultDto.java`
- `backend/src/main/java/com/ynet/mgmt/dto/DocumentUpdateDto.java`

**验收标准**:
- DTO类设计符合API需求
- 数据验证规则正确
- JSON序列化/反序列化正常

### Stream C: ES服务层实现 (4小时)
**负责范围**:
- ElasticsearchDataService核心业务逻辑
- ES操作的封装和异常处理

**具体工作**:
1. 创建 `ElasticsearchDataService.java` 服务类
2. 实现搜索方法 - 支持全文检索和过滤
3. 实现文档更新方法 - 支持部分和全量更新
4. 实现文档删除方法 - 支持单个和批量删除
5. 实现mapping获取方法 - 用于动态表格列生成
6. 添加异常处理和重试机制

**关键文件**:
- `backend/src/main/java/com/ynet/mgmt/service/ElasticsearchDataService.java`

**验收标准**:
- 所有ES操作方法实现完成
- 异常处理完善
- 日志记录规范

### Stream D: REST控制器实现 (2小时)
**负责范围**:
- SearchDataController REST API端点
- 请求映射和参数绑定

**具体工作**:
1. 创建 `SearchDataController.java` 控制器
2. 实现 POST /api/elasticsearch/search - 全文检索
3. 实现 PUT /api/elasticsearch/document/{id} - 更新文档
4. 实现 DELETE /api/elasticsearch/document/{id} - 删除文档
5. 实现 GET /api/elasticsearch/mapping/{index} - 获取索引mapping
6. 添加统一异常处理
7. 添加接口文档注解

**关键文件**:
- `backend/src/main/java/com/ynet/mgmt/controller/SearchDataController.java`

**验收标准**:
- 所有API端点正常响应
- 请求参数验证正确
- 错误响应格式统一

### Stream E: 集成测试和SearchSpace集成 (1小时)
**负责范围**:
- 与现有SearchSpace实体集成
- 端到端测试

**具体工作**:
1. 分析现有SearchSpace实体和服务
2. 集成SearchSpace列表获取功能
3. 编写集成测试用例
4. 验证API与前端的数据契约

**关键文件**:
- 现有的SearchSpace相关类
- `backend/src/test/java/com/ynet/mgmt/controller/SearchDataControllerTest.java`

**验收标准**:
- 成功获取SearchSpace列表
- 所有API集成测试通过
- 数据格式符合前端需求

## 并行执行策略

工作流依赖关系：
- **Stream A** (ES配置) - 独立执行，优先级最高
- **Stream B** (DTO设计) - 独立执行
- **Stream C** (ES服务) - 依赖A完成，可与B并行
- **Stream D** (控制器) - 依赖B和C完成
- **Stream E** (集成测试) - 依赖A、C、D完成

**建议执行顺序**:
1. 同时启动Stream A和B
2. A完成后启动Stream C
3. B和C完成后启动Stream D
4. 最后执行Stream E进行集成测试

## 风险评估

**风险等级**: 中等
**主要风险**:
1. ES连接配置可能需要调整以适应现有环境
2. 与现有SearchSpace实体的集成可能需要接口适配
3. ES查询性能可能需要优化

**缓解措施**:
1. 先分析现有ES连接和配置模式
2. 详细调研SearchSpace实体结构
3. 实现查询缓存和连接池配置

## 完成标准

- [ ] ElasticsearchConfig配置类实现并测试通过
- [ ] ElasticsearchDataService服务类完整实现
- [ ] SearchDataController所有API端点实现
- [ ] 与SearchSpace实体成功集成
- [ ] 所有DTO类设计完成并验证
- [ ] 统一异常处理和响应格式
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试全部通过
- [ ] API文档生成完成