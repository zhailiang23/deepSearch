import { readFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

// ES模块中获取 __dirname 等价物
const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

/**
 * 测试数据加载器
 *
 * 提供标准化的测试数据访问接口
 */

/**
 * 用户数据接口
 */
export interface TestUser {
  username: string
  password: string
  email: string
  role: string
  status: string
  firstName: string
  lastName: string
  description: string
  permissions: string[]
  lastLogin: string | null
  createdAt: string
  mustChangePassword: boolean
  accountLocked: boolean
  loginAttempts: number
  lockedAt?: string
  lockReason?: string
  passwordExpiresAt?: string
  isFirstLogin?: boolean
  sessionTimeout?: number
}

/**
 * 登录场景接口
 */
export interface LoginScenario {
  username: string
  password: string
  rememberMe: boolean
  expectedRedirect?: string
  expectedError?: string
  description: string
}

/**
 * 测试数据配置接口
 */
export interface TestDataConfig {
  bruteForceAttempts: number
  lockoutDuration: number
  passwordMinLength: number
  passwordMaxLength: number
  sessionTimeout: number
  tokenRefreshThreshold: number
  maxConcurrentSessions: number
}

/**
 * API端点配置接口
 */
export interface ApiEndpoints {
  login: string
  logout: string
  refresh: string
  profile: string
  changePassword: string
  resetPassword: string
  unlock: string
}

/**
 * 完整的测试数据结构
 */
export interface TestDataSet {
  description: string
  version: string
  lastUpdated: string
  users: Record<string, TestUser>
  loginScenarios: Record<string, LoginScenario>
  testData: TestDataConfig
  apiEndpoints: ApiEndpoints
}

/**
 * 测试数据加载器类
 */
export class TestDataLoader {
  private static instance: TestDataLoader
  private testData: TestDataSet | null = null

  private constructor() {}

  /**
   * 获取单例实例
   */
  static getInstance(): TestDataLoader {
    if (!TestDataLoader.instance) {
      TestDataLoader.instance = new TestDataLoader()
    }
    return TestDataLoader.instance
  }

  /**
   * 加载测试数据
   */
  private loadTestData(): TestDataSet {
    if (!this.testData) {
      const dataPath = join(__dirname, 'test-users.json')
      const rawData = readFileSync(dataPath, 'utf-8')
      this.testData = JSON.parse(rawData) as TestDataSet
    }
    return this.testData
  }

  /**
   * 获取所有用户数据
   */
  getAllUsers(): Record<string, TestUser> {
    return this.loadTestData().users
  }

  /**
   * 根据用户名获取用户数据
   */
  getUser(username: string): TestUser | null {
    const users = this.getAllUsers()
    return users[username] || null
  }

  /**
   * 根据角色获取用户列表
   */
  getUsersByRole(role: string): TestUser[] {
    const users = this.getAllUsers()
    return Object.values(users).filter(user => user.role === role)
  }

  /**
   * 根据状态获取用户列表
   */
  getUsersByStatus(status: string): TestUser[] {
    const users = this.getAllUsers()
    return Object.values(users).filter(user => user.status === status)
  }

  /**
   * 获取活跃用户列表
   */
  getActiveUsers(): TestUser[] {
    return this.getUsersByStatus('ACTIVE')
  }

  /**
   * 获取管理员用户
   */
  getAdminUser(): TestUser {
    const admin = this.getUser('admin')
    if (!admin) {
      throw new Error('Admin user not found in test data')
    }
    return admin
  }

  /**
   * 获取标准用户
   */
  getStandardUser(): TestUser {
    const user = this.getUser('user')
    if (!user) {
      throw new Error('Standard user not found in test data')
    }
    return user
  }

  /**
   * 获取被锁定的用户
   */
  getLockedUser(): TestUser {
    const user = this.getUser('locked_user')
    if (!user) {
      throw new Error('Locked user not found in test data')
    }
    return user
  }

  /**
   * 获取所有登录场景
   */
  getAllLoginScenarios(): Record<string, LoginScenario> {
    return this.loadTestData().loginScenarios
  }

  /**
   * 获取特定登录场景
   */
  getLoginScenario(scenarioName: string): LoginScenario | null {
    const scenarios = this.getAllLoginScenarios()
    return scenarios[scenarioName] || null
  }

  /**
   * 获取有效登录场景
   */
  getValidLoginScenarios(): LoginScenario[] {
    const scenarios = this.getAllLoginScenarios()
    return Object.values(scenarios).filter(scenario => !scenario.expectedError)
  }

  /**
   * 获取无效登录场景
   */
  getInvalidLoginScenarios(): LoginScenario[] {
    const scenarios = this.getAllLoginScenarios()
    return Object.values(scenarios).filter(scenario => scenario.expectedError)
  }

  /**
   * 获取测试配置
   */
  getTestConfig(): TestDataConfig {
    return this.loadTestData().testData
  }

  /**
   * 获取API端点配置
   */
  getApiEndpoints(): ApiEndpoints {
    return this.loadTestData().apiEndpoints
  }

  /**
   * 生成随机用户数据
   */
  generateRandomUser(overrides: Partial<TestUser> = {}): TestUser {
    const timestamp = Date.now()
    const defaultUser: TestUser = {
      username: `test_user_${timestamp}`,
      password: 'test123456',
      email: `test_${timestamp}@example.com`,
      role: 'USER',
      status: 'ACTIVE',
      firstName: '测试',
      lastName: `用户${timestamp}`,
      description: `随机生成的测试用户 ${timestamp}`,
      permissions: ['read:profile', 'update:profile'],
      lastLogin: null,
      createdAt: new Date().toISOString(),
      mustChangePassword: false,
      accountLocked: false,
      loginAttempts: 0
    }

    return { ...defaultUser, ...overrides }
  }

  /**
   * 生成无效凭据组合
   */
  generateInvalidCredentials(): Array<{ username: string; password: string; description: string }> {
    return [
      { username: '', password: '', description: '空用户名和密码' },
      { username: 'valid', password: '', description: '空密码' },
      { username: '', password: 'valid', description: '空用户名' },
      { username: 'nonexistent', password: 'password', description: '不存在的用户名' },
      { username: 'user', password: 'wrongpassword', description: '错误密码' },
      { username: 'user', password: '123', description: '过短密码' },
      { username: 'u', password: 'password', description: '过短用户名' },
      { username: 'a'.repeat(100), password: 'password', description: '过长用户名' },
      { username: 'user', password: 'a'.repeat(200), description: '过长密码' },
      { username: 'user@domain.com', password: 'password', description: '邮箱格式用户名' },
      { username: 'user', password: '中文密码', description: '包含特殊字符的密码' }
    ]
  }

  /**
   * 获取性能测试用户
   */
  getPerformanceTestUsers(count: number = 10): TestUser[] {
    const users: TestUser[] = []
    for (let i = 1; i <= count; i++) {
      users.push(this.generateRandomUser({
        username: `perf_user_${i}`,
        password: 'perf123456',
        email: `perf_user_${i}@example.com`,
        description: `性能测试用户 ${i}`
      }))
    }
    return users
  }

  /**
   * 验证测试数据完整性
   */
  validateTestData(): { isValid: boolean; errors: string[] } {
    const errors: string[] = []
    const data = this.loadTestData()

    try {
      // 验证必需的用户存在
      const requiredUsers = ['admin', 'user']
      for (const username of requiredUsers) {
        if (!data.users[username]) {
          errors.push(`Required user '${username}' not found`)
        }
      }

      // 验证必需的登录场景存在
      const requiredScenarios = ['validLogin', 'invalidPassword', 'invalidUsername']
      for (const scenario of requiredScenarios) {
        if (!data.loginScenarios[scenario]) {
          errors.push(`Required login scenario '${scenario}' not found`)
        }
      }

      // 验证API端点配置
      const requiredEndpoints = ['login', 'logout', 'refresh', 'profile']
      for (const endpoint of requiredEndpoints) {
        if (!data.apiEndpoints[endpoint as keyof ApiEndpoints]) {
          errors.push(`Required API endpoint '${endpoint}' not found`)
        }
      }

      // 验证用户数据格式
      for (const [username, user] of Object.entries(data.users)) {
        if (!user.username || !user.password || !user.email) {
          errors.push(`User '${username}' missing required fields`)
        }

        if (!['ADMIN', 'MANAGER', 'USER', 'GUEST'].includes(user.role)) {
          errors.push(`User '${username}' has invalid role: ${user.role}`)
        }

        if (!['ACTIVE', 'INACTIVE', 'LOCKED'].includes(user.status)) {
          errors.push(`User '${username}' has invalid status: ${user.status}`)
        }
      }

    } catch (error) {
      errors.push(`Data validation failed: ${error.message}`)
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }

  /**
   * 重置测试数据（清除缓存）
   */
  reset(): void {
    this.testData = null
  }
}

// 导出单例实例
export const testDataLoader = TestDataLoader.getInstance()

// 导出便捷方法
export const getUser = (username: string) => testDataLoader.getUser(username)
export const getLoginScenario = (scenarioName: string) => testDataLoader.getLoginScenario(scenarioName)
export const getTestConfig = () => testDataLoader.getTestConfig()
export const getApiEndpoints = () => testDataLoader.getApiEndpoints()