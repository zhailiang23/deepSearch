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
            </div>
            <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20 mb-2"></div>
            <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-full mb-4"></div>
            <div class="flex justify-between items-center">
              <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-24"></div>
              <div class="flex space-x-2">
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
            <!-- 头部：名称 -->
            <div class="flex items-start justify-between mb-4">
              <div class="flex-1 min-w-0">
                <h4 class="text-lg font-medium text-gray-900 dark:text-white truncate">
                  {{ channel.name }}
                </h4>
              </div>
            </div>

            <!-- 渠道信息 -->
            <div class="mb-4 space-y-2">
              <div class="text-sm text-gray-600 dark:text-gray-400">
                <span class="font-medium">代码:</span> {{ channel.code }}
              </div>
              <div v-if="channel.description" class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
                <span class="font-medium">描述:</span> {{ channel.description }}
              </div>
            </div>

            <!-- 底部：时间和操作按钮 -->
            <div class="flex items-center justify-between">
              <div class="text-xs text-gray-500 dark:text-gray-400">
                创建于 {{ formatDate(channel.createdAt) }}
              </div>
              <div class="flex items-center space-x-1">
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
  Trash2
} from 'lucide-vue-next'
import { useChannelStore } from '@/stores/channel'
import type { Channel } from '@/types/channel'

interface Emits {
  (e: 'create'): void
  (e: 'edit', channel: Channel): void
  (e: 'view', channel: Channel): void
  (e: 'delete', channel: Channel): void
}

const emit = defineEmits<Emits>()

const channelStore = useChannelStore()

// 响应式数据
const searchKeyword = ref('')
const debounceTimer = ref<number | null>(null)

// 计算属性
const loading = computed(() => channelStore.loading)
const channels = computed(() => channelStore.channels)
const pagination = computed(() => channelStore.pagination)

// 工具函数
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
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
  await loadChannels()
}

// 分页切换
const changePage = (page: number) => {
  if (page >= 0 && page < pagination.value.totalPages) {
    channelStore.updateQueryParams({ page })
    loadChannels()
  }
}

// 监听查询参数变化
watch(
  () => channelStore.queryParams,
  () => {
    searchKeyword.value = channelStore.queryParams.keyword || ''
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  refreshList()
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>