import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { roleApi } from '@/services/roleApi'
import type {
  Role,
  CreateRoleRequest,
  UpdateRoleRequest,
  RoleQueryRequest,
  PageResult,
  RoleSearchSpaceConfigRequest
} from '@/types/role'
import type { SearchSpace } from '@/types/searchSpace'

export const useRoleStore = defineStore('role', () => {
  // ========== 状态定义 ==========

  const roles = ref<Role[]>([])
  const currentRole = ref<Role | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 分页状态
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  })

  // 查询参数
  const queryParams = ref<RoleQueryRequest>({
    keyword: '',
    page: 0,
    size: 10,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })

  // ========== 计算属性 ==========

  /**
   * 是否有角色数据
   */
  const hasRoles = computed(() => roles.value.length > 0)

  // ========== 基础操作方法 ==========

  const setLoading = (value: boolean) => {
    loading.value = value
  }

  const setError = (value: string | null) => {
    error.value = value
  }

  const clearError = () => {
    error.value = null
  }

  /**
   * 更新查询参数
   */
  const updateQueryParams = (params: Partial<RoleQueryRequest>) => {
    queryParams.value = { ...queryParams.value, ...params }
  }

  // ========== API调用方法 ==========

  /**
   * 获取角色列表
   */
  const fetchRoles = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await roleApi.list(queryParams.value)
      if (response.data) {
        roles.value = response.data.content
        pagination.value = {
          page: response.data.page,
          size: response.data.size,
          totalElements: response.data.totalElements,
          totalPages: response.data.totalPages,
          first: response.data.first,
          last: response.data.last
        }
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取角色列表失败'
      console.error('Failed to fetch roles:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * 根据ID获取角色
   */
  const fetchRoleById = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      const response = await roleApi.getById(id)
      if (response.data) {
        currentRole.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取角色详情失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 根据代码获取角色
   */
  const fetchRoleByCode = async (code: string) => {
    try {
      setLoading(true)
      clearError()

      const response = await roleApi.getByCode(code)
      if (response.data) {
        currentRole.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '获取角色详情失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 创建角色
   */
  const createRole = async (data: CreateRoleRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await roleApi.create(data)
      if (response.data) {
        // 添加到列表开头
        roles.value.unshift(response.data)
        currentRole.value = response.data
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '创建角色失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 更新角色
   */
  const updateRole = async (id: number, data: UpdateRoleRequest) => {
    try {
      setLoading(true)
      clearError()

      const response = await roleApi.update(id, data)
      if (response.data) {
        // 更新列表中的项目
        const index = roles.value.findIndex(role => role.id === id)
        if (index !== -1) {
          roles.value[index] = response.data
        }
        // 更新当前项目
        if (currentRole.value?.id === id) {
          currentRole.value = response.data
        }
      }
      return response.data
    } catch (err: any) {
      setError(err.message || '更新角色失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 删除角色
   */
  const deleteRole = async (id: number) => {
    try {
      setLoading(true)
      clearError()

      await roleApi.delete(id)

      // 从列表中移除
      roles.value = roles.value.filter(role => role.id !== id)

      // 如果删除的是当前项目，清空当前项目
      if (currentRole.value?.id === id) {
        currentRole.value = null
      }
    } catch (err: any) {
      setError(err.message || '删除角色失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  // ========== 验证操作 ==========

  /**
   * 检查代码可用性
   */
  const checkCodeAvailability = async (code: string, excludeId?: number) => {
    try {
      clearError()
      const response = await roleApi.checkCodeAvailability(code, excludeId)
      return response.data || false
    } catch (err: any) {
      setError(err.message || '检查代码可用性失败')
      throw err
    }
  }

  /**
   * 检查名称可用性
   */
  const checkNameAvailability = async (name: string, excludeId?: number) => {
    try {
      clearError()
      const response = await roleApi.checkNameAvailability(name, excludeId)
      return response.data || false
    } catch (err: any) {
      setError(err.message || '检查名称可用性失败')
      throw err
    }
  }

  // ========== 搜索空间权限配置 ==========

  /**
   * 配置角色的搜索空间权限
   */
  const configureSearchSpaces = async (roleId: number, searchSpaceIds: number[]) => {
    try {
      setLoading(true)
      clearError()

      const request: RoleSearchSpaceConfigRequest = { searchSpaceIds }
      await roleApi.configureSearchSpaces(roleId, request)
    } catch (err: any) {
      setError(err.message || '配置搜索空间权限失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 获取角色关联的搜索空间
   */
  const getRoleSearchSpaces = async (roleId: number): Promise<SearchSpace[]> => {
    try {
      setLoading(true)
      clearError()

      const response = await roleApi.getRoleSearchSpaces(roleId)
      return response.data || []
    } catch (err: any) {
      setError(err.message || '获取角色搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * 获取可配置的搜索空间列表
   */
  const getAvailableSearchSpaces = async (roleId: number): Promise<SearchSpace[]> => {
    try {
      setLoading(true)
      clearError()

      const response = await roleApi.getAvailableSearchSpaces(roleId)
      return response.data || []
    } catch (err: any) {
      setError(err.message || '获取可配置搜索空间失败')
      throw err
    } finally {
      setLoading(false)
    }
  }

  // ========== 重置操作 ==========

  /**
   * 重置状态
   */
  const reset = () => {
    roles.value = []
    currentRole.value = null
    loading.value = false
    error.value = null
    pagination.value = {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    }
    queryParams.value = {
      keyword: '',
      page: 0,
      size: 10,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }
  }

  return {
    // ========== 状态 ==========
    roles,
    currentRole,
    loading,
    error,
    pagination,
    queryParams,

    // ========== 计算属性 ==========
    hasRoles,

    // ========== 基础操作 ==========
    setLoading,
    setError,
    clearError,
    updateQueryParams,
    reset,

    // ========== CRUD操作 ==========
    fetchRoles,
    fetchRoleById,
    fetchRoleByCode,
    createRole,
    updateRole,
    deleteRole,

    // ========== 验证操作 ==========
    checkCodeAvailability,
    checkNameAvailability,

    // ========== 搜索空间权限配置 ==========
    configureSearchSpaces,
    getRoleSearchSpaces,
    getAvailableSearchSpaces
  }
})