/**
 * 大数据量词云优化处理
 * 专门处理大规模词云数据的性能优化
 */

import { ref, reactive, computed, onMounted, onUnmounted, type Ref } from 'vue'
import type { HotWordItem } from '@/types/statistics'
import {
  smartDataChunking,
  normalizeWordData,
  LRUCache,
  MemoryMonitor,
  type OptimizationContext,
  type DataChunk
} from '@/utils/wordCloudOptimizations'

// ============= 类型定义 =============

export interface LargeDataConfig {
  /** 最大同时处理的词语数量 */
  maxConcurrentWords: number
  /** 分批处理大小 */
  batchSize: number
  /** 虚拟化阈值 */
  virtualizationThreshold: number
  /** 缓存大小限制 */
  cacheSize: number
  /** 内存使用阈值 (百分比) */
  memoryThreshold: number
}

export interface DataProcessingState {
  /** 是否正在处理 */
  isProcessing: boolean
  /** 当前批次 */
  currentBatch: number
  /** 总批次数 */
  totalBatches: number
  /** 处理进度 */
  progress: number
  /** 错误信息 */
  error: string | null
  /** 处理开始时间 */
  startTime: number
  /** 处理结束时间 */
  endTime: number
}

export interface VirtualizationState {
  /** 是否启用虚拟化 */
  enabled: boolean
  /** 当前可视区域的词语 */
  visibleWords: HotWordItem[]
  /** 滚动位置 */
  scrollOffset: number
  /** 可视区域大小 */
  viewportSize: number
}

// ============= 主要的composable =============

export function useLargeDataOptimization(config: Partial<LargeDataConfig> = {}) {
  // ============= 配置 =============

  const defaultConfig: LargeDataConfig = {
    maxConcurrentWords: 300,
    batchSize: 50,
    virtualizationThreshold: 500,
    cacheSize: 100,
    memoryThreshold: 80
  }

  const settings = reactive({
    ...defaultConfig,
    ...config
  })

  // ============= 状态管理 =============

  /** 原始数据 */
  const rawData = ref<HotWordItem[]>([])

  /** 处理后的数据 */
  const processedData = ref<HotWordItem[]>([])

  /** 数据处理状态 */
  const processingState = reactive<DataProcessingState>({
    isProcessing: false,
    currentBatch: 0,
    totalBatches: 0,
    progress: 0,
    error: null,
    startTime: 0,
    endTime: 0
  })

  /** 虚拟化状态 */
  const virtualizationState = reactive<VirtualizationState>({
    enabled: false,
    visibleWords: [],
    scrollOffset: 0,
    viewportSize: 0
  })

  /** 数据缓存 */
  const dataCache = new LRUCache<string, HotWordItem[]>(settings.cacheSize)

  /** 内存监控器 */
  const memoryMonitor = new MemoryMonitor(settings.memoryThreshold)

  // ============= 计算属性 =============

  /** 是否需要虚拟化 */
  const shouldVirtualize = computed(() =>
    rawData.value.length > settings.virtualizationThreshold
  )

  /** 当前使用的数据 */
  const currentData = computed(() => {
    if (virtualizationState.enabled && shouldVirtualize.value) {
      return virtualizationState.visibleWords
    }
    return processedData.value
  })

  /** 数据统计 */
  const dataStats = computed(() => ({
    totalWords: rawData.value.length,
    processedWords: processedData.value.length,
    visibleWords: currentData.value.length,
    memoryUsage: getEstimatedMemoryUsage(),
    isVirtualized: virtualizationState.enabled,
    processingTime: processingState.endTime - processingState.startTime
  }))

  // ============= 核心方法 =============

  /**
   * 智能数据处理
   * 根据数据量和设备性能选择最优处理策略
   */
  const processLargeData = async (
    data: HotWordItem[],
    context?: Partial<OptimizationContext>
  ): Promise<HotWordItem[]> => {
    if (!data || data.length === 0) {
      return []
    }

    const cacheKey = generateCacheKey(data, context)
    const cachedResult = dataCache.get(cacheKey)
    if (cachedResult) {
      return cachedResult
    }

    try {
      processingState.isProcessing = true
      processingState.error = null
      processingState.startTime = Date.now()
      processingState.progress = 0

      rawData.value = data

      // 数据预处理和清洗
      const cleanedData = await cleanAndValidateData(data)

      // 根据数据量选择处理策略
      let result: HotWordItem[]

      if (cleanedData.length <= settings.maxConcurrentWords) {
        // 小数据量：直接处理
        result = await processSingleBatch(cleanedData)
      } else {
        // 大数据量：分批处理
        result = await processBatchedData(cleanedData, context)
      }

      // 缓存结果
      dataCache.set(cacheKey, result)
      processedData.value = result

      // 检查是否需要启用虚拟化
      if (shouldVirtualize.value) {
        enableVirtualization(result)
      }

      processingState.endTime = Date.now()
      processingState.progress = 100

      return result
    } catch (error) {
      processingState.error = error instanceof Error ? error.message : '数据处理失败'
      throw error
    } finally {
      processingState.isProcessing = false
    }
  }

  /**
   * 数据清洗和验证
   */
  const cleanAndValidateData = async (data: HotWordItem[]): Promise<HotWordItem[]> => {
    return new Promise((resolve) => {
      // 使用 requestIdleCallback 在浏览器空闲时处理
      const processInIdle = (deadline: IdleDeadline) => {
        const cleaned: HotWordItem[] = []
        let index = 0

        while (index < data.length && deadline.timeRemaining() > 1) {
          const item = data[index]

          // 数据验证
          if (
            item &&
            typeof item.text === 'string' &&
            item.text.trim().length > 0 &&
            typeof item.weight === 'number' &&
            item.weight >= 0 &&
            item.text.length <= 50 // 限制文本长度
          ) {
            cleaned.push({
              text: item.text.trim(),
              weight: Math.max(0, Math.min(100, item.weight)), // 权重限制在0-100
              extraData: item.extraData
            })
          }

          index++
        }

        if (index < data.length) {
          // 继续处理剩余数据
          requestIdleCallback(processInIdle)
        } else {
          // 处理完成，进行归一化
          const normalized = normalizeWordData(cleaned)
          resolve(normalized)
        }
      }

      if ('requestIdleCallback' in window) {
        requestIdleCallback(processInIdle)
      } else {
        // 回退方案
        setTimeout(() => {
          const cleaned = data.filter(item =>
            item &&
            typeof item.text === 'string' &&
            item.text.trim().length > 0 &&
            typeof item.weight === 'number' &&
            item.weight >= 0
          )
          resolve(normalizeWordData(cleaned))
        }, 0)
      }
    })
  }

  /**
   * 单批次处理
   */
  const processSingleBatch = async (data: HotWordItem[]): Promise<HotWordItem[]> => {
    processingState.currentBatch = 1
    processingState.totalBatches = 1

    // 简单排序和限制
    return data
      .sort((a, b) => b.weight - a.weight)
      .slice(0, settings.maxConcurrentWords)
  }

  /**
   * 分批处理大数据
   */
  const processBatchedData = async (
    data: HotWordItem[],
    context?: Partial<OptimizationContext>
  ): Promise<HotWordItem[]> => {
    const optimizationContext: OptimizationContext = {
      containerWidth: 800,
      containerHeight: 600,
      devicePixelRatio: window.devicePixelRatio || 1,
      availableMemory: getAvailableMemory(),
      currentFPS: 60,
      ...context
    }

    // 智能分片
    const chunks = smartDataChunking(data, optimizationContext)
    processingState.totalBatches = chunks.length

    const processedChunks: HotWordItem[] = []

    for (const chunk of chunks) {
      processingState.currentBatch = chunk.index + 1
      processingState.progress = ((chunk.index + 1) / chunks.length) * 100

      // 处理当前批次
      const processed = await processChunk(chunk)
      processedChunks.push(...processed)

      // 让出控制权，避免阻塞UI
      await new Promise(resolve => setTimeout(resolve, 1))

      // 检查内存使用情况
      if (shouldPauseForMemory()) {
        await waitForMemoryCleanup()
      }
    }

    // 最终排序和限制
    return processedChunks
      .sort((a, b) => b.weight - a.weight)
      .slice(0, settings.maxConcurrentWords)
  }

  /**
   * 处理单个数据块
   */
  const processChunk = async (chunk: DataChunk<HotWordItem>): Promise<HotWordItem[]> => {
    return new Promise((resolve) => {
      // 在下一个事件循环中处理，避免阻塞
      setTimeout(() => {
        const processed = chunk.data
          .filter(item => item.weight > 0) // 过滤无效权重
          .map(item => ({
            ...item,
            weight: Math.round(item.weight * 100) / 100 // 保留两位小数
          }))
          .sort((a, b) => b.weight - a.weight)

        resolve(processed)
      }, 0)
    })
  }

  /**
   * 启用虚拟化
   */
  const enableVirtualization = (data: HotWordItem[]) => {
    virtualizationState.enabled = true
    virtualizationState.visibleWords = data.slice(0, 100) // 初始显示100个

    console.log(`启用虚拟化: 总计${data.length}个词语，当前显示${virtualizationState.visibleWords.length}个`)
  }

  /**
   * 更新虚拟化可视区域
   */
  const updateVirtualization = (scrollOffset: number, viewportSize: number) => {
    if (!virtualizationState.enabled) return

    virtualizationState.scrollOffset = scrollOffset
    virtualizationState.viewportSize = viewportSize

    // 计算可视区域内的词语
    const itemsPerRow = Math.floor(viewportSize / 50) // 假设每个词占50px
    const startIndex = Math.floor(scrollOffset / 50) * itemsPerRow
    const endIndex = startIndex + itemsPerRow * Math.ceil(viewportSize / 50) + 20 // 预加载

    virtualizationState.visibleWords = processedData.value.slice(startIndex, endIndex)
  }

  // ============= 工具方法 =============

  /**
   * 生成缓存键
   */
  const generateCacheKey = (data: HotWordItem[], context?: any): string => {
    const dataHash = hashData(data.slice(0, 10)) // 使用前10个数据生成hash
    const contextHash = context ? JSON.stringify(context) : ''
    return `${dataHash}-${contextHash}-${settings.maxConcurrentWords}`
  }

  /**
   * 简单的数据哈希
   */
  const hashData = (data: HotWordItem[]): string => {
    return data
      .map(item => `${item.text}:${item.weight}`)
      .join('|')
      .slice(0, 50)
  }

  /**
   * 获取可用内存
   */
  const getAvailableMemory = (): number => {
    const memory = (performance as any).memory
    return memory ? memory.jsHeapSizeLimit - memory.usedJSHeapSize : 100 * 1024 * 1024
  }

  /**
   * 估算内存使用量
   */
  const getEstimatedMemoryUsage = (): number => {
    const dataSize = processedData.value.length * 100 // 假设每个词语占用100字节
    const cacheSize = dataCache.size * 1000 // 假设每个缓存项占用1KB
    return dataSize + cacheSize
  }

  /**
   * 是否应该暂停等待内存清理
   */
  const shouldPauseForMemory = (): boolean => {
    const memory = (performance as any).memory
    if (!memory) return false

    const usagePercent = (memory.usedJSHeapSize / memory.jsHeapSizeLimit) * 100
    return usagePercent > settings.memoryThreshold
  }

  /**
   * 等待内存清理
   */
  const waitForMemoryCleanup = async (): Promise<void> => {
    return new Promise((resolve) => {
      // 触发垃圾回收（如果支持）
      if (window.gc) {
        window.gc()
      }

      // 清理部分缓存
      const cacheKeys = Array.from(dataCache['cache'].keys())
      const toRemove = Math.floor(cacheKeys.length * 0.3)
      for (let i = 0; i < toRemove; i++) {
        dataCache.delete(cacheKeys[i])
      }

      setTimeout(resolve, 100)
    })
  }

  /**
   * 强制清理资源
   */
  const forceCleanup = () => {
    dataCache.clear()
    processedData.value = []
    virtualizationState.visibleWords = []

    if (window.gc) {
      window.gc()
    }
  }

  // ============= 生命周期管理 =============

  onMounted(() => {
    // 启动内存监控
    memoryMonitor.start()
    memoryMonitor.onThresholdExceeded((usage) => {
      console.warn(`内存使用率过高: ${usage.toFixed(1)}%`)
      forceCleanup()
    })
  })

  onUnmounted(() => {
    // 停止内存监控
    memoryMonitor.stop()

    // 清理资源
    forceCleanup()
  })

  // ============= 返回接口 =============

  return {
    // 状态
    rawData,
    processedData,
    currentData,
    processingState,
    virtualizationState,
    settings,

    // 计算属性
    shouldVirtualize,
    dataStats,

    // 方法
    processLargeData,
    updateVirtualization,
    forceCleanup,

    // 工具方法
    getEstimatedMemoryUsage,
    getAvailableMemory
  }
}

/**
 * 数据流式处理hooks
 * 用于处理实时数据流和增量更新
 */
export function useStreamingDataProcessor() {
  /** 数据流缓冲区 */
  const buffer = ref<HotWordItem[]>([])

  /** 是否正在流式处理 */
  const isStreaming = ref(false)

  /** 批次计数器 */
  const batchCounter = ref(0)

  /**
   * 添加数据到流
   */
  const addToStream = (items: HotWordItem[]) => {
    buffer.value.push(...items)

    // 当缓冲区达到一定大小时触发处理
    if (buffer.value.length >= 50) {
      processBatch()
    }
  }

  /**
   * 处理当前批次
   */
  const processBatch = async () => {
    if (isStreaming.value || buffer.value.length === 0) return

    isStreaming.value = true
    batchCounter.value++

    try {
      const currentBatch = buffer.value.splice(0, 50)
      // 这里可以调用其他处理函数
      console.log(`处理流式数据批次 #${batchCounter.value}:`, currentBatch.length, '个词语')

      // 模拟异步处理
      await new Promise(resolve => setTimeout(resolve, 10))
    } finally {
      isStreaming.value = false

      // 如果还有数据，继续处理
      if (buffer.value.length > 0) {
        setTimeout(processBatch, 10)
      }
    }
  }

  /**
   * 清空缓冲区
   */
  const clearBuffer = () => {
    buffer.value = []
  }

  return {
    buffer,
    isStreaming,
    batchCounter,
    addToStream,
    processBatch,
    clearBuffer
  }
}