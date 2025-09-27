<template>
  <div
    class="stat-card"
    :class="[
      colorClasses,
      { 'stat-card--loading': loading },
      { 'stat-card--clickable': clickable }
    ]"
    @click="handleClick"
  >
    <!-- 卡片头部 -->
    <div class="stat-card__header">
      <div class="stat-card__icon" v-if="iconName">
        <component :is="iconComponent" class="w-6 h-6" />
      </div>
      <div class="stat-card__title-container">
        <h3 class="stat-card__title">{{ title }}</h3>
        <p v-if="subtitle" class="stat-card__subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="stat-card__content">
      <div v-if="loading" class="stat-card__loading">
        <div class="loading-skeleton">
          <div class="skeleton-line skeleton-line--large"></div>
          <div class="skeleton-line skeleton-line--small"></div>
        </div>
      </div>

      <div v-else class="stat-card__value-container">
        <div class="stat-card__value">
          <span class="value-text">{{ displayValue }}</span>
          <span v-if="unit" class="value-unit">{{ unit }}</span>
        </div>

        <!-- 变化趋势 -->
        <div v-if="trend" class="stat-card__trend" :class="trendClasses">
          <component :is="trendIcon" class="w-4 h-4" />
          <span class="trend-text">{{ trendText }}</span>
        </div>
      </div>
    </div>

    <!-- 额外信息或操作 -->
    <div v-if="$slots.footer || description" class="stat-card__footer">
      <slot name="footer">
        <p v-if="description" class="stat-card__description">
          {{ description }}
        </p>
      </slot>
    </div>

    <!-- 加载遮罩 -->
    <div v-if="loading" class="stat-card__overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'

// 图标组件映射
const iconComponents = {
  search: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z'
    })
  ]),
  check: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M5 13l4 4L19 7'
    })
  ]),
  clock: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
    })
  ]),
  cursor: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122'
    })
  ]),
  users: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a4 4 0 11-8 0 4 4 0 018 0z'
    })
  ]),
  trending: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M13 7h8m0 0v8m0-8l-8 8-4-4-6 6'
    })
  ])
}

const trendIcons = {
  up: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M7 11l5-5m0 0l5 5m-5-5v12'
    })
  ]),
  down: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M17 13l-5 5m0 0l-5-5m5 5V6'
    })
  ]),
  stable: () => h('svg', {
    fill: 'none',
    stroke: 'currentColor',
    viewBox: '0 0 24 24'
  }, [
    h('path', {
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'stroke-width': '2',
      d: 'M5 12h14'
    })
  ])
}

interface TrendData {
  direction: 'up' | 'down' | 'stable'
  value: number
  text?: string
}

interface Props {
  title: string
  value: string | number
  subtitle?: string
  description?: string
  icon?: string
  unit?: string
  color?: 'green' | 'blue' | 'purple' | 'orange' | 'red' | 'gray'
  loading?: boolean
  trend?: TrendData
  clickable?: boolean
  formatValue?: boolean
}

interface Emits {
  (e: 'click'): void
}

const props = withDefaults(defineProps<Props>(), {
  color: 'green',
  loading: false,
  clickable: false,
  formatValue: true
})

const emit = defineEmits<Emits>()

// 计算属性
const iconName = computed(() => props.icon?.toLowerCase())

const iconComponent = computed(() => {
  if (!iconName.value || !iconComponents[iconName.value as keyof typeof iconComponents]) {
    return iconComponents.trending
  }
  return iconComponents[iconName.value as keyof typeof iconComponents]
})

const colorClasses = computed(() => {
  const colorMap = {
    green: 'stat-card--green',
    blue: 'stat-card--blue',
    purple: 'stat-card--purple',
    orange: 'stat-card--orange',
    red: 'stat-card--red',
    gray: 'stat-card--gray'
  }
  return colorMap[props.color]
})

const displayValue = computed(() => {
  if (props.loading) return '---'

  if (!props.formatValue) return props.value

  const numValue = Number(props.value)
  if (isNaN(numValue)) return props.value

  // 格式化数字显示
  if (numValue >= 1000000) {
    return (numValue / 1000000).toFixed(1) + 'M'
  }
  if (numValue >= 1000) {
    return (numValue / 1000).toFixed(1) + 'K'
  }

  return numValue.toLocaleString()
})

const trendClasses = computed(() => {
  if (!props.trend) return ''

  const directionMap = {
    up: 'stat-card__trend--up',
    down: 'stat-card__trend--down',
    stable: 'stat-card__trend--stable'
  }
  return directionMap[props.trend.direction]
})

const trendIcon = computed(() => {
  if (!props.trend) return null
  return trendIcons[props.trend.direction]
})

const trendText = computed(() => {
  if (!props.trend) return ''

  if (props.trend.text) {
    return props.trend.text
  }

  const value = props.trend.value
  const sign = props.trend.direction === 'up' ? '+' : props.trend.direction === 'down' ? '-' : ''
  return `${sign}${Math.abs(value)}%`
})

// 方法
const handleClick = () => {
  if (props.clickable && !props.loading) {
    emit('click')
  }
}
</script>

<style scoped>
.stat-card {
  @apply relative bg-white rounded-lg border border-gray-200 p-6 transition-all duration-200;
}

.stat-card--clickable {
  @apply cursor-pointer hover:shadow-md hover:border-gray-300;
}

.stat-card--loading {
  @apply pointer-events-none;
}

/* 颜色主题 */
.stat-card--green {
  @apply border-green-200 hover:border-green-300;
}

.stat-card--green .stat-card__icon {
  @apply text-green-600 bg-green-50;
}

.stat-card--green .stat-card__value {
  @apply text-green-700;
}

.stat-card--blue {
  @apply border-blue-200 hover:border-blue-300;
}

.stat-card--blue .stat-card__icon {
  @apply text-blue-600 bg-blue-50;
}

.stat-card--blue .stat-card__value {
  @apply text-blue-700;
}

.stat-card--purple {
  @apply border-purple-200 hover:border-purple-300;
}

.stat-card--purple .stat-card__icon {
  @apply text-purple-600 bg-purple-50;
}

.stat-card--purple .stat-card__value {
  @apply text-purple-700;
}

.stat-card--orange {
  @apply border-orange-200 hover:border-orange-300;
}

.stat-card--orange .stat-card__icon {
  @apply text-orange-600 bg-orange-50;
}

.stat-card--orange .stat-card__value {
  @apply text-orange-700;
}

.stat-card--red {
  @apply border-red-200 hover:border-red-300;
}

.stat-card--red .stat-card__icon {
  @apply text-red-600 bg-red-50;
}

.stat-card--red .stat-card__value {
  @apply text-red-700;
}

.stat-card--gray {
  @apply border-gray-200 hover:border-gray-300;
}

.stat-card--gray .stat-card__icon {
  @apply text-gray-600 bg-gray-50;
}

.stat-card--gray .stat-card__value {
  @apply text-gray-700;
}

/* 卡片头部 */
.stat-card__header {
  @apply flex items-center mb-4;
}

.stat-card__icon {
  @apply flex items-center justify-center w-12 h-12 rounded-lg mr-4;
}

.stat-card__title-container {
  @apply flex-1;
}

.stat-card__title {
  @apply text-sm font-medium text-gray-700 mb-1;
}

.stat-card__subtitle {
  @apply text-xs text-gray-500;
}

/* 卡片内容 */
.stat-card__content {
  @apply relative;
}

.stat-card__loading {
  @apply mb-4;
}

.loading-skeleton {
  @apply space-y-2;
}

.skeleton-line {
  @apply bg-gray-200 rounded animate-pulse;
}

.skeleton-line--large {
  @apply h-8 w-24;
}

.skeleton-line--small {
  @apply h-4 w-16;
}

.stat-card__value-container {
  @apply mb-4;
}

.stat-card__value {
  @apply flex items-baseline mb-2;
}

.value-text {
  @apply text-3xl font-bold tracking-tight;
}

.value-unit {
  @apply text-lg font-medium text-gray-500 ml-1;
}

/* 趋势指示器 */
.stat-card__trend {
  @apply flex items-center text-sm font-medium;
}

.stat-card__trend--up {
  @apply text-green-600;
}

.stat-card__trend--down {
  @apply text-red-600;
}

.stat-card__trend--stable {
  @apply text-gray-600;
}

.trend-text {
  @apply ml-1;
}

/* 卡片底部 */
.stat-card__footer {
  @apply border-t border-gray-100 pt-4;
}

.stat-card__description {
  @apply text-sm text-gray-600;
}

/* 加载遮罩 */
.stat-card__overlay {
  @apply absolute inset-0 bg-white/70 backdrop-blur-sm flex items-center justify-center rounded-lg;
}

.loading-spinner {
  @apply w-6 h-6 border-2 border-green-500 border-t-transparent rounded-full animate-spin;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .stat-card {
    @apply p-4;
  }

  .stat-card__header {
    @apply mb-3;
  }

  .stat-card__icon {
    @apply w-10 h-10 mr-3;
  }

  .value-text {
    @apply text-2xl;
  }

  .value-unit {
    @apply text-base;
  }
}

/* 动画效果 */
.stat-card {
  animation: fadeInUp 0.4s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 悬停效果增强 */
.stat-card--clickable:hover {
  transform: translateY(-2px);
  @apply shadow-lg;
}

.stat-card--clickable:active {
  transform: translateY(0);
  @apply shadow-md;
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .stat-card {
    @apply bg-gray-800 border-gray-700;
  }

  .stat-card__title {
    @apply text-gray-300;
  }

  .stat-card__subtitle {
    @apply text-gray-400;
  }

  .stat-card__description {
    @apply text-gray-400;
  }

  .stat-card__footer {
    @apply border-gray-700;
  }

  .loading-skeleton .skeleton-line {
    @apply bg-gray-700;
  }
}
</style>