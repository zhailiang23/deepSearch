import { chromium, FullConfig } from '@playwright/test'
import { existsSync, unlinkSync, readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

/**
 * Playwright全局清理
 *
 * 在所有测试运行完成之后执行的清理操作：
 * - 清理测试数据
 * - 注销所有测试用户会话
 * - 清理临时文件
 * - 生成测试报告摘要
 */
async function globalTeardown(config: FullConfig) {
  console.log('🧹 开始E2E测试全局清理...')

  const { baseURL } = config.projects[0].use
  const browser = await chromium.launch()

  try {
    // 1. 清理存储状态文件
    console.log('📁 清理存储状态文件...')

    const stateFiles = [
      'test-results/admin-storage-state.json',
      'test-results/user-storage-state.json'
    ]

    for (const file of stateFiles) {
      if (existsSync(file)) {
        unlinkSync(file)
        console.log(`🗑️ 已删除: ${file}`)
      }
    }

    // 2. 注销所有会话（通过API调用）
    console.log('🔐 注销所有测试会话...')
    const context = await browser.newContext()
    const page = await context.newPage()

    try {
      // 尝试调用登出API
      await page.request.post(baseURL + '/api/auth/logout-all-sessions')
      console.log('✅ 所有会话已注销')
    } catch (error) {
      console.warn('⚠️ 批量注销会话失败，可能API不支持:', error.message)
    }

    await context.close()

    // 3. 生成测试报告摘要
    console.log('📊 生成测试报告摘要...')

    // 读取测试结果
    const resultsPath = 'test-results/results.json'
    if (existsSync(resultsPath)) {
      try {
        const results = JSON.parse(readFileSync(resultsPath, 'utf8'))

        const summary = {
          totalTests: results.suites?.reduce((acc: number, suite: any) => {
            return acc + (suite.specs?.length || 0)
          }, 0) || 0,
          passed: 0,
          failed: 0,
          skipped: 0,
          duration: results.stats?.duration || 0,
          timestamp: new Date().toISOString()
        }

        // 计算通过/失败的测试数
        results.suites?.forEach((suite: any) => {
          suite.specs?.forEach((spec: any) => {
            spec.tests?.forEach((test: any) => {
              if (test.outcome === 'expected') {
                summary.passed++
              } else if (test.outcome === 'unexpected') {
                summary.failed++
              } else {
                summary.skipped++
              }
            })
          })
        })

        console.log('📈 测试结果摘要:')
        console.log(`   总测试数: ${summary.totalTests}`)
        console.log(`   通过: ${summary.passed}`)
        console.log(`   失败: ${summary.failed}`)
        console.log(`   跳过: ${summary.skipped}`)
        console.log(`   耗时: ${Math.round(summary.duration / 1000)}秒`)

        // 保存摘要报告
        writeFileSync(
          'test-results/summary.json',
          JSON.stringify(summary, null, 2)
        )

      } catch (error) {
        console.warn('⚠️ 生成测试摘要失败:', error.message)
      }
    }

    // 4. 清理临时文件
    console.log('🗂️ 清理临时文件...')

    // 清理旧的截图和视频（保留最近的）
    const cleanupDirs = ['test-results/screenshots', 'test-results/videos']

    for (const dir of cleanupDirs) {
      if (existsSync(dir)) {
        const files = readdirSync(dir)
        const now = Date.now()

        files.forEach((file: string) => {
          const filePath = join(dir, file)
          const stats = statSync(filePath)
          const ageInDays = (now - stats.mtime.getTime()) / (1000 * 60 * 60 * 24)

          // 删除7天前的文件
          if (ageInDays > 7) {
            unlinkSync(filePath)
            console.log(`🗑️ 已删除旧文件: ${filePath}`)
          }
        })
      }
    }

  } catch (error) {
    console.error('❌ 全局清理失败:', error)
    // 不抛出错误，避免影响测试结果
  } finally {
    await browser.close()
  }

  console.log('✨ E2E测试全局清理完成')
}

export default globalTeardown