import http from '@/utils/http'

/**
 * 批量创建话题项
 */
export interface TopicItem {
  name: string
  popularity: number
  visible?: boolean
}

/**
 * 批量创建热门话题请求
 */
export interface BatchCreateHotTopicRequest {
  topics: TopicItem[]
  skipExisting?: boolean
}

/**
 * 热门话题DTO
 */
export interface HotTopicDTO {
  id: number
  name: string
  popularity: number
  visible: boolean
}

/**
 * 批量创建热门话题响应
 */
export interface BatchCreateHotTopicResponse {
  successCount: number
  skippedCount: number
  failedCount: number
  createdTopics: HotTopicDTO[]
  skippedTopics: string[]
  failedTopics: string[]
}

/**
 * 批量创建热门话题
 */
export async function batchCreateHotTopics(
  request: BatchCreateHotTopicRequest
): Promise<{ data: BatchCreateHotTopicResponse }> {
  return http.post('/hot-topics/batch', request)
}
