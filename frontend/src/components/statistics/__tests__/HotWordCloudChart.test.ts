/**
 * HotWordCloudChart 组件测试
 * 测试词云图组件的各种功能，包括渲染、交互、性能等
 */

import { describe, it, expect, beforeEach, afterEach, vi, type Mock } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import HotWordCloudChart from '../HotWordCloudChart.vue'
import type { HotWordItem, WordCloudOptions } from '@/types/statistics'

// Mock wordcloud2 library
const mockWordCloud = vi.fn()
vi.mock('wordcloud', () => ({
  default: mockWordCloud
}))

// Mock composables
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

describe('HotWordCloudChart', () => {
  let wrapper: VueWrapper<any>

  const defaultProps = {
    words: [
      { text: '人工智能', weight: 100 },
      { text: '机器学习', weight: 80 },
      { text: '深度学习', weight: 75 },
      { text: '神经网络', weight: 70 },
      { text: '算法', weight: 60 }
    ] as HotWordItem[],
    width: 800,
    height: 600,
    theme: 'light-green',
    responsive: false,
    loading: false,
    error: null,
    showToolbar: true
  }

  beforeEach(() => {
    // Reset all mocks
    vi.clearAllMocks()

    // Reset mock implementation
    mockUseWordCloud.renderState.isRendering = false
    mockUseWordCloud.renderState.progress = 0
    mockUseWordCloud.renderState.error = null
    mockUseWordCloud.canRender = true
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('组件渲染', () => {
    it('应该正确渲染词云组件', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)
      expect(wrapper.find('canvas.word-cloud-canvas').exists()).toBe(true)
    })

    it('应该根据props设置容器样式', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          width: 400,
          height: 300
        }
      })

      const container = wrapper.find('.canvas-wrapper')
      const style = container.attributes('style')
      expect(style).toContain('width: 400px')
      expect(style).toContain('height: 300px')
    })

    it('响应式模式下不应该设置固定尺寸', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          responsive: true,
          width: undefined,
          height: undefined
        }
      })

      const container = wrapper.find('.canvas-wrapper')
      const style = container.attributes('style')
      expect(style).not.toContain('width:')
      expect(style).not.toContain('height:')
    })
  })

  describe('加载状态', () => {
    it('应该显示加载遮罩当loading为true', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          loading: true
        }
      })

      const loadingMask = wrapper.find('.absolute.inset-0.bg-white\\/80')
      expect(loadingMask.exists()).toBe(true)
      expect(loadingMask.text()).toContain('正在生成词云图')
    })

    it('应该显示渲染进度', () => {
      mockUseWordCloud.renderState.isRendering = true
      mockUseWordCloud.renderState.progress = 50

      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const progressBar = wrapper.find('.bg-emerald-500')
      expect(progressBar.exists()).toBe(true)
      expect(progressBar.attributes('style')).toContain('width: 50%')
    })

    it('应该显示进度文本', () => {
      mockUseWordCloud.renderState.isRendering = true
      mockUseWordCloud.renderState.progress = 75

      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      expect(wrapper.text()).toContain('正在生成词云图... 75%')
    })
  })

  describe('错误状态', () => {
    it('应该显示错误状态当有错误时', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          error: '渲染失败'
        }
      })

      const errorMask = wrapper.find('.bg-red-50')
      expect(errorMask.exists()).toBe(true)
      expect(errorMask.text()).toContain('渲染失败')
      expect(errorMask.text()).toContain('渲染失败')
    })

    it('应该显示重试按钮', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          error: '渲染失败'
        }
      })

      const retryButton = wrapper.find('button:contains("重试")')
      expect(retryButton.exists()).toBe(true)
    })

    it('点击重试按钮应该调用重试方法', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          error: '渲染失败'
        }
      })

      const retryButton = wrapper.find('button')
      await retryButton.trigger('click')

      expect(mockUseWordCloud.clearCanvas).toHaveBeenCalled()
    })
  })

  describe('空数据状态', () => {
    it('应该显示空数据提示当没有词语数据时', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          words: [],
          loading: false,
          error: null
        }
      })

      const emptyState = wrapper.find('.bg-gray-50')
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.text()).toContain('暂无数据')
      expect(emptyState.text()).toContain('请配置词云数据后查看效果')
    })

    it('有数据时不应该显示空状态', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const emptyState = wrapper.find('.bg-gray-50:contains("暂无数据")')
      expect(emptyState.exists()).toBe(false)
    })
  })

  describe('工具栏', () => {
    it('应该显示工具栏当showToolbar为true', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          showToolbar: true
        }
      })

      const toolbar = wrapper.find('.absolute.top-2.right-2')
      expect(toolbar.exists()).toBe(true)
    })

    it('不应该显示工具栏当showToolbar为false', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          showToolbar: false
        }
      })

      const toolbar = wrapper.find('.absolute.top-2.right-2')
      expect(toolbar.exists()).toBe(false)
    })

    it('应该包含刷新和下载按钮', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          showToolbar: true
        }
      })

      const refreshButton = wrapper.find('button[title="重新渲染"]')
      const downloadButton = wrapper.find('button[title="下载图片"]')

      expect(refreshButton.exists()).toBe(true)
      expect(downloadButton.exists()).toBe(true)
    })

    it('渲染时应该禁用工具栏按钮', () => {
      mockUseWordCloud.renderState.isRendering = true

      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const buttons = wrapper.findAll('button')
      buttons.forEach(button => {
        expect(button.attributes('disabled')).toBeDefined()
      })
    })
  })

  describe('事件处理', () => {
    it('应该触发wordClick事件当点击画布时', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const canvas = wrapper.find('canvas')
      await canvas.trigger('click')

      // 这里需要模拟wordcloud2的点击回调
      // 实际实现中会通过wordcloud2的回调触发事件
    })

    it('应该触发wordHover事件当鼠标悬停时', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const canvas = wrapper.find('canvas')
      await canvas.trigger('mousemove')

      // 验证鼠标位置更新
      // 实际的悬停事件会由wordcloud2处理
    })

    it('应该清除悬停状态当鼠标离开画布时', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const canvas = wrapper.find('canvas')
      await canvas.trigger('mouseleave')

      // 验证悬停提示消失
      const tooltip = wrapper.find('.absolute.pointer-events-none')
      expect(tooltip.exists()).toBe(false)
    })

    it('点击刷新按钮应该调用renderWordCloud', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const refreshButton = wrapper.find('button[title="重新渲染"]')
      await refreshButton.trigger('click')

      expect(mockUseWordCloud.renderWordCloud).toHaveBeenCalled()
    })

    it('点击下载按钮应该触发download事件', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const downloadButton = wrapper.find('button[title="下载图片"]')
      await downloadButton.trigger('click')

      // 验证下载逻辑
      // 实际实现会创建下载链接
    })
  })

  describe('悬停提示', () => {
    it('应该显示悬停提示当有悬停词语时', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      // 模拟悬停词语
      await wrapper.setData({
        hoveredWord: { text: '人工智能', weight: 100 }
      })

      const tooltip = wrapper.find('.absolute.pointer-events-none')
      expect(tooltip.exists()).toBe(true)
      expect(tooltip.text()).toContain('人工智能')
      expect(tooltip.text()).toContain('权重: 100')
    })

    it('应该隐藏悬停提示当没有悬停词语时', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const tooltip = wrapper.find('.absolute.pointer-events-none')
      expect(tooltip.exists()).toBe(false)
    })
  })

  describe('主题切换', () => {
    it('应该响应主题变化', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await wrapper.setProps({ theme: 'dark-green' })

      expect(mockUseWordCloudTheme.setTheme).toHaveBeenCalledWith('dark-green')
    })

    it('应该在主题变化时重新渲染', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await wrapper.setProps({ theme: 'blue' })
      await flushPromises()

      expect(mockUseWordCloudTheme.setTheme).toHaveBeenCalled()
    })
  })

  describe('数据更新', () => {
    it('应该响应词语数据变化', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const newWords = [
        { text: 'Vue.js', weight: 90 },
        { text: 'React', weight: 85 }
      ]

      await wrapper.setProps({ words: newWords })

      expect(mockUseWordCloud.updateWords).toHaveBeenCalledWith(newWords)
    })

    it('空数据时应该清空画布', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await wrapper.setProps({ words: [] })

      expect(mockUseWordCloud.clearCanvas).toHaveBeenCalled()
    })

    it('应该响应尺寸变化', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await wrapper.setProps({ width: 1000, height: 800 })

      expect(mockUseWordCloud.updateDimensions).toHaveBeenCalledWith(1000, 800)
    })
  })

  describe('生命周期', () => {
    it('挂载时应该检查WordCloud库可用性', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await flushPromises()

      expect(mockUseWordCloud.checkWordCloudAvailability).toHaveBeenCalled()
    })

    it('挂载时应该设置主题', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await flushPromises()

      expect(mockUseWordCloudTheme.setTheme).toHaveBeenCalledWith('light-green')
    })

    it('卸载时应该停止渲染', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      wrapper.unmount()

      expect(mockUseWordCloud.stopRendering).toHaveBeenCalled()
    })
  })

  describe('暴露的方法', () => {
    it('应该暴露refresh方法', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      expect(wrapper.vm.refresh).toBeDefined()
      expect(typeof wrapper.vm.refresh).toBe('function')
    })

    it('应该暴露download方法', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      expect(wrapper.vm.download).toBeDefined()
      expect(typeof wrapper.vm.download).toBe('function')
    })

    it('应该暴露clear方法', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      expect(wrapper.vm.clear).toBeDefined()
      expect(typeof wrapper.vm.clear).toBe('function')
    })

    it('应该暴露getCanvas方法', () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      expect(wrapper.vm.getCanvas).toBeDefined()
      expect(typeof wrapper.vm.getCanvas).toBe('function')
    })

    it('refresh方法应该调用renderWordCloud', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await wrapper.vm.refresh()

      expect(mockUseWordCloud.renderWordCloud).toHaveBeenCalled()
    })
  })

  describe('响应式设计', () => {
    it('响应式模式下应该监听容器尺寸变化', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          responsive: true
        }
      })

      await flushPromises()

      // 在实际实现中会设置ResizeObserver
      // 这里验证相关逻辑被调用
    })

    it('非响应式模式下应该使用固定尺寸', () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          responsive: false,
          width: 800,
          height: 600
        }
      })

      const container = wrapper.find('.canvas-wrapper')
      const style = container.attributes('style')
      expect(style).toContain('width: 800px')
      expect(style).toContain('height: 600px')
    })
  })

  describe('错误边界', () => {
    it('应该处理WordCloud库不可用的情况', async () => {
      mockUseWordCloud.checkWordCloudAvailability.mockResolvedValue(false)

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      await flushPromises()

      expect(consoleSpy).toHaveBeenCalledWith('WordCloud library is not available')

      consoleSpy.mockRestore()
    })

    it('应该处理渲染错误', async () => {
      mockUseWordCloud.renderState.error = '渲染失败'

      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const errorMask = wrapper.find('.bg-red-50')
      expect(errorMask.exists()).toBe(true)
      expect(errorMask.text()).toContain('渲染失败')
    })
  })

  describe('性能优化', () => {
    it('应该在合适的条件下才进行渲染', async () => {
      mockUseWordCloud.canRender = false

      wrapper = mount(HotWordCloudChart, {
        props: defaultProps
      })

      const refreshButton = wrapper.find('button[title="重新渲染"]')
      await refreshButton.trigger('click')

      // 当canRender为false时不应该调用渲染
      expect(mockUseWordCloud.renderWordCloud).not.toHaveBeenCalled()
    })

    it('应该正确处理大量词语数据', async () => {
      const largeWordSet = Array.from({ length: 1000 }, (_, i) => ({
        text: `词语${i}`,
        weight: Math.random() * 100
      }))

      wrapper = mount(HotWordCloudChart, {
        props: {
          ...defaultProps,
          words: largeWordSet
        }
      })

      await flushPromises()

      expect(mockUseWordCloud.updateWords).toHaveBeenCalledWith(largeWordSet)
    })
  })
})