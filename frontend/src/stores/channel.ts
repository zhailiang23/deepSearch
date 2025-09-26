import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { channelApi } from '@/services/channelApi'
import type {
  Channel,
  CreateChannelRequest,
  UpdateChannelRequest,
  ChannelQueryRequest,
  PageResult
} from '@/types/channel'

export const useChannelStore = defineStore('channel', () => {
  // ========== 状态定义 ==========

  const channels = ref<Channel[]>([])
  const currentChannel = ref<Channel | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

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
  const queryParams = ref<ChannelQueryRequest>({
    keyword: '',
    page: 0,
    size: 10,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })

  // ========== 计算属性 ==========

  /**
   * 是否有渠道数据
   */
  const hasChannels = computed(() => channels.value.length > 0)

  // ========== 基础操作方法 ==========

  const setLoading = (value: boolean) => {
    loading.value = value
  }

  const setError = (value: string | null) => {
    error.value = value
  }

  const clearError = () => {
    error.value = null
  }

  /**
   * 更新查询参数
   */
  const updateQueryParams = (params: Partial<ChannelQueryRequest>) => {
    queryParams.value = { ...queryParams.value, ...params }
  }

  // ========== API调用方法 ==========

  /**
   * 获取渠道列表
   */
  const fetchChannels = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await channelApi.list(queryParams.value)
      if (response.data) {
        channels.value = response.data.content
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
      error.value = err instanceof Error ? err.message : '获取渠道列表失败'
      console.error('Failed to fetch channels:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * 根据ID获取渠道
   */
  const fetchChannelById = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.getById(id)
      if (response.data) {
        currentChannel.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取渠道详情失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 根据代码获取渠道
   */
  const fetchChannelByCode = async (code: string) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.getByCode(code)
      if (response.data) {
        currentChannel.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取渠道详情失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 创建渠道
   */
  const createChannel = async (data: CreateChannelRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.create(data)
      if (response.data) {
        // 添加到列表开头
        channels.value.unshift(response.data)
        currentChannel.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '创建渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 更新渠道
   */
  const updateChannel = async (id: number, data: UpdateChannelRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.update(id, data)
      if (response.data) {
        // 更新列表中的项目
        const index = channels.value.findIndex(channel => channel.id === id)
        if (index !== -1) {
          channels.value[index] = response.data
        }
        // 更新当前项目
        if (currentChannel.value?.id === id) {
          currentChannel.value = response.data
        }
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '更新渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 删除渠道
   */
  const deleteChannel = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      await channelApi.delete(id)

      // 从列表中移除
      channels.value = channels.value.filter(channel => channel.id !== id)

      // 如果删除的是当前项目，清空当前项目
      if (currentChannel.value?.id === id) {
        currentChannel.value = null
      }
    } catch (err: any) {
      setError(err.message || '删除渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  // ========== 验证操作 ==========

  /**
   * 检查代码可用性
   */
  const checkCodeAvailability = async (code: string, excludeId?: number) => {
    try {
      clearError()
      const response = await channelApi.checkCodeAvailability(code, excludeId)
      return response.data || false
    } catch (err: any) {
      setError(err.message || '检查代码可用性失败')
      throw err
    }
  }

  /**
   * 检查名称可用性
   */
  const checkNameAvailability = async (name: string, excludeId?: number) => {
    try {
      clearError()
      const response = await channelApi.checkNameAvailability(name, excludeId)
      return response.data || false
    } catch (err: any) {
      setError(err.message || '检查名称可用性失败')
      throw err
    }
  }

  // ========== 重置操作 ==========

  /**
   * 重置状态
   */
  const reset = () => {
    channels.value = []
    currentChannel.value = null
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
  }

  return {
    // ========== 状态 ==========
    channels,
    currentChannel,
    loading,
    error,
    pagination,
    queryParams,

    // ========== 计算属性 ==========
    hasChannels,

    // ========== 基础操作 ==========
    setLoading,
    setError,
    clearError,
    updateQueryParams,
    reset,

    // ========== CRUD操作 ==========
    fetchChannels,
    fetchChannelById,
    fetchChannelByCode,
    createChannel,
    updateChannel,
    deleteChannel,

    // ========== 验证操作 ==========
    checkCodeAvailability,
    checkNameAvailability
  }
})