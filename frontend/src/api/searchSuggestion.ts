/**
 * 搜索建议API接口
 * 提供智能搜索建议功能
 */

import http from '@/utils/http'
import type { ApiResponse } from '@/types/api'

/**
 * 建议类型枚举
 */
export enum SuggestionType {
  ES_COMPLETION = 'ES_COMPLETION',
  HISTORY = 'HISTORY',
  HOT_TOPIC = 'HOT_TOPIC'
}

/**
 * 搜索建议请求参数
 */
export interface SearchSuggestionRequest {
  /** 用户输入的查询词 */
  query: string
  /** 搜索空间ID(可选) @deprecated 使用 searchSpaceIds 替代 */
  searchSpaceId?: number
  /** 搜索空间ID列表(可选,支持多索引搜索) */
  searchSpaceIds?: number[]
  /** 用户ID(可选,用于个性化) */
  userId?: number
  /** 返回建议数量 */
  size?: number
  /** 是否启用模糊匹配 */
  enableFuzzy?: boolean
}

/**
 * 搜索建议响应
 */
export interface SearchSuggestionDTO {
  /** 建议文本 */
  text: string
  /** 综合得分(0-100) */
  score: number
  /** 建议类型 */
  type: SuggestionType
  /** 图标标识 */
  icon: string
  /** 额外元数据 */
  metadata?: Record<string, any>
}

/**
 * 搜索建议API
 */
export const searchSuggestionApi = {
  /**
   * 获取搜索建议
   * @param request 搜索建议请求参数
   * @returns 搜索建议列表
   */
  async getSuggestions(request: SearchSuggestionRequest): Promise<ApiResponse<SearchSuggestionDTO[]>> {
    return http.post('/elasticsearch/search-suggestions', request)
  }
}
