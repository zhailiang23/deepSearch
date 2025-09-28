/**
 * 词云主题工具函数
 * 提供主题切换、颜色生成、配置合并等功能
 */

import type {
  WordCloudConfig,
  WordCloudThemeConfig,
  WordCloudColorConfig,
  WordCloudItem,
  WordCloudPresetTheme
} from '@/types/wordCloudTypes'

import {
  THEME_COLORS,
  PRESET_THEMES,
  DEFAULT_WORDCLOUD_CONFIG,
  LIGHT_THEME_CONFIG,
  DARK_THEME_CONFIG,
  SHAPE_FUNCTIONS,
  getPerformanceConfigBySize
} from '@/constants/wordCloudDefaults'

/**
 * 获取当前系统主题模式
 */
export const getCurrentThemeMode = (): 'light' | 'dark' => {
  // 检查localStorage中的主题设置
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark' || savedTheme === 'light') {
    return savedTheme
  }

  // 检查系统偏好
  if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }

  return 'light'
}

/**
 * 根据主题模式获取对应的主题配置
 */
export const getThemeByMode = (mode: 'light' | 'dark'): WordCloudThemeConfig => {
  return mode === 'dark' ? DARK_THEME_CONFIG : LIGHT_THEME_CONFIG
}

/**
 * 根据主题名称获取主题配置
 */
export const getThemeByName = (themeName: string): WordCloudThemeConfig => {
  const theme = PRESET_THEMES.find(t => t.name === themeName)
  if (!theme) {
    console.warn(`主题 "${themeName}" 不存在，使用默认主题`)
    return getThemeByMode(getCurrentThemeMode())
  }
  return theme
}

/**
 * 生成渐变颜色函数
 */
export const createGradientColorFunction = (colorConfig: WordCloudColorConfig) => {
  return (word: string, weight: string | number, fontSize: number, distance: number, theta: number): string => {
    const weightNum = typeof weight === 'string' ? parseFloat(weight) : weight

    if (colorConfig.type === 'gradient' && colorConfig.startColor && colorConfig.endColor) {
      // 基于权重在起始色和结束色之间插值
      const ratio = Math.min(weightNum / 100, 1) // 假设最大权重为100
      return interpolateColor(colorConfig.startColor, colorConfig.endColor, ratio)
    }

    if (colorConfig.type === 'custom' && colorConfig.colors && colorConfig.colors.length > 0) {
      // 基于权重或随机选择颜色
      const index = Math.floor((weightNum / 100) * colorConfig.colors.length) % colorConfig.colors.length
      return colorConfig.colors[index]
    }

    if (colorConfig.type === 'random-dark') {
      return generateRandomColor(0.3, 0.7, 0.2, 0.6)
    }

    if (colorConfig.type === 'random-light') {
      return generateRandomColor(0.5, 0.9, 0.4, 0.8)
    }

    // 默认返回主题色
    return THEME_COLORS.LIGHT.PRIMARY
  }
}

/**
 * 颜色插值函数
 */
export const interpolateColor = (startColor: string, endColor: string, ratio: number): string => {
  // 解析HSL颜色
  const start = parseHSL(startColor)
  const end = parseHSL(endColor)

  if (!start || !end) {
    return startColor
  }

  // 插值计算
  const h = start.h + (end.h - start.h) * ratio
  const s = start.s + (end.s - start.s) * ratio
  const l = start.l + (end.l - start.l) * ratio

  return `hsl(${h}, ${s}%, ${l}%)`
}

/**
 * 解析HSL颜色字符串
 */
const parseHSL = (hslString: string): { h: number; s: number; l: number } | null => {
  const match = hslString.match(/hsl\((\d+(?:\.\d+)?),\s*(\d+(?:\.\d+)?)%,\s*(\d+(?:\.\d+)?)%\)/)
  if (!match) return null

  return {
    h: parseFloat(match[1]),
    s: parseFloat(match[2]),
    l: parseFloat(match[3])
  }
}

/**
 * 生成随机颜色
 */
export const generateRandomColor = (
  hueMin: number = 0,
  hueMax: number = 1,
  satMin: number = 0.3,
  satMax: number = 0.8,
  lightMin: number = 0.3,
  lightMax: number = 0.7
): string => {
  const h = Math.floor((hueMin + Math.random() * (hueMax - hueMin)) * 360)
  const s = Math.floor((satMin + Math.random() * (satMax - satMin)) * 100)
  const l = Math.floor((lightMin + Math.random() * (lightMax - lightMin)) * 100)

  return `hsl(${h}, ${s}%, ${l}%)`
}

/**
 * 应用主题到wordcloud配置
 */
export const applyThemeToConfig = (
  baseConfig: Partial<WordCloudConfig>,
  theme: WordCloudThemeConfig
): WordCloudConfig => {
  const config: WordCloudConfig = {
    ...DEFAULT_WORDCLOUD_CONFIG,
    ...baseConfig
  }

  // 应用主题配置
  config.backgroundColor = theme.background
  config.fontFamily = theme.font.family
  config.fontWeight = theme.font.weight
  config.minSize = theme.font.minSize
  config.weightFactor = theme.font.weightFactor
  config.gridSize = theme.gridSize

  // 应用颜色配置
  config.color = createGradientColorFunction(theme.colors)

  // 应用旋转配置
  config.rotationSteps = theme.rotation.rotationSteps
  config.minRotation = theme.rotation.minRotation
  config.maxRotation = theme.rotation.maxRotation
  config.rotateRatio = theme.rotation.rotateRatio

  // 应用形状配置
  if (theme.shape.type in SHAPE_FUNCTIONS) {
    config.shape = SHAPE_FUNCTIONS[theme.shape.type as keyof typeof SHAPE_FUNCTIONS]
  }
  config.ellipticity = theme.shape.ellipticity

  return config
}

/**
 * 创建响应式配置
 */
export const createResponsiveConfig = (
  baseConfig: WordCloudConfig,
  containerWidth: number,
  containerHeight: number
): WordCloudConfig => {
  const config = { ...baseConfig }

  // 设置画布中心点
  config.origin = [containerWidth / 2, containerHeight / 2]

  // 根据容器大小调整配置
  if (containerWidth < 640) {
    // 移动端
    config.gridSize = Math.max(4, config.gridSize! - 2)
    config.minSize = Math.max(8, config.minSize! - 2)
  } else if (containerWidth < 1024) {
    // 平板
    config.gridSize = Math.max(6, config.gridSize! - 1)
    config.minSize = Math.max(10, config.minSize! - 1)
  }

  return config
}

/**
 * 优化大数据集配置
 */
export const optimizeConfigForDataset = (
  baseConfig: WordCloudConfig,
  words: WordCloudItem[]
): WordCloudConfig => {
  const config = { ...baseConfig }
  const wordCount = words.length

  // 应用性能优化配置
  const performanceConfig = getPerformanceConfigBySize(wordCount)
  Object.assign(config, performanceConfig)

  // 根据数据量调整字体大小
  if (wordCount > 500) {
    config.minSize = Math.max(8, config.minSize! - 2)
    config.weightFactor = typeof config.weightFactor === 'number'
      ? config.weightFactor * 0.8
      : config.weightFactor
  } else if (wordCount < 20) {
    config.minSize = config.minSize! + 2
    config.weightFactor = typeof config.weightFactor === 'number'
      ? config.weightFactor * 1.2
      : config.weightFactor
  }

  return config
}

/**
 * 创建自定义主题
 */
export const createCustomTheme = (
  baseName: string,
  customizations: Partial<WordCloudThemeConfig>
): WordCloudThemeConfig => {
  const baseTheme = getThemeByName(baseName)

  return {
    ...baseTheme,
    ...customizations,
    name: customizations.name || `${baseName}-custom`,
    colors: {
      ...baseTheme.colors,
      ...customizations.colors
    },
    font: {
      ...baseTheme.font,
      ...customizations.font
    },
    rotation: {
      ...baseTheme.rotation,
      ...customizations.rotation
    },
    shape: {
      ...baseTheme.shape,
      ...customizations.shape
    }
  }
}

/**
 * 导出主题配置为JSON
 */
export const exportThemeConfig = (theme: WordCloudThemeConfig): string => {
  return JSON.stringify(theme, null, 2)
}

/**
 * 从JSON导入主题配置
 */
export const importThemeConfig = (jsonString: string): WordCloudThemeConfig | null => {
  try {
    const config = JSON.parse(jsonString)

    // 验证必要字段
    if (!config.name || !config.mode || !config.colors || !config.font) {
      throw new Error('主题配置格式不正确')
    }

    return config as WordCloudThemeConfig
  } catch (error) {
    console.error('导入主题配置失败:', error)
    return null
  }
}

/**
 * 获取主题预览颜色
 */
export const getThemePreviewColors = (theme: WordCloudThemeConfig): string[] => {
  if (theme.colors.type === 'custom' && theme.colors.colors) {
    return theme.colors.colors.slice(0, 5)
  }

  if (theme.colors.type === 'gradient' && theme.colors.startColor && theme.colors.endColor) {
    const colors = []
    for (let i = 0; i < 5; i++) {
      const ratio = i / 4
      colors.push(interpolateColor(theme.colors.startColor, theme.colors.endColor, ratio))
    }
    return colors
  }

  // 返回默认色板
  return [
    THEME_COLORS.LIGHT.PRIMARY,
    THEME_COLORS.LIGHT.PRIMARY_LIGHT,
    THEME_COLORS.LIGHT.PRIMARY_DARK,
  ]
}

/**
 * 监听系统主题变化
 */
export const watchSystemTheme = (callback: (isDark: boolean) => void): (() => void) => {
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

  const handler = (e: MediaQueryListEvent) => {
    callback(e.matches)
  }

  mediaQuery.addEventListener('change', handler)

  // 返回清理函数
  return () => {
    mediaQuery.removeEventListener('change', handler)
  }
}

/**
 * 切换系统主题
 */
export const toggleSystemTheme = (): 'light' | 'dark' => {
  const htmlElement = document.documentElement
  const isDark = htmlElement.classList.contains('dark')

  if (isDark) {
    htmlElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
    return 'light'
  } else {
    htmlElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
    return 'dark'
  }
}

/**
 * 应用主题到DOM
 */
export const applyThemeToDOM = (mode: 'light' | 'dark'): void => {
  const htmlElement = document.documentElement

  if (mode === 'dark') {
    htmlElement.classList.add('dark')
  } else {
    htmlElement.classList.remove('dark')
  }

  localStorage.setItem('theme', mode)
}