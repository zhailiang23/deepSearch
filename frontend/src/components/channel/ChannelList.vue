<template>
  <div class="space-y-6">
    <!-- 头部工具栏 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div class="flex-1 max-w-md">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索渠道名称、代码..."
            class="w-full pl-10 pr-4 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground"
            @input="debouncedSearch"
          />
        </div>
      </div>
      <div class="flex items-center space-x-3">
        <select
          v-model="selectedStatus"
          class="px-3 py-2 border border-border rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground"
          @change="handleStatusFilter"
        >
          <option value="">全部状态</option>
          <option value="ACTIVE">激活</option>
          <option value="INACTIVE">未激活</option>
          <option value="SUSPENDED">暂停</option>
        </select>
        <button
          @click="refreshList"
          :disabled="loading"
          class="inline-flex items-center px-4 py-2 border border-border shadow-sm text-sm font-medium rounded-md text-foreground bg-background hover:bg-accent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <RefreshCw :class="['h-4 w-4 mr-2', { 'animate-spin': loading }]" />
          刷新
        </button>
        <button
          @click="$emit('create')"
          class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-primary-foreground bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring"
        >
          <Plus class="h-4 w-4 mr-2" />
          新建渠道
        </button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="statistics" class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="bg-card p-4 rounded-lg shadow border border-border">
        <div class="text-2xl font-bold text-card-foreground">{{ statistics.totalChannels }}</div>
        <div class="text-sm text-muted-foreground">总计</div>
      </div>
      <div class="bg-card p-4 rounded-lg shadow border border-border">
        <div class="text-2xl font-bold text-primary">{{ statistics.activeChannels }}</div>
        <div class="text-sm text-muted-foreground">激活</div>
      </div>
      <div class="bg-card p-4 rounded-lg shadow border border-border">
        <div class="text-2xl font-bold text-muted-foreground">{{ statistics.inactiveChannels }}</div>
        <div class="text-sm text-muted-foreground">未激活</div>
      </div>
      <div class="bg-card p-4 rounded-lg shadow border border-border">
        <div class="text-2xl font-bold text-secondary-foreground">{{ statistics.suspendedChannels }}</div>
        <div class="text-sm text-muted-foreground">暂停</div>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="selectedChannels.length > 0" class="bg-primary/10 border border-primary/20 rounded-lg p-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center">
          <CheckSquare class="h-5 w-5 text-primary mr-2" />
          <span class="text-sm text-primary">
            已选择 {{ selectedChannels.length }} 个渠道
          </span>
        </div>
        <div class="flex items-center space-x-2">
          <button
            @click="batchActivate"
            :disabled="batchLoading"
            class="inline-flex items-center px-3 py-1.5 text-xs font-medium text-green-700 bg-green-100 border border-green-200 rounded hover:bg-green-200 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed dark:bg-green-800 dark:text-green-200 dark:border-green-700 dark:hover:bg-green-700"
          >
            <CheckCircle class="h-3 w-3 mr-1" />
            批量激活
          </button>
          <button
            @click="batchDeactivate"
            :disabled="batchLoading"
            class="inline-flex items-center px-3 py-1.5 text-xs font-medium text-yellow-700 bg-yellow-100 border border-yellow-200 rounded hover:bg-yellow-200 focus:outline-none focus:ring-2 focus:ring-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed dark:bg-yellow-800 dark:text-yellow-200 dark:border-yellow-700 dark:hover:bg-yellow-700"
          >
            <PauseCircle class="h-3 w-3 mr-1" />
            批量停用
          </button>
          <button
            @click="clearSelection"
            class="inline-flex items-center px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 border border-gray-200 rounded hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500 dark:bg-gray-700 dark:text-gray-200 dark:border-gray-600 dark:hover:bg-gray-600"
          >
            <X class="h-3 w-3 mr-1" />
            清除选择
          </button>
        </div>
      </div>
    </div>

    <!-- 渠道列表 -->
    <div class="space-y-6">
      <!-- 标题 -->
      <div class="flex items-center justify-between">
        <h3 class="text-lg leading-6 font-medium text-gray-900 dark:text-white">
          渠道列表
        </h3>
        <div v-if="!loading && channels.length" class="text-sm text-gray-500 dark:text-gray-400">
          共 {{ pagination.totalElements }} 个渠道
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="n in 6" :key="`skeleton-${n}`" class="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 p-6">
          <div class="animate-pulse">
            <div class="flex items-center justify-between mb-4">
              <div class="h-6 bg-gray-200 dark:bg-gray-700 rounded w-32"></div>
              <div class="h-6 bg-gray-200 dark:bg-gray-700 rounded-full w-16"></div>
            </div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20 mb-2"></div>
            <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-full mb-4"></div>
            <div class="flex justify-between items-center mb-4">
              <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-24"></div>
              <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-20"></div>
            </div>
            <div class="flex justify-between items-center">
              <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-24"></div>
              <div class="flex space-x-2">
                <div class="h-8 bg-gray-200 dark:bg-gray-700 rounded w-8"></div>
                <div class="h-8 bg-gray-200 dark:bg-gray-700 rounded w-8"></div>
                <div class="h-8 bg-gray-200 dark:bg-gray-700 rounded w-8"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!channels.length" class="text-center py-12">
        <div class="text-gray-500 dark:text-gray-400">
          <Store class="mx-auto h-12 w-12 mb-4 opacity-50" />
          <p class="text-lg font-medium mb-2">暂无渠道</p>
          <p class="text-sm">点击"新建渠道"按钮创建第一个渠道</p>
        </div>
      </div>

      <!-- 渠道卡片 -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="channel in channels"
          :key="channel.id"
          class="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 hover:shadow-lg transition-shadow duration-200"
        >
          <div class="p-6">
            <!-- 头部：选择框、名称和状态 -->
            <div class="flex items-start justify-between mb-4">
              <div class="flex items-center space-x-3">
                <input
                  type="checkbox"
                  :checked="selectedChannels.includes(channel.id)"
                  @change="toggleChannelSelection(channel.id)"
                  class="h-4 w-4 text-green-600 border-gray-300 rounded focus:ring-green-500 focus:ring-2"
                />
                <div class="flex-1 min-w-0">
                  <h4 class="text-lg font-medium text-gray-900 dark:text-white truncate">
                    {{ channel.name }}
                  </h4>
                </div>
              </div>
              <ChannelStatusBadge :status="channel.status" class="ml-4 flex-shrink-0" />
            </div>

            <!-- 渠道信息 -->
            <div class="mb-4 space-y-2">
              <div class="text-sm text-gray-600 dark:text-gray-400">
                <span class="font-medium">代码:</span> {{ channel.code }}
              </div>
              <div class="text-sm text-gray-600 dark:text-gray-400">
                <span class="font-medium">类型:</span> {{ getChannelTypeLabel(channel.type) }}
              </div>
              <div v-if="channel.description" class="text-sm text-gray-600 dark:text-gray-400 truncate">
                <span class="font-medium">描述:</span> {{ channel.description }}
              </div>
            </div>

            <!-- 销售信息 -->
            <div class="mb-4 p-3 bg-gray-50 dark:bg-gray-700 rounded-md">
              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <div class="text-gray-500 dark:text-gray-400">当月销售</div>
                  <div class="font-semibold text-green-600">
                    ¥{{ formatCurrency(channel.currentMonthSales || 0) }}
                  </div>
                </div>
                <div>
                  <div class="text-gray-500 dark:text-gray-400">月度目标</div>
                  <div class="font-semibold text-gray-900 dark:text-white">
                    ¥{{ formatCurrency(channel.monthlyTarget || 0) }}
                  </div>
                </div>
                <div>
                  <div class="text-gray-500 dark:text-gray-400">总销售额</div>
                  <div class="font-semibold text-blue-600">
                    ¥{{ formatCurrency(channel.totalSales || 0) }}
                  </div>
                </div>
                <div>
                  <div class="text-gray-500 dark:text-gray-400">佣金率</div>
                  <div class="font-semibold text-orange-600">
                    {{ (channel.commissionRate || 0) }}%
                  </div>
                </div>
              </div>
              <div v-if="channel.performanceRatio !== undefined" class="mt-2">
                <div class="text-xs text-gray-500 dark:text-gray-400 mb-1">
                  完成率: {{ (channel.performanceRatio * 100).toFixed(1) }}%
                </div>
                <div class="w-full bg-gray-200 dark:bg-gray-600 rounded-full h-2">
                  <div
                    :class="[
                      'h-2 rounded-full',
                      channel.performanceRatio >= 1 ? 'bg-green-500' :
                      channel.performanceRatio >= 0.8 ? 'bg-yellow-500' : 'bg-red-500'
                    ]"
                    :style="{ width: Math.min(channel.performanceRatio * 100, 100) + '%' }"
                  ></div>
                </div>
              </div>
            </div>

            <!-- 底部：时间和操作按钮 -->
            <div class="flex items-center justify-between">
              <div class="text-xs text-gray-500 dark:text-gray-400">
                创建于 {{ formatDate(channel.createdAt) }}
              </div>
              <div class="flex items-center space-x-1">
                <button
                  v-if="channel.status === 'ACTIVE'"
                  @click="$emit('deactivate', channel)"
                  class="p-2 text-yellow-600 hover:text-yellow-900 dark:text-yellow-400 dark:hover:text-yellow-300 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 rounded-md transition-colors"
                  title="停用"
                >
                  <PauseCircle class="h-4 w-4" />
                </button>
                <button
                  v-else-if="channel.status === 'INACTIVE' || channel.status === 'SUSPENDED'"
                  @click="$emit('activate', channel)"
                  class="p-2 text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-md transition-colors"
                  title="激活"
                >
                  <CheckCircle class="h-4 w-4" />
                </button>

                <button
                  @click="$emit('edit', channel)"
                  class="p-2 text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors"
                  title="编辑"
                >
                  <Edit class="h-4 w-4" />
                </button>
                <button
                  @click="$emit('view', channel)"
                  class="p-2 text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-900/20 rounded-md transition-colors"
                  title="查看详情"
                >
                  <Eye class="h-4 w-4" />
                </button>
                <button
                  v-if="channel.status !== 'DELETED'"
                  @click="$emit('delete', channel)"
                  class="p-2 text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md transition-colors"
                  title="删除"
                >
                  <Trash2 class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.totalPages > 1" class="flex items-center justify-between pt-6">
        <div class="text-sm text-gray-700 dark:text-gray-300">
          显示 {{ pagination.page * pagination.size + 1 }} 到
          {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }}
          共 {{ pagination.totalElements }} 条记录
        </div>
        <div class="flex items-center space-x-2">
          <button
            @click="changePage(pagination.page - 1)"
            :disabled="pagination.first"
            class="px-4 py-2 text-sm border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700 dark:border-gray-600 dark:text-gray-300"
          >
            上一页
          </button>
          <span class="text-sm text-gray-700 dark:text-gray-300">
            第 {{ pagination.page + 1 }} 页，共 {{ pagination.totalPages }} 页
          </span>
          <button
            @click="changePage(pagination.page + 1)"
            :disabled="pagination.last"
            class="px-4 py-2 text-sm border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700 dark:border-gray-600 dark:text-gray-300"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Search,
  RefreshCw,
  Plus,
  Store,
  Edit,
  Eye,
  CheckCircle,
  PauseCircle,
  Trash2,
  CheckSquare,
  X
} from 'lucide-vue-next'
import { useChannelStore } from '@/stores/channel'
import ChannelStatusBadge from './ChannelStatusBadge.vue'
import type { Channel, ChannelStatistics, ChannelStatus } from '@/types/channel'
import { CHANNEL_TYPE_LABELS } from '@/types/channel'

interface Emits {
  (e: 'create'): void
  (e: 'edit', channel: Channel): void
  (e: 'view', channel: Channel): void
  (e: 'activate', channel: Channel): void
  (e: 'deactivate', channel: Channel): void
  (e: 'delete', channel: Channel): void
  (e: 'batchUpdate', channels: number[], action: 'activate' | 'deactivate'): void
}

const emit = defineEmits<Emits>()

const channelStore = useChannelStore()

// 响应式数据
const searchKeyword = ref('')
const selectedStatus = ref<ChannelStatus | ''>('')
const debounceTimer = ref<number | null>(null)
const selectedChannels = ref<number[]>([])
const batchLoading = ref(false)

// 计算属性
const loading = computed(() => channelStore.loading)
const channels = computed(() => channelStore.channels)
const pagination = computed(() => channelStore.pagination)
const statistics = computed(() => channelStore.statistics)

// 工具函数
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(amount)
}

const getChannelTypeLabel = (type: string) => {
  return CHANNEL_TYPE_LABELS[type as keyof typeof CHANNEL_TYPE_LABELS] || type
}

// 防抖搜索
const debouncedSearch = () => {
  if (debounceTimer.value) {
    clearTimeout(debounceTimer.value)
  }
  debounceTimer.value = setTimeout(() => {
    channelStore.updateQueryParams({
      keyword: searchKeyword.value,
      page: 0 // 重置到第一页
    })
    loadChannels()
  }, 300)
}

// 状态筛选
const handleStatusFilter = () => {
  channelStore.updateQueryParams({
    status: selectedStatus.value || undefined,
    page: 0 // 重置到第一页
  })
  loadChannels()
}

// 加载渠道列表
const loadChannels = async () => {
  try {
    await channelStore.fetchChannels()
  } catch (error) {
    console.error('加载渠道列表失败:', error)
  }
}

// 刷新列表
const refreshList = async () => {
  await Promise.all([
    loadChannels(),
    loadStatistics()
  ])
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    await channelStore.fetchStatistics()
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 分页切换
const changePage = (page: number) => {
  if (page >= 0 && page < pagination.value.totalPages) {
    channelStore.updateQueryParams({ page })
    loadChannels()
  }
}

// 选择管理
const toggleChannelSelection = (channelId: number) => {
  const index = selectedChannels.value.indexOf(channelId)
  if (index === -1) {
    selectedChannels.value.push(channelId)
  } else {
    selectedChannels.value.splice(index, 1)
  }
}

const clearSelection = () => {
  selectedChannels.value = []
}

// 批量操作
const batchActivate = async () => {
  if (selectedChannels.value.length === 0) return

  try {
    batchLoading.value = true
    await channelStore.batchUpdateStatus({
      channelIds: selectedChannels.value,
      targetStatus: 'ACTIVE'
    })
    emit('batchUpdate', selectedChannels.value, 'activate')
    clearSelection()
  } catch (error) {
    console.error('批量激活失败:', error)
  } finally {
    batchLoading.value = false
  }
}

const batchDeactivate = async () => {
  if (selectedChannels.value.length === 0) return

  try {
    batchLoading.value = true
    await channelStore.batchUpdateStatus({
      channelIds: selectedChannels.value,
      targetStatus: 'INACTIVE'
    })
    emit('batchUpdate', selectedChannels.value, 'deactivate')
    clearSelection()
  } catch (error) {
    console.error('批量停用失败:', error)
  } finally {
    batchLoading.value = false
  }
}

// 监听查询参数变化
watch(
  () => channelStore.queryParams,
  () => {
    searchKeyword.value = channelStore.queryParams.keyword || ''
    selectedStatus.value = channelStore.queryParams.status || ''
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  refreshList()
})
</script>