import { test, expect, Page } from '@playwright/test'

/**
 * 认证系统负载测试
 * 测试认证系统在高并发下的性能表现
 */

interface PerformanceMetrics {
  loadTime: number
  loginTime: number
  logoutTime: number
  tokenRefreshTime: number
  memoryUsage: number
  cpuUsage: number
}

interface ConcurrentResult {
  success: boolean
  duration: number
  error?: string
}

test.describe('认证系统负载测试', () => {
  const CONCURRENT_USERS = 50
  const TEST_TIMEOUT = 60000 // 60秒
  const PERFORMANCE_THRESHOLDS = {
    loginTime: 500, // 登录时间 < 500ms
    logoutTime: 200, // 登出时间 < 200ms
    tokenRefreshTime: 100, // Token刷新 < 100ms
    memoryIncrease: 50 * 1024 * 1024, // 内存增长 < 50MB
    successRate: 0.95 // 成功率 > 95%
  }

  test.beforeEach(async ({ page }) => {
    // 启动性能监控
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('并发登录负载测试', async ({ browser }) => {
    const results: ConcurrentResult[] = []

    // 创建并发用户
    const promises = Array.from({ length: CONCURRENT_USERS }, async (_, index) => {
      const context = await browser.newContext()
      const page = await context.newPage()

      try {
        const startTime = Date.now()

        // 导航到登录页
        await page.goto('/login')
        await page.waitForLoadState('networkidle')

        // 执行登录
        await page.fill('input[name="username"]', `test_user_${index}`)
        await page.fill('input[name="password"]', 'password123')

        const loginStart = Date.now()
        await page.click('button[type="submit"]')

        // 等待登录完成
        await page.waitForURL('/dashboard', { timeout: 10000 })
        const loginEnd = Date.now()

        const duration = loginEnd - startTime
        const loginTime = loginEnd - loginStart

        console.log(`用户 ${index}: 登录成功, 总时间: ${duration}ms, 登录时间: ${loginTime}ms`)

        results.push({
          success: true,
          duration: loginTime
        })

      } catch (error) {
        console.error(`用户 ${index}: 登录失败 -`, error)
        results.push({
          success: false,
          duration: 0,
          error: error instanceof Error ? error.message : String(error)
        })
      } finally {
        await context.close()
      }
    })

    // 等待所有并发登录完成
    await Promise.all(promises)

    // 分析结果
    const successCount = results.filter(r => r.success).length
    const successRate = successCount / CONCURRENT_USERS
    const avgLoginTime = results
      .filter(r => r.success)
      .reduce((sum, r) => sum + r.duration, 0) / successCount

    console.log(`并发登录测试结果:`)
    console.log(`- 总用户数: ${CONCURRENT_USERS}`)
    console.log(`- 成功登录: ${successCount}`)
    console.log(`- 成功率: ${(successRate * 100).toFixed(2)}%`)
    console.log(`- 平均登录时间: ${avgLoginTime.toFixed(2)}ms`)

    // 验证性能指标
    expect(successRate).toBeGreaterThan(PERFORMANCE_THRESHOLDS.successRate)
    expect(avgLoginTime).toBeLessThan(PERFORMANCE_THRESHOLDS.loginTime)
  })

  test('登录API性能基准测试', async ({ page }) => {
    const iterations = 100
    const results: number[] = []

    for (let i = 0; i < iterations; i++) {
      await page.goto('/login')

      // 监听API请求
      const [response] = await Promise.all([
        page.waitForResponse(response =>
          response.url().includes('/api/auth/login') && response.request().method() === 'POST'
        ),
        page.fill('input[name="username"]', 'testuser'),
        page.fill('input[name="password"]', 'password123'),
        page.click('button[type="submit"]')
      ])

      const timing = response.request().timing()
      const apiTime = timing.responseEnd - timing.requestStart
      results.push(apiTime)

      if (i % 10 === 0) {
        console.log(`完成 ${i + 1}/${iterations} 次API测试`)
      }

      // 清理状态
      await page.evaluate(() => {
        localStorage.clear()
        sessionStorage.clear()
      })
    }

    // 计算统计数据
    const avgTime = results.reduce((sum, time) => sum + time, 0) / results.length
    const sortedResults = results.sort((a, b) => a - b)
    const p95 = sortedResults[Math.floor(results.length * 0.95)]
    const p99 = sortedResults[Math.floor(results.length * 0.99)]

    console.log(`登录API性能统计 (${iterations} 次测试):`)
    console.log(`- 平均响应时间: ${avgTime.toFixed(2)}ms`)
    console.log(`- 95th百分位: ${p95.toFixed(2)}ms`)
    console.log(`- 99th百分位: ${p99.toFixed(2)}ms`)
    console.log(`- 最小值: ${Math.min(...results).toFixed(2)}ms`)
    console.log(`- 最大值: ${Math.max(...results).toFixed(2)}ms`)

    // 验证性能阈值
    expect(p95).toBeLessThan(PERFORMANCE_THRESHOLDS.loginTime)
  })

  test('Token刷新性能测试', async ({ page }) => {
    // 先登录获取token
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    // 测试Token刷新性能
    const refreshTimes: number[] = []
    const iterations = 50

    for (let i = 0; i < iterations; i++) {
      const startTime = Date.now()

      // 触发token刷新
      const [response] = await Promise.all([
        page.waitForResponse(response =>
          response.url().includes('/api/auth/refresh')
        ),
        page.evaluate(() => {
          // 手动触发token刷新
          const authStore = (window as any).__AUTH_STORE__
          if (authStore) {
            return authStore.refreshToken()
          }
        })
      ])

      const endTime = Date.now()
      const refreshTime = endTime - startTime
      refreshTimes.push(refreshTime)

      if (i % 10 === 0) {
        console.log(`完成 ${i + 1}/${iterations} 次Token刷新测试`)
      }

      // 短暂等待避免过于频繁的请求
      await page.waitForTimeout(100)
    }

    const avgRefreshTime = refreshTimes.reduce((sum, time) => sum + time, 0) / refreshTimes.length
    const p95RefreshTime = refreshTimes.sort((a, b) => a - b)[Math.floor(refreshTimes.length * 0.95)]

    console.log(`Token刷新性能统计:`)
    console.log(`- 平均刷新时间: ${avgRefreshTime.toFixed(2)}ms`)
    console.log(`- 95th百分位: ${p95RefreshTime.toFixed(2)}ms`)

    expect(p95RefreshTime).toBeLessThan(PERFORMANCE_THRESHOLDS.tokenRefreshTime)
  })

  test('内存使用监控测试', async ({ page }) => {
    const memorySnapshots: number[] = []

    // 获取初始内存使用情况
    await page.goto('/')
    let initialMemory = await getMemoryUsage(page)
    memorySnapshots.push(initialMemory)

    console.log(`初始内存使用: ${(initialMemory / 1024 / 1024).toFixed(2)}MB`)

    // 执行多次登录/登出操作
    const cycles = 10
    for (let i = 0; i < cycles; i++) {
      // 登录
      await page.goto('/login')
      await page.fill('input[name="username"]', 'testuser')
      await page.fill('input[name="password"]', 'password123')
      await page.click('button[type="submit"]')
      await page.waitForURL('/dashboard')

      // 导航到不同页面
      await page.click('a[href="/users"]')
      await page.waitForLoadState('networkidle')
      await page.click('a[href="/settings"]')
      await page.waitForLoadState('networkidle')

      // 登出
      await page.click('[data-testid="user-menu"]')
      await page.click('[data-testid="logout-button"]')
      await page.waitForURL('/login')

      // 记录内存使用
      const memory = await getMemoryUsage(page)
      memorySnapshots.push(memory)

      console.log(`第 ${i + 1} 次循环后内存使用: ${(memory / 1024 / 1024).toFixed(2)}MB`)
    }

    const finalMemory = memorySnapshots[memorySnapshots.length - 1]
    const memoryIncrease = finalMemory - initialMemory

    console.log(`内存使用分析:`)
    console.log(`- 初始内存: ${(initialMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 最终内存: ${(finalMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`- 内存增长: ${(memoryIncrease / 1024 / 1024).toFixed(2)}MB`)

    // 验证内存增长是否在可接受范围内
    expect(memoryIncrease).toBeLessThan(PERFORMANCE_THRESHOLDS.memoryIncrease)
  })

  test('路由切换性能测试', async ({ page }) => {
    // 先登录
    await page.goto('/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const routes = ['/dashboard', '/users', '/settings']
    const navigationTimes: number[] = []

    for (let i = 0; i < 20; i++) {
      for (const route of routes) {
        const startTime = Date.now()

        await page.goto(route)
        await page.waitForLoadState('networkidle')

        const endTime = Date.now()
        const navigationTime = endTime - startTime
        navigationTimes.push(navigationTime)
      }
    }

    const avgNavigationTime = navigationTimes.reduce((sum, time) => sum + time, 0) / navigationTimes.length
    const p95NavigationTime = navigationTimes.sort((a, b) => a - b)[Math.floor(navigationTimes.length * 0.95)]

    console.log(`路由切换性能统计:`)
    console.log(`- 平均切换时间: ${avgNavigationTime.toFixed(2)}ms`)
    console.log(`- 95th百分位: ${p95NavigationTime.toFixed(2)}ms`)

    expect(p95NavigationTime).toBeLessThan(200) // 路由切换 < 200ms
  })
})

/**
 * 获取页面内存使用情况
 */
async function getMemoryUsage(page: Page): Promise<number> {
  return await page.evaluate(() => {
    if ('memory' in performance) {
      return (performance as any).memory.usedJSHeapSize
    }
    return 0
  })
}

/**
 * 等待网络空闲
 */
async function waitForNetworkIdle(page: Page, timeout = 2000): Promise<void> {
  let pendingRequests = new Set()

  page.on('request', request => {
    pendingRequests.add(request)
  })

  page.on('response', response => {
    pendingRequests.delete(response.request())
  })

  page.on('requestfailed', request => {
    pendingRequests.delete(request)
  })

  return new Promise(resolve => {
    const checkIdle = () => {
      if (pendingRequests.size === 0) {
        resolve()
      } else {
        setTimeout(checkIdle, 100)
      }
    }

    setTimeout(() => {
      resolve() // 超时保护
    }, timeout)

    checkIdle()
  })
}