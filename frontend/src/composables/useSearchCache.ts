import { ref, computed, watch, onUnmounted } from 'vue'
import type { Ref } from 'vue'
import type { SearchCacheEntry, SearchResult, SearchDemoConfig } from '@/types/demo'
import { LRUCache, generateCacheKey, isExpired } from '@/utils/searchOptimization'

// 缓存清理策略类型
export type EvictionPolicy = 'lru' | 'lfu' | 'ttl'

export interface UseSearchCacheOptions {
  /** 最大缓存大小 */
  maxSize?: number
  /** 默认TTL（毫秒） */
  defaultTTL?: number
  /** 缓存清理策略 */
  evictionPolicy?: EvictionPolicy
  /** 是否启用持久化到sessionStorage */
  enablePersistence?: boolean
  /** 持久化键前缀 */
  persistenceKeyPrefix?: string
}

export interface UseSearchCacheReturn {
  // 缓存操作
  get: (key: string) => SearchCacheEntry | null
  set: (key: string, entry: Omit<SearchCacheEntry, 'timestamp' | 'expiresAt'>) => void
  delete: (key: string) => boolean
  clear: () => void
  has: (key: string) => boolean

  // 缓存管理
  size: Readonly<Ref<number>>
  maxSize: Readonly<Ref<number>>
  hitRate: Readonly<Ref<number>>
  memory: Readonly<Ref<number>>

  // 缓存策略
  setEvictionPolicy: (policy: EvictionPolicy) => void
  setTTL: (ttl: number) => void
  setMaxSize: (size: number) => void

  // 缓存优化
  preload: (queries: Array<{ query: string; config: SearchDemoConfig }>) => Promise<void>
  invalidate: (pattern: RegExp) => number
  compact: () => number

  // 统计信息
  getStats: () => {
    totalRequests: number
    cacheHits: number
    cacheMisses: number
    hitRate: number
    averageAccessTime: number
    memoryUsage: number
    oldestEntry: number | null
    newestEntry: number | null
  }

  // 生命周期
  cleanup: () => void
}

const DEFAULT_OPTIONS: Required<UseSearchCacheOptions> = {
  maxSize: 100,
  defaultTTL: 5 * 60 * 1000, // 5分钟
  evictionPolicy: 'lru',
  enablePersistence: true,
  persistenceKeyPrefix: 'search-cache'
}

/**
 * 搜索缓存管理 Composable
 */
export function useSearchCache(options: UseSearchCacheOptions = {}): UseSearchCacheReturn {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  // ==================== 状态定义 ====================

  /** 缓存存储 */
  const cache = ref(new LRUCache<string, SearchCacheEntry>(opts.maxSize))

  /** 缓存配置 */
  const maxSizeRef = ref(opts.maxSize)
  const defaultTTL = ref(opts.defaultTTL)
  const evictionPolicy = ref<EvictionPolicy>(opts.evictionPolicy)

  /** 统计信息 */
  const stats = ref({
    totalRequests: 0,
    cacheHits: 0,
    cacheMisses: 0,
    lastAccessTimes: new Map<string, number>(),
    memoryUsage: 0
  })

  /** 清理定时器 */
  let cleanupTimer: number | null = null

  // ==================== 计算属性 ====================

  /** 当前缓存大小 */
  const size = computed(() => cache.value.size())

  /** 缓存命中率 */
  const hitRate = computed(() => {
    const total = stats.value.totalRequests
    return total > 0 ? stats.value.cacheHits / total : 0
  })

  /** 估算内存占用 */
  const memory = computed(() => stats.value.memoryUsage)

  // ==================== 核心方法 ====================

  /**
   * 获取缓存项
   */
  const get = (key: string): SearchCacheEntry | null => {
    stats.value.totalRequests++
    stats.value.lastAccessTimes.set(key, Date.now())

    const entry = cache.value.get(key)
    if (!entry) {
      stats.value.cacheMisses++
      return null
    }

    // 检查是否过期
    if (isExpired(entry)) {
      cache.value.delete(key)
      stats.value.cacheMisses++
      updateMemoryUsage()
      return null
    }

    // 更新命中统计
    stats.value.cacheHits++
    entry.hitCount++

    return entry
  }

  /**
   * 设置缓存项
   */
  const set = (key: string, entry: Omit<SearchCacheEntry, 'timestamp' | 'expiresAt'>): void => {
    const now = Date.now()
    const fullEntry: SearchCacheEntry = {
      ...entry,
      timestamp: now,
      expiresAt: now + defaultTTL.value,
      hitCount: entry.hitCount || 0
    }

    // 根据清理策略处理缓存满的情况
    if (cache.value.size() >= maxSizeRef.value) {
      performEviction()
    }

    cache.value.set(key, fullEntry)
    stats.value.lastAccessTimes.set(key, now)
    updateMemoryUsage()

    // 持久化到 sessionStorage
    if (opts.enablePersistence) {
      persistToStorage(key, fullEntry)
    }
  }

  /**
   * 删除缓存项
   */
  const deleteEntry = (key: string): boolean => {
    const deleted = cache.value.delete(key)
    if (deleted) {
      stats.value.lastAccessTimes.delete(key)
      updateMemoryUsage()
      removeFromStorage(key)
    }
    return deleted
  }

  /**
   * 清空所有缓存
   */
  const clear = (): void => {
    cache.value.clear()
    stats.value.lastAccessTimes.clear()
    stats.value.memoryUsage = 0
    clearStorage()
  }

  /**
   * 检查缓存项是否存在
   */
  const has = (key: string): boolean => {
    const entry = cache.value.get(key)
    if (!entry) return false

    if (isExpired(entry)) {
      cache.value.delete(key)
      updateMemoryUsage()
      return false
    }

    return true
  }

  // ==================== 缓存策略管理 ====================

  /**
   * 设置清理策略
   */
  const setEvictionPolicy = (policy: EvictionPolicy): void => {
    evictionPolicy.value = policy
  }

  /**
   * 设置默认TTL
   */
  const setTTL = (ttl: number): void => {
    defaultTTL.value = ttl
  }

  /**
   * 设置最大缓存大小
   */
  const setMaxSize = (newMaxSize: number): void => {
    maxSizeRef.value = newMaxSize

    // 如果当前大小超过新的最大大小，执行清理
    while (cache.value.size() > newMaxSize) {
      performEviction()
    }
  }

  /**
   * 执行缓存清理
   */
  const performEviction = (): void => {
    if (cache.value.size() === 0) return

    const keys = Array.from(cache.value.keys())
    let keyToRemove: string

    switch (evictionPolicy.value) {
      case 'lru':
        // LRU: 删除最久未访问的项
        keyToRemove = keys.reduce((oldest, key) => {
          const oldestTime = stats.value.lastAccessTimes.get(oldest) || 0
          const keyTime = stats.value.lastAccessTimes.get(key) || 0
          return keyTime < oldestTime ? key : oldest
        })
        break

      case 'lfu':
        // LFU: 删除访问次数最少的项
        keyToRemove = keys.reduce((lfu, key) => {
          const lfuEntry = cache.value.get(lfu)
          const keyEntry = cache.value.get(key)
          const lfuHits = lfuEntry?.hitCount || 0
          const keyHits = keyEntry?.hitCount || 0
          return keyHits < lfuHits ? key : lfu
        })
        break

      case 'ttl':
        // TTL: 删除最先过期的项
        keyToRemove = keys.reduce((earliest, key) => {
          const earliestEntry = cache.value.get(earliest)
          const keyEntry = cache.value.get(key)
          const earliestExpires = earliestEntry?.expiresAt || Infinity
          const keyExpires = keyEntry?.expiresAt || Infinity
          return keyExpires < earliestExpires ? key : earliest
        })
        break

      default:
        keyToRemove = keys[0]
    }

    cache.value.delete(keyToRemove)
    stats.value.lastAccessTimes.delete(keyToRemove)
    removeFromStorage(keyToRemove)
    updateMemoryUsage()
  }

  // ==================== 高级功能 ====================

  /**
   * 预加载搜索结果
   */
  const preload = async (queries: Array<{ query: string; config: SearchDemoConfig }>): Promise<void> => {
    const preloadPromises = queries.map(async ({ query, config }) => {
      const key = generateCacheKey(query, config)
      if (!has(key)) {
        // 这里可以调用实际的搜索API进行预加载
        // 目前只是占位符实现
        console.log(`Preloading: ${query}`)
      }
    })

    await Promise.all(preloadPromises)
  }

  /**
   * 根据模式批量失效缓存
   */
  const invalidate = (pattern: RegExp): number => {
    const keys = Array.from(cache.value.keys())
    let invalidatedCount = 0

    keys.forEach(key => {
      if (pattern.test(key)) {
        cache.value.delete(key)
        stats.value.lastAccessTimes.delete(key)
        removeFromStorage(key)
        invalidatedCount++
      }
    })

    updateMemoryUsage()
    return invalidatedCount
  }

  /**
   * 压缩缓存（删除过期项）
   */
  const compact = (): number => {
    const keys = Array.from(cache.value.keys())
    let removedCount = 0

    keys.forEach(key => {
      const entry = cache.value.get(key)
      if (entry && isExpired(entry)) {
        cache.value.delete(key)
        stats.value.lastAccessTimes.delete(key)
        removeFromStorage(key)
        removedCount++
      }
    })

    updateMemoryUsage()
    return removedCount
  }

  // ==================== 统计信息 ====================

  /**
   * 获取缓存统计信息
   */
  const getStats = () => {
    const entries = Array.from(cache.value.keys()).map(key => cache.value.get(key)!)
    const timestamps = entries.map(entry => entry.timestamp)

    return {
      totalRequests: stats.value.totalRequests,
      cacheHits: stats.value.cacheHits,
      cacheMisses: stats.value.cacheMisses,
      hitRate: hitRate.value,
      averageAccessTime: calculateAverageAccessTime(),
      memoryUsage: stats.value.memoryUsage,
      oldestEntry: timestamps.length > 0 ? Math.min(...timestamps) : null,
      newestEntry: timestamps.length > 0 ? Math.max(...timestamps) : null
    }
  }

  // ==================== 工具方法 ====================

  /**
   * 更新内存使用统计
   */
  const updateMemoryUsage = (): void => {
    let totalSize = 0

    for (const key of cache.value.keys()) {
      const entry = cache.value.get(key)
      if (entry) {
        // 估算条目大小（JSON字符串长度）
        const entrySize = JSON.stringify(entry).length * 2 // 假设每个字符占2字节
        totalSize += entrySize
      }
    }

    stats.value.memoryUsage = totalSize
  }

  /**
   * 计算平均访问时间
   */
  const calculateAverageAccessTime = (): number => {
    const times = Array.from(stats.value.lastAccessTimes.values())
    if (times.length === 0) return 0

    const sum = times.reduce((acc, time) => acc + time, 0)
    return sum / times.length
  }

  /**
   * 持久化到存储
   */
  const persistToStorage = (key: string, entry: SearchCacheEntry): void => {
    try {
      const storageKey = `${opts.persistenceKeyPrefix}:${key}`
      sessionStorage.setItem(storageKey, JSON.stringify(entry))
    } catch (error) {
      console.warn('Failed to persist cache entry:', error)
    }
  }

  /**
   * 从存储中移除
   */
  const removeFromStorage = (key: string): void => {
    try {
      const storageKey = `${opts.persistenceKeyPrefix}:${key}`
      sessionStorage.removeItem(storageKey)
    } catch (error) {
      console.warn('Failed to remove cache entry from storage:', error)
    }
  }

  /**
   * 清空存储
   */
  const clearStorage = (): void => {
    try {
      const keysToRemove: string[] = []
      for (let i = 0; i < sessionStorage.length; i++) {
        const key = sessionStorage.key(i)
        if (key?.startsWith(opts.persistenceKeyPrefix)) {
          keysToRemove.push(key)
        }
      }
      keysToRemove.forEach(key => sessionStorage.removeItem(key))
    } catch (error) {
      console.warn('Failed to clear cache from storage:', error)
    }
  }

  /**
   * 从存储中加载缓存
   */
  const loadFromStorage = (): void => {
    if (!opts.enablePersistence) return

    try {
      for (let i = 0; i < sessionStorage.length; i++) {
        const storageKey = sessionStorage.key(i)
        if (storageKey?.startsWith(opts.persistenceKeyPrefix)) {
          const cacheKey = storageKey.replace(`${opts.persistenceKeyPrefix}:`, '')
          const entryData = sessionStorage.getItem(storageKey)

          if (entryData) {
            const entry: SearchCacheEntry = JSON.parse(entryData)

            // 检查是否过期
            if (!isExpired(entry)) {
              cache.value.set(cacheKey, entry)
              stats.value.lastAccessTimes.set(cacheKey, entry.timestamp)
            } else {
              // 清理过期的存储项
              sessionStorage.removeItem(storageKey)
            }
          }
        }
      }

      updateMemoryUsage()
    } catch (error) {
      console.warn('Failed to load cache from storage:', error)
    }
  }

  /**
   * 定期清理过期缓存
   */
  const startPeriodicCleanup = (): void => {
    const cleanupInterval = Math.max(defaultTTL.value / 4, 60000) // 最小1分钟

    cleanupTimer = window.setInterval(() => {
      compact()
    }, cleanupInterval)
  }

  /**
   * 停止定期清理
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
    clear()
  }

  // ==================== 生命周期 ====================

  // 监听最大大小变化
  watch(maxSizeRef, (newSize) => {
    if (cache.value.size() > newSize) {
      while (cache.value.size() > newSize) {
        performEviction()
      }
    }
  })

  // 初始化
  loadFromStorage()
  startPeriodicCleanup()

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })

  // ==================== 返回接口 ====================

  return {
    // 缓存操作
    get,
    set,
    delete: deleteEntry,
    clear,
    has,

    // 缓存管理
    size,
    maxSize: maxSizeRef,
    hitRate,
    memory,

    // 缓存策略
    setEvictionPolicy,
    setTTL,
    setMaxSize,

    // 缓存优化
    preload,
    invalidate,
    compact,

    // 统计信息
    getStats,

    // 生命周期
    cleanup
  }
}