export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER'
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  DISABLED = 'DISABLED',
  LOCKED = 'LOCKED',
  PENDING = 'PENDING'
}

export interface User {
  id: number
  username: string
  email: string
  fullName?: string
  phone?: string
  role: UserRole
  status: UserStatus
  failedLoginAttempts?: number
  lastLoginAt?: string
  lastLoginIp?: string
  emailVerified?: boolean
  createdAt: string
  updatedAt: string
  createdBy?: string
  updatedBy?: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  fullName?: string
  phone?: string
  role: UserRole
  status: UserStatus
}

export interface UpdateUserRequest {
  email?: string
  fullName?: string
  phone?: string
  role?: UserRole
  status?: UserStatus
}

export interface UserQueryParams {
  page: number
  size: number
  keyword?: string
  status?: UserStatus
  role?: UserRole
}

export interface UserStatistics {
  activeUsers: number
  lockedUsers: number
  totalUsers: number
  adminUsers: number
  normalUsers: number
}