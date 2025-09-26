/**
 * 渠道实体接口
 */
export interface Channel {
  id: number
  name: string
  code: string
  description?: string
  createdAt: string
  updatedAt: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 创建渠道请求接口
 */
export interface CreateChannelRequest {
  name: string
  code: string
  description?: string
}

/**
 * 更新渠道请求接口
 */
export interface UpdateChannelRequest {
  name?: string
  description?: string
}

/**
 * 渠道查询请求接口
 */
export interface ChannelQueryRequest {
  keyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

/**
 * 分页结果泛型接口
 */
export interface PageResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  numberOfElements: number
}

/**
 * API响应泛型接口
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

/**
 * 排序字段选项
 */
export const CHANNEL_SORT_OPTIONS = [
  { value: 'createdAt', label: '创建时间' },
  { value: 'updatedAt', label: '更新时间' },
  { value: 'name', label: '渠道名称' },
  { value: 'code', label: '渠道代码' }
] as const