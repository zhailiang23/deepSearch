import http from '@/utils/http'

/**
 * 批量转换请求参数
 */
export interface BatchConversionRequest {
  dbHost: string
  dbPort: number
  dbName: string
  dbUsername: string
  dbPassword: string
  tableName: string
  imagePathColumn: string
  imageUrlColumn?: string
  primaryKeyColumn?: string
  indexName?: string
}

/**
 * 单张图片识别结果
 */
export interface ImageRecognitionResult {
  id: string
  imagePath: string
  thumbnailBase64: string
  recognizedText: string
  name?: string
  descript?: string
  link?: string
  startDate?: string
  endDate?: string
  status?: string
  success: boolean
  errorMessage?: string
}

/**
 * 批量转换响应
 */
export interface BatchConversionResponse {
  success: boolean
  message: string
  totalCount: number
  successCount: number
  failureCount: number
  insertedCount: number
  skippedCount: number
  results: ImageRecognitionResult[]
}

/**
 * 批量识别数据库中的图片
 * @param request 批量转换请求参数
 * @returns 批量转换响应
 */
export async function batchConvertImages(
  request: BatchConversionRequest
): Promise<BatchConversionResponse> {
  return http.post<BatchConversionResponse>('/image/batch/convert', request, {
    timeout: 300000 // 5分钟超时，因为批量处理可能需要较长时间
  })
}

/**
 * 健康检查
 * @returns 健康状态消息
 */
export async function checkBatchConversionHealth(): Promise<string> {
  return http.get<string>('/image/batch/health')
}
