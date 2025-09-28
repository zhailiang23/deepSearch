/**
 * 热词数据管理组合函数
 * 提供热词数据的获取、缓存、状态管理等功能
 */

import { ref, computed, watch, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useHotWordStatisticsStore } from '@/stores/hotWordStatistics'
import type {
  HotWordRequest,
  HotWordFilter,
  HotWordDisplayOptions,
  HotWordResponse
} from '@/types/hotWord'

export interface UseHotWordDataOptions {
  /** 是否自动加载数据 */
  autoLoad?: boolean
  /** 自动刷新间隔（毫秒） */
  autoRefreshInterval?: number
  /** 是否启用缓存 */
  enableCache?: boolean
  /** 缓存过期时间（毫秒） */
  cacheExpiry?: number
  /** 防抖延迟（毫秒） */
  debounceDelay?: number
}

/**
 * 热词数据管理组合函数
 */
export function useHotWordData(options: UseHotWordDataOptions = {}) {
  const {
    autoLoad = false,
    autoRefreshInterval = 0,
    enableCache = true,
    cacheExpiry = 5 * 60 * 1000, // 5分钟
    debounceDelay = 300
  } = options

  // ============= Store和响应式引用 =============

  const store = useHotWordStatisticsStore()
  const {
    hotWords,
    statistics,
    loading,
    error,
    lastUpdated,
    filter,
    displayOptions,
    pageState,
    sortedHotWords,
    wordCloudData,
    hasData,
    dataSummary
  } = storeToRefs(store)

  // 内部状态
  const autoRefreshTimer = ref<NodeJS.Timeout | null>(null)
  const debounceTimer = ref<NodeJS.Timeout | null>(null)

  // ============= 计算属性 =============

  /** 是否需要刷新数据 */
  const needsRefresh = computed(() => {
    if (!enableCache || !lastUpdated.value) return true
    return Date.now() - lastUpdated.value.getTime() > cacheExpiry
  })

  /** 数据是否过期 */
  const isDataStale = computed(() => {
    if (!lastUpdated.value) return true
    return Date.now() - lastUpdated.value.getTime() > cacheExpiry
  })

  /** 筛选后的热词数据 */
  const filteredHotWords = computed(() => {
    let filtered = [...sortedHotWords.value]

    // 可以在这里添加额外的筛选逻辑
    // 例如根据用户输入的关键词筛选
    return filtered
  })

  // ============= 数据获取方法 =============

  /**
   * 加载热词数据
   */
  async function loadHotWords(params?: Partial<HotWordRequest>) {
    try {
      await store.fetchHotWords(params)
    } catch (err) {
      console.error('加载热词数据失败:', err)
      throw err
    }
  }

  /**
   * 加载统计数据
   */
  async function loadStatistics() {
    try {
      await store.fetchStatistics()
    } catch (err) {
      console.error('加载统计数据失败:', err)
      throw err
    }
  }

  /**
   * 刷新所有数据
   */
  async function refreshData() {
    try {
      await store.refreshAll()
    } catch (err) {
      console.error('刷新数据失败:', err)
      throw err
    }
  }

  /**
   * 防抖的数据加载
   */
  function debouncedLoadHotWords(params?: Partial<HotWordRequest>) {
    if (debounceTimer.value) {
      clearTimeout(debounceTimer.value)
    }

    debounceTimer.value = setTimeout(async () => {
      await loadHotWords(params)
    }, debounceDelay)
  }

  /**
   * 智能加载数据（根据缓存状态决定是否加载）
   */
  async function smartLoadData() {
    if (needsRefresh.value) {
      await refreshData()
    }
  }

  // ============= 筛选和显示管理 =============

  /**
   * 更新筛选条件并重新加载数据
   */
  async function updateFilterAndReload(newFilter: Partial<HotWordFilter>) {
    store.updateFilter(newFilter)
    await debouncedLoadHotWords()
  }

  /**
   * 更新显示选项
   */
  function updateDisplayOptions(newOptions: Partial<HotWordDisplayOptions>) {
    store.updateDisplayOptions(newOptions)
  }

  /**
   * 设置时间范围并重新加载
   */
  async function setTimeRangeAndReload(start: Date, end: Date) {
    store.setTimeRange(start, end)
    await debouncedLoadHotWords()
  }

  /**
   * 设置预设时间范围并重新加载
   */
  async function setPresetTimeRangeAndReload(preset: 'today' | 'week' | 'month' | '3months' | '6months') {
    store.setPresetTimeRange(preset)
    await debouncedLoadHotWords()
  }

  // ============= 自动刷新管理 =============

  /**
   * 启动自动刷新
   */
  function startAutoRefresh() {
    if (autoRefreshInterval > 0 && !autoRefreshTimer.value) {
      autoRefreshTimer.value = setInterval(async () => {
        if (!loading.value) {
          try {
            await refreshData()
            console.log('自动刷新数据完成')
          } catch (err) {
            console.error('自动刷新数据失败:', err)
          }
        }
      }, autoRefreshInterval)

      console.log(`启动自动刷新，间隔: ${autoRefreshInterval}ms`)
    }
  }

  /**
   * 停止自动刷新
   */
  function stopAutoRefresh() {
    if (autoRefreshTimer.value) {
      clearInterval(autoRefreshTimer.value)
      autoRefreshTimer.value = null
      console.log('停止自动刷新')
    }
  }

  // ============= 导出功能 =============

  /**
   * 导出热词报告
   */
  async function exportReport() {
    try {
      await store.exportReport()
    } catch (err) {
      console.error('导出报告失败:', err)
      throw err
    }
  }

  // ============= 工具方法 =============

  /**
   * 重置所有设置
   */
  function resetAll() {
    store.resetFilter()
    store.resetDisplayOptions()
    store.clearData()
  }

  /**
   * 查找热词
   */
  function findHotWord(word: string): HotWordResponse | undefined {
    return hotWords.value.find(hw => hw.word === word)
  }

  /**
   * 获取热词排名
   */
  function getHotWordRank(word: string): number {
    const index = sortedHotWords.value.findIndex(hw => hw.word === word)
    return index >= 0 ? index + 1 : 0
  }

  /**
   * 格式化数字
   */
  function formatNumber(num: number): string {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M'
    } else if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K'
    }
    return num.toString()
  }

  // ============= 生命周期管理 =============

  /**
   * 初始化
   */
  async function initialize() {
    try {
      if (autoLoad) {
        await smartLoadData()
      }

      if (autoRefreshInterval > 0) {
        startAutoRefresh()
      }
    } catch (err) {
      console.error('初始化失败:', err)
    }
  }

  /**
   * 清理资源
   */
  function cleanup() {
    stopAutoRefresh()

    if (debounceTimer.value) {
      clearTimeout(debounceTimer.value)
    }
  }

  // ============= 监听器 =============

  // 监听筛选条件变化，自动重新加载数据
  watch(
    () => [filter.value.dateRange, filter.value.limit, filter.value.minWordLength],
    () => {
      if (autoLoad) {
        debouncedLoadHotWords()
      }
    },
    { deep: true }
  )

  // ============= 返回API =============

  return {
    // 状态数据
    hotWords,
    statistics,
    loading,
    error,
    lastUpdated,
    filter,
    displayOptions,
    pageState,
    sortedHotWords,
    wordCloudData,
    hasData,
    dataSummary,
    filteredHotWords,

    // 计算属性
    needsRefresh,
    isDataStale,

    // 数据获取方法
    loadHotWords,
    loadStatistics,
    refreshData,
    debouncedLoadHotWords,
    smartLoadData,

    // 筛选和显示管理
    updateFilterAndReload,
    updateDisplayOptions,
    setTimeRangeAndReload,
    setPresetTimeRangeAndReload,

    // 自动刷新管理
    startAutoRefresh,
    stopAutoRefresh,

    // 导出功能
    exportReport,

    // 工具方法
    resetAll,
    findHotWord,
    getHotWordRank,
    formatNumber,

    // 生命周期管理
    initialize,
    cleanup
  }
}