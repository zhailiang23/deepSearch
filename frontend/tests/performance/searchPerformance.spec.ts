import { test, expect } from '@playwright/test'
import { PageFactory } from '../e2e/helpers/page-objects'
import { TestUtilsFactory } from '../e2e/helpers/test-utils'

/**
 * 搜索性能测试
 *
 * 覆盖场景:
 * - 搜索响应时间性能测试
 * - 日志记录对搜索性能的影响测试
 * - 点击追踪性能开销测试
 * - 大量并发用户性能测试
 * - 内存使用和泄漏检测
 * - 长时间运行稳定性测试
 * - 网络条件对性能的影响
 * - 前端渲染性能测试
 * - 缓存机制性能验证
 */

test.describe('搜索性能测试', () => {
  let pageFactory: PageFactory
  let testUtils: TestUtilsFactory

  test.beforeEach(async ({ page, browser }) => {
    pageFactory = new PageFactory(page)
    testUtils = new TestUtilsFactory(page, browser)

    // 设置性能测试环境
    await page.addInitScript(() => {
      // 启用性能监控
      if (!window.performance.mark) {
        window.performance.mark = (() => ({} as PerformanceMark))
      }
      if (!window.performance.measure) {
        window.performance.measure = (() => ({} as PerformanceMeasure))
      }

      // 设置测试配置
      localStorage.setItem('performance-test-mode', 'true')
      localStorage.setItem('click-tracking-enabled', 'true')
      localStorage.setItem('search-logging-enabled', 'true')
    })

    await setupPerformanceAPIs(page)
  })

  test.afterEach(async ({ page }) => {
    // 收集性能指标
    const performanceMetrics = await page.evaluate(() => {
      return {
        memory: (performance as any).memory ? {
          usedJSHeapSize: (performance as any).memory.usedJSHeapSize,
          totalJSHeapSize: (performance as any).memory.totalJSHeapSize,
          jsHeapSizeLimit: (performance as any).memory.jsHeapSizeLimit
        } : null,
        timing: performance.timing ? {
          loadEventEnd: performance.timing.loadEventEnd,
          loadEventStart: performance.timing.loadEventStart,
          domComplete: performance.timing.domComplete,
          domContentLoadedEventEnd: performance.timing.domContentLoadedEventEnd
        } : null
      }
    })

    console.log('性能指标:', performanceMetrics)
    await testUtils.getTestDataManager().clearLocalStorage()
  })

  test('搜索响应时间应该在可接受范围内', async ({ page }) => {
    await page.goto('/search')

    // 预热请求
    await page.fill('[data-testid="search-input"]', '预热查询')
    await page.click('[data-testid="search-button"]')
    await page.waitForSelector('[data-testid="search-results"]')

    // 性能测试循环
    const searchTimes: number[] = []
    const testQueries = [
      'Vue.js 组件开发',
      'TypeScript 最佳实践',
      'React Hooks 使用',
      'Node.js 后端开发',
      'JavaScript 性能优化'
    ]

    for (const query of testQueries) {
      // 清除搜索框
      await page.fill('[data-testid="search-input"]', '')

      // 开始计时
      const startTime = Date.now()

      // 执行搜索
      await page.fill('[data-testid="search-input"]', query)
      await page.click('[data-testid="search-button"]')

      // 等待结果显示
      await page.waitForSelector('[data-testid="search-results"]')
      await page.waitForSelector('[data-testid="result-item"]')

      // 结束计时
      const endTime = Date.now()
      const searchTime = endTime - startTime

      searchTimes.push(searchTime)

      // 等待一小段时间避免请求过于频繁
      await page.waitForTimeout(100)
    }

    // 计算性能指标
    const avgSearchTime = searchTimes.reduce((a, b) => a + b, 0) / searchTimes.length
    const maxSearchTime = Math.max(...searchTimes)
    const minSearchTime = Math.min(...searchTimes)

    console.log(`搜索性能指标:
      平均响应时间: ${avgSearchTime}ms
      最大响应时间: ${maxSearchTime}ms
      最小响应时间: ${minSearchTime}ms
      所有响应时间: ${searchTimes.join(', ')}ms
    `)

    // 性能断言
    expect(avgSearchTime).toBeLessThan(2000) // 平均响应时间小于2秒
    expect(maxSearchTime).toBeLessThan(5000) // 最大响应时间小于5秒
    expect(searchTimes.every(time => time > 0)).toBe(true) // 所有响应时间都大于0
  })

  test('日志记录对搜索性能的影响应该小于5%', async ({ page }) => {
    const testQuery = '性能影响测试查询'
    const testRounds = 10

    // 第一阶段：禁用日志记录测试基准性能
    await page.addInitScript(() => {
      localStorage.setItem('search-logging-enabled', 'false')
      localStorage.setItem('click-tracking-enabled', 'false')
    })

    await page.goto('/search')

    const baselineTimes: number[] = []

    for (let i = 0; i < testRounds; i++) {
      await page.fill('[data-testid="search-input"]', '')

      const startTime = performance.now()

      await page.fill('[data-testid="search-input"]', `${testQuery} ${i}`)
      await page.click('[data-testid="search-button"]')
      await page.waitForSelector('[data-testid="search-results"]')

      const endTime = performance.now()
      baselineTimes.push(endTime - startTime)

      await page.waitForTimeout(100)
    }

    const baselineAvg = baselineTimes.reduce((a, b) => a + b, 0) / baselineTimes.length

    // 第二阶段：启用日志记录测试性能影响
    await page.addInitScript(() => {
      localStorage.setItem('search-logging-enabled', 'true')
      localStorage.setItem('click-tracking-enabled', 'true')
    })

    await page.reload()
    await page.waitForSelector('[data-testid="search-input"]')

    const withLoggingTimes: number[] = []

    for (let i = 0; i < testRounds; i++) {
      await page.fill('[data-testid="search-input"]', '')

      const startTime = performance.now()

      await page.fill('[data-testid="search-input"]', `${testQuery} ${i}`)
      await page.click('[data-testid="search-button"]')
      await page.waitForSelector('[data-testid="search-results"]')

      const endTime = performance.now()
      withLoggingTimes.push(endTime - startTime)

      await page.waitForTimeout(100)
    }

    const withLoggingAvg = withLoggingTimes.reduce((a, b) => a + b, 0) / withLoggingTimes.length

    // 计算性能影响
    const performanceImpact = ((withLoggingAvg - baselineAvg) / baselineAvg) * 100

    console.log(`日志记录性能影响分析:
      基准平均时间: ${baselineAvg.toFixed(2)}ms
      启用日志平均时间: ${withLoggingAvg.toFixed(2)}ms
      性能影响: ${performanceImpact.toFixed(2)}%
    `)

    // 验证性能影响小于5%
    expect(performanceImpact).toBeLessThan(5)
  })

  test('点击追踪的性能开销应该可以忽略', async ({ page }) => {
    await page.goto('/search')

    // 执行搜索
    await page.fill('[data-testid="search-input"]', '点击性能测试')
    await page.click('[data-testid="search-button"]')
    await page.waitForSelector('[data-testid="search-results"]')

    const results = page.locator('[data-testid="result-item"]')
    await expect(results).toHaveCount({ min: 3 })

    // 测试点击性能
    const clickTimes: number[] = []
    const clickCount = 20

    for (let i = 0; i < clickCount; i++) {
      const resultIndex = i % 3 // 循环点击前3个结果

      const startTime = performance.now()

      await results.nth(resultIndex).click()

      // 等待点击处理完成
      await page.waitForTimeout(50)

      const endTime = performance.now()
      clickTimes.push(endTime - startTime)

      await page.waitForTimeout(100)
    }

    const avgClickTime = clickTimes.reduce((a, b) => a + b, 0) / clickTimes.length
    const maxClickTime = Math.max(...clickTimes)

    console.log(`点击追踪性能指标:
      平均点击处理时间: ${avgClickTime.toFixed(2)}ms
      最大点击处理时间: ${maxClickTime.toFixed(2)}ms
    `)

    // 验证点击处理时间合理
    expect(avgClickTime).toBeLessThan(200) // 平均点击处理时间小于200ms
    expect(maxClickTime).toBeLessThan(500) // 最大点击处理时间小于500ms
  })

  test('并发用户搜索性能应该保持稳定', async ({ page, browser }) => {
    const concurrentUsers = 5
    const searchesPerUser = 3

    // 创建多个用户会话
    const userPages = []
    for (let i = 0; i < concurrentUsers; i++) {
      const context = await browser.newContext()
      const userPage = await context.newPage()
      await setupPerformanceAPIs(userPage)
      userPages.push({ page: userPage, context })
    }

    // 并发搜索测试
    const startTime = Date.now()

    const searchPromises = userPages.map(async ({ page: userPage }, userIndex) => {
      const userTimes: number[] = []

      for (let searchIndex = 0; searchIndex < searchesPerUser; searchIndex++) {
        const searchStart = performance.now()

        await userPage.goto('/search')
        await userPage.fill('[data-testid="search-input"]', `并发测试查询 ${userIndex}-${searchIndex}`)
        await userPage.click('[data-testid="search-button"]')
        await userPage.waitForSelector('[data-testid="search-results"]')

        const searchEnd = performance.now()
        userTimes.push(searchEnd - searchStart)

        await userPage.waitForTimeout(100)
      }

      return { userIndex, times: userTimes }
    })

    const results = await Promise.all(searchPromises)
    const endTime = Date.now()

    // 分析并发性能
    const allTimes = results.flatMap(r => r.times)
    const avgTime = allTimes.reduce((a, b) => a + b, 0) / allTimes.length
    const totalTime = endTime - startTime

    console.log(`并发性能测试结果:
      并发用户数: ${concurrentUsers}
      每用户搜索次数: ${searchesPerUser}
      总搜索次数: ${concurrentUsers * searchesPerUser}
      平均响应时间: ${avgTime.toFixed(2)}ms
      总测试时间: ${totalTime}ms
    `)

    // 性能断言
    expect(avgTime).toBeLessThan(3000) // 并发情况下平均响应时间仍小于3秒
    expect(totalTime).toBeLessThan(30000) // 总测试时间小于30秒

    // 清理用户会话
    for (const { context } of userPages) {
      await context.close()
    }
  })

  test('内存使用应该保持稳定不出现泄漏', async ({ page }) => {
    await page.goto('/search')

    // 获取初始内存使用
    const initialMemory = await page.evaluate(() => {
      return (performance as any).memory ? {
        used: (performance as any).memory.usedJSHeapSize,
        total: (performance as any).memory.totalJSHeapSize
      } : null
    })

    if (!initialMemory) {
      console.log('浏览器不支持内存监控，跳过内存测试')
      return
    }

    // 执行大量搜索和点击操作
    const testRounds = 20

    for (let i = 0; i < testRounds; i++) {
      // 搜索操作
      await page.fill('[data-testid="search-input"]', `内存测试查询 ${i}`)
      await page.click('[data-testid="search-button"]')
      await page.waitForSelector('[data-testid="search-results"]')

      // 点击操作
      const results = page.locator('[data-testid="result-item"]')
      const resultCount = await results.count()

      for (let j = 0; j < Math.min(resultCount, 3); j++) {
        await results.nth(j).click()
        await page.waitForTimeout(50)
      }

      // 每5轮检查一次内存
      if (i % 5 === 4) {
        // 强制垃圾回收（如果支持）
        await page.evaluate(() => {
          if ((window as any).gc) {
            (window as any).gc()
          }
        })

        const currentMemory = await page.evaluate(() => {
          return (performance as any).memory ? {
            used: (performance as any).memory.usedJSHeapSize,
            total: (performance as any).memory.totalJSHeapSize
          } : null
        })

        if (currentMemory) {
          const memoryIncrease = currentMemory.used - initialMemory.used
          const memoryIncreasePercent = (memoryIncrease / initialMemory.used) * 100

          console.log(`第${i + 1}轮内存使用:
            初始内存: ${(initialMemory.used / 1024 / 1024).toFixed(2)}MB
            当前内存: ${(currentMemory.used / 1024 / 1024).toFixed(2)}MB
            内存增长: ${(memoryIncrease / 1024 / 1024).toFixed(2)}MB (${memoryIncreasePercent.toFixed(2)}%)
          `)

          // 检查内存泄漏
          expect(memoryIncreasePercent).toBeLessThan(50) // 内存增长不超过50%
        }
      }

      await page.waitForTimeout(100)
    }

    // 最终内存检查
    const finalMemory = await page.evaluate(() => {
      return (performance as any).memory ? {
        used: (performance as any).memory.usedJSHeapSize,
        total: (performance as any).memory.totalJSHeapSize
      } : null
    })

    if (finalMemory) {
      const totalMemoryIncrease = finalMemory.used - initialMemory.used
      const totalMemoryIncreasePercent = (totalMemoryIncrease / initialMemory.used) * 100

      console.log(`最终内存分析:
        初始内存: ${(initialMemory.used / 1024 / 1024).toFixed(2)}MB
        最终内存: ${(finalMemory.used / 1024 / 1024).toFixed(2)}MB
        总内存增长: ${(totalMemoryIncrease / 1024 / 1024).toFixed(2)}MB (${totalMemoryIncreasePercent.toFixed(2)}%)
      `)

      // 验证没有严重的内存泄漏
      expect(totalMemoryIncreasePercent).toBeLessThan(100) // 总内存增长不超过100%
    }
  })

  test('长时间运行稳定性测试', async ({ page }) => {
    await page.goto('/search')

    const testDuration = 60000 // 1分钟测试
    const startTime = Date.now()
    let operationCount = 0
    let errorCount = 0

    const performanceHistory: Array<{
      timestamp: number
      responseTime: number
      memoryUsed?: number
    }> = []

    // 持续执行操作直到测试时间结束
    while (Date.now() - startTime < testDuration) {
      try {
        const operationStart = performance.now()

        // 搜索操作
        await page.fill('[data-testid="search-input"]', `稳定性测试 ${operationCount}`)
        await page.click('[data-testid="search-button"]')
        await page.waitForSelector('[data-testid="search-results"]', { timeout: 5000 })

        // 点击操作
        const results = page.locator('[data-testid="result-item"]')
        if (await results.count() > 0) {
          await results.first().click()
        }

        const operationEnd = performance.now()
        const responseTime = operationEnd - operationStart

        // 记录性能数据
        const memoryUsed = await page.evaluate(() => {
          return (performance as any).memory ? (performance as any).memory.usedJSHeapSize : undefined
        })

        performanceHistory.push({
          timestamp: Date.now(),
          responseTime,
          memoryUsed
        })

        operationCount++

        // 短暂休息
        await page.waitForTimeout(200)

      } catch (error) {
        errorCount++
        console.warn(`操作 ${operationCount} 失败:`, error)

        // 如果错误率过高，中断测试
        if (errorCount / (operationCount + errorCount) > 0.1) {
          throw new Error(`错误率过高: ${errorCount}/${operationCount + errorCount}`)
        }
      }
    }

    const actualDuration = Date.now() - startTime

    // 分析稳定性结果
    const avgResponseTime = performanceHistory.reduce((sum, record) => sum + record.responseTime, 0) / performanceHistory.length
    const maxResponseTime = Math.max(...performanceHistory.map(r => r.responseTime))
    const errorRate = errorCount / (operationCount + errorCount)

    console.log(`长时间运行稳定性测试结果:
      测试时长: ${actualDuration}ms
      总操作次数: ${operationCount}
      错误次数: ${errorCount}
      错误率: ${(errorRate * 100).toFixed(2)}%
      平均响应时间: ${avgResponseTime.toFixed(2)}ms
      最大响应时间: ${maxResponseTime.toFixed(2)}ms
    `)

    // 稳定性断言
    expect(errorRate).toBeLessThan(0.05) // 错误率小于5%
    expect(avgResponseTime).toBeLessThan(3000) // 平均响应时间稳定
    expect(operationCount).toBeGreaterThan(50) // 至少完成50次操作
  })

  test('网络延迟对性能的影响测试', async ({ page }) => {
    await page.goto('/search')

    const testQuery = '网络性能测试'

    // 简化的延迟测试，通过API模拟不同延迟
    await page.route('**/api/search-data**', route => {
      // 模拟网络延迟
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              total: 10,
              results: [
                {
                  id: 'network-doc1',
                  title: '网络测试文档',
                  url: 'https://example.com/network/doc1',
                  summary: '网络延迟测试文档',
                  score: 0.95
                }
              ],
              searchLogId: Date.now()
            }
          })
        })
      }, 500) // 500ms延迟
    })

    const responseTimes: number[] = []

    // 测试5次搜索
    for (let i = 0; i < 5; i++) {
      const startTime = performance.now()

      await page.fill('[data-testid="search-input"]', `${testQuery} ${i}`)
      await page.click('[data-testid="search-button"]')
      await page.waitForSelector('[data-testid="search-results"]', { timeout: 10000 })

      const endTime = performance.now()
      responseTimes.push(endTime - startTime)

      await page.waitForTimeout(200)
    }

    const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length
    const maxResponseTime = Math.max(...responseTimes)

    console.log(`网络延迟测试结果:
      平均响应时间: ${avgResponseTime.toFixed(2)}ms
      最大响应时间: ${maxResponseTime.toFixed(2)}ms
    `)

    // 验证在延迟网络下仍能正常工作
    expect(avgResponseTime).toBeGreaterThan(400) // 应该包含模拟延迟
    expect(maxResponseTime).toBeLessThan(10000) // 不超过10秒
  })

  test('前端渲染性能测试', async ({ page }) => {
    await page.goto('/search')

    // 测试大量搜索结果的渲染性能
    await setupLargeResultsAPI(page)

    const startTime = performance.now()

    await page.fill('[data-testid="search-input"]', '大量结果渲染测试')
    await page.click('[data-testid="search-button"]')

    // 等待所有结果渲染完成
    await page.waitForSelector('[data-testid="search-results"]')
    await page.waitForFunction(() => {
      const results = document.querySelectorAll('[data-testid="result-item"]')
      return results.length >= 50 // 等待至少50个结果渲染
    }, { timeout: 10000 })

    const renderTime = performance.now() - startTime

    // 测试渲染后的交互性能
    const interactionStart = performance.now()

    // 滚动到底部
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight)
    })

    // 点击最后一个结果
    const lastResult = page.locator('[data-testid="result-item"]').last()
    await lastResult.click()

    const interactionTime = performance.now() - interactionStart

    console.log(`前端渲染性能指标:
      大量结果渲染时间: ${renderTime.toFixed(2)}ms
      渲染后交互响应时间: ${interactionTime.toFixed(2)}ms
    `)

    // 性能断言
    expect(renderTime).toBeLessThan(5000) // 渲染时间小于5秒
    expect(interactionTime).toBeLessThan(1000) // 交互响应时间小于1秒

    // 验证所有结果都正确渲染
    const resultCount = await page.locator('[data-testid="result-item"]').count()
    expect(resultCount).toBeGreaterThanOrEqual(50)
  })

  test('缓存机制性能验证', async ({ page }) => {
    await page.goto('/search')

    const testQuery = '缓存测试查询'

    // 第一次搜索（无缓存）
    const firstSearchStart = performance.now()
    await page.fill('[data-testid="search-input"]', testQuery)
    await page.click('[data-testid="search-button"]')
    await page.waitForSelector('[data-testid="search-results"]')
    const firstSearchTime = performance.now() - firstSearchStart

    // 等待一下确保缓存生效
    await page.waitForTimeout(500)

    // 第二次搜索（有缓存）
    await page.fill('[data-testid="search-input"]', '')
    const secondSearchStart = performance.now()
    await page.fill('[data-testid="search-input"]', testQuery)
    await page.click('[data-testid="search-button"]')
    await page.waitForSelector('[data-testid="search-results"]')
    const secondSearchTime = performance.now() - secondSearchStart

    console.log(`缓存性能对比:
      首次搜索时间: ${firstSearchTime.toFixed(2)}ms
      缓存搜索时间: ${secondSearchTime.toFixed(2)}ms
      性能提升: ${((firstSearchTime - secondSearchTime) / firstSearchTime * 100).toFixed(2)}%
    `)

    // 验证缓存带来的性能提升
    expect(secondSearchTime).toBeLessThanOrEqual(firstSearchTime)

    // 如果有明显的缓存机制，第二次应该明显更快
    if (firstSearchTime > 500) {
      expect(secondSearchTime).toBeLessThan(firstSearchTime * 0.8) // 至少快20%
    }
  })
})

/**
 * 设置性能测试相关的API模拟
 */
async function setupPerformanceAPIs(page: any) {
  // 模拟正常响应时间的搜索API
  await page.route('**/api/search-data**', route => {
    // 模拟真实的网络延迟
    setTimeout(() => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            total: 25,
            results: [
              {
                id: 'perf-doc1',
                title: '性能测试文档1',
                url: 'https://example.com/perf/doc1',
                summary: '这是性能测试的文档内容...',
                score: 0.95
              },
              {
                id: 'perf-doc2',
                title: '性能测试文档2',
                url: 'https://example.com/perf/doc2',
                summary: '另一个性能测试文档...',
                score: 0.87
              },
              {
                id: 'perf-doc3',
                title: '性能测试文档3',
                url: 'https://example.com/perf/doc3',
                summary: '第三个性能测试文档...',
                score: 0.78
              }
            ],
            searchLogId: Date.now()
          }
        })
      })
    }, Math.random() * 200 + 100) // 100-300ms随机延迟
  })

  // 模拟点击追踪API
  await page.route('**/api/search-logs/clicks**', route => {
    setTimeout(() => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            clickId: Date.now(),
            recorded: true
          }
        })
      })
    }, Math.random() * 50 + 25) // 25-75ms随机延迟
  })
}

/**
 * 设置大量结果的API模拟（用于渲染性能测试）
 */
async function setupLargeResultsAPI(page: any) {
  await page.route('**/api/search-data**', route => {
    const results = []
    for (let i = 0; i < 100; i++) {
      results.push({
        id: `large-doc${i}`,
        title: `大量结果测试文档 ${i + 1}`,
        url: `https://example.com/large/doc${i}`,
        summary: `这是第 ${i + 1} 个测试文档，用于验证大量结果的渲染性能。内容包含一些较长的描述文本以模拟真实场景。`,
        score: 0.9 - (i * 0.01)
      })
    }

    setTimeout(() => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            total: 100,
            results: results,
            searchLogId: Date.now()
          }
        })
      })
    }, 300) // 稍长的延迟模拟大量数据处理
  })
}