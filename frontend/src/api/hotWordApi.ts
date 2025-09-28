/**
 * 热词统计API接口
 * 提供热词数据查询、统计分析等功能的API调用
 */

import http from '@/utils/http'
import type { ApiResponse } from '@/types/api'
import type {
  HotWordRequest,
  HotWordResponse,
  HotWordStatisticsRequest,
  HotWordStatisticsResponse
} from '@/types/hotWord'

/**
 * 热词统计API接口
 */
export const hotWordApi = {
  /**
   * 获取热词统计数据
   * @param params 热词查询参数
   * @returns 热词列表
   */
  async getHotWords(params: HotWordRequest): Promise<ApiResponse<HotWordResponse[]>> {
    const queryParams = new URLSearchParams()

    // 处理时间参数
    if (params.startDate) {
      queryParams.append('startDate', params.startDate.toISOString().slice(0, 19).replace('T', ' '))
    }
    if (params.endDate) {
      queryParams.append('endDate', params.endDate.toISOString().slice(0, 19).replace('T', ' '))
    }

    // 处理其他参数
    if (params.limit !== undefined) {
      queryParams.append('limit', String(params.limit))
    }
    if (params.userId) {
      queryParams.append('userId', params.userId)
    }
    if (params.searchSpaceId) {
      queryParams.append('searchSpaceId', params.searchSpaceId)
    }
    if (params.includeSegmentDetails !== undefined) {
      queryParams.append('includeSegmentDetails', String(params.includeSegmentDetails))
    }
    if (params.minWordLength !== undefined) {
      queryParams.append('minWordLength', String(params.minWordLength))
    }
    if (params.excludeStopWords !== undefined) {
      queryParams.append('excludeStopWords', String(params.excludeStopWords))
    }

    return http.get(`/search-logs/hot-words?${queryParams.toString()}`)
  },

  /**
   * 获取热词统计分析数据
   * @param params 统计查询参数
   * @returns 热词统计分析结果
   */
  async getHotWordStatistics(params: HotWordStatisticsRequest): Promise<ApiResponse<HotWordStatisticsResponse>> {
    const queryParams = new URLSearchParams()

    // 处理时间参数（必需）
    queryParams.append('startTime', params.startTime.toISOString().slice(0, 19).replace('T', ' '))
    queryParams.append('endTime', params.endTime.toISOString().slice(0, 19).replace('T', ' '))

    // 处理可选参数
    if (params.userId) {
      queryParams.append('userId', params.userId)
    }
    if (params.searchSpaceId) {
      queryParams.append('searchSpaceId', params.searchSpaceId)
    }
    if (params.includeDetails !== undefined) {
      queryParams.append('includeDetails', String(params.includeDetails))
    }
    if (params.topQueriesLimit !== undefined) {
      queryParams.append('topQueriesLimit', String(params.topQueriesLimit))
    }
    if (params.topSearchSpacesLimit !== undefined) {
      queryParams.append('topSearchSpacesLimit', String(params.topSearchSpacesLimit))
    }
    if (params.topUsersLimit !== undefined) {
      queryParams.append('topUsersLimit', String(params.topUsersLimit))
    }

    return http.get(`/search-logs/statistics?${queryParams.toString()}`)
  },

  /**
   * 导出热词统计报告
   * @param params 热词查询参数
   * @returns 文件Blob
   */
  async exportHotWordReport(params: HotWordRequest): Promise<Blob> {
    const queryParams = new URLSearchParams()

    // 处理时间参数
    if (params.startDate) {
      queryParams.append('startDate', params.startDate.toISOString().slice(0, 19).replace('T', ' '))
    }
    if (params.endDate) {
      queryParams.append('endDate', params.endDate.toISOString().slice(0, 19).replace('T', ' '))
    }

    // 处理其他参数
    if (params.limit !== undefined) {
      queryParams.append('limit', String(params.limit))
    }
    if (params.userId) {
      queryParams.append('userId', params.userId)
    }
    if (params.searchSpaceId) {
      queryParams.append('searchSpaceId', params.searchSpaceId)
    }

    const response = await http.get(`/search-logs/hot-words/export?${queryParams.toString()}`, {
      responseType: 'blob'
    })

    return response as unknown as Blob
  }
}

/**
 * 辅助函数：将Date对象转换为API所需的字符串格式
 * @param date Date对象
 * @returns 格式化的日期时间字符串 (yyyy-MM-dd HH:mm:ss)
 */
export function formatDateForApi(date: Date): string {
  return date.toISOString().slice(0, 19).replace('T', ' ')
}

/**
 * 辅助函数：构建查询参数
 * @param params 参数对象
 * @returns URLSearchParams对象
 */
export function buildQueryParams(params: Record<string, any>): URLSearchParams {
  const queryParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      if (value instanceof Date) {
        queryParams.append(key, formatDateForApi(value))
      } else {
        queryParams.append(key, String(value))
      }
    }
  })

  return queryParams
}