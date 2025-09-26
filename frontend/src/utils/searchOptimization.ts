/**
 * 搜索优化工具函数
 * 提供防抖、缓存、性能监控等搜索优化功能
 */

import type { SearchDemoConfig, SearchCacheEntry, SearchResult } from '@/types/demo'

/**
 * 防抖函数类型
 */
export type DebounceFunction<T extends (...args: any[]) => any> = (
  ...args: Parameters<T>
) => Promise<ReturnType<T>>

/**
 * 创建防抖函数
 * @param fn 要防抖的函数
 * @param delay 延迟时间（毫秒）
 * @param getDelay 动态获取延迟时间的函数
 */
export function createDebounce<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  delay: number,
  getDelay?: () => number
): DebounceFunction<T> {
  let timeoutId: number | null = null
  let abortController: AbortController | null = null

  return (...args: Parameters<T>) => {
    // 取消之前的调用
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
    if (abortController) {
      abortController.abort()
    }

    return new Promise<ReturnType<T>>((resolve, reject) => {
      const currentDelay = getDelay ? getDelay() : delay

      timeoutId = window.setTimeout(async () => {
        try {
          // 创建新的 AbortController
          abortController = new AbortController()

          const result = await fn(...args)
          resolve(result)
        } catch (error) {
          // 如果是因为 abort 导致的错误，不需要 reject
          if (error instanceof Error && error.name === 'AbortError') {
            return
          }
          reject(error)
        }
      }, currentDelay)
    })
  }
}

/**
 * 智能防抖：根据查询复杂度调整延迟
 */
export function createSmartDebounce<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  baseDelay: number = 300,
  config: {
    minDelay?: number
    maxDelay?: number
    queryLengthFactor?: number
    complexityFactor?: number
  } = {}
): DebounceFunction<T> {
  const {
    minDelay = 100,
    maxDelay = 1000,
    queryLengthFactor = 0.1,
    complexityFactor = 1.2
  } = config

  const calculateDelay = (query: string): number => {
    let delay = baseDelay

    // 根据查询长度调整
    const lengthBonus = Math.min(query.length * queryLengthFactor * 10, 200)
    delay -= lengthBonus

    // 根据查询复杂度调整（包含特殊字符、多个词等）
    const complexity = calculateQueryComplexity(query)
    delay *= Math.pow(complexityFactor, complexity - 1)

    return Math.max(minDelay, Math.min(maxDelay, Math.round(delay)))
  }

  return createDebounce(fn, baseDelay, () => {
    // 从参数中提取查询字符串（假设第一个参数是查询）
    return baseDelay // 如果无法确定查询，使用基础延迟
  })
}

/**
 * 计算查询复杂度
 */
function calculateQueryComplexity(query: string): number {
  let complexity = 1

  // 包含多个单词
  const words = query.trim().split(/\s+/)
  if (words.length > 1) complexity += 0.5

  // 包含特殊字符
  const specialChars = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(query)
  if (specialChars) complexity += 0.3

  // 包含引号（短语搜索）
  const hasQuotes = /["']/.test(query)
  if (hasQuotes) complexity += 0.4

  // 包含通配符
  const hasWildcards = /[*?]/.test(query)
  if (hasWildcards) complexity += 0.6

  // 包含布尔操作符
  const hasBooleanOperators = /\b(AND|OR|NOT)\b/i.test(query)
  if (hasBooleanOperators) complexity += 0.7

  return Math.min(complexity, 3) // 最大复杂度为3
}

/**
 * 生成缓存键
 */
export function generateCacheKey(
  query: string,
  config: SearchDemoConfig,
  page = 0
): string {
  const configHash = hashConfig(config)
  return `search:${query}:${configHash}:${page}`
}

/**
 * 生成配置哈希值
 */
function hashConfig(config: SearchDemoConfig): string {
  const relevantConfig = {
    searchSpaces: config.searchSpaces.selected,
    pagination: config.pagination,
    searchBehavior: {
      minQueryLength: config.searchBehavior.minQueryLength,
      highlightMatch: config.searchBehavior.highlightMatch
    },
    pinyinSearch: config.pinyinSearch,
    resultDisplay: config.resultDisplay
  }

  return btoa(JSON.stringify(relevantConfig)).slice(0, 12)
}

/**
 * 检查缓存项是否过期
 */
export function isExpired(cacheEntry: SearchCacheEntry): boolean {
  return Date.now() > cacheEntry.expiresAt
}

/**
 * LRU 缓存实现
 */
export class LRUCache<K, V> {
  private capacity: number
  private cache = new Map<K, V>()

  constructor(capacity: number) {
    this.capacity = capacity
  }

  get(key: K): V | undefined {
    if (this.cache.has(key)) {
      // 移动到最新位置
      const value = this.cache.get(key)!
      this.cache.delete(key)
      this.cache.set(key, value)
      return value
    }
    return undefined
  }

  set(key: K, value: V): void {
    if (this.cache.has(key)) {
      // 更新现有键值对
      this.cache.delete(key)
    } else if (this.cache.size >= this.capacity) {
      // 删除最旧的项
      const firstKey = this.cache.keys().next().value
      if (firstKey !== undefined) {
        this.cache.delete(firstKey)
      }
    }
    this.cache.set(key, value)
  }

  delete(key: K): boolean {
    return this.cache.delete(key)
  }

  clear(): void {
    this.cache.clear()
  }

  size(): number {
    return this.cache.size
  }

  keys(): IterableIterator<K> {
    return this.cache.keys()
  }
}

/**
 * 搜索请求取消器
 */
export class SearchRequestCanceller {
  private requests = new Map<string, AbortController>()

  /**
   * 创建新的搜索请求
   */
  createRequest(searchId: string): AbortController {
    // 取消之前的同名请求
    this.cancel(searchId)

    const controller = new AbortController()
    this.requests.set(searchId, controller)
    return controller
  }

  /**
   * 取消指定的搜索请求
   */
  cancel(searchId: string): void {
    const controller = this.requests.get(searchId)
    if (controller) {
      controller.abort()
      this.requests.delete(searchId)
    }
  }

  /**
   * 取消所有搜索请求
   */
  cancelAll(): void {
    for (const [searchId, controller] of this.requests) {
      controller.abort()
    }
    this.requests.clear()
  }

  /**
   * 清理完成的请求
   */
  cleanup(searchId: string): void {
    this.requests.delete(searchId)
  }

  /**
   * 检查请求是否被取消
   */
  isCancelled(searchId: string): boolean {
    const controller = this.requests.get(searchId)
    return controller?.signal.aborted ?? true
  }
}

/**
 * 性能计时器
 */
export class PerformanceTimer {
  private timers = new Map<string, number>()

  /**
   * 开始计时
   */
  start(id: string): void {
    this.timers.set(id, performance.now())
  }

  /**
   * 结束计时并返回耗时
   */
  end(id: string): number {
    const startTime = this.timers.get(id)
    if (startTime === undefined) {
      console.warn(`Timer ${id} was not started`)
      return 0
    }

    const duration = performance.now() - startTime
    this.timers.delete(id)
    return Math.round(duration)
  }

  /**
   * 获取当前耗时（不结束计时）
   */
  peek(id: string): number {
    const startTime = this.timers.get(id)
    if (startTime === undefined) {
      return 0
    }
    return Math.round(performance.now() - startTime)
  }

  /**
   * 清理所有计时器
   */
  clear(): void {
    this.timers.clear()
  }
}

/**
 * 搜索结果预加载器
 */
export class SearchPreloader {
  private config: SearchDemoConfig
  private searchFn: (query: string, config: SearchDemoConfig) => Promise<any>
  private preloadCache = new LRUCache<string, Promise<any>>(20)

  constructor(
    config: SearchDemoConfig,
    searchFn: (query: string, config: SearchDemoConfig) => Promise<any>
  ) {
    this.config = config
    this.searchFn = searchFn
  }

  /**
   * 预加载搜索结果
   */
  preload(queries: string[]): void {
    queries.forEach(query => {
      const cacheKey = generateCacheKey(query, this.config)
      if (!this.preloadCache.get(cacheKey)) {
        const promise = this.searchFn(query, this.config)
        this.preloadCache.set(cacheKey, promise)
      }
    })
  }

  /**
   * 获取预加载的结果
   */
  getPreloaded(query: string): Promise<any> | undefined {
    const cacheKey = generateCacheKey(query, this.config)
    return this.preloadCache.get(cacheKey)
  }

  /**
   * 清理预加载缓存
   */
  clear(): void {
    this.preloadCache.clear()
  }
}

/**
 * 搜索建议生成器
 */
export function generateSearchSuggestions(
  query: string,
  history: string[],
  maxSuggestions = 5
): string[] {
  if (!query.trim()) return []

  const suggestions: string[] = []
  const lowerQuery = query.toLowerCase()

  // 从历史记录中匹配
  const historyMatches = history
    .filter(item =>
      item.toLowerCase().includes(lowerQuery) &&
      item.toLowerCase() !== lowerQuery
    )
    .slice(0, maxSuggestions)

  suggestions.push(...historyMatches)

  // 生成自动完成建议（简单实现）
  if (suggestions.length < maxSuggestions) {
    const autoComplete = generateAutoComplete(query)
    suggestions.push(...autoComplete.slice(0, maxSuggestions - suggestions.length))
  }

  return [...new Set(suggestions)] // 去重
}

/**
 * 简单的自动完成生成器
 */
function generateAutoComplete(query: string): string[] {
  const suggestions: string[] = []

  // 这里可以实现更复杂的自动完成逻辑
  // 比如基于词典、拼音等

  return suggestions
}

/**
 * 搜索统计收集器
 */
export class SearchStatsCollector {
  private stats = {
    totalSearches: 0,
    successfulSearches: 0,
    failedSearches: 0,
    totalResponseTime: 0,
    cacheHits: 0,
    queryLengthDistribution: new Map<number, number>(),
    popularQueries: new Map<string, number>(),
    errorTypes: new Map<string, number>()
  }

  /**
   * 记录搜索开始
   */
  recordSearchStart(query: string): void {
    this.stats.totalSearches++

    // 记录查询长度分布
    const length = query.length
    this.stats.queryLengthDistribution.set(
      length,
      (this.stats.queryLengthDistribution.get(length) || 0) + 1
    )

    // 记录热门查询
    this.stats.popularQueries.set(
      query,
      (this.stats.popularQueries.get(query) || 0) + 1
    )
  }

  /**
   * 记录搜索成功
   */
  recordSearchSuccess(responseTime: number): void {
    this.stats.successfulSearches++
    this.stats.totalResponseTime += responseTime
  }

  /**
   * 记录搜索失败
   */
  recordSearchFailure(errorType: string): void {
    this.stats.failedSearches++
    this.stats.errorTypes.set(
      errorType,
      (this.stats.errorTypes.get(errorType) || 0) + 1
    )
  }

  /**
   * 记录缓存命中
   */
  recordCacheHit(): void {
    this.stats.cacheHits++
  }

  /**
   * 获取统计信息
   */
  getStats() {
    const successRate = this.stats.totalSearches > 0
      ? this.stats.successfulSearches / this.stats.totalSearches
      : 0

    const averageResponseTime = this.stats.successfulSearches > 0
      ? this.stats.totalResponseTime / this.stats.successfulSearches
      : 0

    const cacheHitRate = this.stats.totalSearches > 0
      ? this.stats.cacheHits / this.stats.totalSearches
      : 0

    return {
      ...this.stats,
      successRate,
      averageResponseTime,
      cacheHitRate,
      popularQueries: Array.from(this.stats.popularQueries.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10),
      errorTypes: Array.from(this.stats.errorTypes.entries())
        .sort((a, b) => b[1] - a[1])
    }
  }

  /**
   * 重置统计信息
   */
  reset(): void {
    this.stats = {
      totalSearches: 0,
      successfulSearches: 0,
      failedSearches: 0,
      totalResponseTime: 0,
      cacheHits: 0,
      queryLengthDistribution: new Map(),
      popularQueries: new Map(),
      errorTypes: new Map()
    }
  }
}