/**
 * HotWordLimitSelector 热词数量限制选择器组件测试
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import HotWordLimitSelector, { type LimitConfig } from '../filters/HotWordLimitSelector.vue'

describe('HotWordLimitSelector', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(HotWordLimitSelector)
  })

  describe('组件初始化', () => {
    it('应该正确渲染组件标题', () => {
      expect(wrapper.find('.hot-word-limit-selector__title').text()).toBe('显示数量')
    })

    it('应该默认选择50条', () => {
      const activeButton = wrapper.find('.limit-button--active')
      expect(activeButton.exists()).toBe(true)
      expect(activeButton.text()).toContain('50条')
    })

    it('应该显示当前选择的数量', () => {
      const limitDisplay = wrapper.find('.limit-display')
      expect(limitDisplay.exists()).toBe(true)
      expect(limitDisplay.text()).toContain('前 50 条')
    })

    it('应该渲染所有预设选项', () => {
      const presetButtons = wrapper.findAll('.limit-button')
      expect(presetButtons.length).toBeGreaterThanOrEqual(6) // 10, 20, 50, 100, 200, 500条

      const buttonTexts = presetButtons.map((btn: any) => btn.text())
      expect(buttonTexts.some(text => text.includes('10条'))).toBe(true)
      expect(buttonTexts.some(text => text.includes('20条'))).toBe(true)
      expect(buttonTexts.some(text => text.includes('50条'))).toBe(true)
      expect(buttonTexts.some(text => text.includes('100条'))).toBe(true)
    })

    it('应该渲染显示模式选项', () => {
      const displayModeOptions = wrapper.findAll('.display-mode-option')
      expect(displayModeOptions.length).toBe(3) // table, card, compact

      const radioButtons = wrapper.findAll('.display-mode-radio')
      expect(radioButtons.length).toBe(3)
    })

    it('应该默认选择表格显示模式', () => {
      const tableRadio = wrapper.findAll('.display-mode-radio').find((radio: any) =>
        radio.element.value === 'table'
      )
      expect(tableRadio.element.checked).toBe(true)
    })
  })

  describe('预设数量选择功能', () => {
    it('应该能够选择不同的预设数量', async () => {
      const hundredButton = wrapper.findAll('.limit-button').find((btn: any) =>
        btn.text().includes('100条')
      )

      if (hundredButton) {
        await hundredButton.trigger('click')

        expect(wrapper.find('.limit-button--active').text()).toContain('100条')

        const emitted = wrapper.emitted('change')
        expect(emitted).toBeTruthy()

        if (emitted && emitted.length > 0) {
          const limitConfig = emitted[emitted.length - 1][0] as LimitConfig
          expect(limitConfig.limit).toBe(100)
        }
      }
    })

    it('应该在选择预设时更新显示数量', async () => {
      const twentyButton = wrapper.findAll('.limit-button').find((btn: any) =>
        btn.text().includes('20条')
      )

      if (twentyButton) {
        await twentyButton.trigger('click')

        const limitDisplay = wrapper.find('.limit-display')
        expect(limitDisplay.text()).toContain('前 20 条')
      }
    })

    it('应该在选择预设时触发change事件', async () => {
      const tenButton = wrapper.findAll('.limit-button').find((btn: any) =>
        btn.text().includes('10条')
      )

      if (tenButton) {
        await tenButton.trigger('click')

        const emitted = wrapper.emitted('change')
        expect(emitted).toBeTruthy()

        if (emitted && emitted.length > 0) {
          const limitConfig = emitted[emitted.length - 1][0] as LimitConfig
          expect(limitConfig.limit).toBe(10)
          expect(limitConfig.displayMode).toBe('table')
        }
      }
    })
  })

  describe('自定义数量功能', () => {
    it('应该能够输入自定义数量', async () => {
      const customInput = wrapper.find('#custom-limit')
      expect(customInput.exists()).toBe(true)

      await customInput.setValue('75')

      expect(customInput.element.value).toBe('75')
    })

    it('应该验证自定义数量的有效性', async () => {
      const customInput = wrapper.find('#custom-limit')
      const applyButton = wrapper.find('.apply-custom-button')

      // 测试有效值
      await customInput.setValue('75')
      await customInput.trigger('input')

      expect(applyButton.attributes('disabled')).toBeUndefined()

      // 测试无效值（超出范围）
      await customInput.setValue('2000')
      await customInput.trigger('input')

      const errorMessage = wrapper.find('.error-message')
      expect(errorMessage.exists()).toBe(true)
      expect(errorMessage.text()).toContain('数量不能大于')
    })

    it('应该能够应用有效的自定义数量', async () => {
      const customInput = wrapper.find('#custom-limit')
      const applyButton = wrapper.find('.apply-custom-button')

      await customInput.setValue('75')
      await applyButton.trigger('click')

      const limitDisplay = wrapper.find('.limit-display')
      expect(limitDisplay.text()).toContain('前 75 条')

      const emitted = wrapper.emitted('change')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const limitConfig = emitted[emitted.length - 1][0] as LimitConfig
        expect(limitConfig.limit).toBe(75)
      }
    })

    it('应该通过回车键应用自定义数量', async () => {
      const customInput = wrapper.find('#custom-limit')

      await customInput.setValue('85')
      await customInput.trigger('keypress.enter')

      const limitDisplay = wrapper.find('.limit-display')
      expect(limitDisplay.text()).toContain('前 85 条')
    })

    it('应该拒绝非整数值', async () => {
      const customInput = wrapper.find('#custom-limit')

      await customInput.setValue('50.5')
      await customInput.trigger('input')

      const errorMessage = wrapper.find('.error-message')
      expect(errorMessage.exists()).toBe(true)
      expect(errorMessage.text()).toContain('请输入整数')
    })

    it('应该拒绝超出范围的值', async () => {
      const customInput = wrapper.find('#custom-limit')

      // 测试小于最小值
      await customInput.setValue('1')
      await customInput.trigger('input')

      let errorMessage = wrapper.find('.error-message')
      expect(errorMessage.exists()).toBe(true)
      expect(errorMessage.text()).toContain('数量不能小于')

      // 测试大于最大值
      await customInput.setValue('2000')
      await customInput.trigger('input')

      errorMessage = wrapper.find('.error-message')
      expect(errorMessage.exists()).toBe(true)
      expect(errorMessage.text()).toContain('数量不能大于')
    })
  })

  describe('显示模式功能', () => {
    it('应该能够切换显示模式', async () => {
      const cardRadio = wrapper.findAll('.display-mode-radio').find((radio: any) =>
        radio.element.value === 'card'
      )

      if (cardRadio) {
        await cardRadio.setChecked(true)

        const emitted = wrapper.emitted('change')
        expect(emitted).toBeTruthy()

        if (emitted && emitted.length > 0) {
          const limitConfig = emitted[emitted.length - 1][0] as LimitConfig
          expect(limitConfig.displayMode).toBe('card')
        }
      }
    })

    it('应该为每个显示模式提供描述', () => {
      const descriptions = wrapper.findAll('.display-mode-description')
      expect(descriptions.length).toBe(3)

      const descriptionTexts = descriptions.map((desc: any) => desc.text())
      expect(descriptionTexts).toContain('表格形式显示')
      expect(descriptionTexts).toContain('卡片形式显示')
      expect(descriptionTexts).toContain('紧凑列表显示')
    })
  })

  describe('性能警告功能', () => {
    it('应该在数量超过推荐值时显示警告', async () => {
      const fiveHundredButton = wrapper.findAll('.limit-button').find((btn: any) =>
        btn.text().includes('500条')
      )

      if (fiveHundredButton) {
        await fiveHundredButton.trigger('click')

        const warning = wrapper.find('.hot-word-limit-selector__warning')
        expect(warning.exists()).toBe(true)
        expect(warning.text()).toContain('显示过多数据可能影响页面性能')
      }
    })

    it('应该在数量不超过推荐值时隐藏警告', async () => {
      const fiftyButton = wrapper.findAll('.limit-button').find((btn: any) =>
        btn.text().includes('50条')
      )

      if (fiftyButton) {
        await fiftyButton.trigger('click')

        const warning = wrapper.find('.hot-word-limit-selector__warning')
        expect(warning.exists()).toBe(false)
      }
    })
  })

  describe('数据量预估功能', () => {
    it('应该在有预估数据时显示数据量信息', async () => {
      const wrapperWithEstimate = mount(HotWordLimitSelector, {
        props: {
          estimatedDataSize: 1024 * 1024 // 1MB
        }
      })

      const estimate = wrapperWithEstimate.find('.hot-word-limit-selector__estimate')
      expect(estimate.exists()).toBe(true)
      expect(estimate.text()).toContain('预计数据量:')
      expect(estimate.text()).toContain('MB')
    })

    it('应该正确格式化数据大小', () => {
      const wrapperWithLargeData = mount(HotWordLimitSelector, {
        props: {
          estimatedDataSize: 2 * 1024 * 1024 * 1024 // 2GB
        }
      })

      const estimate = wrapperWithLargeData.find('.estimate-value')
      expect(estimate.text()).toContain('GB')
    })
  })

  describe('快速操作功能', () => {
    it('应该渲染快速操作按钮', () => {
      const quickActions = wrapper.findAll('.quick-action')
      expect(quickActions.length).toBe(3) // 标准视图、详细视图、精简视图

      const actionTexts = quickActions.map((action: any) => action.text())
      expect(actionTexts).toContain('标准视图 (50)')
      expect(actionTexts).toContain('详细视图 (100)')
      expect(actionTexts).toContain('精简视图 (20)')
    })

    it('应该能够通过快速操作切换数量', async () => {
      const detailedViewButton = wrapper.findAll('.quick-action').find((btn: any) =>
        btn.text().includes('详细视图')
      )

      if (detailedViewButton) {
        await detailedViewButton.trigger('click')

        const limitDisplay = wrapper.find('.limit-display')
        expect(limitDisplay.text()).toContain('前 100 条')
      }
    })

    it('应该高亮当前选择的快速操作', async () => {
      const compactViewButton = wrapper.findAll('.quick-action').find((btn: any) =>
        btn.text().includes('精简视图')
      )

      if (compactViewButton) {
        await compactViewButton.trigger('click')

        expect(compactViewButton.classes()).toContain('quick-action--active')
      }
    })
  })

  describe('外部值同步', () => {
    it('应该能够接收外部传入的modelValue', async () => {
      const initialConfig: LimitConfig = {
        limit: 75,
        displayMode: 'card'
      }

      const wrapperWithValue = mount(HotWordLimitSelector, {
        props: {
          modelValue: initialConfig
        }
      })

      const limitDisplay = wrapperWithValue.find('.limit-display')
      expect(limitDisplay.text()).toContain('前 75 条')

      const cardRadio = wrapperWithValue.findAll('.display-mode-radio').find((radio: any) =>
        radio.element.value === 'card'
      )
      expect(cardRadio.element.checked).toBe(true)
    })

    it('应该在modelValue变化时更新组件状态', async () => {
      const newConfig: LimitConfig = {
        limit: 200,
        displayMode: 'compact'
      }

      const wrapperWithValue = mount(HotWordLimitSelector)

      await wrapperWithValue.setProps({ modelValue: newConfig })

      const limitDisplay = wrapperWithValue.find('.limit-display')
      expect(limitDisplay.text()).toContain('前 200 条')

      const compactRadio = wrapperWithValue.findAll('.display-mode-radio').find((radio: any) =>
        radio.element.value === 'compact'
      )
      expect(compactRadio.element.checked).toBe(true)
    })
  })

  describe('禁用状态', () => {
    it('应该在disabled为true时禁用所有交互', async () => {
      const disabledWrapper = mount(HotWordLimitSelector, {
        props: {
          disabled: true
        }
      })

      const limitButtons = disabledWrapper.findAll('.limit-button')
      limitButtons.forEach((button: any) => {
        expect(button.attributes('disabled')).toBeDefined()
      })

      const customInput = disabledWrapper.find('#custom-limit')
      expect(customInput.attributes('disabled')).toBeDefined()

      const displayModeRadios = disabledWrapper.findAll('.display-mode-radio')
      displayModeRadios.forEach((radio: any) => {
        expect(radio.attributes('disabled')).toBeDefined()
      })

      const quickActions = disabledWrapper.findAll('.quick-action')
      quickActions.forEach((action: any) => {
        expect(action.attributes('disabled')).toBeDefined()
      })
    })
  })

  describe('自定义限制参数', () => {
    it('应该接受自定义的最小值和最大值', () => {
      const customWrapper = mount(HotWordLimitSelector, {
        props: {
          minLimit: 10,
          maxLimit: 500,
          recommendedLimit: 200
        }
      })

      const customInput = customWrapper.find('#custom-limit')
      expect(customInput.attributes('min')).toBe('10')
      expect(customInput.attributes('max')).toBe('500')

      const rangeDisplay = customWrapper.find('.custom-limit-range')
      expect(rangeDisplay.text()).toContain('(10 - 500)')
    })

    it('应该根据自定义推荐值显示警告', async () => {
      const customWrapper = mount(HotWordLimitSelector, {
        props: {
          recommendedLimit: 30
        }
      })

      // 选择超过自定义推荐值的数量
      const fiftyButton = customWrapper.findAll('.limit-button').find((btn: any) =>
        btn.text().includes('50条')
      )

      if (fiftyButton) {
        await fiftyButton.trigger('click')

        const warning = customWrapper.find('.hot-word-limit-selector__warning')
        expect(warning.exists()).toBe(true)
        expect(warning.text()).toContain('建议控制在30条以内')
      }
    })
  })

  describe('响应式设计', () => {
    it('应该在移动端正确显示', () => {
      expect(wrapper.html()).toContain('md:grid-cols-3')
      expect(wrapper.html()).toContain('@media (max-width: 640px)')
    })
  })

  describe('可访问性', () => {
    it('应该有正确的label和id关联', () => {
      const customLimitLabel = wrapper.find('label[for="custom-limit"]')
      const customLimitInput = wrapper.find('#custom-limit')

      expect(customLimitLabel.exists()).toBe(true)
      expect(customLimitInput.exists()).toBe(true)
    })

    it('应该为显示模式选项提供正确的label', () => {
      const displayModeOptions = wrapper.findAll('.display-mode-option')
      displayModeOptions.forEach((option: any) => {
        const radio = option.find('.display-mode-radio')
        const text = option.find('.display-mode-text')
        expect(radio.exists()).toBe(true)
        expect(text.exists()).toBe(true)
      })
    })
  })
})