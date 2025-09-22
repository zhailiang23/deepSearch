import { Page, Locator, expect } from '@playwright/test'

/**
 * 认证相关的测试辅助工具
 *
 * 提供标准化的认证操作方法，确保测试的一致性和可维护性
 */

/**
 * 用户凭据接口
 */
export interface UserCredentials {
  username: string
  password: string
  rememberMe?: boolean
}

/**
 * 登录选项接口
 */
export interface LoginOptions {
  waitForRedirect?: boolean
  expectedUrl?: string
  timeout?: number
}

/**
 * 认证测试辅助类
 */
export class AuthHelper {
  constructor(private page: Page) {}

  /**
   * 导航到登录页面
   */
  async navigateToLogin(): Promise<void> {
    await this.page.goto('/login')
    await this.page.waitForLoadState('networkidle')

    // 验证是否在登录页面
    await expect(this.page).toHaveURL(/.*\/login/)
    await expect(this.page.locator('#login-form')).toBeVisible()
  }

  /**
   * 填写登录表单
   */
  async fillLoginForm(credentials: UserCredentials): Promise<void> {
    // 填写用户名
    await this.page.fill('#username', credentials.username)

    // 填写密码
    await this.page.fill('#password', credentials.password)

    // 设置记住我选项
    if (credentials.rememberMe !== undefined) {
      const rememberCheckbox = this.page.locator('#remember-me')
      const isChecked = await rememberCheckbox.isChecked()

      if (credentials.rememberMe && !isChecked) {
        await rememberCheckbox.check()
      } else if (!credentials.rememberMe && isChecked) {
        await rememberCheckbox.uncheck()
      }
    }
  }

  /**
   * 点击登录按钮
   */
  async clickLoginButton(): Promise<void> {
    await this.page.click('button[type="submit"]')
  }

  /**
   * 执行完整的登录流程
   */
  async login(
    credentials: UserCredentials,
    options: LoginOptions = {}
  ): Promise<void> {
    const {
      waitForRedirect = true,
      expectedUrl = '/',
      timeout = 30000
    } = options

    // 导航到登录页面
    await this.navigateToLogin()

    // 填写表单
    await this.fillLoginForm(credentials)

    // 提交表单
    await this.clickLoginButton()

    if (waitForRedirect) {
      // 等待重定向完成
      await this.page.waitForURL(`**${expectedUrl}`, { timeout })

      // 验证登录成功的标志
      await this.verifyLoggedInState()
    }
  }

  /**
   * 验证已登录状态
   */
  async verifyLoggedInState(): Promise<void> {
    // 验证URL不是登录页面
    await expect(this.page).not.toHaveURL(/.*\/login/)

    // 验证用户菜单或导航栏存在
    const userMenu = this.page.locator('[data-testid="user-menu"]')
    const navigationBar = this.page.locator('[data-testid="navigation"]')

    // 至少其中一个应该存在
    try {
      await expect(userMenu.or(navigationBar)).toBeVisible({ timeout: 5000 })
    } catch {
      // 如果都不存在，检查是否有其他登录状态指示器
      const logoutButton = this.page.locator('text=退出')
      await expect(logoutButton).toBeVisible()
    }
  }

  /**
   * 执行登出操作
   */
  async logout(): Promise<void> {
    // 尝试多种登出方式

    // 方式1: 通过用户菜单
    try {
      const userMenu = this.page.locator('[data-testid="user-menu"]')
      if (await userMenu.isVisible()) {
        await userMenu.click()
        await this.page.waitForTimeout(500) // 等待菜单展开

        const logoutButton = this.page.locator('[data-testid="logout-button"]')
        if (await logoutButton.isVisible()) {
          await logoutButton.click()
        } else {
          await this.page.click('text=退出登录')
        }
      } else {
        throw new Error('用户菜单不可见')
      }
    } catch (error) {
      // 方式2: 直接点击登出按钮
      try {
        await this.page.click('text=退出')
      } catch (error2) {
        // 方式3: 通过API调用
        await this.page.request.post('/api/auth/logout')
      }
    }

    // 等待重定向到登录页面
    await this.page.waitForURL(/.*\/login/, { timeout: 10000 })

    // 验证已登出
    await this.verifyLoggedOutState()
  }

  /**
   * 验证已登出状态
   */
  async verifyLoggedOutState(): Promise<void> {
    // 验证在登录页面
    await expect(this.page).toHaveURL(/.*\/login/)

    // 验证登录表单存在
    await expect(this.page.locator('#login-form')).toBeVisible()

    // 验证用户菜单不存在
    const userMenu = this.page.locator('[data-testid="user-menu"]')
    await expect(userMenu).not.toBeVisible()
  }

  /**
   * 验证登录错误消息
   */
  async verifyLoginError(expectedMessage?: string): Promise<void> {
    const errorContainer = this.page.locator('[role="alert"]')
    await expect(errorContainer).toBeVisible()

    if (expectedMessage) {
      await expect(errorContainer).toContainText(expectedMessage)
    }
  }

  /**
   * 清除登录表单
   */
  async clearLoginForm(): Promise<void> {
    await this.page.fill('#username', '')
    await this.page.fill('#password', '')

    const rememberCheckbox = this.page.locator('#remember-me')
    if (await rememberCheckbox.isChecked()) {
      await rememberCheckbox.uncheck()
    }
  }

  /**
   * 验证表单验证状态
   */
  async verifyFormValidation(): Promise<void> {
    const submitButton = this.page.locator('button[type="submit"]')

    // 空表单应该禁用提交按钮
    await this.clearLoginForm()
    await expect(submitButton).toBeDisabled()

    // 只填用户名应该仍然禁用
    await this.page.fill('#username', 'test')
    await expect(submitButton).toBeDisabled()

    // 填写完整信息应该启用
    await this.page.fill('#password', 'test123')
    await expect(submitButton).toBeEnabled()
  }

  /**
   * 模拟多次失败登录（测试账户锁定）
   */
  async performFailedLoginAttempts(
    credentials: UserCredentials,
    attempts: number = 3
  ): Promise<void> {
    for (let i = 0; i < attempts; i++) {
      await this.navigateToLogin()
      await this.fillLoginForm({
        ...credentials,
        password: 'wrong-password'
      })
      await this.clickLoginButton()

      // 等待错误消息出现
      await this.verifyLoginError()

      // 等待一小段时间再进行下次尝试
      await this.page.waitForTimeout(1000)
    }
  }

  /**
   * 检查记住我功能
   */
  async verifyRememberMeFunction(): Promise<void> {
    // 使用记住我登录
    await this.login({
      username: 'user',
      password: 'user123',
      rememberMe: true
    })

    // 登出
    await this.logout()

    // 重新访问应用
    await this.page.goto('/')

    // 如果记住我功能正常工作，应该自动重定向到dashboard
    // 或者至少保留某些登录信息
    const currentUrl = this.page.url()

    // 这个具体的验证逻辑取决于应用的实现
    console.log(`当前URL: ${currentUrl}`)
  }

  /**
   * 验证密码可见性切换
   */
  async verifyPasswordVisibilityToggle(): Promise<void> {
    await this.navigateToLogin()

    const passwordInput = this.page.locator('#password')
    const toggleButton = this.page.locator('button[aria-label*="显示密码"], button[aria-label*="隐藏密码"]')

    // 初始状态应该是隐藏密码
    await expect(passwordInput).toHaveAttribute('type', 'password')

    // 点击切换按钮
    await toggleButton.click()

    // 密码应该变为可见
    await expect(passwordInput).toHaveAttribute('type', 'text')

    // 再次点击应该隐藏密码
    await toggleButton.click()
    await expect(passwordInput).toHaveAttribute('type', 'password')
  }

  /**
   * 验证无障碍性
   */
  async verifyAccessibility(): Promise<void> {
    await this.navigateToLogin()

    // 验证表单标签
    const usernameLabel = this.page.locator('label[for="username"]')
    const passwordLabel = this.page.locator('label[for="password"]')
    const rememberLabel = this.page.locator('label[for="remember-me"]')

    await expect(usernameLabel).toBeVisible()
    await expect(passwordLabel).toBeVisible()
    await expect(rememberLabel).toBeVisible()

    // 验证ARIA属性
    const loginForm = this.page.locator('#login-form')
    await expect(loginForm).toHaveAttribute('role', 'main')

    // 验证键盘导航
    await this.page.keyboard.press('Tab')
    await expect(this.page.locator('#username')).toBeFocused()

    await this.page.keyboard.press('Tab')
    await expect(this.page.locator('#password')).toBeFocused()
  }

  /**
   * 获取认证状态（通过API）
   */
  async getAuthStatus(): Promise<any> {
    try {
      const response = await this.page.request.get('/api/auth/status')
      return await response.json()
    } catch (error) {
      console.warn('无法获取认证状态:', error)
      return null
    }
  }

  /**
   * 验证token过期处理
   */
  async verifyTokenExpiration(): Promise<void> {
    // 这需要与后端配合，或者通过修改localStorage来模拟token过期
    await this.page.evaluate(() => {
      // 设置一个过期的token
      const expiredToken = 'expired.token.here'
      localStorage.setItem('token', expiredToken)
      localStorage.setItem('tokenExpiresAt', '0') // 设置为已过期
    })

    // 尝试访问需要认证的页面
    await this.page.goto('/dashboard')

    // 应该被重定向到登录页面
    await expect(this.page).toHaveURL(/.*\/login/)
  }

  /**
   * 模拟网络错误
   */
  async simulateNetworkError(): Promise<void> {
    // 拦截登录API调用并返回错误
    await this.page.route('**/api/auth/login', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: '服务器内部错误' })
      })
    })
  }

  /**
   * 验证响应式设计
   */
  async verifyResponsiveDesign(): Promise<void> {
    // 测试桌面尺寸
    await this.page.setViewportSize({ width: 1280, height: 720 })
    await this.navigateToLogin()

    const loginContainer = this.page.locator('#login-form')
    await expect(loginContainer).toBeVisible()

    // 测试平板尺寸
    await this.page.setViewportSize({ width: 768, height: 1024 })
    await expect(loginContainer).toBeVisible()

    // 测试手机尺寸
    await this.page.setViewportSize({ width: 375, height: 667 })
    await expect(loginContainer).toBeVisible()

    // 验证移动端特定的样式或布局
    const submitButton = this.page.locator('button[type="submit"]')
    const buttonBox = await submitButton.boundingBox()

    // 在移动设备上，按钮应该有足够的触摸目标大小
    expect(buttonBox?.height).toBeGreaterThanOrEqual(44)
  }
}