import http from '@/utils/http'

/**
 * 聚类分析请求
 */
export interface ClusterAnalysisRequest {
  /** 时间范围代码 (7d, 30d, 90d) */
  timeRange: string
  /** DBSCAN eps 参数 */
  eps: number
  /** DBSCAN min_samples 参数 */
  minSamples: number
  /** 距离度量方式 */
  metric: string
}

/**
 * 散点数据
 */
export interface ScatterPoint {
  /** X 坐标 */
  x: number
  /** Y 坐标 */
  y: number
  /** 簇 ID (-1 表示噪声点) */
  cluster: number
  /** 文本内容 */
  text: string
}

/**
 * 聚类话题
 */
export interface ClusterTopic {
  /** 簇 ID */
  clusterId: number
  /** 话题名称 */
  topic: string
  /** 业务标签 */
  tags: string[]
  /** 代表性问题 */
  examples: string[]
  /** 簇大小 */
  size: number
}

/**
 * 聚类分析响应
 */
export interface ClusterAnalysisResponse {
  /** 聚类话题列表 */
  clusters: ClusterTopic[]
  /** 散点图数据 */
  scatterData: ScatterPoint[]
  /** 噪声点数量 */
  noiseCount: number
  /** 总文本数 */
  totalTexts: number
  /** 有效簇数量 */
  validClusters: number
  /** 时间范围描述 */
  timeRangeDesc: string
}

/**
 * 执行聚类分析
 * http.post 的响应拦截器已经解包了 response.data,所以直接返回 ClusterAnalysisResponse
 */
export function analyzeCluster(data: ClusterAnalysisRequest) {
  return http.post<ClusterAnalysisResponse>('/clustering/analyze', data)
}
