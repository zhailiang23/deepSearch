import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright端到端测试配置
 *
 * 这个配置文件设置了全面的E2E测试环境，包括：
 * - 多浏览器支持 (Chrome, Firefox, Safari)
 * - 移动设备模拟
 * - 并行测试执行
 * - 截图和视频录制
 * - 重试策略
 * - 基础URL配置
 */
export default defineConfig({
  // 测试目录
  testDir: '../',

  // 测试文件匹配模式
  testMatch: ['**/*.spec.ts', '**/*.e2e.ts'],

  // 全局超时设置
  timeout: 30000,
  expect: {
    timeout: 5000
  },

  // 测试执行配置
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  // 报告配置
  reporter: [
    ['html'],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/results.xml' }],
    process.env.CI ? ['github'] : ['list']
  ],

  // 全局设置
  use: {
    // 基础URL - 支持环境变量覆盖
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3001',

    // 浏览器设置
    headless: process.env.CI ? true : false,

    // 截图配置
    screenshot: 'only-on-failure',

    // 视频录制
    video: process.env.CI ? 'retain-on-failure' : 'off',

    // 痕迹收集
    trace: process.env.CI ? 'retain-on-failure' : 'on-first-retry',

    // 操作超时
    actionTimeout: 10000,
    navigationTimeout: 30000,

    // 语言环境
    locale: 'zh-CN',

    // 忽略HTTPS错误
    ignoreHTTPSErrors: true,

    // 自动等待策略
    waitForTimeout: 5000
  },

  // 浏览器项目配置
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Chrome特定设置
        viewport: { width: 1280, height: 720 },
        launchOptions: {
          args: [
            '--disable-web-security',
            '--disable-features=TranslateUI',
            '--disable-extensions',
            '--no-sandbox'
          ]
        }
      },
    },

    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        viewport: { width: 1280, height: 720 }
      },
    },

    {
      name: 'webkit',
      use: {
        ...devices['Desktop Safari'],
        viewport: { width: 1280, height: 720 }
      },
    },

    // 移动设备测试
    {
      name: 'Mobile Chrome',
      use: {
        ...devices['Pixel 5'],
        // 移动端特定设置
        hasTouch: true,
        isMobile: true
      },
    },

    {
      name: 'Mobile Safari',
      use: {
        ...devices['iPhone 12'],
        hasTouch: true,
        isMobile: true
      },
    },

    // 平板设备测试
    {
      name: 'iPad',
      use: {
        ...devices['iPad Pro'],
        hasTouch: true
      },
    }
  ],

  // 开发服务器配置
  webServer: process.env.CI ? undefined : {
    command: 'npm run dev',
    port: 3001,
    timeout: 120000,
    reuseExistingServer: !process.env.CI,
    env: {
      NODE_ENV: 'test'
    }
  },

  // 测试输出目录
  outputDir: 'test-results/',

  // 全局设置钩子
  globalSetup: './global-setup.ts',
  globalTeardown: './global-teardown.ts'
})