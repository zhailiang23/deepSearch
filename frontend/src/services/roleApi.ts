import http from '@/utils/http'
import type {
  Role,
  CreateRoleRequest,
  UpdateRoleRequest,
  RoleQueryRequest,
  PageResult,
  ApiResponse
} from '@/types/role'

const BASE_URL = '/roles'

/**
 * 角色管理API服务
 */
export const roleApi = {
  // ========== 基础CRUD操作 ==========

  /**
   * 创建角色
   */
  create: (data: CreateRoleRequest): Promise<ApiResponse<Role>> =>
    http.post(BASE_URL, data),

  /**
   * 分页查询角色列表
   */
  list: (params: RoleQueryRequest): Promise<ApiResponse<PageResult<Role>>> =>
    http.get(BASE_URL, { params }),

  /**
   * 根据ID查询角色
   */
  getById: (id: number): Promise<ApiResponse<Role>> =>
    http.get(`${BASE_URL}/${id}`),

  /**
   * 根据代码查询角色
   */
  getByCode: (code: string): Promise<ApiResponse<Role>> =>
    http.get(`${BASE_URL}/by-code/${code}`),

  /**
   * 更新角色
   */
  update: (id: number, data: UpdateRoleRequest): Promise<ApiResponse<Role>> =>
    http.put(`${BASE_URL}/${id}`, data),

  /**
   * 删除角色
   */
  delete: (id: number): Promise<ApiResponse<void>> =>
    http.delete(`${BASE_URL}/${id}`),

  // ========== 验证操作 ==========

  /**
   * 检查角色代码可用性
   */
  checkCodeAvailability: (code: string, excludeId?: number): Promise<ApiResponse<boolean>> => {
    const params: any = { code }
    if (excludeId) {
      params.excludeId = excludeId
    }
    return http.get(`${BASE_URL}/check-code`, { params })
  },

  /**
   * 检查角色名称可用性
   */
  checkNameAvailability: (name: string, excludeId?: number): Promise<ApiResponse<boolean>> => {
    const params: any = { name }
    if (excludeId) {
      params.excludeId = excludeId
    }
    return http.get(`${BASE_URL}/check-name`, { params })
  },

  /**
   * 获取所有角色（不分页）
   */
  getAll: (): Promise<ApiResponse<Role[]>> =>
    http.get(`${BASE_URL}/all`)
}