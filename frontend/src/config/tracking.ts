/**
 * 点击追踪配置管理
 */

export interface TrackingConfig {
  // 是否启用追踪
  enabled: boolean
  // 批处理大小
  batchSize: number
  // 重试次数
  retryAttempts: number
  // 重试延迟 (毫秒)
  retryDelay: number
  // 是否启用离线存储
  offlineStorage: boolean
  // 调试模式
  debugMode: boolean
  // 最大离线缓存数量
  maxOfflineItems: number
  // 自动同步间隔 (毫秒)
  autoSyncInterval: number
  // 请求超时时间 (毫秒)
  requestTimeout: number
}

// 默认配置
export const defaultTrackingConfig: TrackingConfig = {
  enabled: true,
  batchSize: 10,
  retryAttempts: 3,
  retryDelay: 1000,
  offlineStorage: true,
  debugMode: import.meta.env.DEV,
  maxOfflineItems: 100,
  autoSyncInterval: 30000, // 30秒
  requestTimeout: 5000     // 5秒
}

// 当前配置
export const trackingConfig: TrackingConfig = { ...defaultTrackingConfig }

/**
 * 更新追踪配置
 */
export const updateTrackingConfig = (updates: Partial<TrackingConfig>): void => {
  Object.assign(trackingConfig, updates)

  if (trackingConfig.debugMode) {
    console.debug('追踪配置已更新:', trackingConfig)
  }
}

/**
 * 重置为默认配置
 */
export const resetTrackingConfig = (): void => {
  Object.assign(trackingConfig, defaultTrackingConfig)

  if (trackingConfig.debugMode) {
    console.debug('追踪配置已重置为默认值')
  }
}

/**
 * 验证配置有效性
 */
export const validateTrackingConfig = (config: Partial<TrackingConfig>): string[] => {
  const errors: string[] = []

  if (config.batchSize !== undefined) {
    if (config.batchSize < 1 || config.batchSize > 100) {
      errors.push('批处理大小必须在 1-100 之间')
    }
  }

  if (config.retryAttempts !== undefined) {
    if (config.retryAttempts < 0 || config.retryAttempts > 10) {
      errors.push('重试次数必须在 0-10 之间')
    }
  }

  if (config.retryDelay !== undefined) {
    if (config.retryDelay < 100 || config.retryDelay > 10000) {
      errors.push('重试延迟必须在 100-10000ms 之间')
    }
  }

  if (config.maxOfflineItems !== undefined) {
    if (config.maxOfflineItems < 10 || config.maxOfflineItems > 1000) {
      errors.push('最大离线缓存数量必须在 10-1000 之间')
    }
  }

  if (config.autoSyncInterval !== undefined) {
    if (config.autoSyncInterval < 5000 || config.autoSyncInterval > 300000) {
      errors.push('自动同步间隔必须在 5000-300000ms 之间')
    }
  }

  if (config.requestTimeout !== undefined) {
    if (config.requestTimeout < 1000 || config.requestTimeout > 30000) {
      errors.push('请求超时时间必须在 1000-30000ms 之间')
    }
  }

  return errors
}

/**
 * 获取配置的只读副本
 */
export const getTrackingConfig = (): Readonly<TrackingConfig> => {
  return Object.freeze({ ...trackingConfig })
}