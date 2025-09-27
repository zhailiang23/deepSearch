<template>
  <div
    :class="[
      'table-row border-b border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer',
      {
        'opacity-75': row._score !== undefined && row._score < 0.5,
        'bg-green-50 border-green-200': isClicked
      }
    ]"
    @click="handleRowClick"
    @keydown.enter="handleKeyboardClick"
    @keydown.space.prevent="handleKeyboardClick"
    tabindex="0"
    role="button"
    :aria-label="`表格行 ${index + 1}`"
  >
    <div class="flex items-center" :style="{ width: getCalculatedTableWidth() }">
      <!-- 动态数据列 -->
      <div
        v-for="column in columns"
        :key="column.key"
        :class="[
          'p-3 border-r',
          `text-${column.align || 'left'}`,
          {
            'sticky left-12 bg-white z-10': column.fixed === 'left',
            'sticky right-0 bg-white z-10': column.fixed === 'right'
          }
        ]"
        :style="{
          width: getColumnWidth(column),
          flexShrink: 0
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
      <div class="w-32 flex-shrink-0 p-3 flex items-center justify-end gap-2 sticky right-0 bg-white z-20 border-r">
      
      <!-- 操作按钮 -->
      <div class="flex items-center gap-1">
        <!-- 编辑按钮 -->
        <Button
          variant="ghost"
          size="sm"
          @click="handleEdit"
          class="h-8 w-8 p-0 text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50"
          title="编辑记录"
        >
          <Edit class="w-4 h-4" />
        </Button>

        <!-- 删除按钮 -->
        <Button
          variant="ghost"
          size="sm"
          @click="handleDelete"
          class="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
          title="删除记录"
        >
          <Trash2 class="w-4 h-4" />
        </Button>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Button } from '@/components/ui/button'
import {
  Edit,
  Trash2
} from 'lucide-vue-next'
import CellRenderer from './CellRenderer.vue'
import type { TableColumn, TableRow } from '@/types/tableData'

interface Props {
  row: TableRow
  columns: TableColumn[]
  index: number
}

interface Emits {
  (e: 'edit', row: TableRow): void
  (e: 'delete', row: TableRow): void
  (e: 'click', row: TableRow, index: number, event: MouseEvent | KeyboardEvent): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 点击状态
const isClicked = ref(false)

/**
 * 处理行点击事件
 */
const handleRowClick = (event: MouseEvent) => {
  // 防止编辑和删除按钮触发行点击
  const target = event.target as HTMLElement
  if (target.closest('button')) {
    return
  }

  // 显示点击反馈
  isClicked.value = true
  setTimeout(() => {
    isClicked.value = false
  }, 200)

  emit('click', props.row, props.index, event)
}

/**
 * 处理键盘点击事件
 */
const handleKeyboardClick = (event: KeyboardEvent) => {
  if (event.code === 'Enter' || event.code === 'Space') {
    // 显示点击反馈
    isClicked.value = true
    setTimeout(() => {
      isClicked.value = false
    }, 200)

    emit('click', props.row, props.index, event)
  }
}

/**
 * 处理编辑按钮点击
 */
const handleEdit = (event: MouseEvent) => {
  event.stopPropagation()
  emit('edit', props.row)
}

/**
 * 处理删除按钮点击
 */
const handleDelete = (event: MouseEvent) => {
  event.stopPropagation()
  emit('delete', props.row)
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
  if (!props.columns || props.columns.length === 0) {
    return '800px' // 最小宽度
  }

  // 计算所有列的宽度总和
  let totalWidth = 0
  props.columns.forEach(column => {
    const width = getColumnWidth(column)
    totalWidth += parseInt(width.replace('px', ''))
  })

  // 加上操作列的宽度（128px = w-32）
  totalWidth += 128

  // 返回计算出的总宽度
  return `${totalWidth}px`
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


// 事件处理方法已在上面定义
</script>

<style scoped>
/* 表格行样式 */
.table-row {
  min-height: 60px;
}

/* 点击反馈样式 */
.table-row.bg-green-50 {
  background-color: #f0fdf4 !important;
  border-color: #bbf7d0 !important;
}

/* 行焦点样式 */
.table-row:focus {
  outline: none;
  box-shadow: 0 0 0 2px #10b981;
}

/* 行点击动画 */
.table-row {
  transition: all 0.2s ease-in-out;
}

.table-row:active {
  transform: scale(0.995);
}


/* 固定列阴影效果 */
.sticky.left-12 {
  box-shadow: 2px 0 5px -2px rgba(0, 0, 0, 0.1);
}

.sticky.right-0 {
  box-shadow: -2px 0 5px -2px rgba(0, 0, 0, 0.15);
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