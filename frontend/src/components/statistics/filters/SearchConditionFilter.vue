<template>
  <div class="search-condition-filter">
    <!-- 组件标题 -->
    <div class="search-condition-filter__header">
      <h4 class="search-condition-filter__title">搜索条件</h4>
      <button
        v-if="hasActiveFilters"
        @click="clearAllFilters"
        class="search-condition-filter__clear"
        type="button"
      >
        清除全部
      </button>
    </div>

    <!-- 关键词输入 -->
    <div class="search-condition-filter__section">
      <label for="keywords" class="section-label">关键词过滤</label>
      <div class="keywords-input-group">
        <input
          id="keywords"
          v-model="keywordInput"
          @keypress.enter="addKeyword"
          type="text"
          placeholder="输入关键词，按回车添加"
          class="keywords-input"
          :disabled="disabled"
        />
        <button
          @click="addKeyword"
          :disabled="!keywordInput.trim() || disabled"
          class="add-keyword-button"
          type="button"
        >
          添加
        </button>
      </div>

      <!-- 关键词标签列表 -->
      <div v-if="keywords.length > 0" class="keywords-list">
        <div
          v-for="(keyword, index) in keywords"
          :key="index"
          class="keyword-tag"
        >
          <span class="keyword-text">{{ keyword }}</span>
          <button
            @click="removeKeyword(index)"
            class="keyword-remove"
            type="button"
            :disabled="disabled"
          >
            ×
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索空间过滤 -->
    <div class="search-condition-filter__section">
      <label for="search-space" class="section-label">搜索空间</label>
      <select
        id="search-space"
        v-model="selectedSearchSpace"
        class="search-space-select"
        :disabled="disabled"
      >
        <option value="">全部搜索空间</option>
        <option
          v-for="space in searchSpaceOptions"
          :key="space.value"
          :value="space.value"
        >
          {{ space.label }}
        </option>
      </select>
    </div>

    <!-- 用户类型过滤 -->
    <div class="search-condition-filter__section">
      <label class="section-label">用户类型</label>
      <div class="user-type-options">
        <label
          v-for="userType in userTypeOptions"
          :key="userType.value"
          class="user-type-option"
        >
          <input
            v-model="selectedUserTypes"
            :value="userType.value"
            type="checkbox"
            class="user-type-checkbox"
            :disabled="disabled"
          />
          <span class="user-type-label">{{ userType.label }}</span>
          <span v-if="userType.description" class="user-type-description">
            {{ userType.description }}
          </span>
        </label>
      </div>
    </div>

    <!-- 搜索频次过滤 -->
    <div class="search-condition-filter__section">
      <label class="section-label">搜索频次范围</label>
      <div class="frequency-range">
        <div class="frequency-input-group">
          <label for="min-frequency" class="frequency-label">最小频次</label>
          <input
            id="min-frequency"
            v-model.number="minFrequency"
            type="number"
            min="1"
            :max="maxFrequency || undefined"
            class="frequency-input"
            placeholder="1"
            :disabled="disabled"
          />
        </div>
        <div class="frequency-separator">-</div>
        <div class="frequency-input-group">
          <label for="max-frequency" class="frequency-label">最大频次</label>
          <input
            id="max-frequency"
            v-model.number="maxFrequency"
            type="number"
            :min="minFrequency || 1"
            class="frequency-input"
            placeholder="不限制"
            :disabled="disabled"
          />
        </div>
      </div>
    </div>

    <!-- 排序方式 -->
    <div class="search-condition-filter__section">
      <label for="sort-order" class="section-label">排序方式</label>
      <select
        id="sort-order"
        v-model="sortOrder"
        class="sort-order-select"
        :disabled="disabled"
      >
        <option
          v-for="option in sortOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </option>
      </select>
    </div>

    <!-- 当前过滤条件总结 -->
    <div v-if="hasActiveFilters" class="search-condition-filter__summary">
      <div class="summary-header">
        <span class="summary-title">当前过滤条件</span>
      </div>
      <div class="summary-content">
        <div v-if="keywords.length > 0" class="summary-item">
          <span class="summary-label">关键词:</span>
          <span class="summary-value">{{ keywords.join(', ') }}</span>
        </div>
        <div v-if="selectedSearchSpace" class="summary-item">
          <span class="summary-label">搜索空间:</span>
          <span class="summary-value">{{ getSearchSpaceLabel(selectedSearchSpace) }}</span>
        </div>
        <div v-if="selectedUserTypes.length > 0" class="summary-item">
          <span class="summary-label">用户类型:</span>
          <span class="summary-value">{{ getUserTypesLabel() }}</span>
        </div>
        <div v-if="minFrequency || maxFrequency" class="summary-item">
          <span class="summary-label">搜索频次:</span>
          <span class="summary-value">{{ getFrequencyRangeLabel() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

export interface SearchCondition {
  keywords: string[]
  searchSpace?: string
  userTypes: string[]
  minFrequency?: number
  maxFrequency?: number
  sortOrder: string
}

interface SearchSpaceOption {
  label: string
  value: string
  description?: string
}

interface UserTypeOption {
  label: string
  value: string
  description?: string
}

interface SortOption {
  label: string
  value: string
}

interface Props {
  modelValue?: SearchCondition | null
  disabled?: boolean
  searchSpaceOptions?: SearchSpaceOption[]
  maxKeywords?: number
}

interface Emits {
  (e: 'update:modelValue', value: SearchCondition): void
  (e: 'change', value: SearchCondition): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  searchSpaceOptions: () => [],
  maxKeywords: 10
})

const emit = defineEmits<Emits>()

// 响应式数据
const keywordInput = ref('')
const keywords = ref<string[]>([])
const selectedSearchSpace = ref('')
const selectedUserTypes = ref<string[]>([])
const minFrequency = ref<number | null>(null)
const maxFrequency = ref<number | null>(null)
const sortOrder = ref('frequency_desc')

// 静态选项数据
const userTypeOptions: UserTypeOption[] = [
  {
    label: '普通用户',
    value: 'normal',
    description: '一般用户搜索行为'
  },
  {
    label: '高频用户',
    value: 'frequent',
    description: '搜索频次较高的用户'
  },
  {
    label: '新用户',
    value: 'new',
    description: '近期注册的新用户'
  },
  {
    label: '活跃用户',
    value: 'active',
    description: '近期活跃度较高的用户'
  }
]

const sortOptions: SortOption[] = [
  { label: '搜索频次(高到低)', value: 'frequency_desc' },
  { label: '搜索频次(低到高)', value: 'frequency_asc' },
  { label: '最近搜索时间', value: 'recent_time' },
  { label: '首次搜索时间', value: 'first_time' },
  { label: '关键词长度', value: 'keyword_length' },
  { label: '字母顺序', value: 'alphabetical' }
]

// 计算属性
const hasActiveFilters = computed(() => {
  return keywords.value.length > 0 ||
         selectedSearchSpace.value ||
         selectedUserTypes.value.length > 0 ||
         minFrequency.value !== null ||
         maxFrequency.value !== null ||
         sortOrder.value !== 'frequency_desc'
})

// 方法
const addKeyword = () => {
  const keyword = keywordInput.value.trim()
  if (!keyword) return

  if (keywords.value.includes(keyword)) {
    keywordInput.value = ''
    return
  }

  if (keywords.value.length >= props.maxKeywords) {
    // 可以在这里添加提示逻辑
    return
  }

  keywords.value.push(keyword)
  keywordInput.value = ''
  emitChange()
}

const removeKeyword = (index: number) => {
  keywords.value.splice(index, 1)
  emitChange()
}

const clearAllFilters = () => {
  keywords.value = []
  selectedSearchSpace.value = ''
  selectedUserTypes.value = []
  minFrequency.value = null
  maxFrequency.value = null
  sortOrder.value = 'frequency_desc'
  keywordInput.value = ''
  emitChange()
}

const getSearchSpaceLabel = (value: string): string => {
  const option = props.searchSpaceOptions.find(opt => opt.value === value)
  return option?.label || value
}

const getUserTypesLabel = (): string => {
  return selectedUserTypes.value
    .map(type => userTypeOptions.find(opt => opt.value === type)?.label || type)
    .join(', ')
}

const getFrequencyRangeLabel = (): string => {
  if (minFrequency.value && maxFrequency.value) {
    return `${minFrequency.value} - ${maxFrequency.value}`
  } else if (minFrequency.value) {
    return `≥ ${minFrequency.value}`
  } else if (maxFrequency.value) {
    return `≤ ${maxFrequency.value}`
  }
  return ''
}

const emitChange = () => {
  const condition: SearchCondition = {
    keywords: [...keywords.value],
    searchSpace: selectedSearchSpace.value || undefined,
    userTypes: [...selectedUserTypes.value],
    minFrequency: minFrequency.value || undefined,
    maxFrequency: maxFrequency.value || undefined,
    sortOrder: sortOrder.value
  }

  emit('update:modelValue', condition)
  emit('change', condition)
}

// 初始化组件
const initializeComponent = () => {
  if (props.modelValue) {
    keywords.value = [...(props.modelValue.keywords || [])]
    selectedSearchSpace.value = props.modelValue.searchSpace || ''
    selectedUserTypes.value = [...(props.modelValue.userTypes || [])]
    minFrequency.value = props.modelValue.minFrequency || null
    maxFrequency.value = props.modelValue.maxFrequency || null
    sortOrder.value = props.modelValue.sortOrder || 'frequency_desc'
  }
}

// 监听数据变化
watch([selectedSearchSpace, selectedUserTypes, minFrequency, maxFrequency, sortOrder], () => {
  emitChange()
}, { deep: true })

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    initializeComponent()
  }
}, { deep: true })

// 组件挂载时初始化
initializeComponent()
</script>

<style scoped>
.search-condition-filter {
  @apply bg-white rounded-lg border border-green-200 p-4 space-y-4;
}

/* 组件头部 */
.search-condition-filter__header {
  @apply flex items-center justify-between;
}

.search-condition-filter__title {
  @apply text-sm font-medium text-gray-700;
}

.search-condition-filter__clear {
  @apply text-xs text-red-600 hover:text-red-700 font-medium;
}

/* 各个部分 */
.search-condition-filter__section {
  @apply space-y-2;
}

.section-label {
  @apply block text-xs font-medium text-gray-600;
}

/* 关键词输入 */
.keywords-input-group {
  @apply flex space-x-2;
}

.keywords-input {
  @apply flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500;
}

.add-keyword-button {
  @apply px-4 py-2 text-sm font-medium bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed;
}

/* 关键词标签列表 */
.keywords-list {
  @apply flex flex-wrap gap-2;
}

.keyword-tag {
  @apply inline-flex items-center px-3 py-1 text-sm bg-green-100 text-green-800 rounded-full;
}

.keyword-text {
  @apply mr-1;
}

.keyword-remove {
  @apply ml-1 text-green-600 hover:text-green-800 font-bold text-lg leading-none;
}

/* 选择器样式 */
.search-space-select,
.sort-order-select {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500;
}

/* 用户类型选项 */
.user-type-options {
  @apply space-y-2;
}

.user-type-option {
  @apply flex items-start space-x-2 cursor-pointer;
}

.user-type-checkbox {
  @apply mt-0.5 w-4 h-4 text-green-600 border-gray-300 rounded focus:ring-green-500;
}

.user-type-label {
  @apply text-sm font-medium text-gray-700;
}

.user-type-description {
  @apply text-xs text-gray-500 ml-1;
}

/* 频次范围 */
.frequency-range {
  @apply flex items-center space-x-2;
}

.frequency-input-group {
  @apply flex-1 space-y-1;
}

.frequency-label {
  @apply block text-xs text-gray-500;
}

.frequency-input {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500;
}

.frequency-separator {
  @apply text-gray-500 font-medium;
}

/* 过滤条件总结 */
.search-condition-filter__summary {
  @apply border-t border-gray-100 pt-4 space-y-2;
}

.summary-header {
  @apply flex items-center;
}

.summary-title {
  @apply text-xs font-medium text-gray-600;
}

.summary-content {
  @apply space-y-1;
}

.summary-item {
  @apply flex items-start space-x-2;
}

.summary-label {
  @apply text-xs font-medium text-gray-500 whitespace-nowrap;
}

.summary-value {
  @apply text-xs text-green-700 font-medium;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .search-condition-filter {
    @apply p-3 space-y-3;
  }

  .keywords-input-group {
    @apply flex-col space-x-0 space-y-2;
  }

  .frequency-range {
    @apply flex-col space-x-0 space-y-2;
  }

  .frequency-separator {
    @apply self-center;
  }

  .summary-item {
    @apply flex-col space-x-0 space-y-1;
  }
}

/* 动画效果 */
.search-condition-filter {
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

.keyword-tag {
  animation: slideIn 0.2s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 悬停效果 */
.add-keyword-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.keyword-tag:hover {
  @apply bg-green-200;
}

/* 焦点状态 */
.keywords-input:focus,
.frequency-input:focus,
.search-space-select:focus,
.sort-order-select:focus {
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
}

/* 禁用状态 */
.search-condition-filter:has([disabled]) {
  @apply opacity-60;
}

.search-condition-filter [disabled] {
  @apply cursor-not-allowed;
}
</style>