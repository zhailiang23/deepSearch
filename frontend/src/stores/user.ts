import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/services/userApi'
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UserQueryParams,
  UserStatistics
} from '@/types/user'
import type { PageResult } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const users = ref<User[]>([])
  const currentUser = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const pagination = ref({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: false
  })

  const queryParams = ref<UserQueryParams>({
    page: 0,
    size: 20
  })

  const fetchUsers = async (params?: UserQueryParams) => {
    loading.value = true
    error.value = null
    try {
      const result = await userApi.list(params || queryParams.value)
      users.value = result.content
      pagination.value = {
        page: result.page,
        size: result.size,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        first: result.first,
        last: result.last
      }
    } catch (err: any) {
      error.value = err.message || '获取用户列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const searchUsers = async (keyword: string, params?: UserQueryParams) => {
    loading.value = true
    error.value = null
    try {
      const result = await userApi.search(keyword, params)
      users.value = result.content
      pagination.value = {
        page: result.page,
        size: result.size,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        first: result.first,
        last: result.last
      }
    } catch (err: any) {
      error.value = err.message || '搜索用户失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const createUser = async (data: CreateUserRequest) => {
    loading.value = true
    error.value = null
    try {
      const newUser = await userApi.create(data)
      await fetchUsers(queryParams.value)
      return newUser
    } catch (err: any) {
      error.value = err.message || '创建用户失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateUser = async (id: number, data: UpdateUserRequest) => {
    loading.value = true
    error.value = null
    try {
      const updated = await userApi.update(id, data)
      await fetchUsers(queryParams.value)
      return updated
    } catch (err: any) {
      error.value = err.message || '更新用户失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteUser = async (id: number) => {
    loading.value = true
    error.value = null
    try {
      await userApi.delete(id)
      await fetchUsers(queryParams.value)
    } catch (err: any) {
      error.value = err.message || '删除用户失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const toggleStatus = async (id: number) => {
    try {
      await userApi.toggleStatus(id)
      await fetchUsers(queryParams.value)
    } catch (err: any) {
      error.value = err.message || '切换状态失败'
      throw err
    }
  }

  const getStatistics = async (): Promise<UserStatistics> => {
    try {
      return await userApi.getStatistics()
    } catch (err: any) {
      error.value = err.message || '获取统计信息失败'
      throw err
    }
  }

  const updateQueryParams = (params: Partial<UserQueryParams>) => {
    queryParams.value = { ...queryParams.value, ...params }
  }

  return {
    users,
    currentUser,
    loading,
    error,
    pagination,
    queryParams,
    fetchUsers,
    searchUsers,
    createUser,
    updateUser,
    deleteUser,
    toggleStatus,
    getStatistics,
    updateQueryParams
  }
})