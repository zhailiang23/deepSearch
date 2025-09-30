/**
 * 敏感词相关类型定义
 */

/**
 * 敏感词实体
 */
export interface SensitiveWord {
  id: number
  name: string
  harmLevel: number
  enabled: boolean
  createdAt: string
  updatedAt: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 创建敏感词请求
 */
export interface CreateSensitiveWordRequest {
  name: string
  harmLevel: number
  enabled: boolean
}

/**
 * 更新敏感词请求
 */
export interface UpdateSensitiveWordRequest {
  name: string
  harmLevel: number
  enabled: boolean
}

/**
 * 敏感词查询参数
 */
export interface SensitiveWordQueryParams {
  page?: number
  size?: number
  keyword?: string
  enabled?: boolean
  harmLevel?: number
  sort?: string
  direction?: 'ASC' | 'DESC'
}

/**
 * 敏感词统计信息
 */
export interface SensitiveWordStatistics {
  enabledWords: number
  disabledWords: number
  totalWords: number
  harmLevelDistribution: Record<number, number>
}