import http from '@/utils/http'
import type { ApiResponse } from '@/types/searchSpace'

const BASE_URL = '/search-spaces'

/**
 * Mapping API 调用模块
 * 提供搜索空间的 Elasticsearch 索引 mapping 配置相关的 API 调用
 */
export const mappingApi = {
  /**
   * 获取索引 mapping 配置
   * @param spaceId 搜索空间ID
   * @returns 索引mapping配置（JSON字符串）
   */
  getMapping: (spaceId: number): Promise<ApiResponse<string>> =>
    http.get(`${BASE_URL}/${spaceId}/mapping`),

  /**
   * 更新索引 mapping 配置
   * @param spaceId 搜索空间ID
   * @param mapping mapping配置（JSON字符串）
   * @returns 更新结果
   */
  updateMapping: (spaceId: number, mapping: string): Promise<ApiResponse<void>> =>
    http.put(`${BASE_URL}/${spaceId}/mapping`, mapping, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
}