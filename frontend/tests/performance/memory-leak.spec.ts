import { test, expect, Page } from '@playwright/test'

/**
 * 内存泄漏检测测试
 * 检测认证系统中可能的内存泄漏问题
 */

interface MemoryMetrics {
  usedJSHeapSize: number
  totalJSHeapSize: number
  jsHeapSizeLimit: number
  timestamp: number
}

interface MemoryLeakTestResult {
  initialMemory: MemoryMetrics
  finalMemory: MemoryMetrics
  peakMemory: MemoryMetrics
  averageGrowthPerCycle: number
  totalGrowth: number
  isLeakDetected: boolean
  recommendations: string[]
}

test.describe('内存泄漏检测测试', () => {
  const MEMORY_GROWTH_THRESHOLD = 5 * 1024 * 1024 // 5MB
  const CYCLES_FOR_LEAK_DETECTION = 20
  const GC_TRIGGER_THRESHOLD = 10 * 1024 * 1024 // 10MB

  test.beforeEach(async ({ page }) => {
    // 启用性能监控
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('认证流程内存泄漏检测', async ({ page }) => {
    const result = await detectMemoryLeak(page, async () => {
      // 执行完整的认证流程
      await performAuthenticationCycle(page)
    }, CYCLES_FOR_LEAK_DETECTION)

    console.log('认证流程内存泄漏检测结果:')
    logMemoryLeakResult(result)

    // 验证没有严重的内存泄漏
    expect(result.isLeakDetected).toBeFalsy()
    expect(result.totalGrowth).toBeLessThan(MEMORY_GROWTH_THRESHOLD)
  })

  test('路由导航内存泄漏检测', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const result = await detectMemoryLeak(page, async () => {
      // 执行路由导航循环
      const routes = ['/dashboard', '/users', '/settings', '/dashboard']
      for (const route of routes) {
        await page.goto(route)
        await page.waitForLoadState('networkidle')
        await page.waitForTimeout(100) // 短暂等待
      }
    }, 15)

    console.log('路由导航内存泄漏检测结果:')
    logMemoryLeakResult(result)

    expect(result.isLeakDetected).toBeFalsy()
    expect(result.totalGrowth).toBeLessThan(MEMORY_GROWTH_THRESHOLD)
  })

  test('Token刷新内存泄漏检测', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const result = await detectMemoryLeak(page, async () => {
      // 触发token刷新
      await page.evaluate(async () => {
        const authStore = (window as any).__AUTH_STORE__
        if (authStore && authStore.refreshToken) {
          await authStore.refreshToken()
        }
      })
      await page.waitForTimeout(100)
    }, 25)

    console.log('Token刷新内存泄漏检测结果:')
    logMemoryLeakResult(result)

    expect(result.isLeakDetected).toBeFalsy()
    expect(result.totalGrowth).toBeLessThan(MEMORY_GROWTH_THRESHOLD)
  })

  test('组件挂载卸载内存泄漏检测', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const result = await detectMemoryLeak(page, async () => {
      // 模拟组件的挂载和卸载
      await page.evaluate(() => {
        // 创建临时DOM元素来模拟组件
        const container = document.createElement('div')
        container.id = 'temp-component'
        document.body.appendChild(container)

        // 模拟Vue组件的创建和销毁
        const component = {
          data: new Array(1000).fill(0).map((_, i) => ({ id: i, data: `item-${i}` })),
          cleanup: () => {
            container.remove()
          }
        }

        // 立即清理
        component.cleanup()
      })
      await page.waitForTimeout(50)
    }, 30)

    console.log('组件挂载卸载内存泄漏检测结果:')
    logMemoryLeakResult(result)

    expect(result.isLeakDetected).toBeFalsy()
  })

  test('事件监听器内存泄漏检测', async ({ page }) => {
    await page.goto('/dashboard')

    const result = await detectMemoryLeak(page, async () => {
      // 添加和移除事件监听器
      await page.evaluate(() => {
        const handlers: (() => void)[] = []

        // 添加多个事件监听器
        for (let i = 0; i < 10; i++) {
          const handler = () => console.log(`Handler ${i}`)
          document.addEventListener('click', handler)
          handlers.push(handler)
        }

        // 立即移除事件监听器
        handlers.forEach(handler => {
          document.removeEventListener('click', handler)
        })
      })
      await page.waitForTimeout(50)
    }, 25)

    console.log('事件监听器内存泄漏检测结果:')
    logMemoryLeakResult(result)

    expect(result.isLeakDetected).toBeFalsy()
  })

  test('Pinia Store内存泄漏检测', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const result = await detectMemoryLeak(page, async () => {
      // 触发store状态变化
      await page.evaluate(() => {
        const authStore = (window as any).__AUTH_STORE__
        if (authStore) {
          // 模拟状态更新
          authStore.user = {
            ...authStore.user,
            lastUpdate: Date.now(),
            tempData: new Array(100).fill(0).map(() => Math.random())
          }

          // 清理临时数据
          delete authStore.user.tempData
        }
      })
      await page.waitForTimeout(50)
    }, 20)

    console.log('Pinia Store内存泄漏检测结果:')
    logMemoryLeakResult(result)

    expect(result.isLeakDetected).toBeFalsy()
  })

  test('长时间运行内存稳定性测试', async ({ page }) => {
    // 登录
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const testDuration = 5 * 60 * 1000 // 5分钟
    const samplingInterval = 30 * 1000 // 每30秒采样一次
    const samples: MemoryMetrics[] = []

    const startTime = Date.now()
    console.log('开始长时间内存稳定性测试 (5分钟)')

    // 定期采样内存使用情况
    const samplingTimer = setInterval(async () => {
      const memory = await getMemoryMetrics(page)
      samples.push(memory)

      console.log(`内存采样 (${samples.length}): ${(memory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)

      // 执行一些操作来模拟用户活动
      await simulateUserActivity(page)

      if (Date.now() - startTime >= testDuration) {
        clearInterval(samplingTimer)
      }
    }, samplingInterval)

    // 等待测试完成
    await new Promise(resolve => {
      const checkCompletion = () => {
        if (Date.now() - startTime >= testDuration) {
          resolve(undefined)
        } else {
          setTimeout(checkCompletion, 1000)
        }
      }
      checkCompletion()
    })

    // 分析内存稳定性
    const initialMemory = samples[0].usedJSHeapSize
    const finalMemory = samples[samples.length - 1].usedJSHeapSize
    const maxMemory = Math.max(...samples.map(s => s.usedJSHeapSize))
    const memoryGrowth = finalMemory - initialMemory

    console.log(`长时间内存稳定性测试结果:`)
    console.log(`- 测试时长: ${testDuration / 1000}秒`)
    console.log(`- 采样次数: ${samples.length}`)
    console.log(`- 初始内存: ${(initialMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 最终内存: ${(finalMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 峰值内存: ${(maxMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 内存增长: ${(memoryGrowth / 1024 / 1024).toFixed(2)}MB`)

    // 验证长时间运行的内存稳定性
    expect(memoryGrowth).toBeLessThan(10 * 1024 * 1024) // 5分钟内存增长 < 10MB
    expect(maxMemory - initialMemory).toBeLessThan(20 * 1024 * 1024) // 峰值增长 < 20MB
  })

  /**
   * 检测内存泄漏
   */
  async function detectMemoryLeak(
    page: Page,
    operation: () => Promise<void>,
    cycles: number
  ): Promise<MemoryLeakTestResult> {
    // 强制垃圾回收
    await forceGarbageCollection(page)
    await page.waitForTimeout(1000)

    const initialMemory = await getMemoryMetrics(page)
    const memorySnapshots: MemoryMetrics[] = [initialMemory]
    let peakMemory = initialMemory

    console.log(`开始内存泄漏检测 (${cycles} 个循环)`)
    console.log(`初始内存: ${(initialMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)

    for (let i = 0; i < cycles; i++) {
      await operation()

      // 每5个循环强制垃圾回收一次
      if (i % 5 === 4) {
        await forceGarbageCollection(page)
        await page.waitForTimeout(500)
      }

      const currentMemory = await getMemoryMetrics(page)
      memorySnapshots.push(currentMemory)

      if (currentMemory.usedJSHeapSize > peakMemory.usedJSHeapSize) {
        peakMemory = currentMemory
      }

      if (i % 5 === 4) {
        console.log(`循环 ${i + 1}: ${(currentMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)
      }
    }

    // 最终垃圾回收
    await forceGarbageCollection(page)
    await page.waitForTimeout(1000)

    const finalMemory = await getMemoryMetrics(page)
    const totalGrowth = finalMemory.usedJSHeapSize - initialMemory.usedJSHeapSize
    const averageGrowthPerCycle = totalGrowth / cycles

    // 分析是否存在内存泄漏
    const isLeakDetected = analyzeMemoryLeak(memorySnapshots)
    const recommendations = generateRecommendations(memorySnapshots, totalGrowth)

    return {
      initialMemory,
      finalMemory,
      peakMemory,
      averageGrowthPerCycle,
      totalGrowth,
      isLeakDetected,
      recommendations
    }
  }

  /**
   * 获取内存指标
   */
  async function getMemoryMetrics(page: Page): Promise<MemoryMetrics> {
    return await page.evaluate(() => {
      const memory = (performance as any).memory
      return {
        usedJSHeapSize: memory ? memory.usedJSHeapSize : 0,
        totalJSHeapSize: memory ? memory.totalJSHeapSize : 0,
        jsHeapSizeLimit: memory ? memory.jsHeapSizeLimit : 0,
        timestamp: Date.now()
      }
    })
  }

  /**
   * 强制垃圾回收
   */
  async function forceGarbageCollection(page: Page): Promise<void> {
    await page.evaluate(() => {
      if ((window as any).gc) {
        (window as any).gc()
      }
    })
  }

  /**
   * 执行认证循环
   */
  async function performAuthenticationCycle(page: Page): Promise<void> {
    // 登录
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    // 访问几个页面
    await page.goto('/users')
    await page.waitForLoadState('networkidle')
    await page.goto('/settings')
    await page.waitForLoadState('networkidle')

    // 登出
    await page.click('[data-testid="user-menu"]')
    await page.click('[data-testid="logout-button"]')
    await page.waitForURL('/login')
  }

  /**
   * 模拟用户活动
   */
  async function simulateUserActivity(page: Page): Promise<void> {
    const activities = [
      () => page.goto('/users'),
      () => page.goto('/settings'),
      () => page.goto('/dashboard'),
      () => page.click('[data-testid="user-menu"]'),
      () => page.evaluate(() => window.scrollTo(0, 100))
    ]

    const randomActivity = activities[Math.floor(Math.random() * activities.length)]
    try {
      await randomActivity()
      await page.waitForTimeout(500)
    } catch (error) {
      // 忽略活动中的错误
    }
  }

  /**
   * 分析内存泄漏
   */
  function analyzeMemoryLeak(snapshots: MemoryMetrics[]): boolean {
    if (snapshots.length < 5) return false

    const initialMemory = snapshots[0].usedJSHeapSize
    const finalMemory = snapshots[snapshots.length - 1].usedJSHeapSize
    const totalGrowth = finalMemory - initialMemory

    // 如果总增长超过阈值，检查是否持续增长
    if (totalGrowth > MEMORY_GROWTH_THRESHOLD) {
      let continuousGrowthCount = 0
      for (let i = 1; i < snapshots.length; i++) {
        if (snapshots[i].usedJSHeapSize > snapshots[i - 1].usedJSHeapSize) {
          continuousGrowthCount++
        }
      }

      // 如果超过70%的时间都在增长，认为可能存在泄漏
      return continuousGrowthCount / (snapshots.length - 1) > 0.7
    }

    return false
  }

  /**
   * 生成建议
   */
  function generateRecommendations(snapshots: MemoryMetrics[], totalGrowth: number): string[] {
    const recommendations: string[] = []

    if (totalGrowth > MEMORY_GROWTH_THRESHOLD) {
      recommendations.push('检测到显著的内存增长，建议检查是否存在未清理的引用')
    }

    const maxMemory = Math.max(...snapshots.map(s => s.usedJSHeapSize))
    const minMemory = Math.min(...snapshots.map(s => s.usedJSHeapSize))

    if (maxMemory - minMemory > 20 * 1024 * 1024) {
      recommendations.push('内存使用波动较大，建议优化内存分配模式')
    }

    if (recommendations.length === 0) {
      recommendations.push('内存使用表现良好，未发现明显问题')
    }

    return recommendations
  }

  /**
   * 记录内存泄漏测试结果
   */
  function logMemoryLeakResult(result: MemoryLeakTestResult): void {
    console.log(`- 初始内存: ${(result.initialMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 最终内存: ${(result.finalMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 峰值内存: ${(result.peakMemory.usedJSHeapSize / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 总增长: ${(result.totalGrowth / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 平均每循环增长: ${(result.averageGrowthPerCycle / 1024).toFixed(2)}KB`)
    console.log(`- 泄漏检测: ${result.isLeakDetected ? '是' : '否'}`)
    console.log(`- 建议: ${result.recommendations.join('; ')}`)
  }
})