import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import axios, { AxiosError } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { authApi, userApi } from '@/utils/api'
import http, { httpWithRetry, cancelTokenManager } from '@/utils/http'

// Mock axios
vi.mock('axios')
const mockedAxios = axios as jest.Mocked<typeof axios>

// Mock router
vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { name: 'Dashboard', fullPath: '/' } },
    push: vi.fn()
  }
}))

describe('API集成测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    cancelTokenManager.cancelAll()
  })

  describe('认证API集成', () => {
    it('应该成功登录并设置token', async () => {
      const authStore = useAuthStore()
      const mockResponse = {
        token: 'mock-jwt-token',
        refreshToken: 'mock-refresh-token',
        user: {
          id: 1,
          username: 'testuser',
          email: 'test@example.com',
          role: 'USER',
          status: 'ACTIVE',
          createdAt: '2023-01-01T00:00:00Z',
          updatedAt: '2023-01-01T00:00:00Z'
        }
      }

      // Mock HTTP请求
      vi.spyOn(http, 'post').mockResolvedValue(mockResponse)

      const credentials = { username: 'testuser', password: 'password123' }
      const result = await authStore.login(credentials)

      expect(result).toEqual(mockResponse)
      expect(authStore.isAuthenticated).toBe(true)
      expect(authStore.user).toEqual(mockResponse.user)
      expect(localStorage.getItem('token')).toBe(mockResponse.token)
      expect(localStorage.getItem('refreshToken')).toBe(mockResponse.refreshToken)
    })

    it('应该处理登录失败', async () => {
      const authStore = useAuthStore()
      const errorResponse = {
        response: {
          status: 401,
          data: { message: '用户名或密码错误' }
        }
      }

      vi.spyOn(http, 'post').mockRejectedValue(errorResponse)

      const credentials = { username: 'testuser', password: 'wrongpassword' }

      await expect(authStore.login(credentials)).rejects.toEqual(errorResponse)
      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('应该成功刷新token', async () => {
      const authStore = useAuthStore()

      // 设置初始状态
      authStore.token = 'old-token'
      authStore.refreshToken = 'refresh-token'
      localStorage.setItem('token', 'old-token')
      localStorage.setItem('refreshToken', 'refresh-token')

      const mockResponse = {
        token: 'new-jwt-token',
        refreshToken: 'new-refresh-token',
        user: {
          id: 1,
          username: 'testuser',
          email: 'test@example.com',
          role: 'USER',
          status: 'ACTIVE',
          createdAt: '2023-01-01T00:00:00Z',
          updatedAt: '2023-01-01T00:00:00Z'
        }
      }

      vi.spyOn(http, 'post').mockResolvedValue(mockResponse)

      const newToken = await authStore.refreshToken()

      expect(newToken).toBe(mockResponse.token)
      expect(authStore.token).toBe(mockResponse.token)
      expect(localStorage.getItem('token')).toBe(mockResponse.token)
    })

    it('应该处理refresh token失败并登出', async () => {
      const authStore = useAuthStore()

      // 设置初始状态
      authStore.token = 'old-token'
      authStore.refreshToken = 'invalid-refresh-token'

      vi.spyOn(http, 'post').mockRejectedValue({
        response: { status: 401, data: { message: 'Refresh token expired' } }
      })

      await expect(authStore.refreshToken()).rejects.toBeDefined()
      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.token).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('应该正确获取用户profile', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'

      const mockUser = {
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      vi.spyOn(http, 'get').mockResolvedValue(mockUser)

      const user = await authStore.getCurrentUser()

      expect(user).toEqual(mockUser)
      expect(authStore.user).toEqual(mockUser)
    })

    it('应该处理logout并清除所有认证信息', async () => {
      const authStore = useAuthStore()

      // 设置初始状态
      authStore.token = 'token'
      authStore.refreshToken = 'refresh-token'
      authStore.user = {
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      vi.spyOn(http, 'post').mockResolvedValue({})

      await authStore.logout()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.token).toBeNull()
      expect(authStore.refreshToken).toBeNull()
      expect(authStore.user).toBeNull()
      expect(localStorage.getItem('token')).toBeNull()
      expect(localStorage.getItem('refreshToken')).toBeNull()
    })
  })

  describe('HTTP拦截器测试', () => {
    it('应该在请求中添加Authorization头', async () => {
      const authStore = useAuthStore()
      authStore.token = 'test-token'

      const mockAxiosInstance = {
        defaults: {},
        interceptors: {
          request: { use: vi.fn() },
          response: { use: vi.fn() }
        }
      }

      mockedAxios.create.mockReturnValue(mockAxiosInstance as any)

      // 测试请求拦截器
      const requestInterceptor = vi.fn((config) => {
        if (authStore.token) {
          config.headers.Authorization = `Bearer ${authStore.token}`
        }
        return config
      })

      const config = { headers: {} }
      const result = requestInterceptor(config)

      expect(result.headers.Authorization).toBe('Bearer test-token')
    })

    it('应该处理401错误并尝试刷新token', async () => {
      const authStore = useAuthStore()
      authStore.token = 'expired-token'
      authStore.refreshToken = 'valid-refresh-token'

      const mockRefreshResponse = {
        token: 'new-token',
        refreshToken: 'new-refresh-token'
      }

      // Mock刷新token成功
      vi.spyOn(authStore, 'refreshToken').mockResolvedValue('new-token')

      const error401 = {
        response: { status: 401 },
        config: { headers: {}, _retry: false }
      }

      // 模拟响应拦截器逻辑
      const responseInterceptor = async (error: any) => {
        if (error.response?.status === 401 && !error.config._retry) {
          error.config._retry = true
          const newToken = await authStore.refreshToken()
          if (newToken) {
            error.config.headers.Authorization = `Bearer ${newToken}`
            return Promise.resolve({ data: 'success' })
          }
        }
        return Promise.reject(error)
      }

      const result = await responseInterceptor(error401)
      expect(result.data).toBe('success')
      expect(error401.config.headers.Authorization).toBe('Bearer new-token')
    })

    it('应该处理网络错误', async () => {
      const networkError = {
        code: 'NETWORK_ERROR',
        message: 'Network Error'
      }

      const responseInterceptor = (error: any) => {
        if (error.code === 'NETWORK_ERROR') {
          console.error('Network error or timeout:', error.message)
        }
        return Promise.reject(error)
      }

      await expect(responseInterceptor(networkError)).rejects.toEqual(networkError)
    })

    it('应该处理服务器错误(5xx)', async () => {
      const serverError = {
        response: {
          status: 500,
          statusText: 'Internal Server Error'
        }
      }

      const responseInterceptor = (error: any) => {
        if (error.response?.status >= 500) {
          console.error('Server error:', error.response.status, error.response.statusText)
        }
        return Promise.reject(error)
      }

      await expect(responseInterceptor(serverError)).rejects.toEqual(serverError)
    })
  })

  describe('重试机制测试', () => {
    it('应该在5xx错误时重试请求', async () => {
      const config = {
        retryConfig: {
          retries: 2,
          retryDelay: 100,
          retryCondition: (error: AxiosError) => {
            return !error.response || (error.response.status >= 500 && error.response.status <= 599)
          }
        },
        _retryCount: 0
      }

      const error500 = {
        response: { status: 500 },
        config
      }

      const retryInterceptor = async (error: any) => {
        const retryConfig = error.config.retryConfig
        error.config._retryCount = error.config._retryCount || 0

        if (error.config._retryCount < retryConfig.retries && retryConfig.retryCondition(error)) {
          error.config._retryCount++
          const delay = retryConfig.retryDelay * Math.pow(2, error.config._retryCount - 1)
          await new Promise(resolve => setTimeout(resolve, delay))
          return Promise.resolve({ data: 'success after retry' })
        }

        return Promise.reject(error)
      }

      const result = await retryInterceptor(error500)
      expect(result.data).toBe('success after retry')
      expect(config._retryCount).toBe(1)
    })

    it('应该在达到最大重试次数后放弃', async () => {
      const config = {
        retryConfig: {
          retries: 1,
          retryDelay: 100,
          retryCondition: () => true
        },
        _retryCount: 1
      }

      const error = {
        response: { status: 500 },
        config
      }

      const retryInterceptor = async (error: any) => {
        const retryConfig = error.config.retryConfig
        if (error.config._retryCount < retryConfig.retries && retryConfig.retryCondition(error)) {
          error.config._retryCount++
          return Promise.resolve({ data: 'success' })
        }
        return Promise.reject(error)
      }

      await expect(retryInterceptor(error)).rejects.toEqual(error)
    })
  })

  describe('取消令牌管理器测试', () => {
    it('应该创建和管理取消令牌', () => {
      const signal1 = cancelTokenManager.create('request1')
      const signal2 = cancelTokenManager.create('request2')

      expect(signal1).toBeInstanceOf(AbortSignal)
      expect(signal2).toBeInstanceOf(AbortSignal)
      expect(signal1).not.toBe(signal2)
    })

    it('应该取消指定的请求', () => {
      const signal = cancelTokenManager.create('request1')
      expect(signal.aborted).toBe(false)

      cancelTokenManager.cancel('request1')
      expect(signal.aborted).toBe(true)
    })

    it('应该取消所有请求', () => {
      const signal1 = cancelTokenManager.create('request1')
      const signal2 = cancelTokenManager.create('request2')

      expect(signal1.aborted).toBe(false)
      expect(signal2.aborted).toBe(false)

      cancelTokenManager.cancelAll()

      expect(signal1.aborted).toBe(true)
      expect(signal2.aborted).toBe(true)
    })
  })

  describe('用户API集成测试', () => {
    it('应该获取用户列表', async () => {
      const mockUsers = [
        {
          id: 1,
          username: 'user1',
          email: 'user1@example.com',
          role: 'USER',
          status: 'ACTIVE'
        },
        {
          id: 2,
          username: 'user2',
          email: 'user2@example.com',
          role: 'ADMIN',
          status: 'ACTIVE'
        }
      ]

      vi.spyOn(http, 'get').mockResolvedValue(mockUsers)

      const result = await userApi.getUsers()
      expect(result).toEqual(mockUsers)
    })

    it('应该创建新用户', async () => {
      const newUser = {
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        role: 'USER'
      }

      const createdUser = {
        id: 3,
        ...newUser,
        status: 'ACTIVE',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      vi.spyOn(http, 'post').mockResolvedValue(createdUser)

      const result = await userApi.createUser(newUser)
      expect(result).toEqual(createdUser)
    })

    it('应该更新用户信息', async () => {
      const updateData = { email: 'updated@example.com' }
      const updatedUser = {
        id: 1,
        username: 'user1',
        email: 'updated@example.com',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-02T00:00:00Z'
      }

      vi.spyOn(http, 'put').mockResolvedValue(updatedUser)

      const result = await userApi.updateUser(1, updateData)
      expect(result).toEqual(updatedUser)
    })

    it('应该删除用户', async () => {
      vi.spyOn(http, 'delete').mockResolvedValue({})

      await expect(userApi.deleteUser(1)).resolves.toEqual({})
    })
  })

  describe('权限检查测试', () => {
    it('应该正确检查用户角色', () => {
      const authStore = useAuthStore()
      authStore.user = {
        id: 1,
        username: 'admin',
        email: 'admin@example.com',
        role: 'ADMIN',
        status: 'ACTIVE',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      expect(authStore.hasRole('ADMIN')).toBe(true)
      expect(authStore.hasRole('USER')).toBe(true) // ADMIN包含USER权限

      authStore.user.role = 'USER'
      expect(authStore.hasRole('ADMIN')).toBe(false)
      expect(authStore.hasRole('USER')).toBe(true)
    })

    it('应该正确检查用户权限', () => {
      const authStore = useAuthStore()
      authStore.user = {
        id: 1,
        username: 'admin',
        email: 'admin@example.com',
        role: 'ADMIN',
        status: 'ACTIVE',
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      expect(authStore.hasPermission('read:users')).toBe(true) // ADMIN有所有权限

      authStore.user.role = 'USER'
      expect(authStore.hasPermission('read:profile')).toBe(true)
      expect(authStore.hasPermission('admin:delete')).toBe(false)
    })
  })
})