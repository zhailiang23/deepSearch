/**
 * 响应式词云处理
 * 提供完整的响应式设计支持，包括断点管理、自适应布局、触摸支持等
 */

import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, readonly } from 'vue'
import type { Ref } from 'vue'
import { useResizeObserver, useEventListener, useDeviceOrientation } from '@vueuse/core'
import type { HotWordItem, WordCloudOptions } from '@/types/statistics'
import {
  getBreakpoint,
  adjustConfigForBreakpoint,
  debounce,
  throttle
} from '@/utils/wordCloudOptimizations'

// ============= 类型定义 =============

export type Breakpoint = 'xs' | 'sm' | 'md' | 'lg' | 'xl'

export interface ResponsiveConfig {
  /** 断点配置 */
  breakpoints: Record<Breakpoint, number>
  /** 是否启用触摸支持 */
  enableTouch: boolean
  /** 是否启用方向感知 */
  enableOrientation: boolean
  /** 防抖延迟 */
  debounceDelay: number
  /** 节流延迟 */
  throttleDelay: number
}

export interface ViewportInfo {
  /** 当前宽度 */
  width: number
  /** 当前高度 */
  height: number
  /** 设备像素比 */
  pixelRatio: number
  /** 当前断点 */
  breakpoint: Breakpoint
  /** 是否移动设备 */
  isMobile: boolean
  /** 是否平板设备 */
  isTablet: boolean
  /** 是否桌面设备 */
  isDesktop: boolean
  /** 是否横屏 */
  isLandscape: boolean
  /** 是否竖屏 */
  isPortrait: boolean
}

export interface TouchState {
  /** 是否支持触摸 */
  supported: boolean
  /** 是否正在触摸 */
  active: boolean
  /** 触摸开始位置 */
  startPosition: { x: number; y: number } | null
  /** 当前触摸位置 */
  currentPosition: { x: number; y: number } | null
  /** 触摸类型 */
  gesture: 'none' | 'tap' | 'pan' | 'pinch' | 'swipe'
  /** 缩放比例 */
  scale: number
}

export interface LayoutMetrics {
  /** 容器内边距 */
  padding: number
  /** 词语间距 */
  wordSpacing: number
  /** 最小字体大小 */
  minFontSize: number
  /** 最大字体大小 */
  maxFontSize: number
  /** 网格大小 */
  gridSize: number
  /** 行高 */
  lineHeight: number
}

// ============= 主要composable =============

export function useResponsiveWordCloud(
  containerRef: any,
  config: Partial<ResponsiveConfig> = {}
) {
  // ============= 配置 =============

  const defaultConfig: ResponsiveConfig = {
    breakpoints: {
      xs: 0,
      sm: 640,
      md: 768,
      lg: 1024,
      xl: 1280
    },
    enableTouch: true,
    enableOrientation: true,
    debounceDelay: 300,
    throttleDelay: 100
  }

  const settings = reactive({
    ...defaultConfig,
    ...config
  })

  // ============= 响应式状态 =============

  /** 视口信息 */
  const viewport = reactive<ViewportInfo>({
    width: 0,
    height: 0,
    pixelRatio: window.devicePixelRatio || 1,
    breakpoint: 'md',
    isMobile: false,
    isTablet: false,
    isDesktop: false,
    isLandscape: false,
    isPortrait: false
  })

  /** 触摸状态 */
  const touch = reactive<TouchState>({
    supported: 'ontouchstart' in window,
    active: false,
    startPosition: null,
    currentPosition: null,
    gesture: 'none',
    scale: 1
  })

  /** 布局指标 */
  const layout = reactive<LayoutMetrics>({
    padding: 16,
    wordSpacing: 8,
    minFontSize: 12,
    maxFontSize: 48,
    gridSize: 8,
    lineHeight: 1.2
  })

  /** 容器尺寸 */
  const containerSize = reactive({
    width: 0,
    height: 0
  })

  /** 是否正在调整尺寸 */
  const isResizing = ref(false)

  // ============= 设备方向 =============

  const { isSupported: orientationSupported } = useDeviceOrientation()

  // ============= 计算属性 =============

  /** 当前断点 */
  const currentBreakpoint = computed(() => {
    return getBreakpoint(viewport.width)
  })

  /** 是否移动端 */
  const isMobileDevice = computed(() => {
    return viewport.breakpoint === 'xs' || viewport.breakpoint === 'sm'
  })

  /** 响应式词云配置 */
  const responsiveWordCloudOptions = computed(() => {
    const baseOptions: Partial<WordCloudOptions> = {}

    // 根据断点调整基础配置
    const breakpointConfig = adjustConfigForBreakpoint(baseOptions, currentBreakpoint.value)

    // 根据设备类型调整
    const deviceConfig = getDeviceSpecificConfig()

    // 根据方向调整
    const orientationConfig = getOrientationSpecificConfig()

    return {
      ...breakpointConfig,
      ...deviceConfig,
      ...orientationConfig,
      width: containerSize.width,
      height: containerSize.height
    }
  })

  /** 布局样式 */
  const layoutStyles = computed(() => ({
    '--word-cloud-padding': `${layout.padding}px`,
    '--word-cloud-spacing': `${layout.wordSpacing}px`,
    '--word-cloud-min-font': `${layout.minFontSize}px`,
    '--word-cloud-max-font': `${layout.maxFontSize}px`,
    '--word-cloud-grid': `${layout.gridSize}px`,
    '--word-cloud-line-height': layout.lineHeight
  }))

  // ============= 核心方法 =============

  /**
   * 更新视口信息
   */
  const updateViewport = () => {
    const width = window.innerWidth
    const height = window.innerHeight

    viewport.width = width
    viewport.height = height
    viewport.pixelRatio = window.devicePixelRatio || 1
    viewport.breakpoint = getBreakpoint(width)
    viewport.isMobile = width < settings.breakpoints.md
    viewport.isTablet = width >= settings.breakpoints.md && width < settings.breakpoints.lg
    viewport.isDesktop = width >= settings.breakpoints.lg
    viewport.isLandscape = width > height
    viewport.isPortrait = height > width

    // 更新布局指标
    updateLayoutMetrics()
  }

  /**
   * 更新容器尺寸
   */
  const updateContainerSize = () => {
    if (!containerRef?.value) return

    const rect = containerRef.value.getBoundingClientRect()
    containerSize.width = rect.width
    containerSize.height = rect.height
  }

  /**
   * 更新布局指标
   */
  const updateLayoutMetrics = () => {
    const breakpoint = currentBreakpoint.value

    switch (breakpoint) {
      case 'xs':
        layout.padding = 8
        layout.wordSpacing = 4
        layout.minFontSize = 10
        layout.maxFontSize = 32
        layout.gridSize = 12
        break
      case 'sm':
        layout.padding = 12
        layout.wordSpacing = 6
        layout.minFontSize = 12
        layout.maxFontSize = 36
        layout.gridSize = 10
        break
      case 'md':
        layout.padding = 16
        layout.wordSpacing = 8
        layout.minFontSize = 14
        layout.maxFontSize = 42
        layout.gridSize = 8
        break
      case 'lg':
        layout.padding = 20
        layout.wordSpacing = 10
        layout.minFontSize = 16
        layout.maxFontSize = 48
        layout.gridSize = 6
        break
      case 'xl':
        layout.padding = 24
        layout.wordSpacing = 12
        layout.minFontSize = 18
        layout.maxFontSize = 56
        layout.gridSize = 4
        break
    }
  }

  /**
   * 获取设备特定配置
   */
  const getDeviceSpecificConfig = (): Partial<WordCloudOptions> => {
    const config: Partial<WordCloudOptions> = {}

    if (isMobileDevice.value) {
      // 移动设备优化
      config.gridSize = Math.max(layout.gridSize, 12)
      config.rotationSteps = 2
      config.rotateRatio = 0.3
      config.shrinkToFit = true
    } else {
      // 桌面设备优化
      config.gridSize = layout.gridSize
      config.rotationSteps = 4
      config.rotateRatio = 0.5
    }

    return config
  }

  /**
   * 获取方向特定配置
   */
  const getOrientationSpecificConfig = (): Partial<WordCloudOptions> => {
    const config: Partial<WordCloudOptions> = {}

    if (settings.enableOrientation && orientationSupported) {
      if (viewport.isLandscape) {
        // 横屏模式：更多词语，较小字体
        config.weightFactor = (size: number) => Math.pow(size, 2.1) / 700
      } else {
        // 竖屏模式：较少词语，较大字体
        config.weightFactor = (size: number) => Math.pow(size, 2.5) / 500
      }
    }

    return config
  }

  // ============= 触摸处理 =============

  /**
   * 处理触摸开始
   */
  const handleTouchStart = (event: TouchEvent) => {
    if (!settings.enableTouch || !touch.supported) return

    touch.active = true
    touch.gesture = 'none'

    if (event.touches.length === 1) {
      // 单指触摸
      const touch1 = event.touches[0]
      touch.startPosition = { x: touch1.clientX, y: touch1.clientY }
      touch.currentPosition = { x: touch1.clientX, y: touch1.clientY }
    } else if (event.touches.length === 2) {
      // 双指触摸（缩放）
      touch.gesture = 'pinch'
    }
  }

  /**
   * 处理触摸移动
   */
  const handleTouchMove = (event: TouchEvent) => {
    if (!touch.active || !touch.startPosition) return

    event.preventDefault()

    if (event.touches.length === 1) {
      const touch1 = event.touches[0]
      touch.currentPosition = { x: touch1.clientX, y: touch1.clientY }

      const deltaX = touch1.clientX - touch.startPosition.x
      const deltaY = touch1.clientY - touch.startPosition.y
      const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY)

      if (distance > 10) {
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
          touch.gesture = 'swipe'
        } else {
          touch.gesture = 'pan'
        }
      }
    } else if (event.touches.length === 2) {
      // 处理双指缩放
      const touch1 = event.touches[0]
      const touch2 = event.touches[1]
      const distance = Math.sqrt(
        Math.pow(touch2.clientX - touch1.clientX, 2) +
        Math.pow(touch2.clientY - touch1.clientY, 2)
      )

      // 这里可以根据距离变化计算缩放比例
      // touch.scale = ...
    }
  }

  /**
   * 处理触摸结束
   */
  const handleTouchEnd = (event: TouchEvent) => {
    if (!touch.active) return

    if (touch.gesture === 'none' && touch.startPosition && touch.currentPosition) {
      const deltaX = touch.currentPosition.x - touch.startPosition.x
      const deltaY = touch.currentPosition.y - touch.startPosition.y
      const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY)

      if (distance < 5) {
        touch.gesture = 'tap'
        // 处理点击事件
        handleTap(touch.currentPosition)
      }
    }

    // 重置触摸状态
    touch.active = false
    touch.startPosition = null
    touch.currentPosition = null
    touch.gesture = 'none'
  }

  /**
   * 处理点击
   */
  const handleTap = (position: { x: number; y: number }) => {
    // 这里可以实现点击词语的逻辑
    console.log('Tap at:', position)
  }

  // ============= 性能优化的事件处理 =============

  /** 防抖的视口更新 */
  const debouncedUpdateViewport = debounce(updateViewport, settings.debounceDelay)

  /** 节流的容器尺寸更新 */
  const throttledUpdateContainerSize = throttle(updateContainerSize, settings.throttleDelay)

  /** 防抖的布局指标更新 */
  const debouncedUpdateLayout = debounce(updateLayoutMetrics, settings.debounceDelay)

  // ============= 自适应方法 =============

  /**
   * 自适应词语数量
   */
  const getAdaptiveWordCount = (baseWords: HotWordItem[]): HotWordItem[] => {
    const area = containerSize.width * containerSize.height
    let maxWords: number

    if (area < 200 * 150) {
      maxWords = 20
    } else if (area < 400 * 300) {
      maxWords = 50
    } else if (area < 800 * 600) {
      maxWords = 100
    } else {
      maxWords = 200
    }

    // 移动设备进一步限制
    if (isMobileDevice.value) {
      maxWords = Math.floor(maxWords * 0.7)
    }

    return baseWords.slice(0, maxWords)
  }

  /**
   * 自适应字体大小
   */
  const getAdaptiveFontSize = (weight: number, totalWords: number): number => {
    const baseSize = layout.minFontSize + (weight / 100) * (layout.maxFontSize - layout.minFontSize)

    // 根据容器大小调整
    const sizeMultiplier = Math.min(
      containerSize.width / 800,
      containerSize.height / 600
    )

    // 根据词语数量调整
    const densityMultiplier = Math.max(0.7, 1 - (totalWords - 50) / 200)

    return Math.round(baseSize * sizeMultiplier * densityMultiplier)
  }

  // ============= 生命周期管理 =============

  onMounted(async () => {
    await nextTick()

    // 初始化视口信息
    updateViewport()

    // 设置ResizeObserver监听容器尺寸变化
    if (containerRef?.value) {
      useResizeObserver(containerRef, throttledUpdateContainerSize)
      updateContainerSize()
    }

    // 监听窗口尺寸变化
    useEventListener(window, 'resize', debouncedUpdateViewport)

    // 监听方向变化
    // 注意: useDeviceOrientation 不提供 orientation 属性
    // 如需监听方向变化,可以监听 window resize 事件或使用 matchMedia

    // 触摸事件监听
    if (settings.enableTouch && touch.supported && containerRef?.value) {
      useEventListener(containerRef.value, 'touchstart', handleTouchStart, { passive: false })
      useEventListener(containerRef.value, 'touchmove', handleTouchMove, { passive: false })
      useEventListener(containerRef.value, 'touchend', handleTouchEnd)
    }
  })

  onUnmounted(() => {
    // 清理工作在useEventListener中自动处理
  })

  // ============= 监听器 =============

  // 监听断点变化
  watch(currentBreakpoint, (newBreakpoint, oldBreakpoint) => {
    if (newBreakpoint !== oldBreakpoint) {
      updateLayoutMetrics()
    }
  })

  // 监听容器尺寸变化
  watch(containerSize, () => {
    isResizing.value = true
    setTimeout(() => {
      isResizing.value = false
    }, 200)
  })

  // ============= 返回接口 =============

  return {
    // 状态
    viewport: readonly(viewport),
    touch: readonly(touch),
    layout: readonly(layout),
    containerSize: readonly(containerSize),
    isResizing: readonly(isResizing),

    // 计算属性
    currentBreakpoint,
    isMobileDevice,
    responsiveWordCloudOptions,
    layoutStyles,

    // 方法
    updateViewport,
    updateContainerSize,
    updateLayoutMetrics,
    getAdaptiveWordCount,
    getAdaptiveFontSize,

    // 配置
    settings
  }
}

/**
 * 简化的响应式hooks
 * 为不需要复杂功能的场景提供简单的响应式支持
 */
export function useSimpleResponsive(containerRef: any) {
  const breakpoint = ref<Breakpoint>('md')
  const isMobile = ref(false)

  const updateBreakpoint = () => {
    const width = window.innerWidth
    breakpoint.value = getBreakpoint(width)
    isMobile.value = width < 768
  }

  const debouncedUpdate = debounce(updateBreakpoint, 300)

  onMounted(() => {
    updateBreakpoint()
    useEventListener(window, 'resize', debouncedUpdate)
  })

  return {
    breakpoint: readonly(breakpoint),
    isMobile: readonly(isMobile)
  }
}