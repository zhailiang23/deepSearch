<template>
  <div class="search-log-table">
    <!-- 表格头部信息 -->
    <div class="table-header">
      <div class="table-info">
        <span class="text-sm text-gray-600">
          共 {{ pagination.totalElements }} 条记录
        </span>
      </div>
      <div class="table-actions">
        <button
          @click="$emit('refresh')"
          class="refresh-btn"
          :disabled="loading"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <SortableHeader
                field="id"
                :current-sort="props.currentSort"
                @sort="handleSort"
              >
                ID
              </SortableHeader>
            </TableHead>
            <TableHead>搜索空间</TableHead>
            <TableHead>查询关键字</TableHead>
            <TableHead>
              <SortableHeader
                field="totalResults"
                :current-sort="props.currentSort"
                @sort="handleSort"
              >
                结果数量
              </SortableHeader>
            </TableHead>
            <TableHead>
              <SortableHeader
                field="totalTimeMs"
                :current-sort="props.currentSort"
                @sort="handleSort"
              >
                响应时间
              </SortableHeader>
            </TableHead>
            <TableHead>
              <SortableHeader
                field="status"
                :current-sort="props.currentSort"
                @sort="handleSort"
              >
                状态
              </SortableHeader>
            </TableHead>
            <TableHead>点击情况</TableHead>
            <TableHead>
              <SortableHeader
                field="createdAt"
                :current-sort="props.currentSort"
                @sort="handleSort"
              >
                创建时间
              </SortableHeader>
            </TableHead>
            <TableHead>操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody v-if="!loading">
          <TableRow
            v-for="log in data"
            :key="log.id"
            class="table-row"
            @click="$emit('view-detail', log.id)"
          >
            <TableCell class="font-mono text-sm">
              {{ log.id }}
            </TableCell>
            <TableCell>
              <div class="search-space-info">
                <div class="space-name">{{ log.searchSpaceName || '未命名空间' }}</div>
              </div>
            </TableCell>
            <TableCell>
              <div class="search-query">{{ log.searchQuery }}</div>
            </TableCell>
            <TableCell>
              <span
                class="result-count"
                :class="{
                  'text-green-600': log.totalResults > 0,
                  'text-yellow-600': log.totalResults === 0
                }"
              >
                {{ log.totalResults }}
              </span>
            </TableCell>
            <TableCell>
              <span
                class="response-time font-mono"
                :class="getResponseTimeClass(log.totalTimeMs)"
              >
                {{ log.totalTimeMs }}ms
              </span>
            </TableCell>
            <TableCell>
              <span
                class="status-badge"
                :class="{
                  'bg-green-100 text-green-800': log.status === 'SUCCESS',
                  'bg-red-100 text-red-800': log.status === 'ERROR'
                }"
              >
                {{ log.status === 'SUCCESS' ? '成功' : '失败' }}
              </span>
            </TableCell>
            <TableCell>
              <div class="click-info">
                <span class="click-count font-medium">{{ log.clickLogs?.length || 0 }}</span>
                <button
                  v-if="(log.clickLogs?.length || 0) > 0"
                  @click.stop="$emit('view-clicks', log.id)"
                  class="view-clicks-btn"
                >
                  查看
                </button>
              </div>
            </TableCell>
            <TableCell>
              <time
                :datetime="log.createdAt"
                class="text-sm text-gray-600"
              >
                {{ formatDateTime(log.createdAt) }}
              </time>
            </TableCell>
            <TableCell>
              <div class="action-buttons">
                <button
                  @click.stop="$emit('view-detail', log.id)"
                  class="detail-btn"
                >
                  详情
                </button>
              </div>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && data.length === 0" class="empty-state">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 class="empty-title">暂无数据</h3>
        <p class="empty-description">没有找到符合条件的搜索日志</p>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="!loading && data.length > 0" class="table-footer">
      <div class="pagination-info">
        <span class="text-sm text-gray-700">
          显示第 {{ pagination.number * pagination.size + 1 }} -
          {{ Math.min((pagination.number + 1) * pagination.size, pagination.totalElements) }} 条，
          共 {{ pagination.totalElements }} 条
        </span>
      </div>
      <div class="pagination-controls">
        <button
          @click="handlePageChange(pagination.number - 1, pagination.size)"
          :disabled="pagination.first"
          class="pagination-btn"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ pagination.number + 1 }} / {{ pagination.totalPages }} 页
        </span>
        <button
          @click="handlePageChange(pagination.number + 1, pagination.size)"
          :disabled="pagination.last"
          class="pagination-btn"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatDateTime } from '@/utils/date'
import type { SearchLog, PaginatedResponse } from '@/types/searchLog'

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

// 状态徽章组件
const StatusBadge = ({ status }: { status: 'SUCCESS' | 'ERROR' }) => {
  const statusConfig = {
    SUCCESS: { text: '成功', class: 'bg-green-100 text-green-800' },
    ERROR: { text: '失败', class: 'bg-red-100 text-red-800' }
  }

  const config = statusConfig[status] || statusConfig.ERROR

  return `<span class="status-badge ${config.class}">${config.text}</span>`
}

interface Props {
  data: SearchLog[]
  loading: boolean
  pagination: PaginatedResponse<SearchLog>
  currentSort: {
    field: string
    direction: 'asc' | 'desc'
  }
}

interface Emits {
  (e: 'page-change', page: number, size: number): void
  (e: 'sort-change', field: string, direction: 'asc' | 'desc'): void
  (e: 'view-detail', id: number): void
  (e: 'view-clicks', id: number): void
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()


/**
 * 获取响应时间样式类
 */
const getResponseTimeClass = (responseTime: number) => {
  if (responseTime < 500) return 'text-green-600'
  if (responseTime < 1000) return 'text-yellow-600'
  return 'text-red-600'
}

/**
 * 处理排序
 */
const handleSort = (field: string, direction: 'asc' | 'desc') => {
  emit('sort-change', field, direction)
}

/**
 * 处理分页变化
 */
const handlePageChange = (page: number, size: number) => {
  emit('page-change', page, size)
}
</script>

<style scoped>
.search-log-table {
  @apply bg-white rounded-lg shadow overflow-hidden;
}

.table-header {
  @apply flex items-center justify-between px-4 py-2 border-b border-gray-200 bg-gray-50;
}

.table-info {
  @apply flex items-center;
}

.table-actions {
  @apply flex items-center space-x-2;
}

.refresh-btn {
  @apply
    p-2
    text-gray-500
    hover:text-green-600
    hover:bg-green-50
    rounded
    transition-colors
    duration-200
    disabled:opacity-50
    disabled:cursor-not-allowed;
}

.table-container {
  @apply overflow-x-auto max-h-[600px] overflow-y-auto;
}

.table-row {
  @apply hover:bg-green-50 cursor-pointer transition-colors duration-200;
}

.user-info {
  @apply space-y-1;
}

.user-id {
  @apply font-medium text-gray-900;
}

.user-ip {
  @apply text-xs text-gray-500 font-mono;
}

.search-space-info {
  @apply space-y-1;
}

.space-name {
  @apply font-medium text-gray-900 truncate max-w-[150px];
}

.search-query {
  @apply font-medium truncate max-w-[200px];
}

.result-count {
  @apply font-medium px-2 py-1 rounded text-sm;
}

.response-time {
  @apply font-medium text-sm;
}

.click-info {
  @apply flex items-center space-x-2;
}

.click-count {
  @apply text-gray-900;
}

.view-clicks-btn {
  @apply
    text-xs
    text-green-600
    hover:text-green-700
    hover:underline
    transition-colors
    duration-200;
}

.action-buttons {
  @apply flex items-center space-x-1;
}

.detail-btn {
  @apply
    px-3
    py-1
    text-xs
    font-medium
    text-green-600
    border
    border-green-600
    rounded
    hover:bg-green-50
    transition-colors
    duration-200;
}

.loading-state {
  @apply flex items-center justify-center py-12 space-x-3;
}

.loading-spinner {
  @apply w-6 h-6 border-2 border-green-500 border-t-transparent rounded-full animate-spin;
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
  @apply flex items-center justify-between p-4 border-t border-gray-200 bg-gray-50;
}

.pagination-info {
  @apply flex items-center;
}

.pagination-controls {
  @apply flex items-center space-x-4;
}

.pagination-btn {
  @apply
    px-3
    py-1
    text-sm
    font-medium
    text-gray-700
    bg-white
    border
    border-gray-300
    rounded
    hover:bg-gray-50
    disabled:opacity-50
    disabled:cursor-not-allowed
    transition-colors
    duration-200;
}

.page-info {
  @apply text-sm text-gray-700 font-medium;
}

/* 状态徽章样式 */
:deep(.status-badge) {
  @apply inline-flex items-center px-2 py-1 rounded-full text-xs font-medium;
}
</style>