#!/usr/bin/env node

/**
 * 性能监控脚本
 * 实时监控系统性能指标和健康状况
 */

import { performance } from 'perf_hooks'
import os from 'os'
import fs from 'fs/promises'
import path from 'path'
import { execSync } from 'child_process'

class PerformanceMonitor {
  constructor(options = {}) {
    this.interval = options.interval || 30000 // 30秒采样间隔
    this.duration = options.duration || 300000 // 5分钟监控时长
    this.outputDir = options.outputDir || './performance-reports'
    this.thresholds = {
      cpuUsage: 80,           // CPU使用率阈值
      memoryUsage: 80,        // 内存使用率阈值
      responseTime: 1000,     // 响应时间阈值
      errorRate: 0.05,        // 错误率阈值
      diskUsage: 85,          // 磁盘使用率阈值
      ...options.thresholds
    }

    this.metrics = []
    this.alerts = []
    this.isMonitoring = false
  }

  /**
   * 开始监控
   */
  async start() {
    console.log('🚀 开始性能监控...')
    console.log(`监控间隔: ${this.interval / 1000}秒`)
    console.log(`监控时长: ${this.duration / 1000}秒`)

    this.isMonitoring = true
    const startTime = Date.now()

    // 确保输出目录存在
    await this.ensureOutputDir()

    // 开始采集数据
    const monitoringLoop = setInterval(async () => {
      if (!this.isMonitoring || (Date.now() - startTime) >= this.duration) {
        clearInterval(monitoringLoop)
        await this.generateReport()
        console.log('✅ 监控完成')
        return
      }

      try {
        await this.collectMetrics()
      } catch (error) {
        console.error('❌ 采集指标时出错:', error.message)
      }
    }, this.interval)

    // 优雅关闭处理
    process.on('SIGINT', async () => {
      console.log('\n🛑 收到中断信号，正在生成报告...')
      this.isMonitoring = false
      clearInterval(monitoringLoop)
      await this.generateReport()
      process.exit(0)
    })
  }

  /**
   * 采集性能指标
   */
  async collectMetrics() {
    const timestamp = Date.now()
    const metrics = {
      timestamp,
      system: await this.getSystemMetrics(),
      application: await this.getApplicationMetrics(),
      network: await this.getNetworkMetrics(),
      database: await this.getDatabaseMetrics()
    }

    this.metrics.push(metrics)
    this.checkThresholds(metrics)

    // 输出实时状态
    this.displayRealTimeStatus(metrics)
  }

  /**
   * 获取系统指标
   */
  async getSystemMetrics() {
    const cpus = os.cpus()
    const totalMem = os.totalmem()
    const freeMem = os.freemem()
    const usedMem = totalMem - freeMem

    // CPU使用率计算
    const cpuUsage = await this.getCPUUsage()

    // 磁盘使用率
    const diskUsage = await this.getDiskUsage()

    // 负载平均值
    const loadAvg = os.loadavg()

    return {
      cpu: {
        usage: cpuUsage,
        cores: cpus.length,
        loadAvg: loadAvg
      },
      memory: {
        total: totalMem,
        used: usedMem,
        free: freeMem,
        usage: (usedMem / totalMem) * 100
      },
      disk: diskUsage,
      uptime: os.uptime()
    }
  }

  /**
   * 获取应用程序指标
   */
  async getApplicationMetrics() {
    try {
      // 检查前端服务
      const frontendHealth = await this.checkServiceHealth('http://localhost:3000')

      // 检查后端服务
      const backendHealth = await this.checkServiceHealth('http://localhost:8080/api/actuator/health')

      // API性能测试
      const apiPerformance = await this.testAPIPerformance()

      return {
        frontend: frontendHealth,
        backend: backendHealth,
        api: apiPerformance
      }
    } catch (error) {
      return {
        error: error.message,
        frontend: { status: 'error' },
        backend: { status: 'error' },
        api: { responseTime: null, success: false }
      }
    }
  }

  /**
   * 获取网络指标
   */
  async getNetworkMetrics() {
    try {
      const networkStats = await this.getNetworkStats()
      return networkStats
    } catch (error) {
      return { error: error.message }
    }
  }

  /**
   * 获取数据库指标
   */
  async getDatabaseMetrics() {
    try {
      // 检查PostgreSQL连接
      const pgHealth = await this.checkServiceHealth('http://localhost:8080/api/actuator/health/db')

      return {
        postgresql: pgHealth
      }
    } catch (error) {
      return { error: error.message }
    }
  }

  /**
   * 获取CPU使用率
   */
  async getCPUUsage() {
    return new Promise((resolve) => {
      const startMeasure = this.cpuAverage()

      setTimeout(() => {
        const endMeasure = this.cpuAverage()
        const idleDifference = endMeasure.idle - startMeasure.idle
        const totalDifference = endMeasure.total - startMeasure.total
        const percentageCPU = 100 - ~~(100 * idleDifference / totalDifference)
        resolve(percentageCPU)
      }, 1000)
    })
  }

  /**
   * CPU平均值计算
   */
  cpuAverage() {
    let totalIdle = 0
    let totalTick = 0
    const cpus = os.cpus()

    for (let i = 0; i < cpus.length; i++) {
      for (const type in cpus[i].times) {
        totalTick += cpus[i].times[type]
      }
      totalIdle += cpus[i].times.idle
    }

    return { idle: totalIdle / cpus.length, total: totalTick / cpus.length }
  }

  /**
   * 获取磁盘使用情况
   */
  async getDiskUsage() {
    try {
      const output = execSync('df -h /', { encoding: 'utf8' })
      const lines = output.split('\n')
      const data = lines[1].split(/\s+/)

      return {
        total: data[1],
        used: data[2],
        available: data[3],
        usage: parseFloat(data[4])
      }
    } catch (error) {
      return { error: error.message }
    }
  }

  /**
   * 检查服务健康状态
   */
  async checkServiceHealth(url) {
    const startTime = performance.now()

    try {
      const response = await fetch(url, {
        method: 'GET',
        timeout: 5000
      })

      const endTime = performance.now()
      const responseTime = endTime - startTime

      return {
        status: response.ok ? 'healthy' : 'unhealthy',
        responseTime: Math.round(responseTime),
        statusCode: response.status
      }
    } catch (error) {
      const endTime = performance.now()
      const responseTime = endTime - startTime

      return {
        status: 'error',
        responseTime: Math.round(responseTime),
        error: error.message
      }
    }
  }

  /**
   * 测试API性能
   */
  async testAPIPerformance() {
    const endpoints = [
      { url: 'http://localhost:8080/api/actuator/health', name: 'health' },
      { url: 'http://localhost:8080/api/auth/login', name: 'login', method: 'POST', body: { username: 'test', password: 'test' } }
    ]

    const results = {}

    for (const endpoint of endpoints) {
      const startTime = performance.now()

      try {
        const options = {
          method: endpoint.method || 'GET',
          timeout: 5000
        }

        if (endpoint.body) {
          options.headers = { 'Content-Type': 'application/json' }
          options.body = JSON.stringify(endpoint.body)
        }

        const response = await fetch(endpoint.url, options)
        const endTime = performance.now()

        results[endpoint.name] = {
          responseTime: Math.round(endTime - startTime),
          status: response.status,
          success: response.ok
        }
      } catch (error) {
        const endTime = performance.now()

        results[endpoint.name] = {
          responseTime: Math.round(endTime - startTime),
          success: false,
          error: error.message
        }
      }
    }

    return results
  }

  /**
   * 获取网络统计
   */
  async getNetworkStats() {
    try {
      // 网络连接数
      const connections = execSync('netstat -an | wc -l', { encoding: 'utf8' }).trim()

      // 监听端口
      const listening = execSync('netstat -tlnp 2>/dev/null | grep LISTEN | wc -l', { encoding: 'utf8' }).trim()

      return {
        totalConnections: parseInt(connections),
        listeningPorts: parseInt(listening)
      }
    } catch (error) {
      return { error: error.message }
    }
  }

  /**
   * 检查阈值并生成告警
   */
  checkThresholds(metrics) {
    const alerts = []

    // CPU使用率检查
    if (metrics.system.cpu.usage > this.thresholds.cpuUsage) {
      alerts.push({
        type: 'warning',
        metric: 'cpu_usage',
        value: metrics.system.cpu.usage,
        threshold: this.thresholds.cpuUsage,
        message: `CPU使用率过高: ${metrics.system.cpu.usage.toFixed(2)}%`
      })
    }

    // 内存使用率检查
    if (metrics.system.memory.usage > this.thresholds.memoryUsage) {
      alerts.push({
        type: 'warning',
        metric: 'memory_usage',
        value: metrics.system.memory.usage,
        threshold: this.thresholds.memoryUsage,
        message: `内存使用率过高: ${metrics.system.memory.usage.toFixed(2)}%`
      })
    }

    // 磁盘使用率检查
    if (metrics.system.disk.usage && metrics.system.disk.usage > this.thresholds.diskUsage) {
      alerts.push({
        type: 'warning',
        metric: 'disk_usage',
        value: metrics.system.disk.usage,
        threshold: this.thresholds.diskUsage,
        message: `磁盘使用率过高: ${metrics.system.disk.usage}%`
      })
    }

    // API响应时间检查
    if (metrics.application.backend && metrics.application.backend.responseTime > this.thresholds.responseTime) {
      alerts.push({
        type: 'warning',
        metric: 'response_time',
        value: metrics.application.backend.responseTime,
        threshold: this.thresholds.responseTime,
        message: `API响应时间过长: ${metrics.application.backend.responseTime}ms`
      })
    }

    // 服务状态检查
    if (metrics.application.frontend && metrics.application.frontend.status !== 'healthy') {
      alerts.push({
        type: 'error',
        metric: 'frontend_health',
        message: '前端服务不健康'
      })
    }

    if (metrics.application.backend && metrics.application.backend.status !== 'healthy') {
      alerts.push({
        type: 'error',
        metric: 'backend_health',
        message: '后端服务不健康'
      })
    }

    // 记录告警
    if (alerts.length > 0) {
      alerts.forEach(alert => {
        alert.timestamp = metrics.timestamp
        this.alerts.push(alert)
        console.log(`⚠️  ${alert.message}`)
      })
    }
  }

  /**
   * 显示实时状态
   */
  displayRealTimeStatus(metrics) {
    const timestamp = new Date(metrics.timestamp).toLocaleTimeString()

    console.log(`\n📊 [${timestamp}] 性能监控状态:`)
    console.log(`   CPU: ${metrics.system.cpu.usage.toFixed(1)}% | 内存: ${metrics.system.memory.usage.toFixed(1)}%`)

    if (metrics.system.disk.usage) {
      console.log(`   磁盘: ${metrics.system.disk.usage}%`)
    }

    if (metrics.application.frontend) {
      console.log(`   前端: ${metrics.application.frontend.status} (${metrics.application.frontend.responseTime}ms)`)
    }

    if (metrics.application.backend) {
      console.log(`   后端: ${metrics.application.backend.status} (${metrics.application.backend.responseTime}ms)`)
    }
  }

  /**
   * 确保输出目录存在
   */
  async ensureOutputDir() {
    try {
      await fs.access(this.outputDir)
    } catch {
      await fs.mkdir(this.outputDir, { recursive: true })
    }
  }

  /**
   * 生成性能报告
   */
  async generateReport() {
    console.log('📄 正在生成性能报告...')

    const reportTimestamp = new Date().toISOString().replace(/[:.]/g, '-')
    const reportPath = path.join(this.outputDir, `performance-report-${reportTimestamp}.json`)
    const summaryPath = path.join(this.outputDir, `performance-summary-${reportTimestamp}.txt`)

    // 生成详细报告
    const report = {
      metadata: {
        startTime: this.metrics[0]?.timestamp,
        endTime: this.metrics[this.metrics.length - 1]?.timestamp,
        duration: this.metrics.length * this.interval,
        sampleCount: this.metrics.length,
        thresholds: this.thresholds
      },
      metrics: this.metrics,
      alerts: this.alerts,
      summary: this.generateSummary()
    }

    await fs.writeFile(reportPath, JSON.stringify(report, null, 2))
    console.log(`📄 详细报告已保存: ${reportPath}`)

    // 生成摘要报告
    const summaryText = this.generateTextSummary()
    await fs.writeFile(summaryPath, summaryText)
    console.log(`📄 摘要报告已保存: ${summaryPath}`)

    // 在控制台显示摘要
    console.log('\n' + summaryText)
  }

  /**
   * 生成统计摘要
   */
  generateSummary() {
    if (this.metrics.length === 0) return {}

    const cpuUsages = this.metrics.map(m => m.system.cpu.usage).filter(v => !isNaN(v))
    const memoryUsages = this.metrics.map(m => m.system.memory.usage).filter(v => !isNaN(v))
    const responseTimes = this.metrics.map(m => m.application.backend?.responseTime).filter(v => v != null)

    return {
      cpu: {
        avg: cpuUsages.reduce((a, b) => a + b, 0) / cpuUsages.length,
        max: Math.max(...cpuUsages),
        min: Math.min(...cpuUsages)
      },
      memory: {
        avg: memoryUsages.reduce((a, b) => a + b, 0) / memoryUsages.length,
        max: Math.max(...memoryUsages),
        min: Math.min(...memoryUsages)
      },
      responseTime: responseTimes.length > 0 ? {
        avg: responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length,
        max: Math.max(...responseTimes),
        min: Math.min(...responseTimes)
      } : null,
      alerts: {
        total: this.alerts.length,
        warnings: this.alerts.filter(a => a.type === 'warning').length,
        errors: this.alerts.filter(a => a.type === 'error').length
      }
    }
  }

  /**
   * 生成文本摘要
   */
  generateTextSummary() {
    const summary = this.generateSummary()
    const lines = []

    lines.push('=== deepSearch 性能监控报告 ===\n')

    if (this.metrics.length > 0) {
      const startTime = new Date(this.metrics[0].timestamp).toLocaleString()
      const endTime = new Date(this.metrics[this.metrics.length - 1].timestamp).toLocaleString()

      lines.push(`监控时间: ${startTime} - ${endTime}`)
      lines.push(`采样次数: ${this.metrics.length}`)
      lines.push('')

      // CPU统计
      lines.push('CPU使用率:')
      lines.push(`  平均: ${summary.cpu.avg.toFixed(2)}%`)
      lines.push(`  最大: ${summary.cpu.max.toFixed(2)}%`)
      lines.push(`  最小: ${summary.cpu.min.toFixed(2)}%`)
      lines.push('')

      // 内存统计
      lines.push('内存使用率:')
      lines.push(`  平均: ${summary.memory.avg.toFixed(2)}%`)
      lines.push(`  最大: ${summary.memory.max.toFixed(2)}%`)
      lines.push(`  最小: ${summary.memory.min.toFixed(2)}%`)
      lines.push('')

      // 响应时间统计
      if (summary.responseTime) {
        lines.push('API响应时间:')
        lines.push(`  平均: ${summary.responseTime.avg.toFixed(2)}ms`)
        lines.push(`  最大: ${summary.responseTime.max}ms`)
        lines.push(`  最小: ${summary.responseTime.min}ms`)
        lines.push('')
      }

      // 告警统计
      lines.push('告警统计:')
      lines.push(`  总计: ${summary.alerts.total}`)
      lines.push(`  警告: ${summary.alerts.warnings}`)
      lines.push(`  错误: ${summary.alerts.errors}`)

      if (this.alerts.length > 0) {
        lines.push('\n最近告警:')
        this.alerts.slice(-5).forEach(alert => {
          const time = new Date(alert.timestamp).toLocaleTimeString()
          lines.push(`  [${time}] ${alert.message}`)
        })
      }
    } else {
      lines.push('没有收集到监控数据')
    }

    lines.push('\n=== 报告结束 ===')

    return lines.join('\n')
  }
}

// 命令行参数解析
function parseArgs() {
  const args = process.argv.slice(2)
  const options = {}

  for (let i = 0; i < args.length; i += 2) {
    const key = args[i].replace(/^--/, '')
    const value = args[i + 1]

    switch (key) {
      case 'interval':
        options.interval = parseInt(value) * 1000
        break
      case 'duration':
        options.duration = parseInt(value) * 1000
        break
      case 'output':
        options.outputDir = value
        break
      default:
        console.log(`未知参数: ${args[i]}`)
    }
  }

  return options
}

// 主函数
async function main() {
  const options = parseArgs()
  const monitor = new PerformanceMonitor(options)

  console.log('🔍 deepSearch 性能监控工具')
  console.log('按 Ctrl+C 停止监控并生成报告\n')

  await monitor.start()
}

// 运行监控
if (import.meta.url === `file://${process.argv[1]}`) {
  main().catch(console.error)
}

export default PerformanceMonitor