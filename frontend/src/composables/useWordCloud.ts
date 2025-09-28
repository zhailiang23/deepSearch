/**
 * 词云渲染可组合函数
 * 基于wordcloud2.js实现词云的核心渲染逻辑
 */

import { ref, reactive, computed, onUnmounted, type Ref } from 'vue'
import type {
  WordCloudItem,
  WordCloudConfig,
  WordCloudState,
  WordCloudStats,
  WordCloudDimensions
} from '@/types/statistics'

// 渲染状态接口
interface WordCloudRenderState {
  isRendering: boolean
  hasError: boolean
  errorMessage: string | null
  renderProgress: number
  lastRenderTime: number
  isInitialized: boolean
  canvasReady: boolean
}

// 性能配置接口
interface WordCloudPerformanceConfig {
  enableDebounce: boolean
  debounceDelay: number
  enableThrottleResize: boolean
  throttleDelay: number
  maxRenderTime: number
  batchRenderSize: number
}

export function useWordCloud(
  canvasRef: Ref<HTMLCanvasElement | null>,
  initialConfig?: Partial<WordCloudConfig>
) {
  // 响应式状态
  const words = ref<WordCloudItem[]>([])
  const renderState = reactive<WordCloudRenderState>({
    isRendering: false,
    hasError: false,
    errorMessage: null,
    renderProgress: 0,
    lastRenderTime: 0,
    isInitialized: false,
    canvasReady: false
  })

  const dimensions = reactive<WordCloudDimensions>({
    width: 800,
    height: 600
  })

  const performanceConfig = reactive<WordCloudPerformanceConfig>({
    enableDebounce: true,
    debounceDelay: 300,
    enableThrottleResize: true,
    throttleDelay: 100,
    maxRenderTime: 10000,
    batchRenderSize: 100
  })

  // WordCloud实例引用
  let wordCloudInstance: any = null
  let renderTimeoutId: NodeJS.Timeout | null = null
  let resizeObserver: ResizeObserver | null = null

  // 计算属性
  const canRender = computed(() => {
    return (
      !renderState.isRendering &&
      renderState.canvasReady &&
      words.value.length > 0 &&
      dimensions.width > 0 &&
      dimensions.height > 0
    )
  })

  const mergedOptions = computed<WordCloudConfig>(() => {
    const defaultConfig: WordCloudConfig = {
      list: words.value,
      fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      fontWeight: 'bold',
      color: (word: string, weight: string | number) => {
        const weightNum = typeof weight === 'string' ? parseFloat(weight) : weight
        const ratio = Math.min(weightNum / 100, 1)
        const hue = 142.1
        const saturation = 76.2
        const lightness = 36.3 + (ratio * 20)
        return `hsl(${hue}, ${saturation}%, ${lightness}%)`
      },
      backgroundColor: 'transparent',
      minSize: 12,
      weightFactor: 1,
      gridSize: 8,
      origin: [dimensions.width / 2, dimensions.height / 2],
      rotationSteps: 4,
      minRotation: -Math.PI / 4,
      maxRotation: Math.PI / 4,
      rotateRatio: 0.3,
      shape: 'circle',
      ellipticity: 0.65,
      clearCanvas: true,
      drawOutOfBound: false,
      shrinkToFit: true,
      wait: 0,
      abortThreshold: performanceConfig.maxRenderTime,
      shuffle: true
    }

    return {
      ...defaultConfig,
      ...initialConfig,
      list: words.value,
      origin: [dimensions.width / 2, dimensions.height / 2]
    }
  })

  // 工具函数：检查wordcloud库是否可用
  const checkWordCloudAvailability = async (): Promise<boolean> => {
    try {
      const WordCloudModule = await import('wordcloud')
      return !!(WordCloudModule.default || WordCloudModule)
    } catch (error) {
      console.error('WordCloud库检查失败:', error)
      return false
    }
  }

  // 动态导入wordcloud库
  const loadWordCloudLibrary = async () => {
    try {
      const WordCloudModule = await import('wordcloud')
      const WordCloud = WordCloudModule.default || WordCloudModule

      // 检查库是否支持当前环境
      if (typeof WordCloud.isSupported !== 'undefined' && !WordCloud.isSupported) {
        throw new Error('当前浏览器不支持WordCloud')
      }

      return WordCloud
    } catch (error) {
      console.error('WordCloud库加载失败:', error)
      throw new Error('词云库加载失败，请检查网络连接')
    }
  }

  // 初始化Canvas
  const initializeCanvas = (): boolean => {
    if (!canvasRef.value) {
      renderState.hasError = true
      renderState.errorMessage = 'Canvas元素未找到'
      return false
    }

    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d')

    if (!ctx) {
      renderState.hasError = true
      renderState.errorMessage = 'Canvas 2D上下文获取失败'
      return false
    }

    // 设置Canvas尺寸
    canvas.width = dimensions.width
    canvas.height = dimensions.height

    // 高DPI显示器支持
    const devicePixelRatio = window.devicePixelRatio || 1
    if (devicePixelRatio > 1) {
      const rect = canvas.getBoundingClientRect()
      canvas.width = rect.width * devicePixelRatio
      canvas.height = rect.height * devicePixelRatio
      canvas.style.width = rect.width + 'px'
      canvas.style.height = rect.height + 'px'
      ctx.scale(devicePixelRatio, devicePixelRatio)
    }

    renderState.canvasReady = true
    return true
  }

  // 主渲染方法
  const renderWordCloud = async (): Promise<void> => {
    if (!canRender.value) {
      console.warn('词云渲染条件不满足')
      return
    }

    try {
      renderState.isRendering = true
      renderState.hasError = false
      renderState.errorMessage = null
      renderState.renderProgress = 0

      const startTime = Date.now()

      // 初始化Canvas
      if (!initializeCanvas()) {
        throw new Error('Canvas初始化失败')
      }

      // 加载WordCloud库
      const WordCloud = await loadWordCloudLibrary()

      // 准备配置
      const config = {
        ...mergedOptions.value,
        // 添加进度回调（如果支持）
        progress: (progress: number) => {
          renderState.renderProgress = progress * 100
        }
      }

      // 设置渲染超时
      const renderPromise = new Promise<void>((resolve, reject) => {
        renderTimeoutId = setTimeout(() => {
          reject(new Error('词云渲染超时'))
        }, performanceConfig.maxRenderTime)

        try {
          // 开始渲染
          wordCloudInstance = WordCloud(canvasRef.value, config)

          // 监听完成事件
          if (canvasRef.value) {
            const onComplete = () => {
              if (renderTimeoutId) {
                clearTimeout(renderTimeoutId)
                renderTimeoutId = null
              }
              resolve()
            }

            const onError = (error: any) => {
              if (renderTimeoutId) {
                clearTimeout(renderTimeoutId)
                renderTimeoutId = null
              }
              reject(error)
            }

            canvasRef.value.addEventListener('wordclouddrawn', onComplete, { once: true })
            canvasRef.value.addEventListener('wordcloudabort', onError, { once: true })
          } else {
            // 如果没有Canvas事件支持，使用简单的延时
            setTimeout(resolve, 1000)
          }
        } catch (error) {
          if (renderTimeoutId) {
            clearTimeout(renderTimeoutId)
            renderTimeoutId = null
          }
          reject(error)
        }
      })

      await renderPromise

      // 更新渲染状态
      renderState.lastRenderTime = Date.now() - startTime
      renderState.renderProgress = 100

    } catch (error) {
      console.error('词云渲染失败:', error)
      renderState.hasError = true
      renderState.errorMessage = error instanceof Error ? error.message : '渲染失败'
      throw error
    } finally {
      renderState.isRendering = false
      if (renderTimeoutId) {
        clearTimeout(renderTimeoutId)
        renderTimeoutId = null
      }
    }
  }

  // 停止渲染
  const stopRendering = (): void => {
    if (renderTimeoutId) {
      clearTimeout(renderTimeoutId)
      renderTimeoutId = null
    }

    if (wordCloudInstance && typeof wordCloudInstance.stop === 'function') {
      wordCloudInstance.stop()
    }

    renderState.isRendering = false
  }

  // 清空画布
  const clearCanvas = (): void => {
    if (canvasRef.value) {
      const ctx = canvasRef.value.getContext('2d')
      if (ctx) {
        ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
      }
    }
  }

  // 更新词语数据
  const updateWords = async (newWords: WordCloudItem[]): Promise<void> => {
    words.value = [...newWords]
    if (canRender.value) {
      await renderWordCloud()
    }
  }

  // 更新尺寸
  const updateDimensions = async (width: number, height: number): Promise<void> => {
    dimensions.width = width
    dimensions.height = height

    if (canRender.value) {
      await renderWordCloud()
    }
  }

  // 防抖处理的渲染方法
  let debounceTimeoutId: NodeJS.Timeout | null = null
  const debouncedRender = async (): Promise<void> => {
    if (!performanceConfig.enableDebounce) {
      return renderWordCloud()
    }

    if (debounceTimeoutId) {
      clearTimeout(debounceTimeoutId)
    }

    return new Promise((resolve, reject) => {
      debounceTimeoutId = setTimeout(async () => {
        try {
          await renderWordCloud()
          resolve()
        } catch (error) {
          reject(error)
        }
      }, performanceConfig.debounceDelay)
    })
  }

  // 设置Canvas尺寸监听
  const setupResizeObserver = (): void => {
    if (!canvasRef.value || !window.ResizeObserver) return

    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect
        if (width > 0 && height > 0) {
          updateDimensions(width, height)
        }
      }
    })

    const container = canvasRef.value.parentElement
    if (container) {
      resizeObserver.observe(container)
    }
  }

  // 获取渲染统计信息
  const getRenderStats = (): WordCloudStats => {
    return {
      totalWords: words.value.length,
      renderedWords: words.value.length, // 简化处理
      maxWeight: words.value.length > 0 ? Math.max(...words.value.map(item => Array.isArray(item) ? item[1] : 0)) : 0,
      minWeight: words.value.length > 0 ? Math.min(...words.value.map(item => Array.isArray(item) ? item[1] : 0)) : 0,
      averageWeight: words.value.length > 0 ? words.value.reduce((sum, item) => sum + (Array.isArray(item) ? item[1] : 0), 0) / words.value.length : 0,
      renderTime: renderState.lastRenderTime
    }
  }

  // 清理资源
  const cleanup = (): void => {
    stopRendering()

    if (debounceTimeoutId) {
      clearTimeout(debounceTimeoutId)
      debounceTimeoutId = null
    }

    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }

    wordCloudInstance = null
  }

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })

  // 返回API
  return {
    // 状态
    words: readonly(words),
    renderState: readonly(renderState),
    dimensions: readonly(dimensions),
    performanceConfig,

    // 计算属性
    canRender,
    mergedOptions,

    // 方法
    renderWordCloud,
    stopRendering,
    clearCanvas,
    updateWords,
    updateDimensions,
    debouncedRender,
    setupResizeObserver,
    getRenderStats,
    cleanup,

    // 工具方法
    checkWordCloudAvailability,
    initializeCanvas
  }
}