<template>
  <div class="search-data-manage-page min-h-screen bg-gray-50">
    <!-- 页面头部 -->
    <div class="bg-white border-b border-gray-200 px-6 py-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <h1 class="text-2xl font-bold text-gray-900">搜索数据管理</h1>
          <div class="flex items-center gap-2 text-sm text-gray-600">
            <Database class="w-4 h-4" />
            <span>{{ currentIndex || '请选择索引' }}</span>
          </div>
        </div>
        
        <!-- 操作按钮组 -->
        <div class="flex items-center gap-3">
          <!-- 刷新按钮 -->
          <Button
            variant="outline"
            size="sm"
            @click="handleRefresh"
            :disabled="loading"
            class="text-emerald-600 border-emerald-300 hover:bg-emerald-50"
          >
            <RefreshCw :class="['w-4 h-4 mr-2', { 'animate-spin': loading }]" />
            刷新
          </Button>
          
          <!-- 导出按钮 -->
          <Button
            variant="outline"
            size="sm"
            @click="handleExport"
            :disabled="loading || selectedRows.length === 0"
            class="text-blue-600 border-blue-300 hover:bg-blue-50"
          >
            <Download class="w-4 h-4 mr-2" />
            导出 ({{ selectedRows.length }})
          </Button>
          
          <!-- 字段管理按钮 -->
          <Button
            variant="outline"
            size="sm"
            @click="showFieldManager = true"
            class="text-purple-600 border-purple-300 hover:bg-purple-50"
          >
            <Settings class="w-4 h-4 mr-2" />
            字段管理
          </Button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="flex-1 p-6 space-y-6">
      <!-- 搜索条件面板 -->
      <Card>
        <CardHeader class="pb-3">
          <div class="flex items-center justify-between">
            <CardTitle class="text-lg text-gray-900">搜索条件</CardTitle>
            <Button
              variant="ghost"
              size="sm"
              @click="toggleSearchPanel"
              class="text-gray-600 hover:text-gray-800"
            >
              <component :is="showSearchPanel ? ChevronUp : ChevronDown" class="w-4 h-4" />
            </Button>
          </div>
        </CardHeader>
        
        <CardContent v-if="showSearchPanel" class="pt-0">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <!-- 索引选择 -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-gray-700">索引选择</label>
              <select
                v-model="currentIndex"
                @change="handleIndexChange"
                :disabled="loading"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
              >
                <option value="">请选择索引</option>
                <option 
                  v-for="index in availableIndexes"
                  :key="index.name"
                  :value="index.name"
                >
                  {{ index.name }} ({{ index.docCount }} 条记录)
                </option>
              </select>
            </div>
            
            <!-- 搜索关键词 -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-gray-700">搜索关键词</label>
              <div class="relative">
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="输入关键词搜索..."
                  @keyup.enter="handleSearch"
                  :disabled="loading || !currentIndex"
                  class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
                >
                <Search class="absolute left-3 top-2.5 w-4 h-4 text-gray-400" />
              </div>
            </div>
            
            <!-- 搜索按钮 -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-gray-700 opacity-0">搜索</label>
              <Button
                @click="handleSearch"
                :disabled="loading || !currentIndex"
                class="w-full bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                <Search class="w-4 h-4 mr-2" />
                搜索
              </Button>
            </div>
          </div>
          
          <!-- 高级搜索选项 -->
          <div class="mt-4 pt-4 border-t border-gray-200">
            <Button
              variant="ghost"
              size="sm"
              @click="showAdvancedSearch = !showAdvancedSearch"
              class="text-gray-600 hover:text-gray-800"
            >
              <component :is="showAdvancedSearch ? ChevronUp : ChevronDown" class="w-4 h-4 mr-2" />
              高级搜索
            </Button>
            
            <div v-if="showAdvancedSearch" class="mt-3 grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- 排序设置 -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">排序字段</label>
                <select
                  v-model="sortField"
                  :disabled="loading || !currentIndex"
                  class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
                >
                  <option value="">相关度</option>
                  <option 
                    v-for="column in sortableColumns"
                    :key="column.key"
                    :value="column.key"
                  >
                    {{ column.label }}
                  </option>
                </select>
              </div>
              
              <!-- 排序方向 -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700">排序方向</label>
                <select
                  v-model="sortOrder"
                  :disabled="loading || !sortField"
                  class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
                >
                  <option value="desc">降序</option>
                  <option value="asc">升序</option>
                </select>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 结果统计 -->
      <div v-if="searchResults.total > 0" class="bg-white rounded-lg border border-gray-200 p-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4 text-sm text-gray-600">
            <div class="flex items-center gap-2">
              <FileText class="w-4 h-4" />
              <span>共找到 {{ searchResults.total }} 条记录</span>
            </div>
            <div v-if="searchQuery" class="flex items-center gap-2">
              <Clock class="w-4 h-4" />
              <span>耗时 {{ searchResults.took }}ms</span>
            </div>
            <div v-if="selectedRows.length > 0" class="flex items-center gap-2">
              <CheckSquare class="w-4 h-4" />
              <span>已选择 {{ selectedRows.length }} 条</span>
            </div>
          </div>
          
          <div class="flex items-center gap-2">
            <Button
              v-if="selectedRows.length > 0"
              variant="ghost"
              size="sm"
              @click="clearSelection"
              class="text-gray-600 hover:text-gray-800"
            >
              清空选择
            </Button>
          </div>
        </div>
      </div>

      <!-- 动态结果表格 -->
      <Card>
        <CardContent class="p-0">
          <DynamicResultsTable
            ref="tableRef"
            v-if="currentIndex && indexMapping"
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
          
          <!-- 空状态 -->
          <div v-else-if="!loading" class="flex flex-col items-center justify-center py-16 text-gray-500">
            <Database class="w-16 h-16 mb-4 text-gray-300" />
            <h3 class="text-lg font-medium text-gray-900 mb-2">暂无数据</h3>
            <p class="text-gray-600">{{ currentIndex ? '请执行搜索查看结果' : '请选择索引并执行搜索' }}</p>
          </div>
          
          <!-- 加载状态 -->
          <div v-if="loading" class="flex flex-col items-center justify-center py-16">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mb-4"></div>
            <p class="text-gray-600">{{ loadingText }}</p>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 字段管理弹窗 -->
    <Dialog v-model:open="showFieldManager">
      <DialogContent class="max-w-4xl max-h-[80vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle>字段管理</DialogTitle>
          <DialogDescription>
            管理表格显示字段，调整字段顺序和可见性
          </DialogDescription>
        </DialogHeader>
        
        <div class="flex-1 overflow-hidden">
          <FieldManager
            v-if="indexMapping"
            :mapping="indexMapping"
            :initial-fields="visibleColumns"
            @fields-change="handleFieldsChange"
          />
        </div>
      </DialogContent>
    </Dialog>

    <!-- 数据详情弹窗 -->
    <Dialog v-model:open="showDataDetail">
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/components/ui/toast'
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
  RefreshCw,
  Download,
  Settings,
  ChevronUp,
  ChevronDown,
  FileText,
  Clock,
  CheckSquare
} from 'lucide-vue-next'

// 组件导入
import DynamicResultsTable from '@/components/searchData/DynamicResultsTable.vue'
import FieldManager from '@/components/searchData/FieldManager.vue'

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
const showFieldManager = ref(false)
const showDataDetail = ref(false)

// 搜索相关
const currentIndex = ref('')
const availableIndexes = ref<Array<{name: string, docCount: number}>>([])
const searchQuery = ref('')
const sortField = ref('')
const sortOrder = ref<'asc' | 'desc'>('desc')

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
const visibleColumns = ref<TableColumn[]>([])

// 计算属性
const sortableColumns = computed(() => {
  return visibleColumns.value.filter(col => col.sortable)
})

// 生命周期
onMounted(async () => {
  await loadAvailableIndexes()
  
  // 从路由参数恢复状态
  const { index, query } = router.currentRoute.value.query
  if (index && typeof index === 'string') {
    currentIndex.value = index
    await handleIndexChange()
  }
  if (query && typeof query === 'string') {
    searchQuery.value = query
  }
})

// 监听索引变化
watch(currentIndex, async (newIndex) => {
  if (newIndex) {
    await loadIndexMapping()
    clearResults()
  }
})

// 方法定义
async function loadAvailableIndexes() {
  try {
    loading.value = true
    loadingText.value = '加载索引列表...'
    
    // TODO: 调用实际的API
    // const response = await searchDataApi.getIndexes()
    // availableIndexes.value = response.data
    
    // 模拟数据
    availableIndexes.value = [
      { name: 'sample-index-1', docCount: 1250 },
      { name: 'user-data', docCount: 892 },
      { name: 'product-catalog', docCount: 3456 }
    ]
  } catch (error) {
    console.error('加载索引失败:', error)
    toast({
      title: '错误',
      description: '加载索引列表失败，请检查网络连接',
      variant: 'destructive'
    })
  } finally {
    loading.value = false
  }
}

async function loadIndexMapping() {
  if (!currentIndex.value) return
  
  try {
    loading.value = true
    loadingText.value = '加载字段映射...'
    
    // TODO: 调用实际的API
    // const response = await searchDataApi.getIndexMapping(currentIndex.value)
    // indexMapping.value = response.data
    
    // 模拟映射数据
    indexMapping.value = {
      properties: {
        title: { type: 'text', analyzer: 'standard' },
        category: { type: 'keyword' },
        created_date: { type: 'date', format: 'yyyy-MM-dd HH:mm:ss' },
        price: { type: 'double' },
        active: { type: 'boolean' },
        metadata: { 
          type: 'object',
          properties: {
            tags: { type: 'keyword' },
            description: { type: 'text' }
          }
        }
      }
    }
    
    // 生成初始可见列
    visibleColumns.value = generateInitialColumns()
  } catch (error) {
    console.error('加载字段映射失败:', error)
    toast({
      title: '错误',
      description: '加载字段映射失败',
      variant: 'destructive'
    })
  } finally {
    loading.value = false
  }
}

function generateInitialColumns(): TableColumn[] {
  if (!indexMapping.value?.properties) return []
  
  const columns: TableColumn[] = []
  const properties = indexMapping.value.properties
  
  Object.entries(properties).forEach(([key, field], index) => {
    columns.push({
      key,
      label: key.charAt(0).toUpperCase() + key.slice(1),
      type: mapFieldType(field.type),
      sortable: ['keyword', 'number', 'date', 'boolean'].includes(field.type),
      filterable: true,
      visible: index < 5, // 默认显示前5个字段
      width: getDefaultWidth(field.type),
      esField: key,
      format: getFieldFormat(field)
    })
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
      description: '请先选择索引',
      variant: 'default'
    })
    return
  }
  
  try {
    loading.value = true
    loadingText.value = '搜索中...'
    
    const searchParams: SearchParams = {
      index: currentIndex.value,
      query: searchQuery.value,
      from: (currentPage.value - 1) * pageSize.value,
      size: pageSize.value,
      sort: sortField.value ? {
        field: sortField.value,
        order: sortOrder.value
      } : undefined
    }
    
    // TODO: 调用实际的搜索API
    // const response = await searchDataApi.search(searchParams)
    // searchResults.value = response.data
    
    // 模拟搜索结果
    await new Promise(resolve => setTimeout(resolve, 800))
    searchResults.value = {
      hits: generateMockData(),
      total: 1234,
      took: 45
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
  } catch (error) {
    console.error('搜索失败:', error)
    toast({
      title: '搜索失败',
      description: '请检查网络连接后重试',
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

async function handleRefresh() {
  await handleSearch()
}

function handleExport() {
  if (selectedRows.value.length === 0) {
    toast({
      title: '提示',
      description: '请先选择要导出的数据',
      variant: 'default'
    })
    return
  }
  
  // TODO: 实现导出功能
  toast({
    title: '导出功能',
    description: '导出功能将在Issue #42中实现',
    variant: 'default'
  })
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
  showFieldManager.value = false
  
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
  selectedRowData.value = row
  showDataDetail.value = true
}

function handleRowDelete(row: TableRow) {
  handleDeleteDocument(row)
}

function handleBatchDelete(rows: TableRow[]) {
  handleDeleteDocuments(rows)
}

async function executeSearch() {
  await handleSearch()
}

async function handleDeleteDocument(document: TableRow) {
  if (!currentIndex.value) {
    showErrorToast('请先选择索引')
    return
  }

  try {
    const response = await searchDataService.deleteDocument({
      id: document._id,
      index: currentIndex.value,
      version: document._version
    })

    if (response.result === 'deleted') {
      showSuccessToast('文档删除成功')
      // 移除本地数据
      const index = searchResults.value.hits.findIndex(item => item._id === document._id)
      if (index !== -1) {
        searchResults.value.hits.splice(index, 1)
        searchResults.value.total = Math.max(0, searchResults.value.total - 1)
      }
      // 通知表格组件删除成功
      tableRef.value?.handleDeleteSuccess()
    } else if (response.result === 'not_found') {
      showWarningToast('文档不存在或已被删除')
      // 刷新数据
      await executeSearch()
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

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .search-data-manage-page.bg-gray-50 {
    background-color: #1f2937;
  }
}

/* 高对比度支持 */
@media (prefers-contrast: high) {
  .search-data-manage-page {
    border-width: 2px;
  }
}
</style>