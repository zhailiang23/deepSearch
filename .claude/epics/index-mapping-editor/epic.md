---
name: index-mapping-editor
status: backlog
created: 2025-09-26T04:08:58Z
progress: 0%
prd: .claude/prds/index-mapping-editor.md
github: https://github.com/zhailiang23/deepSearch/issues/58
---

# Epic: index-mapping-editor

## Overview

在现有的搜索空间配置弹窗中增加第三个"Mapping 配置"标签页，通过集成 JSON 编辑器实现 Elasticsearch 索引 mapping 的查看和编辑功能。该实现将复用现有的 Vue.js 3 + TypeScript 架构和 TailwindCSS 样式系统，最小化代码变更并保持界面一致性。

## Architecture Decisions

### 技术选择
- **前端 JSON 编辑器**: 使用 `@codemirror/vue` 作为 JSON 编辑器组件
  - 轻量级，与 Vue 3 集成良好
  - 支持语法高亮、代码折叠、搜索替换
  - 可定制主题以匹配现有淡绿色风格
- **状态管理**: 扩展现有 Pinia store，避免新增独立状态管理
- **API 设计**: RESTful 风格，复用现有错误处理和响应格式

### 设计模式
- **最小化变更原则**: 在现有 `SearchSpaceConfigDialog.vue` 基础上扩展
- **关注点分离**: UI 组件与业务逻辑分离，API 调用与状态管理分离
- **错误边界**: 统一的错误处理机制，用户友好的错误提示

### 集成策略
- 复用现有 Elasticsearch 客户端配置和连接池
- 使用现有的请求拦截器和错误处理中间件
- 遵循现有的 TypeScript 类型定义规范

## Technical Approach

### Frontend Components

**1. SearchSpaceConfigDialog.vue 扩展**
- 在现有标签数组中增加第三个标签：`mapping`
- 使用现有的标签样式和切换逻辑
- 新增 `MappingConfigTab` 子组件

**2. MappingConfigTab.vue (新建)**
```typescript
// 主要功能：
// - 加载和显示当前索引的 mapping 配置
// - 集成 CodeMirror 编辑器
// - 实时 JSON 验证
// - 保存和取消操作
```

**3. 状态管理扩展**
- 扩展现有搜索空间 store，增加 mapping 相关状态
- 新增 actions: `fetchMapping`, `updateMapping`
- 缓存 mapping 数据，避免重复请求

**4. API 调用层**
- 新增 `mappingApi.ts` 模块
- 封装 GET 和 PUT mapping 操作
- 统一错误处理和类型定义

### Backend Services

**1. API 端点设计**
```java
GET  /api/search-spaces/{id}/mapping    // 获取索引 mapping
PUT  /api/search-spaces/{id}/mapping    // 更新索引 mapping
```

**2. 服务层扩展**
- 在 `SearchSpaceService` 中新增两个方法：
  - `getIndexMapping(Long spaceId): String`
  - `updateIndexMapping(Long spaceId, String mappingJson): void`

**3. Elasticsearch 集成**
- 复用现有 `ElasticsearchClient` 实例
- 调用 ES 的 `indices.getMapping()` 和 `indices.putMapping()` API
- 统一的异常处理和错误消息转换

**4. 错误处理优化**
- 索引不存在时返回友好提示
- JSON 格式错误时返回具体错误位置
- ES 连接异常时的降级处理

### Infrastructure

**1. 依赖管理**
- 前端新增 `@codemirror/vue` 依赖
- 后端无需新增依赖，复用现有 Elasticsearch 客户端

**2. 构建配置**
- Vite 配置无需变更，CodeMirror 支持 ES modules
- TypeScript 配置扩展，增加 CodeMirror 类型定义

**3. 样式系统**
- 使用现有 TailwindCSS 类定义编辑器样式
- 定制 CodeMirror 主题以匹配淡绿色系统风格
- 确保深色模式适配

## Implementation Strategy

### 开发阶段

**Phase 1: 基础设施搭建** (1-2 天)
- 安装和配置 CodeMirror 依赖
- 创建基本的 MappingConfigTab 组件框架
- 设置 API 路由和基本控制器

**Phase 2: 核心功能实现** (2-3 天)
- 实现后端 mapping 获取和更新逻辑
- 完成前端 JSON 编辑器集成
- 实现数据绑定和状态管理

**Phase 3: 完善和优化** (1-2 天)
- 添加 JSON 验证和错误提示
- 优化用户体验和样式调整
- 编写单元测试和集成测试

### 风险缓解
- **JSON 编辑器兼容性**: 预先验证 CodeMirror 与项目环境的兼容性
- **大型配置性能**: 实现分页或懒加载以处理超大 mapping 配置
- **ES 版本兼容**: 确认 mapping API 在目标 Elasticsearch 版本的兼容性

### 测试策略
- **单元测试**: 覆盖 mapping 获取/更新的业务逻辑
- **集成测试**: 验证与 Elasticsearch 的完整交互流程
- **E2E 测试**: 模拟用户完整操作流程
- **性能测试**: 验证大型 mapping 配置的加载和编辑性能

## Task Breakdown Preview

基于最小化变更和复用现有功能的原则，总共 **8 个核心任务**：

- [ ] **依赖安装与配置**: 安装 CodeMirror，配置编辑器基础设置
- [ ] **后端 API 开发**: 实现 mapping 获取和更新的 REST 端点
- [ ] **前端组件扩展**: 在现有弹窗中增加第三个标签页
- [ ] **JSON 编辑器集成**: 创建 MappingConfigTab 组件并集成编辑器
- [ ] **状态管理扩展**: 扩展 Pinia store 支持 mapping 数据管理
- [ ] **API 调用实现**: 创建前端 API 调用层和错误处理
- [ ] **样式和主题适配**: 调整编辑器样式以匹配系统设计规范
- [ ] **测试和验证**: 编写测试用例并验证完整功能

## Dependencies

### 外部依赖
- **Elasticsearch 服务**: 版本 7.x+ 可用，支持 mapping API
- **网络连接**: 前后端通信和 ES 连接稳定性

### 内部依赖
- **现有搜索空间模块**: 依赖 `SearchSpaceConfigDialog.vue` 和相关服务
- **Elasticsearch 客户端**: 后端已配置的 ES 连接和客户端实例
- **认证授权系统**: 确保用户具有访问搜索空间配置的权限

### 技术依赖
- **CodeMirror**: 前端 JSON 编辑器库
- **Vue 3 生态**: TypeScript、Pinia、TailwindCSS 等现有技术栈
- **Spring Boot**: 后端 REST API 和服务层框架

## Success Criteria (Technical)

### 性能基准
- 标签页切换响应时间: < 200ms
- JSON 编辑器初始化时间: < 500ms
- Mapping 数据加载时间: < 2s (中等大小配置)
- 保存操作响应时间: < 3s

### 质量门控
- 代码覆盖率: > 85%
- TypeScript 类型检查: 0 错误
- ESLint 规则检查: 0 错误
- 构建成功率: 100%

### 验收标准
- [ ] 新标签页与现有 UI 风格完全一致
- [ ] 支持完整的 Elasticsearch mapping 配置格式
- [ ] JSON 格式验证准确率 > 99%
- [ ] 错误提示清晰且用户友好
- [ ] 支持深色模式和响应式设计
- [ ] 通过所有自动化测试

## Estimated Effort

### 总体时间线
- **开发周期**: 5-7 个工作日
- **测试周期**: 2-3 个工作日
- **总计**: 1.5-2 个工作周

### 资源需求
- **前端开发**: 1 名 Vue.js/TypeScript 开发者
- **后端开发**: 1 名 Spring Boot/Java 开发者
- **测试**: 可由开发者兼任，或配备 0.5 名测试工程师

### 关键路径项目
1. **CodeMirror 集成调试**: 可能需要额外时间解决兼容性问题
2. **ES Mapping API 调试**: 需要验证各种 mapping 配置场景
3. **样式适配**: 确保编辑器样式与系统设计完美匹配

### 交付里程碑
- **Week 1 End**: 后端 API 和基础前端框架完成
- **Week 2 Mid**: 完整功能实现，开始测试验证
- **Week 2 End**: 测试完成，代码审查通过，准备部署

## Tasks Created
- [ ] 001.md - 安装配置 CodeMirror 依赖 (parallel: true)
- [ ] 002.md - 扩展搜索空间配置弹窗 (parallel: true)
- [ ] 003.md - 创建 MappingConfigTab 组件 (parallel: false)
- [ ] 004.md - 实现后端 Mapping API 端点 (parallel: true)
- [ ] 005.md - 实现后端 Mapping 服务逻辑 (parallel: false)
- [ ] 006.md - 实现前端状态管理和 API 集成 (parallel: false)
- [ ] 007.md - 样式和主题适配 (parallel: true)
- [ ] 008.md - 编写测试和功能验证 (parallel: true)

Total tasks: 8
Parallel tasks: 5
Sequential tasks: 3
Estimated total effort: 50-64 小时 (1.5-2 工作周)