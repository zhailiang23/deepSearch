---
name: mobile-search-ui
status: completed
created: 2025-09-26T10:11:55Z
updated: 2025-09-27T02:40:58Z
completed: 2025-09-27T02:40:58Z
progress: 100%
prd: .claude/prds/mobile-search-ui.md
github: https://github.com/zhailiang23/deepSearch/issues/76
---

# Epic: mobile-search-ui

## Overview

基于现有 `ElasticsearchDataController.searchData` 后端服务构建移动端搜索界面演示工具。采用双栏布局设计：左侧高保真iPhone模拟器显示移动端搜索界面，右侧参数设置面板支持实时配置搜索参数。复用现有API和组件架构，最小化代码实现，专注于演示和调试功能。

## Architecture Decisions

- **前端架构**: 基于现有 Vue 3 + TypeScript + shadcn-vue 技术栈，无需引入新框架
- **API复用**: 完全使用现有 `POST /api/elasticsearch/search` 接口，避免后端开发工作
- **组件策略**: 最大化复用现有搜索相关组件，创建专门的演示容器组件
- **状态管理**: 使用 Pinia 管理演示状态，与现有状态管理保持一致
- **样式系统**: 继续使用 TailwindCSS 淡绿色主题，保持视觉一致性
- **设备模拟**: 使用 CSS 变换和 iframe 隔离技术实现设备模拟，避免复杂的第三方库

## Technical Approach

### UI Layout Specification

基于设计图的双栏布局精确规格：

**左侧 - 手机屏幕模拟区** (50% 宽度):
- iPhone设备框架模拟器，包含状态栏、Home按钮等视觉元素
- 内部搜索界面：顶部搜索查询输入框（query） + 下方搜索结果列表
- 设备尺寸：375x812px (iPhone 12 Pro比例)
- 支持滚动查看搜索结果
- 搜索框支持实时输入和搜索功能

**右侧 - 参数设置区** (50% 宽度):
- **基础参数配置**：
  - 搜索空间选择器（searchSpaceId下拉菜单）
  - 页码和页面大小设置（page/size）
- **拼音搜索配置**：
  - 启用拼音搜索开关（enablePinyinSearch）
  - 拼音模式选择（pinyinMode: AUTO/STRICT/FUZZY）
- **配置管理**：
  - 参数预设模板选择
  - 配置导入/导出按钮

### Frontend Components

**主要新增组件**:
- `MobileSearchDemo.vue`: 主演示页面（双栏布局容器）
- `PhoneSimulator.vue`: 手机设备模拟器外壳
- `MobileSearchApp.vue`: 移动端搜索应用界面
- `ParameterPanel.vue`: 参数设置面板容器
- `SearchSpaceSelector.vue`: 搜索空间选择器（searchSpaceId）
- `PinyinSearchConfig.vue`: 拼音搜索配置（enablePinyinSearch + pinyinMode）
- `PagingConfig.vue`: 分页配置（page + size）
- `ConfigManager.vue`: 配置管理组件

**复用现有组件**:
- 复用现有 SearchBar、SearchResults 等搜索组件
- 复用现有 API 服务层和类型定义
- 复用现有 Elasticsearch 数据处理逻辑

**状态管理**:
- 创建 `useMobileSearchDemo` composable 管理演示状态
- 实现参数同步：右侧参数变更 → 左侧界面更新
- 使用 debounce 防止频繁 API 调用

### Backend Services

**API集成实现**:
- 完全使用现有 `ElasticsearchDataController.searchData` API
- **API端点**：`POST /api/elasticsearch/search`
- **请求格式 (SearchDataRequest)**：
  ```json
  {
    "searchSpaceId": "string (required)",
    "query": "string (optional)",
    "page": "number (default: 1)",
    "size": "number (default: 20)",
    "enablePinyinSearch": "boolean (default: true)",
    "pinyinMode": "string (AUTO|STRICT|FUZZY, default: AUTO)",
    "sort": {
      "field": "string (required)",
      "order": "string (asc|desc, required)"
    },
    "filters": [{
      "field": "string (required)",
      "value": "any (required)",
      "operator": "string (eq|contains|startsWith|endsWith|range|in)"
    }]
  }
  ```
- **响应格式 (SearchDataResponse)**：
  ```json
  {
    "data": [{
      "_id": "string",
      "_score": "number",
      "_source": "object",
      "_index": "string",
      "_type": "string",
      "_version": "number"
    }],
    "total": "number",
    "page": "number",
    "size": "number",
    "mapping": {
      "mappings": "object"
    }
  }
  ```

### Infrastructure

**部署集成**:
- 作为现有 frontend 项目的新路由页面
- 使用现有的 Docker + Docker Compose 部署方案
- 无需额外的基础设施配置

## Implementation Strategy

**开发优先级**:
1. **Phase 1** (1-2周): 基础框架和双栏布局
2. **Phase 2** (1-2周): 设备模拟器和移动端界面组件
3. **Phase 3** (1周): 参数面板和实时同步功能
4. **Phase 4** (1周): 配置管理和优化完善

**风险缓解**:
- 渐进式开发，每个阶段都有可用的演示版本
- 优先实现核心功能，高级特性作为增量开发
- 设计简单的回退机制处理API异常

**测试策略**:
- 单元测试：关键 composable 和工具函数
- 集成测试：API 调用和状态同步逻辑
- 手动测试：不同浏览器和屏幕尺寸的兼容性

## Task Breakdown Preview

基于设计图的详细任务分解（9个核心任务）:

- [ ] **双栏布局实现**: 50-50分屏布局、响应式设计、CSS Grid/Flexbox
- [ ] **手机屏幕模拟器**: iPhone 12 Pro外观、375x812px尺寸、状态栏模拟
- [ ] **左侧搜索界面**: 搜索框组件、结果列表、滚动支持、移动端样式
- [ ] **右侧参数面板**:
  - 基础参数：searchSpaceId选择器、page/size配置
  - 拼音搜索：enablePinyinSearch开关、pinyinMode选择（AUTO/STRICT/FUZZY）
- [ ] **实时参数同步**: 右侧参数变更 → 左侧界面更新、防抖优化、状态管理
- [ ] **搜索功能集成**: API调用、数据处理、错误处理、加载状态
- [ ] **配置管理**: 导出/导入配置、预设模板、localStorage持久化
- [ ] **性能优化和测试**: 组件懒加载、内存优化、跨浏览器兼容性
- [ ] **用户体验完善**: 交互动画、错误提示、键盘支持、无障碍功能

## Dependencies

**内部依赖**:
- 现有 `ElasticsearchDataController.searchData` API 稳定运行
- 现有搜索相关组件和类型定义可复用
- Vue Router 配置支持新增路由

**外部依赖**:
- 无新的外部服务依赖
- 现有技术栈（Vue 3、shadcn-vue、TailwindCSS）
- 浏览器最低版本：Chrome 90+、Firefox 88+、Safari 14+

**前置工作**:
- 确认现有搜索API的完整功能和稳定性
- 评估现有组件的复用可行性
- 规划新增路由和导航集成方式

## Success Criteria (Technical)

**界面实现指标**:
- 双栏布局完美实现50-50分屏，适配1920x1080及以上分辨率
- iPhone模拟器严格按375x812px尺寸显示，外观高度还原
- 参数1（搜索空间）下拉菜单正常工作，支持至少5个搜索空间
- 参数2（搜索查询）输入框与左侧搜索框实时同步 < 300ms
- 设备模拟器视觉相似度与真实iPhone > 95%

**性能指标**:
- 页面首次加载时间 < 3秒
- 参数调整后搜索响应时间 < 1秒
- 配置导出/导入操作 < 500ms
- 无内存泄漏，长时间使用内存增长 < 20MB

**质量指标**:
- TypeScript 类型覆盖率 100%
- 单元测试覆盖率 > 80%
- 支持键盘导航和基本无障碍功能
- 在目标浏览器中无 Console 错误

## Estimated Effort

**总体时间估算**: 5-6周

**资源需求**:
- 前端开发: 1人 x 5-6周
- 测试验证: 0.5人 x 2周
- 设计支持: 0.2人 x 1周（UI review）

**关键路径**:
1. 基础框架搭建 → 设备模拟器 → 参数同步机制 → 配置管理
2. 并行：移动端界面开发 + 参数面板开发
3. 集成测试和性能优化

**交付里程碑**:
- 第2周：基础演示版本（静态布局）
- 第4周：功能完整版本（参数同步）
- 第5周：优化版本（配置管理、性能优化）
- 第6周：发布版本（测试完成、文档齐全）