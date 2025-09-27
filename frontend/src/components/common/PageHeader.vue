<template>
  <div class="page-header">
    <div class="header-content">
      <div class="header-text">
        <h1 class="page-title">
          {{ title }}
        </h1>
        <p v-if="description" class="page-description">
          {{ description }}
        </p>
      </div>

      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions" />
      </div>
    </div>

    <!-- 面包屑导航 -->
    <div v-if="breadcrumbs && breadcrumbs.length > 0" class="breadcrumb-nav">
      <nav class="breadcrumb">
        <ol class="breadcrumb-list">
          <li
            v-for="(item, index) in breadcrumbs"
            :key="index"
            class="breadcrumb-item"
          >
            <router-link
              v-if="item.to && index < breadcrumbs.length - 1"
              :to="item.to"
              class="breadcrumb-link"
            >
              {{ item.label }}
            </router-link>
            <span
              v-else
              class="breadcrumb-current"
            >
              {{ item.label }}
            </span>
            <svg
              v-if="index < breadcrumbs.length - 1"
              class="breadcrumb-separator"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </li>
        </ol>
      </nav>
    </div>

    <!-- 统计信息或标签 -->
    <div v-if="$slots.meta || showStats" class="header-meta">
      <slot name="meta">
        <div v-if="showStats && stats" class="stats-summary">
          <div
            v-for="stat in stats"
            :key="stat.key"
            class="stat-item"
          >
            <span class="stat-label">{{ stat.label }}:</span>
            <span class="stat-value">{{ stat.value }}</span>
          </div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
interface BreadcrumbItem {
  label: string
  to?: string | { name: string; params?: any; query?: any }
}

interface StatItem {
  key: string
  label: string
  value: string | number
}

interface Props {
  title: string
  description?: string
  breadcrumbs?: BreadcrumbItem[]
  showStats?: boolean
  stats?: StatItem[]
}

defineProps<Props>()
</script>

<style scoped>
.page-header {
  @apply bg-white border-b border-gray-200 shadow-sm mb-6;
}

.header-content {
  @apply flex items-start justify-between px-6 py-6;
}

.header-text {
  @apply flex-1;
}

.page-title {
  @apply text-2xl font-bold text-gray-900 mb-2;
  background: linear-gradient(135deg, #059669, #10b981);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-description {
  @apply text-gray-600 text-sm leading-relaxed max-w-2xl;
}

.header-actions {
  @apply flex items-center space-x-3 ml-6;
}

.breadcrumb-nav {
  @apply px-6 pb-4;
}

.breadcrumb {
  @apply text-sm;
}

.breadcrumb-list {
  @apply flex items-center space-x-2;
}

.breadcrumb-item {
  @apply flex items-center;
}

.breadcrumb-link {
  @apply text-green-600 hover:text-green-700 hover:underline transition-colors duration-200;
}

.breadcrumb-current {
  @apply text-gray-500 font-medium;
}

.breadcrumb-separator {
  @apply w-4 h-4 text-gray-400 mx-2;
}

.header-meta {
  @apply px-6 pb-4 border-t border-gray-100;
}

.stats-summary {
  @apply flex items-center space-x-6 pt-4;
}

.stat-item {
  @apply flex items-center space-x-1;
}

.stat-label {
  @apply text-sm text-gray-600;
}

.stat-value {
  @apply text-sm font-semibold text-green-700;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    @apply flex-col items-start space-y-4 px-4 py-4;
  }

  .header-actions {
    @apply ml-0 w-full;
  }

  .page-title {
    @apply text-xl;
  }

  .breadcrumb-nav {
    @apply px-4;
  }

  .header-meta {
    @apply px-4;
  }

  .stats-summary {
    @apply flex-col items-start space-x-0 space-y-2;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .page-header {
    @apply bg-gray-800 border-gray-700;
  }

  .page-title {
    background: linear-gradient(135deg, #10b981, #34d399);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .page-description {
    @apply text-gray-400;
  }

  .breadcrumb-link {
    @apply text-green-400 hover:text-green-300;
  }

  .breadcrumb-current {
    @apply text-gray-400;
  }

  .stat-label {
    @apply text-gray-400;
  }

  .stat-value {
    @apply text-green-400;
  }
}

/* 动画效果 */
.page-header {
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 增强的绿色主题 */
.page-title {
  @apply relative;
}

.page-title::after {
  content: '';
  @apply absolute bottom-0 left-0 w-full h-0.5 bg-gradient-to-r from-green-500 to-green-600 rounded-full;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease-out;
}

.page-header:hover .page-title::after {
  transform: scaleX(1);
}
</style>