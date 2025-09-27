import { test, expect } from '@playwright/test'
import { PageFactory } from './helpers/page-objects'
import { TestUtilsFactory } from './helpers/test-utils'

/**
 * 点击行为追踪E2E测试
 *
 * 覆盖场景:
 * - 搜索结果点击追踪
 * - 多次点击同一结果的追踪
 * - 不同类型点击的追踪（鼠标、键盘、不同修饰键）
 * - 离线状态下的点击缓存
 * - 网络恢复后的数据同步
 * - 点击追踪状态管理
 * - 错误处理和重试机制
 * - 追踪数据的完整性验证
 */

test.describe('点击行为追踪', () => {
  let pageFactory: PageFactory
  let testUtils: TestUtilsFactory
  let searchLogId: number

  test.beforeEach(async ({ page, browser }) => {
    pageFactory = new PageFactory(page)
    testUtils = new TestUtilsFactory(page, browser)

    // 设置有效的认证状态
    await testUtils.getTestDataManager().setValidToken('testuser')

    // 设置模拟API响应
    searchLogId = await setupClickTrackingMocks(page)

    // 启用点击追踪
    await page.addInitScript(() => {
      localStorage.setItem('click-tracking-enabled', 'true')
    })
  })

  test.afterEach(async ({ page }) => {
    // 清理追踪状态
    await testUtils.getTestDataManager().clearLocalStorage()
    await testUtils.getTestDataManager().clearSessionStorage()
  })

  test('应该在搜索结果页面正确追踪鼠标点击', async ({ page }) => {
    // 访问搜索页面并执行搜索
    await page.goto('/search')
    await performSearch(page, 'Vue.js 组件开发')

    // 等待搜索结果加载
    await page.waitForSelector('[data-testid="search-results"]')
    const resultItems = page.locator('[data-testid="result-item"]')
    await expect(resultItems).toHaveCount(3)

    // 设置网络请求监听
    const clickTrackingPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks') &&
      request.method() === 'POST'
    )

    // 点击第一个搜索结果
    const firstResult = page.locator('[data-testid="result-item"]').first()
    await expect(firstResult).toBeVisible()

    // 执行点击
    await firstResult.click()

    // 验证点击追踪请求发送
    const clickRequest = await clickTrackingPromise
    const requestBody = JSON.parse(clickRequest.postData() || '{}')

    expect(requestBody).toMatchObject({
      searchLogId: searchLogId,
      documentId: expect.any(String),
      documentTitle: expect.any(String),
      documentUrl: expect.any(String),
      clickPosition: 1,
      clickType: 'normal'
    })

    // 验证点击反馈UI
    await expect(firstResult.locator('.click-feedback')).toBeVisible()
    await expect(firstResult.locator('.click-indicator')).toContainText('1')

    // 验证点击状态
    await expect(firstResult).toHaveClass(/clicked/)
  })

  test('应该正确追踪键盘导航点击', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'TypeScript 最佳实践')

    await page.waitForSelector('[data-testid="search-results"]')

    // 使用Tab键导航到第一个结果
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab') // 跳过搜索框

    // 等待焦点到达第一个结果
    const firstResult = page.locator('[data-testid="result-item"]').first()
    await expect(firstResult).toBeFocused()

    // 设置网络请求监听
    const clickTrackingPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    // 使用Enter键点击
    await page.keyboard.press('Enter')

    // 验证键盘点击追踪
    const clickRequest = await clickTrackingPromise
    const requestBody = JSON.parse(clickRequest.postData() || '{}')

    expect(requestBody).toMatchObject({
      searchLogId: searchLogId,
      clickType: 'keyboard',
      clickPosition: 1
    })
  })

  test('应该正确追踪带修饰键的点击', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'Vue Router 路由配置')

    await page.waitForSelector('[data-testid="search-results"]')

    const firstResult = page.locator('[data-testid="result-item"]').first()

    // 测试Ctrl+点击（新标签页打开）
    const clickTrackingPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    await firstResult.click({ modifiers: ['Control'] })

    const clickRequest = await clickTrackingPromise
    const requestBody = JSON.parse(clickRequest.postData() || '{}')

    expect(requestBody).toMatchObject({
      searchLogId: searchLogId,
      clickType: 'ctrl_click',
      modifierKeys: {
        ctrl: true,
        shift: false,
        alt: false
      }
    })

    // 验证新标签页打开行为（模拟的）
    await expect(page.locator('[data-testid="new-tab-indicator"]')).toBeVisible()
  })

  test('应该支持多次点击同一结果的追踪', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'Pinia 状态管理')

    await page.waitForSelector('[data-testid="search-results"]')

    const firstResult = page.locator('[data-testid="result-item"]').first()
    let clickCount = 0

    // 多次点击同一结果
    for (let i = 0; i < 3; i++) {
      const clickTrackingPromise = page.waitForRequest(request =>
        request.url().includes('/api/search-logs/clicks')
      )

      await firstResult.click()
      clickCount++

      // 验证每次点击都被追踪
      await clickTrackingPromise

      // 验证点击计数更新
      await expect(firstResult.locator('.click-indicator')).toContainText(clickCount.toString())

      // 等待一小段时间避免过快点击
      await page.waitForTimeout(500)
    }

    // 验证最终点击计数
    await expect(firstResult.locator('.click-indicator')).toContainText('3')
  })

  test('应该在不同结果之间正确追踪点击位置', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'Vite 构建工具')

    await page.waitForSelector('[data-testid="search-results"]')

    const results = page.locator('[data-testid="result-item"]')
    await expect(results).toHaveCount(3)

    // 点击第二个结果
    const secondClickPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    await results.nth(1).click()

    const secondClickRequest = await secondClickPromise
    const secondRequestBody = JSON.parse(secondClickRequest.postData() || '{}')

    expect(secondRequestBody.clickPosition).toBe(2)

    // 点击第三个结果
    const thirdClickPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    await results.nth(2).click()

    const thirdClickRequest = await thirdClickPromise
    const thirdRequestBody = JSON.parse(thirdClickRequest.postData() || '{}')

    expect(thirdRequestBody.clickPosition).toBe(3)
  })

  test('应该在网络离线时缓存点击数据', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'Nuxt.js 服务端渲染')

    await page.waitForSelector('[data-testid="search-results"]')

    // 模拟网络离线
    await page.context().setOffline(true)

    const firstResult = page.locator('[data-testid="result-item"]').first()

    // 点击结果（此时网络离线）
    await firstResult.click()

    // 验证离线状态指示
    await expect(page.locator('[data-testid="offline-indicator"]')).toBeVisible()

    // 验证点击仍然有视觉反馈
    await expect(firstResult.locator('.click-indicator')).toContainText('1')

    // 验证离线数据缓存
    const offlineData = await page.evaluate(() => {
      return localStorage.getItem('click-tracking-offline-cache')
    })

    expect(offlineData).toBeTruthy()

    const cachedClicks = JSON.parse(offlineData || '{}')
    expect(Object.keys(cachedClicks)).toHaveLength({ min: 1 })
  })

  test('应该在网络恢复后同步离线数据', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'Tailwind CSS 样式框架')

    await page.waitForSelector('[data-testid="search-results"]')

    // 先模拟离线状态并点击
    await page.context().setOffline(true)

    const results = page.locator('[data-testid="result-item"]')

    // 离线时点击多个结果
    await results.nth(0).click()
    await results.nth(1).click()
    await results.nth(2).click()

    // 验证离线缓存
    const offlineData = await page.evaluate(() => {
      return localStorage.getItem('click-tracking-offline-cache')
    })
    const cachedClicks = JSON.parse(offlineData || '{}')
    expect(Object.keys(cachedClicks)).toHaveLength({ min: 3 })

    // 恢复网络连接
    await page.context().setOffline(false)

    // 设置同步请求监听
    const syncRequestPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks/sync') &&
      request.method() === 'POST'
    )

    // 触发同步（通常在网络恢复时自动进行）
    await page.evaluate(() => {
      window.dispatchEvent(new Event('online'))
    })

    // 验证同步请求
    const syncRequest = await syncRequestPromise
    const syncRequestBody = JSON.parse(syncRequest.postData() || '{}')

    expect(syncRequestBody.clicks).toHaveLength(3)
    expect(syncRequestBody.clicks[0]).toMatchObject({
      searchLogId: searchLogId,
      clickPosition: 1
    })

    // 验证离线状态指示消失
    await expect(page.locator('[data-testid="offline-indicator"]')).not.toBeVisible()

    // 验证缓存被清空
    const clearedCache = await page.evaluate(() => {
      return localStorage.getItem('click-tracking-offline-cache')
    })
    expect(clearedCache).toBeNull()
  })

  test('应该正确处理追踪错误和重试', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'Node.js 后端开发')

    await page.waitForSelector('[data-testid="search-results"]')

    // 模拟API错误响应
    await page.route('**/api/search-logs/clicks', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          message: '服务器内部错误'
        })
      })
    })

    const firstResult = page.locator('[data-testid="result-item"]').first()

    // 点击结果（会触发错误）
    await firstResult.click()

    // 验证错误处理
    await expect(page.locator('[data-testid="tracking-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="tracking-error"]')).toContainText('点击记录失败')

    // 验证重试机制（点击仍然有视觉反馈）
    await expect(firstResult.locator('.click-indicator')).toContainText('1')

    // 恢复正常API响应
    await page.unroute('**/api/search-logs/clicks')
    await setupClickTrackingMocks(page)

    // 再次点击应该成功
    const retryClickPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    await firstResult.click()

    await retryClickPromise

    // 验证错误消息消失
    await expect(page.locator('[data-testid="tracking-error"]')).not.toBeVisible()
  })

  test('应该支持点击追踪的开启和关闭', async ({ page }) => {
    await page.goto('/search')

    // 先关闭点击追踪
    await page.evaluate(() => {
      localStorage.setItem('click-tracking-enabled', 'false')
    })

    await performSearch(page, 'React Hooks 使用指南')
    await page.waitForSelector('[data-testid="search-results"]')

    const firstResult = page.locator('[data-testid="result-item"]').first()

    // 点击结果（追踪已禁用）
    await firstResult.click()

    // 验证没有追踪请求发送
    await page.waitForTimeout(1000)
    const requests = []
    page.on('request', request => {
      if (request.url().includes('/api/search-logs/clicks')) {
        requests.push(request)
      }
    })

    expect(requests).toHaveLength(0)

    // 验证没有追踪UI元素
    await expect(firstResult.locator('.click-indicator')).not.toBeVisible()
    await expect(firstResult.locator('.tracking-indicator')).not.toBeVisible()

    // 启用追踪
    await page.evaluate(() => {
      localStorage.setItem('click-tracking-enabled', 'true')
      window.dispatchEvent(new StorageEvent('storage', {
        key: 'click-tracking-enabled',
        newValue: 'true'
      }))
    })

    // 设置追踪请求监听
    const enabledClickPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    // 再次点击（追踪已启用）
    await firstResult.click()

    // 验证追踪请求发送
    await enabledClickPromise

    // 验证追踪UI显示
    await expect(firstResult.locator('.click-indicator')).toContainText('1')
  })

  test('应该在移动设备上正确追踪触摸点击', async ({ page }) => {
    // 设置移动设备视口
    await page.setViewportSize({ width: 375, height: 667 })

    await page.goto('/search')
    await performSearch(page, 'PWA 渐进式应用开发')

    await page.waitForSelector('[data-testid="search-results"]')

    const firstResult = page.locator('[data-testid="result-item"]').first()

    // 设置触摸点击追踪监听
    const touchClickPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    // 模拟触摸点击
    await firstResult.tap()

    // 验证触摸点击追踪
    const touchClickRequest = await touchClickPromise
    const requestBody = JSON.parse(touchClickRequest.postData() || '{}')

    expect(requestBody).toMatchObject({
      searchLogId: searchLogId,
      clickType: 'touch',
      clickPosition: 1
    })

    // 验证移动端点击反馈
    await expect(firstResult.locator('.click-feedback')).toBeVisible()
  })

  test('应该正确处理复杂用户交互场景', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, '微前端架构设计')

    await page.waitForSelector('[data-testid="search-results"]')

    const results = page.locator('[data-testid="result-item"]')

    // 复杂交互场景：快速连续点击不同结果
    const clickPromises = []

    for (let i = 0; i < 3; i++) {
      const clickPromise = page.waitForRequest(request =>
        request.url().includes('/api/search-logs/clicks')
      )
      clickPromises.push(clickPromise)

      await results.nth(i).click()
      await page.waitForTimeout(200) // 短暂间隔
    }

    // 等待所有点击请求完成
    const clickRequests = await Promise.all(clickPromises)

    // 验证所有点击都被正确追踪
    expect(clickRequests).toHaveLength(3)

    for (let i = 0; i < clickRequests.length; i++) {
      const requestBody = JSON.parse(clickRequests[i].postData() || '{}')
      expect(requestBody.clickPosition).toBe(i + 1)
    }

    // 验证UI状态正确
    for (let i = 0; i < 3; i++) {
      await expect(results.nth(i).locator('.click-indicator')).toContainText('1')
    }
  })

  test('应该正确追踪页面间导航的点击行为', async ({ page }) => {
    await page.goto('/search')
    await performSearch(page, 'GraphQL API 设计')

    await page.waitForSelector('[data-testid="search-results"]')

    const firstResult = page.locator('[data-testid="result-item"]').first()

    // 设置页面导航监听
    const navigationPromise = page.waitForNavigation()

    // 设置点击追踪监听
    const clickTrackingPromise = page.waitForRequest(request =>
      request.url().includes('/api/search-logs/clicks')
    )

    // 点击结果（会导航到新页面）
    await firstResult.click()

    // 验证点击追踪在导航前完成
    await clickTrackingPromise

    // 等待导航完成
    await navigationPromise

    // 验证导航成功
    expect(page.url()).toContain('example.com') // 模拟外部链接
  })
})

/**
 * 执行搜索操作
 */
async function performSearch(page: any, query: string) {
  await page.fill('[data-testid="search-input"]', query)
  await page.click('[data-testid="search-button"]')

  // 等待搜索完成
  await page.waitForSelector('[data-testid="search-results"]')
  await page.waitForTimeout(500) // 等待额外的加载时间
}

/**
 * 设置点击追踪相关的模拟API响应
 */
async function setupClickTrackingMocks(page: any): Promise<number> {
  const searchLogId = 12345

  // 模拟搜索API（创建搜索日志）
  await page.route('**/api/search-data**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          total: 25,
          results: [
            {
              id: 'doc1',
              title: 'Vue.js 组件开发完整指南',
              url: 'https://example.com/vue-components-guide',
              summary: '详细介绍Vue.js组件开发的最佳实践和常见模式...',
              score: 0.95
            },
            {
              id: 'doc2',
              title: 'TypeScript 与 Vue.js 结合使用',
              url: 'https://example.com/vue-typescript',
              summary: 'TypeScript在Vue.js项目中的应用和配置方法...',
              score: 0.89
            },
            {
              id: 'doc3',
              title: '现代前端框架对比分析',
              url: 'https://example.com/frontend-frameworks',
              summary: '对Vue.js、React、Angular等主流框架的详细对比...',
              score: 0.76
            }
          ],
          searchLogId: searchLogId // 返回搜索日志ID供点击追踪使用
        }
      })
    })
  })

  // 模拟点击追踪API
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

  // 模拟离线数据同步API
  await page.route('**/api/search-logs/clicks/sync**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          synced: true,
          processedCount: 3
        }
      })
    })
  })

  return searchLogId
}