// 热词统计组件导出文件
export { default as HotWordFilter } from './HotWordFilter.vue'
export { default as HotWordRankingTable } from './HotWordRankingTable.vue'

// 过滤器子组件
export { default as TimeRangeSelector } from './filters/TimeRangeSelector.vue'
export { default as SearchConditionFilter } from './filters/SearchConditionFilter.vue'
export { default as HotWordLimitSelector } from './filters/HotWordLimitSelector.vue'

// 表格子组件
export { default as RankingTableHeader } from './table/RankingTableHeader.vue'
export { default as RankingTableRow } from './table/RankingTableRow.vue'
export { default as TablePagination } from './table/TablePagination.vue'
export { default as TrendIcon } from './table/TrendIcon.vue'

// 导出类型定义
export type {
  HotWordItem,
  HotWordQueryParams,
  HotWordStatisticsResponse,
  SortConfig,
  PaginationConfig,
  FilterState,
  ComponentState,
  WordCloudConfig,
  WordCloudItem,
  WordCloudDimension
} from '@/types/statistics'