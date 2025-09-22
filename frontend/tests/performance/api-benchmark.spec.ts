import { test, expect, Page, APIRequestContext } from '@playwright/test'

/**
 * API基准性能测试
 * 测试各个API端点的响应时间和吞吐量
 */

interface APIBenchmarkResult {
  endpoint: string
  method: string
  avgResponseTime: number
  p95ResponseTime: number
  p99ResponseTime: number
  successRate: number
  throughput: number
  errorMessages: string[]
}

interface PerformanceThresholds {
  login: number
  profile: number
  refresh: number
  logout: number
  users: number
}

test.describe('API基准性能测试', () => {
  const PERFORMANCE_THRESHOLDS: PerformanceThresholds = {
    login: 500,      // 登录API < 500ms
    profile: 200,    // 用户信息 < 200ms
    refresh: 100,    // Token刷新 < 100ms
    logout: 200,     // 登出 < 200ms
    users: 300       // 用户列表 < 300ms
  }

  const CONCURRENCY_LEVELS = [1, 5, 10, 25, 50]
  const ITERATIONS_PER_LEVEL = 100

  let apiContext: APIRequestContext
  let authToken: string

  test.beforeAll(async ({ playwright }) => {
    // 创建API上下文
    apiContext = await playwright.request.newContext({
      baseURL: 'http://localhost:8080/api',
      extraHTTPHeaders: {
        'Content-Type': 'application/json'
      }
    })

    // 获取认证token
    const loginResponse = await apiContext.post('/auth/login', {
      data: {
        username: 'testuser',
        password: 'password123'
      }
    })

    expect(loginResponse.ok()).toBeTruthy()
    const loginData = await loginResponse.json()
    authToken = loginData.token
  })

  test.afterAll(async () => {
    await apiContext.dispose()
  })

  test('登录API性能基准测试', async () => {
    const results = await runAPIBenchmark({
      endpoint: '/auth/login',
      method: 'POST',
      data: {
        username: 'testuser',
        password: 'password123'
      },
      iterations: 200,
      concurrency: 10
    })

    console.log('登录API性能测试结果:', formatBenchmarkResult(results))

    expect(results.p95ResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.login)
    expect(results.successRate).toBeGreaterThan(0.95)
    expect(results.errorMessages).toHaveLength(0)
  })

  test('用户信息API性能基准测试', async () => {
    const results = await runAPIBenchmark({
      endpoint: '/auth/profile',
      method: 'GET',
      headers: {
        Authorization: `Bearer ${authToken}`
      },
      iterations: 200,
      concurrency: 20
    })

    console.log('用户信息API性能测试结果:', formatBenchmarkResult(results))

    expect(results.p95ResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.profile)
    expect(results.successRate).toBeGreaterThan(0.98)
  })

  test('Token刷新API性能基准测试', async () => {
    // 首先获取refresh token
    const loginResponse = await apiContext.post('/auth/login', {
      data: {
        username: 'testuser',
        password: 'password123'
      }
    })
    const loginData = await loginResponse.json()

    const results = await runAPIBenchmark({
      endpoint: '/auth/refresh',
      method: 'POST',
      data: {
        refreshToken: loginData.refreshToken
      },
      iterations: 100,
      concurrency: 5
    })

    console.log('Token刷新API性能测试结果:', formatBenchmarkResult(results))

    expect(results.p95ResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.refresh)
    expect(results.successRate).toBeGreaterThan(0.99)
  })

  test('用户列表API性能基准测试', async () => {
    const results = await runAPIBenchmark({
      endpoint: '/users',
      method: 'GET',
      headers: {
        Authorization: `Bearer ${authToken}`
      },
      params: {
        page: 1,
        size: 20
      },
      iterations: 150,
      concurrency: 15
    })

    console.log('用户列表API性能测试结果:', formatBenchmarkResult(results))

    expect(results.p95ResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.users)
    expect(results.successRate).toBeGreaterThan(0.95)
  })

  test('并发登录压力测试', async () => {
    const concurrencyResults: { level: number; result: APIBenchmarkResult }[] = []

    for (const concurrency of CONCURRENCY_LEVELS) {
      console.log(`测试并发级别: ${concurrency}`)

      const result = await runAPIBenchmark({
        endpoint: '/auth/login',
        method: 'POST',
        data: {
          username: 'testuser',
          password: 'password123'
        },
        iterations: ITERATIONS_PER_LEVEL,
        concurrency: concurrency
      })

      concurrencyResults.push({ level: concurrency, result })

      console.log(`并发级别 ${concurrency} 结果:`, formatBenchmarkResult(result))

      // 每个级别之间稍作停顿
      await new Promise(resolve => setTimeout(resolve, 1000))
    }

    // 分析并发性能趋势
    console.log('\n并发性能分析:')
    concurrencyResults.forEach(({ level, result }) => {
      console.log(`并发 ${level}: 响应时间 ${result.avgResponseTime.toFixed(2)}ms, 吞吐量 ${result.throughput.toFixed(2)} req/s, 成功率 ${(result.successRate * 100).toFixed(2)}%`)
    })

    // 验证在高并发下仍能保持合理性能
    const highConcurrencyResult = concurrencyResults[concurrencyResults.length - 1].result
    expect(highConcurrencyResult.successRate).toBeGreaterThan(0.9)
    expect(highConcurrencyResult.p95ResponseTime).toBeLessThan(1000) // 高并发下允许更长响应时间
  })

  test('API错误处理性能测试', async () => {
    // 测试无效凭证的错误处理性能
    const invalidLoginResult = await runAPIBenchmark({
      endpoint: '/auth/login',
      method: 'POST',
      data: {
        username: 'invalid',
        password: 'invalid'
      },
      iterations: 100,
      concurrency: 10,
      expectFailure: true
    })

    console.log('无效登录API性能测试结果:', formatBenchmarkResult(invalidLoginResult))

    // 错误响应也应该快速返回
    expect(invalidLoginResult.p95ResponseTime).toBeLessThan(300)

    // 测试无效token的错误处理性能
    const invalidTokenResult = await runAPIBenchmark({
      endpoint: '/auth/profile',
      method: 'GET',
      headers: {
        Authorization: 'Bearer invalid_token'
      },
      iterations: 100,
      concurrency: 10,
      expectFailure: true
    })

    console.log('无效Token API性能测试结果:', formatBenchmarkResult(invalidTokenResult))

    expect(invalidTokenResult.p95ResponseTime).toBeLessThan(200)
  })

  test('长时间稳定性测试', async () => {
    const testDuration = 2 * 60 * 1000 // 2分钟
    const requestInterval = 100 // 每100ms一个请求
    const totalRequests = testDuration / requestInterval

    const results: number[] = []
    const errors: string[] = []
    const startTime = Date.now()

    console.log(`开始长时间稳定性测试 (${testDuration / 1000}秒, ${totalRequests}个请求)`)

    let requestCount = 0
    const intervalId = setInterval(async () => {
      const requestStart = Date.now()

      try {
        const response = await apiContext.get('/auth/profile', {
          headers: {
            Authorization: `Bearer ${authToken}`
          }
        })

        const responseTime = Date.now() - requestStart
        results.push(responseTime)

        if (!response.ok()) {
          errors.push(`Request ${requestCount}: HTTP ${response.status()}`)
        }

      } catch (error) {
        errors.push(`Request ${requestCount}: ${error}`)
      }

      requestCount++

      if (Date.now() - startTime >= testDuration) {
        clearInterval(intervalId)
      }
    }, requestInterval)

    // 等待测试完成
    await new Promise(resolve => {
      const checkCompletion = () => {
        if (Date.now() - startTime >= testDuration) {
          resolve(undefined)
        } else {
          setTimeout(checkCompletion, 1000)
        }
      }
      checkCompletion()
    })

    // 分析稳定性结果
    const avgResponseTime = results.reduce((sum, time) => sum + time, 0) / results.length
    const sortedResults = results.sort((a, b) => a - b)
    const p95 = sortedResults[Math.floor(results.length * 0.95)]
    const successRate = (results.length - errors.length) / results.length

    console.log(`长时间稳定性测试结果:`)
    console.log(`- 总请求数: ${requestCount}`)
    console.log(`- 成功请求: ${results.length}`)
    console.log(`- 错误数: ${errors.length}`)
    console.log(`- 成功率: ${(successRate * 100).toFixed(2)}%`)
    console.log(`- 平均响应时间: ${avgResponseTime.toFixed(2)}ms`)
    console.log(`- 95th百分位: ${p95.toFixed(2)}ms`)

    if (errors.length > 0) {
      console.log(`错误详情:`, errors.slice(0, 10)) // 只显示前10个错误
    }

    expect(successRate).toBeGreaterThan(0.95)
    expect(p95).toBeLessThan(500)
  })

  /**
   * 运行API基准测试
   */
  async function runAPIBenchmark(options: {
    endpoint: string
    method: string
    data?: any
    headers?: Record<string, string>
    params?: Record<string, any>
    iterations: number
    concurrency: number
    expectFailure?: boolean
  }): Promise<APIBenchmarkResult> {
    const {
      endpoint,
      method,
      data,
      headers = {},
      params,
      iterations,
      concurrency,
      expectFailure = false
    } = options

    const results: number[] = []
    const errors: string[] = []
    const batches = Math.ceil(iterations / concurrency)

    const startTime = Date.now()

    for (let batch = 0; batch < batches; batch++) {
      const batchPromises: Promise<void>[] = []
      const batchSize = Math.min(concurrency, iterations - batch * concurrency)

      for (let i = 0; i < batchSize; i++) {
        batchPromises.push(
          (async () => {
            const requestStart = Date.now()

            try {
              let response
              const requestOptions: any = { headers }

              if (data) requestOptions.data = data
              if (params) requestOptions.params = params

              switch (method.toUpperCase()) {
                case 'GET':
                  response = await apiContext.get(endpoint, requestOptions)
                  break
                case 'POST':
                  response = await apiContext.post(endpoint, requestOptions)
                  break
                case 'PUT':
                  response = await apiContext.put(endpoint, requestOptions)
                  break
                case 'DELETE':
                  response = await apiContext.delete(endpoint, requestOptions)
                  break
                default:
                  throw new Error(`Unsupported method: ${method}`)
              }

              const responseTime = Date.now() - requestStart
              results.push(responseTime)

              if (!expectFailure && !response.ok()) {
                errors.push(`HTTP ${response.status()}: ${await response.text()}`)
              } else if (expectFailure && response.ok()) {
                errors.push(`Expected failure but got success: ${response.status()}`)
              }

            } catch (error) {
              const responseTime = Date.now() - requestStart
              if (!expectFailure) {
                results.push(responseTime)
                errors.push(error instanceof Error ? error.message : String(error))
              } else {
                results.push(responseTime)
              }
            }
          })()
        )
      }

      await Promise.all(batchPromises)

      // 批次间稍作停顿，避免过度压力
      if (batch < batches - 1) {
        await new Promise(resolve => setTimeout(resolve, 10))
      }
    }

    const totalTime = Date.now() - startTime
    const successCount = results.length - errors.length
    const successRate = successCount / results.length

    // 计算统计数据
    const sortedResults = results.sort((a, b) => a - b)
    const avgResponseTime = results.reduce((sum, time) => sum + time, 0) / results.length
    const p95ResponseTime = sortedResults[Math.floor(results.length * 0.95)] || 0
    const p99ResponseTime = sortedResults[Math.floor(results.length * 0.99)] || 0
    const throughput = (results.length / totalTime) * 1000 // requests per second

    return {
      endpoint,
      method,
      avgResponseTime,
      p95ResponseTime,
      p99ResponseTime,
      successRate,
      throughput,
      errorMessages: errors
    }
  }

  /**
   * 格式化基准测试结果
   */
  function formatBenchmarkResult(result: APIBenchmarkResult): string {
    return [
      `端点: ${result.method} ${result.endpoint}`,
      `平均响应时间: ${result.avgResponseTime.toFixed(2)}ms`,
      `95th百分位: ${result.p95ResponseTime.toFixed(2)}ms`,
      `99th百分位: ${result.p99ResponseTime.toFixed(2)}ms`,
      `成功率: ${(result.successRate * 100).toFixed(2)}%`,
      `吞吐量: ${result.throughput.toFixed(2)} req/s`,
      `错误数: ${result.errorMessages.length}`
    ].join('\n')
  }
})