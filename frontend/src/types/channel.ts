/**
 * 渠道状态枚举
 */
export type ChannelStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'DELETED'

/**
 * 渠道类型枚举
 */
export type ChannelType = 'ONLINE' | 'OFFLINE' | 'HYBRID' | 'DISTRIBUTOR'

/**
 * 渠道实体接口
 */
export interface Channel {
  id: number
  name: string
  code: string
  description?: string
  type: ChannelType
  typeDisplayName?: string
  status: ChannelStatus
  statusDisplayName?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  website?: string
  commissionRate: number
  monthlyTarget: number
  totalSales: number
  currentMonthSales: number
  performanceRatio?: number
  targetAchieved?: boolean
  lastActivityAt?: string
  sortOrder: number
  version: number
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
  type: ChannelType
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  website?: string
  commissionRate?: number
  monthlyTarget?: number
  sortOrder?: number
}

/**
 * 更新渠道请求接口
 */
export interface UpdateChannelRequest {
  name?: string
  description?: string
  type?: ChannelType
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  website?: string
  commissionRate?: number
  monthlyTarget?: number
  sortOrder?: number
}

/**
 * 渠道查询请求接口
 */
export interface ChannelQueryRequest {
  keyword?: string
  status?: ChannelStatus
  type?: ChannelType
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

/**
 * 销售数据更新请求接口
 */
export interface UpdateSalesRequest {
  salesAmount: number
}

/**
 * 批量状态更新请求接口
 */
export interface BatchStatusUpdateRequest {
  channelIds: number[]
  targetStatus: ChannelStatus
}

/**
 * 渠道统计信息接口
 */
export interface ChannelStatistics {
  totalChannels: number
  activeChannels: number
  inactiveChannels: number
  suspendedChannels: number
  deletedChannels: number
  totalSales: number
  averageSales: number
  topPerformerCount: number
  onlineChannelCount: number
  offlineChannelCount: number
  hybridChannelCount: number
  distributorChannelCount: number
}

/**
 * 分页结果泛型接口（复用现有定义）
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
 * API响应泛型接口（复用现有定义）
 */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

/**
 * 渠道状态选项
 */
export const CHANNEL_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: '激活' },
  { value: 'INACTIVE', label: '未激活' },
  { value: 'SUSPENDED', label: '暂停' },
  { value: 'DELETED', label: '已删除' }
] as const

/**
 * 渠道类型选项
 */
export const CHANNEL_TYPE_OPTIONS = [
  { value: 'ONLINE', label: '线上渠道' },
  { value: 'OFFLINE', label: '线下渠道' },
  { value: 'HYBRID', label: '混合渠道' },
  { value: 'DISTRIBUTOR', label: '分销商渠道' }
] as const

/**
 * 排序字段选项
 */
export const CHANNEL_SORT_OPTIONS = [
  { value: 'createdAt', label: '创建时间' },
  { value: 'updatedAt', label: '更新时间' },
  { value: 'name', label: '渠道名称' },
  { value: 'totalSales', label: '总销售额' },
  { value: 'currentMonthSales', label: '当月销售额' },
  { value: 'sortOrder', label: '排序顺序' },
  { value: 'lastActivityAt', label: '最后活动时间' }
] as const

/**
 * 渠道状态显示名称映射
 */
export const CHANNEL_STATUS_LABELS: Record<ChannelStatus, string> = {
  'ACTIVE': '激活',
  'INACTIVE': '未激活',
  'SUSPENDED': '暂停',
  'DELETED': '已删除'
}

/**
 * 渠道类型显示名称映射
 */
export const CHANNEL_TYPE_LABELS: Record<ChannelType, string> = {
  'ONLINE': '线上渠道',
  'OFFLINE': '线下渠道',
  'HYBRID': '混合渠道',
  'DISTRIBUTOR': '分销商渠道'
}

/**
 * 渠道状态颜色映射（用于UI显示）
 */
export const CHANNEL_STATUS_COLORS: Record<ChannelStatus, string> = {
  'ACTIVE': 'success',
  'INACTIVE': 'warning',
  'SUSPENDED': 'danger',
  'DELETED': 'info'
}

/**
 * 渠道类型图标映射（用于UI显示）
 */
export const CHANNEL_TYPE_ICONS: Record<ChannelType, string> = {
  'ONLINE': 'i-heroicons-computer-desktop',
  'OFFLINE': 'i-heroicons-building-storefront',
  'HYBRID': 'i-heroicons-squares-2x2',
  'DISTRIBUTOR': 'i-heroicons-users'
}