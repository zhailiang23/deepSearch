import http from '@/utils/http'
import type {
  SensitiveWord,
  CreateSensitiveWordRequest,
  UpdateSensitiveWordRequest,
  SensitiveWordQueryParams,
  SensitiveWordStatistics
} from '@/types/sensitiveWord'
import type { ApiResponse, PageResult } from '@/types/api'

/**
 * 敏感词API服务
 */
export const sensitiveWordApi = {
  /**
   * 创建敏感词
   */
  create: async (data: CreateSensitiveWordRequest): Promise<SensitiveWord> => {
    const response = await http.post<ApiResponse<SensitiveWord>>('/sensitive-words', data)
    return response.data
  },

  /**
   * 更新敏感词
   */
  update: async (id: number, data: UpdateSensitiveWordRequest): Promise<SensitiveWord> => {
    const response = await http.put<ApiResponse<SensitiveWord>>(`/sensitive-words/${id}`, data)
    return response.data
  },

  /**
   * 删除敏感词
   */
  delete: async (id: number): Promise<void> => {
    await http.delete(`/sensitive-words/${id}`)
  },

  /**
   * 获取单个敏感词
   */
  getById: async (id: number): Promise<SensitiveWord> => {
    const response = await http.get<ApiResponse<SensitiveWord>>(`/sensitive-words/${id}`)
    return response.data
  },

  /**
   * 分页查询敏感词列表
   */
  list: async (params: SensitiveWordQueryParams): Promise<PageResult<SensitiveWord>> => {
    const response = await http.get<ApiResponse<PageResult<SensitiveWord>>>('/sensitive-words', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        keyword: params.keyword,
        enabled: params.enabled,
        harmLevel: params.harmLevel,
        sort: params.sort ?? 'harmLevel',
        direction: params.direction ?? 'DESC'
      }
    })
    return response.data
  },

  /**
   * 搜索敏感词
   */
  search: async (keyword: string, params?: SensitiveWordQueryParams): Promise<PageResult<SensitiveWord>> => {
    const response = await http.get<ApiResponse<PageResult<SensitiveWord>>>('/sensitive-words/search', {
      params: {
        keyword,
        page: params?.page ?? 0,
        size: params?.size ?? 20,
        sort: params?.sort ?? 'harmLevel',
        direction: params?.direction ?? 'DESC'
      }
    })
    return response.data
  },

  /**
   * 获取所有敏感词列表
   */
  getAll: async (): Promise<SensitiveWord[]> => {
    const response = await http.get<ApiResponse<SensitiveWord[]>>('/sensitive-words/all')
    return response.data
  },

  /**
   * 切换敏感词启用状态
   */
  toggleStatus: async (id: number): Promise<SensitiveWord> => {
    const response = await http.post<ApiResponse<SensitiveWord>>(`/sensitive-words/${id}/toggle-status`)
    return response.data
  },

  /**
   * 更新敏感词危害等级
   */
  updateHarmLevel: async (id: number, harmLevel: number): Promise<SensitiveWord> => {
    const response = await http.patch<ApiResponse<SensitiveWord>>(
      `/sensitive-words/${id}/harm-level`,
      null,
      { params: { harmLevel } }
    )
    return response.data
  },

  /**
   * 检查敏感词名称是否可用
   */
  checkNameAvailable: async (name: string, excludeId?: number): Promise<boolean> => {
    const response = await http.get<ApiResponse<boolean>>('/sensitive-words/name-available', {
      params: { name, excludeId }
    })
    return response.data
  },

  /**
   * 获取敏感词统计信息
   */
  getStatistics: async (): Promise<SensitiveWordStatistics> => {
    const response = await http.get<ApiResponse<SensitiveWordStatistics>>('/sensitive-words/statistics')
    return response.data
  }
}