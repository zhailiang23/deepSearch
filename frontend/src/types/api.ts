/**
 * 通用API类型定义
 */

/**
 * 统一API响应格式
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

/**
 * 分页响应数据
 */
export interface PageResult<T = any> {
  /** 数据内容 */
  content: T[]
  /** 当前页码 */
  page: number
  /** 每页大小 */
  size: number
  /** 总元素数 */
  totalElements: number
  /** 总页数 */
  totalPages: number
  /** 是否为第一页 */
  first: boolean
  /** 是否为最后一页 */
  last: boolean
}

/**
 * 分页查询参数
 */
export interface PageParams {
  /** 页码（从0开始） */
  page?: number
  /** 每页大小 */
  size?: number
  /** 排序字段 */
  sort?: string
  /** 排序方向 */
  direction?: 'ASC' | 'DESC'
}

/**
 * 时间范围参数
 */
export interface TimeRange {
  /** 开始时间 */
  startTime: Date
  /** 结束时间 */
  endTime: Date
}

/**
 * HTTP错误类型
 */
export interface HttpError extends Error {
  /** HTTP状态码 */
  status?: number
  /** 错误代码 */
  code?: string
  /** 错误响应 */
  response?: any
}

/**
 * 请求配置扩展
 */
export interface RequestConfig {
  /** 错误消息模式 */
  errorMessageMode?: 'none' | 'message' | 'modal'
  /** 错误消息语言 */
  errorMessageLanguage?: 'zh' | 'en'
  /** 是否显示加载状态 */
  showLoading?: boolean
  /** 加载提示文本 */
  loadingText?: string
  /** 请求超时时间 */
  timeout?: number
}