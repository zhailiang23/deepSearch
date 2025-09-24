# Task 37 Analysis: 搜索空间选择组件

## Issue: [#37](https://github.com/zhailiang/deepSearch/issues/37) - 搜索空间选择组件

**估算工时**: 10小时
**复杂度**: 中等
**依赖关系**: depends_on: [36] ✅ 已完成

## 工作分解

### Stream A: API服务层实现 (3小时)
**负责范围**:
- 搜索空间API服务封装
- HTTP请求和错误处理

**具体工作**:
1. 创建 `searchDataService.ts` API服务模块
2. 实现 `getSearchSpaces()` 方法获取搜索空间列表
3. 实现 `getSearchSpaceInfo(id)` 方法获取具体空间信息
4. 添加请求缓存和错误处理机制
5. 配置TypeScript类型定义

**关键文件**:
- `frontend/src/services/searchDataService.ts`
- `frontend/src/types/searchData.ts` (类型定义)

**验收标准**:
- API调用成功返回搜索空间数据
- 错误处理机制完善
- TypeScript类型检查通过

### Stream B: Pinia状态管理 (2小时)
**负责范围**:
- 搜索数据管理的状态存储
- 选中搜索空间的状态维护

**具体工作**:
1. 创建 `searchData.ts` Pinia store
2. 定义搜索空间列表状态
3. 定义当前选中搜索空间状态
4. 实现状态更新action方法
5. 添加状态持久化支持(如需要)

**关键文件**:
- `frontend/src/stores/searchData.ts`

**验收标准**:
- 状态管理功能正常
- 组件间状态同步正确
- 状态变化能正确触发响应

### Stream C: 搜索空间选择组件 (4小时)
**负责范围**:
- SearchSpaceSelector.vue组件实现
- 用户界面和交互逻辑

**具体工作**:
1. 创建 `SearchSpaceSelector.vue` 组件
2. 设计下拉选择器UI界面
3. 显示索引名称、文档数量、mapping信息
4. 实现选择状态切换功能
5. 添加loading状态和空状态处理
6. 集成shadcn-vue组件系统
7. 应用淡绿色主题样式

**关键文件**:
- `frontend/src/components/searchData/SearchSpaceSelector.vue`

**验收标准**:
- 组件渲染正常
- 搜索空间信息正确显示
- 选择功能工作正常
- 样式符合系统设计规范

### Stream D: 集成到主页面 (1小时)
**负责范围**:
- 将组件集成到SearchDataManagePage
- 整体页面布局调整

**具体工作**:
1. 在 `SearchDataManagePage.vue` 中引入搜索空间选择器
2. 调整页面布局和样式
3. 处理组件间的数据传递
4. 测试整体功能集成

**关键文件**:
- `frontend/src/pages/searchData/SearchDataManagePage.vue`

**验收标准**:
- 搜索空间选择器正确显示在主页面
- 页面布局美观合理
- 组件间交互正常

## 并行执行策略

工作流依赖关系：
- **Stream A** (API服务) - 独立执行，优先级最高
- **Stream B** (状态管理) - 独立执行，可与A并行
- **Stream C** (组件实现) - 依赖A和B完成，核心工作流
- **Stream D** (页面集成) - 依赖C完成，最后执行

**建议执行顺序**:
1. 同时启动Stream A和B
2. A和B完成后启动Stream C
3. C完成后执行Stream D进行集成测试

## 技术实现要点

### API接口设计
```typescript
interface SearchSpace {
  id: string
  name: string
  indexName: string
  documentCount: number
  lastUpdated: string
  mappingConfig: Record<string, any>
  status: 'active' | 'inactive'
}

interface SearchDataService {
  getSearchSpaces(): Promise<SearchSpace[]>
  getSearchSpaceInfo(id: string): Promise<SearchSpace>
}
```

### 状态管理设计
```typescript
interface SearchDataState {
  searchSpaces: SearchSpace[]
  selectedSearchSpace: SearchSpace | null
  loading: boolean
  error: string | null
}
```

### 组件功能要求
- 支持搜索空间筛选和搜索
- 显示详细的索引信息
- 响应式设计，支持不同屏幕尺寸
- 无障碍访问支持

## 风险评估

**风险等级**: 中等
**主要风险**:
1. 后端API可能未完全实现搜索空间列表接口
2. 搜索空间数据结构可能与预期不一致
3. 性能问题：如果搜索空间数量很大

**缓解措施**:
1. 先确认后端API接口可用性和数据格式
2. 实现mock数据支持开发阶段测试
3. 添加分页或虚拟滚动支持大量数据

## 完成标准

- [ ] SearchSpaceSelector.vue组件创建完成
- [ ] searchDataService.ts API服务实现
- [ ] searchData.ts Pinia store状态管理
- [ ] 成功从后端获取搜索空间列表
- [ ] 显示索引名称、文档数量、mapping信息
- [ ] 搜索空间选择功能正常工作
- [ ] 组件样式符合系统设计规范
- [ ] 集成到SearchDataManagePage主页面
- [ ] 错误处理和loading状态完善
- [ ] TypeScript类型检查通过
- [ ] 组件单元测试编写完成