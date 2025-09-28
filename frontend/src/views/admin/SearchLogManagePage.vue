<template>

    <!-- 筛选器 -->
    <div class="filter-section mb-6">
      <Card class="shadow-sm border-green-100">
        <SearchLogFilter
          v-model="filterParams"
          @filter="handleFilter"
          @reset="handleResetFilter"
        />
      </Card>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <Card class="shadow-sm border-green-100">
        <SearchLogTable
          :data="tableData"
          :loading="tableLoading"
          :pagination="pagination"
          :current-sort="{ field: filterParams.sort || 'createdAt', direction: filterParams.direction || 'desc' }"
          @page-change="handlePageChange"
          @sort-change="handleSortChange"
          @view-detail="handleViewDetail"
          @view-clicks="handleViewClicks"
          @refresh="refreshTableData"
        />
      </Card>
    </div>

    <!-- 详情弹窗 -->
    <SearchLogDetailModal
      v-model:visible="detailModalVisible"
      :log-id="selectedLogId"
      @close="handleCloseDetail"
    />

    <!-- 加载遮罩 -->
    <div v-if="isInitialLoading" class="loading-overlay">
      <div class="loading-content">
        <div class="loading-spinner-large"></div>
        <p class="loading-text">正在加载搜索日志数据...</p>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from '@/utils/message'
import { searchLogApi } from '@/api/searchLog'
import { exportData as exportDataUtil, preprocessData } from '@/utils/export'
import type {
  SearchLog,
  SearchLogQuery,
  PaginatedResponse,
  SearchLogStatistics
} from '@/types/searchLog'

// 组件导入
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import SearchLogFilter from '@/components/search-log/SearchLogFilter.vue'
import SearchLogTable from '@/components/search-log/SearchLogTable.vue'
import SearchLogDetailModal from '@/components/search-log/SearchLogDetailModal.vue'

const router = useRouter()

// 响应式数据
const isInitialLoading = ref(true)
const isRefreshing = ref(false)
const isExporting = ref(false)
const statisticsLoading = ref(false)
const tableLoading = ref(false)
const detailModalVisible = ref(false)
const selectedLogId = ref<number | null>(null)
const isUnmounted = ref(false)

// 面包屑导航
const breadcrumbs = [
  { label: '首页', to: { name: 'Dashboard' } },
  { label: '系统管理', to: '/' },
  { label: '搜索日志管理' }
]

const statistics = reactive<SearchLogStatistics>({
  totalSearches: 0,
  successRate: 0,
  avgResponseTime: 0,
  totalClicks: 0,
  topKeywords: [],
  searchTrends: [],
  searchTrend: { direction: 'stable', value: 0 },
  successTrend: { direction: 'stable', value: 0 },
  responseTrend: { direction: 'stable', value: 0 },
  clickTrend: { direction: 'stable', value: 0 }
})

// 头部统计信息
const headerStats = computed(() => [
  { key: 'active_users', label: '活跃用户', value: statistics.totalSearches > 0 ? Math.floor(statistics.totalSearches / 3) : 0 },
  { key: 'avg_per_user', label: '人均搜索', value: statistics.totalSearches > 0 ? (statistics.totalSearches / Math.max(Math.floor(statistics.totalSearches / 3), 1)).toFixed(1) : '0' },
  { key: 'click_rate', label: '点击率', value: `${statistics.totalSearches > 0 ? ((statistics.totalClicks / statistics.totalSearches) * 100).toFixed(1) : 0}%` }
])

// 筛选参数
const filterParams = reactive<SearchLogQuery>({
  page: 0,
  size: 20,
  sort: 'createdAt',
  direction: 'desc'
})

// 表格数据
const tableData = ref<SearchLog[]>([])
const pagination = reactive<PaginatedResponse<SearchLog>>({
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
  first: true,
  last: true
})

/**
 * 初始化数据
 */
onMounted(async () => {
  try {
    isInitialLoading.value = true
    await loadTableData()
  } catch (error) {
    console.error('初始化数据失败:', error)
    message.error('数据初始化失败，请刷新页面重试')
  } finally {
    isInitialLoading.value = false
  }
})

/**
 * 监听筛选参数变化
 */
const stopWatcher = watch(
  () => ({ ...filterParams }),
  () => {
    if (!isUnmounted.value) {
      loadTableData()
    }
  },
  { deep: true }
)

/**
 * 加载统计数据
 */
const loadStatistics = async () => {
  if (isUnmounted.value) return

  try {
    statisticsLoading.value = true

    const endTime = new Date()
    const startTime = new Date()
    startTime.setDate(startTime.getDate() - 30) // 最近30天

    const response = await searchLogApi.getSearchStatistics(
      startTime.toISOString(),
      endTime.toISOString()
    )

    // 检查组件是否已经卸载
    if (isUnmounted.value) return

    if (response.success && response.data) {
      Object.assign(statistics, {
        totalSearches: response.data.totalSearches || 0,
        successRate: response.data.successRate || 0,
        avgResponseTime: response.data.avgResponseTime || 0,
        totalClicks: response.data.totalClicks || 0,
        topKeywords: response.data.topKeywords || [],
        searchTrends: response.data.searchTrends || [],
        searchTrend: response.data.searchTrend || { direction: 'stable', value: 0 },
        successTrend: response.data.successTrend || { direction: 'stable', value: 0 },
        responseTrend: response.data.responseTrend || { direction: 'stable', value: 0 },
        clickTrend: response.data.clickTrend || { direction: 'stable', value: 0 }
      })
    } else {
      // 使用模拟数据
      Object.assign(statistics, {
        totalSearches: 12485,
        successRate: 87.3,
        avgResponseTime: 342,
        totalClicks: 8721,
        topKeywords: [
          { keyword: 'Vue 3', count: 1250 },
          { keyword: 'TypeScript', count: 980 },
          { keyword: 'Composition API', count: 760 }
        ],
        searchTrends: [
          { date: '2024-01-01', count: 120 },
          { date: '2024-01-02', count: 135 },
          { date: '2024-01-03', count: 142 }
        ],
        searchTrend: { direction: 'up', value: 12.5 },
        successTrend: { direction: 'up', value: 3.2 },
        responseTrend: { direction: 'down', value: 8.1 },
        clickTrend: { direction: 'up', value: 15.7 }
      })
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('统计数据加载失败')
  } finally {
    statisticsLoading.value = false
  }
}

/**
 * 加载表格数据
 */
const loadTableData = async () => {
  if (isUnmounted.value) return

  try {
    tableLoading.value = true

    const response = await searchLogApi.getSearchLogs(filterParams)

    // 检查组件是否已经卸载
    if (isUnmounted.value) return

    if (response.success && response.data) {
      tableData.value = response.data.content || []
      Object.assign(pagination, response.data)
    } else {
      tableData.value = []
      Object.assign(pagination, {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: filterParams.size || 20,
        number: filterParams.page || 0,
        first: true,
        last: true
      })

      if (!response.success) {
        message.error(response.message || '数据加载失败')
      }
    }
  } catch (error) {
    console.error('加载表格数据失败:', error)
    message.error('数据加载失败')
    tableData.value = []
  } finally {
    tableLoading.value = false
  }
}

/**
 * 处理筛选
 */
const handleFilter = (filters: Partial<SearchLogQuery>) => {
  Object.assign(filterParams, filters, { page: 0 })
}

/**
 * 重置筛选
 */
const handleResetFilter = () => {
  Object.assign(filterParams, {
    userId: undefined,
    searchSpaceId: undefined,
    query: undefined,
    status: undefined,
    startTime: undefined,
    endTime: undefined,
    minResponseTime: undefined,
    maxResponseTime: undefined,
    page: 0,
    size: 20,
    sort: 'createdAt',
    direction: 'desc'
  })
}

/**
 * 处理分页变化
 */
const handlePageChange = (page: number, size: number) => {
  filterParams.page = page
  filterParams.size = size
}

/**
 * 处理排序变化
 */
const handleSortChange = (sort: string, direction: 'asc' | 'desc') => {
  filterParams.sort = sort
  filterParams.direction = direction
  filterParams.page = 0
}

/**
 * 查看详情
 */
const handleViewDetail = (logId: number) => {
  selectedLogId.value = logId
  detailModalVisible.value = true
}

/**
 * 查看点击记录
 */
const handleViewClicks = (logId: number) => {
  // 直接打开详情弹窗，详情弹窗中包含点击记录
  handleViewDetail(logId)
}

/**
 * 关闭详情
 */
const handleCloseDetail = () => {
  detailModalVisible.value = false
  selectedLogId.value = null
}

/**
 * 刷新数据
 */
const refreshData = async () => {
  isRefreshing.value = true
  try {
    await loadTableData()
    message.success('数据刷新成功')
  } catch (error) {
    message.error('数据刷新失败')
  } finally {
    isRefreshing.value = false
  }
}

/**
 * 刷新表格数据
 */
const refreshTableData = async () => {
  await loadTableData()
}

/**
 * 导出数据
 */
const exportData = async () => {
  try {
    isExporting.value = true

    // 获取所有数据（不分页）
    const exportParams = { ...filterParams, page: 0, size: 10000 }
    const response = await searchLogApi.getSearchLogs(exportParams)

    if (response.success && response.data?.content) {
      const processedData = preprocessData(response.data.content, {
        dateFields: ['createdAt'],
        fieldMapping: {
          id: 'ID',
          userId: '用户ID',
          userIp: '用户IP',
          searchSpaceId: '搜索空间',
          query: '查询关键词',
          resultCount: '结果数量',
          responseTime: '响应时间(ms)',
          status: '状态',
          clickCount: '点击次数',
          createdAt: '创建时间'
        }
      })

      const filename = `搜索日志_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}`

      await exportDataUtil(processedData, {
        filename,
        format: 'excel'
      })

      message.success('数据导出成功')
    } else {
      message.error('导出数据获取失败')
    }
  } catch (error) {
    console.error('导出失败:', error)
    message.error('数据导出失败')
  } finally {
    isExporting.value = false
  }
}

/**
 * 处理统计卡片点击
 */
const handleStatClick = (type: 'searches' | 'clicks') => {
  if (type === 'searches') {
    // 重置筛选条件显示所有搜索
    handleResetFilter()
  } else if (type === 'clicks') {
    // 筛选有点击的记录
    Object.assign(filterParams, {
      page: 0,
      sort: 'clickCount',
      direction: 'desc'
    })
  }
}

/**
 * 组件卸载前的清理工作
 */
onBeforeUnmount(() => {
  isUnmounted.value = true

  // 停止监听器
  if (stopWatcher) {
    stopWatcher()
  }

  // 重置所有 loading 状态
  isInitialLoading.value = false
  isRefreshing.value = false
  isExporting.value = false
  statisticsLoading.value = false
  tableLoading.value = false

  // 关闭弹窗
  detailModalVisible.value = false
  selectedLogId.value = null
})
</script>

<style scoped>
.search-log-manage-page {
  @apply min-h-screen bg-gradient-to-br from-green-50 to-blue-50 relative;
}

.header-actions {
  @apply flex items-center space-x-3;
}

.stats-dashboard {
  @apply px-6;
}

.filter-section {
  @apply px-6;
}

.table-section {
  @apply px-6 pb-6;
}

/* 加载动画 */
.loading-spinner {
  @apply border-2 border-gray-300 border-t-green-500 rounded-full animate-spin;
}

.loading-spinner-large {
  @apply w-12 h-12 border-4 border-gray-300 border-t-green-500 rounded-full animate-spin;
}

.loading-overlay {
  @apply fixed inset-0 bg-white/80 backdrop-blur-sm flex items-center justify-center z-50;
}

.loading-content {
  @apply flex flex-col items-center space-y-4;
}

.loading-text {
  @apply text-gray-600 text-lg font-medium;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-log-manage-page {
    @apply bg-gray-50;
  }

  .stats-dashboard,
  .filter-section,
  .table-section {
    @apply px-4;
  }

  .header-actions {
    @apply flex-col space-x-0 space-y-2 w-full;
  }
}

/* 卡片阴影增强 */
:deep(.card) {
  @apply transition-all duration-200 hover:shadow-md;
}

/* 绿色主题增强 */
.stats-dashboard :deep(.stat-card--green) {
  @apply border-green-200 bg-gradient-to-br from-green-50 to-green-100;
}

.stats-dashboard :deep(.stat-card--blue) {
  @apply border-blue-200 bg-gradient-to-br from-blue-50 to-blue-100;
}

.stats-dashboard :deep(.stat-card--orange) {
  @apply border-orange-200 bg-gradient-to-br from-orange-50 to-orange-100;
}

.stats-dashboard :deep(.stat-card--purple) {
  @apply border-purple-200 bg-gradient-to-br from-purple-50 to-purple-100;
}

/* 动画效果 */
.search-log-manage-page {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stats-dashboard > .grid > * {
  animation: slideInUp 0.6s ease-out;
  animation-fill-mode: both;
}

.stats-dashboard > .grid > *:nth-child(1) { animation-delay: 0.1s; }
.stats-dashboard > .grid > *:nth-child(2) { animation-delay: 0.2s; }
.stats-dashboard > .grid > *:nth-child(3) { animation-delay: 0.3s; }
.stats-dashboard > .grid > *:nth-child(4) { animation-delay: 0.4s; }

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.filter-section,
.table-section {
  animation: slideInUp 0.6s ease-out 0.5s both;
}
</style>