<template>
  <div class="click-log-table">
    <!-- 表格头部 -->
    <div class="table-header">
      <h4 class="table-title">点击记录详情</h4>
      <div class="click-summary">
        共 {{ clicks.length }} 次点击
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <SortableHeader
                field="clickPosition"
                :current-sort="currentSort"
                @sort="handleSort"
              >
                点击位置
              </SortableHeader>
            </TableHead>
            <TableHead>文档信息</TableHead>
            <TableHead>
              <SortableHeader
                field="clickTime"
                :current-sort="currentSort"
                @sort="handleSort"
              >
                点击时间
              </SortableHeader>
            </TableHead>
            <TableHead>点击类型</TableHead>
            <TableHead>用户代理</TableHead>
            <TableHead>操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody v-if="sortedClicks.length > 0">
          <TableRow
            v-for="click in sortedClicks"
            :key="click.id"
            class="table-row"
          >
            <TableCell>
              <div class="click-position">
                <span class="position-badge">
                  第 {{ click.clickPosition }} 位
                </span>
              </div>
            </TableCell>
            <TableCell>
              <div class="document-info">
                <div class="document-title">
                  <a
                    :href="click.documentUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="document-link"
                  >
                    {{ click.documentTitle }}
                  </a>
                </div>
                <div class="document-url">{{ click.documentUrl }}</div>
                <div class="document-id">ID: {{ click.documentId }}</div>
              </div>
            </TableCell>
            <TableCell>
              <div class="click-time">
                <time :datetime="click.clickTime">
                  {{ formatDateTime(click.clickTime) }}
                </time>
                <div class="relative-time">
                  {{ formatRelativeTime(click.clickTime) }}
                </div>
              </div>
            </TableCell>
            <TableCell>
              <ClickTypeBadge :type="click.clickType" />
            </TableCell>
            <TableCell>
              <div class="user-agent" :title="click.userAgent">
                {{ getUserAgentInfo(click.userAgent) }}
              </div>
            </TableCell>
            <TableCell>
              <div class="action-buttons">
                <button
                  @click="openUrl(click.documentUrl)"
                  class="open-link-btn"
                  title="打开链接"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                </button>
              </div>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>

      <!-- 空状态 -->
      <div v-if="clicks.length === 0" class="empty-state">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122" />
        </svg>
        <h3 class="empty-title">暂无点击记录</h3>
        <p class="empty-description">该搜索日志暂无相关的点击行为数据</p>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="clicks.length > 0" class="table-footer">
      <div class="stats-info">
        <div class="stat-item">
          <span class="stat-label">最高点击位置:</span>
          <span class="stat-value">第 {{ getHighestPosition() }} 位</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">平均点击位置:</span>
          <span class="stat-value">第 {{ getAveragePosition() }} 位</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">点击时间跨度:</span>
          <span class="stat-value">{{ getTimeSpan() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { formatDateTime, formatRelativeTime } from '@/utils/date'
import type { ClickLog } from '@/types/searchLog'

// UI 组件导入
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table'
import SortableHeader from '@/components/ui/SortableHeader.vue'

// 点击类型徽章组件
const ClickTypeBadge = ({ type }: { type: string }) => {
  const typeConfig: Record<string, { text: string; class: string }> = {
    'LEFT_CLICK': { text: '左键点击', class: 'bg-blue-100 text-blue-800' },
    'RIGHT_CLICK': { text: '右键点击', class: 'bg-purple-100 text-purple-800' },
    'MIDDLE_CLICK': { text: '中键点击', class: 'bg-yellow-100 text-yellow-800' },
    'TOUCH': { text: '触摸', class: 'bg-green-100 text-green-800' }
  }

  const config = typeConfig[type] || { text: type, class: 'bg-gray-100 text-gray-800' }

  return `<span class="click-type-badge ${config.class}">${config.text}</span>`
}

interface Props {
  clicks: ClickLog[]
}

const props = defineProps<Props>()

/**
 * 当前排序状态
 */
const currentSort = ref({
  field: 'clickTime',
  direction: 'desc' as 'asc' | 'desc'
})

/**
 * 排序后的点击记录
 */
const sortedClicks = computed(() => {
  if (!props.clicks.length) return []

  const sorted = [...props.clicks].sort((a, b) => {
    const { field, direction } = currentSort.value
    let aValue: any = a[field as keyof ClickLog]
    let bValue: any = b[field as keyof ClickLog]

    // 处理不同类型的排序
    if (field === 'clickTime') {
      aValue = new Date(aValue).getTime()
      bValue = new Date(bValue).getTime()
    } else if (field === 'clickPosition') {
      aValue = Number(aValue)
      bValue = Number(bValue)
    }

    if (direction === 'asc') {
      return aValue > bValue ? 1 : -1
    } else {
      return aValue < bValue ? 1 : -1
    }
  })

  return sorted
})

/**
 * 处理排序
 */
const handleSort = (field: string, direction: 'asc' | 'desc') => {
  currentSort.value = { field, direction }
}

/**
 * 解析用户代理信息
 */
const getUserAgentInfo = (userAgent: string) => {
  if (!userAgent) return '未知'

  // 简单的用户代理解析
  if (userAgent.includes('Chrome')) return 'Chrome'
  if (userAgent.includes('Firefox')) return 'Firefox'
  if (userAgent.includes('Safari')) return 'Safari'
  if (userAgent.includes('Edge')) return 'Edge'
  if (userAgent.includes('Mobile')) return 'Mobile'

  return userAgent.length > 50 ? userAgent.substring(0, 50) + '...' : userAgent
}

/**
 * 打开链接
 */
const openUrl = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

/**
 * 获取最高点击位置
 */
const getHighestPosition = () => {
  if (!props.clicks.length) return 0
  return Math.min(...props.clicks.map(click => click.clickPosition))
}

/**
 * 获取平均点击位置
 */
const getAveragePosition = () => {
  if (!props.clicks.length) return 0
  const total = props.clicks.reduce((sum, click) => sum + click.clickPosition, 0)
  return Math.round(total / props.clicks.length)
}

/**
 * 获取点击时间跨度
 */
const getTimeSpan = () => {
  if (props.clicks.length < 2) return '无'

  const times = props.clicks.map(click => new Date(click.clickTime).getTime())
  const minTime = Math.min(...times)
  const maxTime = Math.max(...times)
  const diffMinutes = Math.round((maxTime - minTime) / (1000 * 60))

  if (diffMinutes < 1) return '不到1分钟'
  if (diffMinutes < 60) return `${diffMinutes}分钟`

  const hours = Math.floor(diffMinutes / 60)
  const minutes = diffMinutes % 60
  return `${hours}小时${minutes}分钟`
}
</script>

<style scoped>
.click-log-table {
  @apply bg-white rounded-lg shadow overflow-hidden;
}

.table-header {
  @apply flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50;
}

.table-title {
  @apply text-lg font-medium text-gray-900;
}

.click-summary {
  @apply text-sm text-gray-600;
}

.table-container {
  @apply overflow-x-auto max-h-[400px] overflow-y-auto;
}

.table-row {
  @apply hover:bg-green-50 transition-colors duration-200;
}

.click-position {
  @apply flex items-center;
}

.position-badge {
  @apply inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800;
}

.document-info {
  @apply space-y-1 max-w-xs;
}

.document-title {
  @apply font-medium;
}

.document-link {
  @apply text-green-600 hover:text-green-700 hover:underline transition-colors duration-200;
}

.document-url {
  @apply text-xs text-gray-500 truncate font-mono;
}

.document-id {
  @apply text-xs text-gray-400 font-mono;
}

.click-time {
  @apply space-y-1;
}

.relative-time {
  @apply text-xs text-gray-500;
}

.user-agent {
  @apply text-sm text-gray-600 max-w-[120px] truncate;
}

.action-buttons {
  @apply flex items-center space-x-1;
}

.open-link-btn {
  @apply
    p-1
    text-gray-500
    hover:text-green-600
    hover:bg-green-50
    rounded
    transition-colors
    duration-200;
}

.empty-state {
  @apply flex flex-col items-center justify-center py-12 text-gray-500;
}

.empty-icon {
  @apply w-12 h-12 mb-4;
}

.empty-title {
  @apply text-lg font-medium text-gray-900 mb-2;
}

.empty-description {
  @apply text-gray-500;
}

.table-footer {
  @apply p-4 border-t border-gray-200 bg-gray-50;
}

.stats-info {
  @apply flex items-center space-x-6;
}

.stat-item {
  @apply flex items-center space-x-1;
}

.stat-label {
  @apply text-sm text-gray-600;
}

.stat-value {
  @apply text-sm font-medium text-gray-900;
}

/* 点击类型徽章样式 */
:deep(.click-type-badge) {
  @apply inline-flex items-center px-2 py-1 rounded-full text-xs font-medium;
}
</style>