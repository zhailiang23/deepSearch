/**
 * 词云性能基准测试工具
 * 提供完整的性能测试、基准对比、优化建议功能
 */

import type { HotWordItem, WordCloudOptions, WordCloudPerformanceConfig } from '@/types/statistics'
import {
  generateTestData,
  generateLargeTestData,
  generatePerformanceTestData,
  generateEdgeCaseTestData
} from './testDataGenerator'
import {
  detectDevicePerformance,
  generateOptimizedConfig,
  measureFPS,
  getMemoryInfo,
  type OptimizationContext,
  type RenderOptimizationResult
} from './wordCloudOptimizations'

// ============= 类型定义 =============

export interface BenchmarkConfig {
  /** 测试名称 */
  name: string
  /** 词语数量 */
  wordCount: number
  /** 容器尺寸 */
  containerSize: { width: number; height: number }
  /** 词云配置 */
  wordCloudConfig: Partial<WordCloudOptions>
  /** 性能配置 */
  performanceConfig: Partial<WordCloudPerformanceConfig>
  /** 测试迭代次数 */
  iterations: number
}

export interface BenchmarkResult {
  /** 测试配置 */
  config: BenchmarkConfig
  /** 测试结果 */
  results: TestResult[]
  /** 统计信息 */
  statistics: BenchmarkStatistics
  /** 时间戳 */
  timestamp: number
  /** 设备信息 */
  deviceInfo: DeviceInfo
}

export interface TestResult {
  /** 迭代索引 */
  iteration: number
  /** 渲染时间 (毫秒) */
  renderTime: number
  /** 数据处理时间 (毫秒) */
  dataProcessingTime: number
  /** 内存使用 (字节) */
  memoryUsage: number
  /** FPS */
  fps: number
  /** 是否成功 */
  success: boolean
  /** 错误信息 */
  error?: string
  /** 优化建议 */
  optimizations?: string[]
}

export interface BenchmarkStatistics {
  /** 平均渲染时间 */
  avgRenderTime: number
  /** 最小渲染时间 */
  minRenderTime: number
  /** 最大渲染时间 */
  maxRenderTime: number
  /** 渲染时间标准差 */
  renderTimeStdDev: number
  /** 平均内存使用 */
  avgMemoryUsage: number
  /** 最大内存使用 */
  maxMemoryUsage: number
  /** 平均FPS */
  avgFPS: number
  /** 成功率 */
  successRate: number
  /** 性能等级 */
  performanceLevel: 'low' | 'medium' | 'high'
  /** 稳定性评分 (0-100) */
  stabilityScore: number
}

export interface DeviceInfo {
  /** 用户代理 */
  userAgent: string
  /** 屏幕尺寸 */
  screenSize: { width: number; height: number }
  /** 设备像素比 */
  devicePixelRatio: number
  /** 硬件并发数 */
  hardwareConcurrency: number
  /** 内存信息 */
  memory: {
    total: number
    used: number
    available: number
  }
  /** 设备性能等级 */
  performanceLevel: 'low' | 'medium' | 'high'
  /** 是否移动设备 */
  isMobile: boolean
}

// ============= 基准测试器类 =============

export class WordCloudBenchmark {
  private results: BenchmarkResult[] = []
  private isRunning = false

  /**
   * 运行标准基准测试套件
   */
  async runStandardBenchmark(): Promise<BenchmarkResult[]> {
    if (this.isRunning) {
      throw new Error('基准测试已在运行中')
    }

    this.isRunning = true
    this.results = []

    try {
      const standardConfigs = this.getStandardBenchmarkConfigs()

      for (const config of standardConfigs) {
        console.log(`运行基准测试: ${config.name}`)
        const result = await this.runSingleBenchmark(config)
        this.results.push(result)

        // 清理内存并等待
        await this.cleanupAndWait()
      }

      return this.results
    } finally {
      this.isRunning = false
    }
  }

  /**
   * 运行自定义基准测试
   */
  async runCustomBenchmark(config: BenchmarkConfig): Promise<BenchmarkResult> {
    const result = await this.runSingleBenchmark(config)
    this.results.push(result)
    return result
  }

  /**
   * 运行单个基准测试
   */
  private async runSingleBenchmark(config: BenchmarkConfig): Promise<BenchmarkResult> {
    const deviceInfo = await this.getDeviceInfo()
    const testResults: TestResult[] = []

    for (let i = 0; i < config.iterations; i++) {
      console.log(`  迭代 ${i + 1}/${config.iterations}`)

      try {
        const result = await this.runSingleIteration(config, i)
        testResults.push(result)
      } catch (error) {
        testResults.push({
          iteration: i,
          renderTime: 0,
          dataProcessingTime: 0,
          memoryUsage: 0,
          fps: 0,
          success: false,
          error: error instanceof Error ? error.message : String(error)
        })
      }

      // 迭代间等待，让系统稳定
      if (i < config.iterations - 1) {
        await new Promise(resolve => setTimeout(resolve, 100))
      }
    }

    const statistics = this.calculateStatistics(testResults)

    return {
      config,
      results: testResults,
      statistics,
      timestamp: Date.now(),
      deviceInfo
    }
  }

  /**
   * 运行单次迭代
   */
  private async runSingleIteration(config: BenchmarkConfig, iteration: number): Promise<TestResult> {
    // 生成测试数据
    const testData = generatePerformanceTestData(config.wordCount, 'medium')

    // 获取基准内存
    const startMemory = getMemoryInfo()

    // 数据处理计时
    const dataProcessingStart = performance.now()
    const processedData = this.preprocessData(testData)
    const dataProcessingTime = performance.now() - dataProcessingStart

    // 创建模拟Canvas
    const canvas = document.createElement('canvas')
    canvas.width = config.containerSize.width
    canvas.height = config.containerSize.height

    // 渲染计时
    const renderStart = performance.now()
    await this.simulateRender(canvas, processedData, config.wordCloudConfig)
    const renderTime = performance.now() - renderStart

    // 计算内存使用
    const endMemory = getMemoryInfo()
    const memoryUsage = Math.max(0, endMemory.used - startMemory.used)

    // 测量FPS
    const fps = await measureFPS(100) // 测量100ms

    // 生成优化建议
    const optimizations = this.generateOptimizations(renderTime, memoryUsage, fps, config.wordCount)

    return {
      iteration,
      renderTime: Math.round(renderTime),
      dataProcessingTime: Math.round(dataProcessingTime),
      memoryUsage,
      fps,
      success: renderTime < 10000 && memoryUsage < 100 * 1024 * 1024,
      optimizations
    }
  }

  /**
   * 预处理数据
   */
  private preprocessData(data: HotWordItem[]): HotWordItem[] {
    // 模拟数据预处理
    return data
      .filter(item => item.text && item.weight > 0)
      .sort((a, b) => b.weight - a.weight)
      .map(item => ({
        ...item,
        weight: Math.round(item.weight * 100) / 100
      }))
  }

  /**
   * 模拟渲染过程
   */
  private async simulateRender(
    canvas: HTMLCanvasElement,
    data: HotWordItem[],
    config: Partial<WordCloudOptions>
  ): Promise<void> {
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('无法获取Canvas上下文')

    // 模拟wordcloud2.js的渲染过程
    const gridSize = config.gridSize || 8
    const words = data.slice(0, 200) // 限制最大词数

    for (let i = 0; i < words.length; i++) {
      const word = words[i]

      // 模拟字体大小计算
      const fontSize = Math.max(12, Math.min(48, word.weight * 0.8))

      // 模拟位置计算 (简化版)
      const x = Math.random() * (canvas.width - fontSize * word.text.length * 0.6)
      const y = Math.random() * (canvas.height - fontSize)

      // 模拟绘制
      ctx.font = `${fontSize}px Arial`
      ctx.fillStyle = `hsl(${Math.random() * 60 + 120}, 70%, 50%)`
      ctx.fillText(word.text, x, y)

      // 模拟逐步渲染的延迟
      if (i % 10 === 0) {
        await new Promise(resolve => setTimeout(resolve, 1))
      }
    }
  }

  /**
   * 计算统计信息
   */
  private calculateStatistics(results: TestResult[]): BenchmarkStatistics {
    const successfulResults = results.filter(r => r.success)
    const renderTimes = successfulResults.map(r => r.renderTime)
    const memoryUsages = successfulResults.map(r => r.memoryUsage)
    const fpsList = successfulResults.map(r => r.fps)

    const avgRenderTime = this.average(renderTimes)
    const avgMemoryUsage = this.average(memoryUsages)
    const avgFPS = this.average(fpsList)

    // 计算性能等级
    let performanceLevel: 'low' | 'medium' | 'high' = 'medium'
    if (avgRenderTime < 200 && avgFPS > 50 && avgMemoryUsage < 20 * 1024 * 1024) {
      performanceLevel = 'high'
    } else if (avgRenderTime > 1000 || avgFPS < 20 || avgMemoryUsage > 80 * 1024 * 1024) {
      performanceLevel = 'low'
    }

    // 计算稳定性评分
    const renderTimeStdDev = this.standardDeviation(renderTimes)
    const stabilityScore = Math.max(0, 100 - (renderTimeStdDev / avgRenderTime) * 100)

    return {
      avgRenderTime: Math.round(avgRenderTime),
      minRenderTime: Math.min(...renderTimes),
      maxRenderTime: Math.max(...renderTimes),
      renderTimeStdDev: Math.round(renderTimeStdDev),
      avgMemoryUsage: Math.round(avgMemoryUsage),
      maxMemoryUsage: Math.max(...memoryUsages),
      avgFPS: Math.round(avgFPS),
      successRate: Math.round((successfulResults.length / results.length) * 100),
      performanceLevel,
      stabilityScore: Math.round(stabilityScore)
    }
  }

  /**
   * 生成优化建议
   */
  private generateOptimizations(
    renderTime: number,
    memoryUsage: number,
    fps: number,
    wordCount: number
  ): string[] {
    const optimizations: string[] = []

    if (renderTime > 1000) {
      optimizations.push('渲染时间过长，建议启用分批渲染或减少词语数量')
    }

    if (memoryUsage > 50 * 1024 * 1024) {
      optimizations.push('内存使用过高，建议启用内存优化或清理缓存')
    }

    if (fps < 30) {
      optimizations.push('FPS过低，建议调整渲染配置或降低复杂度')
    }

    if (wordCount > 500) {
      optimizations.push('词语数量较多，建议启用虚拟化渲染')
    }

    if (renderTime > 500 && wordCount > 200) {
      optimizations.push('大数据量渲染较慢，建议启用性能模式')
    }

    return optimizations
  }

  /**
   * 获取设备信息
   */
  private async getDeviceInfo(): Promise<DeviceInfo> {
    const memory = getMemoryInfo()
    const performanceLevel = detectDevicePerformance()

    return {
      userAgent: navigator.userAgent,
      screenSize: {
        width: window.screen.width,
        height: window.screen.height
      },
      devicePixelRatio: window.devicePixelRatio || 1,
      hardwareConcurrency: navigator.hardwareConcurrency || 2,
      memory: {
        total: memory.total,
        used: memory.used,
        available: memory.available
      },
      performanceLevel,
      isMobile: /Mobile|Android|iPhone|iPad/i.test(navigator.userAgent)
    }
  }

  /**
   * 获取标准基准测试配置
   */
  private getStandardBenchmarkConfigs(): BenchmarkConfig[] {
    return [
      {
        name: '小数据量测试',
        wordCount: 50,
        containerSize: { width: 600, height: 400 },
        wordCloudConfig: { gridSize: 8, rotationSteps: 2 },
        performanceConfig: { batchSize: 50, renderDelay: 10 },
        iterations: 5
      },
      {
        name: '中等数据量测试',
        wordCount: 150,
        containerSize: { width: 800, height: 600 },
        wordCloudConfig: { gridSize: 8, rotationSteps: 3 },
        performanceConfig: { batchSize: 50, renderDelay: 10 },
        iterations: 5
      },
      {
        name: '大数据量测试',
        wordCount: 300,
        containerSize: { width: 1000, height: 700 },
        wordCloudConfig: { gridSize: 6, rotationSteps: 4 },
        performanceConfig: { batchSize: 100, renderDelay: 5 },
        iterations: 3
      },
      {
        name: '移动设备测试',
        wordCount: 80,
        containerSize: { width: 375, height: 667 },
        wordCloudConfig: { gridSize: 12, rotationSteps: 2, shrinkToFit: true },
        performanceConfig: { batchSize: 30, renderDelay: 15 },
        iterations: 5
      },
      {
        name: '高性能测试',
        wordCount: 500,
        containerSize: { width: 1200, height: 800 },
        wordCloudConfig: { gridSize: 4, rotationSteps: 6 },
        performanceConfig: { batchSize: 150, renderDelay: 1 },
        iterations: 3
      }
    ]
  }

  /**
   * 清理和等待
   */
  private async cleanupAndWait(): Promise<void> {
    // 强制垃圾回收
    if (window.gc) {
      window.gc()
    }

    // 等待系统稳定
    await new Promise(resolve => setTimeout(resolve, 500))
  }

  /**
   * 计算平均值
   */
  private average(numbers: number[]): number {
    return numbers.length > 0 ? numbers.reduce((a, b) => a + b, 0) / numbers.length : 0
  }

  /**
   * 计算标准差
   */
  private standardDeviation(numbers: number[]): number {
    const avg = this.average(numbers)
    const squareDiffs = numbers.map(num => Math.pow(num - avg, 2))
    const avgSquareDiff = this.average(squareDiffs)
    return Math.sqrt(avgSquareDiff)
  }

  /**
   * 生成基准报告
   */
  generateReport(): string {
    if (this.results.length === 0) {
      return '暂无基准测试结果'
    }

    const report = []
    report.push('# 词云性能基准测试报告')
    report.push('')
    report.push(`测试时间: ${new Date().toLocaleString()}`)
    report.push(`测试设备: ${this.results[0].deviceInfo.performanceLevel} 性能设备`)
    report.push('')

    this.results.forEach((result, index) => {
      report.push(`## ${index + 1}. ${result.config.name}`)
      report.push('')
      report.push(`- 词语数量: ${result.config.wordCount}`)
      report.push(`- 容器尺寸: ${result.config.containerSize.width}x${result.config.containerSize.height}`)
      report.push(`- 迭代次数: ${result.config.iterations}`)
      report.push('')
      report.push('### 性能指标')
      report.push(`- 平均渲染时间: ${result.statistics.avgRenderTime}ms`)
      report.push(`- 平均FPS: ${result.statistics.avgFPS}`)
      report.push(`- 平均内存使用: ${Math.round(result.statistics.avgMemoryUsage / 1024 / 1024)}MB`)
      report.push(`- 成功率: ${result.statistics.successRate}%`)
      report.push(`- 性能等级: ${result.statistics.performanceLevel}`)
      report.push(`- 稳定性评分: ${result.statistics.stabilityScore}`)
      report.push('')

      // 添加优化建议
      const allOptimizations = result.results
        .flatMap(r => r.optimizations || [])
        .filter((opt, idx, arr) => arr.indexOf(opt) === idx)

      if (allOptimizations.length > 0) {
        report.push('### 优化建议')
        allOptimizations.forEach(opt => {
          report.push(`- ${opt}`)
        })
        report.push('')
      }
    })

    return report.join('\n')
  }

  /**
   * 获取测试结果
   */
  getResults(): BenchmarkResult[] {
    return [...this.results]
  }

  /**
   * 清除结果
   */
  clearResults(): void {
    this.results = []
  }
}

// ============= 便利函数 =============

/**
 * 运行快速性能测试
 */
export async function runQuickPerformanceTest(wordCount: number = 100): Promise<TestResult> {
  const benchmark = new WordCloudBenchmark()

  const config: BenchmarkConfig = {
    name: '快速测试',
    wordCount,
    containerSize: { width: 800, height: 600 },
    wordCloudConfig: {},
    performanceConfig: {},
    iterations: 1
  }

  const result = await benchmark.runCustomBenchmark(config)
  return result.results[0]
}

/**
 * 运行完整基准测试
 */
export async function runFullBenchmark(): Promise<BenchmarkResult[]> {
  const benchmark = new WordCloudBenchmark()
  return await benchmark.runStandardBenchmark()
}

/**
 * 生成性能报告
 */
export async function generatePerformanceReport(): Promise<string> {
  const benchmark = new WordCloudBenchmark()
  await benchmark.runStandardBenchmark()
  return benchmark.generateReport()
}

// WordCloudBenchmark已在上面导出