import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  HotTopic,
  CreateHotTopicRequest,
  UpdateHotTopicRequest,
  HotTopicQueryParams,
  HotTopicStatistics
} from '@/types/hotTopic'
import type { PageResult } from '@/types/api'
import { HotTopicApi } from '@/services/hotTopicApi'

export const useHotTopicStore = defineStore('hotTopic', () => {
  // State
  const hotTopics = ref<HotTopic[]>([])
  const currentHotTopic = ref<HotTopic | null>(null)
  const pagination = ref<Omit<PageResult<HotTopic>, 'content'>>({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })
  const loading = ref(false)
  const error = ref<string | null>(null)
  const queryParams = ref<HotTopicQueryParams>({
    page: 0,
    size: 20,
    sort: 'popularity',
    direction: 'DESC'
  })

  // Getters
  const hasHotTopics = computed(() => hotTopics.value.length > 0)
  const isLoading = computed(() => loading.value)
  const currentError = computed(() => error.value)

  // Actions
  const clearError = () => {
    error.value = null
  }

  const updateQueryParams = (params: Partial<HotTopicQueryParams>) => {
    queryParams.value = { ...queryParams.value, ...params }
  }

  const fetchHotTopics = async (params?: HotTopicQueryParams) => {
    try {
      loading.value = true
      error.value = null

      const mergedParams = { ...queryParams.value, ...params }
      const result = await HotTopicApi.list(mergedParams)

      hotTopics.value = result.content
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
      error.value = err instanceof Error ? err.message : '获取热门话题列表失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const searchHotTopics = async (keyword: string, params?: HotTopicQueryParams) => {
    try {
      loading.value = true
      error.value = null

      const searchParams = { ...queryParams.value, ...params, keyword }
      const result = await HotTopicApi.search(searchParams)

      hotTopics.value = result.content
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
      error.value = err instanceof Error ? err.message : '搜索热门话题失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const getHotTopic = async (id: number) => {
    try {
      loading.value = true
      error.value = null

      const hotTopic = await HotTopicApi.getById(id)
      currentHotTopic.value = hotTopic

      return hotTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取热门话题详情失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const createHotTopic = async (data: CreateHotTopicRequest) => {
    try {
      loading.value = true
      error.value = null

      const hotTopic = await HotTopicApi.create(data)

      // 如果是第一页，将新创建的话题添加到列表开头
      if (queryParams.value.page === 0) {
        hotTopics.value.unshift(hotTopic)

        // 如果超过了页面大小，移除最后一个
        if (hotTopics.value.length > (queryParams.value.size || 20)) {
          hotTopics.value.pop()
        }
      }

      return hotTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '创建热门话题失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateHotTopic = async (id: number, data: UpdateHotTopicRequest) => {
    try {
      loading.value = true
      error.value = null

      const updatedHotTopic = await HotTopicApi.update(id, data)

      // 更新列表中的数据
      const index = hotTopics.value.findIndex(h => h.id === id)
      if (index !== -1) {
        hotTopics.value[index] = updatedHotTopic
      }

      // 更新当前话题
      if (currentHotTopic.value?.id === id) {
        currentHotTopic.value = updatedHotTopic
      }

      return updatedHotTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新热门话题失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteHotTopic = async (id: number) => {
    try {
      loading.value = true
      error.value = null

      await HotTopicApi.delete(id)

      // 从列表中移除
      const index = hotTopics.value.findIndex(h => h.id === id)
      if (index !== -1) {
        hotTopics.value.splice(index, 1)
      }

      // 清除当前话题
      if (currentHotTopic.value?.id === id) {
        currentHotTopic.value = null
      }

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除热门话题失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const toggleVisibility = async (id: number) => {
    try {
      loading.value = true
      error.value = null

      const updatedHotTopic = await HotTopicApi.toggleVisibility(id)

      // 更新列表中的数据
      const index = hotTopics.value.findIndex(h => h.id === id)
      if (index !== -1) {
        hotTopics.value[index] = updatedHotTopic
      }

      // 更新当前话题
      if (currentHotTopic.value?.id === id) {
        currentHotTopic.value = updatedHotTopic
      }

      return updatedHotTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '切换可见性失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const updatePopularity = async (id: number, popularity: number) => {
    try {
      loading.value = true
      error.value = null

      const updatedHotTopic = await HotTopicApi.updatePopularity(id, popularity)

      // 更新列表中的数据
      const index = hotTopics.value.findIndex(h => h.id === id)
      if (index !== -1) {
        hotTopics.value[index] = updatedHotTopic
      }

      // 更新当前话题
      if (currentHotTopic.value?.id === id) {
        currentHotTopic.value = updatedHotTopic
      }

      return updatedHotTopic
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新热度失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const getTopPopularTopics = async (limit: number = 10) => {
    try {
      loading.value = true
      error.value = null

      const topics = await HotTopicApi.getTopPopular(limit)
      return topics
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取热门排行榜失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const isNameAvailable = async (name: string, excludeId?: number) => {
    try {
      const available = await HotTopicApi.isNameAvailable(name, excludeId)
      return available
    } catch (err) {
      error.value = err instanceof Error ? err.message : '检查名称可用性失败'
      throw err
    }
  }

  const getStatistics = async (): Promise<HotTopicStatistics> => {
    try {
      const statistics = await HotTopicApi.getStatistics()
      return statistics
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取统计信息失败'
      throw err
    }
  }

  const clearCurrentHotTopic = () => {
    currentHotTopic.value = null
  }

  const reset = () => {
    hotTopics.value = []
    currentHotTopic.value = null
    pagination.value = {
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    }
    loading.value = false
    error.value = null
    queryParams.value = {
      page: 0,
      size: 20,
      sort: 'popularity',
      direction: 'DESC'
    }
  }

  return {
    // State
    hotTopics,
    currentHotTopic,
    pagination,
    loading,
    error,
    queryParams,

    // Getters
    hasHotTopics,
    isLoading,
    currentError,

    // Actions
    clearError,
    updateQueryParams,
    fetchHotTopics,
    searchHotTopics,
    getHotTopic,
    createHotTopic,
    updateHotTopic,
    deleteHotTopic,
    toggleVisibility,
    updatePopularity,
    getTopPopularTopics,
    isNameAvailable,
    getStatistics,
    clearCurrentHotTopic,
    reset
  }
})