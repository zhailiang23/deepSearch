<template>
  <div class="hot-word-statistics-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-info">
          <h1 class="page-title">热词统计分析</h1>
          <p class="page-description">
            分析搜索日志中的热门关键词，了解用户搜索趋势和热点话题
          </p>
        </div>
        <div class="header-actions">
          <button
            @click="refreshData"
            :disabled="loading"
            class="action-btn refresh-btn"
            title="刷新数据"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            刷新
          </button>
          <button
            @click="exportData"
            :disabled="loading || !hotWordData.length"
            class="action-btn export-btn"
            title="导出数据"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            导出
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="page-content">
      <!-- 过滤器侧边栏 -->
      <div class="filter-sidebar">
        <HotWordFilter
          v-model:filter-state="filterState"
          :search-suggestions="searchSuggestions"
          :loading="loading"
          @filter-change="handleFilterChange"
          @search="handleSearch"
          @reset="handleFilterReset"
        />
      </div>

      <!-- 数据展示区域 -->
      <div class="data-display">
        <!-- 统计概览卡片 -->
        <div class="stats-overview">
          <div class="stats-card">
            <div class="stats-icon">
              <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <div class="stats-content">
              <p class="stats-label">总热词数</p>
              <p class="stats-value">{{ pagination.total }}</p>
            </div>
          </div>

          <div class="stats-card">
            <div class="stats-icon">
              <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
            <div class="stats-content">
              <p class="stats-label">总搜索量</p>
              <p class="stats-value">{{ totalSearchCount.toLocaleString() }}</p>
            </div>
          </div>

          <div class="stats-card">
            <div class="stats-icon">
              <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div class="stats-content">
              <p class="stats-label">时间范围</p>
              <p class="stats-value">{{ timeRangeDisplay }}</p>
            </div>
          </div>
        </div>

        <!-- 热词排行榜表格 -->
        <div class="ranking-section">
          <HotWordRankingTable
            :data="hotWordData"
            :loading="loading"
            :error="error"
            :pagination="pagination"
            :current-sort="currentSort"
            @sort-change="handleSortChange"
            @page-change="handlePageChange"
            @page-size-change="handlePageSizeChange"
            @row-click="handleRowClick"
            @view-detail="handleViewDetail"
            @refresh="refreshData"
            @export="exportData"
            @retry="retryLoadData"
          />
        </div>
      </div>
    </div>

    <!-- 热词详情弹窗 -->
    <HotWordDetailModal
      v-if="selectedHotWord"
      :hot-word="selectedHotWord"
      :visible="showDetailModal"
      @close="closeDetailModal"
      @export="exportHotWordDetail"
    />

    <!-- 导出进度弹窗 -->
    <ExportProgressModal
      :visible="showExportModal"
      :progress="exportProgress"
      :status="exportStatus"
      @close="closeExportModal"
      @cancel="cancelExport"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import HotWordFilter from '@/components/statistics/HotWordFilter.vue'
import HotWordRankingTable from '@/components/statistics/HotWordRankingTable.vue'
// 注意：这些组件还需要创建，暂时注释掉
// import HotWordDetailModal from '@/components/statistics/HotWordDetailModal.vue'
// import ExportProgressModal from '@/components/statistics/ExportProgressModal.vue'
import type {
  HotWordItem,
  FilterState,
  HotWordQueryParams,
  SortConfig,
  PaginationConfig,
  HotWordStatisticsResponse
} from '@/types/statistics'

// 临时的模拟组件
const HotWordDetailModal = { template: '<div></div>' }
const ExportProgressModal = { template: '<div></div>' }

const router = useRouter()

// 响应式数据
const loading = ref(false)
const error = ref<string | null>(null)
const hotWordData = ref<HotWordItem[]>([])
const selectedHotWord = ref<HotWordItem | null>(null)
const showDetailModal = ref(false)
const showExportModal = ref(false)
const exportProgress = ref(0)
const exportStatus = ref<'preparing' | 'exporting' | 'completed' | 'error'>('preparing')

// 搜索建议（可以从历史搜索记录获取）
const searchSuggestions = ref<string[]>([
  '用户管理',
  '权限配置',
  '数据统计',
  '搜索功能',
  '系统设置'
])

// 过滤状态
const filterState = reactive<FilterState>({
  timeRange: {
    startTime: '',
    endTime: '',
    preset: 'last7days'
  },
  searchKeyword: '',
  limit: 50
})

// 排序配置
const currentSort = reactive<SortConfig>({
  field: 'count',
  order: 'desc'
})

// 分页配置
const pagination = reactive<PaginationConfig>({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0
})

/**
 * 计算总搜索次数
 */
const totalSearchCount = computed(() => {
  return hotWordData.value.reduce((total, item) => total + item.count, 0)
})

/**
 * 时间范围显示
 */
const timeRangeDisplay = computed(() => {
  if (filterState.timeRange.preset) {
    const presetLabels: Record<string, string> = {
      today: '今天',
      last7days: '最近7天',
      last30days: '最近30天',
      thisMonth: '本月',
      lastMonth: '上月'
    }
    return presetLabels[filterState.timeRange.preset] || '自定义'
  }
  return '自定义'
})

/**
 * 加载热词数据
 */
const loadHotWordData = async (params?: HotWordQueryParams) => {
  loading.value = true
  error.value = null

  try {
    // 构建查询参数
    const queryParams: HotWordQueryParams = {
      startTime: filterState.timeRange.startTime,
      endTime: filterState.timeRange.endTime,
      searchKeyword: filterState.searchKeyword,
      limit: filterState.limit,
      offset: (pagination.page - 1) * pagination.pageSize,
      sortBy: currentSort.field,
      sortOrder: currentSort.order,
      ...params
    }

    // TODO: 调用实际的API
    // const response = await hotWordService.getHotWordStatistics(queryParams)

    // 模拟API响应
    const mockResponse: HotWordStatisticsResponse = {
      items: generateMockHotWordData(),
      total: 150,
      page: pagination.page,
      pageSize: pagination.pageSize,
      hasNext: pagination.page < 8,
      hasPrevious: pagination.page > 1
    }

    hotWordData.value = mockResponse.items
    pagination.total = mockResponse.total
    pagination.totalPages = Math.ceil(mockResponse.total / pagination.pageSize)

  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载热词数据失败'
    console.error('Failed to load hot word data:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 生成模拟热词数据
 */
const generateMockHotWordData = (): HotWordItem[] => {
  const keywords = [
    '用户管理', '权限配置', '数据统计', '搜索功能', '系统设置',
    '角色管理', '日志管理', '性能监控', '安全配置', '备份恢复',
    '界面设置', '通知管理', '文件上传', '数据导出', 'API接口',
    '数据库优化', '缓存管理', '负载均衡', '集群配置', '监控告警'
  ]

  return keywords.map((keyword, index) => ({
    keyword,
    count: Math.floor(Math.random() * 1000) + 100,
    rank: index + 1,
    trend: ['up', 'down', 'stable'][Math.floor(Math.random() * 3)] as 'up' | 'down' | 'stable',
    percentage: Math.random() * 100
  })).sort((a, b) => b.count - a.count)
}

/**
 * 处理过滤条件变化
 */
const handleFilterChange = (params: HotWordQueryParams) => {
  pagination.page = 1 // 重置页码
  loadHotWordData(params)
}

/**
 * 处理搜索
 */
const handleSearch = (keyword: string) => {
  filterState.searchKeyword = keyword
  pagination.page = 1
  loadHotWordData()
}

/**
 * 处理过滤器重置
 */
const handleFilterReset = () => {
  pagination.page = 1
  loadHotWordData()
}

/**
 * 处理排序变化
 */
const handleSortChange = (field: string, order: 'asc' | 'desc') => {
  currentSort.field = field as 'count' | 'keyword' | 'rank'
  currentSort.order = order
  loadHotWordData()
}

/**
 * 处理分页变化
 */
const handlePageChange = (page: number, pageSize: number) => {
  pagination.page = page
  pagination.pageSize = pageSize
  loadHotWordData()
}

/**
 * 处理页面大小变化
 */
const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadHotWordData()
}

/**
 * 处理行点击
 */
const handleRowClick = (item: HotWordItem) => {
  selectedHotWord.value = item
  showDetailModal.value = true
}

/**
 * 处理查看详情
 */
const handleViewDetail = (item: HotWordItem) => {
  selectedHotWord.value = item
  showDetailModal.value = true
}

/**
 * 关闭详情弹窗
 */
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedHotWord.value = null
}

/**
 * 刷新数据
 */
const refreshData = () => {
  loadHotWordData()
}

/**
 * 重试加载数据
 */
const retryLoadData = () => {
  loadHotWordData()
}

/**
 * 导出数据
 */
const exportData = async () => {
  showExportModal.value = true
  exportProgress.value = 0
  exportStatus.value = 'preparing'

  try {
    // 模拟导出进度
    const progressInterval = setInterval(() => {
      exportProgress.value += 10
      if (exportProgress.value >= 100) {
        clearInterval(progressInterval)
        exportStatus.value = 'completed'
        setTimeout(() => {
          showExportModal.value = false
        }, 2000)
      }
    }, 200)

    exportStatus.value = 'exporting'

  } catch (err) {
    exportStatus.value = 'error'
    console.error('Export failed:', err)
  }
}

/**
 * 导出热词详情
 */
const exportHotWordDetail = (hotWord: HotWordItem) => {
  console.log('Exporting hot word detail:', hotWord)
  // TODO: 实现热词详情导出
}

/**
 * 关闭导出弹窗
 */
const closeExportModal = () => {
  showExportModal.value = false
  exportProgress.value = 0
  exportStatus.value = 'preparing'
}

/**
 * 取消导出
 */
const cancelExport = () => {
  closeExportModal()
}

// 监听路由查询参数
watch(() => router.currentRoute.value.query, (query) => {
  // 从URL查询参数恢复过滤状态
  if (query.keyword) {
    filterState.searchKeyword = query.keyword as string
  }
  if (query.limit) {
    filterState.limit = parseInt(query.limit as string) || 50
  }
}, { immediate: true })

// 组件挂载时加载数据
onMounted(() => {
  loadHotWordData()
})
</script>

<style scoped>
.hot-word-statistics-page {
  @apply min-h-screen bg-gray-50;
}

/* 页面头部 */
.page-header {
  @apply bg-white border-b border-gray-200 px-6 py-4;
}

.header-content {
  @apply max-w-7xl mx-auto flex items-center justify-between;
}

.header-info {
  @apply space-y-1;
}

.page-title {
  @apply text-2xl font-bold text-gray-900;
}

.page-description {
  @apply text-gray-600;
}

.header-actions {
  @apply flex items-center gap-3;
}

.action-btn {
  @apply
    inline-flex
    items-center
    px-4
    py-2
    text-sm
    font-medium
    rounded-md
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-offset-2
    disabled:opacity-50
    disabled:cursor-not-allowed;
}

.refresh-btn {
  @apply
    text-gray-600
    bg-white
    border
    border-gray-300
    hover:bg-gray-50
    hover:border-gray-400
    focus:ring-gray-500;
}

.export-btn {
  @apply
    text-white
    bg-green-600
    border
    border-green-600
    hover:bg-green-700
    hover:border-green-700
    focus:ring-green-500;
}

/* 主要内容 */
.page-content {
  @apply max-w-7xl mx-auto p-6 flex gap-6;
}

.filter-sidebar {
  @apply w-80 flex-shrink-0;
}

.data-display {
  @apply flex-1 space-y-6;
}

/* 统计概览 */
.stats-overview {
  @apply grid grid-cols-1 md:grid-cols-3 gap-6;
}

.stats-card {
  @apply bg-white p-6 rounded-lg shadow-sm border border-gray-200 flex items-center gap-4;
}

.stats-icon {
  @apply
    flex
    items-center
    justify-center
    w-12
    h-12
    rounded-lg
    bg-green-100;
}

.stats-content {
  @apply space-y-1;
}

.stats-label {
  @apply text-sm font-medium text-gray-600;
}

.stats-value {
  @apply text-2xl font-bold text-gray-900;
}

/* 排行榜区域 */
.ranking-section {
  @apply space-y-4;
}

/* 响应式优化 */
@media (max-width: 1024px) {
  .page-content {
    @apply flex-col;
  }

  .filter-sidebar {
    @apply w-full;
  }

  .stats-overview {
    @apply grid-cols-1 md:grid-cols-2;
  }
}

@media (max-width: 768px) {
  .page-header {
    @apply px-4 py-3;
  }

  .header-content {
    @apply flex-col items-start gap-4;
  }

  .header-actions {
    @apply self-end;
  }

  .page-title {
    @apply text-xl;
  }

  .page-content {
    @apply p-4;
  }

  .stats-overview {
    @apply grid-cols-1;
  }

  .stats-card {
    @apply p-4;
  }

  .stats-value {
    @apply text-xl;
  }

  .action-btn {
    @apply text-xs px-3 py-1.5;
  }
}

/* 加载状态 */
.page-content[data-loading="true"] {
  @apply opacity-75;
}

/* 动画优化 */
.stats-card {
  transition: all 0.2s ease-in-out;
}

.stats-card:hover {
  @apply shadow-md transform scale-105;
}

/* 无障碍优化 */
.action-btn:focus {
  @apply ring-offset-white;
}

/* 深色模式兼容 */
@media (prefers-color-scheme: dark) {
  .hot-word-statistics-page {
    @apply bg-gray-900;
  }

  .page-header {
    @apply bg-gray-800 border-gray-700;
  }

  .page-title {
    @apply text-white;
  }

  .page-description {
    @apply text-gray-300;
  }

  .stats-card {
    @apply bg-gray-800 border-gray-700;
  }

  .stats-value {
    @apply text-white;
  }

  .stats-label {
    @apply text-gray-300;
  }
}
</style>