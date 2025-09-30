import { ref, watch } from 'vue'
import type { TableColumn } from '@/types/tableData'

/**
 * 列配置管理 Composable
 * 负责管理表格列的配置,包括顺序、显示/隐藏状态,并持久化到 localStorage
 */
export function useColumnConfig(searchSpaceId: string | number) {
  const storageKey = `column-config-${searchSpaceId}`

  /**
   * 保存列配置到 localStorage
   */
  function saveConfig(columns: TableColumn[]) {
    try {
      const config = columns.map(col => ({
        key: col.key,
        visible: col.visible ?? true,
        width: col.width,
        fixed: col.fixed,
        order: columns.indexOf(col)
      }))

      localStorage.setItem(storageKey, JSON.stringify(config))
      console.log(`列配置已保存到 localStorage: ${storageKey}`, config)
    } catch (error) {
      console.error('保存列配置失败:', error)
    }
  }

  /**
   * 从 localStorage 加载列配置
   */
  function loadConfig(): Record<string, any> | null {
    try {
      const stored = localStorage.getItem(storageKey)
      if (!stored) return null

      const config = JSON.parse(stored)
      console.log(`从 localStorage 加载列配置: ${storageKey}`, config)
      return config
    } catch (error) {
      console.error('加载列配置失败:', error)
      return null
    }
  }

  /**
   * 应用保存的配置到列数组
   * @param allColumns 所有可用的列
   * @returns 应用配置后的列数组
   */
  function applyConfig(allColumns: TableColumn[]): TableColumn[] {
    const savedConfig = loadConfig()
    if (!savedConfig || !Array.isArray(savedConfig)) {
      return allColumns
    }

    // 创建配置映射
    const configMap = new Map<string, any>()
    savedConfig.forEach((item: any) => {
      configMap.set(item.key, item)
    })

    // 应用配置到列
    const configuredColumns = allColumns.map(col => {
      const config = configMap.get(col.key)
      if (!config) return col

      return {
        ...col,
        visible: config.visible ?? col.visible,
        width: config.width ?? col.width,
        fixed: config.fixed ?? col.fixed
      }
    })

    // 按保存的顺序排序
    configuredColumns.sort((a, b) => {
      const aConfig = configMap.get(a.key)
      const bConfig = configMap.get(b.key)

      const aOrder = aConfig?.order ?? 999
      const bOrder = bConfig?.order ?? 999

      return aOrder - bOrder
    })

    return configuredColumns
  }

  /**
   * 清除配置
   */
  function clearConfig() {
    try {
      localStorage.removeItem(storageKey)
      console.log(`列配置已清除: ${storageKey}`)
    } catch (error) {
      console.error('清除列配置失败:', error)
    }
  }

  /**
   * 检查是否存在保存的配置
   */
  function hasConfig(): boolean {
    return !!localStorage.getItem(storageKey)
  }

  return {
    saveConfig,
    loadConfig,
    applyConfig,
    clearConfig,
    hasConfig
  }
}