import http from '@/utils/http'
import type { SearchResult, SearchResponse } from '@/types/demo'

/**
 * 搜索数据请求参数
 */
export interface SearchDataRequest {
  searchSpaceId: string
  query?: string
  page?: number
  size?: number
  sort?: {
    field: string
    order: 'asc' | 'desc'
  }
  filters?: Array<{
    field: string
    value: any
    operator: 'eq' | 'contains' | 'startsWith' | 'endsWith' | 'range' | 'in'
  }>
  enablePinyinSearch?: boolean
  pinyinMode?: 'AUTO' | 'STRICT' | 'FUZZY'
}

/**
 * 搜索数据响应
 */
export interface SearchDataResponse {
  data: Array<{
    _id: string
    _score: number
    _source: Record<string, any>
    _index: string
    _type?: string
    _version?: number
  }>
  total: number
  page: number
  size: number
  mapping?: {
    mappings: Record<string, any>
  }
  searchLogId?: number
}

/**
 * API响应包装
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp?: string
}

/**
 * 搜索数据服务
 */
export class SearchDataService {
  /**
   * 搜索ES数据
   */
  static async searchData(request: SearchDataRequest): Promise<SearchDataResponse> {
    try {
      console.log('发送搜索请求:', request)

      const response = await http.post<ApiResponse<SearchDataResponse>>(
        '/elasticsearch/search',
        request
      )

      console.log('HTTP响应原始数据:', response)

      // 由于 http.ts 中的拦截器已经返回了 response.data，
      // 所以这里 response 就是后端的完整响应对象
      const apiResponse = response as any

      console.log('API响应解析:', {
        success: apiResponse.success,
        message: apiResponse.message,
        hasData: !!apiResponse.data,
        dataKeys: apiResponse.data ? Object.keys(apiResponse.data) : []
      })

      if (apiResponse.success) {
        console.log('搜索成功，返回数据:', apiResponse.data)
        return apiResponse.data
      } else {
        console.error('后端返回失败状态:', apiResponse.message)
        throw new Error(apiResponse.message || '搜索失败')
      }
    } catch (error: any) {
      console.error('搜索数据失败:', error)
      throw error
    }
  }

  /**
   * 获取索引映射
   */
  static async getIndexMapping(searchSpaceId: string): Promise<any> {
    try {
      const response = await http.get<ApiResponse<any>>(
        `/elasticsearch/mapping/${searchSpaceId}`
      )
      const apiResponse = response.data

      if (apiResponse.success) {
        return apiResponse.data
      } else {
        throw new Error(apiResponse.message || '获取映射失败')
      }
    } catch (error: any) {
      console.error('获取索引映射失败:', error)
      throw error
    }
  }
}

/**
 * 将后端搜索响应转换为前端SearchResult格式
 */
export function transformSearchResponse(backendResponse: SearchDataResponse): SearchResponse {
  const results: SearchResult[] = backendResponse.data.map(item => {
    // 从_source中提取标题和摘要
    const source = item._source || {}
    let title = source.title || source.name || source.subject || `文档 ${item._id}`
    let summary = source.descript || source.content || source.description || source.summary || source.body || ''

    // 如果summary太长，截取前200个字符
    if (summary && summary.length > 200) {
      summary = summary.substring(0, 200) + '...'
    }

    // 如果没有摘要，尝试从其他字段构建
    if (!summary) {
      const textFields = Object.entries(source)
        .filter(([key, value]) =>
          typeof value === 'string' &&
          key !== 'title' &&
          key !== 'name' &&
          value.length > 10
        )
        .map(([_, value]) => value)

      if (textFields.length > 0) {
        summary = textFields[0].substring(0, 200) + '...'
      }
    }

    return {
      id: item._id,
      title,
      summary: summary || '暂无摘要信息',
      score: item._score,
      source: source,
      index: item._index,
      type: item._type || 'document',
      createdAt: source.created_at || source.createdAt || source.timestamp,
      updatedAt: source.updated_at || source.updatedAt
    }
  })

  return {
    results,
    total: backendResponse.total,
    duration: 0, // 后端没有返回耗时，前端可以自己计算
    hasMore: (backendResponse.page * backendResponse.size) < backendResponse.total,
    page: backendResponse.page,
    size: backendResponse.size,
    suggestions: [],
    aggregations: {},
    searchLogId: backendResponse.searchLogId
  }
}

export default SearchDataService