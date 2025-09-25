<template>
  <div 
    :class="[
      'table-row-card bg-white border border-gray-200 rounded-lg p-4 mb-3 shadow-sm hover:shadow-md transition-shadow',
      { 
        'border-emerald-300 bg-emerald-50': selected,
        'opacity-75': row._score !== undefined && row._score < 0.5
      }
    ]"
  >
    <!-- 卡片头部 -->
    <div class="card-header flex items-start justify-between mb-3">
      <div class="flex items-start gap-3 flex-1 min-w-0">
        <!-- 选择框 -->
        <input
          type="checkbox"
          :checked="selected"
          @change="handleSelect"
          class="mt-1 rounded border-emerald-300 text-emerald-600 focus:ring-emerald-500"
        >
        
        <!-- 主要信息 -->
        <div class="flex-1 min-w-0">
          <!-- 标题行 -->
          <div class="flex items-center gap-2 mb-1">
            <h3 class="text-sm font-medium text-gray-900 truncate">
              {{ getDisplayTitle() }}
            </h3>
            
            <!-- 评分 -->
            <div 
              v-if="row._score !== undefined"
              class="text-xs px-2 py-1 bg-gray-100 rounded-full text-gray-600"
              :title="`相关度评分: ${row._score.toFixed(3)}`"
            >
              {{ formatScore(row._score) }}
            </div>
          </div>
          
          <!-- 副标题 -->
          <div class="text-xs text-gray-500 mb-2">
            ID: {{ row._id }}
          </div>
        </div>
      </div>
      
      <!-- 操作按钮组 -->
      <div class="flex items-center gap-1 flex-shrink-0">
        <!-- 查看 -->
        <Button
          variant="ghost"
          size="sm"
          @click="handleView"
          class="h-8 w-8 p-0 text-blue-600 hover:text-blue-700 hover:bg-blue-50"
          title="查看详情"
        >
          <Eye class="w-4 h-4" />
        </Button>
        
        <!-- 编辑 (预留给Issue #40) -->
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
          
          <DropdownMenuContent align="end" class="w-40">
            <DropdownMenuItem @click="copyId">
              <Hash class="w-4 h-4 mr-2" />
              复制ID
            </DropdownMenuItem>
            
            <DropdownMenuItem @click="exportJson">
              <FileJson class="w-4 h-4 mr-2" />
              导出JSON
            </DropdownMenuItem>
            
            <DropdownMenuSeparator />
            
            <DropdownMenuItem 
              @click="handleDelete"
              class="text-red-600 hover:text-red-700 hover:bg-red-50"
            >
              <Trash2 class="w-4 h-4 mr-2" />
              删除
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
    
    <!-- 卡片内容 -->
    <div class="card-content">
      <!-- 主要字段 -->
      <div class="primary-fields space-y-2 mb-3">
        <div
          v-for="column in primaryColumns"
          :key="column.key"
          class="field-row flex items-start gap-3"
        >
          <!-- 字段标签 -->
          <div class="field-label flex items-center gap-1 w-20 flex-shrink-0">
            <TypeBadge :type="column.type" />
            <span class="text-xs text-gray-600 truncate">
              {{ column.label }}
            </span>
          </div>
          
          <!-- 字段值 -->
          <div class="field-value flex-1 min-w-0">
            <CellRenderer
              :value="getCellValue(row, column)"
              :type="column.type"
              :format="column.format"
              :field="column.key"
            />
          </div>
        </div>
      </div>
      
      <!-- 展开按钮 -->
      <div 
        v-if="secondaryColumns.length > 0"
        class="expand-toggle border-t pt-3"
      >
        <Button
          variant="ghost"
          size="sm"
          @click="toggleExpanded"
          class="w-full justify-center text-gray-600 hover:text-gray-800"
        >
          <component :is="expanded ? ChevronUp : ChevronDown" class="w-4 h-4 mr-1" />
          {{ expanded ? '收起' : `显示更多 (${secondaryColumns.length} 个字段)` }}
        </Button>
      </div>
      
      <!-- 次要字段 (展开时显示) -->
      <div 
        v-if="expanded && secondaryColumns.length > 0"
        class="secondary-fields mt-3 pt-3 border-t space-y-2"
      >
        <div
          v-for="column in secondaryColumns"
          :key="column.key"
          class="field-row flex items-start gap-3"
        >
          <!-- 字段标签 -->
          <div class="field-label flex items-center gap-1 w-20 flex-shrink-0">
            <TypeBadge :type="column.type" />
            <span class="text-xs text-gray-600 truncate">
              {{ column.label }}
            </span>
          </div>
          
          <!-- 字段值 -->
          <div class="field-value flex-1 min-w-0">
            <CellRenderer
              :value="getCellValue(row, column)"
              :type="column.type"
              :format="column.format"
              :field="column.key"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
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
  Hash,
  FileJson,
  Trash2,
  ChevronUp,
  ChevronDown
} from 'lucide-vue-next'
import CellRenderer from './CellRenderer.vue'
import TypeBadge from '../TypeBadge.vue'
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

// 响应式状态
const expanded = ref(false)

// 字段分组
const primaryColumns = computed(() => {
  // 移动端优先显示前3个重要字段
  return props.columns.slice(0, 3)
})

const secondaryColumns = computed(() => {
  // 其余字段作为次要字段
  return props.columns.slice(3)
})

// 获取显示标题
function getDisplayTitle(): string {
  // 尝试从第一个字段获取有意义的标题
  if (props.columns.length === 0) return '数据记录'
  
  const firstColumn = props.columns[0]
  const value = getCellValue(props.row, firstColumn)
  
  if (value && typeof value === 'string' && value.length > 0) {
    return value.length > 30 ? value.substring(0, 30) + '...' : value
  }
  
  return '数据记录'
}

// 获取单元格值
function getCellValue(row: TableRow, column: TableColumn): any {
  const source = row._source
  if (!source) return null
  
  // 支持嵌套字段访问
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

// 切换展开状态
function toggleExpanded() {
  expanded.value = !expanded.value
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

// 复制ID
async function copyId() {
  try {
    await navigator.clipboard.writeText(props.row._id)
    // TODO: 添加成功提示
  } catch (error) {
    console.error('复制失败:', error)
  }
}

// 导出JSON
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
</script>

<style scoped>
/* 卡片基础样式 */
.table-row-card {
  transition: all 0.2s ease-in-out;
}

.table-row-card:hover {
  transform: translateY(-1px);
}

/* 选中状态 */
.table-row-card.border-emerald-300 {
  box-shadow: 0 0 0 1px #10b981;
}

/* 字段行样式 */
.field-row {
  min-height: 28px;
}

.field-label {
  margin-top: 2px;
}

.field-value {
  overflow: hidden;
}

/* 主要字段和次要字段区分 */
.primary-fields .field-row {
  padding: 4px 0;
}

.secondary-fields {
  background: #fafafa;
  margin: 0 -16px;
  padding: 12px 16px;
  border-radius: 8px;
}

.secondary-fields .field-row {
  padding: 3px 0;
  opacity: 0.9;
}

/* 展开/收起动画 */
.secondary-fields {
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
    max-height: 0;
  }
  to {
    opacity: 1;
    transform: translateY(0);
    max-height: 500px;
  }
}

/* 展开按钮样式 */
.expand-toggle button {
  font-size: 13px;
  height: 32px;
}

.expand-toggle button:hover {
  background-color: #f3f4f6;
}

/* 操作按钮样式优化 */
.card-header button {
  transition: all 0.15s ease-in-out;
}

.card-header button:hover {
  transform: scale(1.05);
}

/* 评分样式 */
.card-header [title*="相关度评分"] {
  cursor: help;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border: 1px solid #d1d5db;
}

/* 低相关度记录样式 */
.table-row-card.opacity-75 {
  background: #fafafa;
  border-color: #e5e7eb;
}

.table-row-card.opacity-75 .text-gray-900 {
  color: #6b7280;
}

/* 响应式优化 */
@media (max-width: 480px) {
  .table-row-card {
    padding: 12px;
    margin-bottom: 12px;
  }
  
  .field-label {
    width: 80px;
    font-size: 11px;
  }
  
  .card-header h3 {
    font-size: 14px;
  }
  
  .secondary-fields {
    margin: 0 -12px;
    padding: 8px 12px;
  }
}

/* 深色主题支持 */
@media (prefers-color-scheme: dark) {
  .secondary-fields {
    background: #1f2937;
  }
}

/* 打印样式 */
@media print {
  .table-row-card {
    box-shadow: none;
    border: 1px solid #000;
    break-inside: avoid;
    margin-bottom: 1rem;
  }
  
  .card-header button {
    display: none;
  }
}

/* 高对比度支持 */
@media (prefers-contrast: high) {
  .table-row-card {
    border-width: 2px;
  }
  
  .field-label {
    font-weight: 600;
  }
}

/* 动画偏好设置 */
@media (prefers-reduced-motion: reduce) {
  .table-row-card {
    transition: none;
  }
  
  .table-row-card:hover {
    transform: none;
  }
  
  .secondary-fields {
    animation: none;
  }
}
</style>