/**
 * 性能监控组合函数
 * 提供性能测量、监控和分析功能
 */

import { ref, computed, onMounted, onUnmounted } from 'vue'

// ============= 类型定义 =============

/**
 * 性能测量结果
 */
export interface PerformanceMeasurement {
  /** 测量名称 */
  name: string
  /** 开始时间 */
  startTime: number
  /** 结束时间 */
  endTime: number
  /** 持续时间（毫秒） */
  duration: number
  /** 额外数据 */
  metadata?: Record<string, any>
}

/**
 * 性能统计
 */
export interface PerformanceStats {
  /** 总测量次数 */
  totalMeasurements: number
  /** 平均持续时间 */
  averageDuration: number
  /** 最小持续时间 */
  minDuration: number
  /** 最大持续时间 */
  maxDuration: number
  /** 最近10次测量的平均时间 */
  recentAverageDuration: number
}

/**
 * FPS 统计
 */
export interface FPSStats {
  /** 当前 FPS */
  current: number
  /** 平均 FPS */
  average: number
  /** 最小 FPS */
  min: number
  /** 最大 FPS */
  max: number
  /** 是否稳定（>= 55 FPS） */
  isStable: boolean
}

/**
 * 内存使用统计
 */
export interface MemoryStats {
  /** 已使用堆内存（MB） */
  usedJSHeapSize: number
  /** 总堆内存（MB） */
  totalJSHeapSize: number
  /** 堆内存限制（MB） */
  jsHeapSizeLimit: number
  /** 内存使用率（%） */
  usagePercentage: number
}

/**
 * 性能监控配置
 */
export interface PerformanceMonitorOptions {
  /** 是否启用FPS监控 */
  enableFPS?: boolean
  /** 是否启用内存监控 */
  enableMemory?: boolean
  /** FPS采样间隔（毫秒） */
  fpsSampleInterval?: number
  /** 内存采样间隔（毫秒） */
  memorySampleInterval?: number
  /** 最大存储的测量记录数 */
  maxMeasurements?: number
  /** 是否启用自动清理 */
  autoCleanup?: boolean
}

// ============= 默认配置 =============

const DEFAULT_OPTIONS: Required<PerformanceMonitorOptions> = {
  enableFPS: true,
  enableMemory: true,
  fpsSampleInterval: 1000,
  memorySampleInterval: 5000,
  maxMeasurements: 100,
  autoCleanup: true
}

// ============= 全局状态 =============

const measurements = ref<Map<string, PerformanceMeasurement[]>>(new Map())
const activeMeasurements = ref<Map<string, number>>(new Map())

// ============= 主要组合函数 =============

/**
 * 性能监控组合函数
 */
export function usePerformanceMonitor(options: PerformanceMonitorOptions = {}) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  // ============= 响应式状态 =============

  const isMonitoring = ref(false)
  const fpsStats = ref<FPSStats>({
    current: 0,
    average: 0,
    min: Infinity,
    max: 0,
    isStable: false
  })
  const memoryStats = ref<MemoryStats>({
    usedJSHeapSize: 0,
    totalJSHeapSize: 0,
    jsHeapSizeLimit: 0,
    usagePercentage: 0
  })

  // ============= FPS 监控 =============

  let fpsFrameCount = 0
  let fpsLastTime = 0
  let fpsSamples: number[] = []
  let fpsAnimationId: number | null = null

  const updateFPS = (currentTime: number) => {
    if (fpsLastTime === 0) {
      fpsLastTime = currentTime
      fpsFrameCount = 0
    }

    fpsFrameCount++

    if (currentTime - fpsLastTime >= config.fpsSampleInterval) {
      const fps = Math.round((fpsFrameCount * 1000) / (currentTime - fpsLastTime))

      fpsSamples.push(fps)
      if (fpsSamples.length > 60) { // 保留最近60个样本
        fpsSamples.shift()
      }

      fpsStats.value = {
        current: fps,
        average: Math.round(fpsSamples.reduce((a, b) => a + b, 0) / fpsSamples.length),
        min: Math.min(fpsStats.value.min, fps),
        max: Math.max(fpsStats.value.max, fps),
        isStable: fps >= 55
      }

      fpsLastTime = currentTime
      fpsFrameCount = 0
    }

    if (config.enableFPS && isMonitoring.value) {
      fpsAnimationId = requestAnimationFrame(updateFPS)
    }
  }

  const startFPSMonitoring = () => {
    if (config.enableFPS && !fpsAnimationId) {
      fpsLastTime = 0
      fpsAnimationId = requestAnimationFrame(updateFPS)
    }
  }

  const stopFPSMonitoring = () => {
    if (fpsAnimationId) {
      cancelAnimationFrame(fpsAnimationId)
      fpsAnimationId = null
    }
  }

  // ============= 内存监控 =============

  let memoryIntervalId: number | null = null

  const updateMemoryStats = () => {
    if ('memory' in performance) {
      const memory = (performance as any).memory
      const usedMB = Math.round(memory.usedJSHeapSize / 1024 / 1024)
      const totalMB = Math.round(memory.totalJSHeapSize / 1024 / 1024)
      const limitMB = Math.round(memory.jsHeapSizeLimit / 1024 / 1024)

      memoryStats.value = {
        usedJSHeapSize: usedMB,
        totalJSHeapSize: totalMB,
        jsHeapSizeLimit: limitMB,
        usagePercentage: Math.round((usedMB / limitMB) * 100)
      }
    }
  }

  const startMemoryMonitoring = () => {
    if (config.enableMemory && !memoryIntervalId) {
      updateMemoryStats()
      memoryIntervalId = setInterval(updateMemoryStats, config.memorySampleInterval)
    }
  }

  const stopMemoryMonitoring = () => {
    if (memoryIntervalId) {
      clearInterval(memoryIntervalId)
      memoryIntervalId = null
    }
  }

  // ============= 性能测量 =============

  /**
   * 开始性能测量
   */
  const startMeasure = (name: string, metadata?: Record<string, any>) => {
    const startTime = performance.now()
    activeMeasurements.value.set(name, startTime)

    // 使用 Performance API 标记（如果可用）
    if ('mark' in performance) {
      try {
        performance.mark(`${name}-start`)
      } catch (error) {
        // 忽略标记错误
      }
    }

    return startTime
  }

  /**
   * 结束性能测量
   */
  const endMeasure = (name: string, metadata?: Record<string, any>): PerformanceMeasurement | null => {
    const startTime = activeMeasurements.value.get(name)
    if (startTime === undefined) {
      console.warn(`Performance measurement '${name}' was not started`)
      return null
    }

    const endTime = performance.now()
    const duration = endTime - startTime

    // 使用 Performance API 测量（如果可用）
    if ('mark' in performance && 'measure' in performance) {
      try {
        performance.mark(`${name}-end`)
        performance.measure(name, `${name}-start`, `${name}-end`)
      } catch (error) {
        // 忽略测量错误
      }
    }

    const measurement: PerformanceMeasurement = {
      name,
      startTime,
      endTime,
      duration,
      metadata
    }

    // 存储测量结果
    if (!measurements.value.has(name)) {
      measurements.value.set(name, [])
    }

    const measurementList = measurements.value.get(name)!
    measurementList.push(measurement)

    // 限制存储数量
    if (measurementList.length > config.maxMeasurements) {
      measurementList.shift()
    }

    // 清理活跃测量
    activeMeasurements.value.delete(name)

    return measurement
  }

  /**
   * 获取最近一次测量结果
   */
  const getLastMeasurement = (name: string): PerformanceMeasurement | null => {
    const measurementList = measurements.value.get(name)
    return measurementList && measurementList.length > 0
      ? measurementList[measurementList.length - 1]
      : null
  }

  /**
   * 获取所有测量结果
   */
  const getAllMeasurements = (name: string): PerformanceMeasurement[] => {
    return measurements.value.get(name) || []
  }

  /**
   * 获取性能统计
   */
  const getStats = (name: string): PerformanceStats | null => {
    const measurementList = measurements.value.get(name)
    if (!measurementList || measurementList.length === 0) {
      return null
    }

    const durations = measurementList.map(m => m.duration)
    const recent = durations.slice(-10) // 最近10次

    return {
      totalMeasurements: measurementList.length,
      averageDuration: durations.reduce((a, b) => a + b, 0) / durations.length,
      minDuration: Math.min(...durations),
      maxDuration: Math.max(...durations),
      recentAverageDuration: recent.reduce((a, b) => a + b, 0) / recent.length
    }
  }

  /**
   * 清理测量数据
   */
  const clearMeasurements = (name?: string) => {
    if (name) {
      measurements.value.delete(name)
    } else {
      measurements.value.clear()
    }
  }

  // ============= 工具函数 =============

  /**
   * 性能标记
   */
  const mark = (name: string) => {
    if ('mark' in performance) {
      try {
        performance.mark(name)
      } catch (error) {
        console.warn('Failed to create performance mark:', error)
      }
    }
  }

  /**
   * 测量两个标记之间的时间
   */
  const measureBetweenMarks = (measureName: string, startMark: string, endMark: string) => {
    if ('measure' in performance) {
      try {
        performance.measure(measureName, startMark, endMark)
        const entries = performance.getEntriesByName(measureName, 'measure')
        return entries.length > 0 ? entries[entries.length - 1].duration : null
      } catch (error) {
        console.warn('Failed to measure between marks:', error)
        return null
      }
    }
    return null
  }

  /**
   * 获取Navigation Timing信息
   */
  const getNavigationTiming = () => {
    if ('getEntriesByType' in performance) {
      const entries = performance.getEntriesByType('navigation') as PerformanceNavigationTiming[]
      return entries.length > 0 ? entries[0] : null
    }
    return null
  }

  /**
   * 获取页面加载性能指标
   */
  const getPageLoadMetrics = () => {
    const navigation = getNavigationTiming()
    if (!navigation) return null

    return {
      /** DNS查询时间 */
      dnsLookup: navigation.domainLookupEnd - navigation.domainLookupStart,
      /** TCP连接时间 */
      tcpConnection: navigation.connectEnd - navigation.connectStart,
      /** HTTP请求响应时间 */
      httpRequest: navigation.responseEnd - navigation.requestStart,
      /** DOM解析时间 */
      domParsing: navigation.domInteractive - navigation.fetchStart,
      /** 资源加载时间 */
      resourceLoading: navigation.loadEventStart - navigation.domContentLoadedEventEnd,
      /** 总加载时间 */
      totalLoad: navigation.loadEventEnd - navigation.fetchStart,
      /** 首次内容绘制时间 */
      firstContentfulPaint: navigation.domContentLoadedEventEnd - navigation.fetchStart
    }
  }

  // ============= 监控控制 =============

  /**
   * 开始监控
   */
  const startMonitoring = () => {
    if (!isMonitoring.value) {
      isMonitoring.value = true
      startFPSMonitoring()
      startMemoryMonitoring()
    }
  }

  /**
   * 停止监控
   */
  const stopMonitoring = () => {
    if (isMonitoring.value) {
      isMonitoring.value = false
      stopFPSMonitoring()
      stopMemoryMonitoring()
    }
  }

  // ============= 计算属性 =============

  const overallStats = computed(() => {
    const allMeasurements: PerformanceMeasurement[] = []
    measurements.value.forEach(measurementList => {
      allMeasurements.push(...measurementList)
    })

    if (allMeasurements.length === 0) {
      return null
    }

    const durations = allMeasurements.map(m => m.duration)

    return {
      totalMeasurements: allMeasurements.length,
      averageDuration: durations.reduce((a, b) => a + b, 0) / durations.length,
      minDuration: Math.min(...durations),
      maxDuration: Math.max(...durations),
      measurementNames: Array.from(measurements.value.keys())
    }
  })

  // ============= 生命周期 =============

  onMounted(() => {
    if (config.enableFPS || config.enableMemory) {
      startMonitoring()
    }
  })

  onUnmounted(() => {
    stopMonitoring()

    if (config.autoCleanup) {
      clearMeasurements()
    }
  })

  // ============= 返回 API =============

  return {
    // 状态
    isMonitoring,
    fpsStats,
    memoryStats,
    overallStats,

    // 测量方法
    startMeasure,
    endMeasure,
    getLastMeasurement,
    getAllMeasurements,
    getStats,
    clearMeasurements,

    // 工具方法
    mark,
    measureBetweenMarks,
    getNavigationTiming,
    getPageLoadMetrics,

    // 监控控制
    startMonitoring,
    stopMonitoring,

    // 原始数据
    measurements: computed(() => measurements.value),
    activeMeasurements: computed(() => activeMeasurements.value)
  }
}

// ============= 全局性能监控实例 =============

let globalMonitor: ReturnType<typeof usePerformanceMonitor> | null = null

/**
 * 获取全局性能监控实例
 */
export function getGlobalPerformanceMonitor() {
  if (!globalMonitor) {
    globalMonitor = usePerformanceMonitor({
      enableFPS: true,
      enableMemory: true,
      maxMeasurements: 200,
      autoCleanup: false
    })
  }
  return globalMonitor
}

// ============= 性能监控装饰器 =============

/**
 * 性能监控装饰器函数
 */
export function withPerformanceMonitoring<T extends (...args: any[]) => any>(
  fn: T,
  measurementName?: string
): T {
  const monitor = getGlobalPerformanceMonitor()
  const name = measurementName || fn.name || 'anonymous-function'

  return ((...args: any[]) => {
    monitor.startMeasure(name)

    try {
      const result = fn(...args)

      if (result instanceof Promise) {
        return result.finally(() => {
          monitor.endMeasure(name)
        })
      } else {
        monitor.endMeasure(name)
        return result
      }
    } catch (error) {
      monitor.endMeasure(name)
      throw error
    }
  }) as T
}

// ============= 性能分析工具 =============

/**
 * 分析性能瓶颈
 */
export function analyzePerformanceBottlenecks(threshold: number = 100): Array<{
  name: string
  averageDuration: number
  maxDuration: number
  problemLevel: 'warning' | 'critical'
}> {
  const monitor = getGlobalPerformanceMonitor()
  const bottlenecks: Array<{
    name: string
    averageDuration: number
    maxDuration: number
    problemLevel: 'warning' | 'critical'
  }> = []

  monitor.measurements.value.forEach((measurementList, name) => {
    const stats = monitor.getStats(name)
    if (stats && stats.averageDuration > threshold) {
      bottlenecks.push({
        name,
        averageDuration: stats.averageDuration,
        maxDuration: stats.maxDuration,
        problemLevel: stats.averageDuration > threshold * 2 ? 'critical' : 'warning'
      })
    }
  })

  return bottlenecks.sort((a, b) => b.averageDuration - a.averageDuration)
}

/**
 * 生成性能报告
 */
export function generatePerformanceReport(): {
  summary: any
  measurements: Record<string, PerformanceStats>
  pageMetrics: any
  bottlenecks: any[]
} {
  const monitor = getGlobalPerformanceMonitor()

  const measurements: Record<string, PerformanceStats> = {}
  monitor.measurements.value.forEach((_, name) => {
    const stats = monitor.getStats(name)
    if (stats) {
      measurements[name] = stats
    }
  })

  return {
    summary: {
      totalMeasurements: monitor.overallStats.value?.totalMeasurements || 0,
      averageDuration: monitor.overallStats.value?.averageDuration || 0,
      fps: monitor.fpsStats.value,
      memory: monitor.memoryStats.value
    },
    measurements,
    pageMetrics: monitor.getPageLoadMetrics(),
    bottlenecks: analyzePerformanceBottlenecks()
  }
}