<template>
  <div class="dynamic-results-table w-full max-w-full overflow-hidden">


    <!-- 字段管理面板 -->
    <div v-if="showFieldManager" class="field-manager mb-4">
      <FieldManager
        v-model:columns="visibleColumns"
        :all-columns="allColumns"
        @update:columns="handleColumnsUpdate"
      />
    </div>

    <!-- 表格内容区域 -->
    <div class="table-content bg-white rounded-lg border border-gray-200 overflow-hidden w-full max-w-full">
      <!-- 桌面端表格视图 -->
      <div v-if="viewMode === 'table'" class="desktop-table hidden md:block w-full max-w-full overflow-hidden">
        <!-- 表格容器 - 统一滚动容器 -->
        <div
          ref="tableScrollContainer"
          class="table-container overflow-x-auto max-h-96 overflow-y-auto"
          style="width: 100%; max-width: 100%;"
          @scroll="handleTableScroll"
        >
          <div :style="{ width: getCalculatedTableWidth() }">
            <!-- 表格头部 -->
            <div class="table-header bg-gray-50 border-b sticky top-0 z-10">
              <div class="flex">
                <!-- 动态列头 -->
                <div
                  v-for="column in visibleColumns"
                  :key="column.key"
                  :class="[
                    'p-3 border-r font-medium text-gray-700 cursor-pointer hover:bg-gray-100 transition-colors select-none',
                    column.sortable ? 'cursor-pointer' : 'cursor-default'
                  ]"
                  :style="{
                    width: getColumnWidth(column),
                    flexShrink: 0
                  }"
                  @click="column.sortable && handleSort(column.key)"
                >
                  <div class="flex items-center justify-between">
                    <span class="truncate">{{ column.label }}</span>
                    <div v-if="column.sortable" class="ml-2 flex flex-col flex-shrink-0">
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

                <!-- 操作列头 -->
                <div class="w-32 flex-shrink-0 p-3 border-r font-medium text-gray-700 sticky right-0 bg-gray-50 z-20">
                  <span>操作</span>
                </div>
              </div>
            </div>

            <!-- 表格体 -->
            <div>
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
                  @edit="handleEdit(item)"
                  @delete="handleDelete(item)"
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
            <div v-else class="table-body">
              <TableRowDesktop
                v-for="(row, index) in tableRows"
                :key="row._id"
                :row="row"
                :columns="visibleColumns"
                :index="index"
                @edit="handleEdit(row)"
                @delete="handleDelete(row)"
              />

              <!-- 空状态 -->
              <div v-if="!loading && tableRows.length === 0" class="text-center py-12">
                <div class="text-6xl mb-4">🔍</div>
                <div class="text-lg text-gray-500 mb-2">暂无搜索结果</div>
                <div class="text-sm text-gray-400">尝试调整搜索条件或搜索空间</div>
              </div>
            </div>
            </div>
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
              @delete="handleDelete(item)"
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
            @delete="handleDelete(row)"
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

    <!-- 删除确认对话框 - 临时禁用 -->
    <!-- 临时禁用删除弹窗，直到查明自动触发原因 -->
    <!--
    <DeleteConfirmDialog
      v-model:open="deleteDialogOpen"
      :document="deletingDocument"
      :documents="deletingDocuments"
      :loading="deleteLoading"
      @confirm="handleDeleteConfirm"
      @cancel="handleDeleteCancel"
    />
    -->
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Button } from '@/components/ui/button'
import { Settings, Table, Grid, ChevronUp, ChevronDown, Trash2 } from 'lucide-vue-next'
import VirtualList from '@/components/ui/VirtualList.vue'
import FieldManager from './FieldManager.vue'
import PaginationControl from './PaginationControl.vue'
import TableRowDesktop from './table/TableRowDesktop.vue'
import TableRowCard from './table/TableRowCard.vue'
import DocumentEditDialog from './DocumentEditDialog.vue'
import DeleteConfirmDialog from './DeleteConfirmDialog.vue'
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
  (e: 'delete', row: TableRow): void
  (e: 'batch-delete', rows: TableRow[]): void
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
const sortConfig = ref<SortConfig>()
const currentPage = ref(1)
const pageSize = ref(props.defaultPageSize)
const virtualListRef = ref()
const visibleRange = ref()
const tableScrollContainer = ref()

// 媒体查询
const isMobile = useMediaQuery('(max-width: 768px)')
const isTablet = useMediaQuery('(min-width: 769px) and (max-width: 1024px)')

// 视图模式
const viewMode = ref<'table' | 'card'>('table')

// 编辑对话框状态
const editDialogOpen = ref(false)
const editingDocument = ref<TableRow | null>(null)

// 删除对话框状态
const deleteDialogOpen = ref(false)
const deletingDocument = ref<TableRow | null>(null)
const deletingDocuments = ref<TableRow[]>([])
const deleteLoading = ref(false)

// 计算属性
const tableRows = computed(() => props.data)
const totalCount = computed(() => props.totalCount)
const loading = computed(() => props.loading)

// 表格采用flex布局，通过min-width和max-width约束总体宽度

// 动态生成列配置
const allColumns = computed(() => generateColumnsFromMapping(props.mapping))
const visibleColumns = ref<TableColumn[]>([])

// 基于ES mapping生成列配置
function generateColumnsFromMapping(mapping?: ESIndexMapping): TableColumn[] {
  if (!mapping) return []

  // 支持不同的映射结构
  let properties = mapping.properties ||
                   mapping.mappings?.properties ||
                   mapping.mappings

  if (!properties) return []

  const columns: TableColumn[] = []

  Object.entries(properties).forEach(([field, fieldMapping]: [string, any]) => {
    // 过滤以下划线开头的字段
    if (field.startsWith('_')) {
      return
    }

    // 从布尔值映射中解析字段类型
    const fieldType = getFieldTypeFromBooleanMapping(fieldMapping)

    const column: TableColumn = {
      key: field,
      label: field.charAt(0).toUpperCase() + field.slice(1),
      type: mapESTypeToColumnType(fieldType),
      sortable: ['keyword', 'date', 'number', 'boolean'].includes(fieldType),
      filterable: true,
      visible: true,
      resizable: true,
      align: fieldType === 'number' ? 'right' : 'left',
      esField: field,
      esType: fieldType,
      format: fieldMapping.format
    }

    columns.push(column)
  })
  
  return columns
}

// 从布尔值映射中解析字段类型
function getFieldTypeFromBooleanMapping(fieldMapping: any): string {
  if (!fieldMapping || typeof fieldMapping !== 'object') {
    return 'text'
  }

  // 检查各种类型的布尔值标识
  if (fieldMapping.boolean === true) return 'boolean'
  if (fieldMapping.date === true) return 'date'
  if (fieldMapping.long === true || fieldMapping.integer === true) return 'number'
  if (fieldMapping.float === true || fieldMapping.double === true) return 'number'
  if (fieldMapping.keyword === true) return 'keyword'
  if (fieldMapping.text === true) return 'text'

  // 如果有传统的type字段，直接使用
  if (fieldMapping.type) return fieldMapping.type

  // 默认为文本类型
  return 'text'
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
  return '160px' // 减小默认宽度
}

// 获取列最小宽度
function getMinColumnWidth(column: TableColumn): string {
  if (column.minWidth) return `${column.minWidth}px`
  if (column.type === 'date') return '120px'
  if (column.type === 'number') return '80px'
  if (column.type === 'boolean') return '80px'
  return '100px' // 设置最小宽度
}

// 获取列最大宽度
function getMaxColumnWidth(column: TableColumn): string {
  if (column.maxWidth) return `${column.maxWidth}px`
  if (column.type === 'date') return '180px'
  if (column.type === 'number') return '150px'
  if (column.type === 'boolean') return '120px'
  return '220px' // 设置最大宽度
}

// 计算表格总宽度
function getCalculatedTableWidth(): string {
  if (!visibleColumns.value || visibleColumns.value.length === 0) {
    return '800px' // 最小宽度
  }

  // 计算所有列的宽度总和
  let totalWidth = 0
  visibleColumns.value.forEach(column => {
    const width = getColumnWidth(column)
    totalWidth += parseInt(width.replace('px', ''))
  })

  // 加上操作列的宽度（128px = w-32）
  totalWidth += 128

  // 返回计算出的总宽度，但不超过最大限制
  return `${totalWidth}px`
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

// 表格统一滚动处理
function handleTableScroll(event: Event) {
  // 表格滚动事件处理，可以在此添加其他逻辑
  // 由于表头和数据行现在在同一个滚动容器中，它们会自动同步滚动
}

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

function handleDelete(row: TableRow) {
  deletingDocument.value = row
  deletingDocuments.value = []
  deleteDialogOpen.value = true
}


// 删除处理方法
function handleDeleteConfirm(options: { forceDelete: boolean }) {
  deleteLoading.value = true
  
  if (deletingDocument.value) {
    // 单个删除
    emit('delete', deletingDocument.value)
  } else if (deletingDocuments.value.length > 0) {
    // 批量删除
    emit('batch-delete', deletingDocuments.value)
  }
}

function handleDeleteCancel() {
  deleteDialogOpen.value = false
  deletingDocument.value = null
  deletingDocuments.value = []
  deleteLoading.value = false
}

function handleDeleteSuccess() {
  // 清理选中状态
  // 批量删除处理已移除选中状态相关逻辑
  
  // 关闭对话框
  deleteDialogOpen.value = false
  deletingDocument.value = null
  deletingDocuments.value = []
  deleteLoading.value = false

  // 显示成功提示
  showSuccessMessage('数据删除成功')
}

function handleDeleteError(error: string) {
  deleteLoading.value = false
  showErrorMessage(error)
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
  if (columns.length > 0) {
    // 每次allColumns变化时都重新生成可见列
    // 显示所有列
    visibleColumns.value = columns.filter(col => col.visible)
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

// 暴露给父组件的方法
defineExpose({
  handleDeleteSuccess,
  handleDeleteError
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

/* 表格容器严格宽度约束 */
.table-container {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

/* 表头容器固定宽度，不允许撑开 */
.table-header {
  position: relative;
}

.table-header .flex {
  /* 宽度由Vue模板中的计算属性控制 */
}

/* 动态列采用固定宽度，通过JS计算控制 */

/* 固定列阴影效果 */
.sticky.right-0 {
  box-shadow: -2px 0 5px -2px rgba(0, 0, 0, 0.15);
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