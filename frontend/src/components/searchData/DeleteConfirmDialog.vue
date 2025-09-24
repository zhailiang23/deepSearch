<template>
  <DialogRoot v-model:open="isOpen">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/30 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
      <DialogContent class="fixed left-[50%] top-[50%] z-50 w-full max-w-2xl translate-x-[-50%] translate-y-[-50%] border bg-white p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] rounded-lg dark:bg-gray-800 dark:border-gray-700 max-h-[90vh] flex flex-col">

        <!-- 对话框头部 -->
        <div class="flex flex-col space-y-3 flex-shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <div class="p-2 bg-red-100 rounded-lg mr-3">
                <AlertTriangle class="h-6 w-6 text-red-600" />
              </div>
              <DialogTitle class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
                确认删除数据记录
              </DialogTitle>
            </div>
            <DialogClose as-child>
              <button
                class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md"
                @click="closeDialog"
              >
                <X class="h-4 w-4" />
              </button>
            </DialogClose>
          </div>

          <!-- 警告信息 -->
          <div class="bg-red-50 border border-red-200 rounded-lg p-4">
            <div class="flex items-start">
              <AlertCircle class="h-5 w-5 text-red-500 mt-0.5 mr-3" />
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
        </div>

        <!-- 记录信息区域 -->
        <div class="flex-1 overflow-y-auto mt-4 space-y-4">
          <!-- 单个记录删除 -->
          <div v-if="!isBatchDelete && document" class="space-y-4">
            <!-- 文档基本信息 -->
            <div class="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <div class="flex items-center text-sm mb-3">
                <Database class="h-4 w-4 text-gray-600 mr-2" />
                <span class="font-medium text-gray-800">要删除的记录:</span>
              </div>
              <div class="space-y-2 text-sm">
                <div class="flex">
                  <span class="font-medium text-gray-600 w-16">ID:</span>
                  <span class="font-mono text-gray-800 bg-white px-2 py-1 rounded border">{{ document._id }}</span>
                </div>
                <div class="flex">
                  <span class="font-medium text-gray-600 w-16">索引:</span>
                  <span class="font-mono text-gray-800 bg-white px-2 py-1 rounded border">{{ document._index }}</span>
                </div>
                <div v-if="document._version" class="flex">
                  <span class="font-medium text-gray-600 w-16">版本:</span>
                  <span class="text-gray-800">{{ document._version }}</span>
                </div>
              </div>
            </div>

            <!-- 关键字段预览 -->
            <div v-if="keyFields.length > 0" class="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <div class="flex items-center text-sm mb-3">
                <Eye class="h-4 w-4 text-gray-600 mr-2" />
                <span class="font-medium text-gray-800">关键字段预览:</span>
              </div>
              <div class="space-y-2">
                <div
                  v-for="field in keyFields"
                  :key="field.key"
                  class="flex items-start"
                >
                  <span class="font-medium text-gray-600 text-sm w-24 flex-shrink-0 mt-1">{{ field.label }}:</span>
                  <div class="flex-1 min-w-0">
                    <div class="text-sm text-gray-800 bg-white px-2 py-1 rounded border break-all">
                      {{ formatFieldValue(field.value, field.type) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 批量删除 -->
          <div v-else-if="isBatchDelete && documents.length > 0" class="space-y-4">
            <div class="bg-gray-50 border border-gray-200 rounded-lg p-4">
              <div class="flex items-center text-sm mb-3">
                <Database class="h-4 w-4 text-gray-600 mr-2" />
                <span class="font-medium text-gray-800">批量删除:</span>
              </div>
              <div class="text-sm text-gray-700">
                即将删除 <span class="font-bold text-red-600">{{ documents.length }}</span> 条记录
              </div>
            </div>

            <!-- 记录列表预览 -->
            <div class="bg-gray-50 border border-gray-200 rounded-lg p-4 max-h-60 overflow-y-auto">
              <div class="space-y-2">
                <div
                  v-for="(doc, index) in documents.slice(0, 10)"
                  :key="doc._id"
                  class="flex items-center text-sm py-1 px-2 bg-white rounded border"
                >
                  <span class="font-mono text-xs text-gray-500 w-8">{{ index + 1 }}.</span>
                  <span class="font-mono text-xs text-gray-700 truncate mr-2">{{ doc._id }}</span>
                  <span class="text-gray-600 truncate">{{ getRecordPreview(doc) }}</span>
                </div>
                <div v-if="documents.length > 10" class="text-xs text-gray-500 text-center py-2">
                  ... 还有 {{ documents.length - 10 }} 条记录
                </div>
              </div>
            </div>
          </div>

          <!-- 删除选项 -->
          <div class="bg-orange-50 border border-orange-200 rounded-lg p-4">
            <div class="flex items-start">
              <Settings class="h-5 w-5 text-orange-500 mt-0.5 mr-3" />
              <div class="flex-1">
                <h3 class="text-sm font-medium text-orange-800 mb-2">删除选项</h3>
                <div class="space-y-2">
                  <label class="flex items-center text-sm text-orange-700">
                    <input
                      v-model="forceDelete"
                      type="checkbox"
                      class="rounded border-orange-300 text-orange-600 focus:ring-orange-500 mr-2"
                    >
                    强制删除（忽略版本冲突）
                  </label>
                  <p class="text-xs text-orange-600 ml-6">
                    勾选此选项将忽略文档版本检查，强制删除记录
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="flex-shrink-0 flex justify-end space-x-3 pt-4 border-t border-gray-200">
          <Button
            variant="outline"
            @click="closeDialog"
            :disabled="deleting"
            class="px-6"
          >
            取消
          </Button>
          <Button
            variant="destructive"
            @click="handleConfirmDelete"
            :disabled="deleting"
            class="px-6 bg-red-600 hover:bg-red-700 text-white"
          >
            <Trash2 v-if="!deleting" class="w-4 h-4 mr-2" />
            <div v-else class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
            {{ deleting ? '删除中...' : isBatchDelete ? `删除 ${documents.length} 条记录` : '确认删除' }}
          </Button>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogClose,
} from '@/components/ui/dialog'
import {
  AlertTriangle,
  AlertCircle,
  X,
  Database,
  Eye,
  Trash2,
  Settings
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

// 本地状态
const isOpen = ref(props.open)
const deleting = ref(false)
const forceDelete = ref(false)

// 计算属性
const isBatchDelete = computed(() => props.documents && props.documents.length > 1)

// 关键字段提取（用于单个记录预览）
const keyFields = computed(() => {
  if (!props.document?._source) return []

  const source = props.document._source
  const fields = []

  // 优先展示的字段类型
  const priorityFields = ['name', 'title', 'subject', 'content', 'description', 'email', 'username']

  // 查找优先字段
  for (const priority of priorityFields) {
    for (const [key, value] of Object.entries(source)) {
      if (key.toLowerCase().includes(priority.toLowerCase()) && value) {
        fields.push({
          key,
          label: key,
          value,
          type: typeof value
        })
        if (fields.length >= 3) break
      }
    }
    if (fields.length >= 3) break
  }

  // 如果优先字段不足，添加其他字段
  if (fields.length < 3) {
    for (const [key, value] of Object.entries(source)) {
      if (!fields.some(f => f.key === key) && value !== null && value !== undefined && value !== '') {
        fields.push({
          key,
          label: key,
          value,
          type: typeof value
        })
        if (fields.length >= 5) break
      }
    }
  }

  return fields
})

// 格式化字段值
function formatFieldValue(value: any, type: string): string {
  if (value === null || value === undefined) return '(空)'

  if (type === 'object') {
    return JSON.stringify(value, null, 2)
  }

  const str = String(value)
  // 限制显示长度
  if (str.length > 100) {
    return str.substring(0, 100) + '...'
  }

  return str
}

// 获取记录预览文本
function getRecordPreview(doc: TableRow): string {
  if (!doc._source) return '(无数据)'

  const source = doc._source

  // 查找可用于预览的字段
  const previewFields = ['name', 'title', 'subject', 'content', 'description']

  for (const field of previewFields) {
    for (const [key, value] of Object.entries(source)) {
      if (key.toLowerCase().includes(field.toLowerCase()) && value) {
        const str = String(value)
        return str.length > 50 ? str.substring(0, 50) + '...' : str
      }
    }
  }

  // 如果没有找到预览字段，返回第一个非空字段
  for (const [key, value] of Object.entries(source)) {
    if (value !== null && value !== undefined && String(value).trim()) {
      const str = String(value)
      return `${key}: ${str.length > 30 ? str.substring(0, 30) + '...' : str}`
    }
  }

  return '(无可预览内容)'
}

// 确认删除处理
async function handleConfirmDelete() {
  deleting.value = true
  try {
    emit('confirm', { forceDelete: forceDelete.value })
  } finally {
    // deleting状态由父组件控制重置
  }
}

// 关闭对话框
function closeDialog() {
  if (!deleting.value) {
    emit('update:open', false)
    emit('cancel')
    // 重置状态
    forceDelete.value = false
  }
}

// 监听props变化
watch(() => props.open, (newVal) => {
  isOpen.value = newVal
})

watch(() => props.loading, (newVal) => {
  deleting.value = newVal
})

watch(isOpen, (newVal) => {
  emit('update:open', newVal)
})
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
  box-shadow: 0 4px 8px rgba(220, 38, 38, 0.4);
  transform: translateY(-1px);
}

.button-destructive:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 滚动条样式 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .max-w-2xl {
    max-width: calc(100vw - 2rem);
    margin: 1rem;
  }

  .p-6 {
    padding: 1rem;
  }

  .space-x-3 > * + * {
    margin-left: 0.5rem;
  }
}

/* 警告区域样式增强 */
.bg-red-50 {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
}

.bg-orange-50 {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

/* 记录信息区域样式 */
.bg-gray-50 {
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
}
</style>