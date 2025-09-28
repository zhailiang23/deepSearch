<template>
  <div class="table-pagination bg-white border-t border-gray-200 p-4">
    <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
      <!-- 左侧信息 -->
      <div class="flex items-center gap-4 text-sm text-gray-600">
        <!-- 每页数量选择 -->
        <div v-if="showSizeChanger" class="flex items-center gap-2">
          <span class="whitespace-nowrap">每页显示</span>
          <select
            :value="pageSize"
            @change="handlePageSizeChange"
            :disabled="loading"
            class="pagination-select"
          >
            <option
              v-for="size in pageSizeOptions"
              :key="size"
              :value="size"
            >
              {{ size }} 条
            </option>
          </select>
        </div>

        <!-- 数据统计 -->
        <div v-if="showTotal" class="text-gray-600 whitespace-nowrap">
          <template v-if="total > 0">
            共 {{ total }} 条，第 {{ startRecord }}-{{ endRecord }} 条
          </template>
          <template v-else>
            暂无数据
          </template>
        </div>
      </div>

      <!-- 右侧分页控制 -->
      <div class="flex items-center gap-2" v-if="totalPages > 1">
        <!-- 第一页 -->
        <button
          :disabled="currentPage <= 1 || loading"
          @click="goToPage(1)"
          class="pagination-btn"
          title="第一页"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
        </button>

        <!-- 上一页 -->
        <button
          :disabled="currentPage <= 1 || loading"
          @click="goToPage(currentPage - 1)"
          class="pagination-btn"
          title="上一页"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          <span class="hidden sm:inline ml-1">上一页</span>
        </button>

        <!-- 页码按钮 -->
        <div class="flex items-center gap-1">
          <template v-for="page in visiblePages" :key="page">
            <button
              v-if="typeof page === 'number'"
              :class="[
                'pagination-btn',
                page === currentPage ? 'pagination-btn-active' : 'pagination-btn-normal'
              ]"
              :disabled="loading"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
            <span
              v-else
              class="px-2 py-1 text-gray-400 text-sm"
            >
              ...
            </span>
          </template>
        </div>

        <!-- 下一页 -->
        <button
          :disabled="currentPage >= totalPages || loading"
          @click="goToPage(currentPage + 1)"
          class="pagination-btn"
          title="下一页"
        >
          <span class="hidden sm:inline mr-1">下一页</span>
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <!-- 最后一页 -->
        <button
          :disabled="currentPage >= totalPages || loading"
          @click="goToPage(totalPages)"
          class="pagination-btn"
          title="最后一页"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
          </svg>
        </button>

        <!-- 页码跳转（桌面端） -->
        <div v-if="showQuickJumper" class="hidden md:flex items-center gap-2 ml-4 border-l border-gray-200 pl-4">
          <span class="text-sm text-gray-600 whitespace-nowrap">跳至</span>
          <input
            v-model.number="jumpPage"
            type="number"
            :min="1"
            :max="totalPages"
            :disabled="loading"
            class="pagination-input"
            @keyup.enter="handleJumpPage"
          >
          <span class="text-sm text-gray-600">页</span>
          <button
            :disabled="loading || !jumpPage || jumpPage < 1 || jumpPage > totalPages"
            @click="handleJumpPage"
            class="pagination-btn pagination-btn-normal"
          >
            跳转
          </button>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center gap-2 text-green-600 text-sm">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
    </div>

    <!-- 移动端页码跳转 -->
    <div v-if="showQuickJumper && totalPages > 1" class="md:hidden mt-4 pt-4 border-t border-gray-200">
      <div class="flex items-center justify-center gap-2">
        <span class="text-sm text-gray-600">跳至</span>
        <input
          v-model.number="jumpPage"
          type="number"
          :min="1"
          :max="totalPages"
          :disabled="loading"
          class="pagination-input w-16"
          @keyup.enter="handleJumpPage"
        >
        <span class="text-sm text-gray-600">页 (共{{ totalPages }}页)</span>
        <button
          :disabled="loading || !jumpPage || jumpPage < 1 || jumpPage > totalPages"
          @click="handleJumpPage"
          class="pagination-btn pagination-btn-normal"
        >
          跳转
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { PaginationConfig } from '@/types/statistics'

interface Props {
  pagination: PaginationConfig
  loading?: boolean
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  showTotal?: boolean
  pageSizeOptions?: number[]
  maxVisiblePages?: number
}

interface Emits {
  (e: 'page-change', page: number, pageSize: number): void
  (e: 'size-change', pageSize: number): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: true,
  pageSizeOptions: () => [20, 50, 100, 200],
  maxVisiblePages: 7
})

const emit = defineEmits<Emits>()

// 跳转页码输入
const jumpPage = ref<number>()

// 计算属性
const currentPage = computed(() => props.pagination.page + 1) // 转换为1-based
const pageSize = computed(() => props.pagination.pageSize)
const total = computed(() => props.pagination.total)
const totalPages = computed(() => props.pagination.totalPages)

const startRecord = computed(() => {
  return props.pagination.total > 0 ? props.pagination.page * props.pagination.pageSize + 1 : 0
})

const endRecord = computed(() => {
  const end = (props.pagination.page + 1) * props.pagination.pageSize
  return Math.min(end, props.pagination.total)
})

// 可见页码计算
const visiblePages = computed(() => {
  const pages: (number | string)[] = []
  const { maxVisiblePages } = props
  const current = currentPage.value
  const totalPagesValue = totalPages.value

  if (totalPagesValue <= maxVisiblePages) {
    // 总页数小于等于最大显示页数，显示所有页码
    for (let i = 1; i <= totalPagesValue; i++) {
      pages.push(i)
    }
  } else {
    // 总页数大于最大显示页数，需要智能显示
    const half = Math.floor(maxVisiblePages / 2)

    if (current <= half + 1) {
      // 当前页在前半部分
      for (let i = 1; i <= maxVisiblePages - 2; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(totalPagesValue)
    } else if (current >= totalPagesValue - half) {
      // 当前页在后半部分
      pages.push(1)
      pages.push('...')
      for (let i = totalPagesValue - maxVisiblePages + 3; i <= totalPagesValue; i++) {
        pages.push(i)
      }
    } else {
      // 当前页在中间
      pages.push(1)
      pages.push('...')
      for (let i = current - half + 1; i <= current + half - 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(totalPagesValue)
    }
  }

  return pages
})

/**
 * 跳转到指定页码
 */
const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) {
    return
  }

  // 转换为0-based页码
  emit('page-change', page - 1, pageSize.value)
}

/**
 * 处理每页数量变更
 */
const handlePageSizeChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  const newSize = parseInt(target.value)

  if (newSize === pageSize.value) return

  emit('size-change', newSize)
}

/**
 * 处理页码跳转
 */
const handleJumpPage = () => {
  if (jumpPage.value && jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    goToPage(jumpPage.value)
    jumpPage.value = undefined
  }
}

// 监听当前页变化，重置跳转输入
watch(() => currentPage.value, () => {
  jumpPage.value = undefined
})
</script>

<style scoped>
/* 分页控件基础样式 */
.table-pagination {
  @apply bg-green-50 border-green-200;
}

/* 选择框样式 */
.pagination-select {
  @apply
    border
    border-gray-300
    rounded-md
    px-2
    py-1
    text-sm
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:border-green-500
    disabled:opacity-50
    disabled:cursor-not-allowed
    transition-colors
    duration-200;
}

.pagination-select:hover:not(:disabled) {
  @apply border-green-400;
}

/* 按钮基础样式 */
.pagination-btn {
  @apply
    inline-flex
    items-center
    justify-center
    min-w-8
    h-8
    px-2
    text-sm
    font-medium
    border
    rounded-md
    transition-all
    duration-200
    disabled:opacity-50
    disabled:cursor-not-allowed;
}

/* 普通分页按钮 */
.pagination-btn-normal {
  @apply
    text-gray-600
    bg-white
    border-gray-300
    hover:bg-green-50
    hover:border-green-400
    hover:text-green-600;
}

/* 当前页按钮 */
.pagination-btn-active {
  @apply
    text-white
    bg-green-600
    border-green-600
    hover:bg-green-700
    hover:border-green-700;
}

/* 输入框样式 */
.pagination-input {
  @apply
    w-16
    px-2
    py-1
    border
    border-gray-300
    rounded-md
    text-sm
    text-center
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:border-green-500
    disabled:opacity-50
    disabled:cursor-not-allowed
    disabled:bg-gray-50;
}

/* 加载动画 */
.loading-spinner {
  @apply
    w-4
    h-4
    border-2
    border-green-500
    border-t-transparent
    rounded-full
    animate-spin;
}

/* 移除数字输入框的箭头 */
input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .table-pagination {
    @apply p-3;
  }

  .pagination-btn {
    @apply min-w-7 h-7;
  }

  .pagination-input {
    @apply w-14;
  }
}

/* 聚焦效果 */
.pagination-input:focus {
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

.pagination-select:focus {
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

/* 省略号样式 */
.table-pagination span {
  @apply
    inline-flex
    items-center
    justify-center
    min-w-8
    h-8;
}
</style>