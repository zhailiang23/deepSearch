import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mappingApi } from '../mappingApi'
import http from '@/utils/http'
import type { ApiResponse } from '@/types/searchSpace'

// Mock HTTP 客户端
vi.mock('@/utils/http', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  }
}))

describe('MappingApi', () => {
  const mockedHttp = http as any

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
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('getMapping', () => {
    it('应该调用正确的GET端点', async () => {
      const spaceId = 1
      const expectedResponse = createMockApiResponse(mockMappingData)

      mockedHttp.get.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.getMapping(spaceId)

      expect(mockedHttp.get).toHaveBeenCalledWith('/search-spaces/1/mapping')
      expect(result).toEqual(expectedResponse)
    })

    it('应该处理成功响应', async () => {
      const spaceId = 42
      const expectedResponse = createMockApiResponse(mockMappingData)

      mockedHttp.get.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.getMapping(spaceId)

      expect(result.success).toBe(true)
      expect(result.data).toBe(mockMappingData)
      expect(result.message).toBe('操作成功')
    })

    it('应该处理空映射响应', async () => {
      const spaceId = 1
      const expectedResponse = createMockApiResponse('{}')

      mockedHttp.get.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.getMapping(spaceId)

      expect(result.success).toBe(true)
      expect(result.data).toBe('{}')
    })

    it('应该处理API错误', async () => {
      const spaceId = 999
      const errorResponse = createMockApiResponse(null, false)
      errorResponse.message = '搜索空间不存在'

      mockedHttp.get.mockRejectedValueOnce(errorResponse)

      await expect(mappingApi.getMapping(spaceId)).rejects.toEqual(errorResponse)
      expect(mockedHttp.get).toHaveBeenCalledWith('/search-spaces/999/mapping')
    })

    it('应该处理网络错误', async () => {
      const spaceId = 1
      const networkError = new Error('Network Error')

      mockedHttp.get.mockRejectedValueOnce(networkError)

      await expect(mappingApi.getMapping(spaceId)).rejects.toThrow('Network Error')
    })

    it('应该处理大型映射配置', async () => {
      const spaceId = 1
      const largeMappingData = JSON.stringify({
        properties: Object.fromEntries(
          Array.from({ length: 1000 }, (_, i) => [
            `field${i}`,
            { type: 'text', analyzer: 'standard' }
          ])
        )
      })

      const expectedResponse = createMockApiResponse(largeMappingData)
      mockedHttp.get.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.getMapping(spaceId)

      expect(result.data).toBe(largeMappingData)
      expect(result.data.length).toBeGreaterThan(10000)
    })

    it('应该处理特殊字符映射配置', async () => {
      const spaceId = 1
      const specialCharMapping = JSON.stringify({
        properties: {
          "field-with-特殊字符": { type: 'text' },
          "field.with.dots": { type: 'keyword' },
          "field_with_emoji_🚀": { type: 'text' }
        }
      })

      const expectedResponse = createMockApiResponse(specialCharMapping)
      mockedHttp.get.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.getMapping(spaceId)

      expect(result.data).toBe(specialCharMapping)
      expect(JSON.parse(result.data)).toHaveProperty('properties.field-with-特殊字符')
    })

    it('应该正确处理不同的spaceId类型', async () => {
      const testCases = [
        { spaceId: 1, expected: '/search-spaces/1/mapping' },
        { spaceId: 999999, expected: '/search-spaces/999999/mapping' },
        { spaceId: 0, expected: '/search-spaces/0/mapping' },
      ]

      for (const testCase of testCases) {
        mockedHttp.get.mockResolvedValueOnce(createMockApiResponse('{}'))

        await mappingApi.getMapping(testCase.spaceId)

        expect(mockedHttp.get).toHaveBeenCalledWith(testCase.expected)
        mockedHttp.get.mockClear()
      }
    })
  })

  describe('updateMapping', () => {
    it('应该调用正确的PUT端点', async () => {
      const spaceId = 1
      const mappingData = '{"properties": {"title": {"type": "text"}}}'
      const expectedResponse = createMockApiResponse(undefined)

      mockedHttp.put.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.updateMapping(spaceId, mappingData)

      expect(mockedHttp.put).toHaveBeenCalledWith(
        '/search-spaces/1/mapping',
        mappingData,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
      expect(result).toEqual(expectedResponse)
    })

    it('应该设置正确的Content-Type头', async () => {
      const spaceId = 1
      const mappingData = '{}'
      const expectedResponse = createMockApiResponse(undefined)

      mockedHttp.put.mockResolvedValueOnce(expectedResponse)

      await mappingApi.updateMapping(spaceId, mappingData)

      expect(mockedHttp.put).toHaveBeenCalledWith(
        '/search-spaces/1/mapping',
        mappingData,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
    })

    it('应该处理成功的更新', async () => {
      const spaceId = 1
      const mappingData = mockMappingData
      const expectedResponse = createMockApiResponse(undefined)

      mockedHttp.put.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.updateMapping(spaceId, mappingData)

      expect(result.success).toBe(true)
      expect(result.message).toBe('操作成功')
    })

    it('应该处理空映射更新', async () => {
      const spaceId = 1
      const emptyMapping = '{}'
      const expectedResponse = createMockApiResponse(undefined)

      mockedHttp.put.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.updateMapping(spaceId, emptyMapping)

      expect(mockedHttp.put).toHaveBeenCalledWith(
        '/search-spaces/1/mapping',
        emptyMapping,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
      expect(result.success).toBe(true)
    })

    it('应该处理更新错误', async () => {
      const spaceId = 1
      const invalidMapping = '{"invalid": "mapping"}'
      const errorResponse = createMockApiResponse(null, false)
      errorResponse.message = '映射配置验证失败'

      mockedHttp.put.mockRejectedValueOnce(errorResponse)

      await expect(mappingApi.updateMapping(spaceId, invalidMapping))
        .rejects.toEqual(errorResponse)

      expect(mockedHttp.put).toHaveBeenCalledWith(
        '/search-spaces/1/mapping',
        invalidMapping,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
    })

    it('应该处理网络错误', async () => {
      const spaceId = 1
      const mappingData = '{}'
      const networkError = new Error('Connection timeout')

      mockedHttp.put.mockRejectedValueOnce(networkError)

      await expect(mappingApi.updateMapping(spaceId, mappingData))
        .rejects.toThrow('Connection timeout')
    })

    it('应该处理大型映射配置更新', async () => {
      const spaceId = 1
      const largeMappingData = JSON.stringify({
        properties: Object.fromEntries(
          Array.from({ length: 500 }, (_, i) => [
            `field${i}`,
            { type: 'text', analyzer: 'standard' }
          ])
        )
      })

      const expectedResponse = createMockApiResponse(undefined)
      mockedHttp.put.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.updateMapping(spaceId, largeMappingData)

      expect(mockedHttp.put).toHaveBeenCalledWith(
        `/search-spaces/${spaceId}/mapping`,
        largeMappingData,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
      expect(result.success).toBe(true)
    })

    it('应该处理特殊字符的映射配置更新', async () => {
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

      const expectedResponse = createMockApiResponse(undefined)
      mockedHttp.put.mockResolvedValueOnce(expectedResponse)

      const result = await mappingApi.updateMapping(spaceId, specialMapping)

      expect(mockedHttp.put).toHaveBeenCalledWith(
        `/search-spaces/${spaceId}/mapping`,
        specialMapping,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
      expect(result.success).toBe(true)
    })

    it('应该正确处理不同的spaceId', async () => {
      const testCases = [
        { spaceId: 1, expected: '/search-spaces/1/mapping' },
        { spaceId: 42, expected: '/search-spaces/42/mapping' },
        { spaceId: 999999, expected: '/search-spaces/999999/mapping' },
      ]

      const mappingData = '{}'
      const expectedResponse = createMockApiResponse(undefined)

      for (const testCase of testCases) {
        mockedHttp.put.mockResolvedValueOnce(expectedResponse)

        await mappingApi.updateMapping(testCase.spaceId, mappingData)

        expect(mockedHttp.put).toHaveBeenCalledWith(
          testCase.expected,
          mappingData,
          {
            headers: {
              'Content-Type': 'application/json'
            }
          }
        )
        mockedHttp.put.mockClear()
      }
    })

    it('应该处理HTTP状态码错误', async () => {
      const spaceId = 1
      const mappingData = '{}'
      const httpError = {
        response: {
          status: 400,
          data: {
            success: false,
            message: 'Bad Request',
            data: null
          }
        }
      }

      mockedHttp.put.mockRejectedValueOnce(httpError)

      await expect(mappingApi.updateMapping(spaceId, mappingData))
        .rejects.toEqual(httpError)
    })

    it('应该处理服务器内部错误', async () => {
      const spaceId = 1
      const mappingData = '{}'
      const serverError = {
        response: {
          status: 500,
          data: {
            success: false,
            message: 'Internal Server Error',
            data: null
          }
        }
      }

      mockedHttp.put.mockRejectedValueOnce(serverError)

      await expect(mappingApi.updateMapping(spaceId, mappingData))
        .rejects.toEqual(serverError)
    })
  })

  describe('API一致性', () => {
    it('两个方法都应该使用相同的基础URL', () => {
      // 这个测试确保两个方法使用相同的URL模式
      const spaceId = 1

      mappingApi.getMapping(spaceId)
      mappingApi.updateMapping(spaceId, '{}')

      const getCalls = mockedHttp.get.mock.calls
      const putCalls = mockedHttp.put.mock.calls

      if (getCalls.length > 0 && putCalls.length > 0) {
        expect(getCalls[0][0]).toBe(putCalls[0][0])
      }
    })

    it('所有方法都应该返回ApiResponse类型', async () => {
      const spaceId = 1
      const mappingData = '{}'

      const getResponse = createMockApiResponse(mappingData)
      const putResponse = createMockApiResponse(undefined)

      mockedHttp.get.mockResolvedValueOnce(getResponse)
      mockedHttp.put.mockResolvedValueOnce(putResponse)

      const getResult = await mappingApi.getMapping(spaceId)
      const putResult = await mappingApi.updateMapping(spaceId, mappingData)

      // 验证响应结构
      expect(getResult).toHaveProperty('success')
      expect(getResult).toHaveProperty('message')
      expect(getResult).toHaveProperty('data')
      expect(getResult).toHaveProperty('timestamp')

      expect(putResult).toHaveProperty('success')
      expect(putResult).toHaveProperty('message')
      expect(putResult).toHaveProperty('data')
      expect(putResult).toHaveProperty('timestamp')
    })
  })

  describe('并发和性能测试', () => {
    it('应该支持并发的GET请求', async () => {
      const spaceId = 1
      const expectedResponse = createMockApiResponse(mockMappingData)

      mockedHttp.get.mockResolvedValue(expectedResponse)

      // 并发发起多个请求
      const promises = Array.from({ length: 10 }, () => mappingApi.getMapping(spaceId))
      const results = await Promise.all(promises)

      // 所有请求都应该成功
      results.forEach(result => {
        expect(result.success).toBe(true)
        expect(result.data).toBe(mockMappingData)
      })

      // 确保所有请求都被发起
      expect(mockedHttp.get).toHaveBeenCalledTimes(10)
    })

    it('应该支持并发的PUT请求', async () => {
      const spaceId = 1
      const mappingData = '{}'
      const expectedResponse = createMockApiResponse(undefined)

      mockedHttp.put.mockResolvedValue(expectedResponse)

      // 并发发起多个更新请求
      const promises = Array.from({ length: 5 }, (_, i) =>
        mappingApi.updateMapping(spaceId, `{"version": ${i}}`)
      )
      const results = await Promise.all(promises)

      // 所有请求都应该成功
      results.forEach(result => {
        expect(result.success).toBe(true)
      })

      // 确保所有请求都被发起
      expect(mockedHttp.put).toHaveBeenCalledTimes(5)
    })
  })
})