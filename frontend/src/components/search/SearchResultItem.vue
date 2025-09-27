<template>
  <div
    class="search-result-item"
    :class="{
      'clicked': isClicked,
      'has-clicks': clickCount > 0
    }"
    @click="handleClick"
    @keydown.enter="handleKeyboardClick"
    @keydown.space.prevent="handleKeyboardClick"
    tabindex="0"
    role="button"
    :aria-label="`搜索结果：${result.title}`"
    :aria-describedby="`result-${result.id}-desc`"
  >
    <!-- 主要内容区域 -->
    <div class="result-main">
      <!-- 标题 -->
      <h3 class="result-title">
        <span class="title-content">{{ result.title }}</span>
        <!-- 点击计数指示器 -->
        <span
          v-if="clickCount > 0"
          class="click-indicator"
          :aria-label="`已点击 ${clickCount} 次`"
        >
          {{ clickCount }}
        </span>
      </h3>

      <!-- 内容摘要 -->
      <p
        v-if="result.summary"
        class="result-summary"
        :id="`result-${result.id}-desc`"
      >
        {{ result.summary }}
      </p>

      <!-- 元数据信息 -->
      <div class="result-meta">
        <!-- URL 显示 -->
        <span v-if="result.url" class="result-url">
          {{ formatUrl(result.url) }}
        </span>

        <!-- 分数显示 -->
        <span v-if="result.score" class="result-score">
          相关度: {{ Math.round(result.score * 100) }}%
        </span>
      </div>
    </div>

    <!-- 点击反馈动画 -->
    <transition name="click-feedback" mode="out-in">
      <div v-if="showClickFeedback" class="click-feedback" key="feedback"></div>
    </transition>

    <!-- 加载状态指示器 -->
    <div v-if="isTracking" class="tracking-indicator" aria-hidden="true">
      <svg class="w-3 h-3 animate-pulse text-green-500" fill="currentColor" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="3"/>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useClickTracking } from '@/composables/useClickTracking'
import type { SearchResult } from '@/types/searchLog'

interface Props {
  result: SearchResult
  searchLogId: number
  position: number
  enableTracking?: boolean
}

interface Emits {
  (e: 'click', result: SearchResult, position: number, event: MouseEvent | KeyboardEvent): void
  (e: 'navigate', result: SearchResult): void
  (e: 'tracking-error', error: string): void
}

const props = withDefaults(defineProps<Props>(), {
  enableTracking: true
})

const emit = defineEmits<Emits>()

// 点击追踪组合函数
const { trackClick, isTracking: globalTracking } = useClickTracking()

// 组件状态
const isClicked = ref(false)
const clickCount = ref(0)
const showClickFeedback = ref(false)
const isTracking = ref(false)

// 计算属性
const canTrack = computed(() => props.enableTracking && globalTracking.value)

/**
 * 处理鼠标点击事件
 */
const handleClick = async (event: MouseEvent) => {
  await recordClick(event)

  // 触发点击事件
  emit('click', props.result, props.position, event)

  // 处理导航
  if (props.result.url) {
    // 根据修饰键决定打开方式
    if (event.ctrlKey || event.metaKey) {
      window.open(props.result.url, '_blank', 'noopener,noreferrer')
    } else {
      emit('navigate', props.result)
      // 如果父组件没有处理导航事件，默认跳转
      setTimeout(() => {
        window.location.href = props.result.url
      }, 100)
    }
  }
}

/**
 * 处理键盘点击事件
 */
const handleKeyboardClick = async (event: KeyboardEvent) => {
  if (event.code === 'Enter' || event.code === 'Space') {
    await recordClick(event)

    // 触发点击事件
    emit('click', props.result, props.position, event)

    // 处理导航
    if (props.result.url) {
      emit('navigate', props.result)
      // 如果父组件没有处理导航事件，默认跳转
      setTimeout(() => {
        window.location.href = props.result.url
      }, 100)
    }
  }
}

/**
 * 记录点击行为
 */
const recordClick = async (event: MouseEvent | KeyboardEvent) => {
  if (!canTrack.value) {
    console.debug('点击追踪已禁用或不可用')
    return
  }

  try {
    // 显示追踪状态
    isTracking.value = true

    // 显示点击反馈
    showClickFeedback.value = true
    isClicked.value = true
    clickCount.value++

    // 记录点击
    await trackClick(
      props.searchLogId,
      props.result,
      props.position,
      event
    )

    // 隐藏反馈动画
    setTimeout(() => {
      showClickFeedback.value = false
    }, 300)

    // 重置点击状态
    setTimeout(() => {
      isClicked.value = false
    }, 1000)

  } catch (error: any) {
    console.error('记录点击失败:', error)
    emit('tracking-error', error.message || '点击记录失败')
    // 即使记录失败也不影响用户操作
  } finally {
    isTracking.value = false
  }
}

/**
 * 格式化 URL 显示
 */
const formatUrl = (url: string): string => {
  try {
    const urlObj = new URL(url)
    return `${urlObj.hostname}${urlObj.pathname}`
  } catch {
    return url
  }
}
</script>

<style scoped>
.search-result-item {
  @apply relative p-4 border border-gray-200 rounded-lg;
  @apply hover:bg-green-50 hover:border-green-300;
  @apply transition-all duration-200 cursor-pointer;
  @apply focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-opacity-50;
}

.search-result-item.clicked {
  @apply bg-green-100 border-green-400;
  transform: scale(0.98);
}

.search-result-item.has-clicks {
  @apply border-l-4 border-l-green-500;
}

.search-result-item:focus-visible {
  @apply ring-2 ring-green-500 ring-opacity-50 outline-none;
}

.result-main {
  @apply flex-1 min-w-0;
}

.result-title {
  @apply flex items-center justify-between mb-2;
}

.title-content {
  @apply text-lg font-medium text-green-800 flex-1;
  @apply line-clamp-2;
}

.click-indicator {
  @apply ml-2 bg-green-100 text-green-800 px-2 py-1 rounded-full text-xs font-medium;
  @apply flex-shrink-0;
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-summary {
  @apply text-gray-600 mb-3 line-clamp-2;
  @apply text-sm leading-relaxed;
}

.result-meta {
  @apply flex items-center gap-4 text-sm text-gray-500;
}

.result-url {
  @apply text-green-600 hover:underline truncate;
  max-width: 200px;
}

.result-score {
  @apply bg-gray-100 text-gray-700 px-2 py-1 rounded text-xs;
}

.click-feedback {
  @apply absolute inset-0 bg-green-200 rounded-lg pointer-events-none;
  @apply opacity-50;
}

.tracking-indicator {
  @apply absolute top-2 right-2;
}

/* 动画效果 */
.click-feedback-enter-active {
  transition: opacity 0.2s ease-out;
}

.click-feedback-leave-active {
  transition: opacity 0.1s ease-in;
}

.click-feedback-enter-from,
.click-feedback-leave-to {
  opacity: 0;
}

/* 行截断样式 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 无障碍支持 */
.search-result-item[aria-pressed="true"] {
  @apply bg-green-100;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .search-result-item {
    @apply p-3;
  }

  .title-content {
    @apply text-base;
  }

  .result-meta {
    @apply flex-col items-start gap-2;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .search-result-item {
    @apply border-gray-400;
  }

  .title-content {
    @apply text-black;
  }

  .result-summary {
    @apply text-gray-800;
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .search-result-item,
  .click-feedback {
    @apply transition-none;
  }

  .search-result-item.clicked {
    transform: none;
  }
}

/* 性能优化 */
.search-result-item {
  contain: layout style paint;
  will-change: transform;
}
</style>