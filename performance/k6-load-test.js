import http from 'k6/http'
import { check, group, sleep } from 'k6'
import { Rate, Counter, Trend } from 'k6/metrics'

/**
 * K6负载测试脚本
 * 测试认证系统的负载能力和性能表现
 */

// 自定义指标
const loginSuccessRate = new Rate('login_success_rate')
const loginFailureRate = new Rate('login_failure_rate')
const apiErrors = new Counter('api_errors')
const authLatency = new Trend('auth_latency')

// 测试配置
export const options = {
  scenarios: {
    // 渐进式负载测试
    ramp_up_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },   // 2分钟内增长到50用户
        { duration: '5m', target: 100 },  // 5分钟内增长到100用户
        { duration: '5m', target: 200 },  // 5分钟内增长到200用户
        { duration: '10m', target: 200 }, // 保持200用户10分钟
        { duration: '3m', target: 0 },    // 3分钟内降到0
      ],
    },

    // 峰值压力测试
    spike_test: {
      executor: 'ramping-vus',
      startTime: '25m',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 500 },  // 1分钟激增到500用户
        { duration: '2m', target: 500 },  // 保持500用户2分钟
        { duration: '1m', target: 0 },    // 1分钟降到0
      ],
    },

    // 稳定性测试
    stability_test: {
      executor: 'constant-vus',
      startTime: '30m',
      vus: 100,
      duration: '30m',
    },

    // 基准测试
    baseline_test: {
      executor: 'constant-vus',
      startTime: '65m',
      vus: 10,
      duration: '5m',
    }
  },

  thresholds: {
    // HTTP请求失败率
    http_req_failed: ['rate<0.05'], // 失败率 < 5%

    // 响应时间
    http_req_duration: [
      'p(50)<500',   // 50% 请求 < 500ms
      'p(95)<1000',  // 95% 请求 < 1s
      'p(99)<2000',  // 99% 请求 < 2s
    ],

    // 自定义指标
    login_success_rate: ['rate>0.95'], // 登录成功率 > 95%
    auth_latency: ['p(95)<500'],       // 认证延迟 < 500ms
    api_errors: ['count<100'],         // API错误 < 100次
  },

  // 环境配置
  ext: {
    loadimpact: {
      projectID: 3614829,
      name: 'deepSearch认证系统负载测试'
    }
  }
}

// 基础URL配置
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const FRONTEND_URL = __ENV.FRONTEND_URL || 'http://localhost:3000'

// 测试用户数据
const testUsers = [
  { username: 'loadtest1', password: 'password123' },
  { username: 'loadtest2', password: 'password123' },
  { username: 'loadtest3', password: 'password123' },
  { username: 'loadtest4', password: 'password123' },
  { username: 'loadtest5', password: 'password123' },
]

/**
 * 设置阶段
 */
export function setup() {
  console.log('开始K6负载测试设置...')

  // 健康检查
  const healthCheck = http.get(`${BASE_URL}/api/actuator/health`)
  check(healthCheck, {
    '后端服务健康': (r) => r.status === 200,
  })

  const frontendCheck = http.get(FRONTEND_URL)
  check(frontendCheck, {
    '前端服务可用': (r) => r.status === 200,
  })

  console.log('K6负载测试设置完成')
  return { baseUrl: BASE_URL, frontendUrl: FRONTEND_URL }
}

/**
 * 主测试函数
 */
export default function(data) {
  const user = testUsers[Math.floor(Math.random() * testUsers.length)]

  group('完整用户会话', function() {
    let authToken = null

    // 1. 用户登录
    group('用户登录', function() {
      authToken = performLogin(user)
    })

    if (authToken) {
      // 2. 获取用户信息
      group('获取用户信息', function() {
        getUserProfile(authToken)
      })

      // 3. 访问受保护资源
      group('访问受保护资源', function() {
        accessProtectedResources(authToken)
      })

      // 4. Token刷新
      if (Math.random() < 0.3) { // 30%概率刷新token
        group('Token刷新', function() {
          refreshToken(authToken)
        })
      }

      // 5. 用户登出
      group('用户登出', function() {
        performLogout(authToken)
      })
    }
  })

  // 模拟用户思考时间
  sleep(Math.random() * 3 + 1) // 1-4秒随机等待
}

/**
 * 执行登录
 */
function performLogin(user) {
  const startTime = Date.now()

  const payload = JSON.stringify({
    username: user.username,
    password: user.password
  })

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: { name: 'login' },
  }

  const response = http.post(`${BASE_URL}/api/auth/login`, payload, params)

  const duration = Date.now() - startTime
  authLatency.add(duration)

  const success = check(response, {
    '登录状态码正确': (r) => r.status === 200,
    '返回token': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.token && body.token.length > 0
      } catch {
        return false
      }
    },
    '响应时间可接受': (r) => r.timings.duration < 1000,
  })

  if (success) {
    loginSuccessRate.add(1)
    try {
      const body = JSON.parse(response.body)
      console.log(`用户 ${user.username} 登录成功，耗时: ${duration}ms`)
      return body.token
    } catch {
      loginFailureRate.add(1)
      apiErrors.add(1)
      return null
    }
  } else {
    loginFailureRate.add(1)
    apiErrors.add(1)
    console.log(`用户 ${user.username} 登录失败，状态码: ${response.status}`)
    return null
  }
}

/**
 * 获取用户信息
 */
function getUserProfile(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { name: 'profile' },
  }

  const response = http.get(`${BASE_URL}/api/auth/profile`, params)

  check(response, {
    '获取用户信息成功': (r) => r.status === 200,
    '返回用户数据': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.id && body.username
      } catch {
        return false
      }
    },
  }) || apiErrors.add(1)
}

/**
 * 访问受保护资源
 */
function accessProtectedResources(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { name: 'protected_resources' },
  }

  // 访问用户列表
  const usersResponse = http.get(`${BASE_URL}/api/users?page=1&size=20`, params)
  check(usersResponse, {
    '用户列表访问成功': (r) => r.status === 200,
  }) || apiErrors.add(1)

  // 模拟页面切换延迟
  sleep(0.5)

  // 访问其他资源
  const otherResponse = http.get(`${BASE_URL}/api/users/stats`, params)
  check(otherResponse, {
    '统计信息访问': (r) => r.status === 200 || r.status === 404, // 可能未实现
  })
}

/**
 * 刷新Token
 */
function refreshToken(token) {
  // 注意：这里需要refresh token，简化处理
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { name: 'refresh' },
  }

  const response = http.post(`${BASE_URL}/api/auth/refresh`, '{}', params)

  check(response, {
    'Token刷新': (r) => r.status === 200 || r.status === 401, // 401也是预期的
  })
}

/**
 * 执行登出
 */
function performLogout(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: { name: 'logout' },
  }

  const response = http.post(`${BASE_URL}/api/auth/logout`, '{}', params)

  check(response, {
    '登出成功': (r) => r.status === 200,
  }) || apiErrors.add(1)
}

/**
 * 错误处理
 */
export function handleSummary(data) {
  console.log('K6负载测试完成，生成报告...')

  return {
    'performance-report.json': JSON.stringify(data),
    'performance-summary.txt': generateTextSummary(data),
  }
}

/**
 * 生成文本摘要
 */
function generateTextSummary(data) {
  const summary = []

  summary.push('=== deepSearch认证系统负载测试报告 ===\n')

  // 基本统计
  summary.push(`测试时长: ${Math.round(data.state.testRunDurationMs / 1000)}秒`)
  summary.push(`虚拟用户数: ${data.metrics.vus?.values?.value || 'N/A'}`)
  summary.push(`总请求数: ${data.metrics.http_reqs?.values?.count || 'N/A'}`)
  summary.push(`失败请求: ${data.metrics.http_req_failed?.values?.passes || 0}`)
  summary.push('')

  // 响应时间
  if (data.metrics.http_req_duration?.values) {
    const duration = data.metrics.http_req_duration.values
    summary.push('响应时间统计:')
    summary.push(`  平均值: ${Math.round(duration.avg)}ms`)
    summary.push(`  中位数: ${Math.round(duration.med)}ms`)
    summary.push(`  95百分位: ${Math.round(duration['p(95)']}ms`)
    summary.push(`  99百分位: ${Math.round(duration['p(99)']}ms`)
    summary.push('')
  }

  // 自定义指标
  if (data.metrics.login_success_rate?.values) {
    const rate = data.metrics.login_success_rate.values.rate
    summary.push(`登录成功率: ${(rate * 100).toFixed(2)}%`)
  }

  if (data.metrics.api_errors?.values) {
    summary.push(`API错误总数: ${data.metrics.api_errors.values.count}`)
  }

  summary.push('')
  summary.push('=== 测试完成 ===')

  return summary.join('\n')
}

/**
 * 清理阶段
 */
export function teardown(data) {
  console.log('开始K6负载测试清理...')
  // 这里可以添加清理逻辑
  console.log('K6负载测试清理完成')
}