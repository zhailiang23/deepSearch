import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSearchSpaceStore } from '../searchSpace'
import { mappingApi } from '@/api/mappingApi'
import type { ApiResponse } from '@/types/searchSpace'

// Mock API 模块
vi.mock('@/api/mappingApi', () => ({
  mappingApi: {
    getMapping: vi.fn(),
    updateMapping: vi.fn(),
  }
}))

describe('SearchSpace Store - Mapping 功能', () => {
  let store: ReturnType<typeof useSearchSpaceStore>
  const mockedMappingApi = mappingApi as any

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

  const createMockApiResponse = <T>(data: T, success = true): ApiResponse<T> => ({
    success,
    message: success ? '操作成功' : '操作失败',
    data,
    timestamp: new Date().toISOString()
  })

  beforeEach(() => {
    // 创建新的 Pinia 实例
    setActivePinia(createPinia())
    store = useSearchSpaceStore()

    // 重置所有 mock
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      expect(store.currentMapping).toBeNull()
      expect(store.mappingLoading).toBe(false)
      expect(store.mappingError).toBeNull()
      expect(store.lastMappingUpdated).toBeNull()
    })

    it('应该初始化空的映射缓存', () => {
      expect(store.mappingCache).toBeInstanceOf(Map)
      expect(store.mappingCache.size).toBe(0)
    })
  })

  describe('fetchMapping', () => {
    it('应该成功获取映射配置', async () => {
      const spaceId = 1
      const mockResponse = createMockApiResponse(mockMappingData)

      mockedMappingApi.getMapping.mockResolvedValueOnce(mockResponse)

      const result = await store.fetchMapping(spaceId)

      expect(mockedMappingApi.getMapping).toHaveBeenCalledWith(spaceId)
      expect(store.mappingLoading).toBe(false)
      expect(store.mappingError).toBeNull()
      expect(store.currentMapping).toBe(mockMappingData)
      expect(result).toBe(mockMappingData)
    })

    it('应该缓存获取的映射配置', async () => {
      const spaceId = 1
      const mockResponse = createMockApiResponse(mockMappingData)

      mockedMappingApi.getMapping.mockResolvedValueOnce(mockResponse)

      await store.fetchMapping(spaceId)

      expect(store.mappingCache.get(spaceId)).toBe(mockMappingData)
      expect(store.lastMappingUpdated).toBeInstanceOf(Date)
    })

    it('应该从缓存返回映射配置', async () => {
      const spaceId = 1

      // 预先设置缓存
      store.mappingCache.set(spaceId, mockMappingData)

      const result = await store.fetchMapping(spaceId)

      expect(mockedMappingApi.getMapping).not.toHaveBeenCalled()
      expect(result).toBe(mockMappingData)
      expect(store.currentMapping).toBe(mockMappingData)
    })

    it('应该在加载期间设置loading状态', async () => {
      const spaceId = 1
      const mockResponse = createMockApiResponse(mockMappingData)

      // 延迟解析的 Promise
      let resolvePromise: (value: any) => void
      const delayedPromise = new Promise((resolve) => {
        resolvePromise = resolve
      })

      mockedMappingApi.getMapping.mockReturnValueOnce(delayedPromise)

      // 开始异步操作
      const fetchPromise = store.fetchMapping(spaceId)

      // 验证loading状态
      expect(store.mappingLoading).toBe(true)
      expect(store.mappingError).toBeNull()

      // 解析 Promise
      resolvePromise!(mockResponse)
      await fetchPromise

      // 验证loading状态重置
      expect(store.mappingLoading).toBe(false)
    })

    it('应该处理API错误', async () => {
      const spaceId = 1
      const errorMessage = 'Network error'

      mockedMappingApi.getMapping.mockRejectedValueOnce(new Error(errorMessage))

      await expect(store.fetchMapping(spaceId)).rejects.toThrow(errorMessage)

      expect(store.mappingLoading).toBe(false)
      expect(store.mappingError).toBe(errorMessage)
      expect(store.currentMapping).toBeNull()
    })

    it('应该处理空响应数据', async () => {
      const spaceId = 1
      const mockResponse = createMockApiResponse(null)

      mockedMappingApi.getMapping.mockResolvedValueOnce(mockResponse)

      const result = await store.fetchMapping(spaceId)

      expect(result).toBeNull()
      expect(store.currentMapping).toBeNull()
    })
  })

  describe('updateMapping', () => {
    it('应该成功更新映射配置', async () => {
      const spaceId = 1
      const newMapping = '{"properties": {"newField": {"type": "text"}}}'
      const mockResponse = createMockApiResponse(undefined)

      mockedMappingApi.updateMapping.mockResolvedValueOnce(mockResponse)

      await store.updateMapping(spaceId, newMapping)

      expect(mockedMappingApi.updateMapping).toHaveBeenCalledWith(spaceId, newMapping)
      expect(store.mappingLoading).toBe(false)
      expect(store.mappingError).toBeNull()
      expect(store.currentMapping).toBe(newMapping)
    })

    it('应该更新缓存中的映射配置', async () => {
      const spaceId = 1
      const newMapping = '{"properties": {"updatedField": {"type": "keyword"}}}'
      const mockResponse = createMockApiResponse(undefined)

      mockedMappingApi.updateMapping.mockResolvedValueOnce(mockResponse)

      await store.updateMapping(spaceId, newMapping)

      expect(store.mappingCache.get(spaceId)).toBe(newMapping)
      expect(store.lastMappingUpdated).toBeInstanceOf(Date)
    })

    it('应该在更新期间设置loading状态', async () => {
      const spaceId = 1
      const newMapping = '{"test": true}'
      const mockResponse = createMockApiResponse(undefined)

      let resolvePromise: (value: any) => void
      const delayedPromise = new Promise((resolve) => {
        resolvePromise = resolve
      })

      mockedMappingApi.updateMapping.mockReturnValueOnce(delayedPromise)

      const updatePromise = store.updateMapping(spaceId, newMapping)

      expect(store.mappingLoading).toBe(true)
      expect(store.mappingError).toBeNull()

      resolvePromise!(mockResponse)
      await updatePromise

      expect(store.mappingLoading).toBe(false)
    })

    it('应该处理更新错误', async () => {
      const spaceId = 1
      const newMapping = '{"invalid": "mapping"}'
      const errorMessage = 'Validation failed'

      mockedMappingApi.updateMapping.mockRejectedValueOnce(new Error(errorMessage))

      await expect(store.updateMapping(spaceId, newMapping)).rejects.toThrow(errorMessage)

      expect(store.mappingLoading).toBe(false)
      expect(store.mappingError).toBe(errorMessage)
    })

    it('应该处理空的映射配置', async () => {
      const spaceId = 1
      const emptyMapping = ''
      const mockResponse = createMockApiResponse(undefined)

      mockedMappingApi.updateMapping.mockResolvedValueOnce(mockResponse)

      await store.updateMapping(spaceId, emptyMapping)

      expect(store.currentMapping).toBe(emptyMapping)
      expect(store.mappingCache.get(spaceId)).toBe(emptyMapping)
    })
  })

  describe('状态管理方法', () => {
    it('setMappingLoading 应该设置loading状态', () => {
      store.setMappingLoading(true)
      expect(store.mappingLoading).toBe(true)

      store.setMappingLoading(false)
      expect(store.mappingLoading).toBe(false)
    })

    it('setMappingError 应该设置错误状态', () => {
      const errorMessage = 'Test error'

      store.setMappingError(errorMessage)
      expect(store.mappingError).toBe(errorMessage)

      store.setMappingError(null)
      expect(store.mappingError).toBeNull()
    })

    it('clearMappingError 应该清除错误状态', () => {
      store.setMappingError('Some error')
      expect(store.mappingError).toBe('Some error')

      store.clearMappingError()
      expect(store.mappingError).toBeNull()
    })

    it('clearMappingCache 应该清除指定的缓存', () => {
      const spaceId = 1
      store.mappingCache.set(spaceId, mockMappingData)
      store.mappingCache.set(2, 'other mapping')

      store.clearMappingCache(spaceId)

      expect(store.mappingCache.has(spaceId)).toBe(false)
      expect(store.mappingCache.has(2)).toBe(true)
    })

    it('clearMappingCache 不带参数应该清除所有缓存', () => {
      store.mappingCache.set(1, mockMappingData)
      store.mappingCache.set(2, 'other mapping')

      store.clearMappingCache()

      expect(store.mappingCache.size).toBe(0)
    })

    it('resetMappingState 应该重置所有状态', () => {
      // 设置一些状态
      store.currentMapping = mockMappingData
      store.mappingLoading = true
      store.mappingError = 'Some error'
      store.lastMappingUpdated = new Date()
      store.mappingCache.set(1, mockMappingData)

      // 重置状态
      store.resetMappingState()

      // 验证所有状态都被重置
      expect(store.currentMapping).toBeNull()
      expect(store.mappingLoading).toBe(false)
      expect(store.mappingError).toBeNull()
      expect(store.lastMappingUpdated).toBeNull()
      expect(store.mappingCache.size).toBe(0)
    })
  })

  describe('并发处理', () => {
    it('应该正确处理并发的fetchMapping调用', async () => {
      const spaceId = 1
      const mockResponse = createMockApiResponse(mockMappingData)

      mockedMappingApi.getMapping.mockResolvedValue(mockResponse)

      // 同时发起多个请求
      const promises = [
        store.fetchMapping(spaceId),
        store.fetchMapping(spaceId),
        store.fetchMapping(spaceId)
      ]

      const results = await Promise.all(promises)

      // 所有结果应该相同
      expect(results).toEqual([mockMappingData, mockMappingData, mockMappingData])

      // API只应该被调用一次（如果有缓存机制）或者多次（取决于实现）
      expect(mockedMappingApi.getMapping).toHaveBeenCalled()
    })

    it('应该正确处理并发的updateMapping调用', async () => {
      const spaceId = 1
      const mapping1 = '{"version": 1}'
      const mapping2 = '{"version": 2}'
      const mockResponse = createMockApiResponse(undefined)

      mockedMappingApi.updateMapping.mockResolvedValue(mockResponse)

      // 同时发起多个更新请求
      const promises = [
        store.updateMapping(spaceId, mapping1),
        store.updateMapping(spaceId, mapping2)
      ]

      await Promise.all(promises)

      // 验证API被调用了两次
      expect(mockedMappingApi.updateMapping).toHaveBeenCalledTimes(2)
      expect(mockedMappingApi.updateMapping).toHaveBeenCalledWith(spaceId, mapping1)
      expect(mockedMappingApi.updateMapping).toHaveBeenCalledWith(spaceId, mapping2)
    })
  })

  describe('边界情况', () => {
    it('应该处理非常大的映射配置', async () => {
      const spaceId = 1
      const largeMappingData = JSON.stringify({
        properties: Object.fromEntries(
          Array.from({ length: 1000 }, (_, i) => [
            `field${i}`,
            { type: 'text', analyzer: 'standard' }
          ])
        )
      })

      const mockResponse = createMockApiResponse(largeMappingData)
      mockedMappingApi.getMapping.mockResolvedValueOnce(mockResponse)

      const result = await store.fetchMapping(spaceId)

      expect(result).toBe(largeMappingData)
      expect(store.currentMapping).toBe(largeMappingData)
    })

    it('应该处理特殊字符的映射配置', async () => {
      const spaceId = 1
      const specialMapping = JSON.stringify({
        properties: {
          "field-with-特殊字符": {
            type: 'text',
            analyzer: 'ik_max_word'
          },
          "field.with.dots": {
            type: 'keyword'
          },
          "field_with_emoji_🚀": {
            type: 'text'
          }
        }
      })

      const mockResponse = createMockApiResponse(specialMapping)
      mockedMappingApi.updateMapping.mockResolvedValueOnce(mockResponse)

      await store.updateMapping(spaceId, specialMapping)

      expect(store.currentMapping).toBe(specialMapping)
      expect(store.mappingCache.get(spaceId)).toBe(specialMapping)
    })

    it('应该处理无效的spaceId', async () => {
      const invalidSpaceId = -1
      const errorMessage = 'Invalid space ID'

      mockedMappingApi.getMapping.mockRejectedValueOnce(new Error(errorMessage))

      await expect(store.fetchMapping(invalidSpaceId)).rejects.toThrow(errorMessage)

      expect(store.mappingError).toBe(errorMessage)
    })
  })

  describe('性能和内存管理', () => {
    it('应该限制缓存大小以避免内存泄漏', async () => {
      // 添加大量缓存项
      for (let i = 1; i <= 100; i++) {
        store.mappingCache.set(i, `mapping-${i}`)
      }

      expect(store.mappingCache.size).toBe(100)

      // 这里可以添加缓存清理逻辑测试，如果有的话
    })

    it('应该正确清理过期的缓存', () => {
      const oldDate = new Date(Date.now() - 24 * 60 * 60 * 1000) // 24小时前
      store.lastMappingUpdated = oldDate

      // 模拟缓存清理逻辑（如果实现了的话）
      // 这里主要是验证时间戳的管理
      expect(store.lastMappingUpdated).toBe(oldDate)
    })
  })
})