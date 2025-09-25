import http from '@/utils/http'
import type {
  Channel,
  CreateChannelRequest,
  UpdateChannelRequest,
  ChannelQueryRequest,
  UpdateSalesRequest,
  BatchStatusUpdateRequest,
  ChannelStatistics,
  ChannelType,
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
   * 删除渠道（软删除）
   */
  delete: (id: number): Promise<ApiResponse<void>> =>
    http.delete(`${BASE_URL}/${id}`),

  // ========== 列表查询操作 ==========

  /**
   * 获取活跃渠道列表
   */
  getActiveChannels: (): Promise<ApiResponse<Channel[]>> =>
    http.get(`${BASE_URL}/active`),

  /**
   * 按类型获取渠道列表
   */
  getChannelsByType: (type: ChannelType): Promise<ApiResponse<Channel[]>> =>
    http.get(`${BASE_URL}/by-type/${type}`),

  /**
   * 获取销售排行榜
   */
  getTopPerformingChannels: (limit = 10): Promise<ApiResponse<Channel[]>> =>
    http.get(`${BASE_URL}/top-performers`, { params: { limit } }),

  // ========== 状态管理操作 ==========

  /**
   * 激活渠道
   */
  activate: (id: number): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}/activate`),

  /**
   * 停用渠道
   */
  deactivate: (id: number): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}/deactivate`),

  /**
   * 暂停渠道
   */
  suspend: (id: number): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}/suspend`),

  /**
   * 恢复渠道
   */
  restore: (id: number): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}/restore`),

  // ========== 销售管理操作 ==========

  /**
   * 更新销售数据
   */
  updateSales: (id: number, data: UpdateSalesRequest): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}/sales`, data),

  /**
   * 重置月度销售
   */
  resetMonthlySales: (id: number): Promise<ApiResponse<Channel>> =>
    http.put(`${BASE_URL}/${id}/reset-monthly-sales`),

  // ========== 统计分析操作 ==========

  /**
   * 获取渠道统计信息
   */
  getStatistics: (): Promise<ApiResponse<ChannelStatistics>> =>
    http.get(`${BASE_URL}/statistics`),

  // ========== 批量操作 ==========

  /**
   * 批量状态变更
   */
  batchUpdateStatus: (data: BatchStatusUpdateRequest): Promise<ApiResponse<Channel[]>> =>
    http.put(`${BASE_URL}/batch/status`, data),

  /**
   * 批量删除渠道
   */
  batchDelete: (channelIds: number[]): Promise<ApiResponse<void>> =>
    http.delete(`${BASE_URL}/batch`, { data: channelIds }),

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