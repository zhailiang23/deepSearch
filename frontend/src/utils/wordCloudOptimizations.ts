/**
 * 词云图性能优化工具函数
 * 提供渲染优化、数据处理、内存管理等工具函数
 */

import type { HotWordItem, WordCloudOptions } from '@/types/statistics'

// ============= 类型定义 =============

export interface OptimizationContext {
  /** 容器尺寸 */
  containerWidth: number
  containerHeight: number
  /** 设备信息 */
  devicePixelRatio: number
  /** 可用内存 (字节) */
  availableMemory: number
  /** 当前FPS */
  currentFPS: number
}

export interface RenderOptimizationResult {
  /** 优化后的词云配置 */
  optimizedOptions: Partial<WordCloudOptions>
  /** 建议的词语数量 */
  recommendedWordCount: number
  /** 性能等级 */
  performanceLevel: 'low' | 'medium' | 'high'
  /** 优化建议 */
  suggestions: string[]
}

export interface DataChunk<T> {
  /** 数据块 */
  data: T[]
  /** 块索引 */
  index: number
  /** 总块数 */
  total: number
  /** 块大小 */
  size: number
}

// ============= 设备性能检测 =============

/**
 * 检测设备性能等级
 */
export function detectDevicePerformance(): 'low' | 'medium' | 'high' {
  const navigator = window.navigator as any

  // 检测硬件并发数
  const cores = navigator.hardwareConcurrency || 2

  // 检测内存信息
  const memory = (performance as any).memory
  const totalMemory = memory ? memory.jsHeapSizeLimit : 0

  // 检测用户代理
  const userAgent = navigator.userAgent.toLowerCase()
  const isMobile = /mobile|android|iphone|ipad|phone|blackberry|opera|mini|windows\sce|palm|smartphone|iemobile/i.test(userAgent)

  // 性能评分
  let score = 0

  // CPU评分
  if (cores >= 8) score += 3
  else if (cores >= 4) score += 2
  else score += 1

  // 内存评分
  if (totalMemory > 2 * 1024 * 1024 * 1024) score += 3 // > 2GB
  else if (totalMemory > 1 * 1024 * 1024 * 1024) score += 2 // > 1GB
  else score += 1

  // 设备类型评分
  if (!isMobile) score += 2

  // 根据总分判断性能等级
  if (score >= 7) return 'high'
  if (score >= 4) return 'medium'
  return 'low'
}

/**
 * 获取系统内存信息
 */
export function getMemoryInfo() {
  const memory = (performance as any).memory

  if (!memory) {
    return {
      used: 0,
      total: 0,
      available: 0,
      usagePercent: 0
    }
  }

  return {
    used: memory.usedJSHeapSize,
    total: memory.totalJSHeapSize,
    available: memory.jsHeapSizeLimit - memory.usedJSHeapSize,
    usagePercent: (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100
  }
}

/**
 * 测量FPS
 */
export function measureFPS(duration: number = 1000): Promise<number> {
  return new Promise((resolve) => {
    let frameCount = 0
    const startTime = performance.now()

    function frame() {
      frameCount++
      const elapsed = performance.now() - startTime

      if (elapsed >= duration) {
        const fps = Math.round((frameCount * 1000) / elapsed)
        resolve(fps)
      } else {
        requestAnimationFrame(frame)
      }
    }

    requestAnimationFrame(frame)
  })
}

// ============= 数据优化 =============

/**
 * 智能数据分片
 * 根据数据量和设备性能将数据分成最优大小的块
 */
export function smartDataChunking<T>(
  data: T[],
  context: OptimizationContext
): DataChunk<T>[] {
  if (data.length === 0) return []

  // 根据设备性能计算最优块大小
  const baseChunkSize = calculateOptimalChunkSize(data.length, context)
  const chunks: DataChunk<T>[] = []

  for (let i = 0; i < data.length; i += baseChunkSize) {
    const chunk = data.slice(i, i + baseChunkSize)
    chunks.push({
      data: chunk,
      index: Math.floor(i / baseChunkSize),
      total: Math.ceil(data.length / baseChunkSize),
      size: chunk.length
    })
  }

  return chunks
}

/**
 * 计算最优块大小
 */
function calculateOptimalChunkSize(
  dataLength: number,
  context: OptimizationContext
): number {
  const { devicePixelRatio, availableMemory, currentFPS } = context

  // 基础块大小
  let baseSize = 50

  // 根据数据量调整
  if (dataLength > 1000) baseSize = 30
  else if (dataLength > 500) baseSize = 40

  // 根据设备像素比调整
  if (devicePixelRatio > 2) baseSize = Math.floor(baseSize * 0.8)

  // 根据可用内存调整
  const memoryMB = availableMemory / (1024 * 1024)
  if (memoryMB < 100) baseSize = Math.floor(baseSize * 0.6)
  else if (memoryMB < 200) baseSize = Math.floor(baseSize * 0.8)

  // 根据FPS调整
  if (currentFPS < 30) baseSize = Math.floor(baseSize * 0.7)
  else if (currentFPS > 50) baseSize = Math.floor(baseSize * 1.2)

  return Math.max(10, Math.min(baseSize, 100))
}

/**
 * 词语数据去重和归一化
 */
export function normalizeWordData(words: HotWordItem[]): HotWordItem[] {
  if (!words || words.length === 0) return []

  // 去重（保留权重最高的）
  const wordMap = new Map<string, HotWordItem>()

  words.forEach(word => {
    if (!word.text || typeof word.weight !== 'number') return

    const existing = wordMap.get(word.text.toLowerCase())
    if (!existing || word.weight > existing.weight) {
      wordMap.set(word.text.toLowerCase(), {
        ...word,
        text: word.text.trim()
      })
    }
  })

  const uniqueWords = Array.from(wordMap.values())

  if (uniqueWords.length === 0) return []

  // 权重归一化
  const weights = uniqueWords.map(w => w.weight)
  const minWeight = Math.min(...weights)
  const maxWeight = Math.max(...weights)
  const weightRange = maxWeight - minWeight

  return uniqueWords.map(word => ({
    ...word,
    weight: weightRange > 0
      ? Math.round(((word.weight - minWeight) / weightRange) * 100)
      : 50
  })).sort((a, b) => b.weight - a.weight)
}

/**
 * 根据重要性过滤词语
 */
export function filterWordsByImportance(
  words: HotWordItem[],
  maxCount: number,
  minWeight: number = 0
): HotWordItem[] {
  return words
    .filter(word => word.weight >= minWeight)
    .slice(0, maxCount)
}

// ============= 渲染优化 =============

/**
 * 生成优化的词云配置
 */
export function generateOptimizedConfig(
  context: OptimizationContext,
  baseOptions: Partial<WordCloudOptions> = {}
): RenderOptimizationResult {
  const devicePerformance = detectDevicePerformance()
  const memoryInfo = getMemoryInfo()

  // 基础配置
  const config: Partial<WordCloudOptions> = {
    ...baseOptions
  }

  const suggestions: string[] = []
  let recommendedWordCount = 100

  // 根据设备性能调整配置
  switch (devicePerformance) {
    case 'low':
      config.gridSize = 16
      config.rotationSteps = 1
      config.rotateRatio = 0.1
      recommendedWordCount = 50
      suggestions.push('检测到低性能设备，已启用性能优化模式')
      break

    case 'medium':
      config.gridSize = 12
      config.rotationSteps = 2
      config.rotateRatio = 0.3
      recommendedWordCount = 100
      suggestions.push('中等性能设备，使用平衡配置')
      break

    case 'high':
      config.gridSize = 8
      config.rotationSteps = 4
      config.rotateRatio = 0.5
      recommendedWordCount = 200
      suggestions.push('高性能设备，可使用最佳画质')
      break
  }

  // 根据容器尺寸调整
  const area = context.containerWidth * context.containerHeight
  if (area < 300 * 200) {
    config.gridSize = Math.max(config.gridSize || 8, 12)
    recommendedWordCount = Math.min(recommendedWordCount, 30)
    suggestions.push('容器尺寸较小，减少词语数量以提升可读性')
  } else if (area > 1200 * 800) {
    recommendedWordCount = Math.min(recommendedWordCount * 1.5, 300)
    suggestions.push('大尺寸容器，可显示更多词语')
  }

  // 根据内存使用情况调整
  if (memoryInfo.usagePercent > 80) {
    recommendedWordCount = Math.floor(recommendedWordCount * 0.7)
    config.gridSize = Math.max(config.gridSize || 8, 14)
    suggestions.push('内存使用率较高，建议减少词语数量')
  }

  // 根据设备像素比调整
  if (context.devicePixelRatio > 2) {
    config.gridSize = Math.max(config.gridSize || 8, 10)
    suggestions.push('高分辨率屏幕，调整网格大小以平衡性能')
  }

  // 设置超时和取消机制
  config.abortThreshold = devicePerformance === 'low' ? 3000 : 5000
  config.wait = devicePerformance === 'low' ? 5 : 1

  return {
    optimizedOptions: config,
    recommendedWordCount,
    performanceLevel: devicePerformance,
    suggestions
  }
}

/**
 * Canvas优化配置
 */
export function optimizeCanvasSettings(
  canvas: HTMLCanvasElement,
  context: OptimizationContext
) {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 根据设备性能调整渲染设置
  const performance = detectDevicePerformance()

  if (performance === 'low') {
    // 低性能设备优化
    ctx.imageSmoothingEnabled = false
    canvas.style.imageRendering = 'pixelated'
  } else {
    // 中高性能设备保持质量
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
  }

  // 设置合适的像素比
  const pixelRatio = Math.min(context.devicePixelRatio, performance === 'low' ? 1 : 2)
  const rect = canvas.getBoundingClientRect()

  canvas.width = rect.width * pixelRatio
  canvas.height = rect.height * pixelRatio
  canvas.style.width = rect.width + 'px'
  canvas.style.height = rect.height + 'px'

  ctx.scale(pixelRatio, pixelRatio)
}

// ============= 内存管理 =============

/**
 * LRU缓存实现
 */
export class LRUCache<K, V> {
  private maxSize: number
  private cache = new Map<K, V>()

  constructor(maxSize: number) {
    this.maxSize = maxSize
  }

  get(key: K): V | undefined {
    const value = this.cache.get(key)
    if (value !== undefined) {
      // 移动到最新位置
      this.cache.delete(key)
      this.cache.set(key, value)
    }
    return value
  }

  set(key: K, value: V): void {
    if (this.cache.has(key)) {
      this.cache.delete(key)
    } else if (this.cache.size >= this.maxSize) {
      // 删除最久未使用的项
      const firstKey = this.cache.keys().next().value
      this.cache.delete(firstKey)
    }
    this.cache.set(key, value)
  }

  has(key: K): boolean {
    return this.cache.has(key)
  }

  delete(key: K): boolean {
    return this.cache.delete(key)
  }

  clear(): void {
    this.cache.clear()
  }

  get size(): number {
    return this.cache.size
  }
}

/**
 * 内存使用监控
 */
export class MemoryMonitor {
  private callbacks: ((usage: number) => void)[] = []
  private interval: number | null = null
  private threshold: number = 80 // 80%

  constructor(threshold: number = 80) {
    this.threshold = threshold
  }

  /**
   * 开始监控
   */
  start(intervalMs: number = 5000): void {
    if (this.interval) return

    this.interval = window.setInterval(() => {
      const memoryInfo = getMemoryInfo()

      if (memoryInfo.usagePercent > this.threshold) {
        this.callbacks.forEach(callback => {
          try {
            callback(memoryInfo.usagePercent)
          } catch (error) {
            console.warn('Memory monitor callback error:', error)
          }
        })
      }
    }, intervalMs)
  }

  /**
   * 停止监控
   */
  stop(): void {
    if (this.interval) {
      clearInterval(this.interval)
      this.interval = null
    }
  }

  /**
   * 添加回调函数
   */
  onThresholdExceeded(callback: (usage: number) => void): void {
    this.callbacks.push(callback)
  }

  /**
   * 移除回调函数
   */
  removeCallback(callback: (usage: number) => void): void {
    const index = this.callbacks.indexOf(callback)
    if (index > -1) {
      this.callbacks.splice(index, 1)
    }
  }
}

// ============= 响应式处理 =============

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: number | null = null

  return (...args: Parameters<T>) => {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
    timeoutId = window.setTimeout(() => func(...args), delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): (...args: Parameters<T>) => void {
  let isThrottled = false
  let lastArgs: Parameters<T> | null = null

  return (...args: Parameters<T>) => {
    if (isThrottled) {
      lastArgs = args
      return
    }

    func(...args)
    isThrottled = true

    setTimeout(() => {
      isThrottled = false
      if (lastArgs) {
        func(...lastArgs)
        lastArgs = null
      }
    }, delay)
  }
}

/**
 * 响应式断点检测
 */
export function getBreakpoint(width: number): 'xs' | 'sm' | 'md' | 'lg' | 'xl' {
  if (width < 640) return 'xs'
  if (width < 768) return 'sm'
  if (width < 1024) return 'md'
  if (width < 1280) return 'lg'
  return 'xl'
}

/**
 * 根据断点调整词云配置
 */
export function adjustConfigForBreakpoint(
  baseConfig: Partial<WordCloudOptions>,
  breakpoint: ReturnType<typeof getBreakpoint>
): Partial<WordCloudOptions> {
  const config = { ...baseConfig }

  switch (breakpoint) {
    case 'xs':
      config.gridSize = 16
      config.rotationSteps = 1
      config.minSize = 8
      break
    case 'sm':
      config.gridSize = 14
      config.rotationSteps = 2
      config.minSize = 10
      break
    case 'md':
      config.gridSize = 12
      config.rotationSteps = 2
      config.minSize = 12
      break
    case 'lg':
      config.gridSize = 10
      config.rotationSteps = 3
      config.minSize = 14
      break
    case 'xl':
      config.gridSize = 8
      config.rotationSteps = 4
      config.minSize = 16
      break
  }

  return config
}

// ============= 错误处理和重试 =============

/**
 * 带重试的异步操作
 */
export async function withRetry<T>(
  operation: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: Error

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await operation()
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))

      if (attempt === maxRetries) {
        throw lastError
      }

      // 指数退避
      const backoffDelay = delay * Math.pow(2, attempt - 1)
      await new Promise(resolve => setTimeout(resolve, backoffDelay))
    }
  }

  throw lastError!
}

/**
 * 安全的渲染执行
 */
export async function safeRender(
  renderFn: () => Promise<void>,
  fallbackFn?: () => void,
  timeout: number = 10000
): Promise<boolean> {
  return new Promise((resolve) => {
    let completed = false

    // 设置超时
    const timer = setTimeout(() => {
      if (!completed) {
        completed = true
        console.warn('Render operation timed out')
        if (fallbackFn) fallbackFn()
        resolve(false)
      }
    }, timeout)

    // 执行渲染
    renderFn()
      .then(() => {
        if (!completed) {
          completed = true
          clearTimeout(timer)
          resolve(true)
        }
      })
      .catch((error) => {
        if (!completed) {
          completed = true
          clearTimeout(timer)
          console.error('Render operation failed:', error)
          if (fallbackFn) fallbackFn()
          resolve(false)
        }
      })
  })
}