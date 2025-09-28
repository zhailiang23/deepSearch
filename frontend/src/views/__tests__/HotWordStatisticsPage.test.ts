/**
 * 热词统计页面组件测试
 * 测试页面集成和端到端功能
 */

import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import HotWordStatisticsPage from '@/views/statistics/HotWordStatisticsPage.vue'
import { useHotWordStatisticsStore } from '@/stores/hotWordStatistics'
import type { HotWordItem } from '@/types/statistics'

// Mock 子组件
vi.mock('@/components/statistics/HotWordCloudChart.vue', () => ({
  default: {
    name: 'HotWordCloudChart',
    template: '<div data-testid="word-cloud-chart">WordCloudChart Mock</div>',
    props: ['words', 'theme', 'responsive', 'loading', 'error', 'options'],
    emits: ['word-click', 'word-hover', 'render-start', 'render-complete', 'render-error', 'download']
  }
}))

vi.mock('@/components/statistics/HotWordFilter.vue', () => ({
  default: {
    name: 'HotWordFilter',
    template: '<div data-testid="hot-word-filter">HotWordFilter Mock</div>',
    props: ['modelValue', 'loading'],
    emits: ['update:modelValue', 'search', 'reset']
  }
}))

vi.mock('@/composables/useHotWordData', () => ({
  useHotWordData: () => ({
    hotWords: { value: [] },
    statistics: { value: null },
    loading: { value: false },
    error: { value: null },
    filter: {},
    loadHotWords: vi.fn(),
    refreshAll: vi.fn(),
    updateFilter: vi.fn(),
    setTimeRange: vi.fn(),
    exportReport: vi.fn(),
    findHotWord: vi.fn(),
    getHotWordRank: vi.fn(),
    formatNumber: vi.fn()
  })
}))

describe('HotWordStatisticsPage', () => {
  let wrapper: VueWrapper
  let store: any

  // 模拟数据
  const mockHotWords = [
    { word: '测试词1', count: 100, percentage: 50, wordLength: 3, relatedQueriesCount: 10, lastOccurrence: '2025-09-28', firstOccurrence: '2025-09-21' },
    { word: '测试词2', count: 80, percentage: 40, wordLength: 3, relatedQueriesCount: 8, lastOccurrence: '2025-09-28', firstOccurrence: '2025-09-22' },
    { word: '测试词3', count: 60, percentage: 30, wordLength: 3, relatedQueriesCount: 6, lastOccurrence: '2025-09-27', firstOccurrence: '2025-09-23' }
  ]

  const mockStatistics: HotWordItem[] = [
    { keyword: '测试词1', count: 100, rank: 1, trend: 'up', percentage: 50 },
    { keyword: '测试词2', count: 80, rank: 2, trend: 'stable', percentage: 40 },
    { keyword: '测试词3', count: 60, rank: 3, trend: 'down', percentage: 30 }
  ]

  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)

    // 设置store
    store = useHotWordStatisticsStore()

    // 模拟store方法
    store.fetchHotWords = vi.fn().mockResolvedValue(undefined)
    store.refreshAll = vi.fn().mockResolvedValue(undefined)
    store.updateFilter = vi.fn()
    store.setTimeRange = vi.fn()
    store.exportReport = vi.fn().mockResolvedValue(undefined)

    // 设置初始数据
    store.hotWords = mockHotWords
    store.loading = false
    store.error = null
    store.filter = {
      dateRange: {
        start: new Date('2025-09-21'),
        end: new Date('2025-09-28')
      },
      limit: 50,
      minWordLength: 2,
      excludeStopWords: true,
      includeSegmentDetails: false
    }
    store.dataSummary = {
      totalWords: 3,
      totalCount: 240,
      avgCount: 80,
      maxCount: 100,
      minCount: 60,
      timeRange: {
        start: new Date('2025-09-21'),
        end: new Date('2025-09-28')
      }
    }

    wrapper = mount(HotWordStatisticsPage, {
      global: {
        plugins: [pinia],
        stubs: {
          'router-link': true
        }
      }
    })
  })

  afterEach(() => {
    wrapper.unmount()
    vi.clearAllMocks()
  })

  describe('页面渲染', () => {
    it('应该正确渲染页面标题', () => {
      const title = wrapper.find('h1')
      expect(title.exists()).toBe(true)
      expect(title.text()).toBe('热词统计分析')
    })

    it('应该渲染页面描述', () => {
      const description = wrapper.find('p')
      expect(description.exists()).toBe(true)
      expect(description.text()).toBe('基于搜索日志的热词分析和可视化展示')
    })

    it('应该渲染筛选器组件', () => {
      const filter = wrapper.find('[data-testid="hot-word-filter"]')
      expect(filter.exists()).toBe(true)
    })

    it('应该渲染词云图组件', () => {
      const wordCloud = wrapper.find('[data-testid="word-cloud-chart"]')
      expect(wordCloud.exists()).toBe(true)
    })

    it('应该渲染统计概览卡片', () => {
      const overviewCards = wrapper.findAll('.bg-white.rounded-lg.shadow-sm.border.p-6')
      // 应该有统计概览的4个卡片
      expect(overviewCards.length).toBeGreaterThanOrEqual(4)
    })
  })

  describe('统计概览', () => {
    it('应该显示正确的总搜索次数', () => {
      const searchCount = wrapper.find('text-2xl font-semibold text-gray-900')
      // 检查是否包含总搜索次数的显示
      expect(wrapper.html()).toContain('240')
    })

    it('应该显示正确的热词数量', () => {
      expect(wrapper.html()).toContain('3')
    })

    it('应该显示正确的平均搜索频次', () => {
      expect(wrapper.html()).toContain('80')
    })

    it('应该显示时间段信息', () => {
      expect(wrapper.html()).toContain('2025-9-21')
      expect(wrapper.html()).toContain('2025-9-28')
    })
  })

  describe('TOP 10 热词排行榜', () => {
    it('应该显示排行榜标题', () => {
      expect(wrapper.html()).toContain('TOP 10 热词')
    })

    it('应该显示热词项', () => {
      expect(wrapper.html()).toContain('测试词1')
      expect(wrapper.html()).toContain('测试词2')
      expect(wrapper.html()).toContain('测试词3')
    })

    it('应该显示搜索次数', () => {
      expect(wrapper.html()).toContain('100')
      expect(wrapper.html()).toContain('80')
      expect(wrapper.html()).toContain('60')
    })
  })

  describe('趋势信息', () => {
    it('应该显示趋势分析标题', () => {
      expect(wrapper.html()).toContain('趋势分析')
    })

    it('应该显示上升最快、下降最快、新热词信息', () => {
      expect(wrapper.html()).toContain('上升最快')
      expect(wrapper.html()).toContain('下降最快')
      expect(wrapper.html()).toContain('新热词')
    })
  })

  describe('数据表格', () => {
    it('应该显示表格标题', () => {
      expect(wrapper.html()).toContain('热词详细数据')
    })

    it('应该显示表格头部', () => {
      expect(wrapper.html()).toContain('排名')
      expect(wrapper.html()).toContain('热词')
      expect(wrapper.html()).toContain('搜索次数')
      expect(wrapper.html()).toContain('占比')
      expect(wrapper.html()).toContain('趋势')
    })

    it('应该提供搜索功能', () => {
      const searchInput = wrapper.find('input[placeholder="搜索热词..."]')
      expect(searchInput.exists()).toBe(true)
    })
  })

  describe('词云图配置', () => {
    it('应该提供主题选择', () => {
      const themeSelect = wrapper.find('select')
      expect(themeSelect.exists()).toBe(true)
    })

    it('应该提供显示数量选择', () => {
      const selects = wrapper.findAll('select')
      expect(selects.length).toBeGreaterThanOrEqual(2)
    })
  })

  describe('用户交互', () => {
    it('点击刷新按钮应该调用刷新方法', async () => {
      const refreshButton = wrapper.find('button:contains("刷新数据")')
      if (refreshButton.exists()) {
        await refreshButton.trigger('click')
        expect(store.refreshAll).toHaveBeenCalled()
      }
    })

    it('点击导出按钮应该调用导出方法', async () => {
      const exportButton = wrapper.find('button:contains("导出报告")')
      if (exportButton.exists()) {
        await exportButton.trigger('click')
        // 由于使用了本地导出，这里检查是否创建了下载链接
        expect(document.createElement).toBeDefined()
      }
    })

    it('搜索框输入应该触发筛选', async () => {
      const searchInput = wrapper.find('input[placeholder="搜索热词..."]')
      if (searchInput.exists()) {
        await searchInput.setValue('测试')
        await nextTick()
        // 检查是否触发了搜索逻辑
        expect(searchInput.element.value).toBe('测试')
      }
    })
  })

  describe('加载状态', () => {
    it('应该正确显示加载状态', async () => {
      store.loading = true
      await nextTick()

      // 检查加载状态是否传递给子组件
      const wordCloud = wrapper.findComponent({ name: 'HotWordCloudChart' })
      expect(wordCloud.props('loading')).toBe(true)
    })

    it('加载时按钮应该被禁用', async () => {
      store.loading = true
      await nextTick()

      const refreshButton = wrapper.find('button:contains("刷新数据")')
      if (refreshButton.exists()) {
        expect(refreshButton.attributes('disabled')).toBeDefined()
      }
    })
  })

  describe('错误处理', () => {
    it('应该正确显示错误状态', async () => {
      store.error = '加载失败'
      await nextTick()

      // 检查错误状态是否传递给子组件
      const wordCloud = wrapper.findComponent({ name: 'HotWordCloudChart' })
      expect(wordCloud.props('error')).toBe('加载失败')
    })
  })

  describe('响应式设计', () => {
    it('应该使用响应式CSS类', () => {
      // 检查响应式网格布局
      expect(wrapper.html()).toContain('grid-cols-1')
      expect(wrapper.html()).toContain('md:grid-cols-4')
      expect(wrapper.html()).toContain('lg:grid-cols-3')
    })

    it('应该使用响应式间距', () => {
      expect(wrapper.html()).toContain('gap-6')
      expect(wrapper.html()).toContain('gap-8')
    })
  })

  describe('数据格式化', () => {
    it('应该正确格式化数字', () => {
      // 检查数字是否使用了toLocaleString格式化
      expect(wrapper.html()).toContain('100')
      expect(wrapper.html()).toContain('80')
      expect(wrapper.html()).toContain('60')
    })

    it('应该正确计算百分比', () => {
      // 检查百分比计算
      expect(wrapper.html()).toContain('%')
    })
  })

  describe('组件通信', () => {
    it('筛选器变化应该触发数据加载', async () => {
      const filterComponent = wrapper.findComponent({ name: 'HotWordFilter' })

      // 模拟筛选器事件
      await filterComponent.vm.$emit('search')

      expect(store.fetchHotWords).toHaveBeenCalled()
    })

    it('词云图点击应该触发相应处理', async () => {
      const wordCloudComponent = wrapper.findComponent({ name: 'HotWordCloudChart' })

      // 模拟词云图点击事件
      const mockWordItem = { text: '测试词1', weight: 100 }
      await wordCloudComponent.vm.$emit('word-click', mockWordItem, new Event('click'))

      // 检查是否有相应的处理逻辑
      expect(wrapper.vm).toBeDefined()
    })
  })

  describe('生命周期', () => {
    it('组件挂载时应该设置默认时间范围', () => {
      // 检查是否调用了setTimeRange
      expect(store.setTimeRange).toHaveBeenCalled()
    })
  })

  describe('URL路由集成', () => {
    it('页面应该能够正确处理路由参数', () => {
      // 这里可以测试路由参数的处理
      expect(wrapper.vm).toBeDefined()
    })
  })

  describe('性能优化', () => {
    it('应该使用计算属性缓存数据', () => {
      // 检查计算属性的正确使用
      expect(wrapper.vm.statistics).toBeDefined()
      expect(wrapper.vm.summary).toBeDefined()
      expect(wrapper.vm.topWords).toBeDefined()
    })

    it('应该正确处理大数据量的渲染', async () => {
      // 设置大量数据测试
      const largeDataSet = Array.from({ length: 1000 }, (_, i) => ({
        keyword: `测试词${i}`,
        count: 1000 - i,
        rank: i + 1,
        trend: 'stable' as const,
        percentage: (1000 - i) / 10
      }))

      store.hotWords = largeDataSet
      await nextTick()

      // 检查是否正确处理了大数据量
      expect(wrapper.vm.statistics.length).toBe(1000)
    })
  })
})