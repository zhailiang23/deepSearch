/**
 * 词云组件相关的TypeScript类型定义
 * 基于wordcloud2.js库和项目需求设计
 */

// 词云数据项 - [单词, 权重, ...其他数据]
export type WordCloudItem = [string, number, ...any[]]

// 词云尺寸信息
export interface WordCloudDimension {
  x: number
  y: number
  w: number
  h: number
}

// 词云颜色配置
export interface WordCloudColorConfig {
  type: 'random-dark' | 'random-light' | 'gradient' | 'custom'
  colors?: string[]
  startColor?: string
  endColor?: string
}

// 词云字体配置
export interface WordCloudFontConfig {
  family: string
  weight: string | number
  minSize: number
  maxSize: number
  weightFactor: number | ((weight: number) => number)
}

// 词云旋转配置
export interface WordCloudRotationConfig {
  minRotation: number
  maxRotation: number
  rotationSteps: number
  rotateRatio: number
}

// 词云形状配置
export interface WordCloudShapeConfig {
  type: 'circle' | 'cardioid' | 'diamond' | 'triangle' | 'pentagon' | 'star' | 'custom'
  ellipticity?: number
  customShape?: (theta: number) => number
}

// 词云主题配置
export interface WordCloudThemeConfig {
  name: string
  mode: 'light' | 'dark'
  background: string
  colors: WordCloudColorConfig
  font: WordCloudFontConfig
  rotation: WordCloudRotationConfig
  shape: WordCloudShapeConfig
  gridSize: number
}

// 词云交互配置
export interface WordCloudInteractionConfig {
  enableHover: boolean
  enableClick: boolean
  hoverEffect?: 'highlight' | 'shadow' | 'scale' | 'none'
  clickAction?: 'search' | 'filter' | 'custom' | 'none'
}

// 词云性能配置
export interface WordCloudPerformanceConfig {
  wait: number
  abortThreshold: number
  shuffle: boolean
  clearCanvas: boolean
  drawOutOfBound: boolean
  shrinkToFit: boolean
}

// 词云完整配置
export interface WordCloudConfig {
  // 数据
  list: WordCloudItem[]

  // 外观
  fontFamily: string
  fontWeight: string | number
  color: string | ((word: string, weight: string | number, fontSize: number, distance: number, theta: number) => string)
  backgroundColor: string

  // 尺寸
  minSize: number
  weightFactor: number | ((weight: number) => number)
  gridSize: number
  origin?: [number, number]

  // 旋转
  rotationSteps: number
  minRotation: number
  maxRotation: number
  rotateRatio: number

  // 形状
  shape?: string | ((theta: number) => number)
  ellipticity?: number

  // 交互
  hover?: (item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => void
  click?: (item: WordCloudItem, dimension: WordCloudDimension, event: MouseEvent) => void

  // 性能
  clearCanvas: boolean
  drawOutOfBound: boolean
  shrinkToFit: boolean
  wait: number
  abortThreshold: number
  shuffle: boolean
}

// 词云组件Props
export interface WordCloudProps {
  words: WordCloudItem[]
  width?: number
  height?: number
  theme?: string
  config?: Partial<WordCloudConfig>
  interactive?: boolean
  loading?: boolean
  error?: string | null
  onWordClick?: (word: string, item: WordCloudItem) => void
  onWordHover?: (word: string | null, item: WordCloudItem | null) => void
}

// 词云组件事件
export interface WordCloudEvents {
  onStart: () => void
  onDrawn: () => void
  onStop: () => void
  onAbort: () => void
  onError: (error: Error) => void
}

// 词云配置面板Props
export interface WordCloudConfigProps {
  config: WordCloudThemeConfig
  themes: WordCloudThemeConfig[]
  onConfigChange: (config: Partial<WordCloudThemeConfig>) => void
  onThemeChange: (themeName: string) => void
  onReset: () => void
  onExport: () => void
}

// 词云导出配置
export interface WordCloudExportConfig {
  format: 'png' | 'jpg' | 'svg'
  quality: number
  scale: number
  filename: string
  includeBackground: boolean
}

// 预设主题类型
export type WordCloudPresetTheme =
  | 'light-green'    // 亮色淡绿主题
  | 'dark-green'     // 暗色淡绿主题
  | 'rainbow'        // 彩虹主题
  | 'monochrome'     // 单色主题
  | 'ocean'          // 海洋主题
  | 'forest'         // 森林主题
  | 'sunset'         // 日落主题

// 词云状态
export interface WordCloudState {
  isRendering: boolean
  hasError: boolean
  errorMessage: string | null
  renderProgress: number
  lastRenderTime: number
}

// 词云统计信息
export interface WordCloudStats {
  totalWords: number
  renderedWords: number
  maxWeight: number
  minWeight: number
  averageWeight: number
  renderTime: number
}