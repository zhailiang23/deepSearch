import { ref, computed, onUnmounted, watch } from 'vue'
import type { Ref, ComputedRef } from 'vue'
import { useStorage } from '@vueuse/core'
import type { SearchPerformanceMetrics } from '@/types/demo'
import { PerformanceTimer, SearchStatsCollector } from '@/utils/searchOptimization'

export interface UseSearchPerformanceOptions {
  /** 是否启用性能监控 */
  enabled?: boolean
  /** 是否持久化性能数据 */
  enablePersistence?: boolean
  /** 持久化键名 */
  persistenceKey?: string
  /** 性能数据保留天数 */
  retentionDays?: number
  /** 是否收集详细的时间指标 */
  collectDetailedMetrics?: boolean
  /** 性能警告阈值 */
  warningThresholds?: {
    responseTime?: number
    errorRate?: number
    memoryUsage?: number
  }
}

export interface PerformanceAlert {
  type: 'response_time' | 'error_rate' | 'memory_usage' | 'cache_miss'
  severity: 'warning' | 'error' | 'critical'
  message: string
  value: number
  threshold: number
  timestamp: number
}

export interface DetailedPerformanceData {
  searchTimes: Array<{
    query: string
    duration: number
    timestamp: number
    cached: boolean
    resultCount: number
  }>
  errorEvents: Array<{
    query: string
    error: string
    timestamp: number
  }>
  cacheEvents: Array<{
    query: string
    hit: boolean
    timestamp: number
  }>
  memorySnapshots: Array<{
    usage: number
    timestamp: number
  }>
}

export interface UseSearchPerformanceReturn {
  // ==================== 核心性能指标 ====================

  /** 基础性能指标 */
  metrics: Readonly<Ref<SearchPerformanceMetrics>>
  /** 详细性能数据 */
  detailedData: Readonly<Ref<DetailedPerformanceData>>
  /** 性能警告 */
  alerts: Readonly<Ref<PerformanceAlert[]>>

  // ==================== 计算属性 ====================

  /** 当前性能状态 */
  performanceStatus: ComputedRef<'excellent' | 'good' | 'fair' | 'poor'>
  /** 平均响应时间趋势 */
  responseTimeTrend: ComputedRef<'improving' | 'stable' | 'declining'>
  /** 缓存效率 */
  cacheEfficiency: ComputedRef<'high' | 'medium' | 'low'>
  /** 内存使用状态 */
  memoryStatus: ComputedRef<'normal' | 'high' | 'critical'>

  // ==================== 性能监控方法 ====================

  /** 开始性能计时 */
  startTimer: (action: string, metadata?: Record<string, any>) => () => void
  /** 记录搜索开始 */
  recordSearchStart: (query: string, config?: any) => string
  /** 记录搜索成功 */
  recordSearchSuccess: (searchId: string, duration: number, resultCount: number, cached?: boolean) => void
  /** 记录搜索失败 */
  recordSearchError: (searchId: string, error: string, duration?: number) => void
  /** 记录缓存命中 */
  recordCacheHit: (query: string, size?: number) => void
  /** 记录缓存未命中 */
  recordCacheMiss: (query: string) => void
  /** 记录内存使用情况 */
  recordMemoryUsage: (usage: number) => void

  // ==================== 性能分析方法 ====================

  /** 获取性能报告 */
  getPerformanceReport: (timeRange?: { start: number; end: number }) => {
    summary: SearchPerformanceMetrics
    trends: {
      responseTime: Array<{ timestamp: number; value: number }>
      errorRate: Array<{ timestamp: number; value: number }>
      cacheHitRate: Array<{ timestamp: number; value: number }>
    }
    recommendations: string[]
  }

  /** 获取慢查询分析 */
  getSlowQueries: (threshold?: number, limit?: number) => Array<{
    query: string
    duration: number
    timestamp: number
    frequency: number
  }>

  /** 获取热门查询性能 */
  getPopularQueriesPerformance: (limit?: number) => Array<{
    query: string
    count: number
    avgDuration: number
    cacheHitRate: number
  }>

  /** 检测性能异常 */
  detectAnomalies: () => Array<{
    type: 'spike' | 'drop' | 'trend'
    metric: 'response_time' | 'error_rate' | 'cache_hit_rate'
    severity: 'low' | 'medium' | 'high'
    description: string
    timestamp: number
  }>

  // ==================== 性能优化建议 ====================

  /** 获取性能优化建议 */
  getOptimizationSuggestions: () => Array<{
    category: 'caching' | 'query' | 'config' | 'system'
    priority: 'high' | 'medium' | 'low'
    suggestion: string
    impact: 'high' | 'medium' | 'low'
    effort: 'low' | 'medium' | 'high'
  }>

  // ==================== 数据管理 ====================

  /** 重置性能数据 */
  resetMetrics: () => void
  /** 清理过期数据 */
  cleanupExpiredData: () => number
  /** 导出性能数据 */
  exportData: () => string
  /** 导入性能数据 */
  importData: (data: string) => boolean

  // ==================== 配置管理 ====================

  /** 设置警告阈值 */
  setWarningThresholds: (thresholds: UseSearchPerformanceOptions['warningThresholds']) => void
  /** 启用/禁用监控 */
  setEnabled: (enabled: boolean) => void
  /** 清理资源 */
  cleanup: () => void
}

const DEFAULT_OPTIONS: Required<UseSearchPerformanceOptions> = {
  enabled: true,
  enablePersistence: true,
  persistenceKey: 'search-performance-data',
  retentionDays: 7,
  collectDetailedMetrics: true,
  warningThresholds: {
    responseTime: 2000, // 2秒
    errorRate: 0.05, // 5%
    memoryUsage: 50 * 1024 * 1024 // 50MB
  }
}

/**
 * 搜索性能监控 Composable
 * 提供全面的性能监控、分析和优化建议功能
 */
export function useSearchPerformance(
  options: UseSearchPerformanceOptions = {}
): UseSearchPerformanceReturn {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  // ==================== 工具实例 ====================

  const performanceTimer = new PerformanceTimer()
  const statsCollector = new SearchStatsCollector()

  // ==================== 状态定义 ====================

  /** 是否启用监控 */
  const enabled = ref(opts.enabled)

  /** 基础性能指标 */
  const metrics = ref<SearchPerformanceMetrics>({
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

  /** 详细性能数据 */
  const detailedData = ref<DetailedPerformanceData>({
    searchTimes: [],
    errorEvents: [],
    cacheEvents: [],
    memorySnapshots: []
  })

  /** 性能警告 */
  const alerts = ref<PerformanceAlert[]>([])

  /** 警告阈值 */
  const warningThresholds = ref(opts.warningThresholds)

  /** 搜索会话映射 */
  const searchSessions = ref<Map<string, {
    query: string
    startTime: number
    config?: any
  }>>(new Map())

  /** 持久化存储 */
  const persistedData = useStorage(opts.persistenceKey, {
    metrics: metrics.value,
    detailedData: detailedData.value,
    alerts: alerts.value
  })

  // ==================== 计算属性 ====================

  /** 当前性能状态 */
  const performanceStatus = computed<'excellent' | 'good' | 'fair' | 'poor'>(() => {
    const avgTime = metrics.value.averageResponseTime
    const errorRate = metrics.value.errorRate

    if (avgTime <= 500 && errorRate <= 0.01) return 'excellent'
    if (avgTime <= 1000 && errorRate <= 0.03) return 'good'
    if (avgTime <= 2000 && errorRate <= 0.05) return 'fair'
    return 'poor'
  })

  /** 响应时间趋势 */
  const responseTimeTrend = computed<'improving' | 'stable' | 'declining'>(() => {
    const recentSearches = detailedData.value.searchTimes.slice(-20)
    if (recentSearches.length < 10) return 'stable'

    const firstHalf = recentSearches.slice(0, 10)
    const secondHalf = recentSearches.slice(-10)

    const firstAvg = firstHalf.reduce((sum, item) => sum + item.duration, 0) / firstHalf.length
    const secondAvg = secondHalf.reduce((sum, item) => sum + item.duration, 0) / secondHalf.length

    const improvement = (firstAvg - secondAvg) / firstAvg

    if (improvement > 0.1) return 'improving'
    if (improvement < -0.1) return 'declining'
    return 'stable'
  })

  /** 缓存效率 */
  const cacheEfficiency = computed<'high' | 'medium' | 'low'>(() => {
    const hitRate = metrics.value.cacheHitRate
    if (hitRate >= 0.7) return 'high'
    if (hitRate >= 0.4) return 'medium'
    return 'low'
  })

  /** 内存使用状态 */
  const memoryStatus = computed<'normal' | 'high' | 'critical'>(() => {
    const recentSnapshot = detailedData.value.memorySnapshots.slice(-1)[0]
    if (!recentSnapshot) return 'normal'

    const usage = recentSnapshot.usage
    const threshold = warningThresholds.value.memoryUsage || 50 * 1024 * 1024

    if (usage >= threshold * 1.5) return 'critical'
    if (usage >= threshold) return 'high'
    return 'normal'
  })

  // ==================== 性能监控方法 ====================

  /**
   * 开始性能计时
   */
  const startTimer = (action: string, metadata?: Record<string, any>) => {
    if (!enabled.value) return () => {}

    const timerId = `${action}-${Date.now()}-${Math.random()}`
    performanceTimer.start(timerId)

    return () => {
      const duration = performanceTimer.end(timerId)

      // 记录到详细数据（如果启用）
      if (opts.collectDetailedMetrics) {
        // 这里可以根据action类型记录不同的详细数据
        console.log(`Performance: ${action} completed in ${duration}ms`, metadata)
      }

      return duration
    }
  }

  /**
   * 记录搜索开始
   */
  const recordSearchStart = (query: string, config?: any): string => {
    if (!enabled.value) return ''

    const searchId = `search-${Date.now()}-${Math.random()}`
    searchSessions.value.set(searchId, {
      query,
      startTime: Date.now(),
      config
    })

    // 使用统计收集器记录
    statsCollector.recordSearchStart(query)

    return searchId
  }

  /**
   * 记录搜索成功
   */
  const recordSearchSuccess = (
    searchId: string,
    duration: number,
    resultCount: number,
    cached = false
  ): void => {
    if (!enabled.value) return

    const session = searchSessions.value.get(searchId)
    if (!session) return

    // 更新基础指标
    metrics.value.searchCount++
    metrics.value.successCount++
    metrics.value.lastSearchTime = duration

    // 更新响应时间统计
    const totalTime = metrics.value.averageResponseTime * (metrics.value.successCount - 1) + duration
    metrics.value.averageResponseTime = totalTime / metrics.value.successCount

    // 更新最快/最慢时间
    if (duration > metrics.value.slowestSearchTime) {
      metrics.value.slowestSearchTime = duration
    }
    if (duration < metrics.value.fastestSearchTime) {
      metrics.value.fastestSearchTime = duration
    }

    // 更新错误率
    metrics.value.errorRate = metrics.value.errorCount / metrics.value.searchCount

    // 记录详细数据
    if (opts.collectDetailedMetrics) {
      detailedData.value.searchTimes.push({
        query: session.query,
        duration,
        timestamp: Date.now(),
        cached,
        resultCount
      })

      // 限制详细数据大小
      if (detailedData.value.searchTimes.length > 1000) {
        detailedData.value.searchTimes = detailedData.value.searchTimes.slice(-500)
      }
    }

    // 使用统计收集器记录
    statsCollector.recordSearchSuccess(duration)

    // 检查性能警告
    checkPerformanceWarnings(duration, 'response_time')

    // 清理会话
    searchSessions.value.delete(searchId)
  }

  /**
   * 记录搜索失败
   */
  const recordSearchError = (searchId: string, error: string, duration = 0): void => {
    if (!enabled.value) return

    const session = searchSessions.value.get(searchId)
    if (!session) return

    // 更新基础指标
    metrics.value.searchCount++
    metrics.value.errorCount++
    metrics.value.errorRate = metrics.value.errorCount / metrics.value.searchCount

    // 记录详细数据
    if (opts.collectDetailedMetrics) {
      detailedData.value.errorEvents.push({
        query: session.query,
        error,
        timestamp: Date.now()
      })

      // 限制详细数据大小
      if (detailedData.value.errorEvents.length > 500) {
        detailedData.value.errorEvents = detailedData.value.errorEvents.slice(-250)
      }
    }

    // 使用统计收集器记录
    statsCollector.recordSearchFailure(error)

    // 检查错误率警告
    checkPerformanceWarnings(metrics.value.errorRate, 'error_rate')

    // 清理会话
    searchSessions.value.delete(searchId)
  }

  /**
   * 记录缓存命中
   */
  const recordCacheHit = (query: string, size?: number): void => {
    if (!enabled.value) return

    // 更新缓存命中率
    const newHitRate = (metrics.value.cacheHitRate * metrics.value.searchCount + 1) / (metrics.value.searchCount + 1)
    metrics.value.cacheHitRate = Math.min(newHitRate, 1)

    // 记录详细数据
    if (opts.collectDetailedMetrics) {
      detailedData.value.cacheEvents.push({
        query,
        hit: true,
        timestamp: Date.now()
      })

      // 限制详细数据大小
      if (detailedData.value.cacheEvents.length > 500) {
        detailedData.value.cacheEvents = detailedData.value.cacheEvents.slice(-250)
      }
    }

    // 使用统计收集器记录
    statsCollector.recordCacheHit()
  }

  /**
   * 记录缓存未命中
   */
  const recordCacheMiss = (query: string): void => {
    if (!enabled.value) return

    // 记录详细数据
    if (opts.collectDetailedMetrics) {
      detailedData.value.cacheEvents.push({
        query,
        hit: false,
        timestamp: Date.now()
      })
    }
  }

  /**
   * 记录内存使用情况
   */
  const recordMemoryUsage = (usage: number): void => {
    if (!enabled.value) return

    // 记录详细数据
    if (opts.collectDetailedMetrics) {
      detailedData.value.memorySnapshots.push({
        usage,
        timestamp: Date.now()
      })

      // 限制详细数据大小（只保留最近的100个快照）
      if (detailedData.value.memorySnapshots.length > 100) {
        detailedData.value.memorySnapshots = detailedData.value.memorySnapshots.slice(-50)
      }
    }

    // 检查内存使用警告
    checkPerformanceWarnings(usage, 'memory_usage')
  }

  // ==================== 性能分析方法 ====================

  /**
   * 获取性能报告
   */
  const getPerformanceReport = (timeRange?: { start: number; end: number }) => {
    const now = Date.now()
    const start = timeRange?.start || (now - 24 * 60 * 60 * 1000) // 默认24小时
    const end = timeRange?.end || now

    // 过滤时间范围内的数据
    const filteredSearches = detailedData.value.searchTimes.filter(
      s => s.timestamp >= start && s.timestamp <= end
    )

    // 生成趋势数据
    const responseTimeTrend = generateTrend(filteredSearches, 'duration')
    const errorEvents = detailedData.value.errorEvents.filter(
      e => e.timestamp >= start && e.timestamp <= end
    )

    const recommendations = generateRecommendations()

    return {
      summary: { ...metrics.value },
      trends: {
        responseTime: responseTimeTrend,
        errorRate: generateErrorRateTrend(errorEvents, start, end),
        cacheHitRate: generateCacheHitRateTrend(start, end)
      },
      recommendations
    }
  }

  /**
   * 获取慢查询分析
   */
  const getSlowQueries = (threshold = 2000, limit = 10) => {
    const slowQueries = detailedData.value.searchTimes
      .filter(s => s.duration >= threshold)
      .sort((a, b) => b.duration - a.duration)
      .slice(0, limit)

    // 计算频率
    const queryFrequency = new Map<string, number>()
    detailedData.value.searchTimes.forEach(s => {
      const count = queryFrequency.get(s.query) || 0
      queryFrequency.set(s.query, count + 1)
    })

    return slowQueries.map(q => ({
      query: q.query,
      duration: q.duration,
      timestamp: q.timestamp,
      frequency: queryFrequency.get(q.query) || 1
    }))
  }

  /**
   * 获取热门查询性能
   */
  const getPopularQueriesPerformance = (limit = 10) => {
    const queryStats = new Map<string, {
      count: number
      totalDuration: number
      cacheHits: number
    }>()

    // 统计搜索数据
    detailedData.value.searchTimes.forEach(s => {
      const stats = queryStats.get(s.query) || { count: 0, totalDuration: 0, cacheHits: 0 }
      stats.count++
      stats.totalDuration += s.duration
      if (s.cached) stats.cacheHits++
      queryStats.set(s.query, stats)
    })

    // 转换并排序
    return Array.from(queryStats.entries())
      .map(([query, stats]) => ({
        query,
        count: stats.count,
        avgDuration: stats.totalDuration / stats.count,
        cacheHitRate: stats.cacheHits / stats.count
      }))
      .sort((a, b) => b.count - a.count)
      .slice(0, limit)
  }

  /**
   * 检测性能异常
   */
  const detectAnomalies = () => {
    const anomalies: Array<{
      type: 'spike' | 'drop' | 'trend'
      metric: 'response_time' | 'error_rate' | 'cache_hit_rate'
      severity: 'low' | 'medium' | 'high'
      description: string
      timestamp: number
    }> = []

    // 响应时间尖峰检测
    const recentSearches = detailedData.value.searchTimes.slice(-50)
    if (recentSearches.length >= 10) {
      const avgDuration = recentSearches.reduce((sum, s) => sum + s.duration, 0) / recentSearches.length
      const spikes = recentSearches.filter(s => s.duration > avgDuration * 2)

      if (spikes.length > recentSearches.length * 0.1) {
        anomalies.push({
          type: 'spike',
          metric: 'response_time',
          severity: spikes.length > recentSearches.length * 0.2 ? 'high' : 'medium',
          description: `检测到响应时间异常峰值，${spikes.length}个请求超过平均响应时间的2倍`,
          timestamp: Date.now()
        })
      }
    }

    // 错误率趋势检测
    const recentErrors = detailedData.value.errorEvents.slice(-20)
    if (recentErrors.length > 5) {
      anomalies.push({
        type: 'trend',
        metric: 'error_rate',
        severity: 'medium',
        description: `错误率呈上升趋势，最近发生了${recentErrors.length}个错误`,
        timestamp: Date.now()
      })
    }

    return anomalies
  }

  // ==================== 性能优化建议 ====================

  /**
   * 获取性能优化建议
   */
  const getOptimizationSuggestions = () => {
    const suggestions: Array<{
      category: 'caching' | 'query' | 'config' | 'system'
      priority: 'high' | 'medium' | 'low'
      suggestion: string
      impact: 'high' | 'medium' | 'low'
      effort: 'low' | 'medium' | 'high'
    }> = []

    // 缓存优化建议
    if (metrics.value.cacheHitRate < 0.5) {
      suggestions.push({
        category: 'caching',
        priority: 'high',
        suggestion: '缓存命中率较低，建议增加缓存大小或优化缓存策略',
        impact: 'high',
        effort: 'medium'
      })
    }

    // 响应时间优化建议
    if (metrics.value.averageResponseTime > 1000) {
      suggestions.push({
        category: 'query',
        priority: 'high',
        suggestion: '平均响应时间较长，建议优化查询逻辑或增加索引',
        impact: 'high',
        effort: 'high'
      })
    }

    // 错误率优化建议
    if (metrics.value.errorRate > 0.03) {
      suggestions.push({
        category: 'system',
        priority: 'high',
        suggestion: '错误率较高，建议检查系统稳定性和错误处理机制',
        impact: 'high',
        effort: 'medium'
      })
    }

    // 慢查询优化建议
    const slowQueries = getSlowQueries(2000, 5)
    if (slowQueries.length > 0) {
      suggestions.push({
        category: 'query',
        priority: 'medium',
        suggestion: `发现${slowQueries.length}个慢查询，建议针对热门慢查询进行优化`,
        impact: 'medium',
        effort: 'medium'
      })
    }

    return suggestions.sort((a, b) => {
      const priorityOrder = { high: 3, medium: 2, low: 1 }
      return priorityOrder[b.priority] - priorityOrder[a.priority]
    })
  }

  // ==================== 工具方法 ====================

  /**
   * 检查性能警告
   */
  const checkPerformanceWarnings = (value: number, metric: keyof NonNullable<UseSearchPerformanceOptions['warningThresholds']>): void => {
    const threshold = warningThresholds.value?.[metric]
    if (!threshold) return

    let shouldAlert = false
    let severity: PerformanceAlert['severity'] = 'warning'
    let alertType: PerformanceAlert['type'] = 'response_time'

    switch (metric) {
      case 'responseTime':
        alertType = 'response_time'
        shouldAlert = value > threshold
        severity = value > threshold * 1.5 ? 'error' : 'warning'
        break
      case 'errorRate':
        alertType = 'error_rate'
        shouldAlert = value > threshold
        severity = value > threshold * 2 ? 'error' : 'warning'
        break
      case 'memoryUsage':
        alertType = 'memory_usage'
        shouldAlert = value > threshold
        severity = value > threshold * 1.5 ? 'critical' : 'warning'
        break
    }

    if (shouldAlert) {
      const alert: PerformanceAlert = {
        type: alertType,
        severity,
        message: `${metric} (${value}) 超过了警告阈值 (${threshold})`,
        value,
        threshold,
        timestamp: Date.now()
      }

      alerts.value.push(alert)

      // 限制警告数量
      if (alerts.value.length > 50) {
        alerts.value = alerts.value.slice(-25)
      }
    }
  }

  /**
   * 生成趋势数据
   */
  const generateTrend = (data: Array<{ duration: number; timestamp: number }>, field: 'duration') => {
    return data.map(item => ({
      timestamp: item.timestamp,
      value: item[field]
    }))
  }

  /**
   * 生成错误率趋势
   */
  const generateErrorRateTrend = (errorEvents: Array<{ timestamp: number }>, start: number, end: number) => {
    // 按小时分组计算错误率
    const hourlyData = new Map<number, { errors: number; total: number }>()

    // 这里需要更复杂的逻辑来计算每小时的总请求数和错误数
    // 简化实现
    return errorEvents.map(e => ({
      timestamp: e.timestamp,
      value: metrics.value.errorRate
    }))
  }

  /**
   * 生成缓存命中率趋势
   */
  const generateCacheHitRateTrend = (start: number, end: number) => {
    const cacheEvents = detailedData.value.cacheEvents.filter(
      e => e.timestamp >= start && e.timestamp <= end
    )

    // 按时间窗口计算命中率
    return cacheEvents.map(e => ({
      timestamp: e.timestamp,
      value: metrics.value.cacheHitRate
    }))
  }

  /**
   * 生成优化建议
   */
  const generateRecommendations = (): string[] => {
    const recommendations: string[] = []

    if (metrics.value.averageResponseTime > 1000) {
      recommendations.push('考虑优化查询算法或增加服务器资源')
    }

    if (metrics.value.cacheHitRate < 0.5) {
      recommendations.push('增加缓存大小或优化缓存策略')
    }

    if (metrics.value.errorRate > 0.05) {
      recommendations.push('检查并修复导致错误的问题')
    }

    return recommendations
  }

  // ==================== 数据管理方法 ====================

  /**
   * 重置性能数据
   */
  const resetMetrics = (): void => {
    metrics.value = {
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

    detailedData.value = {
      searchTimes: [],
      errorEvents: [],
      cacheEvents: [],
      memorySnapshots: []
    }

    alerts.value = []
    statsCollector.reset()
  }

  /**
   * 清理过期数据
   */
  const cleanupExpiredData = (): number => {
    const cutoffTime = Date.now() - (opts.retentionDays * 24 * 60 * 60 * 1000)
    let removedCount = 0

    // 清理详细数据
    const originalSearchCount = detailedData.value.searchTimes.length
    detailedData.value.searchTimes = detailedData.value.searchTimes.filter(s => s.timestamp > cutoffTime)
    removedCount += originalSearchCount - detailedData.value.searchTimes.length

    const originalErrorCount = detailedData.value.errorEvents.length
    detailedData.value.errorEvents = detailedData.value.errorEvents.filter(e => e.timestamp > cutoffTime)
    removedCount += originalErrorCount - detailedData.value.errorEvents.length

    const originalCacheCount = detailedData.value.cacheEvents.length
    detailedData.value.cacheEvents = detailedData.value.cacheEvents.filter(c => c.timestamp > cutoffTime)
    removedCount += originalCacheCount - detailedData.value.cacheEvents.length

    const originalMemoryCount = detailedData.value.memorySnapshots.length
    detailedData.value.memorySnapshots = detailedData.value.memorySnapshots.filter(m => m.timestamp > cutoffTime)
    removedCount += originalMemoryCount - detailedData.value.memorySnapshots.length

    // 清理警告
    const originalAlertCount = alerts.value.length
    alerts.value = alerts.value.filter(a => a.timestamp > cutoffTime)
    removedCount += originalAlertCount - alerts.value.length

    return removedCount
  }

  /**
   * 导出性能数据
   */
  const exportData = (): string => {
    const exportData = {
      version: '1.0',
      timestamp: Date.now(),
      metrics: metrics.value,
      detailedData: detailedData.value,
      alerts: alerts.value,
      warningThresholds: warningThresholds.value
    }
    return JSON.stringify(exportData, null, 2)
  }

  /**
   * 导入性能数据
   */
  const importData = (data: string): boolean => {
    try {
      const importData = JSON.parse(data)

      if (importData.metrics) {
        metrics.value = importData.metrics
      }
      if (importData.detailedData) {
        detailedData.value = importData.detailedData
      }
      if (importData.alerts) {
        alerts.value = importData.alerts
      }
      if (importData.warningThresholds) {
        warningThresholds.value = importData.warningThresholds
      }

      return true
    } catch (error) {
      console.error('Failed to import performance data:', error)
      return false
    }
  }

  // ==================== 配置管理方法 ====================

  /**
   * 设置警告阈值
   */
  const setWarningThresholds = (thresholds: UseSearchPerformanceOptions['warningThresholds']): void => {
    warningThresholds.value = { ...warningThresholds.value, ...thresholds }
  }

  /**
   * 启用/禁用监控
   */
  const setEnabled = (isEnabled: boolean): void => {
    enabled.value = isEnabled
  }

  /**
   * 清理资源
   */
  const cleanup = (): void => {
    performanceTimer.clear()
    statsCollector.reset()
    searchSessions.value.clear()
  }

  // ==================== 数据持久化 ====================

  // 监听数据变化并持久化
  if (opts.enablePersistence) {
    watch(
      [metrics, detailedData, alerts],
      () => {
        persistedData.value = {
          metrics: metrics.value,
          detailedData: detailedData.value,
          alerts: alerts.value
        }
      },
      { deep: true }
    )

    // 初始化时从持久化数据恢复
    if (persistedData.value.metrics) {
      metrics.value = persistedData.value.metrics
    }
    if (persistedData.value.detailedData) {
      detailedData.value = persistedData.value.detailedData
    }
    if (persistedData.value.alerts) {
      alerts.value = persistedData.value.alerts
    }
  }

  // ==================== 定期清理 ====================

  // 定期清理过期数据
  const cleanupInterval = setInterval(() => {
    cleanupExpiredData()
  }, 60 * 60 * 1000) // 每小时清理一次

  // ==================== 清理 ====================

  onUnmounted(() => {
    clearInterval(cleanupInterval)
    cleanup()
  })

  // ==================== 返回接口 ====================

  return {
    // 核心性能指标
    metrics,
    detailedData,
    alerts,

    // 计算属性
    performanceStatus,
    responseTimeTrend,
    cacheEfficiency,
    memoryStatus,

    // 性能监控方法
    startTimer,
    recordSearchStart,
    recordSearchSuccess,
    recordSearchError,
    recordCacheHit,
    recordCacheMiss,
    recordMemoryUsage,

    // 性能分析方法
    getPerformanceReport,
    getSlowQueries,
    getPopularQueriesPerformance,
    detectAnomalies,

    // 性能优化建议
    getOptimizationSuggestions,

    // 数据管理
    resetMetrics,
    cleanupExpiredData,
    exportData,
    importData,

    // 配置管理
    setWarningThresholds,
    setEnabled,
    cleanup
  }
}