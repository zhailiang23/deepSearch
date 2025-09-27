import { test, expect } from '@playwright/test'
import { PageFactory } from './helpers/page-objects'
import { TestUtilsFactory } from './helpers/test-utils'

/**
 * 搜索日志管理页面E2E测试
 *
 * 覆盖场景:
 * - 页面访问和基本元素验证
 * - 筛选功能完整测试
 * - 分页和排序功能
 * - 数据导出功能
 * - 详情查看功能
 * - 统计数据显示
 * - 响应式设计
 * - 错误处理场景
 */

test.describe('搜索日志管理页面', () => {
  let pageFactory: PageFactory
  let testUtils: TestUtilsFactory

  test.beforeEach(async ({ page, browser }) => {
    pageFactory = new PageFactory(page)
    testUtils = new TestUtilsFactory(page, browser)

    // 设置有效的认证状态
    await testUtils.getTestDataManager().setValidToken('admin')

    // 模拟后端API响应
    await setupMockResponses(page)
  })

  test.afterEach(async ({ page }) => {
    // 清理会话状态
    await testUtils.getTestDataManager().clearLocalStorage()
  })

  test('应该成功访问搜索日志管理页面', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 验证页面标题
    await expect(page).toHaveTitle(/搜索日志管理/)

    // 验证页面URL
    await expect(page).toHaveURL('/admin/search-logs')

    // 验证页面主要元素
    await expect(page.locator('h1')).toContainText('搜索日志管理')
    await expect(page.locator('[data-testid="search-log-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="search-log-filter"]')).toBeVisible()

    // 验证统计卡片
    await expect(page.locator('[data-testid="stat-total-searches"]')).toBeVisible()
    await expect(page.locator('[data-testid="stat-success-rate"]')).toBeVisible()
    await expect(page.locator('[data-testid="stat-avg-response-time"]')).toBeVisible()
    await expect(page.locator('[data-testid="stat-total-clicks"]')).toBeVisible()
  })

  test('应该正确显示统计数据', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待统计数据加载
    await page.waitForSelector('[data-testid="stat-total-searches"]')

    // 验证统计数据内容
    const totalSearches = page.locator('[data-testid="stat-total-searches"] .stat-value')
    const successRate = page.locator('[data-testid="stat-success-rate"] .stat-value')
    const avgResponseTime = page.locator('[data-testid="stat-avg-response-time"] .stat-value')
    const totalClicks = page.locator('[data-testid="stat-total-clicks"] .stat-value')

    await expect(totalSearches).toContainText('12,485')
    await expect(successRate).toContainText('87.3%')
    await expect(avgResponseTime).toContainText('342')
    await expect(totalClicks).toContainText('8,721')

    // 验证趋势指示器
    await expect(page.locator('[data-testid="stat-total-searches"] .trend-up')).toBeVisible()
    await expect(page.locator('[data-testid="stat-success-rate"] .trend-up')).toBeVisible()
  })

  test('应该支持统计卡片点击交互', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待页面加载
    await page.waitForSelector('[data-testid="stat-total-searches"]')

    // 点击总搜索次数统计卡片
    await page.click('[data-testid="stat-total-searches"]')

    // 验证筛选条件被重置（应该显示所有搜索）
    await expect(page.locator('[data-testid="filter-query"]')).toHaveValue('')
    await expect(page.locator('[data-testid="filter-status"]')).toHaveValue('')

    // 点击总点击次数统计卡片
    await page.click('[data-testid="stat-total-clicks"]')

    // 验证排序条件改变为按点击次数排序
    await page.waitForTimeout(500) // 等待排序应用
    const sortIndicator = page.locator('[data-testid="sort-clickCount"]')
    await expect(sortIndicator).toHaveAttribute('aria-sort', 'descending')
  })

  test('搜索日志筛选功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待表格加载
    await page.waitForSelector('[data-testid="search-log-table"]')

    // 测试关键词筛选
    await page.fill('[data-testid="filter-query"]', 'Vue.js')
    await page.click('[data-testid="filter-button"]')

    // 验证筛选结果
    await page.waitForSelector('[data-testid="table-row"]')
    const queryColumns = page.locator('[data-testid="query-cell"]')
    const count = await queryColumns.count()

    for (let i = 0; i < count; i++) {
      await expect(queryColumns.nth(i)).toContainText('Vue.js')
    }

    // 测试状态筛选
    await page.selectOption('[data-testid="filter-status"]', 'SUCCESS')
    await page.click('[data-testid="filter-button"]')

    // 验证状态筛选结果
    const statusColumns = page.locator('[data-testid="status-cell"]')
    const statusCount = await statusColumns.count()

    for (let i = 0; i < statusCount; i++) {
      await expect(statusColumns.nth(i)).toContainText('成功')
    }

    // 测试时间范围筛选
    const today = new Date()
    const lastWeek = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)

    await page.fill('[data-testid="filter-start-time"]', lastWeek.toISOString().split('T')[0])
    await page.fill('[data-testid="filter-end-time"]', today.toISOString().split('T')[0])
    await page.click('[data-testid="filter-button"]')

    // 验证时间筛选后有数据显示
    const tableRows = page.locator('[data-testid="table-row"]')
    await expect(tableRows.first()).toBeVisible()
  })

  test('应该支持筛选重置功能', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 设置一些筛选条件
    await page.fill('[data-testid="filter-query"]', 'test query')
    await page.selectOption('[data-testid="filter-status"]', 'ERROR')
    await page.fill('[data-testid="filter-user-id"]', 'user123')

    // 点击重置按钮
    await page.click('[data-testid="reset-filter-button"]')

    // 验证所有筛选条件被清空
    await expect(page.locator('[data-testid="filter-query"]')).toHaveValue('')
    await expect(page.locator('[data-testid="filter-status"]')).toHaveValue('')
    await expect(page.locator('[data-testid="filter-user-id"]')).toHaveValue('')
    await expect(page.locator('[data-testid="filter-start-time"]')).toHaveValue('')
    await expect(page.locator('[data-testid="filter-end-time"]')).toHaveValue('')
  })

  test('分页功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待分页控件加载
    await page.waitForSelector('[data-testid="pagination"]')

    // 验证当前页面显示
    await expect(page.locator('[data-testid="current-page"]')).toContainText('1')

    // 点击下一页
    if (await page.locator('[data-testid="next-page"]').isEnabled()) {
      await page.click('[data-testid="next-page"]')

      // 验证页面更新
      await expect(page.locator('[data-testid="current-page"]')).toContainText('2')

      // 验证URL更新
      await expect(page).toHaveURL(/page=2/)
    }

    // 测试每页显示条数改变
    await page.selectOption('[data-testid="page-size-selector"]', '50')

    // 验证表格重新加载
    await page.waitForSelector('[data-testid="table-row"]')

    // 验证URL参数更新
    await expect(page).toHaveURL(/size=50/)
  })

  test('排序功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待表格加载
    await page.waitForSelector('[data-testid="search-log-table"]')

    // 点击创建时间列标题进行排序
    await page.click('[data-testid="sort-createdAt"]')

    // 验证排序指示器
    await expect(page.locator('[data-testid="sort-createdAt"]')).toHaveAttribute('aria-sort', 'ascending')

    // 再次点击切换为降序
    await page.click('[data-testid="sort-createdAt"]')
    await expect(page.locator('[data-testid="sort-createdAt"]')).toHaveAttribute('aria-sort', 'descending')

    // 点击响应时间列排序
    await page.click('[data-testid="sort-responseTime"]')

    // 验证响应时间排序生效
    await expect(page.locator('[data-testid="sort-responseTime"]')).toHaveAttribute('aria-sort', 'ascending')
    await expect(page.locator('[data-testid="sort-createdAt"]')).toHaveAttribute('aria-sort', 'none')
  })

  test('查看详情功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待表格加载
    await page.waitForSelector('[data-testid="table-row"]')

    // 点击第一行的查看详情按钮
    await page.click('[data-testid="view-detail-button"]:first-child')

    // 验证详情弹窗显示
    await expect(page.locator('[data-testid="detail-modal"]')).toBeVisible()

    // 验证详情内容
    await expect(page.locator('[data-testid="log-id"]')).toBeVisible()
    await expect(page.locator('[data-testid="log-query"]')).toBeVisible()
    await expect(page.locator('[data-testid="log-user-info"]')).toBeVisible()
    await expect(page.locator('[data-testid="log-request-params"]')).toBeVisible()
    await expect(page.locator('[data-testid="log-response-data"]')).toBeVisible()
    await expect(page.locator('[data-testid="click-behavior-section"]')).toBeVisible()

    // 关闭弹窗
    await page.click('[data-testid="close-modal-button"]')
    await expect(page.locator('[data-testid="detail-modal"]')).not.toBeVisible()
  })

  test('点击行为查看功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待表格加载
    await page.waitForSelector('[data-testid="table-row"]')

    // 点击第一行的查看点击按钮
    await page.click('[data-testid="view-clicks-button"]:first-child')

    // 验证详情弹窗显示并定位到点击行为部分
    await expect(page.locator('[data-testid="detail-modal"]')).toBeVisible()
    await expect(page.locator('[data-testid="click-behavior-section"]')).toBeVisible()

    // 验证点击记录列表
    const clickLogs = page.locator('[data-testid="click-log-item"]')
    if (await clickLogs.count() > 0) {
      // 验证点击记录的基本信息
      await expect(clickLogs.first().locator('[data-testid="click-document-title"]')).toBeVisible()
      await expect(clickLogs.first().locator('[data-testid="click-position"]')).toBeVisible()
      await expect(clickLogs.first().locator('[data-testid="click-time"]')).toBeVisible()
    }
  })

  test('数据导出功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待页面加载
    await page.waitForSelector('[data-testid="export-button"]')

    // 设置下载处理
    const downloadPromise = page.waitForEvent('download')

    // 点击导出按钮
    await page.click('[data-testid="export-button"]')

    // 等待下载开始
    const download = await downloadPromise

    // 验证下载文件名
    expect(download.suggestedFilename()).toMatch(/搜索日志_\d{4}-\d{2}-\d{2}\.xlsx$/)

    // 验证导出成功提示
    await expect(page.locator('[data-testid="notification"]')).toContainText('数据导出成功')
  })

  test('数据刷新功能应该正常工作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待初始数据加载
    await page.waitForSelector('[data-testid="table-row"]')

    // 点击刷新按钮
    await page.click('[data-testid="refresh-button"]')

    // 验证刷新状态
    await expect(page.locator('[data-testid="refresh-button"]')).toBeDisabled()
    await expect(page.locator('[data-testid="refresh-button"] .loading-spinner')).toBeVisible()

    // 等待刷新完成
    await page.waitForSelector('[data-testid="refresh-button"]:not([disabled])', { timeout: 10000 })

    // 验证刷新成功提示
    await expect(page.locator('[data-testid="notification"]')).toContainText('数据刷新成功')
  })

  test('响应式设计应该在移动设备上正常工作', async ({ page }) => {
    // 设置移动设备视口
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/admin/search-logs')

    // 验证移动端布局
    await expect(page.locator('.stats-dashboard .grid')).toHaveClass(/grid-cols-1/)

    // 验证统计卡片在移动端的显示
    await expect(page.locator('[data-testid="stat-total-searches"]')).toBeVisible()

    // 验证表格响应式行为（可能是横滑或折叠显示）
    await expect(page.locator('[data-testid="search-log-table"]')).toBeVisible()

    // 验证筛选器在移动端的表现
    await expect(page.locator('[data-testid="search-log-filter"]')).toBeVisible()
  })

  test('错误处理场景应该正确显示', async ({ page }) => {
    // 模拟API错误响应
    await page.route('**/api/search-logs**', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          message: '服务器内部错误'
        })
      })
    })

    await page.goto('/admin/search-logs')

    // 验证错误状态显示
    await expect(page.locator('[data-testid="error-message"]')).toContainText('数据加载失败')

    // 验证重试机制
    if (await page.locator('[data-testid="retry-button"]').isVisible()) {
      await page.click('[data-testid="retry-button"]')
    }
  })

  test('无数据状态应该正确显示', async ({ page }) => {
    // 模拟空数据响应
    await page.route('**/api/search-logs**', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 20,
            number: 0,
            first: true,
            last: true
          }
        })
      })
    })

    await page.goto('/admin/search-logs')

    // 验证空状态显示
    await expect(page.locator('[data-testid="empty-state"]')).toBeVisible()
    await expect(page.locator('[data-testid="empty-state"]')).toContainText('暂无搜索日志数据')
  })

  test('应该支持键盘导航', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待页面加载
    await page.waitForSelector('[data-testid="search-log-table"]')

    // 使用Tab键导航到筛选输入框
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')

    // 验证焦点在筛选输入框上
    await expect(page.locator('[data-testid="filter-query"]')).toBeFocused()

    // 输入筛选条件并按Enter执行筛选
    await page.keyboard.type('test')
    await page.keyboard.press('Enter')

    // 验证筛选执行
    await page.waitForTimeout(500)
    await expect(page.locator('[data-testid="query-cell"]')).toContainText('test')
  })

  test('应该支持批量操作', async ({ page }) => {
    await page.goto('/admin/search-logs')

    // 等待表格加载
    await page.waitForSelector('[data-testid="table-row"]')

    // 选择多行数据（如果支持批量选择）
    if (await page.locator('[data-testid="select-all-checkbox"]').isVisible()) {
      await page.click('[data-testid="select-all-checkbox"]')

      // 验证批量操作按钮可用
      await expect(page.locator('[data-testid="bulk-export-button"]')).toBeEnabled()

      // 执行批量导出
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-testid="bulk-export-button"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/搜索日志_批量导出_\d{4}-\d{2}-\d{2}\.xlsx$/)
    }
  })
})

/**
 * 设置模拟API响应
 */
async function setupMockResponses(page: any) {
  // 模拟搜索日志统计API
  await page.route('**/api/search-logs/statistics**', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          totalSearches: 12485,
          successRate: 87.3,
          avgResponseTime: 342,
          totalClicks: 8721,
          topKeywords: [
            { keyword: 'Vue.js', count: 1250 },
            { keyword: 'TypeScript', count: 980 },
            { keyword: 'Composition API', count: 760 }
          ],
          searchTrends: [
            { date: '2024-01-01', count: 120 },
            { date: '2024-01-02', count: 135 },
            { date: '2024-01-03', count: 142 }
          ],
          searchTrend: { direction: 'up', value: 12.5 },
          successTrend: { direction: 'up', value: 3.2 },
          responseTrend: { direction: 'down', value: 8.1 },
          clickTrend: { direction: 'up', value: 15.7 }
        }
      })
    })
  })

  // 模拟搜索日志列表API
  await page.route('**/api/search-logs**', route => {
    const url = new URL(route.request().url())
    const page_param = parseInt(url.searchParams.get('page') || '0')
    const size_param = parseInt(url.searchParams.get('size') || '20')
    const query = url.searchParams.get('query') || ''

    // 生成模拟数据
    const mockData = generateMockSearchLogs(page_param, size_param, query)

    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: mockData
      })
    })
  })

  // 模拟搜索日志详情API
  await page.route('**/api/search-logs/*/detail**', route => {
    const logId = route.request().url().match(/search-logs\/(\d+)\/detail/)?.[1]

    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: parseInt(logId || '1'),
          userId: 'user123',
          userIp: '192.168.1.100',
          searchSpaceId: 'space-001',
          query: 'Vue.js 组件开发',
          resultCount: 25,
          responseTime: 342,
          status: 'SUCCESS',
          createdAt: '2024-01-15T10:30:00Z',
          clickCount: 3,
          requestParams: JSON.stringify({
            query: 'Vue.js 组件开发',
            filters: {},
            pagination: { page: 0, size: 10 }
          }),
          responseData: JSON.stringify({
            total: 25,
            hits: ['doc1', 'doc2', 'doc3']
          }),
          clickLogs: [
            {
              id: 1,
              documentId: 'doc1',
              documentTitle: 'Vue.js 组件开发指南',
              documentUrl: 'https://example.com/vue-guide',
              clickPosition: 1,
              clickTime: '2024-01-15T10:31:00Z',
              userAgent: 'Mozilla/5.0...',
              clickType: 'normal'
            },
            {
              id: 2,
              documentId: 'doc2',
              documentTitle: 'Vue.js 最佳实践',
              documentUrl: 'https://example.com/vue-best-practices',
              clickPosition: 2,
              clickTime: '2024-01-15T10:32:00Z',
              userAgent: 'Mozilla/5.0...',
              clickType: 'normal'
            }
          ]
        }
      })
    })
  })
}

/**
 * 生成模拟搜索日志数据
 */
function generateMockSearchLogs(page: number, size: number, query: string) {
  const totalElements = query ? 5 : 157 // 如果有查询条件，返回较少数据
  const totalPages = Math.ceil(totalElements / size)

  const content = []
  const startIndex = page * size
  const endIndex = Math.min(startIndex + size, totalElements)

  for (let i = startIndex; i < endIndex; i++) {
    content.push({
      id: i + 1,
      userId: `user${(i % 10) + 1}`,
      userIp: `192.168.1.${100 + (i % 50)}`,
      searchSpaceId: `space-${String(i % 3 + 1).padStart(3, '0')}`,
      query: query || `搜索关键词 ${i + 1}`,
      resultCount: Math.floor(Math.random() * 100) + 1,
      responseTime: Math.floor(Math.random() * 1000) + 100,
      status: Math.random() > 0.1 ? 'SUCCESS' : 'ERROR',
      createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
      clickCount: Math.floor(Math.random() * 5)
    })
  }

  return {
    content,
    totalElements,
    totalPages,
    size,
    number: page,
    first: page === 0,
    last: page === totalPages - 1
  }
}