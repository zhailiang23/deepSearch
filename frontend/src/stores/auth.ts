import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest } from '@/types/auth'
import { authApi } from '@/utils/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => !!token.value)

  const login = async (credentials: LoginRequest) => {
    loading.value = true
    try {
      const response = await authApi.login(credentials)
      token.value = response.token
      user.value = response.user
      localStorage.setItem('token', response.token)
      return response
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  const getCurrentUser = async () => {
    if (!token.value) return null

    try {
      const response = await authApi.getProfile()
      user.value = response
      return response
    } catch (error) {
      // Token可能已过期，清除认证状态
      logout()
      throw error
    }
  }

  return {
    token,
    user,
    loading,
    isAuthenticated,
    login,
    logout,
    getCurrentUser
  }
})