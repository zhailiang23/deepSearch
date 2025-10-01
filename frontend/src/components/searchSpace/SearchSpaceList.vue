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
          class="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed dark:bg-gray-800 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-700"
        >
          <RefreshCw :class="['h-4 w-4 mr-2', { 'animate-spin': loading }]" />
          刷新
        </button>
        <button
          @click="$emit('create')"
          class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
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

    <!-- 卡片网格 -->
    <div class="space-y-6">
      <!-- 标题 -->
      <div class="flex items-center justify-between">
        <h3 class="text-lg leading-6 font-medium text-gray-900 dark:text-white">
          搜索空间列表
        </h3>
        <div v-if="!loading && searchSpaces.length" class="text-sm text-gray-500 dark:text-gray-400">
          共 {{ pagination.totalElements }} 个搜索空间
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
      <div v-else-if="!searchSpaces.length" class="text-center py-12">
        <div class="text-gray-500 dark:text-gray-400">
          <Database class="mx-auto h-12 w-12 mb-4 opacity-50" />
          <p class="text-lg font-medium mb-2">暂无搜索空间</p>
          <p class="text-sm">点击"新建"按钮创建第一个搜索空间</p>
        </div>
      </div>

      <!-- 搜索空间卡片 -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="space in searchSpaces"
          :key="space.id"
          class="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 hover:shadow-lg transition-shadow duration-200"
        >
          <div class="p-6">
            <!-- 头部：名称和状态 -->
            <div class="flex items-start justify-between mb-4">
              <div class="flex-1 min-w-0">
                <h4 class="text-lg font-medium text-gray-900 dark:text-white truncate">
                  {{ space.name }}
                </h4>
              </div>
              <SearchSpaceStatusBadge :status="space.status" class="ml-4 flex-shrink-0" />
            </div>

            <!-- 索引状态 -->
            <div v-if="space.indexStatus" class="mb-4 p-3 bg-gray-50 dark:bg-gray-700 rounded-md">
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm font-medium text-gray-700 dark:text-gray-300">索引状态</span>
                <div class="flex items-center space-x-2">
                  <div
                    :class="[
                      'w-2 h-2 rounded-full',
                      space.indexStatus.exists
                        ? space.indexStatus.health === 'green'
                          ? 'bg-green-500'
                          : space.indexStatus.health === 'yellow'
                          ? 'bg-yellow-500'
                          : 'bg-red-500'
                        : 'bg-gray-400'
                    ]"
                  ></div>
                  <span class="text-xs text-gray-500 dark:text-gray-400">
                    {{ space.indexStatus.exists ? space.indexStatus.health : '不存在' }}
                  </span>
                </div>
              </div>
              <div class="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
                <span>索引名: {{ space.indexStatus.name }}</span>
                <span v-if="space.indexStatus.exists && space.indexStatus.docsCount !== undefined">
                  {{ space.indexStatus.docsCount.toLocaleString() }} 文档
                </span>
              </div>
              <div v-if="space.indexStatus.error" class="text-xs text-red-600 dark:text-red-400 mt-1">
                错误: {{ space.indexStatus.error }}
              </div>
            </div>

            <!-- 底部：创建时间和操作按钮 -->
            <div class="flex items-center justify-between">
              <div class="text-xs text-gray-500 dark:text-gray-400">
                创建于 {{ formatDate(space.createdAt) }}
              </div>
              <div class="flex items-center space-x-1">
                <button
                  v-if="space.status === 'ACTIVE'"
                  @click="$emit('disable', space)"
                  class="p-2 text-yellow-600 hover:text-yellow-900 dark:text-yellow-400 dark:hover:text-yellow-300 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 rounded-md transition-colors"
                  title="禁用"
                >
                  <PauseCircle class="h-4 w-4" />
                </button>
                <button
                  v-else-if="space.status === 'INACTIVE'"
                  @click="$emit('enable', space)"
                  class="p-2 text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-md transition-colors"
                  title="启用"
                >
                  <PlayCircle class="h-4 w-4" />
                </button>
                
                <!-- 文件导入对话框 -->
                <JsonImportDialog
                  v-model:open="importDialogOpen"
                  :search-space="selectedSpace"
                  @import-complete="handleImportComplete"
                />
                <button
                  class="p-2 text-purple-600 hover:text-purple-900 dark:text-purple-400 dark:hover:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-900/20 rounded-md transition-colors"
                  title="导入数据"
                  @click="openImportDialog(space)"
                >
                  <Upload class="h-4 w-4" />
                </button>

                <button
                  @click="$emit('config', space)"
                  class="p-2 text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors"
                  title="配置"
                >
                  <Settings class="h-4 w-4" />
                </button>
                <button
                  @click="confirmDelete(space)"
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

      <!-- 删除确认弹窗 -->
      <div
        v-if="deleteDialogOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
        @click.self="deleteDialogOpen = false"
      >
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4">
          <div class="p-6">
            <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-4">
              确认删除搜索空间
            </h3>
            <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">
              确定要删除搜索空间 "<span class="font-medium text-gray-900 dark:text-white">{{ spaceToDelete?.name }}</span>" 吗？
              <br><br>
              <span class="text-red-600 dark:text-red-400">此操作将同时删除关联的ES索引及其所有数据，且无法恢复！</span>
            </p>
            <div class="flex justify-end space-x-3">
              <button
                @click="deleteDialogOpen = false"
                class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
              >
                取消
              </button>
              <button
                @click="handleDelete"
                :disabled="deleting"
                class="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {{ deleting ? '删除中...' : '确认删除' }}
              </button>
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
  Database,
  Settings,
  PlayCircle,
  PauseCircle,
  Upload,
  Trash2
} from 'lucide-vue-next'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import SearchSpaceStatusBadge from './SearchSpaceStatusBadge.vue'
import JsonImportDialog from './JsonImportDialog.vue'
import type { SearchSpace } from '@/types/searchSpace'

interface Emits {
  (e: 'create'): void
  (e: 'config', space: SearchSpace): void
  (e: 'enable', space: SearchSpace): void
  (e: 'disable', space: SearchSpace): void
  (e: 'import', space: SearchSpace): void
  (e: 'delete', space: SearchSpace): void
}

const emit = defineEmits<Emits>()

const searchSpaceStore = useSearchSpaceStore()

// 响应式数据
const searchKeyword = ref('')
const debounceTimer = ref<number | null>(null)

// 文件上传相关状态
const importDialogOpen = ref(false)
const selectedSpace = ref<SearchSpace | null>(null)

// 删除相关状态
const deleteDialogOpen = ref(false)
const spaceToDelete = ref<SearchSpace | null>(null)
const deleting = ref(false)

// 打开导入对话框
const openImportDialog = (space: SearchSpace) => {
  selectedSpace.value = space
  importDialogOpen.value = true
}

// 处理导入完成
const handleImportComplete = (result: any) => {
  console.log('导入完成:', result)
  // 刷新列表以显示最新数据
  refreshList()
}

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

// 确认删除
const confirmDelete = (space: SearchSpace) => {
  spaceToDelete.value = space
  deleteDialogOpen.value = true
}

// 处理删除
const handleDelete = async () => {
  if (!spaceToDelete.value) return

  deleting.value = true
  try {
    await searchSpaceStore.deleteSearchSpace(spaceToDelete.value.id)
    deleteDialogOpen.value = false
    spaceToDelete.value = null
    // 刷新列表
    await refreshList()
  } catch (error) {
    console.error('删除搜索空间失败:', error)
  } finally {
    deleting.value = false
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