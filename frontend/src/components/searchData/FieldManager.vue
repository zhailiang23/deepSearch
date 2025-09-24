<template>
  <div class="field-manager bg-white border border-gray-200 rounded-lg p-4">
    <!-- 头部控制区 -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-medium text-gray-900">字段管理</h3>
      <div class="flex items-center gap-2">
        <!-- 搜索框 -->
        <div class="relative">
          <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            v-model="searchTerm"
            placeholder="搜索字段..."
            class="pl-9 pr-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
          >
        </div>
        
        <!-- 操作按钮 -->
        <Button
          variant="outline"
          size="sm"
          @click="showAllFields"
          class="text-emerald-700 border-emerald-200 hover:bg-emerald-50"
        >
          全部显示
        </Button>
        
        <Button
          variant="outline"  
          size="sm"
          @click="hideAllFields"
          class="text-gray-600 border-gray-200 hover:bg-gray-50"
        >
          全部隐藏
        </Button>
        
        <Button
          variant="outline"
          size="sm"
          @click="resetToDefault"
          class="text-gray-600 border-gray-200 hover:bg-gray-50"
        >
          恢复默认
        </Button>
      </div>
    </div>
    
    <!-- 字段分组 -->
    <div class="space-y-4">
      <!-- 可见字段 -->
      <div class="visible-fields">
        <div class="flex items-center justify-between mb-2">
          <h4 class="text-sm font-medium text-gray-700 flex items-center">
            <Eye class="w-4 h-4 mr-1 text-emerald-600" />
            可见字段 ({{ visibleFields.length }})
          </h4>
          <span class="text-xs text-gray-500">拖拽调整顺序</span>
        </div>
        
        <draggable
          v-model="visibleFields"
          group="fields"
          item-key="key"
          class="min-h-20 border-2 border-dashed border-emerald-200 rounded-lg p-2 space-y-1"
          :class="{ 'border-emerald-400 bg-emerald-50': isDragging }"
          @start="isDragging = true"
          @end="handleDragEnd"
        >
          <template #item="{ element: field, index }">
            <div
              class="field-item flex items-center p-2 bg-emerald-50 border border-emerald-200 rounded-md cursor-move hover:bg-emerald-100 transition-colors group"
              :class="{ 'opacity-50': field.disabled }"
            >
              <!-- 拖拽手柄 -->
              <GripVertical class="w-4 h-4 text-gray-400 mr-2 group-hover:text-gray-600" />
              
              <!-- 字段信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-900 truncate">{{ field.label }}</span>
                  <TypeBadge :type="field.type" />
                </div>
                <div class="text-xs text-gray-500 truncate">{{ field.key }}</div>
              </div>
              
              <!-- 字段配置 -->
              <div class="flex items-center gap-2 ml-2">
                <!-- 宽度配置 -->
                <div class="flex items-center gap-1 text-xs text-gray-600">
                  <span>宽度:</span>
                  <input
                    v-model.number="field.width"
                    type="number"
                    min="80"
                    max="500"
                    class="w-16 px-1 py-0.5 border border-gray-300 rounded text-xs"
                    @change="updateField(field)"
                  >
                </div>
                
                <!-- 固定配置 -->
                <select
                  v-model="field.fixed"
                  class="text-xs border border-gray-300 rounded px-1 py-0.5"
                  @change="updateField(field)"
                >
                  <option :value="undefined">无固定</option>
                  <option value="left">左固定</option>
                  <option value="right">右固定</option>
                </select>
                
                <!-- 隐藏按钮 -->
                <Button
                  variant="ghost"
                  size="sm"
                  @click="toggleFieldVisibility(field)"
                  class="p-1 h-6 w-6 text-gray-500 hover:text-red-600"
                >
                  <EyeOff class="w-3 h-3" />
                </Button>
              </div>
            </div>
          </template>
        </draggable>
      </div>
      
      <!-- 隐藏字段 -->
      <div class="hidden-fields" v-if="hiddenFields.length > 0">
        <h4 class="text-sm font-medium text-gray-700 mb-2 flex items-center">
          <EyeOff class="w-4 h-4 mr-1 text-gray-400" />
          隐藏字段 ({{ hiddenFields.length }})
        </h4>
        
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
          <div
            v-for="field in filteredHiddenFields"
            :key="field.key"
            class="field-item flex items-center p-2 bg-gray-50 border border-gray-200 rounded-md hover:bg-gray-100 transition-colors cursor-pointer"
            @click="toggleFieldVisibility(field)"
          >
            <!-- 字段信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-700 truncate">{{ field.label }}</span>
                <TypeBadge :type="field.type" />
              </div>
              <div class="text-xs text-gray-500 truncate">{{ field.key }}</div>
            </div>
            
            <!-- 显示按钮 -->
            <Button
              variant="ghost"
              size="sm"
              class="p-1 h-6 w-6 text-gray-500 hover:text-emerald-600"
            >
              <Eye class="w-3 h-3" />
            </Button>
          </div>
        </div>
      </div>
      
      <!-- 无匹配结果 -->
      <div 
        v-if="searchTerm && filteredHiddenFields.length === 0 && filteredVisibleFields.length === 0"
        class="text-center py-8 text-gray-500"
      >
        <Search class="w-8 h-8 mx-auto mb-2 text-gray-400" />
        <div>未找到匹配的字段</div>
        <div class="text-sm">尝试其他搜索词</div>
      </div>
    </div>
    
    <!-- 字段统计 -->
    <div class="mt-4 pt-4 border-t border-gray-200">
      <div class="flex items-center justify-between text-sm text-gray-600">
        <div>
          共 {{ allColumns.length }} 个字段，
          显示 {{ visibleFields.length }} 个，
          隐藏 {{ hiddenFields.length }} 个
        </div>
        <div class="flex items-center gap-2">
          <span>固定:</span>
          <span class="text-emerald-600">左 {{ leftFixedCount }}</span>
          <span class="text-blue-600">右 {{ rightFixedCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Search, Eye, EyeOff, GripVertical } from 'lucide-vue-next'
import draggable from 'vuedraggable'
import TypeBadge from './TypeBadge.vue'
import type { TableColumn } from '@/types/tableData'

interface Props {
  allColumns: TableColumn[]
  modelValue?: TableColumn[]
}

interface Emits {
  (e: 'update:modelValue', columns: TableColumn[]): void
  (e: 'update:columns', columns: TableColumn[]): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => []
})

const emit = defineEmits<Emits>()

// 响应式状态
const searchTerm = ref('')
const isDragging = ref(false)

// 字段分组
const visibleFields = ref<TableColumn[]>([])
const hiddenFields = computed(() => 
  props.allColumns.filter(col => !visibleFields.value.some(v => v.key === col.key))
)

// 搜索过滤
const filteredVisibleFields = computed(() => {
  if (!searchTerm.value) return visibleFields.value
  const term = searchTerm.value.toLowerCase()
  return visibleFields.value.filter(field => 
    field.label.toLowerCase().includes(term) || 
    field.key.toLowerCase().includes(term)
  )
})

const filteredHiddenFields = computed(() => {
  if (!searchTerm.value) return hiddenFields.value
  const term = searchTerm.value.toLowerCase()
  return hiddenFields.value.filter(field => 
    field.label.toLowerCase().includes(term) || 
    field.key.toLowerCase().includes(term)
  )
})

// 统计信息
const leftFixedCount = computed(() => 
  visibleFields.value.filter(field => field.fixed === 'left').length
)

const rightFixedCount = computed(() => 
  visibleFields.value.filter(field => field.fixed === 'right').length
)

// 字段操作
function toggleFieldVisibility(field: TableColumn) {
  const isVisible = visibleFields.value.some(v => v.key === field.key)
  
  if (isVisible) {
    // 隐藏字段
    visibleFields.value = visibleFields.value.filter(v => v.key !== field.key)
  } else {
    // 显示字段
    const fieldCopy = { ...field, visible: true }
    visibleFields.value.push(fieldCopy)
  }
  
  emitUpdate()
}

function updateField(field: TableColumn) {
  // 触发更新
  emitUpdate()
}

function showAllFields() {
  visibleFields.value = [...props.allColumns.map(col => ({ ...col, visible: true }))]
  emitUpdate()
}

function hideAllFields() {
  visibleFields.value = []
  emitUpdate()
}

function resetToDefault() {
  // 显示前6个字段作为默认
  visibleFields.value = props.allColumns
    .slice(0, 6)
    .map(col => ({ ...col, visible: true, fixed: undefined, width: undefined }))
  emitUpdate()
}

function handleDragEnd() {
  isDragging.value = false
  emitUpdate()
}

function emitUpdate() {
  const updatedColumns = visibleFields.value.map(field => ({
    ...field,
    visible: true
  }))
  
  emit('update:modelValue', updatedColumns)
  emit('update:columns', updatedColumns)
}

// 初始化
watch(
  () => props.modelValue,
  (newColumns) => {
    if (newColumns && newColumns.length > 0) {
      visibleFields.value = [...newColumns]
    } else if (props.allColumns.length > 0 && visibleFields.value.length === 0) {
      // 默认显示前6个字段
      resetToDefault()
    }
  },
  { immediate: true }
)

watch(
  () => props.allColumns,
  (newColumns) => {
    if (newColumns.length > 0 && visibleFields.value.length === 0) {
      resetToDefault()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.field-item {
  transition: all 0.2s ease;
}

.field-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 拖拽状态 */
.sortable-ghost {
  opacity: 0.5;
  transform: rotate(5deg);
}

.sortable-chosen {
  cursor: grabbing;
}

/* 搜索高亮 */
.highlight {
  background-color: yellow;
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .field-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .field-item .flex-1 {
    width: 100%;
  }
  
  .field-item > div:last-child {
    width: 100%;
    justify-content: space-between;
  }
}

/* 动画效果 */
.field-item {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 拖拽区域样式 */
.sortable-drag {
  background: white !important;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2) !important;
  border: 2px solid #10b981 !important;
  transform: rotate(5deg) !important;
}

/* 空状态优化 */
.min-h-20:empty::before {
  content: "拖拽字段到此处显示";
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  color: #9ca3af;
  font-size: 14px;
}
</style>