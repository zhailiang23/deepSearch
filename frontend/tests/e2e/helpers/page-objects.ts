import { Page, Locator, expect } from '@playwright/test'

/**
 * 页面对象模型 (Page Object Model)
 *
 * 为每个主要页面提供标准化的交互方法
 */

/**
 * 基础页面类
 */
export class BasePage {
  constructor(protected page: Page) {}

  async navigate(path: string): Promise<void> {
    await this.page.goto(path)
    await this.page.waitForLoadState('networkidle')
  }

  async getTitle(): Promise<string> {
    return await this.page.title()
  }

  async waitForElement(selector: string, timeout = 5000): Promise<Locator> {
    const element = this.page.locator(selector)
    await element.waitFor({ timeout })
    return element
  }

  async takeScreenshot(name: string): Promise<void> {
    await this.page.screenshot({
      path: `test-results/screenshots/${name}.png`,
      fullPage: true
    })
  }
}

/**
 * 登录页面对象
 */
export class LoginPage extends BasePage {
  // 选择器
  private readonly usernameInput = '#username'
  private readonly passwordInput = '#password'
  private readonly rememberMeCheckbox = '#remember-me'
  private readonly submitButton = 'button[type="submit"]'
  private readonly errorAlert = '[role="alert"]'
  private readonly forgotPasswordLink = 'text=忘记密码'
  private readonly registerLink = 'text=注册'
  private readonly passwordToggle = 'button[aria-label*="显示密码"], button[aria-label*="隐藏密码"]'

  async navigate(): Promise<void> {
    await super.navigate('/login')
    await expect(this.page.locator('#login-form')).toBeVisible()
  }

  async fillUsername(username: string): Promise<void> {
    await this.page.fill(this.usernameInput, username)
  }

  async fillPassword(password: string): Promise<void> {
    await this.page.fill(this.passwordInput, password)
  }

  async toggleRememberMe(): Promise<void> {
    await this.page.click(this.rememberMeCheckbox)
  }

  async submitForm(): Promise<void> {
    await this.page.click(this.submitButton)
  }

  async getErrorMessage(): Promise<string> {
    const errorElement = this.page.locator(this.errorAlert)
    await errorElement.waitFor()
    return await errorElement.textContent() || ''
  }

  async isSubmitButtonDisabled(): Promise<boolean> {
    return await this.page.locator(this.submitButton).isDisabled()
  }

  async clickForgotPassword(): Promise<void> {
    await this.page.click(this.forgotPasswordLink)
  }

  async clickRegister(): Promise<void> {
    await this.page.click(this.registerLink)
  }

  async togglePasswordVisibility(): Promise<void> {
    await this.page.click(this.passwordToggle)
  }

  async getPasswordInputType(): Promise<string> {
    return await this.page.getAttribute(this.passwordInput, 'type') || ''
  }
}

/**
 * 仪表板页面对象
 */
export class DashboardPage extends BasePage {
  private readonly userMenu = '[data-testid="user-menu"]'
  private readonly logoutButton = 'text=退出'
  private readonly welcomeMessage = '[data-testid="welcome-message"]'
  private readonly navigationMenu = '[data-testid="navigation"]'

  async navigate(): Promise<void> {
    await super.navigate('/dashboard')
    await this.waitForPageLoad()
  }

  async waitForPageLoad(): Promise<void> {
    // 等待关键元素加载
    await Promise.race([
      this.page.locator(this.userMenu).waitFor(),
      this.page.locator(this.navigationMenu).waitFor(),
      this.page.locator(this.welcomeMessage).waitFor()
    ])
  }

  async logout(): Promise<void> {
    // 尝试通过用户菜单登出
    try {
      if (await this.page.locator(this.userMenu).isVisible()) {
        await this.page.click(this.userMenu)
        await this.page.click(this.logoutButton)
      }
    } catch {
      // 直接点击登出按钮
      await this.page.click(this.logoutButton)
    }
  }

  async getWelcomeMessage(): Promise<string> {
    const element = this.page.locator(this.welcomeMessage)
    return await element.textContent() || ''
  }

  async isUserMenuVisible(): Promise<boolean> {
    return await this.page.locator(this.userMenu).isVisible()
  }
}

/**
 * 用户管理页面对象
 */
export class UserManagementPage extends BasePage {
  private readonly userTable = '[data-testid="user-table"]'
  private readonly addUserButton = '[data-testid="add-user-button"]'
  private readonly searchInput = '[data-testid="user-search"]'
  private readonly deleteUserButton = '[data-testid="delete-user"]'
  private readonly editUserButton = '[data-testid="edit-user"]'

  async navigate(): Promise<void> {
    await super.navigate('/users')
    await this.waitForPageLoad()
  }

  async waitForPageLoad(): Promise<void> {
    await this.page.locator(this.userTable).waitFor()
  }

  async clickAddUser(): Promise<void> {
    await this.page.click(this.addUserButton)
  }

  async searchUser(searchTerm: string): Promise<void> {
    await this.page.fill(this.searchInput, searchTerm)
    await this.page.keyboard.press('Enter')
  }

  async getUserCount(): Promise<number> {
    const rows = this.page.locator(`${this.userTable} tbody tr`)
    return await rows.count()
  }

  async deleteUser(index: number): Promise<void> {
    const deleteButtons = this.page.locator(this.deleteUserButton)
    await deleteButtons.nth(index).click()

    // 确认删除对话框
    await this.page.click('text=确认')
  }

  async editUser(index: number): Promise<void> {
    const editButtons = this.page.locator(this.editUserButton)
    await editButtons.nth(index).click()
  }
}

/**
 * 通用组件对象
 */
export class CommonComponents {
  constructor(private page: Page) {}

  /**
   * 通知组件
   */
  async waitForNotification(type: 'success' | 'error' | 'warning' | 'info'): Promise<string> {
    const notification = this.page.locator(`[data-testid="notification"][data-type="${type}"]`)
    await notification.waitFor()
    return await notification.textContent() || ''
  }

  /**
   * 确认对话框
   */
  async confirmDialog(confirm = true): Promise<void> {
    const dialog = this.page.locator('[data-testid="confirm-dialog"]')
    await dialog.waitFor()

    if (confirm) {
      await this.page.click('[data-testid="confirm-button"]')
    } else {
      await this.page.click('[data-testid="cancel-button"]')
    }
  }

  /**
   * 加载指示器
   */
  async waitForLoading(): Promise<void> {
    const loading = this.page.locator('[data-testid="loading"]')
    await loading.waitFor({ state: 'hidden' })
  }

  /**
   * 导航菜单
   */
  async navigateToSection(section: string): Promise<void> {
    const menuItem = this.page.locator(`[data-testid="nav-${section}"]`)
    await menuItem.click()
  }

  /**
   * 语言选择器
   */
  async changeLanguage(language: 'zh-CN' | 'en-US'): Promise<void> {
    const languageSelector = this.page.locator('[data-testid="language-selector"]')
    await languageSelector.click()

    const languageOption = this.page.locator(`[data-value="${language}"]`)
    await languageOption.click()
  }

  /**
   * 主题切换器
   */
  async toggleTheme(): Promise<void> {
    const themeToggle = this.page.locator('[data-testid="theme-toggle"]')
    await themeToggle.click()
  }
}

/**
 * 页面工厂类
 */
export class PageFactory {
  constructor(private page: Page) {}

  getLoginPage(): LoginPage {
    return new LoginPage(this.page)
  }

  getDashboardPage(): DashboardPage {
    return new DashboardPage(this.page)
  }

  getUserManagementPage(): UserManagementPage {
    return new UserManagementPage(this.page)
  }

  getCommonComponents(): CommonComponents {
    return new CommonComponents(this.page)
  }
}