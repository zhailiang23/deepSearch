<template>
  <div class="cell-renderer">
    <!-- 文本类型 -->
    <div
      v-if="type === 'text'"
      class="text-cell"
      :title="displayValue"
    >
      <span 
        v-if="isHighlighted"
        v-html="highlightedValue"
        class="highlighted-text"
      />
      <span v-else class="truncate">{{ displayValue }}</span>
    </div>
    
    <!-- 关键词类型 -->
    <div
      v-else-if="type === 'keyword'"
      class="keyword-cell"
    >
      <span class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-purple-100 text-purple-800 border border-purple-200">
        {{ displayValue }}
      </span>
    </div>
    
    <!-- 日期类型 -->
    <div
      v-else-if="type === 'date'"
      class="date-cell text-gray-700"
      :title="rawValue"
    >
      <div class="flex items-center gap-1">
        <Calendar class="w-3 h-3 text-gray-500" />
        <span>{{ formattedDate }}</span>
      </div>
    </div>
    
    <!-- 数字类型 -->
    <div
      v-else-if="type === 'number'"
      class="number-cell text-right font-mono"
      :title="rawValue"
    >
      {{ formattedNumber }}
    </div>
    
    <!-- 布尔类型 -->
    <div
      v-else-if="type === 'boolean'"
      class="boolean-cell"
    >
      <span 
        :class="[
          'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
          value 
            ? 'bg-green-100 text-green-800 border border-green-200'
            : 'bg-red-100 text-red-800 border border-red-200'
        ]"
      >
        <component 
          :is="value ? CheckCircle : XCircle" 
          class="w-3 h-3 mr-1" 
        />
        {{ value ? '是' : '否' }}
      </span>
    </div>
    
    <!-- 对象类型 -->
    <div
      v-else-if="type === 'object'"
      class="object-cell"
    >
      <div class="flex items-center gap-2">
        <Braces class="w-3 h-3 text-indigo-500" />
        <span class="text-indigo-600 text-sm">{{ objectSummary }}</span>
        <Button
          v-if="value && typeof value === 'object'"
          variant="ghost"
          size="sm"
          @click="toggleExpanded"
          class="h-5 w-5 p-0 text-indigo-500 hover:text-indigo-700"
        >
          <component :is="expanded ? ChevronUp : ChevronDown" class="w-3 h-3" />
        </Button>
      </div>
      
      <!-- 展开的对象内容 -->
      <div v-if="expanded" class="mt-2 p-2 bg-gray-50 rounded border text-xs">
        <pre class="text-gray-700 whitespace-pre-wrap">{{ formattedObject }}</pre>
      </div>
    </div>
    
    <!-- 嵌套类型 -->
    <div
      v-else-if="type === 'nested'"
      class="nested-cell"
    >
      <div class="flex items-center gap-2">
        <List class="w-3 h-3 text-pink-500" />
        <span class="text-pink-600 text-sm">{{ nestedSummary }}</span>
        <Button
          v-if="Array.isArray(value) && value.length > 0"
          variant="ghost"
          size="sm"
          @click="toggleExpanded"
          class="h-5 w-5 p-0 text-pink-500 hover:text-pink-700"
        >
          <component :is="expanded ? ChevronUp : ChevronDown" class="w-3 h-3" />
        </Button>
      </div>
      
      <!-- 展开的嵌套内容 -->
      <div v-if="expanded" class="mt-2 space-y-1">
        <div
          v-for="(item, index) in value"
          :key="index"
          class="p-2 bg-gray-50 rounded border text-xs"
        >
          <pre class="text-gray-700 whitespace-pre-wrap">{{ JSON.stringify(item, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <!-- 空值 -->
    <div
      v-else-if="value === null || value === undefined"
      class="null-cell text-gray-400 italic"
    >
      <Minus class="w-3 h-3 inline mr-1" />
      空值
    </div>
    
    <!-- 未知类型 -->
    <div v-else class="unknown-cell text-gray-600">
      <span class="truncate" :title="String(value)">
        {{ String(value) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'
import { 
  Calendar, 
  CheckCircle, 
  XCircle, 
  Braces, 
  List, 
  ChevronUp, 
  ChevronDown,
  Minus
} from 'lucide-vue-next'

interface Props {
  value: any
  type: 'text' | 'keyword' | 'date' | 'number' | 'boolean' | 'object' | 'nested' | string
  format?: string
  field?: string
  searchTerm?: string
}

const props = defineProps<Props>()

// 响应式状态
const expanded = ref(false)

// 原始值
const rawValue = computed(() => props.value)

// 显示值
const displayValue = computed(() => {
  if (props.value === null || props.value === undefined) return ''
  return String(props.value)
})

// 格式化日期
const formattedDate = computed(() => {
  if (!props.value) return ''
  
  try {
    const date = new Date(props.value)
    if (isNaN(date.getTime())) return String(props.value)
    
    // 根据format参数决定格式
    if (props.format === 'date') {
      return date.toLocaleDateString('zh-CN')
    } else if (props.format === 'time') {
      return date.toLocaleTimeString('zh-CN')
    } else {
      return date.toLocaleString('zh-CN')
    }
  } catch (error) {
    return String(props.value)
  }
})

// 格式化数字
const formattedNumber = computed(() => {
  if (typeof props.value !== 'number') return String(props.value)
  
  // 根据format参数决定格式
  if (props.format === 'integer') {
    return props.value.toLocaleString('zh-CN', { 
      maximumFractionDigits: 0 
    })
  } else if (props.format === 'decimal') {
    return props.value.toLocaleString('zh-CN', { 
      minimumFractionDigits: 2,
      maximumFractionDigits: 2 
    })
  } else if (props.format === 'percentage') {
    return (props.value * 100).toFixed(1) + '%'
  } else {
    return props.value.toLocaleString('zh-CN')
  }
})

// 对象摘要
const objectSummary = computed(() => {
  if (!props.value || typeof props.value !== 'object') return '空对象'
  
  const keys = Object.keys(props.value)
  if (keys.length === 0) return '空对象'
  
  return `${keys.length} 个属性`
})

// 格式化对象
const formattedObject = computed(() => {
  if (!props.value) return ''
  return JSON.stringify(props.value, null, 2)
})

// 嵌套数组摘要
const nestedSummary = computed(() => {
  if (!Array.isArray(props.value)) return '空数组'
  
  const length = props.value.length
  if (length === 0) return '空数组'
  
  return `${length} 个项目`
})

// 搜索高亮
const isHighlighted = computed(() => {
  return props.searchTerm && 
         props.type === 'text' && 
         displayValue.value.toLowerCase().includes(props.searchTerm.toLowerCase())
})

const highlightedValue = computed(() => {
  if (!props.searchTerm || !displayValue.value) return displayValue.value
  
  const regex = new RegExp(`(${props.searchTerm})`, 'gi')
  return displayValue.value.replace(regex, '<mark class="bg-yellow-200 px-1 rounded">$1</mark>')
})

// 切换展开状态
function toggleExpanded() {
  expanded.value = !expanded.value
}
</script>

<style scoped>
.cell-renderer {
  min-width: 0;
  width: 100%;
}

/* 文本截断 */
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

/* 高亮文本样式 */
.highlighted-text :deep(mark) {
  background-color: #fef08a;
  padding: 0 2px;
  border-radius: 2px;
  font-weight: 500;
}

/* 各类型特殊样式 */
.text-cell {
  max-width: 200px;
}

.keyword-cell span {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.date-cell {
  white-space: nowrap;
}

.number-cell {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.boolean-cell span {
  white-space: nowrap;
}

.object-cell,
.nested-cell {
  max-width: 200px;
}

.object-cell pre,
.nested-cell pre {
  max-height: 200px;
  overflow-y: auto;
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 11px;
  line-height: 1.4;
}

.null-cell {
  font-size: 12px;
  white-space: nowrap;
}

.unknown-cell {
  max-width: 200px;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .text-cell,
  .object-cell,
  .nested-cell,
  .unknown-cell {
    max-width: 120px;
  }
  
  .keyword-cell span {
    max-width: 100px;
  }
  
  .object-cell pre,
  .nested-cell pre {
    font-size: 10px;
    max-height: 150px;
  }
}

/* 展开动画 */
.object-cell > div:last-child,
.nested-cell > div:last-child {
  animation: slideDown 0.2s ease-out;
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
    max-height: 300px;
  }
}

/* 滚动条优化 */
.object-cell pre::-webkit-scrollbar,
.nested-cell pre::-webkit-scrollbar {
  width: 4px;
}

.object-cell pre::-webkit-scrollbar-track,
.nested-cell pre::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.object-cell pre::-webkit-scrollbar-thumb,
.nested-cell pre::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.object-cell pre::-webkit-scrollbar-thumb:hover,
.nested-cell pre::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 类型标识颜色 */
.text-cell {
  --type-color: #3b82f6;
}

.keyword-cell {
  --type-color: #8b5cf6;
}

.date-cell {
  --type-color: #10b981;
}

.number-cell {
  --type-color: #f59e0b;
}

.boolean-cell {
  --type-color: #84cc16;
}

.object-cell {
  --type-color: #6366f1;
}

.nested-cell {
  --type-color: #ec4899;
}
</style>