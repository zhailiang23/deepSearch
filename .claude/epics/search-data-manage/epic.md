---
name: search-data-manage
description: 搜索数据管理模块技术实现
status: planning
created: 2025-09-24T10:30:15Z
updated: 2025-09-24T19:20:00Z
github: https://github.com/zhailiang/deepSearch/issues/35
prd_source: .claude/prds/search-data-manage.md
estimated_effort: 2-3 weeks
---

# Epic: 搜索数据管理模块 (search-data-manage)

## 概述

基于PRD需求，实现搜索数据管理功能，包括ES全文检索、动态字段展示、数据记录CRUD操作等核心功能。

## 技术架构

### 前端组件架构
- **主页面**: SearchDataManagePage.vue - 整合所有功能
- **搜索空间选择**: SearchSpaceSelector.vue - 空间下拉选择器
- **ES查询**: ElasticsearchQuery.vue - 全文检索输入框
- **结果展示**: DynamicResultsTable.vue - 动态字段表格
- **数据编辑**: DocumentEditDialog.vue - 记录编辑对话框

### 后端服务架构
- **搜索控制器**: SearchDataController.java - API端点
- **ES服务**: ElasticsearchDataService.java - ES操作封装

### API设计
- `GET /api/search-spaces` - 获取搜索空间列表
- `POST /api/elasticsearch/search` - ES全文检索
- `PUT /api/elasticsearch/document/{id}` - 更新文档
- `DELETE /api/elasticsearch/document/{id}` - 删除文档

## 实现任务分解

### Task 1: 导航和路由设置
**目标**: 在系统中添加搜索数据管理页面导航和路由

**技术要点**:
- 在左侧导航菜单添加"搜索数据管理"选项
- 配置Vue Router路由映射
- 实现面包屑导航

**验收标准**:
- 导航菜单显示新选项
- 路由正确跳转到管理页面
- 页面加载时间 < 2秒

### Task 2: 搜索空间选择组件
**目标**: 实现搜索空间下拉选择器，展示索引信息

**技术要点**:
- 创建SearchSpaceSelector.vue组件
- 从后端API获取搜索空间列表
- 显示索引名称、文档数量、mapping配置
- 实现空间切换时状态清理

**验收标准**:
- 下拉选择器正确显示所有可用搜索空间
- 显示索引基本信息（名称、文档数量）
- 切换时清空当前搜索结果和条件
- 支持mapping信息的展示

### Task 3: Elasticsearch全文检索功能
**目标**: 实现基于ES的关键字全文检索

**技术要点**:
- 创建ElasticsearchQuery.vue搜索输入组件
- 实现后端ElasticsearchDataService服务
- 支持ES查询语法和通配符
- 实现搜索结果高亮显示
- 添加实时查询验证和提示

**验收标准**:
- 支持ES标准查询语法
- 搜索响应时间 < 3秒
- 支持空搜索显示所有数据
- 结果高亮显示匹配关键字
- 提供查询语法错误提示

### Task 4: 动态结果表格组件
**目标**: 根据索引mapping动态生成表格列，支持分页

**技术要点**:
- 创建DynamicResultsTable.vue组件
- 根据索引mapping配置动态生成表格列
- 实现字段显示/隐藏切换功能
- 支持字段顺序自定义调整
- 实现分页控制（默认10条，可选10/20/50/100）
- 显示记录详情和字段数据类型

**验收标准**:
- 表格列根据索引配置动态生成
- 列头显示字段名称和数据类型
- 支持字段显示/隐藏切换
- 字段顺序可自定义调整
- 分页功能完整，响应时间 < 1秒
- 支持表格数据的横向滚动

### Task 5: 数据记录编辑功能
**目标**: 实现单条记录的编辑功能

**技术要点**:
- 创建DocumentEditDialog.vue编辑对话框
- 根据字段类型提供合适的输入控件
- 实现数据验证和错误提示
- 后端实现文档更新API

**验收标准**:
- 每行记录提供"编辑"操作按钮
- 编辑对话框显示记录的所有可编辑字段
- 支持不同数据类型的输入验证
- 修改后同步更新ES索引
- 显示修改成功/失败的反馈信息

### Task 6: 数据记录删除功能
**目标**: 实现单条记录的物理删除功能

**技术要点**:
- 在表格中添加"删除"操作按钮
- 实现删除确认对话框和警告信息
- 后端实现ES索引物理删除操作
- 删除后实时更新搜索结果

**验收标准**:
- 每行记录提供"删除"操作按钮
- 点击后显示确认对话框和警告信息
- 确认后执行物理删除（从ES索引中移除）
- 删除后立即从搜索结果中移除记录

### Task 7: 后端API实现和ES集成
**目标**: 实现完整的后端API服务和ES集成

**技术要点**:
- 创建SearchDataController控制器
- 实现ElasticsearchDataService服务类
- 集成现有的SearchSpace实体和服务
- 实现ES查询、更新、删除操作

**验收标准**:
- 所有API端点正常工作
- ES操作响应稳定可靠
- API响应时间符合性能要求
- 错误处理和异常捕获完整

## 技术实现细节

### 前端技术栈
- Vue 3 + TypeScript + Composition API
- shadcn-vue组件库
- TailwindCSS样式（淡绿色主题）
- Pinia状态管理
- Axios HTTP请求

### 后端技术栈
- Spring Boot 3.2.1 + Java 17
- Elasticsearch Java Client
- Lombok代码简化

### 关键技术决策
- 使用现有的搜索空间管理基础设施
- 复用JSON导入系统的ES集成经验
- 基于shadcn-vue构建专业搜索界面
- 采用分页加载优化大数据集性能

## 风险评估和缓解策略

### 技术风险
- **ES集群性能影响**: 实现查询限制和缓存机制
- **大数据集渲染性能**: 采用虚拟滚动和分页加载
- **ES查询复杂性**: 提供查询语法提示和验证

### 业务风险
- **误删除数据风险**: 强化确认对话框
- **数据一致性风险**: 实现操作原子性和错误回滚

## 测试策略

### 单元测试
- 前端组件测试（Vue Test Utils）
- 后端服务层测试（JUnit 5）
- ES集成测试（Testcontainers）

### 集成测试
- API端点集成测试
- ES查询功能测试

### 性能测试
- ES查询响应时间测试
- 大数据集分页性能测试
- 并发用户访问测试

## 部署和监控

### 部署要求
- ES集群稳定运行
- 后端服务支持ES连接
- 前端资源正确编译和部署

### 监控指标
- ES查询响应时间
- API请求成功率
- 用户操作错误率
- 系统资源使用率

---

**预计工期**: 2周
**开发人员**: 1-2人（需要ES经验）
**依赖模块**: 搜索空间管理、ES集群

## Tasks Created
- [ ] 36.md - 导航和路由设置 (parallel: true, 6h)
- [ ] 37.md - 搜索空间选择组件 (parallel: true, 10h)
- [ ] 38.md - Elasticsearch全文检索功能 (parallel: true, 16h)
- [ ] 39.md - 动态结果表格组件 (parallel: true, 20h)
- [ ] 40.md - 数据记录编辑功能 (parallel: true, 14h)
- [ ] 41.md - 数据记录删除功能 (parallel: true, 8h)
- [ ] 42.md - 后端API实现和ES集成 (parallel: true, 12h)

Total tasks: 7
Parallel tasks: 7
Sequential tasks: 0
Estimated total effort: 86小时