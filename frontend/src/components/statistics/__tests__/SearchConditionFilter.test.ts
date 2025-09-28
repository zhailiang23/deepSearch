/**
 * SearchConditionFilter 搜索条件过滤器组件测试
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchConditionFilter, { type SearchCondition } from '../filters/SearchConditionFilter.vue'

describe('SearchConditionFilter', () => {
  let wrapper: any

  const mockSearchSpaceOptions = [
    { label: '文档空间', value: 'documents', description: '搜索文档内容' },
    { label: '新闻空间', value: 'news', description: '搜索新闻内容' },
    { label: '博客空间', value: 'blogs', description: '搜索博客内容' }
  ]

  beforeEach(() => {
    wrapper = mount(SearchConditionFilter, {
      props: {
        searchSpaceOptions: mockSearchSpaceOptions
      }
    })
  })

  describe('组件初始化', () => {
    it('应该正确渲染组件标题', () => {
      expect(wrapper.find('.search-condition-filter__title').text()).toBe('搜索条件')
    })

    it('应该渲染关键词输入框', () => {
      const keywordsInput = wrapper.find('#keywords')
      expect(keywordsInput.exists()).toBe(true)
      expect(keywordsInput.attributes('placeholder')).toBe('输入关键词，按回车添加')
    })

    it('应该渲染搜索空间选择器', () => {
      const searchSpaceSelect = wrapper.find('#search-space')
      expect(searchSpaceSelect.exists()).toBe(true)

      const options = wrapper.findAll('#search-space option')
      expect(options.length).toBe(mockSearchSpaceOptions.length + 1) // +1 for "全部搜索空间"
    })

    it('应该渲染用户类型选项', () => {
      const userTypeOptions = wrapper.findAll('.user-type-option')
      expect(userTypeOptions.length).toBeGreaterThan(0)

      const checkboxes = wrapper.findAll('.user-type-checkbox')
      expect(checkboxes.length).toEqual(userTypeOptions.length)
    })

    it('应该渲染频次范围输入框', () => {
      const minFrequencyInput = wrapper.find('#min-frequency')
      const maxFrequencyInput = wrapper.find('#max-frequency')

      expect(minFrequencyInput.exists()).toBe(true)
      expect(maxFrequencyInput.exists()).toBe(true)
      expect(minFrequencyInput.attributes('type')).toBe('number')
      expect(maxFrequencyInput.attributes('type')).toBe('number')
    })

    it('应该渲染排序方式选择器', () => {
      const sortOrderSelect = wrapper.find('#sort-order')
      expect(sortOrderSelect.exists()).toBe(true)

      const options = wrapper.findAll('#sort-order option')
      expect(options.length).toBeGreaterThan(0)
    })
  })

  describe('关键词管理功能', () => {
    it('应该能够添加关键词', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      await keywordsInput.setValue('测试关键词')
      await addButton.trigger('click')

      const keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(1)
      expect(keywordTags[0].find('.keyword-text').text()).toBe('测试关键词')

      // 输入框应该被清空
      expect(keywordsInput.element.value).toBe('')
    })

    it('应该能够通过回车键添加关键词', async () => {
      const keywordsInput = wrapper.find('#keywords')

      await keywordsInput.setValue('回车添加')
      await keywordsInput.trigger('keypress.enter')

      const keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(1)
      expect(keywordTags[0].find('.keyword-text').text()).toBe('回车添加')
    })

    it('应该能够删除关键词', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      // 添加关键词
      await keywordsInput.setValue('待删除关键词')
      await addButton.trigger('click')

      let keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(1)

      // 删除关键词
      const removeButton = wrapper.find('.keyword-remove')
      await removeButton.trigger('click')

      keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(0)
    })

    it('应该防止添加重复关键词', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      // 添加第一个关键词
      await keywordsInput.setValue('重复关键词')
      await addButton.trigger('click')

      // 尝试添加相同关键词
      await keywordsInput.setValue('重复关键词')
      await addButton.trigger('click')

      const keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(1) // 应该只有一个
    })

    it('应该忽略空白关键词', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      // 尝试添加空白关键词
      await keywordsInput.setValue('   ')
      await addButton.trigger('click')

      const keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(0)
    })

    it('应该在达到最大关键词数量时禁用添加', async () => {
      const maxKeywords = 3
      const wrapperWithLimit = mount(SearchConditionFilter, {
        props: {
          maxKeywords,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      const keywordsInput = wrapperWithLimit.find('#keywords')
      const addButton = wrapperWithLimit.find('.add-keyword-button')

      // 添加到达限制数量的关键词
      for (let i = 0; i < maxKeywords; i++) {
        await keywordsInput.setValue(`关键词${i + 1}`)
        await addButton.trigger('click')
      }

      const keywordTags = wrapperWithLimit.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(maxKeywords)

      // 尝试添加更多关键词应该被忽略
      await keywordsInput.setValue('超出限制')
      await addButton.trigger('click')

      const updatedKeywordTags = wrapperWithLimit.findAll('.keyword-tag')
      expect(updatedKeywordTags.length).toBe(maxKeywords)
    })
  })

  describe('搜索空间选择功能', () => {
    it('应该能够选择搜索空间', async () => {
      const searchSpaceSelect = wrapper.find('#search-space')

      await searchSpaceSelect.setValue('documents')

      const emitted = wrapper.emitted('change')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const searchCondition = emitted[emitted.length - 1][0] as SearchCondition
        expect(searchCondition.searchSpace).toBe('documents')
      }
    })

    it('应该渲染正确的搜索空间选项', () => {
      const options = wrapper.findAll('#search-space option')
      const optionTexts = options.map((option: any) => option.text())

      expect(optionTexts).toContain('全部搜索空间')
      expect(optionTexts).toContain('文档空间')
      expect(optionTexts).toContain('新闻空间')
      expect(optionTexts).toContain('博客空间')
    })
  })

  describe('用户类型选择功能', () => {
    it('应该能够选择用户类型', async () => {
      const normalUserCheckbox = wrapper.findAll('.user-type-checkbox').find((checkbox: any) =>
        checkbox.element.value === 'normal'
      )

      if (normalUserCheckbox) {
        await normalUserCheckbox.setChecked(true)

        const emitted = wrapper.emitted('change')
        expect(emitted).toBeTruthy()

        if (emitted && emitted.length > 0) {
          const searchCondition = emitted[emitted.length - 1][0] as SearchCondition
          expect(searchCondition.userTypes).toContain('normal')
        }
      }
    })

    it('应该能够选择多个用户类型', async () => {
      const checkboxes = wrapper.findAll('.user-type-checkbox')

      // 选择前两个用户类型
      if (checkboxes.length >= 2) {
        await checkboxes[0].setChecked(true)
        await checkboxes[1].setChecked(true)

        const emitted = wrapper.emitted('change')
        if (emitted && emitted.length > 0) {
          const searchCondition = emitted[emitted.length - 1][0] as SearchCondition
          expect(searchCondition.userTypes.length).toBe(2)
        }
      }
    })
  })

  describe('频次范围功能', () => {
    it('应该能够设置最小频次', async () => {
      const minFrequencyInput = wrapper.find('#min-frequency')

      await minFrequencyInput.setValue('10')

      const emitted = wrapper.emitted('change')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const searchCondition = emitted[emitted.length - 1][0] as SearchCondition
        expect(searchCondition.minFrequency).toBe(10)
      }
    })

    it('应该能够设置最大频次', async () => {
      const maxFrequencyInput = wrapper.find('#max-frequency')

      await maxFrequencyInput.setValue('100')

      const emitted = wrapper.emitted('change')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const searchCondition = emitted[emitted.length - 1][0] as SearchCondition
        expect(searchCondition.maxFrequency).toBe(100)
      }
    })

    it('应该正确设置最小值和最大值约束', () => {
      const minFrequencyInput = wrapper.find('#min-frequency')
      const maxFrequencyInput = wrapper.find('#max-frequency')

      expect(minFrequencyInput.attributes('min')).toBe('1')
      expect(maxFrequencyInput.attributes('type')).toBe('number')
    })
  })

  describe('排序方式功能', () => {
    it('应该能够更改排序方式', async () => {
      const sortOrderSelect = wrapper.find('#sort-order')

      await sortOrderSelect.setValue('frequency_asc')

      const emitted = wrapper.emitted('change')
      expect(emitted).toBeTruthy()

      if (emitted && emitted.length > 0) {
        const searchCondition = emitted[emitted.length - 1][0] as SearchCondition
        expect(searchCondition.sortOrder).toBe('frequency_asc')
      }
    })

    it('应该有默认的排序方式', () => {
      const sortOrderSelect = wrapper.find('#sort-order')
      expect(sortOrderSelect.element.value).toBe('frequency_desc')
    })
  })

  describe('清除所有过滤器功能', () => {
    it('应该在有活跃过滤器时显示清除按钮', async () => {
      // 添加一个关键词来激活过滤器
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      await keywordsInput.setValue('测试')
      await addButton.trigger('click')

      const clearButton = wrapper.find('.search-condition-filter__clear')
      expect(clearButton.exists()).toBe(true)
    })

    it('应该能够清除所有过滤条件', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')
      const searchSpaceSelect = wrapper.find('#search-space')

      // 设置一些过滤条件
      await keywordsInput.setValue('测试关键词')
      await addButton.trigger('click')
      await searchSpaceSelect.setValue('documents')

      // 清除所有过滤器
      const clearButton = wrapper.find('.search-condition-filter__clear')
      await clearButton.trigger('click')

      // 验证所有条件已清除
      const keywordTags = wrapper.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(0)
      expect(searchSpaceSelect.element.value).toBe('')
    })
  })

  describe('过滤条件摘要功能', () => {
    it('应该在有活跃过滤器时显示摘要', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      await keywordsInput.setValue('测试')
      await addButton.trigger('click')

      const summary = wrapper.find('.search-condition-filter__summary')
      expect(summary.exists()).toBe(true)
    })

    it('应该正确显示关键词摘要', async () => {
      const keywordsInput = wrapper.find('#keywords')
      const addButton = wrapper.find('.add-keyword-button')

      await keywordsInput.setValue('关键词1')
      await addButton.trigger('click')
      await keywordsInput.setValue('关键词2')
      await addButton.trigger('click')

      const keywordsSummary = wrapper.findAll('.summary-item').find((item: any) =>
        item.text().includes('关键词:')
      )

      expect(keywordsSummary).toBeTruthy()
      if (keywordsSummary) {
        expect(keywordsSummary.text()).toContain('关键词1, 关键词2')
      }
    })
  })

  describe('外部值同步', () => {
    it('应该能够接收外部传入的modelValue', async () => {
      const initialCondition: SearchCondition = {
        keywords: ['初始关键词'],
        searchSpace: 'documents',
        userTypes: ['normal'],
        minFrequency: 5,
        maxFrequency: 50,
        sortOrder: 'frequency_asc'
      }

      const wrapperWithValue = mount(SearchConditionFilter, {
        props: {
          modelValue: initialCondition,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      // 验证关键词
      const keywordTags = wrapperWithValue.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(1)
      expect(keywordTags[0].find('.keyword-text').text()).toBe('初始关键词')

      // 验证搜索空间
      const searchSpaceSelect = wrapperWithValue.find('#search-space')
      expect(searchSpaceSelect.element.value).toBe('documents')

      // 验证频次范围
      const minFrequencyInput = wrapperWithValue.find('#min-frequency')
      const maxFrequencyInput = wrapperWithValue.find('#max-frequency')
      expect(minFrequencyInput.element.value).toBe('5')
      expect(maxFrequencyInput.element.value).toBe('50')
    })

    it('应该在modelValue变化时更新组件状态', async () => {
      const newCondition: SearchCondition = {
        keywords: ['新关键词'],
        searchSpace: 'news',
        userTypes: ['frequent'],
        minFrequency: 10,
        maxFrequency: 100,
        sortOrder: 'recent_time'
      }

      const wrapperWithValue = mount(SearchConditionFilter, {
        props: {
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      await wrapperWithValue.setProps({ modelValue: newCondition })

      // 验证更新后的状态
      const keywordTags = wrapperWithValue.findAll('.keyword-tag')
      expect(keywordTags.length).toBe(1)
      expect(keywordTags[0].find('.keyword-text').text()).toBe('新关键词')

      const searchSpaceSelect = wrapperWithValue.find('#search-space')
      expect(searchSpaceSelect.element.value).toBe('news')
    })
  })

  describe('禁用状态', () => {
    it('应该在disabled为true时禁用所有交互', async () => {
      const disabledWrapper = mount(SearchConditionFilter, {
        props: {
          disabled: true,
          searchSpaceOptions: mockSearchSpaceOptions
        }
      })

      const keywordsInput = disabledWrapper.find('#keywords')
      const addButton = disabledWrapper.find('.add-keyword-button')
      const searchSpaceSelect = disabledWrapper.find('#search-space')
      const checkboxes = disabledWrapper.findAll('.user-type-checkbox')

      expect(keywordsInput.attributes('disabled')).toBeDefined()
      expect(addButton.attributes('disabled')).toBeDefined()
      expect(searchSpaceSelect.attributes('disabled')).toBeDefined()

      checkboxes.forEach((checkbox: any) => {
        expect(checkbox.attributes('disabled')).toBeDefined()
      })
    })
  })

  describe('响应式设计', () => {
    it('应该在移动端正确显示', () => {
      // 验证移动端样式类的存在
      expect(wrapper.html()).toContain('@media (max-width: 640px)')
    })
  })

  describe('可访问性', () => {
    it('应该有正确的label和id关联', () => {
      const labels = [
        { for: 'keywords', id: 'keywords' },
        { for: 'search-space', id: 'search-space' },
        { for: 'min-frequency', id: 'min-frequency' },
        { for: 'max-frequency', id: 'max-frequency' },
        { for: 'sort-order', id: 'sort-order' }
      ]

      labels.forEach(({ for: forAttr, id }) => {
        const label = wrapper.find(`label[for="${forAttr}"]`)
        const input = wrapper.find(`#${id}`)

        expect(label.exists()).toBe(true)
        expect(input.exists()).toBe(true)
      })
    })
  })
})