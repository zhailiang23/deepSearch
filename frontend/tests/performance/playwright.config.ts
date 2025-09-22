import { defineConfig, devices } from '@playwright/test'

/**
 * 性能测试专用的Playwright配置
 * 针对性能测试场景进行优化配置
 */
export default defineConfig({
  testDir: '.',
  fullyParallel: false, // 性能测试避免并行，确保资源独占
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1, // 单线程执行，避免资源竞争
  timeout: 5 * 60 * 1000, // 5分钟超时，性能测试需要更长时间

  reporter: [
    ['html', { outputFolder: 'performance-reports/html' }],
    ['json', { outputFile: 'performance-reports/results.json' }],
    ['line'],
    ['list', { printSteps: true }]
  ],

  outputDir: 'performance-reports/artifacts',

  use: {
    baseURL: 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',

    // 性能测试特定配置
    launchOptions: {
      args: [
        '--enable-precise-memory-info', // 启用精确内存信息
        '--enable-memory-info',
        '--js-flags=--expose-gc', // 暴露垃圾回收接口
        '--disable-background-timer-throttling',
        '--disable-renderer-backgrounding',
        '--disable-backgrounding-occluded-windows',
        '--disable-features=TranslateUI',
        '--disable-ipc-flooding-protection'
      ]
    }
  },

  projects: [
    {
      name: 'performance-chrome',
      use: {
        ...devices['Desktop Chrome'],
        // 性能测试使用固定的视口大小
        viewport: { width: 1920, height: 1080 }
      },
    },
    {
      name: 'performance-chrome-mobile',
      use: {
        ...devices['iPhone 13'],
        // 移动端性能测试
      },
    },
    {
      name: 'performance-memory-pressure',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1920, height: 1080 },
        launchOptions: {
          args: [
            '--memory-pressure-off',
            '--max_old_space_size=512', // 限制内存，模拟内存压力
            '--enable-precise-memory-info',
            '--js-flags=--expose-gc'
          ]
        }
      },
    }
  ],

  // webServer 配置注释掉，手动启动服务进行测试

  expect: {
    // 性能测试的断言超时
    timeout: 30 * 1000,
  },

  // 全局设置暂时禁用
})