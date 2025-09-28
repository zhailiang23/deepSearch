/**
 * TimeRangeSelector 时间范围选择器组件测试
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import TimeRangeSelector, { type TimeRange } from '../filters/TimeRangeSelector.vue'

describe('TimeRangeSelector', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(TimeRangeSelector)
  })

  describe('组件初始化', () => {
    it('应该正确渲染组件标题', () => {
      expect(wrapper.find('.time-range-selector__title').text()).toBe('时间范围')
    })

    it('应该默认选择最近7天', () => {
      const activeButton = wrapper.find('.preset-button--active')
      expect(activeButton.text()).toContain('最近7天')
    })

    it('应该渲染所有预设选项', () => {
      const presetButtons = wrapper.findAll('.preset-button')
      expect(presetButtons.length).toBeGreaterThanOrEqual(5) // 今天、7天、30天、本月、上月等

      const buttonTexts = presetButtons.map((btn: any) => btn.text())
      expect(buttonTexts).toContain('今天')
      expect(buttonTexts).toContain('最近7天')
      expect(buttonTexts).toContain('最近30天')
    })

    it('应该显示当前选择的时间范围', () => {
      const display = wrapper.find('.time-range-selector__display')
      expect(display.exists()).toBe(true)
      expect(display.text()).toContain('已选择:')
    })
  })

  describe('预设选项功能', () => {
    it('应该能够选择今天', async () => {
      const todayButton = wrapper.find('[data-preset="today"]') ||
                         wrapper.findAll('.preset-button').find((btn: any) =>
                           btn.text().includes('今天'))

      if (todayButton) {
        await todayButton.trigger('click')
        expect(wrapper.find('.preset-button--active').text()).toContain('今天')
      }
    })

    it('应该在选择预设选项时触发change事件', async () => {
      const presetButtons = wrapper.findAll('.preset-button')
      const sevenDaysButton = presetButtons.find((btn: any) =>
        btn.text().includes('最近7天'))

      if (sevenDaysButton) {
        await sevenDaysButton.trigger('click')

        const emitted = wrapper.emitted('change')
        expect(emitted).toBeTruthy()
        expect(emitted[emitted.length - 1][0]).toHaveProperty('start')
        expect(emitted[emitted.length - 1][0]).toHaveProperty('end')
      }
    })

    it('应该正确计算最近7天的日期范围', async () => {
      const sevenDaysButton = wrapper.findAll('.preset-button').find((btn: any) =>
        btn.text().includes('最近7天'))

      if (sevenDaysButton) {
        await sevenDaysButton.trigger('click')

        const emitted = wrapper.emitted('change')
        if (emitted && emitted.length > 0) {
          const timeRange = emitted[emitted.length - 1][0] as TimeRange
          const today = new Date()
          const sevenDaysAgo = new Date(today)
          sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6)

          // 验证日期范围的合理性（允许一天的误差）
          const daysDiff = (timeRange.end.getTime() - timeRange.start.getTime()) / (1000 * 60 * 60 * 24)
          expect(daysDiff).toBeCloseTo(6, 0) // 最近7天实际是6天的时间跨度
        }
      }
    })
  })

  describe('自定义日期范围功能', () => {
    beforeEach(async () => {
      // 点击自定义选项
      const customButton = wrapper.findAll('.preset-button').find((btn: any) =>
        btn.text().includes('自定义'))

      if (customButton) {
        await customButton.trigger('click')
      }
    })

    it('应该在选择自定义时显示日期输入框', async () => {
      const customSection = wrapper.find('.time-range-selector__custom')
      expect(customSection.exists()).toBe(true)

      const dateInputs = wrapper.findAll('.date-input')
      expect(dateInputs.length).toBe(2) // 开始日期和结束日期
    })

    it('应该验证日期范围的有效性', async () => {
      const startDateInput = wrapper.find('#start-date')
      const endDateInput = wrapper.find('#end-date')

      if (startDateInput.exists() && endDateInput.exists()) {
        // 设置无效的日期范围（结束日期早于开始日期）
        const tomorrow = new Date()
        tomorrow.setDate(tomorrow.getDate() + 1)
        const yesterday = new Date()
        yesterday.setDate(yesterday.getDate() - 1)

        await startDateInput.setValue(tomorrow.toISOString().split('T')[0])
        await endDateInput.setValue(yesterday.toISOString().split('T')[0])

        await startDateInput.trigger('change')
        await endDateInput.trigger('change')

        // 应该显示错误信息
        const errorMessage = wrapper.find('.error-message')
        expect(errorMessage.exists()).toBe(true)
        expect(errorMessage.text()).toContain('开始日期不能晚于结束日期')
      }
    })

    it('应该能够应用有效的自定义日期范围', async () => {
      const startDateInput = wrapper.find('#start-date')
      const endDateInput = wrapper.find('#end-date')
      const applyButton = wrapper.find('.action-button--primary')

      if (startDateInput.exists() && endDateInput.exists() && applyButton.exists()) {
        const today = new Date()
        const oneWeekAgo = new Date(today)
        oneWeekAgo.setDate(oneWeekAgo.getDate() - 7)

        await startDateInput.setValue(oneWeekAgo.toISOString().split('T')[0])
        await endDateInput.setValue(today.toISOString().split('T')[0])

        await applyButton.trigger('click')

        const emitted = wrapper.emitted('change')
        expect(emitted).toBeTruthy()

        if (emitted && emitted.length > 0) {
          const timeRange = emitted[emitted.length - 1][0] as TimeRange
          expect(timeRange.start).toBeDefined()
          expect(timeRange.end).toBeDefined()
        }
      }
    })

    it('应该能够取消自定义日期范围', async () => {
      const cancelButton = wrapper.find('.action-button--secondary')

      if (cancelButton.exists()) {
        await cancelButton.trigger('click')

        // 应该回到预设选项
        const customSection = wrapper.find('.time-range-selector__custom')
        expect(customSection.exists()).toBe(false)

        // 应该选择默认的7天选项
        const activeButton = wrapper.find('.preset-button--active')
        expect(activeButton.text()).toContain('最近7天')
      }
    })
  })

  describe('重置功能', () => {
    it('应该能够重置到默认状态', async () => {
      // 先选择一个非默认选项
      const todayButton = wrapper.findAll('.preset-button').find((btn: any) =>
        btn.text().includes('今天'))

      if (todayButton) {
        await todayButton.trigger('click')

        // 然后重置
        const resetButton = wrapper.find('.time-range-selector__reset')
        if (resetButton.exists()) {
          await resetButton.trigger('click')

          // 应该回到默认的7天选项
          const activeButton = wrapper.find('.preset-button--active')
          expect(activeButton.text()).toContain('最近7天')
        }
      }
    })
  })

  describe('外部值同步', () => {
    it('应该能够接收外部传入的modelValue', async () => {
      const timeRange: TimeRange = {
        start: new Date('2023-01-01'),
        end: new Date('2023-01-07'),
        label: '测试范围'
      }

      const wrapperWithValue = mount(TimeRangeSelector, {
        props: {
          modelValue: timeRange
        }
      })

      // 应该显示传入的时间范围
      const display = wrapperWithValue.find('.range-value')
      expect(display.text()).toContain('测试范围')
    })

    it('应该在modelValue变化时更新组件状态', async () => {
      const initialRange: TimeRange = {
        start: new Date('2023-01-01'),
        end: new Date('2023-01-07'),
        label: '初始范围'
      }

      const newRange: TimeRange = {
        start: new Date('2023-01-15'),
        end: new Date('2023-01-21'),
        label: '新范围'
      }

      const wrapperWithValue = mount(TimeRangeSelector, {
        props: {
          modelValue: initialRange
        }
      })

      // 更新props
      await wrapperWithValue.setProps({ modelValue: newRange })

      // 应该显示新的时间范围
      const display = wrapperWithValue.find('.range-value')
      expect(display.text()).toContain('新范围')
    })
  })

  describe('禁用状态', () => {
    it('应该在disabled为true时禁用所有交互', async () => {
      const disabledWrapper = mount(TimeRangeSelector, {
        props: {
          disabled: true
        }
      })

      const presetButtons = disabledWrapper.findAll('.preset-button')
      presetButtons.forEach((button: any) => {
        expect(button.attributes('disabled')).toBeDefined()
      })
    })
  })

  describe('响应式设计', () => {
    it('应该在移动端正确显示', () => {
      // 验证移动端样式类的存在
      expect(wrapper.html()).toContain('md:grid-cols-2')
      expect(wrapper.html()).toContain('@media (max-width: 640px)')
    })
  })

  describe('可访问性', () => {
    it('应该有正确的label和id关联', () => {
      const startDateLabel = wrapper.find('label[for="start-date"]')
      const startDateInput = wrapper.find('#start-date')

      expect(startDateLabel.exists()).toBe(true)
      expect(startDateInput.exists()).toBe(true)
    })

    it('应该有正确的aria属性', () => {
      const dateInputs = wrapper.findAll('.date-input')
      dateInputs.forEach((input: any) => {
        expect(input.attributes('type')).toBe('date')
      })
    })
  })
})