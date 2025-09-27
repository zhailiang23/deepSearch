<template>
  <div class="date-time-picker">
    <div v-if="type === 'datetimerange'" class="datetime-range">
      <!-- 开始时间 -->
      <div class="datetime-input-wrapper">
        <label v-if="startLabel" class="datetime-label">{{ startLabel }}</label>
        <div class="datetime-input">
          <input
            v-model="localStartValue"
            type="datetime-local"
            :class="inputClass"
            :placeholder="startPlaceholder"
            :disabled="disabled"
            @change="handleStartChange"
          />
          <button
            v-if="clearable && localStartValue"
            @click="clearStart"
            class="clear-btn"
            type="button"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- 分隔符 -->
      <div class="range-separator">至</div>

      <!-- 结束时间 -->
      <div class="datetime-input-wrapper">
        <label v-if="endLabel" class="datetime-label">{{ endLabel }}</label>
        <div class="datetime-input">
          <input
            v-model="localEndValue"
            type="datetime-local"
            :class="inputClass"
            :placeholder="endPlaceholder"
            :disabled="disabled"
            @change="handleEndChange"
          />
          <button
            v-if="clearable && localEndValue"
            @click="clearEnd"
            class="clear-btn"
            type="button"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="single-datetime">
      <div class="datetime-input-wrapper">
        <label v-if="label" class="datetime-label">{{ label }}</label>
        <div class="datetime-input">
          <input
            v-model="localValue"
            :type="inputType"
            :class="inputClass"
            :placeholder="placeholder"
            :disabled="disabled"
            @change="handleChange"
          />
          <button
            v-if="clearable && localValue"
            @click="clear"
            class="clear-btn"
            type="button"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  // 单值模式的值
  modelValue?: string
  // 范围模式的开始值
  start?: string
  // 范围模式的结束值
  end?: string
  // 选择器类型
  type?: 'datetime' | 'date' | 'time' | 'datetimerange'
  // 占位符
  placeholder?: string
  // 范围模式的占位符
  startPlaceholder?: string
  endPlaceholder?: string
  // 标签
  label?: string
  startLabel?: string
  endLabel?: string
  // 是否可清空
  clearable?: boolean
  // 是否禁用
  disabled?: boolean
  // 自定义类名
  class?: string
  // 错误信息
  error?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'update:start', value: string): void
  (e: 'update:end', value: string): void
  (e: 'change', value: string | { start: string; end: string }): void
}

const props = withDefaults(defineProps<Props>(), {
  type: 'datetime',
  clearable: false,
  disabled: false,
  placeholder: '请选择时间',
  startPlaceholder: '开始时间',
  endPlaceholder: '结束时间'
})

const emit = defineEmits<Emits>()

// 本地值
const localValue = ref(props.modelValue || '')
const localStartValue = ref(props.start || '')
const localEndValue = ref(props.end || '')

// 计算输入框类型
const inputType = computed(() => {
  switch (props.type) {
    case 'date':
      return 'date'
    case 'time':
      return 'time'
    case 'datetime':
    case 'datetimerange':
    default:
      return 'datetime-local'
  }
})

// 输入框样式类
const inputClass = computed(() => {
  return cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors',
    'file:border-0 file:bg-transparent file:text-sm file:font-medium',
    'placeholder:text-muted-foreground',
    'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
    'disabled:cursor-not-allowed disabled:opacity-50',
    props.error && 'border-red-500 focus-visible:ring-red-500',
    props.class
  )
})

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  localValue.value = newValue || ''
})

watch(() => props.start, (newValue) => {
  localStartValue.value = newValue || ''
})

watch(() => props.end, (newValue) => {
  localEndValue.value = newValue || ''
})

// 处理单值变化
const handleChange = () => {
  emit('update:modelValue', localValue.value)
  emit('change', localValue.value)
}

// 处理开始时间变化
const handleStartChange = () => {
  emit('update:start', localStartValue.value)
  emit('change', {
    start: localStartValue.value,
    end: localEndValue.value
  })
}

// 处理结束时间变化
const handleEndChange = () => {
  emit('update:end', localEndValue.value)
  emit('change', {
    start: localStartValue.value,
    end: localEndValue.value
  })
}

// 清空值
const clear = () => {
  localValue.value = ''
  emit('update:modelValue', '')
  emit('change', '')
}

const clearStart = () => {
  localStartValue.value = ''
  emit('update:start', '')
  emit('change', {
    start: '',
    end: localEndValue.value
  })
}

const clearEnd = () => {
  localEndValue.value = ''
  emit('update:end', '')
  emit('change', {
    start: localStartValue.value,
    end: ''
  })
}

// 格式化日期时间为ISO字符串
const formatDateTime = (value: string): string => {
  if (!value) return ''
  try {
    return new Date(value).toISOString()
  } catch {
    return value
  }
}
</script>

<style scoped>
.date-time-picker {
  @apply w-full;
}

.datetime-range {
  @apply flex items-center space-x-3;
}

.single-datetime {
  @apply w-full;
}

.datetime-input-wrapper {
  @apply flex-1 min-w-0;
}

.datetime-label {
  @apply block text-sm font-medium text-gray-700 mb-1;
}

.datetime-input {
  @apply relative;
}

.datetime-input input {
  @apply pr-8;
}

.clear-btn {
  @apply absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors;
}

.range-separator {
  @apply text-gray-500 text-sm font-medium px-2 pt-6;
}

.error-message {
  @apply mt-1 text-sm text-red-600;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .datetime-range {
    @apply flex-col space-x-0 space-y-3;
  }

  .range-separator {
    @apply pt-0 text-center;
  }
}
</style>