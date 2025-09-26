<template>
  <div class="search-data-manage-page h-full bg-white flex flex-col min-w-0 overflow-hidden w-full max-w-full">

    <!-- 主要内容区域 -->
    <div class="flex-1 overflow-hidden w-full max-w-full">
      <div class="h-full p-6 overflow-y-auto w-full max-w-full">
        <div class="space-y-6 w-full max-w-full overflow-hidden">
      <!-- 搜索条件面板 -->
      <Card>
        <CardContent class="pt-6">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <!-- 搜索空间选择 -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-gray-700">搜索空间选择</label>
              <select
                v-model="currentIndex"
                @change="handleIndexChange"
                :disabled="loading"
                class="w-full h-10 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
              >
                <option value="">请选择搜索空间</option>
                <option
                  v-for="space in availableSearchSpaces"
                  :key="space.id"
                  :value="space.id"
                >
                  {{ space.name }}
                </option>
              </select>
            </div>
            
            <!-- 搜索关键词和按钮 -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-gray-700">搜索关键词</label>
              <div class="flex gap-2">
                <div class="relative flex-1">
                  <input
                    v-model="searchQuery"
                    type="text"
                    placeholder="输入关键词搜索..."
                    @keyup.enter="handleSearch"
                    :disabled="loading || !currentIndex"
                    class="w-full h-10 pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
                  >
                  <Search class="absolute left-3 top-2.5 w-4 h-4 text-gray-400" />
                </div>
                <Button
                  @click="handleSearch"
                  :disabled="loading || !currentIndex"
                  class="bg-emerald-600 hover:bg-emerald-700 text-white px-6 h-10"
                >
                  <Search class="w-4 h-4 mr-2" />
                  搜索
                </Button>
              </div>
            </div>

            <!-- 空白占位 -->
            <div></div>
          </div>
        </CardContent>
      </Card>


      <!-- 动态结果表格 -->
      <Card class="w-full max-w-full overflow-hidden min-w-0">
        <CardContent class="p-0 w-full max-w-full overflow-hidden">
          <!-- 表格滚动容器 -->
          <div v-if="currentIndex && indexMapping" class="w-full overflow-x-auto" style="max-width: 100%;">
            <div style="min-width: 800px; max-width: 1200px; width: 100%;">
          <DynamicResultsTable
            ref="tableRef"
            :data="searchResults.hits"
            :mapping="indexMapping"
            :loading="loading"
            :total-count="totalCount"
            :enable-virtual-scroll="false"
            :page-size="pageSize"
            :current-page="currentPage"
            :selected-rows="selectedRows"
            :search-term="searchQuery"
            @update:selectedRows="selectedRows = $event"
            @update:currentPage="currentPage = $event"
            @update:pageSize="handlePageSizeChange"
            @edit="handleRowEdit"
            @view="handleRowView"
            @delete="handleRowDelete"
            @batch-delete="handleBatchDelete"
            @sort-change="handleSortChange"
            @page-change="handlePageChange"
            @update-document="handleDocumentUpdate"
          />
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else-if="!loading" class="flex flex-col items-center justify-center py-16 text-gray-500">
            <Database class="w-16 h-16 mb-4 text-gray-300" />
            <h3 class="text-lg font-medium text-gray-900 mb-2">暂无数据</h3>
            <p class="text-gray-600">{{ currentIndex ? '请执行搜索查看结果' : '请选择搜索空间并执行搜索' }}</p>
          </div>

          <!-- 加载状态 -->
          <div v-else-if="loading" class="flex flex-col items-center justify-center py-16">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mb-4"></div>
            <p class="text-gray-600">{{ loadingText }}</p>
          </div>
        </CardContent>
      </Card>
        </div>
      </div>
    </div>


    <!-- 数据详情弹窗 - 完全禁用 -->
    <!-- 临时禁用此弹窗，直到查明自动触发原因 -->
    <!--
    <Dialog v-model:open="showDataDetail" v-if="!isInitializing">
      <DialogContent class="max-w-4xl max-h-[80vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle>数据详情</DialogTitle>
          <DialogDescription v-if="selectedRowData">
            ID: {{ selectedRowData._id }}
          </DialogDescription>
        </DialogHeader>

        <div v-if="selectedRowData" class="flex-1 overflow-auto">
          <pre class="text-sm bg-gray-50 p-4 rounded border">{{ JSON.stringify(selectedRowData._source, null, 2) }}</pre>
        </div>
      </DialogContent>
    </Dialog>
    -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from '@/components/ui/toast'
import http from '@/utils/http'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Database,
  Search,
  ChevronUp,
  ChevronDown,
  FileText,
  Clock,
  CheckSquare
} from 'lucide-vue-next'

// 组件导入
import DynamicResultsTable from '@/components/searchData/DynamicResultsTable.vue'

// 服务导入
import { searchDataService } from '@/services/searchDataService'

// 类型导入
import type { 
  TableColumn, 
  TableRow, 
  ESIndexMapping,
  SearchParams,
  SearchResults
} from '@/types/tableData'

// 响应式数据
const router = useRouter()
const { toast } = useToast()

// 表格引用
const tableRef = ref()

// 界面状态
const loading = ref(false)
const loadingText = ref('')
const showSearchPanel = ref(true)
const showAdvancedSearch = ref(false)
const showDataDetail = ref(false)

// 添加初始化防护
const isInitializing = ref(true)

// 搜索相关
const currentIndex = ref('')
const availableSearchSpaces = ref<Array<{id: number, name: string, description?: string}>>([])
const searchQuery = ref('')
const sortField = ref('')
const sortOrder = ref<'asc' | 'desc'>('desc')

// 计算当前搜索空间名称
const currentSearchSpaceName = computed(() => {
  if (!currentIndex.value) return ''
  const space = availableSearchSpaces.value.find(s => s.id.toString() === currentIndex.value)
  return space ? space.name : ''
})

// 表格相关
const searchResults = ref<SearchResults>({
  hits: [],
  total: 0,
  took: 0
})
const totalCount = computed(() => searchResults.value.total)
const currentPage = ref(1)
const pageSize = ref(20)
const selectedRows = ref<string[]>([])
const selectedRowData = ref<TableRow | null>(null)
const indexMapping = ref<ESIndexMapping>()
const realIndexName = ref('')
const visibleColumns = ref<TableColumn[]>([])

// 计算属性
const sortableColumns = computed(() => {
  return visibleColumns.value.filter(col => col.sortable)
})

// 生命周期
onMounted(async () => {
  // 强制重置所有弹窗状态
  showDataDetail.value = false
  selectedRowData.value = null

  await loadAvailableSearchSpaces()
  
  // 从路由参数恢复状态
  const { index, query } = router.currentRoute.value.query
  if (index && typeof index === 'string') {
    currentIndex.value = index
    await handleIndexChange()
  }
  if (query && typeof query === 'string') {
    searchQuery.value = query
  }
  
  // 延长初始化防护时间到5秒，确保页面完全加载完成
  setTimeout(() => {
    // 再次强制重置弹窗状态
    showDataDetail.value = false
    selectedRowData.value = null
    isInitializing.value = false
    console.log('SearchDataManagePage: 初始化完成，弹窗防护关闭')
  }, 5000)
})

// 监听索引变化
watch(currentIndex, async (newIndex) => {
  if (newIndex) {
    await loadIndexMapping()
    clearResults()
  }
})

// 方法定义
async function loadAvailableSearchSpaces() {
  try {
    console.log('开始加载搜索空间列表...')
    
    // 使用统一的http工具类，自动处理token认证
    const result = await http.get('/search-spaces', {
      params: {
        page: 0,
        size: 100
      }
    })
    
    console.log('搜索空间API响应:', result)
    
    // 处理后端返回的ApiResponse格式
    if (result.success && result.data && result.data.content) {
      availableSearchSpaces.value = result.data.content.map((space: any) => ({
        id: space.id,
        name: space.name,
        description: space.description
      }))
      console.log('搜索空间列表加载成功:', availableSearchSpaces.value)
    } else {
      console.warn('搜索空间API返回格式异常:', result)
      // 使用备用数据
      availableSearchSpaces.value = [
        { id: 1, name: '示例搜索空间1', description: '这是第一个示例搜索空间' },
        { id: 2, name: '示例搜索空间2', description: '这是第二个示例搜索空间' }
      ]
    }
  } catch (error) {
    console.error('加载搜索空间列表时发生错误:', error)
    
    // 显示错误提示
    toast({
      title: '加载搜索空间失败',
      description: '无法获取搜索空间列表，已使用默认数据',
      variant: 'destructive'
    })
    
    // 使用备用数据
    availableSearchSpaces.value = [
      { id: 1, name: '示例搜索空间1', description: '这是第一个示例搜索空间' },
      { id: 2, name: '示例搜索空间2', description: '这是第二个示例搜索空间' }
    ]
  }
}

async function loadIndexMapping() {
  if (!currentIndex.value) return

  try {
    loading.value = true
    loadingText.value = '加载字段映射...'

    console.log('获取索引映射:', currentIndex.value)

    // 调用后端API获取索引映射
    const response = await http.get(`/elasticsearch/mapping/${currentIndex.value}`)

    console.log('索引映射API响应:', response)

    if (response.success && response.data) {
      // 保存真实的索引名称
      realIndexName.value = response.data.index || ''

      // 直接使用后端返回的映射数据
      indexMapping.value = response.data.mapping || response.data

      // 生成初始可见列
      visibleColumns.value = generateInitialColumns()

      console.log('索引映射加载成功:', indexMapping.value)
      console.log('真实索引名称:', realIndexName.value)
      console.log('生成的可见列:', visibleColumns.value)
    } else {
      throw new Error(response.message || '获取索引映射失败')
    }
  } catch (error: any) {
    console.error('加载字段映射失败:', error)

    let errorMessage = '加载字段映射失败'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    toast({
      title: '错误',
      description: errorMessage,
      variant: 'destructive'
    })
  } finally {
    loading.value = false
  }
}

function generateInitialColumns(): TableColumn[] {
  if (!indexMapping.value) return []

  // 支持不同的映射结构
  let properties = indexMapping.value.properties ||
                   indexMapping.value.mappings?.properties ||
                   indexMapping.value.mappings

  if (!properties) return []

  const columns: TableColumn[] = []
  let visibleIndex = 0

  Object.entries(properties).forEach(([key, field]: [string, any]) => {
    // 过滤以下划线开头的字段
    if (key.startsWith('_')) {
      return
    }

    columns.push({
      key,
      label: key.charAt(0).toUpperCase() + key.slice(1),
      type: mapFieldType(field.type),
      sortable: ['keyword', 'number', 'date', 'boolean'].includes(field.type),
      filterable: true,
      visible: true, // 显示所有字段
      resizable: true,
      width: getDefaultWidth(field.type),
      esField: key,
      format: getFieldFormat(field)
    })

    visibleIndex++
  })

  return columns
}

function mapFieldType(esType: string): TableColumn['type'] {
  const typeMap: Record<string, TableColumn['type']> = {
    'text': 'text',
    'keyword': 'keyword',
    'date': 'date',
    'long': 'number',
    'integer': 'number',
    'double': 'number',
    'float': 'number',
    'boolean': 'boolean',
    'object': 'object',
    'nested': 'nested'
  }
  return typeMap[esType] || 'text'
}

function getDefaultWidth(type: string): number {
  const widthMap: Record<string, number> = {
    'text': 200,
    'keyword': 150,
    'date': 160,
    'number': 120,
    'boolean': 100,
    'object': 180,
    'nested': 200
  }
  return widthMap[type] || 150
}

function getFieldFormat(field: any): string | undefined {
  if (field.type === 'date' && field.format) {
    return field.format
  }
  return undefined
}

async function handleIndexChange() {
  if (currentIndex.value) {
    // 清空当前搜索结果
    clearResults()
    await loadIndexMapping()
    // 更新URL
    router.replace({
      query: {
        ...router.currentRoute.value.query,
        index: currentIndex.value
      }
    })
  }
}

async function handleSearch() {
  if (!currentIndex.value) {
    toast({
      title: '提示',
      description: '请先选择搜索空间',
      variant: 'default'
    })
    return
  }

  try {
    loading.value = true
    loadingText.value = '搜索中...'

    // 构建搜索请求参数
    const requestBody = {
      searchSpaceId: currentIndex.value,
      query: searchQuery.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
      sort: sortField.value ? {
        field: sortField.value,
        order: sortOrder.value
      } : undefined
    }

    console.log('发送搜索请求:', requestBody)

    // 调用后端搜索API
    const response = await http.post('/elasticsearch/search', requestBody)

    console.log('搜索API响应:', response)

    if (response.success && response.data) {
      const responseData = response.data

      // 转换响应数据格式以匹配前端期望的格式
      searchResults.value = {
        hits: responseData.data || [],
        total: responseData.total || 0,
        took: 0 // 后端响应中没有took字段，暂时设为0
      }

      // 如果响应中包含映射信息，更新索引映射
      if (responseData.mapping) {
        indexMapping.value = responseData.mapping
        visibleColumns.value = generateInitialColumns()
      }

      // 更新URL
      router.replace({
        query: {
          ...router.currentRoute.value.query,
          query: searchQuery.value
        }
      })

      toast({
        title: '搜索完成',
        description: `找到 ${searchResults.value.total} 条记录`,
        variant: 'default'
      })
    } else {
      throw new Error(response.message || '搜索请求失败')
    }
  } catch (error: any) {
    console.error('搜索失败:', error)

    let errorMessage = '请检查网络连接后重试'

    // 处理具体的错误信息
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    toast({
      title: '搜索失败',
      description: errorMessage,
      variant: 'destructive'
    })
  } finally {
    loading.value = false
  }
}

function generateMockData(): TableRow[] {
  const data: TableRow[] = []
  for (let i = 0; i < pageSize.value; i++) {
    data.push({
      _id: `doc_${Date.now()}_${i}`,
      _score: Math.random(),
      _source: {
        title: `示例标题 ${i + 1}`,
        category: ['电子产品', '服装', '书籍', '家具'][i % 4],
        created_date: new Date(Date.now() - Math.random() * 365 * 24 * 60 * 60 * 1000).toISOString(),
        price: Math.random() * 1000,
        active: Math.random() > 0.5,
        metadata: {
          tags: ['tag1', 'tag2'],
          description: `这是第${i + 1}个示例项目的描述`
        }
      }
    })
  }
  return data
}


function toggleSearchPanel() {
  showSearchPanel.value = !showSearchPanel.value
}

function clearSelection() {
  selectedRows.value = []
}

function clearResults() {
  searchResults.value = {
    hits: [],
    total: 0,
    took: 0
  }
  selectedRows.value = []
  currentPage.value = 1
}

function handlePageSizeChange(newSize: number) {
  pageSize.value = newSize
  currentPage.value = 1
  handleSearch()
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  handleSearch()
}

function handleSortChange(field: string, order: 'asc' | 'desc') {
  sortField.value = field
  sortOrder.value = order
  currentPage.value = 1
  handleSearch()
}

function handleFieldsChange(fields: TableColumn[]) {
  visibleColumns.value = fields

  toast({
    title: '字段设置已更新',
    description: '表格显示已根据新的字段配置更新',
    variant: 'default'
  })
}

// 行操作处理 (预留接口)
function handleRowEdit(row: TableRow) {
  // TODO: Issue #40 - 编辑功能
  toast({
    title: '编辑功能',
    description: '数据编辑功能将在Issue #40中实现',
    variant: 'default'
  })
}

function handleRowView(row: TableRow) {
  // 完全禁用弹窗功能 - 临时解决方案
  console.log('handleRowView被调用，但已被禁用以防止自动弹窗:', row?._id)

  // TODO: 需要进一步调查什么在自动触发这个函数
  // 暂时完全禁用弹窗功能
  return
}

function handleRowDelete(row: TableRow) {
  console.log('SearchDataManagePage.handleRowDelete 收到删除请求:', row._id)
  handleDeleteDocument(row)
}

function handleBatchDelete(rows: TableRow[]) {
  handleDeleteDocuments(rows)
}

async function executeSearch() {
  await handleSearch()
}

async function handleDeleteDocument(document: TableRow) {
  console.log('SearchDataManagePage.handleDeleteDocument 开始处理删除:', document._id)

  if (!currentIndex.value || !realIndexName.value) {
    showErrorToast('请先选择搜索空间')
    return
  }

  try {
    console.log('删除文档:', {
      id: document._id,
      index: realIndexName.value,
      version: document._version
    })

    const response = await searchDataService.deleteDocument({
      id: document._id,
      index: realIndexName.value,
      version: document._version
    })

    console.log('删除响应:', response)
    console.log('响应数据:', response.data)
    console.log('删除结果:', response.data?.result)

    // 删除成功的判断条件：检查ApiResponse的data.result
    if (response.success && response.data?.result === 'deleted') {
      console.log('删除成功，准备通知子组件:', tableRef.value)

      // 移除本地数据
      const index = searchResults.value.hits.findIndex(item => item._id === document._id)
      if (index !== -1) {
        searchResults.value.hits.splice(index, 1)
        searchResults.value.total = Math.max(0, searchResults.value.total - 1)
      }

      // 先显示删除成功提示（在对话框关闭前显示，确保不被遮挡）
      showSuccessToast('数据删除成功')

      // 然后通知表格组件删除成功（关闭弹窗）
      if (tableRef.value?.handleDeleteSuccess) {
        console.log('调用子组件 handleDeleteSuccess')
        tableRef.value.handleDeleteSuccess()
      } else {
        console.error('子组件方法不存在:', tableRef.value)
      }
    } else if (response.success && response.data?.result === 'not_found') {
      showWarningToast('文档不存在或已被删除')
      // 刷新数据
      await executeSearch()
    } else {
      console.error('删除响应格式异常:', response)
      showErrorToast('删除操作异常，请重试')
    }
  } catch (error: any) {
    console.error('删除文档失败:', error)
    let errorMessage = '删除文档失败'
    
    if (error.response?.status === 404) {
      errorMessage = '文档不存在或已被删除'
      // 刷新数据
      await executeSearch()
    } else if (error.response?.status === 409) {
      errorMessage = '文档已被其他用户修改，请刷新后重试'
    } else if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    }
    
    showErrorToast(errorMessage)
    // 通知表格组件删除失败
    tableRef.value?.handleDeleteError(errorMessage)
  }
}

async function handleDeleteDocuments(documents: TableRow[]) {
  if (!currentIndex.value) {
    showErrorToast('请先选择索引')
    return
  }

  if (documents.length === 0) {
    return
  }

  try {
    // 使用批量操作API
    const operations = documents.map(doc => ({
      action: 'delete' as const,
      id: doc._id,
      index: currentIndex.value!,
      source: undefined
    }))

    const response = await searchDataService.bulkOperation(operations)
    
    let successCount = 0
    let errorCount = 0
    
    response.items.forEach((item: any, index: number) => {
      if (item.delete?.result === 'deleted') {
        successCount++
        // 移除本地数据
        const localIndex = searchResults.value.hits.findIndex(row => row._id === documents[index]._id)
        if (localIndex !== -1) {
          searchResults.value.hits.splice(localIndex, 1)
        }
      } else {
        errorCount++
      }
    })

    // 更新总数
    searchResults.value.total = Math.max(0, searchResults.value.total - successCount)

    if (successCount === documents.length) {
      showSuccessToast(`成功删除 ${successCount} 条记录`)
    } else if (successCount > 0) {
      showWarningToast(`成功删除 ${successCount} 条记录，${errorCount} 条记录删除失败`)
    } else {
      showErrorToast(`批量删除失败，所有记录删除失败`)
    }

    // 通知表格组件删除完成
    if (successCount > 0) {
      tableRef.value?.handleDeleteSuccess()
    }

  } catch (error: any) {
    console.error('批量删除失败:', error)
    let errorMessage = '批量删除失败'
    
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    }
    
    showErrorToast(errorMessage)
    // 通知表格组件删除失败
    tableRef.value?.handleDeleteError(errorMessage)
  }
}

function handleDocumentUpdate(document: TableRow) {
  // 更新本地数据
  const index = searchResults.value.hits.findIndex(row => row._id === document._id)
  if (index !== -1) {
    searchResults.value.hits[index] = { ...document }
  }
}

function showSuccessToast(message: string) {
  toast({
    title: '成功',
    description: message,
    variant: 'default'
  })
}

function showWarningToast(message: string) {
  toast({
    title: '警告',
    description: message,
    variant: 'default'
  })
}

function showErrorToast(message: string) {
  toast({
    title: '错误',
    description: message,
    variant: 'destructive'
  })
}
</script>

<style scoped>
/* 页面动画 */
.search-data-manage-page {
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

/* 加载动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* 卡片悬停效果 */
.search-data-manage-page .card {
  transition: all 0.2s ease-in-out;
}

.search-data-manage-page .card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 按钮组样式 */
.search-data-manage-page button {
  transition: all 0.2s ease-in-out;
}

.search-data-manage-page button:hover {
  transform: translateY(-1px);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .search-data-manage-page {
    padding: 1rem;
  }
  
  .search-data-manage-page .grid-cols-1.md\\:grid-cols-3 {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
}

/* 深色模式支持 - 保持白色背景 */
@media (prefers-color-scheme: dark) {
  .search-data-manage-page {
    background-color: white !important;
  }
}

/* 高对比度支持 */
@media (prefers-contrast: high) {
  .search-data-manage-page {
    border-width: 2px;
  }
}
</style>