import { ref, reactive, computed, onMounted, onUnmounted, nextTick, readonly, type Ref } from 'vue'
import type {
  HotWordItem,
  WordCloudOptions,
  WordCloudTheme,
  WordCloudDimensions,
  WordCloudRenderState,
  WordCloudEvents,
  WordCloudPerformanceConfig
} from '@/types/statistics'

// WordCloud库的类型声明
declare global {
  interface Window {
    WordCloud: any
  }
}

// 动态导入wordcloud库
let wordcloudLib: any = null
const initWordCloud = async () => {
  if (!wordcloudLib && typeof window !== 'undefined') {
    try {
      wordcloudLib = await import('wordcloud')
      // 确保全局可用
      window.WordCloud = wordcloudLib.default || wordcloudLib
    } catch (error) {
      console.error('Failed to load wordcloud library:', error)
    }
  }
  return wordcloudLib
}

/**
 * 词云可组合函数
 * 封装词云渲染逻辑，提供响应式状态管理和性能优化
 */
export function useWordCloud(
  canvasElement: Ref<HTMLCanvasElement | null>,
  options: Partial<WordCloudOptions> = {},
  events: WordCloudEvents = {}
) {
  // ============= 响应式状态 =============

  /** 词云数据 */
  const words = ref<HotWordItem[]>([])

  /** 渲染状态 */
  const renderState = reactive<WordCloudRenderState>({
    isRendering: false,
    progress: 0,
    error: null
  })

  /** 画布尺寸 */
  const dimensions = reactive<WordCloudDimensions>({
    width: 800,
    height: 600,
    devicePixelRatio: window.devicePixelRatio || 1
  })

  /** 性能配置 */
  const performanceConfig = reactive<WordCloudPerformanceConfig>({
    batchSize: 50,
    renderDelay: 10,
    maxRenderTime: 5000,
    debounceDelay: 300
  })

  // ============= 默认配置 =============

  /** 默认词云主题（淡绿色系） */
  const defaultTheme: WordCloudTheme = {
    name: 'light-green',
    colors: {
      primary: [
        '#10b981', // emerald-500
        '#059669', // emerald-600
        '#047857', // emerald-700
        '#065f46', // emerald-800
        '#064e3b'  // emerald-900
      ],
      secondary: [
        '#34d399', // emerald-400
        '#6ee7b7', // emerald-300
        '#a7f3d0', // emerald-200
        '#d1fae5'  // emerald-100
      ],
      background: '#ffffff'
    },
    fonts: {
      family: 'Inter, system-ui, sans-serif',
      weight: '600'
    }
  }

  /** 默认wordcloud2.js配置 */
  const defaultOptions: WordCloudOptions = {
    fontFamily: defaultTheme.fonts.family,
    fontWeight: defaultTheme.fonts.weight,
    backgroundColor: defaultTheme.colors.background,
    color: (word: string, weight: number, fontSize: number, distance: number, theta: number) => {
      // 根据权重选择颜色，权重越高颜色越深
      const colors = defaultTheme.colors.primary
      const index = Math.min(Math.floor((weight / 100) * colors.length), colors.length - 1)
      return colors[index] || colors[0]
    },
    rotateRatio: 0.3,
    rotationSteps: 2,
    minRotation: -Math.PI / 4,
    maxRotation: Math.PI / 4,
    gridSize: 8,
    weightFactor: (size: number) => Math.pow(size, 2.3) / 600,
    clearCanvas: true,
    shrinkToFit: true,
    hover: (item: any, dimension: any, event: Event) => {
      if (events.onWordHover) {
        const word: HotWordItem = { text: item[0], weight: item[1], extraData: item[2] }
        events.onWordHover(word, event)
      }
    },
    click: (item: any, dimension: any, event: Event) => {
      if (events.onWordClick) {
        const word: HotWordItem = { text: item[0], weight: item[1], extraData: item[2] }
        events.onWordClick(word, event)
      }
    }
  }

  // ============= 计算属性 =============

  /** 合并后的配置选项 */
  const mergedOptions = computed(() => ({
    ...defaultOptions,
    ...options,
    width: dimensions.width,
    height: dimensions.height
  }))

  /** 是否可以渲染 */
  const canRender = computed(() =>
    canvasElement.value &&
    words.value.length > 0 &&
    !renderState.isRendering &&
    window.WordCloud
  )

  /** 转换为wordcloud2.js格式的数据 */
  const wordCloudData = computed(() =>
    words.value.map(word => [word.text, word.weight, word.extraData])
  )

  // ============= 核心方法 =============

  /** 检查WordCloud库是否可用 */
  const checkWordCloudAvailability = async (): Promise<boolean> => {
    if (typeof window !== 'undefined' && window.WordCloud) {
      return true
    }

    // 尝试动态导入
    try {
      await initWordCloud()
      return !!window.WordCloud
    } catch (error) {
      console.warn('WordCloud library not found:', error)
      return false
    }
  }

  /** 初始化画布 */
  const initializeCanvas = () => {
    if (!canvasElement.value) return

    const canvas = canvasElement.value
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // 设置画布尺寸
    canvas.width = dimensions.width * dimensions.devicePixelRatio
    canvas.height = dimensions.height * dimensions.devicePixelRatio
    canvas.style.width = `${dimensions.width}px`
    canvas.style.height = `${dimensions.height}px`

    // 缩放上下文以适应高DPI显示器
    ctx.scale(dimensions.devicePixelRatio, dimensions.devicePixelRatio)
  }

  /** 渲染词云 */
  const renderWordCloud = async (): Promise<void> => {
    if (!canRender.value) {
      renderState.error = '无法渲染：缺少必要条件'
      return
    }

    try {
      renderState.isRendering = true
      renderState.progress = 0
      renderState.error = null
      renderState.startTime = Date.now()

      if (events.onRenderStart) {
        events.onRenderStart()
      }

      // 初始化画布
      initializeCanvas()

      await nextTick()

      // 创建渲染配置
      const renderOptions = {
        ...mergedOptions.value,
        list: wordCloudData.value,
        // 添加进度回调
        drawProgress: (progress: number) => {
          renderState.progress = Math.min(progress * 100, 100)
        }
      }

      // 执行渲染
      await new Promise<void>((resolve, reject) => {
        const canvas = canvasElement.value!

        // 监听渲染事件
        const handleStart = () => {
          renderState.isRendering = true
        }

        const handleComplete = () => {
          renderState.isRendering = false
          renderState.progress = 100
          renderState.endTime = Date.now()

          canvas.removeEventListener('wordcloudstart', handleStart)
          canvas.removeEventListener('wordclouddrawn', handleComplete)
          canvas.removeEventListener('wordcloudabort', handleAbort)

          if (events.onRenderComplete) {
            events.onRenderComplete()
          }

          resolve()
        }

        const handleAbort = () => {
          renderState.isRendering = false
          renderState.error = '渲染被中止'

          canvas.removeEventListener('wordcloudstart', handleStart)
          canvas.removeEventListener('wordclouddrawn', handleComplete)
          canvas.removeEventListener('wordcloudabort', handleAbort)

          reject(new Error('渲染被中止'))
        }

        canvas.addEventListener('wordcloudstart', handleStart)
        canvas.addEventListener('wordclouddrawn', handleComplete)
        canvas.addEventListener('wordcloudabort', handleAbort)

        // 设置超时
        const timeout = setTimeout(() => {
          window.WordCloud.stop()
          reject(new Error('渲染超时'))
        }, performanceConfig.maxRenderTime)

        // 开始渲染
        try {
          window.WordCloud(canvas, renderOptions)
          clearTimeout(timeout)
        } catch (error) {
          clearTimeout(timeout)
          reject(error)
        }
      })

    } catch (error) {
      renderState.isRendering = false
      renderState.error = error instanceof Error ? error.message : '渲染失败'

      if (events.onRenderError) {
        events.onRenderError(renderState.error)
      }

      throw error
    }
  }

  /** 停止渲染 */
  const stopRendering = () => {
    if (window.WordCloud && window.WordCloud.stop) {
      window.WordCloud.stop()
    }
    renderState.isRendering = false
  }

  /** 清空画布 */
  const clearCanvas = () => {
    if (!canvasElement.value) return

    const ctx = canvasElement.value.getContext('2d')
    if (ctx) {
      ctx.clearRect(0, 0, dimensions.width, dimensions.height)
    }
  }

  /** 更新词云数据 */
  const updateWords = async (newWords: HotWordItem[]) => {
    words.value = [...newWords]

    if (canRender.value) {
      await renderWordCloud()
    }
  }

  /** 更新画布尺寸 */
  const updateDimensions = async (width: number, height: number) => {
    dimensions.width = width
    dimensions.height = height

    if (canRender.value && words.value.length > 0) {
      await renderWordCloud()
    }
  }

  /** 响应式尺寸调整 */
  const handleResize = () => {
    if (!canvasElement.value) return

    const container = canvasElement.value.parentElement
    if (container) {
      const rect = container.getBoundingClientRect()
      updateDimensions(rect.width, rect.height)
    }
  }

  // 防抖处理的尺寸调整
  let resizeTimeout: number | null = null
  const debouncedHandleResize = () => {
    if (resizeTimeout) {
      clearTimeout(resizeTimeout)
    }
    resizeTimeout = window.setTimeout(handleResize, performanceConfig.debounceDelay)
  }

  // ============= 生命周期 =============

  onMounted(async () => {
    // 检查WordCloud库可用性
    const isAvailable = await checkWordCloudAvailability()
    if (!isAvailable) {
      renderState.error = 'WordCloud库未加载'
      return
    }

    // 监听窗口尺寸变化
    window.addEventListener('resize', debouncedHandleResize)

    // 初始化画布尺寸
    nextTick(() => {
      handleResize()
    })
  })

  onUnmounted(() => {
    // 停止渲染
    stopRendering()

    // 清理事件监听器
    window.removeEventListener('resize', debouncedHandleResize)

    // 清理定时器
    if (resizeTimeout) {
      clearTimeout(resizeTimeout)
    }
  })

  // ============= 返回接口 =============

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
    initializeCanvas,

    // 工具方法
    checkWordCloudAvailability
  }
}

/**
 * 词云主题相关的可组合函数
 */
export function useWordCloudTheme() {
  /** 当前主题 */
  const currentTheme = ref<WordCloudTheme | null>(null)

  /** 预定义主题 */
  const themes: WordCloudTheme[] = [
    {
      name: 'light-green',
      colors: {
        primary: ['#10b981', '#059669', '#047857', '#065f46', '#064e3b'],
        secondary: ['#34d399', '#6ee7b7', '#a7f3d0', '#d1fae5'],
        background: '#ffffff'
      },
      fonts: {
        family: 'Inter, system-ui, sans-serif',
        weight: '600'
      }
    },
    {
      name: 'dark-green',
      colors: {
        primary: ['#34d399', '#10b981', '#059669', '#047857', '#065f46'],
        secondary: ['#6ee7b7', '#a7f3d0', '#d1fae5', '#ecfdf5'],
        background: '#064e3b'
      },
      fonts: {
        family: 'Inter, system-ui, sans-serif',
        weight: '600'
      }
    }
  ]

  /** 设置主题 */
  const setTheme = (themeName: string) => {
    const theme = themes.find(t => t.name === themeName)
    if (theme) {
      currentTheme.value = theme
    }
  }

  /** 获取主题颜色函数 */
  const getColorFunction = (theme: WordCloudTheme) => {
    return (word: string, weight: number, fontSize: number, distance: number, theta: number) => {
      const colors = theme.colors.primary
      const index = Math.min(Math.floor((weight / 100) * colors.length), colors.length - 1)
      return colors[index] || colors[0]
    }
  }

  return {
    currentTheme: readonly(currentTheme),
    themes,
    setTheme,
    getColorFunction
  }
}