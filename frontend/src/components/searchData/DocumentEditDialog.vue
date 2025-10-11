<template>
  <DialogRoot v-model:open="isOpen">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/20 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
      <DialogContent class="fixed left-[50%] top-[50%] z-50 w-full max-w-4xl translate-x-[-50%] translate-y-[-50%] border bg-white p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] rounded-lg dark:bg-gray-800 dark:border-gray-700 max-h-[90vh] flex flex-col">

        <!-- 对话框头部 -->
        <div class="flex flex-col space-y-2 flex-shrink-0">
          <div class="flex items-center justify-between">
            <DialogTitle class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
              编辑数据记录
            </DialogTitle>
            <DialogClose as-child>
              <button
                class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md"
                @click="closeDialog"
              >
                <X class="h-4 w-4" />
              </button>
            </DialogClose>
          </div>
          <DialogDescription class="text-sm text-gray-500 dark:text-gray-400">
            编辑此数据记录的字段值。请注意，保存后更改将立即同步到Elasticsearch索引。
          </DialogDescription>

          <!-- 文档信息 -->
          <div class="bg-emerald-50 border border-emerald-200 rounded-lg p-3 mt-2">
            <div class="flex items-center text-sm">
              <Database class="h-4 w-4 text-emerald-600 mr-2" />
              <span class="font-medium text-emerald-800">文档ID:</span>
              <span class="ml-2 font-mono text-emerald-700">{{ document?._id }}</span>
              <span class="mx-2 text-emerald-600">|</span>
              <span class="font-medium text-emerald-800">索引:</span>
              <span class="ml-2 font-mono text-emerald-700">{{ document?._index }}</span>
              <span v-if="document?._version" class="mx-2 text-emerald-600">|</span>
              <span v-if="document?._version" class="font-medium text-emerald-800">版本:</span>
              <span v-if="document?._version" class="ml-2 text-emerald-700">{{ document._version }}</span>
            </div>
          </div>
        </div>

        <!-- 编辑表单 -->
        <div class="flex-1 overflow-y-auto mt-4 space-y-4">
          <form @submit.prevent="handleSubmit">
            <!-- 字段编辑区域 -->
            <div class="space-y-6">
              <template v-if="editableFields.length > 0">
                <div
                  v-for="field in editableFields"
                  :key="field.key"
                  class="border border-gray-200 rounded-lg p-4 hover:border-emerald-300 transition-colors"
                >
                  <FieldEditor
                    :field="field"
                    :model-value="formData[field.key]"
                    @update:model-value="updateFieldValue(field.key, $event)"
                    @validation-change="handleFieldValidation"
                    :disabled="isSubmitting"
                  />
                </div>
              </template>

              <!-- 无可编辑字段 -->
              <template v-else>
                <div class="text-center py-8">
                  <AlertTriangle class="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <div class="text-lg text-gray-500 mb-2">无可编辑字段</div>
                  <div class="text-sm text-gray-400">该文档没有可编辑的字段</div>
                </div>
              </template>
            </div>

            <!-- 表单验证错误 -->
            <div v-if="hasValidationErrors" class="bg-red-50 border border-red-200 rounded-lg p-4 mt-4">
              <div class="flex items-center text-red-800 mb-2">
                <AlertTriangle class="h-4 w-4 mr-2" />
                <span class="font-medium">表单验证失败</span>
              </div>
              <ul class="text-sm text-red-700 space-y-1">
                <li v-for="error in validationErrors" :key="error.field">
                  <strong>{{ error.field }}:</strong> {{ error.message }}
                </li>
              </ul>
            </div>

            <!-- 提交错误 -->
            <div v-if="submitError" class="bg-red-50 border border-red-200 rounded-lg p-4 mt-4">
              <div class="flex items-center text-red-800">
                <AlertTriangle class="h-4 w-4 mr-2" />
                <span class="font-medium">保存失败</span>
              </div>
              <div class="text-sm text-red-700 mt-1">{{ submitError }}</div>
            </div>
          </form>
        </div>

        <!-- 底部按钮 -->
        <div class="flex justify-between items-center pt-6 border-t border-gray-200 dark:border-gray-600 flex-shrink-0">
          <!-- 左侧信息 -->
          <div class="flex items-center text-sm text-gray-500">
            <Clock class="h-4 w-4 mr-1" />
            <span>最后修改: {{ formatDate(document?._source?.updated_at) }}</span>
          </div>

          <!-- 右侧按钮 -->
          <div class="flex space-x-3">
            <!-- 重置按钮 -->
            <Button
              type="button"
              variant="outline"
              @click="resetForm"
              :disabled="isSubmitting || !hasChanges"
              class="border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              <RotateCcw class="h-4 w-4 mr-2" />
              重置
            </Button>

            <!-- 取消按钮 -->
            <DialogClose as-child>
              <Button
                type="button"
                variant="outline"
                @click="closeDialog"
                :disabled="isSubmitting"
                class="border-gray-300 text-gray-700 hover:bg-gray-50"
              >
                取消
              </Button>
            </DialogClose>

            <!-- 保存按钮 -->
            <Button
              type="button"
              @click="handleSubmit"
              :disabled="isSubmitting || hasValidationErrors || !hasChanges"
              class="bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50"
            >
              <template v-if="isSubmitting">
                <Loader2 class="h-4 w-4 mr-2 animate-spin" />
                保存中...
              </template>
              <template v-else>
                <Save class="h-4 w-4 mr-2" />
                保存更改
              </template>
            </Button>
          </div>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, nextTick } from 'vue'
import {
  X,
  Database,
  AlertTriangle,
  Clock,
  Save,
  RotateCcw,
  Loader2
} from 'lucide-vue-next'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogClose
} from 'reka-ui'
import { Button } from '@/components/ui/button'
import FieldEditor, { type FieldEditorField } from './FieldEditor.vue'
import { searchDataService, type UpdateDocumentRequest } from '@/services/searchDataService'
import type { TableRow, TableColumn, ESIndexMapping } from '@/types/tableData'

interface Props {
  open: boolean
  document: TableRow | null
  mapping?: ESIndexMapping
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'save-success', document: TableRow): void
  (e: 'save-error', error: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 组件状态
const isSubmitting = ref(false)
const submitError = ref('')
const validationErrors = ref<Array<{ field: string; message: string }>>([])
const formData = reactive<Record<string, any>>({})
const originalData = ref<Record<string, any>>({})

// 计算属性
const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

const hasValidationErrors = computed(() => validationErrors.value.length > 0)

const hasChanges = computed(() => {
  if (!props.document) return false

  return Object.keys(formData).some(key => {
    const current = formData[key]
    const original = originalData.value[key]
    return JSON.stringify(current) !== JSON.stringify(original)
  })
})

// 生成可编辑字段
const editableFields = computed((): FieldEditorField[] => {
  if (!props.mapping?.mappings?.properties || !props.document) return []

  const fields: FieldEditorField[] = []
  const properties = props.mapping.mappings.properties

  Object.entries(properties).forEach(([fieldName, fieldMapping]) => {
    // 跳过系统字段
    if (fieldName.startsWith('_') || fieldName === 'created_at') {
      return
    }

    const field: FieldEditorField = {
      key: fieldName,
      label: fieldName,
      type: mapESTypeToFieldType(fieldMapping.type),
      sortable: false,
      filterable: false,
      visible: true,
      resizable: false,
      esField: fieldName,
      esType: fieldMapping.type,
      format: fieldMapping.format,
      required: false, // 根据业务需求可配置
      help: getFieldHelp(fieldMapping.type),
      validation: getFieldValidation(fieldMapping.type),
      properties: mapNestedProperties(fieldMapping.properties)
    }

    fields.push(field)
  })

  // 按字段名排序
  return fields.sort((a, b) => a.label.localeCompare(b.label))
})

// 工具函数
function mapESTypeToFieldType(esType: string): FieldEditorField['type'] {
  const typeMap: Record<string, FieldEditorField['type']> = {
    text: 'text',
    keyword: 'keyword',
    date: 'date',
    long: 'number',
    integer: 'number',
    double: 'number',
    float: 'number',
    boolean: 'boolean',
    object: 'object',
    nested: 'nested',
    dense_vector: 'object' // 向量字段作为对象(JSON数组)处理
  }
  return typeMap[esType] || 'text'
}

function getFieldHelp(esType: string): string {
  const helpMap: Record<string, string> = {
    text: '全文搜索文本字段，支持分词和搜索',
    keyword: '精确匹配关键字字段，不进行分词',
    date: '日期时间字段，支持多种日期格式',
    long: '长整型数字字段',
    integer: '整型数字字段',
    double: '双精度浮点数字段',
    float: '单精度浮点数字段',
    boolean: '布尔值字段，true或false',
    object: 'JSON对象字段，可包含嵌套属性',
    nested: '嵌套数组字段，包含多个对象',
    dense_vector: '向量字段，用于语义搜索的数字数组'
  }
  return helpMap[esType] || '未知字段类型'
}

function getFieldValidation(esType: string) {
  const validationMap: Record<string, any> = {
    text: {
      max: 1000 // 文本字段最大长度限制
    },
    keyword: {
      max: 256 // 关键字字段最大长度限制
    },
    integer: {
      min: -2147483648,
      max: 2147483647
    },
    long: {
      min: -9223372036854775808,
      max: 9223372036854775807
    }
  }
  return validationMap[esType]
}

function mapNestedProperties(properties?: Record<string, any>): Record<string, FieldEditorField> | undefined {
  if (!properties) return undefined

  const nestedFields: Record<string, FieldEditorField> = {}

  Object.entries(properties).forEach(([key, mapping]) => {
    nestedFields[key] = {
      key,
      label: key,
      type: mapESTypeToFieldType(mapping.type),
      sortable: false,
      filterable: false,
      visible: true,
      resizable: false,
      esField: key,
      esType: mapping.type,
      format: mapping.format,
      help: getFieldHelp(mapping.type)
    }
  })

  return nestedFields
}

function formatDate(dateString?: string): string {
  if (!dateString) return '未知'

  try {
    return new Date(dateString).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateString
  }
}

// 表单处理
function initializeForm() {
  if (!props.document) return

  // 清空表单数据
  Object.keys(formData).forEach(key => {
    delete formData[key]
  })

  // 复制文档数据到表单
  const sourceData = props.document._source || {}
  Object.assign(formData, JSON.parse(JSON.stringify(sourceData)))

  // 保存原始数据用于重置和变更检测
  originalData.value = JSON.parse(JSON.stringify(sourceData))

  // 清空验证错误
  validationErrors.value = []
  submitError.value = ''
}

function updateFieldValue(fieldKey: string, value: any) {
  formData[fieldKey] = value
}

function handleFieldValidation(field: string, isValid: boolean, message?: string) {
  const existingIndex = validationErrors.value.findIndex(error => error.field === field)

  if (isValid) {
    // 移除验证错误
    if (existingIndex >= 0) {
      validationErrors.value.splice(existingIndex, 1)
    }
  } else {
    // 添加或更新验证错误
    const error = { field, message: message || '验证失败' }
    if (existingIndex >= 0) {
      validationErrors.value[existingIndex] = error
    } else {
      validationErrors.value.push(error)
    }
  }
}

function resetForm() {
  initializeForm()
}

async function handleSubmit() {
  if (!props.document || isSubmitting.value || hasValidationErrors.value || !hasChanges.value) {
    return
  }

  isSubmitting.value = true
  submitError.value = ''

  try {
    const updateRequest: UpdateDocumentRequest = {
      id: props.document._id,
      index: props.document._index,
      source: { ...formData },
      version: props.document._version // 乐观锁
    }

    const response = await searchDataService.updateDocument(updateRequest)

    // 创建更新后的文档对象
    const updatedDocument: TableRow = {
      ...props.document,
      _source: { ...formData },
      _version: response.version
    }

    // 更新原始数据
    originalData.value = JSON.parse(JSON.stringify(formData))

    // 发出成功事件
    emit('save-success', updatedDocument)

    // 关闭对话框
    closeDialog()

  } catch (error: any) {
    console.error('保存文档失败:', error)

    if (error.status === 409) {
      submitError.value = '文档已被其他用户修改，请刷新后重试'
    } else if (error.status === 404) {
      submitError.value = '文档不存在，可能已被删除'
    } else {
      submitError.value = error.message || '保存失败，请稍后重试'
    }

    emit('save-error', submitError.value)
  } finally {
    isSubmitting.value = false
  }
}

function closeDialog() {
  isOpen.value = false
}

// 监听文档变化，重新初始化表单
watch(() => props.document, (newDoc) => {
  if (newDoc) {
    nextTick(() => {
      initializeForm()
    })
  }
}, { immediate: true })

// 监听对话框开启，重新初始化表单
watch(isOpen, (open) => {
  if (open && props.document) {
    nextTick(() => {
      initializeForm()
    })
  }
})
</script>

<style scoped>
/* 对话框内容优化 */
.document-edit-dialog {
  --emerald-color: #10b981;
  --emerald-light: #6ee7b7;
  --emerald-dark: #047857;
}

/* 表单区域滚动条优化 */
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

/* 字段边框悬停效果 */
.border:hover {
  transition: border-color 0.2s ease-in-out;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .max-w-4xl {
    max-width: 95vw;
  }

  .p-6 {
    padding: 1rem;
  }

  .space-x-3 > * + * {
    margin-left: 0.5rem;
  }
}

/* 加载动画优化 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>