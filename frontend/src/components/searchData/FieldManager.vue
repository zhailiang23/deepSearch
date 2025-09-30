<template>
  <div class="field-manager bg-white rounded-lg">
    <!-- 头部控制区 -->
    <div class="flex items-center justify-between mb-4 pb-4 border-b border-gray-200">
      <h3 class="text-lg font-semibold text-gray-900">字段配置</h3>
      <div class="flex items-center gap-2">
        <!-- 搜索框 -->
        <div class="relative">
          <Search class="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            v-model="searchTerm"
            placeholder="搜索字段..."
            class="pl-9 pr-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 w-48"
          >
        </div>

        <!-- 操作按钮 -->
        <Button
          variant="outline"
          size="sm"
          @click="toggleAllFields(true)"
          class="text-emerald-700 border-emerald-300 hover:bg-emerald-50"
        >
          <Eye class="w-3.5 h-3.5 mr-1" />
          全部显示
        </Button>

        <Button
          variant="outline"
          size="sm"
          @click="toggleAllFields(false)"
          class="text-gray-600 border-gray-300 hover:bg-gray-50"
        >
          <EyeOff class="w-3.5 h-3.5 mr-1" />
          全部隐藏
        </Button>
      </div>
    </div>

    <!-- 字段列表 -->
    <div class="space-y-4">
      <!-- 可见字段（可拖拽） -->
      <div v-if="filteredVisibleFields.length > 0" class="visible-fields-section">
        <div class="flex items-center justify-between mb-3">
          <h4 class="text-sm font-medium text-gray-700 flex items-center">
            <Eye class="w-4 h-4 mr-1.5 text-emerald-600" />
            可见字段 <span class="ml-1 text-emerald-600">({{ visibleFieldsList.length }})</span>
          </h4>
          <span class="text-xs text-gray-500">拖拽可调整顺序</span>
        </div>

        <VueDraggable
          v-model="visibleFieldsList"
          :animation="200"
          handle=".drag-handle"
          ghostClass="ghost"
          chosenClass="chosen"
          dragClass="dragging"
          @start="isDragging = true"
          @end="onDragEnd"
          class="space-y-1.5 min-h-[60px] p-2 border-2 border-dashed rounded-lg transition-colors"
          :class="isDragging ? 'border-emerald-400 bg-emerald-50' : 'border-gray-200 bg-gray-50'"
        >
          <div
            v-for="field in filteredVisibleFields"
            :key="field.key"
            class="field-item group flex items-center gap-2 p-2 bg-white border border-gray-200 rounded-md hover:shadow-md transition-all"
          >
            <!-- 拖拽手柄 -->
            <div class="drag-handle cursor-move flex-shrink-0">
              <GripVertical class="w-3.5 h-3.5 text-gray-400 group-hover:text-gray-600" />
            </div>

            <!-- 字段信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <span class="text-sm font-medium text-gray-900 truncate">{{ field.label }}</span>
                <TypeBadge :type="field.type" />
              </div>
            </div>

            <!-- 字段配置 -->
            <div class="flex items-center gap-2 flex-shrink-0">
              <!-- 宽度配置 -->
              <div class="flex items-center gap-1 text-xs">
                <span class="text-gray-600 whitespace-nowrap">宽度:</span>
                <input
                  v-model.number="field.width"
                  type="number"
                  min="80"
                  max="500"
                  step="10"
                  class="w-16 px-1.5 py-0.5 border border-gray-300 rounded text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  @change="emitUpdate"
                >
                <span class="text-gray-400">px</span>
              </div>

              <!-- 固定配置 -->
              <select
                v-model="field.fixed"
                class="text-xs border border-gray-300 rounded px-1.5 py-0.5 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                @change="emitUpdate"
              >
                <option :value="undefined">不固定</option>
                <option value="left">左固定</option>
                <option value="right">右固定</option>
              </select>

              <!-- 隐藏按钮 -->
              <Button
                variant="ghost"
                size="sm"
                @click="toggleFieldVisibility(field, false)"
                class="p-1 h-6 w-6 text-gray-500 hover:text-red-600 hover:bg-red-50"
                title="隐藏字段"
              >
                <EyeOff class="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        </VueDraggable>
      </div>

      <!-- 隐藏字段 -->
      <div v-if="filteredHiddenFields.length > 0" class="hidden-fields-section">
        <h4 class="text-sm font-medium text-gray-700 mb-3 flex items-center">
          <EyeOff class="w-4 h-4 mr-1.5 text-gray-400" />
          隐藏字段 <span class="ml-1 text-gray-600">({{ hiddenFieldsList.length }})</span>
        </h4>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
          <div
            v-for="field in filteredHiddenFields"
            :key="field.key"
            class="field-item flex items-center gap-2 p-2.5 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 hover:border-emerald-300 transition-all cursor-pointer group"
            @click="toggleFieldVisibility(field, true)"
            :title="'点击显示 ' + field.label"
          >
            <!-- 字段信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-700 truncate text-sm">{{ field.label }}</span>
                <TypeBadge :type="field.type" />
              </div>
            </div>

            <!-- 显示按钮 -->
            <Button
              variant="ghost"
              size="sm"
              class="p-1.5 h-7 w-7 text-gray-400 group-hover:text-emerald-600"
              title="显示字段"
            >
              <Eye class="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>

      <!-- 无匹配结果 -->
      <div
        v-if="searchTerm && filteredHiddenFields.length === 0 && filteredVisibleFields.length === 0"
        class="text-center py-12 text-gray-500"
      >
        <Search class="w-12 h-12 mx-auto mb-3 text-gray-300" />
        <div class="text-base font-medium">未找到匹配的字段</div>
        <div class="text-sm mt-1">尝试使用其他关键词搜索</div>
      </div>

      <!-- 空状态 -->
      <div
        v-if="!searchTerm && allColumns.length === 0"
        class="text-center py-12 text-gray-500"
      >
        <Database class="w-12 h-12 mx-auto mb-3 text-gray-300" />
        <div class="text-base font-medium">暂无字段</div>
        <div class="text-sm mt-1">请先选择搜索空间</div>
      </div>
    </div>

    <!-- 字段统计 -->
    <div class="mt-6 pt-4 border-t border-gray-200">
      <div class="flex items-center justify-between text-sm">
        <div class="text-gray-600">
          <span class="font-medium text-gray-900">总计:</span>
          <span class="ml-2">{{ allColumns.length }} 个字段</span>
          <span class="mx-2 text-gray-300">|</span>
          <span class="text-emerald-600 font-medium">{{ visibleFieldsList.length }} 个显示</span>
          <span class="mx-2 text-gray-300">|</span>
          <span class="text-gray-500">{{ hiddenFieldsList.length }} 个隐藏</span>
        </div>
        <div class="flex items-center gap-3 text-xs text-gray-600">
          <span class="flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
            左固定: <span class="font-medium">{{ leftFixedCount }}</span>
          </span>
          <span class="flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-blue-500"></span>
            右固定: <span class="font-medium">{{ rightFixedCount }}</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import { Button } from '@/components/ui/button'
import { Search, Eye, EyeOff, GripVertical, Database } from 'lucide-vue-next'
import TypeBadge from './TypeBadge.vue'
import type { TableColumn } from '@/types/tableData'

interface Props {
  allColumns: TableColumn[]
  visibleColumns: TableColumn[]
}

interface Emits {
  (e: 'update:columns', columns: TableColumn[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式状态
const searchTerm = ref('')
const isDragging = ref(false)

// 可见字段列表（本地副本，用于拖拽排序）
const visibleFieldsList = ref<TableColumn[]>([])

// 隐藏字段列表（计算属性）
const hiddenFieldsList = computed(() =>
  props.allColumns.filter(col => !visibleFieldsList.value.some(v => v.key === col.key))
)

// 搜索过滤
const filteredVisibleFields = computed(() => {
  if (!searchTerm.value) return visibleFieldsList.value
  const term = searchTerm.value.toLowerCase()
  return visibleFieldsList.value.filter(field =>
    field.label.toLowerCase().includes(term) ||
    field.key.toLowerCase().includes(term)
  )
})

const filteredHiddenFields = computed(() => {
  if (!searchTerm.value) return hiddenFieldsList.value
  const term = searchTerm.value.toLowerCase()
  return hiddenFieldsList.value.filter(field =>
    field.label.toLowerCase().includes(term) ||
    field.key.toLowerCase().includes(term)
  )
})

// 统计信息
const leftFixedCount = computed(() =>
  visibleFieldsList.value.filter(field => field.fixed === 'left').length
)

const rightFixedCount = computed(() =>
  visibleFieldsList.value.filter(field => field.fixed === 'right').length
)

/**
 * 切换字段可见性
 */
function toggleFieldVisibility(field: TableColumn, makeVisible: boolean) {
  if (makeVisible) {
    // 显示字段：添加到可见列表
    const fieldCopy = { ...field, visible: true }
    visibleFieldsList.value.push(fieldCopy)
  } else {
    // 隐藏字段：从可见列表移除
    visibleFieldsList.value = visibleFieldsList.value.filter(v => v.key !== field.key)
  }

  emitUpdate()
}

/**
 * 切换全部字段
 */
function toggleAllFields(showAll: boolean) {
  if (showAll) {
    // 显示所有字段
    visibleFieldsList.value = props.allColumns.map(col => ({ ...col, visible: true }))
  } else {
    // 隐藏所有字段
    visibleFieldsList.value = []
  }

  emitUpdate()
}

/**
 * 拖拽结束处理
 */
function onDragEnd() {
  isDragging.value = false
  emitUpdate()
}

/**
 * 发送更新事件
 */
function emitUpdate() {
  // 标记所有可见字段为visible
  const updatedColumns = visibleFieldsList.value.map(field => ({
    ...field,
    visible: true
  }))

  emit('update:columns', updatedColumns)
}

/**
 * 初始化可见字段列表
 */
function initializeVisibleFields() {
  if (props.visibleColumns && props.visibleColumns.length > 0) {
    // 使用传入的可见列
    visibleFieldsList.value = [...props.visibleColumns]
  } else if (props.allColumns && props.allColumns.length > 0) {
    // 默认显示所有字段
    visibleFieldsList.value = props.allColumns.map(col => ({ ...col, visible: true }))
  }
}

// 监听props变化（仅在必要时更新）
watch(
  () => props.visibleColumns,
  (newColumns) => {
    if (newColumns && newColumns.length > 0) {
      // 只在外部传入新的配置时更新
      visibleFieldsList.value = [...newColumns]
    }
  },
  { deep: true }
)

// 初始化
initializeVisibleFields()
</script>

<style scoped>
/* 拖拽状态样式 */
.ghost {
  opacity: 0.4;
  background: #f0fdf4;
  border: 2px dashed #10b981;
}

.chosen {
  opacity: 0.9;
  cursor: grabbing;
}

.dragging {
  opacity: 0.8;
  transform: rotate(2deg);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.2);
  border-color: #10b981;
}

/* 字段项动画 */
.field-item {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.field-item:hover {
  transform: translateX(2px);
}

/* 拖拽手柄 */
.drag-handle {
  transition: color 0.2s;
}

.drag-handle:active {
  cursor: grabbing;
}

/* 输入框样式优化 */
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  opacity: 1;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .field-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .field-item > div:last-child {
    width: 100%;
    justify-content: space-between;
  }
}

/* 动画效果 */
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

.field-item {
  animation: fadeIn 0.3s ease-in-out;
}

/* 空状态提示 */
.min-h-[60px]:empty::after {
  content: "将可见字段拖拽到此处";
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  color: #9ca3af;
  font-size: 14px;
}
</style>