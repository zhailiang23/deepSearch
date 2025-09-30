import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  SensitiveWord,
  CreateSensitiveWordRequest,
  UpdateSensitiveWordRequest,
  SensitiveWordQueryParams,
  SensitiveWordStatistics
} from '@/types/sensitiveWord'
import type { PageResult } from '@/types/api'
import { sensitiveWordApi } from '@/services/sensitiveWordApi'

export const useSensitiveWordStore = defineStore('sensitiveWord', () => {
  // State
  const sensitiveWords = ref<SensitiveWord[]>([])
  const currentSensitiveWord = ref<SensitiveWord | null>(null)
  const pagination = ref<Omit<PageResult<SensitiveWord>, 'content'>>({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })
  const loading = ref(false)
  const error = ref<string | null>(null)
  const queryParams = ref<SensitiveWordQueryParams>({
    page: 0,
    size: 10,
    sort: 'harmLevel',
    direction: 'DESC'
  })

  // Getters
  const hasSensitiveWords = computed(() => sensitiveWords.value.length > 0)
  const isLoading = computed(() => loading.value)
  const currentError = computed(() => error.value)

  // Actions
  const clearError = () => {
    error.value = null
  }

  const updateQueryParams = (params: Partial<SensitiveWordQueryParams>) => {
    queryParams.value = { ...queryParams.value, ...params }
  }

  const fetchSensitiveWords = async (params?: SensitiveWordQueryParams) => {
    try {
      loading.value = true
      error.value = null

      const mergedParams = { ...queryParams.value, ...params }
      const result = await sensitiveWordApi.list(mergedParams)

      sensitiveWords.value = result.content
      pagination.value = {
        page: result.page,
        size: result.size,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        first: result.first,
        last: result.last
      }

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取敏感词列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const searchSensitiveWords = async (keyword: string, params?: SensitiveWordQueryParams) => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.search(keyword, params)

      sensitiveWords.value = result.content
      pagination.value = {
        page: result.page,
        size: result.size,
        totalElements: result.totalElements,
        totalPages: result.totalPages,
        first: result.first,
        last: result.last
      }

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '搜索敏感词失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const getSensitiveWord = async (id: number) => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.getById(id)
      currentSensitiveWord.value = result

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取敏感词详情失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const createSensitiveWord = async (data: CreateSensitiveWordRequest) => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.create(data)

      // 刷新列表
      await fetchSensitiveWords()

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '创建敏感词失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateSensitiveWord = async (id: number, data: UpdateSensitiveWordRequest) => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.update(id, data)

      // 刷新列表
      await fetchSensitiveWords()

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新敏感词失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteSensitiveWord = async (id: number) => {
    try {
      loading.value = true
      error.value = null

      await sensitiveWordApi.delete(id)

      // 刷新列表
      await fetchSensitiveWords()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除敏感词失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const toggleStatus = async (id: number) => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.toggleStatus(id)

      // 更新列表中的对应项
      const index = sensitiveWords.value.findIndex(w => w.id === id)
      if (index !== -1) {
        sensitiveWords.value[index] = result
      }

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '切换敏感词状态失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateHarmLevel = async (id: number, harmLevel: number) => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.updateHarmLevel(id, harmLevel)

      // 更新列表中的对应项
      const index = sensitiveWords.value.findIndex(w => w.id === id)
      if (index !== -1) {
        sensitiveWords.value[index] = result
      }

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新危害等级失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const checkNameAvailable = async (name: string, excludeId?: number): Promise<boolean> => {
    try {
      return await sensitiveWordApi.checkNameAvailable(name, excludeId)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '检查敏感词名称可用性失败'
      throw err
    }
  }

  const getStatistics = async (): Promise<SensitiveWordStatistics> => {
    try {
      return await sensitiveWordApi.getStatistics()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取统计信息失败'
      throw err
    }
  }

  const getAllSensitiveWords = async () => {
    try {
      loading.value = true
      error.value = null

      const result = await sensitiveWordApi.getAll()
      sensitiveWords.value = result

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取所有敏感词失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    sensitiveWords,
    currentSensitiveWord,
    pagination,
    loading,
    error,
    queryParams,

    // Getters
    hasSensitiveWords,
    isLoading,
    currentError,

    // Actions
    clearError,
    updateQueryParams,
    fetchSensitiveWords,
    searchSensitiveWords,
    getSensitiveWord,
    createSensitiveWord,
    updateSensitiveWord,
    deleteSensitiveWord,
    toggleStatus,
    updateHarmLevel,
    checkNameAvailable,
    getStatistics,
    getAllSensitiveWords
  }
})