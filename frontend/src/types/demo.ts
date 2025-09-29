/**
 * 移动端搜索演示相关类型定义
 */

// 搜索空间选项
export interface SearchSpaceOption {
  id: string
  name: string
  description?: string
  fields: string[]
  enabled: boolean
  indexCount?: number
  /** 索引状态 */
  indexStatus?: 'healthy' | 'warning' | 'error' | 'unknown'
  /** 文档数量 */
  docCount?: number
}

// 拼音搜索配置
export interface PinyinSearchConfig {
  enabled: boolean
  mode: 'fuzzy' | 'exact' | 'both'
  toneIgnore: boolean
  segmentMatch: boolean
}

// 分页配置
export interface PaginationConfig {
  pageSize: number
  initialLoad: number
  prefetchNext: boolean
}

// 语义搜索配置
export interface SemanticSearchConfig {
  enabled: boolean
  weight: number  // 语义搜索权重，范围 0.0-1.0
  mode: 'AUTO' | 'KEYWORD_FIRST' | 'SEMANTIC_FIRST' | 'HYBRID'
}

// 搜索行为配置
export interface SearchBehaviorConfig {
  debounceMs: number
  minQueryLength: number
  autoSearch: boolean
  highlightMatch: boolean
}

// 结果显示配置
export interface ResultDisplayConfig {
  showMetadata: boolean
  maxContentLength: number
  showScore: boolean
  groupByType: boolean
}

// 搜索演示配置主接口
export interface SearchDemoConfig {
  // 搜索空间配置
  searchSpaces: {
    selected: string[]
    available: SearchSpaceOption[]
    allowMultiple: boolean
  }

  // 拼音搜索配置
  pinyinSearch: PinyinSearchConfig

  // 语义搜索配置
  semanticSearch: SemanticSearchConfig

  // 分页配置
  pagination: PaginationConfig

  // 搜索行为配置
  searchBehavior: SearchBehaviorConfig

  // 结果显示配置
  resultDisplay: ResultDisplayConfig
}

// 配置预设
export interface ConfigPreset {
  id: string
  name: string
  description?: string
  config: SearchDemoConfig
  isBuiltIn: boolean
  createdAt?: string
  updatedAt?: string
}

// 参数变更事件
export interface ParameterChangeEvent {
  type: 'searchSpace' | 'pinyin' | 'semantic' | 'pagination' | 'behavior' | 'display'
  key: string
  value: any
  previous: any
  timestamp: number
}

// 同步状态
export interface SyncStatus {
  pending: boolean
  error?: string
  lastSync: number
}

// 组件Props接口
export interface ParameterPanelProps {
  modelValue: SearchDemoConfig
  collapsed?: boolean
  showPresets?: boolean
  allowReset?: boolean
}

export interface SearchSpaceSelectorProps {
  modelValue: string[]
  options: SearchSpaceOption[]
  multiple?: boolean
  showDetails?: boolean
}

export interface PinyinSearchConfigProps {
  modelValue: PinyinSearchConfig
  disabled?: boolean
}

export interface PagingConfigProps {
  modelValue: PaginationConfig
  maxPageSize?: number
  minPageSize?: number
}

export interface ConfigManagerProps {
  presets: ConfigPreset[]
  currentConfig: SearchDemoConfig
  allowSave?: boolean
  allowDelete?: boolean
}

// 默认配置
export const DEFAULT_SEARCH_DEMO_CONFIG: SearchDemoConfig = {
  searchSpaces: {
    selected: [],
    available: [],
    allowMultiple: true
  },
  pinyinSearch: {
    enabled: true,
    mode: 'both',
    toneIgnore: true,
    segmentMatch: true
  },
  semanticSearch: {
    enabled: true,
    weight: 0.3,
    mode: 'AUTO'
  },
  pagination: {
    pageSize: 20,
    initialLoad: 20,
    prefetchNext: false
  },
  searchBehavior: {
    debounceMs: 300,
    minQueryLength: 1,
    autoSearch: true,
    highlightMatch: true
  },
  resultDisplay: {
    showMetadata: false,
    maxContentLength: 200,
    showScore: false,
    groupByType: false
  }
}

// 搜索结果项
export interface SearchResult {
  /** 文档ID */
  id: string
  /** 标题 */
  title: string
  /** 内容摘要 */
  summary: string
  /** 高亮信息 */
  highlight?: Record<string, string[]>
  /** 评分 */
  score?: number
  /** 源数据 */
  source: Record<string, any>
  /** 索引名 */
  index: string
  /** 文档类型 */
  type?: string
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

// 搜索响应
export interface SearchResponse {
  /** 搜索结果 */
  results: SearchResult[]
  /** 总命中数 */
  total: number
  /** 搜索耗时 */
  duration: number
  /** 是否有更多数据 */
  hasMore: boolean
  /** 当前页码 */
  page: number
  /** 分页大小 */
  size: number
  /** 建议词 */
  suggestions?: string[]
  /** 聚合信息 */
  aggregations?: Record<string, any>
  /** 搜索日志ID */
  searchLogId?: number
}

// 搜索状态
export interface SearchState {
  /** 加载状态 */
  loading: boolean
  /** 错误信息 */
  error: string | null
  /** 当前查询 */
  query: string
  /** 是否有更多数据 */
  hasMore: boolean
  /** 当前页码 */
  page: number
  /** 总数 */
  total: number
  /** 搜索耗时（毫秒） */
  duration: number
  /** 搜索ID（用于取消请求） */
  searchId?: string
}

// 搜索历史项
export interface SearchHistoryItem {
  /** 查询词 */
  query: string
  /** 时间戳 */
  timestamp: number
  /** 结果数量 */
  resultCount: number
  /** 搜索配置快照 */
  config: Partial<SearchDemoConfig>
  /** 搜索耗时 */
  duration?: number
  /** 搜索空间 */
  searchSpace?: SearchSpaceOption
}

// 搜索性能指标
export interface SearchPerformanceMetrics {
  /** 搜索次数 */
  searchCount: number
  /** 平均响应时间 */
  averageResponseTime: number
  /** 缓存命中率 */
  cacheHitRate: number
  /** 错误率 */
  errorRate: number
  /** 最后搜索时间 */
  lastSearchTime: number
  /** 最慢搜索时间 */
  slowestSearchTime: number
  /** 最快搜索时间 */
  fastestSearchTime: number
  /** 成功搜索次数 */
  successCount: number
  /** 失败搜索次数 */
  errorCount: number
}

// 缓存条目
export interface SearchCacheEntry {
  /** 搜索结果 */
  results: SearchResult[]
  /** 搜索配置 */
  config: SearchDemoConfig
  /** 时间戳 */
  timestamp: number
  /** 命中次数 */
  hitCount: number
  /** 数据大小 */
  size: number
  /** 过期时间 */
  expiresAt: number
  /** 查询关键词 */
  query: string
}

// 参数冲突
export interface ParameterConflict {
  /** 参数路径 */
  path: string
  /** 面板值 */
  panelValue: any
  /** 移动端值 */
  mobileValue: any
  /** 解决策略 */
  resolution: 'usePanel' | 'useMobile' | 'merge' | 'ask'
  /** 冲突描述 */
  description?: string
}

// 搜索 API 适配器接口
export interface SearchApiAdapter {
  /** 执行搜索 */
  search: (query: string, config: SearchDemoConfig, options?: { page?: number; signal?: AbortSignal }) => Promise<SearchResponse>
  /** 获取搜索建议 */
  suggest: (query: string) => Promise<string[]>
  /** 获取搜索空间列表 */
  getSpaces: () => Promise<SearchSpaceOption[]>
  /** 验证配置 */
  validateConfig: (config: SearchDemoConfig) => Promise<ValidationResult>
  /** 取消搜索 */
  cancelSearch: (searchId: string) => Promise<void>
}

// 参数变更类型
export interface ParameterChange {
  path: string
  oldValue: any
  newValue: any
  source: 'panel' | 'mobile' | 'url' | 'api'
  timestamp: number
}

// 同步状态类型
export interface SyncStatus {
  pending: boolean
  error?: string
  lastSync: number
}

// 验证结果
export interface ValidationResult {
  /** 是否有效 */
  valid: boolean
  /** 错误信息 */
  errors: Array<{
    field: string
    message: string
    code?: string
  }>
  /** 警告信息 */
  warnings: Array<{
    field: string
    message: string
    code?: string
  }>
}

// 内置预设配置
export const BUILTIN_PRESETS: ConfigPreset[] = [
  {
    id: 'fast-search',
    name: '快速搜索',
    description: '针对快速响应优化的搜索配置',
    isBuiltIn: true,
    config: {
      ...DEFAULT_SEARCH_DEMO_CONFIG,
      searchBehavior: {
        debounceMs: 100,
        minQueryLength: 1,
        autoSearch: true,
        highlightMatch: true
      },
      pagination: {
        pageSize: 10,
        initialLoad: 10,
        prefetchNext: true
      }
    }
  },
  {
    id: 'detailed-search',
    name: '详细搜索',
    description: '显示完整信息的搜索配置',
    isBuiltIn: true,
    config: {
      ...DEFAULT_SEARCH_DEMO_CONFIG,
      resultDisplay: {
        showMetadata: true,
        maxContentLength: 500,
        showScore: true,
        groupByType: true
      },
      pagination: {
        pageSize: 5,
        initialLoad: 5,
        prefetchNext: false
      }
    }
  },
  {
    id: 'pinyin-search',
    name: '拼音搜索',
    description: '启用拼音搜索功能的配置',
    isBuiltIn: true,
    config: {
      ...DEFAULT_SEARCH_DEMO_CONFIG,
      pinyinSearch: {
        enabled: true,
        mode: 'both',
        toneIgnore: true,
        segmentMatch: true
      },
      searchBehavior: {
        debounceMs: 500,
        minQueryLength: 1,
        autoSearch: true,
        highlightMatch: true
      }
    }
  }
]