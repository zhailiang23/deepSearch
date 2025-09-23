<template>
  <div class="space-y-6">
    <!-- 头部工具栏 -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div class="flex-1 max-w-md">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索搜索空间..."
            class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white dark:placeholder-gray-400"
            @input="debouncedSearch"
          />
        </div>
      </div>
      <div class="flex items-center space-x-3">
        <button
          @click="refreshList"
          :disabled="loading"
          class="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed dark:bg-gray-800 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-700"
        >
          <RefreshCw :class="['h-4 w-4 mr-2', { 'animate-spin': loading }]" />
          刷新
        </button>
        <button
          @click="$emit('create')"
          class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        >
          <Plus class="h-4 w-4 mr-2" />
          新建
        </button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="statistics" class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="bg-white dark:bg-gray-800 p-4 rounded-lg shadow border border-gray-200 dark:border-gray-700">
        <div class="text-2xl font-bold text-gray-900 dark:text-white">{{ statistics.totalSpaces }}</div>
        <div class="text-sm text-gray-500 dark:text-gray-400">总计</div>
      </div>
      <div class="bg-white dark:bg-gray-800 p-4 rounded-lg shadow border border-gray-200 dark:border-gray-700">
        <div class="text-2xl font-bold text-green-600">{{ statistics.activeSpaces }}</div>
        <div class="text-sm text-gray-500 dark:text-gray-400">活跃</div>
      </div>
      <div class="bg-white dark:bg-gray-800 p-4 rounded-lg shadow border border-gray-200 dark:border-gray-700">
        <div class="text-2xl font-bold text-gray-600">{{ statistics.inactiveSpaces }}</div>
        <div class="text-sm text-gray-500 dark:text-gray-400">停用</div>
      </div>
      <div class="bg-white dark:bg-gray-800 p-4 rounded-lg shadow border border-gray-200 dark:border-gray-700">
        <div class="text-2xl font-bold text-yellow-600">{{ statistics.maintenanceSpaces }}</div>
        <div class="text-sm text-gray-500 dark:text-gray-400">维护中</div>
      </div>
    </div>

    <!-- 列表 -->
    <div class="bg-white dark:bg-gray-800 shadow rounded-lg border border-gray-200 dark:border-gray-700">
      <!-- 表头 -->
      <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
        <h3 class="text-lg leading-6 font-medium text-gray-900 dark:text-white">
          搜索空间列表
        </h3>
      </div>

      <!-- 表格 -->
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
          <thead class="bg-gray-50 dark:bg-gray-700">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                名称/代码
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                描述
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                状态
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                创建时间
              </th>
              <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
            <tr v-if="loading" v-for="n in 5" :key="`skeleton-${n}`">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="animate-pulse">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-24 mb-2"></div>
                  <div class="h-3 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
                </div>
              </td>
              <td class="px-6 py-4">
                <div class="animate-pulse">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-48"></div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="animate-pulse">
                  <div class="h-6 bg-gray-200 dark:bg-gray-700 rounded-full w-16"></div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="animate-pulse">
                  <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20"></div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right">
                <div class="animate-pulse flex justify-end space-x-2">
                  <div class="h-8 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
                  <div class="h-8 bg-gray-200 dark:bg-gray-700 rounded w-16"></div>
                </div>
              </td>
            </tr>

            <tr v-else-if="!searchSpaces.length">
              <td colspan="5" class="px-6 py-12 text-center">
                <div class="text-gray-500 dark:text-gray-400">
                  <Database class="mx-auto h-12 w-12 mb-4 opacity-50" />
                  <p class="text-lg font-medium mb-2">暂无搜索空间</p>
                  <p class="text-sm">点击"新建"按钮创建第一个搜索空间</p>
                </div>
              </td>
            </tr>

            <tr
              v-else
              v-for="space in searchSpaces"
              :key="space.id"
              class="hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              <td class="px-6 py-4 whitespace-nowrap">
                <div>
                  <div class="text-sm font-medium text-gray-900 dark:text-white">
                    {{ space.name }}
                  </div>
                  <div class="text-sm text-gray-500 dark:text-gray-400 font-mono">
                    {{ space.code }}
                  </div>
                </div>
              </td>
              <td class="px-6 py-4">
                <div class="text-sm text-gray-900 dark:text-white max-w-xs truncate">
                  {{ space.description || '无描述' }}
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <SearchSpaceStatusBadge :status="space.status" />
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                {{ formatDate(space.createdAt) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <div class="flex justify-end items-center space-x-2">
                  <button
                    v-if="space.status === 'ACTIVE'"
                    @click="$emit('disable', space)"
                    class="text-yellow-600 hover:text-yellow-900 dark:text-yellow-400 dark:hover:text-yellow-300 p-1 rounded"
                    title="禁用"
                  >
                    <PauseCircle class="h-4 w-4" />
                  </button>
                  <button
                    v-else-if="space.status === 'INACTIVE'"
                    @click="$emit('enable', space)"
                    class="text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 p-1 rounded"
                    title="启用"
                  >
                    <PlayCircle class="h-4 w-4" />
                  </button>
                  <button
                    @click="$emit('edit', space)"
                    class="text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 p-1 rounded"
                    title="编辑"
                  >
                    <Edit class="h-4 w-4" />
                  </button>
                  <button
                    @click="$emit('view', space)"
                    class="text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300 p-1 rounded"
                    title="查看详情"
                  >
                    <Eye class="h-4 w-4" />
                  </button>
                  <button
                    @click="$emit('delete', space)"
                    class="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 p-1 rounded"
                    title="删除"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.totalPages > 1" class="px-6 py-4 border-t border-gray-200 dark:border-gray-700">
        <div class="flex items-center justify-between">
          <div class="text-sm text-gray-700 dark:text-gray-300">
            显示 {{ pagination.page * pagination.size + 1 }} 到
            {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }}
            共 {{ pagination.totalElements }} 条记录
          </div>
          <div class="flex items-center space-x-2">
            <button
              @click="changePage(pagination.page - 1)"
              :disabled="pagination.first"
              class="px-3 py-1 text-sm border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700 dark:border-gray-600"
            >
              上一页
            </button>
            <span class="text-sm text-gray-700 dark:text-gray-300">
              第 {{ pagination.page + 1 }} 页，共 {{ pagination.totalPages }} 页
            </span>
            <button
              @click="changePage(pagination.page + 1)"
              :disabled="pagination.last"
              class="px-3 py-1 text-sm border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700 dark:border-gray-600"
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
import { ref, computed, onMounted, watch } from 'vue'
import {
  Search,
  RefreshCw,
  Plus,
  Database,
  Edit,
  Eye,
  Trash2,
  PlayCircle,
  PauseCircle
} from 'lucide-vue-next'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import SearchSpaceStatusBadge from './SearchSpaceStatusBadge.vue'
import type { SearchSpace, SearchSpaceStatistics } from '@/types/searchSpace'

interface Emits {
  (e: 'create'): void
  (e: 'edit', space: SearchSpace): void
  (e: 'view', space: SearchSpace): void
  (e: 'delete', space: SearchSpace): void
  (e: 'enable', space: SearchSpace): void
  (e: 'disable', space: SearchSpace): void
}

const emit = defineEmits<Emits>()

const searchSpaceStore = useSearchSpaceStore()

// 响应式数据
const searchKeyword = ref('')
const debounceTimer = ref<number | null>(null)

// 计算属性
const loading = computed(() => searchSpaceStore.loading)
const searchSpaces = computed(() => searchSpaceStore.searchSpaces)
const pagination = computed(() => searchSpaceStore.pagination)
const statistics = computed(() => searchSpaceStore.statistics)

// 格式化日期
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

// 防抖搜索
const debouncedSearch = () => {
  if (debounceTimer.value) {
    clearTimeout(debounceTimer.value)
  }
  debounceTimer.value = setTimeout(() => {
    searchSpaceStore.updateQueryParams({
      keyword: searchKeyword.value,
      page: 0 // 重置到第一页
    })
    loadSearchSpaces()
  }, 300)
}

// 加载搜索空间列表
const loadSearchSpaces = async () => {
  try {
    await searchSpaceStore.fetchSearchSpaces()
  } catch (error) {
    console.error('加载搜索空间列表失败:', error)
  }
}

// 刷新列表
const refreshList = async () => {
  await Promise.all([
    loadSearchSpaces(),
    loadStatistics()
  ])
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    await searchSpaceStore.fetchStatistics()
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 分页切换
const changePage = (page: number) => {
  if (page >= 0 && page < pagination.value.totalPages) {
    searchSpaceStore.updateQueryParams({ page })
    loadSearchSpaces()
  }
}

// 监听查询参数变化
watch(
  () => searchSpaceStore.queryParams,
  () => {
    searchKeyword.value = searchSpaceStore.queryParams.keyword || ''
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  refreshList()
})
</script>