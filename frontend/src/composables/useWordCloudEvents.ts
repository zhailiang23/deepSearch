/**
 * 词云事件管理可组合函数
 * 提供词云交互事件的统一管理，包括点击、悬停、缩放等功能
 */

import { ref, reactive, computed, onMounted, onUnmounted, type Ref } from 'vue'
import type {
  WordCloudItem,
  WordCloudDimension,
  WordCloudInteractionConfig,
  WordCloudEvents
} from '@/types/wordCloudTypes'

// 事件类型定义
export interface WordCloudEventHandlers {
  onWordClick?: (word: string, item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => void
  onWordHover?: (word: string | null, item: WordCloudItem | null, dimension: WordCloudDimension | null, event: MouseEvent | null) => void
  onCanvasClick?: (event: MouseEvent) => void
  onCanvasHover?: (event: MouseEvent) => void
  onZoom?: (scale: number, event: WheelEvent) => void
  onDrag?: (deltaX: number, deltaY: number, event: MouseEvent) => void
  onRenderStart?: () => void
  onRenderComplete?: () => void
  onRenderError?: (error: Error) => void
}

// 交互状态接口
export interface WordCloudInteractionState {
  isHovering: boolean
  hoveredWord: string | null
  hoveredItem: WordCloudItem | null
  isClicking: boolean
  clickedWord: string | null
  isDragging: boolean
  dragStartX: number
  dragStartY: number
  currentScale: number
  canvasOffset: { x: number; y: number }
  lastInteractionTime: number
}

// 缩放配置
export interface ZoomConfig {
  enabled: boolean
  minScale: number
  maxScale: number
  zoomSpeed: number
  zoomStep: number
}

// 拖拽配置
export interface DragConfig {
  enabled: boolean
  threshold: number
  inertia: boolean
  bounds?: { left: number; top: number; right: number; bottom: number }
}

export function useWordCloudEvents(
  canvasRef: Ref<HTMLCanvasElement | null>,
  config: Ref<WordCloudInteractionConfig>,
  handlers: WordCloudEventHandlers = {}
) {
  // 响应式状态
  const interactionState = reactive<WordCloudInteractionState>({
    isHovering: false,
    hoveredWord: null,
    hoveredItem: null,
    isClicking: false,
    clickedWord: null,
    isDragging: false,
    dragStartX: 0,
    dragStartY: 0,
    currentScale: 1,
    canvasOffset: { x: 0, y: 0 },
    lastInteractionTime: 0
  })

  // 缩放配置
  const zoomConfig = ref<ZoomConfig>({
    enabled: true,
    minScale: 0.1,
    maxScale: 5,
    zoomSpeed: 0.1,
    zoomStep: 0.1
  })

  // 拖拽配置
  const dragConfig = ref<DragConfig>({
    enabled: true,
    threshold: 5,
    inertia: true
  })

  // 事件监听器引用
  const eventListeners = new Map<string, EventListener>()

  // 计算属性
  const isInteractive = computed(() => config.value.enableClick || config.value.enableHover)
  const canZoom = computed(() => zoomConfig.value.enabled)
  const canDrag = computed(() => dragConfig.value.enabled)

  // 工具函数：获取鼠标相对画布的坐标
  const getCanvasCoordinates = (event: MouseEvent) => {
    if (!canvasRef.value) return { x: 0, y: 0 }

    const rect = canvasRef.value.getBoundingClientRect()
    const scaleX = canvasRef.value.width / rect.width
    const scaleY = canvasRef.value.height / rect.height

    return {
      x: (event.clientX - rect.left) * scaleX,
      y: (event.clientY - rect.top) * scaleY
    }
  }

  // 工具函数：检测词语点击位置（需要与wordcloud2.js数据结合）
  const getWordAtPosition = (x: number, y: number): { word: string; item: WordCloudItem; dimension: WordCloudDimension } | null => {
    // 这里需要与wordcloud2.js的内部数据结合
    // 由于wordcloud2.js没有直接提供位置检测API，我们需要通过其他方式实现
    // 暂时返回null，具体实现需要在集成时完成
    return null
  }

  // 词语点击事件处理
  const createWordClickHandler = () => {
    return (item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => {
      if (!config.value.enableClick) return

      const word = Array.isArray(item) ? item[0] : String(item)

      // 更新状态
      interactionState.isClicking = true
      interactionState.clickedWord = word
      interactionState.lastInteractionTime = Date.now()

      // 执行点击动作
      switch (config.value.clickAction) {
        case 'search':
          // 触发搜索事件
          handlers.onWordClick?.(word, item, dimension, event)
          break
        case 'filter':
          // 触发筛选事件
          handlers.onWordClick?.(word, item, dimension, event)
          break
        case 'custom':
          // 自定义处理
          handlers.onWordClick?.(word, item, dimension, event)
          break
        default:
          handlers.onWordClick?.(word, item, dimension, event)
      }

      // 重置点击状态
      setTimeout(() => {
        interactionState.isClicking = false
        interactionState.clickedWord = null
      }, 150)
    }
  }

  // 词语悬停事件处理
  const createWordHoverHandler = () => {
    return (item: WordCloudItem | null, dimension: WordCloudDimension | null, event: MouseEvent | null) => {
      if (!config.value.enableHover) return

      const word = item && Array.isArray(item) ? item[0] : item ? String(item) : null

      // 更新状态
      interactionState.isHovering = !!item
      interactionState.hoveredWord = word
      interactionState.hoveredItem = item
      interactionState.lastInteractionTime = Date.now()

      // 应用悬停效果
      if (item && canvasRef.value) {
        const canvas = canvasRef.value
        const ctx = canvas.getContext('2d')

        if (ctx && dimension) {
          switch (config.value.hoverEffect) {
            case 'highlight':
              // 高亮效果（需要重新绘制）
              break
            case 'shadow':
              // 阴影效果
              break
            case 'scale':
              // 缩放效果
              break
          }
        }
      }

      // 触发悬停回调
      handlers.onWordHover?.(word, item, dimension, event)
    }
  }

  // Canvas点击事件处理
  const handleCanvasClick = (event: MouseEvent) => {
    if (!isInteractive.value) return

    const coords = getCanvasCoordinates(event)
    const wordInfo = getWordAtPosition(coords.x, coords.y)

    if (wordInfo) {
      // 点击到了词语
      const clickHandler = createWordClickHandler()
      clickHandler(wordInfo.item, wordInfo.dimension, event)
    } else {
      // 点击到了空白区域
      handlers.onCanvasClick?.(event)
    }
  }

  // Canvas鼠标移动事件处理
  const handleCanvasMouseMove = (event: MouseEvent) => {
    if (!config.value.enableHover) return

    const coords = getCanvasCoordinates(event)
    const wordInfo = getWordAtPosition(coords.x, coords.y)

    if (wordInfo) {
      // 悬停在词语上
      if (wordInfo.word !== interactionState.hoveredWord) {
        const hoverHandler = createWordHoverHandler()
        hoverHandler(wordInfo.item, wordInfo.dimension, event)
      }
    } else {
      // 离开词语
      if (interactionState.hoveredWord) {
        const hoverHandler = createWordHoverHandler()
        hoverHandler(null, null, null)
      }
    }

    handlers.onCanvasHover?.(event)
  }

  // 缩放事件处理
  const handleWheel = (event: WheelEvent) => {
    if (!canZoom.value) return

    event.preventDefault()

    const delta = event.deltaY > 0 ? -zoomConfig.value.zoomStep : zoomConfig.value.zoomStep
    const newScale = Math.max(
      zoomConfig.value.minScale,
      Math.min(zoomConfig.value.maxScale, interactionState.currentScale + delta)
    )

    if (newScale !== interactionState.currentScale) {
      interactionState.currentScale = newScale

      // 应用缩放变换
      if (canvasRef.value) {
        const ctx = canvasRef.value.getContext('2d')
        if (ctx) {
          ctx.save()
          ctx.scale(newScale, newScale)
          ctx.restore()
        }
      }

      handlers.onZoom?.(newScale, event)
    }
  }

  // 拖拽事件处理
  let isDragging = false
  let dragStartTime = 0

  const handleMouseDown = (event: MouseEvent) => {
    if (!canDrag.value) return

    isDragging = true
    dragStartTime = Date.now()
    interactionState.isDragging = true
    interactionState.dragStartX = event.clientX
    interactionState.dragStartY = event.clientY

    // 添加临时事件监听器
    const handleMouseMove = (moveEvent: MouseEvent) => {
      if (!isDragging) return

      const deltaX = moveEvent.clientX - interactionState.dragStartX
      const deltaY = moveEvent.clientY - interactionState.dragStartY

      // 检查是否超过拖拽阈值
      if (Math.abs(deltaX) > dragConfig.value.threshold || Math.abs(deltaY) > dragConfig.value.threshold) {
        interactionState.canvasOffset.x += deltaX
        interactionState.canvasOffset.y += deltaY

        interactionState.dragStartX = moveEvent.clientX
        interactionState.dragStartY = moveEvent.clientY

        handlers.onDrag?.(deltaX, deltaY, moveEvent)
      }
    }

    const handleMouseUp = () => {
      isDragging = false
      interactionState.isDragging = false

      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  // WordCloud生命周期事件处理
  const handleWordCloudStart = () => {
    handlers.onRenderStart?.()
  }

  const handleWordCloudDrawn = () => {
    handlers.onRenderComplete?.()
  }

  const handleWordCloudError = (event: Event) => {
    const error = new Error('WordCloud rendering error')
    handlers.onRenderError?.(error)
  }

  // 事件监听器注册
  const registerEventListeners = () => {
    if (!canvasRef.value) return

    const canvas = canvasRef.value

    // 基础交互事件
    if (isInteractive.value) {
      const clickListener = handleCanvasClick
      const mouseMoveListener = handleCanvasMouseMove

      canvas.addEventListener('click', clickListener)
      canvas.addEventListener('mousemove', mouseMoveListener)

      eventListeners.set('click', clickListener)
      eventListeners.set('mousemove', mouseMoveListener)
    }

    // 缩放事件
    if (canZoom.value) {
      const wheelListener = handleWheel
      canvas.addEventListener('wheel', wheelListener, { passive: false })
      eventListeners.set('wheel', wheelListener)
    }

    // 拖拽事件
    if (canDrag.value) {
      const mouseDownListener = handleMouseDown
      canvas.addEventListener('mousedown', mouseDownListener)
      eventListeners.set('mousedown', mouseDownListener)
    }

    // WordCloud生命周期事件
    const startListener = handleWordCloudStart
    const drawnListener = handleWordCloudDrawn
    const errorListener = handleWordCloudError

    canvas.addEventListener('wordcloudstart', startListener)
    canvas.addEventListener('wordclouddrawn', drawnListener)
    canvas.addEventListener('wordcloudabort', errorListener)

    eventListeners.set('wordcloudstart', startListener)
    eventListeners.set('wordclouddrawn', drawnListener)
    eventListeners.set('wordcloudabort', errorListener)
  }

  // 事件监听器清理
  const unregisterEventListeners = () => {
    if (!canvasRef.value) return

    const canvas = canvasRef.value

    eventListeners.forEach((listener, eventType) => {
      canvas.removeEventListener(eventType, listener)
    })

    eventListeners.clear()
  }

  // 创建wordcloud2.js兼容的事件处理器
  const createWordCloudCallbacks = () => {
    return {
      hover: createWordHoverHandler(),
      click: createWordClickHandler()
    }
  }

  // 重置交互状态
  const resetInteractionState = () => {
    interactionState.isHovering = false
    interactionState.hoveredWord = null
    interactionState.hoveredItem = null
    interactionState.isClicking = false
    interactionState.clickedWord = null
    interactionState.isDragging = false
    interactionState.currentScale = 1
    interactionState.canvasOffset = { x: 0, y: 0 }
  }

  // 更新配置
  const updateZoomConfig = (newConfig: Partial<ZoomConfig>) => {
    Object.assign(zoomConfig.value, newConfig)
  }

  const updateDragConfig = (newConfig: Partial<DragConfig>) => {
    Object.assign(dragConfig.value, newConfig)
  }

  // 生命周期钩子
  onMounted(() => {
    registerEventListeners()
  })

  onUnmounted(() => {
    unregisterEventListeners()
  })

  // 返回API
  return {
    // 状态
    interactionState: readonly(interactionState),
    zoomConfig,
    dragConfig,

    // 计算属性
    isInteractive,
    canZoom,
    canDrag,

    // 方法
    registerEventListeners,
    unregisterEventListeners,
    createWordCloudCallbacks,
    resetInteractionState,
    updateZoomConfig,
    updateDragConfig,
    getCanvasCoordinates,

    // 工具函数
    handleCanvasClick,
    handleCanvasMouseMove,
    handleWheel,
    handleMouseDown
  }
}

// 导出工具函数
export function createDefaultInteractionConfig(): WordCloudInteractionConfig {
  return {
    enableHover: true,
    enableClick: true,
    hoverEffect: 'highlight',
    clickAction: 'search'
  }
}

export function createDefaultZoomConfig(): ZoomConfig {
  return {
    enabled: true,
    minScale: 0.1,
    maxScale: 5,
    zoomSpeed: 0.1,
    zoomStep: 0.1
  }
}

export function createDefaultDragConfig(): DragConfig {
  return {
    enabled: true,
    threshold: 5,
    inertia: true
  }
}