/**
 * 关键词不匹配度统计相关类型定义
 */

/**
 * 关键词不匹配度排行榜条目
 */
export interface MismatchKeywordRanking {
  /** 排名 */
  rank: number
  /** 关键词 */
  keyword: string
  /** 不匹配率 (0-1之间) */
  mismatchRate: number
  /** 搜索次数 */
  searchCount: number
  /** 不匹配次数 */
  mismatchCount: number
  /** 最后搜索时间 */
  lastSearchTime?: string
  /** 趋势 */
  trend?: 'up' | 'down' | 'stable'
  /** 排名变化 */
  rankChange?: number
}

/**
 * 统计信息
 */
export interface StatisticsInfo {
  /** 总关键词数 */
  totalKeywords: number
  /** 平均不匹配率 */
  avgMismatchRate: number
  /** 最高不匹配率 */
  maxMismatchRate: number
  /** 最低不匹配率 */
  minMismatchRate: number
  /** 数据更新时间 */
  lastUpdated?: string
}

/**
 * 排行榜查询请求
 */
export interface RankingQueryRequest {
  /** 时间范围 */
  timeRange: string
  /** 页码 */
  page: number
  /** 每页大小 */
  size: number
  /** 搜索关键词 (可选) */
  keyword?: string
  /** 最小不匹配率 (可选) */
  minMismatchRate?: number
  /** 最大不匹配率 (可选) */
  maxMismatchRate?: number
}

/**
 * 排行榜查询响应
 */
export interface RankingQueryResponse {
  /** 排行榜数据列表 */
  ranking: MismatchKeywordRanking[]
  /** 查询时间范围 */
  timeRange: string
  /** 总记录数 */
  totalCount: number
  /** 当前页码 */
  page: number
  /** 每页大小 */
  size: number
  /** 是否有下一页 */
  hasNext: boolean
  /** 是否有上一页 */
  hasPrev: boolean
  /** 总页数 */
  totalPages: number
  /** 统计信息 */
  statistics?: StatisticsInfo
  /** 查询时间 */
  queryTime?: string
}

/**
 * 时间范围选项
 */
export const TimeRangeOptions = [
  { label: '最近7天', value: '7d' },
  { label: '最近30天', value: '30d' },
  { label: '最近90天', value: '90d' }
] as const

/**
 * 时间范围类型
 */
export type TimeRange = '7d' | '30d' | '90d'