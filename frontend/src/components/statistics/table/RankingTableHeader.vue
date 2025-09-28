<template>
  <TableHeader>
    <TableRow class="bg-green-50 hover:bg-green-50">
      <!-- 排名列 -->
      <TableHead class="w-16 text-center">
        <SortableHeader
          field="rank"
          :current-sort="{ field: currentSort.field, direction: currentSort.order }"
          @sort="handleSort"
        >
          排名
        </SortableHeader>
      </TableHead>

      <!-- 关键词列 -->
      <TableHead class="min-w-[120px]">
        <SortableHeader
          field="keyword"
          :current-sort="{ field: currentSort.field, direction: currentSort.order }"
          @sort="handleSort"
        >
          关键词
        </SortableHeader>
      </TableHead>

      <!-- 搜索次数列 -->
      <TableHead class="w-24 text-right">
        <SortableHeader
          field="count"
          :current-sort="{ field: currentSort.field, direction: currentSort.order }"
          @sort="handleSort"
        >
          搜索次数
        </SortableHeader>
      </TableHead>

      <!-- 占比列（桌面端显示） -->
      <TableHead v-if="!isMobile" class="w-20 text-right">
        <span class="text-gray-700 font-semibold">
          占比
        </span>
      </TableHead>

      <!-- 趋势列 -->
      <TableHead class="w-20 text-center">
        <span class="text-gray-700 font-semibold">
          趋势
        </span>
      </TableHead>

      <!-- 操作列 -->
      <TableHead class="w-20 text-center">
        操作
      </TableHead>
    </TableRow>
  </TableHeader>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { TableHeader, TableHead, TableRow } from '@/components/ui/table'
import SortableHeader from '@/components/ui/SortableHeader.vue'
import type { SortConfig } from '@/types/statistics'

interface Props {
  currentSort: SortConfig
  isMobile?: boolean
}

interface Emits {
  (e: 'sort', field: string, direction: 'asc' | 'desc'): void
}

const props = withDefaults(defineProps<Props>(), {
  isMobile: false
})

const emit = defineEmits<Emits>()

/**
 * 处理排序事件
 */
const handleSort = (field: string, direction: 'asc' | 'desc') => {
  emit('sort', field, direction)
}
</script>

<style scoped>
/* 表格头部样式 */
:deep(.table-head) {
  @apply
    font-semibold
    text-gray-700
    bg-green-50
    border-b-2
    border-green-200
    px-4
    py-3;
}

/* 排序按钮样式 */
:deep(.sortable-header) {
  @apply
    flex
    items-center
    justify-between
    cursor-pointer
    hover:text-green-600
    transition-colors
    duration-200;
}

/* 排序图标样式 */
:deep(.sort-icon) {
  @apply
    w-4
    h-4
    ml-1
    text-gray-400
    hover:text-green-600
    transition-colors
    duration-200;
}

/* 活跃排序状态 */
:deep(.sort-active) {
  @apply text-green-600;
}

/* 响应式隐藏列 */
@media (max-width: 768px) {
  .hidden-mobile {
    @apply hidden;
  }
}

@media (max-width: 1024px) {
  .hidden-tablet {
    @apply hidden;
  }
}
</style>