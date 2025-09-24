<template>
  <div 
    :class="[
      'table-row flex items-center border-b border-gray-200 hover:bg-gray-50 transition-colors',
      { 
        'bg-emerald-50 border-emerald-200': selected,
        'opacity-75': row._score !== undefined && row._score < 0.5 
      }
    ]"
  >
    <!-- 选择列 -->
    <div class="w-12 flex-shrink-0 p-3 flex items-center justify-center border-r">
      <input
        type="checkbox"
        :checked="selected"
        @change="handleSelect"
        class="rounded border-emerald-300 text-emerald-600 focus:ring-emerald-500"
      >
    </div>
    
    <!-- 动态数据列 -->
    <div
      v-for="column in columns"
      :key="column.key"
      :class="[
        'flex-shrink-0 p-3 border-r',
        `text-${column.align || 'left'}`,
        {
          'sticky left-12 bg-white z-10': column.fixed === 'left',
          'sticky right-0 bg-white z-10': column.fixed === 'right'
        }
      ]"
      :style="{ 
        width: getColumnWidth(column),
        minWidth: column.minWidth ? `${column.minWidth}px` : undefined,
        maxWidth: column.maxWidth ? `${column.maxWidth}px` : undefined
      }"
    >
      <!-- 字段值渲染 -->
      <div class="min-w-0 flex-1">
        <CellRenderer
          :value="getCellValue(row, column)"
          :type="column.type"
          :format="column.format"
          :field="column.key"
        />
      </div>
    </div>
    
    <!-- 操作列 -->
    <div class="w-32 flex-shrink-0 p-3 flex items-center justify-end gap-2 sticky right-0 bg-white z-10">
      <!-- 评分显示 -->
      <div 
        v-if="row._score !== undefined"
        class="text-xs text-gray-500 px-2 py-1 bg-gray-100 rounded"
        :title="`相关度评分: ${row._score.toFixed(3)}`"
      >
        {{ formatScore(row._score) }}
      </div>
      
      <!-- 操作按钮 -->
      <div class="flex items-center gap-1">
        <!-- 查看详情 -->
        <Button
          variant="ghost"
          size="sm"
          @click="handleView"
          class="h-8 w-8 p-0 text-blue-600 hover:text-blue-700 hover:bg-blue-50"
          title="查看详情"
        >
          <Eye class="w-4 h-4" />
        </Button>
        
        <!-- 编辑按钮 (预留给Issue #40) -->
        <Button
          variant="ghost"
          size="sm"
          @click="handleEdit"
          class="h-8 w-8 p-0 text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50"
          title="编辑记录"
        >
          <Edit class="w-4 h-4" />
        </Button>
        
        <!-- 更多操作 -->
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              class="h-8 w-8 p-0 text-gray-500 hover:text-gray-700 hover:bg-gray-50"
              title="更多操作"
            >
              <MoreHorizontal class="w-4 h-4" />
            </Button>
          </DropdownMenuTrigger>
          
          <DropdownMenuContent align="end" class="w-48">
            <!-- 复制操作 -->
            <DropdownMenuItem @click="copyRowData">
              <Copy class="w-4 h-4 mr-2" />
              复制数据
            </DropdownMenuItem>
            
            <DropdownMenuItem @click="copyId">
              <Hash class="w-4 h-4 mr-2" />
              复制ID
            </DropdownMenuItem>
            
            <DropdownMenuSeparator />
            
            <!-- 导出操作 -->
            <DropdownMenuItem @click="exportJson">
              <FileJson class="w-4 h-4 mr-2" />
              导出JSON
            </DropdownMenuItem>
            
            <DropdownMenuItem @click="exportCsv">
              <FileSpreadsheet class="w-4 h-4 mr-2" />
              导出CSV
            </DropdownMenuItem>
            
            <DropdownMenuSeparator />
            
            <!-- 危险操作 (预留给Issue #41) -->
            <DropdownMenuItem 
              @click="handleDelete"
              class="text-red-600 hover:text-red-700 hover:bg-red-50"
            >
              <Trash2 class="w-4 h-4 mr-2" />
              删除记录
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { 
  Eye, 
  Edit, 
  MoreHorizontal, 
  Copy, 
  Hash,
  FileJson,
  FileSpreadsheet,
  Trash2
} from 'lucide-vue-next'
import CellRenderer from './CellRenderer.vue'
import type { TableColumn, TableRow } from '@/types/tableData'

interface Props {
  row: TableRow
  columns: TableColumn[]
  index: number
  selected: boolean
}

interface Emits {
  (e: 'select', rowId: string): void
  (e: 'edit', row: TableRow): void
  (e: 'view', row: TableRow): void
  (e: 'delete', row: TableRow): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 获取列宽度
function getColumnWidth(column: TableColumn): string {
  if (column.width) return `${column.width}px`
  if (column.type === 'date') return '150px'
  if (column.type === 'number') return '120px'
  if (column.type === 'boolean') return '100px'
  return '200px'
}

// 获取单元格值
function getCellValue(row: TableRow, column: TableColumn): any {
  const source = row._source
  if (!source) return null
  
  // 支持嵌套字段访问 (e.g., "user.name")
  const keys = column.esField?.split('.') || column.key.split('.')
  let value = source
  
  for (const key of keys) {
    if (value && typeof value === 'object') {
      value = value[key]
    } else {
      return null
    }
  }
  
  return value
}

// 格式化评分
function formatScore(score: number): string {
  if (score >= 0.9) return '★★★'
  if (score >= 0.7) return '★★☆'
  if (score >= 0.5) return '★☆☆'
  return '☆☆☆'
}

// 事件处理
function handleSelect() {
  emit('select', props.row._id)
}

function handleEdit() {
  emit('edit', props.row)
}

function handleView() {
  emit('view', props.row)
}

function handleDelete() {
  emit('delete', props.row)
}

// 复制操作
async function copyRowData() {
  try {
    const data = JSON.stringify(props.row._source, null, 2)
    await navigator.clipboard.writeText(data)
    // TODO: 添加成功提示
  } catch (error) {
    console.error('复制失败:', error)
  }
}

async function copyId() {
  try {
    await navigator.clipboard.writeText(props.row._id)
    // TODO: 添加成功提示
  } catch (error) {
    console.error('复制失败:', error)
  }
}

// 导出操作
function exportJson() {
  const data = JSON.stringify(props.row._source, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  
  const a = document.createElement('a')
  a.href = url
  a.download = `record_${props.row._id}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function exportCsv() {
  const headers = props.columns.map(col => col.label).join(',')
  const values = props.columns.map(col => {
    const value = getCellValue(props.row, col)
    // 处理CSV转义
    if (value === null || value === undefined) return ''
    const strValue = String(value)
    return strValue.includes(',') || strValue.includes('"') 
      ? `"${strValue.replace(/"/g, '""')}"` 
      : strValue
  }).join(',')
  
  const csv = `${headers}\n${values}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  
  const a = document.createElement('a')
  a.href = url
  a.download = `record_${props.row._id}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
/* 表格行样式 */
.table-row {
  min-height: 60px;
}

/* 选中状态 */
.table-row.bg-emerald-50 {
  border-left: 3px solid #10b981;
}

/* 固定列阴影效果 */
.sticky.left-12 {
  box-shadow: 2px 0 5px -2px rgba(0, 0, 0, 0.1);
}

.sticky.right-0 {
  box-shadow: -2px 0 5px -2px rgba(0, 0, 0, 0.1);
}

/* 单元格内容溢出处理 */
.min-w-0 {
  min-width: 0;
}

/* 评分星级样式 */
.table-row [title*="相关度评分"] {
  cursor: help;
}

/* 操作按钮悬停效果 */
.table-row button {
  transition: all 0.2s ease-in-out;
}

.table-row button:hover {
  transform: scale(1.05);
}

/* 行悬停时操作按钮显示 */
.table-row:not(:hover) .operation-buttons {
  opacity: 0.6;
}

.table-row:hover .operation-buttons {
  opacity: 1;
}

/* 响应式优化 */
@media (max-width: 1024px) {
  .table-row {
    min-height: 56px;
  }
  
  .table-row .p-3 {
    padding: 0.5rem;
  }
}

/* 低相关度记录样式 */
.table-row.opacity-75 {
  background-color: #f9fafb;
}

.table-row.opacity-75 .text-gray-500 {
  color: #9ca3af;
}

/* 动画效果 */
.table-row {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 性能优化 */
.table-row {
  contain: layout style paint;
}
</style>