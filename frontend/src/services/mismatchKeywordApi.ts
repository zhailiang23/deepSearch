import http from '@/utils/http'
import type {
  MismatchKeywordRanking,
  RankingQueryRequest,
  RankingQueryResponse,
  StatisticsInfo
} from '@/types/mismatchKeyword'
import type { ApiResponse } from '@/types/api'

const API_BASE = '/statistics/mismatch-keywords'

/**
 * 关键词不匹配度统计API服务
 */
export class MismatchKeywordApi {
  /**
   * 获取关键词不匹配度排行榜
   */
  static async getRanking(params: {
    timeRange?: string
    page?: number
    size?: number
    keyword?: string
    minMismatchRate?: number
    maxMismatchRate?: number
  } = {}): Promise<RankingQueryResponse> {
    const result: ApiResponse<RankingQueryResponse> = await http.get(`${API_BASE}/ranking`, {
      params: {
        timeRange: params.timeRange || '7d',
        page: params.page || 1,
        size: params.size || 20,
        ...( params.keyword && { keyword: params.keyword }),
        ...( params.minMismatchRate !== undefined && { minMismatchRate: params.minMismatchRate }),
        ...( params.maxMismatchRate !== undefined && { maxMismatchRate: params.maxMismatchRate })
      }
    })
    return result.data
  }

  /**
   * 获取关键词详细信息
   */
  static async getDetail(keyword: string, timeRange: string = '7d'): Promise<MismatchKeywordRanking> {
    const result: ApiResponse<MismatchKeywordRanking> = await http.get(`${API_BASE}/detail`, {
      params: { keyword, timeRange }
    })
    return result.data
  }

  /**
   * 导出排行榜数据
   */
  static async exportData(request: RankingQueryRequest): Promise<string> {
    const result: ApiResponse<string> = await http.post(`${API_BASE}/export`, request)
    return result.data
  }

  /**
   * 获取统计概览
   */
  static async getStatistics(timeRange: string = '7d'): Promise<StatisticsInfo> {
    const result: ApiResponse<StatisticsInfo> = await http.get(`${API_BASE}/statistics`, {
      params: { timeRange }
    })
    return result.data
  }
}

export default MismatchKeywordApi