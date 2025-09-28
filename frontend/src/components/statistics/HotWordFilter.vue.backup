<template>
  <div class="hot-word-filter">
    <!-- 过滤器标题栏 -->
    <div class="hot-word-filter__header">
      <div class="filter-header-content">
        <h3 class="filter-title">热词过滤器</h3>
        <div class="filter-actions">
          <button
            @click="resetAllFilters"
            :disabled="!hasActiveFilters || loading"
            class="reset-button"
            type="button"
          >
            重置全部
          </button>
          <button
            @click="toggleCollapse"
            class="collapse-button"
            type="button"
          >
            {{ isCollapsed ? '展开' : '收起' }}
          </button>
        </div>
      </div>

      <!-- 快速过滤条件摘要 -->
      <div v-if="isCollapsed && hasActiveFilters" class="filter-summary">
        <div class="summary-tags">
          <span v-if="filterData.timeRange" class="summary-tag summary-tag--time">
            {{ getTimeRangeSummary() }}
          </span>
          <span v-if="filterData.searchCondition?.keywords.length" class="summary-tag summary-tag--keywords">
            关键词: {{ filterData.searchCondition.keywords.length }}个
          </span>
          <span v-if="filterData.limitConfig" class="summary-tag summary-tag--limit">
            显示: {{ filterData.limitConfig.limit }}条
          </span>
        </div>
      </div>
    </div>

    <!-- 过滤器内容区域 -->
    <Transition name="collapse">
      <div v-show="!isCollapsed" class="hot-word-filter__content">
        <!-- 过滤器网格布局 -->
        <div class="filter-grid">
          <!-- 时间范围选择器 -->
          <div class="filter-section">
            <TimeRangeSelector
              v-model="filterData.timeRange"
              :disabled="loading"
              @change="handleTimeRangeChange"
            />
          </div>

          <!-- 搜索条件过滤器 -->
          <div class="filter-section">
            <SearchConditionFilter
              v-model="filterData.searchCondition"
              :disabled="loading"
              :search-space-options="searchSpaceOptions"
              @change="handleSearchConditionChange"
            />
          </div>

          <!-- 热词数量限制选择器 -->
          <div class="filter-section">
            <HotWordLimitSelector
              v-model="filterData.limitConfig"
              :disabled="loading"
              :estimated-data-size="estimatedDataSize"
              @change="handleLimitConfigChange"
            />
          </div>
        </div>

        <!-- 高级选项 -->
        <div v-if="showAdvancedOptions" class="advanced-options">
          <div class="advanced-options-header">
            <h4 class="advanced-title">高级选项</h4>
            <button
              @click="toggleAdvancedOptions"
              class="toggle-advanced-button"
              type="button"
            >
              {{ showAdvancedOptions ? '隐藏' : '显示' }}
            </button>
          </div>

          <div class="advanced-options-content">
            <!-- 缓存控制 -->
            <div class="advanced-option">
              <label class="advanced-label">
                <input
                  v-model="advancedOptions.useCache"
                  type="checkbox"
                  class="advanced-checkbox"
                  :disabled="loading"
                />
                <span class="advanced-text">使用缓存数据</span>
                <span class="advanced-description">提高查询速度，但可能不是最新数据</span>
              </label>
            </div>

            <!-- 实时更新 -->
            <div class="advanced-option">
              <label class="advanced-label">
                <input
                  v-model="advancedOptions.realTimeUpdate"
                  type="checkbox"
                  class="advanced-checkbox"
                  :disabled="loading"
                />
                <span class="advanced-text">实时更新</span>
                <span class="advanced-description">数据变化时自动刷新结果</span>
              </label>
            </div>

            <!-- 导出选项 */
            <div class="advanced-option">
              <label for="export-format" class="advanced-label-block">导出格式</label>
              <select
                id="export-format"
                v-model="advancedOptions.exportFormat"
                class="advanced-select"
                :disabled="loading"
              >
                <option value="excel">Excel (.xlsx)</option>
                <option value="csv">CSV (.csv)</option>
                <option value="json">JSON (.json)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- 应用过滤器操作栏 -->
        <div class="filter-actions-bar">
          <div class="actions-left">
            <button
              @click="toggleAdvancedOptions"
              class="advanced-toggle-button"
              type="button"
            >
              <span class="toggle-icon">⚙️</span>
              高级选项
            </button>
          </div>

          <div class="actions-right">
            <button
              @click="saveFilterAsPreset"
              :disabled="!hasActiveFilters || loading"
              class="save-preset-button"
              type="button"
            >
              保存为预设
            </button>
            <button
              @click="applyFilters"
              :disabled="loading"
              class="apply-button"
              type="button"
            >
              <span v-if="loading" class="loading-spinner"></span>
              {{ loading ? '查询中...' : '应用过滤器' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 预设过滤器快捷选择 -->
    <div v-if="filterPresets.length > 0" class="filter-presets">
      <div class="presets-header">
        <span class="presets-title">快捷预设</span>
      </div>
      <div class="presets-list">
        <button
          v-for="preset in filterPresets"
          :key="preset.id"
          @click="loadFilterPreset(preset)"
          class="preset-button"
          type="button"
          :disabled="loading"
        >
          <span class="preset-name">{{ preset.name }}</span>
          <span class="preset-description">{{ preset.description }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import TimeRangeSelector, { type TimeRange } from './filters/TimeRangeSelector.vue'
import SearchConditionFilter, { type SearchCondition } from './filters/SearchConditionFilter.vue'
import HotWordLimitSelector, { type LimitConfig } from './filters/HotWordLimitSelector.vue'

export interface HotWordFilterData {
  timeRange: TimeRange | null
  searchCondition: SearchCondition | null
  limitConfig: LimitConfig | null
}

export interface AdvancedOptions {
  useCache: boolean
  realTimeUpdate: boolean
  exportFormat: 'excel' | 'csv' | 'json'
}

export interface FilterPreset {
  id: string
  name: string
  description: string
  filters: HotWordFilterData
  advancedOptions: AdvancedOptions
}

interface SearchSpaceOption {
  label: string
  value: string
  description?: string
}

interface Props {
  modelValue?: HotWordFilterData | null
  loading?: boolean
  searchSpaceOptions?: SearchSpaceOption[]
  estimatedDataSize?: number
  filterPresets?: FilterPreset[]
  autoApply?: boolean
  showAdvancedOptions?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: HotWordFilterData): void
  (e: 'apply', filters: HotWordFilterData, advanced: AdvancedOptions): void
  (e: 'reset'): void
  (e: 'save-preset', filters: HotWordFilterData, advanced: AdvancedOptions): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  loading: false,
  searchSpaceOptions: () => [],
  estimatedDataSize: 0,
  filterPresets: () => [],
  autoApply: false,
  showAdvancedOptions: false
})

const emit = defineEmits<Emits>()

// 响应式数据
const isCollapsed = ref(false)
const showAdvancedOptions = ref(props.showAdvancedOptions)

const filterData = ref<HotWordFilterData>({
  timeRange: null,
  searchCondition: null,
  limitConfig: null
})

const advancedOptions = ref<AdvancedOptions>({
  useCache: true,
  realTimeUpdate: false,
  exportFormat: 'excel'
})

// 计算属性
const hasActiveFilters = computed(() => {
  return !!(
    filterData.value.timeRange ||
    filterData.value.searchCondition ||
    filterData.value.limitConfig
  )
})

// 方法
const handleTimeRangeChange = (timeRange: TimeRange | null) => {
  filterData.value.timeRange = timeRange
  emitModelValue()
  if (props.autoApply) {
    applyFilters()
  }
}

const handleSearchConditionChange = (searchCondition: SearchCondition) => {
  filterData.value.searchCondition = searchCondition
  emitModelValue()
  if (props.autoApply) {
    applyFilters()
  }
}

const handleLimitConfigChange = (limitConfig: LimitConfig) => {
  filterData.value.limitConfig = limitConfig
  emitModelValue()
  if (props.autoApply) {
    applyFilters()
  }
}

const emitModelValue = () => {
  emit('update:modelValue', { ...filterData.value })
}

const applyFilters = () => {
  emit('apply', { ...filterData.value }, { ...advancedOptions.value })
}

const resetAllFilters = () => {
  filterData.value = {
    timeRange: null,
    searchCondition: null,
    limitConfig: null
  }

  advancedOptions.value = {
    useCache: true,
    realTimeUpdate: false,
    exportFormat: 'excel'
  }

  emitModelValue()
  emit('reset')
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const toggleAdvancedOptions = () => {
  showAdvancedOptions.value = !showAdvancedOptions.value
}

const saveFilterAsPreset = () => {
  emit('save-preset', { ...filterData.value }, { ...advancedOptions.value })
}

const loadFilterPreset = (preset: FilterPreset) => {
  filterData.value = { ...preset.filters }
  advancedOptions.value = { ...preset.advancedOptions }
  emitModelValue()

  if (props.autoApply) {
    nextTick(() => {
      applyFilters()
    })
  }
}

const getTimeRangeSummary = (): string => {
  if (!filterData.value.timeRange) return ''

  if (filterData.value.timeRange.label) {
    return filterData.value.timeRange.label
  }

  const start = filterData.value.timeRange.start.toLocaleDateString('zh-CN')
  const end = filterData.value.timeRange.end.toLocaleDateString('zh-CN')
  return `${start} - ${end}`
}

// 初始化组件
const initializeComponent = () => {
  if (props.modelValue) {
    filterData.value = { ...props.modelValue }
  }
}

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    filterData.value = { ...newValue }
  }
}, { deep: true })

// 组件挂载时初始化
initializeComponent()
</script>

<style scoped>
.hot-word-filter {
  @apply bg-white rounded-lg shadow-sm border border-green-200;
}

/* 过滤器标题栏 */
.hot-word-filter__header {
  @apply p-4 border-b border-gray-100;
}

.filter-header-content {
  @apply flex items-center justify-between;
}

.filter-title {
  @apply text-lg font-semibold text-gray-800;
}

.filter-actions {
  @apply flex items-center space-x-2;
}

.reset-button {
  @apply px-3 py-1.5 text-sm font-medium text-red-600 hover:text-red-700 hover:bg-red-50 rounded-md transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed;
}

.collapse-button {
  @apply px-3 py-1.5 text-sm font-medium text-gray-600 hover:text-gray-700 hover:bg-gray-50 rounded-md transition-colors duration-200;
}

/* 快速摘要 */
.filter-summary {
  @apply mt-3;
}

.summary-tags {
  @apply flex flex-wrap gap-2;
}

.summary-tag {
  @apply px-2 py-1 text-xs font-medium rounded-full;
}

.summary-tag--time {
  @apply bg-blue-100 text-blue-800;
}

.summary-tag--keywords {
  @apply bg-green-100 text-green-800;
}

.summary-tag--limit {
  @apply bg-purple-100 text-purple-800;
}

/* 过滤器内容区域 */
.hot-word-filter__content {
  @apply p-4 space-y-6;
}

.filter-grid {
  @apply grid grid-cols-1 lg:grid-cols-3 gap-6;
}

.filter-section {
  @apply space-y-4;
}

/* 高级选项 */
.advanced-options {
  @apply border-t border-gray-100 pt-6 space-y-4;
}

.advanced-options-header {
  @apply flex items-center justify-between;
}

.advanced-title {
  @apply text-sm font-medium text-gray-700;
}

.toggle-advanced-button {
  @apply text-xs text-green-600 hover:text-green-700 font-medium;
}

.advanced-options-content {
  @apply grid grid-cols-1 md:grid-cols-3 gap-4;
}

.advanced-option {
  @apply space-y-2;
}

.advanced-label {
  @apply flex items-start space-x-2 cursor-pointer;
}

.advanced-label-block {
  @apply block text-xs font-medium text-gray-600 mb-1;
}

.advanced-checkbox {
  @apply mt-0.5 w-4 h-4 text-green-600 border-gray-300 rounded focus:ring-green-500;
}

.advanced-text {
  @apply text-sm font-medium text-gray-700;
}

.advanced-description {
  @apply block text-xs text-gray-500 mt-1 ml-6;
}

.advanced-select {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500;
}

/* 操作栏 */
.filter-actions-bar {
  @apply flex items-center justify-between pt-6 border-t border-gray-100;
}

.actions-left {
  @apply flex items-center;
}

.actions-right {
  @apply flex items-center space-x-3;
}

.advanced-toggle-button {
  @apply flex items-center space-x-2 px-3 py-2 text-sm font-medium text-gray-600 hover:text-gray-700 hover:bg-gray-50 rounded-md transition-colors duration-200;
}

.toggle-icon {
  @apply text-sm;
}

.save-preset-button {
  @apply px-4 py-2 text-sm font-medium text-green-600 border border-green-300 hover:bg-green-50 rounded-md transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed;
}

.apply-button {
  @apply flex items-center space-x-2 px-6 py-2 text-sm font-medium bg-green-600 text-white hover:bg-green-700 rounded-md transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed;
}

.loading-spinner {
  @apply w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin;
}

/* 预设过滤器 */
.filter-presets {
  @apply p-4 border-t border-gray-100 bg-gray-50;
}

.presets-header {
  @apply mb-3;
}

.presets-title {
  @apply text-sm font-medium text-gray-700;
}

.presets-list {
  @apply flex flex-wrap gap-2;
}

.preset-button {
  @apply p-3 text-left bg-white border border-gray-200 hover:border-green-300 hover:bg-green-50 rounded-md transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed;
}

.preset-name {
  @apply block text-sm font-medium text-gray-700;
}

.preset-description {
  @apply block text-xs text-gray-500 mt-1;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .filter-grid {
    @apply grid-cols-1 md:grid-cols-2;
  }
}

@media (max-width: 640px) {
  .hot-word-filter__header,
  .hot-word-filter__content {
    @apply p-3;
  }

  .filter-header-content {
    @apply flex-col space-y-2 items-start;
  }

  .filter-actions {
    @apply w-full justify-end;
  }

  .filter-grid {
    @apply grid-cols-1 gap-4;
  }

  .filter-actions-bar {
    @apply flex-col space-y-3 items-stretch;
  }

  .actions-left,
  .actions-right {
    @apply justify-center;
  }

  .advanced-options-content {
    @apply grid-cols-1;
  }

  .presets-list {
    @apply flex-col;
  }

  .preset-button {
    @apply w-full;
  }
}

/* 折叠动画 */
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.3s ease;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
  overflow: hidden;
}

.collapse-enter-to,
.collapse-leave-from {
  opacity: 1;
  max-height: 1000px;
}

/* 悬停效果 */
.reset-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.apply-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.preset-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 动画效果 */
.hot-word-filter {
  animation: slideIn 0.4s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>