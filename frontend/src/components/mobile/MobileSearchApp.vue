<template>
  <div class="mobile-search-app">
    <!-- 搜索输入框 - 固定在顶部 -->
    <div class="search-header">
      <SearchInput
        v-model="searchQuery"
        :placeholder="searchPlaceholder"
        :disabled="loading"
        @search="handleSearch"
        @clear="handleClear"
      />
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-content">
      <!-- 加载状态 -->
      <LoadingState v-if="loading && results.length === 0" />

      <!-- 错误状态 -->
      <ErrorState
        v-else-if="error"
        :message="error"
        @retry="handleRetry"
      />

      <!-- 空状态 -->
      <EmptyState
        v-else-if="!loading && results.length === 0 && hasSearched"
        :query="searchQuery"
      />

      <!-- 搜索结果 -->
      <SearchResults
        v-else
        :results="results"
        :loading="loading && results.length > 0"
        :has-more="hasMore"
        :highlight="searchQuery"
        @load-more="handleLoadMore"
        @item-click="handleItemClick"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { searchDataService, type SearchDataRequest } from '@/services/searchDataService'
import type { TableRow } from '@/types/tableData'
import SearchInput from './SearchInput.vue'
import SearchResults from './SearchResults.vue'
import LoadingState from './LoadingState.vue'
import EmptyState from './EmptyState.vue'
import ErrorState from './ErrorState.vue'

// 组件属性定义
export interface MobileSearchAppProps {
  apiEndpoint?: string
  initialQuery?: string
  enableHistory?: boolean
  pageSize?: number
  searchSpaceId?: string
  channel?: string
  enableRerank?: boolean    // 是否启用语义重排序
  rerankTopN?: number       // 重排序Top-N
}

const props = withDefaults(defineProps<MobileSearchAppProps>(), {
  apiEndpoint: '',
  initialQuery: '',
  enableHistory: true,
  pageSize: 20,
  searchSpaceId: '1',
  enableRerank: false,
  rerankTopN: 50
})

// 组件事件定义
const emit = defineEmits<{
  itemClick: [result: SearchResult]
  search: [query: string, results: SearchResult[]]
}>()

// 搜索结果数据结构
export interface SearchResult {
  id: string
  title: string
  content: string
  url?: string
  timestamp?: string
  relevance?: number
  metadata?: Record<string, any>
}

// 响应式数据
const searchQuery = ref(props.initialQuery)
const results = ref<SearchResult[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const hasSearched = ref(false)
const hasMore = ref(false)
const currentPage = ref(1)
const total = ref(0)

// 搜索配置
const searchPlaceholder = '请输入搜索关键词...'

// 防抖搜索函数
const debouncedSearch = useDebounceFn((query: string) => {
  if (query.trim()) {
    performSearch(query, true)
  }
}, 300)

// 监听搜索输入变化
watch(searchQuery, (newQuery) => {
  if (newQuery.trim()) {
    debouncedSearch(newQuery)
  } else {
    handleClear()
  }
})

// HTML解码函数
const decodeHtmlEntities = (text: string): string => {
  const textarea = document.createElement('textarea')
  textarea.innerHTML = text
  return textarea.value
}

// HTML清洗函数 - 清理损坏或嵌套的HTML标签
const cleanHtmlTags = (text: string): string => {
  if (!text) return ''

  // 移除损坏的嵌套标签,如 <m<mark>
  let cleaned = text.replace(/<[^>]*<[^>]*>/g, match => {
    // 如果发现嵌套标签,只保留最内层的完整标签
    const innerMatch = match.match(/<([^<>]+)>$/);
    return innerMatch ? `<${innerMatch[1]}>` : ''
  })

  // 规范化高亮标签:将所有 <mark class="search-highlight"> 转换为 <em>
  cleaned = cleaned.replace(/<mark[^>]*class="search-highlight"[^>]*>/gi, '<em>')
  cleaned = cleaned.replace(/<\/mark>/gi, '</em>')

  // 移除其他可能存在的mark标签
  cleaned = cleaned.replace(/<\/?mark[^>]*>/gi, '')

  return cleaned
}

// 转换 TableRow 到 SearchResult
const convertToSearchResult = (row: TableRow): SearchResult => {
  const source = row._source
  // 获取原始数据
  let title = source.title || source.name || '未知标题'
  let content = source.content || source.description || source.summary || ''

  // 如果有highlight字段,优先使用高亮后的内容
  if (row.highlight) {
    if (row.highlight.title && row.highlight.title.length > 0) {
      title = row.highlight.title[0]
    }
    if (row.highlight.content && row.highlight.content.length > 0) {
      content = row.highlight.content[0]
    }
    if (row.highlight.name && row.highlight.name.length > 0) {
      title = row.highlight.name[0]
    }
    if (row.highlight.description && row.highlight.description.length > 0) {
      content = row.highlight.description[0]
    }
  }

  // 清洗HTML标签 - 移除损坏和嵌套的标签
  title = cleanHtmlTags(title)
  content = cleanHtmlTags(content)

  // 注意: 不要使用decodeHtmlEntities,因为它会移除HTML标签
  // highlight字段返回的<em>标签需要保留,让v-html渲染

  console.log('转换搜索结果:', {
    id: row._id,
    highlightTitle: row.highlight?.name?.[0],
    finalTitle: title,
    hasHtmlTags: /<[^>]+>/.test(title)
  })

  return {
    id: row._id,
    title,
    content,
    url: source.url,
    timestamp: source.timestamp || source.createdAt || source.updatedAt,
    relevance: row._score,
    metadata: {
      index: row._index,
      type: row._type,
      version: row._version,
      ...source
    }
  }
}

// 执行搜索
const performSearch = async (query: string, reset = false) => {
  try {
    loading.value = true
    error.value = null

    if (reset) {
      results.value = []
      currentPage.value = 1
    }

    const searchParams: SearchDataRequest = {
      searchSpaceId: props.searchSpaceId,
      query: query.trim(),
      page: currentPage.value,
      size: props.pageSize,
      channel: props.channel,
      enableRerank: props.enableRerank,
      rerankTopN: props.rerankTopN
    }

    const response = await searchDataService.search(searchParams)

    const searchResults = response.data.map(convertToSearchResult)

    if (reset) {
      results.value = searchResults
    } else {
      results.value.push(...searchResults)
    }

    total.value = response.total
    hasMore.value = currentPage.value * props.pageSize < response.total
    hasSearched.value = true

    emit('search', query, searchResults)
  } catch (err: any) {
    console.error('搜索失败:', err)
    error.value = err.message || '搜索请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = (query: string) => {
  searchQuery.value = query
  if (query.trim()) {
    performSearch(query, true)
  }
}

// 处理清空
const handleClear = () => {
  searchQuery.value = ''
  results.value = []
  error.value = null
  hasSearched.value = false
  hasMore.value = false
  currentPage.value = 1
  total.value = 0
}

// 处理重试
const handleRetry = () => {
  if (searchQuery.value.trim()) {
    performSearch(searchQuery.value, true)
  }
}

// 处理加载更多
const handleLoadMore = async () => {
  if (!loading.value && hasMore.value && searchQuery.value.trim()) {
    currentPage.value++
    await performSearch(searchQuery.value, false)
  }
}

// 处理结果项点击
const handleItemClick = (result: SearchResult) => {
  emit('itemClick', result)
}

// 搜索历史管理
const searchHistory = ref<string[]>([])

const addToHistory = (query: string) => {
  if (!props.enableHistory || !query.trim()) return

  const trimmedQuery = query.trim()
  const index = searchHistory.value.indexOf(trimmedQuery)

  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }

  searchHistory.value.unshift(trimmedQuery)

  // 限制历史记录数量
  if (searchHistory.value.length > 10) {
    searchHistory.value = searchHistory.value.slice(0, 10)
  }

  // 保存到本地存储
  try {
    localStorage.setItem('mobile-search-history', JSON.stringify(searchHistory.value))
  } catch (err) {
    console.warn('无法保存搜索历史:', err)
  }
}

// 加载搜索历史
const loadSearchHistory = () => {
  if (!props.enableHistory) return

  try {
    const saved = localStorage.getItem('mobile-search-history')
    if (saved) {
      searchHistory.value = JSON.parse(saved)
    }
  } catch (err) {
    console.warn('无法加载搜索历史:', err)
  }
}

// 组件挂载
onMounted(() => {
  loadSearchHistory()

  // 如果有初始查询，执行搜索
  if (props.initialQuery.trim()) {
    performSearch(props.initialQuery, true)
  }
})

// 监听成功的搜索，添加到历史
watch([searchQuery, () => results.value.length], ([query, resultCount]) => {
  if (query && resultCount > 0 && !loading.value && !error.value) {
    addToHistory(query)
  }
})
</script>

<style scoped>
.mobile-search-app {
  @apply flex flex-col h-full bg-gray-50;
  width: 375px;
  max-width: 375px;
  min-height: 812px;
  max-height: 812px;
  overflow: hidden;
}

.search-header {
  @apply sticky top-0 z-10 bg-white shadow-sm;
  @apply px-4 py-3;
  @apply border-b border-gray-200;
}

.search-content {
  @apply flex-1 overflow-hidden;
  @apply relative;
}

/* iOS 安全区域适配 */
@supports (padding: max(0px)) {
  .search-header {
    padding-top: max(12px, env(safe-area-inset-top));
  }
}

/* 移动端触摸优化 */
.mobile-search-app * {
  -webkit-tap-highlight-color: transparent;
  -webkit-touch-callout: none;
  -webkit-user-select: none;
  user-select: none;
}

/* 输入框除外 */
.mobile-search-app input {
  -webkit-user-select: text;
  user-select: text;
}

/* 滚动优化 */
.search-content {
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}
</style>