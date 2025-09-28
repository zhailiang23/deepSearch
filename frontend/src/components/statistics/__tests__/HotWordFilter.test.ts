/**
 * HotWordFilter 热词过滤器主组件测试
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import HotWordFilter, { type HotWordFilterData, type AdvancedOptions, type FilterPreset } from '../HotWordFilter.vue'

// Mock 子组件
vi.mock('../filters/TimeRangeSelector.vue', () => ({
  default: {
    name: 'TimeRangeSelector',
    template: '<div class="mock-time-range-selector">TimeRangeSelector</div>',
    props: ['modelValue', 'disabled'],
    emits: ['update:modelValue', 'change']
  }
}))

vi.mock('../filters/SearchConditionFilter.vue', () => ({
  default: {
    name: 'SearchConditionFilter',
    template: '<div class="mock-search-condition-filter">SearchConditionFilter</div>',
    props: ['modelValue', 'disabled', 'searchSpaceOptions'],
    emits: ['update:modelValue', 'change']
  }
}))

vi.mock('../filters/HotWordLimitSelector.vue', () => ({
  default: {
    name: 'HotWordLimitSelector',
    template: '<div class="mock-hot-word-limit-selector">HotWordLimitSelector</div>',
    props: ['modelValue', 'disabled', 'estimatedDataSize'],
    emits: ['update:modelValue', 'change']
  }
}))

describe('HotWordFilter', () => {
  let wrapper: any

  const mockSearchSpaceOptions = [
    { label: '文档空间', value: 'documents' },
    { label: '新闻空间', value: 'news' }
  ]

  const mockFilterPresets: FilterPreset[] = [
    {
      id: 'preset-1',
      name: '热门关键词',
      description: '搜索频次最高的关键词',
      filters: {
        timeRange: null,
        searchCondition: {
          keywords: [],
          userTypes: ['frequent'],
          sortOrder: 'frequency_desc'
        },
        limitConfig: {
          limit: 50,
          displayMode: 'table'
        }
      },
      advancedOptions: {
        useCache: true,
        realTimeUpdate: false,
        exportFormat: 'excel'
      }
    }
  ]

  beforeEach(() => {
    wrapper = mount(HotWordFilter, {
      props: {
        searchSpaceOptions: mockSearchSpaceOptions,
        filterPresets: mockFilterPresets
      }
    })
  })

  describe('组件初始化', () => {
    it('应该正确渲染组件标题', () => {
      expect(wrapper.find('.filter-title').text()).toBe('热词过滤器')
    })

    it('应该渲染所有子组件', () => {
      expect(wrapper.find('.mock-time-range-selector').exists()).toBe(true)
      expect(wrapper.find('.mock-search-condition-filter').exists()).toBe(true)
      expect(wrapper.find('.mock-hot-word-limit-selector').exists()).toBe(true)
    })

    it('应该渲染操作按钮', () => {
      const resetButton = wrapper.find('.reset-button')
      const collapseButton = wrapper.find('.collapse-button')
      const applyButton = wrapper.find('.apply-button')

      expect(resetButton.exists()).toBe(true)
      expect(collapseButton.exists()).toBe(true)
      expect(applyButton.exists()).toBe(true)
    })

    it('应该渲染过滤器网格布局', () => {
      const filterGrid = wrapper.find('.filter-grid')
      expect(filterGrid.exists()).toBe(true)

      const filterSections = wrapper.findAll('.filter-section')
      expect(filterSections.length).toBe(3) // 时间范围、搜索条件、数量限制
    })

    it('应该默认展开过滤器内容', () => {
      const filterContent = wrapper.find('.hot-word-filter__content')
      expect(filterContent.isVisible()).toBe(true)
    })
  })

  describe('折叠/展开功能', () => {
    it('应该能够折叠过滤器内容', async () => {
      const collapseButton = wrapper.find('.collapse-button')

      await collapseButton.trigger('click')

      expect(collapseButton.text()).toBe('展开')

      const filterContent = wrapper.find('.hot-word-filter__content')
      expect(filterContent.isVisible()).toBe(false)
    })

    it('应该能够展开已折叠的过滤器内容', async () => {
      const collapseButton = wrapper.find('.collapse-button')

      // 先折叠
      await collapseButton.trigger('click')
      expect(collapseButton.text()).toBe('展开')

      // 再展开
      await collapseButton.trigger('click')
      expect(collapseButton.text()).toBe('收起')

      const filterContent = wrapper.find('.hot-word-filter__content')
      expect(filterContent.isVisible()).toBe(true)
    })
  })

  describe('过滤器状态管理', () => {
    it('应该能够检测是否有活跃的过滤器', async () => {
      // 初始状态应该没有活跃过滤器
      const resetButton = wrapper.find('.reset-button')
      expect(resetButton.attributes('disabled')).toBeDefined()

      // 模拟设置过滤条件
      await wrapper.setData({
        filterData: {
          timeRange: {
            start: new Date(),
            end: new Date(),
            label: '测试范围'
          },
          searchCondition: null,
          limitConfig: null
        }
      })

      // 现在应该有活跃过滤器
      expect(resetButton.attributes('disabled')).toBeUndefined()
    })

    it('应该在有活跃过滤器时显示摘要', async () => {
      // 折叠过滤器
      const collapseButton = wrapper.find('.collapse-button')
      await collapseButton.trigger('click')

      // 设置过滤条件
      await wrapper.setData({
        filterData: {
          timeRange: {
            start: new Date(),
            end: new Date(),
            label: '最近7天'
          },
          searchCondition: {
            keywords: ['测试'],
            userTypes: [],
            sortOrder: 'frequency_desc'
          },
          limitConfig: {
            limit: 100,
            displayMode: 'table'
          }
        }
      })

      const filterSummary = wrapper.find('.filter-summary')
      expect(filterSummary.exists()).toBe(true)

      const summaryTags = wrapper.findAll('.summary-tag')
      expect(summaryTags.length).toBeGreaterThan(0)
    })
  })

  describe('重置功能', () => {
    it('应该能够重置所有过滤器', async () => {
      // 先设置一些过滤条件
      await wrapper.setData({
        filterData: {
          timeRange: {
            start: new Date(),
            end: new Date(),
            label: '测试范围'
          },
          searchCondition: {
            keywords: ['测试'],
            userTypes: ['normal'],
            sortOrder: 'frequency_desc'
          },
          limitConfig: {
            limit: 100,
            displayMode: 'card'
          }
        }
      })

      const resetButton = wrapper.find('.reset-button')
      await resetButton.trigger('click')

      // 验证reset事件被触发
      const emitted = wrapper.emitted('reset')
      expect(emitted).toBeTruthy()

      // 验证数据被重置
      expect(wrapper.vm.filterData.timeRange).toBeNull()
      expect(wrapper.vm.filterData.searchCondition).toBeNull()
      expect(wrapper.vm.filterData.limitConfig).toBeNull()
    })
  })

  describe('应用过滤器功能', () => {
    it('应该能够应用过滤器设置', async () => {
      const applyButton = wrapper.find('.apply-button')

      await applyButton.trigger('click')

      const emitted = wrapper.emitted('apply')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const [filterData, advancedOptions] = emitted[emitted.length - 1]
        expect(filterData).toBeDefined()
        expect(advancedOptions).toBeDefined()
      }
    })

    it('应该在加载状态时显示加载指示器', async () => {
      const loadingWrapper = mount(HotWordFilter, {
        props: {
          loading: true,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      const applyButton = loadingWrapper.find('.apply-button')
      expect(applyButton.text()).toContain('查询中...')

      const loadingSpinner = loadingWrapper.find('.loading-spinner')
      expect(loadingSpinner.exists()).toBe(true)
    })

    it('应该在加载状态时禁用交互', async () => {
      const loadingWrapper = mount(HotWordFilter, {
        props: {
          loading: true,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      const resetButton = loadingWrapper.find('.reset-button')
      const applyButton = loadingWrapper.find('.apply-button')

      expect(resetButton.attributes('disabled')).toBeDefined()
      expect(applyButton.attributes('disabled')).toBeDefined()
    })
  })

  describe('高级选项功能', () => {
    it('应该能够切换高级选项显示', async () => {
      const advancedToggle = wrapper.find('.advanced-toggle-button')

      await advancedToggle.trigger('click')

      const advancedOptions = wrapper.find('.advanced-options')
      expect(advancedOptions.exists()).toBe(true)
    })

    it('应该在showAdvancedOptions为true时默认显示高级选项', () => {
      const advancedWrapper = mount(HotWordFilter, {
        props: {
          showAdvancedOptions: true,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      const advancedOptions = advancedWrapper.find('.advanced-options')
      expect(advancedOptions.exists()).toBe(true)
    })
  })

  describe('预设过滤器功能', () => {
    it('应该渲染预设过滤器列表', () => {
      const filterPresets = wrapper.find('.filter-presets')
      expect(filterPresets.exists()).toBe(true)

      const presetButtons = wrapper.findAll('.preset-button')
      expect(presetButtons.length).toBe(mockFilterPresets.length)

      const presetButton = presetButtons[0]
      expect(presetButton.find('.preset-name').text()).toBe('热门关键词')
      expect(presetButton.find('.preset-description').text()).toBe('搜索频次最高的关键词')
    })

    it('应该能够加载预设过滤器', async () => {
      const presetButton = wrapper.find('.preset-button')

      await presetButton.trigger('click')

      // 验证过滤器数据被更新
      expect(wrapper.vm.filterData.searchCondition.userTypes).toContain('frequent')
      expect(wrapper.vm.filterData.limitConfig.limit).toBe(50)

      // 验证高级选项被更新
      expect(wrapper.vm.advancedOptions.useCache).toBe(true)
      expect(wrapper.vm.advancedOptions.exportFormat).toBe('excel')
    })

    it('应该在autoApply为true时自动应用预设', async () => {
      const autoApplyWrapper = mount(HotWordFilter, {
        props: {
          autoApply: true,
          searchSpaceOptions: mockSearchSpaceOptions,
          filterPresets: mockFilterPresets
        }
      })

      const presetButton = autoApplyWrapper.find('.preset-button')
      await presetButton.trigger('click')

      // 应该触发apply事件
      const emitted = autoApplyWrapper.emitted('apply')
      expect(emitted).toBeTruthy()
    })
  })

  describe('保存预设功能', () => {
    it('应该能够保存当前过滤器为预设', async () => {
      // 设置一些过滤条件
      await wrapper.setData({
        filterData: {
          timeRange: {
            start: new Date(),
            end: new Date(),
            label: '测试范围'
          },
          searchCondition: {
            keywords: ['新预设'],
            userTypes: ['normal'],
            sortOrder: 'frequency_desc'
          },
          limitConfig: {
            limit: 75,
            displayMode: 'card'
          }
        }
      })

      const savePresetButton = wrapper.find('.save-preset-button')
      await savePresetButton.trigger('click')

      const emitted = wrapper.emitted('save-preset')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const [filterData, advancedOptions] = emitted[emitted.length - 1]
        expect(filterData.searchCondition.keywords).toContain('新预设')
        expect(filterData.limitConfig.limit).toBe(75)
      }
    })

    it('应该在没有活跃过滤器时禁用保存按钮', () => {
      const savePresetButton = wrapper.find('.save-preset-button')
      expect(savePresetButton.attributes('disabled')).toBeDefined()
    })
  })

  describe('外部值同步', () => {
    it('应该能够接收外部传入的modelValue', async () => {
      const filterData: HotWordFilterData = {
        timeRange: {
          start: new Date('2023-01-01'),
          end: new Date('2023-01-07'),
          label: '初始范围'
        },
        searchCondition: {
          keywords: ['初始关键词'],
          userTypes: ['normal'],
          sortOrder: 'frequency_desc'
        },
        limitConfig: {
          limit: 100,
          displayMode: 'table'
        }
      }

      const wrapperWithValue = mount(HotWordFilter, {
        props: {
          modelValue: filterData,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      expect(wrapperWithValue.vm.filterData.timeRange.label).toBe('初始范围')
      expect(wrapperWithValue.vm.filterData.searchCondition.keywords).toContain('初始关键词')
      expect(wrapperWithValue.vm.filterData.limitConfig.limit).toBe(100)
    })

    it('应该在modelValue变化时更新组件状态', async () => {
      const newFilterData: HotWordFilterData = {
        timeRange: {
          start: new Date('2023-02-01'),
          end: new Date('2023-02-07'),
          label: '新范围'
        },
        searchCondition: {
          keywords: ['新关键词'],
          userTypes: ['frequent'],
          sortOrder: 'recent_time'
        },
        limitConfig: {
          limit: 200,
          displayMode: 'card'
        }
      }

      await wrapper.setProps({ modelValue: newFilterData })

      expect(wrapper.vm.filterData.timeRange.label).toBe('新范围')
      expect(wrapper.vm.filterData.searchCondition.keywords).toContain('新关键词')
      expect(wrapper.vm.filterData.limitConfig.limit).toBe(200)
    })
  })

  describe('自动应用功能', () => {
    it('应该在autoApply为true时自动应用过滤器变化', async () => {
      const autoApplyWrapper = mount(HotWordFilter, {
        props: {
          autoApply: true,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      // 模拟子组件触发change事件
      await autoApplyWrapper.vm.handleTimeRangeChange({
        start: new Date(),
        end: new Date(),
        label: '测试范围'
      })

      const emitted = autoApplyWrapper.emitted('apply')
      expect(emitted).toBeTruthy()
    })
  })

  describe('禁用状态', () => {
    it('应该将禁用状态传递给子组件', () => {
      const disabledWrapper = mount(HotWordFilter, {
        props: {
          loading: true,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      // 由于我们mock了子组件，这里检查props是否正确传递
      const timeRangeSelector = disabledWrapper.findComponent({ name: 'TimeRangeSelector' })
      const searchConditionFilter = disabledWrapper.findComponent({ name: 'SearchConditionFilter' })
      const hotWordLimitSelector = disabledWrapper.findComponent({ name: 'HotWordLimitSelector' })

      expect(timeRangeSelector.props('disabled')).toBe(true)
      expect(searchConditionFilter.props('disabled')).toBe(true)
      expect(hotWordLimitSelector.props('disabled')).toBe(true)
    })
  })

  describe('响应式设计', () => {
    it('应该在移动端正确显示', () => {
      expect(wrapper.html()).toContain('lg:grid-cols-3')
      expect(wrapper.html()).toContain('@media (max-width: 1024px)')
      expect(wrapper.html()).toContain('@media (max-width: 640px)')
    })
  })

  describe('可访问性', () => {
    it('应该有正确的语义化标签', () => {
      const title = wrapper.find('.filter-title')
      expect(title.element.tagName).toBe('H3')

      const advancedTitle = wrapper.find('.advanced-title')
      expect(advancedTitle.element.tagName).toBe('H4')
    })

    it('应该有正确的按钮角色', () => {
      const buttons = wrapper.findAll('button')
      buttons.forEach((button: any) => {
        expect(button.attributes('type')).toBe('button')
      })
    })
  })

  describe('动画效果', () => {
    it('应该应用正确的CSS动画类', () => {
      expect(wrapper.html()).toContain('transition')
      expect(wrapper.html()).toContain('duration-200')
      expect(wrapper.html()).toContain('ease-out')
    })
  })
})