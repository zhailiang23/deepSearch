<template>
  <div class="hot-word-limit-selector">
    <!-- 组件标题 -->
    <div class="hot-word-limit-selector__header">
      <h4 class="hot-word-limit-selector__title">显示数量</h4>
      <span v-if="selectedLimit" class="limit-display">
        前 {{ selectedLimit }} 条
      </span>
    </div>

    <!-- 预设数量选项 -->
    <div class="hot-word-limit-selector__presets">
      <button
        v-for="preset in limitPresets"
        :key="preset.value"
        @click="selectLimit(preset.value)"
        :class="[
          'limit-button',
          { 'limit-button--active': selectedLimit === preset.value }
        ]"
        type="button"
        :disabled="disabled"
      >
        <span class="limit-value">{{ preset.label }}</span>
        <span v-if="preset.description" class="limit-description">
          {{ preset.description }}
        </span>
      </button>
    </div>

    <!-- 自定义数量输入 -->
    <div v-if="allowCustomLimit" class="hot-word-limit-selector__custom">
      <div class="custom-limit-header">
        <label for="custom-limit" class="custom-limit-label">自定义数量</label>
        <span class="custom-limit-range">({{ minLimit }} - {{ maxLimit }})</span>
      </div>

      <div class="custom-limit-input-group">
        <input
          id="custom-limit"
          v-model.number="customLimitInput"
          type="number"
          :min="minLimit"
          :max="maxLimit"
          step="1"
          class="custom-limit-input"
          placeholder="输入数量"
          :disabled="disabled"
          @input="validateCustomLimit"
          @blur="applyCustomLimit"
          @keypress.enter="applyCustomLimit"
        />
        <button
          @click="applyCustomLimit"
          :disabled="!isValidCustomLimit || disabled"
          class="apply-custom-button"
          type="button"
        >
          应用
        </button>
      </div>

      <!-- 自定义数量错误提示 -->
      <div v-if="customLimitError" class="custom-limit-error">
        <span class="error-message">{{ customLimitError }}</span>
      </div>
    </div>

    <!-- 显示模式选择 -->
    <div class="hot-word-limit-selector__display-mode">
      <label class="display-mode-label">显示模式</label>
      <div class="display-mode-options">
        <label
          v-for="mode in displayModes"
          :key="mode.value"
          class="display-mode-option"
        >
          <input
            v-model="selectedDisplayMode"
            :value="mode.value"
            type="radio"
            class="display-mode-radio"
            :disabled="disabled"
          />
          <span class="display-mode-text">{{ mode.label }}</span>
          <span v-if="mode.description" class="display-mode-description">
            {{ mode.description }}
          </span>
        </label>
      </div>
    </div>

    <!-- 性能提示 -->
    <div v-if="shouldShowPerformanceWarning" class="hot-word-limit-selector__warning">
      <div class="warning-content">
        <span class="warning-icon">⚠️</span>
        <span class="warning-text">
          显示过多数据可能影响页面性能，建议控制在{{ recommendedLimit }}条以内
        </span>
      </div>
    </div>

    <!-- 数据量预估 -->
    <div v-if="estimatedDataSize && selectedLimit" class="hot-word-limit-selector__estimate">
      <div class="estimate-content">
        <span class="estimate-label">预计数据量:</span>
        <span class="estimate-value">{{ formatDataSize(estimatedDataSize) }}</span>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="hot-word-limit-selector__quick-actions">
      <button
        @click="selectLimit(50)"
        :class="['quick-action', { 'quick-action--active': selectedLimit === 50 }]"
        type="button"
        :disabled="disabled"
      >
        标准视图 (50)
      </button>
      <button
        @click="selectLimit(100)"
        :class="['quick-action', { 'quick-action--active': selectedLimit === 100 }]"
        type="button"
        :disabled="disabled"
      >
        详细视图 (100)
      </button>
      <button
        @click="selectLimit(20)"
        :class="['quick-action', { 'quick-action--active': selectedLimit === 20 }]"
        type="button"
        :disabled="disabled"
      >
        精简视图 (20)
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

export interface LimitConfig {
  limit: number
  displayMode: 'table' | 'card' | 'compact'
}

interface LimitPreset {
  label: string
  value: number
  description?: string
  recommended?: boolean
}

interface DisplayMode {
  label: string
  value: 'table' | 'card' | 'compact'
  description?: string
}

interface Props {
  modelValue?: LimitConfig | null
  disabled?: boolean
  allowCustomLimit?: boolean
  minLimit?: number
  maxLimit?: number
  recommendedLimit?: number
  estimatedDataSize?: number // 预估的数据大小(字节)
}

interface Emits {
  (e: 'update:modelValue', value: LimitConfig): void
  (e: 'change', value: LimitConfig): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  allowCustomLimit: true,
  minLimit: 5,
  maxLimit: 1000,
  recommendedLimit: 100,
  estimatedDataSize: 0
})

const emit = defineEmits<Emits>()

// 响应式数据
const selectedLimit = ref(50)
const selectedDisplayMode = ref<'table' | 'card' | 'compact'>('table')
const customLimitInput = ref<number | null>(null)
const customLimitError = ref('')

// 预设选项
const limitPresets = computed<LimitPreset[]>(() => [
  { label: '10条', value: 10, description: '快速预览' },
  { label: '20条', value: 20, description: '精简显示' },
  { label: '50条', value: 50, description: '标准显示', recommended: true },
  { label: '100条', value: 100, description: '详细显示' },
  { label: '200条', value: 200, description: '大量数据' },
  { label: '500条', value: 500, description: '完整数据' }
])

const displayModes: DisplayMode[] = [
  { label: '表格', value: 'table', description: '表格形式显示' },
  { label: '卡片', value: 'card', description: '卡片形式显示' },
  { label: '紧凑', value: 'compact', description: '紧凑列表显示' }
]

// 计算属性
const shouldShowPerformanceWarning = computed(() => {
  return selectedLimit.value > props.recommendedLimit
})

const isValidCustomLimit = computed(() => {
  return customLimitInput.value !== null &&
         customLimitInput.value >= props.minLimit &&
         customLimitInput.value <= props.maxLimit &&
         Number.isInteger(customLimitInput.value)
})

// 方法
const selectLimit = (limit: number) => {
  selectedLimit.value = limit
  customLimitError.value = ''
  emitChange()
}

const validateCustomLimit = () => {
  customLimitError.value = ''

  if (customLimitInput.value === null || customLimitInput.value === undefined) {
    return
  }

  if (!Number.isInteger(customLimitInput.value)) {
    customLimitError.value = '请输入整数'
    return
  }

  if (customLimitInput.value < props.minLimit) {
    customLimitError.value = `数量不能小于 ${props.minLimit}`
    return
  }

  if (customLimitInput.value > props.maxLimit) {
    customLimitError.value = `数量不能大于 ${props.maxLimit}`
    return
  }
}

const applyCustomLimit = () => {
  validateCustomLimit()

  if (isValidCustomLimit.value && customLimitInput.value !== null) {
    selectedLimit.value = customLimitInput.value
    customLimitInput.value = null
    emitChange()
  }
}

const formatDataSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const emitChange = () => {
  const config: LimitConfig = {
    limit: selectedLimit.value,
    displayMode: selectedDisplayMode.value
  }

  emit('update:modelValue', config)
  emit('change', config)
}

// 初始化组件
const initializeComponent = () => {
  if (props.modelValue) {
    selectedLimit.value = props.modelValue.limit
    selectedDisplayMode.value = props.modelValue.displayMode
  }
}

// 监听显示模式变化
watch(selectedDisplayMode, () => {
  emitChange()
})

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    initializeComponent()
  }
}, { deep: true })

// 组件挂载时初始化
initializeComponent()
</script>

<style scoped>
.hot-word-limit-selector {
  @apply bg-white rounded-lg border border-green-200 p-4 space-y-4;
}

/* 组件头部 */
.hot-word-limit-selector__header {
  @apply flex items-center justify-between;
}

.hot-word-limit-selector__title {
  @apply text-sm font-medium text-gray-700;
}

.limit-display {
  @apply text-xs text-green-600 font-medium bg-green-50 px-2 py-1 rounded;
}

/* 预设选项 */
.hot-word-limit-selector__presets {
  @apply grid grid-cols-2 md:grid-cols-3 gap-2;
}

.limit-button {
  @apply p-3 text-left border border-gray-300 rounded-md hover:border-green-300 hover:bg-green-50 transition-colors duration-200;
}

.limit-button--active {
  @apply border-green-300 bg-green-50 ring-2 ring-green-200;
}

.limit-value {
  @apply block text-sm font-medium text-gray-700;
}

.limit-description {
  @apply block text-xs text-gray-500 mt-1;
}

/* 自定义数量输入 */
.hot-word-limit-selector__custom {
  @apply space-y-2 border-t border-gray-100 pt-4;
}

.custom-limit-header {
  @apply flex items-center justify-between;
}

.custom-limit-label {
  @apply text-xs font-medium text-gray-600;
}

.custom-limit-range {
  @apply text-xs text-gray-500;
}

.custom-limit-input-group {
  @apply flex space-x-2;
}

.custom-limit-input {
  @apply flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500;
}

.apply-custom-button {
  @apply px-4 py-2 text-sm font-medium bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed;
}

.custom-limit-error {
  @apply flex items-center;
}

.error-message {
  @apply text-xs text-red-600;
}

/* 显示模式选择 */
.hot-word-limit-selector__display-mode {
  @apply space-y-2 border-t border-gray-100 pt-4;
}

.display-mode-label {
  @apply block text-xs font-medium text-gray-600;
}

.display-mode-options {
  @apply space-y-2;
}

.display-mode-option {
  @apply flex items-start space-x-2 cursor-pointer;
}

.display-mode-radio {
  @apply mt-0.5 w-4 h-4 text-green-600 border-gray-300 focus:ring-green-500;
}

.display-mode-text {
  @apply text-sm font-medium text-gray-700;
}

.display-mode-description {
  @apply text-xs text-gray-500 ml-1;
}

/* 性能警告 */
.hot-word-limit-selector__warning {
  @apply bg-yellow-50 border border-yellow-200 rounded-md p-3;
}

.warning-content {
  @apply flex items-center space-x-2;
}

.warning-icon {
  @apply text-yellow-600;
}

.warning-text {
  @apply text-xs text-yellow-800;
}

/* 数据量预估 */
.hot-word-limit-selector__estimate {
  @apply bg-blue-50 border border-blue-200 rounded-md p-3;
}

.estimate-content {
  @apply flex items-center space-x-2;
}

.estimate-label {
  @apply text-xs font-medium text-blue-700;
}

.estimate-value {
  @apply text-xs text-blue-600 font-mono;
}

/* 快速操作 */
.hot-word-limit-selector__quick-actions {
  @apply flex flex-wrap gap-2 border-t border-gray-100 pt-4;
}

.quick-action {
  @apply px-3 py-2 text-xs font-medium rounded-md border border-gray-300 bg-white text-gray-700 hover:bg-green-50 hover:border-green-300 hover:text-green-700 transition-colors duration-200;
}

.quick-action--active {
  @apply bg-green-50 border-green-300 text-green-700;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .hot-word-limit-selector {
    @apply p-3 space-y-3;
  }

  .hot-word-limit-selector__presets {
    @apply grid-cols-1;
  }

  .custom-limit-input-group {
    @apply flex-col space-x-0 space-y-2;
  }

  .hot-word-limit-selector__quick-actions {
    @apply flex-col;
  }

  .quick-action {
    @apply w-full text-center;
  }
}

/* 动画效果 */
.hot-word-limit-selector {
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

.limit-button:hover {
  transform: translateY(-1px);
}

.quick-action:hover:not(:disabled) {
  transform: translateY(-1px);
}

/* 焦点状态 */
.custom-limit-input:focus {
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

/* 禁用状态 */
.hot-word-limit-selector:has([disabled]) {
  @apply opacity-60;
}

.hot-word-limit-selector [disabled] {
  @apply cursor-not-allowed;
}

/* 推荐选项高亮 */
.limit-button:has(.limit-value:contains('50条')) {
  @apply border-green-300 bg-green-50;
}

/* 悬停状态增强 */
.limit-button:hover:not(:disabled) {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.apply-custom-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
</style>