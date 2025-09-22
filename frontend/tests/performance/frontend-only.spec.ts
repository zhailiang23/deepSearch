import { test, expect, Page } from '@playwright/test'

/**
 * 前端专用性能测试
 * 在没有后端服务的情况下测试前端性能
 */

test.describe('前端性能测试 (离线模式)', () => {
  test.beforeEach(async ({ page }) => {
    // 模拟API响应
    await mockApiResponses(page)
  })

  test('页面加载性能测试', async ({ page }) => {
    const startTime = Date.now()

    // 导航到首页
    await page.goto('http://localhost:3002')
    await page.waitForLoadState('networkidle')

    const loadTime = Date.now() - startTime

    console.log(`页面加载时间: ${loadTime}ms`)

    // 验证加载时间符合要求
    expect(loadTime).toBeLessThan(2000) // < 2秒

    // 检查页面核心元素
    await expect(page.locator('h1')).toBeVisible()
  })

  test('前端构建性能分析', async ({ page }) => {
    await page.goto('http://localhost:3002')

    // 获取页面性能指标
    const performanceEntries = await page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
      return {
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
        loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
        firstPaint: performance.getEntriesByName('first-paint')[0]?.startTime || 0,
        firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0
      }
    })

    console.log('前端性能指标:')
    console.log(`- DOM内容加载: ${performanceEntries.domContentLoaded.toFixed(2)}ms`)
    console.log(`- 页面加载完成: ${performanceEntries.loadComplete.toFixed(2)}ms`)
    console.log(`- 首次绘制: ${performanceEntries.firstPaint.toFixed(2)}ms`)
    console.log(`- 首次内容绘制: ${performanceEntries.firstContentfulPaint.toFixed(2)}ms`)

    // 验证性能指标
    expect(performanceEntries.domContentLoaded).toBeLessThan(500)
    expect(performanceEntries.firstPaint).toBeLessThan(800)
    expect(performanceEntries.firstContentfulPaint).toBeLessThan(1000)
  })

  test('资源加载性能测试', async ({ page }) => {
    await page.goto('http://localhost:3002')
    await page.waitForLoadState('networkidle')

    // 获取资源加载信息
    const resourceTimings = await page.evaluate(() => {
      const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[]
      const resourceInfo = {
        scripts: [] as number[],
        stylesheets: [] as number[],
        images: [] as number[],
        total: resources.length
      }

      resources.forEach(resource => {
        const duration = resource.responseEnd - resource.requestStart

        if (resource.name.includes('.js')) {
          resourceInfo.scripts.push(duration)
        } else if (resource.name.includes('.css')) {
          resourceInfo.stylesheets.push(duration)
        } else if (resource.name.includes('.png') || resource.name.includes('.jpg') || resource.name.includes('.svg')) {
          resourceInfo.images.push(duration)
        }
      })

      return resourceInfo
    })

    console.log('资源加载性能:')
    console.log(`- 总资源数: ${resourceTimings.total}`)
    console.log(`- JS文件数: ${resourceTimings.scripts.length}`)
    console.log(`- CSS文件数: ${resourceTimings.stylesheets.length}`)
    console.log(`- 图片文件数: ${resourceTimings.images.length}`)

    if (resourceTimings.scripts.length > 0) {
      const avgScriptTime = resourceTimings.scripts.reduce((a, b) => a + b, 0) / resourceTimings.scripts.length
      console.log(`- JS平均加载时间: ${avgScriptTime.toFixed(2)}ms`)
      expect(avgScriptTime).toBeLessThan(500)
    }

    if (resourceTimings.stylesheets.length > 0) {
      const avgStyleTime = resourceTimings.stylesheets.reduce((a, b) => a + b, 0) / resourceTimings.stylesheets.length
      console.log(`- CSS平均加载时间: ${avgStyleTime.toFixed(2)}ms`)
      expect(avgStyleTime).toBeLessThan(300)
    }
  })

  test('内存使用基准测试', async ({ page }) => {
    await page.goto('http://localhost:3002')

    // 获取初始内存
    const initialMemory = await getMemoryUsage(page)
    console.log(`初始内存使用: ${(initialMemory / 1024 / 1024).toFixed(2)}MB`)

    // 进行一些操作
    for (let i = 0; i < 5; i++) {
      await page.reload()
      await page.waitForLoadState('networkidle')
      await page.waitForTimeout(1000)
    }

    // 强制垃圾回收
    await page.evaluate(() => {
      if ((window as any).gc) {
        (window as any).gc()
      }
    })

    const finalMemory = await getMemoryUsage(page)
    const memoryGrowth = finalMemory - initialMemory

    console.log(`最终内存使用: ${(finalMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`内存增长: ${(memoryGrowth / 1024 / 1024).toFixed(2)}MB`)

    // 验证内存增长合理
    expect(memoryGrowth).toBeLessThan(10 * 1024 * 1024) // < 10MB
  })

  test('JavaScript执行性能测试', async ({ page }) => {
    await page.goto('http://localhost:3002')

    // 测试JavaScript执行性能
    const performanceResults = await page.evaluate(() => {
      const results: { [key: string]: number } = {}

      // 测试数组操作性能
      const startArray = performance.now()
      const arr = new Array(10000).fill(0).map((_, i) => i)
      const filteredArr = arr.filter(x => x % 2 === 0)
      results.arrayOps = performance.now() - startArray

      // 测试DOM操作性能
      const startDOM = performance.now()
      const div = document.createElement('div')
      for (let i = 0; i < 100; i++) {
        const span = document.createElement('span')
        span.textContent = `Item ${i}`
        div.appendChild(span)
      }
      document.body.appendChild(div)
      document.body.removeChild(div)
      results.domOps = performance.now() - startDOM

      // 测试JSON操作性能
      const startJSON = performance.now()
      const obj = { items: new Array(1000).fill(0).map((_, i) => ({ id: i, name: `Item ${i}` })) }
      const jsonStr = JSON.stringify(obj)
      const parsed = JSON.parse(jsonStr)
      results.jsonOps = performance.now() - startJSON

      return results
    })

    console.log('JavaScript执行性能:')
    console.log(`- 数组操作: ${performanceResults.arrayOps.toFixed(2)}ms`)
    console.log(`- DOM操作: ${performanceResults.domOps.toFixed(2)}ms`)
    console.log(`- JSON操作: ${performanceResults.jsonOps.toFixed(2)}ms`)

    // 验证执行性能
    expect(performanceResults.arrayOps).toBeLessThan(50)
    expect(performanceResults.domOps).toBeLessThan(100)
    expect(performanceResults.jsonOps).toBeLessThan(20)
  })

  test('响应式性能测试', async ({ page }) => {
    // 测试不同视口大小的性能
    const viewports = [
      { width: 1920, height: 1080, name: '桌面端' },
      { width: 768, height: 1024, name: '平板端' },
      { width: 375, height: 667, name: '移动端' }
    ]

    for (const viewport of viewports) {
      await page.setViewportSize(viewport)

      const startTime = Date.now()
      await page.goto('http://localhost:3002')
      await page.waitForLoadState('networkidle')
      const loadTime = Date.now() - startTime

      console.log(`${viewport.name} (${viewport.width}x${viewport.height}) 加载时间: ${loadTime}ms`)

      // 验证在不同视口下性能依然良好
      expect(loadTime).toBeLessThan(3000)

      // 检查响应式元素
      const isResponsive = await page.evaluate(() => {
        const body = document.body
        return body.offsetWidth <= window.innerWidth
      })

      expect(isResponsive).toBeTruthy()
    }
  })
})

/**
 * 模拟API响应
 */
async function mockApiResponses(page: Page): Promise<void> {
  // 模拟健康检查
  await page.route('**/api/actuator/health', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'UP' })
    })
  })

  // 模拟登录API
  await page.route('**/api/auth/login', route => {
    const delay = Math.random() * 200 + 100 // 100-300ms随机延迟
    setTimeout(() => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          token: 'mock-jwt-token',
          refreshToken: 'mock-refresh-token',
          user: {
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            role: 'USER'
          }
        })
      })
    }, delay)
  })

  // 模拟用户信息API
  await page.route('**/api/auth/profile', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER'
      })
    })
  })

  // 模拟用户列表API
  await page.route('**/api/users*', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: new Array(20).fill(0).map((_, i) => ({
          id: i + 1,
          username: `user${i + 1}`,
          email: `user${i + 1}@example.com`,
          role: 'USER',
          active: true
        })),
        totalElements: 100,
        totalPages: 5,
        number: 0,
        size: 20
      })
    })
  })
}

/**
 * 获取内存使用情况
 */
async function getMemoryUsage(page: Page): Promise<number> {
  return await page.evaluate(() => {
    if ('memory' in performance) {
      return (performance as any).memory.usedJSHeapSize
    }
    return 0
  })
}