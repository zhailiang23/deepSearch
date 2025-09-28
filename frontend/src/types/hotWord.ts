/**
 * 热词统计相关类型定义
 * 对应后端HotWordRequest、HotWordResponse等DTO
 */

// ============= API请求类型 =============

/**
 * 热词统计请求参数
 * 对应后端HotWordRequest
 */
export interface HotWordRequest {
  /** 开始时间 */
  startDate?: Date
  /** 结束时间 */
  endDate?: Date
  /** 返回热词数量限制 (1-100) */
  limit?: number
  /** 用户ID筛选 */
  userId?: string
  /** 搜索空间ID筛选 */
  searchSpaceId?: string
  /** 是否包含分词详情 */
  includeSegmentDetails?: boolean
  /** 最小词长 (1-20) */
  minWordLength?: number
  /** 是否排除停用词 */
  excludeStopWords?: boolean
}

/**
 * 热词统计响应数据
 * 对应后端HotWordResponse
 */
export interface HotWordResponse {
  /** 热词 */
  word: string
  /** 出现次数 */
  count: number
  /** 出现频率百分比 */
  percentage: number
  /** 词长 */
  wordLength: number
  /** 关联的搜索查询数量 */
  relatedQueriesCount: number
  /** 最近出现时间 */
  lastOccurrence: string
  /** 首次出现时间 */
  firstOccurrence: string
  /** 分词详情 */
  segmentDetails?: SegmentDetails
}

/**
 * 分词详情
 * 对应后端HotWordResponse.SegmentDetails
 */
export interface SegmentDetails {
  /** 词性 */
  partOfSpeech: string
  /** 分词权重 */
  segmentWeight: number
  /** 是否为停用词 */
  isStopWord: boolean
  /** 相关词汇 */
  relatedWords: string[]
  /** 词频密度 */
  frequencyDensity: number
}

// ============= 搜索日志统计类型 =============

/**
 * 搜索日志统计请求参数
 * 对应后端StatisticsRequest
 */
export interface HotWordStatisticsRequest {
  /** 开始时间（必需） */
  startTime: Date
  /** 结束时间（必需） */
  endTime: Date
  /** 用户ID筛选 */
  userId?: string
  /** 搜索空间ID筛选 */
  searchSpaceId?: string
  /** 包含详细统计 */
  includeDetails?: boolean
  /** 热门查询数量限制 */
  topQueriesLimit?: number
  /** 热门搜索空间数量限制 */
  topSearchSpacesLimit?: number
  /** 热门用户数量限制 */
  topUsersLimit?: number
}

/**
 * 搜索日志统计响应数据
 * 对应后端SearchLogStatistics
 */
export interface HotWordStatisticsResponse {
  /** 总搜索次数 */
  totalSearches: number
  /** 成功搜索次数 */
  successfulSearches: number
  /** 失败搜索次数 */
  failedSearches: number
  /** 成功率 */
  successRate: number
  /** 平均响应时间 */
  averageResponseTime: number
  /** 唯一用户数 */
  uniqueUsers: number
  /** 唯一搜索空间数 */
  uniqueSearchSpaces: number
  /** 热门查询 */
  topQueries: TopQueryStatistic[]
  /** 热门搜索空间 */
  topSearchSpaces: TopSearchSpaceStatistic[]
  /** 热门用户 */
  topUsers: TopUserStatistic[]
  /** 按小时统计 */
  hourlyStatistics: HourlyStatistic[]
  /** 统计时间范围 */
  statisticsTimeRange: {
    startTime: string
    endTime: string
  }
}

/**
 * 热门查询统计
 */
export interface TopQueryStatistic {
  /** 查询关键词 */
  query: string
  /** 搜索次数 */
  searchCount: number
  /** 成功次数 */
  successCount: number
  /** 失败次数 */
  failureCount: number
  /** 成功率 */
  successRate: number
  /** 平均响应时间 */
  averageResponseTime: number
}

/**
 * 热门搜索空间统计
 */
export interface TopSearchSpaceStatistic {
  /** 搜索空间ID */
  searchSpaceId: string
  /** 搜索空间代码 */
  searchSpaceCode: string
  /** 搜索次数 */
  searchCount: number
  /** 成功次数 */
  successCount: number
  /** 失败次数 */
  failureCount: number
  /** 成功率 */
  successRate: number
  /** 平均响应时间 */
  averageResponseTime: number
}

/**
 * 热门用户统计
 */
export interface TopUserStatistic {
  /** 用户ID */
  userId: string
  /** 搜索次数 */
  searchCount: number
  /** 成功次数 */
  successCount: number
  /** 失败次数 */
  failureCount: number
  /** 成功率 */
  successRate: number
  /** 平均响应时间 */
  averageResponseTime: number
}

/**
 * 按小时统计
 */
export interface HourlyStatistic {
  /** 小时 (0-23) */
  hour: number
  /** 搜索次数 */
  searchCount: number
  /** 成功次数 */
  successCount: number
  /** 失败次数 */
  failureCount: number
  /** 成功率 */
  successRate: number
  /** 平均响应时间 */
  averageResponseTime: number
}

// ============= 前端界面状态类型 =============

/**
 * 热词筛选条件
 */
export interface HotWordFilter {
  /** 时间范围 */
  dateRange: {
    start: Date
    end: Date
  }
  /** 用户ID */
  userId?: string
  /** 搜索空间ID */
  searchSpaceId?: string
  /** 热词数量限制 */
  limit: number
  /** 最小词长 */
  minWordLength: number
  /** 是否排除停用词 */
  excludeStopWords: boolean
  /** 是否包含分词详情 */
  includeSegmentDetails: boolean
}

/**
 * 热词显示选项
 */
export interface HotWordDisplayOptions {
  /** 显示模式：列表/词云/排行榜 */
  viewMode: 'list' | 'cloud' | 'ranking'
  /** 排序方式 */
  sortBy: 'count' | 'percentage' | 'length' | 'lastOccurrence'
  /** 排序方向 */
  sortOrder: 'asc' | 'desc'
  /** 是否显示分词详情 */
  showSegmentDetails: boolean
  /** 是否显示统计信息 */
  showStatistics: boolean
}

/**
 * 热词统计页面状态
 */
export interface HotWordPageState {
  /** 热词数据 */
  hotWords: HotWordResponse[]
  /** 统计数据 */
  statistics?: HotWordStatisticsResponse
  /** 筛选条件 */
  filter: HotWordFilter
  /** 显示选项 */
  displayOptions: HotWordDisplayOptions
  /** 加载状态 */
  loading: boolean
  /** 错误信息 */
  error: string | null
  /** 最后更新时间 */
  lastUpdated?: Date
}

// ============= 词云图相关类型 =============

/**
 * 词云图数据项
 */
export interface WordCloudItem {
  /** 词语 */
  text: string
  /** 权重 */
  weight: number
  /** 颜色 */
  color?: string
  /** 额外数据 */
  data?: HotWordResponse
}

/**
 * 词云图配置
 */
export interface WordCloudConfig {
  /** 画布宽度 */
  width: number
  /** 画布高度 */
  height: number
  /** 最小字体大小 */
  minSize: number
  /** 最大字体大小 */
  maxSize: number
  /** 字体系列 */
  fontFamily: string
  /** 背景色 */
  backgroundColor: string
  /** 颜色方案 */
  colorScheme: string[]
  /** 是否旋转 */
  enableRotation: boolean
  /** 最小旋转角度 */
  minRotation: number
  /** 最大旋转角度 */
  maxRotation: number
}

// ============= 通用API响应类型 =============

/**
 * API响应基础类型
 */
export interface ApiResponse<T = any> {
  /** 是否成功 */
  success: boolean
  /** 响应数据 */
  data: T
  /** 响应消息 */
  message?: string
  /** 错误代码 */
  code?: string
  /** 时间戳 */
  timestamp?: number
}