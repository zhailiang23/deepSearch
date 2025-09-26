import { ref, computed, watch, onUnmounted } from 'vue'
import type { SearchHistoryItem, SearchDemoConfig, SearchSpaceOption } from '@/types/demo'

export interface UseSearchHistoryOptions {
  /** 最大历史记录数量 */
  maxItems?: number
  /** 是否启用持久化到localStorage */
  enablePersistence?: boolean
  /** 持久化键名 */
  persistenceKey?: string
  /** 是否去重（相同查询和配置） */
  deduplicate?: boolean
  /** 自动清理过期项的天数 */
  expireDays?: number
}

export interface UseSearchHistoryReturn {
  // 历史记录数据
  history: Readonly<import('vue').Ref<SearchHistoryItem[]>>
  recentQueries: Readonly<import('vue').Ref<string[]>>
  popularQueries: Readonly<import('vue').Ref<Array<{ query: string; count: number }>>>
  totalItems: Readonly<import('vue').Ref<number>>

  // 历史记录操作
  addToHistory: (item: Omit<SearchHistoryItem, 'timestamp'>) => void
  removeFromHistory: (index: number) => void
  clearHistory: () => void
  clearExpiredItems: () => number

  // 查询操作
  searchHistory: (query: string) => SearchHistoryItem[]
  getHistoryByQuery: (query: string) => SearchHistoryItem[]
  getHistoryByTimeRange: (start: number, end: number) => SearchHistoryItem[]
  getHistoryByConfig: (config: Partial<SearchDemoConfig>) => SearchHistoryItem[]

  // 统计功能
  getQueryFrequency: (query: string) => number
  getTotalSearches: () => number
  getAverageResultCount: () => number
  getSearchTrends: (days: number) => Array<{ date: string; count: number }>

  // 导入导出
  exportHistory: () => string
  importHistory: (data: string) => boolean
  mergeHistory: (items: SearchHistoryItem[]) => void

  // 配置管理
  setMaxItems: (max: number) => void
  setExpireDays: (days: number) => void

  // 生命周期
  cleanup: () => void
}

const DEFAULT_OPTIONS: Required<UseSearchHistoryOptions> = {
  maxItems: 100,
  enablePersistence: true,
  persistenceKey: 'mobile-search-demo-history',
  deduplicate: true,
  expireDays: 30
}

/**
 * 搜索历史管理 Composable
 */
export function useSearchHistory(options: UseSearchHistoryOptions = {}): UseSearchHistoryReturn {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  // ==================== 状态定义 ====================

  /** 搜索历史列表 */
  const history = ref<SearchHistoryItem[]>([])

  /** 配置 */
  const maxItems = ref(opts.maxItems)
  const expireDays = ref(opts.expireDays)

  /** 清理定时器 */
  let cleanupTimer: number | null = null

  // ==================== 计算属性 ====================

  /** 最近的查询词（去重） */
  const recentQueries = computed(() => {
    const queries = new Set<string>()
    return history.value
      .filter(item => {
        if (queries.has(item.query)) {
          return false
        }
        queries.add(item.query)
        return true
      })
      .map(item => item.query)
      .slice(0, 10)
  })

  /** 热门查询词（按频率排序） */
  const popularQueries = computed(() => {
    const queryCount = new Map<string, number>()

    history.value.forEach(item => {
      const current = queryCount.get(item.query) || 0
      queryCount.set(item.query, current + 1)
    })

    return Array.from(queryCount.entries())
      .map(([query, count]) => ({ query, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10)
  })

  /** 总条目数 */
  const totalItems = computed(() => history.value.length)

  // ==================== 核心方法 ====================

  /**
   * 添加到历史记录
   */
  const addToHistory = (item: Omit<SearchHistoryItem, 'timestamp'>): void => {
    const now = Date.now()
    const newItem: SearchHistoryItem = {
      ...item,
      timestamp: now
    }

    // 检查去重
    if (opts.deduplicate) {
      const existingIndex = history.value.findIndex(existing =>
        existing.query === newItem.query &&
        JSON.stringify(existing.config) === JSON.stringify(newItem.config)
      )

      if (existingIndex > -1) {
        // 更新现有记录的时间戳
        history.value[existingIndex].timestamp = now
        history.value[existingIndex].resultCount = newItem.resultCount
        if (newItem.duration) {
          history.value[existingIndex].duration = newItem.duration
        }
        // 移动到最前面
        const updatedItem = history.value.splice(existingIndex, 1)[0]
        history.value.unshift(updatedItem)
        persistHistory()
        return
      }
    }

    // 添加新记录到开头
    history.value.unshift(newItem)

    // 限制数量
    if (history.value.length > maxItems.value) {
      history.value = history.value.slice(0, maxItems.value)
    }

    persistHistory()
  }

  /**
   * 从历史记录中删除指定索引的项
   */
  const removeFromHistory = (index: number): void => {
    if (index >= 0 && index < history.value.length) {
      history.value.splice(index, 1)
      persistHistory()
    }
  }

  /**
   * 清空所有历史记录
   */
  const clearHistory = (): void => {
    history.value = []
    clearPersistence()
  }

  /**
   * 清理过期项
   */
  const clearExpiredItems = (): number => {
    const expireTime = Date.now() - (expireDays.value * 24 * 60 * 60 * 1000)
    const originalLength = history.value.length

    history.value = history.value.filter(item => item.timestamp > expireTime)

    const removedCount = originalLength - history.value.length
    if (removedCount > 0) {
      persistHistory()
    }

    return removedCount
  }

  // ==================== 查询方法 ====================

  /**
   * 搜索历史记录
   */
  const searchHistory = (query: string): SearchHistoryItem[] => {
    const lowerQuery = query.toLowerCase()
    return history.value.filter(item =>
      item.query.toLowerCase().includes(lowerQuery)
    )
  }

  /**
   * 根据查询词获取历史记录
   */
  const getHistoryByQuery = (query: string): SearchHistoryItem[] => {
    return history.value.filter(item => item.query === query)
  }

  /**
   * 根据时间范围获取历史记录
   */
  const getHistoryByTimeRange = (start: number, end: number): SearchHistoryItem[] => {
    return history.value.filter(item =>
      item.timestamp >= start && item.timestamp <= end
    )
  }

  /**
   * 根据配置获取历史记录
   */
  const getHistoryByConfig = (config: Partial<SearchDemoConfig>): SearchHistoryItem[] => {
    return history.value.filter(item => {
      if (!item.config) return false

      // 简单的配置匹配（可以根据需要扩展）
      for (const [key, value] of Object.entries(config)) {
        if (JSON.stringify(item.config[key as keyof SearchDemoConfig]) !== JSON.stringify(value)) {
          return false
        }
      }
      return true
    })
  }

  // ==================== 统计方法 ====================

  /**
   * 获取查询词频率
   */
  const getQueryFrequency = (query: string): number => {
    return history.value.filter(item => item.query === query).length
  }

  /**
   * 获取总搜索次数
   */
  const getTotalSearches = (): number => {
    return history.value.length
  }

  /**
   * 获取平均结果数量
   */
  const getAverageResultCount = (): number => {
    if (history.value.length === 0) return 0

    const total = history.value.reduce((sum, item) => sum + item.resultCount, 0)
    return Math.round(total / history.value.length)
  }

  /**
   * 获取搜索趋势
   */
  const getSearchTrends = (days: number): Array<{ date: string; count: number }> => {
    const now = new Date()
    const trends: Array<{ date: string; count: number }> = []

    for (let i = days - 1; i >= 0; i--) {
      const date = new Date(now)
      date.setDate(date.getDate() - i)
      const dateStr = date.toISOString().split('T')[0]

      const startOfDay = new Date(date)
      startOfDay.setHours(0, 0, 0, 0)
      const endOfDay = new Date(date)
      endOfDay.setHours(23, 59, 59, 999)

      const count = history.value.filter(item =>
        item.timestamp >= startOfDay.getTime() &&
        item.timestamp <= endOfDay.getTime()
      ).length

      trends.push({ date: dateStr, count })
    }

    return trends
  }

  // ==================== 导入导出 ====================

  /**
   * 导出历史记录为JSON字符串
   */
  const exportHistory = (): string => {
    const exportData = {
      version: '1.0',
      timestamp: Date.now(),
      history: history.value
    }
    return JSON.stringify(exportData, null, 2)
  }

  /**
   * 从JSON字符串导入历史记录
   */
  const importHistory = (data: string): boolean => {
    try {
      const importData = JSON.parse(data)

      if (!importData.history || !Array.isArray(importData.history)) {
        console.warn('Invalid history data format')
        return false
      }

      // 验证数据格式
      const isValid = importData.history.every((item: any) =>
        typeof item.query === 'string' &&
        typeof item.timestamp === 'number' &&
        typeof item.resultCount === 'number'
      )

      if (!isValid) {
        console.warn('Invalid history item format')
        return false
      }

      history.value = importData.history
      persistHistory()
      return true
    } catch (error) {
      console.error('Failed to import history:', error)
      return false
    }
  }

  /**
   * 合并历史记录
   */
  const mergeHistory = (items: SearchHistoryItem[]): void => {
    const merged = [...history.value, ...items]

    // 去重并按时间戳排序
    const uniqueItems = new Map<string, SearchHistoryItem>()

    merged.forEach(item => {
      const key = `${item.query}:${JSON.stringify(item.config)}`
      const existing = uniqueItems.get(key)

      if (!existing || item.timestamp > existing.timestamp) {
        uniqueItems.set(key, item)
      }
    })

    history.value = Array.from(uniqueItems.values())
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, maxItems.value)

    persistHistory()
  }

  // ==================== 配置管理 ====================

  /**
   * 设置最大条目数
   */
  const setMaxItems = (max: number): void => {
    maxItems.value = max

    if (history.value.length > max) {
      history.value = history.value.slice(0, max)
      persistHistory()
    }
  }

  /**
   * 设置过期天数
   */
  const setExpireDays = (days: number): void => {
    expireDays.value = days
  }

  // ==================== 持久化 ====================

  /**
   * 持久化历史记录
   */
  const persistHistory = (): void => {
    if (!opts.enablePersistence) return

    try {
      const data = {
        version: '1.0',
        timestamp: Date.now(),
        history: history.value
      }
      localStorage.setItem(opts.persistenceKey, JSON.stringify(data))
    } catch (error) {
      console.warn('Failed to persist search history:', error)
    }
  }

  /**
   * 加载持久化的历史记录
   */
  const loadHistory = (): void => {
    if (!opts.enablePersistence) return

    try {
      const stored = localStorage.getItem(opts.persistenceKey)
      if (stored) {
        const data = JSON.parse(stored)
        if (data.history && Array.isArray(data.history)) {
          history.value = data.history
          // 清理过期项
          clearExpiredItems()
        }
      }
    } catch (error) {
      console.warn('Failed to load search history:', error)
      history.value = []
    }
  }

  /**
   * 清除持久化存储
   */
  const clearPersistence = (): void => {
    if (opts.enablePersistence) {
      try {
        localStorage.removeItem(opts.persistenceKey)
      } catch (error) {
        console.warn('Failed to clear persisted history:', error)
      }
    }
  }

  // ==================== 定时清理 ====================

  /**
   * 开始定时清理过期项
   */
  const startPeriodicCleanup = (): void => {
    // 每小时清理一次过期项
    cleanupTimer = window.setInterval(() => {
      clearExpiredItems()
    }, 60 * 60 * 1000)
  }

  /**
   * 停止定时清理
   */
  const stopPeriodicCleanup = (): void => {
    if (cleanupTimer) {
      clearInterval(cleanupTimer)
      cleanupTimer = null
    }
  }

  /**
   * 清理资源
   */
  const cleanup = (): void => {
    stopPeriodicCleanup()
  }

  // ==================== 生命周期 ====================

  // 监听最大条目数变化
  watch(maxItems, (newMax) => {
    if (history.value.length > newMax) {
      history.value = history.value.slice(0, newMax)
      persistHistory()
    }
  })

  // 初始化
  loadHistory()
  startPeriodicCleanup()

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })

  // ==================== 返回接口 ====================

  return {
    // 历史记录数据
    history: history.value as any,
    recentQueries,
    popularQueries,
    totalItems,

    // 历史记录操作
    addToHistory,
    removeFromHistory,
    clearHistory,
    clearExpiredItems,

    // 查询操作
    searchHistory,
    getHistoryByQuery,
    getHistoryByTimeRange,
    getHistoryByConfig,

    // 统计功能
    getQueryFrequency,
    getTotalSearches,
    getAverageResultCount,
    getSearchTrends,

    // 导入导出
    exportHistory,
    importHistory,
    mergeHistory,

    // 配置管理
    setMaxItems,
    setExpireDays,

    // 生命周期
    cleanup
  }
}