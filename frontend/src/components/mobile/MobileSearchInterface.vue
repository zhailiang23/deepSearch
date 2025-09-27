<template>
  <div class="mobile-interface">
    <!-- 手机外壳 -->
    <div class="phone-frame">
      <!-- 屏幕区域 -->
      <div class="phone-screen">
        <!-- 状态栏 -->
        <div class="status-bar">
          <div class="status-left">
            <span class="time">{{ currentTime }}</span>
          </div>
          <div class="status-right">
            <div class="signal-icons">
              <div class="signal-strength">
                <div class="bar" :class="{ active: index < 3 }" v-for="index in 4" :key="index"></div>
              </div>
              <div class="wifi-icon">📶</div>
              <div class="battery">
                <div class="battery-level">🔋</div>
                <span class="battery-percent">85%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 搜索界面内容 -->
        <div class="search-app">
          <!-- 应用头部 -->
          <div class="app-header">
            <h1 class="app-title">智能搜索</h1>
            <button class="settings-btn" @click="showSettings = !showSettings">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4"></path>
              </svg>
            </button>
          </div>

          <!-- 搜索栏 -->
          <div class="search-container">
            <div class="search-input-wrapper">
              <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
              </svg>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索..."
                class="search-input"
                @input="handleSearch"
                @keydown.enter="performSearch"
              />
              <button
                v-if="searchQuery"
                @click="clearSearch"
                class="clear-btn"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <!-- 搜索建议 -->
            <div v-if="suggestions.length > 0 && showSuggestions" class="suggestions">
              <div
                v-for="suggestion in suggestions"
                :key="suggestion"
                class="suggestion-item"
                @click="selectSuggestion(suggestion)"
              >
                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <span>{{ suggestion }}</span>
              </div>
            </div>
          </div>


          <!-- 搜索结果区域 -->
          <div class="results-container">
            <!-- 加载状态 -->
            <div v-if="isLoading" class="loading-state">
              <div class="loading-spinner"></div>
              <p class="loading-text">搜索中...</p>
            </div>

            <!-- 错误状态 -->
            <div v-else-if="searchError" class="error-state">
              <div class="error-icon">❌</div>
              <h3 class="error-title">搜索失败</h3>
              <p class="error-message">{{ searchError }}</p>
              <button @click="retrySearch" class="retry-btn">
                重试
              </button>
            </div>

            <!-- 搜索结果 -->
            <div v-else-if="results.length > 0" class="results-list">

              <div
                v-for="result in results"
                :key="result.id"
                class="result-item"
                @click="viewResult(result)"
              >
                <div class="result-header">
                  <div class="result-title-wrapper">
                    <h3 class="result-title" v-html="highlightText(result.title)"></h3>
                    <span v-if="result.source?.type" class="result-type-tag">{{ result.source.type }}</span>
                  </div>
                  <span v-if="showScore" class="result-score">{{ result.score?.toFixed(2) }}</span>
                </div>
                <p class="result-summary" v-html="highlightText(result.source?.descript || result.summary)"></p>
              </div>

              <!-- 加载更多 -->
              <div v-if="hasMore" class="load-more-container">
                <button
                  @click="loadMore"
                  class="load-more-btn"
                  :disabled="isLoadingMore"
                >
                  <div v-if="isLoadingMore" class="loading-spinner-small"></div>
                  {{ isLoadingMore ? '加载中...' : '加载更多' }}
                </button>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else-if="searchQuery && !isLoading" class="empty-state">
              <div class="empty-icon">🔍</div>
              <h3 class="empty-title">未找到相关结果</h3>
              <p class="empty-message">尝试使用不同的关键词或调整搜索设置</p>
            </div>

            <!-- 初始状态 -->
            <div v-else class="initial-state">
              <div class="welcome-content">
                <div class="welcome-icon">🎯</div>
                <h2 class="welcome-title">智能搜索引擎</h2>
                <p class="welcome-message">输入关键词开始搜索</p>

                <!-- 搜索历史 -->
                <div v-if="searchHistory.length > 0" class="search-history">
                  <h3 class="history-title">最近搜索</h3>
                  <div class="history-list">
                    <button
                      v-for="item in searchHistory.slice(0, 5)"
                      :key="item.timestamp"
                      class="history-item"
                      @click="selectSuggestion(item.query)"
                    >
                      <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                      <span>{{ item.query }}</span>
                      <span class="history-count">({{ item.resultCount }})</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Home指示器 -->
        <div class="home-indicator"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useMobileSearchDemoStore } from '@/stores/mobileSearchDemo'
import { useInfiniteScroll } from '@vueuse/core'
import { SearchDataService, transformSearchResponse } from '@/api/searchData'
import type { SearchResult, SearchResponse } from '@/types/demo'

// 组件Props
interface Props {
  config?: any
  realTimeSync?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  realTimeSync: true
})

// 使用Store
const store = useMobileSearchDemoStore()

// 本地状态
const searchQuery = ref('')
const showSuggestions = ref(false)
const suggestions = ref<string[]>([])
const showSettings = ref(false)
const currentTime = ref('')
const activeSpace = ref('')
const infiniteScrollTrigger = ref<HTMLElement>()
const isLoadingMore = ref(false)

// 搜索防抖
let searchTimeout: number

// 计算属性
const selectedSpaces = computed(() => store.selectedSpaces)
const results = computed(() => store.results)
const isLoading = computed(() => store.isSearching)
const hasMore = computed(() => store.searchState.hasMore)
const totalResults = computed(() => store.searchState.total)
const searchDuration = computed(() => store.searchState.duration)
const searchHistory = computed(() => store.searchHistory)
const searchError = computed(() => store.searchState.error)

// 从配置获取显示选项
const showScore = computed(() => store.config.resultDisplay.showScore)
const showMetadata = computed(() => store.config.resultDisplay.showMetadata)
const debounceMs = computed(() => store.config.searchBehavior.debounceMs)
const autoSearch = computed(() => store.config.searchBehavior.autoSearch)
const minQueryLength = computed(() => store.config.searchBehavior.minQueryLength)

// 方法
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

const handleSearch = () => {
  if (!autoSearch.value) return

  clearTimeout(searchTimeout)

  if (searchQuery.value.length >= minQueryLength.value) {
    searchTimeout = setTimeout(() => {
      performSearch()
    }, debounceMs.value)
  }
}

const performSearch = async () => {
  if (!searchQuery.value.trim() || searchQuery.value.length < minQueryLength.value) return

  showSuggestions.value = false

  try {
    await realSearch()

    // 添加到搜索历史
    store.addToHistory({
      query: searchQuery.value,
      resultCount: store.searchState.total,
      config: { ...store.config }
    })

  } catch (error) {
    console.error('搜索失败:', error)
    // 如果搜索失败，可以考虑fallback到模拟数据或显示错误信息
    store.setSearchState({
      loading: false,
      error: error instanceof Error ? error.message : '搜索失败，请重试'
    })
  }
}

const realSearch = async () => {
  // 检查是否选择了搜索空间
  if (selectedSpaces.value.length === 0) {
    throw new Error('请先选择搜索空间')
  }

  const startTime = Date.now()
  store.setSearchState({ loading: true, query: searchQuery.value })

  try {
    // 构建搜索请求参数
    const searchRequest = {
      searchSpaceId: selectedSpaces.value[0].id, // 目前只支持单个搜索空间
      query: searchQuery.value,
      page: 1,
      size: store.config.pagination.pageSize,
      enablePinyinSearch: store.config.pinyinSearch.enabled,
      pinyinMode: store.config.pinyinSearch.enabled ?
        (store.config.pinyinSearch.mode === 'fuzzy' ? 'FUZZY' as const :
         store.config.pinyinSearch.mode === 'exact' ? 'STRICT' as const : 'AUTO' as const) : 'AUTO' as const
    }

    console.log('搜索请求参数:', searchRequest)

    // 调用搜索API
    const backendResponse = await SearchDataService.searchData(searchRequest)

    // 转换响应格式
    const searchResponse = transformSearchResponse(backendResponse)

    // 计算搜索耗时
    const duration = Date.now() - startTime

    // 更新store状态
    store.setResults(searchResponse.results)
    store.setSearchState({
      loading: false,
      hasMore: searchResponse.hasMore,
      total: searchResponse.total,
      duration,
      page: searchResponse.page
    })

    console.log('搜索完成:', {
      total: searchResponse.total,
      results: searchResponse.results.length,
      duration
    })

  } catch (error) {
    console.error('真实搜索失败:', error)
    throw error
  }
}

const clearSearch = () => {
  searchQuery.value = ''
  store.clearResults()
}

const retrySearch = () => {
  if (searchQuery.value) {
    performSearch()
  }
}

const selectSuggestion = (suggestion: string) => {
  searchQuery.value = suggestion
  showSuggestions.value = false
  performSearch()
}

const setActiveSpace = (spaceId: string) => {
  activeSpace.value = activeSpace.value === spaceId ? '' : spaceId
  if (searchQuery.value) {
    performSearch()
  }
}

const loadMore = async () => {
  if (isLoadingMore.value || !hasMore.value || selectedSpaces.value.length === 0) return

  isLoadingMore.value = true

  try {
    const nextPage = store.searchState.page + 1

    // 构建加载更多的搜索请求参数
    const searchRequest = {
      searchSpaceId: selectedSpaces.value[0].id,
      query: searchQuery.value,
      page: nextPage,
      size: store.config.pagination.pageSize,
      enablePinyinSearch: store.config.pinyinSearch.enabled,
      pinyinMode: store.config.pinyinSearch.enabled ?
        (store.config.pinyinSearch.mode === 'fuzzy' ? 'FUZZY' as const :
         store.config.pinyinSearch.mode === 'exact' ? 'STRICT' as const : 'AUTO' as const) : 'AUTO' as const
    }

    console.log('加载更多请求参数:', searchRequest)

    // 调用搜索API获取下一页数据
    const backendResponse = await SearchDataService.searchData(searchRequest)

    // 转换响应格式
    const searchResponse = transformSearchResponse(backendResponse)

    // 追加新结果到现有结果
    store.setResults(searchResponse.results, true)
    store.setSearchState({
      hasMore: searchResponse.hasMore,
      page: nextPage
    })

    console.log('加载更多完成:', {
      page: nextPage,
      newResults: searchResponse.results.length,
      totalResults: store.results.length
    })

  } catch (error) {
    console.error('加载更多失败:', error)
  } finally {
    isLoadingMore.value = false
  }
}

const viewResult = (result: SearchResult) => {
  console.log('查看结果:', result)
  // 这里可以打开详情页或执行其他操作
}

const highlightText = (text: string): string => {
  if (!searchQuery.value) return text

  const query = searchQuery.value.trim()
  const regex = new RegExp(`(${query})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

const formatCount = (count: number): string => {
  if (count >= 1000000) {
    return (count / 1000000).toFixed(1) + 'M'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'K'
  } else {
    return count.toString()
  }
}

const formatDate = (dateStr: string): string => {
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric'
    })
  } catch {
    return dateStr
  }
}

// 无限滚动已移除，现在只使用按钮加载

// 生命周期
let timeInterval: number

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)

  // 初始化活跃搜索空间
  if (selectedSpaces.value.length > 0) {
    activeSpace.value = selectedSpaces.value[0].id
  }
})

onUnmounted(() => {
  clearInterval(timeInterval)
  clearTimeout(searchTimeout)
})

// 监听配置变更
watch(() => store.config, () => {
  // 配置变更时可能需要重新搜索
  if (searchQuery.value && props.realTimeSync) {
    performSearch()
  }
}, { deep: true })

watch(() => selectedSpaces.value, () => {
  // 搜索空间变更时更新活跃空间
  if (selectedSpaces.value.length > 0 && !selectedSpaces.value.find(s => s.id === activeSpace.value)) {
    activeSpace.value = selectedSpaces.value[0].id
  }
})
</script>

<style scoped>
.mobile-interface {
  @apply block;
}

.phone-frame {
  @apply relative bg-gray-900 rounded-3xl p-2 shadow-2xl;
  width: 375px;
  height: 814px;
}

.phone-screen {
  @apply relative bg-white rounded-2xl overflow-hidden flex flex-col;
  width: 100%;
  height: 100%;
}

/* 状态栏 */
.status-bar {
  @apply flex items-center justify-between px-6 py-2 text-xs font-medium bg-white;
  height: 44px;
}

.time {
  @apply text-gray-900 font-semibold;
}

.signal-icons {
  @apply flex items-center gap-2;
}

.signal-strength {
  @apply flex items-end gap-1;
}

.signal-strength .bar {
  @apply w-1 bg-gray-300 rounded-full;
  height: 8px;
}

.signal-strength .bar:nth-child(1) { height: 4px; }
.signal-strength .bar:nth-child(2) { height: 6px; }
.signal-strength .bar:nth-child(3) { height: 8px; }
.signal-strength .bar:nth-child(4) { height: 10px; }

.signal-strength .bar.active {
  @apply bg-green-500;
}

.battery {
  @apply flex items-center gap-1;
}

.battery-percent {
  @apply text-gray-700;
}

/* 搜索应用 */
.search-app {
  @apply flex-1 flex flex-col bg-gray-50;
  min-height: 0; /* 确保flex容器高度正确 */
}

.app-header {
  @apply flex items-center justify-between px-4 py-3 bg-white border-b border-gray-200;
}

.app-title {
  @apply text-lg font-semibold text-gray-900;
}

.settings-btn {
  @apply p-2 text-gray-600 hover:text-emerald-600 transition-colors;
}

/* 搜索容器 */
.search-container {
  @apply relative px-4 py-3 bg-white border-b border-gray-200;
}

.search-input-wrapper {
  @apply relative flex items-center bg-gray-100 rounded-lg px-3 py-2;
}

.search-icon {
  @apply w-5 h-5 text-gray-400 mr-2 flex-shrink-0;
}

.search-input {
  @apply flex-1 bg-transparent border-none outline-none text-sm text-gray-900 placeholder-gray-500;
}

.clear-btn {
  @apply p-1 text-gray-400 hover:text-gray-600 transition-colors ml-2;
}

.suggestions {
  @apply absolute top-full left-4 right-4 bg-white border border-gray-200 rounded-lg shadow-lg z-10 max-h-60 overflow-y-auto;
}

.suggestion-item {
  @apply flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer border-b border-gray-100 last:border-b-0;
}

/* 搜索空间标签 */
.search-spaces {
  @apply flex gap-2 px-4 py-3 bg-white border-b border-gray-200 overflow-x-auto;
}

.space-chip {
  @apply flex items-center gap-2 px-3 py-1 rounded-full border border-gray-300 cursor-pointer flex-shrink-0 transition-colors;
}

.space-chip.active {
  @apply bg-emerald-100 border-emerald-300 text-emerald-800;
}

.space-status {
  @apply w-2 h-2 rounded-full flex-shrink-0;
}

.status-healthy { @apply bg-green-500; }
.status-warning { @apply bg-yellow-500; }
.status-error { @apply bg-red-500; }
.status-unknown { @apply bg-gray-400; }

.space-name {
  @apply text-sm font-medium;
}

.space-count {
  @apply text-xs text-gray-500;
}

/* 结果容器 */
.results-container {
  @apply flex-1 overflow-y-auto;
  min-height: 0; /* 确保flex子元素可以滚动 */
}

.loading-state {
  @apply flex flex-col items-center justify-center py-12;
}

.loading-spinner {
  @apply w-8 h-8 border-2 border-emerald-200 border-t-emerald-600 rounded-full animate-spin mb-3;
}

.loading-text {
  @apply text-sm text-gray-600;
}

.results-list {
  @apply divide-y divide-gray-200;
}

.results-header {
  @apply flex items-center justify-between px-4 py-3 bg-gray-50 text-sm text-gray-600;
}

.result-item {
  @apply px-4 py-4 hover:bg-gray-50 cursor-pointer transition-colors;
}

.result-header {
  @apply flex items-start justify-between gap-2 mb-2;
}

.result-title-wrapper {
  @apply flex items-center gap-2 flex-1;
}

.result-title {
  @apply text-sm font-semibold text-gray-900;
}

.result-type-tag {
  @apply inline-flex items-center px-2 py-1 text-xs font-medium rounded-full bg-emerald-100 text-emerald-800 flex-shrink-0;
}

.result-score {
  @apply text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded-full flex-shrink-0;
}

.result-summary {
  @apply text-sm text-gray-700 mb-2 line-clamp-2;
}

.result-meta {
  @apply flex items-center gap-2 text-xs text-gray-500;
}

.result-index {
  @apply bg-emerald-100 text-emerald-800 px-2 py-1 rounded-full;
}

.result-type {
  @apply bg-blue-100 text-blue-800 px-2 py-1 rounded-full;
}

.result-date {
  @apply text-gray-500;
}

/* 加载更多 */
.load-more-container {
  @apply px-4 py-4;
}

.load-more-btn {
  @apply w-full flex items-center justify-center gap-2 py-3 px-4 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed;
}

.infinite-scroll-trigger {
  @apply flex items-center justify-center py-4;
}

.loading-spinner-small {
  @apply w-5 h-5 border-2 border-emerald-200 border-t-emerald-600 rounded-full animate-spin;
}

/* 空状态 */
.empty-state, .initial-state {
  @apply flex flex-col items-center justify-center py-12 px-6;
}

.empty-icon, .welcome-icon {
  @apply text-4xl mb-4;
}

.empty-title, .welcome-title {
  @apply text-lg font-semibold text-gray-900 mb-2;
}

.empty-message, .welcome-message {
  @apply text-sm text-gray-600 text-center;
}

/* 错误状态 */
.error-state {
  @apply flex flex-col items-center justify-center py-12 px-6;
}

.error-icon {
  @apply text-4xl mb-4;
}

.error-title {
  @apply text-lg font-semibold text-red-600 mb-2;
}

.error-message {
  @apply text-sm text-red-500 text-center mb-4;
}

.retry-btn {
  @apply px-6 py-2 bg-red-500 text-white text-sm font-medium rounded-lg hover:bg-red-600 transition-colors;
}

.search-history {
  @apply mt-6 w-full;
}

.history-title {
  @apply text-sm font-medium text-gray-700 mb-3;
}

.history-list {
  @apply space-y-2;
}

.history-item {
  @apply flex items-center gap-3 w-full px-3 py-2 text-left bg-white rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors;
}

.history-count {
  @apply text-xs text-gray-500 ml-auto;
}

/* Home指示器 */
.home-indicator {
  @apply w-32 h-1 bg-gray-900 rounded-full mx-auto my-2;
}

/* 搜索高亮 */
:deep(.search-highlight) {
  @apply bg-yellow-200 text-yellow-900 px-1 rounded;
}

/* 滚动条样式 */
.results-container::-webkit-scrollbar {
  width: 4px;
}

.results-container::-webkit-scrollbar-track {
  background: transparent;
}

.results-container::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}

.results-container::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}

.search-spaces::-webkit-scrollbar {
  height: 2px;
}

.search-spaces::-webkit-scrollbar-track {
  background: transparent;
}

.search-spaces::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}
</style>