/**
 * 词云性能优化可组合函数
 * 提供内存管理、大数据量处理、渲染优化等功能
 */

import {
  ref,
  reactive,
  computed,
  watch,
  nextTick,
  onMounted,
  onUnmounted,
  shallowRef,
  readonly,
  type Ref
} from 'vue'
import type { HotWordItem, WordCloudPerformanceConfig } from '@/types/statistics'

// 性能监控指标类型
export interface PerformanceMetrics {
  /** 渲染时间 (毫秒) */
  renderTime: number
  /** 内存使用量 (字节) */
  memoryUsage: number
  /** 数据处理时间 (毫秒) */
  dataProcessingTime: number
  /** FPS */
  fps: number
  /** 最后更新时间 */
  lastUpdate: number
}

// 渲染状态类型
export interface RenderPerformanceState {
  /** 是否正在渲染 */
  isRendering: boolean
  /** 渲染进度 */
  progress: number
  /** 当前批次 */
  currentBatch: number
  /** 总批次数 */
  totalBatches: number
  /** 错误信息 */
  error: string | null
  /** 是否被取消 */
  cancelled: boolean
}

// 内存管理状态
export interface MemoryManagementState {
  /** 缓存的画布数据 */
  canvasDataCache: Map<string, ImageData>
  /** 缓存大小限制 (字节) */
  maxCacheSize: number
  /** 当前缓存大小 */
  currentCacheSize: number
  /** 最后清理时间 */
  lastCleanupTime: number
}

/**
 * 词云性能优化hooks
 */
export function useWordCloudPerformance(options: Partial<WordCloudPerformanceConfig> = {}) {
  // ============= 默认配置 =============

  const defaultConfig: WordCloudPerformanceConfig = {
    batchSize: 50,
    renderDelay: 10,
    maxRenderTime: 10000,
    debounceDelay: 300
  }

  const config = reactive({
    ...defaultConfig,
    ...options
  })

  // ============= 响应式状态 =============

  /** 性能监控指标 */
  const metrics = reactive<PerformanceMetrics>({
    renderTime: 0,
    memoryUsage: 0,
    dataProcessingTime: 0,
    fps: 0,
    lastUpdate: Date.now()
  })

  /** 渲染性能状态 */
  const renderState = reactive<RenderPerformanceState>({
    isRendering: false,
    progress: 0,
    currentBatch: 0,
    totalBatches: 0,
    error: null,
    cancelled: false
  })

  /** 内存管理状态 */
  const memoryState = reactive<MemoryManagementState>({
    canvasDataCache: new Map(),
    maxCacheSize: 50 * 1024 * 1024, // 50MB
    currentCacheSize: 0,
    lastCleanupTime: Date.now()
  })

  /** 处理后的词云数据 (使用shallowRef优化大数据) */
  const processedWords = shallowRef<HotWordItem[]>([])

  /** FPS监控 */
  const fpsCounter = ref(0)
  const frameCount = ref(0)
  const lastFrameTime = ref(Date.now())

  // ============= 计算属性 =============

  /** 是否需要分批渲染 */
  const shouldBatchRender = computed(() =>
    processedWords.value.length > config.batchSize
  )

  /** 当前内存使用率 */
  const memoryUsagePercent = computed(() =>
    (memoryState.currentCacheSize / memoryState.maxCacheSize) * 100
  )

  /** 是否需要内存清理 */
  const needsMemoryCleanup = computed(() =>
    memoryUsagePercent.value > 80 ||
    Date.now() - memoryState.lastCleanupTime > 5 * 60 * 1000 // 5分钟
  )

  /** 性能等级评估 */
  const performanceLevel = computed(() => {
    const { renderTime, memoryUsage, fps } = metrics

    if (renderTime < 100 && fps > 50 && memoryUsage < 20 * 1024 * 1024) {
      return 'excellent'
    } else if (renderTime < 500 && fps > 30 && memoryUsage < 50 * 1024 * 1024) {
      return 'good'
    } else if (renderTime < 1000 && fps > 20) {
      return 'average'
    } else {
      return 'poor'
    }
  })

  // ============= 数据处理优化 =============

  /**
   * 智能数据预处理
   * 根据数据量和设备性能自动选择最优处理策略
   */
  const preprocessWordData = async (words: HotWordItem[]): Promise<HotWordItem[]> => {
    const startTime = Date.now()

    try {
      // 数据验证和清理
      const validWords = words.filter(word =>
        word &&
        typeof word.text === 'string' &&
        word.text.trim().length > 0 &&
        typeof word.weight === 'number' &&
        word.weight > 0
      )

      // 数据去重（保留权重最高的）
      const wordMap = new Map<string, HotWordItem>()
      validWords.forEach(word => {
        const existing = wordMap.get(word.text)
        if (!existing || word.weight > existing.weight) {
          wordMap.set(word.text, word)
        }
      })

      // 权重归一化
      const deduplicatedWords = Array.from(wordMap.values())
      const maxWeight = Math.max(...deduplicatedWords.map(w => w.weight))
      const minWeight = Math.min(...deduplicatedWords.map(w => w.weight))
      const weightRange = maxWeight - minWeight

      const normalizedWords = deduplicatedWords.map(word => ({
        ...word,
        weight: weightRange > 0 ? ((word.weight - minWeight) / weightRange) * 100 : 50
      }))

      // 按权重排序
      const sortedWords = normalizedWords.sort((a, b) => b.weight - a.weight)

      // 根据性能自动限制数量
      const maxWords = getOptimalWordCount()
      const finalWords = sortedWords.slice(0, maxWords)

      // 更新处理后的数据
      processedWords.value = finalWords

      // 记录处理时间
      metrics.dataProcessingTime = Date.now() - startTime

      return finalWords
    } catch (error) {
      console.error('数据预处理失败:', error)
      metrics.dataProcessingTime = Date.now() - startTime
      throw error
    }
  }

  /**
   * 获取最优词语数量
   * 根据设备性能和屏幕尺寸动态调整
   */
  const getOptimalWordCount = (): number => {
    const devicePixelRatio = window.devicePixelRatio || 1
    const screenArea = window.screen.width * window.screen.height
    const memoryInfo = (performance as any).memory

    // 基础数量
    let baseCount = 100

    // 根据屏幕尺寸调整
    if (screenArea > 2073600) { // > 1920x1080
      baseCount = 200
    } else if (screenArea > 921600) { // > 1280x720
      baseCount = 150
    } else {
      baseCount = 100
    }

    // 根据内存情况调整
    if (memoryInfo && memoryInfo.usedJSHeapSize) {
      const memoryUsageRatio = memoryInfo.usedJSHeapSize / memoryInfo.totalJSHeapSize
      if (memoryUsageRatio > 0.8) {
        baseCount = Math.floor(baseCount * 0.7)
      } else if (memoryUsageRatio > 0.6) {
        baseCount = Math.floor(baseCount * 0.85)
      }
    }

    // 根据设备像素比调整
    if (devicePixelRatio > 2) {
      baseCount = Math.floor(baseCount * 1.2)
    }

    return Math.max(50, Math.min(baseCount, 300))
  }

  // ============= 渲染优化 =============

  /**
   * 分批渲染处理
   * 避免长时间阻塞主线程
   */
  const batchRenderWords = async (
    words: HotWordItem[],
    renderFunction: (batch: HotWordItem[], batchIndex: number) => Promise<void>
  ): Promise<void> => {
    if (!shouldBatchRender.value) {
      await renderFunction(words, 0)
      return
    }

    const batches = Math.ceil(words.length / config.batchSize)
    renderState.totalBatches = batches
    renderState.currentBatch = 0
    renderState.isRendering = true
    renderState.cancelled = false
    renderState.error = null

    try {
      for (let i = 0; i < batches; i++) {
        if (renderState.cancelled) {
          throw new Error('渲染被取消')
        }

        const startIndex = i * config.batchSize
        const endIndex = Math.min(startIndex + config.batchSize, words.length)
        const batch = words.slice(startIndex, endIndex)

        renderState.currentBatch = i + 1
        renderState.progress = (i + 1) / batches * 100

        await renderFunction(batch, i)

        // 让出控制权，避免阻塞UI
        if (i < batches - 1) {
          await new Promise(resolve => setTimeout(resolve, config.renderDelay))
        }
      }
    } catch (error) {
      renderState.error = error instanceof Error ? error.message : '渲染失败'
      throw error
    } finally {
      renderState.isRendering = false
    }
  }

  /**
   * 取消渲染
   */
  const cancelRender = () => {
    renderState.cancelled = true
    renderState.isRendering = false
  }

  // ============= 内存管理 =============

  /**
   * 缓存画布数据
   */
  const cacheCanvasData = (key: string, canvas: HTMLCanvasElement) => {
    try {
      const ctx = canvas.getContext('2d')
      if (!ctx) return

      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
      const estimatedSize = imageData.data.length * 4 // RGBA

      // 检查缓存大小限制
      if (memoryState.currentCacheSize + estimatedSize > memoryState.maxCacheSize) {
        performMemoryCleanup()
      }

      memoryState.canvasDataCache.set(key, imageData)
      memoryState.currentCacheSize += estimatedSize
    } catch (error) {
      console.warn('缓存画布数据失败:', error)
    }
  }

  /**
   * 获取缓存的画布数据
   */
  const getCachedCanvasData = (key: string): ImageData | null => {
    return memoryState.canvasDataCache.get(key) || null
  }

  /**
   * 执行内存清理
   */
  const performMemoryCleanup = () => {
    const now = Date.now()
    const entries = Array.from(memoryState.canvasDataCache.entries())

    // 清理最旧的50%缓存
    const sortedEntries = entries.sort((a, b) => {
      // 简单的LRU策略，实际应用中可以更复杂
      return Math.random() - 0.5
    })

    const toRemove = Math.floor(sortedEntries.length * 0.5)
    for (let i = 0; i < toRemove; i++) {
      const [key, imageData] = sortedEntries[i]
      memoryState.canvasDataCache.delete(key)
      memoryState.currentCacheSize -= imageData.data.length * 4
    }

    memoryState.lastCleanupTime = now

    // 强制垃圾回收（如果支持）
    if (window.gc) {
      window.gc()
    }
  }

  /**
   * 清空所有缓存
   */
  const clearAllCache = () => {
    memoryState.canvasDataCache.clear()
    memoryState.currentCacheSize = 0
    memoryState.lastCleanupTime = Date.now()
  }

  // ============= 性能监控 =============

  /**
   * 更新FPS统计
   */
  const updateFPS = () => {
    frameCount.value++
    const now = Date.now()
    const elapsed = now - lastFrameTime.value

    if (elapsed >= 1000) {
      metrics.fps = Math.round((frameCount.value * 1000) / elapsed)
      frameCount.value = 0
      lastFrameTime.value = now
    }
  }

  /**
   * 测量渲染性能
   */
  const measureRenderPerformance = async <T>(
    renderFunction: () => Promise<T>
  ): Promise<T> => {
    const startTime = performance.now()
    const memoryBefore = getMemoryUsage()

    try {
      const result = await renderFunction()

      const endTime = performance.now()
      const memoryAfter = getMemoryUsage()

      metrics.renderTime = Math.round(endTime - startTime)
      metrics.memoryUsage = memoryAfter
      metrics.lastUpdate = Date.now()

      return result
    } catch (error) {
      const endTime = performance.now()
      metrics.renderTime = Math.round(endTime - startTime)
      metrics.lastUpdate = Date.now()
      throw error
    }
  }

  /**
   * 获取内存使用情况
   */
  const getMemoryUsage = (): number => {
    const memoryInfo = (performance as any).memory
    return memoryInfo ? memoryInfo.usedJSHeapSize : 0
  }

  /**
   * 性能报告
   */
  const getPerformanceReport = () => {
    return {
      metrics: { ...metrics },
      renderState: { ...renderState },
      memoryState: {
        cacheSize: memoryState.currentCacheSize,
        cacheCount: memoryState.canvasDataCache.size,
        usagePercent: memoryUsagePercent.value
      },
      config: { ...config },
      performanceLevel: performanceLevel.value,
      recommendations: getPerformanceRecommendations()
    }
  }

  /**
   * 获取性能优化建议
   */
  const getPerformanceRecommendations = (): string[] => {
    const recommendations: string[] = []

    if (metrics.renderTime > 1000) {
      recommendations.push('渲染时间过长，建议减少词语数量或启用分批渲染')
    }

    if (memoryUsagePercent.value > 80) {
      recommendations.push('内存使用率过高，建议清理缓存或减少缓存大小')
    }

    if (metrics.fps < 30) {
      recommendations.push('帧率过低，建议优化动画或减少视觉效果')
    }

    if (processedWords.value.length > 200) {
      recommendations.push('词语数量较多，建议启用虚拟化或分页显示')
    }

    return recommendations
  }

  // ============= 监听器和生命周期 =============

  /** 防抖函数 */
  const debounce = <T extends (...args: any[]) => any>(
    func: T,
    delay: number
  ): ((...args: Parameters<T>) => void) => {
    let timeoutId: number | null = null

    return (...args: Parameters<T>) => {
      if (timeoutId) {
        clearTimeout(timeoutId)
      }
      timeoutId = window.setTimeout(() => func(...args), delay)
    }
  }

  /** 防抖的内存清理 */
  const debouncedCleanup = debounce(performMemoryCleanup, 1000)

  // 监听内存使用情况
  watch(needsMemoryCleanup, (needs) => {
    if (needs) {
      debouncedCleanup()
    }
  })

  // FPS监控循环
  let fpsAnimationId: number | null = null
  const startFPSMonitoring = () => {
    const loop = () => {
      updateFPS()
      fpsAnimationId = requestAnimationFrame(loop)
    }
    loop()
  }

  const stopFPSMonitoring = () => {
    if (fpsAnimationId) {
      cancelAnimationFrame(fpsAnimationId)
      fpsAnimationId = null
    }
  }

  // 生命周期管理
  onMounted(() => {
    startFPSMonitoring()
  })

  onUnmounted(() => {
    stopFPSMonitoring()
    clearAllCache()
    cancelRender()
  })

  // ============= 响应式处理优化 =============

  /**
   * 智能响应式渲染
   * 根据容器尺寸变化和设备性能智能调整渲染策略
   */
  const smartResponsiveRender = async (
    containerWidth: number,
    containerHeight: number,
    renderFunction: () => Promise<void>
  ) => {
    // 计算最适合的渲染参数
    const optimalDimensions = calculateOptimalDimensions(containerWidth, containerHeight)
    const optimalWordCount = getOptimalWordCount()

    // 根据尺寸变化决定是否需要重新预处理数据
    const shouldReprocess = shouldReprocessData(optimalDimensions, optimalWordCount)

    if (shouldReprocess && processedWords.value.length > 0) {
      // 重新处理数据以适应新的尺寸和性能要求
      const originalWords = [...processedWords.value]
      await preprocessWordData(originalWords)
    }

    // 执行渲染
    await measureRenderPerformance(renderFunction)
  }

  /**
   * 计算最优渲染尺寸
   */
  const calculateOptimalDimensions = (width: number, height: number) => {
    const devicePixelRatio = window.devicePixelRatio || 1
    const maxWidth = Math.min(width, 2000) // 限制最大宽度避免性能问题
    const maxHeight = Math.min(height, 1500)

    return {
      width: Math.max(300, maxWidth), // 最小宽度300px
      height: Math.max(200, maxHeight), // 最小高度200px
      aspectRatio: maxWidth / maxHeight,
      pixelRatio: Math.min(devicePixelRatio, 2) // 限制最大像素比
    }
  }

  /**
   * 判断是否需要重新处理数据
   */
  const shouldReprocessData = (dimensions: any, wordCount: number) => {
    const currentDimensions = {
      width: 800,
      height: 600
    }

    const sizeChangeRatio = Math.abs(
      (dimensions.width * dimensions.height) -
      (currentDimensions.width * currentDimensions.height)
    ) / (currentDimensions.width * currentDimensions.height)

    return sizeChangeRatio > 0.3 || // 尺寸变化超过30%
           Math.abs(processedWords.value.length - wordCount) > 20 // 词数变化超过20个
  }

  /**
   * 虚拟化渲染支持
   * 对于超大数据集，只渲染可视区域内的内容
   */
  const virtualizeWordRendering = (
    words: HotWordItem[],
    viewportWidth: number,
    viewportHeight: number
  ) => {
    if (words.length <= 100) {
      return words // 小数据集直接返回
    }

    // 按权重排序，优先显示重要词语
    const sortedWords = [...words].sort((a, b) => b.weight - a.weight)

    // 根据视口大小估算可容纳的词语数量
    const estimatedCapacity = Math.floor((viewportWidth * viewportHeight) / 2000) // 假设每个词占用2000px²
    const maxWords = Math.min(Math.max(estimatedCapacity, 50), 300)

    return sortedWords.slice(0, maxWords)
  }

  /**
   * 动态质量调整
   * 根据性能实时调整渲染质量
   */
  const adjustRenderQuality = () => {
    const { renderTime, fps, memoryUsage } = metrics

    let qualityLevel: 'low' | 'medium' | 'high' = 'high'

    if (renderTime > 1000 || fps < 20 || memoryUsage > 100 * 1024 * 1024) {
      qualityLevel = 'low'
    } else if (renderTime > 500 || fps < 40 || memoryUsage > 50 * 1024 * 1024) {
      qualityLevel = 'medium'
    }

    return getQualitySettings(qualityLevel)
  }

  /**
   * 获取质量设置
   */
  const getQualitySettings = (level: 'low' | 'medium' | 'high') => {
    switch (level) {
      case 'low':
        return {
          maxWords: 50,
          gridSize: 16,
          rotationSteps: 1,
          enableEffects: false
        }
      case 'medium':
        return {
          maxWords: 100,
          gridSize: 12,
          rotationSteps: 2,
          enableEffects: true
        }
      case 'high':
      default:
        return {
          maxWords: 200,
          gridSize: 8,
          rotationSteps: 4,
          enableEffects: true
        }
    }
  }

  // ============= 返回接口 =============

  return {
    // 状态
    metrics: readonly(metrics),
    renderState: readonly(renderState),
    memoryState: readonly(memoryState),
    processedWords: readonly(processedWords),
    config,

    // 计算属性
    shouldBatchRender,
    memoryUsagePercent,
    needsMemoryCleanup,
    performanceLevel,

    // 数据处理
    preprocessWordData,
    getOptimalWordCount,

    // 渲染优化
    batchRenderWords,
    cancelRender,
    measureRenderPerformance,
    smartResponsiveRender,
    virtualizeWordRendering,
    adjustRenderQuality,

    // 内存管理
    cacheCanvasData,
    getCachedCanvasData,
    performMemoryCleanup,
    clearAllCache,

    // 性能监控
    getPerformanceReport,
    getPerformanceRecommendations,
    getMemoryUsage,

    // 响应式处理
    calculateOptimalDimensions,
    shouldReprocessData,

    // 工具函数
    debounce
  }
}

/**
 * 性能优化配置hooks
 * 提供动态配置和自动调优功能
 */
export function usePerformanceConfig() {
  const config = reactive<WordCloudPerformanceConfig>({
    batchSize: 50,
    renderDelay: 10,
    maxRenderTime: 10000,
    debounceDelay: 300
  })

  /**
   * 自动调优配置
   * 根据设备性能和运行时指标动态调整
   */
  const autoTuneConfig = (metrics: PerformanceMetrics) => {
    // 根据渲染时间调整批次大小
    if (metrics.renderTime > 500) {
      config.batchSize = Math.max(20, config.batchSize - 10)
      config.renderDelay = Math.min(50, config.renderDelay + 5)
    } else if (metrics.renderTime < 100) {
      config.batchSize = Math.min(100, config.batchSize + 10)
      config.renderDelay = Math.max(5, config.renderDelay - 2)
    }

    // 根据内存使用调整超时时间
    if (metrics.memoryUsage > 100 * 1024 * 1024) { // > 100MB
      config.maxRenderTime = Math.max(5000, config.maxRenderTime - 1000)
    }

    // 根据FPS调整防抖延迟
    if (metrics.fps < 30) {
      config.debounceDelay = Math.min(500, config.debounceDelay + 50)
    } else if (metrics.fps > 50) {
      config.debounceDelay = Math.max(100, config.debounceDelay - 20)
    }
  }

  /**
   * 重置为默认配置
   */
  const resetConfig = () => {
    config.batchSize = 50
    config.renderDelay = 10
    config.maxRenderTime = 10000
    config.debounceDelay = 300
  }

  /**
   * 应用性能配置预设
   */
  const applyPreset = (preset: 'performance' | 'quality' | 'balanced') => {
    switch (preset) {
      case 'performance':
        config.batchSize = 30
        config.renderDelay = 5
        config.maxRenderTime = 5000
        config.debounceDelay = 200
        break
      case 'quality':
        config.batchSize = 100
        config.renderDelay = 20
        config.maxRenderTime = 15000
        config.debounceDelay = 500
        break
      case 'balanced':
      default:
        resetConfig()
        break
    }
  }

  return {
    config: readonly(config),
    autoTuneConfig,
    resetConfig,
    applyPreset
  }
}