/**
 * 热词统计数据管理Store
 * 负责热词数据的获取、缓存、状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import { hotWordApi } from '@/api/hotWordApi'
import type {
  HotWordRequest,
  HotWordResponse,
  HotWordFilter,
  HotWordDisplayOptions,
  HotWordPageState,
  HotWordStatisticsRequest,
  HotWordStatisticsResponse,
  WordCloudItem,
  LegacyHotWordStatistics,
  LegacyHotWordItem,
  LegacyHotWordStatisticsResponse,
  convertToLegacyFormat,
  convertToLegacyStatistics,
  convertToLegacyResponse
} from '@/types/hotWord'

export const useHotWordStatisticsStore = defineStore('hotWordStatistics', () => {
  // ============= 状态管理 =============

  /** 热词数据 */
  const hotWords = ref<HotWordResponse[]>([])

  /** 统计数据 */
  const statistics = ref<HotWordStatisticsResponse | null>(null)

  /** 加载状态 */
  const loading = ref(false)

  /** 错误信息 */
  const error = ref<string | null>(null)

  /** 最后更新时间 */
  const lastUpdated = ref<Date | null>(null)

  /** 缓存的查询参数 */
  const lastRequest = ref<HotWordRequest | null>(null)

  /** 筛选条件 */
  const filter = reactive<HotWordFilter>({
    dateRange: {
      start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), // 默认7天前
      end: new Date() // 默认当前时间
    },
    limit: 50,
    minWordLength: 2,
    excludeStopWords: true,
    includeSegmentDetails: false
  })

  /** 显示选项 */
  const displayOptions = reactive<HotWordDisplayOptions>({
    viewMode: 'cloud',
    sortBy: 'count',
    sortOrder: 'desc',
    showSegmentDetails: false,
    showStatistics: true
  })

  // ============= 计算属性 =============

  /** 页面状态 */
  const pageState = computed<HotWordPageState>(() => ({
    hotWords: hotWords.value,
    statistics: statistics.value || undefined,
    filter: filter,
    displayOptions: displayOptions,
    loading: loading.value,
    error: error.value,
    lastUpdated: lastUpdated.value || undefined
  }))

  /** 排序后的热词数据 */
  const sortedHotWords = computed(() => {
    const words = [...hotWords.value]
    const { sortBy, sortOrder } = displayOptions

    words.sort((a, b) => {
      let aValue: any, bValue: any

      switch (sortBy) {
        case 'count':
          aValue = a.count
          bValue = b.count
          break
        case 'percentage':
          aValue = a.percentage
          bValue = b.percentage
          break
        case 'length':
          aValue = a.wordLength
          bValue = b.wordLength
          break
        case 'lastOccurrence':
          aValue = new Date(a.lastOccurrence).getTime()
          bValue = new Date(b.lastOccurrence).getTime()
          break
        default:
          return 0
      }

      if (sortOrder === 'asc') {
        return aValue - bValue
      } else {
        return bValue - aValue
      }
    })

    return words
  })

  /** 词云图数据 */
  const wordCloudData = computed<WordCloudItem[]>(() => {
    return sortedHotWords.value.map(word => ({
      text: word.word,
      weight: word.count,
      data: word
    }))
  })

  /** 兼容旧版API的词云数据 */
  const legacyWordCloudData = computed<LegacyHotWordItem[]>(() => {
    return convertToLegacyFormat(hotWords.value)
  })

  /** 兼容旧版API的统计数据 */
  const legacyStatisticsData = computed<LegacyHotWordStatistics[]>(() => {
    return convertToLegacyStatistics(hotWords.value)
  })

  /** 兼容旧版API的响应格式 */
  const legacyResponse = computed<LegacyHotWordStatisticsResponse>(() => {
    return convertToLegacyResponse(hotWords.value)
  })

  /** 是否有数据 */
  const hasData = computed(() => hotWords.value.length > 0)

  /** 数据摘要信息 */
  const dataSummary = computed(() => {
    if (!hasData.value) return null

    const totalCount = hotWords.value.reduce((sum, word) => sum + word.count, 0)
    const avgCount = totalCount / hotWords.value.length
    const maxCount = Math.max(...hotWords.value.map(w => w.count))
    const minCount = Math.min(...hotWords.value.map(w => w.count))

    return {
      totalWords: hotWords.value.length,
      totalCount,
      avgCount: Math.round(avgCount * 100) / 100,
      maxCount,
      minCount,
      timeRange: {
        start: filter.dateRange.start,
        end: filter.dateRange.end
      }
    }
  })

  // ============= 操作方法 =============

  /**
   * 获取热词数据
   */
  async function fetchHotWords(params?: Partial<HotWordRequest>) {
    try {
      loading.value = true
      error.value = null

      // 构建请求参数
      const request: HotWordRequest = {
        startDate: filter.dateRange.start,
        endDate: filter.dateRange.end,
        limit: filter.limit,
        minWordLength: filter.minWordLength,
        excludeStopWords: filter.excludeStopWords,
        includeSegmentDetails: filter.includeSegmentDetails,
        userId: filter.userId,
        searchSpaceId: filter.searchSpaceId,
        ...params
      }

      // 发起API请求
      const response = await hotWordApi.getHotWords(request)

      if (response.success) {
        hotWords.value = response.data
        lastRequest.value = request
        lastUpdated.value = new Date()

        console.log(`获取热词数据成功：${response.data.length}个热词`)
      } else {
        throw new Error(response.message || '获取热词数据失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取热词数据失败'
      error.value = errorMessage
      console.error('获取热词数据失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取热词统计数据
   */
  async function fetchStatistics(params?: Partial<HotWordStatisticsRequest>) {
    try {
      loading.value = true
      error.value = null

      // 构建请求参数
      const request: HotWordStatisticsRequest = {
        startTime: filter.dateRange.start,
        endTime: filter.dateRange.end,
        userId: filter.userId,
        searchSpaceId: filter.searchSpaceId,
        includeDetails: true,
        topQueriesLimit: 10,
        topSearchSpacesLimit: 10,
        topUsersLimit: 10,
        ...params
      }

      // 发起API请求
      const response = await hotWordApi.getHotWordStatistics(request)

      if (response.success) {
        statistics.value = response.data
        lastUpdated.value = new Date()

        console.log('获取统计数据成功')
      } else {
        throw new Error(response.message || '获取统计数据失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取统计数据失败'
      error.value = errorMessage
      console.error('获取统计数据失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新所有数据
   */
  async function refreshAll() {
    try {
      await Promise.all([
        fetchHotWords(),
        fetchStatistics()
      ])
    } catch (err) {
      console.error('刷新数据失败:', err)
      throw err
    }
  }

  /**
   * 更新筛选条件
   */
  function updateFilter(newFilter: Partial<HotWordFilter>) {
    Object.assign(filter, newFilter)
  }

  /**
   * 更新显示选项
   */
  function updateDisplayOptions(newOptions: Partial<HotWordDisplayOptions>) {
    Object.assign(displayOptions, newOptions)
  }

  /**
   * 设置时间范围
   */
  function setTimeRange(start: Date, end: Date) {
    filter.dateRange.start = start
    filter.dateRange.end = end
  }

  /**
   * 设置预设时间范围
   */
  function setPresetTimeRange(preset: 'today' | 'week' | 'month' | '3months' | '6months') {
    const end = new Date()
    let start: Date

    switch (preset) {
      case 'today':
        start = new Date()
        start.setHours(0, 0, 0, 0)
        break
      case 'week':
        start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
        break
      case 'month':
        start = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
        break
      case '3months':
        start = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000)
        break
      case '6months':
        start = new Date(Date.now() - 180 * 24 * 60 * 60 * 1000)
        break
    }

    setTimeRange(start, end)
  }

  /**
   * 导出热词报告
   */
  async function exportReport(params?: Partial<HotWordRequest>) {
    try {
      loading.value = true

      const request: HotWordRequest = {
        startDate: filter.dateRange.start,
        endDate: filter.dateRange.end,
        limit: filter.limit,
        userId: filter.userId,
        searchSpaceId: filter.searchSpaceId,
        ...params
      }

      const blob = await hotWordApi.exportHotWordReport(request)

      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `热词统计报告_${new Date().toISOString().slice(0, 10)}.csv`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      console.log('导出报告成功')
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '导出报告失败'
      error.value = errorMessage
      console.error('导出报告失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 清空数据
   */
  function clearData() {
    hotWords.value = []
    statistics.value = null
    error.value = null
    lastUpdated.value = null
    lastRequest.value = null
  }

  /**
   * 重置筛选条件
   */
  function resetFilter() {
    filter.dateRange.start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
    filter.dateRange.end = new Date()
    filter.limit = 50
    filter.minWordLength = 2
    filter.excludeStopWords = true
    filter.includeSegmentDetails = false
    delete filter.userId
    delete filter.searchSpaceId
  }

  /**
   * 重置显示选项
   */
  function resetDisplayOptions() {
    displayOptions.viewMode = 'cloud'
    displayOptions.sortBy = 'count'
    displayOptions.sortOrder = 'desc'
    displayOptions.showSegmentDetails = false
    displayOptions.showStatistics = true
  }

  // ============= 返回Store API =============

  return {
    // 状态
    hotWords,
    statistics,
    loading,
    error,
    lastUpdated,
    lastRequest,
    filter,
    displayOptions,

    // 计算属性
    pageState,
    sortedHotWords,
    wordCloudData,
    legacyWordCloudData,
    legacyStatisticsData,
    legacyResponse,
    hasData,
    dataSummary,

    // 方法
    fetchHotWords,
    fetchStatistics,
    refreshAll,
    updateFilter,
    updateDisplayOptions,
    setTimeRange,
    setPresetTimeRange,
    exportReport,
    clearData,
    resetFilter,
    resetDisplayOptions
  }
})