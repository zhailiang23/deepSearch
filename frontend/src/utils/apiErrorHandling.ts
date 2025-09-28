/**
 * API错误处理工具
 * 提供统一的错误处理、错误信息格式化、错误恢复等功能
 */

import type { AxiosError } from 'axios'
import type { HttpError, ApiResponse } from '@/types/api'

// ============= 错误类型定义 =============

/**
 * 错误级别
 */
export type ErrorLevel = 'info' | 'warning' | 'error' | 'critical'

/**
 * 错误分类
 */
export type ErrorCategory =
  | 'network'       // 网络错误
  | 'timeout'       // 超时错误
  | 'auth'          // 认证错误
  | 'permission'    // 权限错误
  | 'validation'    // 参数验证错误
  | 'business'      // 业务逻辑错误
  | 'server'        // 服务器错误
  | 'unknown'       // 未知错误

/**
 * 格式化的错误信息
 */
export interface FormattedError {
  /** 错误ID */
  id: string
  /** 错误消息 */
  message: string
  /** 用户友好的消息 */
  userMessage: string
  /** 错误代码 */
  code?: string
  /** HTTP状态码 */
  status?: number
  /** 错误级别 */
  level: ErrorLevel
  /** 错误分类 */
  category: ErrorCategory
  /** 是否可重试 */
  retryable: boolean
  /** 建议的操作 */
  suggestedActions?: string[]
  /** 原始错误 */
  originalError: any
  /** 时间戳 */
  timestamp: number
}

/**
 * 错误处理选项
 */
export interface ErrorHandlingOptions {
  /** 是否显示错误提示 */
  showMessage?: boolean
  /** 错误消息模式 */
  messageMode?: 'toast' | 'modal' | 'notification' | 'console'
  /** 是否记录错误日志 */
  logError?: boolean
  /** 是否上报错误 */
  reportError?: boolean
  /** 自定义错误处理器 */
  customHandler?: (error: FormattedError) => void
}

// ============= 错误分类器 =============

/**
 * 分析错误类型
 */
export function categorizeError(error: any): ErrorCategory {
  // Axios错误
  if (error.isAxiosError || error.response) {
    const status = error.response?.status

    if (!status) {
      // 网络错误或请求被取消
      if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
        return 'timeout'
      }
      return 'network'
    }

    // 根据HTTP状态码分类
    if (status === 401) return 'auth'
    if (status === 403) return 'permission'
    if (status >= 400 && status < 500) return 'validation'
    if (status >= 500) return 'server'
  }

  // 业务错误
  if (error.code && typeof error.code === 'string') {
    if (error.code.includes('BUSINESS') || error.code.includes('BIZ')) {
      return 'business'
    }
  }

  return 'unknown'
}

/**
 * 确定错误级别
 */
export function determineErrorLevel(category: ErrorCategory, status?: number): ErrorLevel {
  switch (category) {
    case 'network':
    case 'timeout':
      return 'warning'
    case 'auth':
    case 'permission':
      return 'warning'
    case 'validation':
      return 'info'
    case 'business':
      return 'error'
    case 'server':
      return status && status >= 500 ? 'critical' : 'error'
    default:
      return 'error'
  }
}

/**
 * 判断错误是否可重试
 */
export function isRetryableError(category: ErrorCategory, status?: number): boolean {
  switch (category) {
    case 'network':
    case 'timeout':
      return true
    case 'server':
      return status === 503 || status === 502 || status === 504
    default:
      return false
  }
}

// ============= 错误消息格式化 =============

/**
 * 格式化错误消息
 */
export function formatErrorMessage(error: any): string {
  // 如果是已格式化的API响应
  if (error.response?.data?.message) {
    return error.response.data.message
  }

  // Axios错误
  if (error.isAxiosError) {
    const status = error.response?.status

    if (!status) {
      if (error.code === 'ECONNABORTED') {
        return '请求超时，请检查网络连接'
      }
      return '网络连接失败，请检查网络设置'
    }

    switch (status) {
      case 400:
        return '请求参数错误'
      case 401:
        return '登录已过期，请重新登录'
      case 403:
        return '没有权限访问此资源'
      case 404:
        return '请求的资源不存在'
      case 408:
        return '请求超时'
      case 409:
        return '资源冲突'
      case 422:
        return '数据验证失败'
      case 429:
        return '请求过于频繁，请稍后再试'
      case 500:
        return '服务器内部错误'
      case 502:
        return '网关错误'
      case 503:
        return '服务暂时不可用'
      case 504:
        return '网关超时'
      default:
        return `服务器错误 (${status})`
    }
  }

  // 普通错误对象
  if (error.message) {
    return error.message
  }

  // 字符串错误
  if (typeof error === 'string') {
    return error
  }

  return '未知错误'
}

/**
 * 生成用户友好的错误消息
 */
export function generateUserFriendlyMessage(category: ErrorCategory, originalMessage: string): string {
  switch (category) {
    case 'network':
      return '网络连接异常，请检查网络设置后重试'
    case 'timeout':
      return '请求超时，请稍后重试'
    case 'auth':
      return '登录状态已过期，请重新登录'
    case 'permission':
      return '您没有权限执行此操作，请联系管理员'
    case 'validation':
      return '输入的数据格式不正确，请检查后重试'
    case 'business':
      return originalMessage || '操作失败，请稍后重试'
    case 'server':
      return '服务器繁忙，请稍后重试'
    default:
      return '发生了意外错误，请刷新页面后重试'
  }
}

/**
 * 生成错误处理建议
 */
export function generateSuggestedActions(category: ErrorCategory, retryable: boolean): string[] {
  const actions: string[] = []

  switch (category) {
    case 'network':
      actions.push('检查网络连接')
      actions.push('尝试刷新页面')
      break
    case 'timeout':
      actions.push('检查网络速度')
      actions.push('稍后重试')
      break
    case 'auth':
      actions.push('重新登录')
      break
    case 'permission':
      actions.push('联系管理员获取权限')
      break
    case 'validation':
      actions.push('检查输入数据格式')
      actions.push('查看帮助文档')
      break
    case 'business':
      actions.push('检查操作条件')
      actions.push('联系技术支持')
      break
    case 'server':
      actions.push('稍后重试')
      actions.push('联系技术支持')
      break
    default:
      actions.push('刷新页面')
      actions.push('清除浏览器缓存')
  }

  if (retryable) {
    actions.unshift('点击重试按钮')
  }

  return actions
}

// ============= 主要处理函数 =============

/**
 * 格式化错误对象
 */
export function formatError(error: any): FormattedError {
  const category = categorizeError(error)
  const status = error.response?.status
  const level = determineErrorLevel(category, status)
  const retryable = isRetryableError(category, status)
  const message = formatErrorMessage(error)
  const userMessage = generateUserFriendlyMessage(category, message)
  const suggestedActions = generateSuggestedActions(category, retryable)

  return {
    id: `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    message,
    userMessage,
    code: error.response?.data?.code || error.code,
    status,
    level,
    category,
    retryable,
    suggestedActions,
    originalError: error,
    timestamp: Date.now()
  }
}

/**
 * 处理API错误
 */
export function handleApiError(
  error: any,
  options: ErrorHandlingOptions = {}
): FormattedError {
  const {
    showMessage = true,
    messageMode = 'toast',
    logError = true,
    reportError = false,
    customHandler
  } = options

  // 格式化错误
  const formattedError = formatError(error)

  // 记录错误日志
  if (logError) {
    console.error('API Error:', {
      id: formattedError.id,
      category: formattedError.category,
      level: formattedError.level,
      message: formattedError.message,
      status: formattedError.status,
      error: formattedError.originalError
    })
  }

  // 上报错误（可选）
  if (reportError) {
    reportErrorToService(formattedError).catch(console.error)
  }

  // 显示错误消息
  if (showMessage) {
    showErrorMessage(formattedError, messageMode)
  }

  // 自定义处理器
  if (customHandler) {
    customHandler(formattedError)
  }

  return formattedError
}

// ============= 错误显示 =============

/**
 * 显示错误消息
 */
function showErrorMessage(error: FormattedError, mode: string) {
  switch (mode) {
    case 'toast':
      // 这里应该调用UI库的toast组件
      console.warn(`Toast: ${error.userMessage}`)
      break
    case 'modal':
      // 这里应该调用UI库的modal组件
      console.warn(`Modal: ${error.userMessage}`)
      break
    case 'notification':
      // 这里应该调用UI库的notification组件
      console.warn(`Notification: ${error.userMessage}`)
      break
    case 'console':
      console.error(`Console: ${error.userMessage}`)
      break
  }
}

// ============= 错误上报 =============

/**
 * 上报错误到服务器
 */
async function reportErrorToService(error: FormattedError): Promise<void> {
  try {
    // 这里应该调用错误上报API
    // await errorReportingApi.report(error)
    console.log('Error reported:', error.id)
  } catch (reportError) {
    console.error('Failed to report error:', reportError)
  }
}

// ============= 工具函数 =============

/**
 * 创建错误处理器
 */
export function createErrorHandler(defaultOptions: ErrorHandlingOptions = {}) {
  return (error: any, options?: ErrorHandlingOptions) => {
    return handleApiError(error, { ...defaultOptions, ...options })
  }
}

/**
 * 是否为API错误
 */
export function isApiError(error: any): boolean {
  return error?.response?.data?.success === false
}

/**
 * 提取错误代码
 */
export function extractErrorCode(error: any): string | undefined {
  return error?.response?.data?.code || error?.code
}

/**
 * 提取错误消息
 */
export function extractErrorMessage(error: any): string {
  return formatErrorMessage(error)
}

// ============= 导出默认处理器 =============

/**
 * 默认错误处理器
 */
export const defaultErrorHandler = createErrorHandler({
  showMessage: true,
  messageMode: 'toast',
  logError: true,
  reportError: false
})