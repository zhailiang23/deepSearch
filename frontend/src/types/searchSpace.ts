export type SearchSpaceStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE' | 'DELETED'

export interface SearchSpace {
  id: number
  name: string
  code: string
  description?: string
  status: SearchSpaceStatus
  version: number
  createdAt: string
  updatedAt: string
  indexStatus?: IndexStatus
}

export interface IndexStatus {
  name: string
  exists: boolean
  health: string
  error?: string
}

export interface CreateSearchSpaceRequest {
  name: string
  code: string
  description?: string
}

export interface UpdateSearchSpaceRequest {
  name?: string
  description?: string
  status?: SearchSpaceStatus
}

export interface SearchSpaceQueryRequest {
  keyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface SearchSpaceStatistics {
  totalSpaces: number
  activeSpaces: number
  inactiveSpaces: number
  maintenanceSpaces: number
  deletedSpaces: number
}

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

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}