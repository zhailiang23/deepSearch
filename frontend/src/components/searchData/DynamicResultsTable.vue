<template>
  <div class="dynamic-results-table w-full">
    <!-- 表格控制栏 -->
    <div class="table-controls flex flex-col sm:flex-row gap-4 mb-4 p-4 bg-emerald-50 rounded-lg">
      <!-- 左侧控制 -->
      <div class="flex flex-wrap items-center gap-2 flex-1">
        <!-- 字段管理按钮 -->
        <Button
          variant="outline"
          size="sm"
          @click="showFieldManager = !showFieldManager"
          class="border-emerald-200 text-emerald-700 hover:bg-emerald-100"
        >
          <Settings class="w-4 h-4 mr-1" />
          字段管理
        </Button>
        
        <!-- 视图切换 -->
        <div class="hidden sm:flex border rounded-md overflow-hidden">
          <Button
            variant="ghost"
            size="sm"
            :class="[
              'rounded-none border-none',
              viewMode === 'table' ? 'bg-emerald-100 text-emerald-700' : 'text-gray-600'
            ]"
            @click="viewMode = 'table'"
          >
            <Table class="w-4 h-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            :class="[
              'rounded-none border-none',
              viewMode === 'card' ? 'bg-emerald-100 text-emerald-700' : 'text-gray-600'
            ]"
            @click="viewMode = 'card'"
          >
            <Grid class="w-4 h-4" />
          </Button>
        </div>
        
        <!-- 虚拟滚动开关 -->
        <label class="hidden md:flex items-center gap-2 text-sm text-gray-600">
          <input
            v-model="virtualScrollEnabled"
            type="checkbox"
            class="rounded border-emerald-300 text-emerald-600 focus:ring-emerald-500"
          >
          虚拟滚动
        </label>
      </div>
      
      <!-- 右侧信息 -->
      <div class="flex items-center gap-4 text-sm text-gray-600">
        <span>共 {{ totalCount }} 条</span>
        <span v-if="virtualScrollEnabled && visibleRange">
          显示 {{ visibleRange.start + 1 }}-{{ Math.min(visibleRange.end, totalCount) }}
        </span>
      </div>
    </div>
    
    <!-- 字段管理面板 -->
    <div v-if="showFieldManager" class="field-manager mb-4">
      <FieldManager
        v-model:columns="visibleColumns"
        :all-columns="allColumns"
        @update:columns="handleColumnsUpdate"
      />
    </div>
    
    <!-- 表格内容区域 -->
    <div class="table-content bg-white rounded-lg border border-gray-200 overflow-hidden">
      <!-- 桌面端表格视图 -->
      <div v-if="viewMode === 'table'" class="desktop-table hidden md:block">
        <!-- 表格头部 -->
        <div class="table-header bg-gray-50 border-b sticky top-0 z-10">
          <div class="flex">
            <!-- 选择列 -->
            <div class="w-12 flex-shrink-0 p-3 flex items-center justify-center border-r">
              <input
                type="checkbox"
                :checked="isAllSelected"
                :indeterminate="isSomeSelected"
                @change="toggleSelectAll"
                class="rounded border-emerald-300 text-emerald-600 focus:ring-emerald-500"
              >
            </div>
            <!-- 动态列头 -->
            <div
              v-for="column in visibleColumns"
              :key="column.key"
              :class="[
                'flex-shrink-0 p-3 border-r font-medium text-gray-700 cursor-pointer hover:bg-gray-100 transition-colors select-none',
                column.sortable ? 'cursor-pointer' : 'cursor-default'
              ]"
              :style="{ width: getColumnWidth(column) }"
              @click="column.sortable && handleSort(column.key)"
            >
              <div class="flex items-center justify-between">
                <span>{{ column.label }}</span>
                <div v-if="column.sortable" class="ml-2 flex flex-col">
                  <ChevronUp 
                    :class="[
                      'w-3 h-3 -mb-1',
                      sortConfig?.field === column.key && sortConfig?.order === 'asc' 
                        ? 'text-emerald-600' : 'text-gray-400'
                    ]" 
                  />
                  <ChevronDown 
                    :class="[
                      'w-3 h-3',
                      sortConfig?.field === column.key && sortConfig?.order === 'desc'
                        ? 'text-emerald-600' : 'text-gray-400'
                    ]" 
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 虚拟滚动表格体 -->
        <VirtualList
          v-if="virtualScrollEnabled"
          ref="virtualListRef"
          :items="tableRows"
          :item-height="60"
          :container-height="500"
          :visible-count="15"
          :buffer-size="5"
          :loading="loading"
          key-field="_id"
          @scroll="handleScroll"
          @reach-bottom="handleReachBottom"
        >
          <template #item="{ item, index }">
            <TableRowDesktop 
              :row="item"
              :columns="visibleColumns"
              :index="index"
              :selected="selectedRows.has(item._id)"
              @select="toggleRowSelection(item._id)"
              @edit="handleEdit(item)"
              @view="handleView(item)"
            />
          </template>
          
          <template #empty>
            <div class="text-center py-12">
              <div class="text-6xl mb-4">🔍</div>
              <div class="text-lg text-gray-500 mb-2">暂无搜索结果</div>
              <div class="text-sm text-gray-400">尝试调整搜索条件或搜索空间</div>
            </div>
          </template>
        </VirtualList>
        
        <!-- 普通滚动表格体 -->
        <div v-else class="table-body max-h-96 overflow-y-auto">
          <TableRowDesktop
            v-for="(row, index) in tableRows"
            :key="row._id"
            :row="row"
            :columns="visibleColumns"
            :index="index"
            :selected="selectedRows.has(row._id)"
            @select="toggleRowSelection(row._id)"
            @edit="handleEdit(row)"
            @view="handleView(row)"
          />
          
          <!-- 空状态 -->
          <div v-if="!loading && tableRows.length === 0" class="text-center py-12">
            <div class="text-6xl mb-4">🔍</div>
            <div class="text-lg text-gray-500 mb-2">暂无搜索结果</div>
            <div class="text-sm text-gray-400">尝试调整搜索条件或搜索空间</div>
          </div>
        </div>
      </div>
      
      <!-- 移动端卡片视图 -->
      <div v-else class="mobile-cards md:hidden">
        <VirtualList
          v-if="virtualScrollEnabled"
          ref="virtualListRef"
          :items="tableRows"
          :item-height="120"
          :container-height="600"
          :visible-count="8"
          :buffer-size="3"
          :loading="loading"
          key-field="_id"
          @scroll="handleScroll"
          @reach-bottom="handleReachBottom"
        >
          <template #item="{ item, index }">
            <TableRowCard 
              :row="item"
              :columns="visibleColumns"
              :index="index"
              :selected="selectedRows.has(item._id)"
              @select="toggleRowSelection(item._id)"
              @edit="handleEdit(item)"
              @view="handleView(item)"
            />
          </template>
        </VirtualList>
        
        <div v-else class="space-y-4 p-4 max-h-96 overflow-y-auto">
          <TableRowCard
            v-for="(row, index) in tableRows"
            :key="row._id"
            :row="row"
            :columns="visibleColumns"
            :index="index"
            :selected="selectedRows.has(row._id)"
            @select="toggleRowSelection(row._id)"
            @edit="handleEdit(row)"
            @view="handleView(row)"
          />
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading && !virtualScrollEnabled" class="flex items-center justify-center py-8">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        <span class="ml-3 text-emerald-600">加载中...</span>
      </div>
    </div>
    
    <!-- 分页控制 -->
    <div v-if="!virtualScrollEnabled" class="pagination-wrapper mt-4">
      <PaginationControl
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="totalCount"
        :loading="loading"
        @change="handlePaginationChange"
      />
    </div>

    <!-- 编辑对话框 -->
    <DocumentEditDialog
      v-model:open="editDialogOpen"
      :document="editingDocument"
      :mapping="mapping"
      @save-success="handleEditSuccess"
      @save-error="handleEditError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Button } from '@/components/ui/button'
import { Settings, Table, Grid, ChevronUp, ChevronDown } from 'lucide-vue-next'
import VirtualList from '@/components/ui/VirtualList.vue'
import FieldManager from './FieldManager.vue'
import PaginationControl from './PaginationControl.vue'
import TableRowDesktop from './table/TableRowDesktop.vue'
import TableRowCard from './table/TableRowCard.vue'
import DocumentEditDialog from './DocumentEditDialog.vue'
import { useMediaQuery } from '@/composables/useMediaQuery'
import { debounce, throttle } from '@/utils/performance'
import type {
  TableColumn,
  TableRow,
  TableData,
  SortConfig,
  FilterConfig,
  ESIndexMapping
} from '@/types/tableData'

interface Props {
  data: TableRow[]
  mapping?: ESIndexMapping
  loading?: boolean
  totalCount: number
  defaultPageSize?: number
  enableVirtualScroll?: boolean
  height?: number
}

interface Emits {
  (e: 'sort', config: SortConfig): void
  (e: 'filter', filters: FilterConfig[]): void
  (e: 'page-change', page: number, size: number): void
  (e: 'selection-change', selectedIds: string[]): void
  (e: 'edit', row: TableRow): void
  (e: 'view', row: TableRow): void
  (e: 'load-more'): void
  (e: 'update-document', document: TableRow): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  defaultPageSize: 20,
  enableVirtualScroll: false,
  height: 600
})

const emit = defineEmits<Emits>()

// 响应式状态
const showFieldManager = ref(false)
const virtualScrollEnabled = ref(props.enableVirtualScroll)
const selectedRows = ref(new Set<string>())
const sortConfig = ref<SortConfig>()
const currentPage = ref(1)
const pageSize = ref(props.defaultPageSize)
const virtualListRef = ref()
const visibleRange = ref()

// 媒体查询
const isMobile = useMediaQuery('(max-width: 768px)')
const isTablet = useMediaQuery('(min-width: 769px) and (max-width: 1024px)')

// 视图模式
const viewMode = ref<'table' | 'card'>('table')

// 编辑对话框状态
const editDialogOpen = ref(false)
const editingDocument = ref<TableRow | null>(null)

// 计算属性
const tableRows = computed(() => props.data)
const totalCount = computed(() => props.totalCount)
const loading = computed(() => props.loading)

// 动态生成列配置
const allColumns = computed(() => generateColumnsFromMapping(props.mapping))
const visibleColumns = ref<TableColumn[]>([])

// 选择相关计算属性
const isAllSelected = computed(() => {
  return tableRows.value.length > 0 && selectedRows.value.size === tableRows.value.length
})

const isSomeSelected = computed(() => {
  return selectedRows.value.size > 0 && selectedRows.value.size < tableRows.value.length
})

// 基于ES mapping生成列配置
function generateColumnsFromMapping(mapping?: ESIndexMapping): TableColumn[] {
  if (!mapping?.mappings?.properties) return []
  
  const columns: TableColumn[] = []
  
  Object.entries(mapping.mappings.properties).forEach(([field, fieldMapping]) => {
    const column: TableColumn = {
      key: field,
      label: field,
      type: mapESTypeToColumnType(fieldMapping.type),
      sortable: ['keyword', 'date', 'number', 'boolean'].includes(fieldMapping.type),
      filterable: true,
      visible: true,
      resizable: true,
      align: fieldMapping.type === 'number' ? 'right' : 'left',
      esField: field,
      esType: fieldMapping.type,
      format: fieldMapping.format
    }
    
    columns.push(column)
  })
  
  return columns
}

// ES类型映射到表格列类型
function mapESTypeToColumnType(esType: string): TableColumn['type'] {
  const typeMap: Record<string, TableColumn['type']> = {
    text: 'text',
    keyword: 'keyword',
    date: 'date',
    long: 'number',
    integer: 'number',
    double: 'number',
    float: 'number',
    boolean: 'boolean',
    object: 'object',
    nested: 'nested'
  }
  
  return typeMap[esType] || 'text'
}

// 获取列宽度
function getColumnWidth(column: TableColumn): string {
  if (column.width) return `${column.width}px`
  if (column.type === 'date') return '150px'
  if (column.type === 'number') return '120px'
  if (column.type === 'boolean') return '100px'
  return '200px'
}

// 排序处理
const handleSort = debounce((field: string) => {
  const currentSort = sortConfig.value
  let newOrder: 'asc' | 'desc' = 'asc'
  
  if (currentSort?.field === field) {
    newOrder = currentSort.order === 'asc' ? 'desc' : 'asc'
  }
  
  const newSortConfig = { field, order: newOrder }
  sortConfig.value = newSortConfig
  emit('sort', newSortConfig)
}, 200)

// 行选择
function toggleRowSelection(rowId: string) {
  if (selectedRows.value.has(rowId)) {
    selectedRows.value.delete(rowId)
  } else {
    selectedRows.value.add(rowId)
  }
  
  emit('selection-change', Array.from(selectedRows.value))
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedRows.value.clear()
  } else {
    tableRows.value.forEach(row => {
      selectedRows.value.add(row._id)
    })
  }
  
  emit('selection-change', Array.from(selectedRows.value))
}

// 列配置更新
function handleColumnsUpdate(newColumns: TableColumn[]) {
  visibleColumns.value = newColumns
}

// 分页处理
function handlePaginationChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  emit('page-change', page, size)
}

// 虚拟滚动事件
const handleScroll = throttle((scrollData: { scrollTop: number; direction: string }) => {
  if (virtualListRef.value) {
    visibleRange.value = virtualListRef.value.getVisibleRange()
  }
}, 100)

function handleReachBottom() {
  if (!loading.value) {
    emit('load-more')
  }
}

// 行操作
function handleEdit(row: TableRow) {
  editingDocument.value = row
  editDialogOpen.value = true
  emit('edit', row)
}

function handleView(row: TableRow) {
  emit('view', row)
}

// 编辑处理方法
function handleEditSuccess(updatedDocument: TableRow) {
  // 更新本地数据
  const index = tableRows.value.findIndex(row => row._id === updatedDocument._id)
  if (index !== -1) {
    // 直接更新props.data需要通过emit通知父组件
    emit('update-document', updatedDocument)
  }

  // 重置编辑状态
  editingDocument.value = null
  editDialogOpen.value = false

  // 显示成功提示
  showSuccessMessage('数据保存成功')
}

function handleEditError(error: string) {
  // 显示错误提示
  showErrorMessage(error)
}

function showSuccessMessage(message: string) {
  // 创建自定义事件显示成功消息
  window.dispatchEvent(new CustomEvent('show-notification', {
    detail: {
      type: 'success',
      message
    }
  }))
}

function showErrorMessage(message: string) {
  // 创建自定义事件显示错误消息
  window.dispatchEvent(new CustomEvent('show-notification', {
    detail: {
      type: 'error',
      message
    }
  }))
}

// 响应式视图模式切换
watch(isMobile, (mobile) => {
  if (mobile) {
    viewMode.value = 'card'
  } else {
    viewMode.value = 'table'
  }
})

// 初始化列配置
watch(allColumns, (columns) => {
  if (columns.length > 0 && visibleColumns.value.length === 0) {
    // 初始显示前6列
    visibleColumns.value = columns.slice(0, 6)
  }
}, { immediate: true })

// 内存优化：清理定时器
onMounted(() => {
  // 初始化虚拟滚动可见范围
  if (virtualScrollEnabled.value && virtualListRef.value) {
    nextTick(() => {
      visibleRange.value = virtualListRef.value?.getVisibleRange()
    })
  }
})
</script>

<style scoped>
.dynamic-results-table {
  --table-border-color: #e5e7eb;
  --table-hover-color: #f3f4f6;
  --emerald-color: #10b981;
}

.table-header {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.desktop-table {
  border-collapse: separate;
  border-spacing: 0;
}

@media (max-width: 768px) {
  .desktop-table {
    display: none;
  }
  
  .mobile-cards {
    display: block;
  }
}

@media (min-width: 769px) {
  .mobile-cards {
    display: none;
  }
  
  .desktop-table {
    display: block;
  }
}

/* 表格行hover效果 */
.table-row:hover {
  background-color: var(--table-hover-color);
}

/* 选中状态 */
.table-row.selected {
  background-color: #ecfdf5;
  border-left: 3px solid var(--emerald-color);
}

/* 滚动条优化 */
.table-body::-webkit-scrollbar,
.mobile-cards::-webkit-scrollbar {
  width: 6px;
}

.table-body::-webkit-scrollbar-track,
.mobile-cards::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.table-body::-webkit-scrollbar-thumb,
.mobile-cards::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.table-body::-webkit-scrollbar-thumb:hover,
.mobile-cards::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 性能优化：减少重绘 */
.virtual-list-item {
  contain: layout style paint;
  will-change: transform;
}

/* 响应式字体大小 */
@media (max-width: 640px) {
  .table-controls {
    padding: 1rem;
  }
  
  .table-controls .text-sm {
    font-size: 0.75rem;
  }
}
</style>