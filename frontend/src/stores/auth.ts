import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { User, LoginRequest, LoginResponse } from '@/types/auth'
import { authApi } from '@/utils/api'

/**
 * Token信息接口
 */
interface TokenInfo {
  token: string
  refreshToken?: string
  expiresAt?: number
}

/**
 * 认证状态
 */
type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated' | 'refreshing'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem('token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const user = ref<User | null>(null)
  const loading = ref(false)
  const status = ref<AuthStatus>('idle')
  const tokenExpiresAt = ref<number | null>(
    localStorage.getItem('tokenExpiresAt')
      ? parseInt(localStorage.getItem('tokenExpiresAt')!)
      : null
  )

  // 计算属性
  const isAuthenticated = computed(() => !!token.value && status.value !== 'unauthenticated')
  const isTokenExpired = computed(() => {
    if (!tokenExpiresAt.value) return false
    return Date.now() >= tokenExpiresAt.value
  })
  const tokenExpiresIn = computed(() => {
    if (!tokenExpiresAt.value) return 0
    return Math.max(0, tokenExpiresAt.value - Date.now())
  })

  // Token过期监听
  let refreshTimer: number | null = null

  const setupTokenRefresh = () => {
    if (refreshTimer) {
      clearTimeout(refreshTimer)
    }

    if (!tokenExpiresAt.value) return

    const timeUntilRefresh = tokenExpiresAt.value - Date.now() - 5 * 60 * 1000 // 提前5分钟刷新

    if (timeUntilRefresh > 0) {
      refreshTimer = setTimeout(() => {
        if (isAuthenticated.value && !isTokenExpired.value) {
          refreshTokenSilently()
        }
      }, timeUntilRefresh)
    }
  }

  // 监听token变化
  watch(tokenExpiresAt, setupTokenRefresh, { immediate: true })

  /**
   * 解析JWT token获取过期时间
   */
  const parseTokenExpiration = (token: string): number | null => {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.exp ? payload.exp * 1000 : null
    } catch {
      return null
    }
  }

  /**
   * 设置token信息
   */
  const setTokenInfo = (tokenInfo: TokenInfo) => {
    token.value = tokenInfo.token
    refreshToken.value = tokenInfo.refreshToken || null

    // 计算token过期时间
    const expiresAt = tokenInfo.expiresAt || parseTokenExpiration(tokenInfo.token)
    tokenExpiresAt.value = expiresAt

    // 存储到localStorage
    localStorage.setItem('token', tokenInfo.token)
    if (tokenInfo.refreshToken) {
      localStorage.setItem('refreshToken', tokenInfo.refreshToken)
    }
    if (expiresAt) {
      localStorage.setItem('tokenExpiresAt', expiresAt.toString())
    }

    setupTokenRefresh()
  }

  /**
   * 清除token信息
   */
  const clearTokenInfo = () => {
    token.value = null
    refreshToken.value = null
    tokenExpiresAt.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresAt')

    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  /**
   * 登录
   */
  const login = async (credentials: LoginRequest): Promise<LoginResponse> => {
    loading.value = true
    status.value = 'loading'

    try {
      const response = await authApi.login(credentials)

      setTokenInfo({
        token: response.token,
        refreshToken: response.refreshToken
      })

      user.value = response.user
      status.value = 'authenticated'

      return response
    } catch (error) {
      status.value = 'unauthenticated'
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 登出
   */
  const logout = async (skipApiCall = false) => {
    loading.value = true

    try {
      if (!skipApiCall && token.value) {
        await authApi.logout()
      }
    } catch (error) {
      console.warn('Logout API call failed:', error)
    } finally {
      clearTokenInfo()
      user.value = null
      status.value = 'unauthenticated'
      loading.value = false
    }
  }

  /**
   * 获取当前用户信息
   */
  const getCurrentUser = async (): Promise<User | null> => {
    if (!token.value) return null

    try {
      const response = await authApi.getProfile()
      user.value = response
      return response
    } catch (error) {
      // Token可能已过期，清除认证状态
      logout(true)
      throw error
    }
  }

  /**
   * 刷新Token
   */
  const refreshTokenSilently = async (): Promise<string | null> => {
    if (!refreshToken.value || status.value === 'refreshing') {
      return null
    }

    status.value = 'refreshing'

    try {
      const response = await authApi.refreshToken(refreshToken.value)

      setTokenInfo({
        token: response.token,
        refreshToken: response.refreshToken || refreshToken.value
      })

      if (response.user) {
        user.value = response.user
      }

      status.value = 'authenticated'
      return response.token
    } catch (error) {
      console.error('Token refresh failed:', error)
      logout(true)
      throw error
    }
  }

  /**
   * 刷新Token（公开方法）
   */
  const refreshTokenAction = async (): Promise<string | null> => {
    return refreshTokenSilently()
  }

  /**
   * 检查用户是否有指定角色
   */
  const hasRole = (role: string): boolean => {
    if (!user.value) return false

    const roleHierarchy = {
      ADMIN: ['ADMIN', 'USER'],
      USER: ['USER']
    }

    return roleHierarchy[user.value.role as keyof typeof roleHierarchy]?.includes(role) ?? false
  }

  /**
   * 检查用户是否有指定权限
   */
  const hasPermission = (permission: string): boolean => {
    if (!user.value) return false

    // 管理员拥有所有权限
    if (user.value.role === 'ADMIN') {
      return true
    }

    // 这里可以根据实际需求实现更复杂的权限检查逻辑
    const userPermissions = {
      USER: ['read:profile', 'update:profile'],
      ADMIN: ['*'] // 所有权限
    }

    const permissions = userPermissions[user.value.role as keyof typeof userPermissions] || []
    return permissions.includes('*') || permissions.includes(permission)
  }

  /**
   * 初始化认证状态
   */
  const initialize = async () => {
    if (!token.value) {
      status.value = 'unauthenticated'
      return
    }

    // 检查token是否过期
    if (isTokenExpired.value) {
      // 尝试刷新token
      if (refreshToken.value) {
        try {
          await refreshTokenSilently()
        } catch {
          logout(true)
        }
      } else {
        logout(true)
      }
      return
    }

    // Token有效，获取用户信息
    try {
      await getCurrentUser()
      status.value = 'authenticated'
    } catch {
      logout(true)
    }
  }

  return {
    // 状态
    token,
    refreshToken,
    user,
    loading,
    status,
    tokenExpiresAt,

    // 计算属性
    isAuthenticated,
    isTokenExpired,
    tokenExpiresIn,

    // 方法
    login,
    logout,
    getCurrentUser,
    refreshTokenAction,
    hasRole,
    hasPermission,
    initialize
  }
})