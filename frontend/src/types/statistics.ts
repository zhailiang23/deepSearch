/**
 * 热词统计相关的TypeScript类型定义
 */

// 热词项
export interface HotWordItem {
  keyword: string
  count: number
  rank: number
  trend?: 'up' | 'down' | 'stable'
  percentage?: number
}

// 热词统计查询参数
export interface HotWordQueryParams {
  startTime?: string
  endTime?: string
  searchKeyword?: string
  limit?: number
  offset?: number
  sortBy?: 'count' | 'keyword' | 'rank'
  sortOrder?: 'asc' | 'desc'
  minCount?: number
}

// 热词统计响应
export interface HotWordStatisticsResponse {
  items: HotWordItem[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
}

// 时间范围选择器选项
export interface TimeRangeOption {
  label: string
  value: string
  startTime: string
  endTime: string
}

// 过滤器状态
export interface FilterState {
  timeRange: {
    startTime: string
    endTime: string
    preset?: string
  }
  searchKeyword: string
  limit: number
  sortBy?: 'count' | 'keyword' | 'rank'
  sortOrder?: 'asc' | 'desc'
  minCount?: number
}

// 排序配置
export interface SortConfig {
  field: 'count' | 'keyword' | 'rank'
  order: 'asc' | 'desc'
}

// 分页配置
export interface PaginationConfig {
  page: number
  pageSize: number
  total: number
  totalPages: number
}

// 表格列配置
export interface TableColumn {
  key: string
  title: string
  sortable?: boolean
  width?: string
  align?: 'left' | 'center' | 'right'
}

// WordCloud配置 - 继承自官方wordcloud类型定义
export interface WordCloudConfig {
  list: WordCloudItem[]
  fontFamily?: string
  fontWeight?: string | number
  color?: string | ((word: string, weight: string | number, fontSize: number, distance: number, theta: number) => string)
  backgroundColor?: string
  minSize?: number
  weightFactor?: number | ((weight: number) => number)
  gridSize?: number
  rotationSteps?: number
  minRotation?: number
  maxRotation?: number
  shape?: string | ((theta: number) => number)
  ellipticity?: number
  hover?: (item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => void
  click?: (item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => void
  clearCanvas?: boolean
  origin?: [number, number]
  drawOutOfBound?: boolean
  shrinkToFit?: boolean
  wait?: number
  abortThreshold?: number
  shuffle?: boolean
  rotateRatio?: number
}

// WordCloud数据项 - [单词, 权重, ...其他数据]
export type WordCloudItem = [string, number, ...any[]]

// WordCloud尺寸信息
export interface WordCloudDimension {
  x: number
  y: number
  w: number
  h: number
}

// 组件状态
export interface ComponentState {
  loading: boolean
  error: string | null
  data: HotWordItem[] | null
}

// API错误响应
export interface ApiError {
  code: string
  message: string
  details?: any
}

// 导出数据格式
export interface ExportData {
  format: 'csv' | 'excel' | 'json'
  filename: string
  data: HotWordItem[]
}

// WordCloud尺寸配置
export interface WordCloudDimensions {
  width: number
  height: number
}

// WordCloud状态
export interface WordCloudState {
  isRendering: boolean
  hasError: boolean
  errorMessage: string | null
  renderProgress: number
  lastRenderTime: number
}

// WordCloud统计信息
export interface WordCloudStats {
  totalWords: number
  renderedWords: number
  maxWeight: number
  minWeight: number
  averageWeight: number
  renderTime: number
}