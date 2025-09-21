export interface User {
  id: number
  username: string
  email: string
  fullName?: string
  role: 'ADMIN' | 'USER'
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED'
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  token: string
  refreshToken?: string
  user: User
}