import axios, { type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

/**
 * HTTP错误类型
 */
export interface HttpError extends Error {
  status?: number
  code?: string
  response?: AxiosResponse
}

/**
 * 重试配置
 */
interface RetryConfig {
  retries: number
  retryDelay: number
  retryCondition: (error: AxiosError) => boolean
}

/**
 * 请求队列项
 */
interface RequestQueueItem {
  resolve: (value: any) => void
  reject: (reason: any) => void
  config: InternalAxiosRequestConfig
}

// Token刷新状态
let isRefreshing = false
let failedQueue: RequestQueueItem[] = []

// 处理队列中的请求
const processQueue = (error: AxiosError | null, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error)
    } else {
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      resolve(axios(config))
    }
  })

  failedQueue = []
}

// 创建axios实例
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  }
})

// 添加调试信息
console.log('HTTP配置 - 现在使用直接API URL:', {
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  isDev: import.meta.env.DEV
})

// 请求拦截器
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()

    // 添加请求时间戳用于超时检测
    ;(config as any).metadata = { startTime: new Date() }

    // 添加认证token
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }

    // 添加请求ID用于追踪
    config.headers['X-Request-ID'] = `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

    return config
  },
  (error: AxiosError) => {
    console.error('Request interceptor error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse) => {
    // 计算请求耗时
    const config = response.config as InternalAxiosRequestConfig & { metadata?: { startTime: Date } }
    if (config.metadata?.startTime) {
      const duration = new Date().getTime() - config.metadata.startTime.getTime()
      console.debug(`Request ${config.headers?.['X-Request-ID']} completed in ${duration}ms`)
    }

    return response.data
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    const authStore = useAuthStore()

    // 记录错误信息
    console.error('HTTP Error:', {
      status: error.response?.status,
      message: error.message,
      url: originalRequest?.url,
      method: originalRequest?.method
    })

    // 处理401错误 - Token过期或无效
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果正在刷新token，将请求加入队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject, config: originalRequest })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 尝试刷新token
        const newToken = await authStore.refreshTokenAsync()

        if (newToken) {
          // 刷新成功，更新Authorization头并重新发送原请求
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          processQueue(null, newToken)
          return axios(originalRequest)
        } else {
          throw new Error('Token refresh failed')
        }
      } catch (refreshError) {
        // 刷新失败，清除认证信息并重定向到登录页
        processQueue(refreshError as AxiosError)
        authStore.logout()

        // 如果不是在登录页，重定向到登录页
        if (router.currentRoute.value.name !== 'Login') {
          router.push({
            name: 'Login',
            query: { redirect: router.currentRoute.value.fullPath }
          })
        }

        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 处理403错误 - 权限不足
    if (error.response?.status === 403) {
      // 可以显示权限不足的提示
      console.warn('Access forbidden - insufficient permissions')
    }

    // 处理网络错误和超时
    if (error.code === 'NETWORK_ERROR' || error.code === 'ECONNABORTED') {
      console.error('Network error or timeout:', error.message)
    }

    // 处理服务器错误 (5xx)
    if (error.response?.status && error.response.status >= 500) {
      console.error('Server error:', error.response.status, error.response.statusText)
    }

    // 创建格式化的错误对象
    const httpError: HttpError = new Error(
      (error.response?.data as any)?.message ||
      error.message ||
      '请求失败'
    )
    httpError.status = error.response?.status
    httpError.code = error.code
    httpError.response = error.response

    return Promise.reject(httpError)
  }
)

/**
 * 添加重试功能的请求方法
 */
export const httpWithRetry = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

// 为重试实例添加重试拦截器
httpWithRetry.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as InternalAxiosRequestConfig & {
      retryConfig?: RetryConfig
      _retryCount?: number
    }

    const retryConfig: RetryConfig = config.retryConfig || {
      retries: 3,
      retryDelay: 1000,
      retryCondition: (error) => {
        return !error.response || (error.response.status >= 500 && error.response.status <= 599)
      }
    }

    config._retryCount = config._retryCount || 0

    if (config._retryCount < retryConfig.retries && retryConfig.retryCondition(error)) {
      config._retryCount++

      // 指数退避延迟
      const delay = retryConfig.retryDelay * Math.pow(2, config._retryCount - 1)

      console.log(`Retrying request (${config._retryCount}/${retryConfig.retries}) after ${delay}ms`)

      await new Promise(resolve => setTimeout(resolve, delay))

      return httpWithRetry(config)
    }

    return Promise.reject(error)
  }
)

/**
 * 取消令牌管理器
 */
class CancelTokenManager {
  private cancelTokens = new Map<string, AbortController>()

  create(key: string): AbortSignal {
    this.cancel(key) // 取消之前的请求
    const controller = new AbortController()
    this.cancelTokens.set(key, controller)
    return controller.signal
  }

  cancel(key: string): void {
    const controller = this.cancelTokens.get(key)
    if (controller) {
      controller.abort()
      this.cancelTokens.delete(key)
    }
  }

  cancelAll(): void {
    this.cancelTokens.forEach(controller => controller.abort())
    this.cancelTokens.clear()
  }
}

export const cancelTokenManager = new CancelTokenManager()

export default http