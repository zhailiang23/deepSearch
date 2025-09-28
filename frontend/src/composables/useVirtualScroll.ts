/**
 * 虚拟滚动组合函数
 * 用于优化大数据量列表的渲染性能
 * 支持固定高度和动态高度的虚拟滚动
 */

import { ref, computed, onMounted, onUnmounted, nextTick, type Ref } from 'vue'

// ============= 类型定义 =============

/**
 * 虚拟滚动项目接口
 */
export interface VirtualScrollItem {
  /** 唯一标识 */
  id: string | number
  /** 项目高度（动态模式下可选） */
  height?: number
  /** 项目数据 */
  data: any
}

/**
 * 虚拟滚动配置选项
 */
export interface VirtualScrollOptions {
  /** 固定项目高度（固定模式） */
  itemSize?: number
  /** 最小项目高度（动态模式） */
  minItemSize?: number
  /** 缓冲区大小（像素） */
  bufferSize?: number
  /** 滚动防抖延迟（毫秒） */
  throttleDelay?: number
  /** 容器高度 */
  containerHeight?: number
  /** 是否启用水平滚动 */
  horizontal?: boolean
  /** 预渲染数量 */
  prerender?: number
}

/**
 * 虚拟滚动状态
 */
export interface VirtualScrollState {
  /** 滚动位置 */
  scrollTop: number
  /** 可见区域开始索引 */
  startIndex: number
  /** 可见区域结束索引 */
  endIndex: number
  /** 可见项目数量 */
  visibleCount: number
  /** 总高度 */
  totalHeight: number
  /** 上方偏移量 */
  offsetTop: number
  /** 下方偏移量 */
  offsetBottom: number
}

/**
 * 渲染项目
 */
export interface RenderItem extends VirtualScrollItem {
  /** 渲染索引 */
  index: number
  /** 是否激活 */
  active: boolean
  /** 计算后的高度 */
  computedHeight: number
  /** 在容器中的位置 */
  top: number
}

// ============= 默认配置 =============

const DEFAULT_OPTIONS: Required<VirtualScrollOptions> = {
  itemSize: 50,
  minItemSize: 50,
  bufferSize: 200,
  throttleDelay: 16,
  containerHeight: 400,
  horizontal: false,
  prerender: 10
}

// ============= 主要组合函数 =============

/**
 * 固定高度虚拟滚动
 */
export function useFixedVirtualScroll(
  containerRef: Ref<HTMLElement | null>,
  items: Ref<VirtualScrollItem[]>,
  options: Partial<VirtualScrollOptions> = {}
) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  // ============= 响应式状态 =============

  const scrollTop = ref(0)
  const containerHeight = ref(config.containerHeight)
  const isScrolling = ref(false)
  const scrollDirection = ref<'up' | 'down'>('down')

  // ============= 计算属性 =============

  /** 总高度 */
  const totalHeight = computed(() => {
    return items.value.length * config.itemSize
  })

  /** 可见区域内可容纳的项目数量 */
  const visibleCount = computed(() => {
    return Math.ceil(containerHeight.value / config.itemSize) + 1
  })

  /** 开始索引 */
  const startIndex = computed(() => {
    const bufferItems = Math.floor(config.bufferSize / config.itemSize)
    const index = Math.floor(scrollTop.value / config.itemSize) - bufferItems
    return Math.max(0, index)
  })

  /** 结束索引 */
  const endIndex = computed(() => {
    const bufferItems = Math.floor(config.bufferSize / config.itemSize)
    const index = startIndex.value + visibleCount.value + bufferItems * 2
    return Math.min(items.value.length - 1, index)
  })

  /** 上方偏移量 */
  const offsetTop = computed(() => {
    return startIndex.value * config.itemSize
  })

  /** 下方偏移量 */
  const offsetBottom = computed(() => {
    const visibleItems = endIndex.value - startIndex.value + 1
    return totalHeight.value - offsetTop.value - (visibleItems * config.itemSize)
  })

  /** 可见项目列表 */
  const visibleItems = computed<RenderItem[]>(() => {
    const result: RenderItem[] = []

    for (let i = startIndex.value; i <= endIndex.value; i++) {
      if (i >= 0 && i < items.value.length) {
        const item = items.value[i]
        result.push({
          ...item,
          index: i,
          active: true,
          computedHeight: config.itemSize,
          top: i * config.itemSize
        })
      }
    }

    return result
  })

  /** 虚拟滚动状态 */
  const virtualState = computed<VirtualScrollState>(() => ({
    scrollTop: scrollTop.value,
    startIndex: startIndex.value,
    endIndex: endIndex.value,
    visibleCount: visibleCount.value,
    totalHeight: totalHeight.value,
    offsetTop: offsetTop.value,
    offsetBottom: offsetBottom.value
  }))

  // ============= 事件处理 =============

  let scrollTimer: number | null = null
  let lastScrollTop = 0

  /** 滚动处理函数 */
  const handleScroll = () => {
    if (!containerRef.value) return

    const currentScrollTop = containerRef.value.scrollTop

    // 检测滚动方向
    scrollDirection.value = currentScrollTop > lastScrollTop ? 'down' : 'up'
    lastScrollTop = currentScrollTop

    scrollTop.value = currentScrollTop
    isScrolling.value = true

    // 防抖处理
    if (scrollTimer) {
      clearTimeout(scrollTimer)
    }

    scrollTimer = setTimeout(() => {
      isScrolling.value = false
      scrollTimer = null
    }, config.throttleDelay * 3)
  }

  /** 节流滚动处理 */
  let throttleTimer: number | null = null

  const throttledHandleScroll = () => {
    if (throttleTimer) return

    throttleTimer = setTimeout(() => {
      handleScroll()
      throttleTimer = null
    }, config.throttleDelay)
  }

  /** 更新容器高度 */
  const updateContainerHeight = () => {
    if (containerRef.value) {
      const rect = containerRef.value.getBoundingClientRect()
      containerHeight.value = rect.height
    }
  }

  /** 滚动到指定索引 */
  const scrollToIndex = (index: number, behavior: ScrollBehavior = 'smooth') => {
    if (!containerRef.value) return

    const targetScrollTop = Math.max(0, index * config.itemSize)

    containerRef.value.scrollTo({
      top: targetScrollTop,
      behavior
    })
  }

  /** 滚动到顶部 */
  const scrollToTop = (behavior: ScrollBehavior = 'smooth') => {
    scrollToIndex(0, behavior)
  }

  /** 滚动到底部 */
  const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    if (!containerRef.value) return

    containerRef.value.scrollTo({
      top: totalHeight.value,
      behavior
    })
  }

  // ============= 生命周期 =============

  let resizeObserver: ResizeObserver | null = null

  onMounted(async () => {
    await nextTick()

    if (containerRef.value) {
      // 绑定滚动事件
      containerRef.value.addEventListener('scroll', throttledHandleScroll, { passive: true })

      // 初始化容器高度
      updateContainerHeight()

      // 监听容器尺寸变化
      if (window.ResizeObserver) {
        resizeObserver = new ResizeObserver(() => {
          updateContainerHeight()
        })
        resizeObserver.observe(containerRef.value)
      }
    }
  })

  onUnmounted(() => {
    if (containerRef.value) {
      containerRef.value.removeEventListener('scroll', throttledHandleScroll)
    }

    if (resizeObserver) {
      resizeObserver.disconnect()
    }

    if (scrollTimer) {
      clearTimeout(scrollTimer)
    }

    if (throttleTimer) {
      clearTimeout(throttleTimer)
    }
  })

  // ============= 返回API =============

  return {
    // 状态
    virtualState,
    isScrolling,
    scrollDirection,

    // 数据
    visibleItems,
    totalHeight,
    offsetTop,
    offsetBottom,

    // 方法
    scrollToIndex,
    scrollToTop,
    scrollToBottom,
    updateContainerHeight,

    // 调试信息
    debug: computed(() => ({
      itemCount: items.value.length,
      visibleRange: `${startIndex.value} - ${endIndex.value}`,
      renderedCount: visibleItems.value.length,
      scrollTop: scrollTop.value,
      containerHeight: containerHeight.value
    }))
  }
}

// ============= 动态高度虚拟滚动 =============

/**
 * 动态高度虚拟滚动
 */
export function useDynamicVirtualScroll(
  containerRef: Ref<HTMLElement | null>,
  items: Ref<VirtualScrollItem[]>,
  options: Partial<VirtualScrollOptions> = {}
) {
  const config = { ...DEFAULT_OPTIONS, ...options }

  // ============= 响应式状态 =============

  const scrollTop = ref(0)
  const containerHeight = ref(config.containerHeight)
  const isScrolling = ref(false)
  const itemHeights = ref<Map<string | number, number>>(new Map())
  const itemPositions = ref<Map<string | number, number>>(new Map())

  // ============= 计算属性 =============

  /** 获取项目高度 */
  const getItemHeight = (item: VirtualScrollItem, index: number): number => {
    const cachedHeight = itemHeights.value.get(item.id)
    if (cachedHeight !== undefined) {
      return cachedHeight
    }

    return item.height || config.minItemSize
  }

  /** 总高度 */
  const totalHeight = computed(() => {
    let height = 0

    items.value.forEach((item, index) => {
      height += getItemHeight(item, index)
    })

    return height
  })

  /** 项目位置映射 */
  const updateItemPositions = () => {
    let currentTop = 0
    const positions = new Map<string | number, number>()

    items.value.forEach((item, index) => {
      positions.set(item.id, currentTop)
      currentTop += getItemHeight(item, index)
    })

    itemPositions.value = positions
  }

  /** 可见项目索引范围 */
  const visibleRange = computed(() => {
    const positions = itemPositions.value
    const viewportTop = scrollTop.value
    const viewportBottom = scrollTop.value + containerHeight.value

    let startIdx = 0
    let endIdx = items.value.length - 1

    // 查找开始索引
    for (let i = 0; i < items.value.length; i++) {
      const itemTop = positions.get(items.value[i].id) || 0
      const itemHeight = getItemHeight(items.value[i], i)

      if (itemTop + itemHeight >= viewportTop) {
        startIdx = Math.max(0, i - Math.floor(config.bufferSize / config.minItemSize))
        break
      }
    }

    // 查找结束索引
    for (let i = startIdx; i < items.value.length; i++) {
      const itemTop = positions.get(items.value[i].id) || 0

      if (itemTop > viewportBottom) {
        endIdx = Math.min(items.value.length - 1, i + Math.floor(config.bufferSize / config.minItemSize))
        break
      }
    }

    return { startIndex: startIdx, endIndex: endIdx }
  })

  /** 可见项目列表 */
  const visibleItems = computed<RenderItem[]>(() => {
    updateItemPositions()

    const { startIndex, endIndex } = visibleRange.value
    const result: RenderItem[] = []

    for (let i = startIndex; i <= endIndex; i++) {
      if (i >= 0 && i < items.value.length) {
        const item = items.value[i]
        const height = getItemHeight(item, i)
        const top = itemPositions.value.get(item.id) || 0

        result.push({
          ...item,
          index: i,
          active: true,
          computedHeight: height,
          top
        })
      }
    }

    return result
  })

  /** 上方偏移量 */
  const offsetTop = computed(() => {
    const { startIndex } = visibleRange.value
    if (startIndex === 0) return 0

    const firstVisibleItem = items.value[startIndex]
    return itemPositions.value.get(firstVisibleItem?.id) || 0
  })

  /** 下方偏移量 */
  const offsetBottom = computed(() => {
    const { endIndex } = visibleRange.value

    let usedHeight = offsetTop.value
    for (let i = visibleRange.value.startIndex; i <= endIndex; i++) {
      if (i < items.value.length) {
        usedHeight += getItemHeight(items.value[i], i)
      }
    }

    return Math.max(0, totalHeight.value - usedHeight)
  })

  // ============= 方法 =============

  /** 更新项目高度 */
  const updateItemHeight = (itemId: string | number, height: number) => {
    if (height > 0 && itemHeights.value.get(itemId) !== height) {
      itemHeights.value.set(itemId, height)
      updateItemPositions()
    }
  }

  /** 批量更新项目高度 */
  const updateItemHeights = (heights: Map<string | number, number>) => {
    let changed = false

    heights.forEach((height, itemId) => {
      if (height > 0 && itemHeights.value.get(itemId) !== height) {
        itemHeights.value.set(itemId, height)
        changed = true
      }
    })

    if (changed) {
      updateItemPositions()
    }
  }

  /** 滚动处理函数 */
  const handleScroll = () => {
    if (!containerRef.value) return

    scrollTop.value = containerRef.value.scrollTop
    isScrolling.value = true

    // 防抖处理
    setTimeout(() => {
      isScrolling.value = false
    }, 150)
  }

  /** 滚动到指定索引 */
  const scrollToIndex = (index: number, behavior: ScrollBehavior = 'smooth') => {
    if (!containerRef.value || index < 0 || index >= items.value.length) return

    const item = items.value[index]
    const targetScrollTop = itemPositions.value.get(item.id) || 0

    containerRef.value.scrollTo({
      top: targetScrollTop,
      behavior
    })
  }

  // ============= 生命周期 =============

  onMounted(async () => {
    await nextTick()

    if (containerRef.value) {
      containerRef.value.addEventListener('scroll', handleScroll, { passive: true })

      // 初始化
      updateItemPositions()

      const rect = containerRef.value.getBoundingClientRect()
      containerHeight.value = rect.height
    }
  })

  onUnmounted(() => {
    if (containerRef.value) {
      containerRef.value.removeEventListener('scroll', handleScroll)
    }
  })

  return {
    // 状态
    isScrolling,
    totalHeight,

    // 数据
    visibleItems,
    offsetTop,
    offsetBottom,

    // 方法
    updateItemHeight,
    updateItemHeights,
    scrollToIndex,

    // 调试信息
    debug: computed(() => ({
      itemCount: items.value.length,
      visibleRange: `${visibleRange.value.startIndex} - ${visibleRange.value.endIndex}`,
      renderedCount: visibleItems.value.length,
      cachedHeights: itemHeights.value.size,
      totalHeight: totalHeight.value
    }))
  }
}

// ============= 辅助工具函数 =============

/**
 * 计算最佳缓冲区大小
 */
export function calculateOptimalBufferSize(
  itemSize: number,
  containerHeight: number,
  itemCount: number
): number {
  const visibleItems = Math.ceil(containerHeight / itemSize)
  const bufferItems = Math.min(
    Math.max(2, Math.ceil(visibleItems * 0.5)), // 至少2个，最多50%
    Math.min(10, Math.ceil(itemCount * 0.1))    // 最多10个或总数的10%
  )

  return bufferItems * itemSize
}

/**
 * 预估项目高度
 */
export function estimateItemHeight(
  content: string,
  baseHeight: number = 50,
  charPerLine: number = 50
): number {
  const lines = Math.max(1, Math.ceil(content.length / charPerLine))
  return baseHeight * lines
}

/**
 * 创建虚拟滚动项目
 */
export function createVirtualScrollItem(
  id: string | number,
  data: any,
  height?: number
): VirtualScrollItem {
  return {
    id,
    data,
    height
  }
}