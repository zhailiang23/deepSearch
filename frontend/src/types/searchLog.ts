/**
 * 搜索日志相关类型定义
 */

// API 响应基础接口
export interface ApiResponse<T = any> {
  success: boolean
  data: T
  message?: string
  timestamp?: string
}

// 分页响应接口
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// 点击追踪数据接口
export interface ClickTrackingData {
  searchLogId: number
  documentId: string
  documentTitle: string
  documentUrl: string
  clickPosition: number
  clickTime: string
  userAgent: string
  clickType: string
  modifierKeys: {
    ctrl: boolean
    shift: boolean
    alt: boolean
  }
}

// 点击记录请求接口
export interface ClickRecordRequest {
  searchLogId: number
  documentId: string
  documentTitle: string
  documentUrl: string
  clickPosition: number
  clickTime: string
  userAgent?: string
  clickType?: string
  modifierKeys?: {
    ctrl: boolean
    shift: boolean
    alt: boolean
  }
}

// 搜索日志基础信息
export interface SearchLog {
  id: number
  userId: string
  userIp: string
  searchSpaceId: string
  query: string
  resultCount: number
  responseTime: number
  status: 'SUCCESS' | 'ERROR'
  createdAt: string
  clickCount: number
}

// 搜索日志详细信息
export interface SearchLogDetail extends SearchLog {
  requestParams: string
  responseData: string
  errorMessage?: string
  clickLogs: ClickLog[]
}

// 点击日志信息
export interface ClickLog {
  id: number
  documentId: string
  documentTitle: string
  documentUrl: string
  clickPosition: number
  clickTime: string
  userAgent: string
  clickType: string
}

// 搜索日志查询参数
export interface SearchLogQuery {
  userId?: string
  searchSpaceId?: string
  query?: string
  status?: 'SUCCESS' | 'ERROR'
  startTime?: string
  endTime?: string
  minResponseTime?: number
  maxResponseTime?: number
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
}

// 搜索结果接口 (用于点击追踪)
export interface SearchResult {
  id: string
  title: string
  url: string
  summary?: string
  score?: number
  [key: string]: any
}

// 离线缓存接口
export interface ClickCache {
  [key: string]: ClickTrackingData[]
}

// 追踪状态接口
export interface TrackingState {
  isEnabled: boolean
  offlineCount: number
  lastSyncTime?: string
  errors: TrackingError[]
}

// 追踪错误接口
export interface TrackingError {
  id: string
  message: string
  timestamp: string
  data?: ClickTrackingData
}