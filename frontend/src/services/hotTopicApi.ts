import http from '@/utils/http'
import type {
  HotTopic,
  CreateHotTopicRequest,
  UpdateHotTopicRequest,
  HotTopicQueryParams,
  HotTopicStatistics
} from '@/types/hotTopic'
import type { ApiResponse, PageResult } from '@/types/api'

const API_BASE = '/hot-topics'

/**
 * 热门话题API服务
 */
export class HotTopicApi {
  // ========== 基础CRUD操作 ==========

  /**
   * 创建热门话题
   */
  static async create(data: CreateHotTopicRequest): Promise<HotTopic> {
    const result: ApiResponse<HotTopic> = await http.post(API_BASE, data)
    return result.data
  }

  /**
   * 更新热门话题
   */
  static async update(id: number, data: UpdateHotTopicRequest): Promise<HotTopic> {
    const result: ApiResponse<HotTopic> = await http.put(`${API_BASE}/${id}`, data)
    return result.data
  }

  /**
   * 删除热门话题
   */
  static async delete(id: number): Promise<void> {
    await http.delete(`${API_BASE}/${id}`)
  }

  /**
   * 获取单个热门话题
   */
  static async getById(id: number): Promise<HotTopic> {
    const result: ApiResponse<HotTopic> = await http.get(`${API_BASE}/${id}`)
    return result.data
  }

  /**
   * 分页查询热门话题列表
   */
  static async list(params: HotTopicQueryParams = {}): Promise<PageResult<HotTopic>> {
    const result: ApiResponse<PageResult<HotTopic>> = await http.get(API_BASE, { params })
    return result.data
  }

  /**
   * 搜索热门话题
   */
  static async search(params: HotTopicQueryParams = {}): Promise<PageResult<HotTopic>> {
    const result: ApiResponse<PageResult<HotTopic>> = await http.get(`${API_BASE}/search`, { params })
    return result.data
  }

  /**
   * 获取所有热门话题
   */
  static async getAll(): Promise<HotTopic[]> {
    const result: ApiResponse<HotTopic[]> = await http.get(`${API_BASE}/all`)
    return result.data
  }

  // ========== 特殊操作 ==========

  /**
   * 切换话题可见性
   */
  static async toggleVisibility(id: number): Promise<HotTopic> {
    const result: ApiResponse<HotTopic> = await http.post(`${API_BASE}/${id}/toggle-visibility`)
    return result.data
  }

  /**
   * 更新话题热度
   */
  static async updatePopularity(id: number, popularity: number): Promise<HotTopic> {
    const result: ApiResponse<HotTopic> = await http.patch(`${API_BASE}/${id}/popularity`, null, {
      params: { popularity }
    })
    return result.data
  }

  /**
   * 获取热门话题排行榜
   */
  static async getTopPopular(limit: number = 10): Promise<HotTopic[]> {
    const result: ApiResponse<HotTopic[]> = await http.get(`${API_BASE}/top`, {
      params: { limit }
    })
    return result.data
  }

  // ========== 验证接口 ==========

  /**
   * 检查话题名称是否可用
   */
  static async isNameAvailable(name: string, excludeId?: number): Promise<boolean> {
    const params: any = { name }
    if (excludeId !== undefined) {
      params.excludeId = excludeId
    }
    const result: ApiResponse<boolean> = await http.get(`${API_BASE}/name-available`, { params })
    return result.data
  }

  // ========== 统计接口 ==========

  /**
   * 获取话题统计信息
   */
  static async getStatistics(): Promise<HotTopicStatistics> {
    const result: ApiResponse<HotTopicStatistics> = await http.get(`${API_BASE}/statistics`)
    return result.data
  }
}

export default HotTopicApi