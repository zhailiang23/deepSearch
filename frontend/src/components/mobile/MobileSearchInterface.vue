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

          <!-- 详情页面 -->
          <div v-if="showDetail && selectedResult" class="detail-view">
            <!-- 详情页面头部 -->
            <div class="detail-header">
              <button @click="backToResults" class="back-btn">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                </svg>
              </button>
              <h2 class="detail-title">详细信息</h2>
              <div class="header-spacer"></div>
            </div>

            <!-- 详情内容 -->
            <div class="detail-content">
              <!-- 标题和类型 -->
              <div class="detail-main-info">
                <h1 class="detail-name">{{ selectedResult.title }}</h1>
                <span class="detail-type-tag">{{ selectedResult.source?.type }}</span>
              </div>

              <!-- 描述 -->
              <div class="detail-section">
                <h3 class="detail-section-title">描述</h3>
                <p class="detail-description">{{ selectedResult.source?.descript || selectedResult.summary }}</p>
              </div>

              <!-- 根据类型显示不同字段 -->
              <!-- 功能类型 -->
              <div v-if="selectedResult.source?.type === '功能'" class="detail-type-specific">
                <div v-if="selectedResult.source?.category" class="detail-section">
                  <h3 class="detail-section-title">分类</h3>
                  <p class="detail-value">{{ selectedResult.source.category }}</p>
                </div>
                <div v-if="selectedResult.source?.permission" class="detail-section">
                  <h3 class="detail-section-title">权限级别</h3>
                  <p class="detail-value">{{ selectedResult.source.permission }}</p>
                </div>
                <div v-if="selectedResult.source?.frequency" class="detail-section">
                  <h3 class="detail-section-title">使用频率</h3>
                  <p class="detail-value">{{ selectedResult.source.frequency }}%</p>
                </div>
              </div>

              <!-- 活动类型 -->
              <div v-else-if="selectedResult.source?.type === '活动'" class="detail-type-specific">
                <div v-if="selectedResult.source?.startDate" class="detail-section">
                  <h3 class="detail-section-title">开始时间</h3>
                  <p class="detail-value">{{ selectedResult.source.startDate }}</p>
                </div>
                <div v-if="selectedResult.source?.endDate" class="detail-section">
                  <h3 class="detail-section-title">结束时间</h3>
                  <p class="detail-value">{{ selectedResult.source.endDate }}</p>
                </div>
                <div v-if="selectedResult.source?.status" class="detail-section">
                  <h3 class="detail-section-title">状态</h3>
                  <span class="detail-status" :class="{
                    'status-active': selectedResult.source.status === '进行中',
                    'status-ended': selectedResult.source.status === '已结束',
                    'status-upcoming': selectedResult.source.status === '即将开始'
                  }">{{ selectedResult.source.status }}</span>
                </div>
              </div>

              <!-- 产品类型 -->
              <div v-else-if="selectedResult.source?.type === '产品'" class="detail-type-specific">
                <div v-if="selectedResult.source?.riskLevel" class="detail-section">
                  <h3 class="detail-section-title">风险级别</h3>
                  <p class="detail-value">{{ selectedResult.source.riskLevel }}</p>
                </div>
                <div v-if="selectedResult.source?.minAmount" class="detail-section">
                  <h3 class="detail-section-title">最低投资金额</h3>
                  <p class="detail-value">¥{{ selectedResult.source.minAmount.toLocaleString() }}</p>
                </div>
                <div v-if="selectedResult.source?.expectedReturn" class="detail-section">
                  <h3 class="detail-section-title">预期收益率</h3>
                  <p class="detail-value">{{ selectedResult.source.expectedReturn }}</p>
                </div>
              </div>

              <!-- 资讯类型 -->
              <div v-else-if="selectedResult.source?.type === '资讯'" class="detail-type-specific">
                <div v-if="selectedResult.source?.publishTime" class="detail-section">
                  <h3 class="detail-section-title">发布时间</h3>
                  <p class="detail-value">{{ selectedResult.source.publishTime }}</p>
                </div>
                <div v-if="selectedResult.source?.author" class="detail-section">
                  <h3 class="detail-section-title">作者</h3>
                  <p class="detail-value">{{ selectedResult.source.author }}</p>
                </div>
                <div v-if="selectedResult.source?.readCount" class="detail-section">
                  <h3 class="detail-section-title">阅读量</h3>
                  <p class="detail-value">{{ selectedResult.source.readCount.toLocaleString() }} 次</p>
                </div>
              </div>

              <!-- 其他信息 -->
              <div class="detail-section">
                <h3 class="detail-section-title">其他信息</h3>
                <div class="detail-meta">
                  <div v-if="selectedResult.score" class="meta-item">
                    <span class="meta-label">相关性分数:</span>
                    <span class="meta-value">{{ selectedResult.score.toFixed(2) }}</span>
                  </div>
                  <div v-if="selectedResult.index" class="meta-item">
                    <span class="meta-label">索引:</span>
                    <span class="meta-value">{{ selectedResult.index }}</span>
                  </div>
                  <div v-if="selectedResult.id" class="meta-item">
                    <span class="meta-label">ID:</span>
                    <span class="meta-value">{{ selectedResult.id }}</span>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="detail-actions">
                <button v-if="selectedResult.source?.link" class="action-btn primary">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"></path>
                  </svg>
                  访问链接
                </button>
                <button @click="backToResults" class="action-btn secondary">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                  返回搜索
                </button>
              </div>
            </div>
          </div>

          <!-- 搜索栏 (仅在非详情页面显示) -->
          <div v-if="!showDetail" class="search-container-wrapper">
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

              <!-- Tab导航 (仅在分组模式下显示) -->
              <div v-if="groupByType" class="tab-navigation">
                <div class="tab-list">
                  <button
                    v-for="tab in availableTabs"
                    :key="tab"
                    @click="switchTab(tab)"
                    :class="[
                      'tab-item',
                      { 'tab-active': activeTab === tab }
                    ]"
                  >
                    <span class="tab-name">{{ getTabDisplayName(tab) }}</span>
                    <span class="tab-count">{{ getTabCount(tab) }}</span>
                  </button>
                </div>
              </div>

              <!-- 结果内容 -->
              <div class="tab-content">
                <div
                  v-for="result in groupByType ? currentTabResults : results"
                  :key="result.id"
                  class="result-item"
                  @click="viewResult(result)"
                >
                  <div class="result-header">
                    <div class="result-title-wrapper">
                      <h3 class="result-title" v-html="highlightText(result.title)"></h3>
                      <span v-if="result.source?.type && !groupByType" class="result-type-tag">{{ result.source.type }}</span>
                    </div>
                    <span v-if="showScore" class="result-score">{{ result.score?.toFixed(2) }}</span>
                  </div>
                  <p class="result-summary" v-html="highlightText(result.source?.descript || result.summary)"></p>
                </div>
              </div>

              <!-- 加载更多 -->
              <div v-if="hasMore && (!groupByType || activeTab === 'all')" class="load-more-container">
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
              

                
                  <!-- 最近搜索 -->
                  <div v-if="searchHistory.length > 0" class="search-section">
                    <h3 class="section-title">最近搜索</h3>
                    <div class="tag-list">
                      <button
                        v-for="item in searchHistory.slice(0, 8)"
                        :key="item.timestamp"
                        class="search-tag recent-tag"
                        @click="selectSuggestion(item.query)"
                      >
                        {{ item.query }}
                      </button>
                    </div>
                  </div>

                  <!-- 大家都在搜 -->
                  <div class="search-section">
                    <h3 class="section-title">大家都在搜</h3>
                    <div class="tag-list">
                      <button
                        v-for="(item, index) in hotSearches"
                        :key="index"
                        class="search-tag hot-tag"
                        :class="{ 'hot-rank': index < 3 }"
                        @click="selectSuggestion(item.query)"
                      >
                        <span class="rank-number">{{ index + 1 }}</span>
                        {{ item.query }}
                        <span class="hot-icon" v-if="item.isHot">🔥</span>
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

// 热门搜索数据
const hotSearches = ref([
  { query: '中国银行', isHot: true },
  { query: '工商银行', isHot: true },
  { query: '建设银行', isHot: false },
  { query: '农业银行', isHot: true },
  { query: '交通银行', isHot: false },
  { query: '招商银行', isHot: true },
  { query: '浦发银行', isHot: false },
  { query: '民生银行', isHot: false },
  { query: '兴业银行', isHot: false },
  { query: '平安银行', isHot: true }
])

// 详情页面状态
const showDetail = ref(false)
const selectedResult = ref<SearchResult | null>(null)

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
const groupByType = computed(() => store.config.resultDisplay.groupByType)
const debounceMs = computed(() => store.config.searchBehavior.debounceMs)
const autoSearch = computed(() => store.config.searchBehavior.autoSearch)
const minQueryLength = computed(() => store.config.searchBehavior.minQueryLength)
const highlightMatch = computed(() => store.config.searchBehavior.highlightMatch)

// 分组相关状态
const activeTab = ref('all')

// 按类型分组的结果
const groupedResults = computed(() => {
  if (!groupByType.value || results.value.length === 0) {
    return { all: results.value }
  }

  const groups: Record<string, any[]> = { all: results.value }

  results.value.forEach(result => {
    const type = result.source?.type || '其他'
    if (!groups[type]) {
      groups[type] = []
    }
    groups[type].push(result)
  })

  return groups
})

// 可用的分组标签
const availableTabs = computed(() => {
  const tabs = Object.keys(groupedResults.value).filter(key => key !== 'all')
  return ['all', ...tabs.sort()]
})

// 当前活跃标签的结果
const currentTabResults = computed(() => {
  return groupedResults.value[activeTab.value] || []
})

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
  selectedResult.value = result
  showDetail.value = true
}

const backToResults = () => {
  showDetail.value = false
  selectedResult.value = null
}

const highlightText = (text: string): string => {
  // 如果没有搜索词或者高亮匹配功能被关闭，直接返回原文本
  if (!searchQuery.value || !highlightMatch.value) return text

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

// 切换tab
const switchTab = (tabName: string) => {
  activeTab.value = tabName
}

// 获取tab显示名称
const getTabDisplayName = (tabName: string) => {
  if (tabName === 'all') return '全部'
  return tabName
}

// 获取tab结果数量
const getTabCount = (tabName: string) => {
  return groupedResults.value[tabName]?.length || 0
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

// 监听分组模式变化
watch(() => groupByType.value, (newValue) => {
  // 当开启分组模式时，默认显示"全部"tab
  if (newValue) {
    activeTab.value = 'all'
  }
})

// 监听搜索结果变化，确保activeTab存在
watch(() => availableTabs.value, (newTabs) => {
  if (groupByType.value && !newTabs.includes(activeTab.value)) {
    activeTab.value = newTabs[0] || 'all'
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

.search-container-wrapper {
  @apply flex-1 flex flex-col;
  min-height: 0; /* 确保flex子容器可以正确计算高度 */
  height: 100%; /* 占满可用空间 */
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
  height: 100%; /* 占满可用高度 */
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
  @apply flex flex-col;
}

/* Tab导航样式 */
.tab-navigation {
  @apply border-b border-gray-200 bg-white sticky top-0 z-10;
}

.tab-list {
  @apply flex overflow-x-auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.tab-list::-webkit-scrollbar {
  display: none;
}

.tab-item {
  @apply flex items-center gap-2 px-4 py-3 text-sm font-medium whitespace-nowrap border-b-2 border-transparent hover:text-emerald-600 hover:border-emerald-200 transition-colors flex-shrink-0;
}

.tab-item.tab-active {
  @apply text-emerald-600 border-emerald-600;
}

.tab-name {
  @apply text-gray-700;
}

.tab-item.tab-active .tab-name {
  @apply text-emerald-600;
}

.tab-count {
  @apply bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-full min-w-[20px] text-center;
}

.tab-item.tab-active .tab-count {
  @apply bg-emerald-100 text-emerald-700;
}

/* Tab内容样式 */
.tab-content {
  @apply divide-y divide-gray-200;
}

.results-header {
  @apply flex items-center justify-between px-4 py-3 bg-gray-50 text-sm text-gray-600;
}

.result-item {
  @apply px-3 py-2 hover:bg-gray-50 cursor-pointer transition-colors;
}

.result-header {
  @apply flex items-start justify-between gap-2 mb-1;
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
  @apply text-sm text-gray-700 line-clamp-2;
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
  @apply flex flex-col justify-start py-6 px-6 h-full min-h-0;
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

.welcome-header {
  @apply flex items-center gap-4 mb-6;
}

.welcome-text {
  @apply flex-1 text-left;
}

.welcome-icon {
  @apply text-3xl flex-shrink-0;
  margin-bottom: 0 !important;
}

.welcome-title {
  @apply text-lg font-semibold text-gray-900 mb-1;
}

.welcome-message {
  @apply text-sm text-gray-600 mb-0 text-left;
}

/* 搜索建议样式 */
.search-suggestions {
  @apply space-y-4 px-3 py-3;
}

.search-section {
  @apply space-y-2;
}

.section-title {
  @apply text-base font-medium text-gray-900 mb-2;
}

.tag-list {
  @apply flex flex-wrap gap-2;
}

.search-tag {
  @apply inline-flex items-center gap-1 px-3 py-2 rounded-full text-sm font-medium transition-all duration-200;
  @apply bg-gray-100 text-gray-700 hover:bg-emerald-100 hover:text-emerald-700;
}

.recent-tag {
  @apply bg-blue-50 text-blue-700 hover:bg-blue-100;
}

.hot-tag {
  @apply bg-emerald-50 text-emerald-700 hover:bg-emerald-100;
}

.hot-rank {
  @apply bg-gradient-to-r from-red-500 to-orange-500 text-white hover:from-red-600 hover:to-orange-600;
}

.rank-number {
  @apply inline-flex items-center justify-center w-4 h-4 text-xs font-bold rounded-full;
}

.hot-rank .rank-number {
  @apply bg-white bg-opacity-20;
}

.hot-tag:not(.hot-rank) .rank-number {
  @apply bg-emerald-700 text-white;
}

.hot-icon {
  @apply text-xs;
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
  @apply w-32 h-1 bg-gray-900 rounded-full mx-auto mb-1 mt-0;
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

/* 搜索结果点击效果 */
.result-item {
  @apply cursor-pointer transition-all duration-200;
}

.result-item:hover {
  @apply bg-emerald-50 transform scale-[1.01] shadow-md;
}

.result-item:active {
  @apply transform scale-[0.99];
}

/* 详情页面样式 */
.detail-view {
  @apply h-full flex flex-col bg-white;
}

.detail-header {
  @apply flex items-center p-4 border-b border-gray-200 bg-emerald-50 sticky top-0 z-20;
}

.back-btn {
  @apply p-2 rounded-full hover:bg-emerald-100 transition-colors text-emerald-700;
}

.detail-title {
  @apply flex-1 text-center text-lg font-semibold text-emerald-800;
}

.header-spacer {
  @apply w-10;
}

.detail-content {
  @apply flex-1 overflow-y-auto p-4 space-y-6;
  min-height: 0;
}

.detail-main-info {
  @apply space-y-3;
}

.detail-name {
  @apply text-xl font-bold text-gray-900 leading-tight;
}

.detail-type-tag {
  @apply inline-block px-3 py-1 text-sm font-medium bg-emerald-100 text-emerald-700 rounded-full;
}

.detail-section {
  @apply space-y-2;
}

.detail-section-title {
  @apply text-sm font-semibold text-emerald-700 uppercase tracking-wide;
}

.detail-description {
  @apply text-gray-700 leading-relaxed;
}

.detail-value {
  @apply text-gray-900 font-medium;
}

.detail-status {
  @apply inline-block px-2 py-1 text-xs font-medium rounded-full;
}

.status-active {
  @apply bg-green-100 text-green-800;
}

.status-ended {
  @apply bg-gray-100 text-gray-800;
}

.status-upcoming {
  @apply bg-blue-100 text-blue-800;
}

.detail-type-specific {
  @apply space-y-4 p-4 bg-gray-50 rounded-lg;
}

.detail-meta {
  @apply space-y-2;
}

.meta-item {
  @apply flex justify-between items-center text-sm;
}

.meta-label {
  @apply text-gray-600;
}

.meta-value {
  @apply text-gray-900 font-medium;
}

.detail-actions {
  @apply flex gap-3 pt-4 border-t border-gray-200;
}

.action-btn {
  @apply flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg font-medium transition-all duration-200;
}

.action-btn.primary {
  @apply bg-emerald-600 text-white hover:bg-emerald-700 active:bg-emerald-800;
}

.action-btn.secondary {
  @apply bg-gray-100 text-gray-700 hover:bg-gray-200 active:bg-gray-300;
}
</style>