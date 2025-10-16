import http from '@/utils/http'

/**
 * ApiResponse 包装类型
 */
interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  code?: string
  timestamp?: string
}

export interface QueryUnderstandingRequest {
  query: string
  includeDetails?: boolean
}

export interface QueryUnderstandingResponse {
  originalQuery: string
  normalizedQuery: string
  correctedQuery: string
  expandedQuery?: string
  rewrittenQuery?: string
  finalQuery: string
  currentQuery: string
  entities?: Entity[]
  intent?: string
  intentConfidence?: number
  synonyms?: string[]
  relatedTerms?: string[]
  detectedPhrases?: string[]
  hotTopics?: string[]
  metadata?: Record<string, any>
  processorTimings?: Record<string, number>
  totalProcessingTime: number
}

export interface Entity {
  text: string
  type: string
  confidence: number
  startPosition: number
  endPosition: number
  normalizedForm: string
}

export interface MetricsSummary {
  totalQueries: number
  cacheHits: number
  cacheMisses: number
  cacheHitRate: number
  simpleQueries: number
  complexQueries: number
  timeoutQueries: number
  errorQueries: number
  averagePipelineTime: number
  lastQueryTime: number
}

export interface ProcessorMetric {
  name: string
  priority: number
  enabled: boolean
  averageTime: number
  errorRate: number
}

/**
 * 执行查询理解
 */
export async function understandQuery(data: QueryUnderstandingRequest) {
  // 默认请求详细信息
  const requestData = {
    ...data,
    includeDetails: data.includeDetails !== false
  }
  const response = await http.post<ApiResponse<QueryUnderstandingResponse>>('/query-understanding/understand', requestData)
  return response.data
}

/**
 * 获取指标摘要
 */
export async function getMetricsSummary() {
  const response = await http.get<ApiResponse<MetricsSummary>>('/query-understanding/metrics/summary')
  return response.data
}

/**
 * 获取处理器指标
 */
export async function getProcessorMetrics() {
  const response = await http.get<ApiResponse<Record<string, ProcessorMetric>>>('/query-understanding/metrics/processors')
  return response.data
}

/**
 * 重置指标
 */
export async function resetMetrics() {
  const response = await http.post<ApiResponse<string>>('/query-understanding/metrics/reset')
  return response.data
}

/**
 * 获取管道配置
 */
export async function getPipelineConfig() {
  const response = await http.get<ApiResponse<{
    processorCount: number
    processors: Array<{
      name: string
      priority: number
      enabled: boolean
    }>
  }>>('/query-understanding/metrics/pipeline/config')
  return response.data
}
