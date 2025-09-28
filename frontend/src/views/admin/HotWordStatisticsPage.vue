<template>
  <div class="hot-word-statistics-page">
    <!-- 页面容器布局 -->
    <StatisticsPageLayout
      :loading="pageState.loading"
      :error="pageState.error"
      @refresh="handleRefresh"
      @export="handleExport"
    >
      <!-- 筛选条件插槽 -->
      <template #filters>
        <HotWordFilter
          v-model="hotWordFilterData"
          :loading="pageState.loading"
          :search-space-options="searchSpaceOptions"
          :estimated-data-size="estimatedDataSize"
          @apply="handleFilterApply"
          @reset="handleFilterReset"
          @save-preset="handleSavePreset"
        />
      </template>

      <!-- 统计概览插槽 -->
      <template #overview>
        <div class="statistics-overview">
          <!-- 统计卡片网格 -->
          <div class="overview-grid">
            <!-- 总搜索次数 -->
            <div class="stat-card stat-card--primary">
              <div class="stat-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <div class="stat-content">
                <p class="stat-label">总搜索次数</p>
                <p class="stat-value">{{ formatNumber(summary.totalSearches) }}</p>
                <p class="stat-change stat-change--positive" v-if="summary.searchesChange > 0">
                  +{{ formatNumber(summary.searchesChange) }}
                </p>
              </div>
            </div>

            <!-- 热词数量 -->
            <div class="stat-card stat-card--secondary">
              <div class="stat-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
              </div>
              <div class="stat-content">
                <p class="stat-label">热词数量</p>
                <p class="stat-value">{{ formatNumber(summary.uniqueKeywords) }}</p>
                <p class="stat-change stat-change--positive" v-if="summary.keywordsChange > 0">
                  +{{ formatNumber(summary.keywordsChange) }}
                </p>
              </div>
            </div>

            <!-- 平均搜索频次 -->
            <div class="stat-card stat-card--accent">
              <div class="stat-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
              </div>
              <div class="stat-content">
                <p class="stat-label">平均搜索频次</p>
                <p class="stat-value">{{ summary.averageFrequency.toFixed(1) }}</p>
                <p class="stat-trend">{{ summary.frequencyTrend }}</p>
              </div>
            </div>

            <!-- 统计时间段 -->
            <div class="stat-card stat-card--info">
              <div class="stat-icon">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="stat-content">
                <p class="stat-label">统计时间段</p>
                <p class="stat-value">{{ summary.timePeriod }}</p>
                <p class="stat-description">{{ summary.timeDescription }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 主内容区域插槽 -->
      <template #main-content>
        <div class="main-content-grid">
          <!-- 词云图区域 -->
          <div class="wordcloud-section">
            <div class="section-header">
              <h2 class="section-title">热词云图</h2>
              <div class="section-controls">
                <!-- 主题选择器 -->
                <div class="control-group">
                  <label class="control-label">主题</label>
                  <select
                    v-model="wordCloudConfig.theme"
                    class="control-select"
                    @change="handleThemeChange"
                  >
                    <option value="light-green">淡绿色</option>
                    <option value="dark-green">深绿色</option>
                    <option value="blue">蓝色</option>
                    <option value="purple">紫色</option>
                    <option value="gradient">渐变色</option>
                  </select>
                </div>

                <!-- 显示数量选择器 -->
                <div class="control-group">
                  <label class="control-label">显示</label>
                  <select
                    v-model="wordCloudConfig.maxWords"
                    class="control-select"
                    @change="handleMaxWordsChange"
                  >
                    <option :value="30">30个词</option>
                    <option :value="50">50个词</option>
                    <option :value="100">100个词</option>
                    <option :value="200">200个词</option>
                  </select>
                </div>

                <!-- 布局选择器 -->
                <div class="control-group">
                  <label class="control-label">布局</label>
                  <select
                    v-model="wordCloudConfig.layout"
                    class="control-select"
                    @change="handleLayoutChange"
                  >
                    <option value="random">随机布局</option>
                    <option value="spiral">螺旋布局</option>
                    <option value="compact">紧凑布局</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- 词云图组件 -->
            <div class="wordcloud-container">
              <HotWordCloudChart
                ref="wordCloudRef"
                :words="wordCloudData"
                :theme="wordCloudConfig.theme"
                :options="wordCloudOptions"
                :responsive="true"
                :loading="pageState.loading"
                :error="pageState.error || undefined"
                @word-click="handleWordClick"
                @word-hover="handleWordHover"
                @render-start="handleRenderStart"
                @render-complete="handleRenderComplete"
                @render-error="handleRenderError"
                @download="handleWordCloudDownload"
              />
            </div>
          </div>

          <!-- 侧边栏区域 -->
          <div class="sidebar-section">
            <!-- TOP 10 热词排行 -->
            <div class="ranking-card">
              <div class="card-header">
                <h3 class="card-title">TOP 10 热词</h3>
                <button
                  @click="toggleRankingSort"
                  class="sort-button"
                  title="切换排序方式"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z"/>
                  </svg>
                </button>
              </div>
              <div class="ranking-list">
                <div
                  v-for="(item, index) in topWords"
                  :key="item.text"
                  class="ranking-item"
                  :class="{ 'ranking-item--active': selectedWord === item.text }"
                  @click="handleRankingItemClick(item)"
                >
                  <div class="ranking-badge" :class="getRankingBadgeClass(index)">
                    {{ index + 1 }}
                  </div>
                  <div class="ranking-content">
                    <span class="ranking-word">{{ item.text }}</span>
                    <div class="ranking-meta">
                      <span class="ranking-count">{{ formatNumber(item.weight) }}</span>
                      <span class="ranking-trend" :class="getTrendClass(item.trend)">
                        {{ getTrendIcon(item.trend) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 趋势分析卡片 -->
            <div class="trends-card">
              <div class="card-header">
                <h3 class="card-title">趋势分析</h3>
              </div>
              <div class="trends-content">
                <div class="trend-item">
                  <span class="trend-label">上升最快</span>
                  <span class="trend-value trend-value--rising">
                    {{ trendInfo.rising || '-' }}
                  </span>
                </div>
                <div class="trend-item">
                  <span class="trend-label">下降最快</span>
                  <span class="trend-value trend-value--falling">
                    {{ trendInfo.falling || '-' }}
                  </span>
                </div>
                <div class="trend-item">
                  <span class="trend-label">新热词</span>
                  <span class="trend-value trend-value--new">
                    {{ trendInfo.new || '-' }}
                  </span>
                </div>
                <div class="trend-item">
                  <span class="trend-label">热度波动</span>
                  <span class="trend-value">
                    {{ trendInfo.volatility || '-' }}
                  </span>
                </div>
              </div>
            </div>

            <!-- 实时状态卡片 -->
            <div class="status-card">
              <div class="card-header">
                <h3 class="card-title">实时状态</h3>
                <div class="status-indicator" :class="getStatusClass()">
                  <div class="status-dot"></div>
                  <span class="status-text">{{ getStatusText() }}</span>
                </div>
              </div>
              <div class="status-content">
                <div class="status-item">
                  <span class="status-label">数据更新</span>
                  <span class="status-value">{{ formatTime(lastUpdateTime) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-label">渲染时间</span>
                  <span class="status-value">{{ renderTime }}ms</span>
                </div>
                <div class="status-item">
                  <span class="status-label">缓存状态</span>
                  <span class="status-value">{{ cacheStatus }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 详细数据表格插槽 -->
      <template #data-table>
        <div class="data-table-section">
          <div class="table-header">
            <h2 class="table-title">热词详细数据</h2>
            <div class="table-controls">
              <!-- 搜索框 -->
              <div class="search-box">
                <input
                  v-model="searchKeyword"
                  type="text"
                  placeholder="搜索热词..."
                  class="search-input"
                >
                <svg class="search-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>

              <!-- 视图切换 -->
              <div class="view-toggle">
                <button
                  @click="tableViewMode = 'list'"
                  :class="{ 'active': tableViewMode === 'list' }"
                  class="view-button"
                >
                  列表
                </button>
                <button
                  @click="tableViewMode = 'grid'"
                  :class="{ 'active': tableViewMode === 'grid' }"
                  class="view-button"
                >
                  网格
                </button>
              </div>
            </div>
          </div>

          <!-- 数据表格 -->
          <div class="table-container" :class="`table-container--${tableViewMode}`">
            <div v-if="tableViewMode === 'list'" class="table-wrapper">
              <table class="data-table">
                <thead class="table-header-row">
                  <tr>
                    <th class="table-cell table-cell--header">排名</th>
                    <th class="table-cell table-cell--header">热词</th>
                    <th class="table-cell table-cell--header">搜索次数</th>
                    <th class="table-cell table-cell--header">占比</th>
                    <th class="table-cell table-cell--header">趋势</th>
                    <th class="table-cell table-cell--header">最后搜索时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(item, index) in paginatedStatistics"
                    :key="item.keyword"
                    class="table-row"
                    :class="{ 'table-row--selected': selectedWord === item.keyword }"
                    @click="handleTableRowClick(item)"
                  >
                    <td class="table-cell">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                    <td class="table-cell">
                      <div class="word-cell">
                        <span class="word-text">{{ item.keyword }}</span>
                        <span v-if="item.channels?.length" class="word-channels">
                          {{ item.channels.join(', ') }}
                        </span>
                      </div>
                    </td>
                    <td class="table-cell">{{ formatNumber(item.searchCount) }}</td>
                    <td class="table-cell">{{ getPercentage(item.searchCount) }}%</td>
                    <td class="table-cell">
                      <span class="trend-badge" :class="getTrendClass(item.trend)">
                        {{ getTrendLabel(item.trend) }}
                      </span>
                    </td>
                    <td class="table-cell">{{ formatDate(item.lastSearchTime) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- 网格视图 -->
            <div v-else class="grid-wrapper">
              <div
                v-for="(item, index) in paginatedStatistics"
                :key="item.keyword"
                class="grid-item"
                :class="{ 'grid-item--selected': selectedWord === item.keyword }"
                @click="handleTableRowClick(item)"
              >
                <div class="grid-item-header">
                  <span class="grid-rank">{{ (currentPage - 1) * pageSize + index + 1 }}</span>
                  <span class="trend-badge" :class="getTrendClass(item.trend)">
                    {{ getTrendIcon(item.trend) }}
                  </span>
                </div>
                <div class="grid-item-content">
                  <h4 class="grid-word">{{ item.keyword }}</h4>
                  <div class="grid-stats">
                    <div class="grid-stat">
                      <span class="grid-stat-label">搜索次数</span>
                      <span class="grid-stat-value">{{ formatNumber(item.searchCount) }}</span>
                    </div>
                    <div class="grid-stat">
                      <span class="grid-stat-label">占比</span>
                      <span class="grid-stat-value">{{ getPercentage(item.searchCount) }}%</span>
                    </div>
                  </div>
                  <div class="grid-meta">
                    <span class="grid-time">{{ formatDate(item.lastSearchTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页组件 -->
          <div v-if="totalPages > 1" class="pagination">
            <div class="pagination-info">
              显示 {{ (currentPage - 1) * pageSize + 1 }} 到 {{ Math.min(currentPage * pageSize, filteredStatistics.length) }}
              条，共 {{ filteredStatistics.length }} 条记录
            </div>
            <div class="pagination-controls">
              <button
                @click="currentPage = 1"
                :disabled="currentPage === 1"
                class="pagination-button"
              >
                首页
              </button>
              <button
                @click="currentPage--"
                :disabled="currentPage === 1"
                class="pagination-button"
              >
                上一页
              </button>
              <span class="pagination-current">{{ currentPage }} / {{ totalPages }}</span>
              <button
                @click="currentPage++"
                :disabled="currentPage === totalPages"
                class="pagination-button"
              >
                下一页
              </button>
              <button
                @click="currentPage = totalPages"
                :disabled="currentPage === totalPages"
                class="pagination-button"
              >
                末页
              </button>
            </div>
          </div>
        </div>
      </template>
    </StatisticsPageLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import StatisticsPageLayout from '@/components/statistics/StatisticsPageLayout.vue'
import HotWordFilter, { type HotWordFilterData } from '@/components/statistics/HotWordFilter.vue'
import HotWordCloudChart from '@/components/statistics/HotWordCloudChart.vue'
import type {
  HotWordItem,
  HotWordStatistics,
  FilterConfig,
  WordCloudOptions,
  StatisticsQueryParams
} from '@/types/statistics'
import { statisticsApi } from '@/api/statistics'

// ============= 响应式状态 =============

const pageState = reactive({
  loading: false,
  error: null as string | null,
  filter: {
    timeRange: {
      start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
      end: new Date()
    },
    searchCondition: '',
    hotWordLimit: 100,
    channels: []
  } as FilterConfig
})

const wordCloudConfig = reactive({
  theme: 'light-green' as string,
  maxWords: 100 as number,
  layout: 'random' as string
})

const statistics = ref<HotWordStatistics[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const tableViewMode = ref<'list' | 'grid'>('list')
const selectedWord = ref<string | null>(null)
const lastUpdateTime = ref<Date>(new Date())
const renderTime = ref(0)
const cacheStatus = ref('已缓存')
const rankingSortMode = ref<'count' | 'trend'>('count')

// ============= 计算属性 =============

const wordCloudData = computed((): HotWordItem[] => {
  return statistics.value
    .slice(0, wordCloudConfig.maxWords)
    .map(stat => ({
      text: stat.keyword,
      weight: stat.searchCount,
      trend: stat.trend,
      lastSearchTime: stat.lastSearchTime
    }))
})

const wordCloudOptions = computed((): Partial<WordCloudOptions> => ({
  gridSize: wordCloudConfig.layout === 'compact' ? 6 : 8,
  weightFactor: (size: number) => Math.pow(size, 2.3) / 600,
  fontFamily: 'Arial, Microsoft YaHei, sans-serif',
  rotateRatio: wordCloudConfig.layout === 'spiral' ? 0.8 : 0.5,
  rotationSteps: 4,
  backgroundColor: 'transparent',
  color: getWordCloudColorFunction()
}))

const summary = computed(() => {
  const totalSearches = statistics.value.reduce((sum, stat) => sum + stat.searchCount, 0)
  const uniqueKeywords = statistics.value.length
  const averageFrequency = uniqueKeywords > 0 ? totalSearches / uniqueKeywords : 0

  return {
    totalSearches,
    uniqueKeywords,
    averageFrequency,
    timePeriod: getTimePeriodLabel(),
    timeDescription: getTimeDescription(),
    searchesChange: Math.floor(totalSearches * 0.15), // 模拟变化
    keywordsChange: Math.floor(uniqueKeywords * 0.08),
    frequencyTrend: averageFrequency > 50 ? '高频' : '中频'
  }
})

const topWords = computed(() => {
  let sorted = [...statistics.value]

  if (rankingSortMode.value === 'trend') {
    sorted.sort((a, b) => {
      const trendOrder = { rising: 3, new: 2, stable: 1, falling: 0 }
      const aTrend = trendOrder[a.trend as keyof typeof trendOrder] || 0
      const bTrend = trendOrder[b.trend as keyof typeof trendOrder] || 0
      return bTrend - aTrend
    })
  }

  return sorted.slice(0, 10).map(stat => ({
    text: stat.keyword,
    weight: stat.searchCount,
    trend: stat.trend
  }))
})

const trendInfo = computed(() => {
  const risingWords = statistics.value.filter(s => s.trend === 'rising')
  const fallingWords = statistics.value.filter(s => s.trend === 'falling')
  const newWords = statistics.value.filter(s => s.trend === 'new')

  return {
    rising: risingWords[0]?.keyword,
    falling: fallingWords[0]?.keyword,
    new: newWords[0]?.keyword,
    volatility: calculateVolatility()
  }
})

const filteredStatistics = computed(() => {
  let filtered = statistics.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(stat =>
      stat.keyword.toLowerCase().includes(keyword)
    )
  }

  return filtered
})

const paginatedStatistics = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredStatistics.value.slice(start, end)
})

const totalPages = computed(() => Math.ceil(filteredStatistics.value.length / pageSize.value))

const searchSpaceOptions = computed(() => [
  { label: '全部渠道', value: 'all', description: '包含所有搜索渠道' },
  { label: 'Web端', value: 'web', description: '网页端搜索' },
  { label: '移动端', value: 'mobile', description: '手机App搜索' },
  { label: 'API', value: 'api', description: 'API接口搜索' },
  { label: '桌面端', value: 'desktop', description: '桌面应用搜索' }
])

const estimatedDataSize = computed(() => statistics.value.length)

// 适配器：转换pageState.filter为HotWordFilterData格式
const hotWordFilterData = computed<HotWordFilterData>({
  get(): HotWordFilterData {
    return {
      timeRange: {
        start: pageState.filter.timeRange.start,
        end: pageState.filter.timeRange.end,
        label: `${pageState.filter.timeRange.start.toLocaleDateString('zh-CN')} - ${pageState.filter.timeRange.end.toLocaleDateString('zh-CN')}`
      },
      searchCondition: {
        keywords: pageState.filter.searchCondition ? [pageState.filter.searchCondition] : [],
        userTypes: [],
        sortOrder: 'frequency_desc'
      },
      limitConfig: {
        limit: pageState.filter.hotWordLimit,
        displayMode: 'table'
      }
    }
  },
  set(value: HotWordFilterData) {
    if (value.timeRange) {
      pageState.filter.timeRange.start = value.timeRange.start
      pageState.filter.timeRange.end = value.timeRange.end
    }
    if (value.searchCondition) {
      pageState.filter.searchCondition = value.searchCondition.keywords?.[0] || ''
    }
    if (value.limitConfig) {
      pageState.filter.hotWordLimit = value.limitConfig.limit
    }
  }
})

// ============= 组件引用 =============

const wordCloudRef = ref()

// ============= 方法 =============

const loadStatistics = async () => {
  pageState.loading = true
  pageState.error = null

  try {
    const startTime = Date.now()

    const response = await statisticsApi.getHotWordStatistics({
      startTime: pageState.filter.timeRange.start.toISOString(),
      endTime: pageState.filter.timeRange.end.toISOString(),
      searchCondition: pageState.filter.searchCondition,
      limit: pageState.filter.hotWordLimit,
      channels: pageState.filter.channels
    })

    statistics.value = response.data
    lastUpdateTime.value = new Date()
    renderTime.value = Date.now() - startTime

  } catch (err) {
    pageState.error = err instanceof Error ? err.message : '加载数据失败'
    console.error('Failed to load statistics:', err)
  } finally {
    pageState.loading = false
  }
}

const handleRefresh = async () => {
  await loadStatistics()
}

const handleExport = async () => {
  try {
    const blob = await statisticsApi.exportStatisticsReport({
      startTime: pageState.filter.timeRange.start.toISOString(),
      endTime: pageState.filter.timeRange.end.toISOString(),
      searchCondition: pageState.filter.searchCondition,
      limit: pageState.filter.hotWordLimit,
      channels: pageState.filter.channels
    })

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `hot-word-statistics-${Date.now()}.txt`
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    console.error('Failed to export data:', err)
  }
}

const handleFilterApply = async (filters: any, advanced: any) => {
  currentPage.value = 1
  await loadStatistics()
}

const handleFilterReset = () => {
  currentPage.value = 1
  selectedWord.value = null
}

const handleSavePreset = (filters: any, advanced: any) => {
  // 实现保存预设逻辑
  console.log('Save preset:', filters, advanced)
}

const handleThemeChange = () => {
  nextTick(() => {
    if (wordCloudRef.value?.refresh) {
      wordCloudRef.value.refresh()
    }
  })
}

const handleMaxWordsChange = () => {
  handleThemeChange()
}

const handleLayoutChange = () => {
  handleThemeChange()
}

const handleWordClick = (word: HotWordItem, event: Event) => {
  selectedWord.value = word.text
  searchKeyword.value = word.text
}

const handleWordHover = (word: HotWordItem, event: Event) => {
  // 悬停效果
}

const handleRenderStart = () => {
  console.log('Word cloud rendering started')
}

const handleRenderComplete = () => {
  console.log('Word cloud rendering completed')
}

const handleRenderError = (errorMessage: string) => {
  console.error('Word cloud rendering error:', errorMessage)
}

const handleWordCloudDownload = (canvas: HTMLCanvasElement) => {
  const link = document.createElement('a')
  link.download = `hot-word-cloud-${Date.now()}.png`
  link.href = canvas.toDataURL()
  link.click()
}

const handleRankingItemClick = (item: any) => {
  selectedWord.value = item.text
  searchKeyword.value = item.text
}

const handleTableRowClick = (item: HotWordStatistics) => {
  selectedWord.value = item.keyword
}

const toggleRankingSort = () => {
  rankingSortMode.value = rankingSortMode.value === 'count' ? 'trend' : 'count'
}

// ============= 工具方法 =============

const formatNumber = (num: number): string => {
  return num.toLocaleString()
}

const formatTime = (date: Date): string => {
  return date.toLocaleTimeString('zh-CN')
}

const formatDate = (dateString?: string): string => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const getPercentage = (count: number): string => {
  const total = summary.value.totalSearches
  return total > 0 ? ((count / total) * 100).toFixed(2) : '0.00'
}

const getTimePeriodLabel = (): string => {
  const start = pageState.filter.timeRange.start.toLocaleDateString('zh-CN')
  const end = pageState.filter.timeRange.end.toLocaleDateString('zh-CN')
  return `${start} - ${end}`
}

const getTimeDescription = (): string => {
  const days = Math.ceil((pageState.filter.timeRange.end.getTime() - pageState.filter.timeRange.start.getTime()) / (1000 * 60 * 60 * 24))
  return `${days}天数据`
}

const getRankingBadgeClass = (index: number): string => {
  if (index === 0) return 'ranking-badge--gold'
  if (index === 1) return 'ranking-badge--silver'
  if (index === 2) return 'ranking-badge--bronze'
  return 'ranking-badge--default'
}

const getTrendClass = (trend?: string): string => {
  switch (trend) {
    case 'rising': return 'trend--rising'
    case 'falling': return 'trend--falling'
    case 'new': return 'trend--new'
    default: return 'trend--stable'
  }
}

const getTrendLabel = (trend?: string): string => {
  switch (trend) {
    case 'rising': return '上升'
    case 'falling': return '下降'
    case 'new': return '新词'
    default: return '稳定'
  }
}

const getTrendIcon = (trend?: string): string => {
  switch (trend) {
    case 'rising': return '↗'
    case 'falling': return '↘'
    case 'new': return '✨'
    default: return '→'
  }
}

const getStatusClass = (): string => {
  if (pageState.loading) return 'status--loading'
  if (pageState.error) return 'status--error'
  return 'status--success'
}

const getStatusText = (): string => {
  if (pageState.loading) return '加载中'
  if (pageState.error) return '错误'
  return '正常'
}

const calculateVolatility = (): string => {
  const risingCount = statistics.value.filter(s => s.trend === 'rising').length
  const fallingCount = statistics.value.filter(s => s.trend === 'falling').length
  const totalCount = statistics.value.length

  if (totalCount === 0) return '无数据'

  const volatilityRatio = (risingCount + fallingCount) / totalCount

  if (volatilityRatio > 0.3) return '高波动'
  if (volatilityRatio > 0.15) return '中波动'
  return '低波动'
}

const getWordCloudColorFunction = () => {
  const themes = {
    'light-green': ['#10b981', '#34d399', '#6ee7b7', '#a7f3d0'],
    'dark-green': ['#047857', '#065f46', '#064e3b', '#022c22'],
    'blue': ['#3b82f6', '#60a5fa', '#93c5fd', '#dbeafe'],
    'purple': ['#8b5cf6', '#a78bfa', '#c4b5fd', '#e9d5ff'],
    'gradient': ['#f59e0b', '#f97316', '#ef4444', '#ec4899']
  }

  const colors = themes[wordCloudConfig.theme as keyof typeof themes] || themes['light-green']

  return (word: string, weight: number) => {
    const index = Math.floor((weight / 100) * colors.length)
    return colors[Math.min(index, colors.length - 1)]
  }
}

// ============= 监听器 =============

watch(searchKeyword, () => {
  currentPage.value = 1
})

watch([() => wordCloudConfig.theme, () => wordCloudConfig.maxWords, () => wordCloudConfig.layout], () => {
  nextTick(() => {
    if (wordCloudRef.value?.refresh) {
      wordCloudRef.value.refresh()
    }
  })
})

// ============= 生命周期 =============

onMounted(async () => {
  await loadStatistics()
})
</script>

<style scoped>
@import '@/styles/statistics.css';

.hot-word-statistics-page {
  @apply min-h-screen bg-gray-50;
}

/* 统计概览样式 */
.statistics-overview {
  @apply mb-8;
}

.overview-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6;
}

.stat-card {
  @apply bg-white rounded-lg shadow-sm border p-6 hover:shadow-md transition-shadow duration-200;
}

.stat-card--primary {
  @apply border-emerald-200 hover:border-emerald-300;
}

.stat-card--secondary {
  @apply border-blue-200 hover:border-blue-300;
}

.stat-card--accent {
  @apply border-purple-200 hover:border-purple-300;
}

.stat-card--info {
  @apply border-yellow-200 hover:border-yellow-300;
}

.stat-icon {
  @apply w-8 h-8 mb-4;
}

.stat-card--primary .stat-icon {
  @apply text-emerald-600;
}

.stat-card--secondary .stat-icon {
  @apply text-blue-600;
}

.stat-card--accent .stat-icon {
  @apply text-purple-600;
}

.stat-card--info .stat-icon {
  @apply text-yellow-600;
}

.stat-content {
  @apply space-y-1;
}

.stat-label {
  @apply text-sm font-medium text-gray-500;
}

.stat-value {
  @apply text-2xl font-bold text-gray-900;
}

.stat-change {
  @apply text-xs font-medium;
}

.stat-change--positive {
  @apply text-green-600;
}

.stat-trend {
  @apply text-sm text-gray-600;
}

.stat-description {
  @apply text-xs text-gray-500;
}

/* 主内容区域样式 */
.main-content-grid {
  @apply grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8;
}

.wordcloud-section {
  @apply lg:col-span-2 bg-white rounded-lg shadow-sm border p-6;
}

.sidebar-section {
  @apply space-y-6;
}

.section-header {
  @apply flex items-center justify-between mb-6;
}

.section-title {
  @apply text-lg font-semibold text-gray-900;
}

.section-controls {
  @apply flex items-center space-x-4;
}

.control-group {
  @apply flex items-center space-x-2;
}

.control-label {
  @apply text-sm text-gray-500;
}

.control-select {
  @apply text-sm border-gray-300 rounded-md focus:ring-emerald-500 focus:border-emerald-500;
}

.wordcloud-container {
  @apply h-96 relative;
}

/* 排行榜样式 */
.ranking-card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.card-header {
  @apply flex items-center justify-between mb-4;
}

.card-title {
  @apply text-lg font-semibold text-gray-900;
}

.sort-button {
  @apply p-1 text-gray-400 hover:text-gray-600 transition-colors;
}

.ranking-list {
  @apply space-y-3;
}

.ranking-item {
  @apply flex items-center space-x-3 p-3 rounded-md hover:bg-gray-50 transition-colors cursor-pointer;
}

.ranking-item--active {
  @apply bg-emerald-50 hover:bg-emerald-100;
}

.ranking-badge {
  @apply w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white;
}

.ranking-badge--gold {
  @apply bg-yellow-500;
}

.ranking-badge--silver {
  @apply bg-gray-400;
}

.ranking-badge--bronze {
  @apply bg-yellow-600;
}

.ranking-badge--default {
  @apply bg-gray-500;
}

.ranking-content {
  @apply flex-1 min-w-0;
}

.ranking-word {
  @apply block font-medium text-gray-900 truncate;
}

.ranking-meta {
  @apply flex items-center justify-between mt-1;
}

.ranking-count {
  @apply text-sm font-semibold text-gray-700;
}

.ranking-trend {
  @apply text-sm;
}

/* 趋势分析样式 */
.trends-card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.trends-content {
  @apply space-y-4;
}

.trend-item {
  @apply flex items-center justify-between;
}

.trend-label {
  @apply text-sm text-gray-500;
}

.trend-value {
  @apply text-sm font-medium;
}

.trend-value--rising {
  @apply text-green-600;
}

.trend-value--falling {
  @apply text-red-600;
}

.trend-value--new {
  @apply text-blue-600;
}

/* 状态卡片样式 */
.status-card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.status-indicator {
  @apply flex items-center space-x-2;
}

.status-dot {
  @apply w-2 h-2 rounded-full;
}

.status--success .status-dot {
  @apply bg-green-500;
}

.status--loading .status-dot {
  @apply bg-yellow-500 animate-pulse;
}

.status--error .status-dot {
  @apply bg-red-500;
}

.status-text {
  @apply text-xs font-medium text-gray-600;
}

.status-content {
  @apply space-y-3 mt-4;
}

.status-item {
  @apply flex items-center justify-between;
}

.status-label {
  @apply text-sm text-gray-500;
}

.status-value {
  @apply text-sm font-medium text-gray-700;
}

/* 数据表格样式 */
.data-table-section {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.table-header {
  @apply flex items-center justify-between mb-6;
}

.table-title {
  @apply text-lg font-semibold text-gray-900;
}

.table-controls {
  @apply flex items-center space-x-3;
}

.search-box {
  @apply relative;
}

.search-input {
  @apply pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm;
}

.search-icon {
  @apply absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400;
}

.view-toggle {
  @apply flex border border-gray-300 rounded-md overflow-hidden;
}

.view-button {
  @apply px-3 py-2 text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors;
}

.view-button.active {
  @apply bg-emerald-50 text-emerald-700 border-emerald-200;
}

.table-container {
  @apply mb-6;
}

.table-wrapper {
  @apply overflow-x-auto;
}

.data-table {
  @apply min-w-full divide-y divide-gray-200;
}

.table-header-row {
  @apply bg-gray-50;
}

.table-cell {
  @apply px-6 py-4 text-sm;
}

.table-cell--header {
  @apply font-medium text-gray-500 uppercase tracking-wider;
}

.table-row {
  @apply hover:bg-gray-50 cursor-pointer transition-colors;
}

.table-row--selected {
  @apply bg-emerald-50 hover:bg-emerald-100;
}

.word-cell {
  @apply space-y-1;
}

.word-text {
  @apply font-medium text-gray-900 block;
}

.word-channels {
  @apply text-xs text-gray-500;
}

.trend-badge {
  @apply inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium;
}

.trend--rising {
  @apply bg-green-100 text-green-800;
}

.trend--falling {
  @apply bg-red-100 text-red-800;
}

.trend--new {
  @apply bg-blue-100 text-blue-800;
}

.trend--stable {
  @apply bg-gray-100 text-gray-800;
}

/* 网格视图样式 */
.grid-wrapper {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4;
}

.grid-item {
  @apply bg-gray-50 rounded-lg p-4 hover:bg-gray-100 transition-colors cursor-pointer;
}

.grid-item--selected {
  @apply bg-emerald-50 hover:bg-emerald-100;
}

.grid-item-header {
  @apply flex items-center justify-between mb-2;
}

.grid-rank {
  @apply text-sm font-bold text-gray-600;
}

.grid-item-content {
  @apply space-y-2;
}

.grid-word {
  @apply text-lg font-semibold text-gray-900 truncate;
}

.grid-stats {
  @apply grid grid-cols-2 gap-2;
}

.grid-stat {
  @apply text-center;
}

.grid-stat-label {
  @apply block text-xs text-gray-500;
}

.grid-stat-value {
  @apply block text-sm font-semibold text-gray-900;
}

.grid-meta {
  @apply pt-2 border-t border-gray-200;
}

.grid-time {
  @apply text-xs text-gray-500;
}

/* 分页样式 */
.pagination {
  @apply flex items-center justify-between pt-6 border-t border-gray-200;
}

.pagination-info {
  @apply text-sm text-gray-700;
}

.pagination-controls {
  @apply flex items-center space-x-2;
}

.pagination-button {
  @apply px-3 py-1 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors;
}

.pagination-current {
  @apply px-3 py-1 text-sm text-gray-700;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content-grid {
    @apply grid-cols-1;
  }

  .overview-grid {
    @apply grid-cols-1 md:grid-cols-2;
  }

  .section-controls {
    @apply flex-wrap gap-2;
  }
}

@media (max-width: 640px) {
  .stat-card {
    @apply p-4;
  }

  .section-header {
    @apply flex-col items-start space-y-4;
  }

  .table-controls {
    @apply flex-col space-y-2 space-x-0 items-stretch;
  }

  .pagination {
    @apply flex-col space-y-4 items-start;
  }

  .pagination-controls {
    @apply w-full justify-center;
  }

  .grid-wrapper {
    @apply grid-cols-1;
  }
}
</style>