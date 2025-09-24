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
                <DialogRoot v-model:open="importDialogOpen">
                  <DialogTrigger as-child>
                    <button
                      class="p-2 text-purple-600 hover:text-purple-900 dark:text-purple-400 dark:hover:text-purple-300 hover:bg-purple-50 dark:hover:bg-purple-900/20 rounded-md transition-colors"
                      title="导入数据"
                      @click="selectedSpace = space"
                    >
                      <Upload class="h-4 w-4" />
                    </button>
                  </DialogTrigger>
                  
                  <DialogPortal>
                    <DialogOverlay class="fixed inset-0 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
                    <DialogContent class="fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-white p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] rounded-lg dark:bg-gray-800 dark:border-gray-700">
                      <div class="flex flex-col space-y-1.5 text-center sm:text-left">
                        <DialogTitle class="text-lg font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
                          导入数据到 "{{ selectedSpace?.name }}"
                        </DialogTitle>
                        <DialogDescription class="text-sm text-gray-500 dark:text-gray-400">
                          选择JSON文件上传数据到搜索空间。文件大小限制为20MB。
                        </DialogDescription>
                      </div>
                      
                      <!-- 文件选择区域 -->
                      <div class="grid gap-4 py-4">
                        <div class="space-y-4">
                          <!-- 文件输入 -->
                          <div class="flex flex-col space-y-2">
                            <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                              选择文件
                            </label>
                            <div class="relative">
                              <input
                                ref="fileInput"
                                type="file"
                                accept=".json,application/json"
                                class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-green-50 file:text-green-700 hover:file:bg-green-100 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:text-gray-400 dark:file:bg-green-900/20 dark:file:text-green-400 dark:hover:file:bg-green-900/30"
                                @change="handleFileSelect"
                              />
                            </div>
                            <p class="text-xs text-gray-500 dark:text-gray-400">
                              支持JSON格式，最大20MB
                            </p>
                          </div>
                          
                          <!-- 文件信息预览 -->
                          <div v-if="selectedFile" class="p-3 bg-gray-50 dark:bg-gray-700 rounded-md">
                            <div class="flex items-start justify-between">
                              <div class="flex-1 min-w-0">
                                <p class="text-sm font-medium text-gray-900 dark:text-white truncate">
                                  {{ selectedFile.name }}
                                </p>
                                <p class="text-xs text-gray-500 dark:text-gray-400">
                                  {{ formatFileSize(selectedFile.size) }}
                                </p>
                                <p class="text-xs text-gray-500 dark:text-gray-400">
                                  修改时间: {{ formatDate(new Date(selectedFile.lastModified).toISOString()) }}
                                </p>
                              </div>
                              <button
                                @click="clearFileSelection"
                                class="ml-2 p-1 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                                title="清除选择"
                              >
                                <X class="h-4 w-4" />
                              </button>
                            </div>
                          </div>
                          
                          <!-- 错误信息 -->
                          <div v-if="fileError" class="p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
                            <div class="flex">
                              <AlertCircle class="h-4 w-4 text-red-400" />
                              <div class="ml-2">
                                <p class="text-sm text-red-800 dark:text-red-400">
                                  {{ fileError }}
                                </p>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                      
                      <!-- 底部按钮 -->
                      <div class="flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2">
                        <DialogClose as-child>
                          <button
                            class="mt-3 sm:mt-0 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-medium text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:w-auto dark:bg-gray-700 dark:text-gray-300 dark:ring-gray-600 dark:hover:bg-gray-600"
                            @click="closeImportDialog"
                          >
                            取消
                          </button>
                        </DialogClose>
                        <button
                          :disabled="!selectedFile || uploadLoading"
                          @click="handleImportFile"
                          class="inline-flex w-full justify-center rounded-md bg-green-600 px-3 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green-600 sm:w-auto disabled:opacity-50 disabled:cursor-not-allowed dark:bg-green-700 dark:hover:bg-green-600"
                        >
                          <span v-if="uploadLoading" class="flex items-center">
                            <RefreshCw class="h-4 w-4 mr-2 animate-spin" />
                            上传中...
                          </span>
                          <span v-else>继续</span>
                        </button>
                      </div>
                    </DialogContent>
                  </DialogPortal>
                </DialogRoot>

                <button
                  @click="$emit('edit', space)"
                  class="p-2 text-blue-600 hover:text-blue-900 dark:text-blue-400 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors"
                  title="编辑"
                >
                  <Edit class="h-4 w-4" />
                </button>
                <button
                  @click="$emit('view', space)"
                  class="p-2 text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-900/20 rounded-md transition-colors"
                  title="查看详情"
                >
                  <Eye class="h-4 w-4" />
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
  Database,
  Edit,
  Eye,
  PlayCircle,
  PauseCircle,
  Upload,
  X,
  AlertCircle
} from 'lucide-vue-next'
import {
  DialogRoot,
  DialogTrigger,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogClose
} from 'reka-ui'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import SearchSpaceStatusBadge from './SearchSpaceStatusBadge.vue'
import type { SearchSpace, SearchSpaceStatistics } from '@/types/searchSpace'

interface Emits {
  (e: 'create'): void
  (e: 'edit', space: SearchSpace): void
  (e: 'view', space: SearchSpace): void
  (e: 'enable', space: SearchSpace): void
  (e: 'disable', space: SearchSpace): void
  (e: 'import', space: SearchSpace): void
}

const emit = defineEmits<Emits>()

const searchSpaceStore = useSearchSpaceStore()

// 响应式数据
const searchKeyword = ref('')
const debounceTimer = ref<number | null>(null)

// 文件上传相关状态
const importDialogOpen = ref(false)
const selectedSpace = ref<SearchSpace | null>(null)
const selectedFile = ref<File | null>(null)
const fileError = ref('')
const uploadLoading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

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

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

// 文件选择处理
const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  
  // 清除之前的错误
  fileError.value = ''
  
  if (!file) {
    selectedFile.value = null
    return
  }
  
  // 文件类型验证
  if (!file.type.includes('json') && !file.name.toLowerCase().endsWith('.json')) {
    fileError.value = '请选择JSON格式的文件'
    selectedFile.value = null
    input.value = ''
    return
  }
  
  // 文件大小验证 (20MB = 20 * 1024 * 1024 bytes)
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    fileError.value = '文件大小不能超过20MB'
    selectedFile.value = null
    input.value = ''
    return
  }
  
  selectedFile.value = file
}

// 清除文件选择
const clearFileSelection = () => {
  selectedFile.value = null
  fileError.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 处理文件导入
const handleImportFile = async () => {
  if (!selectedFile.value || !selectedSpace.value) return
  
  try {
    uploadLoading.value = true
    
    // 首先读取文件内容进行JSON验证
    const fileContent = await readFileAsText(selectedFile.value)
    
    try {
      JSON.parse(fileContent)
    } catch (error) {
      fileError.value = '文件内容不是有效的JSON格式'
      return
    }
    
    // 触发导入事件，传递选定的空间和文件
    emit('import', selectedSpace.value)
    
    // 关闭对话框并重置状态
    closeImportDialog()
    
    // 这里暂时只是演示，实际的文件上传逻辑应该在父组件中处理
    // 或者集成到store中
    
  } catch (error) {
    console.error('文件导入失败:', error)
    fileError.value = '文件读取失败，请重试'
  } finally {
    uploadLoading.value = false
  }
}

// 读取文件内容
const readFileAsText = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const result = e.target?.result
      if (typeof result === 'string') {
        resolve(result)
      } else {
        reject(new Error('Failed to read file as text'))
      }
    }
    reader.onerror = () => reject(new Error('Failed to read file'))
    reader.readAsText(file)
  })
}

// 关闭导入对话框
const closeImportDialog = () => {
  importDialogOpen.value = false
  selectedSpace.value = null
  clearFileSelection()
  uploadLoading.value = false
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