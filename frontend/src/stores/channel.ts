import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { channelApi } from '@/services/channelApi'
import type {
  Channel,
  CreateChannelRequest,
  UpdateChannelRequest,
  ChannelQueryRequest,
  UpdateSalesRequest,
  BatchStatusUpdateRequest,
  ChannelStatistics,
  ChannelType,
  ChannelStatus,
  PageResult
} from '@/types/channel'

export const useChannelStore = defineStore('channel', () => {
  // ========== 状态定义 ==========

  const channels = ref<Channel[]>([])
  const currentChannel = ref<Channel | null>(null)
  const statistics = ref<ChannelStatistics | null>(null)
  const activeChannels = ref<Channel[]>([])
  const topPerformingChannels = ref<Channel[]>([])
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
    status: undefined,
    type: undefined,
    page: 0,
    size: 10,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })

  // ========== 计算属性 ==========

  /**
   * 按状态筛选的渠道
   */
  const channelsByStatus = computed(() => {
    const statusGroups: Record<ChannelStatus, Channel[]> = {
      'ACTIVE': [],
      'INACTIVE': [],
      'SUSPENDED': [],
      'DELETED': []
    }

    channels.value.forEach(channel => {
      if (statusGroups[channel.status]) {
        statusGroups[channel.status].push(channel)
      }
    })

    return statusGroups
  })

  /**
   * 按类型分组的渠道
   */
  const channelsByType = computed(() => {
    const typeGroups: Record<ChannelType, Channel[]> = {
      'ONLINE': [],
      'OFFLINE': [],
      'HYBRID': [],
      'DISTRIBUTOR': []
    }

    channels.value.forEach(channel => {
      if (typeGroups[channel.type]) {
        typeGroups[channel.type].push(channel)
      }
    })

    return typeGroups
  })

  /**
   * 活跃渠道列表
   */
  const activeChannelsList = computed(() =>
    channels.value.filter(channel => channel.status === 'ACTIVE')
  )

  /**
   * 非活跃渠道列表
   */
  const inactiveChannelsList = computed(() =>
    channels.value.filter(channel => channel.status !== 'ACTIVE')
  )

  /**
   * 是否有渠道数据
   */
  const hasChannels = computed(() => channels.value.length > 0)

  /**
   * 总销售额
   */
  const totalSalesAmount = computed(() =>
    channels.value.reduce((sum, channel) => sum + (channel.totalSales || 0), 0)
  )

  /**
   * 当月总销售额
   */
  const currentMonthSalesAmount = computed(() =>
    channels.value.reduce((sum, channel) => sum + (channel.currentMonthSales || 0), 0)
  )

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

  // ========== 状态管理操作 ==========

  /**
   * 激活渠道
   */
  const activateChannel = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.activate(id)
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
      setError(err.message || '激活渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 停用渠道
   */
  const deactivateChannel = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.deactivate(id)
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
      setError(err.message || '停用渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 暂停渠道
   */
  const suspendChannel = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.suspend(id)
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
      setError(err.message || '暂停渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 恢复渠道
   */
  const restoreChannel = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.restore(id)
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
      setError(err.message || '恢复渠道失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  // ========== 销售管理操作 ==========

  /**
   * 更新销售数据
   */
  const updateSales = async (id: number, data: UpdateSalesRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.updateSales(id, data)
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
      setError(err.message || '更新销售数据失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 重置月度销售
   */
  const resetMonthlySales = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.resetMonthlySales(id)
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
      setError(err.message || '重置月度销售失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  // ========== 查询和统计操作 ==========

  /**
   * 获取活跃渠道列表
   */
  const fetchActiveChannels = async () => {
    try {
      clearError()
      const response = await channelApi.getActiveChannels()
      if (response.data) {
        activeChannels.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取活跃渠道失败')
      throw err
    }
  }

  /**
   * 按类型获取渠道
   */
  const fetchChannelsByType = async (type: ChannelType) => {
    try {
      clearError()
      const response = await channelApi.getChannelsByType(type)
      return response.data || []
    } catch (err: any) {
      setError(err.message || `获取${type}类型渠道失败`)
      throw err
    }
  }

  /**
   * 获取销售排行榜
   */
  const fetchTopPerformingChannels = async (limit = 10) => {
    try {
      clearError()
      const response = await channelApi.getTopPerformingChannels(limit)
      if (response.data) {
        topPerformingChannels.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取销售排行榜失败')
      throw err
    }
  }

  /**
   * 获取统计信息
   */
  const fetchStatistics = async () => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.getStatistics()
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

  // ========== 批量操作 ==========

  /**
   * 批量状态变更
   */
  const batchUpdateStatus = async (data: BatchStatusUpdateRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await channelApi.batchUpdateStatus(data)
      if (response.data) {
        // 批量更新列表中的项目
        response.data.forEach(updatedChannel => {
          const index = channels.value.findIndex(channel => channel.id === updatedChannel.id)
          if (index !== -1) {
            channels.value[index] = updatedChannel
          }
        })
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '批量状态变更失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 批量删除
   */
  const batchDeleteChannels = async (channelIds: number[]) => {
    try {
      setLoading(true)
      clearError()

      await channelApi.batchDelete(channelIds)

      // 从列表中移除
      channels.value = channels.value.filter(channel => !channelIds.includes(channel.id))

      // 如果删除的包含当前项目，清空当前项目
      if (currentChannel.value && channelIds.includes(currentChannel.value.id)) {
        currentChannel.value = null
      }
    } catch (err: any) {
      setError(err.message || '批量删除失败')
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
    statistics.value = null
    activeChannels.value = []
    topPerformingChannels.value = []
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
      status: undefined,
      type: undefined,
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
    statistics,
    activeChannels,
    topPerformingChannels,
    loading,
    error,
    pagination,
    queryParams,

    // ========== 计算属性 ==========
    channelsByStatus,
    channelsByType,
    activeChannelsList,
    inactiveChannelsList,
    hasChannels,
    totalSalesAmount,
    currentMonthSalesAmount,

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

    // ========== 状态管理 ==========
    activateChannel,
    deactivateChannel,
    suspendChannel,
    restoreChannel,

    // ========== 销售管理 ==========
    updateSales,
    resetMonthlySales,

    // ========== 查询统计 ==========
    fetchActiveChannels,
    fetchChannelsByType,
    fetchTopPerformingChannels,
    fetchStatistics,

    // ========== 批量操作 ==========
    batchUpdateStatus,
    batchDeleteChannels,

    // ========== 验证操作 ==========
    checkCodeAvailability,
    checkNameAvailability
  }
})