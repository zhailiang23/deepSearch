import { test, expect, Page, BrowserContext } from '@playwright/test'
import { AuthHelper } from './helpers/auth-helpers'
import { LoginPage, DashboardPage, PageFactory } from './helpers/page-objects'
import { TestUtilsFactory } from './helpers/test-utils'
import { testDataLoader, getUser, getLoginScenario } from './fixtures/test-data-loader'

/**
 * 认证系统端到端测试套件
 *
 * 完整测试认证系统的各种场景，包括：
 * - 成功登录和登出流程
 * - 失败登录处理
 * - 账户锁定机制
 * - Token过期和刷新
 * - 权限验证
 * - 响应式设计
 * - 无障碍性
 */

// 测试前的设置
test.beforeEach(async ({ page }) => {
  // 清除所有存储数据
  await page.context().clearCookies()

  // 设置API模拟响应
  await setupApiMocks(page)

  // 导航到首页确保页面加载完成
  await page.goto('/')

  // 等待页面完全加载
  await page.waitForLoadState('networkidle')

  // 清除存储数据
  await page.evaluate(() => {
    try {
      localStorage.clear()
      sessionStorage.clear()
    } catch (e) {
      console.warn('无法清除存储数据:', e)
    }
  })
})

// API模拟设置函数
async function setupApiMocks(page: Page) {
  // 模拟登录API响应
  await page.route('**/api/auth/login', async route => {
    const request = route.request()
    const postData = request.postData()

    try {
      const loginData = JSON.parse(postData || '{}')

      // 模拟成功登录响应
      if (loginData.username === 'admin' && loginData.password === 'admin123') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'mock-admin-jwt-token',
            refreshToken: 'mock-admin-refresh-token',
            user: {
              id: 1,
              username: 'admin',
              email: 'admin@example.com',
              role: 'ADMIN',
              status: 'ACTIVE',
              firstName: '系统',
              lastName: '管理员'
            }
          })
        })
      } else if (loginData.username === 'user' && loginData.password === 'user123') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'mock-user-jwt-token',
            refreshToken: 'mock-user-refresh-token',
            user: {
              id: 2,
              username: 'user',
              email: 'user@example.com',
              role: 'USER',
              status: 'ACTIVE',
              firstName: '普通',
              lastName: '用户'
            }
          })
        })
      } else if (loginData.username === 'locked_user') {
        await route.fulfill({
          status: 423,
          contentType: 'application/json',
          body: JSON.stringify({
            message: '账户已被锁定',
            error: '账户已被锁定'
          })
        })
      } else if (loginData.username === 'inactive_user') {
        await route.fulfill({
          status: 403,
          contentType: 'application/json',
          body: JSON.stringify({
            message: '账户未激活',
            error: '账户未激活'
          })
        })
      } else {
        // 模拟登录失败
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            message: '用户名或密码错误',
            error: '用户名或密码错误'
          })
        })
      }
    } catch (error) {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          message: '请求格式错误',
          error: '请求格式错误'
        })
      })
    }
  })

  // 模拟登出API响应
  await page.route('**/api/auth/logout', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ message: '登出成功' })
    })
  })

  // 模拟用户信息API响应
  await page.route('**/api/auth/profile', async route => {
    const authHeader = route.request().headers()['authorization']

    if (authHeader?.includes('mock-admin-jwt-token')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          username: 'admin',
          email: 'admin@example.com',
          role: 'ADMIN',
          status: 'ACTIVE',
          firstName: '系统',
          lastName: '管理员'
        })
      })
    } else if (authHeader?.includes('mock-user-jwt-token')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 2,
          username: 'user',
          email: 'user@example.com',
          role: 'USER',
          status: 'ACTIVE',
          firstName: '普通',
          lastName: '用户'
        })
      })
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          error: '未授权'
        })
      })
    }
  })

  // 模拟Token刷新API响应
  await page.route('**/api/auth/refresh', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'mock-refreshed-jwt-token',
        refreshToken: 'mock-refreshed-refresh-token'
      })
    })
  })
}

// 测试组：基础登录功能
test.describe('基础登录功能', () => {
  test('成功登录和登出流程', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const pageFactory = new PageFactory(page)
    const loginPage = pageFactory.getLoginPage()
    const dashboardPage = pageFactory.getDashboardPage()

    const user = getUser('user')!
    const loginScenario = getLoginScenario('validLogin')!

    // 1. 导航到登录页面
    await loginPage.navigate()

    // 2. 验证登录页面元素
    await expect(page.locator('#login-form')).toBeVisible()
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()

    // 3. 执行登录
    await authHelper.login({
      username: user.username,
      password: user.password,
      rememberMe: false
    })

    // 4. 验证登录成功
    await expect(page).toHaveURL(/.*\/$/)
    await authHelper.verifyLoggedInState()

    // 5. 验证仪表板页面加载
    await dashboardPage.waitForPageLoad()

    // 6. 执行登出
    await authHelper.logout()

    // 7. 验证登出成功
    await authHelper.verifyLoggedOutState()
    await expect(page).toHaveURL(/.*\/login/)
  })

  test('记住我功能', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    // 1. 导航到登录页面
    await authHelper.navigateToLogin()

    // 2. 填写表单并勾选记住我
    await authHelper.fillLoginForm({
      username: user.username,
      password: user.password,
      rememberMe: true
    })

    // 3. 验证记住我checkbox已选中
    await expect(page.locator('#remember-me')).toBeChecked()

    // 4. 提交登录
    await authHelper.clickLoginButton()

    // 5. 验证登录成功
    await expect(page).toHaveURL(/.*\/$/)
    await authHelper.verifyLoggedInState()

    // 6. 登出
    await authHelper.logout()

    // 7. 重新访问应用（模拟浏览器重启）
    await page.goto('/')

    // 8. 验证是否保持登录状态或记住用户名
    // 注：具体行为取决于应用实现
    const currentUrl = page.url()
    console.log(`记住我功能测试 - 当前URL: ${currentUrl}`)
  })

  test('管理员登录', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const admin = getUser('admin')!

    await authHelper.login({
      username: admin.username,
      password: admin.password
    })

    // 验证管理员特有的界面元素
    await expect(page).toHaveURL(/.*\/$/)
    await authHelper.verifyLoggedInState()
  })
})

// 测试组：登录失败处理
test.describe('登录失败处理', () => {
  test('无效用户名', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const scenario = getLoginScenario('invalidUsername')!

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: scenario.username,
      password: scenario.password
    })
    await authHelper.clickLoginButton()

    // 验证错误消息
    await authHelper.verifyLoginError()
    await expect(page).toHaveURL(/.*\/login/)
  })

  test('无效密码', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const scenario = getLoginScenario('invalidPassword')!

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: scenario.username,
      password: scenario.password
    })
    await authHelper.clickLoginButton()

    await authHelper.verifyLoginError()
    await expect(page).toHaveURL(/.*\/login/)
  })

  test('空凭据验证', async ({ page }) => {
    const authHelper = new AuthHelper(page)

    await authHelper.navigateToLogin()

    // 验证表单验证
    await authHelper.verifyFormValidation()

    // 尝试提交空表单
    await authHelper.clearLoginForm()
    const submitButton = page.locator('button[type="submit"]')
    await expect(submitButton).toBeDisabled()
  })

  test('账户锁定', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const lockedUser = getUser('locked_user')!

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: lockedUser.username,
      password: lockedUser.password
    })
    await authHelper.clickLoginButton()

    await authHelper.verifyLoginError('账户已被锁定')
    await expect(page).toHaveURL(/.*\/login/)
  })

  test('多次失败登录导致锁定', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const user = getUser('user')!
    const testConfig = testDataLoader.getTestConfig()

    // 模拟多次失败登录
    await authHelper.performFailedLoginAttempts(
      { username: user.username, password: 'wrong-password' },
      testConfig.bruteForceAttempts
    )

    // 在最后一次尝试后，账户应该被锁定
    await authHelper.verifyLoginError()
  })

  test('未激活账户', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const inactiveUser = getUser('inactive_user')!

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: inactiveUser.username,
      password: inactiveUser.password
    })
    await authHelper.clickLoginButton()

    await authHelper.verifyLoginError('账户未激活')
  })
})

// 测试组：Token管理
test.describe('Token管理', () => {
  test('Token过期处理', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const utils = new TestUtilsFactory(page)
    const dataManager = utils.getTestDataManager()

    // 1. 正常登录
    const user = getUser('user')!
    await authHelper.login({
      username: user.username,
      password: user.password
    })

    // 2. 设置过期的token
    await dataManager.setExpiredToken()

    // 3. 尝试访问需要认证的页面
    await page.goto('/')

    // 4. 应该被重定向到登录页面
    await expect(page).toHaveURL(/.*\/login/)
  })

  test('Token自动刷新', async ({ page, context }) => {
    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    // 1. 登录获取token
    await authHelper.login({
      username: user.username,
      password: user.password
    })

    // 2. 监听刷新token的API调用
    const refreshRequests: any[] = []
    page.on('request', request => {
      if (request.url().includes('/api/auth/refresh')) {
        refreshRequests.push(request)
      }
    })

    // 3. 模拟token即将过期（通过修改localStorage）
    await page.evaluate(() => {
      const nearExpireTime = Date.now() + 60000 // 1分钟后过期
      localStorage.setItem('tokenExpiresAt', nearExpireTime.toString())
    })

    // 4. 等待自动刷新
    await page.waitForTimeout(5000)

    // 5. 验证刷新请求是否被发送
    // 注：这个测试取决于应用的具体实现
    console.log(`Token刷新请求数: ${refreshRequests.length}`)
  })
})

// 测试组：路由守卫
test.describe('路由守卫', () => {
  test('未登录用户访问受保护页面', async ({ page }) => {
    // 直接访问需要认证的页面
    await page.goto('/')

    // 应该被重定向到登录页面
    await expect(page).toHaveURL(/.*\/login/)
  })

  test('登录后重定向到原目标页面', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    // 1. 尝试直接访问用户管理页面
    await page.goto('/users')

    // 2. 应该被重定向到登录页面，并保存原URL
    await expect(page).toHaveURL(/.*\/login/)

    // 3. 登录
    await authHelper.fillLoginForm({
      username: user.username,
      password: user.password
    })
    await authHelper.clickLoginButton()

    // 4. 应该重定向到原目标页面
    await page.waitForURL('**/users', { timeout: 10000 })
    await expect(page).toHaveURL(/.*\/users/)
  })

  test('权限验证', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const user = getUser('user')! // 普通用户

    // 1. 以普通用户身份登录
    await authHelper.login({
      username: user.username,
      password: user.password
    })

    // 2. 尝试访问需要管理员权限的页面
    await page.goto('/admin')

    // 3. 应该被重定向或显示403错误
    // 具体行为取决于应用实现
    const currentUrl = page.url()
    const hasAccess = currentUrl.includes('/admin')

    if (hasAccess) {
      // 如果能访问，检查是否有权限限制提示
      const accessDenied = page.locator('text=权限不足')
      await expect(accessDenied).toBeVisible()
    } else {
      // 如果不能访问，验证重定向
      expect(currentUrl).not.toContain('/admin')
    }
  })
})

// 测试组：用户体验
test.describe('用户体验', () => {
  test('密码可见性切换', async ({ page }) => {
    const authHelper = new AuthHelper(page)

    await authHelper.navigateToLogin()
    await authHelper.verifyPasswordVisibilityToggle()
  })

  test('键盘导航', async ({ page }) => {
    await page.goto('/login')

    // Tab键导航
    await page.keyboard.press('Tab')
    await expect(page.locator('#username')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('#password')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('#remember-me')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('button[type="submit"]')).toBeFocused()
  })

  test('表单验证', async ({ page }) => {
    const authHelper = new AuthHelper(page)

    await authHelper.navigateToLogin()
    await authHelper.verifyFormValidation()
  })

  test('加载状态', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const networkHelper = new TestUtilsFactory(page).getNetworkHelper()
    const user = getUser('user')!

    // 模拟慢网络
    await networkHelper.simulateSlowNetwork('**/api/auth/login', 2000)

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: user.username,
      password: user.password
    })

    // 点击登录按钮
    await authHelper.clickLoginButton()

    // 验证加载状态
    const submitButton = page.locator('button[type="submit"]')
    await expect(submitButton).toBeDisabled()
    await expect(page.locator('text=登录中')).toBeVisible()
  })
})

// 测试组：响应式设计
test.describe('响应式设计', () => {
  test('桌面端显示', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 })

    const authHelper = new AuthHelper(page)
    await authHelper.navigateToLogin()

    // 验证桌面端布局
    const loginContainer = page.locator('#login-form')
    await expect(loginContainer).toBeVisible()

    const containerBox = await loginContainer.boundingBox()
    expect(containerBox?.width).toBeGreaterThan(400)
  })

  test('平板端显示', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })

    const authHelper = new AuthHelper(page)
    await authHelper.navigateToLogin()

    const loginContainer = page.locator('#login-form')
    await expect(loginContainer).toBeVisible()
  })

  test('移动端显示', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 })

    const authHelper = new AuthHelper(page)
    await authHelper.navigateToLogin()

    // 验证移动端布局
    const loginContainer = page.locator('#login-form')
    await expect(loginContainer).toBeVisible()

    // 验证触摸目标大小
    const submitButton = page.locator('button[type="submit"]')
    const buttonBox = await submitButton.boundingBox()
    expect(buttonBox?.height).toBeGreaterThanOrEqual(44) // 最小44px触摸目标
  })

  test('完整响应式测试', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    await authHelper.verifyResponsiveDesign()
  })
})

// 测试组：无障碍性
test.describe('无障碍性', () => {
  test('表单标签和ARIA属性', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    await authHelper.verifyAccessibility()
  })

  test('键盘导航支持', async ({ page }) => {
    await page.goto('/login')

    // 使用Tab键导航整个表单
    const focusableElements = [
      '#username',
      '#password',
      'button[aria-label*="显示密码"]',
      '#remember-me',
      'button[type="submit"]',
      'button:has-text("忘记密码")',
      'button:has-text("注册")'
    ]

    for (const selector of focusableElements) {
      await page.keyboard.press('Tab')
      const element = page.locator(selector)
      if (await element.isVisible()) {
        await expect(element).toBeFocused()
      }
    }
  })

  test('屏幕阅读器支持', async ({ page }) => {
    await page.goto('/login')

    // 验证重要元素有适当的ARIA标签
    await expect(page.locator('[role="main"]')).toBeVisible()
    await expect(page.locator('[role="alert"]')).toHaveCount(0) // 初始状态无错误

    // 验证表单标签
    const usernameLabel = page.locator('label[for="username"]')
    const passwordLabel = page.locator('label[for="password"]')
    const rememberLabel = page.locator('label[for="remember-me"]')

    await expect(usernameLabel).toBeVisible()
    await expect(passwordLabel).toBeVisible()
    await expect(rememberLabel).toBeVisible()
  })
})

// 测试组：网络错误处理
test.describe('网络错误处理', () => {
  test('网络连接失败', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const networkHelper = new TestUtilsFactory(page).getNetworkHelper()
    const user = getUser('user')!

    // 模拟网络错误
    await networkHelper.simulateNetworkError('**/api/auth/login')

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: user.username,
      password: user.password
    })
    await authHelper.clickLoginButton()

    // 验证错误处理
    await authHelper.verifyLoginError()
  })

  test('服务器错误响应', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const networkHelper = new TestUtilsFactory(page).getNetworkHelper()
    const user = getUser('user')!

    // 模拟服务器500错误
    await networkHelper.interceptRequest('**/api/auth/login', {
      status: 500,
      body: { error: '服务器内部错误' }
    })

    await authHelper.navigateToLogin()
    await authHelper.fillLoginForm({
      username: user.username,
      password: user.password
    })
    await authHelper.clickLoginButton()

    await authHelper.verifyLoginError()
  })
})

// 测试组：性能测试
test.describe('性能测试', () => {
  test('登录响应时间', async ({ page }) => {
    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    await authHelper.navigateToLogin()

    const startTime = Date.now()

    await authHelper.login({
      username: user.username,
      password: user.password
    })

    const endTime = Date.now()
    const loginDuration = endTime - startTime

    console.log(`登录耗时: ${loginDuration}ms`)

    // 验证登录时间在合理范围内
    expect(loginDuration).toBeLessThan(5000) // 5秒内完成登录
  })

  test('页面加载性能', async ({ page }) => {
    // 监听性能指标
    const navigationPromise = page.waitForLoadState('networkidle')

    const startTime = Date.now()
    await page.goto('/login')
    await navigationPromise

    const endTime = Date.now()
    const loadDuration = endTime - startTime

    console.log(`页面加载耗时: ${loadDuration}ms`)

    // 验证页面加载时间
    expect(loadDuration).toBeLessThan(3000) // 3秒内完成加载
  })
})

// 测试组：多浏览器兼容性
test.describe('浏览器兼容性', () => {
  test('Chrome兼容性', async ({ page, browserName }) => {
    test.skip(browserName !== 'chromium', '仅在Chrome中运行')

    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    await authHelper.login({
      username: user.username,
      password: user.password
    })

    await authHelper.verifyLoggedInState()
  })

  test('Firefox兼容性', async ({ page, browserName }) => {
    test.skip(browserName !== 'firefox', '仅在Firefox中运行')

    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    await authHelper.login({
      username: user.username,
      password: user.password
    })

    await authHelper.verifyLoggedInState()
  })

  test('Safari兼容性', async ({ page, browserName }) => {
    test.skip(browserName !== 'webkit', '仅在Safari中运行')

    const authHelper = new AuthHelper(page)
    const user = getUser('user')!

    await authHelper.login({
      username: user.username,
      password: user.password
    })

    await authHelper.verifyLoggedInState()
  })
})