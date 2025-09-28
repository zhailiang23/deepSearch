<template>
  <div class="time-range-selector">
    <!-- 组件标题 -->
    <div class="time-range-selector__header">
      <h4 class="time-range-selector__title">时间范围</h4>
      <button
        v-if="hasCustomRange"
        @click="resetToDefault"
        class="time-range-selector__reset"
        type="button"
      >
        重置
      </button>
    </div>

    <!-- 预设时间范围选项 -->
    <div class="time-range-selector__presets">
      <button
        v-for="preset in presetOptions"
        :key="preset.value"
        @click="selectPreset(preset)"
        :class="[
          'preset-button',
          { 'preset-button--active': selectedPreset === preset.value }
        ]"
        type="button"
      >
        {{ preset.label }}
      </button>
    </div>

    <!-- 自定义日期范围 -->
    <div v-if="showCustomRange" class="time-range-selector__custom">
      <div class="custom-range-header">
        <span class="custom-range-label">自定义范围</span>
      </div>

      <div class="custom-range-inputs">
        <!-- 开始日期 -->
        <div class="date-input-group">
          <label for="start-date" class="date-label">开始日期</label>
          <input
            id="start-date"
            v-model="customStartDate"
            type="date"
            :max="customEndDate || today"
            class="date-input"
            @change="validateDateRange"
          />
        </div>

        <!-- 结束日期 -->
        <div class="date-input-group">
          <label for="end-date" class="date-label">结束日期</label>
          <input
            id="end-date"
            v-model="customEndDate"
            type="date"
            :min="customStartDate"
            :max="today"
            class="date-input"
            @change="validateDateRange"
          />
        </div>
      </div>

      <!-- 自定义范围操作按钮 -->
      <div class="custom-range-actions">
        <button
          @click="applyCustomRange"
          :disabled="!isValidCustomRange"
          class="action-button action-button--primary"
          type="button"
        >
          应用
        </button>
        <button
          @click="cancelCustomRange"
          class="action-button action-button--secondary"
          type="button"
        >
          取消
        </button>
      </div>
    </div>

    <!-- 显示当前选择的时间范围 -->
    <div v-if="!showCustomRange" class="time-range-selector__display">
      <div class="selected-range">
        <span class="range-label">已选择:</span>
        <span class="range-value">{{ displayedRange }}</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="time-range-selector__error">
      <span class="error-message">{{ errorMessage }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

export interface TimeRange {
  start: Date
  end: Date
  label?: string
}

export interface TimeRangePreset {
  label: string
  value: string
  getDates: () => { start: Date; end: Date }
}

interface Props {
  modelValue?: TimeRange | null
  disabled?: boolean
  allowCustomRange?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: TimeRange | null): void
  (e: 'change', value: TimeRange | null): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  allowCustomRange: true
})

const emit = defineEmits<Emits>()

// 响应式数据
const selectedPreset = ref<string>('7d')
const showCustomRange = ref(false)
const customStartDate = ref('')
const customEndDate = ref('')
const errorMessage = ref('')

// 计算属性
const today = computed(() => {
  return new Date().toISOString().split('T')[0]
})

const presetOptions = computed<TimeRangePreset[]>(() => [
  {
    label: '今天',
    value: 'today',
    getDates: () => {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const end = new Date(today)
      end.setHours(23, 59, 59, 999)
      return { start: today, end }
    }
  },
  {
    label: '最近7天',
    value: '7d',
    getDates: () => {
      const end = new Date()
      end.setHours(23, 59, 59, 999)
      const start = new Date(end)
      start.setDate(start.getDate() - 6)
      start.setHours(0, 0, 0, 0)
      return { start, end }
    }
  },
  {
    label: '最近30天',
    value: '30d',
    getDates: () => {
      const end = new Date()
      end.setHours(23, 59, 59, 999)
      const start = new Date(end)
      start.setDate(start.getDate() - 29)
      start.setHours(0, 0, 0, 0)
      return { start, end }
    }
  },
  {
    label: '本月',
    value: 'thisMonth',
    getDates: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      start.setHours(0, 0, 0, 0)
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
      end.setHours(23, 59, 59, 999)
      return { start, end }
    }
  },
  {
    label: '上月',
    value: 'lastMonth',
    getDates: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      start.setHours(0, 0, 0, 0)
      const end = new Date(now.getFullYear(), now.getMonth(), 0)
      end.setHours(23, 59, 59, 999)
      return { start, end }
    }
  },
  ...(props.allowCustomRange ? [{
    label: '自定义',
    value: 'custom',
    getDates: () => ({ start: new Date(), end: new Date() })
  }] : [])
])

const hasCustomRange = computed(() => {
  return selectedPreset.value === 'custom' && customStartDate.value && customEndDate.value
})

const isValidCustomRange = computed(() => {
  if (!customStartDate.value || !customEndDate.value) return false

  const start = new Date(customStartDate.value)
  const end = new Date(customEndDate.value)
  return start <= end && end <= new Date()
})

const displayedRange = computed(() => {
  if (selectedPreset.value === 'custom' && hasCustomRange.value) {
    const start = new Date(customStartDate.value)
    const end = new Date(customEndDate.value)
    return `${formatDate(start)} - ${formatDate(end)}`
  }

  const preset = presetOptions.value.find(p => p.value === selectedPreset.value)
  if (preset && preset.value !== 'custom') {
    const { start, end } = preset.getDates()
    return `${formatDate(start)} - ${formatDate(end)}`
  }

  return '请选择时间范围'
})

// 方法
const formatDate = (date: Date): string => {
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const selectPreset = (preset: TimeRangePreset) => {
  selectedPreset.value = preset.value
  errorMessage.value = ''

  if (preset.value === 'custom') {
    showCustomRange.value = true
    // 设置默认的自定义日期范围为最近7天
    const defaultRange = presetOptions.value.find(p => p.value === '7d')?.getDates()
    if (defaultRange) {
      customStartDate.value = defaultRange.start.toISOString().split('T')[0]
      customEndDate.value = defaultRange.end.toISOString().split('T')[0]
    }
  } else {
    showCustomRange.value = false
    const { start, end } = preset.getDates()
    const timeRange: TimeRange = { start, end, label: preset.label }
    emit('update:modelValue', timeRange)
    emit('change', timeRange)
  }
}

const validateDateRange = () => {
  errorMessage.value = ''

  if (!customStartDate.value || !customEndDate.value) return

  const start = new Date(customStartDate.value)
  const end = new Date(customEndDate.value)

  if (start > end) {
    errorMessage.value = '开始日期不能晚于结束日期'
    return
  }

  if (end > new Date()) {
    errorMessage.value = '结束日期不能超过今天'
    return
  }

  const daysDiff = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)
  if (daysDiff > 365) {
    errorMessage.value = '时间范围不能超过365天'
  }
}

const applyCustomRange = () => {
  if (!isValidCustomRange.value) return

  const start = new Date(customStartDate.value)
  start.setHours(0, 0, 0, 0)
  const end = new Date(customEndDate.value)
  end.setHours(23, 59, 59, 999)

  const timeRange: TimeRange = {
    start,
    end,
    label: `${formatDate(start)} - ${formatDate(end)}`
  }

  showCustomRange.value = false
  emit('update:modelValue', timeRange)
  emit('change', timeRange)
}

const cancelCustomRange = () => {
  showCustomRange.value = false
  // 重置到默认的预设选项
  selectedPreset.value = '7d'
  const defaultPreset = presetOptions.value.find(p => p.value === '7d')
  if (defaultPreset) {
    selectPreset(defaultPreset)
  }
}

const resetToDefault = () => {
  selectedPreset.value = '7d'
  showCustomRange.value = false
  customStartDate.value = ''
  customEndDate.value = ''
  errorMessage.value = ''

  const defaultPreset = presetOptions.value.find(p => p.value === '7d')
  if (defaultPreset) {
    selectPreset(defaultPreset)
  }
}

// 初始化
const initializeComponent = () => {
  if (props.modelValue) {
    // 如果有初始值，尝试匹配到预设选项
    const preset = presetOptions.value.find(p => {
      if (p.value === 'custom') return false
      const { start, end } = p.getDates()
      return Math.abs(start.getTime() - props.modelValue!.start.getTime()) < 1000 &&
             Math.abs(end.getTime() - props.modelValue!.end.getTime()) < 1000
    })

    if (preset) {
      selectedPreset.value = preset.value
    } else {
      // 自定义范围
      selectedPreset.value = 'custom'
      customStartDate.value = props.modelValue.start.toISOString().split('T')[0]
      customEndDate.value = props.modelValue.end.toISOString().split('T')[0]
    }
  } else {
    // 默认选择最近7天
    const defaultPreset = presetOptions.value.find(p => p.value === '7d')
    if (defaultPreset) {
      selectPreset(defaultPreset)
    }
  }
}

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  if (!newValue) {
    resetToDefault()
  }
}, { deep: true })

// 组件挂载时初始化
initializeComponent()
</script>

<style scoped>
.time-range-selector {
  @apply bg-white rounded-lg border border-green-200 p-4 space-y-4;
}

/* 组件头部 */
.time-range-selector__header {
  @apply flex items-center justify-between;
}

.time-range-selector__title {
  @apply text-sm font-medium text-gray-700;
}

.time-range-selector__reset {
  @apply text-xs text-green-600 hover:text-green-700 font-medium;
}

/* 预设选项 */
.time-range-selector__presets {
  @apply flex flex-wrap gap-2;
}

.preset-button {
  @apply px-3 py-2 text-sm font-medium rounded-md border border-gray-300 bg-white text-gray-700 hover:bg-green-50 hover:border-green-300 hover:text-green-700 transition-colors duration-200;
}

.preset-button--active {
  @apply bg-green-50 border-green-300 text-green-700;
}

/* 自定义范围 */
.time-range-selector__custom {
  @apply space-y-4 border-t border-gray-100 pt-4;
}

.custom-range-header {
  @apply flex items-center;
}

.custom-range-label {
  @apply text-sm font-medium text-gray-700;
}

.custom-range-inputs {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.date-input-group {
  @apply space-y-2;
}

.date-label {
  @apply block text-xs font-medium text-gray-600;
}

.date-input {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500;
}

/* 自定义范围操作按钮 */
.custom-range-actions {
  @apply flex space-x-2;
}

.action-button {
  @apply px-4 py-2 text-sm font-medium rounded-md transition-colors duration-200;
}

.action-button--primary {
  @apply bg-green-600 text-white hover:bg-green-700 disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed;
}

.action-button--secondary {
  @apply bg-white text-gray-700 border border-gray-300 hover:bg-gray-50;
}

/* 当前选择显示 */
.time-range-selector__display {
  @apply border-t border-gray-100 pt-4;
}

.selected-range {
  @apply flex items-center space-x-2;
}

.range-label {
  @apply text-sm font-medium text-gray-600;
}

.range-value {
  @apply text-sm text-green-700 font-medium;
}

/* 错误提示 */
.time-range-selector__error {
  @apply flex items-center space-x-1;
}

.error-message {
  @apply text-sm text-red-600;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .time-range-selector {
    @apply p-3 space-y-3;
  }

  .time-range-selector__presets {
    @apply flex-col;
  }

  .preset-button {
    @apply w-full justify-center;
  }

  .custom-range-inputs {
    @apply grid-cols-1;
  }

  .custom-range-actions {
    @apply flex-col space-x-0 space-y-2;
  }

  .action-button {
    @apply w-full;
  }
}

/* 动画效果 */
.time-range-selector {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 悬停效果 */
.preset-button:hover {
  transform: translateY(-1px);
}

.action-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

/* 焦点状态 */
.date-input:focus {
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

/* 禁用状态 */
.time-range-selector:has([disabled]) {
  @apply opacity-60 pointer-events-none;
}
</style>