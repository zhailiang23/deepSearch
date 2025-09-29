/**
 * 热门话题相关类型定义
 */

/**
 * 热门话题实体
 */
export interface HotTopic {
  id: number
  name: string
  popularity: number
  visible: boolean
  createdAt: string
  updatedAt: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 创建热门话题请求
 */
export interface CreateHotTopicRequest {
  name: string
  popularity: number
  visible: boolean
}

/**
 * 更新热门话题请求
 */
export interface UpdateHotTopicRequest {
  name: string
  popularity: number
  visible: boolean
}

/**
 * 热门话题查询参数
 */
export interface HotTopicQueryParams {
  page?: number
  size?: number
  keyword?: string
  visible?: boolean
  sort?: string
  direction?: 'ASC' | 'DESC'
}

/**
 * 热门话题统计信息
 */
export interface HotTopicStatistics {
  visibleTopics: number
  invisibleTopics: number
  totalTopics: number
  avgPopularity: number
}