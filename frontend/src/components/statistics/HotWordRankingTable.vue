<template>
  <div class="hot-word-ranking-table">
    <!-- 表格头部信息栏 -->
    <div class="table-header">
      <div class="table-info">
        <h3 class="table-title">热词排行榜</h3>
        <span v-if="!loading && data.length > 0" class="table-summary">
          共 {{ pagination.total }} 个热词
        </span>
      </div>
      <div class="table-actions">
        <button
          @click="$emit('refresh')"
          :disabled="loading"
          class="action-btn refresh-btn"
          title="刷新数据"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
        </button>
        <button
          @click="$emit('export')"
          :disabled="loading || data.length === 0"
          class="action-btn export-btn"
          title="导出数据"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 数据表格容器 -->
    <div class="table-container">
      <Table class="ranking-table">
        <!-- 表格头部 -->
        <RankingTableHeader
          :current-sort="currentSort"
          :is-mobile="isMobile"
          @sort="handleSort"
        />

        <!-- 表格主体 -->
        <TableBody v-if="!loading">
          <RankingTableRow
            v-for="item in data"
            :key="`${item.keyword}-${item.rank}`"
            :item="item"
            :is-mobile="isMobile"
            @row-click="handleRowClick"
            @view-detail="handleViewDetail"
          />
        </TableBody>
      </Table>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span class="loading-text">正在加载热词数据...</span>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && data.length === 0" class="empty-state">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
        </svg>
        <h3 class="empty-title">暂无热词数据</h3>
        <p class="empty-description">没有找到符合条件的热词，请尝试调整筛选条件</p>
        <button
          @click="$emit('refresh')"
          class="empty-action-btn"
        >
          重新加载
        </button>
      </div>

      <!-- 错误状态 -->
      <div v-if="error" class="error-state">
        <svg class="error-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
        <h3 class="error-title">加载失败</h3>
        <p class="error-description">{{ error }}</p>
        <button
          @click="$emit('retry')"
          class="error-action-btn"
        >
          重试
        </button>
      </div>
    </div>

    <!-- 分页控件 -->
    <TablePagination
      v-if="!loading && data.length > 0"
      :pagination="pagination"
      :loading="loading"
      @page-change="handlePageChange"
      @size-change="handlePageSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { Table, TableBody } from '@/components/ui/table'
import RankingTableHeader from './table/RankingTableHeader.vue'
import RankingTableRow from './table/RankingTableRow.vue'
import TablePagination from './table/TablePagination.vue'
import type { HotWordItem, SortConfig, PaginationConfig } from '@/types/statistics'

interface Props {
  data: HotWordItem[]
  loading: boolean
  error?: string | null
  pagination: PaginationConfig
  currentSort: SortConfig
}

interface Emits {
  (e: 'sort-change', field: string, order: 'asc' | 'desc'): void
  (e: 'page-change', page: number, pageSize: number): void
  (e: 'page-size-change', pageSize: number): void
  (e: 'row-click', item: HotWordItem): void
  (e: 'view-detail', item: HotWordItem): void
  (e: 'refresh'): void
  (e: 'export'): void
  (e: 'retry'): void
}

const props = withDefaults(defineProps<Props>(), {
  error: null
})

const emit = defineEmits<Emits>()

// 响应式断点检测
const windowWidth = ref(window.innerWidth)

const isMobile = computed(() => windowWidth.value < 768)

/**
 * 处理排序变化
 */
const handleSort = (field: string, direction: 'asc' | 'desc') => {
  emit('sort-change', field, direction)
}

/**
 * 处理分页变化
 */
const handlePageChange = (page: number, pageSize: number) => {
  emit('page-change', page, pageSize)
}

/**
 * 处理页面大小变化
 */
const handlePageSizeChange = (pageSize: number) => {
  emit('page-size-change', pageSize)
}

/**
 * 处理行点击事件
 */
const handleRowClick = (item: HotWordItem) => {
  emit('row-click', item)
}

/**
 * 处理查看详情事件
 */
const handleViewDetail = (item: HotWordItem) => {
  emit('view-detail', item)
}

/**
 * 窗口大小变化监听器
 */
const handleResize = () => {
  windowWidth.value = window.innerWidth
}

// 生命周期
onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 主容器样式 */
.hot-word-ranking-table {
  @apply bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden;
}

/* 表格头部信息栏 */
.table-header {
  @apply
    flex
    items-center
    justify-between
    p-4
    bg-green-50
    border-b
    border-green-200;
}

.table-info {
  @apply flex items-center gap-3;
}

.table-title {
  @apply text-lg font-semibold text-gray-900;
}

.table-summary {
  @apply text-sm text-gray-600;
}

.table-actions {
  @apply flex items-center gap-2;
}

/* 操作按钮样式 */
.action-btn {
  @apply
    inline-flex
    items-center
    justify-center
    w-9
    h-9
    rounded-md
    transition-all
    duration-200
    disabled:opacity-50
    disabled:cursor-not-allowed;
}

.refresh-btn {
  @apply
    text-gray-500
    hover:text-green-600
    hover:bg-green-100
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-offset-2;
}

.export-btn {
  @apply
    text-gray-500
    hover:text-blue-600
    hover:bg-blue-50
    focus:outline-none
    focus:ring-2
    focus:ring-blue-500
    focus:ring-offset-2;
}

/* 表格容器 */
.table-container {
  @apply relative;
}

.ranking-table {
  @apply w-full;
}

/* 加载状态样式 */
.loading-state {
  @apply
    flex
    flex-col
    items-center
    justify-center
    py-16
    space-y-4;
}

.loading-spinner {
  @apply
    w-8
    h-8
    border-3
    border-green-500
    border-t-transparent
    rounded-full
    animate-spin;
}

.loading-text {
  @apply text-gray-600 font-medium;
}

/* 空状态样式 */
.empty-state {
  @apply
    flex
    flex-col
    items-center
    justify-center
    py-16
    space-y-4
    text-gray-500;
}

.empty-icon {
  @apply w-16 h-16 text-gray-300;
}

.empty-title {
  @apply text-xl font-semibold text-gray-700;
}

.empty-description {
  @apply text-gray-500 text-center max-w-md;
}

.empty-action-btn {
  @apply
    px-4
    py-2
    text-sm
    font-medium
    text-green-600
    bg-green-50
    border
    border-green-200
    rounded-md
    hover:bg-green-100
    hover:border-green-300
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-offset-2;
}

/* 错误状态样式 */
.error-state {
  @apply
    flex
    flex-col
    items-center
    justify-center
    py-16
    space-y-4
    text-red-500;
}

.error-icon {
  @apply w-16 h-16 text-red-300;
}

.error-title {
  @apply text-xl font-semibold text-red-700;
}

.error-description {
  @apply text-red-500 text-center max-w-md;
}

.error-action-btn {
  @apply
    px-4
    py-2
    text-sm
    font-medium
    text-red-600
    bg-red-50
    border
    border-red-200
    rounded-md
    hover:bg-red-100
    hover:border-red-300
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-red-500
    focus:ring-offset-2;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .table-header {
    @apply flex-col items-start gap-3 p-3;
  }

  .table-info {
    @apply flex-col items-start gap-1;
  }

  .table-actions {
    @apply self-end;
  }

  .table-title {
    @apply text-base;
  }

  .table-summary {
    @apply text-xs;
  }

  .action-btn {
    @apply w-8 h-8;
  }

  .loading-state,
  .empty-state,
  .error-state {
    @apply py-12;
  }

  .empty-icon,
  .error-icon {
    @apply w-12 h-12;
  }

  .empty-title,
  .error-title {
    @apply text-lg;
  }
}

/* 表格滚动优化 */
.table-container {
  @apply overflow-x-auto;
}

/* 确保表格在小屏幕上可以水平滚动 */
@media (max-width: 640px) {
  .ranking-table {
    @apply min-w-[600px];
  }
}

/* 动画优化 */
.loading-spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 专注状态优化 */
.action-btn:focus {
  @apply ring-offset-green-50;
}

/* 禁用状态优化 */
.action-btn:disabled {
  @apply transform-none;
}
</style>