import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { searchSpaceApi } from '@/services/searchSpaceApi'
import { mappingApi } from '@/api/mappingApi'
import type {
  SearchSpace,
  CreateSearchSpaceRequest,
  UpdateSearchSpaceRequest,
  SearchSpaceQueryRequest,
  SearchSpaceStatistics,
  PageResult
} from '@/types/searchSpace'

export const useSearchSpaceStore = defineStore('searchSpace', () => {
  // 状态
  const searchSpaces = ref<SearchSpace[]>([])
  const currentSearchSpace = ref<SearchSpace | null>(null)
  const statistics = ref<SearchSpaceStatistics | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Mapping 相关状态
  const currentMapping = ref<string | null>(null)
  const mappingLoading = ref(false)
  const mappingError = ref<string | null>(null)
  const lastMappingUpdated = ref<Date | null>(null)
  const mappingCache = ref<Map<number, string>>(new Map())

  // 分页状态
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })

  // 查询参数
  const queryParams = ref<SearchSpaceQueryRequest>({
    keyword: '',
    page: 0,
    size: 10,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })

  // Getters
  const activeSearchSpaces = computed(() =>
    searchSpaces.value.filter(space => space.status === 'ACTIVE')
  )

  const inactiveSearchSpaces = computed(() =>
    searchSpaces.value.filter(space => space.status === 'INACTIVE')
  )

  const hasSearchSpaces = computed(() => searchSpaces.value.length > 0)

  // Actions
  const setLoading = (value: boolean) => {
    loading.value = value
  }

  const setError = (value: string | null) => {
    error.value = value
  }

  const clearError = () => {
    error.value = null
  }

  const setMappingLoading = (value: boolean) => {
    mappingLoading.value = value
  }

  const setMappingError = (value: string | null) => {
    mappingError.value = value
  }

  const clearMappingError = () => {
    mappingError.value = null
  }

  /**
   * 获取搜索空间列表
   */
  const fetchSearchSpaces = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await searchSpaceApi.list(queryParams.value)
      if (response.data) {
        searchSpaces.value = response.data.content
        pagination.value = {
          page: response.data.page,
          size: response.data.size,
          totalElements: response.data.totalElements,
          totalPages: response.data.totalPages,
          first: response.data.first,
          last: response.data.last
        }
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取搜索空间列表失败'
      console.error('Failed to fetch search spaces:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * 根据ID获取搜索空间
   */
  const fetchSearchSpaceById = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.getById(id)
      if (response.data) {
        currentSearchSpace.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取搜索空间详情失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 根据代码获取搜索空间
   */
  const fetchSearchSpaceByCode = async (code: string) => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.getByCode(code)
      if (response.data) {
        currentSearchSpace.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取搜索空间详情失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 创建搜索空间
   */
  const createSearchSpace = async (data: CreateSearchSpaceRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.create(data)
      if (response.data) {
        // 添加到列表开头
        searchSpaces.value.unshift(response.data)
        currentSearchSpace.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '创建搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 更新搜索空间
   */
  const updateSearchSpace = async (id: number, data: UpdateSearchSpaceRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.update(id, data)
      if (response.data) {
        // 更新列表中的项目
        const index = searchSpaces.value.findIndex(space => space.id === id)
        if (index !== -1) {
          searchSpaces.value[index] = response.data
        }
        // 更新当前项目
        if (currentSearchSpace.value?.id === id) {
          currentSearchSpace.value = response.data
        }
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '更新搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 删除搜索空间
   */
  const deleteSearchSpace = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      await searchSpaceApi.delete(id)

      // 从列表中移除
      searchSpaces.value = searchSpaces.value.filter(space => space.id !== id)

      // 如果删除的是当前项目，清空当前项目
      if (currentSearchSpace.value?.id === id) {
        currentSearchSpace.value = null
      }
    } catch (err: any) {
      setError(err.message || '删除搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 启用搜索空间
   */
  const enableSearchSpace = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.enable(id)
      if (response.data) {
        // 更新列表中的项目
        const index = searchSpaces.value.findIndex(space => space.id === id)
        if (index !== -1) {
          searchSpaces.value[index] = response.data
        }
        // 更新当前项目
        if (currentSearchSpace.value?.id === id) {
          currentSearchSpace.value = response.data
        }
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '启用搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 禁用搜索空间
   */
  const disableSearchSpace = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.disable(id)
      if (response.data) {
        // 更新列表中的项目
        const index = searchSpaces.value.findIndex(space => space.id === id)
        if (index !== -1) {
          searchSpaces.value[index] = response.data
        }
        // 更新当前项目
        if (currentSearchSpace.value?.id === id) {
          currentSearchSpace.value = response.data
        }
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '禁用搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 获取统计信息
   */
  const fetchStatistics = async () => {
    try {
      setLoading(true)
      clearError()

      const response = await searchSpaceApi.getStatistics()
      if (response.data) {
        statistics.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取统计信息失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 检查代码可用性
   */
  const checkCodeAvailability = async (code: string) => {
    try {
      clearError()
      const response = await searchSpaceApi.checkCodeAvailability(code)
      return response.data?.available || false
    } catch (err: any) {
      setError(err.message || '检查代码可用性失败')
      throw err
    }
  }

  /**
   * 获取搜索空间的 mapping 配置
   */
  const fetchMapping = async (spaceId: number) => {
    try {
      setMappingLoading(true)
      clearMappingError()

      // 检查缓存
      const cached = mappingCache.value.get(spaceId)
      if (cached) {
        currentMapping.value = cached
        return cached
      }

      const response = await mappingApi.getMapping(spaceId)
      if (response.data) {
        currentMapping.value = response.data
        mappingCache.value.set(spaceId, response.data)
        lastMappingUpdated.value = new Date()
      }
      return response.data
    } catch (err: any) {
      setMappingError(err.message || '获取 mapping 配置失败')
      throw err
    } finally {
      setMappingLoading(false)
    }
  }

  /**
   * 更新搜索空间的 mapping 配置
   */
  const updateMapping = async (spaceId: number, mapping: string) => {
    try {
      setMappingLoading(true)
      clearMappingError()

      await mappingApi.updateMapping(spaceId, mapping)

      // 更新本地状态和缓存
      currentMapping.value = mapping
      mappingCache.value.set(spaceId, mapping)
      lastMappingUpdated.value = new Date()

    } catch (err: any) {
      setMappingError(err.message || '更新 mapping 配置失败')
      throw err
    } finally {
      setMappingLoading(false)
    }
  }

  /**
   * 清除 mapping 缓存
   */
  const clearMappingCache = (spaceId?: number) => {
    if (spaceId) {
      mappingCache.value.delete(spaceId)
    } else {
      mappingCache.value.clear()
    }
  }

  /**
   * 重置 mapping 状态
   */
  const resetMappingState = () => {
    currentMapping.value = null
    mappingLoading.value = false
    mappingError.value = null
    lastMappingUpdated.value = null
    mappingCache.value.clear()
  }

  /**
   * 更新查询参数
   */
  const updateQueryParams = (params: Partial<SearchSpaceQueryRequest>) => {
    queryParams.value = { ...queryParams.value, ...params }
  }

  /**
   * 重置状态
   */
  const reset = () => {
    searchSpaces.value = []
    currentSearchSpace.value = null
    statistics.value = null
    loading.value = false
    error.value = null
    pagination.value = {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    }
    queryParams.value = {
      keyword: '',
      page: 0,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }
    resetMappingState()
  }

  return {
    // 状态
    searchSpaces,
    currentSearchSpace,
    statistics,
    loading,
    error,
    pagination,
    queryParams,

    // Mapping 状态
    currentMapping,
    mappingLoading,
    mappingError,
    lastMappingUpdated,
    mappingCache,

    // Getters
    activeSearchSpaces,
    inactiveSearchSpaces,
    hasSearchSpaces,

    // Actions
    setLoading,
    setError,
    clearError,
    fetchSearchSpaces,
    fetchSearchSpaceById,
    fetchSearchSpaceByCode,
    createSearchSpace,
    updateSearchSpace,
    deleteSearchSpace,
    enableSearchSpace,
    disableSearchSpace,
    fetchStatistics,
    checkCodeAvailability,
    updateQueryParams,
    reset,

    // Mapping Actions
    setMappingLoading,
    setMappingError,
    clearMappingError,
    fetchMapping,
    updateMapping,
    clearMappingCache,
    resetMappingState
  }
})