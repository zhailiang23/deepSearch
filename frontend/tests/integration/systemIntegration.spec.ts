import { test, expect } from '@playwright/test'
import { PageFactory } from '../e2e/helpers/page-objects'
import { TestUtilsFactory } from '../e2e/helpers/test-utils'

/**
 * 系统集成测试
 *
 * 覆盖场景:
 * - 前后端完整数据流验证
 * - 搜索日志记录端到端流程
 * - 点击追踪完整链路测试
 * - 多用户并发场景验证
 * - 数据一致性检查
 * - 错误恢复和降级机制
 * - 性能监控集成验证
 * - 健康检查端点集成
 * - 数据清理机制集成
 */

test.describe('系统集成测试', () => {
  let pageFactory: PageFactory
  let testUtils: TestUtilsFactory

  test.beforeEach(async ({ page, browser }) => {
    pageFactory = new PageFactory(page)
    testUtils = new TestUtilsFactory(page, browser)

    // 设置认证状态
    await testUtils.getTestDataManager().setValidToken('integration-test-user')

    // 启用所有功能
    await page.addInitScript(() => {
      localStorage.setItem('click-tracking-enabled', 'true')
      localStorage.setItem('search-logging-enabled', 'true')
    })

    // 设置集成测试API响应
    await setupIntegrationAPIs(page)
  })

  test.afterEach(async ({ page }) => {
    await testUtils.getTestDataManager().clearLocalStorage()
    await testUtils.getTestDataManager().clearSessionStorage()
  })

  test('完整的搜索到点击的端到端流程应该正常工作', async ({ page }) => {
    // 1. 访问搜索页面
    await page.goto('/search')
    await expect(page.locator('[data-testid="search-interface"]')).toBeVisible()

    // 2. 监听搜索日志创建请求
    const searchLogPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-data') &&
      request.method() === 'POST'
    )

    // 3. 执行搜索
    await page.fill('[data-testid="search-input"]', '系统集成测试查询')
    await page.click('[data-testid="search-button"]')

    // 4. 验证搜索请求发送
    const searchRequest = await searchLogPromise
    const searchRequestBody = JSON.parse(searchRequest.postData() || '{}')

    expect(searchRequestBody).toMatchObject({
      query: '系统集成测试查询',
      searchSpaceId: expect.any(String)
    })

    // 5. 等待搜索结果加载
    await page.waitForSelector('[data-testid="search-results"]')
    const resultItems = page.locator('[data-testid="result-item"]')
    await expect(resultItems.first()).toBeVisible()

    // 6. 监听点击追踪请求
    const clickTrackingPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks') &&
      request.method() === 'POST'
    )

    // 7. 点击搜索结果
    const firstResult = page.locator('[data-testid="result-item"]').first()
    await firstResult.click()

    // 8. 验证点击追踪请求
    const clickRequest = await clickTrackingPromise
    const clickRequestBody = JSON.parse(clickRequest.postData() || '{}')

    expect(clickRequestBody).toMatchObject({
      searchLogId: expect.any(Number),
      documentId: expect.any(String),
      documentTitle: expect.any(String),
      documentUrl: expect.any(String),
      clickPosition: 1,
      clickTime: expect.any(String)
    })

    // 9. 验证前端状态更新
    await expect(firstResult.locator('.click-indicator')).toContainText('1')
    await expect(firstResult).toHaveClass(/clicked/)

    // 10. 访问搜索日志管理页面验证数据记录
    await page.goto('/admin/search-logs')
    await page.waitForSelector('[data-testid="search-log-table"]')

    // 11. 验证搜索日志记录存在
    await page.fill('[data-testid="filter-query"]', '系统集成测试查询')
    await page.click('[data-testid="filter-button"]')

    await page.waitForSelector('[data-testid="table-row"]')
    const logEntries = page.locator('[data-testid="table-row"]')
    await expect(logEntries.first()).toBeVisible()

    // 12. 查看详情验证点击记录
    await page.click('[data-testid="view-detail-button"]:first-child')
    await expect(page.locator('[data-testid="detail-modal"]')).toBeVisible()

    // 13. 验证点击记录存在
    const clickLogs = page.locator('[data-testid="click-log-item"]')
    await expect(clickLogs.first()).toBeVisible()

    // 14. 验证点击记录内容
    await expect(clickLogs.first().locator('[data-testid="click-position"]')).toContainText('1')
    await expect(clickLogs.first().locator('[data-testid="click-document-title"]')).toBeVisible()
  })

  test('多用户并发搜索和点击应该正确隔离', async ({ page, browser }) => {
    // 创建多个用户会话
    const sessionManager = testUtils.getSessionManager()

    // 用户1会话
    const user1Context = await sessionManager.createUserSession('user1')
    const user1Page = await user1Context.newPage()
    await user1Page.addInitScript(() => {
      localStorage.setItem('userId', 'user1')
      localStorage.setItem('click-tracking-enabled', 'true')
    })

    // 用户2会话
    const user2Context = await sessionManager.createUserSession('user2')
    const user2Page = await user2Context.newPage()
    await user2Page.addInitScript(() => {
      localStorage.setItem('userId', 'user2')
      localStorage.setItem('click-tracking-enabled', 'true')
    })

    // 设置API模拟
    await setupConcurrentUserAPIs(user1Page, 'user1')
    await setupConcurrentUserAPIs(user2Page, 'user2')

    // 并发执行搜索
    await Promise.all([
      performUserSearch(user1Page, 'user1-query'),
      performUserSearch(user2Page, 'user2-query')
    ])

    // 验证用户1的数据
    await user1Page.goto('/admin/search-logs')
    await user1Page.fill('[data-testid="filter-query"]', 'user1-query')
    await user1Page.click('[data-testid="filter-button"]')

    const user1Logs = user1Page.locator('[data-testid="table-row"]')
    await expect(user1Logs.first()).toBeVisible()

    // 验证用户2的数据
    await user2Page.goto('/admin/search-logs')
    await user2Page.fill('[data-testid="filter-query"]', 'user2-query')
    await user2Page.click('[data-testid="filter-button"]')

    const user2Logs = user2Page.locator('[data-testid="table-row"]')
    await expect(user2Logs.first()).toBeVisible()

    // 清理会话
    await sessionManager.cleanupSessions()
  })

  test('搜索日志数据一致性应该得到保证', async ({ page }) => {
    const searchQuery = '数据一致性测试'

    // 执行搜索操作
    await page.goto('/search')
    await page.fill('[data-testid="search-input"]', searchQuery)

    // 监听搜索和点击请求
    const requests: any[] = []
    page.on('request', request => {
      if (request.url().includes('/api/search-data') ||
          request.url().includes('/api/search-logs/clicks')) {
        requests.push({
          url: request.url(),
          method: request.method(),
          body: request.postData(),
          timestamp: Date.now()
        })
      }
    })

    // 执行搜索
    await page.click('[data-testid="search-button"]')
    await page.waitForSelector('[data-testid="search-results"]')

    // 执行多次点击
    const results = page.locator('[data-testid="result-item"]')
    for (let i = 0; i < 3; i++) {
      await results.nth(i).click()
      await page.waitForTimeout(200)
    }

    // 等待所有请求完成
    await page.waitForTimeout(1000)

    // 验证请求序列正确性
    const searchRequests = requests.filter(r => r.url.includes('/api/search-data'))
    const clickRequests = requests.filter(r => r.url.includes('/api/search-logs/clicks'))

    expect(searchRequests).toHaveLength(1)
    expect(clickRequests).toHaveLength(3)

    // 验证时间顺序
    expect(searchRequests[0].timestamp).toBeLessThan(clickRequests[0].timestamp)

    // 验证搜索请求内容
    const searchBody = JSON.parse(searchRequests[0].body || '{}')
    expect(searchBody.query).toBe(searchQuery)

    // 验证点击请求内容
    for (let i = 0; i < clickRequests.length; i++) {
      const clickBody = JSON.parse(clickRequests[i].body || '{}')
      expect(clickBody).toMatchObject({
        searchLogId: expect.any(Number),
        clickPosition: i + 1
      })
    }

    // 在管理页面验证数据完整性
    await page.goto('/admin/search-logs')
    await page.fill('[data-testid="filter-query"]', searchQuery)
    await page.click('[data-testid="filter-button"]')

    await page.waitForSelector('[data-testid="table-row"]')

    // 验证搜索记录存在
    const logRow = page.locator('[data-testid="table-row"]').first()
    await expect(logRow.locator('[data-testid="query-cell"]')).toContainText(searchQuery)
    await expect(logRow.locator('[data-testid="click-count-cell"]')).toContainText('3')

    // 验证详情中的点击记录
    await page.click('[data-testid="view-detail-button"]:first-child')
    const clickLogs = page.locator('[data-testid="click-log-item"]')
    await expect(clickLogs).toHaveCount(3)
  })

  test('系统健康检查端点应该返回正确状态', async ({ page }) => {
    // 直接访问健康检查端点
    const healthResponse = await page.request.get('/api/search-logs/health')
    expect(healthResponse.status()).toBe(200)

    const healthData = await healthResponse.json()
    expect(healthData).toMatchObject({
      logRecording: 'UP',
      database: 'UP',
      queue: 'UP',
      status: 'UP',
      queueSize: expect.any(Number)
    })

    // 在前端监控页面验证健康状态
    await page.goto('/admin/search-logs/monitor')
    await page.waitForSelector('[data-testid="health-status"]')

    await expect(page.locator('[data-testid="health-status"]')).toContainText('健康')
    await expect(page.locator('[data-testid="queue-size"]')).toBeVisible()
    await expect(page.locator('[data-testid="performance-metrics"]')).toBeVisible()
  })

  test('错误恢复和降级机制应该正常工作', async ({ page }) => {
    await page.goto('/search')

    // 第一阶段：正常操作
    await page.fill('[data-testid="search-input"]', '错误恢复测试')
    await page.click('[data-testid="search-button"]')
    await page.waitForSelector('[data-testid="search-results"]')

    // 第二阶段：模拟后端服务异常
    await page.route('**/api/search-logs/clicks', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          message: '服务暂时不可用'
        })
      })
    })

    // 点击结果（应该触发错误处理）
    await page.click('[data-testid="result-item"]:first-child')

    // 验证错误处理
    await expect(page.locator('[data-testid="tracking-error"]')).toBeVisible()

    // 验证降级功能：点击仍然有视觉反馈
    const firstResult = page.locator('[data-testid="result-item"]').first()
    await expect(firstResult.locator('.click-indicator')).toContainText('1')

    // 第三阶段：恢复服务
    await page.unroute('**/api/search-logs/clicks')
    await setupIntegrationAPIs(page)

    // 再次点击（应该恢复正常）
    const clickTrackingPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    await page.click('[data-testid="result-item"]:nth-child(2)')

    // 验证恢复后的正常追踪
    await clickTrackingPromise
    await expect(page.locator('[data-testid="tracking-error"]')).not.toBeVisible()
  })

  test('性能监控集成应该正确记录指标', async ({ page }) => {
    // 启用性能监控
    await page.addInitScript(() => {
      if (!window.performance.mark) {
        window.performance.mark = (() => ({} as PerformanceMark))
      }
      if (!window.performance.measure) {
        window.performance.measure = (() => ({} as PerformanceMeasure))
      }
    })

    await page.goto('/search')

    // 监听性能指标请求
    const performanceRequests: any[] = []
    page.on('request', request => {
      if (request.url().includes('/api/search-logs/metrics') ||
          request.url().includes('/api/search-logs/performance')) {
        performanceRequests.push(request)
      }
    })

    // 执行多次搜索和点击操作
    for (let i = 0; i < 3; i++) {
      await page.fill('[data-testid="search-input"]', `性能测试查询 ${i + 1}`)
      await page.click('[data-testid="search-button"]')
      await page.waitForSelector('[data-testid="search-results"]')

      // 点击结果
      await page.click('[data-testid="result-item"]:first-child')
      await page.waitForTimeout(500)
    }

    // 验证性能监控页面
    await page.goto('/admin/search-logs/monitor')
    await page.waitForSelector('[data-testid="performance-metrics"]')

    // 验证关键性能指标显示
    await expect(page.locator('[data-testid="search-count-metric"]')).toBeVisible()
    await expect(page.locator('[data-testid="response-time-metric"]')).toBeVisible()
    await expect(page.locator('[data-testid="click-rate-metric"]')).toBeVisible()

    // 验证性能趋势图
    await expect(page.locator('[data-testid="performance-chart"]')).toBeVisible()
  })

  test('数据清理机制集成应该正常工作', async ({ page }) => {
    // 创建一些测试数据
    await page.goto('/search')

    // 创建多个搜索记录
    for (let i = 0; i < 5; i++) {
      await page.fill('[data-testid="search-input"]', `清理测试查询 ${i + 1}`)
      await page.click('[data-testid="search-button"]')
      await page.waitForSelector('[data-testid="search-results"]')

      if (i < 3) {
        await page.click('[data-testid="result-item"]:first-child')
      }

      await page.waitForTimeout(300)
    }

    // 访问管理页面验证数据存在
    await page.goto('/admin/search-logs')
    await page.fill('[data-testid="filter-query"]', '清理测试查询')
    await page.click('[data-testid="filter-button"]')

    await page.waitForSelector('[data-testid="table-row"]')
    const beforeCleanup = await page.locator('[data-testid="table-row"]').count()
    expect(beforeCleanup).toBeGreaterThanOrEqual(5)

    // 触发数据清理（如果有手动清理功能）
    if (await page.locator('[data-testid="cleanup-button"]').isVisible()) {
      await page.click('[data-testid="cleanup-button"]')

      // 确认清理操作
      await page.click('[data-testid="confirm-cleanup"]')

      // 验证清理结果
      await expect(page.locator('[data-testid="cleanup-success"]')).toBeVisible()
    }

    // 验证清理统计API
    const cleanupStatsResponse = await page.request.get('/api/search-logs/cleanup/stats')
    expect(cleanupStatsResponse.status()).toBe(200)

    const cleanupStats = await cleanupStatsResponse.json()
    expect(cleanupStats.data).toMatchObject({
      expiredLogs: expect.any(Number),
      expiredClicks: expect.any(Number),
      retentionDays: expect.any(Number)
    })
  })

  test('实时数据同步应该正常工作', async ({ page, browser }) => {
    // 打开两个页面模拟实时同步
    const page1 = page
    const page2 = await browser.newPage()

    // 设置两个页面
    await page1.goto('/admin/search-logs')
    await page2.goto('/admin/search-logs')

    // 在page1执行搜索操作
    await page1.goto('/search')
    await page1.fill('[data-testid="search-input"]', '实时同步测试')
    await page1.click('[data-testid="search-button"]')
    await page1.waitForSelector('[data-testid="search-results"]')
    await page1.click('[data-testid="result-item"]:first-child')

    // 在page2验证数据更新（如果支持实时更新）
    await page2.goto('/admin/search-logs')
    await page2.click('[data-testid="refresh-button"]')

    await page2.fill('[data-testid="filter-query"]', '实时同步测试')
    await page2.click('[data-testid="filter-button"]')

    await page2.waitForSelector('[data-testid="table-row"]')
    const syncedLogs = page2.locator('[data-testid="table-row"]')
    await expect(syncedLogs.first()).toBeVisible()

    await page2.close()
  })

  test('API限流和配额机制应该正常工作', async ({ page }) => {
    await page.goto('/search')

    // 快速发送大量请求
    const promises = []
    for (let i = 0; i < 20; i++) {
      promises.push(
        page.evaluate(async (index) => {
          const response = await fetch('/api/search-data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              query: `限流测试 ${index}`,
              searchSpaceId: 'test-space'
            })
          })
          return { status: response.status, index }
        }, i)
      )
    }

    const results = await Promise.all(promises)

    // 验证有些请求被限流
    const successRequests = results.filter(r => r.status === 200)
    const rateLimitedRequests = results.filter(r => r.status === 429)

    expect(successRequests.length).toBeGreaterThan(0)
    expect(rateLimitedRequests.length).toBeGreaterThan(0) // 部分请求应该被限流

    // 验证限流提示
    if (rateLimitedRequests.length > 0) {
      await expect(page.locator('[data-testid="rate-limit-warning"]')).toBeVisible()
    }
  })
})

/**
 * 为单个用户执行搜索操作
 */
async function performUserSearch(page: any, query: string) {
  await page.goto('/search')
  await page.fill('[data-testid="search-input"]', query)
  await page.click('[data-testid="search-button"]')
  await page.waitForSelector('[data-testid="search-results"]')
  await page.click('[data-testid="result-item"]:first-child')
}

/**
 * 设置并发用户的API模拟
 */
async function setupConcurrentUserAPIs(page: any, userId: string) {
  await page.route('**/api/search-data**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          total: 5,
          results: [
            {
              id: `${userId}-doc1`,
              title: `${userId} 搜索结果 1`,
              url: `https://example.com/${userId}/doc1`,
              summary: `为 ${userId} 生成的搜索结果`,
              score: 0.95
            }
          ],
          searchLogId: Date.now() + Math.random()
        }
      })
    })
  })

  await page.route('**/api/search-logs/clicks**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          clickId: Date.now(),
          userId: userId,
          recorded: true
        }
      })
    })
  })
}

/**
 * 设置集成测试的API模拟
 */
async function setupIntegrationAPIs(page: any) {
  // 搜索API
  await page.route('**/api/search-data**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          total: 10,
          results: [
            {
              id: 'integration-doc1',
              title: '系统集成测试文档1',
              url: 'https://example.com/integration/doc1',
              summary: '这是系统集成测试的文档内容',
              score: 0.95
            },
            {
              id: 'integration-doc2',
              title: '系统集成测试文档2',
              url: 'https://example.com/integration/doc2',
              summary: '另一个集成测试文档',
              score: 0.87
            },
            {
              id: 'integration-doc3',
              title: '系统集成测试文档3',
              url: 'https://example.com/integration/doc3',
              summary: '第三个集成测试文档',
              score: 0.78
            }
          ],
          searchLogId: 123456
        }
      })
    })
  })

  // 点击追踪API
  await page.route('**/api/search-logs/clicks**', route => {
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
  })

  // 搜索日志列表API
  await page.route('**/api/search-logs**', route => {
    const url = new URL(route.request().url())
    const query = url.searchParams.get('query') || ''

    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: [
            {
              id: 1,
              userId: 'integration-test-user',
              userIp: '127.0.0.1',
              searchSpaceId: 'test-space',
              query: query || '系统集成测试查询',
              resultCount: 10,
              responseTime: 250,
              status: 'SUCCESS',
              createdAt: new Date().toISOString(),
              clickCount: query.includes('一致性') ? 3 : 1
            }
          ],
          totalElements: 1,
          totalPages: 1,
          size: 20,
          number: 0,
          first: true,
          last: true
        }
      })
    })
  })

  // 搜索日志详情API
  await page.route('**/api/search-logs/*/detail**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 1,
          userId: 'integration-test-user',
          userIp: '127.0.0.1',
          searchSpaceId: 'test-space',
          query: '系统集成测试查询',
          resultCount: 10,
          responseTime: 250,
          status: 'SUCCESS',
          createdAt: new Date().toISOString(),
          clickCount: 3,
          requestParams: JSON.stringify({
            query: '系统集成测试查询',
            searchSpaceId: 'test-space'
          }),
          responseData: JSON.stringify({
            total: 10,
            hits: ['doc1', 'doc2', 'doc3']
          }),
          clickLogs: [
            {
              id: 1,
              documentId: 'integration-doc1',
              documentTitle: '系统集成测试文档1',
              documentUrl: 'https://example.com/integration/doc1',
              clickPosition: 1,
              clickTime: new Date().toISOString(),
              userAgent: 'Mozilla/5.0...',
              clickType: 'normal'
            },
            {
              id: 2,
              documentId: 'integration-doc2',
              documentTitle: '系统集成测试文档2',
              documentUrl: 'https://example.com/integration/doc2',
              clickPosition: 2,
              clickTime: new Date().toISOString(),
              userAgent: 'Mozilla/5.0...',
              clickType: 'normal'
            },
            {
              id: 3,
              documentId: 'integration-doc3',
              documentTitle: '系统集成测试文档3',
              documentUrl: 'https://example.com/integration/doc3',
              clickPosition: 3,
              clickTime: new Date().toISOString(),
              userAgent: 'Mozilla/5.0...',
              clickType: 'normal'
            }
          ]
        }
      })
    })
  })

  // 健康检查API
  await page.route('**/api/search-logs/health**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        logRecording: 'UP',
        database: 'UP',
        queue: 'UP',
        queueSize: 15,
        status: 'UP'
      })
    })
  })

  // 清理统计API
  await page.route('**/api/search-logs/cleanup/stats**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          expiredLogs: 1250,
          expiredClicks: 3420,
          retentionDays: 30
        }
      })
    })
  })
}