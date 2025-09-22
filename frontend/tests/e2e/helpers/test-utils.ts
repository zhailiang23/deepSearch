import { Page, Browser, BrowserContext } from '@playwright/test'

/**
 * 测试工具类
 *
 * 提供通用的测试辅助功能
 */

/**
 * 浏览器会话管理
 */
export class SessionManager {
  private contexts: Map<string, BrowserContext> = new Map()

  constructor(private browser: Browser) {}

  /**
   * 创建新的用户会话
   */
  async createUserSession(
    userId: string,
    storageStatePath?: string
  ): Promise<BrowserContext> {
    const context = await this.browser.newContext({
      storageState: storageStatePath
    })

    this.contexts.set(userId, context)
    return context
  }

  /**
   * 获取用户会话
   */
  getUserSession(userId: string): BrowserContext | undefined {
    return this.contexts.get(userId)
  }

  /**
   * 清理所有会话
   */
  async cleanupSessions(): Promise<void> {
    for (const [userId, context] of this.contexts) {
      await context.close()
    }
    this.contexts.clear()
  }

  /**
   * 保存会话状态
   */
  async saveSessionState(
    userId: string,
    filePath: string
  ): Promise<void> {
    const context = this.contexts.get(userId)
    if (context) {
      await context.storageState({ path: filePath })
    }
  }
}

/**
 * 数据管理工具
 */
export class TestDataManager {
  constructor(private page: Page) {}

  /**
   * 清除localStorage数据
   */
  async clearLocalStorage(): Promise<void> {
    await this.page.evaluate(() => {
      localStorage.clear()
    })
  }

  /**
   * 设置localStorage数据
   */
  async setLocalStorageData(data: Record<string, string>): Promise<void> {
    await this.page.evaluate((storageData) => {
      for (const [key, value] of Object.entries(storageData)) {
        localStorage.setItem(key, value)
      }
    }, data)
  }

  /**
   * 获取localStorage数据
   */
  async getLocalStorageData(key: string): Promise<string | null> {
    return await this.page.evaluate((storageKey) => {
      return localStorage.getItem(storageKey)
    }, key)
  }

  /**
   * 清除sessionStorage数据
   */
  async clearSessionStorage(): Promise<void> {
    await this.page.evaluate(() => {
      sessionStorage.clear()
    })
  }

  /**
   * 模拟过期的认证token
   */
  async setExpiredToken(): Promise<void> {
    await this.setLocalStorageData({
      token: 'expired.jwt.token',
      tokenExpiresAt: '0',
      refreshToken: 'expired.refresh.token'
    })
  }

  /**
   * 模拟有效的认证token
   */
  async setValidToken(userId: string = 'user123'): Promise<void> {
    const futureTime = Date.now() + 3600000 // 1小时后过期
    await this.setLocalStorageData({
      token: `valid.jwt.token.${userId}`,
      tokenExpiresAt: futureTime.toString(),
      refreshToken: `valid.refresh.token.${userId}`
    })
  }
}

/**
 * 网络工具
 */
export class NetworkHelper {
  constructor(private page: Page) {}

  /**
   * 拦截API请求
   */
  async interceptRequest(
    urlPattern: string,
    response: {
      status?: number
      body?: any
      contentType?: string
      delay?: number
    }
  ): Promise<void> {
    await this.page.route(urlPattern, async (route) => {
      const { status = 200, body, contentType = 'application/json', delay = 0 } = response

      if (delay > 0) {
        await new Promise(resolve => setTimeout(resolve, delay))
      }

      await route.fulfill({
        status,
        contentType,
        body: typeof body === 'string' ? body : JSON.stringify(body)
      })
    })
  }

  /**
   * 模拟网络错误
   */
  async simulateNetworkError(urlPattern: string): Promise<void> {
    await this.page.route(urlPattern, route => {
      route.abort('failed')
    })
  }

  /**
   * 模拟慢网络
   */
  async simulateSlowNetwork(urlPattern: string, delay: number = 5000): Promise<void> {
    await this.interceptRequest(urlPattern, { delay })
  }

  /**
   * 等待特定的网络请求
   */
  async waitForRequest(urlPattern: string, timeout: number = 30000): Promise<any> {
    return await this.page.waitForRequest(urlPattern, { timeout })
  }

  /**
   * 等待特定的网络响应
   */
  async waitForResponse(urlPattern: string, timeout: number = 30000): Promise<any> {
    return await this.page.waitForResponse(urlPattern, { timeout })
  }

  /**
   * 获取网络请求日志
   */
  getRequestLogs(): any[] {
    // 这需要在测试开始时启用网络日志记录
    return []
  }
}

/**
 * 等待工具
 */
export class WaitHelper {
  constructor(private page: Page) {}

  /**
   * 等待元素出现并可见
   */
  async waitForVisible(selector: string, timeout: number = 5000): Promise<void> {
    await this.page.locator(selector).waitFor({
      state: 'visible',
      timeout
    })
  }

  /**
   * 等待元素消失
   */
  async waitForHidden(selector: string, timeout: number = 5000): Promise<void> {
    await this.page.locator(selector).waitFor({
      state: 'hidden',
      timeout
    })
  }

  /**
   * 等待文本出现
   */
  async waitForText(text: string, timeout: number = 5000): Promise<void> {
    await this.page.locator(`text=${text}`).waitFor({ timeout })
  }

  /**
   * 等待URL匹配
   */
  async waitForUrl(pattern: string | RegExp, timeout: number = 5000): Promise<void> {
    await this.page.waitForURL(pattern, { timeout })
  }

  /**
   * 等待函数返回true
   */
  async waitForFunction(
    fn: () => boolean | Promise<boolean>,
    timeout: number = 5000
  ): Promise<void> {
    await this.page.waitForFunction(fn, { timeout })
  }

  /**
   * 智能等待（等待页面稳定）
   */
  async waitForStable(timeout: number = 2000): Promise<void> {
    await this.page.waitForLoadState('networkidle')
    await this.page.waitForTimeout(timeout)
  }
}

/**
 * 断言工具
 */
export class AssertionHelper {
  constructor(private page: Page) {}

  /**
   * 验证页面标题
   */
  async verifyTitle(expectedTitle: string): Promise<void> {
    const title = await this.page.title()
    if (title !== expectedTitle) {
      throw new Error(`Expected title "${expectedTitle}", but got "${title}"`)
    }
  }

  /**
   * 验证URL包含特定路径
   */
  async verifyUrlContains(path: string): Promise<void> {
    const url = this.page.url()
    if (!url.includes(path)) {
      throw new Error(`Expected URL to contain "${path}", but got "${url}"`)
    }
  }

  /**
   * 验证元素文本
   */
  async verifyElementText(selector: string, expectedText: string): Promise<void> {
    const element = this.page.locator(selector)
    const actualText = await element.textContent()
    if (actualText !== expectedText) {
      throw new Error(`Expected text "${expectedText}", but got "${actualText}"`)
    }
  }

  /**
   * 验证元素可见性
   */
  async verifyElementVisible(selector: string): Promise<void> {
    const element = this.page.locator(selector)
    const isVisible = await element.isVisible()
    if (!isVisible) {
      throw new Error(`Expected element "${selector}" to be visible`)
    }
  }

  /**
   * 验证元素不存在
   */
  async verifyElementNotPresent(selector: string): Promise<void> {
    const element = this.page.locator(selector)
    const count = await element.count()
    if (count > 0) {
      throw new Error(`Expected element "${selector}" not to be present, but found ${count}`)
    }
  }
}

/**
 * 测试工具工厂
 */
export class TestUtilsFactory {
  private sessionManager?: SessionManager
  private testDataManager?: TestDataManager
  private networkHelper?: NetworkHelper
  private waitHelper?: WaitHelper
  private assertionHelper?: AssertionHelper

  constructor(
    private page: Page,
    private browser?: Browser
  ) {}

  getSessionManager(): SessionManager {
    if (!this.sessionManager && this.browser) {
      this.sessionManager = new SessionManager(this.browser)
    }
    return this.sessionManager!
  }

  getTestDataManager(): TestDataManager {
    if (!this.testDataManager) {
      this.testDataManager = new TestDataManager(this.page)
    }
    return this.testDataManager
  }

  getNetworkHelper(): NetworkHelper {
    if (!this.networkHelper) {
      this.networkHelper = new NetworkHelper(this.page)
    }
    return this.networkHelper
  }

  getWaitHelper(): WaitHelper {
    if (!this.waitHelper) {
      this.waitHelper = new WaitHelper(this.page)
    }
    return this.waitHelper
  }

  getAssertionHelper(): AssertionHelper {
    if (!this.assertionHelper) {
      this.assertionHelper = new AssertionHelper(this.page)
    }
    return this.assertionHelper
  }
}