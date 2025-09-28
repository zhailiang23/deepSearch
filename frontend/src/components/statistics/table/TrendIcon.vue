<template>
  <span v-if="trend" :class="iconClass" :title="trendTitle">
    <!-- 上升趋势图标 -->
    <svg v-if="trend === 'up'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
    </svg>

    <!-- 下降趋势图标 -->
    <svg v-else-if="trend === 'down'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6" />
    </svg>

    <!-- 稳定趋势图标 -->
    <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
    </svg>
  </span>

  <!-- 无趋势数据时显示占位符 -->
  <span v-else class="w-4 h-4 flex items-center justify-center text-gray-300">
    <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="2" />
    </svg>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  trend?: 'up' | 'down' | 'stable'
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md'
})

/**
 * 图标样式类
 */
const iconClass = computed(() => {
  const baseClass = 'inline-flex items-center justify-center transition-colors duration-200'

  // 尺寸样式
  const sizeClass = {
    sm: 'w-3 h-3',
    md: 'w-4 h-4',
    lg: 'w-5 h-5'
  }[props.size]

  // 趋势颜色样式
  const colorClass = {
    up: 'text-green-500',
    down: 'text-red-500',
    stable: 'text-gray-400'
  }[props.trend || 'stable']

  return `${baseClass} ${sizeClass} ${colorClass}`
})

/**
 * 趋势提示文本
 */
const trendTitle = computed(() => {
  const trendText = {
    up: '上升趋势',
    down: '下降趋势',
    stable: '稳定趋势'
  }[props.trend || 'stable']

  return trendText
})
</script>

<style scoped>
/* 趋势图标动画效果 */
.trend-icon {
  @apply transition-all duration-200;
}

.trend-icon:hover {
  @apply scale-110;
}

/* 上升趋势特殊效果 */
.text-green-500:hover {
  @apply text-green-600;
}

/* 下降趋势特殊效果 */
.text-red-500:hover {
  @apply text-red-600;
}

/* 稳定趋势特殊效果 */
.text-gray-400:hover {
  @apply text-gray-500;
}
</style>