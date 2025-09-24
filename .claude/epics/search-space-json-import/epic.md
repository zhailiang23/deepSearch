---
name: search-space-json-import
status: backlog
created: 2025-09-24T01:51:56Z
progress: 0%
prd: .claude/prds/search-space-json-import.md
github: https://github.com/zhailiang23/deepSearch/issues/26
---

# Epic: Search Space JSON Import

## Overview

实现搜索空间JSON数据导入功能，扩展现有搜索空间管理系统，新增文件上传、JSON解析、自动索引配置生成和批量数据导入能力。利用现有的Spring Boot后端架构和Vue.js前端组件，最小化新增代码量，重点复用现有的搜索空间管理、认证和UI组件。

## Architecture Decisions

### 技术选型
- **文件上传**: 复用现有的文件上传组件，扩展支持JSON格式
- **JSON解析**: 使用Jackson库（Spring Boot内置），无需额外依赖
- **异步处理**: 利用Spring Boot的@Async注解，避免引入消息队列复杂性
- **索引配置**: 扩展现有SearchSpace实体，新增indexMapping字段
- **前端状态**: 复用现有Pinia store模式，扩展searchSpace store

### 设计原则
- **最小化改动**: 在现有模块基础上扩展，避免大规模重构
- **复用优先**: 充分利用现有组件、服务和数据模型
- **渐进增强**: 分阶段实现，每个阶段都可独立交付价值

## Technical Approach

### Frontend Components
**复用现有组件**:
- SearchSpaceList.vue - 已有导入数据按钮，扩展点击处理
- SearchSpaceForm.vue - 复用表单验证和错误处理逻辑
- 现有文件上传组件 - 扩展支持JSON格式和大文件处理

**新增组件**:
- JsonImportDialog.vue - 文件上传和配置预览对话框
- IndexConfigPreview.vue - 索引配置展示和编辑组件

### Backend Services
**扩展现有服务**:
- SearchSpaceController - 新增 `/api/search-spaces/{id}/import` 端点
- SearchSpaceService - 新增JSON导入相关业务逻辑
- SearchSpace实体 - 新增indexMapping字段

**新增服务组件**:
- JsonAnalysisService - JSON结构分析和类型推断
- IndexConfigService - Elasticsearch索引配置生成
- DataImportService - 批量数据导入处理

### Infrastructure
**利用现有基础设施**:
- PostgreSQL - 存储索引配置和导入状态
- 现有Elasticsearch集群 - 数据存储和搜索
- Spring Boot异步支持 - 后台任务处理
- 现有Docker配置 - 部署和环境管理

## Implementation Strategy

### 开发阶段
**Phase 1: 基础导入功能** (3天)
- 扩展前端导入按钮功能
- 实现文件上传和基本JSON验证
- 后端接收和存储JSON文件

**Phase 2: 自动配置生成** (4天)
- JSON结构分析和字段类型推断
- 生成Elasticsearch映射配置
- 配置预览和基本编辑功能

**Phase 3: 数据导入集成** (3天)
- Elasticsearch索引创建
- 批量数据导入实现
- 进度跟踪和状态更新

### 风险缓解
- **大文件处理**: 使用流式处理，分批上传和解析
- **ES集群稳定性**: 添加健康检查和重试机制
- **用户体验**: 实现进度条和清晰的错误提示

### 测试方法
- 单元测试: 重点测试JSON解析和配置生成逻辑
- 集成测试: 验证完整导入流程
- 性能测试: 验证大文件处理能力和导入速度

## Task Breakdown Preview

高级任务分解（总计8个任务）:

- [ ] **T1: 前端导入UI扩展** - 扩展现有按钮功能，实现文件选择和上传界面
- [ ] **T2: 文件上传后端接口** - 实现JSON文件接收、验证和临时存储
- [ ] **T3: JSON结构分析服务** - 开发字段类型推断和统计分析功能
- [ ] **T4: 索引配置生成器** - 基于分析结果生成Elasticsearch映射配置
- [ ] **T5: 配置预览与编辑** - 前端配置展示和用户自定义编辑功能
- [ ] **T6: Elasticsearch集成** - 索引创建和数据导入API集成
- [ ] **T7: 批量导入处理** - 实现高性能批量数据导入和进度跟踪
- [ ] **T8: 错误处理与监控** - 完善错误处理、日志记录和状态监控

## Dependencies

### 外部依赖
- **Elasticsearch集群**: 必须可用且版本兼容(当前系统已有)
- **文件存储空间**: 临时存储上传的JSON文件(利用现有存储)

### 内部依赖
- **搜索空间管理模块**: 扩展现有功能，需要数据库结构调整
- **用户认证模块**: 复用现有权限验证机制
- **前端组件库**: 扩展现有文件上传和表单组件

### 团队协调
- 前后端开发需要协调API接口设计
- 需要DBA支持数据库结构变更
- 需要DevOps协助监控和日志配置

## Success Criteria (Technical)

### 性能基准
- 文件上传启动时间 < 2秒
- 20MB JSON文件解析完成时间 < 30秒
- 数据导入速度 > 1000条/秒
- 索引创建时间 < 60秒

### 质量标准
- 单元测试覆盖率 > 80%
- 集成测试通过率 100%
- JSON解析准确率 > 99%
- 错误处理覆盖率 > 95%

### 用户体验标准
- 操作成功率 > 90%
- 错误信息清晰度用户满意度 > 4.0/5.0
- 完整导入流程时间 < 5分钟(中等规模数据)

## Estimated Effort

### 总体时间估算
- **开发时间**: 10个工作日 (2周)
- **测试时间**: 3个工作日
- **部署调试**: 2个工作日
- **总计**: 3周完成

### 资源需求
- **后端开发**: 1名开发者，全职2周
- **前端开发**: 1名开发者，全职1.5周
- **测试**: 1名测试工程师，1周
- **DevOps支持**: 0.5周工作量

### 关键路径
1. JSON结构分析服务开发 (核心逻辑)
2. Elasticsearch集成和批量导入 (性能关键)
3. 前端用户界面和交互优化 (用户体验)

### 风险评估
- **低风险**: 前端扩展和基础文件上传
- **中风险**: JSON解析准确性和性能优化
- **高风险**: 大规模数据导入的稳定性和错误恢复

## Tasks Created

- [ ] #28 - 前端导入UI扩展 (parallel: true)
- [ ] #32 - JSON导入对话框组件 (parallel: false)
- [ ] #34 - 索引配置预览组件 (parallel: false)
- [ ] #27 - 文件上传后端接口 (parallel: false)
- [ ] #30 - 数据库扩展 (parallel: true)
- [ ] #33 - JSON结构分析服务 (parallel: false)
- [ ] #29 - 索引配置生成服务 (parallel: false)
- [ ] #31 - 数据导入集成服务 (parallel: false)

**总计任务**: 8个
**并行任务**: 2个 (#28, #30)
**顺序任务**: 6个
**预估总工时**: 56小时 (约7个工作日)

