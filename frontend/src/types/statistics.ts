/**
 * 统计相关类型定义
 * 包含热词统计、词云图、排行榜等功能的类型定义
 */

// ============= 基础统计类型 =============

/**
 * 热词数据项
 */
export interface HotWordItem {
  /** 词语文本 */
  text: string
  /** 权重/频次 */
  weight: number
  /** 额外数据 */
  extraData?: Record<string, any>
  /** 关键词 (兼容旧代码) */
  keyword?: string
  /** 排名 */
  rank?: number
  /** 搜索次数 */
  count?: number
  /** 趋势 */
  trend?: 'up' | 'down' | 'stable' | 'rising' | 'falling' | 'new'
  /** 百分比 */
  percentage?: number
}

/**
 * 热词统计筛选条件
 */
export interface HotWordFilter {
  /** 日期范围 */
  dateRange?: {
    start: string
    end: string
  }
  /** 关键词 */
  keyword?: string
  /** 最小权重 */
  minWeight?: number
  /** 最大权重 */
  maxWeight?: number
  /** 限制数量 */
  limit?: number
}

/**
 * 热词统计响应数据
 */
export interface HotWordStatistics {
  /** 热词列表 */
  words: HotWordItem[]
  /** 总数量 */
  total: number
  /** 更新时间 */
  updatedAt: string
}

// ============= 词云图类型 =============

/**
 * wordcloud2.js 配置选项
 */
export interface WordCloudOptions {
  /** 画布宽度 */
  width?: number
  /** 画布高度 */
  height?: number
  /** 字体系列 */
  fontFamily?: string
  /** 字体权重 */
  fontWeight?: string | ((word: string, weight: number, fontSize: number, extraData?: any) => string)
  /** 文字颜色 */
  color?: string | ((word: string, weight: number, fontSize: number, distance: number, theta: number) => string)
  /** 最小字体大小 */
  minSize?: number
  /** 权重因子 */
  weightFactor?: number | ((size: number) => number)
  /** 背景颜色 */
  backgroundColor?: string
  /** 是否清除画布 */
  clearCanvas?: boolean
  /** 网格大小 */
  gridSize?: number
  /** 原点位置 */
  origin?: [number, number]
  /** 是否允许绘制超出边界 */
  drawOutOfBound?: boolean
  /** 是否收缩以适应 */
  shrinkToFit?: boolean
  /** 最小旋转角度 */
  minRotation?: number
  /** 最大旋转角度 */
  maxRotation?: number
  /** 旋转步数 */
  rotationSteps?: number
  /** 旋转比例 */
  rotateRatio?: number
  /** 是否随机排序 */
  shuffle?: boolean
  /** 形状 */
  shape?: string | ((theta: number) => number)
  /** 椭圆度 */
  ellipticity?: number
  /** 绘制等待时间 */
  wait?: number
  /** 中止阈值 */
  abortThreshold?: number
  /** 中止回调 */
  abort?: () => void
  /** 点击回调 */
  click?: (item: any, dimension: any, event: Event) => void
  /** 悬停回调 */
  hover?: (item: any, dimension: any, event: Event) => void
}

/**
 * 词云主题配置
 */
export interface WordCloudTheme {
  /** 主题名称 */
  name: string
  /** 颜色配置 */
  colors: {
    /** 主色调 */
    primary: readonly string[]
    /** 辅助色调 */
    secondary: readonly string[]
    /** 背景色 */
    background: string
  }
  /** 字体配置 */
  fonts: {
    /** 字体系列 */
    family: string
    /** 字体权重 */
    weight: string
  }
}

/**
 * 词云组件属性
 */
export interface WordCloudProps {
  /** 词语数据 */
  words: HotWordItem[]
  /** 画布宽度 */
  width?: number
  /** 画布高度 */
  height?: number
  /** 词云配置选项 */
  options?: Partial<WordCloudOptions>
  /** 主题 */
  theme?: WordCloudTheme
  /** 是否响应式 */
  responsive?: boolean
  /** 加载状态 */
  loading?: boolean
  /** 错误状态 */
  error?: string | null
}

/**
 * 词云容器尺寸
 */
export interface WordCloudDimensions {
  /** 宽度 */
  width: number
  /** 高度 */
  height: number
  /** 设备像素比 */
  devicePixelRatio: number
}

/**
 * 词云渲染状态
 */
export interface WordCloudRenderState {
  /** 是否正在渲染 */
  isRendering: boolean
  /** 渲染进度 */
  progress: number
  /** 渲染开始时间 */
  startTime?: number
  /** 渲染结束时间 */
  endTime?: number
  /** 渲染错误 */
  error?: string | null
}

/**
 * 词云事件类型
 */
export interface WordCloudEvents {
  /** 词语点击事件 */
  onWordClick?: (word: HotWordItem, event: Event) => void
  /** 词语悬停事件 */
  onWordHover?: (word: HotWordItem, event: Event) => void
  /** 渲染开始事件 */
  onRenderStart?: () => void
  /** 渲染完成事件 */
  onRenderComplete?: () => void
  /** 渲染错误事件 */
  onRenderError?: (error: string) => void
}

/**
 * 词云性能配置
 */
export interface WordCloudPerformanceConfig {
  /** 分批渲染大小 */
  batchSize: number
  /** 渲染延迟 */
  renderDelay: number
  /** 最大渲染时间 */
  maxRenderTime: number
  /** 防抖延迟 */
  debounceDelay: number
}

// ============= 排行榜类型 =============

/**
 * 排行榜项目
 */
export interface RankingItem extends HotWordItem {
  /** 排名 */
  rank: number
  /** 变化趋势 */
  trend?: 'up' | 'down' | 'stable' | 'rising' | 'falling' | 'new'
  /** 变化值 */
  change?: number
  /** 排名变化 */
  rankChange?: number
  /** 增长率 */
  growthRate?: number
  /** 分类 */
  category?: string
  /** 标签 */
  tags?: string[]
}

/**
 * 排行榜配置
 */
export interface RankingTableConfig {
  /** 显示数量 */
  pageSize: number
  /** 是否显示趋势 */
  showTrend: boolean
  /** 是否显示变化值 */
  showChange: boolean
  /** 是否可点击 */
  clickable: boolean
}

// ============= 综合统计页面类型 =============

/**
 * 统计页面状态
 */
export interface StatisticsPageState {
  /** 筛选条件 */
  filter: HotWordFilter
  /** 词云配置 */
  wordCloudConfig: WordCloudProps
  /** 排行榜配置 */
  rankingConfig: RankingTableConfig
  /** 加载状态 */
  loading: boolean
  /** 错误信息 */
  error: string | null
}

/**
 * 热词统计数据结构(HotWordStatisticsPage.vue 中使用的格式)
 */
export interface HotWordStatisticsItem {
  /** 关键词 */
  keyword: string
  /** 搜索次数/计数 */
  count: number
  /** 排名 */
  rank: number
  /** 趋势 */
  trend?: 'up' | 'down' | 'rising' | 'falling' | 'stable' | 'new'
  /** 百分比 */
  percentage?: number
  /** 最后搜索时间 (可选,兼容旧代码) */
  lastSearchTime?: string
  /** 相关渠道 (可选,兼容旧代码) */
  channels?: string[]
  /** 增长率 (可选,兼容旧代码) */
  growthRate?: number
  /** 搜索次数 (可选,兼容旧代码) */
  searchCount?: number
}

/**
 * 筛选配置
 */
export interface FilterConfig {
  /** 时间范围 */
  timeRange: {
    start: Date
    end: Date
  }
  /** 搜索条件 */
  searchCondition: string
  /** 热词数量限制 */
  hotWordLimit: number
  /** 渠道筛选 */
  channels: string[]
}

/**
 * 统计API请求参数
 */
export interface StatisticsQueryParams {
  /** 开始时间 */
  startTime: string
  /** 结束时间 */
  endTime: string
  /** 搜索条件 */
  searchCondition?: string
  /** 数量限制 */
  limit?: number
  /** 渠道列表 */
  channels?: string[]
}
// ============= 其他缺失的导出类型 =============

/** 热词查询参数 */
export interface HotWordQueryParams {
  keyword?: string
  startTime?: string
  endTime?: string
  limit?: number
  offset?: number
}

/** 热词统计响应 */
export interface HotWordStatisticsResponse {
  data: HotWordStatistics[]
  total: number
}

/** 排序配置 */
export interface SortConfig {
  field: string
  order: 'asc' | 'desc'
}

/** 分页配置 */
export interface PaginationConfig {
  page: number
  pageSize: number
  total: number
  totalPages?: number
}

/** 筛选状态 */
export interface FilterState {
  keyword: string
  dateRange: [Date, Date] | null
  channels: string[]
}

/** 组件状态 */
export interface ComponentState {
  loading: boolean
  error: string | null
  data: any
}

/** 词云配置 */
export interface WordCloudConfig {
  width: number
  height: number
  options: Partial<WordCloudOptions>
}

/** 词云项目 (别名) */
export type WordCloudItem = HotWordItem

/** 词云尺寸 (别名) */
export type WordCloudDimension = WordCloudDimensions

/** 词云统计信息 */
export interface WordCloudStats {
  totalWords: number
  renderedWords: number
  renderTime: number
}
