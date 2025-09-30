<template>
  <!-- 只有当open为true时才渲染 -->
  <div
    v-if="open"
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    @click.self="closeDialog"
  >
    <div class="bg-white rounded-lg shadow-xl max-w-5xl w-full max-h-[90vh] flex flex-col">

      <!-- 对话框头部 -->
      <div class="flex items-center justify-between p-6 border-b flex-shrink-0">
        <div class="flex items-center">
          <div class="p-2 bg-emerald-100 rounded-lg mr-3">
            <Settings class="h-6 w-6 text-emerald-600" />
          </div>
          <div>
            <h3 class="text-xl font-semibold text-gray-900">
              字段管理
            </h3>
            <p class="text-sm text-gray-600 mt-1">
              配置表格列的显示顺序、宽度和固定位置
            </p>
          </div>
        </div>
        <button
          class="p-2 text-gray-400 hover:text-gray-600 rounded-md"
          @click="closeDialog"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- FieldManager 组件 -->
      <div class="flex-1 overflow-auto px-6 py-4">
        <FieldManager
          :all-columns="allColumns"
          :visible-columns="localColumns"
          @update:columns="handleColumnsUpdate"
        />
      </div>

      <!-- 底部操作按钮 -->
      <div class="flex items-center justify-between px-6 py-4 border-t bg-gray-50 flex-shrink-0">
        <div class="flex items-center gap-2">
          <button
            @click="handleReset"
            class="px-4 py-2 text-sm text-gray-700 hover:bg-gray-200 rounded-md transition-colors flex items-center"
          >
            <RotateCcw class="w-4 h-4 mr-1.5" />
            重置为默认
          </button>
          <button
            @click="handleClearConfig"
            class="px-4 py-2 text-sm text-red-600 hover:bg-red-50 border border-red-200 rounded-md transition-colors flex items-center"
          >
            <Trash2 class="w-4 h-4 mr-1.5" />
            清除配置
          </button>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="closeDialog"
            class="px-6 py-2 text-gray-700 hover:bg-gray-100 rounded-md transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSave"
            class="px-6 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-md transition-colors flex items-center"
          >
            <Check class="w-4 h-4 mr-1.5" />
            保存配置
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Settings, RotateCcw, Trash2, X, Check } from 'lucide-vue-next'
import FieldManager from './FieldManager.vue'
import type { TableColumn } from '@/types/tableData'

interface Props {
  open: boolean
  allColumns: TableColumn[]
  visibleColumns: TableColumn[]
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'save', columns: TableColumn[]): void
  (e: 'reset'): void
  (e: 'clear'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 本地列配置副本
const localColumns = ref<TableColumn[]>([])

// 监听外部 open 状态变化
watch(
  () => props.open,
  (newVal) => {
    if (newVal) {
      // 对话框打开时，重新加载当前配置
      localColumns.value = [...props.visibleColumns]
      console.log('ColumnConfigDialog opened with columns:', localColumns.value.length)
    }
  },
  { immediate: true }
)

/**
 * 处理列配置更新
 */
function handleColumnsUpdate(newColumns: TableColumn[]) {
  localColumns.value = newColumns
}

/**
 * 保存配置
 */
function handleSave() {
  console.log('Saving column config:', localColumns.value.length)
  emit('save', localColumns.value)
  closeDialog()
}

/**
 * 关闭对话框
 */
function closeDialog() {
  emit('update:open', false)
}

/**
 * 重置为默认
 */
function handleReset() {
  console.log('Resetting column config')
  emit('reset')
}

/**
 * 清除配置
 */
function handleClearConfig() {
  console.log('Clearing column config')
  emit('clear')
}
</script>

<style scoped>
/* 滚动条样式 */
.overflow-auto::-webkit-scrollbar {
  width: 8px;
}

.overflow-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.overflow-auto::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 4px;
}

.overflow-auto::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}
</style>