<template>
  <TableRow
    class="ranking-table-row hover:bg-green-50 transition-colors duration-200 cursor-pointer"
    @click="handleRowClick"
  >
    <!-- 排名列 -->
    <TableCell class="text-center font-semibold">
      <div class="rank-badge" :class="getRankBadgeClass(item.rank)">
        {{ item.rank }}
      </div>
    </TableCell>

    <!-- 关键词列 -->
    <TableCell class="font-medium">
      <div class="keyword-cell">
        <span class="keyword-text" :title="item.keyword">
          {{ item.keyword }}
        </span>
        <!-- 移动端显示简化信息 -->
        <div v-if="isMobile" class="mobile-info">
          <span class="count-mobile">{{ formatCount(item.count) }}</span>
          <TrendIcon :trend="item.trend" class="trend-mobile" />
        </div>
      </div>
    </TableCell>

    <!-- 搜索次数列 -->
    <TableCell class="text-right font-medium">
      <div class="count-cell">
        <span class="count-number">{{ formatCount(item.count) }}</span>
      </div>
    </TableCell>

    <!-- 占比列（桌面端显示） -->
    <TableCell v-if="!isMobile" class="text-right">
      <div class="percentage-cell">
        <span v-if="item.percentage !== undefined" class="percentage-text">
          {{ formatPercentage(item.percentage) }}
        </span>
        <span v-else class="text-gray-400">-</span>
      </div>
    </TableCell>

    <!-- 趋势列 -->
    <TableCell class="text-center">
      <div class="trend-cell">
        <TrendIcon :trend="item.trend" />
      </div>
    </TableCell>

    <!-- 操作列 -->
    <TableCell class="text-center">
      <div class="action-cell">
        <button
          @click.stop="handleViewDetail"
          class="action-btn detail-btn"
          :title="'查看 ' + item.keyword + ' 详情'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
        </button>
      </div>
    </TableCell>
  </TableRow>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TableRow, TableCell } from '@/components/ui/table'
import TrendIcon from './TrendIcon.vue'
import type { HotWordItem } from '@/types/statistics'

interface Props {
  item: HotWordItem
  isMobile?: boolean
}

interface Emits {
  (e: 'row-click', item: HotWordItem): void
  (e: 'view-detail', item: HotWordItem): void
}

const props = withDefaults(defineProps<Props>(), {
  isMobile: false
})

const emit = defineEmits<Emits>()

/**
 * 格式化数字显示
 */
const formatCount = (count: number | undefined): string => {
  if (!count) return '0'
  if (count >= 1000000) {
    return (count / 1000000).toFixed(1) + 'M'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'K'
  }
  return count.toString()
}

/**
 * 格式化百分比显示
 */
const formatPercentage = (percentage: number | undefined): string => {
  if (percentage === undefined) return '0%'
  return percentage.toFixed(2) + '%'
}

/**
 * 获取排名徽章样式
 */
const getRankBadgeClass = (rank: number | undefined): string => {
  if (!rank) return 'rank-normal'
  if (rank <= 3) {
    return 'rank-top'
  } else if (rank <= 10) {
    return 'rank-high'
  } else {
    return 'rank-normal'
  }
}

/**
 * 处理行点击事件
 */
const handleRowClick = () => {
  emit('row-click', props.item)
}

/**
 * 处理查看详情按钮点击
 */
const handleViewDetail = () => {
  emit('view-detail', props.item)
}
</script>

<style scoped>
/* 表格行样式 */
.ranking-table-row {
  @apply border-b border-gray-100;
}

/* 排名徽章样式 */
.rank-badge {
  @apply
    inline-flex
    items-center
    justify-center
    w-8
    h-8
    rounded-full
    text-sm
    font-bold;
}

.rank-top {
  @apply bg-gradient-to-r from-yellow-400 to-yellow-500 text-white shadow-md;
}

.rank-high {
  @apply bg-gradient-to-r from-green-400 to-green-500 text-white;
}

.rank-normal {
  @apply bg-gray-100 text-gray-600;
}

/* 关键词单元格样式 */
.keyword-cell {
  @apply space-y-1;
}

.keyword-text {
  @apply
    text-gray-900
    font-medium
    truncate
    max-w-[150px]
    block;
}

.mobile-info {
  @apply flex items-center justify-between text-sm;
}

.count-mobile {
  @apply text-green-600 font-semibold;
}

.trend-mobile {
  @apply w-4 h-4;
}

/* 搜索次数单元格样式 */
.count-cell {
  @apply text-right;
}

.count-number {
  @apply
    text-gray-900
    font-semibold
    px-2
    py-1
    bg-green-50
    rounded
    text-sm;
}

/* 百分比单元格样式 */
.percentage-cell {
  @apply text-right;
}

.percentage-text {
  @apply
    text-gray-600
    font-medium
    text-sm;
}

/* 趋势单元格样式 */
.trend-cell {
  @apply flex items-center justify-center;
}

/* 操作单元格样式 */
.action-cell {
  @apply flex items-center justify-center;
}

.action-btn {
  @apply
    p-2
    rounded
    transition-all
    duration-200
    hover:scale-110;
}

.detail-btn {
  @apply
    text-gray-500
    hover:text-green-600
    hover:bg-green-50;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .keyword-text {
    @apply max-w-[100px];
  }

  .count-number {
    @apply px-1 text-xs;
  }
}
</style>