import http from '@/utils/http'
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UserQueryParams,
  UserStatistics
} from '@/types/user'
import type { ApiResponse, PageResult } from '@/types/api'

export const userApi = {
  create: async (data: CreateUserRequest): Promise<User> => {
    const response = await http.post<ApiResponse<User>>('/users', data)
    return response.data
  },

  update: async (id: number, data: UpdateUserRequest): Promise<User> => {
    const response = await http.put<ApiResponse<User>>(`/users/${id}`, data)
    return response.data
  },

  delete: async (id: number): Promise<void> => {
    await http.delete(`/users/${id}`)
  },

  getById: async (id: number): Promise<User> => {
    const response = await http.get<ApiResponse<User>>(`/users/${id}`)
    return response.data
  },

  list: async (params: UserQueryParams): Promise<PageResult<User>> => {
    const response = await http.get<ApiResponse<PageResult<User>>>('/users', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        keyword: params.keyword,
        status: params.status,
        role: params.role
      }
    })
    return response.data
  },

  search: async (keyword: string, params?: UserQueryParams): Promise<PageResult<User>> => {
    const response = await http.get<ApiResponse<PageResult<User>>>('/users/search', {
      params: {
        keyword,
        page: params?.page ?? 0,
        size: params?.size ?? 20
      }
    })
    return response.data
  },

  toggleStatus: async (id: number): Promise<User> => {
    const response = await http.post<ApiResponse<User>>(`/users/${id}/toggle-status`)
    return response.data
  },

  getStatistics: async (): Promise<UserStatistics> => {
    const response = await http.get<ApiResponse<UserStatistics>>('/users/statistics')
    return response.data
  },

  getAll: async (): Promise<User[]> => {
    const response = await http.get<ApiResponse<User[]>>('/users/all')
    return response.data
  },

  checkUsernameAvailable: async (username: string, excludeId?: number): Promise<boolean> => {
    const response = await http.get<ApiResponse<boolean>>('/users/username-available', {
      params: { username, excludeId }
    })
    return response.data
  },

  checkEmailAvailable: async (email: string, excludeId?: number): Promise<boolean> => {
    const response = await http.get<ApiResponse<boolean>>('/users/email-available', {
      params: { email, excludeId }
    })
    return response.data
  }
}