/**
 * 点击追踪组合函数
 */

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useLocalStorage } from '@vueuse/core'
import type {
  SearchResult,
  ClickTrackingData,
  ClickCache,
  TrackingState,
  TrackingError,
  ClickRecordRequest
} from '@/types/searchLog'
import { searchLogApi } from '@/api/searchLog'
import { trackingConfig, updateTrackingConfig } from '@/config/tracking'

// 生成唯一ID
const generateId = (): string => {
  return `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

export function useClickTracking() {
  // 追踪状态
  const isTracking = ref(trackingConfig.enabled)
  const isOnline = ref(navigator.onLine)

  // 离线缓存
  const offlineCache = useLocalStorage<ClickCache>('search-click-cache', {})

  // 追踪状态
  const trackingState = reactive<TrackingState>({
    isEnabled: trackingConfig.enabled,
    offlineCount: 0,
    lastSyncTime: undefined,
    errors: []
  })

  // 自动同步定时器
  let syncTimer: number | null = null

  /**
   * 记录点击行为
   */
  const trackClick = async (
    searchLogId: number,
    result: SearchResult,
    clickPosition: number,
    event?: MouseEvent | KeyboardEvent
  ): Promise<void> => {
    if (!isTracking.value) {
      if (trackingConfig.debugMode) {
        console.debug('点击追踪已禁用，跳过记录')
      }
      return
    }

    const clickData: ClickTrackingData = {
      searchLogId,
      documentId: result.id,
      documentTitle: result.title,
      documentUrl: result.url,
      clickPosition,
      clickTime: new Date().toISOString(),
      userAgent: navigator.userAgent,
      clickType: event?.type || 'click',
      modifierKeys: {
        ctrl: event instanceof MouseEvent ? event.ctrlKey : false,
        shift: event instanceof MouseEvent ? event.shiftKey : false,
        alt: event instanceof MouseEvent ? event.altKey : false
      }
    }

    if (trackingConfig.debugMode) {
      console.debug('记录点击数据:', clickData)
    }

    try {
      if (isOnline.value) {
        // 在线时直接发送
        await sendClickData(clickData)
        if (trackingConfig.debugMode) {
          console.debug('点击数据发送成功')
        }
      } else {
        // 离线时缓存
        addToOfflineCache(clickData)
        if (trackingConfig.debugMode) {
          console.debug('点击数据已缓存，等待同步')
        }
      }
    } catch (error: any) {
      console.warn('点击数据发送失败，加入离线缓存:', error)
      addToOfflineCache(clickData)
      addTrackingError(error.message || '发送失败', clickData)
    }
  }

  /**
   * 发送点击数据到后端
   */
  const sendClickData = async (clickData: ClickTrackingData): Promise<void> => {
    const requestData: ClickRecordRequest = {
      searchLogId: clickData.searchLogId,
      documentId: clickData.documentId,
      documentTitle: clickData.documentTitle,
      documentUrl: clickData.documentUrl,
      clickPosition: clickData.clickPosition,
      clickTime: clickData.clickTime,
      userAgent: clickData.userAgent,
      clickType: clickData.clickType,
      modifierKeys: clickData.modifierKeys
    }

    const response = await searchLogApi.recordClick(requestData)

    if (!response.success) {
      throw new Error(response.message || '点击记录失败')
    }
  }

  /**
   * 批量发送点击数据
   */
  const sendClickBatch = async (clicks: ClickTrackingData[]): Promise<void> => {
    const requestData: ClickRecordRequest[] = clicks.map(click => ({
      searchLogId: click.searchLogId,
      documentId: click.documentId,
      documentTitle: click.documentTitle,
      documentUrl: click.documentUrl,
      clickPosition: click.clickPosition,
      clickTime: click.clickTime,
      userAgent: click.userAgent,
      clickType: click.clickType,
      modifierKeys: click.modifierKeys
    }))

    const response = await searchLogApi.recordClicks(requestData)

    if (!response.success) {
      throw new Error(response.message || '批量点击记录失败')
    }
  }

  /**
   * 添加到离线缓存
   */
  const addToOfflineCache = (clickData: ClickTrackingData): void => {
    const cacheKey = `${clickData.searchLogId}`

    if (!offlineCache.value[cacheKey]) {
      offlineCache.value[cacheKey] = []
    }

    offlineCache.value[cacheKey].push(clickData)

    // 检查缓存大小限制
    const totalItems = Object.values(offlineCache.value).flat().length
    if (totalItems > trackingConfig.maxOfflineItems) {
      // 删除最旧的缓存项
      const oldestKey = Object.keys(offlineCache.value)[0]
      if (oldestKey && offlineCache.value[oldestKey].length > 0) {
        offlineCache.value[oldestKey].shift()
        if (offlineCache.value[oldestKey].length === 0) {
          delete offlineCache.value[oldestKey]
        }
      }
    }

    updateTrackingState()
  }

  /**
   * 同步离线缓存数据
   */
  const syncOfflineCache = async (): Promise<void> => {
    if (!isOnline.value) {
      if (trackingConfig.debugMode) {
        console.debug('离线状态，跳过同步')
      }
      return
    }

    const cacheKeys = Object.keys(offlineCache.value)
    if (cacheKeys.length === 0) {
      if (trackingConfig.debugMode) {
        console.debug('没有需要同步的缓存数据')
      }
      return
    }

    if (trackingConfig.debugMode) {
      console.debug('开始同步离线缓存数据，共', cacheKeys.length, '个批次')
    }

    let syncedCount = 0

    for (const key of cacheKeys) {
      const cachedClicks = offlineCache.value[key]
      if (!cachedClicks || cachedClicks.length === 0) continue

      try {
        // 按批处理大小分组发送
        const batches = []
        for (let i = 0; i < cachedClicks.length; i += trackingConfig.batchSize) {
          batches.push(cachedClicks.slice(i, i + trackingConfig.batchSize))
        }

        for (const batch of batches) {
          await sendClickBatch(batch)
          syncedCount += batch.length
        }

        // 同步成功后删除缓存
        delete offlineCache.value[key]

        if (trackingConfig.debugMode) {
          console.debug(`批次 ${key} 同步成功，共 ${cachedClicks.length} 条记录`)
        }
      } catch (error: any) {
        console.error('同步离线点击数据失败:', error)
        addTrackingError(`同步失败: ${error.message}`)
        // 同步失败时保留缓存数据
        break
      }
    }

    if (syncedCount > 0) {
      trackingState.lastSyncTime = new Date().toISOString()
      if (trackingConfig.debugMode) {
        console.debug(`同步完成，共处理 ${syncedCount} 条记录`)
      }
    }

    updateTrackingState()
  }

  /**
   * 批量追踪点击
   */
  const trackMultipleClicks = async (clicks: ClickTrackingData[]): Promise<void> => {
    if (!isTracking.value) return

    for (const click of clicks) {
      await trackClick(
        click.searchLogId,
        {
          id: click.documentId,
          title: click.documentTitle,
          url: click.documentUrl
        } as SearchResult,
        click.clickPosition
      )
    }
  }

  /**
   * 设置追踪状态
   */
  const setTrackingEnabled = (enabled: boolean): void => {
    isTracking.value = enabled
    trackingState.isEnabled = enabled
    updateTrackingConfig({ enabled })

    if (trackingConfig.debugMode) {
      console.debug('追踪状态已更新:', enabled)
    }
  }

  /**
   * 清除离线缓存
   */
  const clearOfflineCache = (): void => {
    offlineCache.value = {}
    updateTrackingState()

    if (trackingConfig.debugMode) {
      console.debug('离线缓存已清除')
    }
  }

  /**
   * 添加追踪错误
   */
  const addTrackingError = (message: string, data?: ClickTrackingData): void => {
    const error: TrackingError = {
      id: generateId(),
      message,
      timestamp: new Date().toISOString(),
      data
    }

    trackingState.errors.push(error)

    // 限制错误日志数量
    if (trackingState.errors.length > 10) {
      trackingState.errors.shift()
    }
  }

  /**
   * 清除追踪错误
   */
  const clearTrackingErrors = (): void => {
    trackingState.errors = []
  }

  /**
   * 更新追踪状态
   */
  const updateTrackingState = (): void => {
    trackingState.offlineCount = Object.values(offlineCache.value).flat().length
  }

  /**
   * 启动自动同步
   */
  const startAutoSync = (): void => {
    if (syncTimer) return

    syncTimer = setInterval(() => {
      if (isOnline.value && trackingState.offlineCount > 0) {
        syncOfflineCache().catch(error => {
          console.error('自动同步失败:', error)
        })
      }
    }, trackingConfig.autoSyncInterval)

    if (trackingConfig.debugMode) {
      console.debug('自动同步已启动，间隔:', trackingConfig.autoSyncInterval, 'ms')
    }
  }

  /**
   * 停止自动同步
   */
  const stopAutoSync = (): void => {
    if (syncTimer) {
      clearInterval(syncTimer)
      syncTimer = null

      if (trackingConfig.debugMode) {
        console.debug('自动同步已停止')
      }
    }
  }

  // 网络状态监听
  const handleOnline = () => {
    isOnline.value = true
    if (trackingConfig.debugMode) {
      console.debug('网络已连接，尝试同步离线数据')
    }
    // 网络恢复时立即同步
    setTimeout(() => {
      syncOfflineCache().catch(error => {
        console.error('网络恢复后同步失败:', error)
      })
    }, 1000)
  }

  const handleOffline = () => {
    isOnline.value = false
    if (trackingConfig.debugMode) {
      console.debug('网络已断开，启用离线模式')
    }
  }

  // 生命周期钩子
  onMounted(() => {
    // 初始化追踪状态
    updateTrackingState()

    // 监听网络状态
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    // 启动自动同步
    startAutoSync()

    // 页面加载时尝试同步离线缓存
    if (isOnline.value && trackingState.offlineCount > 0) {
      setTimeout(() => {
        syncOfflineCache().catch(error => {
          console.error('初始同步失败:', error)
        })
      }, 2000)
    }
  })

  onUnmounted(() => {
    // 清理事件监听器
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)

    // 停止自动同步
    stopAutoSync()
  })

  // 计算属性
  const offlineCacheCount = computed(() => trackingState.offlineCount)
  const hasTrackingErrors = computed(() => trackingState.errors.length > 0)
  const isOffline = computed(() => !isOnline.value)

  return {
    // 状态
    isTracking,
    isOnline,
    isOffline,
    trackingState,
    offlineCacheCount,
    hasTrackingErrors,

    // 方法
    trackClick,
    syncOfflineCache,
    trackMultipleClicks,
    setTrackingEnabled,
    clearOfflineCache,
    clearTrackingErrors,
    startAutoSync,
    stopAutoSync
  }
}