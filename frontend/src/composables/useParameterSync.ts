import { ref, watch, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type {
  SearchDemoConfig,
  ParameterChangeEvent,
  ParameterConflict,
  SyncStatus
} from '@/types/demo'

export interface UseParameterSyncOptions {
  /** 是否启用URL同步 */
  enableUrlSync?: boolean
  /** 是否启用实时同步 */
  enableRealtimeSync?: boolean
  /** 同步防抖延迟（毫秒） */
  syncDebounceMs?: number
  /** URL参数前缀 */
  urlParamPrefix?: string
  /** 冲突解决策略 */
  defaultConflictResolution?: 'usePanel' | 'useMobile' | 'merge' | 'ask'
  /** 是否记录变更历史 */
  enableChangeHistory?: boolean
  /** 最大变更历史条数 */
  maxChangeHistoryItems?: number
}

export interface UseParameterSyncReturn {
  // 同步控制
  enableSync: (enabled: boolean) => void
  syncStatus: Readonly<Ref<SyncStatus>>
  pendingChanges: Readonly<Ref<ParameterChangeEvent[]>>

  // 同步操作
  syncToMobile: (config: SearchDemoConfig) => void
  syncFromMobile: () => SearchDemoConfig | null
  batchSync: (changes: ParameterChangeEvent[]) => void
  forceSyncNow: () => Promise<void>

  // 冲突处理
  resolveConflicts: (conflicts: ParameterConflict[]) => void
  detectConflicts: (newConfig: SearchDemoConfig) => ParameterConflict[]
  getConflicts: () => ParameterConflict[]

  // URL同步
  serializeToUrl: (config: SearchDemoConfig) => string
  deserializeFromUrl: (url?: string) => SearchDemoConfig | null
  syncToUrl: (config: SearchDemoConfig) => void
  syncFromUrl: () => SearchDemoConfig | null

  // 变更历史
  getChangeHistory: () => ParameterChangeEvent[]
  clearChangeHistory: () => void
  undoLastChange: () => ParameterChangeEvent | null
  redoChange: () => ParameterChangeEvent | null

  // 工具方法
  compareConfigs: (config1: SearchDemoConfig, config2: SearchDemoConfig) => ParameterChangeEvent[]
  applyChanges: (config: SearchDemoConfig, changes: ParameterChangeEvent[]) => SearchDemoConfig
  validateConfig: (config: SearchDemoConfig) => { valid: boolean; errors: string[] }

  // 生命周期
  cleanup: () => void
}

const DEFAULT_OPTIONS: Required<UseParameterSyncOptions> = {
  enableUrlSync: true,
  enableRealtimeSync: true,
  syncDebounceMs: 300,
  urlParamPrefix: 'demo',
  defaultConflictResolution: 'ask',
  enableChangeHistory: true,
  maxChangeHistoryItems: 50
}

/**
 * 参数同步管理 Composable
 */
export function useParameterSync(options: UseParameterSyncOptions = {}): UseParameterSyncReturn {
  const opts = { ...DEFAULT_OPTIONS, ...options }
  const router = useRouter()
  const route = useRoute()

  // ==================== 状态定义 ====================

  /** 同步状态 */
  const syncStatus = ref<SyncStatus>({
    pending: false,
    error: undefined,
    lastSync: Date.now()
  })

  /** 待同步的参数变更 */
  const pendingChanges = ref<ParameterChangeEvent[]>([])

  /** 参数冲突列表 */
  const conflicts = ref<ParameterConflict[]>([])

  /** 变更历史 */
  const changeHistory = ref<ParameterChangeEvent[]>([])

  /** 撤销栈 */
  const undoStack = ref<ParameterChangeEvent[]>([])

  /** 重做栈 */
  const redoStack = ref<ParameterChangeEvent[]>([])

  /** 是否启用同步 */
  const syncEnabled = ref(opts.enableRealtimeSync)

  /** 同步防抖计时器 */
  let syncTimer: number | null = null

  /** 当前配置缓存 */
  const configCache = ref<SearchDemoConfig | null>(null)

  // ==================== 计算属性 ====================

  /** 是否有待同步的变更 */
  const hasPendingChanges = computed(() => pendingChanges.value.length > 0)

  /** 是否有冲突 */
  const hasConflicts = computed(() => conflicts.value.length > 0)

  /** 是否可以撤销 */
  const canUndo = computed(() => undoStack.value.length > 0)

  /** 是否可以重做 */
  const canRedo = computed(() => redoStack.value.length > 0)

  // ==================== 核心同步方法 ====================

  /**
   * 启用/禁用同步
   */
  const enableSync = (enabled: boolean): void => {
    syncEnabled.value = enabled

    if (enabled && hasPendingChanges.value) {
      // 如果启用同步且有待同步变更，立即同步
      forceSyncNow()
    }
  }

  /**
   * 同步到移动端界面
   */
  const syncToMobile = (config: SearchDemoConfig): void => {
    if (!syncEnabled.value) {
      // 如果同步未启用，将变更加入队列
      const change: ParameterChangeEvent = {
        type: 'behavior',
        key: 'full-config',
        value: config,
        previous: configCache.value,
        timestamp: Date.now()
      }
      pendingChanges.value.push(change)
      return
    }

    try {
      syncStatus.value.pending = true
      syncStatus.value.error = undefined

      // 检测冲突
      if (configCache.value) {
        const detectedConflicts = detectConflicts(config)
        if (detectedConflicts.length > 0) {
          conflicts.value = detectedConflicts
          // 根据策略自动解决或等待用户处理
          if (opts.defaultConflictResolution !== 'ask') {
            resolveConflicts(detectedConflicts)
          }
        }
      }

      // 更新缓存
      configCache.value = { ...config }

      // 同步到URL
      if (opts.enableUrlSync) {
        syncToUrl(config)
      }

      // 触发同步事件
      dispatchSyncEvent('mobile', config)

      syncStatus.value.lastSync = Date.now()
    } catch (error) {
      syncStatus.value.error = error instanceof Error ? error.message : '同步失败'
      console.error('Failed to sync to mobile:', error)
    } finally {
      syncStatus.value.pending = false
    }
  }

  /**
   * 从移动端界面同步
   */
  const syncFromMobile = (): SearchDemoConfig | null => {
    try {
      // 从URL获取配置
      let config: SearchDemoConfig | null = null

      if (opts.enableUrlSync) {
        config = syncFromUrl()
      }

      if (config) {
        configCache.value = config
        syncStatus.value.lastSync = Date.now()
      }

      return config
    } catch (error) {
      syncStatus.value.error = error instanceof Error ? error.message : '从移动端同步失败'
      console.error('Failed to sync from mobile:', error)
      return null
    }
  }

  /**
   * 批量同步变更
   */
  const batchSync = (changes: ParameterChangeEvent[]): void => {
    if (!syncEnabled.value) {
      pendingChanges.value.push(...changes)
      return
    }

    try {
      syncStatus.value.pending = true

      changes.forEach(change => {
        // 记录变更历史
        if (opts.enableChangeHistory) {
          addToChangeHistory(change)
        }
      })

      // 合并变更到当前配置
      if (configCache.value) {
        const updatedConfig = applyChanges(configCache.value, changes)
        syncToMobile(updatedConfig)
      }
    } catch (error) {
      syncStatus.value.error = error instanceof Error ? error.message : '批量同步失败'
      console.error('Failed to batch sync:', error)
    } finally {
      syncStatus.value.pending = false
    }
  }

  /**
   * 强制立即同步
   */
  const forceSyncNow = async (): Promise<void> => {
    if (syncTimer) {
      clearTimeout(syncTimer)
      syncTimer = null
    }

    if (hasPendingChanges.value) {
      batchSync(pendingChanges.value)
      pendingChanges.value = []
    }
  }

  // ==================== 冲突处理 ====================

  /**
   * 检测配置冲突
   */
  const detectConflicts = (newConfig: SearchDemoConfig): ParameterConflict[] => {
    if (!configCache.value) return []

    const detectedConflicts: ParameterConflict[] = []
    const current = configCache.value

    // 检查搜索空间选择冲突
    if (JSON.stringify(current.searchSpaces.selected) !== JSON.stringify(newConfig.searchSpaces.selected)) {
      detectedConflicts.push({
        path: 'searchSpaces.selected',
        panelValue: current.searchSpaces.selected,
        mobileValue: newConfig.searchSpaces.selected,
        resolution: opts.defaultConflictResolution,
        description: '搜索空间选择不一致'
      })
    }

    // 检查分页配置冲突
    if (current.pagination.pageSize !== newConfig.pagination.pageSize) {
      detectedConflicts.push({
        path: 'pagination.pageSize',
        panelValue: current.pagination.pageSize,
        mobileValue: newConfig.pagination.pageSize,
        resolution: opts.defaultConflictResolution,
        description: '分页大小设置不一致'
      })
    }

    // 检查搜索行为配置冲突
    if (current.searchBehavior.debounceMs !== newConfig.searchBehavior.debounceMs) {
      detectedConflicts.push({
        path: 'searchBehavior.debounceMs',
        panelValue: current.searchBehavior.debounceMs,
        mobileValue: newConfig.searchBehavior.debounceMs,
        resolution: opts.defaultConflictResolution,
        description: '防抖延迟设置不一致'
      })
    }

    return detectedConflicts
  }

  /**
   * 解决冲突
   */
  const resolveConflicts = (conflictList: ParameterConflict[]): void => {
    const resolvedConfig = { ...configCache.value! }

    conflictList.forEach(conflict => {
      const path = conflict.path.split('.')
      let target: any = resolvedConfig

      // 导航到目标属性
      for (let i = 0; i < path.length - 1; i++) {
        target = target[path[i]]
      }

      const finalKey = path[path.length - 1]

      // 应用解决策略
      switch (conflict.resolution) {
        case 'usePanel':
          target[finalKey] = conflict.panelValue
          break
        case 'useMobile':
          target[finalKey] = conflict.mobileValue
          break
        case 'merge':
          // 简单合并策略（可以根据具体情况扩展）
          if (Array.isArray(conflict.panelValue) && Array.isArray(conflict.mobileValue)) {
            target[finalKey] = [...new Set([...conflict.panelValue, ...conflict.mobileValue])]
          } else {
            target[finalKey] = conflict.mobileValue // 默认使用移动端值
          }
          break
      }
    })

    // 更新配置并清除冲突
    configCache.value = resolvedConfig
    conflicts.value = []

    // 触发同步
    syncToMobile(resolvedConfig)
  }

  /**
   * 获取当前冲突
   */
  const getConflicts = (): ParameterConflict[] => {
    return [...conflicts.value]
  }

  // ==================== URL 同步 ====================

  /**
   * 序列化配置到URL参数
   */
  const serializeToUrl = (config: SearchDemoConfig): string => {
    try {
      const params = new URLSearchParams()
      const prefix = opts.urlParamPrefix

      // 序列化关键配置项
      if (config.searchSpaces.selected.length > 0) {
        params.set(`${prefix}_spaces`, config.searchSpaces.selected.join(','))
      }

      if (config.pagination.pageSize !== 20) {
        params.set(`${prefix}_pageSize`, config.pagination.pageSize.toString())
      }

      if (config.searchBehavior.debounceMs !== 300) {
        params.set(`${prefix}_debounce`, config.searchBehavior.debounceMs.toString())
      }

      if (config.pinyinSearch.enabled) {
        params.set(`${prefix}_pinyin`, '1')
        params.set(`${prefix}_pinyinMode`, config.pinyinSearch.mode)
      }

      return params.toString()
    } catch (error) {
      console.error('Failed to serialize config to URL:', error)
      return ''
    }
  }

  /**
   * 从URL参数反序列化配置
   */
  const deserializeFromUrl = (url?: string): SearchDemoConfig | null => {
    try {
      const params = new URLSearchParams(url || window.location.search)
      const prefix = opts.urlParamPrefix

      // 如果没有相关参数，返回null
      const hasRelevantParams = Array.from(params.keys()).some(key => key.startsWith(prefix))
      if (!hasRelevantParams) {
        return null
      }

      // 基础配置
      const config: Partial<SearchDemoConfig> = {}

      // 解析搜索空间
      const spacesParam = params.get(`${prefix}_spaces`)
      if (spacesParam) {
        config.searchSpaces = {
          selected: spacesParam.split(','),
          available: [],
          allowMultiple: true
        }
      }

      // 解析分页配置
      const pageSizeParam = params.get(`${prefix}_pageSize`)
      if (pageSizeParam) {
        const pageSize = parseInt(pageSizeParam, 10)
        if (!isNaN(pageSize)) {
          config.pagination = {
            pageSize,
            initialLoad: pageSize,
            prefetchNext: false
          }
        }
      }

      // 解析搜索行为
      const debounceParam = params.get(`${prefix}_debounce`)
      if (debounceParam) {
        const debounceMs = parseInt(debounceParam, 10)
        if (!isNaN(debounceMs)) {
          config.searchBehavior = {
            debounceMs,
            minQueryLength: 1,
            autoSearch: true,
            highlightMatch: true
          }
        }
      }

      // 解析拼音搜索
      const pinyinParam = params.get(`${prefix}_pinyin`)
      const pinyinModeParam = params.get(`${prefix}_pinyinMode`)
      if (pinyinParam === '1') {
        config.pinyinSearch = {
          enabled: true,
          mode: (pinyinModeParam as any) || 'fuzzy',
          toneIgnore: true,
          segmentMatch: true
        }
      }

      return config as SearchDemoConfig
    } catch (error) {
      console.error('Failed to deserialize config from URL:', error)
      return null
    }
  }

  /**
   * 同步配置到URL
   */
  const syncToUrl = (config: SearchDemoConfig): void => {
    if (!opts.enableUrlSync) return

    try {
      const serialized = serializeToUrl(config)
      const currentParams = new URLSearchParams(window.location.search)

      // 清除之前的参数
      const prefix = opts.urlParamPrefix
      const keysToRemove: string[] = []
      currentParams.forEach((_, key) => {
        if (key.startsWith(prefix)) {
          keysToRemove.push(key)
        }
      })
      keysToRemove.forEach(key => currentParams.delete(key))

      // 添加新参数
      const newParams = new URLSearchParams(serialized)
      newParams.forEach((value, key) => {
        currentParams.set(key, value)
      })

      // 更新URL
      const newUrl = `${window.location.pathname}?${currentParams.toString()}`
      router.replace(newUrl)
    } catch (error) {
      console.error('Failed to sync config to URL:', error)
    }
  }

  /**
   * 从URL同步配置
   */
  const syncFromUrl = (): SearchDemoConfig | null => {
    return deserializeFromUrl(route.query as any)
  }

  // ==================== 变更历史 ====================

  /**
   * 添加到变更历史
   */
  const addToChangeHistory = (change: ParameterChangeEvent): void => {
    changeHistory.value.unshift(change)

    // 限制历史条数
    if (changeHistory.value.length > opts.maxChangeHistoryItems) {
      changeHistory.value = changeHistory.value.slice(0, opts.maxChangeHistoryItems)
    }

    // 添加到撤销栈
    undoStack.value.push(change)

    // 清空重做栈
    redoStack.value = []
  }

  /**
   * 获取变更历史
   */
  const getChangeHistory = (): ParameterChangeEvent[] => {
    return [...changeHistory.value]
  }

  /**
   * 清空变更历史
   */
  const clearChangeHistory = (): void => {
    changeHistory.value = []
    undoStack.value = []
    redoStack.value = []
  }

  /**
   * 撤销最后一次变更
   */
  const undoLastChange = (): ParameterChangeEvent | null => {
    const lastChange = undoStack.value.pop()
    if (!lastChange) return null

    // 添加到重做栈
    redoStack.value.push(lastChange)

    return lastChange
  }

  /**
   * 重做变更
   */
  const redoChange = (): ParameterChangeEvent | null => {
    const change = redoStack.value.pop()
    if (!change) return null

    // 添加回撤销栈
    undoStack.value.push(change)

    return change
  }

  // ==================== 工具方法 ====================

  /**
   * 比较两个配置的差异
   */
  const compareConfigs = (config1: SearchDemoConfig, config2: SearchDemoConfig): ParameterChangeEvent[] => {
    const changes: ParameterChangeEvent[] = []

    // 递归比较对象属性
    const compare = (obj1: any, obj2: any, path: string[] = []) => {
      for (const key in obj1) {
        const currentPath = [...path, key]
        const pathStr = currentPath.join('.')

        if (typeof obj1[key] === 'object' && obj1[key] !== null && !Array.isArray(obj1[key])) {
          // 递归比较对象
          if (obj2[key]) {
            compare(obj1[key], obj2[key], currentPath)
          }
        } else if (JSON.stringify(obj1[key]) !== JSON.stringify(obj2[key])) {
          // 值不同，记录变更
          changes.push({
            type: 'behavior', // 可以根据路径确定更具体的类型
            key: pathStr,
            value: obj2[key],
            previous: obj1[key],
            timestamp: Date.now()
          })
        }
      }
    }

    compare(config1, config2)
    return changes
  }

  /**
   * 应用变更到配置
   */
  const applyChanges = (config: SearchDemoConfig, changes: ParameterChangeEvent[]): SearchDemoConfig => {
    const newConfig = JSON.parse(JSON.stringify(config))

    changes.forEach(change => {
      const path = change.key.split('.')
      let target: any = newConfig

      // 导航到目标属性
      for (let i = 0; i < path.length - 1; i++) {
        if (!target[path[i]]) {
          target[path[i]] = {}
        }
        target = target[path[i]]
      }

      // 设置新值
      target[path[path.length - 1]] = change.value
    })

    return newConfig
  }

  /**
   * 验证配置
   */
  const validateConfig = (config: SearchDemoConfig): { valid: boolean; errors: string[] } => {
    const errors: string[] = []

    // 验证搜索空间
    if (!config.searchSpaces || !Array.isArray(config.searchSpaces.selected)) {
      errors.push('搜索空间配置无效')
    }

    // 验证分页配置
    if (!config.pagination || config.pagination.pageSize <= 0) {
      errors.push('分页配置无效')
    }

    // 验证搜索行为
    if (!config.searchBehavior || config.searchBehavior.debounceMs < 0) {
      errors.push('搜索行为配置无效')
    }

    return {
      valid: errors.length === 0,
      errors
    }
  }

  // ==================== 事件处理 ====================

  /**
   * 触发同步事件
   */
  const dispatchSyncEvent = (source: 'panel' | 'mobile' | 'url', config: SearchDemoConfig): void => {
    const event = new CustomEvent('parameterSync', {
      detail: {
        source,
        config,
        timestamp: Date.now()
      }
    })
    window.dispatchEvent(event)
  }

  /**
   * 防抖同步
   */
  const debouncedSync = (config: SearchDemoConfig): void => {
    if (syncTimer) {
      clearTimeout(syncTimer)
    }

    syncTimer = window.setTimeout(() => {
      syncToMobile(config)
      syncTimer = null
    }, opts.syncDebounceMs)
  }

  // ==================== 清理 ====================

  /**
   * 清理资源
   */
  const cleanup = (): void => {
    if (syncTimer) {
      clearTimeout(syncTimer)
      syncTimer = null
    }
  }

  // ==================== 生命周期 ====================

  // 监听URL变化
  watch(
    () => route.query,
    () => {
      if (opts.enableUrlSync) {
        const urlConfig = syncFromUrl()
        if (urlConfig) {
          configCache.value = urlConfig
          syncStatus.value.lastSync = Date.now()
        }
      }
    },
    { deep: true }
  )

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup()
  })

  // ==================== 返回接口 ====================

  return {
    // 同步控制
    enableSync,
    syncStatus,
    pendingChanges,

    // 同步操作
    syncToMobile,
    syncFromMobile,
    batchSync,
    forceSyncNow,

    // 冲突处理
    resolveConflicts,
    detectConflicts,
    getConflicts,

    // URL同步
    serializeToUrl,
    deserializeFromUrl,
    syncToUrl,
    syncFromUrl,

    // 变更历史
    getChangeHistory,
    clearChangeHistory,
    undoLastChange,
    redoChange,

    // 工具方法
    compareConfigs,
    applyChanges,
    validateConfig,

    // 生命周期
    cleanup
  }
}