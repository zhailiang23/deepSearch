/**
 * 角色实体接口
 */
export interface Role {
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
 * 创建角色请求接口
 */
export interface CreateRoleRequest {
  name: string
  code: string
  description?: string
}

/**
 * 更新角色请求接口
 */
export interface UpdateRoleRequest {
  name?: string
  description?: string
}

/**
 * 角色查询请求接口
 */
export interface RoleQueryRequest {
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
export const ROLE_SORT_OPTIONS = [
  { value: 'createdAt', label: '创建时间' },
  { value: 'updatedAt', label: '更新时间' },
  { value: 'name', label: '角色名称' },
  { value: 'code', label: '角色代码' }
] as const

/**
 * 角色-搜索空间配置请求
 */
export interface RoleSearchSpaceConfigRequest {
  searchSpaceIds: number[]
}

/**
 * 角色-搜索空间关联信息
 */
export interface RoleSearchSpace {
  id: number
  roleId: number
  roleName: string
  roleCode: string
  searchSpaceId: number
  searchSpaceName: string
  searchSpaceCode: string
  createdAt: string
  updatedAt?: string
}