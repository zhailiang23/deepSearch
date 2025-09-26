import http from '@/utils/http'
import type {
  Channel,
  CreateChannelRequest,
  UpdateChannelRequest,
  ChannelQueryRequest,
  PageResult,
  ApiResponse
} from '@/types/channel'

const BASE_URL = '/channels'

/**
 * 渠道管理API服务
 */
export const channelApi = {
  // ========== 基础CRUD操作 ==========

  /**
   * 创建渠道
   */
  create: (data: CreateChannelRequest): Promise<ApiResponse<Channel>> =>
    http.post(BASE_URL, data),

  /**
   * 分页查询渠道列表
   */
  list: (params: ChannelQueryRequest): Promise<ApiResponse<PageResult<Channel>>> =>
    http.get(BASE_URL, { params }),

  /**
   * 根据ID查询渠道
   */
  getById: (id: number): Promise<ApiResponse<Channel>> =>
    http.get(`${BASE_URL}/${id}`),

  /**
   * 根据代码查询渠道
   */
  getByCode: (code: string): Promise<ApiResponse<Channel>> =>
    http.get(`${BASE_URL}/by-code/${code}`),

  /**
   * 更新渠道
   */
  update: (id: number, data: UpdateChannelRequest): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}`, data),

  /**
   * 删除渠道
   */
  delete: (id: number): Promise<ApiResponse<void>> =>
    http.delete(`${BASE_URL}/${id}`),

  // ========== 验证操作 ==========

  /**
   * 检查渠道代码可用性
   */
  checkCodeAvailability: (code: string, excludeId?: number): Promise<ApiResponse<boolean>> => {
    const params: any = { code }
    if (excludeId) {
      params.excludeId = excludeId
    }
    return http.get(`${BASE_URL}/check-code`, { params })
  },

  /**
   * 检查渠道名称可用性
   */
  checkNameAvailability: (name: string, excludeId?: number): Promise<ApiResponse<boolean>> => {
    const params: any = { name }
    if (excludeId) {
      params.excludeId = excludeId
    }
    return http.get(`${BASE_URL}/check-name`, { params })
  }
}