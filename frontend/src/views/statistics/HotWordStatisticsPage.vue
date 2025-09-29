<template>
  
    

    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 筛选条件 -->
        <HotWordFilter
          ref="filterRef"
          v-model="filterConfig as any"
          @filter="handleFilterChange"
          :loading="loading"
        />

      <!-- 主内容区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
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
                :error="error || undefined"
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
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr
                v-for="(item, index) in filteredStatistics"
                :key="item.keyword"
                class="hover:bg-gray-50"
              >
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {{ index + 1 }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center">
                    <span class="text-sm font-medium text-gray-900">{{ item.keyword }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ item.count.toLocaleString() }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ (item.percentage || ((item.count / summary.totalSearches) * 100)).toFixed(2) }}%
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span :class="getTrendClass(item.trend || 'stable')" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium">
                    {{ getTrendLabel(item.trend || 'stable') }}
                  </span>
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
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import HotWordCloudChart from '@/components/statistics/HotWordCloudChart.vue'
import HotWordFilter from '@/components/statistics/HotWordFilter.vue'
import { useHotWordData } from '@/composables/useHotWordData'
import { useHotWordStatisticsStore } from '@/stores/hotWordStatistics'
import type { HotWordItem } from '@/types/statistics'

// ============= 响应式状态 =============

// 使用Pinia Store
const store = useHotWordStatisticsStore()

const wordCloudRef = ref()
const filterRef = ref()

// 从store中获取响应式数据
const hotWords = computed(() => store.hotWords)
const statistics = computed(() => {
  // 创建兼容格式的统计数据
  return store.hotWords.map((word, index) => ({
    keyword: word.word,
    count: word.count,
    rank: index + 1,
    trend: (word as any).trend || 'stable',
    percentage: word.percentage
  }))
})
const loading = computed(() => store.loading)
const error = computed(() => store.error)

// 筛选配置 - 使用Store中的filter
const filterConfig = computed({
  get: () => ({
    timeRange: {
      start: store.filter.dateRange.start,
      end: store.filter.dateRange.end
    },
    searchCondition: {
      keywords: [],
      includeMode: 'any'
    },
    limitConfig: {
      limit: store.filter.limit,
      sortBy: 'count',
      sortOrder: 'desc'
    },
    channels: []
  }),
  set: (value: any) => {
    if (value.timeRange) {
      store.updateFilter({
        dateRange: {
          start: value.timeRange.start,
          end: value.timeRange.end
        },
        limit: value.limitConfig?.limit || store.filter.limit
      })
    }
  }
})

// 词云配置
const wordCloudConfig = reactive({
  theme: 'light-green',
  maxWords: 100
})

// 本地UI状态
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(50)

// ============= 计算属性 =============

const wordCloudData = computed(() => {
  // 从统计数据创建词云数据
  return statistics.value
    .slice(0, wordCloudConfig.maxWords)
    .map(item => ({
      text: item.keyword,
      weight: item.count,
      trend: item.trend
    }))
})

const wordCloudOptions = computed((): any => ({
  gridSize: 8,
  weightFactor: (weight: number) => Math.pow(weight, 2.3) / 600,
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
  // 使用Store中的数据摘要
  const dataSummary = store.dataSummary

  if (!dataSummary) {
    return {
      totalSearches: 0,
      uniqueKeywords: 0,
      averageFrequency: 0,
      timePeriod: ''
    }
  }

  return {
    totalSearches: dataSummary.totalCount,
    uniqueKeywords: dataSummary.totalWords,
    averageFrequency: dataSummary.avgCount,
    timePeriod: `${dataSummary.timeRange.start.toLocaleDateString()} - ${dataSummary.timeRange.end.toLocaleDateString()}`
  }
})

const topWords = computed(() => {
  // 使用统计数据前10个作为top words
  return statistics.value.slice(0, 10).map(item => ({
    text: item.keyword,
    weight: item.count
  }))
})

const trendInfo = computed(() => {
  // 从统计数据中获取趋势信息
  const data = statistics.value
  return {
    rising: data.find(item => item.trend === 'up')?.keyword || '-',
    falling: data.find(item => item.trend === 'down')?.keyword || '-',
    new: data.find(item => item.trend === 'new')?.keyword || '-'
  }
})

const filteredStatistics = computed(() => {
  let filtered = statistics.value

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(item =>
      item.keyword.toLowerCase().includes(keyword)
    )
  }

  return filtered
})

const totalPages = computed(() => Math.ceil(filteredStatistics.value.length / pageSize.value))

// ============= 方法 =============

const loadStatistics = async () => {
  try {
    await store.fetchHotWords()
  } catch (err) {
    console.error('Failed to load statistics:', err)
  }
}

const refreshData = async () => {
  try {
    await store.refreshAll()
  } catch (err) {
    console.error('Failed to refresh data:', err)
  }
}

const exportData = async () => {
  try {
    // 导出当前统计数据
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

const handleFilterChange = async (filterData: any) => {
  console.log('Filter changed:', filterData)

  // 根据时间范围设置store的时间范围
  let startDate: Date
  let endDate = new Date()

  switch (filterData.timeRange) {
    case 'last7days':
      startDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
      break
    case 'last30days':
      startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
      break
    case 'last90days':
      startDate = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000)
      break
    case 'custom':
      if (filterData.customStartDate && filterData.customEndDate) {
        startDate = new Date(filterData.customStartDate)
        endDate = new Date(filterData.customEndDate)
      } else {
        startDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
      }
      break
    default:
      startDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
  }

  // 设置时间范围并重新获取数据
  await store.setTimeRange(startDate, endDate)
  currentPage.value = 1
  await store.fetchHotWords()
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
  // 查找热词并高亮
  const hotWord = statistics.value.find(item => item.keyword === word)
  if (hotWord) {
    console.log('Highlight word:', word, 'Count:', hotWord.count)
    // 实现高亮和详情显示逻辑
    searchKeyword.value = word
  }
}

const getRankingClass = (index: number) => {
  if (index === 0) return 'bg-yellow-500'
  if (index === 1) return 'bg-gray-400'
  if (index === 2) return 'bg-yellow-600'
  return 'bg-gray-500'
}

const getTrendClass = (trend: string) => {
  switch (trend) {
    case 'up':
    case 'rising':
      return 'bg-green-100 text-green-800'
    case 'down':
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
    case 'up':
    case 'rising':
      return '上升'
    case 'down':
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
  // 等待DOM渲染完成后调用过滤器初始化
  await nextTick()
  if (filterRef.value && filterRef.value.initializeFilter) {
    filterRef.value.initializeFilter()
  }
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