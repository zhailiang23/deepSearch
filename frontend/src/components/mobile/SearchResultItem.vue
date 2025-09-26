<template>
  <div class="search-result-item" @click="handleClick">
    <!-- 主要内容区域 -->
    <div class="result-main">
      <!-- 标题 -->
      <h3 class="result-title">
        <component
          :is="titleTag"
          v-html="highlightedTitle"
          class="title-content"
        />
        <!-- 相关度评分 -->
        <span v-if="result.relevance && showRelevance" class="relevance-score">
          {{ Math.round(result.relevance * 100) }}%
        </span>
      </h3>

      <!-- 内容摘要 -->
      <p v-if="result.content" class="result-content" v-html="highlightedContent" />

      <!-- 元数据信息 -->
      <div class="result-metadata">
        <!-- URL 显示 -->
        <div v-if="result.url" class="metadata-item url">
          <svg class="w-3 h-3 text-gray-400 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
          </svg>
          <span class="url-text">{{ formatUrl(result.url) }}</span>
        </div>

        <!-- 时间戳 -->
        <div v-if="result.timestamp" class="metadata-item timestamp">
          <svg class="w-3 h-3 text-gray-400 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ formatTime(result.timestamp) }}</span>
        </div>

        <!-- 索引信息 -->
        <div v-if="result.metadata?.index" class="metadata-item index">
          <svg class="w-3 h-3 text-gray-400 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <span>{{ result.metadata.index }}</span>
        </div>

        <!-- 文档类型 -->
        <div v-if="result.metadata?.type" class="metadata-item type">
          <span class="type-badge">{{ result.metadata.type }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧操作区域 -->
    <div class="result-actions">
      <!-- 收藏按钮 -->
      <button
        v-if="showFavoriteButton"
        class="action-button favorite"
        :class="{ 'is-favorited': isFavorited }"
        @click.stop="toggleFavorite"
      >
        <svg class="w-4 h-4" :class="isFavorited ? 'text-yellow-500' : 'text-gray-400'" fill="currentColor" viewBox="0 0 24 24">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
      </button>

      <!-- 更多操作按钮 -->
      <button
        v-if="showMoreActions"
        class="action-button more"
        @click.stop="toggleMoreActions"
      >
        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
        </svg>
      </button>

      <!-- 箭头指示器 -->
      <div class="arrow-indicator">
        <svg class="w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </div>
    </div>

    <!-- 更多操作菜单 -->
    <div v-if="showMoreMenu" class="more-actions-menu">
      <button class="menu-item" @click="copyLink">
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
        复制链接
      </button>
      <button class="menu-item" @click="shareResult">
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
        </svg>
        分享
      </button>
      <button class="menu-item" @click="viewDetails">
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        查看详情
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { SearchResult } from './MobileSearchApp.vue'

// 组件属性定义
export interface SearchResultItemProps {
  result: SearchResult
  highlight?: string
  index: number
  showRelevance?: boolean
  showFavoriteButton?: boolean
  showMoreActions?: boolean
  titleTag?: 'h3' | 'h4' | 'h5' | 'div'
  maxContentLength?: number
}

const props = withDefaults(defineProps<SearchResultItemProps>(), {
  highlight: '',
  showRelevance: true,
  showFavoriteButton: true,
  showMoreActions: true,
  titleTag: 'h3',
  maxContentLength: 150
})

// 组件事件定义
const emit = defineEmits<{
  click: [result: SearchResult]
  favorite: [result: SearchResult, favorited: boolean]
  share: [result: SearchResult]
  copy: [result: SearchResult]
  details: [result: SearchResult]
}>()

// 响应式数据
const isFavorited = ref(false)
const showMoreMenu = ref(false)

// 高亮显示文本
const highlightText = (text: string, query: string): string => {
  if (!query || !text) return text

  const keywords = query.trim().split(/\s+/)
  let highlightedText = text

  keywords.forEach(keyword => {
    if (keyword.length > 0) {
      const regex = new RegExp(`(${keyword})`, 'gi')
      highlightedText = highlightedText.replace(
        regex,
        '<mark class="bg-yellow-200 text-yellow-900 px-0.5 rounded">$1</mark>'
      )
    }
  })

  return highlightedText
}

// 高亮标题
const highlightedTitle = computed(() => {
  return highlightText(props.result.title, props.highlight)
})

// 高亮内容
const highlightedContent = computed(() => {
  let content = props.result.content || ''

  // 限制内容长度
  if (content.length > props.maxContentLength) {
    content = content.substring(0, props.maxContentLength) + '...'
  }

  return highlightText(content, props.highlight)
})

// 格式化 URL
const formatUrl = (url: string): string => {
  try {
    const urlObj = new URL(url)
    return `${urlObj.hostname}${urlObj.pathname}`
  } catch {
    return url
  }
}

// 格式化时间
const formatTime = (timestamp: string): string => {
  try {
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))

    if (days === 0) {
      return '今天'
    } else if (days === 1) {
      return '昨天'
    } else if (days < 7) {
      return `${days}天前`
    } else {
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }
  } catch {
    return timestamp
  }
}

// 处理点击事件
const handleClick = () => {
  emit('click', props.result)
}

// 切换收藏状态
const toggleFavorite = () => {
  isFavorited.value = !isFavorited.value
  emit('favorite', props.result, isFavorited.value)
}

// 切换更多操作菜单
const toggleMoreActions = () => {
  showMoreMenu.value = !showMoreMenu.value
}

// 复制链接
const copyLink = async () => {
  try {
    const textToCopy = props.result.url || props.result.title
    await navigator.clipboard.writeText(textToCopy)
    emit('copy', props.result)
    showMoreMenu.value = false
  } catch (err) {
    console.error('复制失败:', err)
  }
}

// 分享结果
const shareResult = () => {
  emit('share', props.result)
  showMoreMenu.value = false
}

// 查看详情
const viewDetails = () => {
  emit('details', props.result)
  showMoreMenu.value = false
}

// 处理点击外部关闭菜单
const handleClickOutside = (event: Event) => {
  const target = event.target as Element
  const item = target.closest('.search-result-item')

  if (!item || !item.contains(target)) {
    showMoreMenu.value = false
  }
}

// 检查是否已收藏
const checkFavoriteStatus = () => {
  try {
    const favorites = JSON.parse(localStorage.getItem('search-favorites') || '[]')
    isFavorited.value = favorites.some((fav: SearchResult) => fav.id === props.result.id)
  } catch (err) {
    console.warn('无法检查收藏状态:', err)
  }
}

// 组件挂载
onMounted(() => {
  checkFavoriteStatus()
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.search-result-item {
  @apply relative flex items-start;
  @apply bg-white rounded-lg border border-gray-200;
  @apply p-4 shadow-sm;
  @apply hover:shadow-md hover:border-emerald-300;
  @apply active:bg-emerald-50;
  @apply transition-all duration-150;
  @apply cursor-pointer;
  min-height: 120px;
}

.result-main {
  @apply flex-1 min-w-0;
}

.result-title {
  @apply mb-2;
}

.title-content {
  @apply text-base font-semibold text-gray-900;
  @apply leading-tight;
}

.relevance-score {
  @apply ml-2 px-1.5 py-0.5;
  @apply text-xs font-medium;
  @apply bg-emerald-100 text-emerald-700;
  @apply rounded-full;
}

.result-content {
  @apply text-sm text-gray-600;
  @apply leading-relaxed mb-3;
  @apply line-clamp-3;
}

.result-metadata {
  @apply flex flex-wrap items-center gap-3;
  @apply text-xs text-gray-500;
}

.metadata-item {
  @apply flex items-center;
}

.url-text {
  @apply truncate max-w-32;
}

.type-badge {
  @apply px-1.5 py-0.5;
  @apply bg-gray-100 text-gray-600;
  @apply rounded-full text-xs font-medium;
}

.result-actions {
  @apply flex flex-col items-end gap-2 ml-4;
  @apply relative;
}

.action-button {
  @apply p-2 rounded-full;
  @apply hover:bg-gray-100;
  @apply transition-colors duration-150;
}

.action-button.favorite.is-favorited {
  @apply bg-yellow-50 hover:bg-yellow-100;
}

.arrow-indicator {
  @apply mt-auto;
}

.more-actions-menu {
  @apply absolute right-0 top-full mt-1 z-10;
  @apply bg-white rounded-lg shadow-lg border border-gray-200;
  @apply py-1 min-w-32;
  @apply opacity-100 duration-150;
}

.menu-item {
  @apply w-full flex items-center px-3 py-2;
  @apply text-sm text-gray-700;
  @apply hover:bg-gray-100;
  @apply transition-colors duration-150;
}

/* 高亮样式 */
:deep(mark) {
  @apply bg-yellow-200 text-yellow-900 px-0.5 rounded;
}

/* 行截断 */
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 移动端优化 */
@media (max-width: 640px) {
  .search-result-item {
    @apply min-h-[100px];
    /* iOS 触摸目标最小高度 */
    min-height: 88px;
  }

  .action-button {
    @apply min-w-[44px] min-h-[44px];
  }

  .result-metadata {
    @apply gap-2;
  }

  .metadata-item {
    @apply text-xs;
  }

  .url-text {
    @apply max-w-24;
  }
}

/* 触摸反馈 */
.search-result-item:active {
  transform: scale(0.98);
}

.action-button:active {
  transform: scale(0.95);
}

/* 无障碍优化 */
.search-result-item:focus-visible {
  @apply outline-2 outline-emerald-500 outline-offset-2;
}

.action-button:focus-visible {
  @apply outline-1 outline-emerald-500 outline-offset-1;
}

/* 动画优化 */
.search-result-item {
  will-change: transform;
}

/* 高对比度模式适配 */
@media (prefers-contrast: high) {
  .search-result-item {
    @apply border-gray-400;
  }

  .result-title .title-content {
    @apply text-black;
  }

  .result-content {
    @apply text-gray-800;
  }
}

/* 减少动画模式适配 */
@media (prefers-reduced-motion: reduce) {
  .search-result-item,
  .action-button,
  .more-actions-menu {
    @apply transition-none;
  }

  .more-actions-menu {
    @apply animate-none;
  }
}
</style>