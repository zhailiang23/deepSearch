<template>
  <div class="empty-state">
    <div class="empty-container">
      <!-- 图标区域 -->
      <div class="empty-icon">
        <div v-if="iconType === 'search'" class="icon-wrapper search-icon">
          <svg class="w-16 h-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="m21 21-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>

        <div v-else-if="iconType === 'no-results'" class="icon-wrapper no-results-icon">
          <svg class="w-16 h-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <!-- 放大镜叠加效果 -->
          <div class="search-overlay">
            <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m21 21-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>

        <div v-else-if="iconType === 'folder'" class="icon-wrapper folder-icon">
          <svg class="w-16 h-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
        </div>

        <div v-else class="icon-wrapper custom-icon">
          <slot name="icon">
            <svg class="w-16 h-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </slot>
        </div>
      </div>

      <!-- 主要内容 -->
      <div class="empty-content">
        <h3 class="empty-title">
          {{ title || getDefaultTitle() }}
        </h3>

        <p class="empty-description">
          {{ description || getDefaultDescription() }}
        </p>

        <!-- 搜索查询显示 -->
        <div v-if="query && showQuery" class="query-display">
          <p class="query-text">
            搜索词：<span class="query-highlight">"{{ query }}"</span>
          </p>
        </div>
      </div>

      <!-- 建议操作 -->
      <div class="empty-actions">
        <!-- 搜索建议 -->
        <div v-if="showSuggestions && suggestions.length > 0" class="suggestions-section">
          <p class="suggestions-title">您可以尝试：</p>
          <div class="suggestions-list">
            <button
              v-for="(suggestion, index) in suggestions"
              :key="index"
              class="suggestion-button"
              @click="handleSuggestionClick(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button
            v-if="showClearButton"
            class="action-button secondary"
            @click="handleClear"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            清空搜索
          </button>

          <button
            v-if="showRetryButton"
            class="action-button primary"
            @click="handleRetry"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            重新搜索
          </button>

          <slot name="actions" />
        </div>
      </div>

      <!-- 提示信息 -->
      <div v-if="showTips" class="tips-section">
        <div class="tip-item">
          <svg class="w-4 h-4 text-emerald-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>尝试使用不同的关键词</span>
        </div>
        <div class="tip-item">
          <svg class="w-4 h-4 text-emerald-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>检查拼写是否正确</span>
        </div>
        <div class="tip-item">
          <svg class="w-4 h-4 text-emerald-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>使用更通用的搜索词</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// 组件属性定义
export interface EmptyStateProps {
  iconType?: 'search' | 'no-results' | 'folder' | 'custom'
  title?: string
  description?: string
  query?: string
  showQuery?: boolean
  showSuggestions?: boolean
  suggestions?: string[]
  showClearButton?: boolean
  showRetryButton?: boolean
  showTips?: boolean
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<EmptyStateProps>(), {
  iconType: 'no-results',
  title: '',
  description: '',
  query: '',
  showQuery: true,
  showSuggestions: true,
  suggestions: () => ['用户管理', '系统设置', '数据分析', '权限配置'],
  showClearButton: true,
  showRetryButton: false,
  showTips: true,
  size: 'medium'
})

// 组件事件定义
const emit = defineEmits<{
  clear: []
  retry: []
  suggestion: [suggestion: string]
}>()

// 获取默认标题
const getDefaultTitle = () => {
  if (props.query) {
    return '未找到相关结果'
  }
  switch (props.iconType) {
    case 'search':
      return '开始搜索'
    case 'folder':
      return '暂无数据'
    default:
      return '未找到相关结果'
  }
}

// 获取默认描述
const getDefaultDescription = () => {
  if (props.query) {
    return `没有找到与"${props.query}"相关的搜索结果。`
  }
  switch (props.iconType) {
    case 'search':
      return '在上方输入关键词开始搜索。'
    case 'folder':
      return '当前没有可显示的数据。'
    default:
      return '请尝试调整搜索条件或使用其他关键词。'
  }
}

// 计算样式类
const containerClass = computed(() => {
  const sizeClasses = {
    small: 'empty-small',
    medium: 'empty-medium',
    large: 'empty-large'
  }
  return [sizeClasses[props.size]]
})

// 处理建议点击
const handleSuggestionClick = (suggestion: string) => {
  emit('suggestion', suggestion)
}

// 处理清空
const handleClear = () => {
  emit('clear')
}

// 处理重试
const handleRetry = () => {
  emit('retry')
}
</script>

<style scoped>
.empty-state {
  @apply flex items-center justify-center;
  @apply h-full min-h-96 p-6;
  @apply bg-gray-50;
}

.empty-container {
  @apply flex flex-col items-center;
  @apply max-w-sm w-full text-center;
}

/* 图标区域 */
.empty-icon {
  @apply relative mb-6;
}

.icon-wrapper {
  @apply flex items-center justify-center;
}

.search-overlay {
  @apply absolute -bottom-1 -right-1;
  @apply bg-white rounded-full p-1;
  @apply border-2 border-gray-100;
}

/* 内容区域 */
.empty-content {
  @apply mb-6;
}

.empty-title {
  @apply text-xl font-semibold text-gray-700 mb-2;
}

.empty-description {
  @apply text-base text-gray-500 leading-relaxed mb-4;
}

/* 查询显示 */
.query-display {
  @apply mb-4;
}

.query-text {
  @apply text-sm text-gray-600;
}

.query-highlight {
  @apply font-semibold text-gray-800;
  @apply bg-yellow-100 px-1.5 py-0.5 rounded;
}

/* 操作区域 */
.empty-actions {
  @apply w-full space-y-6;
}

/* 建议区域 */
.suggestions-section {
  @apply w-full;
}

.suggestions-title {
  @apply text-sm font-medium text-gray-600 mb-3;
}

.suggestions-list {
  @apply flex flex-wrap gap-2 justify-center;
}

.suggestion-button {
  @apply px-3 py-2;
  @apply text-sm font-medium text-emerald-600;
  @apply bg-emerald-50 border border-emerald-200;
  @apply rounded-full;
  @apply hover:bg-emerald-100 hover:border-emerald-300;
  @apply active:bg-emerald-200;
  @apply transition-colors duration-150;
  @apply min-h-[36px]; /* 触摸目标最小高度 */
}

/* 操作按钮 */
.action-buttons {
  @apply flex flex-col sm:flex-row gap-3 justify-center;
}

.action-button {
  @apply flex items-center justify-center px-6 py-3;
  @apply font-medium rounded-lg;
  @apply transition-colors duration-150;
  @apply min-h-[44px]; /* iOS 触摸目标最小高度 */
}

.action-button.primary {
  @apply bg-emerald-500 text-white;
  @apply hover:bg-emerald-600;
  @apply active:bg-emerald-700;
}

.action-button.secondary {
  @apply bg-white text-gray-700 border border-gray-300;
  @apply hover:bg-gray-50 hover:border-gray-400;
  @apply active:bg-gray-100;
}

/* 提示区域 */
.tips-section {
  @apply w-full mt-6 pt-6 border-t border-gray-200;
}

.tip-item {
  @apply flex items-center justify-center text-sm text-gray-600 mb-2;
}

/* 尺寸变体 */
.empty-small .empty-icon svg {
  @apply w-12 h-12;
}

.empty-small .empty-title {
  @apply text-lg;
}

.empty-small .empty-description {
  @apply text-sm;
}

.empty-large .empty-icon svg {
  @apply w-20 h-20;
}

.empty-large .empty-title {
  @apply text-2xl;
}

.empty-large .empty-description {
  @apply text-lg;
}

/* 移动端优化 */
@media (max-width: 640px) {
  .empty-state {
    @apply p-4;
  }

  .empty-container {
    @apply max-w-none;
  }

  .action-buttons {
    @apply flex-col;
  }

  .action-button {
    @apply w-full;
  }

  .suggestions-list {
    @apply gap-1.5;
  }

  .suggestion-button {
    @apply text-xs px-2.5 py-1.5;
    @apply min-h-[32px];
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .empty-state {
    @apply bg-gray-900;
  }

  .empty-title {
    @apply text-gray-200;
  }

  .empty-description {
    @apply text-gray-400;
  }

  .query-text {
    @apply text-gray-400;
  }

  .query-highlight {
    @apply text-gray-200 bg-yellow-900;
  }

  .suggestions-title {
    @apply text-gray-300;
  }

  .suggestion-button {
    @apply text-emerald-400 bg-emerald-900 border-emerald-700;
    @apply hover:bg-emerald-800 hover:border-emerald-600;
  }

  .action-button.primary {
    @apply bg-emerald-600 hover:bg-emerald-700;
  }

  .action-button.secondary {
    @apply bg-gray-800 text-gray-300 border-gray-600;
    @apply hover:bg-gray-700 hover:border-gray-500;
  }

  .tip-item {
    @apply text-gray-400;
  }

  .tips-section {
    @apply border-gray-700;
  }

  .search-overlay {
    @apply bg-gray-800 border-gray-600;
  }
}

/* 高对比度模式适配 */
@media (prefers-contrast: high) {
  .empty-title {
    @apply text-black;
  }

  .empty-description {
    @apply text-gray-700;
  }

  .suggestion-button {
    @apply border-2;
  }

  .action-button.primary {
    @apply bg-black hover:bg-gray-800;
  }
}

/* 动画效果 */
.empty-container {
  @apply opacity-100 duration-500;
}

.suggestion-button:active {
  transform: scale(0.95);
}

.action-button:active {
  transform: scale(0.98);
}

/* 减少动画模式适配 */
@media (prefers-reduced-motion: reduce) {
  .empty-container {
    @apply animate-none;
  }

  .suggestion-button:active,
  .action-button:active {
    transform: none;
  }
}
</style>