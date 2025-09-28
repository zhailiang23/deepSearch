/**
 * 词云交互工具函数
 * 提供词云交互功能的实用函数，包括位置检测、动画效果、事件处理等
 */

import type {
  WordCloudItem,
  WordCloudDimension,
  WordCloudConfig,
  WordCloudInteractionConfig
} from '@/types/wordCloudTypes'

// 动画配置接口
export interface AnimationConfig {
  duration: number
  easing: 'linear' | 'ease-in' | 'ease-out' | 'ease-in-out'
  repeat?: number
  direction?: 'normal' | 'reverse' | 'alternate'
}

// 位置信息接口
export interface PositionInfo {
  x: number
  y: number
  width: number
  height: number
  rotation: number
  fontSize: number
  word: string
  item: WordCloudItem
}

// 交互效果接口
export interface InteractionEffect {
  type: 'highlight' | 'shadow' | 'scale' | 'glow' | 'bounce'
  intensity: number
  duration: number
  color?: string
}

// 词云数据管理类
export class WordCloudDataManager {
  private wordPositions: Map<string, PositionInfo> = new Map()
  private canvas: HTMLCanvasElement | null = null
  private context: CanvasRenderingContext2D | null = null

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas
    this.context = canvas.getContext('2d')
  }

  // 存储词语位置信息
  storeWordPosition(word: string, position: PositionInfo): void {
    this.wordPositions.set(word, position)
  }

  // 获取词语位置信息
  getWordPosition(word: string): PositionInfo | null {
    return this.wordPositions.get(word) || null
  }

  // 根据坐标查找词语
  getWordAtPosition(x: number, y: number): PositionInfo | null {
    for (const [, position] of this.wordPositions) {
      if (this.isPointInWord(x, y, position)) {
        return position
      }
    }
    return null
  }

  // 检查点是否在词语范围内（考虑旋转）
  private isPointInWord(x: number, y: number, position: PositionInfo): boolean {
    const { x: wx, y: wy, width, height, rotation } = position

    // 简化处理，使用矩形边界检测
    if (rotation === 0) {
      return (
        x >= wx - width / 2 &&
        x <= wx + width / 2 &&
        y >= wy - height / 2 &&
        y <= wy + height / 2
      )
    }

    // 处理旋转的情况（更复杂的计算）
    const cos = Math.cos(-rotation)
    const sin = Math.sin(-rotation)
    const dx = x - wx
    const dy = y - wy
    const rotatedX = dx * cos - dy * sin
    const rotatedY = dx * sin + dy * cos

    return (
      rotatedX >= -width / 2 &&
      rotatedX <= width / 2 &&
      rotatedY >= -height / 2 &&
      rotatedY <= height / 2
    )
  }

  // 清除所有位置数据
  clearPositions(): void {
    this.wordPositions.clear()
  }

  // 获取所有词语位置
  getAllPositions(): Map<string, PositionInfo> {
    return new Map(this.wordPositions)
  }
}

// 动画管理器
export class InteractionAnimationManager {
  private animationFrameId: number | null = null
  private activeAnimations: Map<string, Animation> = new Map()

  // 创建高亮动画
  createHighlightAnimation(
    canvas: HTMLCanvasElement,
    position: PositionInfo,
    config: AnimationConfig
  ): Animation {
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas context not available')

    let startTime: number | null = null
    const animate = (timestamp: number) => {
      if (!startTime) startTime = timestamp
      const elapsed = timestamp - startTime
      const progress = Math.min(elapsed / config.duration, 1)

      // 清除上一帧（如果需要）
      this.clearAnimationArea(ctx, position)

      // 绘制高亮效果
      this.drawHighlightEffect(ctx, position, progress)

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(animate)
      } else {
        this.completeAnimation(position.word)
      }
    }

    const animation = { animate, stop: () => this.stopAnimation(position.word) }
    this.activeAnimations.set(position.word, animation)

    this.animationFrameId = requestAnimationFrame(animate)
    return animation
  }

  // 创建缩放动画
  createScaleAnimation(
    canvas: HTMLCanvasElement,
    position: PositionInfo,
    targetScale: number,
    config: AnimationConfig
  ): Animation {
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas context not available')

    const initialScale = 1
    let startTime: number | null = null

    const animate = (timestamp: number) => {
      if (!startTime) startTime = timestamp
      const elapsed = timestamp - startTime
      const progress = Math.min(elapsed / config.duration, 1)

      // 计算当前缩放值
      const currentScale = initialScale + (targetScale - initialScale) * this.easeProgress(progress, config.easing)

      // 清除上一帧
      this.clearAnimationArea(ctx, position)

      // 绘制缩放效果
      this.drawScaledWord(ctx, position, currentScale)

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(animate)
      } else {
        this.completeAnimation(position.word)
      }
    }

    const animation = { animate, stop: () => this.stopAnimation(position.word) }
    this.activeAnimations.set(position.word, animation)

    this.animationFrameId = requestAnimationFrame(animate)
    return animation
  }

  // 创建阴影动画
  createShadowAnimation(
    canvas: HTMLCanvasElement,
    position: PositionInfo,
    shadowColor: string,
    config: AnimationConfig
  ): Animation {
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas context not available')

    let startTime: number | null = null

    const animate = (timestamp: number) => {
      if (!startTime) startTime = timestamp
      const elapsed = timestamp - startTime
      const progress = Math.min(elapsed / config.duration, 1)

      // 清除上一帧
      this.clearAnimationArea(ctx, position)

      // 绘制阴影效果
      this.drawShadowEffect(ctx, position, shadowColor, progress)

      if (progress < 1) {
        this.animationFrameId = requestAnimationFrame(animate)
      } else {
        this.completeAnimation(position.word)
      }
    }

    const animation = { animate, stop: () => this.stopAnimation(position.word) }
    this.activeAnimations.set(position.word, animation)

    this.animationFrameId = requestAnimationFrame(animate)
    return animation
  }

  // 停止动画
  stopAnimation(word: string): void {
    const animation = this.activeAnimations.get(word)
    if (animation) {
      this.activeAnimations.delete(word)
    }

    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
      this.animationFrameId = null
    }
  }

  // 停止所有动画
  stopAllAnimations(): void {
    this.activeAnimations.forEach((_, word) => {
      this.stopAnimation(word)
    })
    this.activeAnimations.clear()
  }

  // 完成动画
  private completeAnimation(word: string): void {
    this.activeAnimations.delete(word)
  }

  // 清除动画区域
  private clearAnimationArea(ctx: CanvasRenderingContext2D, position: PositionInfo): void {
    const padding = 20
    ctx.clearRect(
      position.x - position.width / 2 - padding,
      position.y - position.height / 2 - padding,
      position.width + padding * 2,
      position.height + padding * 2
    )
  }

  // 绘制高亮效果
  private drawHighlightEffect(ctx: CanvasRenderingContext2D, position: PositionInfo, progress: number): void {
    const alpha = Math.sin(progress * Math.PI) * 0.5

    ctx.save()
    ctx.globalAlpha = alpha
    ctx.fillStyle = 'rgba(255, 255, 0, 0.3)'
    ctx.fillRect(
      position.x - position.width / 2,
      position.y - position.height / 2,
      position.width,
      position.height
    )
    ctx.restore()
  }

  // 绘制缩放效果
  private drawScaledWord(ctx: CanvasRenderingContext2D, position: PositionInfo, scale: number): void {
    ctx.save()
    ctx.translate(position.x, position.y)
    ctx.scale(scale, scale)
    ctx.rotate(position.rotation)

    ctx.font = `${position.fontSize}px Arial`
    ctx.fillStyle = 'black'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(position.word, 0, 0)

    ctx.restore()
  }

  // 绘制阴影效果
  private drawShadowEffect(ctx: CanvasRenderingContext2D, position: PositionInfo, shadowColor: string, progress: number): void {
    const shadowBlur = progress * 10
    const shadowOffsetX = progress * 2
    const shadowOffsetY = progress * 2

    ctx.save()
    ctx.shadowColor = shadowColor
    ctx.shadowBlur = shadowBlur
    ctx.shadowOffsetX = shadowOffsetX
    ctx.shadowOffsetY = shadowOffsetY

    ctx.translate(position.x, position.y)
    ctx.rotate(position.rotation)
    ctx.font = `${position.fontSize}px Arial`
    ctx.fillStyle = 'black'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(position.word, 0, 0)

    ctx.restore()
  }

  // 缓动函数
  private easeProgress(progress: number, easing: string): number {
    switch (easing) {
      case 'ease-in':
        return progress * progress
      case 'ease-out':
        return 1 - (1 - progress) * (1 - progress)
      case 'ease-in-out':
        return progress < 0.5 ? 2 * progress * progress : 1 - 2 * (1 - progress) * (1 - progress)
      default:
        return progress
    }
  }
}

// 交互效果工具函数
export function createInteractionEffect(
  type: InteractionEffect['type'],
  canvas: HTMLCanvasElement,
  position: PositionInfo,
  config: Partial<InteractionEffect> = {}
): void {
  const animationManager = new InteractionAnimationManager()
  const defaultConfig: AnimationConfig = {
    duration: config.duration || 300,
    easing: 'ease-out'
  }

  switch (type) {
    case 'highlight':
      animationManager.createHighlightAnimation(canvas, position, defaultConfig)
      break
    case 'scale':
      animationManager.createScaleAnimation(canvas, position, 1.2, defaultConfig)
      break
    case 'shadow':
      animationManager.createShadowAnimation(
        canvas,
        position,
        config.color || 'rgba(0, 0, 0, 0.5)',
        defaultConfig
      )
      break
    case 'glow':
      // 发光效果（类似阴影但使用明亮颜色）
      animationManager.createShadowAnimation(
        canvas,
        position,
        config.color || 'rgba(255, 255, 255, 0.8)',
        defaultConfig
      )
      break
  }
}

// 创建词云交互配置的工厂函数
export function createWordCloudInteractionCallbacks(
  dataManager: WordCloudDataManager,
  config: WordCloudInteractionConfig,
  onWordClick?: (word: string, item: WordCloudItem) => void,
  onWordHover?: (word: string | null, item: WordCloudItem | null) => void
) {
  return {
    // wordcloud2.js点击回调
    click: (item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => {
      if (!config.enableClick) return

      const word = Array.isArray(item) ? item[0] : String(item)

      // 存储位置信息
      const position: PositionInfo = {
        x: dimension.x + dimension.w / 2,
        y: dimension.y + dimension.h / 2,
        width: dimension.w,
        height: dimension.h,
        rotation: 0, // wordcloud2.js不直接提供旋转信息
        fontSize: 16, // 需要从其他地方获取
        word,
        item
      }

      dataManager.storeWordPosition(word, position)

      // 触发点击效果
      if (config.clickAction && event.target instanceof HTMLCanvasElement) {
        switch (config.clickAction) {
          case 'search':
            // 搜索相关词语
            onWordClick?.(word, item)
            break
          case 'filter':
            // 筛选操作
            onWordClick?.(word, item)
            break
          case 'custom':
            // 自定义操作
            onWordClick?.(word, item)
            break
        }
      }
    },

    // wordcloud2.js悬停回调
    hover: (item: WordCloudItem | undefined, dimension: WordCloudDimension | undefined, event: MouseEvent) => {
      if (!config.enableHover) return

      if (item && dimension) {
        // 鼠标进入词语
        const word = Array.isArray(item) ? item[0] : String(item)

        const position: PositionInfo = {
          x: dimension.x + dimension.w / 2,
          y: dimension.y + dimension.h / 2,
          width: dimension.w,
          height: dimension.h,
          rotation: 0,
          fontSize: 16,
          word,
          item
        }

        dataManager.storeWordPosition(word, position)

        // 应用悬停效果
        if (event.target instanceof HTMLCanvasElement) {
          createInteractionEffect(config.hoverEffect || 'highlight', event.target, position)
        }

        onWordHover?.(word, item)
      } else {
        // 鼠标离开词语
        onWordHover?(null, null)
      }
    }
  }
}

// 缩放工具函数
export function createZoomTransform(
  canvas: HTMLCanvasElement,
  scale: number,
  centerX?: number,
  centerY?: number
): void {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const cx = centerX || canvas.width / 2
  const cy = centerY || canvas.height / 2

  ctx.save()
  ctx.translate(cx, cy)
  ctx.scale(scale, scale)
  ctx.translate(-cx, -cy)
}

// 拖拽工具函数
export function createDragTransform(
  canvas: HTMLCanvasElement,
  deltaX: number,
  deltaY: number
): void {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.save()
  ctx.translate(deltaX, deltaY)
}

// 重置画布变换
export function resetCanvasTransform(canvas: HTMLCanvasElement): void {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.restore()
  ctx.setTransform(1, 0, 0, 1, 0, 0)
}

// 节流函数（用于性能优化）
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): T {
  let lastCall = 0
  return ((...args: any[]) => {
    const now = Date.now()
    if (now - lastCall >= delay) {
      lastCall = now
      return func(...args)
    }
  }) as T
}

// 防抖函数（用于性能优化）
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  delay: number
): T {
  let timeoutId: NodeJS.Timeout
  return ((...args: any[]) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => func(...args), delay)
  }) as T
}

// 计算两点之间的距离
export function calculateDistance(x1: number, y1: number, x2: number, y2: number): number {
  return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2))
}

// 检查点是否在矩形内
export function isPointInRect(
  x: number,
  y: number,
  rectX: number,
  rectY: number,
  rectWidth: number,
  rectHeight: number
): boolean {
  return x >= rectX && x <= rectX + rectWidth && y >= rectY && y <= rectY + rectHeight
}

// 获取画布相对坐标
export function getCanvasRelativePosition(
  canvas: HTMLCanvasElement,
  clientX: number,
  clientY: number
): { x: number; y: number } {
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height

  return {
    x: (clientX - rect.left) * scaleX,
    y: (clientY - rect.top) * scaleY
  }
}

// 导出默认动画配置
export const DEFAULT_ANIMATION_CONFIG: AnimationConfig = {
  duration: 300,
  easing: 'ease-out'
}

// 导出默认交互效果配置
export const DEFAULT_INTERACTION_EFFECTS: Record<string, InteractionEffect> = {
  highlight: {
    type: 'highlight',
    intensity: 0.5,
    duration: 300
  },
  scale: {
    type: 'scale',
    intensity: 1.2,
    duration: 200
  },
  shadow: {
    type: 'shadow',
    intensity: 0.8,
    duration: 250,
    color: 'rgba(0, 0, 0, 0.3)'
  },
  glow: {
    type: 'glow',
    intensity: 1.0,
    duration: 400,
    color: 'rgba(255, 255, 255, 0.8)'
  }
}