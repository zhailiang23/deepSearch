import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import type { Ref, ComputedRef } from 'vue'
import { useDebounceFn, useStorage, watchDebounced } from '@vueuse/core'
import { useMobileSearchDemoStore } from '@/stores/mobileSearchDemo'
import { useParameterSync } from './useParameterSync'
import { useSearchCache } from './useSearchCache'
import { useSearchHistory } from './useSearchHistory'
import { useSearchPerformance } from './useSearchPerformance'
import type {
  SearchDemoConfig,
  SearchState,
  SearchResult,
  SearchResponse,
  SearchHistoryItem,
  SearchPerformanceMetrics,
  SearchApiAdapter,
  SearchSpaceOption
} from '@/types/demo'
import {
  DEFAULT_SEARCH_DEMO_CONFIG,
  BUILTIN_PRESETS
} from '@/types/demo'
import {
  createSmartDebounce,
  generateCacheKey,
  SearchRequestCanceller,
  PerformanceTimer,
  generateSearchSuggestions,
  SearchStatsCollector,
  isExpired
} from '@/utils/searchOptimization'

export interface UseMobileSearchDemoOptions {
  /** 是否启用缓存 */
  enableCache?: boolean
  /** 是否启用历史记录 */
  enableHistory?: boolean
  /** 是否启用参数同步 */
  enableSync?: boolean
  /** 缓存最大大小 */
  maxCacheSize?: number
  /** 历史记录最大数量 */
  maxHistoryItems?: number
  /** 默认防抖延迟 */
  defaultDebounce?: number
  /** API适配器 */
  apiAdapter?: SearchApiAdapter
  /** 自动保存配置 */
  autoSaveConfig?: boolean
}

export interface UseMobileSearchDemoReturn {
  // ==================== 状态管理 ====================

  /** 当前搜索配置 */
  config: Ref<SearchDemoConfig>
  /** 搜索状态 */
  searchState: Ref<SearchState>
  /** 搜索结果 */
  results: Ref<SearchResult[]>
  /** 可用搜索空间 */
  availableSpaces: Ref<SearchSpaceOption[]>

  // ==================== 计算属性 ====================

  /** 是否有搜索结果 */
  hasResults: ComputedRef<boolean>
  /** 是否正在搜索 */
  isSearching: ComputedRef<boolean>
  /** 是否有错误 */
  hasError: ComputedRef<boolean>
  /** 当前选中的搜索空间 */
  selectedSpaces: ComputedRef<SearchSpaceOption[]>
  /** 是否可以加载更多 */
  canLoadMore: ComputedRef<boolean>

  // ==================== 核心搜索功能 ====================

  /** 执行搜索 */
  search: (query: string, options?: { reset?: boolean; page?: number }) => Promise<void>
  /** 加载更多结果 */
  loadMore: () => Promise<void>
  /** 清空搜索结果 */
  clearResults: () => void
  /** 取消当前搜索 */
  cancelSearch: () => void
  /** 重试搜索 */
  retrySearch: () => Promise<void>

  // ==================== 配置管理 ====================

  /** 更新配置 */
  updateConfig: (updates: Partial<SearchDemoConfig>) => void
  /** 重置配置 */
  resetConfig: () => void
  /** 应用预设配置 */
  applyPreset: (presetId: string) => void
  /** 验证配置 */
  validateConfig: () => { valid: boolean; errors: string[] }

  // ==================== 参数同步 ====================

  /** 参数同步状态 */
  syncStatus: Readonly<Ref<import('./useParameterSync').UseParameterSyncReturn['syncStatus']['value']>>
  /** 强制同步 */
  forceSync: () => Promise<void>
  /** 从URL同步参数 */
  syncFromUrl: () => void
  /** 同步参数到URL */
  syncToUrl: () => void

  // ==================== 搜索历史 ====================

  /** 搜索历史 */
  searchHistory: Readonly<Ref<SearchHistoryItem[]>>
  /** 最近查询 */
  recentQueries: ComputedRef<string[]>
  /** 热门查询 */
  popularQueries: ComputedRef<Array<{ query: string; count: number }>>
  /** 添加到历史 */
  addToHistory: (query: string, resultCount: number, duration?: number) => void
  /** 清空历史 */
  clearHistory: () => void
  /** 从历史搜索 */
  searchFromHistory: (historyItem: SearchHistoryItem) => Promise<void>

  // ==================== 缓存管理 ====================

  /** 缓存统计 */
  cacheStats: ComputedRef<{
    size: number
    hitRate: number
    memoryUsage: number
  }>
  /** 清空缓存 */
  clearCache: () => void
  /** 预加载查询 */
  preloadQueries: (queries: string[]) => Promise<void>

  // ==================== 性能监控 ====================

  /** 性能指标 */
  performance: Readonly<Ref<SearchPerformanceMetrics>>
  /** 搜索建议 */
  searchSuggestions: Readonly<Ref<string[]>>
  /** 获取搜索统计 */
  getSearchStats: () => ReturnType<SearchStatsCollector['getStats']>

  // ==================== 工具方法 ====================

  /** 生成搜索建议 */
  generateSuggestions: (query: string) => string[]
  /** 高亮搜索词 */
  highlightQuery: (text: string, query: string) => string
  /** 格式化搜索时长 */
  formatDuration: (ms: number) => string
  /** 导出搜索历史 */
  exportHistory: () => string
  /** 导入搜索历史 */
  importHistory: (data: string) => boolean

  // ==================== 生命周期 ====================

  /** 初始化 */
  initialize: () => Promise<void>
  /** 重置所有状态 */
  resetAll: () => void
  /** 清理资源 */
  cleanup: () => void
}

const DEFAULT_OPTIONS: Required<UseMobileSearchDemoOptions> = {
  enableCache: true,
  enableHistory: true,
  enableSync: true,
  maxCacheSize: 100,
  maxHistoryItems: 100,
  defaultDebounce: 300,
  apiAdapter: undefined as any, // 将由使用者提供
  autoSaveConfig: true
}

/**
 * 移动端搜索演示主要 Composable
 * 整合状态管理、参数同步、搜索缓存、搜索历史等功能
 */
export function useMobileSearchDemo(
  options: UseMobileSearchDemoOptions = {}
): UseMobileSearchDemoReturn {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  // ==================== 核心依赖 ====================

  const store = useMobileSearchDemoStore()

  // 参数同步
  const parameterSync = useParameterSync({
    enableUrlSync: opts.enableSync,
    enableRealtimeSync: true,
    syncDebounceMs: opts.defaultDebounce
  })

  // 搜索缓存
  const searchCache = useSearchCache({
    maxSize: opts.maxCacheSize,
    enablePersistence: opts.enableCache
  })

  // 搜索历史
  const searchHistory = useSearchHistory({
    maxItems: opts.maxHistoryItems,
    enablePersistence: opts.enableHistory
  })

  // 搜索性能监控
  const searchPerformance = useSearchPerformance({
    enabled: true,
    enablePersistence: true,
    collectDetailedMetrics: true
  })

  // ==================== 工具实例 ====================

  const requestCanceller = new SearchRequestCanceller()
  const performanceTimer = new PerformanceTimer()
  const statsCollector = new SearchStatsCollector()

  // ==================== 状态引用 ====================

  const config = store.config
  const searchState = store.searchState
  const results = store.results
  const availableSpaces = store.availableSpaces
  const performance = store.performance

  // ==================== 持久化配置 ====================

  const persistedConfig = useStorage('mobile-search-demo-config', DEFAULT_SEARCH_DEMO_CONFIG)

  // 初始化时从持久化配置恢复
  if (opts.autoSaveConfig) {
    config.value = { ...DEFAULT_SEARCH_DEMO_CONFIG, ...persistedConfig.value }
  }

  // ==================== 响应式状态 ====================

  /** 搜索建议 */
  const searchSuggestions = ref<string[]>([])

  /** 当前搜索查询 */
  const currentQuery = ref('')

  /** 搜索ID计数器 */
  let searchIdCounter = 0

  // ==================== 计算属性 ====================

  const hasResults = computed(() => results.value.length > 0)

  const isSearching = computed(() => searchState.value.loading)

  const hasError = computed(() => !!searchState.value.error)

  const selectedSpaces = computed(() =>
    store.availableSpaces.filter(space =>
      store.config.searchSpaces.selected.includes(space.id)
    )
  )

  const canLoadMore = computed(() =>
    store.searchState.hasMore && !store.searchState.loading && !store.searchState.error
  )

  const cacheStats = computed(() => ({
    size: searchCache.size.value,
    hitRate: searchCache.hitRate.value,
    memoryUsage: searchCache.memory.value
  }))

  const recentQueries = computed(() => searchHistory.recentQueries.value)

  const popularQueries = computed(() => searchHistory.popularQueries.value)

  // ==================== 搜索防抖 ====================

  const debouncedSearch = createSmartDebounce(
    async (query: string, searchOptions: { reset?: boolean; page?: number } = {}) => {
      await performSearch(query, searchOptions)
    },
    config.value.searchBehavior.debounceMs,
    {
      minDelay: 100,
      maxDelay: 1000,
      queryLengthFactor: 0.1,
      complexityFactor: 1.2
    }
  )

  // ==================== 核心搜索方法 ====================

  /**
   * 执行实际的搜索操作
   */
  async function performSearch(
    query: string,
    options: { reset?: boolean; page?: number } = {}
  ): Promise<void> {
    const { reset = true, page = 0 } = options

    if (!query || query.length < config.value.searchBehavior.minQueryLength) {
      if (reset) {
        clearResults()
      }
      return
    }

    // 生成搜索ID
    const searchId = `search-${++searchIdCounter}`
    const cacheKey = generateCacheKey(query, config.value, page)

    // 记录搜索开始
    statsCollector.recordSearchStart(query)
    const performanceSearchId = searchPerformance.recordSearchStart(query, config.value)

    try {
      performanceTimer.start(searchId)

      // 设置搜索状态
      store.setSearchState({
        loading: true,
        error: null,
        query,
        searchId,
        page: reset ? 0 : searchState.value.page
      })

      // 检查缓存
      if (opts.enableCache) {
        const cached = searchCache.get(cacheKey)
        if (cached && !isExpired(cached)) {
          const duration = performanceTimer.end(searchId)

          // 使用缓存结果
          store.setResults(cached.results, !reset)
          store.setSearchState({
            loading: false,
            total: cached.results.length,
            duration,
            hasMore: false // 缓存结果通常是完整的
          })

          statsCollector.recordCacheHit()
          statsCollector.recordSearchSuccess(duration)
          store.updatePerformanceMetrics(duration, true)
          searchPerformance.recordCacheHit(query, JSON.stringify(cached.results).length)
          searchPerformance.recordSearchSuccess(performanceSearchId, duration, cached.results.length, true)

          // 添加到历史记录
          if (opts.enableHistory && reset) {
            addToHistory(query, cached.results.length, duration)
          }

          return
        }
      }

      // 取消之前的搜索
      requestCanceller.cancel(searchId)
      const controller = requestCanceller.createRequest(searchId)

      // 调用API搜索
      if (!opts.apiAdapter) {
        throw new Error('API adapter not provided')
      }

      const response = await opts.apiAdapter.search(query, config.value, {
        page,
        signal: controller.signal
      })

      // 检查是否已被取消
      if (controller.signal.aborted) {
        return
      }

      const duration = performanceTimer.end(searchId)

      // 更新结果
      store.setResults(response.results, !reset)
      store.setSearchState({
        loading: false,
        total: response.total,
        duration,
        hasMore: response.hasMore,
        page: response.page
      })

      // 缓存结果
      if (opts.enableCache) {
        searchCache.set(cacheKey, {
          results: response.results,
          config: config.value,
          hitCount: 0,
          size: JSON.stringify(response.results).length,
          query
        })
      }

      // 记录成功
      statsCollector.recordSearchSuccess(duration)
      store.updatePerformanceMetrics(duration, true)
      searchPerformance.recordSearchSuccess(performanceSearchId, duration, response.results.length, false)

      // 添加到历史记录
      if (opts.enableHistory && reset) {
        addToHistory(query, response.results.length, duration)
      }

      // 清理请求
      requestCanceller.cleanup(searchId)

    } catch (error) {
      const duration = performanceTimer.end(searchId)
      const errorMessage = error instanceof Error ? error.message : '搜索失败'

      // 设置错误状态
      store.setSearchState({
        loading: false,
        error: errorMessage
      })

      // 记录失败
      statsCollector.recordSearchFailure(errorMessage)
      store.updatePerformanceMetrics(duration, false)
      searchPerformance.recordSearchError(performanceSearchId, errorMessage, duration)

      console.error('Search failed:', error)

      // 清理请求
      requestCanceller.cleanup(searchId)
    }
  }

  /**
   * 执行搜索（公共接口）
   */
  const search = async (
    query: string,
    options: { reset?: boolean; page?: number } = {}
  ): Promise<void> => {
    currentQuery.value = query
    await debouncedSearch(query, options)
  }

  /**
   * 加载更多结果
   */
  const loadMore = async (): Promise<void> => {
    if (!canLoadMore.value) return

    const nextPage = searchState.value.page + 1
    await search(searchState.value.query, { reset: false, page: nextPage })
  }

  /**
   * 清空搜索结果
   */
  const clearResults = (): void => {
    store.clearResults()
    searchSuggestions.value = []
  }

  /**
   * 取消当前搜索
   */
  const cancelSearch = (): void => {
    // 使用 store 的取消方法
    store.cancelCurrentSearch()
    // 同时取消工具类的请求
    requestCanceller.cancelAll()
  }

  /**
   * 重试搜索
   */
  const retrySearch = async (): Promise<void> => {
    if (searchState.value.query) {
      await search(searchState.value.query, { reset: true })
    }
  }

  // ==================== 配置管理 ====================

  /**
   * 更新配置
   */
  const updateConfig = (updates: Partial<SearchDemoConfig>): void => {
    store.updateConfig(updates)

    // 触发参数同步
    if (opts.enableSync) {
      parameterSync.syncToMobile(config.value)
    }
  }

  /**
   * 重置配置
   */
  const resetConfig = (): void => {
    store.resetConfig()

    if (opts.enableSync) {
      parameterSync.syncToMobile(config.value)
    }
  }

  /**
   * 应用预设配置
   */
  const applyPreset = (presetId: string): void => {
    // 这里需要根据预设ID获取配置，暂时使用默认配置
    const preset = BUILTIN_PRESETS.find(p => p.id === presetId)
    if (preset) {
      updateConfig(preset.config)
    }
  }

  /**
   * 验证配置
   */
  const validateConfig = (): { valid: boolean; errors: string[] } => {
    return parameterSync.validateConfig(config.value)
  }

  // ==================== 参数同步方法 ====================

  const forceSync = async (): Promise<void> => {
    await parameterSync.forceSyncNow()
  }

  const syncFromUrl = (): void => {
    const urlConfig = parameterSync.syncFromUrl()
    if (urlConfig) {
      updateConfig(urlConfig)
    }
  }

  const syncToUrl = (): void => {
    parameterSync.syncToUrl(config.value)
  }

  // ==================== 历史记录方法 ====================

  /**
   * 添加到搜索历史
   */
  const addToHistory = (query: string, resultCount: number, duration?: number): void => {
    searchHistory.addToHistory({
      query,
      resultCount,
      config: config.value,
      duration
    })
  }

  /**
   * 清空搜索历史
   */
  const clearHistory = (): void => {
    searchHistory.clearHistory()
  }

  /**
   * 从历史记录搜索
   */
  const searchFromHistory = async (historyItem: SearchHistoryItem): Promise<void> => {
    // 先应用历史配置
    if (historyItem.config) {
      updateConfig(historyItem.config)
    }

    // 等待配置更新后再搜索
    await nextTick()
    await search(historyItem.query)
  }

  // ==================== 缓存管理方法 ====================

  /**
   * 清空缓存
   */
  const clearCache = (): void => {
    searchCache.clear()
  }

  /**
   * 预加载查询
   */
  const preloadQueries = async (queries: string[]): Promise<void> => {
    const preloadData = queries.map(query => ({ query, config: config.value }))
    await searchCache.preload(preloadData)
  }

  // ==================== 工具方法 ====================

  /**
   * 生成搜索建议
   */
  const generateSuggestions = (query: string): string[] => {
    const historyQueries = searchHistory.history.value.map(item => item.query)
    return generateSearchSuggestions(query, historyQueries, 5)
  }

  /**
   * 高亮搜索词
   */
  const highlightQuery = (text: string, query: string): string => {
    if (!query || !config.value.searchBehavior.highlightMatch) {
      return text
    }

    const regex = new RegExp(`(${query})`, 'gi')
    return text.replace(regex, '<mark>$1</mark>')
  }

  /**
   * 格式化搜索时长
   */
  const formatDuration = (ms: number): string => {
    if (ms < 1000) {
      return `${ms}ms`
    } else {
      return `${(ms / 1000).toFixed(1)}s`
    }
  }

  /**
   * 导出搜索历史
   */
  const exportHistory = (): string => {
    return searchHistory.exportHistory()
  }

  /**
   * 导入搜索历史
   */
  const importHistory = (data: string): boolean => {
    return searchHistory.importHistory(data)
  }

  /**
   * 获取搜索统计
   */
  const getSearchStats = () => {
    return statsCollector.getStats()
  }

  // ==================== 生命周期方法 ====================

  /**
   * 初始化
   */
  const initialize = async (): Promise<void> => {
    try {
      // 从URL同步参数
      if (opts.enableSync) {
        syncFromUrl()
      }

      // 加载可用搜索空间（如果API提供了该方法）
      if (opts.apiAdapter && opts.apiAdapter.getSpaces) {
        const spaces = await opts.apiAdapter.getSpaces()
        store.setSearchSpaces(spaces)
      }

    } catch (error) {
      console.error('Failed to initialize mobile search demo:', error)
    }
  }

  /**
   * 重置所有状态
   */
  const resetAll = (): void => {
    store.resetAll()
    searchCache.clear()
    searchHistory.clearHistory()
    parameterSync.clearChangeHistory()
    statsCollector.reset()
    searchSuggestions.value = []
  }

  /**
   * 清理资源
   */
  const cleanup = (): void => {
    requestCanceller.cancelAll()
    performanceTimer.clear()
    searchCache.cleanup()
    searchHistory.cleanup()
    parameterSync.cleanup()
  }

  // ==================== 响应式更新 ====================

  // 监听查询变化生成建议
  watchDebounced(
    currentQuery,
    (newQuery) => {
      if (newQuery && newQuery.length > 0) {
        searchSuggestions.value = generateSuggestions(newQuery)
      } else {
        searchSuggestions.value = []
      }
    },
    { debounce: 200 }
  )

  // 监听配置变化并持久化
  if (opts.autoSaveConfig) {
    watch(
      config,
      (newConfig) => {
        persistedConfig.value = newConfig
      },
      { deep: true }
    )
  }

  // 监听搜索空间变化
  watch(
    () => config.value.searchSpaces.selected,
    () => {
      // 搜索空间变化时清空结果，避免混淆
      if (hasResults.value) {
        clearResults()
      }
    },
    { deep: true }
  )

  // ==================== 清理 ====================

  onUnmounted(() => {
    cleanup()
  })

  // ==================== 返回接口 ====================

  return {
    // 状态管理
    config,
    searchState,
    results,
    availableSpaces,

    // 计算属性
    hasResults,
    isSearching,
    hasError,
    selectedSpaces,
    canLoadMore,

    // 核心搜索功能
    search,
    loadMore,
    clearResults,
    cancelSearch,
    retrySearch,

    // 配置管理
    updateConfig,
    resetConfig,
    applyPreset,
    validateConfig,

    // 参数同步
    syncStatus: parameterSync.syncStatus,
    forceSync,
    syncFromUrl,
    syncToUrl,

    // 搜索历史
    searchHistory: searchHistory.history,
    recentQueries,
    popularQueries,
    addToHistory,
    clearHistory,
    searchFromHistory,

    // 缓存管理
    cacheStats,
    clearCache,
    preloadQueries,

    // 性能监控
    performance,
    searchSuggestions,
    getSearchStats,

    // 工具方法
    generateSuggestions,
    highlightQuery,
    formatDuration,
    exportHistory,
    importHistory,

    // 生命周期
    initialize,
    resetAll,
    cleanup
  }
}