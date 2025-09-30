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
            placeholder="搜索话题名称..."
            class="w-full pl-10 pr-4 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground"
            @input="debouncedSearch"
          />
        </div>
      </div>
      <div class="flex items-center space-x-3">
        <select
          v-model="visibleFilter"
          @change="handleFilterChange"
          class="px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground"
        >
          <option value="">全部状态</option>
          <option value="true">可见</option>
          <option value="false">隐藏</option>
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
          新建话题
        </button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="border rounded-lg overflow-hidden bg-background">
      <!-- 表格头部 -->
      <div class="bg-muted px-6 py-3 border-b border-border">
        <div class="flex items-center justify-between">
          <div>
            <div v-if="!loading && hotTopics.length" class="text-sm text-muted-foreground mt-1">
              共 {{ pagination.totalElements }} 个话题
            </div>
          </div>
          <!-- 统计信息 -->
          <div v-if="props.statistics" class="flex items-center space-x-6 text-sm">
            <div class="flex items-center">
              <span class="w-3 h-3 bg-green-500 rounded-full mr-2"></span>
              <span class="text-muted-foreground">可见: </span>
              <span class="font-medium text-foreground ml-1">{{ props.statistics.visibleTopics }}</span>
            </div>
            <div class="flex items-center">
              <span class="w-3 h-3 bg-gray-400 rounded-full mr-2"></span>
              <span class="text-muted-foreground">隐藏: </span>
              <span class="font-medium text-foreground ml-1">{{ props.statistics.invisibleTopics }}</span>
            </div>
            <div class="flex items-center">
              <TrendingUp class="h-4 w-4 text-orange-500 mr-2" />
              <span class="text-muted-foreground">平均热度: </span>
              <span class="font-medium text-foreground ml-1">{{ Math.round(props.statistics.avgPopularity) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="p-6">
        <div class="space-y-4">
          <div v-for="n in 5" :key="`skeleton-${n}`" class="animate-pulse">
            <div class="flex items-center space-x-4">
              <div class="h-4 bg-muted rounded w-1/4"></div>
              <div class="h-4 bg-muted rounded w-1/6"></div>
              <div class="h-4 bg-muted rounded w-1/6"></div>
              <div class="h-4 bg-muted rounded w-1/4"></div>
              <div class="h-4 bg-muted rounded w-1/6"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!hotTopics.length" class="text-center py-12">
        <div class="text-muted-foreground">
          <Hash class="mx-auto h-12 w-12 mb-4 opacity-50" />
          <p class="text-lg font-medium mb-2">暂无话题</p>
          <p class="text-sm">点击"新建话题"按钮创建第一个热门话题</p>
        </div>
      </div>

      <!-- 表格内容 -->
      <div v-else class="overflow-x-auto">
        <table class="min-w-full divide-y divide-border">
          <thead class="bg-muted/50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                话题名称
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                热度
                <button @click="toggleSort" class="ml-1 hover:text-foreground">
                  <ArrowUpDown class="h-3 w-3" />
                </button>
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                状态
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                创建时间
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody class="bg-background divide-y divide-border">
            <tr
              v-for="topic in hotTopics"
              :key="topic.id"
              class="hover:bg-accent/50 transition-colors"
            >
              <!-- 话题名称 -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div class="text-sm font-medium text-foreground">
                    {{ topic.name }}
                  </div>
                </div>
              </td>

              <!-- 热度 -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <TrendingUp class="h-4 w-4 mr-2 text-orange-500" />
                  <span class="text-sm font-medium text-foreground">{{ formatPopularity(topic.popularity) }}</span>
                </div>
              </td>

              <!-- 状态 -->
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  :class="[
                    'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                    topic.visible
                      ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
                      : 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400'
                  ]"
                >
                  <span :class="[
                    'w-1.5 h-1.5 rounded-full mr-1.5',
                    topic.visible ? 'bg-green-400' : 'bg-gray-400'
                  ]"></span>
                  {{ topic.visible ? '可见' : '隐藏' }}
                </span>
              </td>

              <!-- 创建时间 -->
              <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">
                {{ formatDate(topic.createdAt) }}
              </td>

              <!-- 操作 -->
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex items-center space-x-2">
                  <button
                    @click="$emit('edit', topic)"
                    class="p-1 text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors"
                    title="编辑"
                  >
                    <Edit class="h-4 w-4" />
                  </button>
                  <button
                    @click="handleToggleVisibility(topic)"
                    :class="[
                      'p-1 rounded transition-colors',
                      topic.visible
                        ? 'text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-900/20'
                        : 'text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300 hover:bg-green-50 dark:hover:bg-green-900/20'
                    ]"
                    :title="topic.visible ? '隐藏' : '显示'"
                  >
                    <EyeOff v-if="topic.visible" class="h-4 w-4" />
                    <Eye v-else class="h-4 w-4" />
                  </button>
                  <button
                    @click="$emit('delete', topic)"
                    class="p-1 text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
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
      <div v-if="pagination.totalPages > 1" class="bg-muted/30 px-6 py-3 border-t border-border">
        <div class="flex items-center justify-between">
          <div class="text-sm text-muted-foreground">
            显示 {{ pagination.page * pagination.size + 1 }} 到
            {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }}
            共 {{ pagination.totalElements }} 条记录
          </div>
          <div class="flex items-center space-x-2">
            <button
              @click="changePage(pagination.page - 1)"
              :disabled="pagination.first"
              class="px-4 py-2 text-sm border border-border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-accent dark:border-gray-600 dark:text-gray-300"
            >
              上一页
            </button>
            <span class="text-sm text-muted-foreground">
              第 {{ pagination.page + 1 }} 页，共 {{ pagination.totalPages }} 页
            </span>
            <button
              @click="changePage(pagination.page + 1)"
              :disabled="pagination.last"
              class="px-4 py-2 text-sm border border-border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-accent dark:border-gray-600 dark:text-gray-300"
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
  Hash,
  ArrowUpDown,
  TrendingUp,
  Edit,
  Eye,
  EyeOff,
  ExternalLink,
  Trash2
} from 'lucide-vue-next'
import { useHotTopicStore } from '@/stores/hotTopic'
import type { HotTopic, HotTopicStatistics } from '@/types/hotTopic'

interface Props {
  statistics?: HotTopicStatistics | null
}

interface Emits {
  (e: 'create'): void
  (e: 'edit', topic: HotTopic): void
  (e: 'view', topic: HotTopic): void
  (e: 'delete', topic: HotTopic): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const hotTopicStore = useHotTopicStore()

// 响应式数据
const searchKeyword = ref('')
const visibleFilter = ref('')
const debounceTimer = ref<number | null>(null)

// 计算属性
const loading = computed(() => hotTopicStore.loading)
const hotTopics = computed(() => hotTopicStore.hotTopics)
const pagination = computed(() => hotTopicStore.pagination)

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

const formatPopularity = (popularity: number) => {
  if (popularity >= 10000) {
    return `${(popularity / 10000).toFixed(1)}万`
  } else if (popularity >= 1000) {
    return `${(popularity / 1000).toFixed(1)}k`
  }
  return popularity.toString()
}

// 防抖搜索
const debouncedSearch = () => {
  if (debounceTimer.value) {
    clearTimeout(debounceTimer.value)
  }
  debounceTimer.value = setTimeout(() => {
    hotTopicStore.updateQueryParams({
      keyword: searchKeyword.value,
      page: 0 // 重置到第一页
    })
    loadHotTopics()
  }, 300)
}

// 过滤器变化处理
const handleFilterChange = () => {
  const visible = visibleFilter.value === '' ? undefined : visibleFilter.value === 'true'
  hotTopicStore.updateQueryParams({
    visible,
    page: 0 // 重置到第一页
  })
  loadHotTopics()
}

// 切换排序
const toggleSort = () => {
  const currentDirection = hotTopicStore.queryParams.direction
  const newDirection = currentDirection === 'DESC' ? 'ASC' : 'DESC'

  hotTopicStore.updateQueryParams({
    direction: newDirection,
    page: 0 // 重置到第一页
  })
  loadHotTopics()
}

// 加载热门话题列表
const loadHotTopics = async () => {
  try {
    await hotTopicStore.fetchHotTopics()
  } catch (error) {
    console.error('加载热门话题列表失败:', error)
  }
}

// 刷新列表
const refreshList = async () => {
  await loadHotTopics()
}

// 分页切换
const changePage = (page: number) => {
  if (page >= 0 && page < pagination.value.totalPages) {
    hotTopicStore.updateQueryParams({ page })
    loadHotTopics()
  }
}

// 切换可见性
const handleToggleVisibility = async (topic: HotTopic) => {
  try {
    await hotTopicStore.toggleVisibility(topic.id)
  } catch (error) {
    console.error('切换可见性失败:', error)
  }
}

// 监听查询参数变化
watch(
  () => hotTopicStore.queryParams,
  () => {
    searchKeyword.value = hotTopicStore.queryParams.keyword || ''
    const visible = hotTopicStore.queryParams.visible
    visibleFilter.value = visible === undefined ? '' : visible.toString()
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