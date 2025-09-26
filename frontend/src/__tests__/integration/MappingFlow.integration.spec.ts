import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { nextTick } from 'vue'
import MappingConfigTab from '@/components/searchSpace/MappingConfigTab.vue'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import { mappingApi } from '@/api/mappingApi'
import type { ApiResponse } from '@/types/searchSpace'

// Mock HTTP 客户端
vi.mock('@/utils/http', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  }
}))

// Mock MappingApi - 真实模拟 HTTP 交互
vi.mock('@/api/mappingApi', () => ({
  mappingApi: {
    getMapping: vi.fn(),
    updateMapping: vi.fn(),
  }
}))

// Mock CodeMirror
vi.mock('@codemirror/view', () => ({
  EditorView: vi.fn().mockImplementation(() => ({
    state: {
      doc: {
        toString: vi.fn().mockReturnValue('{}'),
        length: 2
      }
    },
    dispatch: vi.fn(),
    destroy: vi.fn(),
  })),
  basicSetup: {},
}))

vi.mock('@codemirror/state', () => ({
  EditorState: {
    create: vi.fn(() => ({ doc: '{}', extensions: [] })),
    readOnly: { of: vi.fn() },
  },
  StateEffect: {
    reconfigure: { of: vi.fn() },
  },
}))

describe('Mapping 配置完整流程集成测试', () => {
  let wrapper: VueWrapper<any>
  let pinia: any
  let store: any
  const mockedMappingApi = mappingApi as any

  const createMockApiResponse = <T>(data: T, success = true): ApiResponse<T> => ({
    success,
    message: success ? '操作成功' : '操作失败',
    data,
    timestamp: new Date().toISOString()
  })

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

    // 重置所有 mock
    vi.clearAllMocks()

    // 挂载组件
    wrapper = mount(MappingConfigTab, {
      props: {
        spaceId: 1,
        modelValue: '{}',
        readonly: false
      },
      global: {
        plugins: [pinia],
      },
    })

    store = useSearchSpaceStore(pinia)
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('完整的映射配置编辑流程', () => {
    it('应该完成从加载到保存的完整流程', async () => {
      const spaceId = 1

      // 1. 组件加载时获取初始映射配置
      const initialResponse = createMockApiResponse('{}')
      mockedMappingApi.getMapping.mockResolvedValueOnce(initialResponse)

      // 等待组件完成初始化
      await nextTick()

      // 验证初始加载
      expect(mockedMappingApi.getMapping).toHaveBeenCalledWith(spaceId)
      expect(store.currentMapping).toBe('{}')

      // 2. 模拟用户编辑映射配置
      const newMapping = mockMappingData
      wrapper.vm.currentValue = newMapping
      wrapper.vm.originalValue = '{}'

      await nextTick()

      // 验证有未保存的更改
      expect(wrapper.vm.hasChanges).toBe(true)

      // 3. 用户保存配置
      const saveResponse = createMockApiResponse(undefined)
      mockedMappingApi.updateMapping.mockResolvedValueOnce(saveResponse)

      await wrapper.vm.handleSave()

      // 验证保存操作
      expect(mockedMappingApi.updateMapping).toHaveBeenCalledWith(spaceId, newMapping)
      expect(store.currentMapping).toBe(newMapping)

      // 4. 验证保存后状态
      expect(wrapper.vm.hasChanges).toBe(false)
      expect(wrapper.emitted('save')).toBeTruthy()
      expect(wrapper.emitted('save-success')).toBeTruthy()
    })

    it('应该处理加载错误并允许重新加载', async () => {
      const spaceId = 1
      const errorMessage = 'Network error'

      // 1. 模拟初始加载失败
      mockedMappingApi.getMapping.mockRejectedValueOnce(new Error(errorMessage))

      // 触发加载
      await wrapper.vm.loadMappingFromStore().catch(() => {})

      // 验证错误状态
      expect(store.mappingError).toBe(errorMessage)
      expect(wrapper.emitted('load-error')).toBeTruthy()

      // 2. 用户重新尝试加载
      const retryResponse = createMockApiResponse(mockMappingData)
      mockedMappingApi.getMapping.mockResolvedValueOnce(retryResponse)

      await wrapper.vm.reload()

      // 验证重新加载成功
      expect(store.currentMapping).toBe(mockMappingData)
      expect(store.mappingError).toBeNull()
    })

    it('应该处理保存错误并保持编辑状态', async () => {
      const spaceId = 1

      // 设置初始状态
      wrapper.vm.currentValue = mockMappingData
      wrapper.vm.originalValue = '{}'
      wrapper.vm.isValidJson = true

      // 模拟保存失败
      const errorMessage = 'Validation failed'
      mockedMappingApi.updateMapping.mockRejectedValueOnce(new Error(errorMessage))

      await wrapper.vm.handleSave()

      // 验证错误处理
      expect(wrapper.emitted('save-error')).toBeTruthy()
      expect(wrapper.emitted('save-error')![0][0]).toBe(errorMessage)

      // 验证编辑状态保持
      expect(wrapper.vm.hasChanges).toBe(true)
      expect(wrapper.vm.currentValue).toBe(mockMappingData)
    })
  })

  describe('并发操作处理', () => {
    it('应该正确处理多个并发的获取请求', async () => {
      const spaceId = 1
      const response = createMockApiResponse(mockMappingData)

      // 模拟 API 延迟响应
      mockedMappingApi.getMapping.mockImplementation(() =>
        new Promise(resolve => setTimeout(() => resolve(response), 100))
      )

      // 发起多个并发请求
      const promises = [
        store.fetchMapping(spaceId),
        store.fetchMapping(spaceId),
        store.fetchMapping(spaceId)
      ]

      const results = await Promise.all(promises)

      // 验证所有请求都返回相同结果
      expect(results).toEqual([mockMappingData, mockMappingData, mockMappingData])

      // 验证缓存机制（根据 store 实现可能只调用一次）
      expect(mockedMappingApi.getMapping).toHaveBeenCalled()
    })

    it('应该处理保存过程中的加载状态', async () => {
      const spaceId = 1

      wrapper.vm.currentValue = mockMappingData
      wrapper.vm.originalValue = '{}'
      wrapper.vm.isValidJson = true

      // 模拟长时间保存操作
      let resolvePromise: (value: any) => void
      const longSavePromise = new Promise((resolve) => {
        resolvePromise = resolve
      })
      mockedMappingApi.updateMapping.mockReturnValueOnce(longSavePromise)

      // 开始保存操作
      const savePromise = wrapper.vm.handleSave()

      // 验证加载状态
      expect(store.mappingLoading).toBe(true)

      // 解析保存操作
      resolvePromise!(createMockApiResponse(undefined))
      await savePromise

      // 验证加载状态重置
      expect(store.mappingLoading).toBe(false)
    })
  })

  describe('复杂映射配置场景', () => {
    it('应该处理大型映射配置的编辑流程', async () => {
      const spaceId = 1

      // 创建大型映射配置
      const largeMapping = JSON.stringify({
        properties: Object.fromEntries(
          Array.from({ length: 100 }, (_, i) => [
            `field${i}`,
            { type: 'text', analyzer: 'ik_max_word' }
          ])
        )
      }, null, 2)

      // 1. 加载大型配置
      const loadResponse = createMockApiResponse(largeMapping)
      mockedMappingApi.getMapping.mockResolvedValueOnce(loadResponse)

      await store.fetchMapping(spaceId)

      expect(store.currentMapping).toBe(largeMapping)
      expect(store.currentMapping!.length).toBeGreaterThan(1000)

      // 2. 更新大型配置
      const updateResponse = createMockApiResponse(undefined)
      mockedMappingApi.updateMapping.mockResolvedValueOnce(updateResponse)

      await store.updateMapping(spaceId, largeMapping)

      expect(mockedMappingApi.updateMapping).toHaveBeenCalledWith(spaceId, largeMapping)
    })

    it('应该处理特殊字符映射配置', async () => {
      const spaceId = 1

      const specialMapping = JSON.stringify({
        properties: {
          "title-中文": { type: "text", analyzer: "ik_max_word" },
          "field.with.dots": { type: "keyword" },
          "field_with_emoji_🚀": { type: "text" },
          "field@special#chars": { type: "keyword" }
        }
      }, null, 2)

      // 1. 保存特殊字符配置
      const saveResponse = createMockApiResponse(undefined)
      mockedMappingApi.updateMapping.mockResolvedValueOnce(saveResponse)

      wrapper.vm.currentValue = specialMapping
      wrapper.vm.originalValue = '{}'
      wrapper.vm.isValidJson = true

      await wrapper.vm.handleSave()

      expect(mockedMappingApi.updateMapping).toHaveBeenCalledWith(spaceId, specialMapping)

      // 2. 重新加载验证
      const loadResponse = createMockApiResponse(specialMapping)
      mockedMappingApi.getMapping.mockResolvedValueOnce(loadResponse)

      await wrapper.vm.reload()

      expect(wrapper.vm.currentValue).toBe(specialMapping)
    })
  })

  describe('JSON 验证与格式化集成', () => {
    it('应该在保存前验证JSON并拒绝无效配置', async () => {
      const invalidJson = '{ "invalid": json syntax }'

      wrapper.vm.currentValue = invalidJson
      wrapper.vm.originalValue = '{}'

      // 更新验证状态（模拟编辑器验证）
      wrapper.vm.updateValidation(invalidJson)

      await nextTick()

      // 验证无效JSON被检测
      expect(wrapper.vm.isValidJson).toBe(false)

      // 尝试保存应该被阻止（取决于组件实现）
      await wrapper.vm.handleSave()

      // 验证API未被调用
      expect(mockedMappingApi.updateMapping).not.toHaveBeenCalled()
    })

    it('应该支持JSON格式化功能', async () => {
      const unformattedJson = '{"properties":{"title":{"type":"text"}}}'
      const formattedJson = JSON.stringify(JSON.parse(unformattedJson), null, 2)

      wrapper.vm.currentValue = unformattedJson
      wrapper.vm.isValidJson = true

      await wrapper.vm.formatJson()

      expect(wrapper.vm.currentValue).toBe(formattedJson)
    })
  })

  describe('缓存和性能', () => {
    it('应该利用缓存避免重复的API调用', async () => {
      const spaceId = 1
      const response = createMockApiResponse(mockMappingData)

      // 第一次调用
      mockedMappingApi.getMapping.mockResolvedValueOnce(response)
      await store.fetchMapping(spaceId)

      expect(mockedMappingApi.getMapping).toHaveBeenCalledTimes(1)
      expect(store.mappingCache.get(spaceId)).toBe(mockMappingData)

      // 第二次调用应该使用缓存
      mockedMappingApi.getMapping.mockClear()
      await store.fetchMapping(spaceId)

      // 验证API未被再次调用（使用缓存）
      expect(mockedMappingApi.getMapping).not.toHaveBeenCalled()
      expect(store.currentMapping).toBe(mockMappingData)
    })

    it('应该在更新后清理相关缓存', async () => {
      const spaceId = 1

      // 先加载数据到缓存
      const loadResponse = createMockApiResponse(mockMappingData)
      mockedMappingApi.getMapping.mockResolvedValueOnce(loadResponse)
      await store.fetchMapping(spaceId)

      expect(store.mappingCache.has(spaceId)).toBe(true)

      // 更新配置
      const newMapping = '{"properties": {"updated": {"type": "text"}}}'
      const updateResponse = createMockApiResponse(undefined)
      mockedMappingApi.updateMapping.mockResolvedValueOnce(updateResponse)

      await store.updateMapping(spaceId, newMapping)

      // 验证缓存已更新
      expect(store.mappingCache.get(spaceId)).toBe(newMapping)
      expect(store.currentMapping).toBe(newMapping)
    })
  })

  describe('错误恢复场景', () => {
    it('应该从网络错误中恢复并重试', async () => {
      const spaceId = 1

      // 第一次尝试失败
      mockedMappingApi.getMapping.mockRejectedValueOnce(new Error('Network Error'))

      await expect(store.fetchMapping(spaceId)).rejects.toThrow('Network Error')
      expect(store.mappingError).toBe('Network Error')

      // 第二次尝试成功
      const successResponse = createMockApiResponse(mockMappingData)
      mockedMappingApi.getMapping.mockResolvedValueOnce(successResponse)

      const result = await store.fetchMapping(spaceId)

      expect(result).toBe(mockMappingData)
      expect(store.mappingError).toBeNull()
      expect(store.currentMapping).toBe(mockMappingData)
    })

    it('应该处理API响应格式错误', async () => {
      const spaceId = 1

      // 模拟格式错误的响应
      const malformedResponse = { success: false, message: '服务器错误', data: null }
      mockedMappingApi.getMapping.mockRejectedValueOnce(malformedResponse)

      await expect(store.fetchMapping(spaceId)).rejects.toEqual(malformedResponse)
      expect(store.mappingError).toBe('[object Object]')
    })
  })

  describe('用户交互场景', () => {
    it('应该支持重置操作并确认用户意图', async () => {
      const originalValue = '{}'
      const modifiedValue = mockMappingData

      // 设置修改状态
      wrapper.vm.originalValue = originalValue
      wrapper.vm.currentValue = modifiedValue

      expect(wrapper.vm.hasChanges).toBe(true)

      // 执行重置
      wrapper.vm.handleReset()

      // 验证重置结果
      expect(wrapper.vm.currentValue).toBe(originalValue)
      expect(wrapper.vm.hasChanges).toBe(false)
    })

    it('应该在切换搜索空间时重新加载映射配置', async () => {
      const spaceId1 = 1
      const spaceId2 = 2
      const mapping1 = '{"properties": {"space1": {"type": "text"}}}'
      const mapping2 = '{"properties": {"space2": {"type": "text"}}}'

      // 加载第一个搜索空间
      mockedMappingApi.getMapping.mockResolvedValueOnce(createMockApiResponse(mapping1))

      await wrapper.setProps({ spaceId: spaceId1 })
      await nextTick()

      expect(mockedMappingApi.getMapping).toHaveBeenCalledWith(spaceId1)

      // 切换到第二个搜索空间
      mockedMappingApi.getMapping.mockResolvedValueOnce(createMockApiResponse(mapping2))

      await wrapper.setProps({ spaceId: spaceId2 })
      await nextTick()

      expect(mockedMappingApi.getMapping).toHaveBeenCalledWith(spaceId2)
    })
  })
})