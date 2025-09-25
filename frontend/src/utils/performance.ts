// 性能优化工具函数

/**
 * 防抖函数 - 延迟执行，频繁调用时重新计时
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  immediate = false
): (...args: Parameters<T>) => void {
  let timeout: number | null = null
  let result: ReturnType<T>

  return function (this: any, ...args: Parameters<T>) {
    const context = this
    const later = () => {
      timeout = null
      if (!immediate) result = func.apply(context, args)
    }

    const callNow = immediate && !timeout

    if (timeout) clearTimeout(timeout)
    timeout = window.setTimeout(later, wait)

    if (callNow) result = func.apply(context, args)

    return result
  }
}

/**
 * 节流函数 - 限制执行频率
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean
  let lastResult: ReturnType<T>

  return function (this: any, ...args: Parameters<T>) {
    const context = this

    if (!inThrottle) {
      lastResult = func.apply(context, args)
      inThrottle = true

      setTimeout(() => {
        inThrottle = false
      }, limit)
    }

    return lastResult
  }
}

/**
 * 请求动画帧节流 - 使用 requestAnimationFrame 进行节流
 */
export function rafThrottle<T extends (...args: any[]) => any>(
  func: T
): (...args: Parameters<T>) => void {
  let isScheduled = false

  return function (this: any, ...args: Parameters<T>) {
    if (isScheduled) return

    isScheduled = true
    const context = this

    requestAnimationFrame(() => {
      func.apply(context, args)
      isScheduled = false
    })
  }
}

/**
 * 批处理函数 - 将多个调用合并为一次执行
 */
export function batchProcess<T>(
  processor: (items: T[]) => void,
  delay = 0
) {
  let batch: T[] = []
  let timeoutId: number | null = null

  return (item: T) => {
    batch.push(item)

    if (timeoutId) clearTimeout(timeoutId)

    timeoutId = window.setTimeout(() => {
      if (batch.length > 0) {
        processor([...batch])
        batch = []
      }
      timeoutId = null
    }, delay)
  }
}

/**
 * 内存管理类 - 管理组件的内存使用
 */
export class MemoryManager {
  private static instance: MemoryManager
  private caches = new Map<string, Map<string, any>>()
  private timers = new Set<number>()
  private observers = new Set<IntersectionObserver | MutationObserver | ResizeObserver>()

  static getInstance() {
    if (!MemoryManager.instance) {
      MemoryManager.instance = new MemoryManager()
    }
    return MemoryManager.instance
  }

  // 缓存管理
  setCache<T>(namespace: string, key: string, value: T, ttl?: number) {
    if (!this.caches.has(namespace)) {
      this.caches.set(namespace, new Map())
    }

    const cache = this.caches.get(namespace)!
    cache.set(key, {
      value,
      timestamp: Date.now(),
      ttl: ttl || 0
    })

    // 清理过期缓存
    this.cleanExpiredCache(namespace)
  }

  getCache<T>(namespace: string, key: string): T | null {
    const cache = this.caches.get(namespace)
    if (!cache) return null

    const item = cache.get(key)
    if (!item) return null

    // 检查是否过期
    if (item.ttl > 0 && Date.now() - item.timestamp > item.ttl) {
      cache.delete(key)
      return null
    }

    return item.value
  }

  clearCache(namespace?: string) {
    if (namespace) {
      this.caches.delete(namespace)
    } else {
      this.caches.clear()
    }
  }

  private cleanExpiredCache(namespace: string) {
    const cache = this.caches.get(namespace)
    if (!cache) return

    const now = Date.now()
    for (const [key, item] of cache.entries()) {
      if (item.ttl > 0 && now - item.timestamp > item.ttl) {
        cache.delete(key)
      }
    }
  }

  // 定时器管理
  addTimer(timerId: number) {
    this.timers.add(timerId)
  }

  removeTimer(timerId: number) {
    this.timers.delete(timerId)
    clearTimeout(timerId)
  }

  clearAllTimers() {
    this.timers.forEach(id => clearTimeout(id))
    this.timers.clear()
  }

  // Observer 管理
  addObserver(observer: IntersectionObserver | MutationObserver | ResizeObserver) {
    this.observers.add(observer)
  }

  removeObserver(observer: IntersectionObserver | MutationObserver | ResizeObserver) {
    this.observers.delete(observer)
    observer.disconnect()
  }

  clearAllObservers() {
    this.observers.forEach(observer => observer.disconnect())
    this.observers.clear()
  }

  // 全部清理
  cleanup() {
    this.clearAllTimers()
    this.clearAllObservers()
    this.clearCache()
  }
}

/**
 * 虚拟滚动优化器 - 优化大列表渲染性能
 */
export class VirtualScrollOptimizer {
  private intersectionObserver?: IntersectionObserver
  private visibleItems = new Set<string>()

  constructor(private container: HTMLElement) {
    this.setupIntersectionObserver()
  }

  private setupIntersectionObserver() {
    if (!('IntersectionObserver' in window)) return

    this.intersectionObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          const itemId = entry.target.getAttribute('data-item-id')
          if (!itemId) return

          if (entry.isIntersecting) {
            this.visibleItems.add(itemId)
          } else {
            this.visibleItems.delete(itemId)
          }
        })
      },
      {
        root: this.container,
        rootMargin: '50px',
        threshold: 0
      }
    )

    // 管理observer
    MemoryManager.getInstance().addObserver(this.intersectionObserver)
  }

  observeItem(element: HTMLElement, itemId: string) {
    if (!this.intersectionObserver) return

    element.setAttribute('data-item-id', itemId)
    this.intersectionObserver.observe(element)
  }

  unobserveItem(element: HTMLElement) {
    if (!this.intersectionObserver) return
    this.intersectionObserver.unobserve(element)
  }

  getVisibleItems(): string[] {
    return Array.from(this.visibleItems)
  }

  destroy() {
    if (this.intersectionObserver) {
      MemoryManager.getInstance().removeObserver(this.intersectionObserver)
      this.intersectionObserver = undefined
    }
    this.visibleItems.clear()
  }
}

/**
 * 渲染性能监控器
 */
export class RenderPerformanceMonitor {
  private renderTimes: number[] = []
  private frameCount = 0
  private lastFrameTime = 0

  startFrame() {
    this.lastFrameTime = performance.now()
  }

  endFrame() {
    const frameTime = performance.now() - this.lastFrameTime
    this.renderTimes.push(frameTime)
    this.frameCount++

    // 保持最近100帧的记录
    if (this.renderTimes.length > 100) {
      this.renderTimes.shift()
    }
  }

  getAverageFrameTime(): number {
    if (this.renderTimes.length === 0) return 0
    const sum = this.renderTimes.reduce((a, b) => a + b, 0)
    return sum / this.renderTimes.length
  }

  getFPS(): number {
    const avgFrameTime = this.getAverageFrameTime()
    return avgFrameTime > 0 ? 1000 / avgFrameTime : 0
  }

  getPerformanceReport() {
    return {
      frameCount: this.frameCount,
      averageFrameTime: this.getAverageFrameTime(),
      fps: this.getFPS(),
      slowFrames: this.renderTimes.filter(time => time > 16.67).length // > 60fps
    }
  }

  reset() {
    this.renderTimes = []
    this.frameCount = 0
    this.lastFrameTime = 0
  }
}

/**
 * 图片懒加载优化
 */
export function setupImageLazyLoading(
  container: HTMLElement,
  selector = 'img[data-src]'
): () => void {
  const imageObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target as HTMLImageElement
          const src = img.getAttribute('data-src')
          
          if (src) {
            img.src = src
            img.removeAttribute('data-src')
            imageObserver.unobserve(img)
          }
        }
      })
    },
    {
      root: container,
      rootMargin: '50px'
    }
  )

  // 观察所有图片
  const images = container.querySelectorAll(selector)
  images.forEach(img => imageObserver.observe(img))

  // 管理observer
  MemoryManager.getInstance().addObserver(imageObserver)

  // 返回清理函数
  return () => {
    MemoryManager.getInstance().removeObserver(imageObserver)
  }
}

/**
 * 自适应批处理大小计算
 */
export function calculateOptimalBatchSize(itemHeight: number, containerHeight: number): number {
  const visibleCount = Math.ceil(containerHeight / itemHeight)
  const bufferMultiplier = visibleCount < 10 ? 2 : visibleCount < 20 ? 1.5 : 1.2
  return Math.ceil(visibleCount * bufferMultiplier)
}

/**
 * 内存使用监控
 */
export function monitorMemoryUsage(): {
  used: number;
  total: number;
  percentage: number;
} | null {
  if ('memory' in performance) {
    const memory = (performance as any).memory
    return {
      used: memory.usedJSHeapSize,
      total: memory.totalJSHeapSize,
      percentage: (memory.usedJSHeapSize / memory.totalJSHeapSize) * 100
    }
  }
  return null
}

/**
 * 滚动性能优化助手
 */
export function createScrollOptimizer(
  element: HTMLElement,
  options: {
    passive?: boolean;
    throttleMs?: number;
  } = {}
) {
  const { passive = true, throttleMs = 16 } = options
  let isScrolling = false

  const handleScroll = throttle((event: Event) => {
    // 滚动处理逻辑
    if (!isScrolling) {
      requestAnimationFrame(() => {
        // 在下一帧执行滚动相关的DOM更新
        isScrolling = false
      })
      isScrolling = true
    }
  }, throttleMs)

  element.addEventListener('scroll', handleScroll, { passive })

  return () => {
    element.removeEventListener('scroll', handleScroll)
  }
}