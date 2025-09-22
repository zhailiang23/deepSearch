import { chromium, FullConfig } from '@playwright/test'

/**
 * 性能测试全局设置
 * 在所有性能测试开始前执行的准备工作
 */
async function globalSetup(config: FullConfig) {
  console.log('🚀 开始性能测试全局设置...')

  // 等待服务启动
  await waitForServices()

  // 创建测试用户
  await createTestUsers()

  // 预热系统
  await warmupSystem()

  console.log('✅ 性能测试全局设置完成')
}

/**
 * 等待服务启动
 */
async function waitForServices() {
  const maxRetries = 30
  const retryInterval = 2000

  console.log('⏳ 等待服务启动...')

  // 等待前端服务
  await waitForService('http://localhost:3000', 'Frontend', maxRetries, retryInterval)

  // 等待后端服务
  await waitForService('http://localhost:8080/api/actuator/health', 'Backend', maxRetries, retryInterval)

  console.log('✅ 所有服务已启动')
}

/**
 * 等待单个服务
 */
async function waitForService(url: string, serviceName: string, maxRetries: number, retryInterval: number) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url)
      if (response.ok) {
        console.log(`✅ ${serviceName} 服务已就绪`)
        return
      }
    } catch (error) {
      // 服务未就绪，继续等待
    }

    console.log(`⏳ 等待 ${serviceName} 服务启动... (${i + 1}/${maxRetries})`)
    await new Promise(resolve => setTimeout(resolve, retryInterval))
  }

  throw new Error(`❌ ${serviceName} 服务启动超时`)
}

/**
 * 创建测试用户
 */
async function createTestUsers() {
  console.log('👥 创建测试用户...')

  const testUsers = [
    { username: 'loadtest1', password: 'password123', email: 'loadtest1@example.com' },
    { username: 'loadtest2', password: 'password123', email: 'loadtest2@example.com' },
    { username: 'loadtest3', password: 'password123', email: 'loadtest3@example.com' },
    { username: 'loadtest4', password: 'password123', email: 'loadtest4@example.com' },
    { username: 'loadtest5', password: 'password123', email: 'loadtest5@example.com' },
    { username: 'testuser', password: 'password123', email: 'testuser@example.com' }
  ]

  for (const user of testUsers) {
    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(user)
      })

      if (response.ok) {
        console.log(`✅ 用户 ${user.username} 创建成功`)
      } else if (response.status === 409) {
        console.log(`ℹ️  用户 ${user.username} 已存在`)
      } else {
        console.warn(`⚠️  用户 ${user.username} 创建失败: ${response.status}`)
      }
    } catch (error) {
      console.warn(`⚠️  用户 ${user.username} 创建出错:`, error)
    }
  }

  console.log('✅ 测试用户创建完成')
}

/**
 * 系统预热
 */
async function warmupSystem() {
  console.log('🔥 系统预热中...')

  const browser = await chromium.launch()
  const context = await browser.newContext()
  const page = await context.newPage()

  try {
    // 预热前端应用
    console.log('预热前端应用...')
    await page.goto('http://localhost:3000')
    await page.waitForLoadState('networkidle')

    // 预热登录流程
    console.log('预热登录流程...')
    await page.goto('http://localhost:3000/login')
    await page.fill('input[name="username"]', 'testuser')
    await page.fill('input[name="password"]', 'password123')
    await page.click('button[type="submit"]')
    await page.waitForURL('**/dashboard')

    // 预热主要页面
    const pages = ['/users', '/settings']
    for (const route of pages) {
      console.log(`预热页面: ${route}`)
      await page.goto(`http://localhost:3000${route}`)
      await page.waitForLoadState('networkidle')
      await page.waitForTimeout(1000)
    }

    // 预热API端点
    console.log('预热API端点...')
    const apiEndpoints = [
      '/api/actuator/health',
      '/api/auth/profile',
      '/api/users?page=1&size=20'
    ]

    for (const endpoint of apiEndpoints) {
      try {
        await fetch(`http://localhost:8080${endpoint}`, {
          headers: {
            'Authorization': 'Bearer fake-token' // 预热时使用假token
          }
        })
      } catch (error) {
        // 忽略预热时的错误
      }
    }

    console.log('✅ 系统预热完成')

  } catch (error) {
    console.warn('⚠️  系统预热过程中出现错误:', error)
  } finally {
    await context.close()
    await browser.close()
  }
}

export default globalSetup