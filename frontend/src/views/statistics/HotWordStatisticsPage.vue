<template>
  <div class="hot-word-statistics-page min-h-screen bg-gray-50">
    <!-- 页面头部 -->
    <div class="bg-white shadow-sm border-b">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="py-6">
          <div class="md:flex md:items-center md:justify-between">
            <div class="flex-1 min-w-0">
              <h1 class="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
                热词统计分析
              </h1>
              <p class="mt-1 text-sm text-gray-500">
                基于搜索日志的热词分析和可视化展示
              </p>
            </div>
            <div class="mt-4 flex md:mt-0 md:ml-4 space-x-3">
              <button
                @click="refreshData"
                :disabled="loading"
                class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 disabled:opacity-50"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                刷新数据
              </button>
              <button
                @click="exportData"
                :disabled="loading || !statistics.length"
                class="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 disabled:opacity-50"
              >
                <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                导出报告
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 筛选条件 -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mb-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">筛选条件</h2>

        <HotWordFilter
          v-model:timeRange="filterConfig.timeRange"
          v-model:searchCondition="filterConfig.searchCondition"
          v-model:hotWordLimit="filterConfig.hotWordLimit"
          v-model:channels="filterConfig.channels"
          @filter-change="handleFilterChange"
          :loading="loading"
        />
      </div>

      <!-- 统计概览 -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="bg-white rounded-lg shadow-sm border p-6">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 bg-emerald-100 rounded-md flex items-center justify-center">
                <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">总搜索次数</p>
              <p class="text-2xl font-semibold text-gray-900">{{ summary.totalSearches.toLocaleString() }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow-sm border p-6">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 bg-blue-100 rounded-md flex items-center justify-center">
                <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">热词数量</p>
              <p class="text-2xl font-semibold text-gray-900">{{ summary.uniqueKeywords }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow-sm border p-6">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 bg-purple-100 rounded-md flex items-center justify-center">
                <svg class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">平均搜索频次</p>
              <p class="text-2xl font-semibold text-gray-900">{{ summary.averageFrequency.toFixed(1) }}</p>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-lg shadow-sm border p-6">
          <div class="flex items-center">
            <div class="flex-shrink-0">
              <div class="w-8 h-8 bg-yellow-100 rounded-md flex items-center justify-center">
                <svg class="w-5 h-5 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-500">统计时间段</p>
              <p class="text-2xl font-semibold text-gray-900">{{ summary.timePeriod }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- 词云图 -->
        <div class="lg:col-span-2">
          <div class="bg-white rounded-lg shadow-sm border p-6">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-lg font-semibold text-gray-900">热词云图</h2>
              <div class="flex items-center space-x-4">
                <!-- 主题切换 -->
                <div class="flex items-center space-x-2">
                  <label class="text-sm text-gray-500">主题:</label>
                  <select
                    v-model="wordCloudConfig.theme"
                    class="text-sm border-gray-300 rounded-md focus:ring-emerald-500 focus:border-emerald-500"
                  >
                    <option value="light-green">淡绿色</option>
                    <option value="dark-green">深绿色</option>
                    <option value="blue">蓝色</option>
                    <option value="purple">紫色</option>
                  </select>
                </div>

                <!-- 显示数量 -->
                <div class="flex items-center space-x-2">
                  <label class="text-sm text-gray-500">显示:</label>
                  <select
                    v-model="wordCloudConfig.maxWords"
                    class="text-sm border-gray-300 rounded-md focus:ring-emerald-500 focus:border-emerald-500"
                  >
                    <option :value="50">50个词</option>
                    <option :value="100">100个词</option>
                    <option :value="200">200个词</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- 词云图组件 -->
            <div class="h-96 relative">
              <HotWordCloudChart
                ref="wordCloudRef"
                :words="wordCloudData"
                :theme="wordCloudConfig.theme"
                :responsive="true"
                :loading="loading"
                :error="error"
                :options="wordCloudOptions"
                @word-click="handleWordClick"
                @word-hover="handleWordHover"
                @render-start="handleRenderStart"
                @render-complete="handleRenderComplete"
                @render-error="handleRenderError"
                @download="handleWordCloudDownload"
              />
            </div>
          </div>
        </div>

        <!-- 热词排行榜 -->
        <div class="space-y-6">
          <!-- TOP 10 热词 -->
          <div class="bg-white rounded-lg shadow-sm border p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">TOP 10 热词</h3>
            <div class="space-y-3">
              <div
                v-for="(item, index) in topWords"
                :key="item.text"
                class="flex items-center justify-between p-3 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                @click="highlightWord(item.text)"
              >
                <div class="flex items-center space-x-3">
                  <div
                    class="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white"
                    :class="getRankingClass(index)"
                  >
                    {{ index + 1 }}
                  </div>
                  <span class="font-medium text-gray-900">{{ item.text }}</span>
                </div>
                <div class="text-right">
                  <div class="text-sm font-semibold text-gray-900">{{ item.weight }}</div>
                  <div class="text-xs text-gray-500">搜索次数</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 趋势信息 -->
          <div class="bg-white rounded-lg shadow-sm border p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">趋势分析</h3>
            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-500">上升最快</span>
                <span class="text-sm font-medium text-green-600">{{ trendInfo.rising || '-' }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-500">下降最快</span>
                <span class="text-sm font-medium text-red-600">{{ trendInfo.falling || '-' }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gray-500">新热词</span>
                <span class="text-sm font-medium text-blue-600">{{ trendInfo.new || '-' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 详细数据表格 -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mt-8">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold text-gray-900">热词详细数据</h2>
          <div class="flex items-center space-x-3">
            <div class="relative">
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索热词..."
                class="pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm"
              >
              <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <svg class="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">排名</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">热词</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">搜索次数</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">占比</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">趋势</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">最后搜索时间</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr
                v-for="(item, index) in filteredStatistics"
                :key="item.text"
                class="hover:bg-gray-50"
              >
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {{ index + 1 }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center">
                    <span class="text-sm font-medium text-gray-900">{{ item.text }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ item.weight.toLocaleString() }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ ((item.weight / summary.totalSearches) * 100).toFixed(2) }}%
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span :class="getTrendClass(item.trend || 'stable')" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium">
                    {{ getTrendLabel(item.trend || 'stable') }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ formatDate(item.lastSearchTime) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div v-if="filteredStatistics.length > pageSize" class="flex items-center justify-between mt-6">
          <div class="text-sm text-gray-700">
            显示 {{ (currentPage - 1) * pageSize + 1 }} 到 {{ Math.min(currentPage * pageSize, filteredStatistics.length) }}
            条，共 {{ filteredStatistics.length }} 条记录
          </div>
          <div class="flex items-center space-x-2">
            <button
              @click="currentPage--"
              :disabled="currentPage === 1"
              class="px-3 py-1 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              上一页
            </button>
            <span class="px-3 py-1 text-sm text-gray-700">{{ currentPage }} / {{ totalPages }}</span>
            <button
              @click="currentPage++"
              :disabled="currentPage === totalPages"
              class="px-3 py-1 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import HotWordCloudChart from '@/components/statistics/HotWordCloudChart.vue'
import HotWordFilter from '@/components/statistics/HotWordFilter.vue'
import type { HotWordItem, HotWordStatistics, FilterConfig, WordCloudOptions } from '@/types/statistics'
import { statisticsApi } from '@/api/statistics'

// ============= 响应式状态 =============

const loading = ref(false)
const error = ref<string | null>(null)
const wordCloudRef = ref()

// 筛选配置
const filterConfig = reactive<FilterConfig>({
  timeRange: {
    start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), // 默认最近7天
    end: new Date()
  },
  searchCondition: '',
  hotWordLimit: 100,
  channels: []
})

// 词云配置
const wordCloudConfig = reactive({
  theme: 'light-green',
  maxWords: 100
})

// 数据状态
const statistics = ref<HotWordStatistics[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(50)

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
  gridSize: 8,
  weightFactor: (size: number) => Math.pow(size, 2.3) / 600,
  fontFamily: 'Arial, sans-serif',
  rotateRatio: 0.5,
  rotationSteps: 4,
  backgroundColor: 'transparent',
  color: (word: string, weight: number) => {
    // 根据权重返回不同颜色
    const colors = ['#10b981', '#059669', '#047857', '#065f46']
    const index = Math.floor((weight / 100) * colors.length)
    return colors[Math.min(index, colors.length - 1)]
  }
}))

const summary = computed(() => {
  const totalSearches = statistics.value.reduce((sum, stat) => sum + stat.searchCount, 0)
  const uniqueKeywords = statistics.value.length
  const averageFrequency = uniqueKeywords > 0 ? totalSearches / uniqueKeywords : 0

  return {
    totalSearches,
    uniqueKeywords,
    averageFrequency,
    timePeriod: `${filterConfig.timeRange.start.toLocaleDateString()} - ${filterConfig.timeRange.end.toLocaleDateString()}`
  }
})

const topWords = computed(() => {
  return statistics.value.slice(0, 10).map(stat => ({
    text: stat.keyword,
    weight: stat.searchCount
  }))
})

const trendInfo = computed(() => {
  const rising = statistics.value.find(s => s.trend === 'rising')?.keyword
  const falling = statistics.value.find(s => s.trend === 'falling')?.keyword
  const newWord = statistics.value.find(s => s.trend === 'new')?.keyword

  return {
    rising,
    falling,
    new: newWord
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

const totalPages = computed(() => Math.ceil(filteredStatistics.value.length / pageSize.value))

// ============= 方法 =============

const loadStatistics = async () => {
  loading.value = true
  error.value = null

  try {
    const response = await statisticsApi.getHotWordStatistics({
      startTime: filterConfig.timeRange.start.toISOString(),
      endTime: filterConfig.timeRange.end.toISOString(),
      searchCondition: filterConfig.searchCondition,
      limit: filterConfig.hotWordLimit,
      channels: filterConfig.channels
    })

    statistics.value = response.data
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载数据失败'
    console.error('Failed to load statistics:', err)
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  await loadStatistics()
}

const exportData = async () => {
  try {
    const blob = new Blob([JSON.stringify(statistics.value, null, 2)], {
      type: 'application/json'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `hot-word-statistics-${Date.now()}.json`
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    console.error('Failed to export data:', err)
  }
}

const handleFilterChange = async () => {
  currentPage.value = 1
  await loadStatistics()
}

const handleWordClick = (word: HotWordItem, event: Event) => {
  console.log('Word clicked:', word)
  // 可以在这里实现更多交互逻辑，比如显示详细信息
}

const handleWordHover = (word: HotWordItem, event: Event) => {
  // 悬停效果已由组件内部处理
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

const highlightWord = (word: string) => {
  // 实现词语高亮逻辑
  console.log('Highlight word:', word)
}

const getRankingClass = (index: number) => {
  if (index === 0) return 'bg-yellow-500'
  if (index === 1) return 'bg-gray-400'
  if (index === 2) return 'bg-yellow-600'
  return 'bg-gray-500'
}

const getTrendClass = (trend: string) => {
  switch (trend) {
    case 'rising':
      return 'bg-green-100 text-green-800'
    case 'falling':
      return 'bg-red-100 text-red-800'
    case 'new':
      return 'bg-blue-100 text-blue-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

const getTrendLabel = (trend: string) => {
  switch (trend) {
    case 'rising':
      return '上升'
    case 'falling':
      return '下降'
    case 'new':
      return '新词'
    default:
      return '稳定'
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// ============= 监听器 =============

watch([() => wordCloudConfig.theme, () => wordCloudConfig.maxWords], () => {
  // 词云配置变化时重新渲染
  nextTick(() => {
    if (wordCloudRef.value?.refresh) {
      wordCloudRef.value.refresh()
    }
  })
})

watch(searchKeyword, () => {
  currentPage.value = 1
})

// ============= 生命周期 =============

onMounted(async () => {
  await loadStatistics()
})
</script>

<style scoped>
.hot-word-statistics-page {
  min-height: 100vh;
}

/* 自定义滚动条样式 */
.overflow-x-auto::-webkit-scrollbar {
  height: 6px;
}

.overflow-x-auto::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.overflow-x-auto::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.overflow-x-auto::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>