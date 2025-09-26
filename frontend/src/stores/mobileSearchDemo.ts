import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type {
  SearchDemoConfig,
  SearchState,
  SearchResult,
  SearchHistoryItem,
  SearchPerformanceMetrics,
  SearchCacheEntry,
  ParameterChangeEvent,
  SyncStatus,
  SearchSpaceOption
} from '@/types/demo'
import { DEFAULT_SEARCH_DEMO_CONFIG } from '@/types/demo'

export const useMobileSearchDemoStore = defineStore('mobileSearchDemo', () => {
  // ==================== 核心状态 ====================

  /** 搜索演示配置 */
  const config = ref<SearchDemoConfig>({ ...DEFAULT_SEARCH_DEMO_CONFIG })

  /** 搜索状态 */
  const searchState = ref<SearchState>({
    loading: false,
    error: null,
    query: '',
    hasMore: false,
    page: 0,
    total: 0,
    duration: 0
  })

  /** 搜索结果 */
  const results = ref<SearchResult[]>([])

  /** 可用的搜索空间列表 */
  const availableSpaces = ref<SearchSpaceOption[]>([])

  // ==================== 历史记录状态 ====================

  /** 搜索历史 */
  const searchHistory = ref<SearchHistoryItem[]>([])

  /** 最大历史记录数量 */
  const maxHistoryItems = ref(50)

  // ==================== 缓存状态 ====================

  /** 搜索结果缓存 */
  const searchCache = ref<Map<string, SearchCacheEntry>>(new Map())

  /** 缓存最大大小 */
  const maxCacheSize = ref(100)

  /** 缓存过期时间（毫秒） */
  const cacheTimeout = ref(5 * 60 * 1000) // 5分钟

  // ==================== 性能监控状态 ====================

  /** 性能指标 */
  const performance = ref<SearchPerformanceMetrics>({
    searchCount: 0,
    averageResponseTime: 0,
    cacheHitRate: 0,
    errorRate: 0,
    lastSearchTime: 0,
    slowestSearchTime: 0,
    fastestSearchTime: Number.MAX_SAFE_INTEGER,
    successCount: 0,
    errorCount: 0
  })

  // ==================== 同步状态 ====================

  /** 参数同步状态 */
  const syncStatus = ref<SyncStatus>({
    pending: false,
    error: undefined,
    lastSync: Date.now()
  })

  /** 参数变更队列 */
  const parameterChanges = ref<ParameterChangeEvent[]>([])

  // ==================== 搜索控制 ====================

  /** 当前搜索请求的中止控制器 */
  const currentAbortController = ref<AbortController | null>(null)

  /** 搜索请求队列 */
  const searchRequestQueue = ref<Array<{
    id: string
    query: string
    config: SearchDemoConfig
    timestamp: number
  }>>([])

  // ==================== Getters ====================

  /** 当前选中的搜索空间 */
  const selectedSpaces = computed(() =>
    availableSpaces.value.filter(space =>
      config.value.searchSpaces.selected.includes(space.id)
    )
  )

  /** 是否有搜索结果 */
  const hasResults = computed(() => results.value.length > 0)

  /** 是否正在搜索 */
  const isSearching = computed(() => searchState.value.loading)

  /** 是否有搜索错误 */
  const hasError = computed(() => !!searchState.value.error)

  /** 缓存命中率 */
  const cacheHitRate = computed(() => {
    if (performance.value.searchCount === 0) return 0
    return performance.value.cacheHitRate / performance.value.searchCount
  })

  /** 搜索成功率 */
  const successRate = computed(() => {
    if (performance.value.searchCount === 0) return 0
    return performance.value.successCount / performance.value.searchCount
  })

  // ==================== Actions ====================

  /**
   * 更新搜索配置
   */
  const updateConfig = (updates: Partial<SearchDemoConfig>) => {
    const oldConfig = { ...config.value }
    config.value = { ...config.value, ...updates }

    // 记录参数变更
    const change: ParameterChangeEvent = {
      type: 'behavior',
      key: 'config',
      value: config.value,
      previous: oldConfig,
      timestamp: Date.now()
    }
    parameterChanges.value.push(change)

    // 触发同步
    triggerSync()
  }

  /**
   * 设置搜索空间
   */
  const setSearchSpaces = (spaces: SearchSpaceOption[]) => {
    availableSpaces.value = spaces

    // 更新配置中的可用空间
    config.value.searchSpaces.available = spaces

    // 验证当前选中的搜索空间是否仍然有效
    const validSpaceIds = spaces.map(space => space.id)
    const invalidSelected = config.value.searchSpaces.selected.filter(
      id => !validSpaceIds.includes(id)
    )

    // 移除无效的搜索空间选择
    if (invalidSelected.length > 0) {
      config.value.searchSpaces.selected = config.value.searchSpaces.selected.filter(
        id => validSpaceIds.includes(id)
      )
      console.warn('移除无效的搜索空间:', invalidSelected)
    }
  }

  /**
   * 选择搜索空间
   */
  const selectSearchSpaces = (spaceIds: string[]) => {
    const change: ParameterChangeEvent = {
      type: 'searchSpace',
      key: 'selected',
      value: spaceIds,
      previous: config.value.searchSpaces.selected,
      timestamp: Date.now()
    }

    config.value.searchSpaces.selected = spaceIds
    parameterChanges.value.push(change)
    triggerSync()
  }

  /**
   * 设置搜索状态
   */
  const setSearchState = (updates: Partial<SearchState>) => {
    searchState.value = { ...searchState.value, ...updates }
  }

  /**
   * 设置搜索结果
   */
  const setResults = (newResults: SearchResult[], append = false) => {
    if (append) {
      results.value = [...results.value, ...newResults]
    } else {
      results.value = newResults
    }
  }

  /**
   * 清空搜索结果
   */
  const clearResults = () => {
    results.value = []
    searchState.value.total = 0
    searchState.value.hasMore = false
    searchState.value.page = 0
  }

  /**
   * 添加搜索历史
   */
  const addToHistory = (historyItem: Omit<SearchHistoryItem, 'timestamp'>) => {
    const item: SearchHistoryItem = {
      ...historyItem,
      timestamp: Date.now()
    }

    // 检查是否已存在相同查询
    const existingIndex = searchHistory.value.findIndex(
      h => h.query === item.query &&
      JSON.stringify(h.config) === JSON.stringify(item.config)
    )

    if (existingIndex > -1) {
      // 更新现有记录
      searchHistory.value[existingIndex] = item
    } else {
      // 添加新记录
      searchHistory.value.unshift(item)

      // 限制历史记录数量
      if (searchHistory.value.length > maxHistoryItems.value) {
        searchHistory.value = searchHistory.value.slice(0, maxHistoryItems.value)
      }
    }

    // 持久化到本地存储
    persistSearchHistory()
  }

  /**
   * 清空搜索历史
   */
  const clearHistory = () => {
    searchHistory.value = []
    localStorage.removeItem('mobile-search-demo-history')
  }

  /**
   * 缓存搜索结果
   */
  const cacheResults = (key: string, entry: Omit<SearchCacheEntry, 'timestamp' | 'expiresAt'>) => {
    const cacheEntry: SearchCacheEntry = {
      ...entry,
      timestamp: Date.now(),
      expiresAt: Date.now() + cacheTimeout.value
    }

    searchCache.value.set(key, cacheEntry)

    // 检查缓存大小限制
    if (searchCache.value.size > maxCacheSize.value) {
      // 删除最旧的缓存项
      const oldestKey = Array.from(searchCache.value.keys())[0]
      searchCache.value.delete(oldestKey)
    }
  }

  /**
   * 获取缓存结果
   */
  const getCachedResults = (key: string): SearchCacheEntry | null => {
    const entry = searchCache.value.get(key)
    if (!entry) return null

    // 检查是否过期
    if (Date.now() > entry.expiresAt) {
      searchCache.value.delete(key)
      return null
    }

    // 增加命中次数
    entry.hitCount++
    return entry
  }

  /**
   * 清空缓存
   */
  const clearCache = () => {
    searchCache.value.clear()
  }

  /**
   * 更新性能指标
   */
  const updatePerformanceMetrics = (duration: number, success: boolean) => {
    performance.value.searchCount++
    performance.value.lastSearchTime = duration

    if (success) {
      performance.value.successCount++

      // 更新响应时间统计
      const totalResponseTime = performance.value.averageResponseTime *
        (performance.value.successCount - 1) + duration
      performance.value.averageResponseTime = totalResponseTime / performance.value.successCount

      // 更新最快/最慢搜索时间
      if (duration > performance.value.slowestSearchTime) {
        performance.value.slowestSearchTime = duration
      }
      if (duration < performance.value.fastestSearchTime) {
        performance.value.fastestSearchTime = duration
      }
    } else {
      performance.value.errorCount++
    }

    // 更新错误率
    performance.value.errorRate = performance.value.errorCount / performance.value.searchCount
  }

  /**
   * 取消当前搜索
   */
  const cancelCurrentSearch = () => {
    if (currentAbortController.value) {
      currentAbortController.value.abort()
      currentAbortController.value = null
    }

    // 设置搜索状态
    searchState.value.loading = false
    searchState.value.error = null

    // 清空搜索队列
    searchRequestQueue.value = []
  }

  /**
   * 开始新的搜索请求
   */
  const startSearchRequest = (query: string, options: { reset?: boolean } = {}) => {
    // 取消之前的搜索
    cancelCurrentSearch()

    // 创建新的中止控制器
    const abortController = new AbortController()
    currentAbortController.value = abortController

    // 生成请求ID
    const requestId = `search-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

    // 添加到请求队列
    const request = {
      id: requestId,
      query,
      config: { ...config.value },
      timestamp: Date.now()
    }
    searchRequestQueue.value.push(request)

    // 限制队列大小
    if (searchRequestQueue.value.length > 10) {
      searchRequestQueue.value = searchRequestQueue.value.slice(-5)
    }

    return {
      requestId,
      abortController,
      signal: abortController.signal
    }
  }

  /**
   * 完成搜索请求
   */
  const completeSearchRequest = (requestId: string) => {
    // 从队列中移除完成的请求
    searchRequestQueue.value = searchRequestQueue.value.filter(req => req.id !== requestId)

    // 如果是当前请求，清理控制器
    if (currentAbortController.value) {
      currentAbortController.value = null
    }
  }

  /**
   * 重置配置为默认值
   */
  const resetConfig = () => {
    config.value = { ...DEFAULT_SEARCH_DEMO_CONFIG }
    triggerSync()
  }

  /**
   * 触发同步
   */
  const triggerSync = () => {
    syncStatus.value.pending = true
    syncStatus.value.lastSync = Date.now()

    // 在下一个 tick 中完成同步
    setTimeout(() => {
      syncStatus.value.pending = false
    }, 0)
  }

  /**
   * 持久化搜索历史到本地存储
   */
  const persistSearchHistory = () => {
    try {
      localStorage.setItem(
        'mobile-search-demo-history',
        JSON.stringify(searchHistory.value)
      )
    } catch (error) {
      console.warn('Failed to persist search history:', error)
    }
  }

  /**
   * 从本地存储加载搜索历史
   */
  const loadSearchHistory = () => {
    try {
      const stored = localStorage.getItem('mobile-search-demo-history')
      if (stored) {
        searchHistory.value = JSON.parse(stored)
      }
    } catch (error) {
      console.warn('Failed to load search history:', error)
      searchHistory.value = []
    }
  }

  /**
   * 持久化配置到本地存储
   */
  const persistConfig = () => {
    try {
      localStorage.setItem(
        'mobile-search-demo-config',
        JSON.stringify(config.value)
      )
    } catch (error) {
      console.warn('Failed to persist config:', error)
    }
  }

  /**
   * 从本地存储加载配置
   */
  const loadConfig = () => {
    try {
      const stored = localStorage.getItem('mobile-search-demo-config')
      if (stored) {
        config.value = { ...DEFAULT_SEARCH_DEMO_CONFIG, ...JSON.parse(stored) }
      }
    } catch (error) {
      console.warn('Failed to load config:', error)
      config.value = { ...DEFAULT_SEARCH_DEMO_CONFIG }
    }
  }

  /**
   * 重置所有状态
   */
  const resetAll = () => {
    config.value = { ...DEFAULT_SEARCH_DEMO_CONFIG }
    searchState.value = {
      loading: false,
      error: null,
      query: '',
      hasMore: false,
      page: 0,
      total: 0,
      duration: 0
    }
    results.value = []
    searchHistory.value = []
    searchCache.value.clear()
    performance.value = {
      searchCount: 0,
      averageResponseTime: 0,
      cacheHitRate: 0,
      errorRate: 0,
      lastSearchTime: 0,
      slowestSearchTime: 0,
      fastestSearchTime: Number.MAX_SAFE_INTEGER,
      successCount: 0,
      errorCount: 0
    }
    parameterChanges.value = []

    // 清理本地存储
    localStorage.removeItem('mobile-search-demo-config')
    localStorage.removeItem('mobile-search-demo-history')
  }

  // ==================== 生命周期 ====================

  // 监听配置变化并持久化
  watch(config, persistConfig, { deep: true })

  // 初始化时加载数据
  loadConfig()
  loadSearchHistory()

  // ==================== 返回 ====================

  return {
    // 状态
    config,
    searchState,
    results,
    availableSpaces,
    searchHistory,
    searchCache,
    performance,
    syncStatus,
    parameterChanges,

    // Getters
    selectedSpaces,
    hasResults,
    isSearching,
    hasError,
    cacheHitRate,
    successRate,

    // Actions
    updateConfig,
    setSearchSpaces,
    selectSearchSpaces,
    setSearchState,
    setResults,
    clearResults,
    addToHistory,
    clearHistory,
    cacheResults,
    getCachedResults,
    clearCache,
    updatePerformanceMetrics,
    resetConfig,
    resetAll,
    triggerSync,
    loadConfig,
    loadSearchHistory,

    // 搜索控制
    cancelCurrentSearch,
    startSearchRequest,
    completeSearchRequest,
    currentAbortController,
    searchRequestQueue
  }
})