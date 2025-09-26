---
name: index-mapping-editor
description: 在搜索空间配置弹窗中增加索引mapping配置的JSON编辑器标签页
status: backlog
created: 2025-09-26T03:57:25Z
---

# PRD: index-mapping-editor

## Executive Summary

为 deepSearch 系统的搜索空间配置弹窗增加一个新的 "Mapping 配置" 标签页，让系统管理员能够以 JSON 格式直观地查看、编辑和更新 Elasticsearch 索引的 mapping 配置。该功能将大大提升搜索空间管理的便利性，让用户无需使用外部工具就能完成索引 mapping 的配置管理。

## Problem Statement

### 当前痛点
1. **无法查看索引 mapping**：管理员无法在系统内查看当前索引的字段映射配置
2. **配置管理困难**：需要使用 Kibana 或 curl 等外部工具才能管理索引 mapping
3. **操作复杂**：修改 mapping 需要多个步骤，容易出错
4. **缺乏可视性**：无法直观了解索引结构和字段配置

### 为什么现在重要
- 系统已有完整的搜索空间配置界面基础
- Elasticsearch 连接已配置完成
- 管理员需要更便捷的索引管理方式
- 提升系统的完整性和用户体验

## User Stories

### 主要用户角色
**系统管理员**：负责管理 Elasticsearch 索引配置，优化搜索性能

### 详细用户旅程

**故事 1：查看索引 Mapping 配置**
```
作为系统管理员
我想要在搜索空间配置弹窗中查看索引的 mapping 配置
这样我可以了解当前的字段映射结构
```
**验收标准：**
- 在现有弹窗中增加 "Mapping 配置" 标签页
- 以格式化的 JSON 形式显示当前索引的完整 mapping
- 包含字段类型、分析器、分词器等完整信息
- 支持深色/浅色主题显示

**故事 2：编辑索引 Mapping 配置**
```
作为系统管理员
我想要直接在界面中编辑 mapping 配置
这样我可以快速调整字段映射而无需使用外部工具
```
**验收标准：**
- 提供内置 JSON 编辑器
- 支持语法高亮和自动缩进
- 实时 JSON 格式验证
- 编辑器支持快捷键操作

**故事 3：应用 Mapping 变更**
```
作为系统管理员
我想要保存并应用 mapping 配置变更
这样新的配置可以立即生效
```
**验收标准：**
- 点击保存按钮应用配置到 Elasticsearch
- 显示操作结果反馈（成功/失败）
- 失败时显示具体错误信息
- 成功后自动刷新显示最新配置

## Requirements

### Functional Requirements

#### 核心功能
1. **新增 Mapping 配置标签页**
   - 在现有的 "查看详情" 和 "编辑配置" 基础上增加第三个标签
   - 标签图标使用 `Settings` 或 `Code` 图标
   - 标签文本：Mapping 配置

2. **Mapping 显示功能**
   - 从 Elasticsearch 获取指定索引的 mapping 配置
   - 以格式化 JSON 形式展示完整的 mapping 结构
   - 处理索引不存在的情况（显示友好提示）

3. **JSON 编辑器集成**
   - 集成代码编辑器组件（推荐 CodeMirror 或 Monaco Editor）
   - 支持 JSON 语法高亮
   - 支持代码折叠和自动缩进
   - 支持搜索和替换功能

4. **实时验证**
   - JSON 格式验证
   - 错误位置标识
   - 错误提示信息

5. **配置保存功能**
   - 保存按钮和取消按钮
   - 调用后端 API 更新 Elasticsearch 索引 mapping
   - 操作状态反馈

#### 用户交互流程
1. 打开搜索空间配置弹窗
2. 点击 "Mapping 配置" 标签页
3. 查看当前 mapping 配置
4. 编辑 JSON 配置
5. 验证 JSON 格式
6. 保存并应用配置
7. 查看操作结果

### Non-Functional Requirements

#### 性能要求
- 标签页切换响应时间 < 200ms
- JSON 编辑器加载时间 < 500ms
- 保存操作响应时间 < 3s
- 支持大型 mapping 配置（> 10KB）

#### 用户体验要求
- 与现有界面风格保持一致
- 支持深色模式适配
- 响应式设计，支持不同屏幕尺寸
- 错误提示清晰友好

#### 技术要求
- 兼容现有 Vue.js 3 + TypeScript 技术栈
- 使用 TailwindCSS 保持样式一致性
- 遵循项目代码规范
- 支持 ESLint 和 TypeScript 检查

## Success Criteria

### 可衡量指标
1. **功能完整性**：新标签页能够正确显示和编辑 mapping 配置
2. **用户体验**：管理员能够在 3 分钟内完成一次 mapping 配置修改
3. **错误处理**：99% 的 JSON 格式错误能够被正确识别和提示
4. **性能指标**：页面加载和操作响应时间满足预期要求

### 验收标准
- [ ] 新标签页正确集成到现有弹窗
- [ ] 能够获取并显示索引 mapping 配置
- [ ] JSON 编辑器功能完整可用
- [ ] 保存功能正常工作
- [ ] 错误处理机制完善
- [ ] 界面样式与现有系统一致
- [ ] 通过所有单元测试和集成测试

## Constraints & Assumptions

### 技术约束
- 必须使用现有的 Vue.js 3 + TypeScript 技术栈
- 遵循现有的代码风格和项目结构
- 复用现有的 Elasticsearch 客户端配置
- 使用项目现有的 UI 组件库和样式系统

### 业务约束
- 不改变现有弹窗的基本结构和逻辑
- 保持向后兼容性
- 不影响现有功能的正常使用

### 假设条件
- Elasticsearch 服务可用且配置正确
- 用户具备基本的 JSON 和 Elasticsearch mapping 知识
- 系统具有足够的权限访问 Elasticsearch API
- 网络连接稳定

## Out of Scope

以下功能明确不在此次开发范围内：
- **权限控制系统**：暂不实现基于角色的权限控制
- **Mapping 配置模板**：不提供预设的 mapping 模板
- **配置历史记录**：不实现配置变更历史追踪
- **批量操作**：不支持多个索引的批量 mapping 配置
- **高级验证**：不实现 mapping 结构的业务逻辑验证
- **可视化编辑器**：不提供拖拽式的可视化配置界面

## Dependencies

### 外部依赖
- **Elasticsearch 服务**：必须可用且版本兼容
- **网络连接**：前端需要能够访问后端 API

### 内部依赖
- **现有搜索空间配置弹窗**：需要在此基础上扩展
- **后端 API**：需要新增获取和更新 mapping 的 API 端点
- **Elasticsearch 客户端**：后端已有的 Elasticsearch 配置和客户端

### 技术依赖
- **JSON 编辑器组件**：需要选择和集成合适的编辑器库
- **Vue.js 生态系统**：依赖现有的 Vue 3、Pinia、Vue Router 等
- **构建工具**：使用现有的 Vite 构建配置

## 技术实现建议

### 前端实现
1. **新增标签页**：在 `SearchSpaceConfigDialog.vue` 中增加第三个标签
2. **JSON 编辑器**：集成 `@codemirror/vue` 或类似组件
3. **API 调用**：新增获取和更新 mapping 的 API 调用方法
4. **状态管理**：使用现有的 Pinia store 管理 mapping 数据

### 后端实现
1. **API 端点**：新增 GET 和 PUT `/api/search-spaces/{id}/mapping` 端点
2. **服务层**：在搜索空间服务中新增 mapping 管理方法
3. **Elasticsearch 集成**：使用现有客户端调用相关 API
4. **错误处理**：完善的异常处理和用户友好的错误消息

### 测试策略
1. **单元测试**：覆盖核心业务逻辑
2. **集成测试**：测试与 Elasticsearch 的集成
3. **E2E 测试**：测试完整的用户操作流程
4. **性能测试**：验证大型配置的处理性能