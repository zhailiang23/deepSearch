<template>
  <div class="search-results">
    <!-- 搜索结果统计 -->
    <div v-if="results.length > 0" class="results-stats">
      <span class="text-sm text-gray-600">
        找到 {{ total }} 条结果
        <span v-if="searchDuration" class="ml-2 text-xs text-gray-400">
          ({{ searchDuration }}ms)
        </span>
      </span>
    </div>

    <!-- 虚拟滚动容器 -->
    <div
      ref="containerRef"
      class="results-container"
      @scroll="handleScroll"
    >
      <div ref="scrollerRef" class="results-scroller">
        <!-- 搜索结果列表 -->
        <div class="results-list">
          <SearchResultItem
            v-for="(item, index) in visibleResults"
            :key="item.data.id"
            :result="item.data"
            :index="index"
            :highlight="highlight"
            @click="handleItemClick(item.data)"
          />
        </div>

        <!-- 加载更多指示器 -->
        <div v-if="hasMore" class="load-more-container">
          <div v-if="loading" class="loading-indicator">
            <div class="loading-spinner"></div>
            <span class="text-sm text-gray-500 ml-2">加载中...</span>
          </div>
          <button
            v-else
            class="load-more-button"
            @click="handleLoadMore"
          >
            加载更多结果
          </button>
        </div>

        <!-- 没有更多结果 -->
        <div v-else-if="results.length > 0" class="no-more-results">
          <span class="text-sm text-gray-400">已显示全部结果</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useInfiniteScroll } from '@vueuse/core'
import SearchResultItem from './SearchResultItem.vue'
import type { SearchResult } from './MobileSearchApp.vue'

// 组件属性定义
export interface SearchResultsProps {
  results: SearchResult[]
  loading?: boolean
  hasMore?: boolean
  highlight?: string
  error?: string | null
  total?: number
  searchDuration?: number
  enableVirtualScroll?: boolean
  itemHeight?: number
}

const props = withDefaults(defineProps<SearchResultsProps>(), {
  loading: false,
  hasMore: false,
  highlight: '',
  error: null,
  total: 0,
  searchDuration: 0,
  enableVirtualScroll: true,
  itemHeight: 120
})

// 组件事件定义
const emit = defineEmits<{
  loadMore: []
  itemClick: [result: SearchResult]
  scroll: [position: { x: number; y: number }]
}>()

// DOM 引用
const containerRef = ref<HTMLElement>()
const scrollerRef = ref<HTMLElement>()

// 简化的结果列表（暂不使用复杂的虚拟滚动）
const visibleResults = computed(() => {
  return props.results.map((item, index) => ({ data: item, index }))
})

// 无限滚动
const { reset: resetInfiniteScroll } = useInfiniteScroll(
  containerRef,
  () => {
    if (props.hasMore && !props.loading) {
      emit('loadMore')
    }
  },
  {
    distance: 100,
    canLoadMore: () => props.hasMore && !props.loading
  }
)

// 处理滚动事件
const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  const scrollPosition = {
    x: target.scrollLeft,
    y: target.scrollTop
  }
  emit('scroll', scrollPosition)

  // 检查是否需要加载更多
  const { scrollTop, scrollHeight, clientHeight } = target
  const threshold = 100 // 距离底部 100px 时触发

  if (scrollHeight - scrollTop <= clientHeight + threshold) {
    if (props.hasMore && !props.loading) {
      emit('loadMore')
    }
  }
}

// 处理结果项点击
const handleItemClick = (result: SearchResult) => {
  emit('itemClick', result)
}

// 处理加载更多按钮点击
const handleLoadMore = () => {
  if (props.hasMore && !props.loading) {
    emit('loadMore')
  }
}

// 滚动到顶部
const scrollToTop = (smooth = true) => {
  if (containerRef.value) {
    containerRef.value.scrollTo({
      top: 0,
      behavior: smooth ? 'smooth' : 'auto'
    })
  }
}

// 滚动到指定位置
const scrollToPosition = (position: { x?: number; y?: number }, smooth = true) => {
  if (containerRef.value) {
    containerRef.value.scrollTo({
      left: position.x || 0,
      top: position.y || 0,
      behavior: smooth ? 'smooth' : 'auto'
    })
  }
}

// 监听结果变化，重置滚动位置
watch(
  () => props.results,
  async (newResults, oldResults) => {
    if (newResults !== oldResults && newResults.length > 0) {
      await nextTick()
      if (newResults.length < (oldResults?.length || 0)) {
        // 结果减少，滚动到顶部
        scrollToTop(false)
      }
      resetInfiniteScroll()
    }
  }
)

// 组件挂载时初始化
onMounted(() => {
  // 初始化无限滚动
  resetInfiniteScroll()
})

// 暴露方法给父组件
defineExpose({
  scrollToTop,
  scrollToPosition,
  reset: resetInfiniteScroll
})
</script>

<style scoped>
.search-results {
  @apply flex flex-col h-full;
  @apply bg-gray-50;
}

.results-stats {
  @apply px-4 py-3;
  @apply bg-white border-b border-gray-200;
  @apply sticky top-0 z-10;
}

.results-container {
  @apply flex-1 overflow-auto;
  @apply relative;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.results-scroller {
  @apply min-h-full;
}

.results-list {
  @apply space-y-1;
  @apply px-4 py-2;
}

.load-more-container {
  @apply flex items-center justify-center;
  @apply py-6 px-4;
}

.loading-indicator {
  @apply flex items-center justify-center;
  @apply text-gray-500;
}

.loading-spinner {
  @apply w-5 h-5 border-2 border-emerald-500 border-t-transparent;
  @apply rounded-full animate-spin;
}

.load-more-button {
  @apply px-6 py-3;
  @apply bg-white border border-emerald-500;
  @apply text-emerald-600 font-medium;
  @apply rounded-lg shadow-sm;
  @apply hover:bg-emerald-50 hover:border-emerald-600;
  @apply active:bg-emerald-100;
  @apply transition-colors duration-150;
  @apply min-h-[44px]; /* iOS 触摸目标最小高度 */
}

.no-more-results {
  @apply flex items-center justify-center;
  @apply py-6 px-4;
}

/* 结果容器样式优化 */
.results-container {
  @apply h-full;
}

/* 滚动条样式 */
.results-container::-webkit-scrollbar {
  @apply w-1;
}

.results-container::-webkit-scrollbar-track {
  @apply bg-transparent;
}

.results-container::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
  @apply hover:bg-gray-400;
}

/* 移动端优化 */
@media (max-width: 640px) {
  .results-container {
    /* 改善移动端滚动性能 */
    -webkit-overflow-scrolling: touch;
    transform: translateZ(0);
  }

  .load-more-button {
    @apply w-full max-w-xs;
  }
}

/* 下拉刷新指示器 */
.pull-refresh-indicator {
  @apply absolute top-0 left-0 right-0;
  @apply flex items-center justify-center;
  @apply py-4 bg-white;
  @apply transform -translate-y-full;
  @apply transition-transform duration-300;
}

.pull-refresh-indicator.show {
  @apply translate-y-0;
}

/* 空状态时的样式调整 */
.results-list:empty + .load-more-container {
  @apply hidden;
}

/* 性能优化 */
.results-list {
  /* 启用硬件加速 */
  transform: translateZ(0);
  will-change: transform;
}

/* 触摸反馈 */
.load-more-button:active {
  transform: scale(0.98);
}

/* 加载动画优化 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-spinner {
  animation: spin 1s linear infinite;
}

/* 结果项进入动画 */
.results-list > * {
  @apply opacity-100;
  @apply duration-300;
}

/* 延迟动画，创造更好的视觉效果 */
.results-list > *:nth-child(1) { animation-delay: 0ms; }
.results-list > *:nth-child(2) { animation-delay: 50ms; }
.results-list > *:nth-child(3) { animation-delay: 100ms; }
.results-list > *:nth-child(4) { animation-delay: 150ms; }
.results-list > *:nth-child(5) { animation-delay: 200ms; }
</style>