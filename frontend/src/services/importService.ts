import http from '@/utils/http'

// 文件上传响应
export interface FileUploadResponse {
  taskId: string
  fileName: string
  fileSize: number
  recordCount: number
  uploadTime: string
  message: string
}

// 同步导入响应接口
export interface ImportSyncResponse {
  success: boolean
  totalRecords: number
  successRecords: number
  failedRecords: number
  skippedRecords: number
  startTime: string
  endTime: string
  processingTimeMs: number
  errors?: string[]
  warnings?: string[]
  message: string
}

// 导入任务状态
export interface ImportTaskStatus {
  taskId: string
  searchSpaceId: number
  state: 'pending' | 'running' | 'analyzing_data' | 'creating_index' | 'processing_data' | 'optimizing_index' | 'completed' | 'failed' | 'cancelled'
  totalRecords?: number
  processedRecords?: number
  successCount?: number
  errorCount?: number
  currentBatch?: number
  totalBatches?: number
  progressPercentage?: number
  startTime?: string
  endTime?: string
  errorMessage?: string
  errorDetails?: string[]
  statusMessage?: string
  estimatedRemainingTime?: number
  resultSummary?: {
    success: boolean
    durationMs: number
    createdIndexName: string
    indexedDocuments: number
    averageSpeed: number
    indexSize: number
  }
}

// 导入执行请求
export interface ImportExecuteRequest {
  taskId: string
  mode: 'APPEND' | 'REPLACE'
  batchSize: number
  errorHandling: 'STOP_ON_ERROR' | 'SKIP_ERROR'
  enableIndexOptimization?: boolean
  customMappingConfig?: string
}

// API响应包装类型
interface ApiResponse<T> {
  success: boolean
  code: number
  message: string
  data: T
  timestamp: string
}

/**
 * 导入服务API
 */
export class ImportService {

  /**
   * 同步导入JSON文件（新的简化方法）
   */
  static async importFileSync(
    searchSpaceId: number,
    file: File,
    mode: 'APPEND' | 'REPLACE' = 'APPEND',
    batchSize: number = 100, // 降低默认批次大小，避免ES熔断器错误
    errorHandling: 'STOP_ON_ERROR' | 'SKIP_ERROR' = 'SKIP_ERROR'
  ): Promise<ImportSyncResponse> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('mode', mode)
    formData.append('batchSize', batchSize.toString())
    formData.append('errorHandling', errorHandling)

    const response = await http.post<ApiResponse<ImportSyncResponse>>(
      `/search-spaces/${searchSpaceId}/import-sync`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        timeout: 300000 // 5分钟超时，专门用于数据导入
      }
    )

    // 添加调试信息以便排查问题 - 注意：由于http拦截器，response实际上是ApiResponse对象
    console.log('🔍 API响应结构 (已被拦截器处理):', response)
    console.log('🔍 response.success:', (response as any).success)
    console.log('🔍 response.data:', (response as any).data)
    
    // 修正：由于http拦截器返回response.data，这里response实际上是ApiResponse对象
    const apiResponse = response as any as ApiResponse<ImportSyncResponse>
    
    if (!apiResponse.success) {
      throw new Error(apiResponse.message)
    }

    // 确保返回的data字段存在
    if (!apiResponse.data) {
      throw new Error('响应数据格式错误：缺少data字段')
    }

    return apiResponse.data
  }

  /**
   * 上传JSON文件
   */
  static async uploadFile(searchSpaceId: number, file: File): Promise<FileUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await http.post<ApiResponse<FileUploadResponse>>(
      `/search-spaces/${searchSpaceId}/import`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        timeout: 300000 // 5分钟超时，适配大文件上传
      }
    )

    if (!response.data.success) {
      throw new Error(response.data.message)
    }

    return response.data.data
  }

  /**
   * 执行JSON数据导入
   */
  static async executeImport(searchSpaceId: number, request: ImportExecuteRequest): Promise<ImportTaskStatus> {
    const response = await http.post<ApiResponse<ImportTaskStatus>>(
      `/search-spaces/${searchSpaceId}/import/execute`,
      request,
      {
        timeout: 300000 // 5分钟超时，适配数据导入任务
      }
    )

    if (!response.data.success) {
      throw new Error(response.data.message)
    }

    return response.data.data
  }

  /**
   * 查询导入任务状态
   */
  static async getImportStatus(searchSpaceId: number, taskId: string): Promise<ImportTaskStatus> {
    const response = await http.get<ApiResponse<ImportTaskStatus>>(
      `/search-spaces/${searchSpaceId}/import/status/${taskId}`
    )

    if (!response.data.success) {
      throw new Error(response.data.message)
    }

    return response.data.data
  }

  /**
   * 取消导入任务
   */
  static async cancelImport(searchSpaceId: number, taskId: string): Promise<void> {
    const response = await http.delete<ApiResponse<void>>(
      `/search-spaces/${searchSpaceId}/import/cancel/${taskId}`
    )

    if (!response.data.success) {
      throw new Error(response.data.message)
    }
  }

  /**
   * 轮询任务状态
   *
   * @param searchSpaceId 搜索空间ID
   * @param taskId 任务ID
   * @param onUpdate 状态更新回调
   * @param interval 轮询间隔（毫秒，默认1000）
   * @returns 取消轮询的函数
   */
  static pollImportStatus(
    searchSpaceId: number,
    taskId: string,
    onUpdate: (status: ImportTaskStatus) => void,
    onError?: (error: Error) => void,
    interval: number = 1000
  ): () => void {
    let cancelled = false

    const poll = async () => {
      if (cancelled) return

      try {
        const status = await this.getImportStatus(searchSpaceId, taskId)
        onUpdate(status)

        // 如果任务已完成（成功、失败或取消），停止轮询
        if (status.state === 'completed' || status.state === 'failed' || status.state === 'cancelled') {
          return
        }

        // 继续轮询
        setTimeout(poll, interval)

      } catch (error) {
        if (onError) {
          onError(error as Error)
        } else {
          console.error('轮询任务状态失败:', error)
        }

        // 发生错误时继续轮询（可能是网络暂时问题）
        if (!cancelled) {
          setTimeout(poll, interval * 2) // 错误时增加轮询间隔
        }
      }
    }

    // 立即执行第一次轮询
    poll()

    // 返回取消轮询的函数
    return () => {
      cancelled = true
    }
  }

  /**
   * 获取状态描述文本
   */
  static getStateDescription(state: ImportTaskStatus['state']): string {
    const descriptions: Record<ImportTaskStatus['state'], string> = {
      pending: '等待开始',
      running: '正在运行',
      analyzing_data: '分析数据',
      creating_index: '创建索引',
      processing_data: '处理数据',
      optimizing_index: '优化索引',
      completed: '已完成',
      failed: '已失败',
      cancelled: '已取消'
    }

    return descriptions[state] || '未知状态'
  }

  /**
   * 检查任务是否正在运行
   */
  static isTaskRunning(state: ImportTaskStatus['state']): boolean {
    return ['running', 'analyzing_data', 'creating_index', 'processing_data', 'optimizing_index'].includes(state)
  }

  /**
   * 检查任务是否已完成（包括成功、失败、取消）
   */
  static isTaskFinished(state: ImportTaskStatus['state']): boolean {
    return ['completed', 'failed', 'cancelled'].includes(state)
  }

  /**
   * 检查任务是否可以取消
   */
  static canCancelTask(state: ImportTaskStatus['state']): boolean {
    return ['pending', 'running', 'analyzing_data', 'creating_index', 'processing_data', 'optimizing_index'].includes(state)
  }

  /**
   * 格式化进度百分比
   */
  static formatProgress(progress?: number): string {
    if (typeof progress !== 'number') return '0%'
    return `${Math.round(progress)}%`
  }

  /**
   * 格式化持续时间
   */
  static formatDuration(startTime?: string, endTime?: string): string {
    if (!startTime) return '--'

    const start = new Date(startTime)
    const end = endTime ? new Date(endTime) : new Date()
    const duration = end.getTime() - start.getTime()

    const seconds = Math.floor(duration / 1000) % 60
    const minutes = Math.floor(duration / (1000 * 60)) % 60
    const hours = Math.floor(duration / (1000 * 60 * 60))

    if (hours > 0) {
      return `${hours}h ${minutes}m ${seconds}s`
    } else if (minutes > 0) {
      return `${minutes}m ${seconds}s`
    } else {
      return `${seconds}s`
    }
  }

  /**
   * 格式化速度
   */
  static formatSpeed(speed?: number): string {
    if (typeof speed !== 'number') return '--'

    if (speed < 1) {
      return '<1 条/秒'
    } else if (speed < 1000) {
      return `${Math.round(speed)} 条/秒`
    } else {
      return `${(speed / 1000).toFixed(1)}K 条/秒`
    }
  }
}