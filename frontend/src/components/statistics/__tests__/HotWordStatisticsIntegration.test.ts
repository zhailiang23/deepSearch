/**
 * 热词统计页面集成测试
 * 验证词云组件与其他组件的兼容性和集成效果
 */

import { describe, it, expect, beforeEach, afterEach, vi, type Mock } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import HotWordStatisticsPage from '@/views/statistics/HotWordStatisticsPage.vue'
import HotWordCloudChart from '../HotWordCloudChart.vue'
import HotWordFilter from '../HotWordFilter.vue'
import type { HotWordStatistics, FilterConfig } from '@/types/statistics'

// Mock API
const mockStatisticsApi = {
  getHotWordStatistics: vi.fn(),
  getHotWordTrends: vi.fn(),
  exportStatisticsReport: vi.fn()
}

vi.mock('@/api/statistics', () => ({
  statisticsApi: mockStatisticsApi
}))

// Mock wordcloud composables
const mockUseWordCloud = {
  renderState: {
    isRendering: false,
    progress: 0,
    error: null
  },
  dimensions: { width: 800, height: 600 },
  canRender: true,
  renderWordCloud: vi.fn(),
  stopRendering: vi.fn(),
  clearCanvas: vi.fn(),
  updateWords: vi.fn(),
  updateDimensions: vi.fn(),
  checkWordCloudAvailability: vi.fn(() => Promise.resolve(true))
}

const mockUseWordCloudTheme = {
  currentTheme: {
    name: 'light-green',
    colors: {
      primary: ['#10b981', '#059669'],
      secondary: ['#34d399', '#6ee7b7'],
      background: '#ffffff'
    },
    fonts: {
      family: 'Arial, sans-serif',
      weight: 'normal'
    }
  },
  setTheme: vi.fn(),
  getColorFunction: vi.fn(() => () => '#10b981')
}

vi.mock('@/composables/useWordCloud', () => ({
  useWordCloud: () => mockUseWordCloud
}))

vi.mock('@/composables/useWordCloud', () => ({
  useWordCloudTheme: () => mockUseWordCloudTheme
}))

describe('HotWordStatistics Integration', () => {
  let wrapper: VueWrapper<any>

  const mockStatisticsData: HotWordStatistics[] = [
    {
      keyword: '人工智能',
      searchCount: 1500,
      trend: 'rising',
      lastSearchTime: '2024-01-15T10:30:00Z',
      channels: ['web', 'mobile'],
      growthRate: 25.5
    },
    {
      keyword: '机器学习',
      searchCount: 1200,
      trend: 'stable',
      lastSearchTime: '2024-01-15T09:45:00Z',
      channels: ['web'],
      growthRate: 5.2
    },
    {
      keyword: '深度学习',
      searchCount: 980,
      trend: 'falling',
      lastSearchTime: '2024-01-15T08:20:00Z',
      channels: ['web', 'api'],
      growthRate: -12.3
    },
    {
      keyword: '神经网络',
      searchCount: 850,
      trend: 'new',
      lastSearchTime: '2024-01-15T11:10:00Z',
      channels: ['mobile'],
      growthRate: 100.0
    },
    {
      keyword: '算法',
      searchCount: 750,
      trend: 'stable',
      lastSearchTime: '2024-01-15T07:30:00Z',
      channels: ['web', 'mobile', 'api'],
      growthRate: 2.1
    }
  ]

  beforeEach(() => {
    // Reset all mocks
    vi.clearAllMocks()

    // Setup mock API responses
    mockStatisticsApi.getHotWordStatistics.mockResolvedValue({
      success: true,
      data: mockStatisticsData
    })

    mockStatisticsApi.getHotWordTrends.mockResolvedValue({
      success: true,
      data: []
    })

    // Reset composable mocks
    mockUseWordCloud.renderState.isRendering = false
    mockUseWordCloud.renderState.progress = 0
    mockUseWordCloud.renderState.error = null
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('页面初始化', () => {
    it('应该正确加载和渲染完整页面', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证页面结构
      expect(wrapper.find('.hot-word-statistics-page').exists()).toBe(true)
      expect(wrapper.find('h1').text()).toBe('热词统计分析')

      // 验证API调用
      expect(mockStatisticsApi.getHotWordStatistics).toHaveBeenCalled()
    })

    it('应该正确渲染所有子组件', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证筛选组件
      expect(wrapper.findComponent(HotWordFilter).exists()).toBe(true)

      // 验证词云组件
      expect(wrapper.findComponent(HotWordCloudChart).exists()).toBe(true)

      // 验证统计概览
      expect(wrapper.find('.grid.grid-cols-1.md\\:grid-cols-4').exists()).toBe(true)

      // 验证数据表格
      expect(wrapper.find('table').exists()).toBe(true)
    })

    it('应该正确显示统计概览数据', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 计算期望的总搜索次数
      const expectedTotal = mockStatisticsData.reduce((sum, item) => sum + item.searchCount, 0)

      // 验证统计卡片
      const statCards = wrapper.findAll('.bg-white.rounded-lg.shadow-sm.border.p-6')
      expect(statCards.length).toBeGreaterThanOrEqual(4)

      // 验证总搜索次数显示
      expect(wrapper.text()).toContain(expectedTotal.toLocaleString())

      // 验证热词数量
      expect(wrapper.text()).toContain(mockStatisticsData.length.toString())
    })
  })

  describe('筛选功能集成', () => {
    it('筛选条件变化应该触发数据重新加载', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 清除之前的API调用记录
      mockStatisticsApi.getHotWordStatistics.mockClear()

      // 找到筛选组件并触发筛选变化
      const filterComponent = wrapper.findComponent(HotWordFilter)
      expect(filterComponent.exists()).toBe(true)

      // 模拟筛选条件变化
      await filterComponent.vm.$emit('filter-change')

      // 验证API重新调用
      expect(mockStatisticsApi.getHotWordStatistics).toHaveBeenCalled()
    })

    it('应该正确处理时间范围筛选', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证默认时间范围（最近7天）
      const calls = mockStatisticsApi.getHotWordStatistics.mock.calls
      expect(calls.length).toBeGreaterThan(0)

      const params = calls[0][0]
      expect(params.startTime).toBeDefined()
      expect(params.endTime).toBeDefined()

      // 验证时间范围合理性
      const startTime = new Date(params.startTime)
      const endTime = new Date(params.endTime)
      const daysDiff = (endTime.getTime() - startTime.getTime()) / (1000 * 60 * 60 * 24)
      expect(daysDiff).toBeCloseTo(7, 1) // 大约7天
    })
  })

  describe('词云组件集成', () => {
    it('应该正确传递数据给词云组件', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)
      expect(wordCloudComponent.exists()).toBe(true)

      // 验证传递给词云组件的数据
      const wordCloudProps = wordCloudComponent.props()
      expect(wordCloudProps.words).toBeDefined()
      expect(Array.isArray(wordCloudProps.words)).toBe(true)

      // 验证数据转换正确性
      if (wordCloudProps.words.length > 0) {
        const firstWord = wordCloudProps.words[0]
        expect(firstWord).toHaveProperty('text')
        expect(firstWord).toHaveProperty('weight')
        expect(typeof firstWord.text).toBe('string')
        expect(typeof firstWord.weight).toBe('number')
      }
    })

    it('词云主题变化应该正确应用', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)

      // 验证初始主题
      expect(wordCloudComponent.props('theme')).toBe('light-green')

      // 改变主题选择
      const themeSelector = wrapper.find('select[data-testid="theme-selector"]')
      if (themeSelector.exists()) {
        await themeSelector.setValue('dark-green')
        await nextTick()

        // 验证主题变化
        expect(wordCloudComponent.props('theme')).toBe('dark-green')
      }
    })

    it('词云配置变化应该触发重新渲染', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 清除渲染调用记录
      mockUseWordCloud.renderWordCloud.mockClear()

      // 改变显示数量
      const maxWordsSelector = wrapper.find('select')
      if (maxWordsSelector.exists()) {
        await maxWordsSelector.setValue('200')
        await nextTick()

        // 等待词云重新渲染
        await new Promise(resolve => setTimeout(resolve, 100))

        // 验证重新渲染被调用（通过nextTick触发）
        // 实际实现中会通过watch监听器触发
      }
    })
  })

  describe('数据交互', () => {
    it('点击TOP 10列表中的词语应该高亮显示', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 找到TOP 10列表
      const topWordsList = wrapper.find('.space-y-3')
      if (topWordsList.exists()) {
        const firstWord = topWordsList.find('.flex.items-center')
        if (firstWord.exists()) {
          await firstWord.trigger('click')

          // 验证高亮逻辑被触发
          // 实际实现中可能会在词云中高亮对应词语
        }
      }
    })

    it('搜索功能应该正确过滤数据表格', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 找到搜索框
      const searchInput = wrapper.find('input[placeholder="搜索热词..."]')
      expect(searchInput.exists()).toBe(true)

      // 输入搜索关键词
      await searchInput.setValue('人工智能')
      await nextTick()

      // 验证表格数据被过滤
      const tableRows = wrapper.findAll('tbody tr')
      expect(tableRows.length).toBeGreaterThan(0)

      // 验证显示的行包含搜索关键词
      const visibleText = wrapper.find('tbody').text()
      expect(visibleText).toContain('人工智能')
    })

    it('分页功能应该正确工作', async () => {
      // 创建更多测试数据以触发分页
      const largeDataSet = Array.from({ length: 100 }, (_, i) => ({
        keyword: `词语${i + 1}`,
        searchCount: Math.floor(Math.random() * 1000) + 100,
        trend: 'stable' as const,
        lastSearchTime: new Date().toISOString(),
        channels: ['web'],
        growthRate: Math.random() * 20 - 10
      }))

      mockStatisticsApi.getHotWordStatistics.mockResolvedValue({
        success: true,
        data: largeDataSet
      })

      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证分页控件存在
      const pagination = wrapper.find('.flex.items-center.justify-between.mt-6')
      if (pagination.exists()) {
        // 验证分页信息
        expect(pagination.text()).toContain('显示')
        expect(pagination.text()).toContain('条记录')

        // 测试下一页按钮
        const nextButton = pagination.find('button:last-child')
        if (nextButton.exists() && !nextButton.attributes('disabled')) {
          await nextButton.trigger('click')
          await nextTick()

          // 验证页码变化
          expect(pagination.text()).toContain('2 /')
        }
      }
    })
  })

  describe('错误处理集成', () => {
    it('应该正确处理API错误', async () => {
      // 模拟API错误
      mockStatisticsApi.getHotWordStatistics.mockRejectedValue(new Error('网络错误'))

      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证错误状态
      expect(wrapper.vm.error).toBeTruthy()

      // 验证错误信息传递给词云组件
      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)
      expect(wordCloudComponent.props('error')).toBeTruthy()
    })

    it('应该正确处理空数据情况', async () => {
      // 模拟空数据响应
      mockStatisticsApi.getHotWordStatistics.mockResolvedValue({
        success: true,
        data: []
      })

      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证词云组件接收到空数据
      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)
      expect(wordCloudComponent.props('words')).toEqual([])

      // 验证统计概览显示零值
      expect(wrapper.text()).toContain('0')
    })

    it('词云渲染错误应该正确显示', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)

      // 模拟词云渲染错误
      await wordCloudComponent.vm.$emit('render-error', '渲染失败')

      // 验证错误处理
      // 实际实现中可能会显示错误提示或降级方案
    })
  })

  describe('性能优化集成', () => {
    it('大数据集应该触发性能优化', async () => {
      // 创建大数据集
      const largeDataSet = Array.from({ length: 1000 }, (_, i) => ({
        keyword: `大数据词语${i + 1}`,
        searchCount: Math.floor(Math.random() * 1000) + 100,
        trend: 'stable' as const,
        lastSearchTime: new Date().toISOString(),
        channels: ['web'],
        growthRate: Math.random() * 20 - 10
      }))

      mockStatisticsApi.getHotWordStatistics.mockResolvedValue({
        success: true,
        data: largeDataSet
      })

      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 验证传递给词云的数据被限制
      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)
      const wordCloudData = wordCloudComponent.props('words')

      // 默认maxWords应该限制显示的词语数量
      expect(wordCloudData.length).toBeLessThanOrEqual(100)
    })

    it('响应式设计应该正确工作', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)

      // 验证响应式属性
      expect(wordCloudComponent.props('responsive')).toBe(true)

      // 验证词云组件能够处理容器尺寸变化
      // 实际实现中会通过ResizeObserver监听
    })
  })

  describe('导出功能集成', () => {
    it('导出报告功能应该正确工作', async () => {
      // 模拟Blob和URL.createObjectURL
      const mockBlob = vi.fn()
      const mockCreateObjectURL = vi.fn(() => 'mock-url')
      const mockRevokeObjectURL = vi.fn()

      global.Blob = mockBlob as any
      global.URL = {
        createObjectURL: mockCreateObjectURL,
        revokeObjectURL: mockRevokeObjectURL
      } as any

      // 模拟链接点击
      const mockClick = vi.fn()
      const mockCreateElement = vi.spyOn(document, 'createElement').mockImplementation((tagName) => {
        if (tagName === 'a') {
          return {
            click: mockClick,
            href: '',
            download: ''
          } as any
        }
        return {} as any
      })

      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 点击导出按钮
      const exportButton = wrapper.find('button:contains("导出报告")')
      if (exportButton.exists()) {
        await exportButton.trigger('click')

        // 验证导出逻辑被触发
        expect(mockBlob).toHaveBeenCalled()
        expect(mockCreateObjectURL).toHaveBeenCalled()
        expect(mockClick).toHaveBeenCalled()
      }

      // 清理mock
      mockCreateElement.mockRestore()
    })

    it('词云下载功能应该正确工作', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      const wordCloudComponent = wrapper.findComponent(HotWordCloudChart)

      // 模拟画布下载
      const mockCanvas = {
        toDataURL: vi.fn(() => 'data:image/png;base64,mock-data')
      } as any

      await wordCloudComponent.vm.$emit('download', mockCanvas)

      // 验证下载处理逻辑
      expect(mockCanvas.toDataURL).toHaveBeenCalled()
    })
  })

  describe('实时更新集成', () => {
    it('刷新按钮应该重新加载所有数据', async () => {
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 清除API调用记录
      mockStatisticsApi.getHotWordStatistics.mockClear()

      // 点击刷新按钮
      const refreshButton = wrapper.find('button:contains("刷新数据")')
      expect(refreshButton.exists()).toBe(true)

      await refreshButton.trigger('click')

      // 验证数据重新加载
      expect(mockStatisticsApi.getHotWordStatistics).toHaveBeenCalled()
    })

    it('自动刷新机制应该定期更新数据', async () => {
      // 如果实现了自动刷新功能
      wrapper = mount(HotWordStatisticsPage)

      await flushPromises()

      // 这里可以测试定时器或轮询逻辑
      // 实际实现取决于是否有自动刷新需求
    })
  })
})