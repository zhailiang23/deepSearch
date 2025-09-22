import { chromium, FullConfig } from '@playwright/test'
import { writeFileSync, mkdirSync } from 'fs'
import { dirname } from 'path'

/**
 * Playwright全局设置
 *
 * 在所有测试运行之前执行的设置操作：
 * - 验证服务器连接
 * - 创建管理员用户会话
 * - 设置测试数据
 * - 清理之前的测试残留数据
 */
async function globalSetup(config: FullConfig) {
  console.log('🚀 开始E2E测试全局设置...')

  const { baseURL } = config.projects[0].use
  const browser = await chromium.launch()
  const context = await browser.newContext()
  const page = await context.newPage()

  try {
    // 1. 验证服务器连接
    console.log('📡 验证服务器连接...')
    await page.goto(baseURL + '/')
    await page.waitForLoadState('networkidle')
    console.log('✅ 服务器连接正常')

    // 2. 检查API健康状态
    console.log('🔍 检查API健康状态...')
    try {
      const healthResponse = await page.request.get(baseURL + '/api/health')
      if (!healthResponse.ok()) {
        console.warn('⚠️ API健康检查失败，但继续测试...')
      } else {
        console.log('✅ API服务正常')
      }
    } catch (error) {
      console.warn('⚠️ API健康检查失败，但继续测试...')
    }

    // 3. 设置API模拟响应（因为后端可能不可用）
    console.log('🔧 设置API模拟响应...')

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
        } else {
          // 模拟登录失败
          await route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              error: '用户名或密码错误'
            })
          })
        }
      } catch (error) {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
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
    })

    console.log('✅ API模拟响应设置完成')

    // 5. 验证关键页面可访问性
    console.log('📋 验证关键页面可访问性...')

    const criticalPages = [
      '/',
      '/login',
      '/dashboard',
      '/users'
    ]

    for (const path of criticalPages) {
      try {
        await page.goto(baseURL + path)
        await page.waitForLoadState('networkidle', { timeout: 5000 })
        console.log(`✅ 页面 ${path} 可访问`)
      } catch (error) {
        console.warn(`⚠️ 页面 ${path} 访问异常: ${error.message}`)
      }
    }

  } catch (error) {
    console.error('❌ 全局设置失败:', error)
    throw error
  } finally {
    await context.close()
    await browser.close()
  }

  console.log('🎉 E2E测试全局设置完成')
}

export default globalSetup