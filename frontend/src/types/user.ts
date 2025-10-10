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
  status: UserStatus
  customRoleId: number
  customRoleName: string
  customRoleCode: string
  roleCode: string  // 兼容旧代码
  role?: string     // 兼容旧代码
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
  status: UserStatus
  customRoleId: number
}

export interface UpdateUserRequest {
  email?: string
  fullName?: string
  phone?: string
  status?: UserStatus
  customRoleId?: number
}

export interface UserQueryParams {
  page: number
  size: number
  keyword?: string
  status?: UserStatus
  customRoleId?: number
  role?: string  // 兼容旧代码
}

export interface RoleStatistics {
  roleId: number
  roleName: string
  status: UserStatus
  count: number
}

export interface UserStatistics {
  activeUsers: number
  lockedUsers: number
  totalUsers: number
  roleStatistics: RoleStatistics[]
}