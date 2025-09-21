import http from './http'
import type { LoginRequest, User, LoginResponse } from '@/types/auth'

export const authApi = {
  login: (credentials: LoginRequest): Promise<LoginResponse> =>
    http.post('/auth/login', credentials),

  getProfile: (): Promise<User> =>
    http.get('/auth/profile'),

  logout: (): Promise<void> =>
    http.post('/auth/logout')
}

export const userApi = {
  getUsers: (params?: any) =>
    http.get('/users', { params }),

  createUser: (data: any) =>
    http.post('/users', data),

  updateUser: (id: number, data: any) =>
    http.put(`/users/${id}`, data),

  deleteUser: (id: number) =>
    http.delete(`/users/${id}`)
}