/**
 * 词云组件默认配置常量
 * 基于项目淡绿色主题和wordcloud2.js最佳实践
 */

import type {
  WordCloudConfig,
  WordCloudThemeConfig,
  WordCloudColorConfig,
  WordCloudFontConfig,
  WordCloudRotationConfig,
  WordCloudShapeConfig,
  WordCloudInteractionConfig,
  WordCloudPerformanceConfig,
  WordCloudExportConfig
} from '@/types/wordCloudTypes'

// 淡绿色主题色值 (与项目主题一致)
export const THEME_COLORS = {
  // 亮色模式
  LIGHT: {
    PRIMARY: 'hsl(142.1, 76.2%, 36.3%)',      // --primary
    PRIMARY_LIGHT: 'hsl(142.1, 76.2%, 50%)',
    PRIMARY_DARK: 'hsl(142.1, 76.2%, 25%)',
    SECONDARY: 'hsl(138, 62%, 96%)',          // --secondary
    BACKGROUND: 'hsl(0, 0%, 100%)',           // --background
    FOREGROUND: 'hsl(142.1, 84%, 4.9%)',     // --foreground
    MUTED: 'hsl(142.4, 16.3%, 46.9%)',       // --muted-foreground
    ACCENT: 'hsl(138, 62%, 96%)',             // --accent
  },

  // 暗色模式
  DARK: {
    PRIMARY: 'hsl(142.1, 91.2%, 59.8%)',      // --primary (dark)
    PRIMARY_LIGHT: 'hsl(142.1, 91.2%, 70%)',
    PRIMARY_DARK: 'hsl(142.1, 91.2%, 45%)',
    SECONDARY: 'hsl(142.2, 32.6%, 17.5%)',    // --secondary (dark)
    BACKGROUND: 'hsl(142.2, 84%, 4.9%)',      // --background (dark)
    FOREGROUND: 'hsl(138, 40%, 98%)',         // --foreground (dark)
    MUTED: 'hsl(142, 20.2%, 65.1%)',          // --muted-foreground (dark)
    ACCENT: 'hsl(142.2, 32.6%, 17.5%)',       // --accent (dark)
  }
} as const

// 默认颜色配置
export const DEFAULT_COLOR_CONFIG: WordCloudColorConfig = {
  type: 'gradient',
  startColor: THEME_COLORS.LIGHT.PRIMARY_DARK,
  endColor: THEME_COLORS.LIGHT.PRIMARY_LIGHT,
  colors: [
    THEME_COLORS.LIGHT.PRIMARY,
    THEME_COLORS.LIGHT.PRIMARY_LIGHT,
    THEME_COLORS.LIGHT.PRIMARY_DARK,
    'hsl(142.1, 76.2%, 60%)',
    'hsl(142.1, 76.2%, 30%)',
  ]
}

// 默认字体配置
export const DEFAULT_FONT_CONFIG: WordCloudFontConfig = {
  family: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  weight: 'bold',
  minSize: 20,
  maxSize: 80,
  weightFactor: 3
}

// 默认旋转配置
export const DEFAULT_ROTATION_CONFIG: WordCloudRotationConfig = {
  minRotation: -Math.PI / 4,
  maxRotation: Math.PI / 4,
  rotationSteps: 4,
  rotateRatio: 0.3
}

// 默认形状配置
export const DEFAULT_SHAPE_CONFIG: WordCloudShapeConfig = {
  type: 'circle',
  ellipticity: 0.65
}

// 默认交互配置
export const DEFAULT_INTERACTION_CONFIG: WordCloudInteractionConfig = {
  enableHover: true,
  enableClick: true,
  hoverEffect: 'highlight',
  clickAction: 'search'
}

// 默认性能配置
export const DEFAULT_PERFORMANCE_CONFIG: WordCloudPerformanceConfig = {
  wait: 0,
  abortThreshold: 10000,
  shuffle: true,
  clearCanvas: true,
  drawOutOfBound: false,
  shrinkToFit: true
}

// 亮色主题配置
export const LIGHT_THEME_CONFIG: WordCloudThemeConfig = {
  name: 'light-green',
  mode: 'light',
  background: THEME_COLORS.LIGHT.BACKGROUND,
  colors: DEFAULT_COLOR_CONFIG,
  font: DEFAULT_FONT_CONFIG,
  rotation: DEFAULT_ROTATION_CONFIG,
  shape: DEFAULT_SHAPE_CONFIG,
  gridSize: 16
}

// 暗色主题配置
export const DARK_THEME_CONFIG: WordCloudThemeConfig = {
  name: 'dark-green',
  mode: 'dark',
  background: THEME_COLORS.DARK.BACKGROUND,
  colors: {
    type: 'gradient',
    startColor: THEME_COLORS.DARK.PRIMARY_DARK,
    endColor: THEME_COLORS.DARK.PRIMARY_LIGHT,
    colors: [
      THEME_COLORS.DARK.PRIMARY,
      THEME_COLORS.DARK.PRIMARY_LIGHT,
      THEME_COLORS.DARK.PRIMARY_DARK,
      'hsl(142.1, 91.2%, 70%)',
      'hsl(142.1, 91.2%, 45%)',
    ]
  },
  font: {
    ...DEFAULT_FONT_CONFIG,
    weight: '600'
  },
  rotation: DEFAULT_ROTATION_CONFIG,
  shape: DEFAULT_SHAPE_CONFIG,
  gridSize: 16
}

// 默认wordcloud2.js配置
export const DEFAULT_WORDCLOUD_CONFIG: WordCloudConfig = {
  list: [],
  fontFamily: DEFAULT_FONT_CONFIG.family,
  fontWeight: DEFAULT_FONT_CONFIG.weight,
  color: (word: string, weight: string | number, fontSize: number, distance: number, theta: number) => {
    // 基于权重返回渐变色
    const weightNum = typeof weight === 'string' ? parseFloat(weight) : weight
    const ratio = Math.min(weightNum / 100, 1) // 假设最大权重为100
    const hue = 142.1 // 淡绿色色相
    const saturation = 76.2
    const lightness = 36.3 + (ratio * 20) // 根据权重调整亮度
    return `hsl(${hue}, ${saturation}%, ${lightness}%)`
  },
  backgroundColor: THEME_COLORS.LIGHT.BACKGROUND,
  minSize: DEFAULT_FONT_CONFIG.minSize,
  weightFactor: DEFAULT_FONT_CONFIG.weightFactor,
  gridSize: 16,
  origin: undefined, // 将由组件计算
  rotationSteps: DEFAULT_ROTATION_CONFIG.rotationSteps,
  minRotation: DEFAULT_ROTATION_CONFIG.minRotation,
  maxRotation: DEFAULT_ROTATION_CONFIG.maxRotation,
  rotateRatio: DEFAULT_ROTATION_CONFIG.rotateRatio,
  shape: 'circle',
  ellipticity: DEFAULT_SHAPE_CONFIG.ellipticity,
  clearCanvas: DEFAULT_PERFORMANCE_CONFIG.clearCanvas,
  drawOutOfBound: DEFAULT_PERFORMANCE_CONFIG.drawOutOfBound,
  shrinkToFit: DEFAULT_PERFORMANCE_CONFIG.shrinkToFit,
  wait: DEFAULT_PERFORMANCE_CONFIG.wait,
  abortThreshold: DEFAULT_PERFORMANCE_CONFIG.abortThreshold,
  shuffle: DEFAULT_PERFORMANCE_CONFIG.shuffle
}

// 导出配置默认值
export const DEFAULT_EXPORT_CONFIG: WordCloudExportConfig = {
  format: 'png',
  quality: 0.9,
  scale: 2,
  filename: 'wordcloud',
  includeBackground: true
}

// 预设主题列表
export const PRESET_THEMES: WordCloudThemeConfig[] = [
  LIGHT_THEME_CONFIG,
  DARK_THEME_CONFIG,

  // 彩虹主题
  {
    name: 'rainbow',
    mode: 'light',
    background: THEME_COLORS.LIGHT.BACKGROUND,
    colors: {
      type: 'custom',
      colors: [
        'hsl(0, 70%, 50%)',    // 红
        'hsl(30, 70%, 50%)',   // 橙
        'hsl(60, 70%, 50%)',   // 黄
        'hsl(120, 70%, 50%)',  // 绿
        'hsl(240, 70%, 50%)',  // 蓝
        'hsl(270, 70%, 50%)',  // 紫
      ]
    },
    font: DEFAULT_FONT_CONFIG,
    rotation: DEFAULT_ROTATION_CONFIG,
    shape: DEFAULT_SHAPE_CONFIG,
    gridSize: 16
  },

  // 单色主题
  {
    name: 'monochrome',
    mode: 'light',
    background: THEME_COLORS.LIGHT.BACKGROUND,
    colors: {
      type: 'gradient',
      startColor: 'hsl(0, 0%, 20%)',
      endColor: 'hsl(0, 0%, 60%)',
      colors: [
        'hsl(0, 0%, 20%)',
        'hsl(0, 0%, 35%)',
        'hsl(0, 0%, 50%)',
        'hsl(0, 0%, 65%)',
      ]
    },
    font: DEFAULT_FONT_CONFIG,
    rotation: DEFAULT_ROTATION_CONFIG,
    shape: DEFAULT_SHAPE_CONFIG,
    gridSize: 16
  },

  // 海洋主题
  {
    name: 'ocean',
    mode: 'light',
    background: 'hsl(200, 100%, 97%)',
    colors: {
      type: 'gradient',
      startColor: 'hsl(200, 80%, 30%)',
      endColor: 'hsl(220, 80%, 60%)',
      colors: [
        'hsl(200, 80%, 30%)',
        'hsl(210, 80%, 40%)',
        'hsl(220, 80%, 50%)',
        'hsl(230, 80%, 60%)',
      ]
    },
    font: DEFAULT_FONT_CONFIG,
    rotation: DEFAULT_ROTATION_CONFIG,
    shape: DEFAULT_SHAPE_CONFIG,
    gridSize: 16
  }
]

// 形状函数映射
export const SHAPE_FUNCTIONS = {
  circle: 'circle',
  cardioid: 'cardioid',
  diamond: (theta: number) => {
    const t = theta % (2 * Math.PI)
    return Math.abs(Math.cos(t * 2))
  },
  triangle: (theta: number) => {
    const t = theta % (2 * Math.PI)
    return Math.abs(Math.cos(t * 1.5))
  },
  pentagon: (theta: number) => {
    const t = theta % (2 * Math.PI)
    return Math.abs(Math.cos(t * 2.5))
  },
  star: (theta: number) => {
    const t = theta % (2 * Math.PI)
    return (Math.cos(t * 5) + 1) / 2
  }
} as const

// 响应式断点
export const RESPONSIVE_BREAKPOINTS = {
  mobile: { maxWidth: 640, gridSize: 12, minSize: 16, maxSize: 50 },
  tablet: { maxWidth: 1024, gridSize: 16, minSize: 18, maxSize: 65 },
  desktop: { maxWidth: Infinity, gridSize: 20, minSize: 20, maxSize: 80 }
} as const

// 性能阈值
export const PERFORMANCE_THRESHOLDS = {
  SMALL_DATASET: 50,      // 小数据集：50个词以下
  MEDIUM_DATASET: 200,    // 中数据集：50-200个词
  LARGE_DATASET: 500,     // 大数据集：200-500个词
  HUGE_DATASET: 1000      // 超大数据集：500个词以上
} as const

// 根据数据集大小调整性能配置
export const getPerformanceConfigBySize = (wordCount: number): Partial<WordCloudConfig> => {
  if (wordCount <= PERFORMANCE_THRESHOLDS.SMALL_DATASET) {
    return { wait: 0, abortThreshold: 5000, gridSize: 20 }
  } else if (wordCount <= PERFORMANCE_THRESHOLDS.MEDIUM_DATASET) {
    return { wait: 1, abortThreshold: 8000, gridSize: 16 }
  } else if (wordCount <= PERFORMANCE_THRESHOLDS.LARGE_DATASET) {
    return { wait: 2, abortThreshold: 12000, gridSize: 12 }
  } else {
    return { wait: 5, abortThreshold: 15000, gridSize: 8 }
  }
}