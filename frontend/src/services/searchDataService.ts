import http from '@/utils/http'
import type {
  TableRow,
  ESIndexMapping
} from '@/types/tableData'

// 搜索数据请求参数
export interface SearchDataRequest {
  searchSpaceId: string
  query?: string
  page?: number
  size?: number
  sort?: {
    field: string
    order: 'asc' | 'desc'
  }
  filters?: Array<{
    field: string
    value: any
    operator: 'eq' | 'contains' | 'startsWith' | 'endsWith' | 'range' | 'in'
  }>
  channel?: string
}

// 搜索数据响应
export interface SearchDataResponse {
  data: TableRow[]
  total: number
  page: number
  size: number
  mapping?: ESIndexMapping
}

// 文档更新请求
export interface UpdateDocumentRequest {
  id: string
  index: string
  source: Record<string, any>
  version?: number // 乐观锁版本号
}

// 文档更新响应
export interface UpdateDocumentResponse {
  id: string
  index: string
  version: number
  result: 'created' | 'updated' | 'noop'
}

// 删除文档请求
export interface DeleteDocumentRequest {
  id: string
  index: string
  version?: number
}

// 删除文档响应
export interface DeleteDocumentResponse {
  id: string
  index: string
  version: number
  result: 'deleted' | 'not_found'
}

// 获取索引映射响应
export interface IndexMappingResponse {
  index: string
  mapping: ESIndexMapping
}

/**
 * 搜索数据管理API服务
 */
export const searchDataService = {
  /**
   * 搜索ES数据
   */
  search: (params: SearchDataRequest): Promise<SearchDataResponse> =>
    http.post('/elasticsearch/search', params),

  /**
   * 获取索引映射
   */
  getIndexMapping: (searchSpaceId: string): Promise<IndexMappingResponse> =>
    http.get(`/elasticsearch/mapping/${searchSpaceId}`),

  /**
   * 更新文档
   */
  updateDocument: (params: UpdateDocumentRequest): Promise<UpdateDocumentResponse> =>
    http.put(`/elasticsearch/document/${params.id}`, {
      index: params.index,
      source: params.source,
      version: params.version
    }),

  /**
   * 删除文档
   */
  deleteDocument: (params: DeleteDocumentRequest): Promise<DeleteDocumentResponse> =>
    http.delete(`/elasticsearch/document/${params.id}`, {
      data: {
        index: params.index,
        version: params.version
      }
    }),

  /**
   * 获取文档详情
   */
  getDocument: (id: string, index: string): Promise<TableRow> =>
    http.get(`/elasticsearch/document/${id}`, {
      params: { index }
    }),

  /**
   * 批量操作文档
   */
  bulkOperation: (operations: Array<{
    action: 'index' | 'update' | 'delete'
    id: string
    index: string
    source?: Record<string, any>
  }>): Promise<{
    took: number
    errors: boolean
    items: Array<any>
  }> =>
    http.post('/elasticsearch/bulk', { operations }),

  /**
   * 更新文档渠道配置
   */
  updateDocumentChannels: (id: string, index: string, channels: string[]): Promise<UpdateDocumentResponse> =>
    http.put(`/elasticsearch/document/${id}/channels`, channels, {
      params: { index }
    }),

  /**
   * 更新文档角色配置
   */
  updateDocumentRoles: (id: string, index: string, roles: string[]): Promise<UpdateDocumentResponse> =>
    http.put(`/elasticsearch/document/${id}/roles`, roles, {
      params: { index }
    }),

  /**
   * 推荐文档
   */
  recommendDocument: (id: string, index: string): Promise<UpdateDocumentResponse> =>
    http.put(`/elasticsearch/document/${id}/recommend`, null, {
      params: { index }
    }),

  /**
   * 取消推荐文档
   */
  unrecommendDocument: (id: string, index: string): Promise<UpdateDocumentResponse> =>
    http.delete(`/elasticsearch/document/${id}/recommend`, {
      params: { index }
    })
}