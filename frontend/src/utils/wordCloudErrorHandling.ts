/**
 * 词云图错误处理和边界情况处理工具
 * 提供全面的错误处理、边界检查、用户友好的错误信息等功能
 */

import type { HotWordItem, WordCloudOptions } from '@/types/statistics'

// ============= 错误类型定义 =============

export enum WordCloudErrorType {
  /** 数据验证错误 */
  DATA_VALIDATION = 'DATA_VALIDATION',
  /** 渲染错误 */
  RENDER_ERROR = 'RENDER_ERROR',
  /** 配置错误 */
  CONFIG_ERROR = 'CONFIG_ERROR',
  /** 浏览器兼容性错误 */
  COMPATIBILITY_ERROR = 'COMPATIBILITY_ERROR',
  /** 性能错误 */
  PERFORMANCE_ERROR = 'PERFORMANCE_ERROR',
  /** 网络错误 */
  NETWORK_ERROR = 'NETWORK_ERROR',
  /** 未知错误 */
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

export interface WordCloudError {
  /** 错误类型 */
  type: WordCloudErrorType
  /** 错误代码 */
  code: string
  /** 错误消息 */
  message: string
  /** 用户友好的消息 */
  userMessage: string
  /** 错误详情 */
  details?: any
  /** 修复建议 */
  suggestions?: string[]
  /** 时间戳 */
  timestamp: number
}

// ============= 数据验证 =============

/**
 * 验证词云数据
 */
export function validateWordCloudData(words: HotWordItem[]): {
  isValid: boolean
  errors: WordCloudError[]
  sanitizedWords: HotWordItem[]
} {
  const errors: WordCloudError[] = []
  const sanitizedWords: HotWordItem[] = []

  // 检查是否为数组
  if (!Array.isArray(words)) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'INVALID_ARRAY',
      'Words data must be an array',
      '词云数据必须是数组格式',
      { provided: typeof words },
      ['请确保传入的是有效的数组数据']
    ))
    return { isValid: false, errors, sanitizedWords: [] }
  }

  // 检查数组长度
  if (words.length === 0) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'EMPTY_ARRAY',
      'Words array is empty',
      '没有词语数据可显示',
      {},
      ['请提供至少一个词语数据']
    ))
    return { isValid: false, errors, sanitizedWords: [] }
  }

  if (words.length > 1000) {
    errors.push(createError(
      WordCloudErrorType.PERFORMANCE_ERROR,
      'TOO_MANY_WORDS',
      `Too many words: ${words.length}. Maximum recommended: 1000`,
      '词语数量过多，可能影响性能',
      { count: words.length, max: 1000 },
      ['建议限制词语数量在1000个以内', '考虑使用分页或筛选功能']
    ))
  }

  // 验证每个词语项
  words.forEach((word, index) => {
    const validation = validateWordItem(word, index)
    errors.push(...validation.errors)

    if (validation.sanitizedWord) {
      sanitizedWords.push(validation.sanitizedWord)
    }
  })

  // 检查重复词语
  const duplicates = findDuplicateWords(sanitizedWords)
  if (duplicates.length > 0) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'DUPLICATE_WORDS',
      `Duplicate words found: ${duplicates.join(', ')}`,
      '发现重复的词语',
      { duplicates },
      ['考虑合并重复词语的权重', '移除重复的词语条目']
    ))
  }

  return {
    isValid: errors.length === 0 || errors.every(e => e.type !== WordCloudErrorType.DATA_VALIDATION),
    errors,
    sanitizedWords: deduplicateWords(sanitizedWords)
  }
}

/**
 * 验证单个词语项
 */
function validateWordItem(word: any, index: number): {
  isValid: boolean
  errors: WordCloudError[]
  sanitizedWord?: HotWordItem
} {
  const errors: WordCloudError[] = []

  // 检查是否为对象
  if (!word || typeof word !== 'object') {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'INVALID_WORD_OBJECT',
      `Word at index ${index} is not an object`,
      `第${index + 1}个词语数据格式错误`,
      { index, provided: typeof word },
      ['确保每个词语都是包含text和weight字段的对象']
    ))
    return { isValid: false, errors }
  }

  // 验证text字段
  if (!word.text || typeof word.text !== 'string') {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'INVALID_TEXT',
      `Word at index ${index} has invalid text`,
      `第${index + 1}个词语的文本无效`,
      { index, text: word.text },
      ['确保text字段是非空字符串']
    ))
    return { isValid: false, errors }
  }

  const trimmedText = word.text.trim()
  if (trimmedText.length === 0) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'EMPTY_TEXT',
      `Word at index ${index} has empty text`,
      `第${index + 1}个词语的文本为空`,
      { index },
      ['确保词语文本不为空']
    ))
    return { isValid: false, errors }
  }

  if (trimmedText.length > 50) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'TEXT_TOO_LONG',
      `Word text at index ${index} is too long: ${trimmedText.length} characters`,
      `第${index + 1}个词语的文本过长`,
      { index, length: trimmedText.length, max: 50 },
      ['建议将词语长度控制在50个字符以内']
    ))
  }

  // 验证weight字段
  if (typeof word.weight !== 'number' || isNaN(word.weight)) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'INVALID_WEIGHT',
      `Word at index ${index} has invalid weight`,
      `第${index + 1}个词语的权重值无效`,
      { index, weight: word.weight },
      ['确保weight字段是有效数字']
    ))
    return { isValid: false, errors }
  }

  if (word.weight <= 0) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'INVALID_WEIGHT_VALUE',
      `Word at index ${index} has non-positive weight: ${word.weight}`,
      `第${index + 1}个词语的权重值必须大于0`,
      { index, weight: word.weight },
      ['确保权重值大于0']
    ))
    return { isValid: false, errors }
  }

  if (word.weight > 10000) {
    errors.push(createError(
      WordCloudErrorType.DATA_VALIDATION,
      'WEIGHT_TOO_LARGE',
      `Word weight at index ${index} is too large: ${word.weight}`,
      `第${index + 1}个词语的权重值过大`,
      { index, weight: word.weight, max: 10000 },
      ['建议将权重值控制在合理范围内(≤10000)']
    ))
  }

  // 创建净化后的词语对象
  const sanitizedWord: HotWordItem = {
    text: trimmedText,
    weight: Math.max(1, Math.min(10000, word.weight)), // 限制权重范围
    ...word.extraData && { extraData: word.extraData }
  }

  return {
    isValid: errors.length === 0,
    errors,
    sanitizedWord
  }
}

/**
 * 查找重复词语
 */
function findDuplicateWords(words: HotWordItem[]): string[] {
  const textCounts = new Map<string, number>()
  const duplicates: string[] = []

  words.forEach(word => {
    const count = textCounts.get(word.text) || 0
    textCounts.set(word.text, count + 1)

    if (count === 1) { // 第二次出现时标记为重复
      duplicates.push(word.text)
    }
  })

  return duplicates
}

/**
 * 去重词语（保留权重最高的）
 */
function deduplicateWords(words: HotWordItem[]): HotWordItem[] {
  const wordMap = new Map<string, HotWordItem>()

  words.forEach(word => {
    const existing = wordMap.get(word.text)
    if (!existing || word.weight > existing.weight) {
      wordMap.set(word.text, word)
    }
  })

  return Array.from(wordMap.values())
}

// ============= 配置验证 =============

/**
 * 验证词云配置
 */
export function validateWordCloudOptions(options: Partial<WordCloudOptions>): {
  isValid: boolean
  errors: WordCloudError[]
  sanitizedOptions: Partial<WordCloudOptions>
} {
  const errors: WordCloudError[] = []
  const sanitizedOptions: Partial<WordCloudOptions> = { ...options }

  // 验证尺寸配置
  if (options.width !== undefined) {
    if (typeof options.width !== 'number' || options.width <= 0) {
      errors.push(createError(
        WordCloudErrorType.CONFIG_ERROR,
        'INVALID_WIDTH',
        `Invalid width: ${options.width}`,
        '无效的宽度配置',
        { width: options.width },
        ['确保宽度是正数']
      ))
    } else {
      sanitizedOptions.width = Math.max(100, Math.min(4000, options.width))
    }
  }

  if (options.height !== undefined) {
    if (typeof options.height !== 'number' || options.height <= 0) {
      errors.push(createError(
        WordCloudErrorType.CONFIG_ERROR,
        'INVALID_HEIGHT',
        `Invalid height: ${options.height}`,
        '无效的高度配置',
        { height: options.height },
        ['确保高度是正数']
      ))
    } else {
      sanitizedOptions.height = Math.max(100, Math.min(3000, options.height))
    }
  }

  // 验证字体配置
  if (options.fontFamily !== undefined && typeof options.fontFamily !== 'string') {
    errors.push(createError(
      WordCloudErrorType.CONFIG_ERROR,
      'INVALID_FONT_FAMILY',
      `Invalid fontFamily: ${options.fontFamily}`,
      '无效的字体系列配置',
      { fontFamily: options.fontFamily },
      ['确保字体系列是有效字符串']
    ))
    delete sanitizedOptions.fontFamily
  }

  // 验证网格大小
  if (options.gridSize !== undefined) {
    if (typeof options.gridSize !== 'number' || options.gridSize <= 0) {
      errors.push(createError(
        WordCloudErrorType.CONFIG_ERROR,
        'INVALID_GRID_SIZE',
        `Invalid gridSize: ${options.gridSize}`,
        '无效的网格大小配置',
        { gridSize: options.gridSize },
        ['确保网格大小是正数']
      ))
    } else {
      sanitizedOptions.gridSize = Math.max(1, Math.min(50, options.gridSize))
    }
  }

  // 验证旋转配置
  if (options.rotationSteps !== undefined) {
    if (typeof options.rotationSteps !== 'number' || options.rotationSteps < 0) {
      errors.push(createError(
        WordCloudErrorType.CONFIG_ERROR,
        'INVALID_ROTATION_STEPS',
        `Invalid rotationSteps: ${options.rotationSteps}`,
        '无效的旋转步数配置',
        { rotationSteps: options.rotationSteps },
        ['确保旋转步数是非负数']
      ))
    } else {
      sanitizedOptions.rotationSteps = Math.max(0, Math.min(10, options.rotationSteps))
    }
  }

  if (options.rotateRatio !== undefined) {
    if (typeof options.rotateRatio !== 'number' || options.rotateRatio < 0 || options.rotateRatio > 1) {
      errors.push(createError(
        WordCloudErrorType.CONFIG_ERROR,
        'INVALID_ROTATE_RATIO',
        `Invalid rotateRatio: ${options.rotateRatio}`,
        '无效的旋转比例配置',
        { rotateRatio: options.rotateRatio },
        ['确保旋转比例在0-1之间']
      ))
    } else {
      sanitizedOptions.rotateRatio = Math.max(0, Math.min(1, options.rotateRatio))
    }
  }

  return {
    isValid: errors.length === 0,
    errors,
    sanitizedOptions
  }
}

// ============= 浏览器兼容性检查 =============

/**
 * 检查浏览器兼容性
 */
export function checkBrowserCompatibility(): {
  isSupported: boolean
  errors: WordCloudError[]
  missingFeatures: string[]
} {
  const errors: WordCloudError[] = []
  const missingFeatures: string[] = []

  // 检查Canvas支持
  if (!window.HTMLCanvasElement) {
    missingFeatures.push('Canvas')
    errors.push(createError(
      WordCloudErrorType.COMPATIBILITY_ERROR,
      'NO_CANVAS_SUPPORT',
      'Canvas is not supported',
      '当前浏览器不支持Canvas',
      {},
      ['请使用支持Canvas的现代浏览器', '推荐使用Chrome、Firefox、Safari或Edge']
    ))
  }

  // 检查Canvas 2D Context支持
  if (window.HTMLCanvasElement) {
    try {
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        missingFeatures.push('Canvas 2D Context')
        errors.push(createError(
          WordCloudErrorType.COMPATIBILITY_ERROR,
          'NO_2D_CONTEXT',
          'Canvas 2D context is not supported',
          '不支持Canvas 2D上下文',
          {},
          ['请检查浏览器是否启用了Canvas功能']
        ))
      }
    } catch (error) {
      missingFeatures.push('Canvas 2D Context')
      errors.push(createError(
        WordCloudErrorType.COMPATIBILITY_ERROR,
        'CANVAS_ERROR',
        'Error accessing Canvas',
        'Canvas访问错误',
        { error: error instanceof Error ? error.message : String(error) },
        ['请检查浏览器设置和安全策略']
      ))
    }
  }

  // 检查requestAnimationFrame支持
  if (!window.requestAnimationFrame) {
    missingFeatures.push('RequestAnimationFrame')
    errors.push(createError(
      WordCloudErrorType.COMPATIBILITY_ERROR,
      'NO_RAF_SUPPORT',
      'RequestAnimationFrame is not supported',
      '不支持RequestAnimationFrame',
      {},
      ['部分动画功能可能无法正常工作']
    ))
  }

  // 检查ResizeObserver支持
  if (!window.ResizeObserver) {
    missingFeatures.push('ResizeObserver')
    // 这不是致命错误，只是功能受限
  }

  // 检查最小屏幕尺寸
  if (window.screen && (window.screen.width < 320 || window.screen.height < 240)) {
    errors.push(createError(
      WordCloudErrorType.COMPATIBILITY_ERROR,
      'SCREEN_TOO_SMALL',
      'Screen size too small',
      '屏幕尺寸过小',
      { width: window.screen.width, height: window.screen.height },
      ['在小屏幕设备上词云显示效果可能不佳']
    ))
  }

  return {
    isSupported: !missingFeatures.includes('Canvas') && !missingFeatures.includes('Canvas 2D Context'),
    errors,
    missingFeatures
  }
}

// ============= 性能监控 =============

/**
 * 检查性能相关问题
 */
export function checkPerformanceConstraints(
  wordCount: number,
  canvasWidth: number,
  canvasHeight: number
): {
  isOptimal: boolean
  warnings: WordCloudError[]
  recommendations: string[]
} {
  const warnings: WordCloudError[] = []
  const recommendations: string[] = []

  // 检查词语数量
  if (wordCount > 500) {
    warnings.push(createError(
      WordCloudErrorType.PERFORMANCE_ERROR,
      'HIGH_WORD_COUNT',
      `High word count: ${wordCount}`,
      '词语数量较多，可能影响性能',
      { wordCount, recommended: 500 },
      ['考虑限制显示的词语数量', '使用分页或筛选功能']
    ))
    recommendations.push('减少词语数量到500以下')
  }

  // 检查画布尺寸
  const canvasArea = canvasWidth * canvasHeight
  if (canvasArea > 2000000) { // 超过2M像素
    warnings.push(createError(
      WordCloudErrorType.PERFORMANCE_ERROR,
      'LARGE_CANVAS_SIZE',
      `Large canvas size: ${canvasWidth}x${canvasHeight}`,
      '画布尺寸过大，可能影响渲染性能',
      { width: canvasWidth, height: canvasHeight, area: canvasArea },
      ['考虑减小画布尺寸', '在高DPI屏幕上限制像素比']
    ))
    recommendations.push('减小画布尺寸')
  }

  // 检查内存使用
  if (typeof performance !== 'undefined' && (performance as any).memory) {
    const memoryInfo = (performance as any).memory
    const usageRatio = memoryInfo.usedJSHeapSize / memoryInfo.totalJSHeapSize

    if (usageRatio > 0.8) {
      warnings.push(createError(
        WordCloudErrorType.PERFORMANCE_ERROR,
        'HIGH_MEMORY_USAGE',
        `High memory usage: ${(usageRatio * 100).toFixed(1)}%`,
        '内存使用率过高',
        { usageRatio, used: memoryInfo.usedJSHeapSize, total: memoryInfo.totalJSHeapSize },
        ['清理不需要的缓存', '减少词语数量或画布尺寸']
      ))
      recommendations.push('优化内存使用')
    }
  }

  // 检查设备性能
  if (navigator.hardwareConcurrency && navigator.hardwareConcurrency < 4) {
    warnings.push(createError(
      WordCloudErrorType.PERFORMANCE_ERROR,
      'LOW_CPU_CORES',
      `Low CPU core count: ${navigator.hardwareConcurrency}`,
      '设备CPU核心数较少',
      { cores: navigator.hardwareConcurrency },
      ['在低性能设备上可能需要更长的渲染时间']
    ))
    recommendations.push('启用性能优化模式')
  }

  return {
    isOptimal: warnings.length === 0,
    warnings,
    recommendations
  }
}

// ============= 错误恢复 =============

/**
 * 尝试从错误中恢复
 */
export function attemptErrorRecovery(error: WordCloudError): {
  canRecover: boolean
  recoverySteps: string[]
  fallbackOptions?: Partial<WordCloudOptions>
} {
  const recoverySteps: string[] = []
  let fallbackOptions: Partial<WordCloudOptions> | undefined

  switch (error.type) {
    case WordCloudErrorType.DATA_VALIDATION:
      return {
        canRecover: true,
        recoverySteps: [
          '清理无效数据',
          '应用数据净化',
          '重新验证数据',
          '重试渲染'
        ]
      }

    case WordCloudErrorType.PERFORMANCE_ERROR:
      fallbackOptions = {
        gridSize: 16,
        rotationSteps: 2,
        shrinkToFit: true
      }
      return {
        canRecover: true,
        recoverySteps: [
          '启用性能优化模式',
          '减少词语数量',
          '降低渲染质量',
          '重试渲染'
        ],
        fallbackOptions
      }

    case WordCloudErrorType.CONFIG_ERROR:
      fallbackOptions = {
        width: 800,
        height: 600,
        gridSize: 8,
        fontFamily: 'Arial, sans-serif',
        rotationSteps: 4,
        rotateRatio: 0.5
      }
      return {
        canRecover: true,
        recoverySteps: [
          '应用默认配置',
          '重新验证配置',
          '重试渲染'
        ],
        fallbackOptions
      }

    case WordCloudErrorType.COMPATIBILITY_ERROR:
      return {
        canRecover: false,
        recoverySteps: [
          '提示用户升级浏览器',
          '显示兼容性说明',
          '提供替代方案'
        ]
      }

    default:
      return {
        canRecover: true,
        recoverySteps: [
          '清除缓存',
          '重置配置',
          '重新初始化',
          '重试操作'
        ]
      }
  }
}

// ============= 工具函数 =============

/**
 * 创建错误对象
 */
function createError(
  type: WordCloudErrorType,
  code: string,
  message: string,
  userMessage: string,
  details?: any,
  suggestions?: string[]
): WordCloudError {
  return {
    type,
    code,
    message,
    userMessage,
    details,
    suggestions: suggestions || [],
    timestamp: Date.now()
  }
}

/**
 * 格式化错误信息用于显示
 */
export function formatErrorForDisplay(error: WordCloudError): string {
  let display = error.userMessage

  if (error.suggestions && error.suggestions.length > 0) {
    display += '\n\n解决建议：\n' + error.suggestions.map(s => `• ${s}`).join('\n')
  }

  return display
}

/**
 * 检查是否为致命错误
 */
export function isFatalError(error: WordCloudError): boolean {
  return error.type === WordCloudErrorType.COMPATIBILITY_ERROR &&
         (error.code === 'NO_CANVAS_SUPPORT' || error.code === 'NO_2D_CONTEXT')
}

/**
 * 获取错误严重程度
 */
export function getErrorSeverity(error: WordCloudError): 'low' | 'medium' | 'high' | 'critical' {
  if (isFatalError(error)) {
    return 'critical'
  }

  switch (error.type) {
    case WordCloudErrorType.DATA_VALIDATION:
      return error.code.includes('INVALID') ? 'high' : 'medium'
    case WordCloudErrorType.CONFIG_ERROR:
      return 'medium'
    case WordCloudErrorType.PERFORMANCE_ERROR:
      return 'low'
    case WordCloudErrorType.RENDER_ERROR:
      return 'high'
    default:
      return 'medium'
  }
}

/**
 * 收集和聚合多个错误
 */
export function aggregateErrors(errors: WordCloudError[]): {
  criticalErrors: WordCloudError[]
  warnings: WordCloudError[]
  suggestions: string[]
  canProceed: boolean
} {
  const criticalErrors = errors.filter(e => getErrorSeverity(e) === 'critical')
  const warnings = errors.filter(e => getErrorSeverity(e) !== 'critical')

  const allSuggestions = errors.flatMap(e => e.suggestions || [])
  const uniqueSuggestions = Array.from(new Set(allSuggestions))

  return {
    criticalErrors,
    warnings,
    suggestions: uniqueSuggestions,
    canProceed: criticalErrors.length === 0
  }
}