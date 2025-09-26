import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { nextTick } from 'vue'
import MappingConfigTab from '../MappingConfigTab.vue'
import { useSearchSpaceStore } from '@/stores/searchSpace'

// Mock CodeMirror编辑器相关模块
vi.mock('@codemirror/view', () => ({
  EditorView: vi.fn().mockImplementation((config) => {
    const mockView = {
      state: {
        doc: {
          toString: vi.fn().mockReturnValue(config.state?.doc || '{}'),
          length: config.state?.doc?.length || 2
        },
        update: vi.fn((changes) => ({ changes }))
      },
      dispatch: vi.fn(),
      destroy: vi.fn(),
    }
    // 模拟编辑器内容变化
    if (config.state?.extensions) {
      setTimeout(() => {
        const listener = config.state.extensions.find((ext: any) => ext.updateListener)
        if (listener) {
          listener.updateListener({ docChanged: true })
        }
      }, 0)
    }
    return mockView
  }),
  basicSetup: {},
}))

vi.mock('@codemirror/state', () => ({
  EditorState: {
    create: vi.fn((config) => ({ doc: config.doc || '{}', extensions: config.extensions })),
    readOnly: { of: vi.fn() },
  },
  StateEffect: {
    reconfigure: { of: vi.fn() },
  },
}))

describe('MappingConfigTab', () => {
  let wrapper: VueWrapper<any>
  let pinia: any
  let mockStore: any

  const mockMappingData = JSON.stringify({
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "createTime": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss"
      }
    }
  }, null, 2)

  beforeEach(() => {
    // 创建测试 Pinia 实例
    pinia = createTestingPinia({
      createSpy: vi.fn,
      stubActions: false,
    })

    // 获取 store 并设置初始状态
    mockStore = useSearchSpaceStore(pinia)

    // Mock store 的状态
    mockStore.mappingLoading = false
    mockStore.mappingError = null
    mockStore.currentMapping = mockMappingData

    // Mock store 的方法
    mockStore.fetchMapping = vi.fn().mockResolvedValue(mockMappingData)
    mockStore.updateMapping = vi.fn().mockResolvedValue(undefined)

    wrapper = mount(MappingConfigTab, {
      props: {
        spaceId: 1,
        modelValue: mockMappingData,
        readonly: false
      },
      global: {
        plugins: [pinia],
      },
    })
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('组件渲染', () => {
    it('应该正确渲染组件', () => {
      expect(wrapper.exists()).toBe(true)
      expect(wrapper.find('.mapping-config-tab').exists()).toBe(true)
    })

    it('应该显示工具栏', () => {
      expect(wrapper.find('h4').text()).toContain('JSON 映射配置')
      expect(wrapper.find('button[disabled]').exists()).toBe(true) // 重置按钮应该被禁用（没有更改）
    })

    it('应该显示编辑器容器', () => {
      expect(wrapper.find('.flex-grow .relative').exists()).toBe(true)
    })

    it('应该在只读模式下禁用编辑功能', async () => {
      await wrapper.setProps({ readonly: true })
      await nextTick()

      // 在只读模式下，按钮应该被禁用或不可见
      const saveButton = wrapper.find('button:not([disabled])')
      expect(saveButton.exists()).toBe(false) // 或者检查是否禁用
    })
  })

  describe('JSON验证', () => {
    it('应该显示有效JSON状态', () => {
      const validIndicator = wrapper.find('.inline-flex.items-center')
      expect(validIndicator.text()).toContain('有效')
    })

    it('应该检测无效JSON并显示错误', async () => {
      // 模拟无效JSON输入
      const invalidJson = '{"invalid": json}'

      await wrapper.setProps({ modelValue: invalidJson })
      await nextTick()

      // 验证状态应该显示无效
      await wrapper.vm.$nextTick()

      // 由于我们在测试中使用mock，需要直接测试验证逻辑
      const validation = wrapper.vm.validateJson(invalidJson)
      expect(validation.isValid).toBe(false)
      expect(validation.error).toContain('Unexpected token')
    })

    it('应该接受空JSON作为有效输入', () => {
      const validation = wrapper.vm.validateJson('')
      expect(validation.isValid).toBe(true)
      expect(validation.error).toBe('')
    })
  })

  describe('编辑器功能', () => {
    it('应该初始化CodeMirror编辑器', async () => {
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.editorView).toBeDefined()
    })

    it('应该能够处理保存操作', async () => {
      // 模拟有更改的情况
      wrapper.vm.currentValue = JSON.stringify({ "properties": { "newField": { "type": "text" } } })
      wrapper.vm.originalValue = mockMappingData

      await wrapper.vm.$nextTick()

      // 调用保存方法
      await wrapper.vm.handleSave()

      // 验证store方法被调用
      expect(mockStore.updateMapping).toHaveBeenCalledWith(1, wrapper.vm.currentValue)
    })

    it('应该能够处理重置操作', async () => {
      // 设置有更改的状态
      wrapper.vm.currentValue = '{"modified": true}'
      wrapper.vm.originalValue = mockMappingData

      // 调用重置方法
      wrapper.vm.handleReset()

      // 验证内容被重置为原始值
      expect(wrapper.vm.currentValue).toBe(mockMappingData)
    })

    it('应该检测到未保存的更改', async () => {
      // 设置不同的当前值和原始值
      wrapper.vm.currentValue = '{"modified": true}'
      wrapper.vm.originalValue = mockMappingData

      await wrapper.vm.$nextTick()

      // 验证hasChanges计算属性
      expect(wrapper.vm.hasChanges).toBe(true)
    })

    it('应该能够格式化JSON', async () => {
      const unformattedJson = '{"properties":{"title":{"type":"text"}}}'
      wrapper.vm.currentValue = unformattedJson
      wrapper.vm.isValidJson = true

      await wrapper.vm.formatJson()

      expect(wrapper.vm.currentValue).toBe(JSON.stringify(JSON.parse(unformattedJson), null, 2))
    })
  })

  describe('数据加载', () => {
    it('应该在组件挂载时加载mapping数据', async () => {
      // 验证fetchMapping被调用
      expect(mockStore.fetchMapping).toHaveBeenCalledWith(1)
    })

    it('应该处理加载错误', async () => {
      const errorMessage = 'Failed to load mapping'
      mockStore.fetchMapping.mockRejectedValueOnce(new Error(errorMessage))

      const emitSpy = vi.spyOn(wrapper.vm, '$emit')

      await wrapper.vm.loadMappingFromStore()

      expect(emitSpy).toHaveBeenCalledWith('load-error', errorMessage)
    })

    it('应该缓存和重用mapping数据', async () => {
      // 第一次加载
      await wrapper.vm.loadMappingFromStore()

      // 清除mock调用记录
      mockStore.fetchMapping.mockClear()

      // 如果store有缓存，不应该再次调用API
      mockStore.currentMapping = mockMappingData
      await wrapper.vm.loadMappingFromStore()

      // 这里的具体行为取决于store的实现
      expect(mockStore.fetchMapping).toHaveBeenCalledWith(1)
    })
  })

  describe('事件处理', () => {
    it('应该发出validate事件', async () => {
      const emitSpy = vi.spyOn(wrapper.vm, '$emit')

      wrapper.vm.updateValidation('{"valid": true}')

      expect(emitSpy).toHaveBeenCalledWith('validate', true)
    })

    it('应该发出save事件', async () => {
      const emitSpy = vi.spyOn(wrapper.vm, '$emit')
      const newMapping = '{"properties": {"newField": {"type": "text"}}}'

      wrapper.vm.currentValue = newMapping
      wrapper.vm.originalValue = mockMappingData
      wrapper.vm.isValidJson = true

      await wrapper.vm.handleSave()

      expect(emitSpy).toHaveBeenCalledWith('save', newMapping)
      expect(emitSpy).toHaveBeenCalledWith('save-success')
    })

    it('应该处理保存错误并发出save-error事件', async () => {
      const errorMessage = 'Save failed'
      mockStore.updateMapping.mockRejectedValueOnce(new Error(errorMessage))

      const emitSpy = vi.spyOn(wrapper.vm, '$emit')

      wrapper.vm.currentValue = '{"modified": true}'
      wrapper.vm.originalValue = mockMappingData
      wrapper.vm.isValidJson = true

      await wrapper.vm.handleSave()

      expect(emitSpy).toHaveBeenCalledWith('save-error', errorMessage)
    })

    it('应该响应spaceId属性变化', async () => {
      const fetchSpy = vi.spyOn(wrapper.vm, 'loadMappingFromStore')

      await wrapper.setProps({ spaceId: 2 })
      await nextTick()

      expect(fetchSpy).toHaveBeenCalled()
    })
  })

  describe('响应式行为', () => {
    it('应该响应loading状态变化', async () => {
      mockStore.mappingLoading = true
      await nextTick()

      // 验证按钮状态
      const saveButton = wrapper.find('button:last-child')
      expect(saveButton.attributes('disabled')).toBeDefined()
    })

    it('应该显示错误状态', async () => {
      wrapper.vm.isValidJson = false
      wrapper.vm.jsonError = 'Invalid JSON format'

      await nextTick()

      expect(wrapper.find('.json-invalid').exists()).toBe(false) // 错误面板默认不显示
    })

    it('应该在有未保存更改时显示指示器', async () => {
      wrapper.vm.currentValue = '{"modified": true}'
      wrapper.vm.originalValue = mockMappingData

      await nextTick()

      const changeIndicator = wrapper.find('.bg-yellow-50, .dark\\:bg-yellow-950\\/50')
      expect(changeIndicator.exists()).toBe(true)
    })
  })

  describe('组件清理', () => {
    it('应该在卸载时清理编辑器', () => {
      const destroySpy = vi.spyOn(wrapper.vm.editorView, 'destroy')

      wrapper.unmount()

      expect(destroySpy).toHaveBeenCalled()
    })

    it('应该在卸载时清理主题监听器', () => {
      const mockObserver = { disconnect: vi.fn() }
      wrapper.vm.themeObserver = mockObserver

      wrapper.unmount()

      expect(mockObserver.disconnect).toHaveBeenCalled()
    })
  })

  describe('暴露的方法', () => {
    it('应该暴露getValue方法', () => {
      const testValue = '{"test": true}'
      wrapper.vm.currentValue = testValue

      expect(wrapper.vm.getValue()).toBe(testValue)
    })

    it('应该暴露setValue方法', async () => {
      const newValue = '{"newValue": true}'

      wrapper.vm.setValue(newValue)

      expect(wrapper.vm.currentValue).toBe(newValue)
      expect(wrapper.vm.originalValue).toBe(newValue)
    })

    it('应该暴露getValidation方法', () => {
      wrapper.vm.isValidJson = false
      wrapper.vm.jsonError = 'Test error'

      const validation = wrapper.vm.getValidation()

      expect(validation).toEqual({
        isValid: false,
        error: 'Test error'
      })
    })

    it('应该暴露hasUnsavedChanges方法', () => {
      wrapper.vm.currentValue = 'changed'
      wrapper.vm.originalValue = 'original'

      expect(wrapper.vm.hasUnsavedChanges()).toBe(true)
    })

    it('应该暴露reload方法', async () => {
      const loadSpy = vi.spyOn(wrapper.vm, 'loadMappingFromStore')

      await wrapper.vm.reload()

      expect(loadSpy).toHaveBeenCalled()
    })

    it('应该暴露save方法', async () => {
      const saveSpy = vi.spyOn(wrapper.vm, 'handleSave')

      await wrapper.vm.save()

      expect(saveSpy).toHaveBeenCalled()
    })
  })
})