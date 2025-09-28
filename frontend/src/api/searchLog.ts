/**
 * 搜索日志 API 服务
 */

import http from '@/utils/http'
import type {
  ApiResponse,
  PaginatedResponse,
  SearchLogQuery,
  SearchLog,
  SearchLogDetail,
  ClickRecordRequest
} from '@/types/searchLog'

export const searchLogApi = {
  /**
   * 记录点击行为
   */
  async recordClick(data: ClickRecordRequest): Promise<ApiResponse<void>> {
    try {
      const response = await http.post('/search-logs/click', {
        searchLogId: data.searchLogId,
        documentId: data.documentId,
        documentTitle: data.documentTitle,
        documentUrl: data.documentUrl,
        clickPosition: data.clickPosition,
        userAgent: data.userAgent || navigator.userAgent,
        clickType: data.clickType || 'click',
        modifierKeys: JSON.stringify(data.modifierKeys || {
          ctrl: false,
          shift: false,
          alt: false
        })
      })

      return {
        success: true,
        data: undefined,
        message: '点击记录成功'
      }
    } catch (error: any) {
      console.error('记录点击失败:', error)
      return {
        success: false,
        data: undefined,
        message: error.message || '记录点击失败'
      }
    }
  },

  /**
   * 批量记录点击行为
   */
  async recordClicks(clicks: ClickRecordRequest[]): Promise<ApiResponse<void>> {
    try {
      const response = await http.post('/search-logs/clicks/batch', clicks)

      return {
        success: true,
        data: undefined,
        message: `批量记录 ${clicks.length} 个点击成功`
      }
    } catch (error: any) {
      console.error('批量记录点击失败:', error)
      return {
        success: false,
        data: undefined,
        message: error.message || '批量记录点击失败'
      }
    }
  },

  /**
   * 获取搜索日志列表
   */
  async getSearchLogs(
    params: SearchLogQuery = {}
  ): Promise<ApiResponse<PaginatedResponse<SearchLog>>> {
    try {
      const sortParam = params.sort || 'createdAt'
      const directionParam = params.direction || 'desc'
      const { sort, direction, ...otherParams } = params
      const response = await http.get('/search-logs', {
        params: {
          page: params.page || 0,
          size: params.size || 20,
          sort: `${sortParam},${directionParam}`,
          ...otherParams
        }
      })

      return {
        success: true,
        data: response.data,
        message: '获取搜索日志成功'
      }
    } catch (error: any) {
      console.error('获取搜索日志失败:', error)
      return {
        success: false,
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 0,
          number: 0,
          first: true,
          last: true
        },
        message: error.message || '获取搜索日志失败'
      }
    }
  },

  /**
   * 获取搜索日志详情
   */
  async getSearchLogDetail(id: number): Promise<ApiResponse<SearchLogDetail>> {
    try {
      const response = await http.get(`/search-logs/${id}`)

      return {
        success: true,
        data: response.data,
        message: '获取搜索日志详情成功'
      }
    } catch (error: any) {
      console.error('获取搜索日志详情失败:', error)
      return {
        success: false,
        data: null as any,
        message: error.message || '获取搜索日志详情失败'
      }
    }
  },

  /**
   * 获取搜索统计数据
   */
  async getSearchStatistics(
    startTime: string,
    endTime: string
  ): Promise<ApiResponse<any>> {
    try {
      const response = await http.get('/search-logs/statistics', {
        params: {
          startTime,
          endTime
        }
      })

      return {
        success: true,
        data: response.data,
        message: '获取搜索统计数据成功'
      }
    } catch (error: any) {
      console.error('获取搜索统计数据失败:', error)
      return {
        success: false,
        data: null as any,
        message: error.message || '获取搜索统计数据失败'
      }
    }
  },

  /**
   * 获取点击统计数据
   */
  async getClickStatistics(
    searchLogId?: number,
    startTime?: string,
    endTime?: string
  ): Promise<ApiResponse<any>> {
    try {
      const params: any = {}
      if (searchLogId) params.searchLogId = searchLogId
      if (startTime) params.startTime = startTime
      if (endTime) params.endTime = endTime

      const response = await http.get('/search-logs/clicks/statistics', {
        params
      })

      return {
        success: true,
        data: response.data,
        message: '获取点击统计数据成功'
      }
    } catch (error: any) {
      console.error('获取点击统计数据失败:', error)
      return {
        success: false,
        data: null as any,
        message: error.message || '获取点击统计数据失败'
      }
    }
  }
}