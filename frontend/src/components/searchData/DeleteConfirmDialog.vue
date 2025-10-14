<template>
  <!-- 只有当open为true时才渲染 -->
  <div
    v-if="open"
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    @click.self="handleCancel"
  >
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full flex flex-col border">

      <!-- 对话框头部 -->
      <div class="flex flex-col space-y-3 flex-shrink-0 p-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <div class="p-2 bg-red-100 rounded-lg mr-3">
              <AlertTriangle class="h-6 w-6 text-red-600" />
            </div>
            <h3 class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
              确认删除数据记录
            </h3>
          </div>
          <button
            class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md transition-colors"
            @click="handleCancel"
            :disabled="isDeleting"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- 警告信息 -->
        <div class="bg-red-50 border border-red-200 rounded-lg p-4">
          <div class="flex items-start">
            <AlertCircle class="h-5 w-5 text-red-500 mt-0.5 mr-3 flex-shrink-0" />
            <div class="flex-1">
              <h3 class="text-sm font-medium text-red-800 mb-1">
                这是一个不可逆的操作
              </h3>
              <p class="text-sm text-red-700">
                删除后的数据将从Elasticsearch索引中永久移除，无法恢复。请确认您真的要删除{{ isBatchDelete ? '这些' : '这个' }}数据记录。
              </p>
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="flex justify-end space-x-3 pt-4">
          <button
            @click="handleCancel"
            :disabled="isDeleting"
            class="px-6 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            取消
          </button>
          <button
            @click="handleConfirmDelete"
            :disabled="isDeleting"
            class="px-6 py-2 bg-red-600 hover:bg-red-700 text-white rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
          >
            <Trash2 v-if="!isDeleting" class="w-4 h-4 mr-2" />
            <div v-else class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
            {{ isDeleting ? '删除中...' : isBatchDelete ? `删除 ${documents.length} 条记录` : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import {
  AlertTriangle,
  AlertCircle,
  X,
  Trash2
} from 'lucide-vue-next'
import type { TableRow } from '@/types/tableData'

interface Props {
  open: boolean
  document?: TableRow | null
  documents?: TableRow[]
  loading?: boolean
}

interface Emits {
  (e: 'update:open', open: boolean): void
  (e: 'confirm', options: { forceDelete: boolean }): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  documents: () => []
})

const emit = defineEmits<Emits>()

// 使用父组件传递的loading状态
const isDeleting = computed(() => props.loading)

// 计算属性
const isBatchDelete = computed(() => props.documents && props.documents.length > 1)

// 监听loading状态变化，当loading从true变为false时自动关闭对话框
watch(() => props.loading, (newVal, oldVal) => {
  // 当loading从true变为false时，说明删除操作已完成
  if (oldVal === true && newVal === false && props.open) {
    // 自动关闭对话框
    emit('update:open', false)
  }
})

// 确认删除处理
function handleConfirmDelete() {
  if (isDeleting.value) return
  emit('confirm', { forceDelete: true })
}

// 取消删除
function handleCancel() {
  if (isDeleting.value) return
  emit('update:open', false)
  emit('cancel')
}
</script>

<style scoped>
/* 对话框动画优化 */
.dialog-overlay {
  backdrop-filter: blur(2px);
}

/* 危险按钮样式 */
.button-destructive {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 2px 4px rgba(220, 38, 38, 0.3);
}

.button-destructive:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
}

/* 滚动条优化 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>