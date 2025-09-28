---
name: statistics-hot-word
status: backlog
created: 2025-09-28T04:00:38Z
updated: 2025-09-28T07:20:00Z
progress: 50.0%
prd: .claude/prds/statistics-hot-word.md
github: https://github.com/zhailiang23/deepSearch/issues/92
---

# Epic: statistics-hot-word

## Overview

基于现有搜索日志管理功能，开发热词统计分析功能。核心思路是利用现有的SearchLog表数据，通过中文分词技术提取查询关键词，生成热词排行榜和词云图可视化展示。整体架构简单直观：后端提供热词统计API，前端实现筛选控制和可视化展示，充分复用现有代码架构。

## Architecture Decisions

### 核心技术选择
- **中文分词工具**: 集成jieba分词库（轻量级，适合中小数据量）
- **词云生成**: 使用wordcloud2.js前端库（基础功能，无需自定义配置）
- **数据存储**: 直接查询SearchLog表，无需额外数据表（简化架构）
- **缓存策略**: 无需预计算缓存（数据量小，实时计算即可）
- **API设计**: 扩展现有SearchLog Controller，添加热词统计端点

### 设计模式
- **服务层扩展**: 在SearchLogService中添加热词统计方法
- **DTO复用**: 利用现有的SearchLogQueryRequest进行参数传递
- **前端组件化**: 创建独立的热词统计页面组件
- **路由扩展**: 在现有管理页面添加热词统计入口

## Technical Approach

### Frontend Components
- **HotWordStatisticsPage.vue**: 主页面组件，整合筛选器和展示区域
- **HotWordFilter.vue**: 时间范围和搜索空间筛选组件（复用现有筛选逻辑）
- **HotWordRankingTable.vue**: 热词排行榜表格组件
- **HotWordCloudChart.vue**: 词云图可视化组件（基础展示，无自定义配置）
- **状态管理**: 使用Pinia管理热词数据状态
- **API集成**: 扩展现有searchLogApi添加热词统计接口

### Backend Services
- **API端点**: `GET /search-logs/hot-words` - 获取热词统计数据
- **服务方法**: `SearchLogService.getHotWordStatistics()` - 核心统计逻辑
- **分词服务**: 创建`ChineseSegmentationService` - 处理中文分词
- **数据模型**:
  - `HotWordStatisticsRequest` - 请求参数DTO
  - `HotWordStatisticsResponse` - 响应数据DTO
  - `HotWordItem` - 单个热词信息

### Infrastructure
- **Maven依赖**: 添加jieba-analysis分词库
- **前端依赖**: 添加wordcloud2.js词云库
- **部署方式**: 无需额外基础设施，基于现有系统部署
- **监控需求**: 复用现有的API监控和日志系统

## Implementation Strategy

### 开发阶段
1. **Phase 1**: 后端分词服务和热词统计API开发
2. **Phase 2**: 前端热词排行榜和筛选功能实现
3. **Phase 3**: 前端词云图可视化集成
4. **Phase 4**: 系统集成测试和性能优化

### 风险控制
- **分词准确性**: 使用jieba默认词典，针对银行业务添加自定义词汇
- **性能风险**: 数据量预期较小，若性能问题可考虑后台定时任务
- **UI兼容性**: 词云图组件需要测试各浏览器兼容性

### 测试策略
- **基础功能测试**: 确保热词统计和展示功能可用
- **API测试**: 验证热词统计接口正常工作
- **页面交互测试**: 确保筛选和展示功能正常

## Task Breakdown Preview

高级任务分类（控制在8个任务以内）：
- [ ] 后端分词服务开发：集成jieba分词，创建分词服务类
- [ ] 后端热词统计API：实现热词统计接口和业务逻辑
- [ ] 前端热词排行榜组件：开发排行榜表格和筛选功能
- [ ] 前端词云图组件：集成wordcloud2.js实现词云可视化
- [ ] 页面路由和导航集成：添加热词统计页面到系统导航
- [ ] API接口联调：前后端接口对接和数据格式验证
- [ ] 系统测试和优化：功能测试、性能测试和bug修复
- [ ] 文档和部署：接口文档、用户手册和部署发布

## Dependencies

### 外部依赖
- **jieba-analysis**: Java版jieba分词库
- **wordcloud2.js**: 前端词云图生成库
- **现有SearchLog数据**: 依赖搜索日志记录的数据质量

### 内部依赖
- **搜索日志管理功能**: 必须在searchlog模块稳定后开发
- **Vue.js框架**: 基于现有前端技术栈开发
- **Spring Boot框架**: 基于现有后端架构扩展

### 团队依赖
- **后端开发**: 分词服务集成和API开发
- **前端开发**: 词云组件和页面开发
- **测试团队**: 功能测试和用户体验验证

## Success Criteria (Technical)

### 性能指标
- **响应时间**: 热词统计API响应时间<2秒
- **页面加载**: 热词页面完整加载时间<3秒
- **分词准确率**: 中文分词结果准确率>90%

### 功能完整性
- **筛选功能**: 支持7/30/90天时间范围筛选
- **空间筛选**: 支持按搜索空间维度筛选
- **数据展示**: TOP 20热词排行榜+词云图双重展示
- **交互体验**: 筛选条件变更后数据实时更新

### 质量要求
- **基础可用性**: 确保核心功能稳定工作
- **兼容性**: 支持主流浏览器
- **稳定性**: 功能正常可用

## Estimated Effort

### 总体时间线
- **开发周期**: 2周（7个工作日）
- **测试周期**: 3天（基础功能验证）
- **总计**: 2.5-3周完成

### 资源需求
- **后端开发**: 1人×5天（分词+API开发）
- **前端开发**: 1人×5天（组件+页面开发，简化配置）
- **测试验证**: 1人×2天（基础功能测试）

### 关键路径
1. 后端分词服务开发（2天）→ 热词统计API（2天）
2. 前端排行榜组件（2天）→ 词云图组件（2天）
3. 系统集成测试（2天）→ 部署发布（1天）

**总体评估**: 该功能相对简单，主要是数据查询和可视化展示，技术风险低，可以快速交付。

## Tasks Created
- [x] #93 - 后端依赖集成和分词服务开发 (parallel: true) ✅ 已完成
- [x] #94 - 后端热词统计API和DTO开发 (parallel: false, depends: 93) ✅ 已完成
- [x] #95 - 前端依赖集成和热词排行榜组件开发 (parallel: true) ✅ 已完成
- [ ] #96 - 前端词云图组件开发 (parallel: false, depends: 95)
- [ ] #97 - 热词统计页面集成和路由配置 (parallel: false, depends: 94, 96)
- [ ] #98 - 系统集成测试和性能优化 (parallel: false, depends: 97)

Total tasks: 6
Parallel tasks: 2 (93, 95)
Sequential tasks: 4 (94→97, 96→97, 97→98)
Estimated total effort: 70-100 hours (9-12 工作日)