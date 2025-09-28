// 热词排行榜组件导出文件
export { default as HotWordRankingTable } from './HotWordRankingTable.vue'
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