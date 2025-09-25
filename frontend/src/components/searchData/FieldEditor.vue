<template>
  <div class="field-editor space-y-2">
    <!-- 字段标签 -->
    <label
      :for="fieldId"
      class="block text-sm font-medium text-gray-700"
      :class="{ 'text-red-600': hasError }"
    >
      {{ field.label }}
      <span v-if="field.required" class="text-red-500">*</span>
      <span class="text-xs text-gray-500 font-normal ml-1">({{ field.esType }})</span>
    </label>

    <!-- 文本类型字段 -->
    <template v-if="field.type === 'text' || field.type === 'keyword'">
      <Input
        :id="fieldId"
        v-model="internalValue"
        :placeholder="getPlaceholder()"
        :class="inputClass"
        :disabled="disabled"
        @blur="validateField"
      />
    </template>

    <!-- 数字类型字段 -->
    <template v-else-if="field.type === 'number'">
      <Input
        :id="fieldId"
        v-model.number="internalValue"
        type="number"
        :placeholder="getPlaceholder()"
        :class="inputClass"
        :disabled="disabled"
        @blur="validateField"
      />
    </template>

    <!-- 日期类型字段 -->
    <template v-else-if="field.type === 'date'">
      <div class="relative">
        <Input
          :id="fieldId"
          v-model="dateInputValue"
          type="datetime-local"
          :class="inputClass"
          :disabled="disabled"
          @blur="validateField"
        />
        <CalendarIcon class="absolute right-3 top-2.5 h-4 w-4 text-gray-400" />
      </div>
    </template>

    <!-- 布尔类型字段 -->
    <template v-else-if="field.type === 'boolean'">
      <div class="flex items-center space-x-3">
        <label class="flex items-center cursor-pointer">
          <input
            :id="fieldId"
            v-model="internalValue"
            type="checkbox"
            :disabled="disabled"
            class="h-4 w-4 text-emerald-600 border-gray-300 rounded focus:ring-emerald-500 focus:ring-2"
            @change="validateField"
          />
          <span class="ml-2 text-sm text-gray-600">
            {{ internalValue ? '是' : '否' }}
          </span>
        </label>
      </div>
    </template>

    <!-- 对象类型字段 -->
    <template v-else-if="field.type === 'object'">
      <div class="border border-gray-200 rounded-md p-3 bg-gray-50">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm text-gray-600">JSON对象</span>
          <Button
            variant="ghost"
            size="sm"
            @click="toggleJsonMode"
            class="text-xs h-6 px-2"
          >
            {{ isJsonMode ? '表单模式' : 'JSON模式' }}
          </Button>
        </div>

        <!-- JSON编辑模式 -->
        <template v-if="isJsonMode">
          <textarea
            :id="fieldId"
            v-model="jsonValue"
            :disabled="disabled"
            rows="4"
            class="w-full text-sm font-mono border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
            :class="{ 'border-red-300 bg-red-50': hasJsonError }"
            @blur="validateJson"
            placeholder="请输入有效的JSON格式"
          />
          <div v-if="jsonError" class="text-xs text-red-600 mt-1">
            {{ jsonError }}
          </div>
        </template>

        <!-- 表单编辑模式（如果有嵌套属性） -->
        <template v-else>
          <div v-if="objectFields.length > 0" class="space-y-2">
            <FieldEditor
              v-for="subField in objectFields"
              :key="subField.key"
              :field="subField"
              :model-value="internalValue?.[subField.key]"
              @update:model-value="updateObjectField(subField.key, $event)"
              :disabled="disabled"
              class="ml-2"
            />
          </div>
          <div v-else class="text-sm text-gray-500 italic">
            无嵌套字段定义
          </div>
        </template>
      </div>
    </template>

    <!-- 嵌套类型字段 -->
    <template v-else-if="field.type === 'nested'">
      <div class="border border-gray-200 rounded-md p-3 bg-blue-50">
        <div class="flex justify-between items-center mb-2">
          <span class="text-sm text-gray-600">嵌套数组</span>
          <Button
            variant="ghost"
            size="sm"
            @click="addNestedItem"
            :disabled="disabled"
            class="text-xs h-6 px-2"
          >
            <Plus class="h-3 w-3 mr-1" />
            添加项
          </Button>
        </div>

        <div v-if="nestedItems.length > 0" class="space-y-3">
          <div
            v-for="(item, index) in nestedItems"
            :key="index"
            class="border border-gray-200 rounded p-2 bg-white"
          >
            <div class="flex justify-between items-center mb-2">
              <span class="text-xs text-gray-500">项目 {{ index + 1 }}</span>
              <Button
                variant="ghost"
                size="sm"
                @click="removeNestedItem(index)"
                :disabled="disabled"
                class="text-xs h-5 px-1 text-red-600 hover:bg-red-50"
              >
                <Trash2 class="h-3 w-3" />
              </Button>
            </div>

            <div v-if="nestedFields.length > 0" class="space-y-2">
              <FieldEditor
                v-for="subField in nestedFields"
                :key="subField.key"
                :field="subField"
                :model-value="item?.[subField.key]"
                @update:model-value="updateNestedField(index, subField.key, $event)"
                :disabled="disabled"
                class="ml-2"
              />
            </div>
          </div>
        </div>

        <div v-else class="text-sm text-gray-500 italic">
          暂无嵌套项
        </div>
      </div>
    </template>

    <!-- 未知类型字段 -->
    <template v-else>
      <Input
        :id="fieldId"
        v-model="internalValue"
        :placeholder="`未知类型: ${field.type}`"
        :class="inputClass"
        :disabled="disabled"
        @blur="validateField"
      />
    </template>

    <!-- 验证错误信息 -->
    <div v-if="hasError && errorMessage" class="text-xs text-red-600">
      {{ errorMessage }}
    </div>

    <!-- 字段帮助信息 -->
    <div v-if="field.help" class="text-xs text-gray-500">
      {{ field.help }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Calendar as CalendarIcon, Plus, Trash2 } from 'lucide-vue-next'
import type { TableColumn } from '@/types/tableData'

export interface FieldEditorField extends Omit<TableColumn, 'key'> {
  key: string
  label: string
  type: 'text' | 'keyword' | 'date' | 'number' | 'boolean' | 'object' | 'nested'
  required?: boolean
  help?: string
  validation?: {
    min?: number
    max?: number
    pattern?: string
    custom?: (value: any) => string | null
  }
  properties?: Record<string, FieldEditorField> // 用于object和nested类型
}

interface Props {
  field: FieldEditorField
  modelValue: any
  disabled?: boolean
}

interface Emits {
  (e: 'update:model-value', value: any): void
  (e: 'validation-change', field: string, isValid: boolean, message?: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 组件状态
const fieldId = computed(() => `field_${props.field.key}`)
const internalValue = ref(props.modelValue)
const isJsonMode = ref(false)
const jsonValue = ref('')
const jsonError = ref('')
const errorMessage = ref('')

// 验证状态
const hasError = computed(() => !!errorMessage.value)
const hasJsonError = computed(() => !!jsonError.value)

// 样式类
const inputClass = computed(() => [
  'transition-colors duration-200',
  {
    'border-red-300 bg-red-50 focus:ring-red-500': hasError.value,
    'border-emerald-300 focus:ring-emerald-500': !hasError.value,
  }
])

// 日期处理
const dateInputValue = computed({
  get: () => {
    if (!internalValue.value) return ''
    const date = new Date(internalValue.value)
    if (isNaN(date.getTime())) return ''

    // 转换为本地时间的 datetime-local 格式
    const offset = date.getTimezoneOffset() * 60000
    const localDate = new Date(date.getTime() - offset)
    return localDate.toISOString().slice(0, 16)
  },
  set: (value: string) => {
    if (!value) {
      internalValue.value = null
    } else {
      internalValue.value = new Date(value).toISOString()
    }
    emit('update:model-value', internalValue.value)
  }
})

// 对象类型处理
const objectFields = computed(() => {
  if (!props.field.properties) return []
  return Object.entries(props.field.properties).map(([key, field]) => ({
    ...field,
    key
  }))
})

// 嵌套类型处理
const nestedFields = computed(() => objectFields.value)
const nestedItems = ref<Array<Record<string, any>>>(
  Array.isArray(internalValue.value) ? internalValue.value : []
)

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  internalValue.value = newValue

  // 更新JSON字符串
  if (props.field.type === 'object' && typeof newValue === 'object') {
    jsonValue.value = JSON.stringify(newValue, null, 2)
  }

  // 更新嵌套数组
  if (props.field.type === 'nested' && Array.isArray(newValue)) {
    nestedItems.value = newValue
  }
}, { immediate: true })

// 监听内部值变化
watch(internalValue, (newValue) => {
  emit('update:model-value', newValue)
}, { deep: true })

// 获取占位符文本
function getPlaceholder(): string {
  if (props.field.type === 'text') return '请输入文本内容'
  if (props.field.type === 'keyword') return '请输入关键字'
  if (props.field.type === 'number') return '请输入数字'
  return `请输入${props.field.label}`
}

// 字段验证
function validateField() {
  errorMessage.value = ''

  const value = internalValue.value
  const validation = props.field.validation

  // 必填验证
  if (props.field.required && (value === null || value === undefined || value === '')) {
    errorMessage.value = `${props.field.label}为必填项`
    emit('validation-change', props.field.key, false, errorMessage.value)
    return
  }

  // 如果没有值且不是必填，跳过验证
  if (!value && !props.field.required) {
    emit('validation-change', props.field.key, true)
    return
  }

  // 类型特定验证
  if (validation) {
    if (props.field.type === 'number' && typeof value === 'number') {
      if (validation.min !== undefined && value < validation.min) {
        errorMessage.value = `最小值为 ${validation.min}`
      } else if (validation.max !== undefined && value > validation.max) {
        errorMessage.value = `最大值为 ${validation.max}`
      }
    }

    if (props.field.type === 'text' || props.field.type === 'keyword') {
      if (validation.min !== undefined && String(value).length < validation.min) {
        errorMessage.value = `最少需要 ${validation.min} 个字符`
      } else if (validation.max !== undefined && String(value).length > validation.max) {
        errorMessage.value = `最多允许 ${validation.max} 个字符`
      } else if (validation.pattern && !new RegExp(validation.pattern).test(String(value))) {
        errorMessage.value = '格式不正确'
      }
    }

    // 自定义验证
    if (validation.custom) {
      const customError = validation.custom(value)
      if (customError) {
        errorMessage.value = customError
      }
    }
  }

  const isValid = !errorMessage.value
  emit('validation-change', props.field.key, isValid, errorMessage.value)
}

// JSON模式切换
function toggleJsonMode() {
  if (isJsonMode.value) {
    // 从JSON模式切换到表单模式
    try {
      if (jsonValue.value.trim()) {
        const parsed = JSON.parse(jsonValue.value)
        internalValue.value = parsed
      }
      jsonError.value = ''
      isJsonMode.value = false
    } catch (error) {
      jsonError.value = '无效的JSON格式，无法切换到表单模式'
    }
  } else {
    // 从表单模式切换到JSON模式
    jsonValue.value = JSON.stringify(internalValue.value || {}, null, 2)
    isJsonMode.value = true
  }
}

// JSON验证
function validateJson() {
  jsonError.value = ''

  if (!jsonValue.value.trim()) {
    internalValue.value = null
    return
  }

  try {
    const parsed = JSON.parse(jsonValue.value)
    internalValue.value = parsed
    jsonError.value = ''
  } catch (error) {
    jsonError.value = '无效的JSON格式'
  }
}

// 对象字段更新
function updateObjectField(key: string, value: any) {
  if (!internalValue.value) {
    internalValue.value = {}
  }
  internalValue.value[key] = value
  emit('update:model-value', internalValue.value)
}

// 嵌套项管理
function addNestedItem() {
  const newItem: Record<string, any> = {}
  nestedFields.value.forEach(field => {
    newItem[field.key] = null
  })

  nestedItems.value.push(newItem)
  internalValue.value = [...nestedItems.value]
  emit('update:model-value', internalValue.value)
}

function removeNestedItem(index: number) {
  nestedItems.value.splice(index, 1)
  internalValue.value = [...nestedItems.value]
  emit('update:model-value', internalValue.value)
}

function updateNestedField(itemIndex: number, fieldKey: string, value: any) {
  if (nestedItems.value[itemIndex]) {
    nestedItems.value[itemIndex][fieldKey] = value
    internalValue.value = [...nestedItems.value]
    emit('update:model-value', internalValue.value)
  }
}
</script>

<style scoped>
/* 自定义样式 */
.field-editor {
  --emerald-color: #10b981;
  --emerald-light: #6ee7b7;
  --emerald-dark: #047857;
}

/* JSON编辑器样式 */
textarea.font-mono {
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Consolas, 'Liberation Mono', Menlo, monospace;
}

/* 嵌套组件边距调整 */
.field-editor .field-editor {
  margin-left: 0.5rem;
  padding-left: 0.75rem;
  border-left: 2px solid #e5e7eb;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .field-editor .field-editor {
    margin-left: 0.25rem;
    padding-left: 0.5rem;
  }
}
</style>