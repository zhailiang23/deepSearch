# Statistics Components

热词统计功能组件目录结构

## 目录结构

```
statistics/
├── README.md                    # 目录说明文档
├── HotWordFilter.vue           # 热词过滤器主组件
├── HotWordRankingTable.vue     # 热词排行榜表格组件
├── filters/                    # 过滤器子组件
│   ├── TimeRangeSelector.vue   # 时间范围选择器
│   ├── SearchConditionFilter.vue # 搜索条件过滤器
│   └── HotWordLimitSelector.vue   # 热词数量限制选择器
├── table/                      # 表格子组件
│   ├── RankingTableHeader.vue  # 排行榜表格头部
│   ├── RankingTableRow.vue     # 排行榜表格行
│   └── TablePagination.vue     # 表格分页组件
└── wordcloud/                  # 词云相关组件
    ├── WordCloudCanvas.vue     # 词云画布组件
    └── WordCloudControls.vue   # 词云控制面板
```

## 组件功能说明

### 主组件
- **HotWordFilter.vue**: 热词过滤器，包含时间范围、搜索条件、数量限制等过滤功能
- **HotWordRankingTable.vue**: 热词排行榜表格，支持分页、排序、响应式设计

### 过滤器子组件
- **TimeRangeSelector.vue**: 时间范围选择器，支持预设和自定义时间范围
- **SearchConditionFilter.vue**: 搜索条件过滤器，支持关键词搜索
- **HotWordLimitSelector.vue**: 热词数量限制选择器，控制显示的热词数量

### 表格子组件
- **RankingTableHeader.vue**: 表格头部，支持排序功能
- **RankingTableRow.vue**: 表格行，展示热词数据
- **TablePagination.vue**: 分页组件，支持页码跳转

### 词云子组件
- **WordCloudCanvas.vue**: 词云画布，使用wordcloud2.js渲染词云
- **WordCloudControls.vue**: 词云控制面板，调整词云显示参数

## 设计原则

1. **组件化**: 将复杂功能拆分为小的、可复用的组件
2. **类型安全**: 所有组件使用TypeScript，确保类型安全
3. **响应式设计**: 支持移动端和桌面端
4. **淡绿色主题**: 遵循系统的淡绿色主题设计
5. **性能优化**: 合理使用Vue 3的Composition API