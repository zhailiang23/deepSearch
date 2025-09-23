import http from '@/utils/http'
import type {
  SearchSpace,
  CreateSearchSpaceRequest,
  UpdateSearchSpaceRequest,
  SearchSpaceQueryRequest,
  SearchSpaceStatistics,
  PageResult,
  ApiResponse
} from '@/types/searchSpace'

const BASE_URL = '/search-spaces'

export const searchSpaceApi = {
  /**
   * 创建搜索空间
   */
  create: (data: CreateSearchSpaceRequest): Promise<ApiResponse<SearchSpace>> =>
    http.post(BASE_URL, data),

  /**
   * 分页查询搜索空间列表
   */
  list: (params: SearchSpaceQueryRequest): Promise<ApiResponse<PageResult<SearchSpace>>> =>
    http.get(BASE_URL, { params }),

  /**
   * 根据ID查询搜索空间
   */
  getById: (id: number): Promise<ApiResponse<SearchSpace>> =>
    http.get(`${BASE_URL}/${id}`),

  /**
   * 根据代码查询搜索空间
   */
  getByCode: (code: string): Promise<ApiResponse<SearchSpace>> =>
    http.get(`${BASE_URL}/code/${code}`),

  /**
   * 更新搜索空间
   */
  update: (id: number, data: UpdateSearchSpaceRequest): Promise<ApiResponse<SearchSpace>> =>
    http.put(`${BASE_URL}/${id}`, data),

  /**
   * 删除搜索空间
   */
  delete: (id: number): Promise<ApiResponse<void>> =>
    http.delete(`${BASE_URL}/${id}`),

  /**
   * 启用搜索空间
   */
  enable: (id: number): Promise<ApiResponse<SearchSpace>> =>
    http.post(`${BASE_URL}/${id}/enable`),

  /**
   * 禁用搜索空间
   */
  disable: (id: number): Promise<ApiResponse<SearchSpace>> =>
    http.post(`${BASE_URL}/${id}/disable`),

  /**
   * 获取搜索空间统计信息
   */
  getStatistics: (): Promise<ApiResponse<SearchSpaceStatistics>> =>
    http.get(`${BASE_URL}/statistics`),

  /**
   * 检查代码是否可用
   */
  checkCodeAvailability: (code: string): Promise<ApiResponse<{ code: string; available: boolean }>> =>
    http.get(`${BASE_URL}/code/${code}/available`)
}