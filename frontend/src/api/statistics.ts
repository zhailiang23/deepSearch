/**
 * 统计相关API接口
 */

import http from '@/utils/http'
import type {
  HotWordStatistics,
  StatisticsQueryParams
} from '@/types/statistics'

export interface StatisticsResponse<T = any> {
  success: boolean
  data: T
  message?: string
}

/**
 * 统计API接口
 */
export const statisticsApi = {
  /**
   * 获取热词统计数据
   */
  async getHotWordStatistics(params: StatisticsQueryParams): Promise<StatisticsResponse<HotWordStatistics[]>> {
    try {
      // 模拟API调用，实际项目中应该调用真实的API
      await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟网络延迟

      // 生成模拟数据
      const mockData = generateMockStatistics(params.limit || 100)

      return {
        success: true,
        data: mockData
      }
    } catch (error) {
      console.error('Failed to fetch hot word statistics:', error)
      throw new Error('获取热词统计数据失败')
    }
  },

  /**
   * 获取热词趋势数据
   */
  async getHotWordTrends(params: StatisticsQueryParams): Promise<StatisticsResponse<any[]>> {
    try {
      await new Promise(resolve => setTimeout(resolve, 800))

      const mockTrends = generateMockTrends()

      return {
        success: true,
        data: mockTrends
      }
    } catch (error) {
      console.error('Failed to fetch hot word trends:', error)
      throw new Error('获取热词趋势数据失败')
    }
  },

  /**
   * 导出统计报告
   */
  async exportStatisticsReport(params: StatisticsQueryParams): Promise<Blob> {
    try {
      // 实际项目中这里应该调用后端API获取报告文件
      const data = await this.getHotWordStatistics(params)
      const reportContent = generateReportContent(data.data)

      return new Blob([reportContent], { type: 'text/plain;charset=utf-8' })
    } catch (error) {
      console.error('Failed to export statistics report:', error)
      throw new Error('导出统计报告失败')
    }
  }
}

/**
 * 生成模拟统计数据
 */
function generateMockStatistics(limit: number): HotWordStatistics[] {
  const baseKeywords = [
    // 科技类
    '人工智能', '机器学习', '深度学习', '神经网络', '大数据', '云计算', '物联网', '区块链',
    '算法', '数据科学', '编程', '软件开发', '系统架构', '网络安全', '自动化', '智能化',

    // 编程语言
    'Python', 'JavaScript', 'Java', 'TypeScript', 'Go', 'Rust', 'C++', 'Swift', 'Kotlin', 'PHP',

    // 框架技术
    'Vue.js', 'React', 'Angular', 'Node.js', 'Spring Boot', 'Django', 'Flask', 'Express',
    'TensorFlow', 'PyTorch', 'Docker', 'Kubernetes', 'Redis', 'MySQL', 'PostgreSQL',

    // 概念方法
    '微服务', '容器化', 'DevOps', '敏捷开发', '测试驱动', '持续集成', '性能优化',
    '用户体验', '响应式设计', '移动端开发', 'API设计', '数据库设计',

    // 行业应用
    '金融科技', '电子商务', '在线教育', '智能制造', '智慧城市', '医疗健康',
    '新能源', '自动驾驶', '虚拟现实', '增强现实', '游戏开发', '移动应用',

    // 工具平台
    'GitHub', 'GitLab', 'AWS', 'Azure', 'Google Cloud', 'Linux', 'Windows', 'macOS',
    'Visual Studio Code', 'IntelliJ IDEA', 'Webpack', 'Vite', 'npm', 'yarn'
  ]

  const trends: Array<'rising' | 'falling' | 'stable' | 'new'> = ['rising', 'falling', 'stable', 'new']

  const statistics: HotWordStatistics[] = []

  for (let i = 0; i < Math.min(limit, baseKeywords.length); i++) {
    const keyword = baseKeywords[i]
    const baseCount = Math.floor(Math.random() * 10000) + 100
    const trend = trends[Math.floor(Math.random() * trends.length)]

    statistics.push({
      keyword,
      searchCount: baseCount,
      trend,
      lastSearchTime: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      channels: generateRandomChannels(),
      growthRate: Math.random() * 200 - 100 // -100% 到 +100%
    })
  }

  // 按搜索次数排序
  return statistics.sort((a, b) => b.searchCount - a.searchCount)
}

/**
 * 生成随机渠道
 */
function generateRandomChannels(): string[] {
  const channels = ['web', 'mobile', 'api', 'desktop']
  const count = Math.floor(Math.random() * 3) + 1
  const result = []

  for (let i = 0; i < count; i++) {
    const channel = channels[Math.floor(Math.random() * channels.length)]
    if (!result.includes(channel)) {
      result.push(channel)
    }
  }

  return result
}

/**
 * 生成模拟趋势数据
 */
function generateMockTrends() {
  const trends = []
  const now = Date.now()

  for (let i = 6; i >= 0; i--) {
    const date = new Date(now - i * 24 * 60 * 60 * 1000)
    trends.push({
      date: date.toISOString().split('T')[0],
      searchCount: Math.floor(Math.random() * 5000) + 1000,
      uniqueKeywords: Math.floor(Math.random() * 200) + 50
    })
  }

  return trends
}

/**
 * 生成报告内容
 */
function generateReportContent(data: HotWordStatistics[]): string {
  let content = '热词统计报告\n'
  content += '====================\n\n'
  content += `生成时间: ${new Date().toLocaleString('zh-CN')}\n`
  content += `数据条数: ${data.length}\n\n`

  content += '排名\t关键词\t搜索次数\t趋势\t最后搜索时间\n'
  content += '----\t------\t--------\t----\t------------\n'

  data.forEach((item, index) => {
    const trendMap = {
      'rising': '上升',
      'falling': '下降',
      'stable': '稳定',
      'new': '新词'
    }

    content += `${index + 1}\t${item.keyword}\t${item.searchCount}\t${trendMap[item.trend || 'stable']}\t${item.lastSearchTime ? new Date(item.lastSearchTime).toLocaleString('zh-CN') : '-'}\n`
  })

  return content
}